package io.github.multiapples;

import com.google.gson.*;


import java.io.*;

public class Config {

    public final boolean netherDOT = true;
    public final float netherDOTYlevel = 123.5f;
    public final int eyeOfEnderDeadZone = 11000;
    public final JsonObject mobScaling = defaultMobScalingJson();
    private final JsonObject defaultMobScalingJson = null;

    public static JsonObject defaultMobScalingJson() {
        return JsonParser.parseString("{\"hey\": 727}").getAsJsonObject(); // TODO: fill in default json
        // TODO: save this instead of recalculating it every time
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
