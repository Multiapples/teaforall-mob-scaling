# TeaForAll Mob Scaling

This is a configurable server-side plugin for scaling mob stats 
the further they spawn from (0,0) in the overworld, nether, and end.

## Mob Scaling Effects

Mobs may spawn in with some permanent combination of these effects.
The exact generation mechanics are somewhat configurable and described further down.

* Increased Health
* Fire Resistance
* Resistance 1
* Strength 1 - 4
* Speed 1 - 5
* Invisibility

## Other Features

### Nether Roof Damage over Time
Players take constant Wither damage on the roof of the Nether until the Ender Dragon has been defeated.
The exact Y-level can be configured.
### Stronghold Dead Zone
Prevents strongholds from being discovered via `/locate` or ender eyes within a configurable block radius around (0,0).

## Default Config

See `example_config.json` for a recommended default config.
Copy it into the config directory as `teaforall-mob-scaling.json` and modify as you wish.
The same example config generates automatically  if one cannot be found.
Any misformatted or invalid fields in the config causes undefined behaviour.
See this [desmos graph](https://www.desmos.com/calculator/8aoyppt4vs) for a visual of the default rampings.

# Technical

## Mob Scaling Mechanics

Upon spawning, mobs are assigned a number of points randomly chosen within an interval based on distance from (0,0).
These points are then randomly partitioned into Health, Damage, and Tech points.
Modifiers are then applied in a random order.
Each modifier has a cost and a category (Health/Damage/Tech),
and only succeeds in applying if there are enough points in that category to expend on the modifier.
Remaining points from all categories are collected and dumped into maximum health at a certain ratio.

## Config

Format the config correctly, or else the mod may crash or produced unexpected behaviour.
See the default config for reference.

* `netherDOT : boolean` Whether to enable the nether roof DoT effect. (Default `true`)
* `netherDOTYlevel : float` Nether DoT begins above this Y-level. (Default `123.5`)
* `strongholdDeadZone : double` Strongholds cannot be located within this radius. (Default `6000.0`)
* `mobScaling.eligibleMobs` Array of MobEntity identifiers. Only these mobs will be scaled.
* `mobScaling.rampings.overworld` Array of ramping segments to control points over distance in the overworld.
It is a similar case for the nether and end.
  * `startDist` and `endDist` refer to the distance interval for this specific ramping.
  * `startMinPoints` and `startMaxPoints` refer to the possible point range when a mob spawns at `startDist`
  * `endMinPoints` and `endMaxPoints` refer to the possible point range when a mob spawns at `endDist`
  * When a mobs spawns in the middle of the interval, the point range is linearly interpolated between start and end.
  * Any mob that doesn't spawn in a ramping interval receives zero points. 
    Mobs in overlapping intervals pick the first interval in the array.
  * Point ranges can go negative as mobs with less than or equal to zero points are unaltered.
* `mobScaling.defaultScaling` Base parameters that all mobs use initially for mob scaling.
  * `healthCost : int` When leftover points are converted into max health, every `healthCost` number of points is converted into 1 hp (0.5 hearts).
    Must be greater than zero.
  * `healthRealloc : float` After points are partition into categories but before modifiers are applied,
    `healthRealloc * healthPoints` points are taken from the health category and distributed evenly to the other categories.
    Use this to encourage more effects and less hp scaling.
  * `modifiers` Table that defines available modifiers and their attributes. See Modifier Identifiers for ID's.
    * `cost : int` The cost in points of applying the modifier. Must be greater than zero.
    * `failureChance : float` The chance that the modifier gets skipped. Must be between `0.0` and `1.0`.
* `mobScaling.overrides` Allows overriding or adding to any of the default attributes or modifiers, unique for each mob identifier given.
  If you want to disable a modifier in the overrides, override that modifier's failure chance to `1.0`. 

## Modifier Identifiers
* `speed-#` (tech) where `#` is 1 to 5
* `strength-#` (damage) where `#` is 1 to 4
* `invisibility` (tech)
* `fire-resistance` (tech)
* `resistance` (health)
