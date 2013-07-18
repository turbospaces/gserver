package com.katesoft.gserver.core;

import java.util.HashMap;

import org.apache.commons.chain.Context;

import com.google.common.base.Optional;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.api.AbstractProtocolException;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;

@SuppressWarnings({ "serial", "rawtypes", "unchecked" })
public class NetworkCommandContext extends HashMap implements Context {
    public NetworkCommandContext(BaseCommand cmd, CommandsQualifierCodec codec, UserConnection uc) {
        put( "cmd", cmd );
        put( "codec", codec );
        put( "uc", uc );
    }

    public BaseCommand getCmd() {
        return (BaseCommand) get( "cmd" );
    }
    public CommandsQualifierCodec getCmdCodec() {
        return (CommandsQualifierCodec) get( "codec" );
    }
    public UserConnection getUserConnection() {
        return (UserConnection) get( "uc" );
    }
    public void recognizeCmd() throws Exception {
        Optional<Class<? extends GeneratedMessage>> opt = getCmdCodec().decoder().apply( getCmd() );
        if ( !opt.isPresent() ) {
            throw new AbstractProtocolException.UnknownCommandQualifierException( getCmd().getQualifier() );
        }
    }
}
