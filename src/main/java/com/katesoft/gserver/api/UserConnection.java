package com.katesoft.gserver.api;

import static com.google.common.util.concurrent.Futures.immediateCheckedFuture;

import java.io.Closeable;
import java.util.UUID;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.katesoft.gserver.commands.Commands.BaseCommand;

public interface UserConnection extends Closeable {
	String id();
	Player asociatePlayer(Player p);
	Player getAssociatedPlayer();
	long socketAcceptTimestamp();
	Future<Void> writeAsync(BaseCommand message);
	void writeSync(BaseCommand message);
	Future<Void> writeAllAsync(BaseCommand message);

	public static class UserConnectionStub implements UserConnection {
		private Logger logger = LoggerFactory.getLogger(getClass());
		private String id = UUID.randomUUID().toString();
		private Player player;

		@Override
		public String id() {
			return id;
		}
		@Override
		public Player asociatePlayer(Player p) {
			this.player = p;
			return player;
		}
		@Override
		public Player getAssociatedPlayer() {
			return player;
		}
		@Override
		public long socketAcceptTimestamp() {
			return System.currentTimeMillis();
		}
		@Override
		public  Future<Void> writeAsync(BaseCommand message) {
			logger.trace("writing reply={} async", message);
			Void v = null;
			return (Future<Void>) immediateCheckedFuture(v);
		}
		@Override
		public void writeSync(BaseCommand message) {
			logger.trace("writing reply={} sync", message);
		}
		@Override
		public Future<Void> writeAllAsync(BaseCommand message) {
			throw new UnsupportedOperationException();
		}
		@Override
		public void close() {}
	}
}
