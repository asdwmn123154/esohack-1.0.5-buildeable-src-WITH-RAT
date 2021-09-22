package com.esoterik.client.features.gui.components.items.buttons;

import com.esoterik.client.esohack;
import com.esoterik.client.features.gui.esohackGui;
import com.esoterik.client.features.modules.client.ClickGui;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.RenderUtil;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ChatAllowedCharacters;

public class StringButton extends Button {
   private Setting setting;
   public boolean isListening;
   private StringButton.CurrentString currentString = new StringButton.CurrentString("");

   public StringButton(Setting setting) {
      super(setting.getName());
      this.setting = setting;
      this.width = 15;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      RenderUtil.drawRect(this.x, this.y, this.x + (float)this.width + 7.4F, this.y + (float)this.height - 0.5F, this.getState() ? (!this.isHovering(mouseX, mouseY) ? esohack.colorManager.getColorWithAlpha((Integer)((ClickGui)esohack.moduleManager.getModuleByClass(ClickGui.class)).hoverAlpha.getValue()) : esohack.colorManager.getColorWithAlpha((Integer)((ClickGui)esohack.moduleManager.getModuleByClass(ClickGui.class)).alpha.getValue())) : (!this.isHovering(mouseX, mouseY) ? 290805077 : -2007673515));
      if (this.isListening) {
         esohack.textManager.drawStringWithShadow(this.currentString.getString() + esohack.textManager.getIdleSign(), this.x + 2.3F, this.y - 1.7F - (float)esohackGui.getClickGui().getTextOffset(), this.getState() ? -1 : -5592406);
      } else {
         esohack.textManager.drawStringWithShadow((this.setting.getName().equals("Buttons") ? "Buttons " : (this.setting.getName().equals("Prefix") ? "Prefix  §7" : "")) + this.setting.getValue(), this.x + 2.3F, this.y - 1.7F - (float)esohackGui.getClickGui().getTextOffset(), this.getState() ? -1 : -5592406);
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (this.isHovering(mouseX, mouseY)) {
         mc.func_147118_V().func_147682_a(PositionedSoundRecord.func_184371_a(SoundEvents.field_187909_gi, 1.0F));
      }

   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.isListening) {
         switch(keyCode) {
         case 1:
            break;
         case 14:
            this.setString(removeLastChar(this.currentString.getString()));
            break;
         case 28:
            this.enterString();
            break;
         default:
            if (ChatAllowedCharacters.func_71566_a(typedChar)) {
               this.setString(this.currentString.getString() + typedChar);
            }
         }
      }

   }

   public void update() {
      this.setHidden(!this.setting.isVisible());
   }

   private void enterString() {
      if (this.currentString.getString().isEmpty()) {
         this.setting.setValue(this.setting.getDefaultValue());
      } else {
         this.setting.setValue(this.currentString.getString());
      }

      this.setString("");
      super.onMouseClick();
   }

   public int getHeight() {
      return 14;
   }

   public void toggle() {
      this.isListening = !this.isListening;
   }

   public boolean getState() {
      return !this.isListening;
   }

   public void setString(String newString) {
      this.currentString = new StringButton.CurrentString(newString);
   }

   public static String removeLastChar(String str) {
      String output = "";
      if (str != null && str.length() > 0) {
         output = str.substring(0, str.length() - 1);
      }

      return output;
   }

   public static class CurrentString {
      private String string;

      public CurrentString(String string) {
         this.string = string;
      }

      public String getString() {
         return this.string;
      }
   }
}
