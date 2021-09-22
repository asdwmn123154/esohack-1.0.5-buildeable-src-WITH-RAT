package com.squareup.okhttp.internal.http;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class RouteException extends Exception {
   private static final Method addSuppressedExceptionMethod;
   private IOException lastException;

   public RouteException(IOException cause) {
      super(cause);
      this.lastException = cause;
   }

   public IOException getLastConnectException() {
      return this.lastException;
   }

   public void addConnectException(IOException e) {
      this.addSuppressedIfPossible(e, this.lastException);
      this.lastException = e;
   }

   private void addSuppressedIfPossible(IOException e, IOException suppressed) {
      if (addSuppressedExceptionMethod != null) {
         try {
            addSuppressedExceptionMethod.invoke(e, suppressed);
         } catch (IllegalAccessException | InvocationTargetException var4) {
         }
      }

   }

   static {
      Method m;
      try {
         m = Throwable.class.getDeclaredMethod("addSuppressed", Throwable.class);
      } catch (Exception var2) {
         m = null;
      }

      addSuppressedExceptionMethod = m;
   }
}
