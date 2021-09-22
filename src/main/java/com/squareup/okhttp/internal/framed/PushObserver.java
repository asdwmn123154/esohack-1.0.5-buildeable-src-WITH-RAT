package com.squareup.okhttp.internal.framed;

import java.io.IOException;
import java.util.List;
import okio.BufferedSource;

public interface PushObserver {
   PushObserver CANCEL = new PushObserver() {
      public boolean onRequest(int streamId, List<Header> requestHeaders) {
         return true;
      }

      public boolean onHeaders(int streamId, List<Header> responseHeaders, boolean last) {
         return true;
      }

      public boolean onData(int streamId, BufferedSource source, int byteCount, boolean last) throws IOException {
         source.skip((long)byteCount);
         return true;
      }

      public void onReset(int streamId, ErrorCode errorCode) {
      }
   };

   boolean onRequest(int var1, List<Header> var2);

   boolean onHeaders(int var1, List<Header> var2, boolean var3);

   boolean onData(int var1, BufferedSource var2, int var3, boolean var4) throws IOException;

   void onReset(int var1, ErrorCode var2);
}
