package net.okocraft.altmanager.command;

import java.util.List;

import org.bukkit.command.CommandSender;

public final class ReloadCommand extends BaseCommand {

    protected ReloadCommand() {
        super(
                "altmanager.reload",
                1,
                true,
                "/altmanager reload"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        config.reloadAllConfigs();
        messages.sendReloadSuccess(sender);
        return false;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
} 