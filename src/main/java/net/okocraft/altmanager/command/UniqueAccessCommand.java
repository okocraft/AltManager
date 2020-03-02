package net.okocraft.altmanager.command;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public final class UniqueAccessCommand extends BaseCommand {

    protected UniqueAccessCommand() {
        super(
                "altemanager.uniqueaccess",
                1,
                false,
                "/altmanager uniqueaccess",
                "unique"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        sender.sendMessage(String.valueOf(Bukkit.getOnlinePlayers().stream()
                .map(player -> player.getAddress().getAddress().getHostAddress()).distinct().count()));

        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
} 