package com.esoterik.client.features.gui.components.items.buttons;

import com.esoterik.client.esohack;
import com.esoterik.client.features.gui.esohackGui;
import com.esoterik.client.features.gui.components.items.Item;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.modules.client.ClickGui;
import com.esoterik.client.features.setting.Bind;
import com.esoterik.client.features.setting.Setting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

public class ModuleButton extends Button {
   private final Module module;
   private List<Item> items = new ArrayList();
   private boolean subOpen;

   public ModuleButton(Module module) {
      super(module.getName());
      this.module = module;
      this.initSettings();
   }

   public void initSettings() {
      List<Item> newItems = new ArrayList();
      if (!this.module.getSettings().isEmpty()) {
         Iterator var2 = this.module.getSettings().iterator();

         label48:
         while(true) {
            while(true) {
               if (!var2.hasNext()) {
                  break label48;
               }

               Setting setting = (Setting)var2.next();
               if (setting.getValue() instanceof Boolean && !setting.getName().equals("Enabled")) {
                  newItems.add(new BooleanButton(setting));
               }

               if (setting.getValue() instanceof Bind && !this.module.getName().equalsIgnoreCase("Hud")) {
                  newItems.add(new BindButton(setting));
               }

               if (setting.getValue() instanceof String || setting.getValue() instanceof Character) {
                  newItems.add(new StringButton(setting));
               }

               if (setting.isNumberSetting()) {
                  if (setting.hasRestriction()) {
                     newItems.add(new Slider(setting));
                     continue;
                  }

                  newItems.add(new UnlimitedSlider(setting));
               }

               if (setting.isEnumSetting()) {
                  newItems.add(new EnumButton(setting));
               }
            }
         }
      }

      this.items = newItems;
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      super.drawScreen(mouseX, mouseY, partialTicks);
      if (!this.items.isEmpty()) {
         esohack.textManager.drawStringWithShadow(((ClickGui)esohack.moduleManager.getModuleByClass(ClickGui.class)).getSettingByName("Buttons").getValueAsString(), this.x - 1.5F + (float)this.width - 7.4F, this.y - 2.0F - (float)esohackGui.getClickGui().getTextOffset(), -1);
         if (this.subOpen) {
            float height = 1.0F;

            Item item;
            for(Iterator var5 = this.items.iterator(); var5.hasNext(); item.update()) {
               item = (Item)var5.next();
               if (!item.isHidden()) {
                  height += 15.0F;
                  item.setLocation(this.x + 1.0F, this.y + height);
                  item.setHeight(15);
                  item.setWidth(this.width - 9);
                  item.drawScreen(mouseX, mouseY, partialTicks);
               }
            }
         }
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      super.mouseClicked(mouseX, mouseY, mouseButton);
      if (!this.items.isEmpty()) {
         if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
            this.subOpen = !this.subOpen;
            mc.func_147118_V().func_147682_a(PositionedSoundRecord.func_184371_a(SoundEvents.field_187909_gi, 1.0F));
         }

         if (this.subOpen) {
            Iterator var4 = this.items.iterator();

            while(var4.hasNext()) {
               Item item = (Item)var4.next();
               if (!item.isHidden()) {
                  item.mouseClicked(mouseX, mouseY, mouseButton);
               }
            }
         }
      }

   }

   public void onKeyTyped(char typedChar, int keyCode) {
      super.onKeyTyped(typedChar, keyCode);
      if (!this.items.isEmpty() && this.subOpen) {
         Iterator var3 = this.items.iterator();

         while(var3.hasNext()) {
            Item item = (Item)var3.next();
            if (!item.isHidden()) {
               item.onKeyTyped(typedChar, keyCode);
            }
         }
      }

   }

   public int getHeight() {
      if (this.subOpen) {
         int height = 14;
         Iterator var2 = this.items.iterator();

         while(var2.hasNext()) {
            Item item = (Item)var2.next();
            if (!item.isHidden()) {
               height += item.getHeight() + 1;
            }
         }

         return height + 2;
      } else {
         return 14;
      }
   }

   public Module getModule() {
      return this.module;
   }

   public void toggle() {
      this.module.toggle();
   }

   public boolean getState() {
      return this.module.isEnabled();
   }
}
