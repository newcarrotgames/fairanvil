package com.scorp.fairanvil;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * FairAnvil for Forge 1.12.2.
 *
 * <p>Server-side only: {@code acceptableRemoteVersions = "*"} lets vanilla and modpack clients
 * connect without installing the mod, and it registers no blocks/items/GUIs of its own. Anvils and
 * enchanting tables are intercepted on right-click and re-opened with custom containers that charge
 * flat XP points (and, for anvils, ignore the "Too Expensive!" limit).
 */
@Mod(modid = FairAnvilMod.MODID, name = FairAnvilMod.NAME, version = FairAnvilMod.VERSION,
        acceptableRemoteVersions = "*")
public class FairAnvilMod {

    public static final String MODID = "fairanvil";
    public static final String NAME = "FairAnvil";
    public static final String VERSION = "1.2";

    public static boolean enabled = true;
    public static boolean fairAnvil = true;
    public static boolean fairEnchant = true;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();

        fairAnvil = config.getBoolean("fairAnvil", Configuration.CATEGORY_GENERAL, true,
                "Anvils charge their level cost as XP points counted from level 0, instead of taking levels off the top.");
        fairEnchant = config.getBoolean("fairEnchant", Configuration.CATEGORY_GENERAL, true,
                "The enchanting table charges a flat XP-point cost instead of consuming whole levels.");
        // Removed in 1.2: the cost is no longer clamped below 40.
        config.getCategory(Configuration.CATEGORY_GENERAL).remove("hideTooExpensive");

        if (config.hasChanged()) {
            config.save();
        }

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandFairAnvil());
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!enabled) {
            return;
        }
        if (event.getHand() != EnumHand.MAIN_HAND) {
            return;
        }

        World world = event.getWorld();
        if (world.isRemote) {
            return;
        }

        EntityPlayer player = event.getEntityPlayer();
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        BlockPos pos = event.getPos();
        Block block = world.getBlockState(pos).getBlock();
        boolean isAnvil = block == Blocks.ANVIL;
        boolean isEnchant = block == Blocks.ENCHANTING_TABLE;

        if (!isAnvil && !isEnchant) {
            return;
        }
        if (isAnvil && !fairAnvil) {
            return;
        }
        if (isEnchant && !fairEnchant) {
            return;
        }

        // Mirror vanilla: sneaking with an item in hand should place/use the item, not open the block.
        boolean holdingItem = !player.getHeldItemMainhand().isEmpty() || !player.getHeldItemOffhand().isEmpty();
        if (player.isSneaking() && holdingItem) {
            return;
        }

        event.setCanceled(true);

        if (isAnvil) {
            player.displayGui(new FairAnvilInteractionObject(world, pos));
        } else {
            player.displayGui(new FairEnchantInteractionObject(world, pos));
        }
    }
}
