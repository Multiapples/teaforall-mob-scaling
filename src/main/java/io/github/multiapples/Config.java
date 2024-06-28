package io.github.multiapples;

import com.google.gson.*;


import java.io.*;

public class Config {

    public boolean netherDOT = true;
    public boolean dragonBuffsMobs = true;
    public int dragonBuffsMobsPoints = 3000;
    public boolean fartherStrongholdGen = true;
    public int fartherStrongholdGenOffset = 5000;
    public JsonObject mobScaling = null;

    public Config() {
        // TODO: use logger
        System.out.println("Warning, missing or invalid config found, default config settings will be used.");
        mobScaling = defaultMobScalingJson();
    }
    public Config(JsonObject json) {
        try { netherDOT = json.get("netherDOT").getAsBoolean(); }
        catch (Exception e) { System.out.println("Warning, config.netherDOT is invalid, using default setting"); }

        try { dragonBuffsMobs = json.get("dragonBuffsMobs").getAsBoolean(); }
        catch (Exception e) { System.out.println("Warning, config.dragonBuffsMobs is invalid, using default setting"); }

        try { dragonBuffsMobsPoints = json.get("dragonBuffsMobsPoints").getAsInt(); }
        catch (Exception e) { System.out.println("Warning, config.dragonBuffsMobsPoints is invalid, using default setting"); }

        try { fartherStrongholdGen = json.get("fartherStrongholdGen").getAsBoolean(); }
        catch (Exception e) { System.out.println("Warning, config.fartherStrongholdGen is invalid, using default setting"); }

        try { fartherStrongholdGenOffset = json.get("fartherStrongholdGenOffset").getAsInt(); }
        catch (Exception e) { System.out.println("Warning, config.fartherStrongholdGenOffset is invalid, using default setting"); }

        try { mobScaling = json.get("mobScaling").getAsJsonObject(); }
        catch (Exception e) { System.out.println("Warning, config.mobScaling is invalid, using default setting"); }
    }

    public static JsonObject defaultMobScalingJson() {
        return JsonParser.parseString("{\"hey\": 727}").getAsJsonObject(); // TODO: fill in default json
    }

    public static Config load(File file) throws FileNotFoundException {
        FileReader reader = new FileReader(file);
        JsonElement json = JsonParser.parseReader(reader);
        if (json == null || json.isJsonNull() || !json.isJsonObject()) {
            return new Config();
        }
        return new Config(json.getAsJsonObject());
    }

    public static void save(File file, Config config) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = new FileWriter(file);
        gson.toJson(config, writer);
        writer.close();
    }
}
