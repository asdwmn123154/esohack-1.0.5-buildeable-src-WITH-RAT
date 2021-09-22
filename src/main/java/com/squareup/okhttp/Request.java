package com.squareup.okhttp;

import com.squareup.okhttp.internal.http.HttpMethod;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

public final class Request {
   private final HttpUrl url;
   private final String method;
   private final Headers headers;
   private final RequestBody body;
   private final Object tag;
   private volatile URL javaNetUrl;
   private volatile URI javaNetUri;
   private volatile CacheControl cacheControl;

   private Request(Request.Builder builder) {
      this.url = builder.url;
      this.method = builder.method;
      this.headers = builder.headers.build();
      this.body = builder.body;
      this.tag = builder.tag != null ? builder.tag : this;
   }

   public HttpUrl httpUrl() {
      return this.url;
   }

   public URL url() {
      URL result = this.javaNetUrl;
      return result != null ? result : (this.javaNetUrl = this.url.url());
   }

   public URI uri() throws IOException {
      try {
         URI result = this.javaNetUri;
         return result != null ? result : (this.javaNetUri = this.url.uri());
      } catch (IllegalStateException var2) {
         throw new IOException(var2.getMessage());
      }
   }

   public String urlString() {
      return this.url.toString();
   }

   public String method() {
      return this.method;
   }

   public Headers headers() {
      return this.headers;
   }

   public String header(String name) {
      return this.headers.get(name);
   }

   public List<String> headers(String name) {
      return this.headers.values(name);
   }

   public RequestBody body() {
      return this.body;
   }

   public Object tag() {
      return this.tag;
   }

   public Request.Builder newBuilder() {
      return new Request.Builder(this);
   }

   public CacheControl cacheControl() {
      CacheControl result = this.cacheControl;
      return result != null ? result : (this.cacheControl = CacheControl.parse(this.headers));
   }

   public boolean isHttps() {
      return this.url.isHttps();
   }

   public String toString() {
      return "Request{method=" + this.method + ", url=" + this.url + ", tag=" + (this.tag != this ? this.tag : null) + '}';
   }

   // $FF: synthetic method
   Request(Request.Builder x0, Object x1) {
      this(x0);
   }

   public static class Builder {
      private HttpUrl url;
      private String method;
      private Headers.Builder headers;
      private RequestBody body;
      private Object tag;

      public Builder() {
         this.method = "GET";
         this.headers = new Headers.Builder();
      }

      private Builder(Request request) {
         this.url = request.url;
         this.method = request.method;
         this.body = request.body;
         this.tag = request.tag;
         this.headers = request.headers.newBuilder();
      }

      public Request.Builder url(HttpUrl url) {
         if (url == null) {
            throw new IllegalArgumentException("url == null");
         } else {
            this.url = url;
            return this;
         }
      }

      public Request.Builder url(String url) {
         if (url == null) {
            throw new IllegalArgumentException("url == null");
         } else {
            if (url.regionMatches(true, 0, "ws:", 0, 3)) {
               url = "http:" + url.substring(3);
            } else if (url.regionMatches(true, 0, "wss:", 0, 4)) {
               url = "https:" + url.substring(4);
            }

            HttpUrl parsed = HttpUrl.parse(url);
            if (parsed == null) {
               throw new IllegalArgumentException("unexpected url: " + url);
            } else {
               return this.url(parsed);
            }
         }
      }

      public Request.Builder url(URL url) {
         if (url == null) {
            throw new IllegalArgumentException("url == null");
         } else {
            HttpUrl parsed = HttpUrl.get(url);
            if (parsed == null) {
               throw new IllegalArgumentException("unexpected url: " + url);
            } else {
               return this.url(parsed);
            }
         }
      }

      public Request.Builder header(String name, String value) {
         this.headers.set(name, value);
         return this;
      }

      public Request.Builder addHeader(String name, String value) {
         this.headers.add(name, value);
         return this;
      }

      public Request.Builder removeHeader(String name) {
         this.headers.removeAll(name);
         return this;
      }

      public Request.Builder headers(Headers headers) {
         this.headers = headers.newBuilder();
         return this;
      }

      public Request.Builder cacheControl(CacheControl cacheControl) {
         String value = cacheControl.toString();
         return value.isEmpty() ? this.removeHeader("Cache-Control") : this.header("Cache-Control", value);
      }

      public Request.Builder get() {
         return this.method("GET", (RequestBody)null);
      }

      public Request.Builder head() {
         return this.method("HEAD", (RequestBody)null);
      }

      public Request.Builder post(RequestBody body) {
         return this.method("POST", body);
      }

      public Request.Builder delete(RequestBody body) {
         return this.method("DELETE", body);
      }

      public Request.Builder delete() {
         return this.delete(RequestBody.create((MediaType)null, (byte[])(new byte[0])));
      }

      public Request.Builder put(RequestBody body) {
         return this.method("PUT", body);
      }

      public Request.Builder patch(RequestBody body) {
         return this.method("PATCH", body);
      }

      public Request.Builder method(String method, RequestBody body) {
         if (method != null && method.length() != 0) {
            if (body != null && !HttpMethod.permitsRequestBody(method)) {
               throw new IllegalArgumentException("method " + method + " must not have a request body.");
            } else if (body == null && HttpMethod.requiresRequestBody(method)) {
               throw new IllegalArgumentException("method " + method + " must have a request body.");
            } else {
               this.method = method;
               this.body = body;
               return this;
            }
         } else {
            throw new IllegalArgumentException("method == null || method.length() == 0");
         }
      }

      public Request.Builder tag(Object tag) {
         this.tag = tag;
         return this;
      }

      public Request build() {
         if (this.url == null) {
            throw new IllegalStateException("url == null");
         } else {
            return new Request(this);
         }
      }

      // $FF: synthetic method
      Builder(Request x0, Object x1) {
         this(x0);
      }
   }
}
