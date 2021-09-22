package com.esoterik.client.features.modules.movement;

import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import net.minecraft.client.entity.EntityPlayerSP;

public class ReverseStep extends Module {
   private final Setting<Integer> speed = this.register(new Setting("Speed", 10, 1, 20));

   public ReverseStep() {
      super("ReverseStep", "Go down", Module.Category.MOVEMENT, true, false, false);
   }

   public void onUpdate() {
      if (!fullNullCheck() && !mc.field_71439_g.func_70090_H() && !mc.field_71439_g.func_180799_ab() && !mc.field_71439_g.func_70617_f_()) {
         if (mc.field_71439_g.field_70122_E) {
            EntityPlayerSP var10000 = mc.field_71439_g;
            var10000.field_70181_x -= (double)((float)(Integer)this.speed.getValue() / 10.0F);
         }

      }
   }

   public void onDisable() {
      super.onDisable();
      mc.field_71439_g.field_70181_x = 0.0D;
   }
}
