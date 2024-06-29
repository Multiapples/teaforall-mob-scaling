package io.github.multiapples;

import com.google.gson.*;


import java.io.*;

public class Config {

    public final boolean netherDOT = true;
    public final float netherDOTYlevel = 123.5f;
    public final double strongholdDeadZone = 6000.0;
    public final JsonObject mobScaling = defaultMobScalingJson();
    private static JsonObject defaultMobScalingJson = null;
    private static final String defaultMobScalingJsonStr = "{\"eligibleMobs\":[\"zombie\",\"creeper\",\"skeleton\",\"spider\",\"zombie_villager\",\"husk\",\"drowned\",\"stray\",\"cave_spider\",\"phantom\",\"slime\",\"warden\",\"witch\",\"pillager\",\"vindicator\",\"ravager\",\"evoker\",\"vex\",\"guardian\",\"elder_guardian\",\"bogged\",\"breeze\",\"hoglin\",\"piglin\",\"zoglin\",\"zombified_piglin\",\"piglin_brute\",\"ghast\",\"magma_cube\",\"blaze\",\"wither_skeleton\",\"silverfish\",\"enderman\",\"shulker\",\"endermite\"],\"rampings\":{\"overworld\":[{\"startDist\":0,\"endDist\":3500,\"startMinPoints\":-50,\"startMaxPoints\":-20,\"endMinPoints\":40,\"endMaxPoints\":70},{\"startDist\":3500,\"endDist\":6500,\"startMinPoints\":70,\"startMaxPoints\":110,\"endMinPoints\":220,\"endMaxPoints\":330},{\"startDist\":6500,\"endDist\":10000,\"startMinPoints\":240,\"startMaxPoints\":560,\"endMinPoints\":340,\"endMaxPoints\":660},{\"startDist\":10000,\"endDist\":30000,\"startMinPoints\":400,\"startMaxPoints\":700,\"endMinPoints\":600,\"endMaxPoints\":1600},{\"startDist\":30000,\"endDist\":30000000,\"startMinPoints\":600,\"startMaxPoints\":1600,\"endMinPoints\":100000,\"endMaxPoints\":200000}],\"nether\":[{\"startDist\":0,\"endDist\":500,\"startMinPoints\":-100,\"startMaxPoints\":-50,\"endMinPoints\":-100,\"endMaxPoints\":100},{\"startDist\":500,\"endDist\":30000000,\"startMinPoints\":-100,\"startMaxPoints\":100,\"endMinPoints\":300000,\"endMaxPoints\":1200000}],\"end\":[{\"startDist\":0,\"endDist\":1000,\"startMinPoints\":-100,\"startMaxPoints\":300,\"endMinPoints\":-300,\"endMaxPoints\":300},{\"startDist\":1000,\"endDist\":30000000,\"startMinPoints\":-300,\"startMaxPoints\":300,\"endMinPoints\":200000,\"endMaxPoints\":2000000}]},\"defaultScaling\":{\"healthCost\":10,\"healthRealloc\":0.25,\"modifiers\":{\"speed-1\":{\"cost\":20,\"failureChance\":0},\"speed-2\":{\"cost\":50,\"failureChance\":0},\"speed-3\":{\"cost\":150,\"failureChance\":0},\"speed-4\":{\"cost\":300,\"failureChance\":0.05},\"speed-5\":{\"cost\":500,\"failureChance\":0.1},\"strength-1\":{\"cost\":50,\"failureChance\":0},\"strength-2\":{\"cost\":150,\"failureChance\":0},\"strength-3\":{\"cost\":200,\"failureChance\":0.05},\"strength-4\":{\"cost\":300,\"failureChance\":0.1},\"invisibility\":{\"cost\":200,\"failureChance\":0.95}}},\"mobScalingOverrides\":{\"zombie\":{\"healthCost\":6,\"modifiers\":{\"fire-resistance\":{\"cost\":50,\"failureChance\":0.5}}},\"creeper\":{\"healthRealloc\":0.5},\"skeleton\":{\"healthCost\":6,\"modifiers\":{\"fire-resistance\":{\"cost\":50,\"failureChance\":0.5}}},\"spider\":{\"healthCost\":6},\"zombie_villager\":{\"healthCost\":6,\"modifiers\":{\"fire-resistance\":{\"cost\":20,\"failureChance\":0}}},\"husk\":{\"healthCost\":6,\"modifiers\":{\"fire-resistance\":{\"cost\":20,\"failureChance\":0.25},\"resistance-1\":{\"cost\":150,\"failureChance\":0.5}}},\"drowned\":{\"healthCost\":6},\"stray\":{\"healthCost\":6,\"healthRealloc\":0.5},\"cave_spider\":{\"healthCost\":6},\"phantom\":{\"healthCost\":6,\"modifiers\":{\"fire-resistance\":{\"cost\":200,\"failureChance\":0.75}}},\"slime\":{},\"warden\":{},\"witch\":{},\"pillager\":{},\"vindicator\":{},\"ravager\":{},\"evoker\":{},\"vex\":{\"healthCost\":50},\"guardian\":{},\"elder_guardian\":{},\"bogged\":{},\"breeze\":{},\"hoglin\":{},\"piglin\":{},\"zoglin\":{\"healthCost\":6},\"zombified_piglin\":{\"healthCost\":6},\"piglin_brute\":{},\"ghast\":{},\"magma_cube\":{},\"blaze\":{},\"wither_skeleton\":{\"healthCost\":6},\"silverfish\":{\"healthCost\":4},\"enderman\":{\"healthCost\":25,\"modifiers\":{\"invisibility\":{\"cost\":10,\"failureChance\":0.8},\"fire-resistance\":{\"cost\":50,\"failureChance\":0.2}}},\"shulker\":{\"healthCost\":50},\"endermite\":{\"healthCost\":6,\"healthRealloc\":1}}}";

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
