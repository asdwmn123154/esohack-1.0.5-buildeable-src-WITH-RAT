package com.squareup.okhttp;

import com.squareup.okhttp.internal.Util;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLPeerUnverifiedException;
import okio.ByteString;

public final class CertificatePinner {
   public static final CertificatePinner DEFAULT = (new CertificatePinner.Builder()).build();
   private final Map<String, Set<ByteString>> hostnameToPins;

   private CertificatePinner(CertificatePinner.Builder builder) {
      this.hostnameToPins = Util.immutableMap(builder.hostnameToPins);
   }

   public void check(String hostname, List<Certificate> peerCertificates) throws SSLPeerUnverifiedException {
      Set<ByteString> pins = this.findMatchingPins(hostname);
      if (pins != null) {
         int i = 0;

         int i;
         for(i = peerCertificates.size(); i < i; ++i) {
            X509Certificate x509Certificate = (X509Certificate)peerCertificates.get(i);
            if (pins.contains(sha1(x509Certificate))) {
               return;
            }
         }

         StringBuilder message = (new StringBuilder()).append("Certificate pinning failure!").append("\n  Peer certificate chain:");
         i = 0;

         for(int size = peerCertificates.size(); i < size; ++i) {
            X509Certificate x509Certificate = (X509Certificate)peerCertificates.get(i);
            message.append("\n    ").append(pin(x509Certificate)).append(": ").append(x509Certificate.getSubjectDN().getName());
         }

         message.append("\n  Pinned certificates for ").append(hostname).append(":");
         Iterator var9 = pins.iterator();

         while(var9.hasNext()) {
            ByteString pin = (ByteString)var9.next();
            message.append("\n    sha1/").append(pin.base64());
         }

         throw new SSLPeerUnverifiedException(message.toString());
      }
   }

   /** @deprecated */
   public void check(String hostname, Certificate... peerCertificates) throws SSLPeerUnverifiedException {
      this.check(hostname, Arrays.asList(peerCertificates));
   }

   Set<ByteString> findMatchingPins(String hostname) {
      Set<ByteString> directPins = (Set)this.hostnameToPins.get(hostname);
      Set<ByteString> wildcardPins = null;
      int indexOfFirstDot = hostname.indexOf(46);
      int indexOfLastDot = hostname.lastIndexOf(46);
      if (indexOfFirstDot != indexOfLastDot) {
         wildcardPins = (Set)this.hostnameToPins.get("*." + hostname.substring(indexOfFirstDot + 1));
      }

      if (directPins == null && wildcardPins == null) {
         return null;
      } else if (directPins != null && wildcardPins != null) {
         Set<ByteString> pins = new LinkedHashSet();
         pins.addAll(directPins);
         pins.addAll(wildcardPins);
         return pins;
      } else {
         return directPins != null ? directPins : wildcardPins;
      }
   }

   public static String pin(Certificate certificate) {
      if (!(certificate instanceof X509Certificate)) {
         throw new IllegalArgumentException("Certificate pinning requires X509 certificates");
      } else {
         return "sha1/" + sha1((X509Certificate)certificate).base64();
      }
   }

   private static ByteString sha1(X509Certificate x509Certificate) {
      return Util.sha1(ByteString.of(x509Certificate.getPublicKey().getEncoded()));
   }

   // $FF: synthetic method
   CertificatePinner(CertificatePinner.Builder x0, Object x1) {
      this(x0);
   }

   public static final class Builder {
      private final Map<String, Set<ByteString>> hostnameToPins = new LinkedHashMap();

      public CertificatePinner.Builder add(String hostname, String... pins) {
         if (hostname == null) {
            throw new IllegalArgumentException("hostname == null");
         } else {
            Set<ByteString> hostPins = new LinkedHashSet();
            Set<ByteString> previousPins = (Set)this.hostnameToPins.put(hostname, Collections.unmodifiableSet(hostPins));
            if (previousPins != null) {
               hostPins.addAll(previousPins);
            }

            String[] var5 = pins;
            int var6 = pins.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               String pin = var5[var7];
               if (!pin.startsWith("sha1/")) {
                  throw new IllegalArgumentException("pins must start with 'sha1/': " + pin);
               }

               ByteString decodedPin = ByteString.decodeBase64(pin.substring("sha1/".length()));
               if (decodedPin == null) {
                  throw new IllegalArgumentException("pins must be base64: " + pin);
               }

               hostPins.add(decodedPin);
            }

            return this;
         }
      }

      public CertificatePinner build() {
         return new CertificatePinner(this);
      }
   }
}
