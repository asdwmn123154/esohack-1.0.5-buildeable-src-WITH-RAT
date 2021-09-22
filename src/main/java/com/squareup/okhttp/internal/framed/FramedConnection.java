package com.squareup.okhttp.internal.framed;

import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.internal.Internal;
import com.squareup.okhttp.internal.NamedRunnable;
import com.squareup.okhttp.internal.Util;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

public final class FramedConnection implements Closeable {
   private static final ExecutorService executor;
   final Protocol protocol;
   final boolean client;
   private final FramedConnection.Listener listener;
   private final Map<Integer, FramedStream> streams;
   private final String hostName;
   private int lastGoodStreamId;
   private int nextStreamId;
   private boolean shutdown;
   private long idleStartTimeNs;
   private final ExecutorService pushExecutor;
   private Map<Integer, Ping> pings;
   private final PushObserver pushObserver;
   private int nextPingId;
   long unacknowledgedBytesRead;
   long bytesLeftInWriteWindow;
   Settings okHttpSettings;
   private static final int OKHTTP_CLIENT_WINDOW_SIZE = 16777216;
   final Settings peerSettings;
   private boolean receivedInitialPeerSettings;
   final Variant variant;
   final Socket socket;
   final FrameWriter frameWriter;
   final FramedConnection.Reader readerRunnable;
   private final Set<Integer> currentPushRequests;

   private FramedConnection(FramedConnection.Builder builder) throws IOException {
      this.streams = new HashMap();
      this.idleStartTimeNs = System.nanoTime();
      this.unacknowledgedBytesRead = 0L;
      this.okHttpSettings = new Settings();
      this.peerSettings = new Settings();
      this.receivedInitialPeerSettings = false;
      this.currentPushRequests = new LinkedHashSet();
      this.protocol = builder.protocol;
      this.pushObserver = builder.pushObserver;
      this.client = builder.client;
      this.listener = builder.listener;
      this.nextStreamId = builder.client ? 1 : 2;
      if (builder.client && this.protocol == Protocol.HTTP_2) {
         this.nextStreamId += 2;
      }

      this.nextPingId = builder.client ? 1 : 2;
      if (builder.client) {
         this.okHttpSettings.set(7, 0, 16777216);
      }

      this.hostName = builder.hostName;
      if (this.protocol == Protocol.HTTP_2) {
         this.variant = new Http2();
         this.pushExecutor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory(String.format("OkHttp %s Push Observer", this.hostName), true));
         this.peerSettings.set(7, 0, 65535);
         this.peerSettings.set(5, 0, 16384);
      } else {
         if (this.protocol != Protocol.SPDY_3) {
            throw new AssertionError(this.protocol);
         }

         this.variant = new Spdy3();
         this.pushExecutor = null;
      }

      this.bytesLeftInWriteWindow = (long)this.peerSettings.getInitialWindowSize(65536);
      this.socket = builder.socket;
      this.frameWriter = this.variant.newWriter(builder.sink, this.client);
      this.readerRunnable = new FramedConnection.Reader(this.variant.newReader(builder.source, this.client));
      (new Thread(this.readerRunnable)).start();
   }

   public Protocol getProtocol() {
      return this.protocol;
   }

   public synchronized int openStreamCount() {
      return this.streams.size();
   }

   synchronized FramedStream getStream(int id) {
      return (FramedStream)this.streams.get(id);
   }

   synchronized FramedStream removeStream(int streamId) {
      FramedStream stream = (FramedStream)this.streams.remove(streamId);
      if (stream != null && this.streams.isEmpty()) {
         this.setIdle(true);
      }

      this.notifyAll();
      return stream;
   }

   private synchronized void setIdle(boolean value) {
      this.idleStartTimeNs = value ? System.nanoTime() : Long.MAX_VALUE;
   }

   public synchronized boolean isIdle() {
      return this.idleStartTimeNs != Long.MAX_VALUE;
   }

   public synchronized int maxConcurrentStreams() {
      return this.peerSettings.getMaxConcurrentStreams(Integer.MAX_VALUE);
   }

   public synchronized long getIdleStartTimeNs() {
      return this.idleStartTimeNs;
   }

   public FramedStream pushStream(int associatedStreamId, List<Header> requestHeaders, boolean out) throws IOException {
      if (this.client) {
         throw new IllegalStateException("Client cannot push requests.");
      } else if (this.protocol != Protocol.HTTP_2) {
         throw new IllegalStateException("protocol != HTTP_2");
      } else {
         return this.newStream(associatedStreamId, requestHeaders, out, false);
      }
   }

   public FramedStream newStream(List<Header> requestHeaders, boolean out, boolean in) throws IOException {
      return this.newStream(0, requestHeaders, out, in);
   }

   private FramedStream newStream(int associatedStreamId, List<Header> requestHeaders, boolean out, boolean in) throws IOException {
      boolean outFinished = !out;
      boolean inFinished = !in;
      FramedStream stream;
      synchronized(this.frameWriter) {
         int streamId;
         synchronized(this) {
            if (this.shutdown) {
               throw new IOException("shutdown");
            }

            streamId = this.nextStreamId;
            this.nextStreamId += 2;
            stream = new FramedStream(streamId, this, outFinished, inFinished, requestHeaders);
            if (stream.isOpen()) {
               this.streams.put(streamId, stream);
               this.setIdle(false);
            }
         }

         if (associatedStreamId == 0) {
            this.frameWriter.synStream(outFinished, inFinished, streamId, associatedStreamId, requestHeaders);
         } else {
            if (this.client) {
               throw new IllegalArgumentException("client streams shouldn't have associated stream IDs");
            }

            this.frameWriter.pushPromise(associatedStreamId, streamId, requestHeaders);
         }
      }

      if (!out) {
         this.frameWriter.flush();
      }

      return stream;
   }

   void writeSynReply(int streamId, boolean outFinished, List<Header> alternating) throws IOException {
      this.frameWriter.synReply(outFinished, streamId, alternating);
   }

   public void writeData(int streamId, boolean outFinished, Buffer buffer, long byteCount) throws IOException {
      if (byteCount == 0L) {
         this.frameWriter.data(outFinished, streamId, buffer, 0);
      } else {
         while(byteCount > 0L) {
            int toWrite;
            synchronized(this) {
               try {
                  while(this.bytesLeftInWriteWindow <= 0L) {
                     if (!this.streams.containsKey(streamId)) {
                        throw new IOException("stream closed");
                     }

                     this.wait();
                  }
               } catch (InterruptedException var10) {
                  throw new InterruptedIOException();
               }

               toWrite = (int)Math.min(byteCount, this.bytesLeftInWriteWindow);
               toWrite = Math.min(toWrite, this.frameWriter.maxDataLength());
               this.bytesLeftInWriteWindow -= (long)toWrite;
            }

            byteCount -= (long)toWrite;
            this.frameWriter.data(outFinished && byteCount == 0L, streamId, buffer, toWrite);
         }

      }
   }

   void addBytesToWriteWindow(long delta) {
      this.bytesLeftInWriteWindow += delta;
      if (delta > 0L) {
         this.notifyAll();
      }

   }

   void writeSynResetLater(final int streamId, final ErrorCode errorCode) {
      executor.submit(new NamedRunnable("OkHttp %s stream %d", new Object[]{this.hostName, streamId}) {
         public void execute() {
            try {
               FramedConnection.this.writeSynReset(streamId, errorCode);
            } catch (IOException var2) {
            }

         }
      });
   }

   void writeSynReset(int streamId, ErrorCode statusCode) throws IOException {
      this.frameWriter.rstStream(streamId, statusCode);
   }

   void writeWindowUpdateLater(final int streamId, final long unacknowledgedBytesRead) {
      executor.execute(new NamedRunnable("OkHttp Window Update %s stream %d", new Object[]{this.hostName, streamId}) {
         public void execute() {
            try {
               FramedConnection.this.frameWriter.windowUpdate(streamId, unacknowledgedBytesRead);
            } catch (IOException var2) {
            }

         }
      });
   }

   public Ping ping() throws IOException {
      Ping ping = new Ping();
      int pingId;
      synchronized(this) {
         if (this.shutdown) {
            throw new IOException("shutdown");
         }

         pingId = this.nextPingId;
         this.nextPingId += 2;
         if (this.pings == null) {
            this.pings = new HashMap();
         }

         this.pings.put(pingId, ping);
      }

      this.writePing(false, pingId, 1330343787, ping);
      return ping;
   }

   private void writePingLater(final boolean reply, final int payload1, final int payload2, final Ping ping) {
      executor.execute(new NamedRunnable("OkHttp %s ping %08x%08x", new Object[]{this.hostName, payload1, payload2}) {
         public void execute() {
            try {
               FramedConnection.this.writePing(reply, payload1, payload2, ping);
            } catch (IOException var2) {
            }

         }
      });
   }

   private void writePing(boolean reply, int payload1, int payload2, Ping ping) throws IOException {
      synchronized(this.frameWriter) {
         if (ping != null) {
            ping.send();
         }

         this.frameWriter.ping(reply, payload1, payload2);
      }
   }

   private synchronized Ping removePing(int id) {
      return this.pings != null ? (Ping)this.pings.remove(id) : null;
   }

   public void flush() throws IOException {
      this.frameWriter.flush();
   }

   public void shutdown(ErrorCode statusCode) throws IOException {
      synchronized(this.frameWriter) {
         int lastGoodStreamId;
         synchronized(this) {
            if (this.shutdown) {
               return;
            }

            this.shutdown = true;
            lastGoodStreamId = this.lastGoodStreamId;
         }

         this.frameWriter.goAway(lastGoodStreamId, statusCode, Util.EMPTY_BYTE_ARRAY);
      }
   }

   public void close() throws IOException {
      this.close(ErrorCode.NO_ERROR, ErrorCode.CANCEL);
   }

   private void close(ErrorCode connectionCode, ErrorCode streamCode) throws IOException {
      assert !Thread.holdsLock(this);

      IOException thrown = null;

      try {
         this.shutdown(connectionCode);
      } catch (IOException var12) {
         thrown = var12;
      }

      FramedStream[] streamsToClose = null;
      Ping[] pingsToCancel = null;
      synchronized(this) {
         if (!this.streams.isEmpty()) {
            streamsToClose = (FramedStream[])this.streams.values().toArray(new FramedStream[this.streams.size()]);
            this.streams.clear();
            this.setIdle(false);
         }

         if (this.pings != null) {
            pingsToCancel = (Ping[])this.pings.values().toArray(new Ping[this.pings.size()]);
            this.pings = null;
         }
      }

      int var7;
      int var8;
      if (streamsToClose != null) {
         FramedStream[] var6 = streamsToClose;
         var7 = streamsToClose.length;

         for(var8 = 0; var8 < var7; ++var8) {
            FramedStream stream = var6[var8];

            try {
               stream.close(streamCode);
            } catch (IOException var14) {
               if (thrown != null) {
                  thrown = var14;
               }
            }
         }
      }

      if (pingsToCancel != null) {
         Ping[] var16 = pingsToCancel;
         var7 = pingsToCancel.length;

         for(var8 = 0; var8 < var7; ++var8) {
            Ping ping = var16[var8];
            ping.cancel();
         }
      }

      try {
         this.frameWriter.close();
      } catch (IOException var13) {
         if (thrown == null) {
            thrown = var13;
         }
      }

      try {
         this.socket.close();
      } catch (IOException var11) {
         thrown = var11;
      }

      if (thrown != null) {
         throw thrown;
      }
   }

   public void sendConnectionPreface() throws IOException {
      this.frameWriter.connectionPreface();
      this.frameWriter.settings(this.okHttpSettings);
      int windowSize = this.okHttpSettings.getInitialWindowSize(65536);
      if (windowSize != 65536) {
         this.frameWriter.windowUpdate(0, (long)(windowSize - 65536));
      }

   }

   public void setSettings(Settings settings) throws IOException {
      synchronized(this.frameWriter) {
         synchronized(this) {
            if (this.shutdown) {
               throw new IOException("shutdown");
            }

            this.okHttpSettings.merge(settings);
            this.frameWriter.settings(settings);
         }

      }
   }

   private boolean pushedStream(int streamId) {
      return this.protocol == Protocol.HTTP_2 && streamId != 0 && (streamId & 1) == 0;
   }

   private void pushRequestLater(final int streamId, final List<Header> requestHeaders) {
      synchronized(this) {
         if (this.currentPushRequests.contains(streamId)) {
            this.writeSynResetLater(streamId, ErrorCode.PROTOCOL_ERROR);
            return;
         }

         this.currentPushRequests.add(streamId);
      }

      this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Request[%s]", new Object[]{this.hostName, streamId}) {
         public void execute() {
            boolean cancel = FramedConnection.this.pushObserver.onRequest(streamId, requestHeaders);

            try {
               if (cancel) {
                  FramedConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
                  synchronized(FramedConnection.this) {
                     FramedConnection.this.currentPushRequests.remove(streamId);
                  }
               }
            } catch (IOException var5) {
            }

         }
      });
   }

   private void pushHeadersLater(final int streamId, final List<Header> requestHeaders, final boolean inFinished) {
      this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Headers[%s]", new Object[]{this.hostName, streamId}) {
         public void execute() {
            boolean cancel = FramedConnection.this.pushObserver.onHeaders(streamId, requestHeaders, inFinished);

            try {
               if (cancel) {
                  FramedConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
               }

               if (cancel || inFinished) {
                  synchronized(FramedConnection.this) {
                     FramedConnection.this.currentPushRequests.remove(streamId);
                  }
               }
            } catch (IOException var5) {
            }

         }
      });
   }

   private void pushDataLater(final int streamId, BufferedSource source, final int byteCount, final boolean inFinished) throws IOException {
      final Buffer buffer = new Buffer();
      source.require((long)byteCount);
      source.read(buffer, (long)byteCount);
      if (buffer.size() != (long)byteCount) {
         throw new IOException(buffer.size() + " != " + byteCount);
      } else {
         this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Data[%s]", new Object[]{this.hostName, streamId}) {
            public void execute() {
               try {
                  boolean cancel = FramedConnection.this.pushObserver.onData(streamId, buffer, byteCount, inFinished);
                  if (cancel) {
                     FramedConnection.this.frameWriter.rstStream(streamId, ErrorCode.CANCEL);
                  }

                  if (cancel || inFinished) {
                     synchronized(FramedConnection.this) {
                        FramedConnection.this.currentPushRequests.remove(streamId);
                     }
                  }
               } catch (IOException var5) {
               }

            }
         });
      }
   }

   private void pushResetLater(final int streamId, final ErrorCode errorCode) {
      this.pushExecutor.execute(new NamedRunnable("OkHttp %s Push Reset[%s]", new Object[]{this.hostName, streamId}) {
         public void execute() {
            FramedConnection.this.pushObserver.onReset(streamId, errorCode);
            synchronized(FramedConnection.this) {
               FramedConnection.this.currentPushRequests.remove(streamId);
            }
         }
      });
   }

   // $FF: synthetic method
   FramedConnection(FramedConnection.Builder x0, Object x1) throws IOException {
      this(x0);
   }

   static {
      executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp FramedConnection", true));
   }

   public abstract static class Listener {
      public static final FramedConnection.Listener REFUSE_INCOMING_STREAMS = new FramedConnection.Listener() {
         public void onStream(FramedStream stream) throws IOException {
            stream.close(ErrorCode.REFUSED_STREAM);
         }
      };

      public abstract void onStream(FramedStream var1) throws IOException;

      public void onSettings(FramedConnection connection) {
      }
   }

   class Reader extends NamedRunnable implements FrameReader.Handler {
      final FrameReader frameReader;

      private Reader(FrameReader frameReader) {
         super("OkHttp %s", FramedConnection.this.hostName);
         this.frameReader = frameReader;
      }

      protected void execute() {
         ErrorCode connectionErrorCode = ErrorCode.INTERNAL_ERROR;
         ErrorCode streamErrorCode = ErrorCode.INTERNAL_ERROR;

         try {
            if (!FramedConnection.this.client) {
               this.frameReader.readConnectionPreface();
            }

            while(true) {
               if (!this.frameReader.nextFrame(this)) {
                  connectionErrorCode = ErrorCode.NO_ERROR;
                  streamErrorCode = ErrorCode.CANCEL;
                  break;
               }
            }
         } catch (IOException var12) {
            connectionErrorCode = ErrorCode.PROTOCOL_ERROR;
            streamErrorCode = ErrorCode.PROTOCOL_ERROR;
         } finally {
            try {
               FramedConnection.this.close(connectionErrorCode, streamErrorCode);
            } catch (IOException var11) {
            }

            Util.closeQuietly((Closeable)this.frameReader);
         }

      }

      public void data(boolean inFinished, int streamId, BufferedSource source, int length) throws IOException {
         if (FramedConnection.this.pushedStream(streamId)) {
            FramedConnection.this.pushDataLater(streamId, source, length, inFinished);
         } else {
            FramedStream dataStream = FramedConnection.this.getStream(streamId);
            if (dataStream == null) {
               FramedConnection.this.writeSynResetLater(streamId, ErrorCode.INVALID_STREAM);
               source.skip((long)length);
            } else {
               dataStream.receiveData(source, length);
               if (inFinished) {
                  dataStream.receiveFin();
               }

            }
         }
      }

      public void headers(boolean outFinished, boolean inFinished, int streamId, int associatedStreamId, List<Header> headerBlock, HeadersMode headersMode) {
         if (FramedConnection.this.pushedStream(streamId)) {
            FramedConnection.this.pushHeadersLater(streamId, headerBlock, inFinished);
         } else {
            FramedStream stream;
            synchronized(FramedConnection.this) {
               if (FramedConnection.this.shutdown) {
                  return;
               }

               stream = FramedConnection.this.getStream(streamId);
               if (stream == null) {
                  if (headersMode.failIfStreamAbsent()) {
                     FramedConnection.this.writeSynResetLater(streamId, ErrorCode.INVALID_STREAM);
                     return;
                  }

                  if (streamId <= FramedConnection.this.lastGoodStreamId) {
                     return;
                  }

                  if (streamId % 2 == FramedConnection.this.nextStreamId % 2) {
                     return;
                  }

                  final FramedStream newStream = new FramedStream(streamId, FramedConnection.this, outFinished, inFinished, headerBlock);
                  FramedConnection.this.lastGoodStreamId = streamId;
                  FramedConnection.this.streams.put(streamId, newStream);
                  FramedConnection.executor.execute(new NamedRunnable("OkHttp %s stream %d", new Object[]{FramedConnection.this.hostName, streamId}) {
                     public void execute() {
                        try {
                           FramedConnection.this.listener.onStream(newStream);
                        } catch (IOException var4) {
                           Internal.logger.log(Level.INFO, "FramedConnection.Listener failure for " + FramedConnection.this.hostName, var4);

                           try {
                              newStream.close(ErrorCode.PROTOCOL_ERROR);
                           } catch (IOException var3) {
                           }
                        }

                     }
                  });
                  return;
               }
            }

            if (headersMode.failIfStreamPresent()) {
               stream.closeLater(ErrorCode.PROTOCOL_ERROR);
               FramedConnection.this.removeStream(streamId);
            } else {
               stream.receiveHeaders(headerBlock, headersMode);
               if (inFinished) {
                  stream.receiveFin();
               }

            }
         }
      }

      public void rstStream(int streamId, ErrorCode errorCode) {
         if (FramedConnection.this.pushedStream(streamId)) {
            FramedConnection.this.pushResetLater(streamId, errorCode);
         } else {
            FramedStream rstStream = FramedConnection.this.removeStream(streamId);
            if (rstStream != null) {
               rstStream.receiveRstStream(errorCode);
            }

         }
      }

      public void settings(boolean clearPrevious, Settings newSettings) {
         long delta = 0L;
         FramedStream[] streamsToNotify = null;
         int priorWriteWindowSize;
         int peerInitialWindowSize;
         synchronized(FramedConnection.this) {
            priorWriteWindowSize = FramedConnection.this.peerSettings.getInitialWindowSize(65536);
            if (clearPrevious) {
               FramedConnection.this.peerSettings.clear();
            }

            FramedConnection.this.peerSettings.merge(newSettings);
            if (FramedConnection.this.getProtocol() == Protocol.HTTP_2) {
               this.ackSettingsLater(newSettings);
            }

            peerInitialWindowSize = FramedConnection.this.peerSettings.getInitialWindowSize(65536);
            if (peerInitialWindowSize != -1 && peerInitialWindowSize != priorWriteWindowSize) {
               delta = (long)(peerInitialWindowSize - priorWriteWindowSize);
               if (!FramedConnection.this.receivedInitialPeerSettings) {
                  FramedConnection.this.addBytesToWriteWindow(delta);
                  FramedConnection.this.receivedInitialPeerSettings = true;
               }

               if (!FramedConnection.this.streams.isEmpty()) {
                  streamsToNotify = (FramedStream[])FramedConnection.this.streams.values().toArray(new FramedStream[FramedConnection.this.streams.size()]);
               }
            }

            FramedConnection.executor.execute(new NamedRunnable("OkHttp %s settings", new Object[]{FramedConnection.this.hostName}) {
               public void execute() {
                  FramedConnection.this.listener.onSettings(FramedConnection.this);
               }
            });
         }

         if (streamsToNotify != null && delta != 0L) {
            FramedStream[] var6 = streamsToNotify;
            priorWriteWindowSize = streamsToNotify.length;

            for(peerInitialWindowSize = 0; peerInitialWindowSize < priorWriteWindowSize; ++peerInitialWindowSize) {
               FramedStream stream = var6[peerInitialWindowSize];
               synchronized(stream) {
                  stream.addBytesToWriteWindow(delta);
               }
            }
         }

      }

      private void ackSettingsLater(final Settings peerSettings) {
         FramedConnection.executor.execute(new NamedRunnable("OkHttp %s ACK Settings", new Object[]{FramedConnection.this.hostName}) {
            public void execute() {
               try {
                  FramedConnection.this.frameWriter.ackSettings(peerSettings);
               } catch (IOException var2) {
               }

            }
         });
      }

      public void ackSettings() {
      }

      public void ping(boolean reply, int payload1, int payload2) {
         if (reply) {
            Ping ping = FramedConnection.this.removePing(payload1);
            if (ping != null) {
               ping.receive();
            }
         } else {
            FramedConnection.this.writePingLater(true, payload1, payload2, (Ping)null);
         }

      }

      public void goAway(int lastGoodStreamId, ErrorCode errorCode, ByteString debugData) {
         if (debugData.size() > 0) {
         }

         FramedStream[] streamsCopy;
         synchronized(FramedConnection.this) {
            streamsCopy = (FramedStream[])FramedConnection.this.streams.values().toArray(new FramedStream[FramedConnection.this.streams.size()]);
            FramedConnection.this.shutdown = true;
         }

         FramedStream[] var5 = streamsCopy;
         int var6 = streamsCopy.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            FramedStream framedStream = var5[var7];
            if (framedStream.getId() > lastGoodStreamId && framedStream.isLocallyInitiated()) {
               framedStream.receiveRstStream(ErrorCode.REFUSED_STREAM);
               FramedConnection.this.removeStream(framedStream.getId());
            }
         }

      }

      public void windowUpdate(int streamId, long windowSizeIncrement) {
         if (streamId == 0) {
            synchronized(FramedConnection.this) {
               FramedConnection var10000 = FramedConnection.this;
               var10000.bytesLeftInWriteWindow += windowSizeIncrement;
               FramedConnection.this.notifyAll();
            }
         } else {
            FramedStream stream = FramedConnection.this.getStream(streamId);
            if (stream != null) {
               synchronized(stream) {
                  stream.addBytesToWriteWindow(windowSizeIncrement);
               }
            }
         }

      }

      public void priority(int streamId, int streamDependency, int weight, boolean exclusive) {
      }

      public void pushPromise(int streamId, int promisedStreamId, List<Header> requestHeaders) {
         FramedConnection.this.pushRequestLater(promisedStreamId, requestHeaders);
      }

      public void alternateService(int streamId, String origin, ByteString protocol, String host, int port, long maxAge) {
      }

      // $FF: synthetic method
      Reader(FrameReader x1, Object x2) {
         this(x1);
      }
   }

   public static class Builder {
      private Socket socket;
      private String hostName;
      private BufferedSource source;
      private BufferedSink sink;
      private FramedConnection.Listener listener;
      private Protocol protocol;
      private PushObserver pushObserver;
      private boolean client;

      public Builder(boolean client) throws IOException {
         this.listener = FramedConnection.Listener.REFUSE_INCOMING_STREAMS;
         this.protocol = Protocol.SPDY_3;
         this.pushObserver = PushObserver.CANCEL;
         this.client = client;
      }

      public FramedConnection.Builder socket(Socket socket) throws IOException {
         return this.socket(socket, ((InetSocketAddress)socket.getRemoteSocketAddress()).getHostName(), Okio.buffer(Okio.source(socket)), Okio.buffer(Okio.sink(socket)));
      }

      public FramedConnection.Builder socket(Socket socket, String hostName, BufferedSource source, BufferedSink sink) {
         this.socket = socket;
         this.hostName = hostName;
         this.source = source;
         this.sink = sink;
         return this;
      }

      public FramedConnection.Builder listener(FramedConnection.Listener listener) {
         this.listener = listener;
         return this;
      }

      public FramedConnection.Builder protocol(Protocol protocol) {
         this.protocol = protocol;
         return this;
      }

      public FramedConnection.Builder pushObserver(PushObserver pushObserver) {
         this.pushObserver = pushObserver;
         return this;
      }

      public FramedConnection build() throws IOException {
         return new FramedConnection(this);
      }
   }
}
