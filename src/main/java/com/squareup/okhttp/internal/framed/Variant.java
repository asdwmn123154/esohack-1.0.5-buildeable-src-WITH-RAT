package com.squareup.okhttp.internal.framed;

import com.squareup.okhttp.Protocol;
import okio.BufferedSink;
import okio.BufferedSource;

public interface Variant {
   Protocol getProtocol();

   FrameReader newReader(BufferedSource var1, boolean var2);

   FrameWriter newWriter(BufferedSink var1, boolean var2);
}
