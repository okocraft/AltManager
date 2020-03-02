package net.okocraft.altmanager.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import net.okocraft.altmanager.Util;

public final class SearchCommand extends BaseCommand {

    protected SearchCommand() {
        super(
                "altemanager.search",
                2,
                false,
                "/altmanager search <player> [true | false]"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        String player = args[1];
        if (!database.existPlayer(player)) {
            messages.sendPlayerNotFound(sender, player);
            return false;
        }

        boolean includeAuthrized = false;
        if (args.length >= 3) {
            try {
                includeAuthrized = Boolean.valueOf(args[2]);
            } catch (IllegalArgumentException e) {
                messages.sendInvalidArgument(sender, args[2]);
                return false;
            }
        }

        String address = database.getPlayerData("address", player);
        Collection<String> alts = database.getAlts(address).values();
        alts.remove(Util.toUniqueId(player));
        if (!includeAuthrized) {
            Set<String> authorizedAlts = database.getAuthorizedAlts(player);
            alts.removeIf(authorizedAlts::contains);
        }

        StringBuilder sb = new StringBuilder("");
        alts.forEach(playerUniqueId -> sb.append(Util.toPlayerName(playerUniqueId)).append(", "));
        String altsString = sb.length() > 1 ? sb.substring(0, sb.length() - 2).toString() : "";
        
        messages.sendAlts(sender, player, altsString);
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], database.getPlayersMap().values(), new ArrayList<>());
        }

        if (args.length == 3) {
            return StringUtil.copyPartialMatches(args[2], List.of("true", "false"), new ArrayList<>());
        }
        return List.of();
    }
} 