package com.esoterik.client.mixin.mixins;

import com.esoterik.client.esohack;
import com.esoterik.client.features.modules.client.HUD;
import com.esoterik.client.features.modules.render.NoRender;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GuiIngame.class})
public class MixinGuiIngame extends Gui {
   @Inject(
      method = {"renderPortal"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void renderPortalHook(float n, ScaledResolution scaledResolution, CallbackInfo info) {
      if (NoRender.getInstance().isOn() && (Boolean)NoRender.getInstance().portal.getValue()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"renderPumpkinOverlay"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void renderPumpkinOverlayHook(ScaledResolution scaledRes, CallbackInfo info) {
      if (NoRender.getInstance().isOn() && (Boolean)NoRender.getInstance().pumpkin.getValue()) {
         info.cancel();
      }

   }

   @Inject(
      method = {"renderPotionEffects"},
      at = {@At("HEAD")},
      cancellable = true
   )
   protected void renderPotionEffectsHook(ScaledResolution scaledRes, CallbackInfo info) {
      if (esohack.moduleManager != null && (Boolean)HUD.getInstance().potionIcons.getValue()) {
         info.cancel();
      }

   }
}
