package io.github.multiapples;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.*;
import net.minecraft.scoreboard.*;
import net.minecraft.scoreboard.number.FixedNumberFormat;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.joml.Vector2d;

import java.util.*;

public class MobScaling {
    private enum DIMENSION { OVERWORLD, NETHER, END }

    private static final Collection<EntityType<?>> scalingEligible = new HashSet<>();
    private static final String OBJECTIVE_SCALING_POINTS = "teaforallscalingpoints";
    private static final String PERSISTENT_MODIFIER_HEALTH_SCALING = "teaforallmobscaling";
    private static final Text scalingPointsLocalization = Text.of("Scaling Points"); // TODO: Localization

    private static class mobScalingModifier {
        public String modifier; //TODO: use an enum
        public int cost;

        public mobScalingModifier(String modifier, int cost) {
            assert(modifier != null);
            assert(cost >= 0);
            this.modifier = modifier;
            this.cost = cost;
        }
    }

    public static void populateEligible() {
        Registries.ENTITY_TYPE.containsId(Identifier.of("ender_dragon"));
        Registries.ENTITY_TYPE.get(Identifier.of("ender_dragon")); // returns EntityType<?>; //TODO: make this work

        scalingEligible.add(Registries.ENTITY_TYPE.get(Identifier.of("ghast")));
        scalingEligible.add(Registries.ENTITY_TYPE.get(Identifier.of("creeper")));
        scalingEligible.add(Registries.ENTITY_TYPE.get(Identifier.of("zombie")));
    }

    private static boolean mobEligibleForScaling(MobEntity mob) {
        return scalingEligible.contains(mob.getType());
    }

    public static void initMobScalingPoints(MobEntity mob) {
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

        // Pull config data here from public fields, set when this mod inits
        // but for now, give mobs a flat points
        int scalingPoints = Math.clamp(Math.round(pos.multiply(1, 0, 1).length() / 100f),
                0, Integer.MAX_VALUE); // Randomize here
        setScalingPoints(mob, scalingPoints);
        mobScaleMobEntity(mob, scalingPoints);
    }

    private static List<mobScalingModifier> tempppp = null;
    public static List<mobScalingModifier> getMobScalingModifierPool(MobEntity mob) {
        assert(mob != null);

        // TODO: contact config file to grab these. also this temp code is not thread safe
        if (tempppp == null) {
            tempppp = new ArrayList<>();
            tempppp.add(new mobScalingModifier("speed_2", 20));
            tempppp.add(new mobScalingModifier("strength_2", 60));
        }
        return tempppp;
    }

    public static void mobScaleMobEntity(MobEntity mob, int scalingPoints) {
        assert(mob != null);
        if (scalingPoints <= 0) {
            return;
        }

        Random random = mob.getRandom();
        boolean canHaveMobSpecificModifiers = false; //TODO
        boolean canHavePotions = true;
        boolean canHaveWeapons = false;

        int[] partitions = partitionInt(mob.getRandom(), scalingPoints, 3);
        assert(partitions.length == 3);
        int healthPoints = partitions[0];
        int damagePoints = partitions[1];
        int techPoints = partitions[2];

        System.out.println("HPP: " + healthPoints); //TODO remove
        System.out.println("DMG: " + damagePoints);
        System.out.println("TCP: " + techPoints);

        // healthPoints
        // TODO: Grab costs and weights from config file
        // temp:
        List<mobScalingModifier> modifiers = getMobScalingModifierPool(mob);
        int[] order = shuffledRange(random, modifiers.size());
        for (int i : order) {
            mobScalingModifier m = modifiers.get(i);
            if (m.cost > healthPoints) {
                continue;
            }
            healthPoints -= m.cost;

            // Apply the modifier
            switch (m.modifier) {
                case "speed_2" ->
                        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, StatusEffectInstance.INFINITE, 1));
                case "strength_2" ->
                        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, StatusEffectInstance.INFINITE, 1));
                default ->
                        System.out.println("uh oh, " + m.modifier + " was not a valid modifier!!!");
            }
        }

        // Dump remaining points into health.
        healthPoints += damagePoints + techPoints;
        damagePoints = 0;
        techPoints = 0;
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
        System.out.println(Arrays.toString(cuts));
        System.out.println(total);
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
}
