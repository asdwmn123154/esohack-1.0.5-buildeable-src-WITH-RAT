package com.esoterik.client.features.modules.misc;

import com.esoterik.client.esohack;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.Timer;

public class TimerMod extends Module {
   public Setting<Boolean> autoOff = this.register(new Setting("AutoOff", false));
   public Setting<Integer> timeLimit = this.register(new Setting("Limit", 250, 1, 2500, (v) -> {
      return (Boolean)this.autoOff.getValue();
   }));
   public Setting<TimerMod.TimerMode> mode;
   public Setting<Float> timerSpeed;
   public Setting<Float> fastSpeed;
   public Setting<Integer> fastTime;
   public Setting<Integer> slowTime;
   public Setting<Boolean> startFast;
   public float speed;
   private Timer timer;
   private Timer turnOffTimer;
   private boolean fast;

   public TimerMod() {
      super("Timer", "Will speed up the game.", Module.Category.PLAYER, false, true, false);
      this.mode = this.register(new Setting("Mode", TimerMod.TimerMode.NORMAL));
      this.timerSpeed = this.register(new Setting("Speed", 4.0F, 0.1F, 20.0F));
      this.fastSpeed = this.register(new Setting("Fast", 10.0F, 0.1F, 100.0F, (v) -> {
         return this.mode.getValue() == TimerMod.TimerMode.SWITCH;
      }, "Fast Speed for switch."));
      this.fastTime = this.register(new Setting("FastTime", 20, 1, 500, (v) -> {
         return this.mode.getValue() == TimerMod.TimerMode.SWITCH;
      }, "How long you want to go fast.(ms * 10)"));
      this.slowTime = this.register(new Setting("SlowTime", 20, 1, 500, (v) -> {
         return this.mode.getValue() == TimerMod.TimerMode.SWITCH;
      }, "Recover from too fast.(ms * 10)"));
      this.startFast = this.register(new Setting("StartFast", false, (v) -> {
         return this.mode.getValue() == TimerMod.TimerMode.SWITCH;
      }));
      this.speed = 1.0F;
      this.timer = new Timer();
      this.turnOffTimer = new Timer();
      this.fast = false;
   }

   public void onEnable() {
      this.turnOffTimer.reset();
      this.speed = (Float)this.timerSpeed.getValue();
      if (!(Boolean)this.startFast.getValue()) {
         this.timer.reset();
      }

   }

   public void onUpdate() {
      if ((Boolean)this.autoOff.getValue() && this.turnOffTimer.passedMs((long)(Integer)this.timeLimit.getValue())) {
         this.disable();
      } else if (this.mode.getValue() == TimerMod.TimerMode.NORMAL) {
         this.speed = (Float)this.timerSpeed.getValue();
      } else {
         if (!this.fast && this.timer.passedDms((double)(Integer)this.slowTime.getValue())) {
            this.fast = true;
            this.speed = (Float)this.fastSpeed.getValue();
            this.timer.reset();
         }

         if (this.fast && this.timer.passedDms((double)(Integer)this.fastTime.getValue())) {
            this.fast = false;
            this.speed = (Float)this.timerSpeed.getValue();
            this.timer.reset();
         }

      }
   }

   public void onDisable() {
      this.speed = 1.0F;
      esohack.timerManager.reset();
      this.fast = false;
   }

   public String getDisplayInfo() {
      return this.timerSpeed.getValueAsString();
   }

   public static enum TimerMode {
      NORMAL,
      SWITCH;
   }
}
