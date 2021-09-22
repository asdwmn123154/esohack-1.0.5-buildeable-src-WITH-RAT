package com.esoterik.client.util;

import java.util.Iterator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

public class ItemUtil implements Minecraftable {
   public static int getItemFromHotbar(Item item) {
      int slot = -1;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.field_71439_g.field_71071_by.func_70301_a(i);
         if (stack.func_77973_b() == item) {
            slot = i;
         }
      }

      return slot;
   }

   public static int getItemSlot(Class clss) {
      int itemSlot = -1;

      for(int i = 45; i > 0; --i) {
         if (mc.field_71439_g.field_71071_by.func_70301_a(i).func_77973_b().getClass() == clss) {
            itemSlot = i;
            break;
         }
      }

      return itemSlot;
   }

   public static int getItemSlot(Item item) {
      int itemSlot = -1;

      for(int i = 45; i > 0; --i) {
         if (mc.field_71439_g.field_71071_by.func_70301_a(i).func_77973_b().equals(item)) {
            itemSlot = i;
            break;
         }
      }

      return itemSlot;
   }

   public static int getItemCount(Item item) {
      int count = 0;
      int size = mc.field_71439_g.field_71071_by.field_70462_a.size();

      for(int i = 0; i < size; ++i) {
         ItemStack itemStack = (ItemStack)mc.field_71439_g.field_71071_by.field_70462_a.get(i);
         if (itemStack.func_77973_b() == item) {
            count += itemStack.func_190916_E();
         }
      }

      ItemStack offhandStack = mc.field_71439_g.func_184592_cb();
      if (offhandStack.func_77973_b() == item) {
         count += offhandStack.func_190916_E();
      }

      return count;
   }

   public static boolean isArmorLow(EntityPlayer player, int durability) {
      Iterator iterator = player.field_71071_by.field_70460_b.iterator();

      ItemStack piece;
      do {
         if (!iterator.hasNext()) {
            return false;
         }

         piece = (ItemStack)iterator.next();
      } while(piece != null && !(getDamageInPercent(piece) < (float)durability));

      return true;
   }

   public static int getItemDamage(ItemStack stack) {
      return stack.func_77958_k() - stack.func_77952_i();
   }

   public static float getDamageInPercent(ItemStack stack) {
      float green = ((float)stack.func_77958_k() - (float)stack.func_77952_i()) / (float)stack.func_77958_k();
      float red = 1.0F - green;
      return (float)(100 - (int)(red * 100.0F));
   }

   public static int getRoundedDamage(ItemStack stack) {
      return (int)getDamageInPercent(stack);
   }

   public static boolean hasDurability(ItemStack stack) {
      Item item = stack.func_77973_b();
      return item instanceof ItemArmor || item instanceof ItemSword || item instanceof ItemTool || item instanceof ItemShield;
   }
}
