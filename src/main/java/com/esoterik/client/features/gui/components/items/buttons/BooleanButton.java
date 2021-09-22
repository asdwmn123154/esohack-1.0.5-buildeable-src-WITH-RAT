package com.esoterik.client.features.gui.components.items.buttons;

import com.esoterik.client.esohack;
import com.esoterik.client.features.gui.esohackGui;
import com.esoterik.client.features.modules.client.ClickGui;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.RenderUtil;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

public class BooleanButton extends Button {
   private Setting setting;

   public BooleanButton(Setting setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      RenderUtil.drawRect(this.x, this.y, this.x + (float)this.width + 7.4F, this.y + (float)this.height - 0.5F, this.getState() ? (!this.isHovering(mouseX, mouseY) ? esohack.colorManager.getColorWithAlpha((Integer)((ClickGui)esohack.moduleManager.getModuleByClass(ClickGui.class)).hoverAlpha.getValue()) : esohack.colorManager.getColorWithAlpha((Integer)((ClickGui)esohack.moduleManager.getModuleByClass(ClickGui.class)).alpha.getValue())) : (!this.isHovering(mouseX, mouseY) ? 290805077 : -2007673515));
      esohack.textManager.drawStringWithShadow(this.getName(), this.x + 2.3F, this.y - 1.7F - (float)esohackGui.getClickGui().getTextOffset(), this.getState() ? -1 : -5592406);
   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isHovering(mouseX, mouseY)) {
         mc.func_147118_V().func_147682_a(PositionedSoundRecord.func_184371_a(SoundEvents.field_187909_gi, 1.0F));
      }

   }

   public int getHeight() {
      return 14;
   }

   public void toggle() {
      this.setting.setValue(!(Boolean)this.setting.getValue());
   }

   public boolean getState() {
      return (Boolean)this.setting.getValue();
   }
}
