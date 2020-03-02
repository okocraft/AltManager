package net.okocraft.altmanager.config;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Messages extends CustomConfig {
    
    private static final Messages INSTANCE = new Messages();
    
    Messages() {
        super("messages.yml");
    }

    public static Messages getInstance() {
        return INSTANCE;
    }

    /**
     * Send message to player.
     * 
     * @param player
     * @param addPrefix
     * @param path
     * @param placeholders
     */
    public void sendMessage(CommandSender sender, boolean addPrefix, String path, Map<String, Object> placeholders) {
        String prefix = addPrefix ? get().getString("plugin.prefix", "&8[&6AltManager&8]&r") + " " : "";
        String message = prefix + getMessage(path);
        for (Map.Entry<String, Object> placeholder : placeholders.entrySet()) {
            message = message.replace(placeholder.getKey(), placeholder.getValue().toString());
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        return;
    }

    /**
     * Send message to player.
     * 
     * @param player
     * @param path
     * @param placeholders
     */
    public void sendMessage(CommandSender sender, String path, Map<String, Object> placeholders) {
        sendMessage(sender, true, path, placeholders);
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param path
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, Map.of());
    }

    /**
     * Send message to player.
     * 
     * @param sender
     * @param addPrefix
     * @param path
     */
    public void sendMessage(CommandSender sender, boolean addPrefix, String path) {
        sendMessage(sender, addPrefix, path, Map.of());
    }

    /**
     * Gets message from key. Returned messages will not translated its color code.
     * 
     * @param path
     * @return
     */
    public String getMessage(String path) {
        return get().getString(path, path);
    }
    
    public void sendNoPermission(CommandSender sender) {
        sendMessage(sender, "command.general.no-permission");
    }

    public void sendInvalidArgument(CommandSender sender, String argument) {
        sendMessage(sender, "command.general.invalid-argument", Map.of("%argument%", argument));
    }

    public void sendNotEnoughArguments(CommandSender sender) {
        sendMessage(sender, "command.general.not-enough-arguments");
    }

    public void sendRenameNotification(CommandSender sender, String oldName, String newName) {
        sendMessage(sender, "other.rename-notification", Map.of("%old-name%", oldName, "%new-name%", newName));
    }

    public void sendPlayerNotFound(CommandSender sender, String player) {
        sendMessage(sender, "command.general.player-not-found", Map.of("%player%", player));
    }

    public void sendPlayerOnly(CommandSender sender) {
        sendMessage(sender, "command.general.player-only");
    }

    public void sendCommandDoesNotExist(CommandSender sender, String command) {
        sendMessage(sender, "command.general.command-does-not-exist", Map.of("%command%", command));
    }

    public void sendPreviousName(CommandSender sender, String player, String oldName) {
        sendMessage(sender, "other.previous-notification", Map.of("%player%", player, "%old-name%", oldName));
    }

    public void sendAlts(CommandSender sender, String player, String altsStr) {
        sendMessage(sender, "other.alts-format", Map.of("%player%", player, "%alts%", altsStr));
    }
    
    public void sendAltsAuthorized(CommandSender sender, String firstPlayer, String secondPlayer) {
        sendMessage(sender, "command.authorize.alt-authorized", Map.of("%player-1%", firstPlayer, "%player-2%", secondPlayer));
    }
    
    public void sendAltUnauthorized(CommandSender sender, String player) {
        sendMessage(sender, "command.unauthorize.alt-unauthorized", Map.of("%player%", player));
    }

    public void sendNotSameIP(CommandSender sender, String firstPlayer, String secondPlayer) {
        sendMessage(sender, "command.authorize.not-same-ip", Map.of("%player-1%", firstPlayer, "%player-2%", secondPlayer));
    }
    
    public void sendUsage(CommandSender sender, String usage) {
        sendMessage(sender, "command.general.usage-format", Map.of("%usage%", usage));
    }

    public void sendNoAuthorizedPlayers(CommandSender sender, String player) {
        sendMessage(sender, "command.unauthorize.no-authorized-players", Map.of("%player%", player));
    }
    
    public void sendReloadSuccess(CommandSender sender) {
        sendMessage(sender, "command.reload.success");
    }
}