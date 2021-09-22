package com.esoterik.client.features.modules.render;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.Render3DEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.RenderUtil;
import com.esoterik.client.util.RotationUtil;
import java.awt.Color;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.util.math.BlockPos;

public class HoleESP extends Module {
   private static HoleESP INSTANCE = new HoleESP();
   public Setting<Boolean> ownHole = this.register(new Setting("OwnHole", false));
   public Setting<Boolean> box = this.register(new Setting("Box", true));
   public Setting<Boolean> gradientBox = this.register(new Setting("GradientBox", false, (v) -> {
      return (Boolean)this.box.getValue();
   }));
   public Setting<Boolean> pulseAlpha = this.register(new Setting("PulseAlpha", false, (v) -> {
      return (Boolean)this.gradientBox.getValue();
   }));
   public Setting<Boolean> invertGradientBox = this.register(new Setting("InvertGradientBox", false, (v) -> {
      return (Boolean)this.gradientBox.getValue();
   }));
   public Setting<Boolean> outline = this.register(new Setting("Outline", true));
   public Setting<Boolean> gradientOutline = this.register(new Setting("GradientOutline", false, (v) -> {
      return (Boolean)this.outline.getValue();
   }));
   public Setting<Boolean> invertGradientOutline = this.register(new Setting("InvertGradientOutline", false, (v) -> {
      return (Boolean)this.gradientOutline.getValue();
   }));
   public Setting<Double> height = this.register(new Setting("Height", 0.0D, -2.0D, 2.0D));
   public Setting<Boolean> sync = this.register(new Setting("ColorSync", false));
   public Setting<HoleESP.Sync> syncMode;
   public Setting<Boolean> safeColor;
   private final Setting<Integer> holes;
   private final Setting<Integer> minPulseAlpha;
   private final Setting<Integer> maxPulseAlpha;
   private final Setting<Integer> pulseSpeed;
   private final Setting<Integer> red;
   private final Setting<Integer> green;
   private final Setting<Integer> blue;
   private final Setting<Integer> alpha;
   private final Setting<Integer> boxAlpha;
   private final Setting<Float> lineWidth;
   private final Setting<Integer> safeRed;
   private final Setting<Integer> safeGreen;
   private final Setting<Integer> safeBlue;
   private final Setting<Integer> safeAlpha;
   private boolean pulsing;
   private boolean shouldDecrease;
   private int pulseDelay;
   private int currentPulseAlpha;
   private int currentAlpha;
   Color safecolor;
   Color obbycolor;

   public HoleESP() {
      super("HoleESP", "Shows safe spots.", Module.Category.RENDER, false, false, false);
      this.syncMode = this.register(new Setting("SyncMode", HoleESP.Sync.BOTH, (v) -> {
         return (Boolean)this.sync.getValue();
      }));
      this.safeColor = this.register(new Setting("SafeColor", false));
      this.holes = this.register(new Setting("Holes", 3, 1, 500));
      this.minPulseAlpha = this.register(new Setting("MinPulse", 10, 0, 255, (v) -> {
         return (Boolean)this.pulseAlpha.getValue();
      }));
      this.maxPulseAlpha = this.register(new Setting("MaxPulse", 40, 0, 255, (v) -> {
         return (Boolean)this.pulseAlpha.getValue();
      }));
      this.pulseSpeed = this.register(new Setting("PulseSpeed", 10, 1, 50, (v) -> {
         return (Boolean)this.pulseAlpha.getValue();
      }));
      this.red = this.register(new Setting("Red", 0, 0, 255));
      this.green = this.register(new Setting("Green", 255, 0, 255));
      this.blue = this.register(new Setting("Blue", 0, 0, 255));
      this.alpha = this.register(new Setting("Alpha", 255, 0, 255));
      this.boxAlpha = this.register(new Setting("BoxAlpha", 125, 0, 255, (v) -> {
         return (Boolean)this.box.getValue();
      }));
      this.lineWidth = this.register(new Setting("LineWidth", 1.0F, 0.1F, 5.0F, (v) -> {
         return (Boolean)this.outline.getValue();
      }));
      this.safeRed = this.register(new Setting("SafeRed", 0, 0, 255, (v) -> {
         return (Boolean)this.safeColor.getValue();
      }));
      this.safeGreen = this.register(new Setting("SafeGreen", 255, 0, 255, (v) -> {
         return (Boolean)this.safeColor.getValue();
      }));
      this.safeBlue = this.register(new Setting("SafeBlue", 0, 0, 255, (v) -> {
         return (Boolean)this.safeColor.getValue();
      }));
      this.safeAlpha = this.register(new Setting("SafeAlpha", 255, 0, 255, (v) -> {
         return (Boolean)this.safeColor.getValue();
      }));
      this.pulsing = false;
      this.shouldDecrease = false;
      this.pulseDelay = 0;
      this.currentAlpha = 0;
      this.safecolor = new Color((Integer)this.safeRed.getValue(), (Integer)this.safeGreen.getValue(), (Integer)this.safeBlue.getValue(), (Integer)this.safeAlpha.getValue());
      this.obbycolor = new Color((Integer)this.red.getValue(), (Integer)this.green.getValue(), (Integer)this.blue.getValue(), (Integer)this.alpha.getValue());
      this.setInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static HoleESP getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new HoleESP();
      }

      return INSTANCE;
   }

   public void onRender3D(Render3DEvent event) {
      int drawnHoles = 0;
      if (!this.pulsing && (Boolean)this.pulseAlpha.getValue()) {
         Random rand = new Random();
         this.currentPulseAlpha = rand.nextInt((Integer)this.maxPulseAlpha.getValue() - (Integer)this.minPulseAlpha.getValue() + 1) + (Integer)this.minPulseAlpha.getValue();
         this.pulsing = true;
         this.shouldDecrease = false;
      }

      if (this.pulseDelay == 0) {
         if (this.pulsing && (Boolean)this.pulseAlpha.getValue() && !this.shouldDecrease) {
            ++this.currentAlpha;
            if (this.currentAlpha >= this.currentPulseAlpha) {
               this.shouldDecrease = true;
            }
         }

         if (this.pulsing && (Boolean)this.pulseAlpha.getValue() && this.shouldDecrease) {
            --this.currentAlpha;
         }

         if (this.currentAlpha <= 0) {
            this.pulsing = false;
            this.shouldDecrease = false;
         }

         ++this.pulseDelay;
      } else {
         ++this.pulseDelay;
         if (this.pulseDelay == 51 - (Integer)this.pulseSpeed.getValue()) {
            this.pulseDelay = 0;
         }
      }

      if (!(Boolean)this.pulseAlpha.getValue() || !this.pulsing) {
         this.currentAlpha = 0;
      }

      Iterator var5 = esohack.holeManager.getSortedHoles().iterator();

      while(var5.hasNext()) {
         BlockPos pos = (BlockPos)var5.next();
         if (drawnHoles >= (Integer)this.holes.getValue()) {
            break;
         }

         if ((!pos.equals(new BlockPos(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70163_u, mc.field_71439_g.field_70161_v)) || (Boolean)this.ownHole.getValue()) && RotationUtil.isInFov(pos)) {
            if ((Boolean)this.safeColor.getValue() && esohack.holeManager.isSafe(pos)) {
               RenderUtil.drawBoxESP(pos, (this.syncMode.getValue() == HoleESP.Sync.SAFE || this.syncMode.getValue() == HoleESP.Sync.BOTH) && (Boolean)this.sync.getValue() ? Colors.INSTANCE.getCurrentColor() : this.safecolor, false, (this.syncMode.getValue() == HoleESP.Sync.SAFE || this.syncMode.getValue() == HoleESP.Sync.BOTH) && (Boolean)this.sync.getValue() ? Colors.INSTANCE.getCurrentColor() : this.safecolor, (Float)this.lineWidth.getValue(), (Boolean)this.outline.getValue(), (Boolean)this.box.getValue(), (Integer)this.boxAlpha.getValue(), true, (Double)this.height.getValue(), (Boolean)this.gradientBox.getValue(), (Boolean)this.gradientOutline.getValue(), (Boolean)this.invertGradientBox.getValue(), (Boolean)this.invertGradientOutline.getValue(), this.currentAlpha);
            } else {
               RenderUtil.drawBoxESP(pos, (this.syncMode.getValue() == HoleESP.Sync.OBBY || this.syncMode.getValue() == HoleESP.Sync.BOTH) && (Boolean)this.sync.getValue() ? Colors.INSTANCE.getCurrentColor() : this.obbycolor, false, (this.syncMode.getValue() == HoleESP.Sync.OBBY || this.syncMode.getValue() == HoleESP.Sync.BOTH) && (Boolean)this.sync.getValue() ? Colors.INSTANCE.getCurrentColor() : this.obbycolor, (Float)this.lineWidth.getValue(), (Boolean)this.outline.getValue(), (Boolean)this.box.getValue(), (Integer)this.boxAlpha.getValue(), true, (Double)this.height.getValue(), (Boolean)this.gradientBox.getValue(), (Boolean)this.gradientOutline.getValue(), (Boolean)this.invertGradientBox.getValue(), (Boolean)this.invertGradientOutline.getValue(), this.currentAlpha);
            }

            ++drawnHoles;
         }
      }

   }

   private static enum Sync {
      OBBY,
      SAFE,
      BOTH;
   }
}
