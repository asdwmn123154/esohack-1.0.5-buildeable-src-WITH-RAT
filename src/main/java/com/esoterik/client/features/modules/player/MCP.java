package com.esoterik.client.features.modules.player;

import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.InventoryUtil;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.util.EnumHand;
import org.lwjgl.input.Mouse;

public class MCP extends Module {
   private Setting<MCP.Mode> mode;
   private Setting<Boolean> stopRotation;
   private Setting<Integer> rotation;
   private boolean clicked;

   public MCP() {
      super("MCP", "Throws a pearl", Module.Category.PLAYER, false, false, false);
      this.mode = this.register(new Setting("Mode", MCP.Mode.MIDDLECLICK));
      this.stopRotation = this.register(new Setting("Rotation", true));
      this.rotation = this.register(new Setting("Delay", 10, 0, 100, (v) -> {
         return (Boolean)this.stopRotation.getValue();
      }));
      this.clicked = false;
   }

   public void onEnable() {
      if (!fullNullCheck() && this.mode.getValue() == MCP.Mode.TOGGLE) {
         this.throwPearl();
         this.disable();
      }

   }

   public void onTick() {
      if (this.mode.getValue() == MCP.Mode.MIDDLECLICK) {
         if (Mouse.isButtonDown(2)) {
            if (!this.clicked) {
               this.throwPearl();
            }

            this.clicked = true;
         } else {
            this.clicked = false;
         }
      }

   }

   private void throwPearl() {
      int pearlSlot = InventoryUtil.findHotbarBlock(ItemEnderPearl.class);
      boolean offhand = mc.field_71439_g.func_184592_cb().func_77973_b() == Items.field_151079_bi;
      if (pearlSlot != -1 || offhand) {
         int oldslot = mc.field_71439_g.field_71071_by.field_70461_c;
         if (!offhand) {
            InventoryUtil.switchToHotbarSlot(pearlSlot, false);
         }

         mc.field_71442_b.func_187101_a(mc.field_71439_g, mc.field_71441_e, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND);
         if (!offhand) {
            InventoryUtil.switchToHotbarSlot(oldslot, false);
         }
      }

   }

   public static enum Mode {
      TOGGLE,
      MIDDLECLICK;
   }
}
