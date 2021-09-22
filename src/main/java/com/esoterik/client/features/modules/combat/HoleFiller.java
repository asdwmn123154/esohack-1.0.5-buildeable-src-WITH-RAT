package com.esoterik.client.features.modules.combat;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.PacketEvent;
import com.esoterik.client.event.events.Render3DEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.BlockUtil;
import com.esoterik.client.util.EntityUtil;
import com.esoterik.client.util.RenderUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HoleFiller extends Module {
   private static BlockPos PlayerPos;
   private Setting<Double> range = this.register(new Setting("Range", 4.5D, 0.1D, 6));
   private Setting<Boolean> smart = this.register(new Setting("Smart", false));
   private Setting<Integer> smartRange = this.register(new Setting("Smart Range", 4));
   private Setting<Boolean> announceUsage = this.register(new Setting("Announce Usage", false));
   private BlockPos render;
   private Entity renderEnt;
   private EntityPlayer closestTarget;
   private long systemTime = -1L;
   private static boolean togglePitch = false;
   private boolean switchCooldown = false;
   private boolean isAttacking = false;
   private boolean caOn;
   private int newSlot;
   double d;
   private static boolean isSpoofingAngles;
   private static double yaw;
   private static double pitch;
   private static HoleFiller INSTANCE = new HoleFiller();

   public HoleFiller() {
      super("HoleFiller", "Fills holes around you.", Module.Category.COMBAT, true, false, true);
      this.setInstance();
   }

   public static HoleFiller getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new HoleFiller();
      }

      return INSTANCE;
   }

   private void setInstance() {
      INSTANCE = this;
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      Packet packet = event.getPacket();
      if (packet instanceof CPacketPlayer && isSpoofingAngles) {
         ((CPacketPlayer)packet).field_149476_e = (float)yaw;
         ((CPacketPlayer)packet).field_149473_f = (float)pitch;
      }

   }

   public void onEnable() {
      if (esohack.moduleManager.isModuleEnabled("AutoGondal")) {
         this.caOn = true;
      }

      super.onEnable();
   }

   public void onUpdate() {
      if (mc.field_71441_e != null) {
         if ((Boolean)this.smart.getValue()) {
            this.findClosestTarget();
         }

         List<BlockPos> blocks = this.findCrystalBlocks();
         BlockPos q = null;
         double dist = 0.0D;
         double prevDist = 0.0D;
         int obsidianSlot = mc.field_71439_g.func_184614_ca().func_77973_b() == Item.func_150898_a(Blocks.field_150343_Z) ? mc.field_71439_g.field_71071_by.field_70461_c : -1;
         int oldSlot;
         if (obsidianSlot == -1) {
            for(oldSlot = 0; oldSlot < 9; ++oldSlot) {
               if (mc.field_71439_g.field_71071_by.func_70301_a(oldSlot).func_77973_b() == Item.func_150898_a(Blocks.field_150343_Z)) {
                  obsidianSlot = oldSlot;
                  break;
               }
            }
         }

         if (obsidianSlot != -1) {
            Iterator var11 = blocks.iterator();

            while(true) {
               while(true) {
                  BlockPos blockPos;
                  do {
                     if (!var11.hasNext()) {
                        this.render = q;
                        if (q != null && mc.field_71439_g.field_70122_E) {
                           oldSlot = mc.field_71439_g.field_71071_by.field_70461_c;
                           if (mc.field_71439_g.field_71071_by.field_70461_c != obsidianSlot) {
                              mc.field_71439_g.field_71071_by.field_70461_c = obsidianSlot;
                           }

                           this.lookAtPacket((double)q.func_177958_n() + 0.5D, (double)q.func_177956_o() - 0.5D, (double)q.func_177952_p() + 0.5D, mc.field_71439_g);
                           BlockUtil.placeBlockScaffold(this.render);
                           mc.field_71439_g.func_184609_a(EnumHand.MAIN_HAND);
                           mc.field_71439_g.field_71071_by.field_70461_c = oldSlot;
                           resetRotation();
                        }

                        return;
                     }

                     blockPos = (BlockPos)var11.next();
                  } while(!mc.field_71441_e.func_72872_a(Entity.class, new AxisAlignedBB(blockPos)).isEmpty());

                  if ((Boolean)this.smart.getValue() && this.isInRange(blockPos)) {
                     q = blockPos;
                  } else {
                     q = blockPos;
                  }
               }
            }
         }
      }
   }

   public void onRender3D(Render3DEvent event) {
      if (this.render != null) {
         RenderUtil.drawBoxESP(this.render, Colors.INSTANCE.getCurrentColor(), false, Colors.INSTANCE.getCurrentColor(), 2.0F, true, true, 150, true, -0.9D, false, false, false, false, 255);
      }

   }

   private double getDistanceToBlockPos(BlockPos pos1, BlockPos pos2) {
      double x = (double)(pos1.func_177958_n() - pos2.func_177958_n());
      double y = (double)(pos1.func_177956_o() - pos2.func_177956_o());
      double z = (double)(pos1.func_177952_p() - pos2.func_177952_p());
      return Math.sqrt(x * x + y * y + z * z);
   }

   private void lookAtPacket(double px, double py, double pz, EntityPlayer me) {
      double[] v = EntityUtil.calculateLookAt(px, py, pz, me);
      setYawAndPitch((float)v[0], (float)v[1]);
   }

   private boolean IsHole(BlockPos blockPos) {
      BlockPos boost = blockPos.func_177982_a(0, 1, 0);
      BlockPos boost2 = blockPos.func_177982_a(0, 0, 0);
      BlockPos boost3 = blockPos.func_177982_a(0, 0, -1);
      BlockPos boost4 = blockPos.func_177982_a(1, 0, 0);
      BlockPos boost5 = blockPos.func_177982_a(-1, 0, 0);
      BlockPos boost6 = blockPos.func_177982_a(0, 0, 1);
      BlockPos boost7 = blockPos.func_177982_a(0, 2, 0);
      BlockPos boost8 = blockPos.func_177963_a(0.5D, 0.5D, 0.5D);
      BlockPos boost9 = blockPos.func_177982_a(0, -1, 0);
      return mc.field_71441_e.func_180495_p(boost).func_177230_c() == Blocks.field_150350_a && mc.field_71441_e.func_180495_p(boost2).func_177230_c() == Blocks.field_150350_a && mc.field_71441_e.func_180495_p(boost7).func_177230_c() == Blocks.field_150350_a && (mc.field_71441_e.func_180495_p(boost3).func_177230_c() == Blocks.field_150343_Z || mc.field_71441_e.func_180495_p(boost3).func_177230_c() == Blocks.field_150357_h) && (mc.field_71441_e.func_180495_p(boost4).func_177230_c() == Blocks.field_150343_Z || mc.field_71441_e.func_180495_p(boost4).func_177230_c() == Blocks.field_150357_h) && (mc.field_71441_e.func_180495_p(boost5).func_177230_c() == Blocks.field_150343_Z || mc.field_71441_e.func_180495_p(boost5).func_177230_c() == Blocks.field_150357_h) && (mc.field_71441_e.func_180495_p(boost6).func_177230_c() == Blocks.field_150343_Z || mc.field_71441_e.func_180495_p(boost6).func_177230_c() == Blocks.field_150357_h) && mc.field_71441_e.func_180495_p(boost8).func_177230_c() == Blocks.field_150350_a && (mc.field_71441_e.func_180495_p(boost9).func_177230_c() == Blocks.field_150343_Z || mc.field_71441_e.func_180495_p(boost9).func_177230_c() == Blocks.field_150357_h);
   }

   public static BlockPos getPlayerPos() {
      return new BlockPos(Math.floor(mc.field_71439_g.field_70165_t), Math.floor(mc.field_71439_g.field_70163_u), Math.floor(mc.field_71439_g.field_70161_v));
   }

   public BlockPos getClosestTargetPos() {
      return this.closestTarget != null ? new BlockPos(Math.floor(this.closestTarget.field_70165_t), Math.floor(this.closestTarget.field_70163_u), Math.floor(this.closestTarget.field_70161_v)) : null;
   }

   private void findClosestTarget() {
      List<EntityPlayer> playerList = mc.field_71441_e.field_73010_i;
      this.closestTarget = null;
      Iterator var2 = playerList.iterator();

      while(var2.hasNext()) {
         EntityPlayer target = (EntityPlayer)var2.next();
         if (target != mc.field_71439_g && !esohack.friendManager.isFriend(target.func_70005_c_()) && EntityUtil.isLiving(target) && !(target.func_110143_aJ() <= 0.0F)) {
            if (this.closestTarget == null) {
               this.closestTarget = target;
            } else if (mc.field_71439_g.func_70032_d(target) < mc.field_71439_g.func_70032_d(this.closestTarget)) {
               this.closestTarget = target;
            }
         }
      }

   }

   private boolean isInRange(BlockPos blockPos) {
      NonNullList positions = NonNullList.func_191196_a();
      positions.addAll((Collection)this.getSphere(getPlayerPos(), ((Double)this.range.getValue()).floatValue(), ((Double)this.range.getValue()).intValue(), false, true, 0).stream().filter(this::IsHole).collect(Collectors.toList()));
      return positions.contains(blockPos);
   }

   private List<BlockPos> findCrystalBlocks() {
      NonNullList positions = NonNullList.func_191196_a();
      if ((Boolean)this.smart.getValue() && this.closestTarget != null) {
         positions.addAll((Collection)this.getSphere(this.getClosestTargetPos(), ((Integer)this.smartRange.getValue()).floatValue(), ((Double)this.range.getValue()).intValue(), false, true, 0).stream().filter(this::IsHole).filter(this::isInRange).collect(Collectors.toList()));
      } else if (!(Boolean)this.smart.getValue()) {
         positions.addAll((Collection)this.getSphere(getPlayerPos(), ((Double)this.range.getValue()).floatValue(), ((Double)this.range.getValue()).intValue(), false, true, 0).stream().filter(this::IsHole).collect(Collectors.toList()));
      }

      return positions;
   }

   public List<BlockPos> getSphere(BlockPos loc, float r, int h, boolean hollow, boolean sphere, int plus_y) {
      ArrayList<BlockPos> circleblocks = new ArrayList();
      int cx = loc.func_177958_n();
      int cy = loc.func_177956_o();
      int cz = loc.func_177952_p();

      for(int x = cx - (int)r; (float)x <= (float)cx + r; ++x) {
         for(int z = cz - (int)r; (float)z <= (float)cz + r; ++z) {
            int y = sphere ? cy - (int)r : cy;

            while(true) {
               float f = (float)y;
               float f2 = sphere ? (float)cy + r : (float)(cy + h);
               if (!(f < f2)) {
                  break;
               }

               double dist = (double)((cx - x) * (cx - x) + (cz - z) * (cz - z) + (sphere ? (cy - y) * (cy - y) : 0));
               if (dist < (double)(r * r) && (!hollow || !(dist < (double)((r - 1.0F) * (r - 1.0F))))) {
                  BlockPos l = new BlockPos(x, y + plus_y, z);
                  circleblocks.add(l);
               }

               ++y;
            }
         }
      }

      return circleblocks;
   }

   private static void setYawAndPitch(float yaw1, float pitch1) {
      yaw = (double)yaw1;
      pitch = (double)pitch1;
      isSpoofingAngles = true;
   }

   private static void resetRotation() {
      if (isSpoofingAngles) {
         yaw = (double)mc.field_71439_g.field_70177_z;
         pitch = (double)mc.field_71439_g.field_70125_A;
         isSpoofingAngles = false;
      }

   }

   public void onDisable() {
      this.closestTarget = null;
      this.render = null;
      resetRotation();
      super.onDisable();
   }
}
