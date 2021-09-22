package com.squareup.okhttp.internal.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import okio.Okio;
import okio.Sink;
import okio.Source;

public interface FileSystem {
   FileSystem SYSTEM = new FileSystem() {
      public Source source(File file) throws FileNotFoundException {
         return Okio.source(file);
      }

      public Sink sink(File file) throws FileNotFoundException {
         try {
            return Okio.sink(file);
         } catch (FileNotFoundException var3) {
            file.getParentFile().mkdirs();
            return Okio.sink(file);
         }
      }

      public Sink appendingSink(File file) throws FileNotFoundException {
         try {
            return Okio.appendingSink(file);
         } catch (FileNotFoundException var3) {
            file.getParentFile().mkdirs();
            return Okio.appendingSink(file);
         }
      }

      public void delete(File file) throws IOException {
         if (!file.delete() && file.exists()) {
            throw new IOException("failed to delete " + file);
         }
      }

      public boolean exists(File file) throws IOException {
         return file.exists();
      }

      public long size(File file) {
         return file.length();
      }

      public void rename(File from, File to) throws IOException {
         this.delete(to);
         if (!from.renameTo(to)) {
            throw new IOException("failed to rename " + from + " to " + to);
         }
      }

      public void deleteContents(File directory) throws IOException {
         File[] files = directory.listFiles();
         if (files == null) {
            throw new IOException("not a readable directory: " + directory);
         } else {
            File[] var3 = files;
            int var4 = files.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               File file = var3[var5];
               if (file.isDirectory()) {
                  this.deleteContents(file);
               }

               if (!file.delete()) {
                  throw new IOException("failed to delete " + file);
               }
            }

         }
      }
   };

   Source source(File var1) throws FileNotFoundException;

   Sink sink(File var1) throws FileNotFoundException;

   Sink appendingSink(File var1) throws FileNotFoundException;

   void delete(File var1) throws IOException;

   boolean exists(File var1) throws IOException;

   long size(File var1);

   void rename(File var1, File var2) throws IOException;

   void deleteContents(File var1) throws IOException;
}
