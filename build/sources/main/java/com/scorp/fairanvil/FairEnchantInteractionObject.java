package com.scorp.fairanvil;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;

/**
 * Opens our {@link ContainerFairEnchantment} while reusing the vanilla
 * {@code minecraft:enchanting_table} GUI on the client (server-side only).
 */
public class FairEnchantInteractionObject implements IInteractionObject {

    private final World world;
    private final BlockPos pos;

    public FairEnchantInteractionObject(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new ContainerFairEnchantment(playerInventory, this.world, this.pos);
    }

    @Override
    public String getGuiID() {
        return "minecraft:enchanting_table";
    }

    @Override
    public String getName() {
        return "container.enchant";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation("container.enchant");
    }
}
