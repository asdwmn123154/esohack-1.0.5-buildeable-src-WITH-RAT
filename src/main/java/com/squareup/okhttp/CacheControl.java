package com.squareup.okhttp;

import com.squareup.okhttp.internal.http.HeaderParser;
import java.util.concurrent.TimeUnit;

public final class CacheControl {
   public static final CacheControl FORCE_NETWORK = (new CacheControl.Builder()).noCache().build();
   public static final CacheControl FORCE_CACHE;
   private final boolean noCache;
   private final boolean noStore;
   private final int maxAgeSeconds;
   private final int sMaxAgeSeconds;
   private final boolean isPrivate;
   private final boolean isPublic;
   private final boolean mustRevalidate;
   private final int maxStaleSeconds;
   private final int minFreshSeconds;
   private final boolean onlyIfCached;
   private final boolean noTransform;
   String headerValue;

   private CacheControl(boolean noCache, boolean noStore, int maxAgeSeconds, int sMaxAgeSeconds, boolean isPrivate, boolean isPublic, boolean mustRevalidate, int maxStaleSeconds, int minFreshSeconds, boolean onlyIfCached, boolean noTransform, String headerValue) {
      this.noCache = noCache;
      this.noStore = noStore;
      this.maxAgeSeconds = maxAgeSeconds;
      this.sMaxAgeSeconds = sMaxAgeSeconds;
      this.isPrivate = isPrivate;
      this.isPublic = isPublic;
      this.mustRevalidate = mustRevalidate;
      this.maxStaleSeconds = maxStaleSeconds;
      this.minFreshSeconds = minFreshSeconds;
      this.onlyIfCached = onlyIfCached;
      this.noTransform = noTransform;
      this.headerValue = headerValue;
   }

   private CacheControl(CacheControl.Builder builder) {
      this.noCache = builder.noCache;
      this.noStore = builder.noStore;
      this.maxAgeSeconds = builder.maxAgeSeconds;
      this.sMaxAgeSeconds = -1;
      this.isPrivate = false;
      this.isPublic = false;
      this.mustRevalidate = false;
      this.maxStaleSeconds = builder.maxStaleSeconds;
      this.minFreshSeconds = builder.minFreshSeconds;
      this.onlyIfCached = builder.onlyIfCached;
      this.noTransform = builder.noTransform;
   }

   public boolean noCache() {
      return this.noCache;
   }

   public boolean noStore() {
      return this.noStore;
   }

   public int maxAgeSeconds() {
      return this.maxAgeSeconds;
   }

   public int sMaxAgeSeconds() {
      return this.sMaxAgeSeconds;
   }

   public boolean isPrivate() {
      return this.isPrivate;
   }

   public boolean isPublic() {
      return this.isPublic;
   }

   public boolean mustRevalidate() {
      return this.mustRevalidate;
   }

   public int maxStaleSeconds() {
      return this.maxStaleSeconds;
   }

   public int minFreshSeconds() {
      return this.minFreshSeconds;
   }

   public boolean onlyIfCached() {
      return this.onlyIfCached;
   }

   public boolean noTransform() {
      return this.noTransform;
   }

   public static CacheControl parse(Headers headers) {
      boolean noCache = false;
      boolean noStore = false;
      int maxAgeSeconds = -1;
      int sMaxAgeSeconds = -1;
      boolean isPrivate = false;
      boolean isPublic = false;
      boolean mustRevalidate = false;
      int maxStaleSeconds = -1;
      int minFreshSeconds = -1;
      boolean onlyIfCached = false;
      boolean noTransform = false;
      boolean canUseHeaderValue = true;
      String headerValue = null;
      int i = 0;

      for(int size = headers.size(); i < size; ++i) {
         String name = headers.name(i);
         String value = headers.value(i);
         if (name.equalsIgnoreCase("Cache-Control")) {
            if (headerValue != null) {
               canUseHeaderValue = false;
            } else {
               headerValue = value;
            }
         } else {
            if (!name.equalsIgnoreCase("Pragma")) {
               continue;
            }

            canUseHeaderValue = false;
         }

         int pos = 0;

         while(pos < value.length()) {
            int tokenStart = pos;
            pos = HeaderParser.skipUntil(value, pos, "=,;");
            String directive = value.substring(tokenStart, pos).trim();
            String parameter;
            if (pos != value.length() && value.charAt(pos) != ',' && value.charAt(pos) != ';') {
               ++pos;
               pos = HeaderParser.skipWhitespace(value, pos);
               int parameterStart;
               if (pos < value.length() && value.charAt(pos) == '"') {
                  ++pos;
                  parameterStart = pos;
                  pos = HeaderParser.skipUntil(value, pos, "\"");
                  parameter = value.substring(parameterStart, pos);
                  ++pos;
               } else {
                  parameterStart = pos;
                  pos = HeaderParser.skipUntil(value, pos, ",;");
                  parameter = value.substring(parameterStart, pos).trim();
               }
            } else {
               ++pos;
               parameter = null;
            }

            if ("no-cache".equalsIgnoreCase(directive)) {
               noCache = true;
            } else if ("no-store".equalsIgnoreCase(directive)) {
               noStore = true;
            } else if ("max-age".equalsIgnoreCase(directive)) {
               maxAgeSeconds = HeaderParser.parseSeconds(parameter, -1);
            } else if ("s-maxage".equalsIgnoreCase(directive)) {
               sMaxAgeSeconds = HeaderParser.parseSeconds(parameter, -1);
            } else if ("private".equalsIgnoreCase(directive)) {
               isPrivate = true;
            } else if ("public".equalsIgnoreCase(directive)) {
               isPublic = true;
            } else if ("must-revalidate".equalsIgnoreCase(directive)) {
               mustRevalidate = true;
            } else if ("max-stale".equalsIgnoreCase(directive)) {
               maxStaleSeconds = HeaderParser.parseSeconds(parameter, Integer.MAX_VALUE);
            } else if ("min-fresh".equalsIgnoreCase(directive)) {
               minFreshSeconds = HeaderParser.parseSeconds(parameter, -1);
            } else if ("only-if-cached".equalsIgnoreCase(directive)) {
               onlyIfCached = true;
            } else if ("no-transform".equalsIgnoreCase(directive)) {
               noTransform = true;
            }
         }
      }

      if (!canUseHeaderValue) {
         headerValue = null;
      }

      return new CacheControl(noCache, noStore, maxAgeSeconds, sMaxAgeSeconds, isPrivate, isPublic, mustRevalidate, maxStaleSeconds, minFreshSeconds, onlyIfCached, noTransform, headerValue);
   }

   public String toString() {
      String result = this.headerValue;
      return result != null ? result : (this.headerValue = this.headerValue());
   }

   private String headerValue() {
      StringBuilder result = new StringBuilder();
      if (this.noCache) {
         result.append("no-cache, ");
      }

      if (this.noStore) {
         result.append("no-store, ");
      }

      if (this.maxAgeSeconds != -1) {
         result.append("max-age=").append(this.maxAgeSeconds).append(", ");
      }

      if (this.sMaxAgeSeconds != -1) {
         result.append("s-maxage=").append(this.sMaxAgeSeconds).append(", ");
      }

      if (this.isPrivate) {
         result.append("private, ");
      }

      if (this.isPublic) {
         result.append("public, ");
      }

      if (this.mustRevalidate) {
         result.append("must-revalidate, ");
      }

      if (this.maxStaleSeconds != -1) {
         result.append("max-stale=").append(this.maxStaleSeconds).append(", ");
      }

      if (this.minFreshSeconds != -1) {
         result.append("min-fresh=").append(this.minFreshSeconds).append(", ");
      }

      if (this.onlyIfCached) {
         result.append("only-if-cached, ");
      }

      if (this.noTransform) {
         result.append("no-transform, ");
      }

      if (result.length() == 0) {
         return "";
      } else {
         result.delete(result.length() - 2, result.length());
         return result.toString();
      }
   }

   // $FF: synthetic method
   CacheControl(CacheControl.Builder x0, Object x1) {
      this(x0);
   }

   static {
      FORCE_CACHE = (new CacheControl.Builder()).onlyIfCached().maxStale(Integer.MAX_VALUE, TimeUnit.SECONDS).build();
   }

   public static final class Builder {
      boolean noCache;
      boolean noStore;
      int maxAgeSeconds = -1;
      int maxStaleSeconds = -1;
      int minFreshSeconds = -1;
      boolean onlyIfCached;
      boolean noTransform;

      public CacheControl.Builder noCache() {
         this.noCache = true;
         return this;
      }

      public CacheControl.Builder noStore() {
         this.noStore = true;
         return this;
      }

      public CacheControl.Builder maxAge(int maxAge, TimeUnit timeUnit) {
         if (maxAge < 0) {
            throw new IllegalArgumentException("maxAge < 0: " + maxAge);
         } else {
            long maxAgeSecondsLong = timeUnit.toSeconds((long)maxAge);
            this.maxAgeSeconds = maxAgeSecondsLong > 2147483647L ? Integer.MAX_VALUE : (int)maxAgeSecondsLong;
            return this;
         }
      }

      public CacheControl.Builder maxStale(int maxStale, TimeUnit timeUnit) {
         if (maxStale < 0) {
            throw new IllegalArgumentException("maxStale < 0: " + maxStale);
         } else {
            long maxStaleSecondsLong = timeUnit.toSeconds((long)maxStale);
            this.maxStaleSeconds = maxStaleSecondsLong > 2147483647L ? Integer.MAX_VALUE : (int)maxStaleSecondsLong;
            return this;
         }
      }

      public CacheControl.Builder minFresh(int minFresh, TimeUnit timeUnit) {
         if (minFresh < 0) {
            throw new IllegalArgumentException("minFresh < 0: " + minFresh);
         } else {
            long minFreshSecondsLong = timeUnit.toSeconds((long)minFresh);
            this.minFreshSeconds = minFreshSecondsLong > 2147483647L ? Integer.MAX_VALUE : (int)minFreshSecondsLong;
            return this;
         }
      }

      public CacheControl.Builder onlyIfCached() {
         this.onlyIfCached = true;
         return this;
      }

      public CacheControl.Builder noTransform() {
         this.noTransform = true;
         return this;
      }

      public CacheControl build() {
         return new CacheControl(this);
      }
   }
}
