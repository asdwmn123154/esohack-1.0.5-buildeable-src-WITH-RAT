package com.esoterik.client.mixin.mixins;

import com.esoterik.client.event.events.PushEvent;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Entity.class})
public abstract class MixinEntity {
   public MixinEntity(World worldIn) {
   }

   @Redirect(
      method = {"applyEntityCollision"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/entity/Entity;addVelocity(DDD)V"
)
   )
   public void addVelocityHook(Entity entity, double x, double y, double z) {
      PushEvent event = new PushEvent(entity, x, y, z, true);
      MinecraftForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         entity.field_70159_w += event.x;
         entity.field_70181_x += event.y;
         entity.field_70179_y += event.z;
         entity.field_70160_al = event.airbone;
      }

   }
}
