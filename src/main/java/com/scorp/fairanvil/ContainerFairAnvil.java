package com.scorp.fairanvil;

import net.minecraft.block.BlockAnvil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

/**
 * A drop-in replacement for the vanilla anvil container that charges the level cost as a flat
 * number of XP points, counted from level 0 (see {@link Experience}), instead of taking whole
 * levels off the top of the player's current level.
 *
 * <p>The level cost itself is never computed here: {@link #updateRepairOutput()} defers to
 * vanilla's {@code ContainerRepair#updateRepairOutput}, so every mixin and event that patches the
 * vanilla anvil (EnchantmentControl's cost scaling, "Never Too Expensive", free book combination,
 * Forge's {@code AnvilUpdateEvent}, ...) applies exactly as it would without FairAnvil. Only the
 * output slot is swapped, to change how that cost is paid.
 */
public class ContainerFairAnvil extends ContainerRepair {

    private final World world;
    private final BlockPos pos;
    private final IInventory inputInv;
    private final IInventory outputInv;

    public ContainerFairAnvil(InventoryPlayer playerInv, World world, BlockPos pos, EntityPlayer player) {
        super(playerInv, world, pos, player);
        this.world = world;
        this.pos = pos;
        this.inputInv = this.inventorySlots.get(0).inventory;
        this.outputInv = this.inventorySlots.get(2).inventory;

        // Swap the vanilla output slot (which deducts whole levels) for our point-based one.
        FairOutputSlot output = new FairOutputSlot(this.outputInv, 0, 134, 47);
        output.slotNumber = 2;
        this.inventorySlots.set(2, output);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        // Stop shift-clicking the result when the player cannot afford it (vanilla's transfer path
        // would otherwise bypass the point check).
        Slot slot = this.inventorySlots.get(index);
        if (index == 2 && slot != null && slot.getHasStack() && !slot.canTakeStack(playerIn)) {
            return ItemStack.EMPTY;
        }
        return super.transferStackInSlot(playerIn, index);
    }

    @Override
    public void updateRepairOutput() {
        LegacyRepairCost.normalize(this.inputInv.getStackInSlot(0));
        LegacyRepairCost.normalize(this.inputInv.getStackInSlot(1));
        super.updateRepairOutput();
    }

    /** Output slot that gates and charges in flat XP points instead of levels. */
    private class FairOutputSlot extends Slot {

        FairOutputSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            int cost = ContainerFairAnvil.this.maximumCost;
            return (playerIn.capabilities.isCreativeMode
                    || Experience.getTotalExperience(playerIn) >= Experience.getExpFromLevel(cost))
                    && cost > 0
                    && this.getHasStack();
        }

        @Override
        public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
            if (!thePlayer.capabilities.isCreativeMode) {
                Experience.changeExp(thePlayer, -Experience.getExpFromLevel(ContainerFairAnvil.this.maximumCost));
            }

            // Mirror vanilla: post AnvilRepairEvent while both inputs are still in place. Mods hook it
            // to keep or refund inputs (e.g. Enchanter Tools puts the Enchanted Inkwell and the rest of
            // the book stack back next tick) and to change the anvil break chance.
            float breakChance = ForgeHooks.onAnvilRepair(thePlayer, stack,
                    ContainerFairAnvil.this.inputInv.getStackInSlot(0),
                    ContainerFairAnvil.this.inputInv.getStackInSlot(1));

            ContainerFairAnvil.this.inputInv.setInventorySlotContents(0, ItemStack.EMPTY);

            int matCost = ContainerFairAnvil.this.materialCost;
            if (matCost > 0) {
                ItemStack material = ContainerFairAnvil.this.inputInv.getStackInSlot(1);

                if (!material.isEmpty() && material.getCount() > matCost) {
                    material.shrink(matCost);
                    ContainerFairAnvil.this.inputInv.setInventorySlotContents(1, material);
                } else {
                    ContainerFairAnvil.this.inputInv.setInventorySlotContents(1, ItemStack.EMPTY);
                }
            } else {
                ContainerFairAnvil.this.inputInv.setInventorySlotContents(1, ItemStack.EMPTY);
            }

            ContainerFairAnvil.this.maximumCost = 0;

            IBlockState state = ContainerFairAnvil.this.world.getBlockState(ContainerFairAnvil.this.pos);

            if (!thePlayer.capabilities.isCreativeMode && !ContainerFairAnvil.this.world.isRemote
                    && state.getBlock() == Blocks.ANVIL && thePlayer.getRNG().nextFloat() < breakChance) {
                int damage = state.getValue(BlockAnvil.DAMAGE).intValue() + 1;

                if (damage > 2) {
                    ContainerFairAnvil.this.world.setBlockToAir(ContainerFairAnvil.this.pos);
                    ContainerFairAnvil.this.world.playEvent(1029, ContainerFairAnvil.this.pos, 0);
                } else {
                    ContainerFairAnvil.this.world.setBlockState(ContainerFairAnvil.this.pos,
                            state.withProperty(BlockAnvil.DAMAGE, Integer.valueOf(damage)), 2);
                    ContainerFairAnvil.this.world.playEvent(1030, ContainerFairAnvil.this.pos, 0);
                }
            } else if (!ContainerFairAnvil.this.world.isRemote) {
                ContainerFairAnvil.this.world.playEvent(1030, ContainerFairAnvil.this.pos, 0);
            }

            return stack;
        }
    }
}
