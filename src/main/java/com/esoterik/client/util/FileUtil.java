package com.esoterik.client.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {
   public static boolean appendTextFile(String data, String file) {
      try {
         Path path = Paths.get(file);
         Files.write(path, Collections.singletonList(data), StandardCharsets.UTF_8, Files.exists(path, new LinkOption[0]) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
         return true;
      } catch (IOException var3) {
         System.out.println("WARNING: Unable to write file: " + file);
         return false;
      }
   }

   public static List<String> readTextFileAllLines(String file) {
      try {
         Path path = Paths.get(file);
         return Files.readAllLines(path, StandardCharsets.UTF_8);
      } catch (IOException var2) {
         System.out.println("WARNING: Unable to read file, creating new file: " + file);
         appendTextFile("", file);
         return Collections.emptyList();
      }
   }

   public static List<File> getFiles(String dir) {
      try {
         Stream<Path> paths = Files.walk(Paths.get(dir));
         Throwable var2 = null;

         List var3;
         try {
            var3 = (List)paths.filter((x$0) -> {
               return Files.isRegularFile(x$0, new LinkOption[0]);
            }).map(Path::toFile).collect(Collectors.toList());
         } catch (Throwable var13) {
            var2 = var13;
            throw var13;
         } finally {
            if (paths != null) {
               if (var2 != null) {
                  try {
                     paths.close();
                  } catch (Throwable var12) {
                     var2.addSuppressed(var12);
                  }
               } else {
                  paths.close();
               }
            }

         }

         return var3;
      } catch (Exception var15) {
         return new ArrayList();
      }
   }

   public static Optional<File> getFile(String name) {
      return Optional.of(new File(name));
   }

   public static String randomString() {
      String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
      StringBuilder sb = new StringBuilder(20);

      for(int i = 0; i < 20; ++i) {
         int index = (int)((double)AlphaNumericString.length() * Math.random());
         sb.append(AlphaNumericString.charAt(index));
      }

      return sb.toString();
   }
}
