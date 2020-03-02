package net.okocraft.altmanager.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public final class AuthorizeCommand extends BaseCommand {

    protected AuthorizeCommand() {
        super(
                "altemanager.authorize",
                3,
                false,
                "/altmanager authorize <player-1> <player-2>",
                "auth"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        String firstPlayer = args[1];
        String secondPlayer = args[2];

        if (!database.existPlayer(firstPlayer)) {
            messages.sendPlayerNotFound(sender, firstPlayer);
            return false;
        }
        if (!database.existPlayer(secondPlayer)) {
            messages.sendPlayerNotFound(sender, secondPlayer);
            return false;
        }
        if (firstPlayer.equalsIgnoreCase(secondPlayer)) {
            messages.sendInvalidArgument(sender, secondPlayer);
            return false;
        }

        String firstPlayerUuid = database.getPlayerData("uuid", firstPlayer);
        String secondPlayerUuid = database.getPlayerData("uuid", secondPlayer);

        if (!database.getPlayerData("address", firstPlayerUuid).equals(database.getPlayerData("address", secondPlayerUuid))) {
            messages.sendNotSameIP(sender, firstPlayer, secondPlayer);
            return false;
        }

        Set<String> authorizedAlts = database.getAuthorizedAlts(firstPlayerUuid);
        authorizedAlts.addAll(database.getAuthorizedAlts(secondPlayerUuid));

        authorizedAlts.add(secondPlayerUuid);
        authorizedAlts.remove(firstPlayerUuid);
        database.setAuthorizedAlts(firstPlayerUuid, authorizedAlts);

        authorizedAlts.add(firstPlayerUuid);
        authorizedAlts.remove(secondPlayerUuid);
        database.setAuthorizedAlts(secondPlayerUuid, authorizedAlts);

        messages.sendAltsAuthorized(sender, firstPlayerUuid, secondPlayerUuid);
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2 || args.length == 3) {
            return StringUtil.copyPartialMatches(args[args.length - 1], database.getPlayersMap().values(), new ArrayList<>());
        }
        return List.of();
    }
} 