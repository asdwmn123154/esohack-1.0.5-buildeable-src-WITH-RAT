package com.esoterik.client.manager;

import com.esoterik.client.esohack;
import com.esoterik.client.features.Feature;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.modules.client.Notifications;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.player.EntityPlayer;

public class TotemPopManager extends Feature {
   private Notifications notifications;
   private Map<EntityPlayer, Integer> poplist = new ConcurrentHashMap();
   private Set<EntityPlayer> toAnnounce = new HashSet();

   public void onUpdate() {
      if (this.notifications.totemAnnounce.passedMs((long)(Integer)this.notifications.delay.getValue()) && this.notifications.isOn() && (Boolean)this.notifications.totemPops.getValue()) {
         Iterator var1 = this.toAnnounce.iterator();

         while(var1.hasNext()) {
            EntityPlayer player = (EntityPlayer)var1.next();
            if (player != null) {
               Command.sendMessage("§c" + player.func_70005_c_() + " popped " + "§a" + this.getTotemPops(player) + "§c" + " Totem" + (this.getTotemPops(player) == 1 ? "" : "s") + ".", (Boolean)this.notifications.totemNoti.getValue());
               this.toAnnounce.remove(player);
               this.notifications.totemAnnounce.reset();
               break;
            }
         }
      }

   }

   public void onLogout() {
      this.onOwnLogout((Boolean)this.notifications.clearOnLogout.getValue());
   }

   public void init() {
      this.notifications = (Notifications)esohack.moduleManager.getModuleByClass(Notifications.class);
   }

   public void onTotemPop(EntityPlayer player) {
      this.popTotem(player);
      if (!player.equals(mc.field_71439_g)) {
         this.toAnnounce.add(player);
         this.notifications.totemAnnounce.reset();
      }

   }

   public void onDeath(EntityPlayer player) {
      if (this.getTotemPops(player) != 0 && !player.equals(mc.field_71439_g) && this.notifications.isOn() && (Boolean)this.notifications.totemPops.getValue()) {
         Command.sendMessage("§c" + player.func_70005_c_() + " died after popping " + "§a" + this.getTotemPops(player) + "§c" + " Totem" + (this.getTotemPops(player) == 1 ? "" : "s") + ".", (Boolean)this.notifications.totemNoti.getValue());
         this.toAnnounce.remove(player);
      }

      this.resetPops(player);
   }

   public void onLogout(EntityPlayer player, boolean clearOnLogout) {
      if (clearOnLogout) {
         this.resetPops(player);
      }

   }

   public void onOwnLogout(boolean clearOnLogout) {
      if (clearOnLogout) {
         this.clearList();
      }

   }

   public void clearList() {
      this.poplist = new ConcurrentHashMap();
   }

   public void resetPops(EntityPlayer player) {
      this.setTotemPops(player, 0);
   }

   public void popTotem(EntityPlayer player) {
      this.poplist.merge(player, 1, Integer::sum);
   }

   public void setTotemPops(EntityPlayer player, int amount) {
      this.poplist.put(player, amount);
   }

   public int getTotemPops(EntityPlayer player) {
      Integer pops = (Integer)this.poplist.get(player);
      return pops == null ? 0 : pops;
   }

   public String getTotemPopString(EntityPlayer player) {
      return "§f" + (this.getTotemPops(player) <= 0 ? "" : "-" + this.getTotemPops(player) + " ");
   }
}
