package com.esoterik.client.features.modules.player;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.BlockEvent;
import com.esoterik.client.event.events.PacketEvent;
import com.esoterik.client.event.events.Render3DEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.BlockUtil;
import com.esoterik.client.util.RenderUtil;
import com.esoterik.client.util.Timer;
import java.awt.Color;
import java.util.Iterator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Blocks;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketPlayerDigging;
import net.minecraft.network.play.client.CPacketPlayerDigging.Action;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Speedmine extends Module {
   public Setting<Boolean> tweaks = this.register(new Setting("Tweaks", true));
   public Setting<Boolean> reset = this.register(new Setting("Reset", true));
   public Setting<Boolean> noBreakAnim = this.register(new Setting("NoBreakAnim", false));
   public Setting<Boolean> noDelay = this.register(new Setting("NoDelay", false));
   public Setting<Boolean> noSwing = this.register(new Setting("NoSwing", false));
   public Setting<Boolean> noTrace = this.register(new Setting("NoTrace", false));
   public Setting<Boolean> allow = this.register(new Setting("AllowMultiTask", false));
   public Setting<Boolean> pickaxe = this.register(new Setting("Pickaxe", true, (v) -> {
      return (Boolean)this.noTrace.getValue();
   }));
   public Setting<Boolean> doubleBreak = this.register(new Setting("DoubleBreak", false));
   public Setting<Boolean> render = this.register(new Setting("Render", false));
   public Setting<Boolean> box = this.register(new Setting("Box", false, (v) -> {
      return (Boolean)this.render.getValue();
   }));
   public Setting<Boolean> outline = this.register(new Setting("Outline", true, (v) -> {
      return (Boolean)this.render.getValue();
   }));
   private final Setting<Integer> boxAlpha = this.register(new Setting("BoxAlpha", 85, 0, 255, (v) -> {
      return (Boolean)this.box.getValue() && (Boolean)this.render.getValue();
   }));
   private final Setting<Float> lineWidth = this.register(new Setting("LineWidth", 1.0F, 0.1F, 5.0F, (v) -> {
      return (Boolean)this.outline.getValue() && (Boolean)this.render.getValue();
   }));
   private static Speedmine INSTANCE = new Speedmine();
   public BlockPos currentPos;
   public IBlockState currentBlockState;
   private final Timer timer = new Timer();
   private boolean isMining = false;
   private BlockPos lastPos = null;
   private EnumFacing lastFacing = null;

   public Speedmine() {
      super("Speedmine", "Speeds up mining.", Module.Category.PLAYER, true, false, false);
      this.setInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static Speedmine getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Speedmine();
      }

      return INSTANCE;
   }

   public void onTick() {
      if (this.currentPos != null && (!mc.field_71441_e.func_180495_p(this.currentPos).equals(this.currentBlockState) || mc.field_71441_e.func_180495_p(this.currentPos).func_177230_c() == Blocks.field_150350_a)) {
         this.currentPos = null;
         this.currentBlockState = null;
      }

   }

   public void onUpdate() {
      if (!fullNullCheck()) {
         if ((Boolean)this.noDelay.getValue()) {
            mc.field_71442_b.field_78781_i = 0;
         }

         if (this.isMining && this.lastPos != null && this.lastFacing != null && (Boolean)this.noBreakAnim.getValue()) {
            mc.field_71439_g.field_71174_a.func_147297_a(new CPacketPlayerDigging(Action.ABORT_DESTROY_BLOCK, this.lastPos, this.lastFacing));
         }

         if ((Boolean)this.reset.getValue() && mc.field_71474_y.field_74313_G.func_151470_d() && !(Boolean)this.allow.getValue()) {
            mc.field_71442_b.field_78778_j = false;
         }

      }
   }

   public void onRender3D(Render3DEvent event) {
      if ((Boolean)this.render.getValue() && this.currentPos != null) {
         Color color = new Color(255, 255, 255, 255);
         Color readyColor = Colors.INSTANCE.isEnabled() ? Colors.INSTANCE.getCurrentColor() : new Color(125, 105, 255, 255);
         RenderUtil.drawBoxESP(this.currentPos, this.timer.passedMs((long)((int)(2000.0F * esohack.serverManager.getTpsFactor()))) ? readyColor : color, false, color, (Float)this.lineWidth.getValue(), (Boolean)this.outline.getValue(), (Boolean)this.box.getValue(), (Integer)this.boxAlpha.getValue(), false);
      }

   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (!fullNullCheck()) {
         if (event.getStage() == 0) {
            if ((Boolean)this.noSwing.getValue() && event.getPacket() instanceof CPacketAnimation) {
               event.setCanceled(true);
            }

            if ((Boolean)this.noBreakAnim.getValue() && event.getPacket() instanceof CPacketPlayerDigging) {
               CPacketPlayerDigging packet = (CPacketPlayerDigging)event.getPacket();
               if (packet != null && packet.func_179715_a() != null) {
                  try {
                     Iterator var3 = mc.field_71441_e.func_72839_b((Entity)null, new AxisAlignedBB(packet.func_179715_a())).iterator();

                     while(var3.hasNext()) {
                        Entity entity = (Entity)var3.next();
                        if (entity instanceof EntityEnderCrystal) {
                           this.showAnimation();
                           return;
                        }
                     }
                  } catch (Exception var5) {
                  }

                  if (packet.func_180762_c().equals(Action.START_DESTROY_BLOCK)) {
                     this.showAnimation(true, packet.func_179715_a(), packet.func_179714_b());
                  }

                  if (packet.func_180762_c().equals(Action.STOP_DESTROY_BLOCK)) {
                     this.showAnimation();
                  }
               }
            }
         }

      }
   }

   @SubscribeEvent
   public void onBlockEvent(BlockEvent event) {
      if (!fullNullCheck()) {
         if (event.getStage() == 3 && (Boolean)this.reset.getValue() && mc.field_71442_b.field_78770_f > 0.1F) {
            mc.field_71442_b.field_78778_j = true;
         }

         if (event.getStage() == 4 && (Boolean)this.tweaks.getValue()) {
            if (BlockUtil.canBreak(event.pos)) {
               if ((Boolean)this.reset.getValue()) {
                  mc.field_71442_b.field_78778_j = false;
               }

               if (this.currentPos == null) {
                  this.currentPos = event.pos;
                  this.currentBlockState = mc.field_71441_e.func_180495_p(this.currentPos);
                  this.timer.reset();
               }

               mc.field_71439_g.func_184609_a(EnumHand.MAIN_HAND);
               mc.field_71439_g.field_71174_a.func_147297_a(new CPacketPlayerDigging(Action.START_DESTROY_BLOCK, event.pos, event.facing));
               mc.field_71439_g.field_71174_a.func_147297_a(new CPacketPlayerDigging(Action.STOP_DESTROY_BLOCK, event.pos, event.facing));
               event.setCanceled(true);
            }

            if ((Boolean)this.doubleBreak.getValue()) {
               BlockPos above = event.pos.func_177982_a(0, 1, 0);
               if (BlockUtil.canBreak(above) && mc.field_71439_g.func_70011_f((double)above.func_177958_n(), (double)above.func_177956_o(), (double)above.func_177952_p()) <= 5.0D) {
                  mc.field_71439_g.func_184609_a(EnumHand.MAIN_HAND);
                  mc.field_71439_g.field_71174_a.func_147297_a(new CPacketPlayerDigging(Action.START_DESTROY_BLOCK, above, event.facing));
                  mc.field_71439_g.field_71174_a.func_147297_a(new CPacketPlayerDigging(Action.STOP_DESTROY_BLOCK, above, event.facing));
                  mc.field_71442_b.func_187103_a(above);
                  mc.field_71441_e.func_175698_g(above);
               }
            }
         }

      }
   }

   private void showAnimation(boolean isMining, BlockPos lastPos, EnumFacing lastFacing) {
      this.isMining = isMining;
      this.lastPos = lastPos;
      this.lastFacing = lastFacing;
   }

   public void showAnimation() {
      this.showAnimation(false, (BlockPos)null, (EnumFacing)null);
   }

   public String getDisplayInfo() {
      return "Packet";
   }
}
