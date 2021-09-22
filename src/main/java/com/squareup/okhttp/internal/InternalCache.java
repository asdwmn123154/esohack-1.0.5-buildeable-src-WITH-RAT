package com.squareup.okhttp.internal;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.http.CacheRequest;
import com.squareup.okhttp.internal.http.CacheStrategy;
import java.io.IOException;

public interface InternalCache {
   Response get(Request var1) throws IOException;

   CacheRequest put(Response var1) throws IOException;

   void remove(Request var1) throws IOException;

   void update(Response var1, Response var2) throws IOException;

   void trackConditionalCacheHit();

   void trackResponse(CacheStrategy var1);
}
