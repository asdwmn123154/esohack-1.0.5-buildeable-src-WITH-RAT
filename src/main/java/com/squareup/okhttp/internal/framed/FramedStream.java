package com.squareup.okhttp.internal.framed;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import okio.AsyncTimeout;
import okio.Buffer;
import okio.BufferedSource;
import okio.Sink;
import okio.Source;
import okio.Timeout;

public final class FramedStream {
   long unacknowledgedBytesRead = 0L;
   long bytesLeftInWriteWindow;
   private final int id;
   private final FramedConnection connection;
   private final List<Header> requestHeaders;
   private List<Header> responseHeaders;
   private final FramedStream.FramedDataSource source;
   final FramedStream.FramedDataSink sink;
   private final FramedStream.StreamTimeout readTimeout = new FramedStream.StreamTimeout();
   private final FramedStream.StreamTimeout writeTimeout = new FramedStream.StreamTimeout();
   private ErrorCode errorCode = null;

   FramedStream(int id, FramedConnection connection, boolean outFinished, boolean inFinished, List<Header> requestHeaders) {
      if (connection == null) {
         throw new NullPointerException("connection == null");
      } else if (requestHeaders == null) {
         throw new NullPointerException("requestHeaders == null");
      } else {
         this.id = id;
         this.connection = connection;
         this.bytesLeftInWriteWindow = (long)connection.peerSettings.getInitialWindowSize(65536);
         this.source = new FramedStream.FramedDataSource((long)connection.okHttpSettings.getInitialWindowSize(65536));
         this.sink = new FramedStream.FramedDataSink();
         this.source.finished = inFinished;
         this.sink.finished = outFinished;
         this.requestHeaders = requestHeaders;
      }
   }

   public int getId() {
      return this.id;
   }

   public synchronized boolean isOpen() {
      if (this.errorCode != null) {
         return false;
      } else {
         return !this.source.finished && !this.source.closed || !this.sink.finished && !this.sink.closed || this.responseHeaders == null;
      }
   }

   public boolean isLocallyInitiated() {
      boolean streamIsClient = (this.id & 1) == 1;
      return this.connection.client == streamIsClient;
   }

   public FramedConnection getConnection() {
      return this.connection;
   }

   public List<Header> getRequestHeaders() {
      return this.requestHeaders;
   }

   public synchronized List<Header> getResponseHeaders() throws IOException {
      this.readTimeout.enter();

      try {
         while(this.responseHeaders == null && this.errorCode == null) {
            this.waitForIo();
         }
      } finally {
         this.readTimeout.exitAndThrowIfTimedOut();
      }

      if (this.responseHeaders != null) {
         return this.responseHeaders;
      } else {
         throw new IOException("stream was reset: " + this.errorCode);
      }
   }

   public synchronized ErrorCode getErrorCode() {
      return this.errorCode;
   }

   public void reply(List<Header> responseHeaders, boolean out) throws IOException {
      assert !Thread.holdsLock(this);

      boolean outFinished = false;
      synchronized(this) {
         if (responseHeaders == null) {
            throw new NullPointerException("responseHeaders == null");
         }

         if (this.responseHeaders != null) {
            throw new IllegalStateException("reply already sent");
         }

         this.responseHeaders = responseHeaders;
         if (!out) {
            this.sink.finished = true;
            outFinished = true;
         }
      }

      this.connection.writeSynReply(this.id, outFinished, responseHeaders);
      if (outFinished) {
         this.connection.flush();
      }

   }

   public Timeout readTimeout() {
      return this.readTimeout;
   }

   public Timeout writeTimeout() {
      return this.writeTimeout;
   }

   public Source getSource() {
      return this.source;
   }

   public Sink getSink() {
      synchronized(this) {
         if (this.responseHeaders == null && !this.isLocallyInitiated()) {
            throw new IllegalStateException("reply before requesting the sink");
         }
      }

      return this.sink;
   }

   public void close(ErrorCode rstStatusCode) throws IOException {
      if (this.closeInternal(rstStatusCode)) {
         this.connection.writeSynReset(this.id, rstStatusCode);
      }
   }

   public void closeLater(ErrorCode errorCode) {
      if (this.closeInternal(errorCode)) {
         this.connection.writeSynResetLater(this.id, errorCode);
      }
   }

   private boolean closeInternal(ErrorCode errorCode) {
      assert !Thread.holdsLock(this);

      synchronized(this) {
         if (this.errorCode != null) {
            return false;
         }

         if (this.source.finished && this.sink.finished) {
            return false;
         }

         this.errorCode = errorCode;
         this.notifyAll();
      }

      this.connection.removeStream(this.id);
      return true;
   }

   void receiveHeaders(List<Header> headers, HeadersMode headersMode) {
      assert !Thread.holdsLock(this);

      ErrorCode errorCode = null;
      boolean open = true;
      synchronized(this) {
         if (this.responseHeaders == null) {
            if (headersMode.failIfHeadersAbsent()) {
               errorCode = ErrorCode.PROTOCOL_ERROR;
            } else {
               this.responseHeaders = headers;
               open = this.isOpen();
               this.notifyAll();
            }
         } else if (headersMode.failIfHeadersPresent()) {
            errorCode = ErrorCode.STREAM_IN_USE;
         } else {
            List<Header> newHeaders = new ArrayList();
            newHeaders.addAll(this.responseHeaders);
            newHeaders.addAll(headers);
            this.responseHeaders = newHeaders;
         }
      }

      if (errorCode != null) {
         this.closeLater(errorCode);
      } else if (!open) {
         this.connection.removeStream(this.id);
      }

   }

   void receiveData(BufferedSource in, int length) throws IOException {
      assert !Thread.holdsLock(this);

      this.source.receive(in, (long)length);
   }

   void receiveFin() {
      assert !Thread.holdsLock(this);

      boolean open;
      synchronized(this) {
         this.source.finished = true;
         open = this.isOpen();
         this.notifyAll();
      }

      if (!open) {
         this.connection.removeStream(this.id);
      }

   }

   synchronized void receiveRstStream(ErrorCode errorCode) {
      if (this.errorCode == null) {
         this.errorCode = errorCode;
         this.notifyAll();
      }

   }

   private void cancelStreamIfNecessary() throws IOException {
      assert !Thread.holdsLock(this);

      boolean open;
      boolean cancel;
      synchronized(this) {
         cancel = !this.source.finished && this.source.closed && (this.sink.finished || this.sink.closed);
         open = this.isOpen();
      }

      if (cancel) {
         this.close(ErrorCode.CANCEL);
      } else if (!open) {
         this.connection.removeStream(this.id);
      }

   }

   void addBytesToWriteWindow(long delta) {
      this.bytesLeftInWriteWindow += delta;
      if (delta > 0L) {
         this.notifyAll();
      }

   }

   private void checkOutNotClosed() throws IOException {
      if (this.sink.closed) {
         throw new IOException("stream closed");
      } else if (this.sink.finished) {
         throw new IOException("stream finished");
      } else if (this.errorCode != null) {
         throw new IOException("stream was reset: " + this.errorCode);
      }
   }

   private void waitForIo() throws InterruptedIOException {
      try {
         this.wait();
      } catch (InterruptedException var2) {
         throw new InterruptedIOException();
      }
   }

   class StreamTimeout extends AsyncTimeout {
      protected void timedOut() {
         FramedStream.this.closeLater(ErrorCode.CANCEL);
      }

      protected IOException newTimeoutException(IOException cause) {
         SocketTimeoutException socketTimeoutException = new SocketTimeoutException("timeout");
         if (cause != null) {
            socketTimeoutException.initCause(cause);
         }

         return socketTimeoutException;
      }

      public void exitAndThrowIfTimedOut() throws IOException {
         if (this.exit()) {
            throw this.newTimeoutException((IOException)null);
         }
      }
   }

   final class FramedDataSink implements Sink {
      private static final long EMIT_BUFFER_SIZE = 16384L;
      private final Buffer sendBuffer = new Buffer();
      private boolean closed;
      private boolean finished;

      public void write(Buffer source, long byteCount) throws IOException {
         assert !Thread.holdsLock(FramedStream.this);

         this.sendBuffer.write(source, byteCount);

         while(this.sendBuffer.size() >= 16384L) {
            this.emitDataFrame(false);
         }

      }

      private void emitDataFrame(boolean outFinished) throws IOException {
         long toWrite;
         synchronized(FramedStream.this) {
            FramedStream.this.writeTimeout.enter();

            try {
               while(FramedStream.this.bytesLeftInWriteWindow <= 0L && !this.finished && !this.closed && FramedStream.this.errorCode == null) {
                  FramedStream.this.waitForIo();
               }
            } finally {
               FramedStream.this.writeTimeout.exitAndThrowIfTimedOut();
            }

            FramedStream.this.checkOutNotClosed();
            toWrite = Math.min(FramedStream.this.bytesLeftInWriteWindow, this.sendBuffer.size());
            FramedStream var10000 = FramedStream.this;
            var10000.bytesLeftInWriteWindow -= toWrite;
         }

         FramedStream.this.writeTimeout.enter();

         try {
            FramedStream.this.connection.writeData(FramedStream.this.id, outFinished && toWrite == this.sendBuffer.size(), this.sendBuffer, toWrite);
         } finally {
            FramedStream.this.writeTimeout.exitAndThrowIfTimedOut();
         }

      }

      public void flush() throws IOException {
         assert !Thread.holdsLock(FramedStream.this);

         synchronized(FramedStream.this) {
            FramedStream.this.checkOutNotClosed();
         }

         while(this.sendBuffer.size() > 0L) {
            this.emitDataFrame(false);
            FramedStream.this.connection.flush();
         }

      }

      public Timeout timeout() {
         return FramedStream.this.writeTimeout;
      }

      public void close() throws IOException {
         assert !Thread.holdsLock(FramedStream.this);

         synchronized(FramedStream.this) {
            if (this.closed) {
               return;
            }
         }

         if (!FramedStream.this.sink.finished) {
            if (this.sendBuffer.size() > 0L) {
               while(this.sendBuffer.size() > 0L) {
                  this.emitDataFrame(true);
               }
            } else {
               FramedStream.this.connection.writeData(FramedStream.this.id, true, (Buffer)null, 0L);
            }
         }

         synchronized(FramedStream.this) {
            this.closed = true;
         }

         FramedStream.this.connection.flush();
         FramedStream.this.cancelStreamIfNecessary();
      }
   }

   private final class FramedDataSource implements Source {
      private final Buffer receiveBuffer;
      private final Buffer readBuffer;
      private final long maxByteCount;
      private boolean closed;
      private boolean finished;

      private FramedDataSource(long maxByteCount) {
         this.receiveBuffer = new Buffer();
         this.readBuffer = new Buffer();
         this.maxByteCount = maxByteCount;
      }

      public long read(Buffer sink, long byteCount) throws IOException {
         if (byteCount < 0L) {
            throw new IllegalArgumentException("byteCount < 0: " + byteCount);
         } else {
            long read;
            synchronized(FramedStream.this) {
               this.waitUntilReadable();
               this.checkNotClosed();
               if (this.readBuffer.size() == 0L) {
                  return -1L;
               }

               read = this.readBuffer.read(sink, Math.min(byteCount, this.readBuffer.size()));
               FramedStream var10000 = FramedStream.this;
               var10000.unacknowledgedBytesRead += read;
               if (FramedStream.this.unacknowledgedBytesRead >= (long)(FramedStream.this.connection.okHttpSettings.getInitialWindowSize(65536) / 2)) {
                  FramedStream.this.connection.writeWindowUpdateLater(FramedStream.this.id, FramedStream.this.unacknowledgedBytesRead);
                  FramedStream.this.unacknowledgedBytesRead = 0L;
               }
            }

            synchronized(FramedStream.this.connection) {
               FramedConnection var11 = FramedStream.this.connection;
               var11.unacknowledgedBytesRead += read;
               if (FramedStream.this.connection.unacknowledgedBytesRead >= (long)(FramedStream.this.connection.okHttpSettings.getInitialWindowSize(65536) / 2)) {
                  FramedStream.this.connection.writeWindowUpdateLater(0, FramedStream.this.connection.unacknowledgedBytesRead);
                  FramedStream.this.connection.unacknowledgedBytesRead = 0L;
               }

               return read;
            }
         }
      }

      private void waitUntilReadable() throws IOException {
         FramedStream.this.readTimeout.enter();

         try {
            while(this.readBuffer.size() == 0L && !this.finished && !this.closed && FramedStream.this.errorCode == null) {
               FramedStream.this.waitForIo();
            }
         } finally {
            FramedStream.this.readTimeout.exitAndThrowIfTimedOut();
         }

      }

      void receive(BufferedSource in, long byteCount) throws IOException {
         assert !Thread.holdsLock(FramedStream.this);

         while(byteCount > 0L) {
            boolean finished;
            boolean flowControlError;
            synchronized(FramedStream.this) {
               finished = this.finished;
               flowControlError = byteCount + this.readBuffer.size() > this.maxByteCount;
            }

            if (flowControlError) {
               in.skip(byteCount);
               FramedStream.this.closeLater(ErrorCode.FLOW_CONTROL_ERROR);
               return;
            }

            if (finished) {
               in.skip(byteCount);
               return;
            }

            long read = in.read(this.receiveBuffer, byteCount);
            if (read == -1L) {
               throw new EOFException();
            }

            byteCount -= read;
            synchronized(FramedStream.this) {
               boolean wasEmpty = this.readBuffer.size() == 0L;
               this.readBuffer.writeAll(this.receiveBuffer);
               if (wasEmpty) {
                  FramedStream.this.notifyAll();
               }
            }
         }

      }

      public Timeout timeout() {
         return FramedStream.this.readTimeout;
      }

      public void close() throws IOException {
         synchronized(FramedStream.this) {
            this.closed = true;
            this.readBuffer.clear();
            FramedStream.this.notifyAll();
         }

         FramedStream.this.cancelStreamIfNecessary();
      }

      private void checkNotClosed() throws IOException {
         if (this.closed) {
            throw new IOException("stream closed");
         } else if (FramedStream.this.errorCode != null) {
            throw new IOException("stream was reset: " + FramedStream.this.errorCode);
         }
      }

      // $FF: synthetic method
      FramedDataSource(long x1, Object x2) {
         this(x1);
      }
   }
}
