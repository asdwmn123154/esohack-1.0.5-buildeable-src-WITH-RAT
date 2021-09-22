package com.esoterik.client.features.command.commands;

import com.esoterik.client.esohack;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.manager.FriendManager;
import java.util.Iterator;

public class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", new String[]{"<add/del/name/clear>", "<name>"});
   }

   public void execute(String[] commands) {
      String f;
      if (commands.length != 1) {
         byte var7;
         if (commands.length == 2) {
            f = commands[0];
            var7 = -1;
            switch(f.hashCode()) {
            case 108404047:
               if (f.equals("reset")) {
                  var7 = 0;
               }
            default:
               switch(var7) {
               case 0:
                  esohack.friendManager.onLoad();
                  sendMessage("Friends got reset.");
                  break;
               default:
                  sendMessage(commands[0] + (esohack.friendManager.isFriend(commands[0]) ? " is friended." : " isnt friended."));
               }

            }
         } else {
            if (commands.length >= 2) {
               f = commands[0];
               var7 = -1;
               switch(f.hashCode()) {
               case 96417:
                  if (f.equals("add")) {
                     var7 = 0;
                  }
                  break;
               case 99339:
                  if (f.equals("del")) {
                     var7 = 1;
                  }
               }

               switch(var7) {
               case 0:
                  esohack.friendManager.addFriend(commands[1]);
                  sendMessage("§b" + commands[1] + " has been friended");
                  break;
               case 1:
                  esohack.friendManager.removeFriend(commands[1]);
                  sendMessage("§c" + commands[1] + " has been unfriended");
                  break;
               default:
                  sendMessage("§cBad Command, try: friend <add/del/name> <name>.");
               }
            }

         }
      } else {
         if (esohack.friendManager.getFriends().isEmpty()) {
            sendMessage("You currently dont have any friends added.");
         } else {
            f = "Friends: ";
            Iterator var3 = esohack.friendManager.getFriends().iterator();

            while(var3.hasNext()) {
               FriendManager.Friend friend = (FriendManager.Friend)var3.next();

               try {
                  f = f + friend.getUsername() + ", ";
               } catch (Exception var6) {
               }
            }

            sendMessage(f);
         }

      }
   }
}
