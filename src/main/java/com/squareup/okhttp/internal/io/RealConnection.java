package com.squareup.okhttp.internal.io;

import com.squareup.okhttp.Address;
import com.squareup.okhttp.CertificatePinner;
import com.squareup.okhttp.Connection;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.Handshake;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.Route;
import com.squareup.okhttp.internal.ConnectionSpecSelector;
import com.squareup.okhttp.internal.Platform;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.Version;
import com.squareup.okhttp.internal.framed.FramedConnection;
import com.squareup.okhttp.internal.http.Http1xStream;
import com.squareup.okhttp.internal.http.OkHeaders;
import com.squareup.okhttp.internal.http.RouteException;
import com.squareup.okhttp.internal.http.StreamAllocation;
import com.squareup.okhttp.internal.tls.CertificateChainCleaner;
import com.squareup.okhttp.internal.tls.OkHostnameVerifier;
import com.squareup.okhttp.internal.tls.TrustRootIndex;
import java.io.IOException;
import java.lang.ref.Reference;
import java.net.ConnectException;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownServiceException;
import java.net.Proxy.Type;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;

public final class RealConnection implements Connection {
   private final Route route;
   private Socket rawSocket;
   public Socket socket;
   private Handshake handshake;
   private Protocol protocol;
   public volatile FramedConnection framedConnection;
   public int streamCount;
   public BufferedSource source;
   public BufferedSink sink;
   public final List<Reference<StreamAllocation>> allocations = new ArrayList();
   public boolean noNewStreams;
   public long idleAtNanos = Long.MAX_VALUE;
   private static SSLSocketFactory lastSslSocketFactory;
   private static TrustRootIndex lastTrustRootIndex;

   public RealConnection(Route route) {
      this.route = route;
   }

   public void connect(int connectTimeout, int readTimeout, int writeTimeout, List<ConnectionSpec> connectionSpecs, boolean connectionRetryEnabled) throws RouteException {
      if (this.protocol != null) {
         throw new IllegalStateException("already connected");
      } else {
         RouteException routeException = null;
         ConnectionSpecSelector connectionSpecSelector = new ConnectionSpecSelector(connectionSpecs);
         Proxy proxy = this.route.getProxy();
         Address address = this.route.getAddress();
         if (this.route.getAddress().getSslSocketFactory() == null && !connectionSpecs.contains(ConnectionSpec.CLEARTEXT)) {
            throw new RouteException(new UnknownServiceException("CLEARTEXT communication not supported: " + connectionSpecs));
         } else {
            while(this.protocol == null) {
               try {
                  this.rawSocket = proxy.type() != Type.DIRECT && proxy.type() != Type.HTTP ? new Socket(proxy) : address.getSocketFactory().createSocket();
                  this.connectSocket(connectTimeout, readTimeout, writeTimeout, connectionSpecSelector);
               } catch (IOException var11) {
                  Util.closeQuietly(this.socket);
                  Util.closeQuietly(this.rawSocket);
                  this.socket = null;
                  this.rawSocket = null;
                  this.source = null;
                  this.sink = null;
                  this.handshake = null;
                  this.protocol = null;
                  if (routeException == null) {
                     routeException = new RouteException(var11);
                  } else {
                     routeException.addConnectException(var11);
                  }

                  if (!connectionRetryEnabled || !connectionSpecSelector.connectionFailed(var11)) {
                     throw routeException;
                  }
               }
            }

         }
      }
   }

   private void connectSocket(int connectTimeout, int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException {
      this.rawSocket.setSoTimeout(readTimeout);

      try {
         Platform.get().connectSocket(this.rawSocket, this.route.getSocketAddress(), connectTimeout);
      } catch (ConnectException var6) {
         throw new ConnectException("Failed to connect to " + this.route.getSocketAddress());
      }

      this.source = Okio.buffer(Okio.source(this.rawSocket));
      this.sink = Okio.buffer(Okio.sink(this.rawSocket));
      if (this.route.getAddress().getSslSocketFactory() != null) {
         this.connectTls(readTimeout, writeTimeout, connectionSpecSelector);
      } else {
         this.protocol = Protocol.HTTP_1_1;
         this.socket = this.rawSocket;
      }

      if (this.protocol == Protocol.SPDY_3 || this.protocol == Protocol.HTTP_2) {
         this.socket.setSoTimeout(0);
         FramedConnection framedConnection = (new FramedConnection.Builder(true)).socket(this.socket, this.route.getAddress().url().host(), this.source, this.sink).protocol(this.protocol).build();
         framedConnection.sendConnectionPreface();
         this.framedConnection = framedConnection;
      }

   }

   private void connectTls(int readTimeout, int writeTimeout, ConnectionSpecSelector connectionSpecSelector) throws IOException {
      if (this.route.requiresTunnel()) {
         this.createTunnel(readTimeout, writeTimeout);
      }

      Address address = this.route.getAddress();
      SSLSocketFactory sslSocketFactory = address.getSslSocketFactory();
      boolean success = false;
      SSLSocket sslSocket = null;

      try {
         sslSocket = (SSLSocket)sslSocketFactory.createSocket(this.rawSocket, address.getUriHost(), address.getUriPort(), true);
         ConnectionSpec connectionSpec = connectionSpecSelector.configureSecureSocket(sslSocket);
         if (connectionSpec.supportsTlsExtensions()) {
            Platform.get().configureTlsExtensions(sslSocket, address.getUriHost(), address.getProtocols());
         }

         sslSocket.startHandshake();
         Handshake unverifiedHandshake = Handshake.get(sslSocket.getSession());
         if (!address.getHostnameVerifier().verify(address.getUriHost(), sslSocket.getSession())) {
            X509Certificate cert = (X509Certificate)unverifiedHandshake.peerCertificates().get(0);
            throw new SSLPeerUnverifiedException("Hostname " + address.getUriHost() + " not verified:" + "\n    certificate: " + CertificatePinner.pin(cert) + "\n    DN: " + cert.getSubjectDN().getName() + "\n    subjectAltNames: " + OkHostnameVerifier.allSubjectAltNames(cert));
         }

         if (address.getCertificatePinner() != CertificatePinner.DEFAULT) {
            TrustRootIndex trustRootIndex = trustRootIndex(address.getSslSocketFactory());
            List<Certificate> certificates = (new CertificateChainCleaner(trustRootIndex)).clean(unverifiedHandshake.peerCertificates());
            address.getCertificatePinner().check(address.getUriHost(), certificates);
         }

         String maybeProtocol = connectionSpec.supportsTlsExtensions() ? Platform.get().getSelectedProtocol(sslSocket) : null;
         this.socket = sslSocket;
         this.source = Okio.buffer(Okio.source(this.socket));
         this.sink = Okio.buffer(Okio.sink(this.socket));
         this.handshake = unverifiedHandshake;
         this.protocol = maybeProtocol != null ? Protocol.get(maybeProtocol) : Protocol.HTTP_1_1;
         success = true;
      } catch (AssertionError var15) {
         if (Util.isAndroidGetsocknameError(var15)) {
            throw new IOException(var15);
         }

         throw var15;
      } finally {
         if (sslSocket != null) {
            Platform.get().afterHandshake(sslSocket);
         }

         if (!success) {
            Util.closeQuietly((Socket)sslSocket);
         }

      }

   }

   private static synchronized TrustRootIndex trustRootIndex(SSLSocketFactory sslSocketFactory) {
      if (sslSocketFactory != lastSslSocketFactory) {
         X509TrustManager trustManager = Platform.get().trustManager(sslSocketFactory);
         lastTrustRootIndex = Platform.get().trustRootIndex(trustManager);
         lastSslSocketFactory = sslSocketFactory;
      }

      return lastTrustRootIndex;
   }

   private void createTunnel(int readTimeout, int writeTimeout) throws IOException {
      Request tunnelRequest = this.createTunnelRequest();
      HttpUrl url = tunnelRequest.httpUrl();
      String requestLine = "CONNECT " + url.host() + ":" + url.port() + " HTTP/1.1";

      do {
         Http1xStream tunnelConnection = new Http1xStream((StreamAllocation)null, this.source, this.sink);
         this.source.timeout().timeout((long)readTimeout, TimeUnit.MILLISECONDS);
         this.sink.timeout().timeout((long)writeTimeout, TimeUnit.MILLISECONDS);
         tunnelConnection.writeRequest(tunnelRequest.headers(), requestLine);
         tunnelConnection.finishRequest();
         Response response = tunnelConnection.readResponse().request(tunnelRequest).build();
         long contentLength = OkHeaders.contentLength(response);
         if (contentLength == -1L) {
            contentLength = 0L;
         }

         Source body = tunnelConnection.newFixedLengthSource(contentLength);
         Util.skipAll(body, Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
         body.close();
         switch(response.code()) {
         case 200:
            if (this.source.buffer().exhausted() && this.sink.buffer().exhausted()) {
               return;
            }

            throw new IOException("TLS tunnel buffered too many bytes!");
         case 407:
            tunnelRequest = OkHeaders.processAuthHeader(this.route.getAddress().getAuthenticator(), response, this.route.getProxy());
            break;
         default:
            throw new IOException("Unexpected response code for CONNECT: " + response.code());
         }
      } while(tunnelRequest != null);

      throw new IOException("Failed to authenticate with proxy");
   }

   private Request createTunnelRequest() throws IOException {
      return (new Request.Builder()).url(this.route.getAddress().url()).header("Host", Util.hostHeader(this.route.getAddress().url())).header("Proxy-Connection", "Keep-Alive").header("User-Agent", Version.userAgent()).build();
   }

   boolean isConnected() {
      return this.protocol != null;
   }

   public Route getRoute() {
      return this.route;
   }

   public void cancel() {
      Util.closeQuietly(this.rawSocket);
   }

   public Socket getSocket() {
      return this.socket;
   }

   public int allocationLimit() {
      FramedConnection framedConnection = this.framedConnection;
      return framedConnection != null ? framedConnection.maxConcurrentStreams() : 1;
   }

   public boolean isHealthy(boolean doExtensiveChecks) {
      if (!this.socket.isClosed() && !this.socket.isInputShutdown() && !this.socket.isOutputShutdown()) {
         if (this.framedConnection != null) {
            return true;
         } else {
            if (doExtensiveChecks) {
               try {
                  int readTimeout = this.socket.getSoTimeout();

                  boolean var3;
                  try {
                     this.socket.setSoTimeout(1);
                     if (this.source.exhausted()) {
                        var3 = false;
                        return var3;
                     }

                     var3 = true;
                  } finally {
                     this.socket.setSoTimeout(readTimeout);
                  }

                  return var3;
               } catch (SocketTimeoutException var9) {
               } catch (IOException var10) {
                  return false;
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public Handshake getHandshake() {
      return this.handshake;
   }

   public boolean isMultiplexed() {
      return this.framedConnection != null;
   }

   public Protocol getProtocol() {
      return this.protocol != null ? this.protocol : Protocol.HTTP_1_1;
   }

   public String toString() {
      return "Connection{" + this.route.getAddress().url().host() + ":" + this.route.getAddress().url().port() + ", proxy=" + this.route.getProxy() + " hostAddress=" + this.route.getSocketAddress() + " cipherSuite=" + (this.handshake != null ? this.handshake.cipherSuite() : "none") + " protocol=" + this.protocol + '}';
   }
}
