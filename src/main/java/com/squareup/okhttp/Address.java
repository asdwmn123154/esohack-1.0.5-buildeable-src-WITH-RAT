package com.squareup.okhttp;

import com.squareup.okhttp.internal.Util;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

public final class Address {
   final HttpUrl url;
   final Dns dns;
   final SocketFactory socketFactory;
   final Authenticator authenticator;
   final List<Protocol> protocols;
   final List<ConnectionSpec> connectionSpecs;
   final ProxySelector proxySelector;
   final Proxy proxy;
   final SSLSocketFactory sslSocketFactory;
   final HostnameVerifier hostnameVerifier;
   final CertificatePinner certificatePinner;

   public Address(String uriHost, int uriPort, Dns dns, SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier, CertificatePinner certificatePinner, Authenticator authenticator, Proxy proxy, List<Protocol> protocols, List<ConnectionSpec> connectionSpecs, ProxySelector proxySelector) {
      this.url = (new HttpUrl.Builder()).scheme(sslSocketFactory != null ? "https" : "http").host(uriHost).port(uriPort).build();
      if (dns == null) {
         throw new IllegalArgumentException("dns == null");
      } else {
         this.dns = dns;
         if (socketFactory == null) {
            throw new IllegalArgumentException("socketFactory == null");
         } else {
            this.socketFactory = socketFactory;
            if (authenticator == null) {
               throw new IllegalArgumentException("authenticator == null");
            } else {
               this.authenticator = authenticator;
               if (protocols == null) {
                  throw new IllegalArgumentException("protocols == null");
               } else {
                  this.protocols = Util.immutableList(protocols);
                  if (connectionSpecs == null) {
                     throw new IllegalArgumentException("connectionSpecs == null");
                  } else {
                     this.connectionSpecs = Util.immutableList(connectionSpecs);
                     if (proxySelector == null) {
                        throw new IllegalArgumentException("proxySelector == null");
                     } else {
                        this.proxySelector = proxySelector;
                        this.proxy = proxy;
                        this.sslSocketFactory = sslSocketFactory;
                        this.hostnameVerifier = hostnameVerifier;
                        this.certificatePinner = certificatePinner;
                     }
                  }
               }
            }
         }
      }
   }

   public HttpUrl url() {
      return this.url;
   }

   /** @deprecated */
   @Deprecated
   public String getUriHost() {
      return this.url.host();
   }

   /** @deprecated */
   @Deprecated
   public int getUriPort() {
      return this.url.port();
   }

   public Dns getDns() {
      return this.dns;
   }

   public SocketFactory getSocketFactory() {
      return this.socketFactory;
   }

   public Authenticator getAuthenticator() {
      return this.authenticator;
   }

   public List<Protocol> getProtocols() {
      return this.protocols;
   }

   public List<ConnectionSpec> getConnectionSpecs() {
      return this.connectionSpecs;
   }

   public ProxySelector getProxySelector() {
      return this.proxySelector;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public SSLSocketFactory getSslSocketFactory() {
      return this.sslSocketFactory;
   }

   public HostnameVerifier getHostnameVerifier() {
      return this.hostnameVerifier;
   }

   public CertificatePinner getCertificatePinner() {
      return this.certificatePinner;
   }

   public boolean equals(Object other) {
      if (!(other instanceof Address)) {
         return false;
      } else {
         Address that = (Address)other;
         return this.url.equals(that.url) && this.dns.equals(that.dns) && this.authenticator.equals(that.authenticator) && this.protocols.equals(that.protocols) && this.connectionSpecs.equals(that.connectionSpecs) && this.proxySelector.equals(that.proxySelector) && Util.equal(this.proxy, that.proxy) && Util.equal(this.sslSocketFactory, that.sslSocketFactory) && Util.equal(this.hostnameVerifier, that.hostnameVerifier) && Util.equal(this.certificatePinner, that.certificatePinner);
      }
   }

   public int hashCode() {
      int result = 17;
      int result = 31 * result + this.url.hashCode();
      result = 31 * result + this.dns.hashCode();
      result = 31 * result + this.authenticator.hashCode();
      result = 31 * result + this.protocols.hashCode();
      result = 31 * result + this.connectionSpecs.hashCode();
      result = 31 * result + this.proxySelector.hashCode();
      result = 31 * result + (this.proxy != null ? this.proxy.hashCode() : 0);
      result = 31 * result + (this.sslSocketFactory != null ? this.sslSocketFactory.hashCode() : 0);
      result = 31 * result + (this.hostnameVerifier != null ? this.hostnameVerifier.hashCode() : 0);
      result = 31 * result + (this.certificatePinner != null ? this.certificatePinner.hashCode() : 0);
      return result;
   }
}
