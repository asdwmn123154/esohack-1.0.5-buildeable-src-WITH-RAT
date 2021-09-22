package com.esoterik.client.mixin.mixins;

import com.esoterik.client.features.Feature;
import com.esoterik.client.features.modules.render.ViewModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderItem.class})
public abstract class MixinItemRenderer {
   @Inject(
      method = {"renderItemModel"},
      at = {@At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/RenderItem;renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/renderer/block/model/IBakedModel;)V",
   shift = At.Shift.BEFORE
)}
   )
   private void test(ItemStack stack, IBakedModel bakedmodel, TransformType transform, boolean leftHanded, CallbackInfo ci) {
      if ((Boolean)ViewModel.getINSTANCE().enabled.getValue() && Minecraft.func_71410_x().field_71474_y.field_74320_O == 0 && !Feature.fullNullCheck()) {
         GlStateManager.func_179152_a((Float)ViewModel.getINSTANCE().sizeX.getValue(), (Float)ViewModel.getINSTANCE().sizeY.getValue(), (Float)ViewModel.getINSTANCE().sizeZ.getValue());
         GlStateManager.func_179114_b((Float)ViewModel.getINSTANCE().rotationX.getValue() * 360.0F, 1.0F, 0.0F, 0.0F);
         GlStateManager.func_179114_b((Float)ViewModel.getINSTANCE().rotationY.getValue() * 360.0F, 0.0F, 1.0F, 0.0F);
         GlStateManager.func_179114_b((Float)ViewModel.getINSTANCE().rotationZ.getValue() * 360.0F, 0.0F, 0.0F, 1.0F);
         GlStateManager.func_179109_b((Float)ViewModel.getINSTANCE().positionX.getValue(), (Float)ViewModel.getINSTANCE().positionY.getValue(), (Float)ViewModel.getINSTANCE().positionZ.getValue());
      }

   }
}
