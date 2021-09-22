package com.squareup.okhttp.internal.framed;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import okio.BufferedSource;
import okio.ByteString;

public interface FrameReader extends Closeable {
   void readConnectionPreface() throws IOException;

   boolean nextFrame(FrameReader.Handler var1) throws IOException;

   public interface Handler {
      void data(boolean var1, int var2, BufferedSource var3, int var4) throws IOException;

      void headers(boolean var1, boolean var2, int var3, int var4, List<Header> var5, HeadersMode var6);

      void rstStream(int var1, ErrorCode var2);

      void settings(boolean var1, Settings var2);

      void ackSettings();

      void ping(boolean var1, int var2, int var3);

      void goAway(int var1, ErrorCode var2, ByteString var3);

      void windowUpdate(int var1, long var2);

      void priority(int var1, int var2, int var3, boolean var4);

      void pushPromise(int var1, int var2, List<Header> var3) throws IOException;

      void alternateService(int var1, String var2, ByteString var3, String var4, int var5, long var6);
   }
}
