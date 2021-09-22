package com.esoterik.client.features.modules.render;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.Render3DEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.RenderUtil;
import com.esoterik.client.util.Timer;
import java.awt.Color;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class BreakESP extends Module {
   public Setting<Boolean> box = this.register(new Setting("Box", false));
   public Setting<Boolean> outline = this.register(new Setting("Outline", true));
   private final Setting<Integer> boxAlpha = this.register(new Setting("BoxAlpha", 85, 0, 255));
   private final Setting<Float> lineWidth = this.register(new Setting("LineWidth", 1.0F, 0.1F, 5.0F));
   private BlockPos lastPos = null;
   public BlockPos currentPos;
   public IBlockState currentBlockState;
   private final Timer timer = new Timer();

   public BreakESP() {
      super("BreakESP", "Highlights blocks you mine", Module.Category.RENDER, true, false, false);
   }

   public void onTick() {
      if (this.currentPos != null && (!mc.field_71441_e.func_180495_p(this.currentPos).equals(this.currentBlockState) || mc.field_71441_e.func_180495_p(this.currentPos).func_177230_c() == Blocks.field_150350_a)) {
         this.currentPos = null;
         this.currentBlockState = null;
      }

   }

   public void onRender3D(Render3DEvent event) {
      if (this.currentPos != null) {
         Color color = new Color(255, 255, 255, 255);
         Color readyColor = Colors.INSTANCE.isEnabled() ? Colors.INSTANCE.getCurrentColor() : new Color(125, 105, 255, 255);
         RenderUtil.drawBoxESP(this.currentPos, this.timer.passedMs((long)((int)(2000.0F * esohack.serverManager.getTpsFactor()))) ? readyColor : color, false, color, (Float)this.lineWidth.getValue(), (Boolean)this.outline.getValue(), (Boolean)this.box.getValue(), (Integer)this.boxAlpha.getValue(), false);
      }

   }
}
