package com.scorp.fairanvil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ContainerEnchantment;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Enchanting-table container that keeps vanilla's level <em>requirement</em> (you still must reach
 * the displayed level to use a slot) but charges a flat XP-point cost instead of consuming whole
 * levels, matching the original FairAnvil plugin.
 *
 * <p>It defers to vanilla for the actual enchant generation (lapis cost, enchantment rolls, clues,
 * seed) and then corrects only the experience that was spent.
 */
public class ContainerFairEnchantment extends ContainerEnchantment {

    public ContainerFairEnchantment(InventoryPlayer playerInv, World worldIn, BlockPos pos) {
        super(playerInv, worldIn, pos);
    }

    @Override
    public boolean enchantItem(EntityPlayer player, int id) {
        if (player.world.isRemote || player.capabilities.isCreativeMode) {
            return super.enchantItem(player, id);
        }

        // Total points before vanilla deducts whole levels (vanilla only changes experienceLevel).
        int before = Experience.getTotalExperience(player);
        boolean enchanted = super.enchantItem(player, id);

        if (enchanted) {
            int levelsSpent = id + 1; // vanilla charges (button + 1) levels and lapis
            int pointCost = Experience.getExpFromLevel(levelsSpent);
            Experience.setTotalExperience(player, Math.max(0, before - pointCost));
        }

        return enchanted;
    }
}
