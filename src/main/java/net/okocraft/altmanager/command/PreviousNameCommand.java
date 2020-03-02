package net.okocraft.altmanager.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.util.StringUtil;

public final class PreviousNameCommand extends BaseCommand {

    protected PreviousNameCommand() {
        super(
                "altemanager.previousname",
                1,
                false,
                "/altmanager previousname <player>",
                "prevname"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        if (sender instanceof ConsoleCommandSender && args.length == 1) {
            messages.sendNotEnoughArguments(sender);
            return false;
        }

        String player = args.length > 2 ? args[1] : sender.getName();
        if (!database.existPlayer(player)) {
            messages.sendPlayerNotFound(sender, player);
            return false;
        }

        String previousName = database.getPlayerData("previous", player);
        previousName = previousName.isEmpty() ? player : previousName;
        sender.sendMessage(previousName);
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