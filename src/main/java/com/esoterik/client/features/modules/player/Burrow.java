package com.esoterik.client.features.modules.player;

import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.BurrowUtil;
import java.util.Iterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockObsidian;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.network.play.client.CPacketPlayer.Position;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class Burrow extends Module {
   private final Setting<Integer> offset = this.register(new Setting("Offset", 3, -5, 5));
   private final Setting<Boolean> rotate = this.register(new Setting("Rotate", false));
   private final Setting<Burrow.Mode> mode;
   private BlockPos originalPos;
   private int oldSlot;
   Block returnBlock;

   public Burrow() {
      super("Cripple", "TPs you into a block", Module.Category.PLAYER, true, false, false);
      this.mode = this.register(new Setting("Mode", Burrow.Mode.OBBY));
      this.oldSlot = -1;
      this.returnBlock = null;
   }

   public void onEnable() {
      super.onEnable();
      this.originalPos = new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v);
      switch((Burrow.Mode)this.mode.getValue()) {
      case OBBY:
         this.returnBlock = Blocks.field_150343_Z;
         break;
      case ECHEST:
         this.returnBlock = Blocks.field_150477_bB;
         break;
      case CHEST:
         this.returnBlock = Blocks.field_150486_ae;
      }

      if (!mc.field_71441_e.func_180495_p(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v)).func_177230_c().equals(this.returnBlock) && !this.intersectsWithEntity(this.originalPos)) {
         this.oldSlot = mc.field_71439_g.field_71071_by.field_70461_c;
      } else {
         this.toggle();
      }
   }

   public void onUpdate() {
      switch((Burrow.Mode)this.mode.getValue()) {
      case OBBY:
         if (BurrowUtil.findHotbarBlock(BlockObsidian.class) == -1) {
            Command.sendMessage("Can't find obby in hotbar!");
            this.toggle();
         }
         break;
      case ECHEST:
         if (BurrowUtil.findHotbarBlock(BlockEnderChest.class) == -1) {
            Command.sendMessage("Can't find echest in hotbar!");
            this.toggle();
         }
         break;
      case CHEST:
         if (BurrowUtil.findHotbarBlock(BlockChest.class) == -1) {
            Command.sendMessage("Can't find chest in hotbar!");
            this.toggle();
         }
      }

      switch((Burrow.Mode)this.mode.getValue()) {
      case OBBY:
         BurrowUtil.switchToSlot(BurrowUtil.findHotbarBlock(BlockObsidian.class));
         break;
      case ECHEST:
         BurrowUtil.switchToSlot(BurrowUtil.findHotbarBlock(BlockEnderChest.class));
         break;
      case CHEST:
         BurrowUtil.switchToSlot(BurrowUtil.findHotbarBlock(BlockChest.class));
      }

      mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 0.41999998688698D, mc.field_71439_g.field_70161_v, true));
      mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 0.7531999805211997D, mc.field_71439_g.field_70161_v, true));
      mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 1.00133597911214D, mc.field_71439_g.field_70161_v, true));
      mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 1.16610926093821D, mc.field_71439_g.field_70161_v, true));
      BurrowUtil.placeBlock(this.originalPos, EnumHand.MAIN_HAND, (Boolean)this.rotate.getValue(), true, false);
      mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + (double)(Integer)this.offset.getValue(), mc.field_71439_g.field_70161_v, false));
      mc.field_71439_g.field_71174_a.func_147297_a(new CPacketEntityAction(mc.field_71439_g, Action.STOP_SNEAKING));
      mc.field_71439_g.func_70095_a(false);
      BurrowUtil.switchToSlot(this.oldSlot);
      this.toggle();
   }

   private boolean intersectsWithEntity(BlockPos pos) {
      Iterator var2 = mc.field_71441_e.field_72996_f.iterator();

      Entity entity;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         entity = (Entity)var2.next();
      } while(entity.equals(mc.field_71439_g) || entity instanceof EntityItem || !(new AxisAlignedBB(pos)).func_72326_a(entity.func_174813_aQ()));

      return true;
   }

   public static enum Mode {
      OBBY,
      ECHEST,
      CHEST;
   }
}
