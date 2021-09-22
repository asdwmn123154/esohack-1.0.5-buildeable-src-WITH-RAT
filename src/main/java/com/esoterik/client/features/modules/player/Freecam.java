package com.esoterik.client.features.modules.player;

import com.esoterik.client.event.events.PacketEvent;
import com.esoterik.client.event.events.PushEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.MathUtil;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CPacketInput;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Freecam extends Module {
   public Setting<Double> speed = this.register(new Setting("Speed", 0.5D, 0.1D, 5.0D));
   public Setting<Boolean> view = this.register(new Setting("3D", false));
   public Setting<Boolean> packet = this.register(new Setting("Packet", true));
   public Setting<Boolean> disable = this.register(new Setting("Logout/Off", true));
   private static Freecam INSTANCE = new Freecam();
   private AxisAlignedBB oldBoundingBox;
   private EntityOtherPlayerMP entity;
   private Vec3d position;
   private Entity riding;
   private float yaw;
   private float pitch;

   public Freecam() {
      super("Freecam", "Look around freely.", Module.Category.PLAYER, true, false, false);
      this.setInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static Freecam getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new Freecam();
      }

      return INSTANCE;
   }

   public void onEnable() {
      if (!fullNullCheck()) {
         this.oldBoundingBox = mc.field_71439_g.func_174813_aQ();
         mc.field_71439_g.func_174826_a(new AxisAlignedBB(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v));
         if (mc.field_71439_g.func_184187_bx() != null) {
            this.riding = mc.field_71439_g.func_184187_bx();
            mc.field_71439_g.func_184210_p();
         }

         this.entity = new EntityOtherPlayerMP(mc.field_71441_e, mc.field_71449_j.func_148256_e());
         this.entity.func_82149_j(mc.field_71439_g);
         this.entity.field_70177_z = mc.field_71439_g.field_70177_z;
         this.entity.field_70759_as = mc.field_71439_g.field_70759_as;
         this.entity.field_71071_by.func_70455_b(mc.field_71439_g.field_71071_by);
         mc.field_71441_e.func_73027_a(69420, this.entity);
         this.position = mc.field_71439_g.func_174791_d();
         this.yaw = mc.field_71439_g.field_70177_z;
         this.pitch = mc.field_71439_g.field_70125_A;
         mc.field_71439_g.field_70145_X = true;
      }

   }

   public void onDisable() {
      if (!fullNullCheck()) {
         mc.field_71439_g.func_174826_a(this.oldBoundingBox);
         if (this.riding != null) {
            mc.field_71439_g.func_184205_a(this.riding, true);
         }

         if (this.entity != null) {
            mc.field_71441_e.func_72900_e(this.entity);
         }

         if (this.position != null) {
            mc.field_71439_g.func_70107_b(this.position.field_72450_a, this.position.field_72448_b, this.position.field_72449_c);
         }

         mc.field_71439_g.field_70177_z = this.yaw;
         mc.field_71439_g.field_70125_A = this.pitch;
         mc.field_71439_g.field_70145_X = false;
      }

   }

   public void onUpdate() {
      mc.field_71439_g.field_70145_X = true;
      mc.field_71439_g.func_70016_h(0.0D, 0.0D, 0.0D);
      mc.field_71439_g.field_70747_aH = ((Double)this.speed.getValue()).floatValue();
      double[] dir = MathUtil.directionSpeed((Double)this.speed.getValue());
      if (mc.field_71439_g.field_71158_b.field_78902_a == 0.0F && mc.field_71439_g.field_71158_b.field_192832_b == 0.0F) {
         mc.field_71439_g.field_70159_w = 0.0D;
         mc.field_71439_g.field_70179_y = 0.0D;
      } else {
         mc.field_71439_g.field_70159_w = dir[0];
         mc.field_71439_g.field_70179_y = dir[1];
      }

      mc.field_71439_g.func_70031_b(false);
      if ((Boolean)this.view.getValue() && !mc.field_71474_y.field_74311_E.func_151470_d() && !mc.field_71474_y.field_74314_A.func_151470_d()) {
         mc.field_71439_g.field_70181_x = (Double)this.speed.getValue() * -MathUtil.degToRad((double)mc.field_71439_g.field_70125_A) * (double)mc.field_71439_g.field_71158_b.field_192832_b;
      }

      EntityPlayerSP var10000;
      if (mc.field_71474_y.field_74314_A.func_151470_d()) {
         var10000 = mc.field_71439_g;
         var10000.field_70181_x += (Double)this.speed.getValue();
      }

      if (mc.field_71474_y.field_74311_E.func_151470_d()) {
         var10000 = mc.field_71439_g;
         var10000.field_70181_x -= (Double)this.speed.getValue();
      }

   }

   public void onLogout() {
      if ((Boolean)this.disable.getValue()) {
         this.disable();
      }

   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getStage() == 0 && (event.getPacket() instanceof CPacketPlayer || event.getPacket() instanceof CPacketInput)) {
         event.setCanceled(true);
      }

   }

   @SubscribeEvent
   public void onPush(PushEvent event) {
      if (event.getStage() == 1) {
         event.setCanceled(true);
      }

   }
}
