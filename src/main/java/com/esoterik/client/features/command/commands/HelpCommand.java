package com.esoterik.client.features.command.commands;

import com.esoterik.client.esohack;
import com.esoterik.client.features.command.Command;
import java.util.Iterator;

public class HelpCommand extends Command {
   public HelpCommand() {
      super("commands");
   }

   public void execute(String[] commands) {
      sendMessage("You can use following commands: ");
      Iterator var2 = esohack.commandManager.getCommands().iterator();

      while(var2.hasNext()) {
         Command command = (Command)var2.next();
         sendMessage(esohack.commandManager.getPrefix() + command.getName());
      }

   }
}
