package com.esoterik.client.features.modules;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.ClientEvent;
import com.esoterik.client.event.events.Render2DEvent;
import com.esoterik.client.event.events.Render3DEvent;
import com.esoterik.client.features.Feature;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.setting.Bind;
import com.esoterik.client.features.setting.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;

public class Module extends Feature {
   private final String description;
   private final Module.Category category;
   public Setting<Boolean> enabled = this.register(new Setting("Enabled", false));
   public Setting<Boolean> drawn = this.register(new Setting("Drawn", true));
   public Setting<Bind> bind = this.register(new Setting("Bind", new Bind(-1)));
   public Setting<String> displayName;
   public boolean hasListener;
   public boolean alwaysListening;
   public boolean hidden;

   public Module(String name, String description, Module.Category category, boolean hasListener, boolean hidden, boolean alwaysListening) {
      super(name);
      this.displayName = this.register(new Setting("DisplayName", name));
      this.description = description;
      this.category = category;
      this.hasListener = hasListener;
      this.hidden = hidden;
      this.alwaysListening = alwaysListening;
   }

   public void onEnable() {
      Command.sendMessage("§a" + this.getDisplayName() + " enabled.");
   }

   public void onDisable() {
      Command.sendMessage("§c" + this.getDisplayName() + " disabled.");
   }

   public void onToggle() {
   }

   public void onLoad() {
   }

   public void onTick() {
   }

   public void onLogin() {
   }

   public void onLogout() {
   }

   public void onUpdate() {
   }

   public void onRender2D(Render2DEvent event) {
   }

   public void onRender3D(Render3DEvent event) {
   }

   public void onUnload() {
   }

   public void onServerUpdate() {
   }

   public String getDisplayInfo() {
      return null;
   }

   public boolean isOn() {
      return (Boolean)this.enabled.getValue();
   }

   public boolean isOff() {
      return !(Boolean)this.enabled.getValue();
   }

   public void setEnabled(boolean enabled) {
      if (enabled) {
         this.enable();
      } else {
         this.disable();
      }

   }

   public void enable() {
      this.enabled.setValue(true);
      this.onToggle();
      this.onEnable();
      if (this.isOn() && this.hasListener && !this.alwaysListening) {
         MinecraftForge.EVENT_BUS.register(this);
      }

   }

   public void disable() {
      if (this.hasListener && !this.alwaysListening) {
         MinecraftForge.EVENT_BUS.unregister(this);
      }

      this.enabled.setValue(false);
      this.onToggle();
      this.onDisable();
   }

   public void toggle() {
      ClientEvent event = new ClientEvent(!this.isEnabled() ? 1 : 0, this);
      MinecraftForge.EVENT_BUS.post(event);
      if (!event.isCanceled()) {
         this.setEnabled(!this.isEnabled());
      }

   }

   public String getDisplayName() {
      return (String)this.displayName.getValue();
   }

   public String getDescription() {
      return this.description;
   }

   public void setDisplayName(String name) {
      Module module = esohack.moduleManager.getModuleByDisplayName(name);
      Module originalModule = esohack.moduleManager.getModuleByName(name);
      if (module == null && originalModule == null) {
         Command.sendMessage(this.getDisplayName() + ", Original name: " + this.getName() + ", has been renamed to: " + name);
         this.displayName.setValue(name);
      } else {
         Command.sendMessage("§cA module of this name already exists.");
      }
   }

   public boolean isDrawn() {
      return (Boolean)this.drawn.getValue();
   }

   public void setDrawn(boolean drawn) {
      this.drawn.setValue(drawn);
   }

   public Module.Category getCategory() {
      return this.category;
   }

   public String getInfo() {
      return null;
   }

   public Bind getBind() {
      return (Bind)this.bind.getValue();
   }

   public void setBind(int key) {
      this.bind.setValue(new Bind(key));
   }

   public boolean listening() {
      return this.hasListener && this.isOn() || this.alwaysListening;
   }

   public Vec3d process(Entity entity, double x, double y, double z) {
      return new Vec3d((entity.field_70165_t - entity.field_70142_S) * x, (entity.field_70163_u - entity.field_70137_T) * y, (entity.field_70161_v - entity.field_70136_U) * z);
   }

   public String getFullArrayString() {
      return this.getDisplayName() + "§8" + (this.getDisplayInfo() != null ? " [§r" + this.getDisplayInfo() + "§8" + "]" : "");
   }

   public static enum Category {
      COMBAT("Combat"),
      MISC("Misc"),
      RENDER("Render"),
      MOVEMENT("Movement"),
      PLAYER("Player"),
      CLIENT("Client");

      private final String name;

      private Category(String name) {
         this.name = name;
      }

      public String getName() {
         return this.name;
      }
   }
}
