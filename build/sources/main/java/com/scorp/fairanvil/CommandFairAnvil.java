package com.scorp.fairanvil;

import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/**
 * {@code /fairanvil <toggle|anvil|enchant|status>} - the Forge equivalent of the plugin's
 * {@code /fairanvils toggle}. Requires permission level 2 (op) by default.
 */
public class CommandFairAnvil extends CommandBase {

    @Override
    public String getName() {
        return "fairanvil";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/fairanvil <toggle|anvil|enchant|status>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            sender.sendMessage(new TextComponentString(status()));
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("toggle")) {
            FairAnvilMod.enabled = !FairAnvilMod.enabled;
            sender.sendMessage(new TextComponentString("FairAnvil " + (FairAnvilMod.enabled ? "enabled" : "disabled")));
        } else if (sub.equals("anvil")) {
            FairAnvilMod.fairAnvil = !FairAnvilMod.fairAnvil;
            sender.sendMessage(new TextComponentString("Fair anvils " + (FairAnvilMod.fairAnvil ? "enabled" : "disabled")));
        } else if (sub.equals("enchant")) {
            FairAnvilMod.fairEnchant = !FairAnvilMod.fairEnchant;
            sender.sendMessage(new TextComponentString("Fair enchanting " + (FairAnvilMod.fairEnchant ? "enabled" : "disabled")));
        } else if (sub.equals("status")) {
            sender.sendMessage(new TextComponentString(status()));
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private static String status() {
        return "FairAnvil: " + (FairAnvilMod.enabled ? "ON" : "OFF")
                + " | anvil=" + FairAnvilMod.fairAnvil
                + " | enchant=" + FairAnvilMod.fairEnchant;
    }
}
