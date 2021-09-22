package com.squareup.okhttp;

import com.squareup.okhttp.internal.Util;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import okio.BufferedSink;
import okio.ByteString;
import okio.Okio;
import okio.Source;

public abstract class RequestBody {
   public abstract MediaType contentType();

   public long contentLength() throws IOException {
      return -1L;
   }

   public abstract void writeTo(BufferedSink var1) throws IOException;

   public static RequestBody create(MediaType contentType, String content) {
      Charset charset = Util.UTF_8;
      if (contentType != null) {
         charset = contentType.charset();
         if (charset == null) {
            charset = Util.UTF_8;
            contentType = MediaType.parse(contentType + "; charset=utf-8");
         }
      }

      byte[] bytes = content.getBytes(charset);
      return create(contentType, bytes);
   }

   public static RequestBody create(final MediaType contentType, final ByteString content) {
      return new RequestBody() {
         public MediaType contentType() {
            return contentType;
         }

         public long contentLength() throws IOException {
            return (long)content.size();
         }

         public void writeTo(BufferedSink sink) throws IOException {
            sink.write(content);
         }
      };
   }

   public static RequestBody create(MediaType contentType, byte[] content) {
      return create(contentType, content, 0, content.length);
   }

   public static RequestBody create(final MediaType contentType, final byte[] content, final int offset, final int byteCount) {
      if (content == null) {
         throw new NullPointerException("content == null");
      } else {
         Util.checkOffsetAndCount((long)content.length, (long)offset, (long)byteCount);
         return new RequestBody() {
            public MediaType contentType() {
               return contentType;
            }

            public long contentLength() {
               return (long)byteCount;
            }

            public void writeTo(BufferedSink sink) throws IOException {
               sink.write(content, offset, byteCount);
            }
         };
      }
   }

   public static RequestBody create(final MediaType contentType, final File file) {
      if (file == null) {
         throw new NullPointerException("content == null");
      } else {
         return new RequestBody() {
            public MediaType contentType() {
               return contentType;
            }

            public long contentLength() {
               return file.length();
            }

            public void writeTo(BufferedSink sink) throws IOException {
               Source source = null;

               try {
                  source = Okio.source(file);
                  sink.writeAll(source);
               } finally {
                  Util.closeQuietly((Closeable)source);
               }

            }
         };
      }
   }
}
