package com.katesoft.gserver.core;

import static com.katesoft.gserver.core.Commands.toReply;

import org.apache.commons.chain.Chain;
import org.apache.commons.chain.impl.ChainBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataRetrievalFailureException;

import com.google.protobuf.ExtensionRegistry;
import com.katesoft.gserver.api.DeadConnectionException;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.UnknownCommadException;
import com.katesoft.gserver.spi.PlatformInterface;
import com.katesoft.gserver.transport.TransportMessageListener;

public class MessageDispatcher implements TransportMessageListener {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final PlatformInterface platformInterface;
    private final ExtensionRegistry registry;

    public MessageDispatcher(PlatformInterface platformInterface, ExtensionRegistry registry) {
        this.platformInterface = platformInterface;
        this.registry = registry;
    }
    @Override
    public void onMessage(BaseCommand cmd, UserConnection uc) throws Exception {
        if ( cmd.getDebug() ) {
            logger.debug( "onMessage(connection={})={}", uc.id(), cmd );
        }
        NetworkCommandContext ctx = new NetworkCommandContext( cmd, platformInterface.commandsCodec(), uc );

        Chain chain = cmdExecChain( ctx, uc );
        try {
            boolean processed = chain.execute( ctx );

            if ( !processed ) {
                UnknownCommadException reply = UnknownCommadException.newBuilder().setReq( ctx.getCmd() ).build();
                uc.writeAsync( toReply( ctx.getCmd(), ctx.getCmdCodec(), UnknownCommadException.cmd, reply ) );
            }
        }
        /**
         * handle some common exceptions:
         * 1) unexpected data retrieval (object retrieval)
         * 2) concurrency modification failures
         * 3) dead connection exception.
         */
        catch ( DataRetrievalFailureException ex ) {
            throw ex;
        }
        catch ( ConcurrencyFailureException ex ) {
            throw ex;
        }
        catch ( DeadConnectionException ex ) {
            logger.error( ex.getMessage(), ex );
            logger.error( "closing connection = {} due to failed game login procedure", uc );
            uc.close();
        }
    }
    /**
     * build command interpretation chain - by default use {@link PlatformInterface#platformCommandsInterpreter()} and
     * {@link Player}, i.e. try to interpret command by platform first, otherwise treat command as game specific
     * command.</p>
     * 
     * sub-classes may want to override this method and add custom handlers.
     * 
     * @param ctx - network command execution context.
     * @param uc - user connection.
     * @return interpretation chain.
     */
    protected Chain cmdExecChain(NetworkCommandContext ctx, UserConnection uc) {
        Chain chain = new ChainBase();
        chain.addCommand( platformInterface.platformCommandsInterpreter() );
        if ( uc.getAssociatedPlayer() != null ) {
            chain.addCommand( uc.getAssociatedPlayer() );
        }
        return chain;
    }
    @Override
    public ExtensionRegistry extentionRegistry() {
        return registry;
    }
    @Override
    public PlatformInterface getPlatformInterface() {
        return platformInterface;
    }
}
