package com.esoterik.client.manager;

import com.esoterik.client.event.events.Render2DEvent;
import com.esoterik.client.event.events.Render3DEvent;
import com.esoterik.client.features.Feature;
import com.esoterik.client.features.gui.esohackGui;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.modules.client.ClickGui;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.features.modules.client.Components;
import com.esoterik.client.features.modules.client.Gondal;
import com.esoterik.client.features.modules.client.HUD;
import com.esoterik.client.features.modules.client.Managers;
import com.esoterik.client.features.modules.client.Notifications;
import com.esoterik.client.features.modules.combat.AutoArmor;
import com.esoterik.client.features.modules.combat.AutoGondal;
import com.esoterik.client.features.modules.combat.AutoTrap;
import com.esoterik.client.features.modules.combat.Criticals;
import com.esoterik.client.features.modules.combat.HoleFiller;
import com.esoterik.client.features.modules.combat.Offhand;
import com.esoterik.client.features.modules.combat.Surround;
import com.esoterik.client.features.modules.misc.ChatModifier;
import com.esoterik.client.features.modules.misc.DonkeyNotifier;
import com.esoterik.client.features.modules.misc.RPC;
import com.esoterik.client.features.modules.misc.Spammer;
import com.esoterik.client.features.modules.movement.NoSlowDown;
import com.esoterik.client.features.modules.movement.ReverseStep;
import com.esoterik.client.features.modules.movement.Speed;
import com.esoterik.client.features.modules.movement.Sprint;
import com.esoterik.client.features.modules.movement.Step;
import com.esoterik.client.features.modules.movement.Velocity;
import com.esoterik.client.features.modules.player.Burrow;
import com.esoterik.client.features.modules.player.FakePlayer;
import com.esoterik.client.features.modules.player.FastPlace;
import com.esoterik.client.features.modules.player.MCP;
import com.esoterik.client.features.modules.player.Replenish;
import com.esoterik.client.features.modules.player.Speedmine;
import com.esoterik.client.features.modules.render.CameraClip;
import com.esoterik.client.features.modules.render.CrystalChams;
import com.esoterik.client.features.modules.render.ESP;
import com.esoterik.client.features.modules.render.Fullbright;
import com.esoterik.client.features.modules.render.HoleESP;
import com.esoterik.client.features.modules.render.Nametags;
import com.esoterik.client.features.modules.render.NoRender;
import com.esoterik.client.features.modules.render.ViewModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import org.lwjgl.input.Keyboard;

public class ModuleManager extends Feature {
   public static ArrayList<Module> modules = new ArrayList();
   public List<Module> sortedModules = new ArrayList();
   boolean hasRun = false;

   public void init() {
      modules.add(new AutoTrap());
      modules.add(new Offhand());
      modules.add(new Criticals());
      modules.add(new AutoArmor());
      modules.add(new Surround());
      modules.add(new HoleFiller());
      modules.add(new AutoGondal());
      modules.add(new ChatModifier());
      modules.add(new Spammer());
      modules.add(new DonkeyNotifier());
      modules.add(new Velocity());
      modules.add(new Step());
      modules.add(new ReverseStep());
      modules.add(new Sprint());
      modules.add(new NoSlowDown());
      modules.add(new Speed());
      modules.add(new FakePlayer());
      modules.add(new Speedmine());
      modules.add(new Replenish());
      modules.add(new MCP());
      modules.add(new FastPlace());
      modules.add(new Burrow());
      modules.add(new NoRender());
      modules.add(new Fullbright());
      modules.add(new Nametags());
      modules.add(new CameraClip());
      modules.add(new ViewModel());
      modules.add(new ESP());
      modules.add(new HoleESP());
      modules.add(new CrystalChams());
      modules.add(new Gondal());
      modules.add(new Colors());
      modules.add(new Notifications());
      modules.add(new HUD());
      modules.add(new ClickGui());
      modules.add(new Managers());
      modules.add(new Components());
      modules.add(new RPC());
   }

   public Module getModuleByName(String name) {
      Iterator var2 = modules.iterator();

      Module module;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         module = (Module)var2.next();
      } while(!module.getName().equalsIgnoreCase(name));

      return module;
   }

   public <T extends Module> T getModuleByClass(Class<T> clazz) {
      Iterator var2 = modules.iterator();

      Module module;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         module = (Module)var2.next();
      } while(!clazz.isInstance(module));

      return module;
   }

   public void enableModule(Class clazz) {
      Module module = this.getModuleByClass(clazz);
      if (module != null) {
         module.enable();
      }

   }

   public void disableModule(Class clazz) {
      Module module = this.getModuleByClass(clazz);
      if (module != null) {
         module.disable();
      }

   }

   public void enableModule(String name) {
      Module module = this.getModuleByName(name);
      if (module != null) {
         module.enable();
      }

   }

   public void disableModule(String name) {
      Module module = this.getModuleByName(name);
      if (module != null) {
         module.disable();
      }

   }

   public boolean isModuleEnabled(String name) {
      Module module = this.getModuleByName(name);
      return module != null && module.isOn();
   }

   public boolean isModuleEnabled(Class clazz) {
      Module module = this.getModuleByClass(clazz);
      return module != null && module.isOn();
   }

   public Module getModuleByDisplayName(String displayName) {
      Iterator var2 = modules.iterator();

      Module module;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         module = (Module)var2.next();
      } while(!module.getDisplayName().equalsIgnoreCase(displayName));

      return module;
   }

   public ArrayList<Module> getEnabledModules() {
      ArrayList<Module> enabledModules = new ArrayList();
      Iterator var2 = modules.iterator();

      while(var2.hasNext()) {
         Module module = (Module)var2.next();
         if (module.isEnabled()) {
            enabledModules.add(module);
         }
      }

      return enabledModules;
   }

   public ArrayList<Module> getModulesByCategory(Module.Category category) {
      ArrayList<Module> modulesCategory = new ArrayList();
      modules.forEach((module) -> {
         if (module.getCategory() == category) {
            modulesCategory.add(module);
         }

      });
      return modulesCategory;
   }

   public List<Module.Category> getCategories() {
      return Arrays.asList(Module.Category.values());
   }

   public void onLoad() {
      Stream var10000 = modules.stream().filter(Module::listening);
      EventBus var10001 = MinecraftForge.EVENT_BUS;
      var10000.forEach(var10001::register);
      modules.forEach(Module::onLoad);
   }

   public void onUpdate() {
      modules.stream().filter(Feature::isEnabled).forEach(Module::onUpdate);
   }

   public void onTick() {
      modules.stream().filter(Feature::isEnabled).forEach(Module::onTick);
   }

   public void onRender2D(Render2DEvent event) {
      modules.stream().filter(Feature::isEnabled).forEach((module) -> {
         module.onRender2D(event);
      });
   }

   public void onRender3D(Render3DEvent event) {
      modules.stream().filter(Feature::isEnabled).forEach((module) -> {
         module.onRender3D(event);
      });
   }

   public void sortModules(boolean reverse) {
      this.sortedModules = (List)this.getEnabledModules().stream().filter(Module::isDrawn).sorted(Comparator.comparing((module) -> {
         return this.renderer.getStringWidth(module.getFullArrayString()) * (reverse ? -1 : 1);
      })).collect(Collectors.toList());
   }

   public void onLogout() {
      modules.forEach(Module::onLogout);
   }

   public void onLogin() {
      modules.forEach(Module::onLogin);
   }

   public void onUnload() {
      EventBus var10001 = MinecraftForge.EVENT_BUS;
      modules.forEach(var10001::unregister);
      modules.forEach(Module::onUnload);
   }

   public void onUnloadPost() {
      Iterator var1 = modules.iterator();

      while(var1.hasNext()) {
         Module module = (Module)var1.next();
         module.enabled.setValue(false);
      }

   }

   public void onKeyPressed(int eventKey) {
      if (eventKey != 0 && Keyboard.getEventKeyState() && !(mc.field_71462_r instanceof esohackGui)) {
         modules.forEach((module) -> {
            if (module.getBind().getKey() == eventKey) {
               module.toggle();
            }

         });
      }
   }

   public static void onServerUpdate() {
      modules.stream().filter(Feature::isEnabled).forEach(Module::onServerUpdate);
   }
}
