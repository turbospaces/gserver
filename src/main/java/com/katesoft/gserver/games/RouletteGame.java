package com.katesoft.gserver.games;

import static com.google.common.base.Objects.toStringHelper;
import static com.google.common.primitives.Ints.asList;
import static com.katesoft.gserver.core.CommandsBuilder.toReply;
import static com.katesoft.gserver.games.RouletteGame.PositionAndPayout.of;
import static com.katesoft.gserver.games.roulette.RoulleteCommands.RouletteBetPosition.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.api.GameCommand;
import com.katesoft.gserver.api.GameCommandInterpreter;
import com.katesoft.gserver.core.AbstractGame;
import com.katesoft.gserver.games.roulette.RoulleteCommands.GetRoulettePositionInfoCommand;
import com.katesoft.gserver.games.roulette.RoulleteCommands.GetRoulettePositionInfoReply;
import com.katesoft.gserver.games.roulette.RoulleteCommands.GetRoulettePositionInfoReply.Builder;
import com.katesoft.gserver.games.roulette.RoulleteCommands.RouletteBetPosition;
import com.katesoft.gserver.games.roulette.RoulleteCommands.RoulettePositionInfo;
import com.katesoft.gserver.games.roulette.RoulleteCommands.RouletteSpinCommand;
import com.katesoft.gserver.games.roulette.RoulleteCommands.RouletteSpinReply;

public class RouletteGame extends AbstractGame {
    static final Map<RouletteBetPosition, PositionAndPayout> ALL = Maps.newLinkedHashMap();
    static final Map<Integer, Set<PositionAndPayout>> NUMS = Maps.newHashMap();
    static GetRoulettePositionInfoReply POSITION_INFO_REPLY;

    public RouletteGame() {
        interpreter = new GameCommandInterpreter() {
            @Override
            public void interpretCommand(final GameCommand e) throws Exception {
                e.interpretIfPossible( GetRoulettePositionInfoCommand.class, new Runnable() {
                    @Override
                    public void run() {
                        GetRoulettePositionInfoCommand cmd = e.getCmd().getExtension( GetRoulettePositionInfoCommand.cmd );
                        List<RouletteBetPosition> positionsList = cmd.getPositionsList();
                        if ( positionsList.isEmpty() ) {
                            e.replyAsync( toReply( e, GetRoulettePositionInfoReply.cmd, POSITION_INFO_REPLY ) );
                        }
                        else {
                            Builder b = GetRoulettePositionInfoReply.newBuilder();
                            List<RoulettePositionInfo> list = POSITION_INFO_REPLY.getPositionsList();
                            for ( RoulettePositionInfo i : list ) {
                                b.addPositions( i );
                            }
                            e.replyAsync( toReply( e, GetRoulettePositionInfoReply.cmd, b.build() ) );
                        }
                    }
                } );
                e.interpretIfPossible( RouletteSpinCommand.class, new Runnable() {
                    @Override
                    public void run() {
                        RouletteSpinCommand cmd = e.getCmd().getExtension( RouletteSpinCommand.cmd );

                        PositionAndPayout position = ALL.get( cmd.getPosition() );
                        int number = gamePlayContext.rng().nextInt( 37 ) - 1;
                        Set<PositionAndPayout> positions = NUMS.get( number );
                        boolean win = positions.contains( position );

                        BetWrapper bet = new BetWrapper( cmd.getBet(), win );
                        gamePlayContext.creditWin( bet );
                        RouletteSpinReply spinReply = RouletteSpinReply
                                .newBuilder()
                                .setBetResult( bet.toBetResult() )
                                .setPosition( position.getPosition() )
                                .build();
                        e.replyAsync( toReply( e, RouletteSpinReply.cmd, spinReply ) );
                    }
                } );
            }
        };
    }

    public static final class PositionAndPayout {
        private int payout;
        private ImmutableCollection<Integer> numbers;
        private RouletteBetPosition position;

        public static PositionAndPayout of(int payout, List<Integer> numbers, RouletteBetPosition position) {
            PositionAndPayout p = new PositionAndPayout();

            p.payout = payout;
            p.numbers = ImmutableList.copyOf( numbers );
            p.position = position;

            return p;
        }
        public int getPayout() {
            return payout;
        }
        public RouletteBetPosition getPosition() {
            return position;
        }
        @Override
        public String toString() {
            return toStringHelper( this ).add( "position", position ).add( "payout", payout ).add( "numbers", numbers ).toString();
        }
    }

    static {
        ALL.put( none, of( 0, asList(), none ) );
        ALL.put( number_00, of( 35, asList( -1 ), number_00 ) );
        ALL.put( number_0, of( 35, asList( 0 ), number_0 ) );
        ALL.put( number_1, of( 35, asList( 1 ), number_1 ) );
        ALL.put( number_2, of( 35, asList( 2 ), number_2 ) );
        ALL.put( number_3, of( 35, asList( 3 ), number_3 ) );
        ALL.put( number_4, of( 35, asList( 4 ), number_4 ) );
        ALL.put( number_5, of( 35, asList( 5 ), number_5 ) );
        ALL.put( number_6, of( 35, asList( 6 ), number_6 ) );
        ALL.put( number_7, of( 35, asList( 7 ), number_7 ) );
        ALL.put( number_8, of( 35, asList( 8 ), number_8 ) );
        ALL.put( number_9, of( 35, asList( 9 ), number_9 ) );
        ALL.put( number_10, of( 35, asList( 10 ), number_10 ) );
        ALL.put( number_11, of( 35, asList( 11 ), number_11 ) );
        ALL.put( number_12, of( 35, asList( 12 ), number_12 ) );
        ALL.put( number_13, of( 35, asList( 13 ), number_13 ) );
        ALL.put( number_14, of( 35, asList( 14 ), number_14 ) );
        ALL.put( number_15, of( 35, asList( 15 ), number_15 ) );
        ALL.put( number_16, of( 35, asList( 16 ), number_16 ) );
        ALL.put( number_17, of( 35, asList( 17 ), number_17 ) );
        ALL.put( number_18, of( 35, asList( 18 ), number_18 ) );
        ALL.put( number_19, of( 35, asList( 19 ), number_19 ) );
        ALL.put( number_20, of( 35, asList( 20 ), number_20 ) );
        ALL.put( number_21, of( 35, asList( 21 ), number_21 ) );
        ALL.put( number_22, of( 35, asList( 22 ), number_22 ) );
        ALL.put( number_23, of( 35, asList( 23 ), number_23 ) );
        ALL.put( number_24, of( 35, asList( 24 ), number_24 ) );
        ALL.put( number_25, of( 35, asList( 25 ), number_25 ) );
        ALL.put( number_26, of( 35, asList( 26 ), number_26 ) );
        ALL.put( number_27, of( 35, asList( 27 ), number_27 ) );
        ALL.put( number_28, of( 35, asList( 28 ), number_28 ) );
        ALL.put( number_29, of( 35, asList( 29 ), number_29 ) );
        ALL.put( number_30, of( 35, asList( 30 ), number_30 ) );
        ALL.put( number_31, of( 35, asList( 31 ), number_31 ) );
        ALL.put( number_32, of( 35, asList( 32 ), number_32 ) );
        ALL.put( number_33, of( 35, asList( 33 ), number_33 ) );
        ALL.put( number_34, of( 35, asList( 34 ), number_34 ) );
        ALL.put( number_35, of( 35, asList( 35 ), number_35 ) );
        ALL.put( number_36, of( 35, asList( 36 ), number_36 ) );
        ALL.put( even, of( 1, asList( 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30, 32, 34, 36 ), even ) );
        ALL.put( odd, of( 1, asList( 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29, 31, 33, 35 ), odd ) );
        ALL.put( red, of( 1, asList( 1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36 ), red ) );
        ALL.put( black, of( 1, asList( 2, 4, 6, 8, 10, 11, 13, 15, 17, 20, 22, 24, 26, 28, 29, 31, 33, 35 ), black ) );
        ALL.put( range_1to18, of( 1, asList( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18 ), range_1to18 ) );
        ALL.put( range_19to36, of( 1, asList( 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36 ), range_19to36 ) );
        ALL.put( dozen_1to12, of( 2, asList( 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 ), dozen_1to12 ) );
        ALL.put( dozen_13to24, of( 2, asList( 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24 ), dozen_13to24 ) );
        ALL.put( dozen_25to36, of( 2, asList( 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36 ), dozen_25to36 ) );
        ALL.put( column_1, of( 2, asList( 1, 4, 7, 10, 13, 16, 19, 22, 25, 28, 31, 34 ), column_1 ) );
        ALL.put( column_2, of( 2, asList( 2, 5, 8, 11, 14, 17, 20, 23, 26, 29, 32, 35 ), column_2 ) );
        ALL.put( column_3, of( 2, asList( 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36 ), column_3 ) );
        ALL.put( sixline_1, of( 5, asList( 1, 2, 3, 4, 5, 6 ), sixline_1 ) );
        ALL.put( sixline_2, of( 5, asList( 4, 5, 6, 7, 8, 9 ), sixline_2 ) );
        ALL.put( sixline_3, of( 5, asList( 7, 8, 9, 10, 11, 12 ), sixline_3 ) );
        ALL.put( sixline_4, of( 5, asList( 10, 11, 12, 13, 14, 15 ), sixline_4 ) );
        ALL.put( sixline_5, of( 5, asList( 13, 14, 15, 16, 17, 18 ), sixline_5 ) );
        ALL.put( sixline_6, of( 5, asList( 16, 17, 18, 19, 20, 21 ), sixline_6 ) );
        ALL.put( sixline_7, of( 5, asList( 19, 20, 21, 22, 23, 24 ), sixline_7 ) );
        ALL.put( sixline_8, of( 5, asList( 22, 23, 24, 25, 26, 27 ), sixline_8 ) );
        ALL.put( sixline_9, of( 5, asList( 25, 26, 27, 28, 29, 30 ), sixline_9 ) );
        ALL.put( sixline_10, of( 5, asList( 28, 29, 30, 31, 32, 33 ), sixline_10 ) );
        ALL.put( sixline_11, of( 5, asList( 31, 32, 33, 34, 35, 36 ), sixline_11 ) );
        ALL.put( topline, of( 6, asList( -1, 0, 1, 2, 3 ), topline ) );
        ALL.put( corner_1, of( 8, asList( 1, 2, 4, 5 ), corner_1 ) );
        ALL.put( corner_2, of( 8, asList( 2, 3, 5, 6 ), corner_2 ) );
        ALL.put( corner_4, of( 8, asList( 4, 5, 7, 8 ), corner_4 ) );
        ALL.put( corner_5, of( 8, asList( 5, 6, 8, 9 ), corner_5 ) );
        ALL.put( corner_7, of( 8, asList( 7, 8, 10, 11 ), corner_7 ) );
        ALL.put( corner_8, of( 8, asList( 8, 9, 11, 12 ), corner_8 ) );
        ALL.put( corner_10, of( 8, asList( 10, 11, 13, 14 ), corner_10 ) );
        ALL.put( corner_11, of( 8, asList( 11, 12, 14, 15 ), corner_11 ) );
        ALL.put( corner_13, of( 8, asList( 13, 14, 16, 17 ), corner_13 ) );
        ALL.put( corner_14, of( 8, asList( 14, 15, 17, 18 ), corner_14 ) );
        ALL.put( corner_16, of( 8, asList( 16, 17, 19, 20 ), corner_16 ) );
        ALL.put( corner_17, of( 8, asList( 17, 18, 20, 21 ), corner_17 ) );
        ALL.put( corner_18, of( 8, asList( 19, 20, 22, 23 ), corner_18 ) );
        ALL.put( corner_19, of( 8, asList( 20, 21, 23, 24 ), corner_19 ) );
        ALL.put( corner_20, of( 8, asList( 22, 23, 25, 26 ), corner_20 ) );
        ALL.put( corner_23, of( 8, asList( 23, 24, 26, 27 ), corner_23 ) );
        ALL.put( corner_25, of( 8, asList( 25, 26, 28, 29 ), corner_25 ) );
        ALL.put( corner_26, of( 8, asList( 26, 27, 29, 30 ), corner_26 ) );
        ALL.put( corner_28, of( 8, asList( 28, 29, 31, 32 ), corner_28 ) );
        ALL.put( corner_29, of( 8, asList( 29, 30, 32, 33 ), corner_29 ) );
        ALL.put( corner_31, of( 8, asList( 31, 32, 34, 35 ), corner_31 ) );
        ALL.put( corner_32, of( 8, asList( 32, 33, 35, 36 ), corner_32 ) );
        ALL.put( street_1, of( 11, asList( 1, 2, 3 ), street_1 ) );
        ALL.put( street_4, of( 11, asList( 4, 5, 6 ), street_4 ) );
        ALL.put( street_7, of( 11, asList( 7, 8, 9 ), street_7 ) );
        ALL.put( street_10, of( 11, asList( 10, 11, 12 ), street_10 ) );
        ALL.put( street_13, of( 11, asList( 13, 14, 15 ), street_13 ) );
        ALL.put( street_16, of( 11, asList( 16, 17, 18 ), street_16 ) );
        ALL.put( street_19, of( 11, asList( 19, 20, 21 ), street_19 ) );
        ALL.put( street_22, of( 11, asList( 22, 23, 24 ), street_22 ) );
        ALL.put( street_25, of( 11, asList( 25, 26, 27 ), street_25 ) );
        ALL.put( street_28, of( 11, asList( 28, 29, 30 ), street_28 ) );
        ALL.put( street_31, of( 11, asList( 31, 32, 33 ), street_31 ) );
        ALL.put( street_34, of( 11, asList( 34, 35, 36 ), street_34 ) );
        ALL.put( basket_1, of( 11, asList( 0, 1, 2 ), basket_1 ) );
        ALL.put( basket_2, of( 11, asList( -1, 2, 3 ), basket_2 ) );
        ALL.put( basket_3, of( 11, asList( -1, 0, 2 ), basket_3 ) );
        ALL.put( row_00, of( 18, asList( -1, 0 ), row_00 ) );
        ALL.put( split_1_2, of( 17, asList( 1, 2 ), split_1_2 ) );
        ALL.put( split_1_4, of( 17, asList( 1, 4 ), split_1_4 ) );
        ALL.put( split_2_3, of( 17, asList( 2, 3 ), split_2_3 ) );
        ALL.put( split_2_5, of( 17, asList( 2, 5 ), split_2_5 ) );
        ALL.put( split_3_6, of( 17, asList( 3, 6 ), split_3_6 ) );
        ALL.put( split_4_5, of( 17, asList( 4, 5 ), split_4_5 ) );
        ALL.put( split_4_7, of( 17, asList( 4, 7 ), split_4_7 ) );
        ALL.put( split_5_6, of( 17, asList( 5, 6 ), split_5_6 ) );
        ALL.put( split_5_8, of( 17, asList( 5, 8 ), split_5_8 ) );
        ALL.put( split_6_9, of( 17, asList( 6, 9 ), split_6_9 ) );
        ALL.put( split_7_8, of( 17, asList( 7, 8 ), split_7_8 ) );
        ALL.put( split_7_10, of( 17, asList( 7, 10 ), split_7_10 ) );
        ALL.put( split_8_9, of( 17, asList( 8, 9 ), split_8_9 ) );
        ALL.put( split_8_11, of( 17, asList( 8, 11 ), split_8_11 ) );
        ALL.put( split_9_12, of( 17, asList( 9, 12 ), split_9_12 ) );
        ALL.put( split_10_11, of( 17, asList( 10, 11 ), split_10_11 ) );
        ALL.put( split_10_13, of( 17, asList( 10, 13 ), split_10_13 ) );
        ALL.put( split_11_12, of( 17, asList( 11, 12 ), split_11_12 ) );
        ALL.put( split_11_14, of( 17, asList( 11, 14 ), split_11_14 ) );
        ALL.put( split_12_15, of( 17, asList( 12, 15 ), split_12_15 ) );
        ALL.put( split_13_14, of( 17, asList( 13, 14 ), split_13_14 ) );
        ALL.put( split_13_16, of( 17, asList( 13, 16 ), split_13_16 ) );
        ALL.put( split_14_15, of( 17, asList( 14, 15 ), split_14_15 ) );
        ALL.put( split_14_17, of( 17, asList( 14, 17 ), split_14_17 ) );
        ALL.put( split_15_18, of( 17, asList( 15, 18 ), split_15_18 ) );
        ALL.put( split_16_17, of( 17, asList( 16, 17 ), split_16_17 ) );
        ALL.put( split_16_19, of( 17, asList( 16, 19 ), split_16_19 ) );
        ALL.put( split_17_18, of( 17, asList( 17, 18 ), split_17_18 ) );
        ALL.put( split_17_20, of( 17, asList( 17, 20 ), split_17_20 ) );
        ALL.put( split_18_21, of( 17, asList( 18, 21 ), split_18_21 ) );
        ALL.put( split_19_20, of( 17, asList( 19, 20 ), split_19_20 ) );
        ALL.put( split_19_22, of( 17, asList( 19, 22 ), split_19_22 ) );
        ALL.put( split_20_21, of( 17, asList( 20, 21 ), split_20_21 ) );
        ALL.put( split_20_23, of( 17, asList( 20, 23 ), split_20_23 ) );
        ALL.put( split_21_24, of( 17, asList( 21, 24 ), split_21_24 ) );
        ALL.put( split_22_23, of( 17, asList( 22, 23 ), split_22_23 ) );
        ALL.put( split_22_25, of( 17, asList( 22, 25 ), split_22_25 ) );
        ALL.put( split_23_24, of( 17, asList( 23, 24 ), split_23_24 ) );
        ALL.put( split_23_26, of( 17, asList( 23, 26 ), split_23_26 ) );
        ALL.put( split_24_27, of( 17, asList( 24, 27 ), split_24_27 ) );
        ALL.put( split_25_26, of( 17, asList( 25, 26 ), split_25_26 ) );
        ALL.put( split_25_28, of( 17, asList( 25, 28 ), split_25_28 ) );
        ALL.put( split_26_27, of( 17, asList( 26, 27 ), split_26_27 ) );
        ALL.put( split_26_29, of( 17, asList( 26, 29 ), split_26_29 ) );
        ALL.put( split_27_30, of( 17, asList( 27, 30 ), split_27_30 ) );
        ALL.put( split_28_29, of( 17, asList( 28, 29 ), split_28_29 ) );
        ALL.put( split_28_31, of( 17, asList( 28, 31 ), split_28_31 ) );
        ALL.put( split_29_30, of( 17, asList( 29, 30 ), split_29_30 ) );
        ALL.put( split_29_32, of( 17, asList( 29, 32 ), split_29_32 ) );
        ALL.put( split_30_33, of( 17, asList( 30, 33 ), split_30_33 ) );
        ALL.put( split_31_32, of( 17, asList( 31, 32 ), split_31_32 ) );
        ALL.put( split_31_34, of( 17, asList( 31, 34 ), split_31_34 ) );
        ALL.put( split_32_33, of( 17, asList( 32, 33 ), split_32_33 ) );
        ALL.put( split_32_35, of( 17, asList( 32, 35 ), split_32_35 ) );
        ALL.put( split_33_36, of( 17, asList( 33, 36 ), split_33_36 ) );
        ALL.put( split_34_35, of( 17, asList( 34, 35 ), split_34_35 ) );
        ALL.put( split_35_36, of( 17, asList( 35, 36 ), split_35_36 ) );

        for ( int i = -1; i <= 36; i++ ) {
            NUMS.put( i, possiblePositionsFor( i ) );
        }

        Builder positionInfoBuilder = GetRoulettePositionInfoReply.newBuilder();
        for ( PositionAndPayout pp : ALL.values() ) {
            positionInfoBuilder.addPositions( RoulettePositionInfo
                    .newBuilder()
                    .setName( pp.position.name() )
                    .setPayout( pp.payout )
                    .addAllNumbers( pp.numbers )
                    .build() );
        }
        POSITION_INFO_REPLY = positionInfoBuilder.build();
    }

    private static Set<PositionAndPayout> possiblePositionsFor(int number) {
        Set<PositionAndPayout> result = Sets.newHashSet();
        for ( RouletteBetPosition next : RouletteBetPosition.values() ) {
            PositionAndPayout positionPayout = ALL.get( next );
            if ( positionPayout.numbers.contains( number ) ) {
                result.add( positionPayout );
            }
        }
        return result;
    }
}
