package com.katesoft.gserver.spi;

import org.apache.commons.chain.Chain;

import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.core.CommandsQualifierCodec;

public interface PlatformContext {
    Chain platformCommandsInterpreter();
    CommandsQualifierCodec commandsCodec();
    GamePlayContext gamePlayContext();
}
