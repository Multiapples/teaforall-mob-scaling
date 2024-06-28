package io.github.multiapples;

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

import java.util.*;

public class MobScaling {
    private enum DIMENSION { OVERWORLD, NETHER, END }
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
    };
    private static final String OBJECTIVE_SCALING_POINTS = "teaforallscalingpoints";
    private static final String PERSISTENT_MODIFIER_HEALTH_SCALING = "teaforallmobscaling";
    private static final Text scalingPointsLocalization = Text.of("Scaling Points"); // TODO: Localization

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
            assert(startDist >= 0f);
            assert(endDist > startDist);
            assert(startMinPoints <= startMaxPoints);
            assert(endMinPoints <= endMaxPoints);
            this.startDist = startDist;
            this.endDist = endDist;
            this.startMinPoints = startMinPoints;
            this.startMaxPoints = startMaxPoints;
            this.endMinPoints = endMinPoints;
            this.endMaxPoints = endMaxPoints;
        }
    }

    private static class ScalingParameters {
        public float healthRealloc; // The fraction of health that gets split among damage and tech points
                                    // before applying modifiers.
        public Map<String, ScalingModifier> modifiersByIdentifier;

        public ScalingParameters() {
            healthRealloc = 0f;
            modifiersByIdentifier = new HashMap<>();
        }
    }

    private static class ScalingModifier {
        public String identifier; //TODO: use an enum
        public int cost;
        public float failureChance;

        public ScalingModifier(final String identifier, final int cost, final float failureChance) {
            assert(identifier != null);
            assert(cost >= 0);
            assert(failureChance >= 0f);
            assert(failureChance <= 1f);
            this.identifier = identifier;
            this.cost = cost;
            this.failureChance = failureChance;
        }
    }

    public static void initialize() { //TODO: take a config parameter
        //Registries.ENTITY_TYPE.containsId(Identifier.of("ender_dragon"));
        //Registries.ENTITY_TYPE.get(Identifier.of("ender_dragon")); // returns EntityType<?>; //TODO: make this work

        // Init modifier categories
        categoriesByModifier.put("speed-2", MODIFIER_CATEGORIES.TECH);
        categoriesByModifier.put("strength-2", MODIFIER_CATEGORIES.DAMAGE);
        assert(categoriesByModifier.entrySet()
                .stream()
                .anyMatch(entry -> isInIntRange(entry.getValue().getValue(), 0, NUMBER_OF_POINT_CATEGORIES - 1)));

        // Add eligible mobs for scaling.
        scalingEligible.add(Registries.ENTITY_TYPE.get(Identifier.of("ghast")));
        scalingEligible.add(Registries.ENTITY_TYPE.get(Identifier.of("creeper")));
        scalingEligible.add(Registries.ENTITY_TYPE.get(Identifier.of("zombie")));

        // Add rampings
        rampingsByDimension.put(DIMENSION.OVERWORLD, new ArrayList<>());
        rampingsByDimension.get(DIMENSION.OVERWORLD).add(new Ramping(10, 500, 0, 20, 500, 600));

        // Add default scaling parameters
        defaultScalingParameters.healthRealloc = 0.25f;
        defaultScalingParameters.modifiersByIdentifier.put("speed-2",
                new ScalingModifier("speed-2", 20, 0));
        defaultScalingParameters.modifiersByIdentifier.put("strength-2",
                new ScalingModifier("strength-2", 60, 0));

        // Add mob overrides
        ScalingParameters override = new ScalingParameters();
        override.healthRealloc = 0.95f;
        override.modifiersByIdentifier.put("speed-2",
                new ScalingModifier("speed-2", 20, 0.5f));
        scalingOverridesByMob.put(EntityType.ZOMBIE, override);
    }

    public static void assignMobScaling(MobEntity mob) {
        assert(mob != null);

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
        // TODO RAMPING
        int scalingPoints = Math.clamp(Math.round(pos.multiply(1, 0, 1).length() / 100f),
                0, Integer.MAX_VALUE); // Randomize here
        setScalingPoints(mob, scalingPoints);
        scaleMobEntity(mob, scalingPoints);
    }

    public static ScalingParameters getMobScalingParameters(MobEntity mob) {
        assert(mob != null);

        if (scalingOverridesByMob.containsKey(mob.getType())) {
            return scalingOverridesByMob.get(mob.getType());
        }
        return defaultScalingParameters;
    }

    public static void scaleMobEntity(MobEntity mob, int scalingPoints) {
        assert(mob != null);
        if (scalingPoints <= 0) {
            return;
        }

        Random random = mob.getRandom();
        boolean canHaveMobSpecificModifiers = false; //TODO
        boolean canHavePotions = true;
        boolean canHaveWeapons = false;

        ScalingParameters scalingParameters = getMobScalingParameters(mob);
        List<ScalingModifier> modifiers = new ArrayList<>(scalingParameters.modifiersByIdentifier.values()); // Maybe not the most efficient thing

        int[] budgets = partitionInt(mob.getRandom(), scalingPoints, 3); // Point budgets for each modifier category.
        assert(budgets.length == NUMBER_OF_POINT_CATEGORIES);

        // Apply health realloc.
        if (NUMBER_OF_POINT_CATEGORIES > 1) {
            int reallocDelta = (int) (scalingParameters.healthRealloc * budgets[0] / (float) (NUMBER_OF_POINT_CATEGORIES - 1));
            for (int category = 1; category < NUMBER_OF_POINT_CATEGORIES; category++) {
                budgets[category] += reallocDelta;
            }
            budgets[0] -= reallocDelta * (NUMBER_OF_POINT_CATEGORIES - 1);
        }

        System.out.println("HPP: " + budgets[0]); //TODO remove
        System.out.println("DMG: " + budgets[1]);
        System.out.println("TCP: " + budgets[2]);

        int[] order = shuffledRange(random, modifiers.size());
        for (int i : order) {
            ScalingModifier mod = modifiers.get(i);

            if (!categoriesByModifier.containsKey(mod.identifier)) {
                System.out.println("Warning, modifier lacks associated points category"); //TODO use logger
                continue;
            }
            int category = categoriesByModifier.get(mod.identifier).getValue();

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
                case "speed-2" ->
                        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, StatusEffectInstance.INFINITE, 1));
                case "strength-2" ->
                        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, StatusEffectInstance.INFINITE, 1));
                default ->
                        System.out.println("uh oh, " + mod.identifier + " was not a valid modifier!!!"); //TODO: use logger
            }
        }

        System.out.println("HPP': " + budgets[0]); //TODO remove
        System.out.println("DMG': " + budgets[1]);
        System.out.println("TCP': " + budgets[2]);

        // Dump remaining points into health.
        for (int category = 1; category < NUMBER_OF_POINT_CATEGORIES; category++) {
            budgets[0] += budgets[category];
            budgets[category] = 0;
        }
        int healthPoints = budgets[0];
        EntityAttributeInstance attributeInstance = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attributeInstance == null) {
            System.out.println("Null EntityAttributeInstance"); // TODO: Use Logger
        } else {
            float addHealth = healthPoints * 0.1f;
            attributeInstance.addPersistentModifier(new EntityAttributeModifier(
                    Identifier.of(PERSISTENT_MODIFIER_HEALTH_SCALING),
                    addHealth, EntityAttributeModifier.Operation.ADD_VALUE));
            mob.heal(healthPoints);
        }

        // Glow for now, for testing: //TODO, remove
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, StatusEffectInstance.INFINITE, 0));
    }

    public static ScoreboardObjective getScalingObjective(Scoreboard scoreboard) {
        assert(scoreboard != null);

        ScoreboardObjective scalingPointsObjective = scoreboard.getNullableObjective(OBJECTIVE_SCALING_POINTS);
        if (scalingPointsObjective == null) {
            scalingPointsObjective = scoreboard.addObjective(OBJECTIVE_SCALING_POINTS, ScoreboardCriterion.DUMMY, scalingPointsLocalization,
                    ScoreboardCriterion.RenderType.INTEGER, true, null);
        }
        assert(scalingPointsObjective != null); // sanity check
        assert(scalingPointsObjective.getScoreboard() == scoreboard); // sanity check
        return scalingPointsObjective;
    }

    public static void setScalingPoints(MobEntity mob, int scalingPoints) {
        assert(mob != null);

        Scoreboard scoreboard = mob.getEntityWorld().getScoreboard();
        ScoreboardObjective objective = getScalingObjective(scoreboard);
        ScoreAccess score = scoreboard.getOrCreateScore(mob, objective, true);
        score.setScore(scalingPoints);
    }

    public static int getScalingPoints(MobEntity mob, int scalingPoints) {
        assert(mob != null);

        Scoreboard scoreboard = mob.getEntityWorld().getScoreboard();
        ScoreAccess score = scoreboard.getOrCreateScore(mob, getScalingObjective(scoreboard));
        return score.getScore();
    }

    private static boolean mobEligibleForScaling(MobEntity mob) {
        return scalingEligible.contains(mob.getType());
    }

    private static int[] partitionInt(Random random, int total, int numPartitions) {
        assert(random != null);
        assert(total >= 0);
        assert(numPartitions >= 1);

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
        assert(random != null);
        assert(n >= 0);

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
