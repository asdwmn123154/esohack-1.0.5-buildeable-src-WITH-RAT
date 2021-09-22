package com.squareup.okhttp;

import com.squareup.okhttp.internal.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import okio.Buffer;
import okio.BufferedSink;
import okio.ByteString;

public final class MultipartBuilder {
   public static final MediaType MIXED = MediaType.parse("multipart/mixed");
   public static final MediaType ALTERNATIVE = MediaType.parse("multipart/alternative");
   public static final MediaType DIGEST = MediaType.parse("multipart/digest");
   public static final MediaType PARALLEL = MediaType.parse("multipart/parallel");
   public static final MediaType FORM = MediaType.parse("multipart/form-data");
   private static final byte[] COLONSPACE = new byte[]{58, 32};
   private static final byte[] CRLF = new byte[]{13, 10};
   private static final byte[] DASHDASH = new byte[]{45, 45};
   private final ByteString boundary;
   private MediaType type;
   private final List<Headers> partHeaders;
   private final List<RequestBody> partBodies;

   public MultipartBuilder() {
      this(UUID.randomUUID().toString());
   }

   public MultipartBuilder(String boundary) {
      this.type = MIXED;
      this.partHeaders = new ArrayList();
      this.partBodies = new ArrayList();
      this.boundary = ByteString.encodeUtf8(boundary);
   }

   public MultipartBuilder type(MediaType type) {
      if (type == null) {
         throw new NullPointerException("type == null");
      } else if (!type.type().equals("multipart")) {
         throw new IllegalArgumentException("multipart != " + type);
      } else {
         this.type = type;
         return this;
      }
   }

   public MultipartBuilder addPart(RequestBody body) {
      return this.addPart((Headers)null, body);
   }

   public MultipartBuilder addPart(Headers headers, RequestBody body) {
      if (body == null) {
         throw new NullPointerException("body == null");
      } else if (headers != null && headers.get("Content-Type") != null) {
         throw new IllegalArgumentException("Unexpected header: Content-Type");
      } else if (headers != null && headers.get("Content-Length") != null) {
         throw new IllegalArgumentException("Unexpected header: Content-Length");
      } else {
         this.partHeaders.add(headers);
         this.partBodies.add(body);
         return this;
      }
   }

   private static StringBuilder appendQuotedString(StringBuilder target, String key) {
      target.append('"');
      int i = 0;

      for(int len = key.length(); i < len; ++i) {
         char ch = key.charAt(i);
         switch(ch) {
         case '\n':
            target.append("%0A");
            break;
         case '\r':
            target.append("%0D");
            break;
         case '"':
            target.append("%22");
            break;
         default:
            target.append(ch);
         }
      }

      target.append('"');
      return target;
   }

   public MultipartBuilder addFormDataPart(String name, String value) {
      return this.addFormDataPart(name, (String)null, RequestBody.create((MediaType)null, (String)value));
   }

   public MultipartBuilder addFormDataPart(String name, String filename, RequestBody value) {
      if (name == null) {
         throw new NullPointerException("name == null");
      } else {
         StringBuilder disposition = new StringBuilder("form-data; name=");
         appendQuotedString(disposition, name);
         if (filename != null) {
            disposition.append("; filename=");
            appendQuotedString(disposition, filename);
         }

         return this.addPart(Headers.of("Content-Disposition", disposition.toString()), value);
      }
   }

   public RequestBody build() {
      if (this.partHeaders.isEmpty()) {
         throw new IllegalStateException("Multipart body must have at least one part.");
      } else {
         return new MultipartBuilder.MultipartRequestBody(this.type, this.boundary, this.partHeaders, this.partBodies);
      }
   }

   private static final class MultipartRequestBody extends RequestBody {
      private final ByteString boundary;
      private final MediaType contentType;
      private final List<Headers> partHeaders;
      private final List<RequestBody> partBodies;
      private long contentLength = -1L;

      public MultipartRequestBody(MediaType type, ByteString boundary, List<Headers> partHeaders, List<RequestBody> partBodies) {
         if (type == null) {
            throw new NullPointerException("type == null");
         } else {
            this.boundary = boundary;
            this.contentType = MediaType.parse(type + "; boundary=" + boundary.utf8());
            this.partHeaders = Util.immutableList(partHeaders);
            this.partBodies = Util.immutableList(partBodies);
         }
      }

      public MediaType contentType() {
         return this.contentType;
      }

      public long contentLength() throws IOException {
         long result = this.contentLength;
         return result != -1L ? result : (this.contentLength = this.writeOrCountBytes((BufferedSink)null, true));
      }

      private long writeOrCountBytes(BufferedSink sink, boolean countBytes) throws IOException {
         long byteCount = 0L;
         Buffer byteCountBuffer = null;
         if (countBytes) {
            sink = byteCountBuffer = new Buffer();
         }

         int p = 0;

         for(int partCount = this.partHeaders.size(); p < partCount; ++p) {
            Headers headers = (Headers)this.partHeaders.get(p);
            RequestBody body = (RequestBody)this.partBodies.get(p);
            ((BufferedSink)sink).write(MultipartBuilder.DASHDASH);
            ((BufferedSink)sink).write(this.boundary);
            ((BufferedSink)sink).write(MultipartBuilder.CRLF);
            if (headers != null) {
               int h = 0;

               for(int headerCount = headers.size(); h < headerCount; ++h) {
                  ((BufferedSink)sink).writeUtf8(headers.name(h)).write(MultipartBuilder.COLONSPACE).writeUtf8(headers.value(h)).write(MultipartBuilder.CRLF);
               }
            }

            MediaType contentType = body.contentType();
            if (contentType != null) {
               ((BufferedSink)sink).writeUtf8("Content-Type: ").writeUtf8(contentType.toString()).write(MultipartBuilder.CRLF);
            }

            long contentLength = body.contentLength();
            if (contentLength != -1L) {
               ((BufferedSink)sink).writeUtf8("Content-Length: ").writeDecimalLong(contentLength).write(MultipartBuilder.CRLF);
            } else if (countBytes) {
               byteCountBuffer.clear();
               return -1L;
            }

            ((BufferedSink)sink).write(MultipartBuilder.CRLF);
            if (countBytes) {
               byteCount += contentLength;
            } else {
               ((RequestBody)this.partBodies.get(p)).writeTo((BufferedSink)sink);
            }

            ((BufferedSink)sink).write(MultipartBuilder.CRLF);
         }

         ((BufferedSink)sink).write(MultipartBuilder.DASHDASH);
         ((BufferedSink)sink).write(this.boundary);
         ((BufferedSink)sink).write(MultipartBuilder.DASHDASH);
         ((BufferedSink)sink).write(MultipartBuilder.CRLF);
         if (countBytes) {
            byteCount += byteCountBuffer.size();
            byteCountBuffer.clear();
         }

         return byteCount;
      }

      public void writeTo(BufferedSink sink) throws IOException {
         this.writeOrCountBytes(sink, false);
      }
   }
}
