package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Challenge;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.Platform;
import com.squareup.okhttp.internal.Util;
import java.io.IOException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public final class OkHeaders {
   private static final Comparator<String> FIELD_NAME_COMPARATOR = new Comparator<String>() {
      public int compare(String a, String b) {
         if (a == b) {
            return 0;
         } else if (a == null) {
            return -1;
         } else {
            return b == null ? 1 : String.CASE_INSENSITIVE_ORDER.compare(a, b);
         }
      }
   };
   static final String PREFIX = Platform.get().getPrefix();
   public static final String SENT_MILLIS;
   public static final String RECEIVED_MILLIS;
   public static final String SELECTED_PROTOCOL;
   public static final String RESPONSE_SOURCE;

   private OkHeaders() {
   }

   public static long contentLength(Request request) {
      return contentLength(request.headers());
   }

   public static long contentLength(Response response) {
      return contentLength(response.headers());
   }

   public static long contentLength(Headers headers) {
      return stringToLong(headers.get("Content-Length"));
   }

   private static long stringToLong(String s) {
      if (s == null) {
         return -1L;
      } else {
         try {
            return Long.parseLong(s);
         } catch (NumberFormatException var2) {
            return -1L;
         }
      }
   }

   public static Map<String, List<String>> toMultimap(Headers headers, String valueForNullKey) {
      Map<String, List<String>> result = new TreeMap(FIELD_NAME_COMPARATOR);
      int i = 0;

      for(int size = headers.size(); i < size; ++i) {
         String fieldName = headers.name(i);
         String value = headers.value(i);
         List<String> allValues = new ArrayList();
         List<String> otherValues = (List)result.get(fieldName);
         if (otherValues != null) {
            allValues.addAll(otherValues);
         }

         allValues.add(value);
         result.put(fieldName, Collections.unmodifiableList(allValues));
      }

      if (valueForNullKey != null) {
         result.put((Object)null, Collections.unmodifiableList(Collections.singletonList(valueForNullKey)));
      }

      return Collections.unmodifiableMap(result);
   }

   public static void addCookies(Request.Builder builder, Map<String, List<String>> cookieHeaders) {
      Iterator var2 = cookieHeaders.entrySet().iterator();

      while(true) {
         Entry entry;
         String key;
         do {
            if (!var2.hasNext()) {
               return;
            }

            entry = (Entry)var2.next();
            key = (String)entry.getKey();
         } while(!"Cookie".equalsIgnoreCase(key) && !"Cookie2".equalsIgnoreCase(key));

         if (!((List)entry.getValue()).isEmpty()) {
            builder.addHeader(key, buildCookieHeader((List)entry.getValue()));
         }
      }
   }

   private static String buildCookieHeader(List<String> cookies) {
      if (cookies.size() == 1) {
         return (String)cookies.get(0);
      } else {
         StringBuilder sb = new StringBuilder();
         int i = 0;

         for(int size = cookies.size(); i < size; ++i) {
            if (i > 0) {
               sb.append("; ");
            }

            sb.append((String)cookies.get(i));
         }

         return sb.toString();
      }
   }

   public static boolean varyMatches(Response cachedResponse, Headers cachedRequest, Request newRequest) {
      Iterator var3 = varyFields(cachedResponse).iterator();

      String field;
      do {
         if (!var3.hasNext()) {
            return true;
         }

         field = (String)var3.next();
      } while(Util.equal(cachedRequest.values(field), newRequest.headers(field)));

      return false;
   }

   public static boolean hasVaryAll(Response response) {
      return hasVaryAll(response.headers());
   }

   public static boolean hasVaryAll(Headers responseHeaders) {
      return varyFields(responseHeaders).contains("*");
   }

   private static Set<String> varyFields(Response response) {
      return varyFields(response.headers());
   }

   public static Set<String> varyFields(Headers responseHeaders) {
      Set<String> result = Collections.emptySet();
      int i = 0;

      for(int size = responseHeaders.size(); i < size; ++i) {
         if ("Vary".equalsIgnoreCase(responseHeaders.name(i))) {
            String value = responseHeaders.value(i);
            if (((Set)result).isEmpty()) {
               result = new TreeSet(String.CASE_INSENSITIVE_ORDER);
            }

            String[] var5 = value.split(",");
            int var6 = var5.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               String varyField = var5[var7];
               ((Set)result).add(varyField.trim());
            }
         }
      }

      return (Set)result;
   }

   public static Headers varyHeaders(Response response) {
      Headers requestHeaders = response.networkResponse().request().headers();
      Headers responseHeaders = response.headers();
      return varyHeaders(requestHeaders, responseHeaders);
   }

   public static Headers varyHeaders(Headers requestHeaders, Headers responseHeaders) {
      Set<String> varyFields = varyFields(responseHeaders);
      if (varyFields.isEmpty()) {
         return (new Headers.Builder()).build();
      } else {
         Headers.Builder result = new Headers.Builder();
         int i = 0;

         for(int size = requestHeaders.size(); i < size; ++i) {
            String fieldName = requestHeaders.name(i);
            if (varyFields.contains(fieldName)) {
               result.add(fieldName, requestHeaders.value(i));
            }
         }

         return result.build();
      }
   }

   static boolean isEndToEnd(String fieldName) {
      return !"Connection".equalsIgnoreCase(fieldName) && !"Keep-Alive".equalsIgnoreCase(fieldName) && !"Proxy-Authenticate".equalsIgnoreCase(fieldName) && !"Proxy-Authorization".equalsIgnoreCase(fieldName) && !"TE".equalsIgnoreCase(fieldName) && !"Trailers".equalsIgnoreCase(fieldName) && !"Transfer-Encoding".equalsIgnoreCase(fieldName) && !"Upgrade".equalsIgnoreCase(fieldName);
   }

   public static List<Challenge> parseChallenges(Headers responseHeaders, String challengeHeader) {
      List<Challenge> result = new ArrayList();
      int i = 0;

      for(int size = responseHeaders.size(); i < size; ++i) {
         if (challengeHeader.equalsIgnoreCase(responseHeaders.name(i))) {
            String value = responseHeaders.value(i);
            int pos = 0;

            while(pos < value.length()) {
               int tokenStart = pos;
               pos = HeaderParser.skipUntil(value, pos, " ");
               String scheme = value.substring(tokenStart, pos).trim();
               pos = HeaderParser.skipWhitespace(value, pos);
               if (!value.regionMatches(true, pos, "realm=\"", 0, "realm=\"".length())) {
                  break;
               }

               pos += "realm=\"".length();
               int realmStart = pos;
               pos = HeaderParser.skipUntil(value, pos, "\"");
               String realm = value.substring(realmStart, pos);
               ++pos;
               pos = HeaderParser.skipUntil(value, pos, ",");
               ++pos;
               pos = HeaderParser.skipWhitespace(value, pos);
               result.add(new Challenge(scheme, realm));
            }
         }
      }

      return result;
   }

   public static Request processAuthHeader(Authenticator authenticator, Response response, Proxy proxy) throws IOException {
      return response.code() == 407 ? authenticator.authenticateProxy(proxy, response) : authenticator.authenticate(proxy, response);
   }

   static {
      SENT_MILLIS = PREFIX + "-Sent-Millis";
      RECEIVED_MILLIS = PREFIX + "-Received-Millis";
      SELECTED_PROTOCOL = PREFIX + "-Selected-Protocol";
      RESPONSE_SOURCE = PREFIX + "-Response-Source";
   }
}
