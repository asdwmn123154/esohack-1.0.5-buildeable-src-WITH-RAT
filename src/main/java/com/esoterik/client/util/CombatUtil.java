package com.esoterik.client.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class CombatUtil implements Minecraftable {
   public static EntityPlayer getTarget(float range) {
      EntityPlayer currentTarget = null;
      int size = mc.field_71441_e.field_73010_i.size();

      for(int i = 0; i < size; ++i) {
         EntityPlayer player = (EntityPlayer)mc.field_71441_e.field_73010_i.get(i);
         if (!EntityUtil.isntValid(player, (double)range)) {
            if (currentTarget == null) {
               currentTarget = player;
            } else if (mc.field_71439_g.func_70068_e(player) < mc.field_71439_g.func_70068_e(currentTarget)) {
               currentTarget = player;
            }
         }
      }

      return currentTarget;
   }

   public static boolean isInHole(EntityPlayer entity) {
      return isBlockValid(new BlockPos(entity.field_70165_t, entity.field_70163_u, entity.field_70161_v));
   }

   public static boolean isBlockValid(BlockPos blockPos) {
      return isBedrockHole(blockPos) || isObbyHole(blockPos) || isBothHole(blockPos);
   }

   public static int isInHoleInt(EntityPlayer entity) {
      BlockPos playerPos = new BlockPos(entity.func_174791_d());
      if (isBedrockHole(playerPos)) {
         return 1;
      } else {
         return !isObbyHole(playerPos) && !isBothHole(playerPos) ? 0 : 2;
      }
   }

   public static boolean isObbyHole(BlockPos blockPos) {
      BlockPos[] var2 = new BlockPos[]{blockPos.func_177978_c(), blockPos.func_177968_d(), blockPos.func_177974_f(), blockPos.func_177976_e(), blockPos.func_177977_b()};
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         BlockPos pos = var2[var4];
         IBlockState touchingState = mc.field_71441_e.func_180495_p(pos);
         if (touchingState.func_177230_c() != Blocks.field_150343_Z) {
            return false;
         }
      }

      return true;
   }

   public static boolean isBedrockHole(BlockPos blockPos) {
      BlockPos[] var2 = new BlockPos[]{blockPos.func_177978_c(), blockPos.func_177968_d(), blockPos.func_177974_f(), blockPos.func_177976_e(), blockPos.func_177977_b()};
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         BlockPos pos = var2[var4];
         IBlockState touchingState = mc.field_71441_e.func_180495_p(pos);
         if (touchingState.func_177230_c() != Blocks.field_150357_h) {
            return false;
         }
      }

      return true;
   }

   public static boolean isBothHole(BlockPos blockPos) {
      BlockPos[] var2 = new BlockPos[]{blockPos.func_177978_c(), blockPos.func_177968_d(), blockPos.func_177974_f(), blockPos.func_177976_e(), blockPos.func_177977_b()};
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         BlockPos pos = var2[var4];
         IBlockState touchingState = mc.field_71441_e.func_180495_p(pos);
         if (touchingState.func_177230_c() != Blocks.field_150357_h && touchingState.func_177230_c() != Blocks.field_150343_Z) {
            return false;
         }
      }

      return true;
   }
}
