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
 * Tells the server to open our {@link ContainerFairAnvil} while reusing the vanilla
 * {@code minecraft:anvil} GUI on the client. That is what keeps this mod server-side only:
 * the client never needs our code, it just renders its built-in anvil screen.
 */
public class FairAnvilInteractionObject implements IInteractionObject {

    private final World world;
    private final BlockPos pos;

    public FairAnvilInteractionObject(World world, BlockPos pos) {
        this.world = world;
        this.pos = pos;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new ContainerFairAnvil(playerInventory, this.world, this.pos, playerIn);
    }

    @Override
    public String getGuiID() {
        return "minecraft:anvil";
    }

    @Override
    public String getName() {
        return "container.repair";
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentTranslation("container.repair");
    }
}
