package com.github.asm0dey.cligen.runtime;

import java.util.*;

/**
 * Dispatches command-line arguments to appropriate CommandParser implementations
 */
public class CommandDispatcher {
    private final Map<String, CommandParser<?>> commands = new HashMap<>();
    private final String appName;
    private final String appVersion;

    public CommandDispatcher(String appName, String appVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    /**
     * Register a command parser for a command name
     */
    public <T> void register(String commandName, CommandParser<T> parser) {
        commands.put(commandName, parser);
    }

    /**
     * Parse arguments and return the result
     * First argument is the command name
     */
    public ParseResult<?> dispatch(String[] args) throws ParseException {
        if (args.length == 0) {
            throw new ParseException("No command specified. Use --help for available commands.");
        }

        String commandName = args[0];

        // Handle global help
        if ("--help".equals(commandName) || "-h".equals(commandName) || "help".equals(commandName)) {
            System.out.println(getGlobalHelp());
            return null;  // Signal that help was shown
        }

        // Handle version
        if ("--version".equals(commandName) || "-v".equals(commandName) || "version".equals(commandName)) {
            System.out.println(appName + " version " + appVersion);
            return null;
        }

        // Find the command parser
        CommandParser<?> parser = commands.get(commandName);
        if (parser == null) {
            throw new ParseException("Unknown command: " + commandName + ". Use --help for available commands.");
        }

        // Parse remaining arguments (skip command name)
        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
        return parser.parse(remainingArgs);
    }

    /**
     * Get help for a specific command
     */
    public String getCommandHelp(String commandName) throws ParseException {
        CommandParser<?> parser = commands.get(commandName);
        if (parser == null) {
            throw new ParseException("Unknown command: " + commandName);
        }
        return parser.getHelpText();
    }

    /**
     * Get global help text showing all commands
     */
    public String getGlobalHelp() {
        StringBuilder help = new StringBuilder();
        help.append(appName).append(" - CLI Application\n");
        help.append("Version: ").append(appVersion).append("\n\n");
        help.append("Usage: ").append(appName.toLowerCase()).append(" <command> [options]\n\n");
        help.append("Commands:\n");

        for (String commandName : commands.keySet()) {
            help.append("  ").append(commandName).append("\n");
        }

        help.append("\nGlobal Options:\n");
        help.append("  --help, -h     Show this help message\n");
        help.append("  --version      Show version information\n\n");
        help.append("Use '").append(appName.toLowerCase()).append(" <command> --help' for command-specific help\n");

        return help.toString();
    }

    /**
     * Get list of all registered commands
     */
    public Set<String> getCommandNames() {
        return commands.keySet();
    }
}
