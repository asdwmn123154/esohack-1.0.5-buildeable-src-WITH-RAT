package com.squareup.okhttp.internal.framed;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import okio.Buffer;

public interface FrameWriter extends Closeable {
   void connectionPreface() throws IOException;

   void ackSettings(Settings var1) throws IOException;

   void pushPromise(int var1, int var2, List<Header> var3) throws IOException;

   void flush() throws IOException;

   void synStream(boolean var1, boolean var2, int var3, int var4, List<Header> var5) throws IOException;

   void synReply(boolean var1, int var2, List<Header> var3) throws IOException;

   void headers(int var1, List<Header> var2) throws IOException;

   void rstStream(int var1, ErrorCode var2) throws IOException;

   int maxDataLength();

   void data(boolean var1, int var2, Buffer var3, int var4) throws IOException;

   void settings(Settings var1) throws IOException;

   void ping(boolean var1, int var2, int var3) throws IOException;

   void goAway(int var1, ErrorCode var2, byte[] var3) throws IOException;

   void windowUpdate(int var1, long var2) throws IOException;
}
