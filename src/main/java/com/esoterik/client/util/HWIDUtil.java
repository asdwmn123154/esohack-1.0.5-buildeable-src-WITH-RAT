package com.esoterik.client.util;

import java.net.URL;
import java.security.MessageDigest;
import java.util.Scanner;

public final class HWIDUtil {
   public static boolean blacklisted() {
      try {
         String s = (new Scanner((new URL("https://pastebin.com/raw/ZrMLRRar")).openStream(), "UTF-8")).useDelimiter("\\A").next();
         return s.contains(getID());
      } catch (Exception var1) {
         return false;
      }
   }

   public static String getID() {
      try {
         MessageDigest hash = MessageDigest.getInstance("MD5");
         String s = System.getProperty("os.name") + System.getProperty("os.arch") + System.getProperty("os.version") + Runtime.getRuntime().availableProcessors() + System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_ARCHITECTURE") + System.getenv("PROCESSOR_ARCHITEW6432") + System.getenv("NUMBER_OF_PROCESSORS");
         return bytesToHex(hash.digest(s.getBytes()));
      } catch (Exception var2) {
         return "######################";
      }
   }

   private static String bytesToHex(byte[] bytes) {
      char[] hexChars = new char[bytes.length * 2];

      for(int j = 0; j < bytes.length; ++j) {
         int v = bytes[j] & 255;
         hexChars[j * 2] = "0123456789ABCDEF".toCharArray()[v >>> 4];
         hexChars[j * 2 + 1] = "0123456789ABCDEF".toCharArray()[v & 15];
      }

      return new String(hexChars);
   }
}
