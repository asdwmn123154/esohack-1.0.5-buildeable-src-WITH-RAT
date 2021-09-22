package com.esoterik.client.features.modules.client;

import com.esoterik.client.esohack;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.manager.FileManager;
import com.esoterik.client.util.Timer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;

public class Notifications extends Module {
   public Setting<Boolean> totemPops = this.register(new Setting("TotemPops", false));
   public Setting<Boolean> totemNoti = this.register(new Setting("TotemNoti", true, (v) -> {
      return (Boolean)this.totemPops.getValue();
   }));
   public Setting<Integer> delay = this.register(new Setting("Delay", 2000, 0, 5000, (v) -> {
      return (Boolean)this.totemPops.getValue();
   }, "Delays messages."));
   public Setting<Boolean> clearOnLogout = this.register(new Setting("LogoutClear", false));
   public Setting<Boolean> visualRange = this.register(new Setting("VisualRange", false));
   public Setting<Boolean> coords = this.register(new Setting("Coords", true, (v) -> {
      return (Boolean)this.visualRange.getValue();
   }));
   public Setting<Boolean> leaving = this.register(new Setting("Leaving", false, (v) -> {
      return (Boolean)this.visualRange.getValue();
   }));
   public Setting<Boolean> crash = this.register(new Setting("Crash", false));
   private List<EntityPlayer> knownPlayers = new ArrayList();
   private static List<String> modules = new ArrayList();
   private static final String fileName = "client/util/ModuleMessage_List.txt";
   private final Timer timer = new Timer();
   public Timer totemAnnounce = new Timer();
   private boolean check;
   private static Notifications INSTANCE = new Notifications();

   public Notifications() {
      super("Notifications", "Sends Messages.", Module.Category.CLIENT, true, true, false);
      this.setInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public void onLoad() {
      this.check = true;
      this.loadFile();
      this.check = false;
   }

   public void onEnable() {
      this.knownPlayers = new ArrayList();
      if (!this.check) {
         this.loadFile();
      }

   }

   public void onUpdate() {
      if ((Boolean)this.visualRange.getValue()) {
         List<EntityPlayer> tickPlayerList = new ArrayList(mc.field_71441_e.field_73010_i);
         Iterator var2;
         EntityPlayer player;
         if (tickPlayerList.size() > 0) {
            var2 = tickPlayerList.iterator();

            while(var2.hasNext()) {
               player = (EntityPlayer)var2.next();
               if (!player.func_70005_c_().equals(mc.field_71439_g.func_70005_c_()) && !this.knownPlayers.contains(player)) {
                  this.knownPlayers.add(player);
                  if (esohack.friendManager.isFriend(player)) {
                     Command.sendMessage("Player §a" + player.func_70005_c_() + "§r" + " entered your visual range" + ((Boolean)this.coords.getValue() ? " at (" + (int)player.field_70165_t + ", " + (int)player.field_70163_u + ", " + (int)player.field_70161_v + ")!" : "!"), true);
                  } else {
                     Command.sendMessage("Player §c" + player.func_70005_c_() + "§r" + " entered your visual range" + ((Boolean)this.coords.getValue() ? " at (" + (int)player.field_70165_t + ", " + (int)player.field_70163_u + ", " + (int)player.field_70161_v + ")!" : "!"), true);
                  }

                  return;
               }
            }
         }

         if (this.knownPlayers.size() > 0) {
            var2 = this.knownPlayers.iterator();

            while(var2.hasNext()) {
               player = (EntityPlayer)var2.next();
               if (!tickPlayerList.contains(player)) {
                  this.knownPlayers.remove(player);
                  if ((Boolean)this.leaving.getValue()) {
                     if (esohack.friendManager.isFriend(player)) {
                        Command.sendMessage("Player §a" + player.func_70005_c_() + "§r" + " left your visual range" + ((Boolean)this.coords.getValue() ? " at (" + (int)player.field_70165_t + ", " + (int)player.field_70163_u + ", " + (int)player.field_70161_v + ")!" : "!"), true);
                     } else {
                        Command.sendMessage("Player §c" + player.func_70005_c_() + "§r" + " left your visual range" + ((Boolean)this.coords.getValue() ? " at (" + (int)player.field_70165_t + ", " + (int)player.field_70163_u + ", " + (int)player.field_70161_v + ")!" : "!"), true);
                     }
                  }

                  return;
               }
            }
         }
      }

   }

   public void loadFile() {
      List<String> fileInput = FileManager.readTextFileAllLines("client/util/ModuleMessage_List.txt");
      Iterator<String> i = fileInput.iterator();
      modules.clear();

      while(i.hasNext()) {
         String s = (String)i.next();
         if (!s.replaceAll("\\s", "").isEmpty()) {
            modules.add(s);
         }
      }

   }

   public static Notifications getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Notifications();
      }

      return INSTANCE;
   }

   public static void displayCrash(Exception e) {
      Command.sendMessage("§cException caught: " + e.getMessage());
   }
}
