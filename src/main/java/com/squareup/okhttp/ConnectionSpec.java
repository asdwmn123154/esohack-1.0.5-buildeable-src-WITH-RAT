package com.squareup.okhttp;

import com.squareup.okhttp.internal.Util;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLSocket;

public final class ConnectionSpec {
   private static final CipherSuite[] APPROVED_CIPHER_SUITES;
   public static final ConnectionSpec MODERN_TLS;
   public static final ConnectionSpec COMPATIBLE_TLS;
   public static final ConnectionSpec CLEARTEXT;
   private final boolean tls;
   private final boolean supportsTlsExtensions;
   private final String[] cipherSuites;
   private final String[] tlsVersions;

   private ConnectionSpec(ConnectionSpec.Builder builder) {
      this.tls = builder.tls;
      this.cipherSuites = builder.cipherSuites;
      this.tlsVersions = builder.tlsVersions;
      this.supportsTlsExtensions = builder.supportsTlsExtensions;
   }

   public boolean isTls() {
      return this.tls;
   }

   public List<CipherSuite> cipherSuites() {
      if (this.cipherSuites == null) {
         return null;
      } else {
         CipherSuite[] result = new CipherSuite[this.cipherSuites.length];

         for(int i = 0; i < this.cipherSuites.length; ++i) {
            result[i] = CipherSuite.forJavaName(this.cipherSuites[i]);
         }

         return Util.immutableList((Object[])result);
      }
   }

   public List<TlsVersion> tlsVersions() {
      if (this.tlsVersions == null) {
         return null;
      } else {
         TlsVersion[] result = new TlsVersion[this.tlsVersions.length];

         for(int i = 0; i < this.tlsVersions.length; ++i) {
            result[i] = TlsVersion.forJavaName(this.tlsVersions[i]);
         }

         return Util.immutableList((Object[])result);
      }
   }

   public boolean supportsTlsExtensions() {
      return this.supportsTlsExtensions;
   }

   void apply(SSLSocket sslSocket, boolean isFallback) {
      ConnectionSpec specToApply = this.supportedSpec(sslSocket, isFallback);
      if (specToApply.tlsVersions != null) {
         sslSocket.setEnabledProtocols(specToApply.tlsVersions);
      }

      if (specToApply.cipherSuites != null) {
         sslSocket.setEnabledCipherSuites(specToApply.cipherSuites);
      }

   }

   private ConnectionSpec supportedSpec(SSLSocket sslSocket, boolean isFallback) {
      String[] cipherSuitesIntersection = this.cipherSuites != null ? (String[])Util.intersect(String.class, this.cipherSuites, sslSocket.getEnabledCipherSuites()) : sslSocket.getEnabledCipherSuites();
      String[] tlsVersionsIntersection = this.tlsVersions != null ? (String[])Util.intersect(String.class, this.tlsVersions, sslSocket.getEnabledProtocols()) : sslSocket.getEnabledProtocols();
      if (isFallback && Util.contains(sslSocket.getSupportedCipherSuites(), "TLS_FALLBACK_SCSV")) {
         cipherSuitesIntersection = Util.concat(cipherSuitesIntersection, "TLS_FALLBACK_SCSV");
      }

      return (new ConnectionSpec.Builder(this)).cipherSuites(cipherSuitesIntersection).tlsVersions(tlsVersionsIntersection).build();
   }

   public boolean isCompatible(SSLSocket socket) {
      if (!this.tls) {
         return false;
      } else if (this.tlsVersions != null && !nonEmptyIntersection(this.tlsVersions, socket.getEnabledProtocols())) {
         return false;
      } else {
         return this.cipherSuites == null || nonEmptyIntersection(this.cipherSuites, socket.getEnabledCipherSuites());
      }
   }

   private static boolean nonEmptyIntersection(String[] a, String[] b) {
      if (a != null && b != null && a.length != 0 && b.length != 0) {
         String[] var2 = a;
         int var3 = a.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            String toFind = var2[var4];
            if (Util.contains(b, toFind)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean equals(Object other) {
      if (!(other instanceof ConnectionSpec)) {
         return false;
      } else if (other == this) {
         return true;
      } else {
         ConnectionSpec that = (ConnectionSpec)other;
         if (this.tls != that.tls) {
            return false;
         } else {
            if (this.tls) {
               if (!Arrays.equals(this.cipherSuites, that.cipherSuites)) {
                  return false;
               }

               if (!Arrays.equals(this.tlsVersions, that.tlsVersions)) {
                  return false;
               }

               if (this.supportsTlsExtensions != that.supportsTlsExtensions) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public int hashCode() {
      int result = 17;
      if (this.tls) {
         result = 31 * result + Arrays.hashCode(this.cipherSuites);
         result = 31 * result + Arrays.hashCode(this.tlsVersions);
         result = 31 * result + (this.supportsTlsExtensions ? 0 : 1);
      }

      return result;
   }

   public String toString() {
      if (!this.tls) {
         return "ConnectionSpec()";
      } else {
         String cipherSuitesString = this.cipherSuites != null ? this.cipherSuites().toString() : "[all enabled]";
         String tlsVersionsString = this.tlsVersions != null ? this.tlsVersions().toString() : "[all enabled]";
         return "ConnectionSpec(cipherSuites=" + cipherSuitesString + ", tlsVersions=" + tlsVersionsString + ", supportsTlsExtensions=" + this.supportsTlsExtensions + ")";
      }
   }

   // $FF: synthetic method
   ConnectionSpec(ConnectionSpec.Builder x0, Object x1) {
      this(x0);
   }

   static {
      APPROVED_CIPHER_SUITES = new CipherSuite[]{CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_DHE_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_DHE_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256, CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA, CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA, CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA};
      MODERN_TLS = (new ConnectionSpec.Builder(true)).cipherSuites(APPROVED_CIPHER_SUITES).tlsVersions(TlsVersion.TLS_1_2, TlsVersion.TLS_1_1, TlsVersion.TLS_1_0).supportsTlsExtensions(true).build();
      COMPATIBLE_TLS = (new ConnectionSpec.Builder(MODERN_TLS)).tlsVersions(TlsVersion.TLS_1_0).supportsTlsExtensions(true).build();
      CLEARTEXT = (new ConnectionSpec.Builder(false)).build();
   }

   public static final class Builder {
      private boolean tls;
      private String[] cipherSuites;
      private String[] tlsVersions;
      private boolean supportsTlsExtensions;

      Builder(boolean tls) {
         this.tls = tls;
      }

      public Builder(ConnectionSpec connectionSpec) {
         this.tls = connectionSpec.tls;
         this.cipherSuites = connectionSpec.cipherSuites;
         this.tlsVersions = connectionSpec.tlsVersions;
         this.supportsTlsExtensions = connectionSpec.supportsTlsExtensions;
      }

      public ConnectionSpec.Builder allEnabledCipherSuites() {
         if (!this.tls) {
            throw new IllegalStateException("no cipher suites for cleartext connections");
         } else {
            this.cipherSuites = null;
            return this;
         }
      }

      public ConnectionSpec.Builder cipherSuites(CipherSuite... cipherSuites) {
         if (!this.tls) {
            throw new IllegalStateException("no cipher suites for cleartext connections");
         } else {
            String[] strings = new String[cipherSuites.length];

            for(int i = 0; i < cipherSuites.length; ++i) {
               strings[i] = cipherSuites[i].javaName;
            }

            return this.cipherSuites(strings);
         }
      }

      public ConnectionSpec.Builder cipherSuites(String... cipherSuites) {
         if (!this.tls) {
            throw new IllegalStateException("no cipher suites for cleartext connections");
         } else if (cipherSuites.length == 0) {
            throw new IllegalArgumentException("At least one cipher suite is required");
         } else {
            this.cipherSuites = (String[])cipherSuites.clone();
            return this;
         }
      }

      public ConnectionSpec.Builder allEnabledTlsVersions() {
         if (!this.tls) {
            throw new IllegalStateException("no TLS versions for cleartext connections");
         } else {
            this.tlsVersions = null;
            return this;
         }
      }

      public ConnectionSpec.Builder tlsVersions(TlsVersion... tlsVersions) {
         if (!this.tls) {
            throw new IllegalStateException("no TLS versions for cleartext connections");
         } else {
            String[] strings = new String[tlsVersions.length];

            for(int i = 0; i < tlsVersions.length; ++i) {
               strings[i] = tlsVersions[i].javaName;
            }

            return this.tlsVersions(strings);
         }
      }

      public ConnectionSpec.Builder tlsVersions(String... tlsVersions) {
         if (!this.tls) {
            throw new IllegalStateException("no TLS versions for cleartext connections");
         } else if (tlsVersions.length == 0) {
            throw new IllegalArgumentException("At least one TLS version is required");
         } else {
            this.tlsVersions = (String[])tlsVersions.clone();
            return this;
         }
      }

      public ConnectionSpec.Builder supportsTlsExtensions(boolean supportsTlsExtensions) {
         if (!this.tls) {
            throw new IllegalStateException("no TLS extensions for cleartext connections");
         } else {
            this.supportsTlsExtensions = supportsTlsExtensions;
            return this;
         }
      }

      public ConnectionSpec build() {
         return new ConnectionSpec(this);
      }
   }
}
