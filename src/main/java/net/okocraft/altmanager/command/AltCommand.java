package net.okocraft.altmanager.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import net.okocraft.altmanager.AltManager;

public class AltCommand extends BaseCommand implements CommandExecutor, TabCompleter {

    private static final AltCommand INSTANCE = new AltCommand();

    protected AltCommand() {
        super("", 0, true, "/altmanager help");
    }

    private enum SubCommands {
        GET_IP(new GetIPCommand()),
        AUTHORIZE(new AuthorizeCommand()),
        UNAUTHRORIZE(new UnauthorizeCommand()),
        AUTHORIZED_LIST(new AuthorizedListCommand()),
        ONLINE_ALTS(new OnlineAltsCommand()),
        PREVIOUS_NAME(new PreviousNameCommand()),
        RELOAD(new ReloadCommand()),
        SEARCH(new SearchCommand()),
        UNIQUE_ACCESS(new UniqueAccessCommand());

        private final BaseCommand subCommand;

        private SubCommands(BaseCommand subCommand) {
            this.subCommand = subCommand;
        }

        public BaseCommand get() {
            return subCommand;
        }

        public static BaseCommand getByName(String name) {
            for (SubCommands subCommand : values()) {
                if (subCommand.get().getName().equalsIgnoreCase(name)) {
                    return subCommand.get();
                }
                if (subCommand.get().getAlias().contains(name.toLowerCase(Locale.ROOT))) {
                    return subCommand.get();
                }
            }

            throw new IllegalArgumentException("There is no command with the name " + name);
        }

        public static List<String> getPermittedCommandNames(CommandSender sender) {
            List<String> result = new ArrayList<>();
            for (SubCommands subCommand : values()) {
                if (subCommand.get().hasPermission(sender)) {
                    result.add(subCommand.get().getName().toLowerCase(Locale.ROOT));
                    result.addAll(subCommand.get().getAlias());
                }
            }
            return result;
        }
    }

    public static void init() {
        PluginCommand pluginCommand = Objects.requireNonNull(AltManager.getInstance().getCommand("altmanager"), "Command is not written in plugin.yml");
        pluginCommand.setExecutor(INSTANCE);
        pluginCommand.setTabCompleter(INSTANCE);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            return this.runCommand(sender, args);
        }

        BaseCommand subCommand;
        try {
            subCommand = SubCommands.getByName(args[0]);
        } catch (IllegalArgumentException e) {
            messages.sendInvalidArgument(sender, args[0]);
            this.runCommand(sender, args);
            return false;
        }

        if (subCommand.isPlayerOnly() && !(sender instanceof Player)) {
            messages.sendPlayerOnly(sender);
            return false;
        }

        if (!subCommand.hasPermission(sender)) {
            messages.sendNoPermission(sender);
            return false;
        }

        if (!subCommand.isValidArgsLength(args.length)) {
            messages.sendNotEnoughArguments(sender);
            messages.sendUsage(sender, subCommand.getUsage());
            return false;
        }

        return subCommand.runCommand(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> permittedCommands = SubCommands.getPermittedCommandNames(sender);
        permittedCommands.add("help");
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], permittedCommands, new ArrayList<>());
        }

        if (args[0].equalsIgnoreCase("help") || !permittedCommands.contains(args[0].toLowerCase(Locale.ROOT))) {
            return List.of();
        }

        return SubCommands.getByName(args[0]).runTabComplete(sender, args);
    }

    /**
     * Show help.
     * 
     * @param sender
     * @param args
     * 
     * @return true
     */
    @Override
    public boolean runCommand(CommandSender sender, String[] args) {
        messages.sendMessage(sender, "command.help.header");
        messages.sendMessage(sender, false, "command.help.format",
                Map.of("%usage%", getUsage(), "%description%", messages.getMessage("command.help.command-description.help")));
        for (SubCommands subCommand : SubCommands.values()) {
            messages.sendMessage(sender, false, "command.help.format",
                    Map.of("%usage%", subCommand.get().getUsage(), "%description%", subCommand.get().getDescription()));
        }
        return true;
    }

    @Override
    public List<String> runTabComplete(CommandSender sender, String[] args) {
        // Not used.
        return null;
    }

    @Override
    public String getName() {
        return "altmanager";
    }
}