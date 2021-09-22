package com.squareup.okhttp;

import com.squareup.okhttp.internal.http.HttpDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

public final class Headers {
   private final String[] namesAndValues;

   private Headers(Headers.Builder builder) {
      this.namesAndValues = (String[])builder.namesAndValues.toArray(new String[builder.namesAndValues.size()]);
   }

   private Headers(String[] namesAndValues) {
      this.namesAndValues = namesAndValues;
   }

   public String get(String name) {
      return get(this.namesAndValues, name);
   }

   public Date getDate(String name) {
      String value = this.get(name);
      return value != null ? HttpDate.parse(value) : null;
   }

   public int size() {
      return this.namesAndValues.length / 2;
   }

   public String name(int index) {
      int nameIndex = index * 2;
      return nameIndex >= 0 && nameIndex < this.namesAndValues.length ? this.namesAndValues[nameIndex] : null;
   }

   public String value(int index) {
      int valueIndex = index * 2 + 1;
      return valueIndex >= 0 && valueIndex < this.namesAndValues.length ? this.namesAndValues[valueIndex] : null;
   }

   public Set<String> names() {
      TreeSet<String> result = new TreeSet(String.CASE_INSENSITIVE_ORDER);
      int i = 0;

      for(int size = this.size(); i < size; ++i) {
         result.add(this.name(i));
      }

      return Collections.unmodifiableSet(result);
   }

   public List<String> values(String name) {
      List<String> result = null;
      int i = 0;

      for(int size = this.size(); i < size; ++i) {
         if (name.equalsIgnoreCase(this.name(i))) {
            if (result == null) {
               result = new ArrayList(2);
            }

            result.add(this.value(i));
         }
      }

      return result != null ? Collections.unmodifiableList(result) : Collections.emptyList();
   }

   public Headers.Builder newBuilder() {
      Headers.Builder result = new Headers.Builder();
      Collections.addAll(result.namesAndValues, this.namesAndValues);
      return result;
   }

   public String toString() {
      StringBuilder result = new StringBuilder();
      int i = 0;

      for(int size = this.size(); i < size; ++i) {
         result.append(this.name(i)).append(": ").append(this.value(i)).append("\n");
      }

      return result.toString();
   }

   public Map<String, List<String>> toMultimap() {
      Map<String, List<String>> result = new LinkedHashMap();
      int i = 0;

      for(int size = this.size(); i < size; ++i) {
         String name = this.name(i);
         List<String> values = (List)result.get(name);
         if (values == null) {
            values = new ArrayList(2);
            result.put(name, values);
         }

         ((List)values).add(this.value(i));
      }

      return result;
   }

   private static String get(String[] namesAndValues, String name) {
      for(int i = namesAndValues.length - 2; i >= 0; i -= 2) {
         if (name.equalsIgnoreCase(namesAndValues[i])) {
            return namesAndValues[i + 1];
         }
      }

      return null;
   }

   public static Headers of(String... namesAndValues) {
      if (namesAndValues != null && namesAndValues.length % 2 == 0) {
         namesAndValues = (String[])namesAndValues.clone();

         int i;
         for(i = 0; i < namesAndValues.length; ++i) {
            if (namesAndValues[i] == null) {
               throw new IllegalArgumentException("Headers cannot be null");
            }

            namesAndValues[i] = namesAndValues[i].trim();
         }

         for(i = 0; i < namesAndValues.length; i += 2) {
            String name = namesAndValues[i];
            String value = namesAndValues[i + 1];
            if (name.length() == 0 || name.indexOf(0) != -1 || value.indexOf(0) != -1) {
               throw new IllegalArgumentException("Unexpected header: " + name + ": " + value);
            }
         }

         return new Headers(namesAndValues);
      } else {
         throw new IllegalArgumentException("Expected alternating header names and values");
      }
   }

   public static Headers of(Map<String, String> headers) {
      if (headers == null) {
         throw new IllegalArgumentException("Expected map with header names and values");
      } else {
         String[] namesAndValues = new String[headers.size() * 2];
         int i = 0;
         Iterator var3 = headers.entrySet().iterator();

         while(var3.hasNext()) {
            Entry<String, String> header = (Entry)var3.next();
            if (header.getKey() != null && header.getValue() != null) {
               String name = ((String)header.getKey()).trim();
               String value = ((String)header.getValue()).trim();
               if (name.length() != 0 && name.indexOf(0) == -1 && value.indexOf(0) == -1) {
                  namesAndValues[i] = name;
                  namesAndValues[i + 1] = value;
                  i += 2;
                  continue;
               }

               throw new IllegalArgumentException("Unexpected header: " + name + ": " + value);
            }

            throw new IllegalArgumentException("Headers cannot be null");
         }

         return new Headers(namesAndValues);
      }
   }

   // $FF: synthetic method
   Headers(Headers.Builder x0, Object x1) {
      this(x0);
   }

   public static final class Builder {
      private final List<String> namesAndValues = new ArrayList(20);

      Headers.Builder addLenient(String line) {
         int index = line.indexOf(":", 1);
         if (index != -1) {
            return this.addLenient(line.substring(0, index), line.substring(index + 1));
         } else {
            return line.startsWith(":") ? this.addLenient("", line.substring(1)) : this.addLenient("", line);
         }
      }

      public Headers.Builder add(String line) {
         int index = line.indexOf(":");
         if (index == -1) {
            throw new IllegalArgumentException("Unexpected header: " + line);
         } else {
            return this.add(line.substring(0, index).trim(), line.substring(index + 1));
         }
      }

      public Headers.Builder add(String name, String value) {
         this.checkNameAndValue(name, value);
         return this.addLenient(name, value);
      }

      Headers.Builder addLenient(String name, String value) {
         this.namesAndValues.add(name);
         this.namesAndValues.add(value.trim());
         return this;
      }

      public Headers.Builder removeAll(String name) {
         for(int i = 0; i < this.namesAndValues.size(); i += 2) {
            if (name.equalsIgnoreCase((String)this.namesAndValues.get(i))) {
               this.namesAndValues.remove(i);
               this.namesAndValues.remove(i);
               i -= 2;
            }
         }

         return this;
      }

      public Headers.Builder set(String name, String value) {
         this.checkNameAndValue(name, value);
         this.removeAll(name);
         this.addLenient(name, value);
         return this;
      }

      private void checkNameAndValue(String name, String value) {
         if (name == null) {
            throw new IllegalArgumentException("name == null");
         } else if (name.isEmpty()) {
            throw new IllegalArgumentException("name is empty");
         } else {
            int i = 0;

            int length;
            char c;
            for(length = name.length(); i < length; ++i) {
               c = name.charAt(i);
               if (c <= 31 || c >= 127) {
                  throw new IllegalArgumentException(String.format("Unexpected char %#04x at %d in header name: %s", Integer.valueOf(c), i, name));
               }
            }

            if (value == null) {
               throw new IllegalArgumentException("value == null");
            } else {
               i = 0;

               for(length = value.length(); i < length; ++i) {
                  c = value.charAt(i);
                  if (c <= 31 || c >= 127) {
                     throw new IllegalArgumentException(String.format("Unexpected char %#04x at %d in header value: %s", Integer.valueOf(c), i, value));
                  }
               }

            }
         }
      }

      public String get(String name) {
         for(int i = this.namesAndValues.size() - 2; i >= 0; i -= 2) {
            if (name.equalsIgnoreCase((String)this.namesAndValues.get(i))) {
               return (String)this.namesAndValues.get(i + 1);
            }
         }

         return null;
      }

      public Headers build() {
         return new Headers(this);
      }
   }
}
