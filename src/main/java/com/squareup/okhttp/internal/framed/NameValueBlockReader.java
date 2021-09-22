package com.squareup.okhttp.internal.framed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import okio.Buffer;
import okio.BufferedSource;
import okio.ByteString;
import okio.ForwardingSource;
import okio.InflaterSource;
import okio.Okio;
import okio.Source;

class NameValueBlockReader {
   private final InflaterSource inflaterSource;
   private int compressedLimit;
   private final BufferedSource source;

   public NameValueBlockReader(BufferedSource source) {
      Source throttleSource = new ForwardingSource(source) {
         public long read(Buffer sink, long byteCount) throws IOException {
            if (NameValueBlockReader.this.compressedLimit == 0) {
               return -1L;
            } else {
               long read = super.read(sink, Math.min(byteCount, (long)NameValueBlockReader.this.compressedLimit));
               if (read == -1L) {
                  return -1L;
               } else {
                  NameValueBlockReader.this.compressedLimit = (int)((long)NameValueBlockReader.this.compressedLimit - read);
                  return read;
               }
            }
         }
      };
      Inflater inflater = new Inflater() {
         public int inflate(byte[] buffer, int offset, int count) throws DataFormatException {
            int result = super.inflate(buffer, offset, count);
            if (result == 0 && this.needsDictionary()) {
               this.setDictionary(Spdy3.DICTIONARY);
               result = super.inflate(buffer, offset, count);
            }

            return result;
         }
      };
      this.inflaterSource = new InflaterSource(throttleSource, inflater);
      this.source = Okio.buffer((Source)this.inflaterSource);
   }

   public List<Header> readNameValueBlock(int length) throws IOException {
      this.compressedLimit += length;
      int numberOfPairs = this.source.readInt();
      if (numberOfPairs < 0) {
         throw new IOException("numberOfPairs < 0: " + numberOfPairs);
      } else if (numberOfPairs > 1024) {
         throw new IOException("numberOfPairs > 1024: " + numberOfPairs);
      } else {
         List<Header> entries = new ArrayList(numberOfPairs);

         for(int i = 0; i < numberOfPairs; ++i) {
            ByteString name = this.readByteString().toAsciiLowercase();
            ByteString values = this.readByteString();
            if (name.size() == 0) {
               throw new IOException("name.size == 0");
            }

            entries.add(new Header(name, values));
         }

         this.doneReading();
         return entries;
      }
   }

   private ByteString readByteString() throws IOException {
      int length = this.source.readInt();
      return this.source.readByteString((long)length);
   }

   private void doneReading() throws IOException {
      if (this.compressedLimit > 0) {
         this.inflaterSource.refill();
         if (this.compressedLimit != 0) {
            throw new IOException("compressedLimit > 0: " + this.compressedLimit);
         }
      }

   }

   public void close() throws IOException {
      this.source.close();
   }
}
