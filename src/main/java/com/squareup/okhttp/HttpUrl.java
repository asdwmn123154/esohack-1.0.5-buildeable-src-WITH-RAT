package com.squareup.okhttp;

import java.net.IDN;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import okio.Buffer;

public final class HttpUrl {
   private static final char[] HEX_DIGITS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
   static final String USERNAME_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
   static final String PASSWORD_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#";
   static final String PATH_SEGMENT_ENCODE_SET = " \"<>^`{}|/\\?#";
   static final String PATH_SEGMENT_ENCODE_SET_URI = "[]";
   static final String QUERY_ENCODE_SET = " \"'<>#";
   static final String QUERY_COMPONENT_ENCODE_SET = " \"'<>#&=";
   static final String QUERY_COMPONENT_ENCODE_SET_URI = "\\^`{|}";
   static final String FORM_ENCODE_SET = " \"':;<=>@[]^`{}|/\\?#&!$(),~";
   static final String FRAGMENT_ENCODE_SET = "";
   static final String FRAGMENT_ENCODE_SET_URI = " \"#<>\\^`{|}";
   private final String scheme;
   private final String username;
   private final String password;
   private final String host;
   private final int port;
   private final List<String> pathSegments;
   private final List<String> queryNamesAndValues;
   private final String fragment;
   private final String url;

   private HttpUrl(HttpUrl.Builder builder) {
      this.scheme = builder.scheme;
      this.username = percentDecode(builder.encodedUsername, false);
      this.password = percentDecode(builder.encodedPassword, false);
      this.host = builder.host;
      this.port = builder.effectivePort();
      this.pathSegments = this.percentDecode(builder.encodedPathSegments, false);
      this.queryNamesAndValues = builder.encodedQueryNamesAndValues != null ? this.percentDecode(builder.encodedQueryNamesAndValues, true) : null;
      this.fragment = builder.encodedFragment != null ? percentDecode(builder.encodedFragment, false) : null;
      this.url = builder.toString();
   }

   public URL url() {
      try {
         return new URL(this.url);
      } catch (MalformedURLException var2) {
         throw new RuntimeException(var2);
      }
   }

   public URI uri() {
      try {
         String uri = this.newBuilder().reencodeForUri().toString();
         return new URI(uri);
      } catch (URISyntaxException var2) {
         throw new IllegalStateException("not valid as a java.net.URI: " + this.url);
      }
   }

   public String scheme() {
      return this.scheme;
   }

   public boolean isHttps() {
      return this.scheme.equals("https");
   }

   public String encodedUsername() {
      if (this.username.isEmpty()) {
         return "";
      } else {
         int usernameStart = this.scheme.length() + 3;
         int usernameEnd = delimiterOffset(this.url, usernameStart, this.url.length(), ":@");
         return this.url.substring(usernameStart, usernameEnd);
      }
   }

   public String username() {
      return this.username;
   }

   public String encodedPassword() {
      if (this.password.isEmpty()) {
         return "";
      } else {
         int passwordStart = this.url.indexOf(58, this.scheme.length() + 3) + 1;
         int passwordEnd = this.url.indexOf(64);
         return this.url.substring(passwordStart, passwordEnd);
      }
   }

   public String password() {
      return this.password;
   }

   public String host() {
      return this.host;
   }

   public int port() {
      return this.port;
   }

   public static int defaultPort(String scheme) {
      if (scheme.equals("http")) {
         return 80;
      } else {
         return scheme.equals("https") ? 443 : -1;
      }
   }

   public int pathSize() {
      return this.pathSegments.size();
   }

   public String encodedPath() {
      int pathStart = this.url.indexOf(47, this.scheme.length() + 3);
      int pathEnd = delimiterOffset(this.url, pathStart, this.url.length(), "?#");
      return this.url.substring(pathStart, pathEnd);
   }

   static void pathSegmentsToString(StringBuilder out, List<String> pathSegments) {
      int i = 0;

      for(int size = pathSegments.size(); i < size; ++i) {
         out.append('/');
         out.append((String)pathSegments.get(i));
      }

   }

   public List<String> encodedPathSegments() {
      int pathStart = this.url.indexOf(47, this.scheme.length() + 3);
      int pathEnd = delimiterOffset(this.url, pathStart, this.url.length(), "?#");
      List<String> result = new ArrayList();

      int segmentEnd;
      for(int i = pathStart; i < pathEnd; i = segmentEnd) {
         ++i;
         segmentEnd = delimiterOffset(this.url, i, pathEnd, "/");
         result.add(this.url.substring(i, segmentEnd));
      }

      return result;
   }

   public List<String> pathSegments() {
      return this.pathSegments;
   }

   public String encodedQuery() {
      if (this.queryNamesAndValues == null) {
         return null;
      } else {
         int queryStart = this.url.indexOf(63) + 1;
         int queryEnd = delimiterOffset(this.url, queryStart + 1, this.url.length(), "#");
         return this.url.substring(queryStart, queryEnd);
      }
   }

   static void namesAndValuesToQueryString(StringBuilder out, List<String> namesAndValues) {
      int i = 0;

      for(int size = namesAndValues.size(); i < size; i += 2) {
         String name = (String)namesAndValues.get(i);
         String value = (String)namesAndValues.get(i + 1);
         if (i > 0) {
            out.append('&');
         }

         out.append(name);
         if (value != null) {
            out.append('=');
            out.append(value);
         }
      }

   }

   static List<String> queryStringToNamesAndValues(String encodedQuery) {
      List<String> result = new ArrayList();

      int ampersandOffset;
      for(int pos = 0; pos <= encodedQuery.length(); pos = ampersandOffset + 1) {
         ampersandOffset = encodedQuery.indexOf(38, pos);
         if (ampersandOffset == -1) {
            ampersandOffset = encodedQuery.length();
         }

         int equalsOffset = encodedQuery.indexOf(61, pos);
         if (equalsOffset != -1 && equalsOffset <= ampersandOffset) {
            result.add(encodedQuery.substring(pos, equalsOffset));
            result.add(encodedQuery.substring(equalsOffset + 1, ampersandOffset));
         } else {
            result.add(encodedQuery.substring(pos, ampersandOffset));
            result.add((Object)null);
         }
      }

      return result;
   }

   public String query() {
      if (this.queryNamesAndValues == null) {
         return null;
      } else {
         StringBuilder result = new StringBuilder();
         namesAndValuesToQueryString(result, this.queryNamesAndValues);
         return result.toString();
      }
   }

   public int querySize() {
      return this.queryNamesAndValues != null ? this.queryNamesAndValues.size() / 2 : 0;
   }

   public String queryParameter(String name) {
      if (this.queryNamesAndValues == null) {
         return null;
      } else {
         int i = 0;

         for(int size = this.queryNamesAndValues.size(); i < size; i += 2) {
            if (name.equals(this.queryNamesAndValues.get(i))) {
               return (String)this.queryNamesAndValues.get(i + 1);
            }
         }

         return null;
      }
   }

   public Set<String> queryParameterNames() {
      if (this.queryNamesAndValues == null) {
         return Collections.emptySet();
      } else {
         Set<String> result = new LinkedHashSet();
         int i = 0;

         for(int size = this.queryNamesAndValues.size(); i < size; i += 2) {
            result.add(this.queryNamesAndValues.get(i));
         }

         return Collections.unmodifiableSet(result);
      }
   }

   public List<String> queryParameterValues(String name) {
      if (this.queryNamesAndValues == null) {
         return Collections.emptyList();
      } else {
         List<String> result = new ArrayList();
         int i = 0;

         for(int size = this.queryNamesAndValues.size(); i < size; i += 2) {
            if (name.equals(this.queryNamesAndValues.get(i))) {
               result.add(this.queryNamesAndValues.get(i + 1));
            }
         }

         return Collections.unmodifiableList(result);
      }
   }

   public String queryParameterName(int index) {
      return (String)this.queryNamesAndValues.get(index * 2);
   }

   public String queryParameterValue(int index) {
      return (String)this.queryNamesAndValues.get(index * 2 + 1);
   }

   public String encodedFragment() {
      if (this.fragment == null) {
         return null;
      } else {
         int fragmentStart = this.url.indexOf(35) + 1;
         return this.url.substring(fragmentStart);
      }
   }

   public String fragment() {
      return this.fragment;
   }

   public HttpUrl resolve(String link) {
      HttpUrl.Builder builder = new HttpUrl.Builder();
      HttpUrl.Builder.ParseResult result = builder.parse(this, link);
      return result == HttpUrl.Builder.ParseResult.SUCCESS ? builder.build() : null;
   }

   public HttpUrl.Builder newBuilder() {
      HttpUrl.Builder result = new HttpUrl.Builder();
      result.scheme = this.scheme;
      result.encodedUsername = this.encodedUsername();
      result.encodedPassword = this.encodedPassword();
      result.host = this.host;
      result.port = this.port != defaultPort(this.scheme) ? this.port : -1;
      result.encodedPathSegments.clear();
      result.encodedPathSegments.addAll(this.encodedPathSegments());
      result.encodedQuery(this.encodedQuery());
      result.encodedFragment = this.encodedFragment();
      return result;
   }

   public static HttpUrl parse(String url) {
      HttpUrl.Builder builder = new HttpUrl.Builder();
      HttpUrl.Builder.ParseResult result = builder.parse((HttpUrl)null, url);
      return result == HttpUrl.Builder.ParseResult.SUCCESS ? builder.build() : null;
   }

   public static HttpUrl get(URL url) {
      return parse(url.toString());
   }

   static HttpUrl getChecked(String url) throws MalformedURLException, UnknownHostException {
      HttpUrl.Builder builder = new HttpUrl.Builder();
      HttpUrl.Builder.ParseResult result = builder.parse((HttpUrl)null, url);
      switch(result) {
      case SUCCESS:
         return builder.build();
      case INVALID_HOST:
         throw new UnknownHostException("Invalid host: " + url);
      case UNSUPPORTED_SCHEME:
      case MISSING_SCHEME:
      case INVALID_PORT:
      default:
         throw new MalformedURLException("Invalid URL: " + result + " for " + url);
      }
   }

   public static HttpUrl get(URI uri) {
      return parse(uri.toString());
   }

   public boolean equals(Object o) {
      return o instanceof HttpUrl && ((HttpUrl)o).url.equals(this.url);
   }

   public int hashCode() {
      return this.url.hashCode();
   }

   public String toString() {
      return this.url;
   }

   private static int delimiterOffset(String input, int pos, int limit, String delimiters) {
      for(int i = pos; i < limit; ++i) {
         if (delimiters.indexOf(input.charAt(i)) != -1) {
            return i;
         }
      }

      return limit;
   }

   static String percentDecode(String encoded, boolean plusIsSpace) {
      return percentDecode(encoded, 0, encoded.length(), plusIsSpace);
   }

   private List<String> percentDecode(List<String> list, boolean plusIsSpace) {
      List<String> result = new ArrayList(list.size());
      Iterator var4 = list.iterator();

      while(var4.hasNext()) {
         String s = (String)var4.next();
         result.add(s != null ? percentDecode(s, plusIsSpace) : null);
      }

      return Collections.unmodifiableList(result);
   }

   static String percentDecode(String encoded, int pos, int limit, boolean plusIsSpace) {
      for(int i = pos; i < limit; ++i) {
         char c = encoded.charAt(i);
         if (c == '%' || c == '+' && plusIsSpace) {
            Buffer out = new Buffer();
            out.writeUtf8(encoded, pos, i);
            percentDecode(out, encoded, i, limit, plusIsSpace);
            return out.readUtf8();
         }
      }

      return encoded.substring(pos, limit);
   }

   static void percentDecode(Buffer out, String encoded, int pos, int limit, boolean plusIsSpace) {
      int codePoint;
      for(int i = pos; i < limit; i += Character.charCount(codePoint)) {
         codePoint = encoded.codePointAt(i);
         if (codePoint == 37 && i + 2 < limit) {
            int d1 = decodeHexDigit(encoded.charAt(i + 1));
            int d2 = decodeHexDigit(encoded.charAt(i + 2));
            if (d1 != -1 && d2 != -1) {
               out.writeByte((d1 << 4) + d2);
               i += 2;
               continue;
            }
         } else if (codePoint == 43 && plusIsSpace) {
            out.writeByte(32);
            continue;
         }

         out.writeUtf8CodePoint(codePoint);
      }

   }

   static int decodeHexDigit(char c) {
      if (c >= '0' && c <= '9') {
         return c - 48;
      } else if (c >= 'a' && c <= 'f') {
         return c - 97 + 10;
      } else {
         return c >= 'A' && c <= 'F' ? c - 65 + 10 : -1;
      }
   }

   static String canonicalize(String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean plusIsSpace, boolean asciiOnly) {
      int codePoint;
      for(int i = pos; i < limit; i += Character.charCount(codePoint)) {
         codePoint = input.codePointAt(i);
         if (codePoint < 32 || codePoint == 127 || codePoint >= 128 && asciiOnly || encodeSet.indexOf(codePoint) != -1 || codePoint == 37 && !alreadyEncoded || codePoint == 43 && plusIsSpace) {
            Buffer out = new Buffer();
            out.writeUtf8(input, pos, i);
            canonicalize(out, input, i, limit, encodeSet, alreadyEncoded, plusIsSpace, asciiOnly);
            return out.readUtf8();
         }
      }

      return input.substring(pos, limit);
   }

   static void canonicalize(Buffer out, String input, int pos, int limit, String encodeSet, boolean alreadyEncoded, boolean plusIsSpace, boolean asciiOnly) {
      Buffer utf8Buffer = null;

      int codePoint;
      for(int i = pos; i < limit; i += Character.charCount(codePoint)) {
         codePoint = input.codePointAt(i);
         if (!alreadyEncoded || codePoint != 9 && codePoint != 10 && codePoint != 12 && codePoint != 13) {
            if (codePoint == 43 && plusIsSpace) {
               out.writeUtf8(alreadyEncoded ? "+" : "%2B");
            } else if (codePoint >= 32 && codePoint != 127 && (codePoint < 128 || !asciiOnly) && encodeSet.indexOf(codePoint) == -1 && (codePoint != 37 || alreadyEncoded)) {
               out.writeUtf8CodePoint(codePoint);
            } else {
               if (utf8Buffer == null) {
                  utf8Buffer = new Buffer();
               }

               utf8Buffer.writeUtf8CodePoint(codePoint);

               while(!utf8Buffer.exhausted()) {
                  int b = utf8Buffer.readByte() & 255;
                  out.writeByte(37);
                  out.writeByte(HEX_DIGITS[b >> 4 & 15]);
                  out.writeByte(HEX_DIGITS[b & 15]);
               }
            }
         }
      }

   }

   static String canonicalize(String input, String encodeSet, boolean alreadyEncoded, boolean plusIsSpace, boolean asciiOnly) {
      return canonicalize(input, 0, input.length(), encodeSet, alreadyEncoded, plusIsSpace, asciiOnly);
   }

   // $FF: synthetic method
   HttpUrl(HttpUrl.Builder x0, Object x1) {
      this(x0);
   }

   public static final class Builder {
      String scheme;
      String encodedUsername = "";
      String encodedPassword = "";
      String host;
      int port = -1;
      final List<String> encodedPathSegments = new ArrayList();
      List<String> encodedQueryNamesAndValues;
      String encodedFragment;

      public Builder() {
         this.encodedPathSegments.add("");
      }

      public HttpUrl.Builder scheme(String scheme) {
         if (scheme == null) {
            throw new IllegalArgumentException("scheme == null");
         } else {
            if (scheme.equalsIgnoreCase("http")) {
               this.scheme = "http";
            } else {
               if (!scheme.equalsIgnoreCase("https")) {
                  throw new IllegalArgumentException("unexpected scheme: " + scheme);
               }

               this.scheme = "https";
            }

            return this;
         }
      }

      public HttpUrl.Builder username(String username) {
         if (username == null) {
            throw new IllegalArgumentException("username == null");
         } else {
            this.encodedUsername = HttpUrl.canonicalize(username, " \"':;<=>@[]^`{}|/\\?#", false, false, true);
            return this;
         }
      }

      public HttpUrl.Builder encodedUsername(String encodedUsername) {
         if (encodedUsername == null) {
            throw new IllegalArgumentException("encodedUsername == null");
         } else {
            this.encodedUsername = HttpUrl.canonicalize(encodedUsername, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
            return this;
         }
      }

      public HttpUrl.Builder password(String password) {
         if (password == null) {
            throw new IllegalArgumentException("password == null");
         } else {
            this.encodedPassword = HttpUrl.canonicalize(password, " \"':;<=>@[]^`{}|/\\?#", false, false, true);
            return this;
         }
      }

      public HttpUrl.Builder encodedPassword(String encodedPassword) {
         if (encodedPassword == null) {
            throw new IllegalArgumentException("encodedPassword == null");
         } else {
            this.encodedPassword = HttpUrl.canonicalize(encodedPassword, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
            return this;
         }
      }

      public HttpUrl.Builder host(String host) {
         if (host == null) {
            throw new IllegalArgumentException("host == null");
         } else {
            String encoded = canonicalizeHost(host, 0, host.length());
            if (encoded == null) {
               throw new IllegalArgumentException("unexpected host: " + host);
            } else {
               this.host = encoded;
               return this;
            }
         }
      }

      public HttpUrl.Builder port(int port) {
         if (port > 0 && port <= 65535) {
            this.port = port;
            return this;
         } else {
            throw new IllegalArgumentException("unexpected port: " + port);
         }
      }

      int effectivePort() {
         return this.port != -1 ? this.port : HttpUrl.defaultPort(this.scheme);
      }

      public HttpUrl.Builder addPathSegment(String pathSegment) {
         if (pathSegment == null) {
            throw new IllegalArgumentException("pathSegment == null");
         } else {
            this.push(pathSegment, 0, pathSegment.length(), false, false);
            return this;
         }
      }

      public HttpUrl.Builder addEncodedPathSegment(String encodedPathSegment) {
         if (encodedPathSegment == null) {
            throw new IllegalArgumentException("encodedPathSegment == null");
         } else {
            this.push(encodedPathSegment, 0, encodedPathSegment.length(), false, true);
            return this;
         }
      }

      public HttpUrl.Builder setPathSegment(int index, String pathSegment) {
         if (pathSegment == null) {
            throw new IllegalArgumentException("pathSegment == null");
         } else {
            String canonicalPathSegment = HttpUrl.canonicalize(pathSegment, 0, pathSegment.length(), " \"<>^`{}|/\\?#", false, false, true);
            if (!this.isDot(canonicalPathSegment) && !this.isDotDot(canonicalPathSegment)) {
               this.encodedPathSegments.set(index, canonicalPathSegment);
               return this;
            } else {
               throw new IllegalArgumentException("unexpected path segment: " + pathSegment);
            }
         }
      }

      public HttpUrl.Builder setEncodedPathSegment(int index, String encodedPathSegment) {
         if (encodedPathSegment == null) {
            throw new IllegalArgumentException("encodedPathSegment == null");
         } else {
            String canonicalPathSegment = HttpUrl.canonicalize(encodedPathSegment, 0, encodedPathSegment.length(), " \"<>^`{}|/\\?#", true, false, true);
            this.encodedPathSegments.set(index, canonicalPathSegment);
            if (!this.isDot(canonicalPathSegment) && !this.isDotDot(canonicalPathSegment)) {
               return this;
            } else {
               throw new IllegalArgumentException("unexpected path segment: " + encodedPathSegment);
            }
         }
      }

      public HttpUrl.Builder removePathSegment(int index) {
         this.encodedPathSegments.remove(index);
         if (this.encodedPathSegments.isEmpty()) {
            this.encodedPathSegments.add("");
         }

         return this;
      }

      public HttpUrl.Builder encodedPath(String encodedPath) {
         if (encodedPath == null) {
            throw new IllegalArgumentException("encodedPath == null");
         } else if (!encodedPath.startsWith("/")) {
            throw new IllegalArgumentException("unexpected encodedPath: " + encodedPath);
         } else {
            this.resolvePath(encodedPath, 0, encodedPath.length());
            return this;
         }
      }

      public HttpUrl.Builder query(String query) {
         this.encodedQueryNamesAndValues = query != null ? HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(query, " \"'<>#", false, true, true)) : null;
         return this;
      }

      public HttpUrl.Builder encodedQuery(String encodedQuery) {
         this.encodedQueryNamesAndValues = encodedQuery != null ? HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(encodedQuery, " \"'<>#", true, true, true)) : null;
         return this;
      }

      public HttpUrl.Builder addQueryParameter(String name, String value) {
         if (name == null) {
            throw new IllegalArgumentException("name == null");
         } else {
            if (this.encodedQueryNamesAndValues == null) {
               this.encodedQueryNamesAndValues = new ArrayList();
            }

            this.encodedQueryNamesAndValues.add(HttpUrl.canonicalize(name, " \"'<>#&=", false, true, true));
            this.encodedQueryNamesAndValues.add(value != null ? HttpUrl.canonicalize(value, " \"'<>#&=", false, true, true) : null);
            return this;
         }
      }

      public HttpUrl.Builder addEncodedQueryParameter(String encodedName, String encodedValue) {
         if (encodedName == null) {
            throw new IllegalArgumentException("encodedName == null");
         } else {
            if (this.encodedQueryNamesAndValues == null) {
               this.encodedQueryNamesAndValues = new ArrayList();
            }

            this.encodedQueryNamesAndValues.add(HttpUrl.canonicalize(encodedName, " \"'<>#&=", true, true, true));
            this.encodedQueryNamesAndValues.add(encodedValue != null ? HttpUrl.canonicalize(encodedValue, " \"'<>#&=", true, true, true) : null);
            return this;
         }
      }

      public HttpUrl.Builder setQueryParameter(String name, String value) {
         this.removeAllQueryParameters(name);
         this.addQueryParameter(name, value);
         return this;
      }

      public HttpUrl.Builder setEncodedQueryParameter(String encodedName, String encodedValue) {
         this.removeAllEncodedQueryParameters(encodedName);
         this.addEncodedQueryParameter(encodedName, encodedValue);
         return this;
      }

      public HttpUrl.Builder removeAllQueryParameters(String name) {
         if (name == null) {
            throw new IllegalArgumentException("name == null");
         } else if (this.encodedQueryNamesAndValues == null) {
            return this;
         } else {
            String nameToRemove = HttpUrl.canonicalize(name, " \"'<>#&=", false, true, true);
            this.removeAllCanonicalQueryParameters(nameToRemove);
            return this;
         }
      }

      public HttpUrl.Builder removeAllEncodedQueryParameters(String encodedName) {
         if (encodedName == null) {
            throw new IllegalArgumentException("encodedName == null");
         } else if (this.encodedQueryNamesAndValues == null) {
            return this;
         } else {
            this.removeAllCanonicalQueryParameters(HttpUrl.canonicalize(encodedName, " \"'<>#&=", true, true, true));
            return this;
         }
      }

      private void removeAllCanonicalQueryParameters(String canonicalName) {
         for(int i = this.encodedQueryNamesAndValues.size() - 2; i >= 0; i -= 2) {
            if (canonicalName.equals(this.encodedQueryNamesAndValues.get(i))) {
               this.encodedQueryNamesAndValues.remove(i + 1);
               this.encodedQueryNamesAndValues.remove(i);
               if (this.encodedQueryNamesAndValues.isEmpty()) {
                  this.encodedQueryNamesAndValues = null;
                  return;
               }
            }
         }

      }

      public HttpUrl.Builder fragment(String fragment) {
         this.encodedFragment = fragment != null ? HttpUrl.canonicalize(fragment, "", false, false, false) : null;
         return this;
      }

      public HttpUrl.Builder encodedFragment(String encodedFragment) {
         this.encodedFragment = encodedFragment != null ? HttpUrl.canonicalize(encodedFragment, "", true, false, false) : null;
         return this;
      }

      HttpUrl.Builder reencodeForUri() {
         int i = 0;

         int size;
         String component;
         for(size = this.encodedPathSegments.size(); i < size; ++i) {
            component = (String)this.encodedPathSegments.get(i);
            this.encodedPathSegments.set(i, HttpUrl.canonicalize(component, "[]", true, false, true));
         }

         if (this.encodedQueryNamesAndValues != null) {
            i = 0;

            for(size = this.encodedQueryNamesAndValues.size(); i < size; ++i) {
               component = (String)this.encodedQueryNamesAndValues.get(i);
               if (component != null) {
                  this.encodedQueryNamesAndValues.set(i, HttpUrl.canonicalize(component, "\\^`{|}", true, true, true));
               }
            }
         }

         if (this.encodedFragment != null) {
            this.encodedFragment = HttpUrl.canonicalize(this.encodedFragment, " \"#<>\\^`{|}", true, false, false);
         }

         return this;
      }

      public HttpUrl build() {
         if (this.scheme == null) {
            throw new IllegalStateException("scheme == null");
         } else if (this.host == null) {
            throw new IllegalStateException("host == null");
         } else {
            return new HttpUrl(this);
         }
      }

      public String toString() {
         StringBuilder result = new StringBuilder();
         result.append(this.scheme);
         result.append("://");
         if (!this.encodedUsername.isEmpty() || !this.encodedPassword.isEmpty()) {
            result.append(this.encodedUsername);
            if (!this.encodedPassword.isEmpty()) {
               result.append(':');
               result.append(this.encodedPassword);
            }

            result.append('@');
         }

         if (this.host.indexOf(58) != -1) {
            result.append('[');
            result.append(this.host);
            result.append(']');
         } else {
            result.append(this.host);
         }

         int effectivePort = this.effectivePort();
         if (effectivePort != HttpUrl.defaultPort(this.scheme)) {
            result.append(':');
            result.append(effectivePort);
         }

         HttpUrl.pathSegmentsToString(result, this.encodedPathSegments);
         if (this.encodedQueryNamesAndValues != null) {
            result.append('?');
            HttpUrl.namesAndValuesToQueryString(result, this.encodedQueryNamesAndValues);
         }

         if (this.encodedFragment != null) {
            result.append('#');
            result.append(this.encodedFragment);
         }

         return result.toString();
      }

      HttpUrl.Builder.ParseResult parse(HttpUrl base, String input) {
         int pos = this.skipLeadingAsciiWhitespace(input, 0, input.length());
         int limit = this.skipTrailingAsciiWhitespace(input, pos, input.length());
         int schemeDelimiterOffset = schemeDelimiterOffset(input, pos, limit);
         if (schemeDelimiterOffset != -1) {
            if (input.regionMatches(true, pos, "https:", 0, 6)) {
               this.scheme = "https";
               pos += "https:".length();
            } else {
               if (!input.regionMatches(true, pos, "http:", 0, 5)) {
                  return HttpUrl.Builder.ParseResult.UNSUPPORTED_SCHEME;
               }

               this.scheme = "http";
               pos += "http:".length();
            }
         } else {
            if (base == null) {
               return HttpUrl.Builder.ParseResult.MISSING_SCHEME;
            }

            this.scheme = base.scheme;
         }

         boolean hasUsername = false;
         boolean hasPassword = false;
         int slashCount = slashCount(input, pos, limit);
         int componentDelimiterOffset;
         int queryDelimiterOffset;
         if (slashCount < 2 && base != null && base.scheme.equals(this.scheme)) {
            this.encodedUsername = base.encodedUsername();
            this.encodedPassword = base.encodedPassword();
            this.host = base.host;
            this.port = base.port;
            this.encodedPathSegments.clear();
            this.encodedPathSegments.addAll(base.encodedPathSegments());
            if (pos == limit || input.charAt(pos) == '#') {
               this.encodedQuery(base.encodedQuery());
            }
         } else {
            pos += slashCount;

            label79:
            while(true) {
               componentDelimiterOffset = HttpUrl.delimiterOffset(input, pos, limit, "@/\\?#");
               queryDelimiterOffset = componentDelimiterOffset != limit ? input.charAt(componentDelimiterOffset) : -1;
               int passwordColonOffset;
               switch(queryDelimiterOffset) {
               case -1:
               case 35:
               case 47:
               case 63:
               case 92:
                  passwordColonOffset = portColonOffset(input, pos, componentDelimiterOffset);
                  if (passwordColonOffset + 1 < componentDelimiterOffset) {
                     this.host = canonicalizeHost(input, pos, passwordColonOffset);
                     this.port = parsePort(input, passwordColonOffset + 1, componentDelimiterOffset);
                     if (this.port == -1) {
                        return HttpUrl.Builder.ParseResult.INVALID_PORT;
                     }
                  } else {
                     this.host = canonicalizeHost(input, pos, passwordColonOffset);
                     this.port = HttpUrl.defaultPort(this.scheme);
                  }

                  if (this.host == null) {
                     return HttpUrl.Builder.ParseResult.INVALID_HOST;
                  }

                  pos = componentDelimiterOffset;
                  break label79;
               case 64:
                  if (!hasPassword) {
                     passwordColonOffset = HttpUrl.delimiterOffset(input, pos, componentDelimiterOffset, ":");
                     String canonicalUsername = HttpUrl.canonicalize(input, pos, passwordColonOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
                     this.encodedUsername = hasUsername ? this.encodedUsername + "%40" + canonicalUsername : canonicalUsername;
                     if (passwordColonOffset != componentDelimiterOffset) {
                        hasPassword = true;
                        this.encodedPassword = HttpUrl.canonicalize(input, passwordColonOffset + 1, componentDelimiterOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
                     }

                     hasUsername = true;
                  } else {
                     this.encodedPassword = this.encodedPassword + "%40" + HttpUrl.canonicalize(input, pos, componentDelimiterOffset, " \"':;<=>@[]^`{}|/\\?#", true, false, true);
                  }

                  pos = componentDelimiterOffset + 1;
               }
            }
         }

         componentDelimiterOffset = HttpUrl.delimiterOffset(input, pos, limit, "?#");
         this.resolvePath(input, pos, componentDelimiterOffset);
         pos = componentDelimiterOffset;
         if (componentDelimiterOffset < limit && input.charAt(componentDelimiterOffset) == '?') {
            queryDelimiterOffset = HttpUrl.delimiterOffset(input, componentDelimiterOffset, limit, "#");
            this.encodedQueryNamesAndValues = HttpUrl.queryStringToNamesAndValues(HttpUrl.canonicalize(input, componentDelimiterOffset + 1, queryDelimiterOffset, " \"'<>#", true, true, true));
            pos = queryDelimiterOffset;
         }

         if (pos < limit && input.charAt(pos) == '#') {
            this.encodedFragment = HttpUrl.canonicalize(input, pos + 1, limit, "", true, false, false);
         }

         return HttpUrl.Builder.ParseResult.SUCCESS;
      }

      private void resolvePath(String input, int pos, int limit) {
         if (pos != limit) {
            char c = input.charAt(pos);
            if (c != '/' && c != '\\') {
               this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, "");
            } else {
               this.encodedPathSegments.clear();
               this.encodedPathSegments.add("");
               ++pos;
            }

            int i = pos;

            while(i < limit) {
               int pathSegmentDelimiterOffset = HttpUrl.delimiterOffset(input, i, limit, "/\\");
               boolean segmentHasTrailingSlash = pathSegmentDelimiterOffset < limit;
               this.push(input, i, pathSegmentDelimiterOffset, segmentHasTrailingSlash, true);
               i = pathSegmentDelimiterOffset;
               if (segmentHasTrailingSlash) {
                  i = pathSegmentDelimiterOffset + 1;
               }
            }

         }
      }

      private void push(String input, int pos, int limit, boolean addTrailingSlash, boolean alreadyEncoded) {
         String segment = HttpUrl.canonicalize(input, pos, limit, " \"<>^`{}|/\\?#", alreadyEncoded, false, true);
         if (!this.isDot(segment)) {
            if (this.isDotDot(segment)) {
               this.pop();
            } else {
               if (((String)this.encodedPathSegments.get(this.encodedPathSegments.size() - 1)).isEmpty()) {
                  this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, segment);
               } else {
                  this.encodedPathSegments.add(segment);
               }

               if (addTrailingSlash) {
                  this.encodedPathSegments.add("");
               }

            }
         }
      }

      private boolean isDot(String input) {
         return input.equals(".") || input.equalsIgnoreCase("%2e");
      }

      private boolean isDotDot(String input) {
         return input.equals("..") || input.equalsIgnoreCase("%2e.") || input.equalsIgnoreCase(".%2e") || input.equalsIgnoreCase("%2e%2e");
      }

      private void pop() {
         String removed = (String)this.encodedPathSegments.remove(this.encodedPathSegments.size() - 1);
         if (removed.isEmpty() && !this.encodedPathSegments.isEmpty()) {
            this.encodedPathSegments.set(this.encodedPathSegments.size() - 1, "");
         } else {
            this.encodedPathSegments.add("");
         }

      }

      private int skipLeadingAsciiWhitespace(String input, int pos, int limit) {
         int i = pos;

         while(i < limit) {
            switch(input.charAt(i)) {
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case ' ':
               ++i;
               break;
            default:
               return i;
            }
         }

         return limit;
      }

      private int skipTrailingAsciiWhitespace(String input, int pos, int limit) {
         int i = limit - 1;

         while(i >= pos) {
            switch(input.charAt(i)) {
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case ' ':
               --i;
               break;
            default:
               return i + 1;
            }
         }

         return pos;
      }

      private static int schemeDelimiterOffset(String input, int pos, int limit) {
         if (limit - pos < 2) {
            return -1;
         } else {
            char c0 = input.charAt(pos);
            if ((c0 < 'a' || c0 > 'z') && (c0 < 'A' || c0 > 'Z')) {
               return -1;
            } else {
               for(int i = pos + 1; i < limit; ++i) {
                  char c = input.charAt(i);
                  if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && c != '+' && c != '-' && c != '.') {
                     if (c == ':') {
                        return i;
                     }

                     return -1;
                  }
               }

               return -1;
            }
         }
      }

      private static int slashCount(String input, int pos, int limit) {
         int slashCount;
         for(slashCount = 0; pos < limit; ++pos) {
            char c = input.charAt(pos);
            if (c != '\\' && c != '/') {
               break;
            }

            ++slashCount;
         }

         return slashCount;
      }

      private static int portColonOffset(String input, int pos, int limit) {
         for(int i = pos; i < limit; ++i) {
            switch(input.charAt(i)) {
            case ':':
               return i;
            case '[':
               do {
                  ++i;
               } while(i < limit && input.charAt(i) != ']');
            }
         }

         return limit;
      }

      private static String canonicalizeHost(String input, int pos, int limit) {
         String percentDecoded = HttpUrl.percentDecode(input, pos, limit, false);
         if (percentDecoded.startsWith("[") && percentDecoded.endsWith("]")) {
            InetAddress inetAddress = decodeIpv6(percentDecoded, 1, percentDecoded.length() - 1);
            if (inetAddress == null) {
               return null;
            } else {
               byte[] address = inetAddress.getAddress();
               if (address.length == 16) {
                  return inet6AddressToAscii(address);
               } else {
                  throw new AssertionError();
               }
            }
         } else {
            return domainToAscii(percentDecoded);
         }
      }

      private static InetAddress decodeIpv6(String input, int pos, int limit) {
         byte[] address = new byte[16];
         int b = 0;
         int compress = -1;
         int groupOffset = -1;

         int value;
         for(int i = pos; i < limit; address[b++] = (byte)(value & 255)) {
            if (b == address.length) {
               return null;
            }

            if (i + 2 <= limit && input.regionMatches(i, "::", 0, 2)) {
               if (compress != -1) {
                  return null;
               }

               i += 2;
               b += 2;
               compress = b;
               if (i == limit) {
                  break;
               }
            } else if (b != 0) {
               if (!input.regionMatches(i, ":", 0, 1)) {
                  if (!input.regionMatches(i, ".", 0, 1)) {
                     return null;
                  }

                  if (!decodeIpv4Suffix(input, groupOffset, limit, address, b - 2)) {
                     return null;
                  }

                  b += 2;
                  break;
               }

               ++i;
            }

            value = 0;

            for(groupOffset = i; i < limit; ++i) {
               char c = input.charAt(i);
               int hexDigit = HttpUrl.decodeHexDigit(c);
               if (hexDigit == -1) {
                  break;
               }

               value = (value << 4) + hexDigit;
            }

            int groupLength = i - groupOffset;
            if (groupLength == 0 || groupLength > 4) {
               return null;
            }

            address[b++] = (byte)(value >>> 8 & 255);
         }

         if (b != address.length) {
            if (compress == -1) {
               return null;
            }

            System.arraycopy(address, compress, address, address.length - (b - compress), b - compress);
            Arrays.fill(address, compress, compress + (address.length - b), (byte)0);
         }

         try {
            return InetAddress.getByAddress(address);
         } catch (UnknownHostException var11) {
            throw new AssertionError();
         }
      }

      private static boolean decodeIpv4Suffix(String input, int pos, int limit, byte[] address, int addressOffset) {
         int b = addressOffset;

         int value;
         for(int i = pos; i < limit; address[b++] = (byte)value) {
            if (b == address.length) {
               return false;
            }

            if (b != addressOffset) {
               if (input.charAt(i) != '.') {
                  return false;
               }

               ++i;
            }

            value = 0;

            int groupOffset;
            for(groupOffset = i; i < limit; ++i) {
               char c = input.charAt(i);
               if (c < '0' || c > '9') {
                  break;
               }

               if (value == 0 && groupOffset != i) {
                  return false;
               }

               value = value * 10 + c - 48;
               if (value > 255) {
                  return false;
               }
            }

            int groupLength = i - groupOffset;
            if (groupLength == 0) {
               return false;
            }
         }

         if (b != addressOffset + 4) {
            return false;
         } else {
            return true;
         }
      }

      private static String domainToAscii(String input) {
         try {
            String result = IDN.toASCII(input).toLowerCase(Locale.US);
            if (result.isEmpty()) {
               return null;
            } else {
               return containsInvalidHostnameAsciiCodes(result) ? null : result;
            }
         } catch (IllegalArgumentException var2) {
            return null;
         }
      }

      private static boolean containsInvalidHostnameAsciiCodes(String hostnameAscii) {
         for(int i = 0; i < hostnameAscii.length(); ++i) {
            char c = hostnameAscii.charAt(i);
            if (c <= 31 || c >= 127) {
               return true;
            }

            if (" #%/:?@[\\]".indexOf(c) != -1) {
               return true;
            }
         }

         return false;
      }

      private static String inet6AddressToAscii(byte[] address) {
         int longestRunOffset = -1;
         int longestRunLength = 0;

         int i;
         int group;
         for(int i = 0; i < address.length; i += 2) {
            for(i = i; i < 16 && address[i] == 0 && address[i + 1] == 0; i += 2) {
            }

            group = i - i;
            if (group > longestRunLength) {
               longestRunOffset = i;
               longestRunLength = group;
            }
         }

         Buffer result = new Buffer();
         i = 0;

         while(i < address.length) {
            if (i == longestRunOffset) {
               result.writeByte(58);
               i += longestRunLength;
               if (i == 16) {
                  result.writeByte(58);
               }
            } else {
               if (i > 0) {
                  result.writeByte(58);
               }

               group = (address[i] & 255) << 8 | address[i + 1] & 255;
               result.writeHexadecimalUnsignedLong((long)group);
               i += 2;
            }
         }

         return result.readUtf8();
      }

      private static int parsePort(String input, int pos, int limit) {
         try {
            String portString = HttpUrl.canonicalize(input, pos, limit, "", false, false, true);
            int i = Integer.parseInt(portString);
            return i > 0 && i <= 65535 ? i : -1;
         } catch (NumberFormatException var5) {
            return -1;
         }
      }

      static enum ParseResult {
         SUCCESS,
         MISSING_SCHEME,
         UNSUPPORTED_SCHEME,
         INVALID_PORT,
         INVALID_HOST;
      }
   }
}
