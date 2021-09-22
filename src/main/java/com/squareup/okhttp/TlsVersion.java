package com.squareup.okhttp;

public enum TlsVersion {
   TLS_1_2("TLSv1.2"),
   TLS_1_1("TLSv1.1"),
   TLS_1_0("TLSv1"),
   SSL_3_0("SSLv3");

   final String javaName;

   private TlsVersion(String javaName) {
      this.javaName = javaName;
   }

   public static TlsVersion forJavaName(String javaName) {
      byte var2 = -1;
      switch(javaName.hashCode()) {
      case -503070503:
         if (javaName.equals("TLSv1.1")) {
            var2 = 1;
         }
         break;
      case -503070502:
         if (javaName.equals("TLSv1.2")) {
            var2 = 0;
         }
         break;
      case 79201641:
         if (javaName.equals("SSLv3")) {
            var2 = 3;
         }
         break;
      case 79923350:
         if (javaName.equals("TLSv1")) {
            var2 = 2;
         }
      }

      switch(var2) {
      case 0:
         return TLS_1_2;
      case 1:
         return TLS_1_1;
      case 2:
         return TLS_1_0;
      case 3:
         return SSL_3_0;
      default:
         throw new IllegalArgumentException("Unexpected TLS version: " + javaName);
      }
   }

   public String javaName() {
      return this.javaName;
   }
}
