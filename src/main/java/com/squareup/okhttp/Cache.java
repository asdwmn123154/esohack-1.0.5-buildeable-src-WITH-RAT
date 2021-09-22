package com.squareup.okhttp;

import com.squareup.okhttp.internal.DiskLruCache;
import com.squareup.okhttp.internal.InternalCache;
import com.squareup.okhttp.internal.Util;
import com.squareup.okhttp.internal.http.CacheRequest;
import com.squareup.okhttp.internal.http.CacheStrategy;
import com.squareup.okhttp.internal.http.HttpMethod;
import com.squareup.okhttp.internal.http.OkHeaders;
import com.squareup.okhttp.internal.http.StatusLine;
import com.squareup.okhttp.internal.io.FileSystem;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.ForwardingSink;
import okio.ForwardingSource;
import okio.Okio;
import okio.Sink;
import okio.Source;

public final class Cache {
   private static final int VERSION = 201105;
   private static final int ENTRY_METADATA = 0;
   private static final int ENTRY_BODY = 1;
   private static final int ENTRY_COUNT = 2;
   final InternalCache internalCache;
   private final DiskLruCache cache;
   private int writeSuccessCount;
   private int writeAbortCount;
   private int networkCount;
   private int hitCount;
   private int requestCount;

   public Cache(File directory, long maxSize) {
      this(directory, maxSize, FileSystem.SYSTEM);
   }

   Cache(File directory, long maxSize, FileSystem fileSystem) {
      this.internalCache = new InternalCache() {
         public Response get(Request request) throws IOException {
            return Cache.this.get(request);
         }

         public CacheRequest put(Response response) throws IOException {
            return Cache.this.put(response);
         }

         public void remove(Request request) throws IOException {
            Cache.this.remove(request);
         }

         public void update(Response cached, Response network) throws IOException {
            Cache.this.update(cached, network);
         }

         public void trackConditionalCacheHit() {
            Cache.this.trackConditionalCacheHit();
         }

         public void trackResponse(CacheStrategy cacheStrategy) {
            Cache.this.trackResponse(cacheStrategy);
         }
      };
      this.cache = DiskLruCache.create(fileSystem, directory, 201105, 2, maxSize);
   }

   private static String urlToKey(Request request) {
      return Util.md5Hex(request.urlString());
   }

   Response get(Request request) {
      String key = urlToKey(request);

      DiskLruCache.Snapshot snapshot;
      try {
         snapshot = this.cache.get(key);
         if (snapshot == null) {
            return null;
         }
      } catch (IOException var7) {
         return null;
      }

      Cache.Entry entry;
      try {
         entry = new Cache.Entry(snapshot.getSource(0));
      } catch (IOException var6) {
         Util.closeQuietly((Closeable)snapshot);
         return null;
      }

      Response response = entry.response(request, snapshot);
      if (!entry.matches(request, response)) {
         Util.closeQuietly((Closeable)response.body());
         return null;
      } else {
         return response;
      }
   }

   private CacheRequest put(Response response) throws IOException {
      String requestMethod = response.request().method();
      if (HttpMethod.invalidatesCache(response.request().method())) {
         try {
            this.remove(response.request());
         } catch (IOException var6) {
         }

         return null;
      } else if (!requestMethod.equals("GET")) {
         return null;
      } else if (OkHeaders.hasVaryAll(response)) {
         return null;
      } else {
         Cache.Entry entry = new Cache.Entry(response);
         DiskLruCache.Editor editor = null;

         try {
            editor = this.cache.edit(urlToKey(response.request()));
            if (editor == null) {
               return null;
            } else {
               entry.writeTo(editor);
               return new Cache.CacheRequestImpl(editor);
            }
         } catch (IOException var7) {
            this.abortQuietly(editor);
            return null;
         }
      }
   }

   private void remove(Request request) throws IOException {
      this.cache.remove(urlToKey(request));
   }

   private void update(Response cached, Response network) {
      Cache.Entry entry = new Cache.Entry(network);
      DiskLruCache.Snapshot snapshot = ((Cache.CacheResponseBody)cached.body()).snapshot;
      DiskLruCache.Editor editor = null;

      try {
         editor = snapshot.edit();
         if (editor != null) {
            entry.writeTo(editor);
            editor.commit();
         }
      } catch (IOException var7) {
         this.abortQuietly(editor);
      }

   }

   private void abortQuietly(DiskLruCache.Editor editor) {
      try {
         if (editor != null) {
            editor.abort();
         }
      } catch (IOException var3) {
      }

   }

   public void initialize() throws IOException {
      this.cache.initialize();
   }

   public void delete() throws IOException {
      this.cache.delete();
   }

   public void evictAll() throws IOException {
      this.cache.evictAll();
   }

   public Iterator<String> urls() throws IOException {
      return new Iterator<String>() {
         final Iterator<DiskLruCache.Snapshot> delegate;
         String nextUrl;
         boolean canRemove;

         {
            this.delegate = Cache.this.cache.snapshots();
         }

         public boolean hasNext() {
            if (this.nextUrl != null) {
               return true;
            } else {
               this.canRemove = false;

               while(true) {
                  if (this.delegate.hasNext()) {
                     DiskLruCache.Snapshot snapshot = (DiskLruCache.Snapshot)this.delegate.next();

                     boolean var3;
                     try {
                        BufferedSource metadata = Okio.buffer(snapshot.getSource(0));
                        this.nextUrl = metadata.readUtf8LineStrict();
                        var3 = true;
                     } catch (IOException var7) {
                        continue;
                     } finally {
                        snapshot.close();
                     }

                     return var3;
                  }

                  return false;
               }
            }
         }

         public String next() {
            if (!this.hasNext()) {
               throw new NoSuchElementException();
            } else {
               String result = this.nextUrl;
               this.nextUrl = null;
               this.canRemove = true;
               return result;
            }
         }

         public void remove() {
            if (!this.canRemove) {
               throw new IllegalStateException("remove() before next()");
            } else {
               this.delegate.remove();
            }
         }
      };
   }

   public synchronized int getWriteAbortCount() {
      return this.writeAbortCount;
   }

   public synchronized int getWriteSuccessCount() {
      return this.writeSuccessCount;
   }

   public long getSize() throws IOException {
      return this.cache.size();
   }

   public long getMaxSize() {
      return this.cache.getMaxSize();
   }

   public void flush() throws IOException {
      this.cache.flush();
   }

   public void close() throws IOException {
      this.cache.close();
   }

   public File getDirectory() {
      return this.cache.getDirectory();
   }

   public boolean isClosed() {
      return this.cache.isClosed();
   }

   private synchronized void trackResponse(CacheStrategy cacheStrategy) {
      ++this.requestCount;
      if (cacheStrategy.networkRequest != null) {
         ++this.networkCount;
      } else if (cacheStrategy.cacheResponse != null) {
         ++this.hitCount;
      }

   }

   private synchronized void trackConditionalCacheHit() {
      ++this.hitCount;
   }

   public synchronized int getNetworkCount() {
      return this.networkCount;
   }

   public synchronized int getHitCount() {
      return this.hitCount;
   }

   public synchronized int getRequestCount() {
      return this.requestCount;
   }

   private static int readInt(BufferedSource source) throws IOException {
      try {
         long result = source.readDecimalLong();
         String line = source.readUtf8LineStrict();
         if (result >= 0L && result <= 2147483647L && line.isEmpty()) {
            return (int)result;
         } else {
            throw new IOException("expected an int but was \"" + result + line + "\"");
         }
      } catch (NumberFormatException var4) {
         throw new IOException(var4.getMessage());
      }
   }

   private static class CacheResponseBody extends ResponseBody {
      private final DiskLruCache.Snapshot snapshot;
      private final BufferedSource bodySource;
      private final String contentType;
      private final String contentLength;

      public CacheResponseBody(final DiskLruCache.Snapshot snapshot, String contentType, String contentLength) {
         this.snapshot = snapshot;
         this.contentType = contentType;
         this.contentLength = contentLength;
         Source source = snapshot.getSource(1);
         this.bodySource = Okio.buffer((Source)(new ForwardingSource(source) {
            public void close() throws IOException {
               snapshot.close();
               super.close();
            }
         }));
      }

      public MediaType contentType() {
         return this.contentType != null ? MediaType.parse(this.contentType) : null;
      }

      public long contentLength() {
         try {
            return this.contentLength != null ? Long.parseLong(this.contentLength) : -1L;
         } catch (NumberFormatException var2) {
            return -1L;
         }
      }

      public BufferedSource source() {
         return this.bodySource;
      }
   }

   private static final class Entry {
      private final String url;
      private final Headers varyHeaders;
      private final String requestMethod;
      private final Protocol protocol;
      private final int code;
      private final String message;
      private final Headers responseHeaders;
      private final Handshake handshake;

      public Entry(Source in) throws IOException {
         try {
            BufferedSource source = Okio.buffer(in);
            this.url = source.readUtf8LineStrict();
            this.requestMethod = source.readUtf8LineStrict();
            Headers.Builder varyHeadersBuilder = new Headers.Builder();
            int varyRequestHeaderLineCount = Cache.readInt(source);

            for(int i = 0; i < varyRequestHeaderLineCount; ++i) {
               varyHeadersBuilder.addLenient(source.readUtf8LineStrict());
            }

            this.varyHeaders = varyHeadersBuilder.build();
            StatusLine statusLine = StatusLine.parse(source.readUtf8LineStrict());
            this.protocol = statusLine.protocol;
            this.code = statusLine.code;
            this.message = statusLine.message;
            Headers.Builder responseHeadersBuilder = new Headers.Builder();
            int responseHeaderLineCount = Cache.readInt(source);
            int i = 0;

            while(true) {
               if (i >= responseHeaderLineCount) {
                  this.responseHeaders = responseHeadersBuilder.build();
                  if (this.isHttps()) {
                     String blank = source.readUtf8LineStrict();
                     if (blank.length() > 0) {
                        throw new IOException("expected \"\" but was \"" + blank + "\"");
                     }

                     String cipherSuite = source.readUtf8LineStrict();
                     List<Certificate> peerCertificates = this.readCertificateList(source);
                     List<Certificate> localCertificates = this.readCertificateList(source);
                     this.handshake = Handshake.get(cipherSuite, peerCertificates, localCertificates);
                  } else {
                     this.handshake = null;
                  }
                  break;
               }

               responseHeadersBuilder.addLenient(source.readUtf8LineStrict());
               ++i;
            }
         } finally {
            in.close();
         }

      }

      public Entry(Response response) {
         this.url = response.request().urlString();
         this.varyHeaders = OkHeaders.varyHeaders(response);
         this.requestMethod = response.request().method();
         this.protocol = response.protocol();
         this.code = response.code();
         this.message = response.message();
         this.responseHeaders = response.headers();
         this.handshake = response.handshake();
      }

      public void writeTo(DiskLruCache.Editor editor) throws IOException {
         BufferedSink sink = Okio.buffer(editor.newSink(0));
         sink.writeUtf8(this.url);
         sink.writeByte(10);
         sink.writeUtf8(this.requestMethod);
         sink.writeByte(10);
         sink.writeDecimalLong((long)this.varyHeaders.size());
         sink.writeByte(10);
         int i = 0;

         int size;
         for(size = this.varyHeaders.size(); i < size; ++i) {
            sink.writeUtf8(this.varyHeaders.name(i));
            sink.writeUtf8(": ");
            sink.writeUtf8(this.varyHeaders.value(i));
            sink.writeByte(10);
         }

         sink.writeUtf8((new StatusLine(this.protocol, this.code, this.message)).toString());
         sink.writeByte(10);
         sink.writeDecimalLong((long)this.responseHeaders.size());
         sink.writeByte(10);
         i = 0;

         for(size = this.responseHeaders.size(); i < size; ++i) {
            sink.writeUtf8(this.responseHeaders.name(i));
            sink.writeUtf8(": ");
            sink.writeUtf8(this.responseHeaders.value(i));
            sink.writeByte(10);
         }

         if (this.isHttps()) {
            sink.writeByte(10);
            sink.writeUtf8(this.handshake.cipherSuite());
            sink.writeByte(10);
            this.writeCertList(sink, this.handshake.peerCertificates());
            this.writeCertList(sink, this.handshake.localCertificates());
         }

         sink.close();
      }

      private boolean isHttps() {
         return this.url.startsWith("https://");
      }

      private List<Certificate> readCertificateList(BufferedSource source) throws IOException {
         int length = Cache.readInt(source);
         if (length == -1) {
            return Collections.emptyList();
         } else {
            try {
               CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
               List<Certificate> result = new ArrayList(length);

               for(int i = 0; i < length; ++i) {
                  String line = source.readUtf8LineStrict();
                  Buffer bytes = new Buffer();
                  bytes.write(ByteString.decodeBase64(line));
                  result.add(certificateFactory.generateCertificate(bytes.inputStream()));
               }

               return result;
            } catch (CertificateException var8) {
               throw new IOException(var8.getMessage());
            }
         }
      }

      private void writeCertList(BufferedSink sink, List<Certificate> certificates) throws IOException {
         try {
            sink.writeDecimalLong((long)certificates.size());
            sink.writeByte(10);
            int i = 0;

            for(int size = certificates.size(); i < size; ++i) {
               byte[] bytes = ((Certificate)certificates.get(i)).getEncoded();
               String line = ByteString.of(bytes).base64();
               sink.writeUtf8(line);
               sink.writeByte(10);
            }

         } catch (CertificateEncodingException var7) {
            throw new IOException(var7.getMessage());
         }
      }

      public boolean matches(Request request, Response response) {
         return this.url.equals(request.urlString()) && this.requestMethod.equals(request.method()) && OkHeaders.varyMatches(response, this.varyHeaders, request);
      }

      public Response response(Request request, DiskLruCache.Snapshot snapshot) {
         String contentType = this.responseHeaders.get("Content-Type");
         String contentLength = this.responseHeaders.get("Content-Length");
         Request cacheRequest = (new Request.Builder()).url(this.url).method(this.requestMethod, (RequestBody)null).headers(this.varyHeaders).build();
         return (new Response.Builder()).request(cacheRequest).protocol(this.protocol).code(this.code).message(this.message).headers(this.responseHeaders).body(new Cache.CacheResponseBody(snapshot, contentType, contentLength)).handshake(this.handshake).build();
      }
   }

   private final class CacheRequestImpl implements CacheRequest {
      private final DiskLruCache.Editor editor;
      private Sink cacheOut;
      private boolean done;
      private Sink body;

      public CacheRequestImpl(final DiskLruCache.Editor editor) throws IOException {
         this.editor = editor;
         this.cacheOut = editor.newSink(1);
         this.body = new ForwardingSink(this.cacheOut) {
            public void close() throws IOException {
               synchronized(Cache.this) {
                  if (CacheRequestImpl.this.done) {
                     return;
                  }

                  CacheRequestImpl.this.done = true;
                  Cache.this.writeSuccessCount++;
               }

               super.close();
               editor.commit();
            }
         };
      }

      public void abort() {
         synchronized(Cache.this) {
            if (this.done) {
               return;
            }

            this.done = true;
            Cache.this.writeAbortCount++;
         }

         Util.closeQuietly((Closeable)this.cacheOut);

         try {
            this.editor.abort();
         } catch (IOException var3) {
         }

      }

      public Sink body() {
         return this.body;
      }
   }
}
