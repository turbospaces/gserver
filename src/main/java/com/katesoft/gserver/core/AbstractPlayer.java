package com.katesoft.gserver.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.find;
import static java.lang.String.format;

import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.katesoft.gserver.api.CommandWrapperEvent;
import com.katesoft.gserver.api.Game;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.UnknownCommadException;

public abstract class AbstractPlayer implements Player {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Set<PlayerSession> sessions = Sets.newHashSet();
	protected String id, displayName, email;

	public AbstractPlayer(String id, String email) {
		this.id = id;
		this.email = email;
	}
	protected AbstractPlayer() {
	}
	@Override
	public String id() {
		return id;
	}
	@Override
	public String displayName() {
		return displayName;
	}
	@Override
	public String email() {
		return email;
	}
	@Override
	public synchronized boolean addPlayerSession(PlayerSession s) {
		return sessions.add(Preconditions.checkNotNull(s));
	}
	@Override
    public synchronized void closePlayerSession(final String sessionId) {
        PlayerSession session = find(sessions, new Predicate<PlayerSession>() {
            @Override
            public boolean apply(@Nullable PlayerSession input) {
                return input.id().equals(sessionId);
            }
        });
        session.close();
        sessions.remove(session);
    }
    @Override
	public void close() {
		for (PlayerSession s : sessions) {
			try {
				s.close();
			} catch (Throwable t) {
				logger.error(t.getMessage(), t);
			}
		}
	}
	@Override
	public boolean dispatchCommand(BaseCommand cmd, CommandsQualifierCodec codec, GamePlayContext ctx) {
		final String sessionId = checkNotNull(cmd.getSessionId(), "got command=%s detached from session", cmd);
		PlayerSession session = find(sessions, new Predicate<PlayerSession>() {
			@Override
			public boolean apply(PlayerSession input) {
				return input.id().equals(sessionId);
			}
		});
		Game g = session.getAssociatedGame();
		for (;;) {
			try {
				CommandWrapperEvent e = new CommandWrapperEvent(cmd, codec, session);
				g.commandsInterpreter().interpretCommand(e);
				boolean acknowledged = e.isAcknowledged();
				if (!acknowledged) {
					UnknownCommadException reply = UnknownCommadException.newBuilder().setGame(g.getClass().getSimpleName()).setReq(cmd).build();
					session.getAssociatedUserConnection().writeAsync( Commands.toReply(cmd, codec, UnknownCommadException.cmd, reply));
				}
				return acknowledged;
			} catch (Throwable t) {
				logger.error(
						format("Unable to interpet game specific command %s:%s",
								g.getClass().getSimpleName(), cmd.getClass().getSimpleName()), t);
				Throwables.propagate(t);
			}
		}
	}
}
