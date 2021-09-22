package com.squareup.okhttp.internal;

import android.util.Log;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.internal.tls.AndroidTrustRootIndex;
import com.squareup.okhttp.internal.tls.RealTrustRootIndex;
import com.squareup.okhttp.internal.tls.TrustRootIndex;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okio.Buffer;

public class Platform {
   private static final Platform PLATFORM = findPlatform();

   public static Platform get() {
      return PLATFORM;
   }

   public String getPrefix() {
      return "OkHttp";
   }

   public void logW(String warning) {
      System.out.println(warning);
   }

   public void tagSocket(Socket socket) throws SocketException {
   }

   public void untagSocket(Socket socket) throws SocketException {
   }

   public X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
      return null;
   }

   public TrustRootIndex trustRootIndex(X509TrustManager trustManager) {
      return new RealTrustRootIndex(trustManager.getAcceptedIssuers());
   }

   public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) {
   }

   public void afterHandshake(SSLSocket sslSocket) {
   }

   public String getSelectedProtocol(SSLSocket socket) {
      return null;
   }

   public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException {
      socket.connect(address, connectTimeout);
   }

   public void log(String message) {
      System.out.println(message);
   }

   private static Platform findPlatform() {
      Class sslParametersClass;
      try {
         try {
            sslParametersClass = Class.forName("com.android.org.conscrypt.SSLParametersImpl");
         } catch (ClassNotFoundException var13) {
            sslParametersClass = Class.forName("org.apache.harmony.xnet.provider.jsse.SSLParametersImpl");
         }

         OptionalMethod<Socket> setUseSessionTickets = new OptionalMethod((Class)null, "setUseSessionTickets", new Class[]{Boolean.TYPE});
         OptionalMethod<Socket> setHostname = new OptionalMethod((Class)null, "setHostname", new Class[]{String.class});
         Method trafficStatsTagSocket = null;
         Method trafficStatsUntagSocket = null;
         OptionalMethod<Socket> getAlpnSelectedProtocol = null;
         OptionalMethod setAlpnProtocols = null;

         try {
            Class<?> trafficStats = Class.forName("android.net.TrafficStats");
            trafficStatsTagSocket = trafficStats.getMethod("tagSocket", Socket.class);
            trafficStatsUntagSocket = trafficStats.getMethod("untagSocket", Socket.class);

            try {
               Class.forName("android.net.Network");
               getAlpnSelectedProtocol = new OptionalMethod(byte[].class, "getAlpnSelectedProtocol", new Class[0]);
               setAlpnProtocols = new OptionalMethod((Class)null, "setAlpnProtocols", new Class[]{byte[].class});
            } catch (ClassNotFoundException var11) {
            }
         } catch (NoSuchMethodException | ClassNotFoundException var12) {
         }

         return new Platform.Android(sslParametersClass, setUseSessionTickets, setHostname, trafficStatsTagSocket, trafficStatsUntagSocket, getAlpnSelectedProtocol, setAlpnProtocols);
      } catch (ClassNotFoundException var14) {
         try {
            sslParametersClass = Class.forName("sun.security.ssl.SSLContextImpl");

            try {
               String negoClassName = "org.eclipse.jetty.alpn.ALPN";
               Class<?> negoClass = Class.forName(negoClassName);
               Class<?> providerClass = Class.forName(negoClassName + "$Provider");
               Class<?> clientProviderClass = Class.forName(negoClassName + "$ClientProvider");
               Class<?> serverProviderClass = Class.forName(negoClassName + "$ServerProvider");
               Method putMethod = negoClass.getMethod("put", SSLSocket.class, providerClass);
               Method getMethod = negoClass.getMethod("get", SSLSocket.class);
               Method removeMethod = negoClass.getMethod("remove", SSLSocket.class);
               return new Platform.JdkWithJettyBootPlatform(sslParametersClass, putMethod, getMethod, removeMethod, clientProviderClass, serverProviderClass);
            } catch (NoSuchMethodException | ClassNotFoundException var9) {
               return new Platform.JdkPlatform(sslParametersClass);
            }
         } catch (ClassNotFoundException var10) {
            return new Platform();
         }
      }
   }

   static byte[] concatLengthPrefixed(List<Protocol> protocols) {
      Buffer result = new Buffer();
      int i = 0;

      for(int size = protocols.size(); i < size; ++i) {
         Protocol protocol = (Protocol)protocols.get(i);
         if (protocol != Protocol.HTTP_1_0) {
            result.writeByte(protocol.toString().length());
            result.writeUtf8(protocol.toString());
         }
      }

      return result.readByteArray();
   }

   static <T> T readFieldOrNull(Object instance, Class<T> fieldType, String fieldName) {
      Class c = instance.getClass();

      while(c != Object.class) {
         try {
            Field field = c.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(instance);
            if (value != null && fieldType.isInstance(value)) {
               return fieldType.cast(value);
            }

            return null;
         } catch (NoSuchFieldException var6) {
            c = c.getSuperclass();
         } catch (IllegalAccessException var7) {
            throw new AssertionError();
         }
      }

      if (!fieldName.equals("delegate")) {
         Object delegate = readFieldOrNull(instance, Object.class, "delegate");
         if (delegate != null) {
            return readFieldOrNull(delegate, fieldType, fieldName);
         }
      }

      return null;
   }

   private static class JettyNegoProvider implements InvocationHandler {
      private final List<String> protocols;
      private boolean unsupported;
      private String selected;

      public JettyNegoProvider(List<String> protocols) {
         this.protocols = protocols;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         String methodName = method.getName();
         Class<?> returnType = method.getReturnType();
         if (args == null) {
            args = Util.EMPTY_STRING_ARRAY;
         }

         if (methodName.equals("supports") && Boolean.TYPE == returnType) {
            return true;
         } else if (methodName.equals("unsupported") && Void.TYPE == returnType) {
            this.unsupported = true;
            return null;
         } else if (methodName.equals("protocols") && ((Object[])args).length == 0) {
            return this.protocols;
         } else if ((methodName.equals("selectProtocol") || methodName.equals("select")) && String.class == returnType && ((Object[])args).length == 1 && ((Object[])args)[0] instanceof List) {
            List<String> peerProtocols = (List)((Object[])args)[0];
            int i = 0;

            for(int size = peerProtocols.size(); i < size; ++i) {
               if (this.protocols.contains(peerProtocols.get(i))) {
                  return this.selected = (String)peerProtocols.get(i);
               }
            }

            return this.selected = (String)this.protocols.get(0);
         } else if ((methodName.equals("protocolSelected") || methodName.equals("selected")) && ((Object[])args).length == 1) {
            this.selected = (String)((Object[])args)[0];
            return null;
         } else {
            return method.invoke(this, (Object[])args);
         }
      }
   }

   private static class JdkWithJettyBootPlatform extends Platform.JdkPlatform {
      private final Method putMethod;
      private final Method getMethod;
      private final Method removeMethod;
      private final Class<?> clientProviderClass;
      private final Class<?> serverProviderClass;

      public JdkWithJettyBootPlatform(Class<?> sslContextClass, Method putMethod, Method getMethod, Method removeMethod, Class<?> clientProviderClass, Class<?> serverProviderClass) {
         super(sslContextClass);
         this.putMethod = putMethod;
         this.getMethod = getMethod;
         this.removeMethod = removeMethod;
         this.clientProviderClass = clientProviderClass;
         this.serverProviderClass = serverProviderClass;
      }

      public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) {
         List<String> names = new ArrayList(protocols.size());
         int i = 0;

         for(int size = protocols.size(); i < size; ++i) {
            Protocol protocol = (Protocol)protocols.get(i);
            if (protocol != Protocol.HTTP_1_0) {
               names.add(protocol.toString());
            }
         }

         try {
            Object provider = Proxy.newProxyInstance(Platform.class.getClassLoader(), new Class[]{this.clientProviderClass, this.serverProviderClass}, new Platform.JettyNegoProvider(names));
            this.putMethod.invoke((Object)null, sslSocket, provider);
         } catch (IllegalAccessException | InvocationTargetException var8) {
            throw new AssertionError(var8);
         }
      }

      public void afterHandshake(SSLSocket sslSocket) {
         try {
            this.removeMethod.invoke((Object)null, sslSocket);
         } catch (InvocationTargetException | IllegalAccessException var3) {
            throw new AssertionError();
         }
      }

      public String getSelectedProtocol(SSLSocket socket) {
         try {
            Platform.JettyNegoProvider provider = (Platform.JettyNegoProvider)Proxy.getInvocationHandler(this.getMethod.invoke((Object)null, socket));
            if (!provider.unsupported && provider.selected == null) {
               Internal.logger.log(Level.INFO, "ALPN callback dropped: SPDY and HTTP/2 are disabled. Is alpn-boot on the boot class path?");
               return null;
            } else {
               return provider.unsupported ? null : provider.selected;
            }
         } catch (IllegalAccessException | InvocationTargetException var3) {
            throw new AssertionError();
         }
      }
   }

   private static class JdkPlatform extends Platform {
      private final Class<?> sslContextClass;

      public JdkPlatform(Class<?> sslContextClass) {
         this.sslContextClass = sslContextClass;
      }

      public X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
         Object context = readFieldOrNull(sslSocketFactory, this.sslContextClass, "context");
         return context == null ? null : (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "trustManager");
      }
   }

   private static class Android extends Platform {
      private static final int MAX_LOG_LENGTH = 4000;
      private final Class<?> sslParametersClass;
      private final OptionalMethod<Socket> setUseSessionTickets;
      private final OptionalMethod<Socket> setHostname;
      private final Method trafficStatsTagSocket;
      private final Method trafficStatsUntagSocket;
      private final OptionalMethod<Socket> getAlpnSelectedProtocol;
      private final OptionalMethod<Socket> setAlpnProtocols;

      public Android(Class<?> sslParametersClass, OptionalMethod<Socket> setUseSessionTickets, OptionalMethod<Socket> setHostname, Method trafficStatsTagSocket, Method trafficStatsUntagSocket, OptionalMethod<Socket> getAlpnSelectedProtocol, OptionalMethod<Socket> setAlpnProtocols) {
         this.sslParametersClass = sslParametersClass;
         this.setUseSessionTickets = setUseSessionTickets;
         this.setHostname = setHostname;
         this.trafficStatsTagSocket = trafficStatsTagSocket;
         this.trafficStatsUntagSocket = trafficStatsUntagSocket;
         this.getAlpnSelectedProtocol = getAlpnSelectedProtocol;
         this.setAlpnProtocols = setAlpnProtocols;
      }

      public void connectSocket(Socket socket, InetSocketAddress address, int connectTimeout) throws IOException {
         try {
            socket.connect(address, connectTimeout);
         } catch (AssertionError var6) {
            if (Util.isAndroidGetsocknameError(var6)) {
               throw new IOException(var6);
            } else {
               throw var6;
            }
         } catch (SecurityException var7) {
            IOException ioException = new IOException("Exception in connect");
            ioException.initCause(var7);
            throw ioException;
         }
      }

      public X509TrustManager trustManager(SSLSocketFactory sslSocketFactory) {
         Object context = readFieldOrNull(sslSocketFactory, this.sslParametersClass, "sslParameters");
         if (context == null) {
            try {
               Class<?> gmsSslParametersClass = Class.forName("com.google.android.gms.org.conscrypt.SSLParametersImpl", false, sslSocketFactory.getClass().getClassLoader());
               context = readFieldOrNull(sslSocketFactory, gmsSslParametersClass, "sslParameters");
            } catch (ClassNotFoundException var4) {
               return null;
            }
         }

         X509TrustManager x509TrustManager = (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "x509TrustManager");
         return x509TrustManager != null ? x509TrustManager : (X509TrustManager)readFieldOrNull(context, X509TrustManager.class, "trustManager");
      }

      public TrustRootIndex trustRootIndex(X509TrustManager trustManager) {
         TrustRootIndex result = AndroidTrustRootIndex.get(trustManager);
         return result != null ? result : super.trustRootIndex(trustManager);
      }

      public void configureTlsExtensions(SSLSocket sslSocket, String hostname, List<Protocol> protocols) {
         if (hostname != null) {
            this.setUseSessionTickets.invokeOptionalWithoutCheckedException(sslSocket, true);
            this.setHostname.invokeOptionalWithoutCheckedException(sslSocket, hostname);
         }

         if (this.setAlpnProtocols != null && this.setAlpnProtocols.isSupported(sslSocket)) {
            Object[] parameters = new Object[]{concatLengthPrefixed(protocols)};
            this.setAlpnProtocols.invokeWithoutCheckedException(sslSocket, parameters);
         }

      }

      public String getSelectedProtocol(SSLSocket socket) {
         if (this.getAlpnSelectedProtocol == null) {
            return null;
         } else if (!this.getAlpnSelectedProtocol.isSupported(socket)) {
            return null;
         } else {
            byte[] alpnResult = (byte[])((byte[])this.getAlpnSelectedProtocol.invokeWithoutCheckedException(socket));
            return alpnResult != null ? new String(alpnResult, Util.UTF_8) : null;
         }
      }

      public void tagSocket(Socket socket) throws SocketException {
         if (this.trafficStatsTagSocket != null) {
            try {
               this.trafficStatsTagSocket.invoke((Object)null, socket);
            } catch (IllegalAccessException var3) {
               throw new RuntimeException(var3);
            } catch (InvocationTargetException var4) {
               throw new RuntimeException(var4.getCause());
            }
         }
      }

      public void untagSocket(Socket socket) throws SocketException {
         if (this.trafficStatsUntagSocket != null) {
            try {
               this.trafficStatsUntagSocket.invoke((Object)null, socket);
            } catch (IllegalAccessException var3) {
               throw new RuntimeException(var3);
            } catch (InvocationTargetException var4) {
               throw new RuntimeException(var4.getCause());
            }
         }
      }

      public void log(String message) {
         int i = 0;

         int end;
         for(int length = message.length(); i < length; i = end + 1) {
            int newline = message.indexOf(10, i);
            newline = newline != -1 ? newline : length;

            do {
               end = Math.min(newline, i + 4000);
               Log.d("OkHttp", message.substring(i, end));
               i = end;
            } while(end < newline);
         }

      }
   }
}
