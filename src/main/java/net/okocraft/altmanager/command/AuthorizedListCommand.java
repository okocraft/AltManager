package net.okocraft.altmanager.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import net.okocraft.altmanager.Util;

public final class AuthorizedListCommand extends BaseCommand {

    protected AuthorizedListCommand() {
        super(
                "altemanager.authorizedlist",
                2,
                false,
                "/altmanager authorizedlist <player>",
                "authlist"
        );
    }

    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        String player = args[1];
        if (!database.existPlayer(player)) {
            messages.sendPlayerNotFound(sender, player);
            return false;
        }

        Set<String> authorizedAlts = database.getAuthorizedAlts(player);
        if (authorizedAlts.isEmpty()) {
            sender.sendMessage("");
            return true;
        }
        StringBuilder sb = new StringBuilder();
        authorizedAlts.forEach(authorizedAltUuid -> {
            String authorizedAlt = Util.toPlayerName(authorizedAltUuid);
            if (!authorizedAlt.isEmpty()) {
                sb.append(ChatColor.AQUA + authorizedAlt + ChatColor.GRAY + ", ");
            }
        });
        sender.sendMessage(sb.substring(0, sb.length() - 2));
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