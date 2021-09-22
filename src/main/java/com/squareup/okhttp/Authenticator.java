package com.squareup.okhttp;

import java.io.IOException;
import java.net.Proxy;

public interface Authenticator {
   Request authenticate(Proxy var1, Response var2) throws IOException;

   Request authenticateProxy(Proxy var1, Response var2) throws IOException;
}
