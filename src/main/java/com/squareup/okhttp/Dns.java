package com.squareup.okhttp;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

public interface Dns {
   Dns SYSTEM = new Dns() {
      public List<InetAddress> lookup(String hostname) throws UnknownHostException {
         if (hostname == null) {
            throw new UnknownHostException("hostname == null");
         } else {
            return Arrays.asList(InetAddress.getAllByName(hostname));
         }
      }
   };

   List<InetAddress> lookup(String var1) throws UnknownHostException;
}
