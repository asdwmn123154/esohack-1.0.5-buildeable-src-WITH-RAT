package com.esoterik.client.features.modules.movement;

import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;

public class Step extends Module {
   public Setting<Integer> height = this.register(new Setting("Height", 2, 0, 5));

   public Step() {
      super("Step", "Allows you to step up blocks", Module.Category.MOVEMENT, true, false, false);
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         mc.field_71439_g.field_70138_W = 2.0F;
      }
   }

   public void onDisable() {
      super.onDisable();
      mc.field_71439_g.field_70138_W = 0.6F;
   }
}
