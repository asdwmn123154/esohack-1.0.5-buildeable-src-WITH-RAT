package com.esoterik.client.features.modules.client;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.ClientEvent;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.gui.esohackGui;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import java.awt.Color;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.GameSettings.Options;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ClickGui extends Module {
   public Setting<String> prefix = this.register(new Setting("Prefix", "."));
   public Setting<Integer> red = this.register(new Setting("Red", 255, 0, 255));
   public Setting<Integer> green = this.register(new Setting("Green", 0, 0, 255));
   public Setting<Integer> blue = this.register(new Setting("Blue", 0, 0, 255));
   public Setting<Integer> hoverAlpha = this.register(new Setting("Alpha", 180, 0, 255));
   public Setting<Integer> alpha = this.register(new Setting("HoverAlpha", 240, 0, 255));
   public Setting<Integer> backgroundAlpha = this.register(new Setting("BackgroundAlpha", 140, 0, 255));
   public Setting<Boolean> customFov = this.register(new Setting("CustomFov", false));
   public Setting<Float> fov = this.register(new Setting("Fov", 150.0F, -180.0F, 180.0F, (v) -> {
      return (Boolean)this.customFov.getValue();
   }));
   public Setting<String> moduleButton = this.register(new Setting("Buttons", ""));
   public Setting<Boolean> colorSync = this.register(new Setting("ColorSync", false));
   private static ClickGui INSTANCE = new ClickGui();

   public ClickGui() {
      super("ClickGui", "Opens the ClickGui", Module.Category.CLIENT, true, false, false);
      this.setInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static ClickGui getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ClickGui();
      }

      return INSTANCE;
   }

   public void onUpdate() {
      if ((Boolean)this.customFov.getValue()) {
         mc.field_71474_y.func_74304_a(Options.FOV, (Float)this.fov.getValue());
      }

   }

   @SubscribeEvent
   public void onSettingChange(ClientEvent event) {
      if (event.getStage() == 2 && event.getSetting().getFeature().equals(this)) {
         if (event.getSetting().equals(this.prefix)) {
            esohack.commandManager.setPrefix((String)this.prefix.getPlannedValue());
            Command.sendMessage("Prefix set to §a" + esohack.commandManager.getPrefix());
         }

         esohack.colorManager.setColor((Integer)this.red.getPlannedValue(), (Integer)this.green.getPlannedValue(), (Integer)this.blue.getPlannedValue(), (Integer)this.hoverAlpha.getPlannedValue());
      }

   }

   public void onEnable() {
      mc.func_147108_a(new esohackGui());
   }

   public void onLoad() {
      esohack.colorManager.setColor((Integer)this.red.getValue(), (Integer)this.green.getValue(), (Integer)this.blue.getValue(), (Integer)this.hoverAlpha.getValue());
      esohack.commandManager.setPrefix((String)this.prefix.getValue());
   }

   public Color getColor() {
      return new Color((Integer)this.red.getValue(), (Integer)this.green.getValue(), (Integer)this.blue.getValue(), (Integer)this.alpha.getValue());
   }

   public void onTick() {
      if (!(mc.field_71462_r instanceof esohackGui)) {
         this.disable();
      }

   }

   public void onDisable() {
      if (mc.field_71462_r instanceof esohackGui) {
         mc.func_147108_a((GuiScreen)null);
      }

   }
}
