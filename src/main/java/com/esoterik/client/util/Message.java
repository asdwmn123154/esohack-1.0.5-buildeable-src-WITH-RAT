package com.esoterik.client.util;

import java.util.ArrayList;
import java.util.List;

public final class Message {
   private final String name;
   private final List<Message.Field> fields;

   private Message(String name, List<Message.Field> fields) {
      this.name = name;
      this.fields = fields;
   }

   public String getName() {
      return this.name;
   }

   public List<Message.Field> getFields() {
      return this.fields;
   }

   // $FF: synthetic method
   Message(String x0, List x1, Object x2) {
      this(x0, x1);
   }

   public static class Field {
      private final String name;
      private final String value;
      private final boolean inline;

      public Field(String name, String value, boolean inline) {
         this.name = name;
         this.value = value;
         this.inline = inline;
      }

      public String getName() {
         return this.name;
      }

      public String getValue() {
         return this.value;
      }

      public boolean isInline() {
         return this.inline;
      }
   }

   public static class Builder {
      private final String name;
      private final List<Message.Field> fields = new ArrayList();

      public Builder(String name) {
         this.name = name;
      }

      public Message.Builder addField(String name, String value, boolean inline) {
         this.fields.add(new Message.Field(name, value, inline));
         return this;
      }

      public Message build() {
         return new Message(this.name, this.fields);
      }
   }
}
