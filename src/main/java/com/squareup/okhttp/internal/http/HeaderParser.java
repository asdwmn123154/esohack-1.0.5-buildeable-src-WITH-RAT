package com.squareup.okhttp.internal.http;

public final class HeaderParser {
   public static int skipUntil(String input, int pos, String characters) {
      while(pos < input.length() && characters.indexOf(input.charAt(pos)) == -1) {
         ++pos;
      }

      return pos;
   }

   public static int skipWhitespace(String input, int pos) {
      while(true) {
         if (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == ' ' || c == '\t') {
               ++pos;
               continue;
            }
         }

         return pos;
      }
   }

   public static int parseSeconds(String value, int defaultValue) {
      try {
         long seconds = Long.parseLong(value);
         if (seconds > 2147483647L) {
            return Integer.MAX_VALUE;
         } else {
            return seconds < 0L ? 0 : (int)seconds;
         }
      } catch (NumberFormatException var4) {
         return defaultValue;
      }
   }

   private HeaderParser() {
   }
}
