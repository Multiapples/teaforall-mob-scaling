package io.github.multiapples;

import com.google.gson.*;


import java.io.*;

public class Config {

    public final boolean netherDOT = true;
    public final float netherDOTYlevel = 123.5f;
    public final JsonObject mobScaling = defaultMobScalingJson();
    private static JsonObject defaultMobScalingJson = null;
    private static final String defaultMobScalingJsonStr = "{\"eligibleMobs\":[\"zombie\",\"creeper\",\"skeleton\",\"spider\",\"zombie_villager\",\"husk\",\"drowned\",\"stray\",\"cave_spider\",\"phantom\",\"slime\",\"warden\",\"witch\",\"pillager\",\"vindicator\",\"ravager\",\"evoker\",\"vex\",\"guardian\",\"elder_guardian\",\"bogged\",\"breeze\",\"hoglin\",\"piglin\",\"zoglin\",\"zombified_piglin\",\"piglin_brute\",\"ghast\",\"magma_cube\",\"blaze\",\"wither_skeleton\",\"silverfish\",\"enderman\",\"shulker\",\"endermite\"],\"rampings\":{\"overworld\":[{\"startDist\":0,\"endDist\":500,\"startMinPoints\":-50,\"startMaxPoints\":-20,\"endMinPoints\":40,\"endMaxPoints\":70},{\"startDist\":500,\"endDist\":900,\"startMinPoints\":70,\"startMaxPoints\":110,\"endMinPoints\":220,\"endMaxPoints\":330},{\"startDist\":900,\"endDist\":1500,\"startMinPoints\":240,\"startMaxPoints\":560,\"endMinPoints\":340,\"endMaxPoints\":660},{\"startDist\":1500,\"endDist\":30000,\"startMinPoints\":400,\"startMaxPoints\":700,\"endMinPoints\":600,\"endMaxPoints\":1600},{\"startDist\":30000,\"endDist\":30000000,\"startMinPoints\":600,\"startMaxPoints\":1600,\"endMinPoints\":100000,\"endMaxPoints\":200000}],\"nether\":[{\"startDist\":0,\"endDist\":500,\"startMinPoints\":-100,\"startMaxPoints\":-50,\"endMinPoints\":-50,\"endMaxPoints\":300},{\"startDist\":500,\"endDist\":30000000,\"startMinPoints\":-50,\"startMaxPoints\":300,\"endMinPoints\":1000000,\"endMaxPoints\":1200000}],\"end\":[{\"startDist\":0,\"endDist\":1000,\"startMinPoints\":-100,\"startMaxPoints\":300,\"endMinPoints\":-300,\"endMaxPoints\":300},{\"startDist\":1000,\"endDist\":30000000,\"startMinPoints\":-300,\"startMaxPoints\":300,\"endMinPoints\":1000000,\"endMaxPoints\":7000000}]},\"defaultScaling\":{\"healthCost\":10,\"healthRealloc\":0.25,\"modifiers\":{\"speed-1\":{\"cost\":20,\"failureChance\":0},\"speed-2\":{\"cost\":50,\"failureChance\":0},\"speed-3\":{\"cost\":150,\"failureChance\":0},\"speed-4\":{\"cost\":300,\"failureChance\":0},\"speed-5\":{\"cost\":500,\"failureChance\":0},\"strength-1\":{\"cost\":50,\"failureChance\":0},\"strength-2\":{\"cost\":150,\"failureChance\":0},\"strength-3\":{\"cost\":200,\"failureChance\":0},\"strength-4\":{\"cost\":300,\"failureChance\":0},\"invisibility\":{\"cost\":200,\"failureChance\":0.99}}},\"mobScalingOverrides\":{\"zombie\":{\"healthCost\":6},\"creeper\":{\"healthRealloc\":0.5},\"skeleton\":{\"healthCost\":6},\"spider\":{\"healthCost\":6},\"zombie_villager\":{\"healthCost\":6},\"husk\":{\"healthCost\":6,\"modifiers\":{\"fire-resistance\":{\"cost\":150,\"failureChance\":0.5},\"resistance-1\":{\"cost\":150,\"failureChance\":0.5}}},\"drowned\":{\"healthCost\":6},\"stray\":{\"healthCost\":6,\"healthRealloc\":0.5},\"cave_spider\":{\"healthCost\":6},\"phantom\":{\"healthCost\":6},\"slime\":{},\"warden\":{},\"witch\":{},\"pillager\":{},\"vindicator\":{},\"ravager\":{},\"evoker\":{},\"vex\":{},\"guardian\":{},\"elder_guardian\":{},\"bogged\":{},\"breeze\":{},\"hoglin\":{},\"piglin\":{},\"zoglin\":{\"healthCost\":6},\"zombified_piglin\":{\"healthCost\":6},\"piglin_brute\":{},\"ghast\":{},\"magma_cube\":{},\"blaze\":{},\"wither_skeleton\":{\"healthCost\":6},\"silverfish\":{\"healthCost\":4},\"enderman\":{\"healthCost\":25},\"shulker\":{\"healthCost\":50},\"endermite\":{\"healthCost\":6,\"healthRealloc\":1}}}";

    public static JsonObject defaultMobScalingJson() {
        if (defaultMobScalingJson == null) {
            defaultMobScalingJson = JsonParser.parseString(defaultMobScalingJsonStr).getAsJsonObject();
        }
        return defaultMobScalingJson;
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
