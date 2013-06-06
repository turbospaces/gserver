package com.katesoft.gserver.core;

import com.google.protobuf.ExtensionRegistry;
import com.katesoft.gserver.games.roullete.RoulleteCommands;

public abstract class Commands {
    public static ExtensionRegistry newMessageRegistry() {
        final ExtensionRegistry registry = ExtensionRegistry.newInstance();
        com.katesoft.gserver.commands.Commands.registerAllExtensions(registry);
        RoulleteCommands.registerAllExtensions(registry);
        return registry;
    }
    private Commands() {}
}
