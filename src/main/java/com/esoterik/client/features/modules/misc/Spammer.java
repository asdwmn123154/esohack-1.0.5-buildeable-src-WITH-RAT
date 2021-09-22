package com.esoterik.client.features.modules.misc;

import com.esoterik.client.esohack;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.FileUtil;
import com.esoterik.client.util.Timer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.network.play.client.CPacketChatMessage;

public class Spammer extends Module {
   public Setting<Integer> delay = this.register(new Setting("Delay", 10, 1, 20));
   public Setting<Boolean> greentext = this.register(new Setting("Greentext", false));
   public Setting<Boolean> random = this.register(new Setting("Random", false));
   public Setting<Boolean> loadFile = this.register(new Setting("LoadFile", false));
   private final Timer timer = new Timer();
   private final List<String> sendPlayers = new ArrayList();
   private static final String fileName = "client/util/Spammer.txt";
   private static final String defaultMessage = esohack.getName() + " owns all https://discord.gg/wJq5nMEdNT";
   private static final List<String> spamMessages = new ArrayList();
   private static final Random rnd = new Random();

   public Spammer() {
      super("Spammer", "Spams stuff.", Module.Category.MISC, true, false, false);
   }

   public void onLoad() {
      this.readSpamFile();
      this.disable();
   }

   public void onEnable() {
      if (fullNullCheck()) {
         this.disable();
      } else {
         this.readSpamFile();
      }
   }

   public void onLogin() {
      this.disable();
   }

   public void onLogout() {
      this.disable();
   }

   public void onDisable() {
      spamMessages.clear();
      this.timer.reset();
   }

   public void onUpdate() {
      if (fullNullCheck()) {
         this.disable();
      } else {
         if ((Boolean)this.loadFile.getValue()) {
            this.readSpamFile();
            this.loadFile.setValue(false);
         }

         if (this.timer.passedS((double)(Integer)this.delay.getValue())) {
            if (spamMessages.size() > 0) {
               String messageOut;
               if ((Boolean)this.random.getValue()) {
                  int index = rnd.nextInt(spamMessages.size());
                  messageOut = (String)spamMessages.get(index);
                  spamMessages.remove(index);
               } else {
                  messageOut = (String)spamMessages.get(0);
                  spamMessages.remove(0);
               }

               spamMessages.add(messageOut);
               if ((Boolean)this.greentext.getValue()) {
                  messageOut = "> " + messageOut;
               }

               mc.field_71439_g.field_71174_a.func_147297_a(new CPacketChatMessage(messageOut.replaceAll("§", "")));
            }

            this.timer.reset();
         }
      }
   }

   private void readSpamFile() {
      List<String> fileInput = FileUtil.readTextFileAllLines("client/util/Spammer.txt");
      Iterator<String> i = fileInput.iterator();
      spamMessages.clear();

      while(i.hasNext()) {
         String s = (String)i.next();
         if (!s.replaceAll("\\s", "").isEmpty()) {
            spamMessages.add(s);
         }
      }

      if (spamMessages.size() == 0) {
         spamMessages.add(defaultMessage);
      }

   }
}
