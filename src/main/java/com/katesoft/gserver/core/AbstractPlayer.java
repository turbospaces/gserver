package com.katesoft.gserver.core;

import java.util.Set;

import com.google.common.collect.Sets;
import com.katesoft.gserver.api.CommandWrapperEvent;
import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.commands.Commands.BaseCommand;

public class AbstractPlayer implements Player {
	private Set<PlayerSession> sessions = Sets.newHashSet();
	private String id, displayName, email;

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
	public void addPlayerSession(PlayerSession s) {
	}
	@Override
	public void close() {
	}
	@Override
	public void dispatchCommand(CommandWrapperEvent cmd) {
	}
}
