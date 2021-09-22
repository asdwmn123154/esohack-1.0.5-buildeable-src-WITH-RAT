package com.esoterik.client.features.modules.misc;

import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.modules.Module;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityDonkey;

public class DonkeyNotifier extends Module {
   private static DonkeyNotifier instance;
   private Set<Entity> entities = new HashSet();

   public DonkeyNotifier() {
      super("DonkeyNotifier", "Notifies you when a donkey is discovered", Module.Category.MISC, true, false, false);
      instance = this;
   }

   public void onEnable() {
      this.entities.clear();
   }

   public void onUpdate() {
      Iterator var1 = mc.field_71441_e.field_72996_f.iterator();

      while(var1.hasNext()) {
         Entity entity = (Entity)var1.next();
         if (entity instanceof EntityDonkey && !this.entities.contains(entity)) {
            Command.sendMessage("Donkey Detected at: " + entity.field_70165_t + "x, " + entity.field_70163_u + "y, " + entity.field_70161_v + "z.");
            this.entities.add(entity);
         }
      }

   }
}
