package com.esoterik.client.features.modules.player;

import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.InventoryUtil;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemEndCrystal;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

public class FastPlace extends Module {
   private final Setting<Boolean> all = this.register(new Setting("AllItems", false));
   private BlockPos mousePos = null;

   public FastPlace() {
      super("FastPlace", "Fast everything.", Module.Category.PLAYER, true, false, false);
   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         if ((Boolean)this.all.getValue()) {
            mc.field_71467_ac = 0;
         }

         if (InventoryUtil.holdingItem(ItemExpBottle.class)) {
            mc.field_71467_ac = 0;
         }

         if (InventoryUtil.holdingItem(ItemEndCrystal.class)) {
            mc.field_71467_ac = 0;
         }

         if (mc.field_71474_y.field_74313_G.func_151470_d()) {
            boolean offhand = mc.field_71439_g.func_184592_cb().func_77973_b() == Items.field_185158_cP;
            if (offhand || mc.field_71439_g.func_184614_ca().func_77973_b() == Items.field_185158_cP) {
               RayTraceResult result = mc.field_71476_x;
               if (result == null) {
                  return;
               }

               switch(result.field_72313_a) {
               case MISS:
                  this.mousePos = null;
                  break;
               case BLOCK:
                  this.mousePos = mc.field_71476_x.func_178782_a();
                  break;
               case ENTITY:
                  Entity entity;
                  if (this.mousePos != null && (entity = result.field_72308_g) != null && this.mousePos.equals(new BlockPos(entity.field_70165_t, entity.field_70163_u - 1.0D, entity.field_70161_v))) {
                     mc.field_71439_g.field_71174_a.func_147297_a(new CPacketPlayerTryUseItemOnBlock(this.mousePos, EnumFacing.DOWN, offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, 0.0F, 0.0F, 0.0F));
                  }
               }
            }
         }

      }
   }
}
