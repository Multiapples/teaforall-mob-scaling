package io.github.multiapples;

import com.google.gson.*;


import java.io.*;

public class Config {

    public final boolean netherDOT = true;
    public final boolean dragonBuffsMobs = true;
    public final int dragonBuffsMobsPoints = 3000;
    public final boolean fartherStrongholdGen = true;
    public final int fartherStrongholdGenOffset = 5000;
    public final JsonObject mobScaling = defaultMobScalingJson();

    public static JsonObject defaultMobScalingJson() {
        return JsonParser.parseString("{\"hey\": 727}").getAsJsonObject(); // TODO: fill in default json
    }

    public static Config load(File file) throws FileNotFoundException {
        Gson gson = new Gson();
        FileReader reader = new FileReader(file);
        return gson.fromJson(reader, Config.class);
    }

    public static void save(File file, Config config) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter writer = new FileWriter(file);
        gson.toJson(config, writer);
        writer.close();
    }
}
