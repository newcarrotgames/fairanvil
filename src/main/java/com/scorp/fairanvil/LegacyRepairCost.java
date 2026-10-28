package com.scorp.fairanvil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.Loader;

/**
 * One-time repair for items worked on by FairAnvil 1.1 and older.
 *
 * <p>Those versions re-implemented the anvil maths, which bypassed EnchantmentControl's mixins on
 * {@code ContainerRepair}. Such items got vanilla's doubling prior-work penalty (RepairCost
 * {@code 2^n - 1}, so 32767 after 15 uses) and no EnchantmentControl {@code AnvilCount}. This
 * rebases them onto the scaling EnchantmentControl is configured with, as if it had handled every
 * one of those {@code n} uses. Items that already carry {@code AnvilCount} are left alone.
 *
 * <p>EnchantmentControl is called by reflection so FairAnvil still runs without it; if anything
 * is missing the item is left untouched.
 */
final class LegacyRepairCost {

    private static final String ANVIL_COUNT = "AnvilCount";

    private static boolean resolved;
    private static Method nextRepairCost;
    private static Object anvilConfig;
    private static Field scalingEnabled;
    private static Field scalingFactor;

    private LegacyRepairCost() {}

    static void normalize(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        NBTTagCompound tag = stack.getTagCompound();
        int cost = stack.getRepairCost();
        if (tag == null || cost <= 0 || tag.hasKey(ANVIL_COUNT) || Integer.bitCount(cost + 1) != 1) {
            return;
        }
        if (!resolve()) {
            return;
        }

        int uses = Integer.numberOfTrailingZeros(cost + 1);
        try {
            if (!scalingEnabled.getBoolean(anvilConfig)) {
                return; // EnchantmentControl keeps vanilla's doubling, which these items already have
            }
            float factor = scalingFactor.getFloat(anvilConfig);
            int scaled = 0;
            for (int use = 1; use <= uses; ++use) {
                scaled = (Integer) nextRepairCost.invoke(null, scaled, use, factor);
            }
            tag.setInteger(ANVIL_COUNT, uses);
            stack.setRepairCost(scaled);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Leave the item as it is.
        }
    }

    private static boolean resolve() {
        if (!resolved) {
            resolved = true;
            if (Loader.isModLoaded("enchantmentcontrol")) {
                try {
                    nextRepairCost = Class.forName("enchantmentcontrol.util.AnvilCostUtil")
                            .getMethod("getNextRepairCost", int.class, int.class, float.class);
                    Field anvil = Class.forName("enchantmentcontrol.config.ConfigHandler").getField("anvil");
                    anvilConfig = anvil.get(null);
                    scalingEnabled = anvilConfig.getClass().getField("enableAnvilScalingChange");
                    scalingFactor = anvilConfig.getClass().getField("repairCostScalingFactor");
                } catch (ReflectiveOperationException | RuntimeException e) {
                    nextRepairCost = null;
                }
            }
        }
        return nextRepairCost != null;
    }
}
