package com.squareup.okhttp.internal.tls;

import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import javax.net.ssl.SSLPeerUnverifiedException;

public final class CertificateChainCleaner {
   private static final int MAX_SIGNERS = 9;
   private final TrustRootIndex trustRootIndex;

   public CertificateChainCleaner(TrustRootIndex trustRootIndex) {
      this.trustRootIndex = trustRootIndex;
   }

   public List<Certificate> clean(List<Certificate> chain) throws SSLPeerUnverifiedException {
      Deque<Certificate> queue = new ArrayDeque(chain);
      List<Certificate> result = new ArrayList();
      result.add(queue.removeFirst());
      boolean foundTrustedCertificate = false;

      for(int c = 0; c < 9; ++c) {
         X509Certificate toVerify = (X509Certificate)result.get(result.size() - 1);
         X509Certificate trustedCert = this.trustRootIndex.findByIssuerAndSignature(toVerify);
         if (trustedCert == null) {
            Iterator i = queue.iterator();

            X509Certificate signingCert;
            do {
               if (!i.hasNext()) {
                  if (foundTrustedCertificate) {
                     return result;
                  }

                  throw new SSLPeerUnverifiedException("Failed to find a trusted cert that signed " + toVerify);
               }

               signingCert = (X509Certificate)i.next();
            } while(!this.verifySignature(toVerify, signingCert));

            i.remove();
            result.add(signingCert);
         } else {
            if (result.size() > 1 || !toVerify.equals(trustedCert)) {
               result.add(trustedCert);
            }

            if (this.verifySignature(trustedCert, trustedCert)) {
               return result;
            }

            foundTrustedCertificate = true;
         }
      }

      throw new SSLPeerUnverifiedException("Certificate chain too long: " + result);
   }

   private boolean verifySignature(X509Certificate toVerify, X509Certificate signingCert) {
      if (!toVerify.getIssuerDN().equals(signingCert.getSubjectDN())) {
         return false;
      } else {
         try {
            toVerify.verify(signingCert.getPublicKey());
            return true;
         } catch (GeneralSecurityException var4) {
            return false;
         }
      }
   }
}
