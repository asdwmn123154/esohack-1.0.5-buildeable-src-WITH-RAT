package com.squareup.okhttp.internal.framed;

import com.squareup.okhttp.Protocol;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Source;
import okio.Timeout;

public final class Http2 implements Variant {
   private static final Logger logger = Logger.getLogger(Http2.FrameLogger.class.getName());
   private static final ByteString CONNECTION_PREFACE = ByteString.encodeUtf8("PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n");
   static final int INITIAL_MAX_FRAME_SIZE = 16384;
   static final byte TYPE_DATA = 0;
   static final byte TYPE_HEADERS = 1;
   static final byte TYPE_PRIORITY = 2;
   static final byte TYPE_RST_STREAM = 3;
   static final byte TYPE_SETTINGS = 4;
   static final byte TYPE_PUSH_PROMISE = 5;
   static final byte TYPE_PING = 6;
   static final byte TYPE_GOAWAY = 7;
   static final byte TYPE_WINDOW_UPDATE = 8;
   static final byte TYPE_CONTINUATION = 9;
   static final byte FLAG_NONE = 0;
   static final byte FLAG_ACK = 1;
   static final byte FLAG_END_STREAM = 1;
   static final byte FLAG_END_HEADERS = 4;
   static final byte FLAG_END_PUSH_PROMISE = 4;
   static final byte FLAG_PADDED = 8;
   static final byte FLAG_PRIORITY = 32;
   static final byte FLAG_COMPRESSED = 32;

   public Protocol getProtocol() {
      return Protocol.HTTP_2;
   }

   public FrameReader newReader(BufferedSource source, boolean client) {
      return new Http2.Reader(source, 4096, client);
   }

   public FrameWriter newWriter(BufferedSink sink, boolean client) {
      return new Http2.Writer(sink, client);
   }

   private static IllegalArgumentException illegalArgument(String message, Object... args) {
      throw new IllegalArgumentException(String.format(message, args));
   }

   private static IOException ioException(String message, Object... args) throws IOException {
      throw new IOException(String.format(message, args));
   }

   private static int lengthWithoutPadding(int length, byte flags, short padding) throws IOException {
      if ((flags & 8) != 0) {
         --length;
      }

      if (padding > length) {
         throw ioException("PROTOCOL_ERROR padding %s > remaining length %s", padding, length);
      } else {
         return (short)(length - padding);
      }
   }

   private static int readMedium(BufferedSource source) throws IOException {
      return (source.readByte() & 255) << 16 | (source.readByte() & 255) << 8 | source.readByte() & 255;
   }

   private static void writeMedium(BufferedSink sink, int i) throws IOException {
      sink.writeByte(i >>> 16 & 255);
      sink.writeByte(i >>> 8 & 255);
      sink.writeByte(i & 255);
   }

   static final class FrameLogger {
      private static final String[] TYPES = new String[]{"DATA", "HEADERS", "PRIORITY", "RST_STREAM", "SETTINGS", "PUSH_PROMISE", "PING", "GOAWAY", "WINDOW_UPDATE", "CONTINUATION"};
      private static final String[] FLAGS = new String[64];
      private static final String[] BINARY = new String[256];

      static String formatHeader(boolean inbound, int streamId, int length, byte type, byte flags) {
         String formattedType = type < TYPES.length ? TYPES[type] : String.format("0x%02x", type);
         String formattedFlags = formatFlags(type, flags);
         return String.format("%s 0x%08x %5d %-13s %s", inbound ? "<<" : ">>", streamId, length, formattedType, formattedFlags);
      }

      static String formatFlags(byte type, byte flags) {
         if (flags == 0) {
            return "";
         } else {
            switch(type) {
            case 2:
            case 3:
            case 7:
            case 8:
               return BINARY[flags];
            case 4:
            case 6:
               return flags == 1 ? "ACK" : BINARY[flags];
            case 5:
            default:
               String result = flags < FLAGS.length ? FLAGS[flags] : BINARY[flags];
               if (type == 5 && (flags & 4) != 0) {
                  return result.replace("HEADERS", "PUSH_PROMISE");
               } else {
                  return type == 0 && (flags & 32) != 0 ? result.replace("PRIORITY", "COMPRESSED") : result;
               }
            }
         }
      }

      static {
         for(int i = 0; i < BINARY.length; ++i) {
            BINARY[i] = String.format("%8s", Integer.toBinaryString(i)).replace(' ', '0');
         }

         FLAGS[0] = "";
         FLAGS[1] = "END_STREAM";
         int[] prefixFlags = new int[]{1};
         FLAGS[8] = "PADDED";
         int[] frameFlags = prefixFlags;
         int i = prefixFlags.length;

         int var3;
         int prefixFlag;
         for(var3 = 0; var3 < i; ++var3) {
            prefixFlag = frameFlags[var3];
            FLAGS[prefixFlag | 8] = FLAGS[prefixFlag] + "|PADDED";
         }

         FLAGS[4] = "END_HEADERS";
         FLAGS[32] = "PRIORITY";
         FLAGS[36] = "END_HEADERS|PRIORITY";
         frameFlags = new int[]{4, 32, 36};
         int[] var11 = frameFlags;
         var3 = frameFlags.length;

         for(prefixFlag = 0; prefixFlag < var3; ++prefixFlag) {
            int frameFlag = var11[prefixFlag];
            int[] var6 = prefixFlags;
            int var7 = prefixFlags.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               int prefixFlag = var6[var8];
               FLAGS[prefixFlag | frameFlag] = FLAGS[prefixFlag] + '|' + FLAGS[frameFlag];
               FLAGS[prefixFlag | frameFlag | 8] = FLAGS[prefixFlag] + '|' + FLAGS[frameFlag] + "|PADDED";
            }
         }

         for(i = 0; i < FLAGS.length; ++i) {
            if (FLAGS[i] == null) {
               FLAGS[i] = BINARY[i];
            }
         }

      }
   }

   static final class ContinuationSource implements Source {
      private final BufferedSource source;
      int length;
      byte flags;
      int streamId;
      int left;
      short padding;

      public ContinuationSource(BufferedSource source) {
         this.source = source;
      }

      public long read(Buffer sink, long byteCount) throws IOException {
         while(this.left == 0) {
            this.source.skip((long)this.padding);
            this.padding = 0;
            if ((this.flags & 4) != 0) {
               return -1L;
            }

            this.readContinuationHeader();
         }

         long read = this.source.read(sink, Math.min(byteCount, (long)this.left));
         if (read == -1L) {
            return -1L;
         } else {
            this.left = (int)((long)this.left - read);
            return read;
         }
      }

      public Timeout timeout() {
         return this.source.timeout();
      }

      public void close() throws IOException {
      }

      private void readContinuationHeader() throws IOException {
         int previousStreamId = this.streamId;
         this.length = this.left = Http2.readMedium(this.source);
         byte type = (byte)(this.source.readByte() & 255);
         this.flags = (byte)(this.source.readByte() & 255);
         if (Http2.logger.isLoggable(Level.FINE)) {
            Http2.logger.fine(Http2.FrameLogger.formatHeader(true, this.streamId, this.length, type, this.flags));
         }

         this.streamId = this.source.readInt() & Integer.MAX_VALUE;
         if (type != 9) {
            throw Http2.ioException("%s != TYPE_CONTINUATION", type);
         } else if (this.streamId != previousStreamId) {
            throw Http2.ioException("TYPE_CONTINUATION streamId changed");
         }
      }
   }

   static final class Writer implements FrameWriter {
      private final BufferedSink sink;
      private final boolean client;
      private final Buffer hpackBuffer;
      private final Hpack.Writer hpackWriter;
      private int maxFrameSize;
      private boolean closed;

      Writer(BufferedSink sink, boolean client) {
         this.sink = sink;
         this.client = client;
         this.hpackBuffer = new Buffer();
         this.hpackWriter = new Hpack.Writer(this.hpackBuffer);
         this.maxFrameSize = 16384;
      }

      public synchronized void flush() throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            this.sink.flush();
         }
      }

      public synchronized void ackSettings(Settings peerSettings) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            this.maxFrameSize = peerSettings.getMaxFrameSize(this.maxFrameSize);
            int length = 0;
            byte type = 4;
            byte flags = 1;
            int streamId = 0;
            this.frameHeader(streamId, length, type, flags);
            this.sink.flush();
         }
      }

      public synchronized void connectionPreface() throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else if (this.client) {
            if (Http2.logger.isLoggable(Level.FINE)) {
               Http2.logger.fine(String.format(">> CONNECTION %s", Http2.CONNECTION_PREFACE.hex()));
            }

            this.sink.write(Http2.CONNECTION_PREFACE.toByteArray());
            this.sink.flush();
         }
      }

      public synchronized void synStream(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock) throws IOException {
         if (inFinished) {
            throw new UnsupportedOperationException();
         } else if (this.closed) {
            throw new IOException("closed");
         } else {
            this.headers(outFinished, streamId, headerBlock);
         }
      }

      public synchronized void synReply(boolean outFinished, int streamId, List<Header> headerBlock) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            this.headers(outFinished, streamId, headerBlock);
         }
      }

      public synchronized void headers(int streamId, List<Header> headerBlock) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            this.headers(false, streamId, headerBlock);
         }
      }

      public synchronized void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            this.hpackWriter.writeHeaders(requestHeaders);
            long byteCount = this.hpackBuffer.size();
            int length = (int)Math.min((long)(this.maxFrameSize - 4), byteCount);
            byte type = 5;
            byte flags = byteCount == (long)length ? 4 : 0;
            this.frameHeader(streamId, length + 4, type, (byte)flags);
            this.sink.writeInt(promisedStreamId & Integer.MAX_VALUE);
            this.sink.write(this.hpackBuffer, (long)length);
            if (byteCount > (long)length) {
               this.writeContinuationFrames(streamId, byteCount - (long)length);
            }

         }
      }

      void headers(boolean outFinished, int streamId, List<Header> headerBlock) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            this.hpackWriter.writeHeaders(headerBlock);
            long byteCount = this.hpackBuffer.size();
            int length = (int)Math.min((long)this.maxFrameSize, byteCount);
            byte type = 1;
            byte flags = byteCount == (long)length ? 4 : 0;
            if (outFinished) {
               flags = (byte)(flags | 1);
            }

            this.frameHeader(streamId, length, type, (byte)flags);
            this.sink.write(this.hpackBuffer, (long)length);
            if (byteCount > (long)length) {
               this.writeContinuationFrames(streamId, byteCount - (long)length);
            }

         }
      }

      private void writeContinuationFrames(int streamId, long byteCount) throws IOException {
         while(byteCount > 0L) {
            int length = (int)Math.min((long)this.maxFrameSize, byteCount);
            byteCount -= (long)length;
            this.frameHeader(streamId, length, (byte)9, (byte)(byteCount == 0L ? 4 : 0));
            this.sink.write(this.hpackBuffer, (long)length);
         }

      }

      public synchronized void rstStream(int streamId, ErrorCode errorCode) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else if (errorCode.httpCode == -1) {
            throw new IllegalArgumentException();
         } else {
            int length = 4;
            byte type = 3;
            byte flags = 0;
            this.frameHeader(streamId, length, type, flags);
            this.sink.writeInt(errorCode.httpCode);
            this.sink.flush();
         }
      }

      public int maxDataLength() {
         return this.maxFrameSize;
      }

      public synchronized void data(boolean outFinished, int streamId, Buffer source, int byteCount) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            byte flags = 0;
            if (outFinished) {
               flags = (byte)(flags | 1);
            }

            this.dataFrame(streamId, flags, source, byteCount);
         }
      }

      void dataFrame(int streamId, byte flags, Buffer buffer, int byteCount) throws IOException {
         byte type = 0;
         this.frameHeader(streamId, byteCount, type, flags);
         if (byteCount > 0) {
            this.sink.write(buffer, (long)byteCount);
         }

      }

      public synchronized void settings(Settings settings) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            int length = settings.size() * 6;
            byte type = 4;
            byte flags = 0;
            int streamId = 0;
            this.frameHeader(streamId, length, type, flags);

            for(int i = 0; i < 10; ++i) {
               if (settings.isSet(i)) {
                  int id = i;
                  if (i == 4) {
                     id = 3;
                  } else if (i == 7) {
                     id = 4;
                  }

                  this.sink.writeShort(id);
                  this.sink.writeInt(settings.get(i));
               }
            }

            this.sink.flush();
         }
      }

      public synchronized void ping(boolean ack, int payload1, int payload2) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else {
            int length = 8;
            byte type = 6;
            byte flags = ack ? 1 : 0;
            int streamId = 0;
            this.frameHeader(streamId, length, type, (byte)flags);
            this.sink.writeInt(payload1);
            this.sink.writeInt(payload2);
            this.sink.flush();
         }
      }

      public synchronized void goAway(int lastGoodStreamId, ErrorCode errorCode, byte[] debugData) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else if (errorCode.httpCode == -1) {
            throw Http2.illegalArgument("errorCode.httpCode == -1");
         } else {
            int length = 8 + debugData.length;
            byte type = 7;
            byte flags = 0;
            int streamId = 0;
            this.frameHeader(streamId, length, type, flags);
            this.sink.writeInt(lastGoodStreamId);
            this.sink.writeInt(errorCode.httpCode);
            if (debugData.length > 0) {
               this.sink.write(debugData);
            }

            this.sink.flush();
         }
      }

      public synchronized void windowUpdate(int streamId, long windowSizeIncrement) throws IOException {
         if (this.closed) {
            throw new IOException("closed");
         } else if (windowSizeIncrement != 0L && windowSizeIncrement <= 2147483647L) {
            int length = 4;
            byte type = 8;
            byte flags = 0;
            this.frameHeader(streamId, length, type, flags);
            this.sink.writeInt((int)windowSizeIncrement);
            this.sink.flush();
         } else {
            throw Http2.illegalArgument("windowSizeIncrement == 0 || windowSizeIncrement > 0x7fffffffL: %s", windowSizeIncrement);
         }
      }

      public synchronized void close() throws IOException {
         this.closed = true;
         this.sink.close();
      }

      void frameHeader(int streamId, int length, byte type, byte flags) throws IOException {
         if (Http2.logger.isLoggable(Level.FINE)) {
            Http2.logger.fine(Http2.FrameLogger.formatHeader(false, streamId, length, type, flags));
         }

         if (length > this.maxFrameSize) {
            throw Http2.illegalArgument("FRAME_SIZE_ERROR length > %d: %d", this.maxFrameSize, length);
         } else if ((streamId & Integer.MIN_VALUE) != 0) {
            throw Http2.illegalArgument("reserved bit set: %s", streamId);
         } else {
            Http2.writeMedium(this.sink, length);
            this.sink.writeByte(type & 255);
            this.sink.writeByte(flags & 255);
            this.sink.writeInt(streamId & Integer.MAX_VALUE);
         }
      }
   }

   static final class Reader implements FrameReader {
      private final BufferedSource source;
      private final Http2.ContinuationSource continuation;
      private final boolean client;
      final Hpack.Reader hpackReader;

      Reader(BufferedSource source, int headerTableSize, boolean client) {
         this.source = source;
         this.client = client;
         this.continuation = new Http2.ContinuationSource(this.source);
         this.hpackReader = new Hpack.Reader(headerTableSize, this.continuation);
      }

      public void readConnectionPreface() throws IOException {
         if (!this.client) {
            ByteString connectionPreface = this.source.readByteString((long)Http2.CONNECTION_PREFACE.size());
            if (Http2.logger.isLoggable(Level.FINE)) {
               Http2.logger.fine(String.format("<< CONNECTION %s", connectionPreface.hex()));
            }

            if (!Http2.CONNECTION_PREFACE.equals(connectionPreface)) {
               throw Http2.ioException("Expected a connection header but was %s", connectionPreface.utf8());
            }
         }
      }

      public boolean nextFrame(FrameReader.Handler handler) throws IOException {
         try {
            this.source.require(9L);
         } catch (IOException var6) {
            return false;
         }

         int length = Http2.readMedium(this.source);
         if (length >= 0 && length <= 16384) {
            byte type = (byte)(this.source.readByte() & 255);
            byte flags = (byte)(this.source.readByte() & 255);
            int streamId = this.source.readInt() & Integer.MAX_VALUE;
            if (Http2.logger.isLoggable(Level.FINE)) {
               Http2.logger.fine(Http2.FrameLogger.formatHeader(true, streamId, length, type, flags));
            }

            switch(type) {
            case 0:
               this.readData(handler, length, flags, streamId);
               break;
            case 1:
               this.readHeaders(handler, length, flags, streamId);
               break;
            case 2:
               this.readPriority(handler, length, flags, streamId);
               break;
            case 3:
               this.readRstStream(handler, length, flags, streamId);
               break;
            case 4:
               this.readSettings(handler, length, flags, streamId);
               break;
            case 5:
               this.readPushPromise(handler, length, flags, streamId);
               break;
            case 6:
               this.readPing(handler, length, flags, streamId);
               break;
            case 7:
               this.readGoAway(handler, length, flags, streamId);
               break;
            case 8:
               this.readWindowUpdate(handler, length, flags, streamId);
               break;
            default:
               this.source.skip((long)length);
            }

            return true;
         } else {
            throw Http2.ioException("FRAME_SIZE_ERROR: %s", length);
         }
      }

      private void readHeaders(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         if (streamId == 0) {
            throw Http2.ioException("PROTOCOL_ERROR: TYPE_HEADERS streamId == 0");
         } else {
            boolean endStream = (flags & 1) != 0;
            short padding = (flags & 8) != 0 ? (short)(this.source.readByte() & 255) : 0;
            if ((flags & 32) != 0) {
               this.readPriority(handler, streamId);
               length -= 5;
            }

            length = Http2.lengthWithoutPadding(length, flags, padding);
            List<Header> headerBlock = this.readHeaderBlock(length, padding, flags, streamId);
            handler.headers(false, endStream, streamId, -1, headerBlock, HeadersMode.HTTP_20_HEADERS);
         }
      }

      private List<Header> readHeaderBlock(int length, short padding, byte flags, int streamId) throws IOException {
         this.continuation.length = this.continuation.left = length;
         this.continuation.padding = padding;
         this.continuation.flags = flags;
         this.continuation.streamId = streamId;
         this.hpackReader.readHeaders();
         return this.hpackReader.getAndResetHeaderList();
      }

      private void readData(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         boolean inFinished = (flags & 1) != 0;
         boolean gzipped = (flags & 32) != 0;
         if (gzipped) {
            throw Http2.ioException("PROTOCOL_ERROR: FLAG_COMPRESSED without SETTINGS_COMPRESS_DATA");
         } else {
            short padding = (flags & 8) != 0 ? (short)(this.source.readByte() & 255) : 0;
            length = Http2.lengthWithoutPadding(length, flags, padding);
            handler.data(inFinished, streamId, this.source, length);
            this.source.skip((long)padding);
         }
      }

      private void readPriority(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         if (length != 5) {
            throw Http2.ioException("TYPE_PRIORITY length: %d != 5", length);
         } else if (streamId == 0) {
            throw Http2.ioException("TYPE_PRIORITY streamId == 0");
         } else {
            this.readPriority(handler, streamId);
         }
      }

      private void readPriority(FrameReader.Handler handler, int streamId) throws IOException {
         int w1 = this.source.readInt();
         boolean exclusive = (w1 & Integer.MIN_VALUE) != 0;
         int streamDependency = w1 & Integer.MAX_VALUE;
         int weight = (this.source.readByte() & 255) + 1;
         handler.priority(streamId, streamDependency, weight, exclusive);
      }

      private void readRstStream(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         if (length != 4) {
            throw Http2.ioException("TYPE_RST_STREAM length: %d != 4", length);
         } else if (streamId == 0) {
            throw Http2.ioException("TYPE_RST_STREAM streamId == 0");
         } else {
            int errorCodeInt = this.source.readInt();
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
               throw Http2.ioException("TYPE_RST_STREAM unexpected error code: %d", errorCodeInt);
            } else {
               handler.rstStream(streamId, errorCode);
            }
         }
      }

      private void readSettings(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         if (streamId != 0) {
            throw Http2.ioException("TYPE_SETTINGS streamId != 0");
         } else if ((flags & 1) != 0) {
            if (length != 0) {
               throw Http2.ioException("FRAME_SIZE_ERROR ack frame should be empty!");
            } else {
               handler.ackSettings();
            }
         } else if (length % 6 != 0) {
            throw Http2.ioException("TYPE_SETTINGS length %% 6 != 0: %s", length);
         } else {
            Settings settings = new Settings();

            for(int i = 0; i < length; i += 6) {
               short id = this.source.readShort();
               int value = this.source.readInt();
               switch(id) {
               case 1:
               case 6:
                  break;
               case 2:
                  if (value != 0 && value != 1) {
                     throw Http2.ioException("PROTOCOL_ERROR SETTINGS_ENABLE_PUSH != 0 or 1");
                  }
                  break;
               case 3:
                  id = 4;
                  break;
               case 4:
                  id = 7;
                  if (value < 0) {
                     throw Http2.ioException("PROTOCOL_ERROR SETTINGS_INITIAL_WINDOW_SIZE > 2^31 - 1");
                  }
                  break;
               case 5:
                  if (value < 16384 || value > 16777215) {
                     throw Http2.ioException("PROTOCOL_ERROR SETTINGS_MAX_FRAME_SIZE: %s", value);
                  }
                  break;
               default:
                  throw Http2.ioException("PROTOCOL_ERROR invalid settings id: %s", id);
               }

               settings.set(id, 0, value);
            }

            handler.settings(false, settings);
            if (settings.getHeaderTableSize() >= 0) {
               this.hpackReader.headerTableSizeSetting(settings.getHeaderTableSize());
            }

         }
      }

      private void readPushPromise(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         if (streamId == 0) {
            throw Http2.ioException("PROTOCOL_ERROR: TYPE_PUSH_PROMISE streamId == 0");
         } else {
            short padding = (flags & 8) != 0 ? (short)(this.source.readByte() & 255) : 0;
            int promisedStreamId = this.source.readInt() & Integer.MAX_VALUE;
            length -= 4;
            length = Http2.lengthWithoutPadding(length, flags, padding);
            List<Header> headerBlock = this.readHeaderBlock(length, padding, flags, streamId);
            handler.pushPromise(streamId, promisedStreamId, headerBlock);
         }
      }

      private void readPing(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         if (length != 8) {
            throw Http2.ioException("TYPE_PING length != 8: %s", length);
         } else if (streamId != 0) {
            throw Http2.ioException("TYPE_PING streamId != 0");
         } else {
            int payload1 = this.source.readInt();
            int payload2 = this.source.readInt();
            boolean ack = (flags & 1) != 0;
            handler.ping(ack, payload1, payload2);
         }
      }

      private void readGoAway(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         if (length < 8) {
            throw Http2.ioException("TYPE_GOAWAY length < 8: %s", length);
         } else if (streamId != 0) {
            throw Http2.ioException("TYPE_GOAWAY streamId != 0");
         } else {
            int lastStreamId = this.source.readInt();
            int errorCodeInt = this.source.readInt();
            int opaqueDataLength = length - 8;
            ErrorCode errorCode = ErrorCode.fromHttp2(errorCodeInt);
            if (errorCode == null) {
               throw Http2.ioException("TYPE_GOAWAY unexpected error code: %d", errorCodeInt);
            } else {
               ByteString debugData = ByteString.EMPTY;
               if (opaqueDataLength > 0) {
                  debugData = this.source.readByteString((long)opaqueDataLength);
               }

               handler.goAway(lastStreamId, errorCode, debugData);
            }
         }
      }

      private void readWindowUpdate(FrameReader.Handler handler, int length, byte flags, int streamId) throws IOException {
         if (length != 4) {
            throw Http2.ioException("TYPE_WINDOW_UPDATE length !=4: %s", length);
         } else {
            long increment = (long)this.source.readInt() & 2147483647L;
            if (increment == 0L) {
               throw Http2.ioException("windowSizeIncrement was 0", increment);
            } else {
               handler.windowUpdate(streamId, increment);
            }
         }
      }

      public void close() throws IOException {
         this.source.close();
      }
   }
}
