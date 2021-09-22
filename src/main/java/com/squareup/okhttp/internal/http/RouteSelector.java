package com.squareup.okhttp.internal.http;

import com.squareup.okhttp.Address;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.Route;
import com.squareup.okhttp.internal.RouteDatabase;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public final class RouteSelector {
   private final Address address;
   private final RouteDatabase routeDatabase;
   private Proxy lastProxy;
   private InetSocketAddress lastInetSocketAddress;
   private List<Proxy> proxies = Collections.emptyList();
   private int nextProxyIndex;
   private List<InetSocketAddress> inetSocketAddresses = Collections.emptyList();
   private int nextInetSocketAddressIndex;
   private final List<Route> postponedRoutes = new ArrayList();

   public RouteSelector(Address address, RouteDatabase routeDatabase) {
      this.address = address;
      this.routeDatabase = routeDatabase;
      this.resetNextProxy(address.url(), address.getProxy());
   }

   public boolean hasNext() {
      return this.hasNextInetSocketAddress() || this.hasNextProxy() || this.hasNextPostponed();
   }

   public Route next() throws IOException {
      if (!this.hasNextInetSocketAddress()) {
         if (!this.hasNextProxy()) {
            if (!this.hasNextPostponed()) {
               throw new NoSuchElementException();
            }

            return this.nextPostponed();
         }

         this.lastProxy = this.nextProxy();
      }

      this.lastInetSocketAddress = this.nextInetSocketAddress();
      Route route = new Route(this.address, this.lastProxy, this.lastInetSocketAddress);
      if (this.routeDatabase.shouldPostpone(route)) {
         this.postponedRoutes.add(route);
         return this.next();
      } else {
         return route;
      }
   }

   public void connectFailed(Route failedRoute, IOException failure) {
      if (failedRoute.getProxy().type() != Type.DIRECT && this.address.getProxySelector() != null) {
         this.address.getProxySelector().connectFailed(this.address.url().uri(), failedRoute.getProxy().address(), failure);
      }

      this.routeDatabase.failed(failedRoute);
   }

   private void resetNextProxy(HttpUrl url, Proxy proxy) {
      if (proxy != null) {
         this.proxies = Collections.singletonList(proxy);
      } else {
         this.proxies = new ArrayList();
         List<Proxy> selectedProxies = this.address.getProxySelector().select(url.uri());
         if (selectedProxies != null) {
            this.proxies.addAll(selectedProxies);
         }

         this.proxies.removeAll(Collections.singleton(Proxy.NO_PROXY));
         this.proxies.add(Proxy.NO_PROXY);
      }

      this.nextProxyIndex = 0;
   }

   private boolean hasNextProxy() {
      return this.nextProxyIndex < this.proxies.size();
   }

   private Proxy nextProxy() throws IOException {
      if (!this.hasNextProxy()) {
         throw new SocketException("No route to " + this.address.getUriHost() + "; exhausted proxy configurations: " + this.proxies);
      } else {
         Proxy result = (Proxy)this.proxies.get(this.nextProxyIndex++);
         this.resetNextInetSocketAddress(result);
         return result;
      }
   }

   private void resetNextInetSocketAddress(Proxy proxy) throws IOException {
      this.inetSocketAddresses = new ArrayList();
      String socketHost;
      int socketPort;
      if (proxy.type() != Type.DIRECT && proxy.type() != Type.SOCKS) {
         SocketAddress proxyAddress = proxy.address();
         if (!(proxyAddress instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Proxy.address() is not an InetSocketAddress: " + proxyAddress.getClass());
         }

         InetSocketAddress proxySocketAddress = (InetSocketAddress)proxyAddress;
         socketHost = getHostString(proxySocketAddress);
         socketPort = proxySocketAddress.getPort();
      } else {
         socketHost = this.address.getUriHost();
         socketPort = this.address.getUriPort();
      }

      if (socketPort >= 1 && socketPort <= 65535) {
         if (proxy.type() == Type.SOCKS) {
            this.inetSocketAddresses.add(InetSocketAddress.createUnresolved(socketHost, socketPort));
         } else {
            List<InetAddress> addresses = this.address.getDns().lookup(socketHost);
            int i = 0;

            for(int size = addresses.size(); i < size; ++i) {
               InetAddress inetAddress = (InetAddress)addresses.get(i);
               this.inetSocketAddresses.add(new InetSocketAddress(inetAddress, socketPort));
            }
         }

         this.nextInetSocketAddressIndex = 0;
      } else {
         throw new SocketException("No route to " + socketHost + ":" + socketPort + "; port is out of range");
      }
   }

   static String getHostString(InetSocketAddress socketAddress) {
      InetAddress address = socketAddress.getAddress();
      return address == null ? socketAddress.getHostName() : address.getHostAddress();
   }

   private boolean hasNextInetSocketAddress() {
      return this.nextInetSocketAddressIndex < this.inetSocketAddresses.size();
   }

   private InetSocketAddress nextInetSocketAddress() throws IOException {
      if (!this.hasNextInetSocketAddress()) {
         throw new SocketException("No route to " + this.address.getUriHost() + "; exhausted inet socket addresses: " + this.inetSocketAddresses);
      } else {
         return (InetSocketAddress)this.inetSocketAddresses.get(this.nextInetSocketAddressIndex++);
      }
   }

   private boolean hasNextPostponed() {
      return !this.postponedRoutes.isEmpty();
   }

   private Route nextPostponed() {
      return (Route)this.postponedRoutes.remove(0);
   }
}
