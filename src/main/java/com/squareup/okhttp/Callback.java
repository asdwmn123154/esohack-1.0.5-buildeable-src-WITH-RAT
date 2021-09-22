package com.squareup.okhttp;

import java.io.IOException;

public interface Callback {
   void onFailure(Request var1, IOException var2);

   void onResponse(Response var1) throws IOException;
}
