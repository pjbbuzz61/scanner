package scanner.scanner.util;

import java.util.Arrays;
import java.util.List;

public enum Action {

	OVER,
	UNDER,
	OVER_3WAY,
	UNDER_3WAY,
	OU_PUSH_3WAY,
	
	HOME_OVER,
	HOME_UNDER,
	HOME_OVER_3WAY,
	HOME_UNDER_3WAY,
	HOME_OU_PUSH_3WAY,
	
	AWAY_OVER,
	AWAY_UNDER,
	AWAY_OVER_3WAY,
	AWAY_UNDER_3WAY,
	AWAY_OU_PUSH_3WAY,
	
	HOME_SPREAD,
	AWAY_SPREAD,
	HOME_SPREAD_3WAY,
	AWAY_SPREAD_3WAY,
	SPREAD_PUSH_3WAY,

	HOME_MONEYLINE,
	AWAY_MONEYLINE,
	HOME_MONEYLINE_3WAY,
	AWAY_MONEYLINE_3WAY,
	MONEYLINE_PUSH_3WAY;

	public static List<List<Action>> offerMatches() {

		return Arrays.asList(
				Arrays.asList(Action.OVER,                Action.UNDER), 
				Arrays.asList(Action.AWAY_SPREAD,         Action.HOME_SPREAD),
				Arrays.asList(Action.AWAY_MONEYLINE,      Action.HOME_MONEYLINE),
				Arrays.asList(Action.AWAY_OVER,           Action.AWAY_UNDER),
				Arrays.asList(Action.HOME_OVER,           Action.HOME_UNDER),
				Arrays.asList(Action.OVER_3WAY,           Action.UNDER_3WAY,          Action.OU_PUSH_3WAY),
				Arrays.asList(Action.AWAY_SPREAD_3WAY,    Action.HOME_SPREAD_3WAY,    Action.SPREAD_PUSH_3WAY),
				Arrays.asList(Action.AWAY_MONEYLINE_3WAY, Action.HOME_MONEYLINE_3WAY, Action.MONEYLINE_PUSH_3WAY),
				Arrays.asList(Action.AWAY_OVER_3WAY,      Action.AWAY_UNDER_3WAY,     Action.AWAY_OU_PUSH_3WAY),
				Arrays.asList(Action.HOME_OVER_3WAY,      Action.HOME_UNDER_3WAY,     Action.HOME_OU_PUSH_3WAY));
	}

	public static boolean isSpreadType(Action o) {
		return Arrays.asList(
				Action.AWAY_SPREAD,      Action.HOME_SPREAD,
				Action.AWAY_SPREAD_3WAY, Action.HOME_SPREAD_3WAY, Action.SPREAD_PUSH_3WAY).contains(o);

	}

	public static boolean isThreeWay(Action o) {
		return Arrays.asList(
				Action.OVER_3WAY,
				Action.UNDER_3WAY,
				Action.OU_PUSH_3WAY,
				Action.AWAY_SPREAD_3WAY,
				Action.HOME_SPREAD_3WAY,
				Action.SPREAD_PUSH_3WAY,
				Action.AWAY_MONEYLINE_3WAY,
				Action.HOME_MONEYLINE_3WAY,
				Action.MONEYLINE_PUSH_3WAY,
				Action.AWAY_OVER_3WAY,
				Action.AWAY_UNDER_3WAY,
				Action.AWAY_OU_PUSH_3WAY,
				Action.HOME_OVER_3WAY,
				Action.HOME_UNDER_3WAY,
				Action.HOME_OU_PUSH_3WAY)
				.contains(o);
	}

}
