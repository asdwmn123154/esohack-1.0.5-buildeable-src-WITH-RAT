package com.esoterik.client.mixin.mixins;

import com.esoterik.client.util.Util;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityLivingBase.class})
public abstract class MixinEntityLivingBase extends Entity {
   public MixinEntityLivingBase(World worldIn) {
      super(worldIn);
   }

   @Inject(
      method = {"isElytraFlying"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void isElytraFlyingHook(CallbackInfoReturnable<Boolean> info) {
      if (Util.mc.field_71439_g != null && Util.mc.field_71439_g.equals(this)) {
      }

   }
}
