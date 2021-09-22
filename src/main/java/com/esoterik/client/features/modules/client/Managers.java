package com.esoterik.client.features.modules.client;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.ClientEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.TextUtil;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Managers extends Module {
   public Setting<Boolean> betterFrames = this.register(new Setting("BetterMaxFPS", false));
   private static Managers INSTANCE = new Managers();
   public Setting<Integer> betterFPS = this.register(new Setting("MaxFPS", 300, 30, 1000, (v) -> {
      return (Boolean)this.betterFrames.getValue();
   }));
   public Setting<Boolean> potions = this.register(new Setting("Potions", true));
   public Setting<Integer> textRadarUpdates = this.register(new Setting("TRUpdates", 500, 0, 1000));
   public Setting<Integer> respondTime = this.register(new Setting("SeverTime", 500, 0, 1000));
   public Setting<Float> holeRange = this.register(new Setting("HoleRange", 6.0F, 1.0F, 32.0F));
   public Setting<Boolean> speed = this.register(new Setting("Speed", true));
   public Setting<Boolean> tRadarInv = this.register(new Setting("TRadarInv", true));
   public Setting<Boolean> unfocusedCpu = this.register(new Setting("UnfocusedCPU", false));
   public Setting<Integer> cpuFPS = this.register(new Setting("UnfocusedFPS", 60, 1, 60, (v) -> {
      return (Boolean)this.unfocusedCpu.getValue();
   }));
   public Setting<Boolean> safety = this.register(new Setting("SafetyPlayer", false));
   public Setting<Integer> safetyCheck = this.register(new Setting("SafetyCheck", 50, 1, 150));
   public Setting<Integer> safetySync = this.register(new Setting("SafetySync", 250, 1, 10000));
   public Setting<Boolean> oneDot15 = this.register(new Setting("1.15", false));
   public Setting<Integer> holeUpdates = this.register(new Setting("HoleUpdates", 100, 0, 1000));
   public Setting<Integer> holeSync = this.register(new Setting("HoleSync", 10000, 1, 10000));
   public Setting<Managers.ThreadMode> holeThread;
   public TextUtil.Color bracketColor;
   public TextUtil.Color commandColor;
   public String commandBracket;
   public String commandBracket2;
   public String command;
   public int moduleListUpdates;
   public boolean rainbowPrefix;

   public Managers() {
      super("Management", "ClientManagement", Module.Category.CLIENT, false, true, true);
      this.holeThread = this.register(new Setting("HoleThread", Managers.ThreadMode.WHILE));
      this.bracketColor = TextUtil.Color.WHITE;
      this.commandColor = TextUtil.Color.DARK_PURPLE;
      this.commandBracket = "[";
      this.commandBracket2 = "]";
      this.command = esohack.getName();
      this.moduleListUpdates = 0;
      this.rainbowPrefix = true;
      this.setInstance();
   }

   public static Managers getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Managers();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public void onLoad() {
      esohack.commandManager.setClientMessage(this.getCommandMessage());
   }

   @SubscribeEvent
   public void onSettingChange(ClientEvent event) {
      if (event.getStage() == 2 && event.getSetting() != null && this.equals(event.getSetting().getFeature())) {
         if (event.getSetting().equals(this.holeThread)) {
            esohack.holeManager.settingChanged();
         }

         esohack.commandManager.setClientMessage(this.getCommandMessage());
      }

   }

   public String getCommandMessage() {
      return TextUtil.coloredString(this.commandBracket, this.bracketColor) + TextUtil.coloredString(this.command, this.commandColor) + TextUtil.coloredString(this.commandBracket2, this.bracketColor);
   }

   public String getRawCommandMessage() {
      return this.commandBracket + this.command + this.commandBracket2;
   }

   public static enum ThreadMode {
      POOL,
      WHILE,
      NONE;
   }
}
