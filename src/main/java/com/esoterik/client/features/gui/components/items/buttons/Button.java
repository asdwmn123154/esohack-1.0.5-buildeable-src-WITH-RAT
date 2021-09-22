package com.esoterik.client.features.gui.components.items.buttons;

import com.esoterik.client.esohack;
import com.esoterik.client.features.gui.esohackGui;
import com.esoterik.client.features.gui.components.Component;
import com.esoterik.client.features.gui.components.items.Item;
import com.esoterik.client.features.modules.client.ClickGui;
import com.esoterik.client.util.RenderUtil;
import java.util.Iterator;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

public class Button extends Item {
   private boolean state;

   public Button(String name) {
      super(name);
      this.height = 15;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      RenderUtil.drawRect(this.x, this.y, this.x + (float)this.width, this.y + (float)this.height - 0.5F, this.getState() ? (!this.isHovering(mouseX, mouseY) ? esohack.colorManager.getColorWithAlpha((Integer)((ClickGui)esohack.moduleManager.getModuleByClass(ClickGui.class)).hoverAlpha.getValue()) : esohack.colorManager.getColorWithAlpha((Integer)((ClickGui)esohack.moduleManager.getModuleByClass(ClickGui.class)).alpha.getValue())) : (!this.isHovering(mouseX, mouseY) ? 290805077 : -2007673515));
      esohack.textManager.drawStringWithShadow(this.getName(), this.x + 2.3F, this.y - 2.0F - (float)esohackGui.getClickGui().getTextOffset(), this.getState() ? -1 : -5592406);
   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.onMouseClick();
      }

   }

   public void onMouseClick() {
      this.state = !this.state;
      this.toggle();
      mc.func_147118_V().func_147682_a(PositionedSoundRecord.func_184371_a(SoundEvents.field_187909_gi, 1.0F));
   }

   public void toggle() {
   }

   public boolean getState() {
      return this.state;
   }

   public int getHeight() {
      return 14;
   }

   public boolean isHovering(int mouseX, int mouseY) {
      Iterator var3 = esohackGui.getClickGui().getComponents().iterator();

      while(var3.hasNext()) {
         Component component = (Component)var3.next();
         if (component.drag) {
            return false;
         }
      }

      return (float)mouseX >= this.getX() && (float)mouseX <= this.getX() + (float)this.getWidth() && (float)mouseY >= this.getY() && (float)mouseY <= this.getY() + (float)this.height;
   }
}
