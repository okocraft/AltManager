package net.okocraft.altmanager.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public final class GetIPCommand extends BaseCommand {

    protected GetIPCommand() {
        super(
                "altemanager.getip",
                2,
                false,
                "/altmanager getip <player>"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        if (!database.existPlayer(args[1])) {
            messages.sendPlayerNotFound(sender, args[1]);
            return false;
        }
        String address = database.getPlayerData("address", args[1]);
        sender.sendMessage(address);
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], database.getPlayersMap().values(), new ArrayList<>());
        }
        return List.of();
    }
} 