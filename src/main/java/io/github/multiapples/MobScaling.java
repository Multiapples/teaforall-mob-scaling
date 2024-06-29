package io.github.multiapples;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.*;
import net.minecraft.scoreboard.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class MobScaling {
    private enum DIMENSION { OVERWORLD, NETHER, END }
    private enum MODIFIERS {
        SPEED_2("speed-2"),
        STRENGTH_2("strength-2");
        public static final Map<String, MODIFIERS> BY_IDENTIFIER = Arrays.stream(MODIFIERS.values())
                .collect(Collectors.toMap(MODIFIERS::getValue, e -> e));
        private final String value;
        private MODIFIERS(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }
    private enum MODIFIER_CATEGORIES {
        HEALTH(0),
        DAMAGE(1),
        TECH(2);
        private final int value;
        private MODIFIER_CATEGORIES(int value) {
            this.value = value;
        }
        public int getValue() {
            return value;
        }
    }
    private static final String OBJECTIVE_SCALING_POINTS = "teaforallscalingpoints";
    private static final String OBJECTIVE_DOWNED_ENDER_DRAGON = "teaforalldownededragon";
    private static final String PERSISTENT_MODIFIER_HEALTH_SCALING = "teaforallmobscaling";
    private static final int NUMBER_OF_POINT_CATEGORIES = 3;
    private static final Collection<EntityType<?>> scalingEligible = new HashSet<>();
    private static final Map<DIMENSION, List<Ramping>> rampingsByDimension = new HashMap<>();
    private static final ScalingParameters defaultScalingParameters = new ScalingParameters();
    private static final Map<EntityType<?>, ScalingParameters> scalingOverridesByMob = new HashMap<>();
    private static final Map<String, MODIFIER_CATEGORIES> categoriesByModifier = new HashMap<>();

    private static class Ramping {
        public float startDist, endDist;
        public int startMinPoints, startMaxPoints, endMinPoints, endMaxPoints;

        public Ramping(final float startDist, final float endDist,
                       final int startMinPoints, final int startMaxPoints,
                       final int endMinPoints, final int endMaxPoints) {
            if (!(startDist >= 0f)) throw new AssertionError();
            if (!(endDist > startDist)) throw new AssertionError();
            if (!(startMinPoints <= startMaxPoints)) throw new AssertionError();
            if (!(endMinPoints <= endMaxPoints)) throw new AssertionError();
            this.startDist = startDist;
            this.endDist = endDist;
            this.startMinPoints = startMinPoints;
            this.startMaxPoints = startMaxPoints;
            this.endMinPoints = endMinPoints;
            this.endMaxPoints = endMaxPoints;
        }

        public boolean inRange(float dist) {
            return startDist <= dist && dist <= endDist;
        }

    }

    private static class ScalingParameters {
        public float healthRealloc; // The fraction of health that gets split among damage and tech points
                                    // before applying modifiers.
        public int healthCost; // Each health added costs this many points.
        public Map<MODIFIERS, ScalingModifier> modifiersByIdentifier;

        public ScalingParameters() {
            healthRealloc = 0f;
            healthCost = 1;
            modifiersByIdentifier = new HashMap<>();
        }

        public ScalingParameters(ScalingParameters src) {
            healthRealloc = src.healthRealloc;
            healthCost = src.healthCost;
            modifiersByIdentifier = src.modifiersByIdentifier.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new ScalingModifier(e.getValue())));
        }
    }

    private static class ScalingModifier {
        public MODIFIERS identifier;
        public int cost;
        public float failureChance;

        public ScalingModifier(final ScalingModifier src) {
            this(src.identifier, src.cost, src.failureChance);
        }
        public ScalingModifier(final MODIFIERS identifier, final int cost, final float failureChance) {
            if (!(identifier != null)) throw new AssertionError();
            if (!(cost >= 0)) throw new AssertionError();
            if (!(failureChance >= 0f)) throw new AssertionError();
            if (!(failureChance <= 1f)) throw new AssertionError();
            this.identifier = identifier;
            this.cost = cost;
            this.failureChance = failureChance;
        }
    }

    public static void initialize(Config config, Logger logger) throws IllegalStateException { //TODO: take a config parameter
        //Registries.ENTITY_TYPE.containsId(Identifier.of("ender_dragon"));
        //Registries.ENTITY_TYPE.get(Identifier.of("ender_dragon")); // returns EntityType<?>; //TODO: make this work

        JsonObject json = config.mobScaling;

        // Init modifier categories
        categoriesByModifier.put(MODIFIERS.SPEED_2.getValue(), MODIFIER_CATEGORIES.TECH);
        categoriesByModifier.put(MODIFIERS.STRENGTH_2.getValue(), MODIFIER_CATEGORIES.DAMAGE);
        if (!(categoriesByModifier.entrySet()
                .stream()
                .anyMatch(entry -> isInIntRange(entry.getValue().getValue(), 0, NUMBER_OF_POINT_CATEGORIES - 1))))
            throw new AssertionError();

        // Add eligible mobs for scaling.
        JsonArray jsonEligibleMobs = json.getAsJsonArray("eligibleMobs");
        for (JsonElement e : jsonEligibleMobs.asList()) {
            String mobId = new JsonOption<>(e).unwrapAsString();
            if (mobId == null || !Registries.ENTITY_TYPE.containsId(Identifier.of(mobId))) {
                logger.info("Invalid mob identifier \"" + mobId + "\" in config $mobScaling.eligibleMobs");
                throw new IllegalStateException();
            }
            scalingEligible.add(Registries.ENTITY_TYPE.get(Identifier.of(mobId)));
        }

        // Add rampings
        JsonObject jsonRampings = json.getAsJsonObject("rampings");
        Gson gson = new Gson();
        rampingsByDimension.put(DIMENSION.OVERWORLD, new ArrayList<>());
        JsonArray jsonOverworld = jsonRampings.getAsJsonArray("overworld");
        for (JsonElement ramp : jsonOverworld.asList()) {
            rampingsByDimension.get(DIMENSION.OVERWORLD).add(gson.fromJson(ramp, Ramping.class));
        }
        rampingsByDimension.put(DIMENSION.NETHER, new ArrayList<>());
        JsonArray jsonNether = jsonRampings.getAsJsonArray("nether");
        for (JsonElement ramp : jsonNether.asList()) {
            rampingsByDimension.get(DIMENSION.NETHER).add(gson.fromJson(ramp, Ramping.class));
        }
        rampingsByDimension.put(DIMENSION.END, new ArrayList<>());
        JsonArray jsonEnd = jsonRampings.getAsJsonArray("end");
        for (JsonElement ramp : jsonEnd.asList()) {
            rampingsByDimension.get(DIMENSION.END).add(gson.fromJson(ramp, Ramping.class));
        }

        // Add default scaling parameters
        JsonObject jsonDefaultScaling = json.getAsJsonObject("defaultScaling");
        defaultScalingParameters.healthRealloc = jsonDefaultScaling.getAsJsonPrimitive("healthRealloc").getAsFloat();
        defaultScalingParameters.healthCost = jsonDefaultScaling.getAsJsonPrimitive("healthCost").getAsInt();
        JsonObject jsonDefaultModifiers = jsonDefaultScaling.getAsJsonObject("modifiers");
        for (Map.Entry<String, JsonElement> entry : jsonDefaultModifiers.asMap().entrySet()) {
            String identifier = entry.getKey();
            JsonObject body = entry.getValue().getAsJsonObject();
            if (!MODIFIERS.BY_IDENTIFIER.containsKey(identifier)) {
                logger.info("Invalid modifier \"" + identifier + "\" in config $mobScaling.defaultScaling.modifiers");
                throw new IllegalStateException();
            }
            int cost = body.getAsJsonPrimitive("cost").getAsInt();
            float failureChance = body.getAsJsonPrimitive("failureChance").getAsFloat();
            defaultScalingParameters.modifiersByIdentifier.put(MODIFIERS.BY_IDENTIFIER.get(identifier),
                    new ScalingModifier(MODIFIERS.BY_IDENTIFIER.get(identifier), cost, failureChance));
        }

        // Add mob overrides
        JsonObject jsonOverrides = json.getAsJsonObject("mobScalingOverrides");
        for (Map.Entry<String, JsonElement> entry : jsonOverrides.asMap().entrySet()) {
            String identifier = entry.getKey();
            JsonObject body = entry.getValue().getAsJsonObject();
            if (!Registries.ENTITY_TYPE.containsId(Identifier.of(identifier))) {
                logger.info("Invalid entity \"" + identifier + "\" in config $mobScaling.mobScalingOverrides");
                throw new IllegalStateException();
            }
            JsonObject jsonOverrideModifiers = body.getAsJsonObject("modifiers");
            ScalingParameters override = new ScalingParameters(defaultScalingParameters);
            override.healthRealloc = new JsonOption<>(body).get("healthRealloc").unwrapAsNumber(defaultScalingParameters.healthRealloc).floatValue();
            override.healthCost = new JsonOption<>(body).get("healthCost").unwrapAsNumber(defaultScalingParameters.healthCost).intValue();
            for (Map.Entry<String, JsonElement> modEntry : jsonOverrideModifiers.asMap().entrySet()) {
                String modIdentifierStr = modEntry.getKey();
                JsonObject modBody = modEntry.getValue().getAsJsonObject();
                if (!MODIFIERS.BY_IDENTIFIER.containsKey(modIdentifierStr)) {
                    logger.info("Invalid modifier \"" + modIdentifierStr + "\" in config $mobScaling.mobScalingOverrides." + identifier + ".modifiers");
                    throw new IllegalStateException();
                }
                MODIFIERS modIdentifier = MODIFIERS.BY_IDENTIFIER.get(modIdentifierStr);
                JsonOption<JsonElement> cost = new JsonOption<>(modBody).get("cost");
                JsonOption<JsonElement> failureChance = new JsonOption<>(modBody).get("failureChance");
                if (override.modifiersByIdentifier.containsKey(modIdentifier)) {
                    if (cost.elementExists())
                        override.modifiersByIdentifier.get(modIdentifier).cost = cost.unwrapAsNumber().intValue();
                    if (failureChance.elementExists())
                        override.modifiersByIdentifier.get(modIdentifier).failureChance = failureChance.unwrapAsNumber().floatValue();
                } else {
                    override.modifiersByIdentifier.put(modIdentifier,
                            new ScalingModifier(modIdentifier, cost.unwrapAsNumber().intValue(),
                                    failureChance.unwrapAsNumber().floatValue()));
                }
            }
            scalingOverridesByMob.put(Registries.ENTITY_TYPE.get(Identifier.of(identifier)), override);
        }
    }

    public static void assignMobScaling(MobEntity mob) {
        if (!(mob != null)) throw new AssertionError();

        if (!mobEligibleForScaling(mob)) {
            return;
        }

        Vec3d pos = mob.getPos();
        RegistryKey<World> mobDimension = mob.getEntityWorld().getRegistryKey();
        DIMENSION dimension;
        if (mobDimension == World.OVERWORLD) {
            dimension = DIMENSION.OVERWORLD;
        } else if (mobDimension == World.END) {
            dimension = DIMENSION.END;
        } else {
            dimension = DIMENSION.NETHER; // Treat unknown dimensions as Nether by default.
        }

        // Find and use ramping
        Random random = mob.getRandom();
        float dist = (float)pos.multiply(1, 0, 1).length();
        int scalingPoints = 0;
        List<Ramping> rampings = rampingsByDimension.get(dimension);
        for (Ramping ramp : rampings) {
            if (!ramp.inRange(dist)) {
                continue;
            }
            int pointsMin = (int)MathHelper.map(dist, ramp.startDist, ramp.endDist, ramp.startMinPoints, ramp.endMinPoints);
            int pointsMax = (int)MathHelper.map(dist, ramp.startDist, ramp.endDist, ramp.startMaxPoints, ramp.endMaxPoints);
            scalingPoints = random.nextBetween(pointsMin, pointsMax);
            break;
        }
        setScalingPoints(mob, scalingPoints);
        scaleMobEntity(mob, scalingPoints);
    }

    public static ScalingParameters getMobScalingParameters(MobEntity mob) {
        if (!(mob != null)) throw new AssertionError();

        if (scalingOverridesByMob.containsKey(mob.getType())) {
            return scalingOverridesByMob.get(mob.getType());
        }
        return defaultScalingParameters;
    }

    public static void scaleMobEntity(MobEntity mob, int scalingPoints) {
        if (!(mob != null)) throw new AssertionError();
        if (scalingPoints <= 0) {
            return;
        }

        Random random = mob.getRandom();
        ScalingParameters scalingParameters = getMobScalingParameters(mob);
        List<ScalingModifier> modifiers = new ArrayList<>(scalingParameters.modifiersByIdentifier.values()); // Maybe not the most efficient thing

        int[] budgets = partitionInt(mob.getRandom(), scalingPoints, 3); // Point budgets for each modifier category.
        if (!(budgets.length == NUMBER_OF_POINT_CATEGORIES)) throw new AssertionError();

        // Apply health realloc.
        if (NUMBER_OF_POINT_CATEGORIES > 1) {
            int healthCategory = MODIFIER_CATEGORIES.HEALTH.getValue();
            int reallocDelta = (int)(scalingParameters.healthRealloc * budgets[healthCategory] / (float) (NUMBER_OF_POINT_CATEGORIES - 1));
            for (MODIFIER_CATEGORIES categoryEnum : MODIFIER_CATEGORIES.values()) {
                int category = categoryEnum.getValue();
                if (category == healthCategory) {
                    budgets[category] -= reallocDelta * (NUMBER_OF_POINT_CATEGORIES - 1);
                } else {
                    budgets[category] += reallocDelta;
                }
            }
        }

        System.out.println("HPP: " + budgets[0]); //TODO remove
        System.out.println("DMG: " + budgets[1]);
        System.out.println("TCP: " + budgets[2]);

        int[] order = shuffledRange(random, modifiers.size());
        for (int i : order) {
            ScalingModifier mod = modifiers.get(i);

            if (!categoriesByModifier.containsKey(mod.identifier.getValue())) {
                System.out.println("Warning, modifier lacks associated points category"); //TODO use logger
                continue;
            }
            int category = categoriesByModifier.get(mod.identifier.getValue()).getValue();

            if (mod.cost > budgets[category]) {
                continue;
            }
            if (random.nextFloat() % 1f < mod.failureChance) {
                continue;
            }

            // Apply the modifier
            System.out.println("Applying " + mod.identifier); // TODO: remove
            budgets[category] -= mod.cost;
            switch (mod.identifier) {
                case SPEED_2 ->
                        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, StatusEffectInstance.INFINITE, 1));
                case STRENGTH_2 ->
                        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, StatusEffectInstance.INFINITE, 1));
                default ->
                        System.out.println("uh oh, " + mod.identifier.getValue() + " was not a valid modifier!!!"); //TODO: use logger
            }
        }

        System.out.println("HPP': " + budgets[0]); //TODO remove
        System.out.println("DMG': " + budgets[1]);
        System.out.println("TCP': " + budgets[2]);

        // Dump remaining points into health.
        if (NUMBER_OF_POINT_CATEGORIES > 1) {
            int healthCategory = MODIFIER_CATEGORIES.HEALTH.getValue();
            for (MODIFIER_CATEGORIES categoryEnum : MODIFIER_CATEGORIES.values()) {
                int category = categoryEnum.getValue();
                if (category == healthCategory) {
                    continue;
                }
                budgets[healthCategory] += budgets[category];
                budgets[category] = 0;
            }
        }
        int healthPoints = budgets[MODIFIER_CATEGORIES.HEALTH.getValue()];
        EntityAttributeInstance attributeInstance = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attributeInstance == null) {
            System.out.println("Null EntityAttributeInstance"); // TODO: Use Logger
        } else {
            int addHealth = healthPoints / scalingParameters.healthCost;
            attributeInstance.addPersistentModifier(new EntityAttributeModifier(
                    Identifier.of(PERSISTENT_MODIFIER_HEALTH_SCALING),
                    addHealth, EntityAttributeModifier.Operation.ADD_VALUE));
            mob.heal(healthPoints);
        }

        // Glow for now, for testing: //TODO, remove
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, StatusEffectInstance.INFINITE, 0));
    }

    public static ScoreboardObjective getScalingObjective(Scoreboard scoreboard) {
        if (!(scoreboard != null)) throw new AssertionError();

        ScoreboardObjective scalingPointsObjective = scoreboard.getNullableObjective(OBJECTIVE_SCALING_POINTS);
        if (scalingPointsObjective == null) {
            scalingPointsObjective = scoreboard.addObjective(OBJECTIVE_SCALING_POINTS, ScoreboardCriterion.DUMMY, Text.of(OBJECTIVE_SCALING_POINTS),
                    ScoreboardCriterion.RenderType.INTEGER, true, null);
        }
        if (!(scalingPointsObjective != null)) throw new AssertionError(); // sanity check
        if (!(scalingPointsObjective.getScoreboard() == scoreboard)) throw new AssertionError(); // sanity check
        return scalingPointsObjective;
    }

    public static void setScalingPoints(MobEntity mob, int scalingPoints) {
        if (!(mob != null)) throw new AssertionError();

        Scoreboard scoreboard = mob.getEntityWorld().getScoreboard();
        ScoreboardObjective objective = getScalingObjective(scoreboard);
        ScoreAccess score = scoreboard.getOrCreateScore(mob, objective, true);
        score.setScore(scalingPoints);
    }

    public static int getScalingPoints(MobEntity mob, int scalingPoints) {
        if (!(mob != null)) throw new AssertionError();

        Scoreboard scoreboard = mob.getEntityWorld().getScoreboard();
        ScoreAccess score = scoreboard.getOrCreateScore(mob, getScalingObjective(scoreboard));
        return score.getScore();
    }

    public static void flagDownedEnderDragon(Scoreboard scoreboard) {
        if (!(scoreboard != null)) throw new AssertionError();

        ScoreboardObjective flag = scoreboard.getNullableObjective(OBJECTIVE_DOWNED_ENDER_DRAGON);
        if (flag == null) {
            scoreboard.addObjective(OBJECTIVE_DOWNED_ENDER_DRAGON, ScoreboardCriterion.DUMMY, Text.of(OBJECTIVE_DOWNED_ENDER_DRAGON),
                    ScoreboardCriterion.RenderType.INTEGER, true, null);
        }
    }

    public static boolean getFlagDownedEnderDragon(Scoreboard scoreboard) {
        if (!(scoreboard != null)) throw new AssertionError();

        ScoreboardObjective flag = scoreboard.getNullableObjective(OBJECTIVE_DOWNED_ENDER_DRAGON);
        return flag != null;
    }

    private static boolean mobEligibleForScaling(MobEntity mob) {
        return scalingEligible.contains(mob.getType());
    }

    private static int[] partitionInt(Random random, int total, int numPartitions) {
        if (!(random != null)) throw new AssertionError();
        if (!(total >= 0)) throw new AssertionError();
        if (!(numPartitions >= 1)) throw new AssertionError();

        // Insert numPartitions - 1 cuts randomly in [0, total].
        int numCuts = numPartitions - 1;
        int[] cuts = new int[numPartitions];
        for (int i = 0; i < cuts.length; i++) {
            cuts[i] = random.nextBetween(0, total);
        }
        Arrays.sort(cuts);

        // Use the distance between each cut to partition total.
        int totalSpent = 0;
        int[] partitions = new int[numPartitions];
        for (int i = 0; i < partitions.length - 1; i++) {
            partitions[i] = cuts[i] - totalSpent;
            totalSpent += partitions[i];
        }
        partitions[partitions.length - 1] = total - totalSpent;
        return partitions;
    }

    private static int[] shuffledRange(Random random, int n) {
        if (!(random != null)) throw new AssertionError();
        if (!(n >= 0)) throw new AssertionError();

        int[] arr = new int[n];
        for (int i = 0; i < n; i++) {
            arr[i] = i;
        }
        for (int i = 0; i < n; i++) {
            int pick = random.nextBetweenExclusive(i, n);
            int temp = arr[i];
            arr[i] = arr[pick];
            arr[pick] = temp;
        }
        return arr;
    }

    private static boolean isInIntRange(int n, int min, int max) {
        return min <= n && n <= max;
    }
}
