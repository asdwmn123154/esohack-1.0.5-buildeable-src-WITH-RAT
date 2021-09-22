package com.squareup.okhttp.internal.tls;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.x500.X500Principal;

public final class RealTrustRootIndex implements TrustRootIndex {
   private final Map<X500Principal, List<X509Certificate>> subjectToCaCerts = new LinkedHashMap();

   public RealTrustRootIndex(X509Certificate... caCerts) {
      X509Certificate[] var2 = caCerts;
      int var3 = caCerts.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         X509Certificate caCert = var2[var4];
         X500Principal subject = caCert.getSubjectX500Principal();
         List<X509Certificate> subjectCaCerts = (List)this.subjectToCaCerts.get(subject);
         if (subjectCaCerts == null) {
            subjectCaCerts = new ArrayList(1);
            this.subjectToCaCerts.put(subject, subjectCaCerts);
         }

         ((List)subjectCaCerts).add(caCert);
      }

   }

   public X509Certificate findByIssuerAndSignature(X509Certificate cert) {
      X500Principal issuer = cert.getIssuerX500Principal();
      List<X509Certificate> subjectCaCerts = (List)this.subjectToCaCerts.get(issuer);
      if (subjectCaCerts == null) {
         return null;
      } else {
         Iterator var4 = subjectCaCerts.iterator();

         while(var4.hasNext()) {
            X509Certificate caCert = (X509Certificate)var4.next();
            PublicKey publicKey = caCert.getPublicKey();

            try {
               cert.verify(publicKey);
               return caCert;
            } catch (Exception var8) {
            }
         }

         return null;
      }
   }
}
