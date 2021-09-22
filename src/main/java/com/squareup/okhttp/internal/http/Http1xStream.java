package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.Internal;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.io.RealConnection;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.concurrent.TimeUnit;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingTimeout;
import okio.Okio;
import okio.Sink;
import okio.Source;
import okio.Timeout;

public final class Http1xStream implements HttpStream {
   private static final int STATE_IDLE = 0;
   private static final int STATE_OPEN_REQUEST_BODY = 1;
   private static final int STATE_WRITING_REQUEST_BODY = 2;
   private static final int STATE_READ_RESPONSE_HEADERS = 3;
   private static final int STATE_OPEN_RESPONSE_BODY = 4;
   private static final int STATE_READING_RESPONSE_BODY = 5;
   private static final int STATE_CLOSED = 6;
   private final StreamAllocation streamAllocation;
   private final BufferedSource source;
   private final BufferedSink sink;
   private HttpEngine httpEngine;
   private int state = 0;

   public Http1xStream(StreamAllocation streamAllocation, BufferedSource source, BufferedSink sink) {
      this.streamAllocation = streamAllocation;
      this.source = source;
      this.sink = sink;
   }

   public void setHttpEngine(HttpEngine httpEngine) {
      this.httpEngine = httpEngine;
   }

   public Sink createRequestBody(Request request, long contentLength) throws IOException {
      if ("chunked".equalsIgnoreCase(request.header("Transfer-Encoding"))) {
         return this.newChunkedSink();
      } else if (contentLength != -1L) {
         return this.newFixedLengthSink(contentLength);
      } else {
         throw new IllegalStateException("Cannot stream a request body without chunked encoding or a known content length!");
      }
   }

   public void cancel() {
      RealConnection connection = this.streamAllocation.connection();
      if (connection != null) {
         connection.cancel();
      }

   }

   public void writeRequestHeaders(Request request) throws IOException {
      this.httpEngine.writingRequestHeaders();
      String requestLine = RequestLine.get(request, this.httpEngine.getConnection().getRoute().getProxy().type());
      this.writeRequest(request.headers(), requestLine);
   }

   public Response.Builder readResponseHeaders() throws IOException {
      return this.readResponse();
   }

   public ResponseBody openResponseBody(Response response) throws IOException {
      Source source = this.getTransferStream(response);
      return new RealResponseBody(response.headers(), Okio.buffer(source));
   }

   private Source getTransferStream(Response response) throws IOException {
      if (!HttpEngine.hasBody(response)) {
         return this.newFixedLengthSource(0L);
      } else if ("chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) {
         return this.newChunkedSource(this.httpEngine);
      } else {
         long contentLength = OkHeaders.contentLength(response);
         return contentLength != -1L ? this.newFixedLengthSource(contentLength) : this.newUnknownLengthSource();
      }
   }

   public boolean isClosed() {
      return this.state == 6;
   }

   public void finishRequest() throws IOException {
      this.sink.flush();
   }

   public void writeRequest(Headers headers, String requestLine) throws IOException {
      if (this.state != 0) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.sink.writeUtf8(requestLine).writeUtf8("\r\n");
         int i = 0;

         for(int size = headers.size(); i < size; ++i) {
            this.sink.writeUtf8(headers.name(i)).writeUtf8(": ").writeUtf8(headers.value(i)).writeUtf8("\r\n");
         }

         this.sink.writeUtf8("\r\n");
         this.state = 1;
      }
   }

   public Response.Builder readResponse() throws IOException {
      if (this.state != 1 && this.state != 3) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         try {
            StatusLine statusLine;
            Response.Builder responseBuilder;
            do {
               statusLine = StatusLine.parse(this.source.readUtf8LineStrict());
               responseBuilder = (new Response.Builder()).protocol(statusLine.protocol).code(statusLine.code).message(statusLine.message).headers(this.readHeaders());
            } while(statusLine.code == 100);

            this.state = 4;
            return responseBuilder;
         } catch (EOFException var3) {
            IOException exception = new IOException("unexpected end of stream on " + this.streamAllocation);
            exception.initCause(var3);
            throw exception;
         }
      }
   }

   public Headers readHeaders() throws IOException {
      Headers.Builder headers = new Headers.Builder();

      String line;
      while((line = this.source.readUtf8LineStrict()).length() != 0) {
         Internal.instance.addLenient(headers, line);
      }

      return headers.build();
   }

   public Sink newChunkedSink() {
      if (this.state != 1) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 2;
         return new Http1xStream.ChunkedSink();
      }
   }

   public Sink newFixedLengthSink(long contentLength) {
      if (this.state != 1) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 2;
         return new Http1xStream.FixedLengthSink(contentLength);
      }
   }

   public void writeRequestBody(RetryableSink requestBody) throws IOException {
      if (this.state != 1) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 3;
         requestBody.writeToSocket(this.sink);
      }
   }

   public Source newFixedLengthSource(long length) throws IOException {
      if (this.state != 4) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 5;
         return new Http1xStream.FixedLengthSource(length);
      }
   }

   public Source newChunkedSource(HttpEngine httpEngine) throws IOException {
      if (this.state != 4) {
         throw new IllegalStateException("state: " + this.state);
      } else {
         this.state = 5;
         return new Http1xStream.ChunkedSource(httpEngine);
      }
   }

   public Source newUnknownLengthSource() throws IOException {
      if (this.state != 4) {
         throw new IllegalStateException("state: " + this.state);
      } else if (this.streamAllocation == null) {
         throw new IllegalStateException("streamAllocation == null");
      } else {
         this.state = 5;
         this.streamAllocation.noNewStreams();
         return new Http1xStream.UnknownLengthSource();
      }
   }

   private void detachTimeout(ForwardingTimeout timeout) {
      Timeout oldDelegate = timeout.delegate();
      timeout.setDelegate(Timeout.NONE);
      oldDelegate.clearDeadline();
      oldDelegate.clearTimeout();
   }

   private class UnknownLengthSource extends Http1xStream.AbstractSource {
      private boolean inputExhausted;

      private UnknownLengthSource() {
         super(null);
      }

      public long read(Buffer sink, long byteCount) throws IOException {
         if (byteCount < 0L) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
         } else if (this.closed) {
            throw new IllegalStateException("closed");
         } else if (this.inputExhausted) {
            return -1L;
         } else {
            long read = Http1xStream.this.source.read(sink, byteCount);
            if (read == -1L) {
               this.inputExhausted = true;
               this.endOfInput();
               return -1L;
            } else {
               return read;
            }
         }
      }

      public void close() throws IOException {
         if (!this.closed) {
            if (!this.inputExhausted) {
               this.unexpectedEndOfInput();
            }

            this.closed = true;
         }
      }

      // $FF: synthetic method
      UnknownLengthSource(Object x1) {
         this();
      }
   }

   private class ChunkedSource extends Http1xStream.AbstractSource {
      private static final long NO_CHUNK_YET = -1L;
      private long bytesRemainingInChunk = -1L;
      private boolean hasMoreChunks = true;
      private final HttpEngine httpEngine;

      ChunkedSource(HttpEngine httpEngine) throws IOException {
         super(null);
         this.httpEngine = httpEngine;
      }

      public long read(Buffer sink, long byteCount) throws IOException {
         if (byteCount < 0L) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
         } else if (this.closed) {
            throw new IllegalStateException("closed");
         } else if (!this.hasMoreChunks) {
            return -1L;
         } else {
            if (this.bytesRemainingInChunk == 0L || this.bytesRemainingInChunk == -1L) {
               this.readChunkSize();
               if (!this.hasMoreChunks) {
                  return -1L;
               }
            }

            long read = Http1xStream.this.source.read(sink, Math.min(byteCount, this.bytesRemainingInChunk));
            if (read == -1L) {
               this.unexpectedEndOfInput();
               throw new ProtocolException("unexpected end of stream");
            } else {
               this.bytesRemainingInChunk -= read;
               return read;
            }
         }
      }

      private void readChunkSize() throws IOException {
         if (this.bytesRemainingInChunk != -1L) {
            Http1xStream.this.source.readUtf8LineStrict();
         }

         try {
            this.bytesRemainingInChunk = Http1xStream.this.source.readHexadecimalUnsignedLong();
            String extensions = Http1xStream.this.source.readUtf8LineStrict().trim();
            if (this.bytesRemainingInChunk < 0L || !extensions.isEmpty() && !extensions.startsWith(";")) {
               throw new ProtocolException("expected chunk size and optional extensions but was \"" + this.bytesRemainingInChunk + extensions + "\"");
            }
         } catch (NumberFormatException var2) {
            throw new ProtocolException(var2.getMessage());
         }

         if (this.bytesRemainingInChunk == 0L) {
            this.hasMoreChunks = false;
            this.httpEngine.receiveHeaders(Http1xStream.this.readHeaders());
            this.endOfInput();
         }

      }

      public void close() throws IOException {
         if (!this.closed) {
            if (this.hasMoreChunks && !Util.discard(this, 100, TimeUnit.MILLISECONDS)) {
               this.unexpectedEndOfInput();
            }

            this.closed = true;
         }
      }
   }

   private class FixedLengthSource extends Http1xStream.AbstractSource {
      private long bytesRemaining;

      public FixedLengthSource(long length) throws IOException {
         super(null);
         this.bytesRemaining = length;
         if (this.bytesRemaining == 0L) {
            this.endOfInput();
         }

      }

      public long read(Buffer sink, long byteCount) throws IOException {
         if (byteCount < 0L) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
         } else if (this.closed) {
            throw new IllegalStateException("closed");
         } else if (this.bytesRemaining == 0L) {
            return -1L;
         } else {
            long read = Http1xStream.this.source.read(sink, Math.min(this.bytesRemaining, byteCount));
            if (read == -1L) {
               this.unexpectedEndOfInput();
               throw new ProtocolException("unexpected end of stream");
            } else {
               this.bytesRemaining -= read;
               if (this.bytesRemaining == 0L) {
                  this.endOfInput();
               }

               return read;
            }
         }
      }

      public void close() throws IOException {
         if (!this.closed) {
            if (this.bytesRemaining != 0L && !Util.discard(this, 100, TimeUnit.MILLISECONDS)) {
               this.unexpectedEndOfInput();
            }

            this.closed = true;
         }
      }
   }

   private abstract class AbstractSource implements Source {
      protected final ForwardingTimeout timeout;
      protected boolean closed;

      private AbstractSource() {
         this.timeout = new ForwardingTimeout(Http1xStream.this.source.timeout());
      }

      public Timeout timeout() {
         return this.timeout;
      }

      protected final void endOfInput() throws IOException {
         if (Http1xStream.this.state != 5) {
            throw new IllegalStateException("state: " + Http1xStream.this.state);
         } else {
            Http1xStream.this.detachTimeout(this.timeout);
            Http1xStream.this.state = 6;
            if (Http1xStream.this.streamAllocation != null) {
               Http1xStream.this.streamAllocation.streamFinished(Http1xStream.this);
            }

         }
      }

      protected final void unexpectedEndOfInput() {
         if (Http1xStream.this.state != 6) {
            Http1xStream.this.state = 6;
            if (Http1xStream.this.streamAllocation != null) {
               Http1xStream.this.streamAllocation.noNewStreams();
               Http1xStream.this.streamAllocation.streamFinished(Http1xStream.this);
            }

         }
      }

      // $FF: synthetic method
      AbstractSource(Object x1) {
         this();
      }
   }

   private final class ChunkedSink implements Sink {
      private final ForwardingTimeout timeout;
      private boolean closed;

      private ChunkedSink() {
         this.timeout = new ForwardingTimeout(Http1xStream.this.sink.timeout());
      }

      public Timeout timeout() {
         return this.timeout;
      }

      public void write(Buffer source, long byteCount) throws IOException {
         if (this.closed) {
            throw new IllegalStateException("closed");
         } else if (byteCount != 0L) {
            Http1xStream.this.sink.writeHexadecimalUnsignedLong(byteCount);
            Http1xStream.this.sink.writeUtf8("\r\n");
            Http1xStream.this.sink.write(source, byteCount);
            Http1xStream.this.sink.writeUtf8("\r\n");
         }
      }

      public synchronized void flush() throws IOException {
         if (!this.closed) {
            Http1xStream.this.sink.flush();
         }
      }

      public synchronized void close() throws IOException {
         if (!this.closed) {
            this.closed = true;
            Http1xStream.this.sink.writeUtf8("0\r\n\r\n");
            Http1xStream.this.detachTimeout(this.timeout);
            Http1xStream.this.state = 3;
         }
      }

      // $FF: synthetic method
      ChunkedSink(Object x1) {
         this();
      }
   }

   private final class FixedLengthSink implements Sink {
      private final ForwardingTimeout timeout;
      private boolean closed;
      private long bytesRemaining;

      private FixedLengthSink(long bytesRemaining) {
         this.timeout = new ForwardingTimeout(Http1xStream.this.sink.timeout());
         this.bytesRemaining = bytesRemaining;
      }

      public Timeout timeout() {
         return this.timeout;
      }

      public void write(Buffer source, long byteCount) throws IOException {
         if (this.closed) {
            throw new IllegalStateException("closed");
         } else {
            Util.checkOffsetAndCount(source.size(), 0L, byteCount);
            if (byteCount > this.bytesRemaining) {
               throw new ProtocolException("expected " + this.bytesRemaining + " bytes but received " + byteCount);
            } else {
               Http1xStream.this.sink.write(source, byteCount);
               this.bytesRemaining -= byteCount;
            }
         }
      }

      public void flush() throws IOException {
         if (!this.closed) {
            Http1xStream.this.sink.flush();
         }
      }

      public void close() throws IOException {
         if (!this.closed) {
            this.closed = true;
            if (this.bytesRemaining > 0L) {
               throw new ProtocolException("unexpected end of stream");
            } else {
               Http1xStream.this.detachTimeout(this.timeout);
               Http1xStream.this.state = 3;
            }
         }
      }

      // $FF: synthetic method
      FixedLengthSink(long x1, Object x2) {
         this(x1);
      }
   }
}
