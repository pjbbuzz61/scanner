package scanner.scanner.util;

import java.util.Arrays;
import java.util.List;

public enum Period {
	NONE,
	GAME,              // For all Sports
	GAME_NO_OT,        // Just NHL so far
	FIRST_HALF,        //                         NHL: 1st period, NBA: 1st half, NFL: 1st half
	SECOND_HALF,       //                         NHL: 2nd period, NBA: 2nd half, NFL: 2nd half
	SECOND_HALF_NO_OT, 
	FIRST_PERIOD,      // MLB: 1st inning,        NHL: 3rd period, NBA: 1st qtr,  NFL: 1st qtr
	SECOND_PERIOD,     // MLB: 2nd inning,        NHL: OT,         NBA: 2nd qtr,  NFL: 2nd qtr
	THIRD_PERIOD,      // MLB: 3rd inning,        NHL: Shootout,   NBA: 3rd qtr,  NFL: 3rd qtr
	FOURTH_PERIOD,     // MLB: 4th inning,                         NBA: 4th qtr,  NFL: 4th qtr
	FIFTH_PERIOD,      // MLB: 5th inning
	SIXTH_PERIOD,      // MLB: 6th inning
	SEVENTH_PERIOD,    // MLB: 7th inning
	EIGHTH_PERIOD,     // MLB: 8th inning
	NINTH_PERIOD,      // MLB: 9th inning
	INNING1_7,         // MLB: 1-7 inning
	INNING1_4_5,       // MLB: First 4.5 innings
	INNING4_5_9,       // MLB: Last 4.5 innings
	INNING1_5,         // MLB: First 5 innings
	INNING5_9,
	OVERTIME,
	SHOOTOUT, 
	MATCH,
	SET1,
	SET2,
	SET3,
	SET4,
	SET5;


	public static List<Period> popularPeriods() {
		return Arrays.asList(Period.GAME, Period.FIRST_HALF, Period.FIRST_PERIOD);
	}
}
