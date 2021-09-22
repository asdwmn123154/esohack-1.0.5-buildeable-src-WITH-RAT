package com.esoterik.client.features.notifications;

import com.esoterik.client.esohack;
import com.esoterik.client.features.modules.client.HUD;
import com.esoterik.client.util.RenderUtil;
import com.esoterik.client.util.Timer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class Notifications {
   private final String text;
   private final long disableTime;
   private final float width;
   private final Timer timer = new Timer();

   public Notifications(String text, long disableTime) {
      this.text = text;
      this.disableTime = disableTime;
      this.width = (float)((HUD)esohack.moduleManager.getModuleByClass(HUD.class)).renderer.getStringWidth(text);
      this.timer.reset();
   }

   public void onDraw(int y) {
      ScaledResolution scaledResolution = new ScaledResolution(Minecraft.func_71410_x());
      if (this.timer.passedMs(this.disableTime)) {
         esohack.notificationManager.getNotifications().remove(this);
      }

      RenderUtil.drawRect((float)(scaledResolution.func_78326_a() - 4) - this.width, (float)y, (float)(scaledResolution.func_78326_a() - 2), (float)(y + ((HUD)esohack.moduleManager.getModuleByClass(HUD.class)).renderer.getFontHeight() + 3), 1962934272);
      ((HUD)esohack.moduleManager.getModuleByClass(HUD.class)).renderer.drawString(this.text, (float)scaledResolution.func_78326_a() - this.width - 3.0F, (float)(y + 2), -1, true);
   }
}
