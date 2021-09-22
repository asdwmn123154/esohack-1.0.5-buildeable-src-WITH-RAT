package com.esoterik.client.manager;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.DeathEvent;
import com.esoterik.client.event.events.PacketEvent;
import com.esoterik.client.event.events.Render2DEvent;
import com.esoterik.client.event.events.Render3DEvent;
import com.esoterik.client.event.events.TotemPopEvent;
import com.esoterik.client.event.events.UpdateWalkingPlayerEvent;
import com.esoterik.client.features.Feature;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.modules.client.Managers;
import com.esoterik.client.util.Timer;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketPlayerListItem;
import net.minecraft.network.play.server.SPacketTimeUpdate;
import net.minecraft.network.play.server.SPacketPlayerListItem.Action;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Post;
import net.minecraftforge.client.event.RenderGameOverlayEvent.Text;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import org.lwjgl.input.Keyboard;

public class EventManager extends Feature {
   private final Timer timer = new Timer();
   private final Timer logoutTimer = new Timer();
   private AtomicBoolean tickOngoing = new AtomicBoolean(false);

   public void init() {
      MinecraftForge.EVENT_BUS.register(this);
   }

   public void onUnload() {
      MinecraftForge.EVENT_BUS.unregister(this);
   }

   @SubscribeEvent
   public void onUpdate(LivingUpdateEvent event) {
      if (!fullNullCheck() && event.getEntity().func_130014_f_().field_72995_K && event.getEntityLiving().equals(mc.field_71439_g)) {
         esohack.potionManager.update();
         esohack.totemPopManager.onUpdate();
         esohack.inventoryManager.update();
         esohack.holeManager.update();
         esohack.moduleManager.onUpdate();
         esohack.timerManager.update();
         if (this.timer.passedMs((long)Managers.getInstance().moduleListUpdates)) {
            esohack.moduleManager.sortModules(true);
            this.timer.reset();
         }
      }

   }

   @SubscribeEvent
   public void onClientConnect(ClientConnectedToServerEvent event) {
      this.logoutTimer.reset();
      esohack.moduleManager.onLogin();
   }

   @SubscribeEvent
   public void onClientDisconnect(ClientDisconnectionFromServerEvent event) {
      esohack.moduleManager.onLogout();
      esohack.totemPopManager.onLogout();
      esohack.potionManager.onLogout();
   }

   public boolean ticksOngoing() {
      return this.tickOngoing.get();
   }

   @SubscribeEvent
   public void onTick(ClientTickEvent event) {
      if (!fullNullCheck()) {
         esohack.moduleManager.onTick();
         Iterator var2 = mc.field_71441_e.field_73010_i.iterator();

         while(var2.hasNext()) {
            EntityPlayer player = (EntityPlayer)var2.next();
            if (player != null && !(player.func_110143_aJ() > 0.0F)) {
               MinecraftForge.EVENT_BUS.post(new DeathEvent(player));
               esohack.totemPopManager.onDeath(player);
            }
         }

      }
   }

   @SubscribeEvent
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (!fullNullCheck()) {
         if (event.getStage() == 0) {
            esohack.speedManager.updateValues();
            esohack.rotationManager.updateRotations();
            esohack.positionManager.updatePosition();
         }

         if (event.getStage() == 1) {
            esohack.rotationManager.restoreRotations();
            esohack.positionManager.restorePosition();
         }

      }
   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getStage() == 0) {
         esohack.serverManager.onPacketReceived();
         if (event.getPacket() instanceof SPacketEntityStatus) {
            SPacketEntityStatus packet = (SPacketEntityStatus)event.getPacket();
            if (packet.func_149160_c() == 35 && packet.func_149161_a(mc.field_71441_e) instanceof EntityPlayer) {
               EntityPlayer player = (EntityPlayer)packet.func_149161_a(mc.field_71441_e);
               MinecraftForge.EVENT_BUS.post(new TotemPopEvent(player));
               esohack.totemPopManager.onTotemPop(player);
               esohack.potionManager.onTotemPop(player);
            }
         }

         if (event.getPacket() instanceof SPacketPlayerListItem && !fullNullCheck() && this.logoutTimer.passedS(1.0D)) {
            SPacketPlayerListItem packet = (SPacketPlayerListItem)event.getPacket();
            if (!Action.ADD_PLAYER.equals(packet.func_179768_b()) && !Action.REMOVE_PLAYER.equals(packet.func_179768_b())) {
               return;
            }
         }

         if (event.getPacket() instanceof SPacketTimeUpdate) {
            esohack.serverManager.update();
         }

      }
   }

   @SubscribeEvent
   public void onWorldRender(RenderWorldLastEvent event) {
      if (!event.isCanceled()) {
         mc.field_71424_I.func_76320_a("client");
         GlStateManager.func_179090_x();
         GlStateManager.func_179147_l();
         GlStateManager.func_179118_c();
         GlStateManager.func_179120_a(770, 771, 1, 0);
         GlStateManager.func_179103_j(7425);
         GlStateManager.func_179097_i();
         GlStateManager.func_187441_d(1.0F);
         Render3DEvent render3dEvent = new Render3DEvent(event.getPartialTicks());
         esohack.moduleManager.onRender3D(render3dEvent);
         GlStateManager.func_187441_d(1.0F);
         GlStateManager.func_179103_j(7424);
         GlStateManager.func_179084_k();
         GlStateManager.func_179141_d();
         GlStateManager.func_179098_w();
         GlStateManager.func_179126_j();
         GlStateManager.func_179089_o();
         GlStateManager.func_179089_o();
         GlStateManager.func_179132_a(true);
         GlStateManager.func_179098_w();
         GlStateManager.func_179147_l();
         GlStateManager.func_179126_j();
         mc.field_71424_I.func_76319_b();
      }
   }

   @SubscribeEvent
   public void renderHUD(Post event) {
      if (event.getType() == ElementType.HOTBAR) {
         esohack.textManager.updateResolution();
      }

   }

   @SubscribeEvent(
      priority = EventPriority.LOW
   )
   public void onRenderGameOverlayEvent(Text event) {
      if (event.getType().equals(ElementType.TEXT)) {
         ScaledResolution resolution = new ScaledResolution(mc);
         Render2DEvent render2DEvent = new Render2DEvent(event.getPartialTicks(), resolution);
         esohack.moduleManager.onRender2D(render2DEvent);
         GlStateManager.func_179131_c(1.0F, 1.0F, 1.0F, 1.0F);
      }

   }

   @SubscribeEvent(
      priority = EventPriority.NORMAL,
      receiveCanceled = true
   )
   public void onKeyInput(KeyInputEvent event) {
      if (Keyboard.getEventKeyState()) {
         esohack.moduleManager.onKeyPressed(Keyboard.getEventKey());
      }

   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   public void onChatSent(ClientChatEvent event) {
      if (event.getMessage().startsWith(Command.getCommandPrefix())) {
         event.setCanceled(true);

         try {
            mc.field_71456_v.func_146158_b().func_146239_a(event.getMessage());
            if (event.getMessage().length() > 1) {
               esohack.commandManager.executeCommand(event.getMessage().substring(Command.getCommandPrefix().length() - 1));
            } else {
               Command.sendMessage("Please enter a command.");
            }
         } catch (Exception var3) {
            var3.printStackTrace();
            Command.sendMessage("§cAn error occurred while running this command. Check the log!");
         }

         event.setMessage("");
      }

   }
}
