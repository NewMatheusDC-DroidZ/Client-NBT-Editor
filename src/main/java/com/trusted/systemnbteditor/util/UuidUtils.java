package com.trusted.systemnbteditor.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Uuids;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UuidUtils {

    public static CompletableFuture<UUID> fetchUuidAsync(String name) {
        return CompletableFuture.supplyAsync(() -> fetchUuid(name));
    }

    public static UUID fetchUuid(String name) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                String json = response.toString();
                Matcher m = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                if (m.find()) {
                    String rawId = m.group(1);
                    String uuidStr = rawId.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                    return UUID.fromString(uuidStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static CompletableFuture<GameProfile> fetchProfileWithTexturesAsync(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> fetchProfileWithTextures(uuid, name));
    }

    public static GameProfile fetchProfileWithTextures(UUID uuid, String name) {
        GameProfile profile = new GameProfile(uuid, name);
        try {
            String urlStr = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "") + "?unsigned=false";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                String json = response.toString();

                Matcher valMatcher = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                Matcher sigMatcher = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"").matcher(json);

                if (valMatcher.find()) {
                    String value = valMatcher.group(1);
                    String signature = sigMatcher.find() ? sigMatcher.group(1) : null;

                    try {
                        Method propertiesMethod;
                        try {
                            propertiesMethod = profile.getClass().getMethod("getProperties");
                        } catch (Exception e) {
                            propertiesMethod = profile.getClass().getMethod("properties");
                        }

                        Object propertyMap = propertiesMethod.invoke(profile);
                        Method putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);

                        Property property;
                        try {
                            property = new Property("textures", value, signature);
                        } catch (Exception e) {
                            property = new Property("textures", value);
                        }
                        putMethod.invoke(propertyMap, "textures", property);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return profile;
    }

    public static String formatIntArrayUuid(UUID uuid) {
        int[] ints = Uuids.toIntArray(uuid);
        return String.format("I;%d,%d,%d,%d", ints[0], ints[1], ints[2], ints[3]);
    }
}
