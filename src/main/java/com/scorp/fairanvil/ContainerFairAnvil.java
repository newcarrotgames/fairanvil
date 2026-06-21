package com.scorp.fairanvil;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.block.BlockAnvil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemEnchantedBook;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;

/**
 * A drop-in replacement for the vanilla anvil container that:
 * <ul>
 *   <li>removes the "Too Expensive!" 40-level limit, and</li>
 *   <li>charges a flat XP-point cost (see {@link Experience}) instead of scaling levels.</li>
 * </ul>
 *
 * <p>The combine/repair maths mirror vanilla {@code ContainerRepair#updateRepairOutput} so that
 * RLCraft's enchantments behave exactly as the pack expects; only the cost gate and the level
 * deduction are changed. {@link AnvilUpdateEvent} is still posted so other mods that add anvil
 * recipes keep working.
 */
public class ContainerFairAnvil extends ContainerRepair {

    private final World world;
    private final BlockPos pos;
    private final EntityPlayer user;
    private final IInventory inputInv;
    private final IInventory outputInv;

    /** Level cost actually shown and charged (clamped per hideTooExpensive); drives the XP-point cost. */
    private int costLevels;
    /** Number of repair-material items consumed from the second input slot. */
    private int matCost;
    private String repairName = "";

    public ContainerFairAnvil(InventoryPlayer playerInv, World world, BlockPos pos, EntityPlayer player) {
        super(playerInv, world, pos, player);
        this.world = world;
        this.pos = pos;
        this.user = player;
        this.inputInv = this.inventorySlots.get(0).inventory;
        this.outputInv = this.inventorySlots.get(2).inventory;

        // Swap the vanilla output slot (which deducts whole levels) for our point-based one.
        FairOutputSlot output = new FairOutputSlot(this.outputInv, 0, 134, 47);
        output.slotNumber = 2;
        this.inventorySlots.set(2, output);
    }

    @Override
    public void updateItemName(String newName) {
        this.repairName = newName == null ? "" : newName;
        this.updateRepairOutput();
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
        ItemStack itemstack = this.inputInv.getStackInSlot(0);
        this.costLevels = 0;
        int i = 0;
        int j = 0;
        int k = 0;

        // Mod compatibility: let other mods (or RLCraft) supply or cancel the anvil result.
        AnvilUpdateEvent event = new AnvilUpdateEvent(itemstack, this.inputInv.getStackInSlot(1), this.repairName, 0);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            this.outputInv.setInventorySlotContents(0, ItemStack.EMPTY);
            this.applyCost(0);
            return;
        }
        if (!event.getOutput().isEmpty()) {
            this.outputInv.setInventorySlotContents(0, event.getOutput());
            this.matCost = event.getMaterialCost();
            this.applyCost(event.getCost());
            return;
        }

        if (itemstack.isEmpty()) {
            this.outputInv.setInventorySlotContents(0, ItemStack.EMPTY);
            this.applyCost(0);
            return;
        }

        ItemStack itemstack1 = itemstack.copy();
        ItemStack itemstack2 = this.inputInv.getStackInSlot(1);
        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(itemstack1);
        boolean flag = false;
        j = j + itemstack.getRepairCost() + (itemstack2.isEmpty() ? 0 : itemstack2.getRepairCost());
        this.matCost = 0;

        if (!itemstack2.isEmpty()) {
            flag = itemstack2.getItem() == Items.ENCHANTED_BOOK && ItemEnchantedBook.getEnchantments(itemstack2).tagCount() > 0;

            if (itemstack1.isItemStackDamageable() && itemstack1.getItem().getIsRepairable(itemstack, itemstack2)) {
                int l2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);

                if (l2 <= 0) {
                    this.outputInv.setInventorySlotContents(0, ItemStack.EMPTY);
                    this.applyCost(0);
                    return;
                }

                int i3;
                for (i3 = 0; l2 > 0 && i3 < itemstack2.getCount(); ++i3) {
                    int j3 = itemstack1.getItemDamage() - l2;
                    itemstack1.setItemDamage(j3);
                    ++i;
                    l2 = Math.min(itemstack1.getItemDamage(), itemstack1.getMaxDamage() / 4);
                }

                this.matCost = i3;
            } else {
                if (!flag && (itemstack1.getItem() != itemstack2.getItem() || !itemstack1.isItemStackDamageable())) {
                    this.outputInv.setInventorySlotContents(0, ItemStack.EMPTY);
                    this.applyCost(0);
                    return;
                }

                if (itemstack1.isItemStackDamageable() && !flag) {
                    int l = itemstack.getMaxDamage() - itemstack.getItemDamage();
                    int i1 = itemstack2.getMaxDamage() - itemstack2.getItemDamage();
                    int j1 = i1 + itemstack1.getMaxDamage() * 12 / 100;
                    int k1 = l + j1;
                    int l1 = itemstack1.getMaxDamage() - k1;

                    if (l1 < 0) {
                        l1 = 0;
                    }

                    if (l1 < itemstack1.getMetadata()) {
                        itemstack1.setItemDamage(l1);
                        i += 2;
                    }
                }

                Map<Enchantment, Integer> map1 = EnchantmentHelper.getEnchantments(itemstack2);
                boolean flag2 = false;
                boolean flag3 = false;

                for (Enchantment enchantment1 : map1.keySet()) {
                    if (enchantment1 != null) {
                        int i2 = map.containsKey(enchantment1) ? map.get(enchantment1).intValue() : 0;
                        int j2 = map1.get(enchantment1).intValue();
                        j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2);
                        boolean flag1 = enchantment1.canApply(itemstack);

                        if (this.user.capabilities.isCreativeMode || itemstack.getItem() == Items.ENCHANTED_BOOK) {
                            flag1 = true;
                        }

                        for (Enchantment enchantment : map.keySet()) {
                            if (enchantment != enchantment1 && !enchantment1.isCompatibleWith(enchantment)) {
                                flag1 = false;
                                ++i;
                            }
                        }

                        if (!flag1) {
                            flag3 = true;
                        } else {
                            flag2 = true;

                            if (j2 > enchantment1.getMaxLevel()) {
                                j2 = enchantment1.getMaxLevel();
                            }

                            map.put(enchantment1, Integer.valueOf(j2));
                            int k3 = 0;

                            switch (enchantment1.getRarity()) {
                                case COMMON:
                                    k3 = 1;
                                    break;
                                case UNCOMMON:
                                    k3 = 2;
                                    break;
                                case RARE:
                                    k3 = 4;
                                    break;
                                case VERY_RARE:
                                    k3 = 8;
                            }

                            if (flag) {
                                k3 = Math.max(1, k3 / 2);
                            }

                            i += k3 * j2;

                            if (itemstack.getCount() > 1) {
                                i = 40;
                            }
                        }
                    }
                }

                if (flag3 && !flag2) {
                    this.outputInv.setInventorySlotContents(0, ItemStack.EMPTY);
                    this.applyCost(0);
                    return;
                }
            }
        }

        if (StringUtils.isBlank(this.repairName)) {
            if (itemstack.hasDisplayName()) {
                k = 1;
                i += k;
                itemstack1.clearCustomName();
            }
        } else if (!this.repairName.equals(itemstack.getDisplayName())) {
            k = 1;
            i += k;
            itemstack1.setStackDisplayName(this.repairName);
        }

        int cost = j + i;

        if (i <= 0) {
            itemstack1 = ItemStack.EMPTY;
        }

        if (k == i && k > 0 && cost >= 40) {
            cost = 39;
        }

        // Vanilla clears the output here when cost >= 40 ("Too Expensive!"). FairAnvil keeps it.

        if (!itemstack1.isEmpty()) {
            int k2 = itemstack1.getRepairCost();

            if (!itemstack2.isEmpty() && k2 < itemstack2.getRepairCost()) {
                k2 = itemstack2.getRepairCost();
            }

            if (k != i || k == 0) {
                k2 = k2 * 2 + 1;
            }

            itemstack1.setRepairCost(k2);
            EnchantmentHelper.setEnchantments(map, itemstack1);
        }

        this.outputInv.setInventorySlotContents(0, itemstack1);
        this.applyCost(cost);
    }

    /**
     * Sets the level cost that is BOTH shown to the client and used to derive the XP-point charge,
     * so the displayed price always matches what the player is actually charged.
     *
     * <p>When {@code hideTooExpensive} is on, the cost is clamped below 40 so the vanilla client never
     * renders "Too Expensive!" - and the point charge is clamped to match, otherwise an item that
     * looks affordable would be silently un-takeable for players who lack the (much larger) XP for the
     * true uncapped cost.
     */
    private void applyCost(int cost) {
        this.costLevels = FairAnvilMod.hideTooExpensive ? Math.min(cost, 39) : cost;
        this.maximumCost = this.costLevels;
        for (IContainerListener listener : this.listeners) {
            listener.sendWindowProperty(this, 0, this.maximumCost);
        }
        this.detectAndSendChanges();
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
            return (playerIn.capabilities.isCreativeMode
                    || Experience.getTotalExperience(playerIn) >= Experience.getExpFromLevel(ContainerFairAnvil.this.costLevels))
                    && ContainerFairAnvil.this.costLevels > 0
                    && this.getHasStack();
        }

        @Override
        public ItemStack onTake(EntityPlayer thePlayer, ItemStack stack) {
            if (!thePlayer.capabilities.isCreativeMode) {
                Experience.changeExp(thePlayer, -Experience.getExpFromLevel(ContainerFairAnvil.this.costLevels));
            }

            ContainerFairAnvil.this.inputInv.setInventorySlotContents(0, ItemStack.EMPTY);

            if (ContainerFairAnvil.this.matCost > 0) {
                ItemStack material = ContainerFairAnvil.this.inputInv.getStackInSlot(1);

                if (!material.isEmpty() && material.getCount() > ContainerFairAnvil.this.matCost) {
                    material.shrink(ContainerFairAnvil.this.matCost);
                    ContainerFairAnvil.this.inputInv.setInventorySlotContents(1, material);
                } else {
                    ContainerFairAnvil.this.inputInv.setInventorySlotContents(1, ItemStack.EMPTY);
                }
            } else {
                ContainerFairAnvil.this.inputInv.setInventorySlotContents(1, ItemStack.EMPTY);
            }

            ContainerFairAnvil.this.maximumCost = 0;
            ContainerFairAnvil.this.costLevels = 0;

            IBlockState state = ContainerFairAnvil.this.world.getBlockState(ContainerFairAnvil.this.pos);

            if (!thePlayer.capabilities.isCreativeMode && !ContainerFairAnvil.this.world.isRemote
                    && state.getBlock() == Blocks.ANVIL && thePlayer.getRNG().nextFloat() < 0.12F) {
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
