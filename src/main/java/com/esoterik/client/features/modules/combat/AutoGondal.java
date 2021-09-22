package com.esoterik.client.features.modules.combat;

import com.esoterik.client.esohack;
import com.esoterik.client.event.events.ClientEvent;
import com.esoterik.client.event.events.PacketEvent;
import com.esoterik.client.event.events.Render3DEvent;
import com.esoterik.client.event.events.UpdateWalkingPlayerEvent;
import com.esoterik.client.features.command.Command;
import com.esoterik.client.features.gui.esohackGui;
import com.esoterik.client.features.modules.Module;
import com.esoterik.client.features.modules.client.Colors;
import com.esoterik.client.features.setting.Bind;
import com.esoterik.client.features.setting.Setting;
import com.esoterik.client.util.BlockUtil;
import com.esoterik.client.util.DamageUtil;
import com.esoterik.client.util.EntityUtil;
import com.esoterik.client.util.InventoryUtil;
import com.esoterik.client.util.MathUtil;
import com.esoterik.client.util.RenderUtil;
import com.esoterik.client.util.Timer;
import com.mojang.authlib.GameProfile;
import io.netty.util.internal.ConcurrentSet;
import java.awt.Color;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemEndCrystal;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketUseEntity.Action;
import net.minecraft.network.play.server.SPacketDestroyEntities;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketExplosion;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.network.play.server.SPacketSpawnObject;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class AutoGondal extends Module {
   public static EntityPlayer target = null;
   public static Set<BlockPos> lowDmgPos = new ConcurrentSet();
   public static Set<BlockPos> placedPos = new HashSet();
   public static Set<BlockPos> brokenPos = new HashSet();
   private static AutoGondal instance;
   public final Timer threadTimer = new Timer();
   private final Setting<AutoGondal.Settings> setting;
   public final Setting<Boolean> attackOppositeHand;
   public final Setting<Boolean> removeAfterAttack;
   public final Setting<Boolean> antiBlock;
   private final Setting<Integer> switchCooldown;
   private final Setting<Integer> eventMode;
   private final Timer switchTimer;
   private final Timer manualTimer;
   private final Timer breakTimer;
   private final Timer placeTimer;
   private final Timer syncTimer;
   private final Timer predictTimer;
   private final Timer renderTimer;
   private final AtomicBoolean shouldInterrupt;
   private final Timer syncroTimer;
   private final Map<EntityPlayer, Timer> totemPops;
   private final Queue<CPacketUseEntity> packetUseEntities;
   private final AtomicBoolean threadOngoing;
   public Setting<AutoGondal.Raytrace> raytrace;
   public Setting<Boolean> place;
   public Setting<Integer> placeDelay;
   public Setting<Float> placeRange;
   public Setting<Float> minDamage;
   public Setting<Float> maxSelfPlace;
   public Setting<Integer> wasteAmount;
   public Setting<Boolean> wasteMinDmgCount;
   public Setting<Float> facePlace;
   public Setting<Float> placetrace;
   public Setting<Boolean> antiSurround;
   public Setting<Boolean> limitFacePlace;
   public Setting<Boolean> oneDot15;
   public Setting<Boolean> doublePop;
   public Setting<Double> popHealth;
   public Setting<Float> popDamage;
   public Setting<Integer> popTime;
   public Setting<Boolean> explode;
   public Setting<AutoGondal.Switch> switchMode;
   public Setting<Integer> breakDelay;
   public Setting<Float> breakRange;
   public Setting<Integer> packets;
   public Setting<Float> maxSelfBreak;
   public Setting<Float> breaktrace;
   public Setting<Boolean> manual;
   public Setting<Boolean> manualMinDmg;
   public Setting<Integer> manualBreak;
   public Setting<Boolean> sync;
   public Setting<Boolean> instant;
   public Setting<AutoGondal.PredictTimer> instantTimer;
   public Setting<Boolean> resetBreakTimer;
   public Setting<Integer> predictDelay;
   public Setting<Boolean> predictCalc;
   public Setting<Boolean> superSafe;
   public Setting<Boolean> antiCommit;
   public Setting<Boolean> render;
   private final Setting<Integer> red;
   private final Setting<Integer> green;
   private final Setting<Integer> blue;
   private final Setting<Integer> alpha;
   public Setting<Boolean> colorSync;
   public Setting<Boolean> box;
   private final Setting<Integer> boxAlpha;
   public Setting<Boolean> outline;
   private final Setting<Float> lineWidth;
   public Setting<Boolean> text;
   public Setting<Boolean> customOutline;
   private final Setting<Integer> cRed;
   private final Setting<Integer> cGreen;
   private final Setting<Integer> cBlue;
   private final Setting<Integer> cAlpha;
   public Setting<Boolean> holdFacePlace;
   public Setting<Boolean> holdFaceBreak;
   public Setting<Boolean> slowFaceBreak;
   public Setting<Boolean> actualSlowBreak;
   public Setting<Integer> facePlaceSpeed;
   public Setting<Boolean> antiNaked;
   public Setting<Float> range;
   public Setting<AutoGondal.Target> targetMode;
   public Setting<Integer> minArmor;
   public Setting<AutoGondal.AutoSwitch> autoSwitch;
   public Setting<Bind> switchBind;
   public Setting<Boolean> offhandSwitch;
   public Setting<Boolean> switchBack;
   public Setting<Boolean> lethalSwitch;
   public Setting<Boolean> mineSwitch;
   public Setting<AutoGondal.Rotate> rotate;
   public Setting<Boolean> suicide;
   public Setting<Boolean> webAttack;
   public Setting<Boolean> fullCalc;
   public Setting<Boolean> sound;
   public Setting<Float> soundRange;
   public Setting<Float> soundPlayer;
   public Setting<Boolean> soundConfirm;
   public Setting<Boolean> extraSelfCalc;
   public Setting<AutoGondal.AntiFriendPop> antiFriendPop;
   public Setting<Boolean> noCount;
   public Setting<Boolean> calcEvenIfNoDamage;
   public Setting<Boolean> predictFriendDmg;
   public Setting<Float> minMinDmg;
   public Setting<Boolean> breakSwing;
   public Setting<Boolean> placeSwing;
   public Setting<Boolean> exactHand;
   public Setting<Boolean> justRender;
   public Setting<Boolean> fakeSwing;
   public Setting<AutoGondal.Logic> logic;
   public Setting<AutoGondal.DamageSync> damageSync;
   public Setting<Integer> damageSyncTime;
   public Setting<Float> dropOff;
   public Setting<Integer> confirm;
   public Setting<Boolean> syncedFeetPlace;
   public Setting<Boolean> fullSync;
   public Setting<Boolean> syncCount;
   public Setting<Boolean> hyperSync;
   public Setting<Boolean> gigaSync;
   public Setting<Boolean> syncySync;
   public Setting<Boolean> enormousSync;
   public Setting<Boolean> holySync;
   public Setting<Boolean> rotateFirst;
   public Setting<AutoGondal.ThreadMode> threadMode;
   public Setting<Integer> threadDelay;
   public Setting<Boolean> syncThreadBool;
   public Setting<Integer> syncThreads;
   public Setting<Boolean> predictPos;
   public Setting<Boolean> renderExtrapolation;
   public Setting<Integer> predictTicks;
   public Setting<Integer> rotations;
   public Setting<Boolean> predictRotate;
   public Setting<Float> predictOffset;
   public Setting<Boolean> brownZombie;
   public Setting<Boolean> doublePopOnDamage;
   public boolean rotating;
   private Queue<Entity> attackList;
   private Map<Entity, Float> crystalMap;
   private Entity efficientTarget;
   private double currentDamage;
   private double renderDamage;
   private double lastDamage;
   private boolean didRotation;
   private boolean switching;
   private BlockPos placePos;
   private BlockPos renderPos;
   private boolean mainHand;
   private boolean offHand;
   private int crystalCount;
   private int minDmgCount;
   private int lastSlot;
   private float yaw;
   private float pitch;
   private BlockPos webPos;
   private BlockPos lastPos;
   private boolean posConfirmed;
   private boolean foundDoublePop;
   private int rotationPacketsSpoofed;
   private ScheduledExecutorService executor;
   private Thread thread;
   private EntityPlayer currentSyncTarget;
   private BlockPos syncedPlayerPos;
   private BlockPos syncedCrystalPos;
   private AutoGondal.PlaceInfo placeInfo;
   private boolean addTolowDmg;

   public AutoGondal() {
      super("AutoGondal", "Best CA on the market", Module.Category.COMBAT, true, false, false);
      this.setting = this.register(new Setting("Settings", AutoGondal.Settings.PLACE));
      this.attackOppositeHand = this.register(new Setting("OppositeHand", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.removeAfterAttack = this.register(new Setting("AttackRemove", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.antiBlock = this.register(new Setting("AntiFeetPlace", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.switchCooldown = this.register(new Setting("Cooldown", 500, 0, 1000, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.eventMode = this.register(new Setting("Updates", 3, 1, 3, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.switchTimer = new Timer();
      this.manualTimer = new Timer();
      this.breakTimer = new Timer();
      this.placeTimer = new Timer();
      this.syncTimer = new Timer();
      this.predictTimer = new Timer();
      this.renderTimer = new Timer();
      this.shouldInterrupt = new AtomicBoolean(false);
      this.syncroTimer = new Timer();
      this.totemPops = new ConcurrentHashMap();
      this.packetUseEntities = new LinkedList();
      this.threadOngoing = new AtomicBoolean(false);
      this.raytrace = this.register(new Setting("Raytrace", AutoGondal.Raytrace.NONE, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.place = this.register(new Setting("Place", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE;
      }));
      this.placeDelay = this.register(new Setting("PlaceDelay", 25, 0, 500, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.placeRange = this.register(new Setting("PlaceRange", 6.0F, 0.0F, 10.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.minDamage = this.register(new Setting("MinDamage", 7.0F, 0.1F, 20.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.maxSelfPlace = this.register(new Setting("MaxSelfPlace", 10.0F, 0.1F, 36.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.wasteAmount = this.register(new Setting("WasteAmount", 2, 1, 5, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.wasteMinDmgCount = this.register(new Setting("CountMinDmg", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.facePlace = this.register(new Setting("FacePlace", 8.0F, 0.1F, 20.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.placetrace = this.register(new Setting("Placetrace", 4.5F, 0.0F, 10.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue() && this.raytrace.getValue() != AutoGondal.Raytrace.NONE && this.raytrace.getValue() != AutoGondal.Raytrace.BREAK;
      }));
      this.antiSurround = this.register(new Setting("AntiSurround", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.limitFacePlace = this.register(new Setting("LimitFacePlace", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.oneDot15 = this.register(new Setting("1.15", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.doublePop = this.register(new Setting("AntiTotem", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue();
      }));
      this.popHealth = this.register(new Setting("PopHealth", 1.0D, 0.0D, 3.0D, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue() && (Boolean)this.doublePop.getValue();
      }));
      this.popDamage = this.register(new Setting("PopDamage", 4.0F, 0.0F, 6.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue() && (Boolean)this.doublePop.getValue();
      }));
      this.popTime = this.register(new Setting("PopTime", 500, 0, 1000, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue() && (Boolean)this.doublePop.getValue();
      }));
      this.explode = this.register(new Setting("Break", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK;
      }));
      this.switchMode = this.register(new Setting("Attack", AutoGondal.Switch.BREAKSLOT, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue();
      }));
      this.breakDelay = this.register(new Setting("BreakDelay", 50, 0, 500, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue();
      }));
      this.breakRange = this.register(new Setting("BreakRange", 6.0F, 0.0F, 10.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue();
      }));
      this.packets = this.register(new Setting("Packets", 1, 1, 6, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue();
      }));
      this.maxSelfBreak = this.register(new Setting("MaxSelfBreak", 10.0F, 0.1F, 36.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue();
      }));
      this.breaktrace = this.register(new Setting("Breaktrace", 4.5F, 0.0F, 10.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue() && this.raytrace.getValue() != AutoGondal.Raytrace.NONE && this.raytrace.getValue() != AutoGondal.Raytrace.PLACE;
      }));
      this.manual = this.register(new Setting("Manual", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK;
      }));
      this.manualMinDmg = this.register(new Setting("ManMinDmg", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.manual.getValue();
      }));
      this.manualBreak = this.register(new Setting("ManualDelay", 500, 0, 500, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.manual.getValue();
      }));
      this.sync = this.register(new Setting("Sync", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && ((Boolean)this.explode.getValue() || (Boolean)this.manual.getValue());
      }));
      this.instant = this.register(new Setting("Predict", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue() && (Boolean)this.place.getValue();
      }));
      this.instantTimer = this.register(new Setting("PredictTimer", AutoGondal.PredictTimer.NONE, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue() && (Boolean)this.place.getValue() && (Boolean)this.instant.getValue();
      }));
      this.resetBreakTimer = this.register(new Setting("ResetBreakTimer", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue() && (Boolean)this.place.getValue() && (Boolean)this.instant.getValue();
      }));
      this.predictDelay = this.register(new Setting("PredictDelay", 12, 0, 500, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue() && (Boolean)this.place.getValue() && (Boolean)this.instant.getValue() && this.instantTimer.getValue() == AutoGondal.PredictTimer.PREDICT;
      }));
      this.predictCalc = this.register(new Setting("PredictCalc", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue() && (Boolean)this.place.getValue() && (Boolean)this.instant.getValue();
      }));
      this.superSafe = this.register(new Setting("SuperSafe", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue() && (Boolean)this.place.getValue() && (Boolean)this.instant.getValue();
      }));
      this.antiCommit = this.register(new Setting("AntiOverCommit", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.BREAK && (Boolean)this.explode.getValue() && (Boolean)this.place.getValue() && (Boolean)this.instant.getValue();
      }));
      this.render = this.register(new Setting("Render", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER;
      }));
      this.red = this.register(new Setting("Red", 255, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue();
      }));
      this.green = this.register(new Setting("Green", 255, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue();
      }));
      this.blue = this.register(new Setting("Blue", 255, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue();
      }));
      this.alpha = this.register(new Setting("Alpha", 255, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue();
      }));
      this.colorSync = this.register(new Setting("CSync", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER;
      }));
      this.box = this.register(new Setting("Box", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue();
      }));
      this.boxAlpha = this.register(new Setting("BoxAlpha", 125, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue() && (Boolean)this.box.getValue();
      }));
      this.outline = this.register(new Setting("Outline", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue();
      }));
      this.lineWidth = this.register(new Setting("LineWidth", 1.5F, 0.1F, 5.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue() && (Boolean)this.outline.getValue();
      }));
      this.text = this.register(new Setting("Text", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue();
      }));
      this.customOutline = this.register(new Setting("CustomLine", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue() && (Boolean)this.outline.getValue();
      }));
      this.cRed = this.register(new Setting("OL-Red", 255, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue() && (Boolean)this.customOutline.getValue() && (Boolean)this.outline.getValue();
      }));
      this.cGreen = this.register(new Setting("OL-Green", 255, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue() && (Boolean)this.customOutline.getValue() && (Boolean)this.outline.getValue();
      }));
      this.cBlue = this.register(new Setting("OL-Blue", 255, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue() && (Boolean)this.customOutline.getValue() && (Boolean)this.outline.getValue();
      }));
      this.cAlpha = this.register(new Setting("OL-Alpha", 255, 0, 255, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.RENDER && (Boolean)this.render.getValue() && (Boolean)this.customOutline.getValue() && (Boolean)this.outline.getValue();
      }));
      this.holdFacePlace = this.register(new Setting("HoldFacePlace", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.holdFaceBreak = this.register(new Setting("HoldSlowBreak", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && (Boolean)this.holdFacePlace.getValue();
      }));
      this.slowFaceBreak = this.register(new Setting("SlowFaceBreak", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.actualSlowBreak = this.register(new Setting("ActuallySlow", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.facePlaceSpeed = this.register(new Setting("FaceSpeed", 500, 0, 500, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.antiNaked = this.register(new Setting("AntiNaked", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.range = this.register(new Setting("Range", 12.0F, 0.1F, 20.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.targetMode = this.register(new Setting("Target", AutoGondal.Target.CLOSEST, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.minArmor = this.register(new Setting("MinArmor", 5, 0, 125, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.autoSwitch = this.register(new Setting("Switch", AutoGondal.AutoSwitch.TOGGLE, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.switchBind = this.register(new Setting("SwitchBind", new Bind(-1), (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && this.autoSwitch.getValue() == AutoGondal.AutoSwitch.TOGGLE;
      }));
      this.offhandSwitch = this.register(new Setting("Offhand", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && this.autoSwitch.getValue() != AutoGondal.AutoSwitch.NONE;
      }));
      this.switchBack = this.register(new Setting("Switchback", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && this.autoSwitch.getValue() != AutoGondal.AutoSwitch.NONE && (Boolean)this.offhandSwitch.getValue();
      }));
      this.lethalSwitch = this.register(new Setting("LethalSwitch", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && this.autoSwitch.getValue() != AutoGondal.AutoSwitch.NONE;
      }));
      this.mineSwitch = this.register(new Setting("MineSwitch", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && this.autoSwitch.getValue() != AutoGondal.AutoSwitch.NONE;
      }));
      this.rotate = this.register(new Setting("Rotate", AutoGondal.Rotate.OFF, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.suicide = this.register(new Setting("Suicide", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.webAttack = this.register(new Setting("WebAttack", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && this.targetMode.getValue() != AutoGondal.Target.DAMAGE;
      }));
      this.fullCalc = this.register(new Setting("ExtraCalc", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.sound = this.register(new Setting("Sound", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.soundRange = this.register(new Setting("SoundRange", 12.0F, 0.0F, 12.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.soundPlayer = this.register(new Setting("SoundPlayer", 6.0F, 0.0F, 12.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.soundConfirm = this.register(new Setting("SoundConfirm", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.extraSelfCalc = this.register(new Setting("MinSelfDmg", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.antiFriendPop = this.register(new Setting("FriendPop", AutoGondal.AntiFriendPop.NONE, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.noCount = this.register(new Setting("AntiCount", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && (this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.ALL || this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.BREAK);
      }));
      this.calcEvenIfNoDamage = this.register(new Setting("BigFriendCalc", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && (this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.ALL || this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.BREAK) && this.targetMode.getValue() != AutoGondal.Target.DAMAGE;
      }));
      this.predictFriendDmg = this.register(new Setting("PredictFriend", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC && (this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.ALL || this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.BREAK) && (Boolean)this.instant.getValue();
      }));
      this.minMinDmg = this.register(new Setting("MinMinDmg", 0.0F, 0.0F, 3.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && (Boolean)this.place.getValue();
      }));
      this.breakSwing = this.register(new Setting("BreakSwing", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.placeSwing = this.register(new Setting("PlaceSwing", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.exactHand = this.register(new Setting("ExactHand", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && (Boolean)this.placeSwing.getValue();
      }));
      this.justRender = this.register(new Setting("JustRender", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.fakeSwing = this.register(new Setting("FakeSwing", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && (Boolean)this.justRender.getValue();
      }));
      this.logic = this.register(new Setting("Logic", AutoGondal.Logic.BREAKPLACE, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.damageSync = this.register(new Setting("DamageSync", AutoGondal.DamageSync.NONE, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.damageSyncTime = this.register(new Setting("SyncDelay", 500, 0, 500, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE;
      }));
      this.dropOff = this.register(new Setting("DropOff", 5.0F, 0.0F, 10.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() == AutoGondal.DamageSync.BREAK;
      }));
      this.confirm = this.register(new Setting("Confirm", 250, 0, 1000, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE;
      }));
      this.syncedFeetPlace = this.register(new Setting("FeetSync", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE;
      }));
      this.fullSync = this.register(new Setting("FullSync", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && (Boolean)this.syncedFeetPlace.getValue();
      }));
      this.syncCount = this.register(new Setting("SyncCount", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && (Boolean)this.syncedFeetPlace.getValue();
      }));
      this.hyperSync = this.register(new Setting("HyperSync", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && (Boolean)this.syncedFeetPlace.getValue();
      }));
      this.gigaSync = this.register(new Setting("GigaSync", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && (Boolean)this.syncedFeetPlace.getValue();
      }));
      this.syncySync = this.register(new Setting("SyncySync", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && (Boolean)this.syncedFeetPlace.getValue();
      }));
      this.enormousSync = this.register(new Setting("EnormousSync", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && (Boolean)this.syncedFeetPlace.getValue();
      }));
      this.holySync = this.register(new Setting("UnbelievableSync", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && (Boolean)this.syncedFeetPlace.getValue();
      }));
      this.rotateFirst = this.register(new Setting("FirstRotation", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.rotate.getValue() != AutoGondal.Rotate.OFF && (Integer)this.eventMode.getValue() == 2;
      }));
      this.threadMode = this.register(new Setting("Thread", AutoGondal.ThreadMode.NONE, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.threadDelay = this.register(new Setting("ThreadDelay", 50, 1, 1000, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.threadMode.getValue() != AutoGondal.ThreadMode.NONE;
      }));
      this.syncThreadBool = this.register(new Setting("ThreadSync", true, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.threadMode.getValue() != AutoGondal.ThreadMode.NONE;
      }));
      this.syncThreads = this.register(new Setting("SyncThreads", 1000, 1, 10000, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && this.threadMode.getValue() != AutoGondal.ThreadMode.NONE && (Boolean)this.syncThreadBool.getValue();
      }));
      this.predictPos = this.register(new Setting("PredictPos", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.renderExtrapolation = this.register(new Setting("RenderExtrapolation", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && (Boolean)this.predictPos.getValue();
      }));
      this.predictTicks = this.register(new Setting("ExtrapolationTicks", 2, 1, 20, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV && (Boolean)this.predictPos.getValue();
      }));
      this.rotations = this.register(new Setting("Spoofs", 1, 1, 20, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.predictRotate = this.register(new Setting("PredictRotate", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.predictOffset = this.register(new Setting("PredictOffset", 0.0F, 0.0F, 4.0F, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.DEV;
      }));
      this.brownZombie = this.register(new Setting("BrownZombieMode", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.MISC;
      }));
      this.doublePopOnDamage = this.register(new Setting("DamagePop", false, (v) -> {
         return this.setting.getValue() == AutoGondal.Settings.PLACE && (Boolean)this.place.getValue() && (Boolean)this.doublePop.getValue() && this.targetMode.getValue() == AutoGondal.Target.DAMAGE;
      }));
      this.rotating = false;
      this.attackList = new ConcurrentLinkedQueue();
      this.crystalMap = new HashMap();
      this.efficientTarget = null;
      this.currentDamage = 0.0D;
      this.renderDamage = 0.0D;
      this.lastDamage = 0.0D;
      this.didRotation = false;
      this.switching = false;
      this.placePos = null;
      this.renderPos = null;
      this.mainHand = false;
      this.offHand = false;
      this.crystalCount = 0;
      this.minDmgCount = 0;
      this.lastSlot = -1;
      this.yaw = 0.0F;
      this.pitch = 0.0F;
      this.webPos = null;
      this.lastPos = null;
      this.posConfirmed = false;
      this.foundDoublePop = false;
      this.rotationPacketsSpoofed = 0;
      instance = this;
   }

   public static AutoGondal getInstance() {
      if (instance == null) {
         instance = new AutoGondal();
      }

      return instance;
   }

   public void onTick() {
      if (this.threadMode.getValue() == AutoGondal.ThreadMode.NONE && (Integer)this.eventMode.getValue() == 3) {
         this.doAutoGondal();
      }

   }

   @SubscribeEvent
   public void onUpdateWalkingPlayer(UpdateWalkingPlayerEvent event) {
      if (event.getStage() == 1) {
         this.postProcessing();
      }

      if (event.getStage() == 0) {
         if ((Integer)this.eventMode.getValue() == 2) {
            this.doAutoGondal();
         }

      }
   }

   public void postTick() {
      if (this.threadMode.getValue() != AutoGondal.ThreadMode.NONE) {
         this.processMultiThreading();
      }

   }

   public void onUpdate() {
      if (this.threadMode.getValue() == AutoGondal.ThreadMode.NONE && (Integer)this.eventMode.getValue() == 1) {
         this.doAutoGondal();
      }

   }

   public void onToggle() {
      brokenPos.clear();
      placedPos.clear();
      this.totemPops.clear();
      this.rotating = false;
   }

   public void onDisable() {
      if (this.thread != null) {
         this.shouldInterrupt.set(true);
      }

      if (this.executor != null) {
         this.executor.shutdown();
      }

      super.onDisable();
   }

   public void onEnable() {
      if (this.threadMode.getValue() != AutoGondal.ThreadMode.NONE) {
         this.processMultiThreading();
      }

      super.onEnable();
   }

   public String getDisplayInfo() {
      if (this.switching) {
         return "§aSwitch";
      } else {
         return target != null ? target.func_70005_c_() : null;
      }
   }

   @SubscribeEvent
   public void onPacketSend(PacketEvent.Send event) {
      if (event.getStage() == 0 && this.rotate.getValue() != AutoGondal.Rotate.OFF && this.rotating && (Integer)this.eventMode.getValue() != 2 && event.getPacket() instanceof CPacketPlayer) {
         CPacketPlayer packet2 = (CPacketPlayer)event.getPacket();
         packet2.field_149476_e = this.yaw;
         packet2.field_149473_f = this.pitch;
         ++this.rotationPacketsSpoofed;
         if (this.rotationPacketsSpoofed >= (Integer)this.rotations.getValue()) {
            this.rotating = false;
            this.rotationPacketsSpoofed = 0;
         }
      }

      BlockPos pos = null;
      CPacketUseEntity packet;
      if (event.getStage() == 0 && event.getPacket() instanceof CPacketUseEntity && (packet = (CPacketUseEntity)event.getPacket()).func_149565_c() == Action.ATTACK && packet.func_149564_a(mc.field_71441_e) instanceof EntityEnderCrystal) {
         pos = packet.func_149564_a(mc.field_71441_e).func_180425_c();
         if ((Boolean)this.removeAfterAttack.getValue()) {
            ((Entity)Objects.requireNonNull(packet.func_149564_a(mc.field_71441_e))).func_70106_y();
            mc.field_71441_e.func_73028_b(packet.field_149567_a);
         }
      }

      if (event.getStage() == 0 && event.getPacket() instanceof CPacketUseEntity && (packet = (CPacketUseEntity)event.getPacket()).func_149565_c() == Action.ATTACK && packet.func_149564_a(mc.field_71441_e) instanceof EntityEnderCrystal) {
         EntityEnderCrystal crystal = (EntityEnderCrystal)packet.func_149564_a(mc.field_71441_e);
         if ((Boolean)this.antiBlock.getValue() && EntityUtil.isCrystalAtFeet(crystal, (double)(Float)this.range.getValue()) && pos != null) {
            this.rotateToPos(pos);
            BlockUtil.placeCrystalOnBlock(this.placePos, this.offHand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, (Boolean)this.placeSwing.getValue(), (Boolean)this.exactHand.getValue());
         }
      }

   }

   @SubscribeEvent(
      priority = EventPriority.HIGH,
      receiveCanceled = true
   )
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck()) {
         BlockPos pos;
         if (!(Boolean)this.justRender.getValue() && this.switchTimer.passedMs((long)(Integer)this.switchCooldown.getValue()) && (Boolean)this.explode.getValue() && (Boolean)this.instant.getValue() && event.getPacket() instanceof SPacketSpawnObject && (this.syncedCrystalPos == null || !(Boolean)this.syncedFeetPlace.getValue() || this.damageSync.getValue() == AutoGondal.DamageSync.NONE)) {
            SPacketSpawnObject packet2 = (SPacketSpawnObject)event.getPacket();
            if (packet2.func_148993_l() == 51 && mc.field_71439_g.func_174818_b(pos = new BlockPos(packet2.func_186880_c(), packet2.func_186882_d(), packet2.func_186881_e())) + (double)(Float)this.predictOffset.getValue() <= MathUtil.square((double)(Float)this.breakRange.getValue()) && (this.instantTimer.getValue() == AutoGondal.PredictTimer.NONE || this.instantTimer.getValue() == AutoGondal.PredictTimer.BREAK && this.breakTimer.passedMs((long)(Integer)this.breakDelay.getValue()) || this.instantTimer.getValue() == AutoGondal.PredictTimer.PREDICT && this.predictTimer.passedMs((long)(Integer)this.predictDelay.getValue()))) {
               if (this.predictSlowBreak(pos.func_177977_b())) {
                  return;
               }

               if ((Boolean)this.predictFriendDmg.getValue() && (this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.BREAK || this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.ALL) && this.isRightThread()) {
                  Iterator var14 = mc.field_71441_e.field_73010_i.iterator();

                  while(var14.hasNext()) {
                     EntityPlayer friend = (EntityPlayer)var14.next();
                     if (friend != null && !mc.field_71439_g.equals(friend) && !(friend.func_174818_b(pos) > MathUtil.square((double)((Float)this.range.getValue() + (Float)this.placeRange.getValue()))) && esohack.friendManager.isFriend(friend) && (double)DamageUtil.calculateDamage((BlockPos)pos, friend) > (double)EntityUtil.getHealth(friend) + 0.5D) {
                        return;
                     }
                  }
               }

               float selfDamage;
               if (!placedPos.contains(pos.func_177977_b())) {
                  if ((Boolean)this.predictCalc.getValue() && this.isRightThread()) {
                     selfDamage = -1.0F;
                     if (DamageUtil.canTakeDamage((Boolean)this.suicide.getValue())) {
                        selfDamage = DamageUtil.calculateDamage((BlockPos)pos, mc.field_71439_g);
                     }

                     if ((double)selfDamage + 0.5D < (double)EntityUtil.getHealth(mc.field_71439_g) && selfDamage <= (Float)this.maxSelfBreak.getValue()) {
                        Iterator var17 = mc.field_71441_e.field_73010_i.iterator();

                        EntityPlayer player;
                        float damage;
                        do {
                           do {
                              do {
                                 do {
                                    if (!var17.hasNext()) {
                                       return;
                                    }

                                    player = (EntityPlayer)var17.next();
                                 } while(!(player.func_174818_b(pos) <= MathUtil.square((double)(Float)this.range.getValue())));
                              } while(!EntityUtil.isValid(player, (double)((Float)this.range.getValue() + (Float)this.breakRange.getValue())));
                           } while((Boolean)this.antiNaked.getValue() && DamageUtil.isNaked(player));
                        } while(!((damage = DamageUtil.calculateDamage((BlockPos)pos, player)) > selfDamage) && (!(damage > (Float)this.minDamage.getValue()) || DamageUtil.canTakeDamage((Boolean)this.suicide.getValue())) && !(damage > EntityUtil.getHealth(player)));

                        if ((Boolean)this.predictRotate.getValue() && (Integer)this.eventMode.getValue() != 2 && (this.rotate.getValue() == AutoGondal.Rotate.BREAK || this.rotate.getValue() == AutoGondal.Rotate.ALL)) {
                           this.rotateToPos(pos);
                        }

                        this.attackCrystalPredict(packet2.func_149001_c(), pos);
                     }
                  }
               } else {
                  if (this.isRightThread() && (Boolean)this.superSafe.getValue()) {
                     if (DamageUtil.canTakeDamage((Boolean)this.suicide.getValue()) && ((double)(selfDamage = DamageUtil.calculateDamage((BlockPos)pos, mc.field_71439_g)) - 0.5D > (double)EntityUtil.getHealth(mc.field_71439_g) || selfDamage > (Float)this.maxSelfBreak.getValue())) {
                        return;
                     }
                  } else if ((Boolean)this.superSafe.getValue()) {
                     return;
                  }

                  this.attackCrystalPredict(packet2.func_149001_c(), pos);
                  return;
               }
            }
         } else if (!(Boolean)this.soundConfirm.getValue() && event.getPacket() instanceof SPacketExplosion) {
            SPacketExplosion packet3 = (SPacketExplosion)event.getPacket();
            BlockPos pos = (new BlockPos(packet3.func_149148_f(), packet3.func_149143_g(), packet3.func_149145_h())).func_177977_b();
            this.removePos(pos);
         } else if (event.getPacket() instanceof SPacketDestroyEntities) {
            SPacketDestroyEntities packet4 = (SPacketDestroyEntities)event.getPacket();
            int[] var4 = packet4.func_149098_c();
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               int id = var4[var6];
               Entity entity = mc.field_71441_e.func_73045_a(id);
               if (entity instanceof EntityEnderCrystal) {
                  brokenPos.remove((new BlockPos(entity.func_174791_d())).func_177977_b());
                  placedPos.remove((new BlockPos(entity.func_174791_d())).func_177977_b());
               }
            }
         } else if (event.getPacket() instanceof SPacketEntityStatus) {
            SPacketEntityStatus packet5 = (SPacketEntityStatus)event.getPacket();
            if (packet5.func_149160_c() == 35 && packet5.func_149161_a(mc.field_71441_e) instanceof EntityPlayer) {
               this.totemPops.put((EntityPlayer)packet5.func_149161_a(mc.field_71441_e), (new Timer()).reset());
            }
         } else {
            SPacketSoundEffect packet;
            if (event.getPacket() instanceof SPacketSoundEffect && (packet = (SPacketSoundEffect)event.getPacket()).func_186977_b() == SoundCategory.BLOCKS && packet.func_186978_a() == SoundEvents.field_187539_bB) {
               pos = new BlockPos(packet.func_149207_d(), packet.func_149211_e(), packet.func_149210_f());
               if ((Boolean)this.sound.getValue() || this.threadMode.getValue() == AutoGondal.ThreadMode.SOUND) {
                  removeEntities(packet, (Float)this.soundRange.getValue());
               }

               if ((Boolean)this.soundConfirm.getValue()) {
                  this.removePos(pos);
               }

               if (this.threadMode.getValue() == AutoGondal.ThreadMode.SOUND && this.isRightThread() && mc.field_71439_g != null && mc.field_71439_g.func_174818_b(pos) < MathUtil.square((double)(Float)this.soundPlayer.getValue())) {
                  this.handlePool(true);
               }
            }
         }

      }
   }

   public static void removeEntities(SPacketSoundEffect packet, float range) {
      BlockPos pos = new BlockPos(packet.func_149207_d(), packet.func_149211_e(), packet.func_149210_f());
      ArrayList<Entity> toRemove = new ArrayList();
      Iterator var4 = mc.field_71441_e.field_72996_f.iterator();

      Entity entity;
      while(var4.hasNext()) {
         entity = (Entity)var4.next();
         if (entity instanceof EntityEnderCrystal && entity.func_174818_b(pos) <= MathUtil.square((double)range)) {
            toRemove.add(entity);
         }
      }

      var4 = toRemove.iterator();

      while(var4.hasNext()) {
         entity = (Entity)var4.next();
         entity.func_70106_y();
      }

   }

   private boolean predictSlowBreak(BlockPos pos) {
      return (Boolean)this.antiCommit.getValue() && lowDmgPos.remove(pos) ? this.shouldSlowBreak(false) : false;
   }

   private boolean isRightThread() {
      return mc.func_152345_ab() || !esohack.eventManager.ticksOngoing() && !this.threadOngoing.get();
   }

   private void attackCrystalPredict(int entityID, BlockPos pos) {
      if ((Boolean)this.predictRotate.getValue() && ((Integer)this.eventMode.getValue() != 2 || this.threadMode.getValue() != AutoGondal.ThreadMode.NONE) && (this.rotate.getValue() == AutoGondal.Rotate.BREAK || this.rotate.getValue() == AutoGondal.Rotate.ALL)) {
         this.rotateToPos(pos);
      }

      CPacketUseEntity attackPacket = new CPacketUseEntity();
      attackPacket.field_149567_a = entityID;
      attackPacket.field_149566_b = Action.ATTACK;
      mc.field_71439_g.field_71174_a.func_147297_a(attackPacket);
      if ((Boolean)this.breakSwing.getValue()) {
         mc.field_71439_g.field_71174_a.func_147297_a(new CPacketAnimation(EnumHand.MAIN_HAND));
      }

      if ((Boolean)this.resetBreakTimer.getValue()) {
         this.breakTimer.reset();
      }

      this.predictTimer.reset();
   }

   private void removePos(BlockPos pos) {
      if (this.damageSync.getValue() == AutoGondal.DamageSync.PLACE) {
         if (placedPos.remove(pos)) {
            this.posConfirmed = true;
         }
      } else if (this.damageSync.getValue() == AutoGondal.DamageSync.BREAK && brokenPos.remove(pos)) {
         this.posConfirmed = true;
      }

   }

   public void onRender3D(Render3DEvent event) {
      if ((this.offHand || this.mainHand || this.switchMode.getValue() == AutoGondal.Switch.CALC) && this.renderPos != null && (Boolean)this.render.getValue() && ((Boolean)this.box.getValue() || (Boolean)this.text.getValue() || (Boolean)this.outline.getValue())) {
         RenderUtil.drawBoxESP(this.renderPos, (Boolean)this.colorSync.getValue() ? Colors.INSTANCE.getCurrentColor() : new Color((Integer)this.red.getValue(), (Integer)this.green.getValue(), (Integer)this.blue.getValue(), (Integer)this.alpha.getValue()), (Boolean)this.customOutline.getValue(), (Boolean)this.colorSync.getValue() ? Colors.INSTANCE.getCurrentColor() : new Color((Integer)this.cRed.getValue(), (Integer)this.cGreen.getValue(), (Integer)this.cBlue.getValue(), (Integer)this.cAlpha.getValue()), (Float)this.lineWidth.getValue(), (Boolean)this.outline.getValue(), (Boolean)this.box.getValue(), (Integer)this.boxAlpha.getValue(), false);
         if ((Boolean)this.text.getValue()) {
            RenderUtil.drawText(this.renderPos, (Math.floor(this.renderDamage) == this.renderDamage ? (int)this.renderDamage : String.format("%.1f", this.renderDamage)) + "");
         }
      }

   }

   @SubscribeEvent
   public void onKeyInput(KeyInputEvent event) {
      if (Keyboard.getEventKeyState() && !(mc.field_71462_r instanceof esohackGui) && ((Bind)this.switchBind.getValue()).getKey() == Keyboard.getEventKey()) {
         if ((Boolean)this.switchBack.getValue() && (Boolean)this.offhandSwitch.getValue() && this.offHand) {
            Offhand module = (Offhand)esohack.moduleManager.getModuleByClass(Offhand.class);
            if (module.isOff()) {
               Command.sendMessage("<" + this.getDisplayName() + "> §cSwitch failed. Enable the Offhand module.");
            } else {
               module.setMode(Offhand.Mode2.TOTEMS);
               module.doSwitch();
            }

            return;
         }

         this.switching = !this.switching;
      }

   }

   @SubscribeEvent
   public void onSettingChange(ClientEvent event) {
      if (event.getStage() == 2 && event.getSetting() != null && event.getSetting().getFeature() != null && event.getSetting().getFeature().equals(this) && this.isEnabled() && (event.getSetting().equals(this.threadDelay) || event.getSetting().equals(this.threadMode))) {
         if (this.executor != null) {
            this.executor.shutdown();
         }

         if (this.thread != null) {
            this.shouldInterrupt.set(true);
         }
      }

   }

   private void postProcessing() {
      if (this.threadMode.getValue() == AutoGondal.ThreadMode.NONE && (Integer)this.eventMode.getValue() == 2 && this.rotate.getValue() != AutoGondal.Rotate.OFF && (Boolean)this.rotateFirst.getValue()) {
         switch((AutoGondal.Logic)this.logic.getValue()) {
         case BREAKPLACE:
            this.postProcessBreak();
            this.postProcessPlace();
            break;
         case PLACEBREAK:
            this.postProcessPlace();
            this.postProcessBreak();
         }

      }
   }

   private void postProcessBreak() {
      for(; !this.packetUseEntities.isEmpty(); this.breakTimer.reset()) {
         CPacketUseEntity packet = (CPacketUseEntity)this.packetUseEntities.poll();
         mc.field_71439_g.field_71174_a.func_147297_a(packet);
         if ((Boolean)this.breakSwing.getValue()) {
            mc.field_71439_g.func_184609_a(EnumHand.MAIN_HAND);
         }
      }

   }

   private void postProcessPlace() {
      if (this.placeInfo != null) {
         this.placeInfo.runPlace();
         this.placeTimer.reset();
         this.placeInfo = null;
      }

   }

   private void processMultiThreading() {
      if (!this.isOff()) {
         if (this.threadMode.getValue() == AutoGondal.ThreadMode.WHILE) {
            this.handleWhile();
         } else if (this.threadMode.getValue() != AutoGondal.ThreadMode.NONE) {
            this.handlePool(false);
         }

      }
   }

   private void handlePool(boolean justDoIt) {
      if (justDoIt || this.executor == null || this.executor.isTerminated() || this.executor.isShutdown() || this.syncroTimer.passedMs((long)(Integer)this.syncThreads.getValue()) && (Boolean)this.syncThreadBool.getValue()) {
         if (this.executor != null) {
            this.executor.shutdown();
         }

         this.executor = this.getExecutor();
         this.syncroTimer.reset();
      }

   }

   private void handleWhile() {
      if (this.thread == null || this.thread.isInterrupted() || !this.thread.isAlive() || this.syncroTimer.passedMs((long)(Integer)this.syncThreads.getValue()) && (Boolean)this.syncThreadBool.getValue()) {
         if (this.thread == null) {
            this.thread = new Thread(AutoGondal.RAutoGondal.getInstance(this));
         } else if (this.syncroTimer.passedMs((long)(Integer)this.syncThreads.getValue()) && !this.shouldInterrupt.get() && (Boolean)this.syncThreadBool.getValue()) {
            this.shouldInterrupt.set(true);
            this.syncroTimer.reset();
            return;
         }

         if (this.thread != null && (this.thread.isInterrupted() || !this.thread.isAlive())) {
            this.thread = new Thread(AutoGondal.RAutoGondal.getInstance(this));
         }

         if (this.thread != null && this.thread.getState() == State.NEW) {
            try {
               this.thread.start();
            } catch (Exception var2) {
               var2.printStackTrace();
            }

            this.syncroTimer.reset();
         }
      }

   }

   private ScheduledExecutorService getExecutor() {
      ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
      service.scheduleAtFixedRate(AutoGondal.RAutoGondal.getInstance(this), 0L, (long)(Integer)this.threadDelay.getValue(), TimeUnit.MILLISECONDS);
      return service;
   }

   public void doAutoGondal() {
      if (!(Boolean)this.brownZombie.getValue()) {
         if (this.check()) {
            switch((AutoGondal.Logic)this.logic.getValue()) {
            case BREAKPLACE:
               this.breakCrystal();
               this.placeCrystal();
               break;
            case PLACEBREAK:
               this.placeCrystal();
               this.breakCrystal();
            }

            this.manualBreaker();
         }

      }
   }

   private boolean check() {
      if (fullNullCheck()) {
         return false;
      } else {
         if (this.syncTimer.passedMs((long)(Integer)this.damageSyncTime.getValue())) {
            this.currentSyncTarget = null;
            this.syncedCrystalPos = null;
            this.syncedPlayerPos = null;
         } else if ((Boolean)this.syncySync.getValue() && this.syncedCrystalPos != null) {
            this.posConfirmed = true;
         }

         this.foundDoublePop = false;
         if (this.renderTimer.passedMs(500L)) {
            this.renderPos = null;
            this.renderTimer.reset();
         }

         this.mainHand = mc.field_71439_g.func_184614_ca().func_77973_b() == Items.field_185158_cP;
         this.offHand = mc.field_71439_g.func_184592_cb().func_77973_b() == Items.field_185158_cP;
         this.currentDamage = 0.0D;
         this.placePos = null;
         if (this.lastSlot != mc.field_71439_g.field_71071_by.field_70461_c || AutoTrap.isPlacing || Surround.isPlacing) {
            this.lastSlot = mc.field_71439_g.field_71071_by.field_70461_c;
            this.switchTimer.reset();
         }

         if (!this.offHand && !this.mainHand) {
            this.placeInfo = null;
            this.packetUseEntities.clear();
         }

         if (this.offHand || this.mainHand) {
            this.switching = false;
         }

         if ((this.offHand || this.mainHand || this.switchMode.getValue() != AutoGondal.Switch.BREAKSLOT || this.switching) && DamageUtil.canBreakWeakness(mc.field_71439_g) && this.switchTimer.passedMs((long)(Integer)this.switchCooldown.getValue())) {
            if ((Boolean)this.mineSwitch.getValue() && Mouse.isButtonDown(0) && (this.switching || this.autoSwitch.getValue() == AutoGondal.AutoSwitch.ALWAYS) && Mouse.isButtonDown(1) && mc.field_71439_g.func_184614_ca().func_77973_b() instanceof ItemPickaxe) {
               this.switchItem();
            }

            this.mapCrystals();
            if (!this.posConfirmed && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && this.syncTimer.passedMs((long)(Integer)this.confirm.getValue())) {
               this.syncTimer.setMs((long)((Integer)this.damageSyncTime.getValue() + 1));
            }

            return true;
         } else {
            this.renderPos = null;
            target = null;
            this.rotating = false;
            return false;
         }
      }
   }

   private void mapCrystals() {
      this.efficientTarget = null;
      if ((Integer)this.packets.getValue() != 1) {
         this.attackList = new ConcurrentLinkedQueue();
         this.crystalMap = new HashMap();
      }

      this.crystalCount = 0;
      this.minDmgCount = 0;
      Entity maxCrystal = null;
      float maxDamage = 0.5F;
      Iterator var3 = mc.field_71441_e.field_72996_f.iterator();

      Entity entity;
      while(var3.hasNext()) {
         entity = (Entity)var3.next();
         if (!entity.field_70128_L && entity instanceof EntityEnderCrystal && this.isValid(entity)) {
            if ((Boolean)this.syncedFeetPlace.getValue() && entity.func_180425_c().func_177977_b().equals(this.syncedCrystalPos) && this.damageSync.getValue() != AutoGondal.DamageSync.NONE) {
               ++this.minDmgCount;
               ++this.crystalCount;
               if ((Boolean)this.syncCount.getValue()) {
                  this.minDmgCount = (Integer)this.wasteAmount.getValue() + 1;
                  this.crystalCount = (Integer)this.wasteAmount.getValue() + 1;
               }

               if ((Boolean)this.hyperSync.getValue()) {
                  maxCrystal = null;
                  break;
               }
            } else {
               boolean count = false;
               boolean countMin = false;
               float selfDamage = -1.0F;
               if (DamageUtil.canTakeDamage((Boolean)this.suicide.getValue())) {
                  selfDamage = DamageUtil.calculateDamage((Entity)entity, mc.field_71439_g);
               }

               if ((double)selfDamage + 0.5D < (double)EntityUtil.getHealth(mc.field_71439_g) && selfDamage <= (Float)this.maxSelfBreak.getValue()) {
                  Iterator var10 = mc.field_71441_e.field_73010_i.iterator();

                  label218:
                  while(true) {
                     while(true) {
                        EntityPlayer player;
                        float damage;
                        do {
                           label193:
                           do {
                              do {
                                 do {
                                    if (!var10.hasNext()) {
                                       break label218;
                                    }

                                    player = (EntityPlayer)var10.next();
                                 } while(!(player.func_70068_e(entity) <= MathUtil.square((double)(Float)this.range.getValue())));

                                 if (EntityUtil.isValid(player, (double)((Float)this.range.getValue() + (Float)this.breakRange.getValue()))) {
                                    continue label193;
                                 }
                              } while(this.antiFriendPop.getValue() != AutoGondal.AntiFriendPop.BREAK && this.antiFriendPop.getValue() != AutoGondal.AntiFriendPop.ALL || !esohack.friendManager.isFriend(player.func_70005_c_()) || !((double)DamageUtil.calculateDamage((Entity)entity, player) > (double)EntityUtil.getHealth(player) + 0.5D));

                              maxCrystal = maxCrystal;
                              maxDamage = maxDamage;
                              this.crystalMap.remove(entity);
                              if ((Boolean)this.noCount.getValue()) {
                                 count = false;
                                 countMin = false;
                              }
                              break label218;
                           } while((Boolean)this.antiNaked.getValue() && DamageUtil.isNaked(player));
                        } while(!((damage = DamageUtil.calculateDamage((Entity)entity, player)) > selfDamage) && (!(damage > (Float)this.minDamage.getValue()) || DamageUtil.canTakeDamage((Boolean)this.suicide.getValue())) && !(damage > EntityUtil.getHealth(player)));

                        if (damage > maxDamage) {
                           maxDamage = damage;
                           maxCrystal = entity;
                        }

                        if ((Integer)this.packets.getValue() == 1) {
                           if (damage >= (Float)this.minDamage.getValue() || !(Boolean)this.wasteMinDmgCount.getValue()) {
                              count = true;
                           }

                           countMin = true;
                        } else if (this.crystalMap.get(entity) == null || (Float)this.crystalMap.get(entity) < damage) {
                           this.crystalMap.put(entity, damage);
                        }
                     }
                  }
               }

               if (countMin) {
                  ++this.minDmgCount;
                  if (count) {
                     ++this.crystalCount;
                  }
               }
            }
         }
      }

      if (this.damageSync.getValue() == AutoGondal.DamageSync.BREAK && ((double)maxDamage > this.lastDamage || this.syncTimer.passedMs((long)(Integer)this.damageSyncTime.getValue()) || this.damageSync.getValue() == AutoGondal.DamageSync.NONE)) {
         this.lastDamage = (double)maxDamage;
      }

      if ((Boolean)this.enormousSync.getValue() && (Boolean)this.syncedFeetPlace.getValue() && this.damageSync.getValue() != AutoGondal.DamageSync.NONE && this.syncedCrystalPos != null) {
         if ((Boolean)this.syncCount.getValue()) {
            this.minDmgCount = (Integer)this.wasteAmount.getValue() + 1;
            this.crystalCount = (Integer)this.wasteAmount.getValue() + 1;
         }

      } else {
         if ((Boolean)this.webAttack.getValue() && this.webPos != null) {
            if (mc.field_71439_g.func_174818_b(this.webPos.func_177984_a()) > MathUtil.square((double)(Float)this.breakRange.getValue())) {
               this.webPos = null;
            } else {
               var3 = mc.field_71441_e.func_72872_a(Entity.class, new AxisAlignedBB(this.webPos.func_177984_a())).iterator();

               while(var3.hasNext()) {
                  entity = (Entity)var3.next();
                  if (entity instanceof EntityEnderCrystal) {
                     this.attackList.add(entity);
                     this.efficientTarget = entity;
                     this.webPos = null;
                     this.lastDamage = 0.5D;
                     return;
                  }
               }
            }
         }

         if (!this.shouldSlowBreak(true) || !(maxDamage < (Float)this.minDamage.getValue()) || target != null && EntityUtil.getHealth(target) <= (Float)this.facePlace.getValue() && (this.breakTimer.passedMs((long)(Integer)this.facePlaceSpeed.getValue()) || !(Boolean)this.slowFaceBreak.getValue() || !Mouse.isButtonDown(0) || !(Boolean)this.holdFacePlace.getValue() || !(Boolean)this.holdFaceBreak.getValue())) {
            if ((Integer)this.packets.getValue() == 1) {
               this.efficientTarget = maxCrystal;
            } else {
               this.crystalMap = MathUtil.sortByValue(this.crystalMap, true);

               for(var3 = this.crystalMap.entrySet().iterator(); var3.hasNext(); ++this.minDmgCount) {
                  Entry entry = (Entry)var3.next();
                  Entity crystal = (Entity)entry.getKey();
                  float damage = (Float)entry.getValue();
                  if (damage >= (Float)this.minDamage.getValue() || !(Boolean)this.wasteMinDmgCount.getValue()) {
                     ++this.crystalCount;
                  }

                  this.attackList.add(crystal);
               }
            }

         } else {
            this.efficientTarget = null;
         }
      }
   }

   private boolean shouldSlowBreak(boolean withManual) {
      return withManual && (Boolean)this.manual.getValue() && (Boolean)this.manualMinDmg.getValue() && Mouse.isButtonDown(1) && (!Mouse.isButtonDown(0) || !(Boolean)this.holdFacePlace.getValue()) || (Boolean)this.holdFacePlace.getValue() && (Boolean)this.holdFaceBreak.getValue() && Mouse.isButtonDown(0) && !this.breakTimer.passedMs((long)(Integer)this.facePlaceSpeed.getValue()) || (Boolean)this.slowFaceBreak.getValue() && !this.breakTimer.passedMs((long)(Integer)this.facePlaceSpeed.getValue());
   }

   private void placeCrystal() {
      int crystalLimit = (Integer)this.wasteAmount.getValue();
      if (this.placeTimer.passedMs((long)(Integer)this.placeDelay.getValue()) && (Boolean)this.place.getValue() && (this.offHand || this.mainHand || this.switchMode.getValue() == AutoGondal.Switch.CALC || this.switchMode.getValue() == AutoGondal.Switch.BREAKSLOT && this.switching)) {
         if ((this.offHand || this.mainHand || this.switchMode.getValue() != AutoGondal.Switch.ALWAYS && !this.switching) && this.crystalCount >= crystalLimit && (!(Boolean)this.antiSurround.getValue() || this.lastPos == null || !this.lastPos.equals(this.placePos))) {
            return;
         }

         this.calculateDamage(this.getTarget(this.targetMode.getValue() == AutoGondal.Target.UNSAFE));
         if (target != null && this.placePos != null) {
            if (!this.offHand && !this.mainHand && this.autoSwitch.getValue() != AutoGondal.AutoSwitch.NONE && (this.currentDamage > (double)(Float)this.minDamage.getValue() || (Boolean)this.lethalSwitch.getValue() && EntityUtil.getHealth(target) <= (Float)this.facePlace.getValue()) && !this.switchItem()) {
               return;
            }

            if (this.currentDamage < (double)(Float)this.minDamage.getValue() && (Boolean)this.limitFacePlace.getValue()) {
               crystalLimit = 1;
            }

            if (this.currentDamage >= (double)(Float)this.minMinDmg.getValue() && (this.offHand || this.mainHand || this.autoSwitch.getValue() != AutoGondal.AutoSwitch.NONE) && (this.crystalCount < crystalLimit || (Boolean)this.antiSurround.getValue() && this.lastPos != null && this.lastPos.equals(this.placePos)) && (this.currentDamage > (double)(Float)this.minDamage.getValue() || this.minDmgCount < crystalLimit) && this.currentDamage >= 1.0D && (DamageUtil.isArmorLow(target, (Integer)this.minArmor.getValue()) || EntityUtil.getHealth(target) <= (Float)this.facePlace.getValue() || this.currentDamage > (double)(Float)this.minDamage.getValue() || this.shouldHoldFacePlace())) {
               float damageOffset = this.damageSync.getValue() == AutoGondal.DamageSync.BREAK ? (Float)this.dropOff.getValue() - 5.0F : 0.0F;
               boolean syncflag = false;
               if ((Boolean)this.syncedFeetPlace.getValue() && this.placePos.equals(this.lastPos) && this.isEligableForFeetSync(target, this.placePos) && !this.syncTimer.passedMs((long)(Integer)this.damageSyncTime.getValue()) && target.equals(this.currentSyncTarget) && target.func_180425_c().equals(this.syncedPlayerPos) && this.damageSync.getValue() != AutoGondal.DamageSync.NONE) {
                  this.syncedCrystalPos = this.placePos;
                  this.lastDamage = this.currentDamage;
                  if ((Boolean)this.fullSync.getValue()) {
                     this.lastDamage = 100.0D;
                  }

                  syncflag = true;
               }

               if (syncflag || this.currentDamage - (double)damageOffset > this.lastDamage || this.syncTimer.passedMs((long)(Integer)this.damageSyncTime.getValue()) || this.damageSync.getValue() == AutoGondal.DamageSync.NONE) {
                  if (!syncflag && this.damageSync.getValue() != AutoGondal.DamageSync.BREAK) {
                     this.lastDamage = this.currentDamage;
                  }

                  this.renderPos = this.placePos;
                  this.renderDamage = this.currentDamage;
                  if (this.switchItem()) {
                     this.currentSyncTarget = target;
                     this.syncedPlayerPos = target.func_180425_c();
                     if (this.foundDoublePop) {
                        this.totemPops.put(target, (new Timer()).reset());
                     }

                     this.rotateToPos(this.placePos);
                     if (this.addTolowDmg || (Boolean)this.actualSlowBreak.getValue() && this.currentDamage < (double)(Float)this.minDamage.getValue()) {
                        lowDmgPos.add(this.placePos);
                     }

                     placedPos.add(this.placePos);
                     if (!(Boolean)this.justRender.getValue()) {
                        if ((Integer)this.eventMode.getValue() == 2 && this.threadMode.getValue() == AutoGondal.ThreadMode.NONE && (Boolean)this.rotateFirst.getValue() && this.rotate.getValue() != AutoGondal.Rotate.OFF) {
                           this.placeInfo = new AutoGondal.PlaceInfo(this.placePos, this.offHand, (Boolean)this.placeSwing.getValue(), (Boolean)this.exactHand.getValue());
                        } else {
                           BlockUtil.placeCrystalOnBlock(this.placePos, this.offHand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, (Boolean)this.placeSwing.getValue(), (Boolean)this.exactHand.getValue());
                        }
                     }

                     this.lastPos = this.placePos;
                     this.placeTimer.reset();
                     this.posConfirmed = false;
                     if (this.syncTimer.passedMs((long)(Integer)this.damageSyncTime.getValue())) {
                        this.syncedCrystalPos = null;
                        this.syncTimer.reset();
                     }
                  }
               }
            }
         } else {
            this.renderPos = null;
         }
      }

   }

   private boolean shouldHoldFacePlace() {
      this.addTolowDmg = false;
      if ((Boolean)this.holdFacePlace.getValue() && Mouse.isButtonDown(0)) {
         this.addTolowDmg = true;
         return true;
      } else {
         return false;
      }
   }

   private boolean switchItem() {
      if (!this.offHand && !this.mainHand) {
         switch((AutoGondal.AutoSwitch)this.autoSwitch.getValue()) {
         case NONE:
            return false;
         case TOGGLE:
            if (!this.switching) {
               return false;
            }
         case ALWAYS:
            if (this.doSwitch()) {
               return true;
            }
         default:
            return false;
         }
      } else {
         return true;
      }
   }

   private boolean doSwitch() {
      if ((Boolean)this.offhandSwitch.getValue()) {
         Offhand module = (Offhand)esohack.moduleManager.getModuleByClass(Offhand.class);
         if (module.isOff()) {
            Command.sendMessage("<" + this.getDisplayName() + "> §cSwitch failed. Enable the Offhand module.");
            this.switching = false;
            return false;
         } else {
            module.setMode(Offhand.Mode2.CRYSTALS);
            module.doSwitch();
            this.switching = false;
            return true;
         }
      } else {
         if (mc.field_71439_g.func_184592_cb().func_77973_b() == Items.field_185158_cP) {
            this.mainHand = false;
         } else {
            InventoryUtil.switchToHotbarSlot(ItemEndCrystal.class, false);
            this.mainHand = true;
         }

         this.switching = false;
         return true;
      }
   }

   private void calculateDamage(EntityPlayer targettedPlayer) {
      if (targettedPlayer != null || this.targetMode.getValue() == AutoGondal.Target.DAMAGE || (Boolean)this.fullCalc.getValue()) {
         float maxDamage = 0.5F;
         EntityPlayer currentTarget = null;
         BlockPos currentPos = null;
         float maxSelfDamage = 0.0F;
         this.foundDoublePop = false;
         BlockPos setToAir = null;
         IBlockState state = null;
         BlockPos playerPos;
         if ((Boolean)this.webAttack.getValue() && targettedPlayer != null && mc.field_71441_e.func_180495_p(playerPos = new BlockPos(targettedPlayer.func_174791_d())).func_177230_c() == Blocks.field_150321_G) {
            setToAir = playerPos;
            state = mc.field_71441_e.func_180495_p(playerPos);
            mc.field_71441_e.func_175698_g(playerPos);
         }

         Iterator var10 = BlockUtil.possiblePlacePositions((Float)this.placeRange.getValue(), (Boolean)this.antiSurround.getValue(), (Boolean)this.oneDot15.getValue()).iterator();

         while(true) {
            while(true) {
               BlockPos pos;
               float selfDamage;
               float playerDamage;
               boolean friendPop;
               label204:
               do {
                  label179:
                  while(true) {
                     do {
                        do {
                           do {
                              if (!var10.hasNext()) {
                                 if (setToAir != null) {
                                    mc.field_71441_e.func_175656_a(setToAir, state);
                                    this.webPos = currentPos;
                                 }

                                 target = currentTarget;
                                 this.currentDamage = (double)maxDamage;
                                 this.placePos = currentPos;
                                 return;
                              }

                              pos = (BlockPos)var10.next();
                           } while(!BlockUtil.rayTracePlaceCheck(pos, (this.raytrace.getValue() == AutoGondal.Raytrace.PLACE || this.raytrace.getValue() == AutoGondal.Raytrace.FULL) && mc.field_71439_g.func_174818_b(pos) > MathUtil.square((double)(Float)this.placetrace.getValue()), 1.0F));

                           selfDamage = -1.0F;
                           if (DamageUtil.canTakeDamage((Boolean)this.suicide.getValue())) {
                              selfDamage = DamageUtil.calculateDamage((BlockPos)pos, mc.field_71439_g);
                           }
                        } while(!((double)selfDamage + 0.5D < (double)EntityUtil.getHealth(mc.field_71439_g)));
                     } while(!(selfDamage <= (Float)this.maxSelfPlace.getValue()));

                     if (targettedPlayer != null) {
                        playerDamage = DamageUtil.calculateDamage((BlockPos)pos, targettedPlayer);
                        if (!(Boolean)this.calcEvenIfNoDamage.getValue() || this.antiFriendPop.getValue() != AutoGondal.AntiFriendPop.ALL && this.antiFriendPop.getValue() != AutoGondal.AntiFriendPop.PLACE) {
                           break label204;
                        }

                        friendPop = false;
                        Iterator var15 = mc.field_71441_e.field_73010_i.iterator();

                        while(var15.hasNext()) {
                           EntityPlayer friend = (EntityPlayer)var15.next();
                           if (friend != null && !mc.field_71439_g.equals(friend) && !(friend.func_174818_b(pos) > MathUtil.square((double)((Float)this.range.getValue() + (Float)this.placeRange.getValue()))) && esohack.friendManager.isFriend(friend) && (double)DamageUtil.calculateDamage((BlockPos)pos, friend) > (double)EntityUtil.getHealth(friend) + 0.5D) {
                              friendPop = true;
                              continue label204;
                           }
                        }
                        break;
                     }

                     Iterator var17 = mc.field_71441_e.field_73010_i.iterator();

                     while(true) {
                        while(true) {
                           EntityPlayer player;
                           label150:
                           do {
                              do {
                                 if (!var17.hasNext()) {
                                    continue label179;
                                 }

                                 player = (EntityPlayer)var17.next();
                                 if (EntityUtil.isValid(player, (double)((Float)this.placeRange.getValue() + (Float)this.range.getValue()))) {
                                    continue label150;
                                 }
                              } while(this.antiFriendPop.getValue() != AutoGondal.AntiFriendPop.ALL && this.antiFriendPop.getValue() != AutoGondal.AntiFriendPop.PLACE || player == null || !(player.func_174818_b(pos) <= MathUtil.square((double)((Float)this.range.getValue() + (Float)this.placeRange.getValue()))) || !esohack.friendManager.isFriend(player) || !((double)DamageUtil.calculateDamage((BlockPos)pos, player) > (double)EntityUtil.getHealth(player) + 0.5D));

                              maxDamage = maxDamage;
                              currentTarget = currentTarget;
                              currentPos = currentPos;
                              maxSelfDamage = maxSelfDamage;
                              continue label179;
                           } while((Boolean)this.antiNaked.getValue() && DamageUtil.isNaked(player));

                           float playerDamage = DamageUtil.calculateDamage((BlockPos)pos, player);
                           if ((Boolean)this.doublePopOnDamage.getValue() && this.isDoublePoppable(player, playerDamage) && (currentPos == null || player.func_174818_b(pos) < player.func_174818_b(currentPos))) {
                              currentTarget = player;
                              maxDamage = playerDamage;
                              currentPos = pos;
                              maxSelfDamage = selfDamage;
                              this.foundDoublePop = true;
                              if (this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.BREAK || this.antiFriendPop.getValue() == AutoGondal.AntiFriendPop.PLACE) {
                                 continue label179;
                              }
                           } else if (!this.foundDoublePop && (playerDamage > maxDamage || (Boolean)this.extraSelfCalc.getValue() && playerDamage >= maxDamage && selfDamage < maxSelfDamage) && (playerDamage > selfDamage || playerDamage > (Float)this.minDamage.getValue() && !DamageUtil.canTakeDamage((Boolean)this.suicide.getValue()) || playerDamage > EntityUtil.getHealth(player))) {
                              maxDamage = playerDamage;
                              currentTarget = player;
                              currentPos = pos;
                              maxSelfDamage = selfDamage;
                           }
                        }
                     }
                  }
               } while(friendPop);

               if (this.isDoublePoppable(targettedPlayer, playerDamage) && (currentPos == null || targettedPlayer.func_174818_b(pos) < targettedPlayer.func_174818_b(currentPos))) {
                  currentTarget = targettedPlayer;
                  maxDamage = playerDamage;
                  currentPos = pos;
                  this.foundDoublePop = true;
               } else if (!this.foundDoublePop && (playerDamage > maxDamage || (Boolean)this.extraSelfCalc.getValue() && playerDamage >= maxDamage && selfDamage < maxSelfDamage) && (playerDamage > selfDamage || playerDamage > (Float)this.minDamage.getValue() && !DamageUtil.canTakeDamage((Boolean)this.suicide.getValue()) || playerDamage > EntityUtil.getHealth(targettedPlayer))) {
                  maxDamage = playerDamage;
                  currentTarget = targettedPlayer;
                  currentPos = pos;
                  maxSelfDamage = selfDamage;
               }
            }
         }
      }
   }

   private EntityPlayer getTarget(boolean unsafe) {
      if (this.targetMode.getValue() == AutoGondal.Target.DAMAGE) {
         return null;
      } else {
         EntityPlayer currentTarget = null;
         Iterator var3 = mc.field_71441_e.field_73010_i.iterator();

         while(var3.hasNext()) {
            EntityPlayer player = (EntityPlayer)var3.next();
            if (!EntityUtil.isntValid(player, (double)((Float)this.placeRange.getValue() + (Float)this.range.getValue())) && (!(Boolean)this.antiNaked.getValue() || !DamageUtil.isNaked(player)) && (!unsafe || !EntityUtil.isSafe(player))) {
               if ((Integer)this.minArmor.getValue() > 0 && DamageUtil.isArmorLow(player, (Integer)this.minArmor.getValue())) {
                  currentTarget = player;
                  break;
               }

               if (currentTarget == null) {
                  currentTarget = player;
               } else if (mc.field_71439_g.func_70068_e(player) < mc.field_71439_g.func_70068_e((Entity)currentTarget)) {
                  currentTarget = player;
               }
            }
         }

         if (unsafe && currentTarget == null) {
            return this.getTarget(false);
         } else {
            if ((Boolean)this.predictPos.getValue() && currentTarget != null) {
               GameProfile profile = new GameProfile(((EntityPlayer)currentTarget).func_110124_au() == null ? UUID.fromString("8af022c8-b926-41a0-8b79-2b544ff00fcf") : ((EntityPlayer)currentTarget).func_110124_au(), ((EntityPlayer)currentTarget).func_70005_c_());
               EntityOtherPlayerMP newTarget = new EntityOtherPlayerMP(mc.field_71441_e, profile);
               Vec3d extrapolatePosition = MathUtil.extrapolatePlayerPosition((EntityPlayer)currentTarget, (Integer)this.predictTicks.getValue());
               newTarget.func_82149_j((Entity)currentTarget);
               newTarget.field_70165_t = extrapolatePosition.field_72450_a;
               newTarget.field_70163_u = extrapolatePosition.field_72448_b;
               newTarget.field_70161_v = extrapolatePosition.field_72449_c;
               newTarget.func_70606_j(EntityUtil.getHealth((Entity)currentTarget));
               newTarget.field_71071_by.func_70455_b(((EntityPlayer)currentTarget).field_71071_by);
               currentTarget = newTarget;
            }

            return (EntityPlayer)currentTarget;
         }
      }
   }

   private void breakCrystal() {
      if ((Boolean)this.explode.getValue() && this.breakTimer.passedMs((long)(Integer)this.breakDelay.getValue()) && (this.switchMode.getValue() == AutoGondal.Switch.ALWAYS || this.mainHand || this.offHand)) {
         if ((Integer)this.packets.getValue() == 1 && this.efficientTarget != null) {
            if ((Boolean)this.justRender.getValue()) {
               this.doFakeSwing();
               return;
            }

            if ((Boolean)this.syncedFeetPlace.getValue() && (Boolean)this.gigaSync.getValue() && this.syncedCrystalPos != null && this.damageSync.getValue() != AutoGondal.DamageSync.NONE) {
               return;
            }

            this.rotateTo(this.efficientTarget);
            this.attackEntity(this.efficientTarget);
            this.breakTimer.reset();
         } else if (!this.attackList.isEmpty()) {
            if ((Boolean)this.justRender.getValue()) {
               this.doFakeSwing();
               return;
            }

            if ((Boolean)this.syncedFeetPlace.getValue() && (Boolean)this.gigaSync.getValue() && this.syncedCrystalPos != null && this.damageSync.getValue() != AutoGondal.DamageSync.NONE) {
               return;
            }

            for(int i = 0; i < (Integer)this.packets.getValue(); ++i) {
               Entity entity = (Entity)this.attackList.poll();
               if (entity != null) {
                  this.rotateTo(entity);
                  this.attackEntity(entity);
               }
            }

            this.breakTimer.reset();
         }
      }

   }

   private void attackEntity(Entity entity) {
      if (entity != null) {
         if ((Integer)this.eventMode.getValue() == 2 && this.threadMode.getValue() == AutoGondal.ThreadMode.NONE && (Boolean)this.rotateFirst.getValue() && this.rotate.getValue() != AutoGondal.Rotate.OFF) {
            this.packetUseEntities.add(new CPacketUseEntity(entity));
         } else {
            EntityUtil.attackEntity(entity, (Boolean)this.sync.getValue(), (Boolean)this.breakSwing.getValue());
            brokenPos.add((new BlockPos(entity.func_174791_d())).func_177977_b());
         }
      }

   }

   private void doFakeSwing() {
      if ((Boolean)this.fakeSwing.getValue()) {
         EntityUtil.swingArmNoPacket(EnumHand.MAIN_HAND, mc.field_71439_g);
      }

   }

   private void manualBreaker() {
      if (this.rotate.getValue() != AutoGondal.Rotate.OFF && (Integer)this.eventMode.getValue() != 2 && this.rotating) {
         if (this.didRotation) {
            mc.field_71439_g.field_70125_A = (float)((double)mc.field_71439_g.field_70125_A + 4.0E-4D);
            this.didRotation = false;
         } else {
            mc.field_71439_g.field_70125_A = (float)((double)mc.field_71439_g.field_70125_A - 4.0E-4D);
            this.didRotation = true;
         }
      }

      RayTraceResult result;
      if ((this.offHand || this.mainHand) && (Boolean)this.manual.getValue() && this.manualTimer.passedMs((long)(Integer)this.manualBreak.getValue()) && Mouse.isButtonDown(1) && mc.field_71439_g.func_184592_cb().func_77973_b() != Items.field_151153_ao && mc.field_71439_g.field_71071_by.func_70448_g().func_77973_b() != Items.field_151153_ao && mc.field_71439_g.field_71071_by.func_70448_g().func_77973_b() != Items.field_151031_f && mc.field_71439_g.field_71071_by.func_70448_g().func_77973_b() != Items.field_151062_by && (result = mc.field_71476_x) != null) {
         switch(result.field_72313_a) {
         case ENTITY:
            Entity entity = result.field_72308_g;
            if (entity instanceof EntityEnderCrystal) {
               EntityUtil.attackEntity(entity, (Boolean)this.sync.getValue(), (Boolean)this.breakSwing.getValue());
               this.manualTimer.reset();
            }
            break;
         case BLOCK:
            BlockPos mousePos = mc.field_71476_x.func_178782_a().func_177984_a();
            Iterator var3 = mc.field_71441_e.func_72839_b((Entity)null, new AxisAlignedBB(mousePos)).iterator();

            while(var3.hasNext()) {
               Entity target = (Entity)var3.next();
               if (target instanceof EntityEnderCrystal) {
                  EntityUtil.attackEntity(target, (Boolean)this.sync.getValue(), (Boolean)this.breakSwing.getValue());
                  this.manualTimer.reset();
               }
            }
         }
      }

   }

   private void rotateTo(Entity entity) {
      switch((AutoGondal.Rotate)this.rotate.getValue()) {
      case OFF:
         this.rotating = false;
      case PLACE:
      default:
         break;
      case BREAK:
      case ALL:
         float[] angle = MathUtil.calcAngle(mc.field_71439_g.func_174824_e(mc.func_184121_ak()), entity.func_174791_d());
         if ((Integer)this.eventMode.getValue() == 2 && this.threadMode.getValue() == AutoGondal.ThreadMode.NONE) {
            esohack.rotationManager.setPlayerRotations(angle[0], angle[1]);
         } else {
            this.yaw = angle[0];
            this.pitch = angle[1];
            this.rotating = true;
         }
      }

   }

   private void rotateToPos(BlockPos pos) {
      switch((AutoGondal.Rotate)this.rotate.getValue()) {
      case OFF:
         this.rotating = false;
         break;
      case PLACE:
      case ALL:
         float[] angle = MathUtil.calcAngle(mc.field_71439_g.func_174824_e(mc.func_184121_ak()), new Vec3d((double)((float)pos.func_177958_n() + 0.5F), (double)((float)pos.func_177956_o() - 0.5F), (double)((float)pos.func_177952_p() + 0.5F)));
         if ((Integer)this.eventMode.getValue() == 2 && this.threadMode.getValue() == AutoGondal.ThreadMode.NONE) {
            esohack.rotationManager.setPlayerRotations(angle[0], angle[1]);
         } else {
            this.yaw = angle[0];
            this.pitch = angle[1];
            this.rotating = true;
         }
      case BREAK:
      }

   }

   private boolean isDoublePoppable(EntityPlayer player, float damage) {
      float health;
      if ((Boolean)this.doublePop.getValue() && (double)(health = EntityUtil.getHealth(player)) <= (Double)this.popHealth.getValue() && (double)damage > (double)health + 0.5D && damage <= (Float)this.popDamage.getValue()) {
         Timer timer = (Timer)this.totemPops.get(player);
         return timer == null || timer.passedMs((long)(Integer)this.popTime.getValue());
      } else {
         return false;
      }
   }

   private boolean isValid(Entity entity) {
      return entity != null && mc.field_71439_g.func_70068_e(entity) <= MathUtil.square((double)(Float)this.breakRange.getValue()) && (this.raytrace.getValue() == AutoGondal.Raytrace.NONE || this.raytrace.getValue() == AutoGondal.Raytrace.PLACE || mc.field_71439_g.func_70685_l(entity) || !mc.field_71439_g.func_70685_l(entity) && mc.field_71439_g.func_70068_e(entity) <= MathUtil.square((double)(Float)this.breaktrace.getValue()));
   }

   private boolean isEligableForFeetSync(EntityPlayer player, BlockPos pos) {
      if ((Boolean)this.holySync.getValue()) {
         BlockPos playerPos = new BlockPos(player.func_174791_d());
         EnumFacing[] var4 = EnumFacing.values();
         int var5 = var4.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            EnumFacing facing = var4[var6];
            if (facing != EnumFacing.DOWN && facing != EnumFacing.UP && pos.equals(playerPos.func_177977_b().func_177972_a(facing))) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static class RAutoGondal implements Runnable {
      private static AutoGondal.RAutoGondal instance;
      private AutoGondal AutoGondal;

      public static AutoGondal.RAutoGondal getInstance(AutoGondal AutoGondal) {
         if (instance == null) {
            instance = new AutoGondal.RAutoGondal();
            instance.AutoGondal = AutoGondal;
         }

         return instance;
      }

      public void run() {
         if (this.AutoGondal.threadMode.getValue() != AutoGondal.ThreadMode.WHILE) {
            if (this.AutoGondal.threadMode.getValue() != AutoGondal.ThreadMode.NONE && this.AutoGondal.isOn()) {
               while(true) {
                  if (!esohack.eventManager.ticksOngoing()) {
                     this.AutoGondal.threadOngoing.set(true);
                     esohack.safetyManager.doSafetyCheck();
                     this.AutoGondal.doAutoGondal();
                     this.AutoGondal.threadOngoing.set(false);
                     break;
                  }
               }
            }
         } else {
            while(this.AutoGondal.isOn() && this.AutoGondal.threadMode.getValue() == AutoGondal.ThreadMode.WHILE) {
               while(esohack.eventManager.ticksOngoing()) {
               }

               if (this.AutoGondal.shouldInterrupt.get()) {
                  this.AutoGondal.shouldInterrupt.set(false);
                  this.AutoGondal.syncroTimer.reset();
                  this.AutoGondal.thread.interrupt();
                  break;
               }

               this.AutoGondal.threadOngoing.set(true);
               esohack.safetyManager.doSafetyCheck();
               this.AutoGondal.doAutoGondal();
               this.AutoGondal.threadOngoing.set(false);

               try {
                  Thread.sleep((long)(Integer)this.AutoGondal.threadDelay.getValue());
               } catch (InterruptedException var2) {
                  this.AutoGondal.thread.interrupt();
                  var2.printStackTrace();
               }
            }
         }

      }
   }

   public static class PlaceInfo {
      private final BlockPos pos;
      private final boolean offhand;
      private final boolean placeSwing;
      private final boolean exactHand;

      public PlaceInfo(BlockPos pos, boolean offhand, boolean placeSwing, boolean exactHand) {
         this.pos = pos;
         this.offhand = offhand;
         this.placeSwing = placeSwing;
         this.exactHand = exactHand;
      }

      public void runPlace() {
         BlockUtil.placeCrystalOnBlock(this.pos, this.offhand ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND, this.placeSwing, this.exactHand);
      }
   }

   public static enum Settings {
      PLACE,
      BREAK,
      RENDER,
      MISC,
      DEV;
   }

   public static enum DamageSync {
      NONE,
      PLACE,
      BREAK;
   }

   public static enum Rotate {
      OFF,
      PLACE,
      BREAK,
      ALL;
   }

   public static enum Target {
      CLOSEST,
      UNSAFE,
      DAMAGE;
   }

   public static enum Logic {
      BREAKPLACE,
      PLACEBREAK;
   }

   public static enum Switch {
      ALWAYS,
      BREAKSLOT,
      CALC;
   }

   public static enum Raytrace {
      NONE,
      PLACE,
      BREAK,
      FULL;
   }

   public static enum AutoSwitch {
      NONE,
      TOGGLE,
      ALWAYS;
   }

   public static enum ThreadMode {
      NONE,
      POOL,
      SOUND,
      WHILE;
   }

   public static enum AntiFriendPop {
      NONE,
      PLACE,
      BREAK,
      ALL;
   }

   public static enum PredictTimer {
      NONE,
      BREAK,
      PREDICT;
   }
}
