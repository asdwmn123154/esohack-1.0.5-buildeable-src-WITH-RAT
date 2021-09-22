package com.esoterik.client.features.modules.misc;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.PacketEvent;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.TextUtil;
import com.esoterik.client.util.Timer;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatModifier extends Module {
   public Setting<TextUtil.Color> timeStamps;
   public Setting<TextUtil.Color> bracket;
   public Setting<Boolean> suffix;
   public Setting<Boolean> versionSuffix;
   public Setting<Boolean> clean;
   public Setting<Boolean> infinite;
   private final Timer timer;
   private static ChatModifier INSTANCE = new ChatModifier();

   public ChatModifier() {
      super("CustomChat", "Customises aspects of the chat", Module.Category.MISC, true, false, false);
      this.timeStamps = this.register(new Setting("Time", TextUtil.Color.NONE));
      this.bracket = this.register(new Setting("BracketColor", TextUtil.Color.WHITE, (v) -> {
         return this.timeStamps.getValue() != TextUtil.Color.NONE;
      }));
      this.suffix = this.register(new Setting("ChatSuffix", true, "Appends esohack suffix to all messages."));
      this.versionSuffix = this.register(new Setting("IncludeVersion", true, (v) -> {
         return (Boolean)this.suffix.getValue();
      }));
      this.clean = this.register(new Setting("CleanChat", false, "Cleans your chat."));
      this.infinite = this.register(new Setting("InfiniteLength", false, "Makes your chat infinitely scrollable."));
      this.timer = new Timer();
      this.setInstance();
   }

   private void setInstance() {
      INSTANCE = this;
   }

   public static ChatModifier getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ChatModifier();
      }

      return INSTANCE;
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getStage() == 0 && event.getPacket() instanceof CPacketChatMessage) {
         CPacketChatMessage packet = (CPacketChatMessage)event.getPacket();
         String s = packet.func_149439_c();
         if (s.startsWith("/") || s.startsWith("!")) {
            return;
         }

         if ((Boolean)this.suffix.getValue()) {
            s = s + " ⏐ " + esohack.getName();
            if ((Boolean)this.versionSuffix.getValue()) {
               s = s + " v1.0.5";
            }
         }

         if (s.length() >= 256) {
            s = s.substring(0, 256);
         }

         packet.field_149440_a = s;
      }

   }

   @SubscribeEvent
   public void onPacketReceive(PacketEvent.Receive event) {
      if (event.getStage() == 0 && this.timeStamps.getValue() != TextUtil.Color.NONE && event.getPacket() instanceof SPacketChat) {
         if (!((SPacketChat)event.getPacket()).func_148916_d()) {
            return;
         }

         String originalMessage = ((SPacketChat)event.getPacket()).field_148919_a.func_150260_c();
         String message = this.getTimeString() + originalMessage;
         ((SPacketChat)event.getPacket()).field_148919_a = new TextComponentString(message);
      }

   }

   public String getTimeString() {
      String date = (new SimpleDateFormat("k:mm")).format(new Date());
      return (this.bracket.getValue() == TextUtil.Color.NONE ? "" : TextUtil.coloredString("<", (TextUtil.Color)this.bracket.getValue())) + TextUtil.coloredString(date, (TextUtil.Color)this.timeStamps.getValue()) + (this.bracket.getValue() == TextUtil.Color.NONE ? "" : TextUtil.coloredString(">", (TextUtil.Color)this.bracket.getValue())) + " " + "§r";
   }

   private boolean shouldSendMessage(EntityPlayer player) {
      return player.field_71093_bK != 1 ? false : player.func_180425_c().equals(new Vec3i(0, 240, 0));
   }
}
