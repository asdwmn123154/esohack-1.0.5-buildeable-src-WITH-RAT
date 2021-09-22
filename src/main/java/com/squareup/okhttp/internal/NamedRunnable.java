package com.squareup.okhttp.internal;

public abstract class NamedRunnable implements Runnable {
   protected final String name;

   public NamedRunnable(String format, Object... args) {
      this.name = String.format(format, args);
   }

   public final void run() {
      String oldName = Thread.currentThread().getName();
      Thread.currentThread().setName(this.name);

      try {
         this.execute();
      } finally {
         Thread.currentThread().setName(oldName);
      }

   }

   protected abstract void execute();
}
