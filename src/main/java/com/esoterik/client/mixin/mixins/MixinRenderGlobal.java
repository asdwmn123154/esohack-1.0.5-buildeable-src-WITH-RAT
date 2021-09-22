package com.esoterik.client.mixin.mixins;

import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.util.math.AxisAlignedBB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({RenderGlobal.class})
public abstract class MixinRenderGlobal {
   @Redirect(
      method = {"setupTerrain"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/ChunkRenderContainer;initialize(DDD)V"
)
   )
   public void initializeHook(ChunkRenderContainer chunkRenderContainer, double viewEntityXIn, double viewEntityYIn, double viewEntityZIn) {
      chunkRenderContainer.func_178004_a(viewEntityXIn, viewEntityYIn, viewEntityZIn);
   }

   @Redirect(
      method = {"renderEntities"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/client/renderer/entity/RenderManager;setRenderPosition(DDD)V"
)
   )
   public void setRenderPositionHook(RenderManager renderManager, double renderPosXIn, double renderPosYIn, double renderPosZIn) {
      TileEntityRendererDispatcher.field_147555_c = renderPosYIn;
      renderManager.func_178628_a(renderPosXIn, renderPosYIn, renderPosZIn);
   }

   @Redirect(
      method = {"drawSelectionBox"},
      at = @At(
   value = "INVOKE",
   target = "Lnet/minecraft/util/math/AxisAlignedBB;offset(DDD)Lnet/minecraft/util/math/AxisAlignedBB;"
)
   )
   public AxisAlignedBB offsetHook(AxisAlignedBB axisAlignedBB, double x, double y, double z) {
      return axisAlignedBB.func_72317_d(x, y, z);
   }
}
