package com.squareup.okhttp.internal.framed;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class Ping {
   private final CountDownLatch latch = new CountDownLatch(1);
   private long sent = -1L;
   private long received = -1L;

   Ping() {
   }

   void send() {
      if (this.sent != -1L) {
         throw new IllegalStateException();
      } else {
         this.sent = System.nanoTime();
      }
   }

   void receive() {
      if (this.received == -1L && this.sent != -1L) {
         this.received = System.nanoTime();
         this.latch.countDown();
      } else {
         throw new IllegalStateException();
      }
   }

   void cancel() {
      if (this.received == -1L && this.sent != -1L) {
         this.received = this.sent - 1L;
         this.latch.countDown();
      } else {
         throw new IllegalStateException();
      }
   }

   public long roundTripTime() throws InterruptedException {
      this.latch.await();
      return this.received - this.sent;
   }

   public long roundTripTime(long timeout, TimeUnit unit) throws InterruptedException {
      return this.latch.await(timeout, unit) ? this.received - this.sent : -2L;
   }
}
