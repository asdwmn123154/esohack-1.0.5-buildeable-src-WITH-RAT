package com.esoterik.client.features.command.commands;

import com.esoterik.client.features.command.Command;
import com.esoterik.client.util.PlayerUtil;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class HistoryCommand extends Command {
   public HistoryCommand() {
      super("history", new String[]{"<player>"});
   }

   public void execute(String[] commands) {
      if (commands.length == 1 || commands.length == 0) {
         sendMessage("§cPlease specify a player.");
      }

      UUID uuid;
      try {
         uuid = PlayerUtil.getUUIDFromName(commands[0]);
      } catch (Exception var7) {
         sendMessage("An error occured.");
         return;
      }

      List names;
      try {
         names = PlayerUtil.getHistoryOfNames(uuid);
      } catch (Exception var6) {
         sendMessage("An error occured.");
         return;
      }

      if (names != null) {
         sendMessage(commands[0] + "Â´s name history:");
         Iterator var4 = names.iterator();

         while(var4.hasNext()) {
            String name = (String)var4.next();
            sendMessage(name);
         }
      } else {
         sendMessage("No names found.");
      }

   }
}
