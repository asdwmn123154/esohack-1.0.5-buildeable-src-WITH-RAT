package com.esoterik.client.features.command.commands;

import com.esoterik.client.esohack;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.manager.ConfigManager;
import com.google.gson.JsonParser;
import java.util.Iterator;

public class ModuleCommand extends Command {
   public ModuleCommand() {
      super("module", new String[]{"<module>", "<set/reset>", "<setting>", "<value>"});
   }

   public void execute(String[] commands) {
      if (commands.length == 1) {
         sendMessage("Modules: ");
         Iterator var8 = esohack.moduleManager.getCategories().iterator();

         while(var8.hasNext()) {
            Module.Category category = (Module.Category)var8.next();
            String modules = category.getName() + ": ";

            Module module;
            for(Iterator var5 = esohack.moduleManager.getModulesByCategory(category).iterator(); var5.hasNext(); modules = modules + (module.isEnabled() ? "§a" : "§c") + module.getName() + "§r" + ", ") {
               module = (Module)var5.next();
            }

            sendMessage(modules);
         }

      } else {
         Module module = esohack.moduleManager.getModuleByDisplayName(commands[0]);
         if (module == null) {
            module = esohack.moduleManager.getModuleByName(commands[0]);
            if (module == null) {
               sendMessage("§cThis module doesnt exist.");
            } else {
               sendMessage("§c This is the original name of the module. Its current name is: " + module.getDisplayName());
            }
         } else {
            Iterator var9;
            Setting setting;
            if (commands.length == 2) {
               sendMessage(module.getDisplayName() + " : " + module.getDescription());
               var9 = module.getSettings().iterator();

               while(var9.hasNext()) {
                  setting = (Setting)var9.next();
                  sendMessage(setting.getName() + " : " + setting.getValue() + ", " + setting.getDescription());
               }

            } else if (commands.length == 3) {
               if (commands[1].equalsIgnoreCase("set")) {
                  sendMessage("§cPlease specify a setting.");
               } else if (commands[1].equalsIgnoreCase("reset")) {
                  var9 = module.getSettings().iterator();

                  while(var9.hasNext()) {
                     setting = (Setting)var9.next();
                     setting.setValue(setting.getDefaultValue());
                  }
               } else {
                  sendMessage("§cThis command doesnt exist.");
               }

            } else if (commands.length == 4) {
               sendMessage("§cPlease specify a value.");
            } else {
               if (commands.length == 5) {
                  Setting setting = module.getSettingByName(commands[2]);
                  if (setting != null) {
                     JsonParser jp = new JsonParser();
                     if (setting.getType().equalsIgnoreCase("String")) {
                        setting.setValue(commands[3]);
                        sendMessage("§a" + module.getName() + " " + setting.getName() + " has been set to " + commands[3] + ".");
                        return;
                     }

                     try {
                        if (setting.getName().equalsIgnoreCase("Enabled")) {
                           if (commands[3].equalsIgnoreCase("true")) {
                              module.enable();
                           }

                           if (commands[3].equalsIgnoreCase("false")) {
                              module.disable();
                           }
                        }

                        ConfigManager.setValueFromJson(module, setting, jp.parse(commands[3]));
                     } catch (Exception var7) {
                        sendMessage("§cBad Value! This setting requires a: " + setting.getType() + " value.");
                        return;
                     }

                     sendMessage("§a" + module.getName() + " " + setting.getName() + " has been set to " + commands[3] + ".");
                  }
               }

            }
         }
      }
   }
}
