package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import java.io.IOException;
import okio.Sink;

public interface HttpStream {
   int DISCARD_STREAM_TIMEOUT_MILLIS = 100;

   Sink createRequestBody(Request var1, long var2) throws IOException;

   void writeRequestHeaders(Request var1) throws IOException;

   void writeRequestBody(RetryableSink var1) throws IOException;

   void finishRequest() throws IOException;

   Response.Builder readResponseHeaders() throws IOException;

   ResponseBody openResponseBody(Response var1) throws IOException;

   void setHttpEngine(HttpEngine var1);

   void cancel();
}
