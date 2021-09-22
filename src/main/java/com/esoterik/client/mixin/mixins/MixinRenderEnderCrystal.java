package com.esoterik.client.mixin.mixins;

import com.esoterik.client.event.events.RenderEntityModelEvent;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.features.modules.render.CrystalChams;
import com.esoterik.client.util.EntityUtil;
import com.esoterik.client.util.RenderUtil;
import java.awt.Color;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderEnderCrystal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({RenderEnderCrystal.class})
public class MixinRenderEnderCrystal {
   @Shadow
   @Final
   private static ResourceLocation field_110787_a;
   private static ResourceLocation glint;

   @Redirect(
      method = {"doRender"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/model/ModelBase;render(Lnet/minecraft/entity/Entity;FFFFFF)V"
)
   )
   public void renderModelBaseHook(ModelBase model, Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
      if (CrystalChams.INSTANCE.isEnabled()) {
         if (CrystalChams.INSTANCE.scaleMap.containsKey((EntityEnderCrystal)entity)) {
            GlStateManager.func_179152_a((Float)CrystalChams.INSTANCE.scaleMap.get((EntityEnderCrystal)entity), (Float)CrystalChams.INSTANCE.scaleMap.get((EntityEnderCrystal)entity), (Float)CrystalChams.INSTANCE.scaleMap.get((EntityEnderCrystal)entity));
         } else {
            GlStateManager.func_179152_a((Float)CrystalChams.INSTANCE.scale.getValue(), (Float)CrystalChams.INSTANCE.scale.getValue(), (Float)CrystalChams.INSTANCE.scale.getValue());
         }

         if ((Boolean)CrystalChams.INSTANCE.wireframe.getValue()) {
            RenderEntityModelEvent event = new RenderEntityModelEvent(0, model, entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            CrystalChams.INSTANCE.onRenderModel(event);
         }
      }

      if (CrystalChams.INSTANCE.isEnabled()) {
         GL11.glPushAttrib(1048575);
         GL11.glDisable(3008);
         GL11.glDisable(3553);
         GL11.glDisable(2896);
         GL11.glEnable(3042);
         GL11.glBlendFunc(770, 771);
         GL11.glLineWidth(1.5F);
         GL11.glEnable(2960);
         Color hiddenColor;
         Color visibleColor;
         if ((Boolean)CrystalChams.INSTANCE.rainbow.getValue()) {
            visibleColor = (Boolean)CrystalChams.INSTANCE.colorSync.getValue() ? Colors.INSTANCE.getCurrentColor() : new Color(RenderUtil.getRainbow((Integer)CrystalChams.INSTANCE.speed.getValue() * 100, 0, (float)(Integer)CrystalChams.INSTANCE.saturation.getValue() / 100.0F, (float)(Integer)CrystalChams.INSTANCE.brightness.getValue() / 100.0F));
            hiddenColor = EntityUtil.getColor(entity, visibleColor.getRed(), visibleColor.getGreen(), visibleColor.getBlue(), (Integer)CrystalChams.INSTANCE.alpha.getValue(), true);
            if ((Boolean)CrystalChams.INSTANCE.throughWalls.getValue()) {
               GL11.glDisable(2929);
               GL11.glDepthMask(false);
            }

            GL11.glEnable(10754);
            GL11.glColor4f((float)hiddenColor.getRed() / 255.0F, (float)hiddenColor.getGreen() / 255.0F, (float)hiddenColor.getBlue() / 255.0F, (float)(Integer)CrystalChams.INSTANCE.alpha.getValue() / 255.0F);
            model.func_78088_a(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            if ((Boolean)CrystalChams.INSTANCE.throughWalls.getValue()) {
               GL11.glEnable(2929);
               GL11.glDepthMask(true);
            }
         } else if ((Boolean)CrystalChams.INSTANCE.xqz.getValue() && (Boolean)CrystalChams.INSTANCE.throughWalls.getValue()) {
            hiddenColor = (Boolean)CrystalChams.INSTANCE.colorSync.getValue() ? EntityUtil.getColor(entity, (Integer)CrystalChams.INSTANCE.hiddenRed.getValue(), (Integer)CrystalChams.INSTANCE.hiddenGreen.getValue(), (Integer)CrystalChams.INSTANCE.hiddenBlue.getValue(), (Integer)CrystalChams.INSTANCE.hiddenAlpha.getValue(), true) : EntityUtil.getColor(entity, (Integer)CrystalChams.INSTANCE.hiddenRed.getValue(), (Integer)CrystalChams.INSTANCE.hiddenGreen.getValue(), (Integer)CrystalChams.INSTANCE.hiddenBlue.getValue(), (Integer)CrystalChams.INSTANCE.hiddenAlpha.getValue(), true);
            visibleColor = (Boolean)CrystalChams.INSTANCE.colorSync.getValue() ? EntityUtil.getColor(entity, (Integer)CrystalChams.INSTANCE.red.getValue(), (Integer)CrystalChams.INSTANCE.green.getValue(), (Integer)CrystalChams.INSTANCE.blue.getValue(), (Integer)CrystalChams.INSTANCE.alpha.getValue(), true) : EntityUtil.getColor(entity, (Integer)CrystalChams.INSTANCE.red.getValue(), (Integer)CrystalChams.INSTANCE.green.getValue(), (Integer)CrystalChams.INSTANCE.blue.getValue(), (Integer)CrystalChams.INSTANCE.alpha.getValue(), true);
            if ((Boolean)CrystalChams.INSTANCE.throughWalls.getValue()) {
               GL11.glDisable(2929);
               GL11.glDepthMask(false);
            }

            GL11.glEnable(10754);
            GL11.glColor4f((float)hiddenColor.getRed() / 255.0F, (float)hiddenColor.getGreen() / 255.0F, (float)hiddenColor.getBlue() / 255.0F, (float)(Integer)CrystalChams.INSTANCE.alpha.getValue() / 255.0F);
            model.func_78088_a(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            if ((Boolean)CrystalChams.INSTANCE.throughWalls.getValue()) {
               GL11.glEnable(2929);
               GL11.glDepthMask(true);
            }

            GL11.glColor4f((float)visibleColor.getRed() / 255.0F, (float)visibleColor.getGreen() / 255.0F, (float)visibleColor.getBlue() / 255.0F, (float)(Integer)CrystalChams.INSTANCE.alpha.getValue() / 255.0F);
            model.func_78088_a(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
         } else {
            visibleColor = (Boolean)CrystalChams.INSTANCE.colorSync.getValue() ? Colors.INSTANCE.getCurrentColor() : EntityUtil.getColor(entity, (Integer)CrystalChams.INSTANCE.red.getValue(), (Integer)CrystalChams.INSTANCE.green.getValue(), (Integer)CrystalChams.INSTANCE.blue.getValue(), (Integer)CrystalChams.INSTANCE.alpha.getValue(), true);
            if ((Boolean)CrystalChams.INSTANCE.throughWalls.getValue()) {
               GL11.glDisable(2929);
               GL11.glDepthMask(false);
            }

            GL11.glEnable(10754);
            GL11.glColor4f((float)visibleColor.getRed() / 255.0F, (float)visibleColor.getGreen() / 255.0F, (float)visibleColor.getBlue() / 255.0F, (float)(Integer)CrystalChams.INSTANCE.alpha.getValue() / 255.0F);
            model.func_78088_a(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            if ((Boolean)CrystalChams.INSTANCE.throughWalls.getValue()) {
               GL11.glEnable(2929);
               GL11.glDepthMask(true);
            }
         }

         GL11.glEnable(3042);
         GL11.glEnable(2896);
         GL11.glEnable(3553);
         GL11.glEnable(3008);
         GL11.glPopAttrib();
      } else {
         model.func_78088_a(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
      }

      if (CrystalChams.INSTANCE.scaleMap.containsKey((EntityEnderCrystal)entity)) {
         GlStateManager.func_179152_a(1.0F / (Float)CrystalChams.INSTANCE.scaleMap.get((EntityEnderCrystal)entity), 1.0F / (Float)CrystalChams.INSTANCE.scaleMap.get((EntityEnderCrystal)entity), 1.0F / (Float)CrystalChams.INSTANCE.scaleMap.get((EntityEnderCrystal)entity));
      } else {
         GlStateManager.func_179152_a(1.0F / (Float)CrystalChams.INSTANCE.scale.getValue(), 1.0F / (Float)CrystalChams.INSTANCE.scale.getValue(), 1.0F / (Float)CrystalChams.INSTANCE.scale.getValue());
      }

   }
}
