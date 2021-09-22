package com.squareup.okhttp.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class OptionalMethod<T> {
   private final Class<?> returnType;
   private final String methodName;
   private final Class[] methodParams;

   public OptionalMethod(Class<?> returnType, String methodName, Class... methodParams) {
      this.returnType = returnType;
      this.methodName = methodName;
      this.methodParams = methodParams;
   }

   public boolean isSupported(T target) {
      return this.getMethod(target.getClass()) != null;
   }

   public Object invokeOptional(T target, Object... args) throws InvocationTargetException {
      Method m = this.getMethod(target.getClass());
      if (m == null) {
         return null;
      } else {
         try {
            return m.invoke(target, args);
         } catch (IllegalAccessException var5) {
            return null;
         }
      }
   }

   public Object invokeOptionalWithoutCheckedException(T target, Object... args) {
      try {
         return this.invokeOptional(target, args);
      } catch (InvocationTargetException var6) {
         Throwable targetException = var6.getTargetException();
         if (targetException instanceof RuntimeException) {
            throw (RuntimeException)targetException;
         } else {
            AssertionError error = new AssertionError("Unexpected exception");
            error.initCause(targetException);
            throw error;
         }
      }
   }

   public Object invoke(T target, Object... args) throws InvocationTargetException {
      Method m = this.getMethod(target.getClass());
      if (m == null) {
         throw new AssertionError("Method " + this.methodName + " not supported for object " + target);
      } else {
         try {
            return m.invoke(target, args);
         } catch (IllegalAccessException var6) {
            AssertionError error = new AssertionError("Unexpectedly could not call: " + m);
            error.initCause(var6);
            throw error;
         }
      }
   }

   public Object invokeWithoutCheckedException(T target, Object... args) {
      try {
         return this.invoke(target, args);
      } catch (InvocationTargetException var6) {
         Throwable targetException = var6.getTargetException();
         if (targetException instanceof RuntimeException) {
            throw (RuntimeException)targetException;
         } else {
            AssertionError error = new AssertionError("Unexpected exception");
            error.initCause(targetException);
            throw error;
         }
      }
   }

   private Method getMethod(Class<?> clazz) {
      Method method = null;
      if (this.methodName != null) {
         method = getPublicMethod(clazz, this.methodName, this.methodParams);
         if (method != null && this.returnType != null && !this.returnType.isAssignableFrom(method.getReturnType())) {
            method = null;
         }
      }

      return method;
   }

   private static Method getPublicMethod(Class<?> clazz, String methodName, Class[] parameterTypes) {
      Method method = null;

      try {
         method = clazz.getMethod(methodName, parameterTypes);
         if ((method.getModifiers() & 1) == 0) {
            method = null;
         }
      } catch (NoSuchMethodException var5) {
      }

      return method;
   }
}
