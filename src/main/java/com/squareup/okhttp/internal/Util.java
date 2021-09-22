package com.squareup.okhttp.internal;

import com.squareup.okhttp.HttpUrl;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import okio.Buffer;
import okio.ByteString;
import okio.Source;

public final class Util {
   public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
   public static final String[] EMPTY_STRING_ARRAY = new String[0];
   public static final Charset UTF_8 = Charset.forName("UTF-8");

   private Util() {
   }

   public static void checkOffsetAndCount(long arrayLength, long offset, long count) {
      if ((offset | count) < 0L || offset > arrayLength || arrayLength - offset < count) {
         throw new ArrayIndexOutOfBoundsException();
      }
   }

   public static boolean equal(Object a, Object b) {
      return a == b || a != null && a.equals(b);
   }

   public static void closeQuietly(Closeable closeable) {
      if (closeable != null) {
         try {
            closeable.close();
         } catch (RuntimeException var2) {
            throw var2;
         } catch (Exception var3) {
         }
      }

   }

   public static void closeQuietly(Socket socket) {
      if (socket != null) {
         try {
            socket.close();
         } catch (AssertionError var2) {
            if (!isAndroidGetsocknameError(var2)) {
               throw var2;
            }
         } catch (RuntimeException var3) {
            throw var3;
         } catch (Exception var4) {
         }
      }

   }

   public static void closeQuietly(ServerSocket serverSocket) {
      if (serverSocket != null) {
         try {
            serverSocket.close();
         } catch (RuntimeException var2) {
            throw var2;
         } catch (Exception var3) {
         }
      }

   }

   public static void closeAll(Closeable a, Closeable b) throws IOException {
      Throwable thrown = null;

      try {
         a.close();
      } catch (Throwable var4) {
         thrown = var4;
      }

      try {
         b.close();
      } catch (Throwable var5) {
         if (thrown == null) {
            thrown = var5;
         }
      }

      if (thrown != null) {
         if (thrown instanceof IOException) {
            throw (IOException)thrown;
         } else if (thrown instanceof RuntimeException) {
            throw (RuntimeException)thrown;
         } else if (thrown instanceof Error) {
            throw (Error)thrown;
         } else {
            throw new AssertionError(thrown);
         }
      }
   }

   public static boolean discard(Source source, int timeout, TimeUnit timeUnit) {
      try {
         return skipAll(source, timeout, timeUnit);
      } catch (IOException var4) {
         return false;
      }
   }

   public static boolean skipAll(Source source, int duration, TimeUnit timeUnit) throws IOException {
      long now = System.nanoTime();
      long originalDuration = source.timeout().hasDeadline() ? source.timeout().deadlineNanoTime() - now : Long.MAX_VALUE;
      source.timeout().deadlineNanoTime(now + Math.min(originalDuration, timeUnit.toNanos((long)duration)));

      boolean var8;
      try {
         Buffer skipBuffer = new Buffer();

         while(source.read(skipBuffer, 2048L) != -1L) {
            skipBuffer.clear();
         }

         var8 = true;
         return var8;
      } catch (InterruptedIOException var12) {
         var8 = false;
      } finally {
         if (originalDuration == Long.MAX_VALUE) {
            source.timeout().clearDeadline();
         } else {
            source.timeout().deadlineNanoTime(now + originalDuration);
         }

      }

      return var8;
   }

   public static String md5Hex(String s) {
      try {
         MessageDigest messageDigest = MessageDigest.getInstance("MD5");
         byte[] md5bytes = messageDigest.digest(s.getBytes("UTF-8"));
         return ByteString.of(md5bytes).hex();
      } catch (UnsupportedEncodingException | NoSuchAlgorithmException var3) {
         throw new AssertionError(var3);
      }
   }

   public static String shaBase64(String s) {
      try {
         MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
         byte[] sha1Bytes = messageDigest.digest(s.getBytes("UTF-8"));
         return ByteString.of(sha1Bytes).base64();
      } catch (UnsupportedEncodingException | NoSuchAlgorithmException var3) {
         throw new AssertionError(var3);
      }
   }

   public static ByteString sha1(ByteString s) {
      try {
         MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
         byte[] sha1Bytes = messageDigest.digest(s.toByteArray());
         return ByteString.of(sha1Bytes);
      } catch (NoSuchAlgorithmException var3) {
         throw new AssertionError(var3);
      }
   }

   public static <T> List<T> immutableList(List<T> list) {
      return Collections.unmodifiableList(new ArrayList(list));
   }

   public static <T> List<T> immutableList(T... elements) {
      return Collections.unmodifiableList(Arrays.asList((Object[])elements.clone()));
   }

   public static <K, V> Map<K, V> immutableMap(Map<K, V> map) {
      return Collections.unmodifiableMap(new LinkedHashMap(map));
   }

   public static ThreadFactory threadFactory(final String name, final boolean daemon) {
      return new ThreadFactory() {
         public Thread newThread(Runnable runnable) {
            Thread result = new Thread(runnable, name);
            result.setDaemon(daemon);
            return result;
         }
      };
   }

   public static <T> T[] intersect(Class<T> arrayType, T[] first, T[] second) {
      List<T> result = intersect(first, second);
      return result.toArray((Object[])((Object[])Array.newInstance(arrayType, result.size())));
   }

   private static <T> List<T> intersect(T[] first, T[] second) {
      List<T> result = new ArrayList();
      Object[] var3 = first;
      int var4 = first.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         T a = var3[var5];
         Object[] var7 = second;
         int var8 = second.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            T b = var7[var9];
            if (a.equals(b)) {
               result.add(b);
               break;
            }
         }
      }

      return result;
   }

   public static String hostHeader(HttpUrl url) {
      return url.port() != HttpUrl.defaultPort(url.scheme()) ? url.host() + ":" + url.port() : url.host();
   }

   public static String toHumanReadableAscii(String s) {
      int i = 0;

      int c;
      for(int length = s.length(); i < length; i += Character.charCount(c)) {
         c = s.codePointAt(i);
         if (c <= 31 || c >= 127) {
            Buffer buffer = new Buffer();
            buffer.writeUtf8(s, 0, i);

            for(int j = i; j < length; j += Character.charCount(c)) {
               c = s.codePointAt(j);
               buffer.writeUtf8CodePoint(c > 31 && c < 127 ? c : 63);
            }

            return buffer.readUtf8();
         }
      }

      return s;
   }

   public static boolean isAndroidGetsocknameError(AssertionError e) {
      return e.getCause() != null && e.getMessage() != null && e.getMessage().contains("getsockname failed");
   }

   public static boolean contains(String[] array, String value) {
      return Arrays.asList(array).contains(value);
   }

   public static String[] concat(String[] array, String value) {
      String[] result = new String[array.length + 1];
      System.arraycopy(array, 0, result, 0, array.length);
      result[result.length - 1] = value;
      return result;
   }
}
