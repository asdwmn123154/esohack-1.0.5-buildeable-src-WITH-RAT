package com.squareup.okhttp.internal;

import com.squareup.okhttp.Address;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.internal.http.StreamAllocation;
import com.squareup.okhttp.internal.io.RealConnection;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.logging.Logger;
import javax.net.ssl.SSLSocket;

public abstract class Internal {
   public static final Logger logger = Logger.getLogger(OkHttpClient.class.getName());
   public static Internal instance;

   public static void initializeInstanceForTests() {
      new OkHttpClient();
   }

   public abstract void addLenient(Headers.Builder var1, String var2);

   public abstract void addLenient(Headers.Builder var1, String var2, String var3);

   public abstract void setCache(OkHttpClient var1, InternalCache var2);

   public abstract InternalCache internalCache(OkHttpClient var1);

   public abstract RealConnection get(ConnectionPool var1, Address var2, StreamAllocation var3);

   public abstract void put(ConnectionPool var1, RealConnection var2);

   public abstract boolean connectionBecameIdle(ConnectionPool var1, RealConnection var2);

   public abstract RouteDatabase routeDatabase(ConnectionPool var1);

   public abstract void apply(ConnectionSpec var1, SSLSocket var2, boolean var3);

   public abstract HttpUrl getHttpUrlChecked(String var1) throws MalformedURLException, UnknownHostException;

   public abstract void callEnqueue(Call var1, Callback var2, boolean var3);

   public abstract StreamAllocation callEngineGetStreamAllocation(Call var1);
}
