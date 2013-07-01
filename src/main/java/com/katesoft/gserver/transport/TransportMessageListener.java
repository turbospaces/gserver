package com.katesoft.gserver.transport;

import java.util.EventListener;

import com.google.protobuf.ExtensionRegistry;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.spi.PlatformInterface;

public interface TransportMessageListener extends EventListener {
    void onMessage(BaseCommand cmd, UserConnection userConnection) throws Exception;
    ExtensionRegistry extentionRegistry();
    PlatformInterface getPlatformInterface();
}
