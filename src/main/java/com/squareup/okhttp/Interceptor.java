package com.squareup.okhttp;

import java.io.IOException;

public interface Interceptor {
   Response intercept(Interceptor.Chain var1) throws IOException;

   public interface Chain {
      Request request();

      Response proceed(Request var1) throws IOException;

      Connection connection();
   }
}
