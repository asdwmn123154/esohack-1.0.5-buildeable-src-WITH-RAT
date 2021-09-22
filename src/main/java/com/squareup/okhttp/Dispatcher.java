package com.squareup.okhttp;

import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.http.HttpEngine;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Dispatcher {
   private int maxRequests = 64;
   private int maxRequestsPerHost = 5;
   private ExecutorService executorService;
   private final Deque<Call.AsyncCall> readyCalls = new ArrayDeque();
   private final Deque<Call.AsyncCall> runningCalls = new ArrayDeque();
   private final Deque<Call> executedCalls = new ArrayDeque();

   public Dispatcher(ExecutorService executorService) {
      this.executorService = executorService;
   }

   public Dispatcher() {
   }

   public synchronized ExecutorService getExecutorService() {
      if (this.executorService == null) {
         this.executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue(), Util.threadFactory("OkHttp Dispatcher", false));
      }

      return this.executorService;
   }

   public synchronized void setMaxRequests(int maxRequests) {
      if (maxRequests < 1) {
         throw new IllegalArgumentException("max < 1: " + maxRequests);
      } else {
         this.maxRequests = maxRequests;
         this.promoteCalls();
      }
   }

   public synchronized int getMaxRequests() {
      return this.maxRequests;
   }

   public synchronized void setMaxRequestsPerHost(int maxRequestsPerHost) {
      if (maxRequestsPerHost < 1) {
         throw new IllegalArgumentException("max < 1: " + maxRequestsPerHost);
      } else {
         this.maxRequestsPerHost = maxRequestsPerHost;
         this.promoteCalls();
      }
   }

   public synchronized int getMaxRequestsPerHost() {
      return this.maxRequestsPerHost;
   }

   synchronized void enqueue(Call.AsyncCall call) {
      if (this.runningCalls.size() < this.maxRequests && this.runningCallsForHost(call) < this.maxRequestsPerHost) {
         this.runningCalls.add(call);
         this.getExecutorService().execute(call);
      } else {
         this.readyCalls.add(call);
      }

   }

   public synchronized void cancel(Object tag) {
      Iterator var2 = this.readyCalls.iterator();

      Call.AsyncCall call;
      while(var2.hasNext()) {
         call = (Call.AsyncCall)var2.next();
         if (Util.equal(tag, call.tag())) {
            call.cancel();
         }
      }

      var2 = this.runningCalls.iterator();

      while(var2.hasNext()) {
         call = (Call.AsyncCall)var2.next();
         if (Util.equal(tag, call.tag())) {
            call.get().canceled = true;
            HttpEngine engine = call.get().engine;
            if (engine != null) {
               engine.cancel();
            }
         }
      }

      var2 = this.executedCalls.iterator();

      while(var2.hasNext()) {
         Call call = (Call)var2.next();
         if (Util.equal(tag, call.tag())) {
            call.cancel();
         }
      }

   }

   synchronized void finished(Call.AsyncCall call) {
      if (!this.runningCalls.remove(call)) {
         throw new AssertionError("AsyncCall wasn't running!");
      } else {
         this.promoteCalls();
      }
   }

   private void promoteCalls() {
      if (this.runningCalls.size() < this.maxRequests) {
         if (!this.readyCalls.isEmpty()) {
            Iterator i = this.readyCalls.iterator();

            do {
               if (!i.hasNext()) {
                  return;
               }

               Call.AsyncCall call = (Call.AsyncCall)i.next();
               if (this.runningCallsForHost(call) < this.maxRequestsPerHost) {
                  i.remove();
                  this.runningCalls.add(call);
                  this.getExecutorService().execute(call);
               }
            } while(this.runningCalls.size() < this.maxRequests);

         }
      }
   }

   private int runningCallsForHost(Call.AsyncCall call) {
      int result = 0;
      Iterator var3 = this.runningCalls.iterator();

      while(var3.hasNext()) {
         Call.AsyncCall c = (Call.AsyncCall)var3.next();
         if (c.host().equals(call.host())) {
            ++result;
         }
      }

      return result;
   }

   synchronized void executed(Call call) {
      this.executedCalls.add(call);
   }

   synchronized void finished(Call call) {
      if (!this.executedCalls.remove(call)) {
         throw new AssertionError("Call wasn't in-flight!");
      }
   }

   public synchronized int getRunningCallCount() {
      return this.runningCalls.size();
   }

   public synchronized int getQueuedCallCount() {
      return this.readyCalls.size();
   }
}
