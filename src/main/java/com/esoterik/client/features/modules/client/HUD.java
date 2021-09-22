package com.esoterik.client.features.modules.client;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.Render2DEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.ColorUtil;
import com.esoterik.client.util.EntityUtil;
import com.esoterik.client.util.MathUtil;
import com.esoterik.client.util.RenderUtil;
import com.esoterik.client.util.Timer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HUD extends Module {
   public Setting<Boolean> colorSync = this.register(new Setting("ColorSync", false));
   public Setting<Boolean> rainbow = this.register(new Setting("Rainbow", false, (v) -> {
      return !(Boolean)this.colorSync.getValue();
   }));
   public Setting<Integer> rainbowSpeed = this.register(new Setting("Speed", 70, 0, 400, (v) -> {
      return (Boolean)this.rainbow.getValue() && !(Boolean)this.colorSync.getValue();
   }));
   public Setting<Boolean> potionIcons = this.register(new Setting("RemovePotionIcons", true, "Draws Potion Icons."));
   private final Setting<Boolean> watermark = this.register(new Setting("Watermark", false, "WaterMark"));
   private final Setting<Boolean> arrayList = this.register(new Setting("ArrayList", false, "Lists the active modules."));
   private final Setting<Boolean> serverBrand = this.register(new Setting("ServerBrand", false, "Brand of the server you are on."));
   private final Setting<Boolean> ping = this.register(new Setting("Ping", false, "Your response time to the server."));
   private final Setting<Boolean> tps = this.register(new Setting("TPS", false, "Ticks per second of the server."));
   private final Setting<Boolean> fps = this.register(new Setting("FPS", false, "Your frames per second."));
   private final Setting<Boolean> coords = this.register(new Setting("Coords", false, "Your current coordinates"));
   private final Setting<Boolean> direction = this.register(new Setting("Direction", false, "The Direction you are facing."));
   private final Setting<Boolean> speed = this.register(new Setting("Speed", false, "Your Speed"));
   private final Setting<Boolean> potions = this.register(new Setting("Potions", false, "Your Speed"));
   public Setting<Boolean> textRadar = this.register(new Setting("TextRadar", false, "A TextRadar"));
   private final Setting<Boolean> armor = this.register(new Setting("Armor", false, "ArmorHUD"));
   private final Setting<Boolean> percent = this.register(new Setting("Percent", false, (v) -> {
      return (Boolean)this.armor.getValue();
   }));
   private final Setting<Boolean> totems = this.register(new Setting("Totems", false, "TotemHUD"));
   private final Setting<HUD.Greeter> greeter;
   public Setting<Boolean> time;
   public Setting<Integer> hudRed;
   public Setting<Integer> hudGreen;
   public Setting<Integer> hudBlue;
   private static HUD INSTANCE = new HUD();
   private Map<String, Integer> players;
   private static final ResourceLocation box = new ResourceLocation("textures/gui/container/shulker_box.png");
   private static final ItemStack totem;
   private int color;
   private boolean shouldIncrement;
   private int hitMarkerTimer;
   private final Timer timer;
   private boolean shadow;

   public HUD() {
      super("HUD", "HUD Elements rendered on your screen", Module.Category.CLIENT, true, false, false);
      this.greeter = this.register(new Setting("Greeter", HUD.Greeter.NONE, "Greets you."));
      this.time = this.register(new Setting("Time", false, "The time"));
      this.hudRed = this.register(new Setting("Red", 255, 0, 255));
      this.hudGreen = this.register(new Setting("Green", 0, 0, 255));
      this.hudBlue = this.register(new Setting("Blue", 0, 0, 255));
      this.players = new HashMap();
      this.timer = new Timer();
      this.shadow = true;
      this.setInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static HUD getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new HUD();
      }

      return INSTANCE;
   }

   public void onUpdate() {
      if (this.timer.passedMs((long)(Integer)Managers.getInstance().textRadarUpdates.getValue())) {
         this.players = this.getTextRadarPlayers();
         this.timer.reset();
      }

      if (this.shouldIncrement) {
         ++this.hitMarkerTimer;
      }

      if (this.hitMarkerTimer == 10) {
         this.hitMarkerTimer = 0;
         this.shouldIncrement = false;
      }

   }

   public void onRender2D(Render2DEvent event) {
      if (!fullNullCheck()) {
         int width = this.renderer.scaledWidth;
         int height = this.renderer.scaledHeight;
         this.color = (Boolean)this.colorSync.getValue() ? ColorUtil.toARGB(Colors.INSTANCE.getCurrentColor().getRed(), Colors.INSTANCE.getCurrentColor().getGreen(), Colors.INSTANCE.getCurrentColor().getBlue(), 255) : ColorUtil.toRGBA((Integer)this.hudRed.getValue(), (Integer)this.hudGreen.getValue(), (Integer)this.hudBlue.getValue());
         String whiteString = "§f";
         float f;
         char[] stringToCharArray;
         int[] arrayOfInt;
         int x;
         int var10002;
         if ((Boolean)this.watermark.getValue()) {
            arrayOfInt = new int[]{1};
            String string = esohack.getName() + " v" + "1.0.5";
            stringToCharArray = string.toCharArray();
            f = 0.0F;
            char[] var13 = stringToCharArray;
            int var14 = stringToCharArray.length;

            for(x = 0; x < var14; ++x) {
               char c = var13[x];
               this.renderer.drawString(String.valueOf(c), 2.0F + f, 2.0F, (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
               f += (float)this.renderer.getStringWidth(String.valueOf(c));
               var10002 = arrayOfInt[0]++;
            }
         }

         int j = 0;
         int i;
         String text;
         if ((Boolean)this.arrayList.getValue()) {
            arrayOfInt = new int[]{1};
            f = 0.0F;

            for(i = 0; i < esohack.moduleManager.sortedModules.size(); ++i) {
               Module module = (Module)esohack.moduleManager.sortedModules.get(i);
               text = module.getDisplayName() + (module.getDisplayInfo() != null ? " [§f" + module.getDisplayInfo() + "§r" + "]" : "");
               this.renderer.drawString(text, (float)(width - 2 - this.renderer.getStringWidth(text)), (float)(2 + j * 10), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
               var10002 = arrayOfInt[0]++;
               ++j;
            }
         }

         i = mc.field_71462_r instanceof GuiChat ? 14 : 0;
         char c;
         String fpsText;
         char[] var30;
         int posX;
         if ((Boolean)this.serverBrand.getValue()) {
            fpsText = "Server brand " + esohack.serverManager.getServerBrand();
            arrayOfInt = new int[]{1};
            stringToCharArray = fpsText.toCharArray();
            f = 0.0F;
            i += 10;
            var30 = stringToCharArray;
            x = stringToCharArray.length;

            for(posX = 0; posX < x; ++posX) {
               c = var30[posX];
               this.renderer.drawString(String.valueOf(c), (float)(width - this.renderer.getStringWidth(fpsText)) + f - 2.0F, (float)(height - i), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
               f += (float)this.renderer.getStringWidth(String.valueOf(c));
               var10002 = arrayOfInt[0]++;
            }
         }

         int posZ;
         int posY;
         if ((Boolean)this.potions.getValue()) {
            List<String> effects = new ArrayList();
            Iterator var32 = esohack.potionManager.getOwnPotions().iterator();

            while(var32.hasNext()) {
               PotionEffect effect = (PotionEffect)var32.next();
               fpsText = esohack.potionManager.getPotionString(effect);
               effects.add(fpsText);
            }

            Collections.sort(effects, Comparator.comparing(String::length));

            for(x = effects.size() - 1; x >= 0; --x) {
               i += 10;
               fpsText = (String)effects.get(x);
               arrayOfInt = new int[]{1};
               f = 0.0F;
               stringToCharArray = fpsText.toCharArray();
               char[] var35 = stringToCharArray;
               posY = stringToCharArray.length;

               for(posZ = 0; posZ < posY; ++posZ) {
                  char c = var35[posZ];
                  this.renderer.drawString(String.valueOf(c), (float)(width - this.renderer.getStringWidth(fpsText)) + f - 2.0F, (float)(height - i), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
                  f += (float)this.renderer.getStringWidth(String.valueOf(c));
                  var10002 = arrayOfInt[0]++;
               }
            }
         }

         if ((Boolean)this.speed.getValue()) {
            fpsText = "Speed " + esohack.speedManager.getSpeedKpH() + " km/h";
            arrayOfInt = new int[]{1};
            stringToCharArray = fpsText.toCharArray();
            f = 0.0F;
            i += 10;
            var30 = stringToCharArray;
            x = stringToCharArray.length;

            for(posX = 0; posX < x; ++posX) {
               c = var30[posX];
               this.renderer.drawString(String.valueOf(c), (float)(width - this.renderer.getStringWidth(fpsText)) + f - 2.0F, (float)(height - i), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
               f += (float)this.renderer.getStringWidth(String.valueOf(c));
               var10002 = arrayOfInt[0]++;
            }
         }

         if ((Boolean)this.time.getValue()) {
            fpsText = "Time " + (new SimpleDateFormat("h:mm a")).format(new Date());
            arrayOfInt = new int[]{1};
            stringToCharArray = fpsText.toCharArray();
            f = 0.0F;
            i += 10;
            var30 = stringToCharArray;
            x = stringToCharArray.length;

            for(posX = 0; posX < x; ++posX) {
               c = var30[posX];
               this.renderer.drawString(String.valueOf(c), (float)(width - this.renderer.getStringWidth(fpsText)) + f - 2.0F, (float)(height - i), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
               f += (float)this.renderer.getStringWidth(String.valueOf(c));
               var10002 = arrayOfInt[0]++;
            }
         }

         if ((Boolean)this.tps.getValue()) {
            fpsText = "TPS " + esohack.serverManager.getTPS();
            arrayOfInt = new int[]{1};
            stringToCharArray = fpsText.toCharArray();
            f = 0.0F;
            i += 10;
            var30 = stringToCharArray;
            x = stringToCharArray.length;

            for(posX = 0; posX < x; ++posX) {
               c = var30[posX];
               this.renderer.drawString(String.valueOf(c), (float)(width - this.renderer.getStringWidth(fpsText)) + f - 2.0F, (float)(height - i), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
               f += (float)this.renderer.getStringWidth(String.valueOf(c));
               var10002 = arrayOfInt[0]++;
            }
         }

         fpsText = "FPS " + Minecraft.field_71470_ab;
         text = "Ping " + esohack.serverManager.getPing();
         char[] var37;
         char c;
         if ((Boolean)this.fps.getValue()) {
            arrayOfInt = new int[]{1};
            stringToCharArray = fpsText.toCharArray();
            f = 0.0F;
            i += 10;
            var37 = stringToCharArray;
            posX = stringToCharArray.length;

            for(posY = 0; posY < posX; ++posY) {
               c = var37[posY];
               this.renderer.drawString(String.valueOf(c), (float)(width - this.renderer.getStringWidth(fpsText)) + f - 2.0F, (float)(height - i), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
               f += (float)this.renderer.getStringWidth(String.valueOf(c));
               var10002 = arrayOfInt[0]++;
            }
         }

         if ((Boolean)this.ping.getValue()) {
            arrayOfInt = new int[]{1};
            stringToCharArray = text.toCharArray();
            f = 0.0F;
            i += 10;
            var37 = stringToCharArray;
            posX = stringToCharArray.length;

            for(posY = 0; posY < posX; ++posY) {
               c = var37[posY];
               this.renderer.drawString(String.valueOf(c), (float)(width - this.renderer.getStringWidth(text)) + f - 2.0F, (float)(height - i), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
               f += (float)this.renderer.getStringWidth(String.valueOf(c));
               var10002 = arrayOfInt[0]++;
            }
         }

         boolean inHell = mc.field_71441_e.func_180494_b(mc.field_71439_g.func_180425_c()).func_185359_l().equals("Hell");
         posX = (int)mc.field_71439_g.field_70165_t;
         posY = (int)mc.field_71439_g.field_70163_u;
         posZ = (int)mc.field_71439_g.field_70161_v;
         float nether = !inHell ? 0.125F : 8.0F;
         int hposX = (int)(mc.field_71439_g.field_70165_t * (double)nether);
         int hposZ = (int)(mc.field_71439_g.field_70161_v * (double)nether);
         esohack.notificationManager.handleNotifications(height - (i + 16));
         i = mc.field_71462_r instanceof GuiChat ? 14 : 0;
         String coordinates = posX + ", " + posY + ", " + posZ + " [" + hposX + ", " + hposZ + "]";
         text = ((Boolean)this.direction.getValue() ? esohack.rotationManager.getDirection4D(false) + " " : "") + ((Boolean)this.coords.getValue() ? coordinates : "") + "";
         arrayOfInt = new int[]{1};
         stringToCharArray = text.toCharArray();
         f = 0.0F;
         i += 10;
         char[] var23 = stringToCharArray;
         int var24 = stringToCharArray.length;

         for(int var25 = 0; var25 < var24; ++var25) {
            char c = var23[var25];
            this.renderer.drawString(String.valueOf(c), 2.0F + f, (float)(height - i), (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
            f += (float)this.renderer.getStringWidth(String.valueOf(c));
            var10002 = arrayOfInt[0]++;
         }

         if ((Boolean)this.armor.getValue()) {
            this.renderArmorHUD((Boolean)this.percent.getValue());
         }

         if ((Boolean)this.totems.getValue()) {
            this.renderTotemHUD();
         }

         if (this.greeter.getValue() != HUD.Greeter.NONE) {
            this.renderGreeter();
         }

      }
   }

   public Map<String, Integer> getTextRadarPlayers() {
      return EntityUtil.getTextRadarPlayers();
   }

   public void renderGreeter() {
      int width = this.renderer.scaledWidth;
      String text = "";
      switch((HUD.Greeter)this.greeter.getValue()) {
      case TIME:
         text = text + MathUtil.getTimeOfDay() + mc.field_71439_g.getDisplayNameString();
         break;
      case LONG:
         text = text + "looking swag today, " + mc.field_71439_g.getDisplayNameString() + " :^)";
         break;
      default:
         text = text + "Welcome " + mc.field_71439_g.getDisplayNameString();
      }

      int[] arrayOfInt = new int[]{1};
      char[] stringToCharArray = text.toCharArray();
      float f = 0.0F;
      char[] var6 = stringToCharArray;
      int var7 = stringToCharArray.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         char c = var6[var8];
         this.renderer.drawString(String.valueOf(c), (float)width / 2.0F - (float)this.renderer.getStringWidth(text) / 2.0F + 2.0F + f, 2.0F, (Boolean)this.rainbow.getValue() ? ColorUtil.rainbow(arrayOfInt[0] * (Integer)getInstance().rainbowSpeed.getValue()).getRGB() : this.color, true);
         f += (float)this.renderer.getStringWidth(String.valueOf(c));
         int var10002 = arrayOfInt[0]++;
      }

   }

   public void renderTotemHUD() {
      int width = this.renderer.scaledWidth;
      int height = this.renderer.scaledHeight;
      int totems = mc.field_71439_g.field_71071_by.field_70462_a.stream().filter((itemStack) -> {
         return itemStack.func_77973_b() == Items.field_190929_cY;
      }).mapToInt(ItemStack::func_190916_E).sum();
      if (mc.field_71439_g.func_184592_cb().func_77973_b() == Items.field_190929_cY) {
         totems += mc.field_71439_g.func_184592_cb().func_190916_E();
      }

      if (totems > 0) {
         GlStateManager.func_179098_w();
         int i = width / 2;
         int iteration = false;
         int y = height - 55 - (mc.field_71439_g.func_70090_H() && mc.field_71442_b.func_78763_f() ? 10 : 0);
         int x = i - 189 + 180 + 2;
         GlStateManager.func_179126_j();
         RenderUtil.itemRender.field_77023_b = 200.0F;
         RenderUtil.itemRender.func_180450_b(totem, x, y);
         RenderUtil.itemRender.func_180453_a(mc.field_71466_p, totem, x, y, "");
         RenderUtil.itemRender.field_77023_b = 0.0F;
         GlStateManager.func_179098_w();
         GlStateManager.func_179140_f();
         GlStateManager.func_179097_i();
         this.renderer.drawStringWithShadow(totems + "", (float)(x + 19 - 2 - this.renderer.getStringWidth(totems + "")), (float)(y + 9), 16777215);
         GlStateManager.func_179126_j();
         GlStateManager.func_179140_f();
      }

   }

   public void renderArmorHUD(boolean percent) {
      int width = this.renderer.scaledWidth;
      int height = this.renderer.scaledHeight;
      GlStateManager.func_179098_w();
      int i = width / 2;
      int iteration = 0;
      int y = height - 55 - (mc.field_71439_g.func_70090_H() && mc.field_71442_b.func_78763_f() ? 10 : 0);
      Iterator var7 = mc.field_71439_g.field_71071_by.field_70460_b.iterator();

      while(var7.hasNext()) {
         ItemStack is = (ItemStack)var7.next();
         ++iteration;
         if (!is.func_190926_b()) {
            int x = i - 90 + (9 - iteration) * 20 + 2;
            GlStateManager.func_179126_j();
            RenderUtil.itemRender.field_77023_b = 200.0F;
            RenderUtil.itemRender.func_180450_b(is, x, y);
            RenderUtil.itemRender.func_180453_a(mc.field_71466_p, is, x, y, "");
            RenderUtil.itemRender.field_77023_b = 0.0F;
            GlStateManager.func_179098_w();
            GlStateManager.func_179140_f();
            GlStateManager.func_179097_i();
            String s = is.func_190916_E() > 1 ? is.func_190916_E() + "" : "";
            this.renderer.drawStringWithShadow(s, (float)(x + 19 - 2 - this.renderer.getStringWidth(s)), (float)(y + 9), 16777215);
            if (percent) {
               int dmg = false;
               int itemDurability = is.func_77958_k() - is.func_77952_i();
               float green = ((float)is.func_77958_k() - (float)is.func_77952_i()) / (float)is.func_77958_k();
               float red = 1.0F - green;
               int dmg;
               if (percent) {
                  dmg = 100 - (int)(red * 100.0F);
               } else {
                  dmg = itemDurability;
               }

               this.renderer.drawStringWithShadow(dmg + "", (float)(x + 8 - this.renderer.getStringWidth(dmg + "") / 2), (float)(y - 11), ColorUtil.toRGBA((int)(red * 255.0F), (int)(green * 255.0F), 0));
            }
         }
      }

      GlStateManager.func_179126_j();
      GlStateManager.func_179140_f();
   }

   @SubscribeEvent
   public void onUpdateWalkingPlayer(AttackEntityEvent event) {
      this.shouldIncrement = true;
   }

   static {
      totem = new ItemStack(Items.field_190929_cY);
   }

   public static enum Greeter {
      NONE,
      TIME,
      LONG;
   }
}
