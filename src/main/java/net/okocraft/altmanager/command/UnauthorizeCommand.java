package net.okocraft.altmanager.command;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

public final class UnauthorizeCommand extends BaseCommand {

    protected UnauthorizeCommand() {
        super(
                "altemanager.unauthrize",
                2,
                false,
                "/altmanager unauthrize <player>",
                "unauth"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        String player = args[1];
        if (!database.existPlayer(player)) {
            messages.sendPlayerNotFound(sender, player);
            return false;
        }

        String playerUuid = database.getPlayerData("uuid", player);

        Set<String> authorizedAlts = database.getAuthorizedAlts(playerUuid);
        if (authorizedAlts.isEmpty()) {
            messages.sendNoAuthorizedPlayers(sender, player);
            return true;
        }

        authorizedAlts.forEach(authorizedAltsUuid -> {
            Set<String> authorizedAltsForOthers = database.getAuthorizedAlts(authorizedAltsUuid);
            authorizedAltsForOthers.remove(playerUuid);
            database.setAuthorizedAlts(authorizedAltsUuid, authorizedAltsForOthers);
        });

        database.setAuthorizedAlts(playerUuid, new HashSet<>());
        
        messages.sendAltUnauthorized(sender, player);
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