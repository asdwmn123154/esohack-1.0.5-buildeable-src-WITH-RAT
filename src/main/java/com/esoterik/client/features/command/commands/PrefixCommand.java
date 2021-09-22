package com.esoterik.client.features.command.commands;

import com.esoterik.client.esohack;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.modules.client.ClickGui;

public class PrefixCommand extends Command {
   public PrefixCommand() {
      super("prefix", new String[]{"<char>"});
   }

   public void execute(String[] commands) {
      if (commands.length == 1) {
         Command.sendMessage("§cSpecify a new prefix.");
      } else {
         ((ClickGui)esohack.moduleManager.getModuleByClass(ClickGui.class)).prefix.setValue(commands[0]);
         Command.sendMessage("Prefix set to §a" + esohack.commandManager.getPrefix());
      }
   }
}
