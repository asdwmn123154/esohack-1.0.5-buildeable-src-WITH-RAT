package com.squareup.okhttp;

import com.squareup.okhttp.internal.Util;

public final class Challenge {
   private final String scheme;
   private final String realm;

   public Challenge(String scheme, String realm) {
      this.scheme = scheme;
      this.realm = realm;
   }

   public String getScheme() {
      return this.scheme;
   }

   public String getRealm() {
      return this.realm;
   }

   public boolean equals(Object o) {
      return o instanceof Challenge && Util.equal(this.scheme, ((Challenge)o).scheme) && Util.equal(this.realm, ((Challenge)o).realm);
   }

   public int hashCode() {
      int result = 29;
      int result = 31 * result + (this.realm != null ? this.realm.hashCode() : 0);
      result = 31 * result + (this.scheme != null ? this.scheme.hashCode() : 0);
      return result;
   }

   public String toString() {
      return this.scheme + " realm=\"" + this.realm + "\"";
   }
}
