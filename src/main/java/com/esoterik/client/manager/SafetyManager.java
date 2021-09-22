package com.esoterik.client.manager;

import com.esoterik.client.features.Feature;
import com.esoterik.client.features.modules.client.Managers;
import com.esoterik.client.util.BlockUtil;
import com.esoterik.client.util.DamageUtil;
import com.esoterik.client.util.EntityUtil;
import com.esoterik.client.util.Timer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

public class SafetyManager extends Feature implements Runnable {
   private final Timer syncTimer = new Timer();
   private ScheduledExecutorService service;
   private final AtomicBoolean SAFE = new AtomicBoolean(false);

   public void run() {
   }

   public void doSafetyCheck() {
      if (!fullNullCheck()) {
         boolean safe = true;
         EntityPlayer closest = (Boolean)Managers.getInstance().safety.getValue() ? EntityUtil.getClosestEnemy(18.0D) : null;
         if ((Boolean)Managers.getInstance().safety.getValue() && closest == null) {
            this.SAFE.set(true);
            return;
         }

         ArrayList<Entity> crystals = new ArrayList(mc.field_71441_e.field_72996_f);
         Iterator var5 = crystals.iterator();

         label67: {
            Entity crystal;
            do {
               do {
                  do {
                     if (!var5.hasNext()) {
                        break label67;
                     }

                     crystal = (Entity)var5.next();
                  } while(!(crystal instanceof EntityEnderCrystal));
               } while(!((double)DamageUtil.calculateDamage((Entity)crystal, mc.field_71439_g) > 4.0D));
            } while(closest != null && !(closest.func_70068_e(crystal) < 40.0D));

            safe = false;
         }

         if (safe) {
            label76: {
               var5 = BlockUtil.possiblePlacePositions(4.0F, false, (Boolean)Managers.getInstance().oneDot15.getValue()).iterator();

               BlockPos pos;
               do {
                  do {
                     if (!var5.hasNext()) {
                        break label76;
                     }

                     pos = (BlockPos)var5.next();
                  } while(!((double)DamageUtil.calculateDamage((BlockPos)pos, mc.field_71439_g) > 4.0D));
               } while(closest != null && !(closest.func_174818_b(pos) < 40.0D));

               safe = false;
            }
         }

         this.SAFE.set(safe);
      }

   }

   public void onUpdate() {
      this.run();
   }

   public String getSafetyString() {
      return this.SAFE.get() ? "§aSecure" : "§cUnsafe";
   }

   public boolean isSafe() {
      return this.SAFE.get();
   }

   public ScheduledExecutorService getService() {
      ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
      service.scheduleAtFixedRate(this, 0L, (long)(Integer)Managers.getInstance().safetyCheck.getValue(), TimeUnit.MILLISECONDS);
      return service;
   }
}
