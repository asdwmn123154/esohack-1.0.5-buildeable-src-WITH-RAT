package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.Address;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.Route;
import com.squareup.okhttp.internal.Internal;
import com.squareup.okhttp.internal.RouteDatabase;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.io.RealConnection;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import okio.Sink;

public final class StreamAllocation {
   public final Address address;
   private final ConnectionPool connectionPool;
   private RouteSelector routeSelector;
   private RealConnection connection;
   private boolean released;
   private boolean canceled;
   private HttpStream stream;

   public StreamAllocation(ConnectionPool connectionPool, Address address) {
      this.connectionPool = connectionPool;
      this.address = address;
   }

   public HttpStream newStream(int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled, boolean doExtensiveHealthChecks) throws RouteException, IOException {
      try {
         RealConnection resultConnection = this.findHealthyConnection(connectTimeout, readTimeout, writeTimeout, connectionRetryEnabled, doExtensiveHealthChecks);
         Object resultStream;
         if (resultConnection.framedConnection != null) {
            resultStream = new Http2xStream(this, resultConnection.framedConnection);
         } else {
            resultConnection.getSocket().setSoTimeout(readTimeout);
            resultConnection.source.timeout().timeout((long)readTimeout, TimeUnit.MILLISECONDS);
            resultConnection.sink.timeout().timeout((long)writeTimeout, TimeUnit.MILLISECONDS);
            resultStream = new Http1xStream(this, resultConnection.source, resultConnection.sink);
         }

         synchronized(this.connectionPool) {
            ++resultConnection.streamCount;
            this.stream = (HttpStream)resultStream;
            return (HttpStream)resultStream;
         }
      } catch (IOException var11) {
         throw new RouteException(var11);
      }
   }

   private RealConnection findHealthyConnection(int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled, boolean doExtensiveHealthChecks) throws IOException, RouteException {
      while(true) {
         RealConnection candidate = this.findConnection(connectTimeout, readTimeout, writeTimeout, connectionRetryEnabled);
         synchronized(this.connectionPool) {
            if (candidate.streamCount == 0) {
               return candidate;
            }
         }

         if (candidate.isHealthy(doExtensiveHealthChecks)) {
            return candidate;
         }

         this.connectionFailed();
      }
   }

   private RealConnection findConnection(int connectTimeout, int readTimeout, int writeTimeout, boolean connectionRetryEnabled) throws IOException, RouteException {
      RealConnection newConnection;
      synchronized(this.connectionPool) {
         if (this.released) {
            throw new IllegalStateException("released");
         }

         if (this.stream != null) {
            throw new IllegalStateException("stream != null");
         }

         if (this.canceled) {
            throw new IOException("Canceled");
         }

         newConnection = this.connection;
         if (newConnection != null && !newConnection.noNewStreams) {
            return newConnection;
         }

         RealConnection pooledConnection = Internal.instance.get(this.connectionPool, this.address, this);
         if (pooledConnection != null) {
            this.connection = pooledConnection;
            return pooledConnection;
         }

         if (this.routeSelector == null) {
            this.routeSelector = new RouteSelector(this.address, this.routeDatabase());
         }
      }

      Route route = this.routeSelector.next();
      newConnection = new RealConnection(route);
      this.acquire(newConnection);
      synchronized(this.connectionPool) {
         Internal.instance.put(this.connectionPool, newConnection);
         this.connection = newConnection;
         if (this.canceled) {
            throw new IOException("Canceled");
         }
      }

      newConnection.connect(connectTimeout, readTimeout, writeTimeout, this.address.getConnectionSpecs(), connectionRetryEnabled);
      this.routeDatabase().connected(newConnection.getRoute());
      return newConnection;
   }

   public void streamFinished(HttpStream stream) {
      synchronized(this.connectionPool) {
         if (stream == null || stream != this.stream) {
            throw new IllegalStateException("expected " + this.stream + " but was " + stream);
         }
      }

      this.deallocate(false, false, true);
   }

   public HttpStream stream() {
      synchronized(this.connectionPool) {
         return this.stream;
      }
   }

   private RouteDatabase routeDatabase() {
      return Internal.instance.routeDatabase(this.connectionPool);
   }

   public synchronized RealConnection connection() {
      return this.connection;
   }

   public void release() {
      this.deallocate(false, true, false);
   }

   public void noNewStreams() {
      this.deallocate(true, false, false);
   }

   private void deallocate(boolean noNewStreams, boolean released, boolean streamFinished) {
      RealConnection connectionToClose = null;
      synchronized(this.connectionPool) {
         if (streamFinished) {
            this.stream = null;
         }

         if (released) {
            this.released = true;
         }

         if (this.connection != null) {
            if (noNewStreams) {
               this.connection.noNewStreams = true;
            }

            if (this.stream == null && (this.released || this.connection.noNewStreams)) {
               this.release(this.connection);
               if (this.connection.streamCount > 0) {
                  this.routeSelector = null;
               }

               if (this.connection.allocations.isEmpty()) {
                  this.connection.idleAtNanos = System.nanoTime();
                  if (Internal.instance.connectionBecameIdle(this.connectionPool, this.connection)) {
                     connectionToClose = this.connection;
                  }
               }

               this.connection = null;
            }
         }
      }

      if (connectionToClose != null) {
         Util.closeQuietly(connectionToClose.getSocket());
      }

   }

   public void cancel() {
      HttpStream streamToCancel;
      RealConnection connectionToCancel;
      synchronized(this.connectionPool) {
         this.canceled = true;
         streamToCancel = this.stream;
         connectionToCancel = this.connection;
      }

      if (streamToCancel != null) {
         streamToCancel.cancel();
      } else if (connectionToCancel != null) {
         connectionToCancel.cancel();
      }

   }

   private void connectionFailed(IOException e) {
      synchronized(this.connectionPool) {
         if (this.routeSelector != null) {
            if (this.connection.streamCount == 0) {
               Route failedRoute = this.connection.getRoute();
               this.routeSelector.connectFailed(failedRoute, e);
            } else {
               this.routeSelector = null;
            }
         }
      }

      this.connectionFailed();
   }

   public void connectionFailed() {
      this.deallocate(true, false, true);
   }

   public void acquire(RealConnection connection) {
      connection.allocations.add(new WeakReference(this));
   }

   private void release(RealConnection connection) {
      int i = 0;

      for(int size = connection.allocations.size(); i < size; ++i) {
         Reference<StreamAllocation> reference = (Reference)connection.allocations.get(i);
         if (reference.get() == this) {
            connection.allocations.remove(i);
            return;
         }
      }

      throw new IllegalStateException();
   }

   public boolean recover(RouteException e) {
      if (this.connection != null) {
         this.connectionFailed(e.getLastConnectException());
      }

      return (this.routeSelector == null || this.routeSelector.hasNext()) && this.isRecoverable(e);
   }

   public boolean recover(IOException e, Sink requestBodyOut) {
      if (this.connection != null) {
         int streamCount = this.connection.streamCount;
         this.connectionFailed(e);
         if (streamCount == 1) {
            return false;
         }
      }

      boolean canRetryRequestBody = requestBodyOut == null || requestBodyOut instanceof RetryableSink;
      return (this.routeSelector == null || this.routeSelector.hasNext()) && this.isRecoverable(e) && canRetryRequestBody;
   }

   private boolean isRecoverable(IOException e) {
      if (e instanceof ProtocolException) {
         return false;
      } else {
         return !(e instanceof InterruptedIOException);
      }
   }

   private boolean isRecoverable(RouteException e) {
      IOException ioe = e.getLastConnectException();
      if (ioe instanceof ProtocolException) {
         return false;
      } else if (ioe instanceof InterruptedIOException) {
         return ioe instanceof SocketTimeoutException;
      } else if (ioe instanceof SSLHandshakeException && ioe.getCause() instanceof CertificateException) {
         return false;
      } else {
         return !(ioe instanceof SSLPeerUnverifiedException);
      }
   }

   public String toString() {
      return this.address.toString();
   }
}
