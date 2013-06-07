package com.katesoft.gserver.api;

import java.security.SecureRandom;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public interface GamePlayContext {
    Random rng();
    void creditWin(BetWrapper bet);
    
    GamePlayContext MOCK = new GamePlayContext() {
    	private Logger logger = LoggerFactory.getLogger(GamePlayContext.class);
    	private SecureRandom rng = new SecureRandom();
		@Override
		public Random rng() {
			return rng;
		}
		@Override
		public void creditWin(BetWrapper bet) {
			logger.debug("creditWin(:)={}", bet);
		}
	};
}
