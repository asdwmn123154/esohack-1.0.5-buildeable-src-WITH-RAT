package com.squareup.okhttp;

import com.squareup.okhttp.internal.Internal;
import com.squareup.okhttp.internal.RouteDatabase;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.http.StreamAllocation;
import com.squareup.okhttp.internal.io.RealConnection;
import java.lang.ref.Reference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class ConnectionPool {
   private static final long DEFAULT_KEEP_ALIVE_DURATION_MS = 300000L;
   private static final ConnectionPool systemDefault;
   private final Executor executor;
   private final int maxIdleConnections;
   private final long keepAliveDurationNs;
   private Runnable cleanupRunnable;
   private final Deque<RealConnection> connections;
   final RouteDatabase routeDatabase;

   public ConnectionPool(int maxIdleConnections, long keepAliveDurationMs) {
      this(maxIdleConnections, keepAliveDurationMs, TimeUnit.MILLISECONDS);
   }

   public ConnectionPool(int maxIdleConnections, long keepAliveDuration, TimeUnit timeUnit) {
      this.executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory("OkHttp ConnectionPool", true));
      this.cleanupRunnable = new Runnable() {
         public void run() {
            while(true) {
               long waitNanos = ConnectionPool.this.cleanup(System.nanoTime());
               if (waitNanos == -1L) {
                  return;
               }

               if (waitNanos > 0L) {
                  long waitMillis = waitNanos / 1000000L;
                  waitNanos -= waitMillis * 1000000L;
                  synchronized(ConnectionPool.this) {
                     try {
                        ConnectionPool.this.wait(waitMillis, (int)waitNanos);
                     } catch (InterruptedException var8) {
                     }
                  }
               }
            }
         }
      };
      this.connections = new ArrayDeque();
      this.routeDatabase = new RouteDatabase();
      this.maxIdleConnections = maxIdleConnections;
      this.keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration);
      if (keepAliveDuration <= 0L) {
         throw new IllegalArgumentException("keepAliveDuration <= 0: " + keepAliveDuration);
      }
   }

   public static ConnectionPool getDefault() {
      return systemDefault;
   }

   public synchronized int getIdleConnectionCount() {
      int total = 0;
      Iterator var2 = this.connections.iterator();

      while(var2.hasNext()) {
         RealConnection connection = (RealConnection)var2.next();
         if (connection.allocations.isEmpty()) {
            ++total;
         }
      }

      return total;
   }

   public synchronized int getConnectionCount() {
      return this.connections.size();
   }

   /** @deprecated */
   @Deprecated
   public synchronized int getSpdyConnectionCount() {
      return this.getMultiplexedConnectionCount();
   }

   public synchronized int getMultiplexedConnectionCount() {
      int total = 0;
      Iterator var2 = this.connections.iterator();

      while(var2.hasNext()) {
         RealConnection connection = (RealConnection)var2.next();
         if (connection.isMultiplexed()) {
            ++total;
         }
      }

      return total;
   }

   public synchronized int getHttpConnectionCount() {
      return this.connections.size() - this.getMultiplexedConnectionCount();
   }

   RealConnection get(Address address, StreamAllocation streamAllocation) {
      assert Thread.holdsLock(this);

      Iterator var3 = this.connections.iterator();

      RealConnection connection;
      do {
         if (!var3.hasNext()) {
            return null;
         }

         connection = (RealConnection)var3.next();
      } while(connection.allocations.size() >= connection.allocationLimit() || !address.equals(connection.getRoute().address) || connection.noNewStreams);

      streamAllocation.acquire(connection);
      return connection;
   }

   void put(RealConnection connection) {
      assert Thread.holdsLock(this);

      if (this.connections.isEmpty()) {
         this.executor.execute(this.cleanupRunnable);
      }

      this.connections.add(connection);
   }

   boolean connectionBecameIdle(RealConnection connection) {
      assert Thread.holdsLock(this);

      if (!connection.noNewStreams && this.maxIdleConnections != 0) {
         this.notifyAll();
         return false;
      } else {
         this.connections.remove(connection);
         return true;
      }
   }

   public void evictAll() {
      List<RealConnection> evictedConnections = new ArrayList();
      synchronized(this) {
         Iterator i = this.connections.iterator();

         while(true) {
            if (!i.hasNext()) {
               break;
            }

            RealConnection connection = (RealConnection)i.next();
            if (connection.allocations.isEmpty()) {
               connection.noNewStreams = true;
               evictedConnections.add(connection);
               i.remove();
            }
         }
      }

      Iterator var2 = evictedConnections.iterator();

      while(var2.hasNext()) {
         RealConnection connection = (RealConnection)var2.next();
         Util.closeQuietly(connection.getSocket());
      }

   }

   long cleanup(long now) {
      int inUseConnectionCount = 0;
      int idleConnectionCount = 0;
      RealConnection longestIdleConnection = null;
      long longestIdleDurationNs = Long.MIN_VALUE;
      synchronized(this) {
         Iterator i = this.connections.iterator();

         while(i.hasNext()) {
            RealConnection connection = (RealConnection)i.next();
            if (this.pruneAndGetAllocationCount(connection, now) > 0) {
               ++inUseConnectionCount;
            } else {
               ++idleConnectionCount;
               long idleDurationNs = now - connection.idleAtNanos;
               if (idleDurationNs > longestIdleDurationNs) {
                  longestIdleDurationNs = idleDurationNs;
                  longestIdleConnection = connection;
               }
            }
         }

         if (longestIdleDurationNs < this.keepAliveDurationNs && idleConnectionCount <= this.maxIdleConnections) {
            if (idleConnectionCount > 0) {
               return this.keepAliveDurationNs - longestIdleDurationNs;
            }

            if (inUseConnectionCount > 0) {
               return this.keepAliveDurationNs;
            }

            return -1L;
         }

         this.connections.remove(longestIdleConnection);
      }

      Util.closeQuietly(longestIdleConnection.getSocket());
      return 0L;
   }

   private int pruneAndGetAllocationCount(RealConnection connection, long now) {
      List<Reference<StreamAllocation>> references = connection.allocations;
      int i = 0;

      while(i < references.size()) {
         Reference<StreamAllocation> reference = (Reference)references.get(i);
         if (reference.get() != null) {
            ++i;
         } else {
            Internal.logger.warning("A connection to " + connection.getRoute().getAddress().url() + " was leaked. Did you forget to close a response body?");
            references.remove(i);
            connection.noNewStreams = true;
            if (references.isEmpty()) {
               connection.idleAtNanos = now - this.keepAliveDurationNs;
               return 0;
            }
         }
      }

      return references.size();
   }

   void setCleanupRunnableForTest(Runnable cleanupRunnable) {
      this.cleanupRunnable = cleanupRunnable;
   }

   static {
      String keepAlive = System.getProperty("http.keepAlive");
      String keepAliveDuration = System.getProperty("http.keepAliveDuration");
      String maxIdleConnections = System.getProperty("http.maxConnections");
      long keepAliveDurationMs = keepAliveDuration != null ? Long.parseLong(keepAliveDuration) : 300000L;
      if (keepAlive != null && !Boolean.parseBoolean(keepAlive)) {
         systemDefault = new ConnectionPool(0, keepAliveDurationMs);
      } else if (maxIdleConnections != null) {
         systemDefault = new ConnectionPool(Integer.parseInt(maxIdleConnections), keepAliveDurationMs);
      } else {
         systemDefault = new ConnectionPool(5, keepAliveDurationMs);
      }

   }
}
