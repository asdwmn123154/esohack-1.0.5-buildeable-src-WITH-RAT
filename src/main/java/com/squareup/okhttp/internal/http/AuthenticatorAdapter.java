package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.Challenge;
import com.squareup.okhttp.Credentials;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Authenticator.RequestorType;
import java.net.Proxy.Type;
import java.util.List;

public final class AuthenticatorAdapter implements Authenticator {
   public static final Authenticator INSTANCE = new AuthenticatorAdapter();

   public Request authenticate(Proxy proxy, Response response) throws IOException {
      List<Challenge> challenges = response.challenges();
      Request request = response.request();
      HttpUrl url = request.httpUrl();
      int i = 0;

      for(int size = challenges.size(); i < size; ++i) {
         Challenge challenge = (Challenge)challenges.get(i);
         if ("Basic".equalsIgnoreCase(challenge.getScheme())) {
            PasswordAuthentication auth = java.net.Authenticator.requestPasswordAuthentication(url.host(), this.getConnectToInetAddress(proxy, url), url.port(), url.scheme(), challenge.getRealm(), challenge.getScheme(), url.url(), RequestorType.SERVER);
            if (auth != null) {
               String credential = Credentials.basic(auth.getUserName(), new String(auth.getPassword()));
               return request.newBuilder().header("Authorization", credential).build();
            }
         }
      }

      return null;
   }

   public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
      List<Challenge> challenges = response.challenges();
      Request request = response.request();
      HttpUrl url = request.httpUrl();
      int i = 0;

      for(int size = challenges.size(); i < size; ++i) {
         Challenge challenge = (Challenge)challenges.get(i);
         if ("Basic".equalsIgnoreCase(challenge.getScheme())) {
            InetSocketAddress proxyAddress = (InetSocketAddress)proxy.address();
            PasswordAuthentication auth = java.net.Authenticator.requestPasswordAuthentication(proxyAddress.getHostName(), this.getConnectToInetAddress(proxy, url), proxyAddress.getPort(), url.scheme(), challenge.getRealm(), challenge.getScheme(), url.url(), RequestorType.PROXY);
            if (auth != null) {
               String credential = Credentials.basic(auth.getUserName(), new String(auth.getPassword()));
               return request.newBuilder().header("Proxy-Authorization", credential).build();
            }
         }
      }

      return null;
   }

   private InetAddress getConnectToInetAddress(Proxy proxy, HttpUrl url) throws IOException {
      return proxy != null && proxy.type() != Type.DIRECT ? ((InetSocketAddress)proxy.address()).getAddress() : InetAddress.getByName(url.host());
   }
}
