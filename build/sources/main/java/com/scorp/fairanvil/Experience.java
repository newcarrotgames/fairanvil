package com.scorp.fairanvil;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Experience point math, ported from the original Bukkit FairAnvil plugin.
 *
 * <p>Minecraft stores a player's XP as a level + progress bar, but the number of points per level
 * grows non-linearly. Vanilla anvils/enchanting charge whole <em>levels</em>, which becomes brutally
 * expensive at high levels. FairAnvil instead charges a flat number of <em>points</em> equal to the
 * cumulative XP of {@code cost} levels measured from level 0, so the price no longer scales with how
 * high the player's level happens to be.
 *
 * @see <a href="https://minecraft.gamepedia.com/Experience#Leveling_up">Experience#Leveling_up</a>
 */
public final class Experience {

    private Experience() {}

    /** Total cumulative experience required to reach {@code level} starting from level 0. */
    public static int getExpFromLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level > 30) {
            return (int) (4.5D * level * level - 162.5D * level + 2220.0D);
        }
        if (level > 15) {
            return (int) (2.5D * level * level - 40.5D * level + 360.0D);
        }
        return level * level + 6 * level;
    }

    /** Points required to progress from {@code level} to {@code level + 1}. */
    private static int getExpToNext(int level) {
        if (level >= 30) {
            return level * 9 - 158;
        }
        if (level >= 15) {
            return level * 5 - 38;
        }
        return level * 2 + 7;
    }

    /** Whole level for a given total experience. */
    public static int getIntLevelFromExp(long exp) {
        if (exp > 1395L) {
            return (int) ((Math.sqrt(72.0D * exp - 54215.0D) + 325.0D) / 18.0D);
        }
        if (exp > 315L) {
            return (int) (Math.sqrt(40.0D * exp - 7839.0D) / 10.0D + 8.1D);
        }
        if (exp > 0L) {
            return (int) (Math.sqrt(exp + 9.0D) - 3.0D);
        }
        return 0;
    }

    /** Level including fractional progress for a given total experience. */
    public static double getLevelFromExp(long exp) {
        int level = getIntLevelFromExp(exp);
        float remainder = exp - (float) getExpFromLevel(level);
        float progress = remainder / getExpToNext(level);
        return (double) level + progress;
    }

    /** A player's current total experience derived from level + progress bar. */
    public static int getTotalExperience(EntityPlayer player) {
        return getExpFromLevel(player.experienceLevel)
                + Math.round(getExpToNext(player.experienceLevel) * player.experience);
    }

    /** Overwrite a player's experience with an exact point total. */
    public static void setTotalExperience(EntityPlayer player, int exp) {
        if (exp < 0) {
            exp = 0;
        }
        double levelAndExp = getLevelFromExp(exp);
        int level = (int) levelAndExp;
        player.experienceLevel = level;
        player.experience = (float) (levelAndExp - level);
        player.experienceTotal = exp;
    }

    /** Add or remove points from a player, keeping level + progress + total consistent. */
    public static void changeExp(EntityPlayer player, int delta) {
        setTotalExperience(player, getTotalExperience(player) + delta);
    }
}
