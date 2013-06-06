package com.katesoft.gserver.api;

public interface Player {
	String id();
	String displayName();
	String email();
	void addPlayerSession(PlayerSession s);
}
