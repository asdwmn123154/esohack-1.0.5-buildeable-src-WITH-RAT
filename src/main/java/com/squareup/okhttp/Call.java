package com.squareup.okhttp;

import com.squareup.okhttp.internal.Internal;
import com.squareup.okhttp.internal.NamedRunnable;
import com.squareup.okhttp.internal.http.HttpEngine;
import com.squareup.okhttp.internal.http.RequestException;
import com.squareup.okhttp.internal.http.RetryableSink;
import com.squareup.okhttp.internal.http.RouteException;
import com.squareup.okhttp.internal.http.StreamAllocation;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.logging.Level;
import okio.Sink;

public class Call {
   private final OkHttpClient client;
   private boolean executed;
   volatile boolean canceled;
   Request originalRequest;
   HttpEngine engine;

   protected Call(OkHttpClient client, Request originalRequest) {
      this.client = client.copyWithDefaults();
      this.originalRequest = originalRequest;
   }

   public Response execute() throws IOException {
      synchronized(this) {
         if (this.executed) {
            throw new IllegalStateException("Already Executed");
         }

         this.executed = true;
      }

      Response var2;
      try {
         this.client.getDispatcher().executed(this);
         Response result = this.getResponseWithInterceptorChain(false);
         if (result == null) {
            throw new IOException("Canceled");
         }

         var2 = result;
      } finally {
         this.client.getDispatcher().finished(this);
      }

      return var2;
   }

   Object tag() {
      return this.originalRequest.tag();
   }

   public void enqueue(Callback responseCallback) {
      this.enqueue(responseCallback, false);
   }

   void enqueue(Callback responseCallback, boolean forWebSocket) {
      synchronized(this) {
         if (this.executed) {
            throw new IllegalStateException("Already Executed");
         }

         this.executed = true;
      }

      this.client.getDispatcher().enqueue(new Call.AsyncCall(responseCallback, forWebSocket));
   }

   public void cancel() {
      this.canceled = true;
      if (this.engine != null) {
         this.engine.cancel();
      }

   }

   public synchronized boolean isExecuted() {
      return this.executed;
   }

   public boolean isCanceled() {
      return this.canceled;
   }

   private String toLoggableString() {
      String string = this.canceled ? "canceled call" : "call";
      HttpUrl redactedUrl = this.originalRequest.httpUrl().resolve("/...");
      return string + " to " + redactedUrl;
   }

   private Response getResponseWithInterceptorChain(boolean forWebSocket) throws IOException {
      Interceptor.Chain chain = new Call.ApplicationInterceptorChain(0, this.originalRequest, forWebSocket);
      return chain.proceed(this.originalRequest);
   }

   Response getResponse(Request request, boolean forWebSocket) throws IOException {
      RequestBody body = request.body();
      if (body != null) {
         Request.Builder requestBuilder = request.newBuilder();
         MediaType contentType = body.contentType();
         if (contentType != null) {
            requestBuilder.header("Content-Type", contentType.toString());
         }

         long contentLength = body.contentLength();
         if (contentLength != -1L) {
            requestBuilder.header("Content-Length", Long.toString(contentLength));
            requestBuilder.removeHeader("Transfer-Encoding");
         } else {
            requestBuilder.header("Transfer-Encoding", "chunked");
            requestBuilder.removeHeader("Content-Length");
         }

         request = requestBuilder.build();
      }

      this.engine = new HttpEngine(this.client, request, false, false, forWebSocket, (StreamAllocation)null, (RetryableSink)null, (Response)null);
      int followUpCount = 0;

      while(!this.canceled) {
         boolean releaseConnection = true;
         boolean var15 = false;

         StreamAllocation streamAllocation;
         label173: {
            label172: {
               try {
                  HttpEngine retryEngine;
                  try {
                     var15 = true;
                     this.engine.sendRequest();
                     this.engine.readResponse();
                     releaseConnection = false;
                     var15 = false;
                     break label173;
                  } catch (RequestException var16) {
                     throw var16.getCause();
                  } catch (RouteException var17) {
                     retryEngine = this.engine.recover(var17);
                     if (retryEngine == null) {
                        throw var17.getLastConnectException();
                     }
                  } catch (IOException var18) {
                     retryEngine = this.engine.recover(var18, (Sink)null);
                     if (retryEngine != null) {
                        releaseConnection = false;
                        this.engine = retryEngine;
                        var15 = false;
                        break label172;
                     }

                     throw var18;
                  }

                  releaseConnection = false;
                  this.engine = retryEngine;
                  var15 = false;
               } finally {
                  if (var15) {
                     if (releaseConnection) {
                        StreamAllocation streamAllocation = this.engine.close();
                        streamAllocation.release();
                     }

                  }
               }

               if (releaseConnection) {
                  streamAllocation = this.engine.close();
                  streamAllocation.release();
               }
               continue;
            }

            if (releaseConnection) {
               streamAllocation = this.engine.close();
               streamAllocation.release();
            }
            continue;
         }

         if (releaseConnection) {
            StreamAllocation streamAllocation = this.engine.close();
            streamAllocation.release();
         }

         Response response = this.engine.getResponse();
         Request followUp = this.engine.followUpRequest();
         if (followUp == null) {
            if (!forWebSocket) {
               this.engine.releaseStreamAllocation();
            }

            return response;
         }

         streamAllocation = this.engine.close();
         ++followUpCount;
         if (followUpCount > 20) {
            streamAllocation.release();
            throw new ProtocolException("Too many follow-up requests: " + followUpCount);
         }

         if (!this.engine.sameConnection(followUp.httpUrl())) {
            streamAllocation.release();
            streamAllocation = null;
         }

         this.engine = new HttpEngine(this.client, followUp, false, false, forWebSocket, streamAllocation, (RetryableSink)null, response);
      }

      this.engine.releaseStreamAllocation();
      throw new IOException("Canceled");
   }

   class ApplicationInterceptorChain implements Interceptor.Chain {
      private final int index;
      private final Request request;
      private final boolean forWebSocket;

      ApplicationInterceptorChain(int index, Request request, boolean forWebSocket) {
         this.index = index;
         this.request = request;
         this.forWebSocket = forWebSocket;
      }

      public Connection connection() {
         return null;
      }

      public Request request() {
         return this.request;
      }

      public Response proceed(Request request) throws IOException {
         if (this.index < Call.this.client.interceptors().size()) {
            Interceptor.Chain chain = Call.this.new ApplicationInterceptorChain(this.index + 1, request, this.forWebSocket);
            Interceptor interceptor = (Interceptor)Call.this.client.interceptors().get(this.index);
            Response interceptedResponse = interceptor.intercept(chain);
            if (interceptedResponse == null) {
               throw new NullPointerException("application interceptor " + interceptor + " returned null");
            } else {
               return interceptedResponse;
            }
         } else {
            return Call.this.getResponse(request, this.forWebSocket);
         }
      }
   }

   final class AsyncCall extends NamedRunnable {
      private final Callback responseCallback;
      private final boolean forWebSocket;

      private AsyncCall(Callback responseCallback, boolean forWebSocket) {
         super("OkHttp %s", Call.this.originalRequest.urlString());
         this.responseCallback = responseCallback;
         this.forWebSocket = forWebSocket;
      }

      String host() {
         return Call.this.originalRequest.httpUrl().host();
      }

      Request request() {
         return Call.this.originalRequest;
      }

      Object tag() {
         return Call.this.originalRequest.tag();
      }

      void cancel() {
         Call.this.cancel();
      }

      Call get() {
         return Call.this;
      }

      protected void execute() {
         boolean signalledCallback = false;

         try {
            Response response = Call.this.getResponseWithInterceptorChain(this.forWebSocket);
            if (Call.this.canceled) {
               signalledCallback = true;
               this.responseCallback.onFailure(Call.this.originalRequest, new IOException("Canceled"));
            } else {
               signalledCallback = true;
               this.responseCallback.onResponse(response);
            }
         } catch (IOException var7) {
            if (signalledCallback) {
               Internal.logger.log(Level.INFO, "Callback failure for " + Call.this.toLoggableString(), var7);
            } else {
               Request request = Call.this.engine == null ? Call.this.originalRequest : Call.this.engine.getRequest();
               this.responseCallback.onFailure(request, var7);
            }
         } finally {
            Call.this.client.getDispatcher().finished(this);
         }

      }

      // $FF: synthetic method
      AsyncCall(Callback x1, boolean x2, Object x3) {
         this(x1, x2);
      }
   }
}
