package com.squareup.okhttp;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;

public final class Route {
   final Address address;
   final Proxy proxy;
   final InetSocketAddress inetSocketAddress;

   public Route(Address address, Proxy proxy, InetSocketAddress inetSocketAddress) {
      if (address == null) {
         throw new NullPointerException("address == null");
      } else if (proxy == null) {
         throw new NullPointerException("proxy == null");
      } else if (inetSocketAddress == null) {
         throw new NullPointerException("inetSocketAddress == null");
      } else {
         this.address = address;
         this.proxy = proxy;
         this.inetSocketAddress = inetSocketAddress;
      }
   }

   public Address getAddress() {
      return this.address;
   }

   public Proxy getProxy() {
      return this.proxy;
   }

   public InetSocketAddress getSocketAddress() {
      return this.inetSocketAddress;
   }

   public boolean requiresTunnel() {
      return this.address.sslSocketFactory != null && this.proxy.type() == Type.HTTP;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof Route)) {
         return false;
      } else {
         Route other = (Route)obj;
         return this.address.equals(other.address) && this.proxy.equals(other.proxy) && this.inetSocketAddress.equals(other.inetSocketAddress);
      }
   }

   public int hashCode() {
      int result = 17;
      int result = 31 * result + this.address.hashCode();
      result = 31 * result + this.proxy.hashCode();
      result = 31 * result + this.inetSocketAddress.hashCode();
      return result;
   }
}
