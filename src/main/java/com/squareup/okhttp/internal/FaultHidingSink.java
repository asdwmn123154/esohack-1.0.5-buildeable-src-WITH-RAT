package com.squareup.okhttp.internal;

import java.io.IOException;
import okio.Buffer;
import okio.ForwardingSink;
import okio.Sink;

class FaultHidingSink extends ForwardingSink {
   private boolean hasErrors;

   public FaultHidingSink(Sink delegate) {
      super(delegate);
   }

   public void write(Buffer source, long byteCount) throws IOException {
      if (this.hasErrors) {
         source.skip(byteCount);
      } else {
         try {
            super.write(source, byteCount);
         } catch (IOException var5) {
            this.hasErrors = true;
            this.onException(var5);
         }

      }
   }

   public void flush() throws IOException {
      if (!this.hasErrors) {
         try {
            super.flush();
         } catch (IOException var2) {
            this.hasErrors = true;
            this.onException(var2);
         }

      }
   }

   public void close() throws IOException {
      if (!this.hasErrors) {
         try {
            super.close();
         } catch (IOException var2) {
            this.hasErrors = true;
            this.onException(var2);
         }

      }
   }

   protected void onException(IOException e) {
   }
}
