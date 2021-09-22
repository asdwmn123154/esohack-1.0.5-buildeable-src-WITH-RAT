package com.esoterik.client.features.gui.components;

import com.esoterik.client.esohack;
import com.esoterik.client.features.Feature;
import com.esoterik.client.features.gui.esohackGui;
import com.esoterik.client.features.gui.components.items.Item;
import com.esoterik.client.features.gui.components.items.buttons.Button;
import com.esoterik.client.features.modules.client.ClickGui;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.util.ColorUtil;
import com.esoterik.client.util.RenderUtil;
import java.util.ArrayList;
import java.util.Iterator;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.init.SoundEvents;

public class Component extends Feature {
   private int x;
   private int y;
   private int x2;
   private int y2;
   private int width;
   private int height;
   private boolean open;
   public boolean drag;
   private final ArrayList<Item> items = new ArrayList();
   private boolean hidden = false;

   public Component(String name, int x, int y, boolean open) {
      super(name);
      this.x = x;
      this.y = y;
      this.width = 88;
      this.height = 18;
      this.open = open;
      this.setupItems();
   }

   public void setupItems() {
   }

   private void drag(int mouseX, int mouseY) {
      if (this.drag) {
         this.x = this.x2 + mouseX;
         this.y = this.y2 + mouseY;
      }
   }

   public void drawScreen(int mouseX, int mouseY, float partialTicks) {
      this.drag(mouseX, mouseY);
      float totalItemHeight = this.open ? this.getTotalItemHeight() - 2.0F : 0.0F;
      int color = ColorUtil.toARGB(0, 0, 0, 255);
      RenderUtil.drawRect((float)this.x, (float)this.y - 1.5F, (float)(this.x + this.width), (float)(this.y + this.height - 6), color);
      RenderUtil.drawRect((float)this.x, (float)this.y + 11.0F, (float)(this.x + this.width), (float)(this.y + this.height - 6), (Boolean)ClickGui.getInstance().colorSync.getValue() ? Colors.INSTANCE.getCurrentColor().getRGB() : ClickGui.getInstance().getColor().getRGB());
      if (this.open) {
         RenderUtil.drawRect((float)this.x, (float)this.y + 12.5F, (float)(this.x + this.width), (float)(this.y + this.height) + totalItemHeight, ColorUtil.toARGB(10, 10, 10, (Integer)ClickGui.getInstance().backgroundAlpha.getValue()));
      }

      esohack.textManager.drawStringWithShadow(this.getName(), (float)(this.x + this.width / 2) - (float)this.renderer.getStringWidth(this.getName()) / 2.0F, (float)this.y - 4.5F - (float)esohackGui.getClickGui().getTextOffset(), -1);
      if (this.open) {
         float y = (float)(this.getY() + this.getHeight()) - 3.0F;
         Iterator var7 = this.getItems().iterator();

         while(var7.hasNext()) {
            Item item = (Item)var7.next();
            if (!item.isHidden()) {
               item.setLocation((float)this.x + 2.0F, y);
               item.setWidth(this.getWidth() - 4);
               item.drawScreen(mouseX, mouseY, partialTicks);
               y += (float)item.getHeight() + 1.5F;
            }
         }
      }

   }

   public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
      if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
         this.x2 = this.x - mouseX;
         this.y2 = this.y - mouseY;
         esohackGui.getClickGui().getComponents().forEach((component) -> {
            if (component.drag) {
               component.drag = false;
            }

         });
         this.drag = true;
      } else if (mouseButton == 1 && this.isHovering(mouseX, mouseY)) {
         this.open = !this.open;
         mc.func_147118_V().func_147682_a(PositionedSoundRecord.func_184371_a(SoundEvents.field_187909_gi, 1.0F));
      } else if (this.open) {
         this.getItems().forEach((item) -> {
            item.mouseClicked(mouseX, mouseY, mouseButton);
         });
      }
   }

   public void mouseReleased(int mouseX, int mouseY, int releaseButton) {
      if (releaseButton == 0) {
         this.drag = false;
      }

      if (this.open) {
         this.getItems().forEach((item) -> {
            item.mouseReleased(mouseX, mouseY, releaseButton);
         });
      }
   }

   public void onKeyTyped(char typedChar, int keyCode) {
      if (this.open) {
         this.getItems().forEach((item) -> {
            item.onKeyTyped(typedChar, keyCode);
         });
      }
   }

   public void addButton(Button button) {
      this.items.add(button);
   }

   public void setX(int x) {
      this.x = x;
   }

   public void setY(int y) {
      this.y = y;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public void setHeight(int height) {
      this.height = height;
   }

   public void setWidth(int width) {
      this.width = width;
   }

   public void setHidden(boolean hidden) {
      this.hidden = hidden;
   }

   public boolean isHidden() {
      return this.hidden;
   }

   public boolean isOpen() {
      return this.open;
   }

   public final ArrayList<Item> getItems() {
      return this.items;
   }

   private boolean isHovering(int mouseX, int mouseY) {
      return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth() && mouseY >= this.getY() && mouseY <= this.getY() + this.getHeight() - (this.open ? 2 : 0);
   }

   private float getTotalItemHeight() {
      float height = 0.0F;

      Item item;
      for(Iterator var2 = this.getItems().iterator(); var2.hasNext(); height += (float)item.getHeight() + 1.5F) {
         item = (Item)var2.next();
      }

      return height;
   }
}
