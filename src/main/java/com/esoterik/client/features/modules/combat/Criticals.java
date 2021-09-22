package com.esoterik.client.features.modules.combat;

import com.esoterik.client.event.events.PacketEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.Timer;
import java.util.Objects;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketPlayer.Position;
import net.minecraft.network.play.client.CPacketUseEntity.Action;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Criticals extends Module {
   private Setting<Criticals.Mode> mode;
   public Setting<Boolean> noDesync;
   private Setting<Integer> packets;
   private Setting<Integer> desyncDelay;
   public Setting<Boolean> cancelFirst;
   public Setting<Integer> delay32k;
   private Timer timer;
   private Timer timer32k;
   private boolean firstCanceled;
   private boolean resetTimer;

   public Criticals() {
      super("Criticals", "Scores criticals for you", Module.Category.COMBAT, true, false, false);
      this.mode = this.register(new Setting("Mode", Criticals.Mode.PACKET));
      this.noDesync = this.register(new Setting("NoDesync", true));
      this.packets = this.register(new Setting("Packets", 2, 1, 4, (v) -> {
         return this.mode.getValue() == Criticals.Mode.PACKET;
      }, "Amount of packets you want to send."));
      this.desyncDelay = this.register(new Setting("DesyncDelay", 10, 0, 500, (v) -> {
         return this.mode.getValue() == Criticals.Mode.PACKET;
      }, "Amount of packets you want to send."));
      this.cancelFirst = this.register(new Setting("CancelFirst32k", true));
      this.delay32k = this.register(new Setting("32kDelay", 25, 0, 500, (v) -> {
         return (Boolean)this.cancelFirst.getValue();
      }));
      this.timer = new Timer();
      this.timer32k = new Timer();
      this.firstCanceled = false;
      this.resetTimer = false;
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (!(Boolean)this.cancelFirst.getValue()) {
         this.firstCanceled = false;
      }

      if (event.getPacket() instanceof CPacketUseEntity) {
         CPacketUseEntity packet = (CPacketUseEntity)event.getPacket();
         if (packet.func_149565_c() == Action.ATTACK) {
            if (this.firstCanceled) {
               this.timer32k.reset();
               this.resetTimer = true;
               this.timer.setMs((long)((Integer)this.desyncDelay.getValue() + 1));
               this.firstCanceled = false;
               return;
            }

            if (this.resetTimer && !this.timer32k.passedMs((long)(Integer)this.delay32k.getValue())) {
               return;
            }

            if (this.resetTimer && this.timer32k.passedMs((long)(Integer)this.delay32k.getValue())) {
               this.resetTimer = false;
            }

            if (!this.timer.passedMs((long)(Integer)this.desyncDelay.getValue())) {
               return;
            }

            if (mc.field_71439_g.field_70122_E && !mc.field_71474_y.field_74314_A.func_151470_d() && (packet.func_149564_a(mc.field_71441_e) instanceof EntityLivingBase || !(Boolean)this.noDesync.getValue()) && !mc.field_71439_g.func_70090_H() && !mc.field_71439_g.func_180799_ab()) {
               if (this.mode.getValue() == Criticals.Mode.PACKET) {
                  switch((Integer)this.packets.getValue()) {
                  case 1:
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 0.10000000149011612D, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, false));
                     break;
                  case 2:
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 0.0625101D, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 1.1E-5D, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, false));
                     break;
                  case 3:
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 0.0625101D, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 0.0125D, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, false));
                     break;
                  case 4:
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 0.1625D, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 4.0E-6D, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u + 1.0E-6D, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new Position(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v, false));
                     mc.field_71439_g.field_71174_a.func_147297_a(new CPacketPlayer());
                     mc.field_71439_g.func_71009_b((Entity)Objects.requireNonNull(packet.func_149564_a(mc.field_71441_e)));
                  }
               } else {
                  mc.field_71439_g.func_70664_aZ();
               }

               this.timer.reset();
            }
         }
      }

   }

   public String getDisplayInfo() {
      return this.mode.currentEnumName();
   }

   public static enum Mode {
      JUMP,
      PACKET;
   }
}
