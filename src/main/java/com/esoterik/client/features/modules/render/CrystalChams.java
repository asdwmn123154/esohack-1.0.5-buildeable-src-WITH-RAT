package com.esoterik.client.features.modules.render;

import com.esoterik.client.event.events.RenderEntityModelEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.EntityUtil;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.item.EntityEnderCrystal;
import org.lwjgl.opengl.GL11;

public class CrystalChams extends Module {
   public Setting<Boolean> colorSync = this.register(new Setting("ColorSync", false));
   public Setting<Boolean> rainbow = this.register(new Setting("Rainbow", false));
   public Setting<Boolean> throughWalls = this.register(new Setting("ThroughWalls", true));
   public Setting<Boolean> wireframe = this.register(new Setting("Outline", false));
   public Setting<Float> lineWidth = this.register(new Setting("OutlineWidth", 1.0F, 0.1F, 3.0F));
   public Setting<Boolean> wireframeThroughWalls = this.register(new Setting("OutlineThroughWalls", true));
   public Setting<Integer> speed = this.register(new Setting("Speed", 40, 1, 100, (v) -> {
      return (Boolean)this.rainbow.getValue();
   }));
   public Setting<Boolean> xqz = this.register(new Setting("XQZ", false, (v) -> {
      return !(Boolean)this.rainbow.getValue() && (Boolean)this.throughWalls.getValue();
   }));
   public Setting<Integer> saturation = this.register(new Setting("Saturation", 50, 0, 100, (v) -> {
      return (Boolean)this.rainbow.getValue();
   }));
   public Setting<Integer> brightness = this.register(new Setting("Brightness", 100, 0, 100, (v) -> {
      return (Boolean)this.rainbow.getValue();
   }));
   public Setting<Integer> red = this.register(new Setting("Red", 0, 0, 255, (v) -> {
      return !(Boolean)this.rainbow.getValue();
   }));
   public Setting<Integer> green = this.register(new Setting("Green", 255, 0, 255, (v) -> {
      return !(Boolean)this.rainbow.getValue();
   }));
   public Setting<Integer> blue = this.register(new Setting("Blue", 0, 0, 255, (v) -> {
      return !(Boolean)this.rainbow.getValue();
   }));
   public Setting<Integer> alpha = this.register(new Setting("Alpha", 255, 0, 255));
   public Map<EntityEnderCrystal, Float> scaleMap = new ConcurrentHashMap();
   public Setting<Integer> hiddenRed = this.register(new Setting("Hidden Red", 255, 0, 255, (v) -> {
      return (Boolean)this.xqz.getValue() && !(Boolean)this.rainbow.getValue();
   }));
   public Setting<Integer> hiddenGreen = this.register(new Setting("Hidden Green", 0, 0, 255, (v) -> {
      return (Boolean)this.xqz.getValue() && !(Boolean)this.rainbow.getValue();
   }));
   public Setting<Integer> hiddenBlue = this.register(new Setting("Hidden Blue", 255, 0, 255, (v) -> {
      return (Boolean)this.xqz.getValue() && !(Boolean)this.rainbow.getValue();
   }));
   public Setting<Integer> hiddenAlpha = this.register(new Setting("Hidden Alpha", 255, 0, 255, (v) -> {
      return (Boolean)this.xqz.getValue() && !(Boolean)this.rainbow.getValue();
   }));
   public Setting<Float> scale = this.register(new Setting("Scale", 1.0F, 0.1F, 2.0F));
   public static CrystalChams INSTANCE = new CrystalChams();

   public CrystalChams() {
      super("CrystalChams", "Renders players through walls.", Module.Category.RENDER, false, false, false);
      this.setInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static CrystalChams getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new CrystalChams();
      }

      return INSTANCE;
   }

   public void onRenderModel(RenderEntityModelEvent event) {
      if (event.getStage() == 0 && event.entity instanceof EntityEnderCrystal && (Boolean)this.wireframe.getValue()) {
         Color color = (Boolean)this.colorSync.getValue() ? Colors.INSTANCE.getCurrentColor() : EntityUtil.getColor(event.entity, (Integer)this.red.getValue(), (Integer)this.green.getValue(), (Integer)this.blue.getValue(), (Integer)this.alpha.getValue(), false);
         boolean fancyGraphics = mc.field_71474_y.field_74347_j;
         mc.field_71474_y.field_74347_j = false;
         float gamma = mc.field_71474_y.field_74333_Y;
         mc.field_71474_y.field_74333_Y = 10000.0F;
         GL11.glPushMatrix();
         GL11.glPushAttrib(1048575);
         GL11.glPolygonMode(1032, 6913);
         GL11.glDisable(3553);
         GL11.glDisable(2896);
         if ((Boolean)this.wireframeThroughWalls.getValue()) {
            GL11.glDisable(2929);
         }

         GL11.glEnable(2848);
         GL11.glEnable(3042);
         GlStateManager.func_179112_b(770, 771);
         GlStateManager.func_179131_c((float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F);
         GlStateManager.func_187441_d((Float)this.lineWidth.getValue());
         event.modelBase.func_78088_a(event.entity, event.limbSwing, event.limbSwingAmount, event.age, event.headYaw, event.headPitch, event.scale);
         GL11.glPopAttrib();
         GL11.glPopMatrix();
      }
   }
}
