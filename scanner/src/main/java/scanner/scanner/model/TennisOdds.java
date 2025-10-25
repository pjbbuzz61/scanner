package scanner.scanner.model;

import java.io.Serializable;

public class TennisOdds implements Serializable {

	private static final long serialVersionUID = 7037016695675780265L;

	private TENNIS_PERIOD period;
	private int player1_odds;
	private int player2_odds;
	private int player1_odds_nojuice;
	private int player2_odds_nojuice;
	
	
	@Override
	public String toString() {
		return String.format("%s %5d/%5d %5d/%5d", period, player1_odds, player2_odds,player1_odds_nojuice, player2_odds_nojuice);
	}

	public int getPlayer1_odds(boolean nojuice) {
		if(nojuice) return player1_odds_nojuice;
		return player1_odds;
	}

	public int getPlayer2_odds(boolean nojuice) {
		if(nojuice) return player2_odds_nojuice;
		return player2_odds;
	}

	public TENNIS_PERIOD getPeriod() {
		return period;
	}
	public void setPeriod(TENNIS_PERIOD period) {
		this.period = period;
	}
	public int getPlayer1_odds() {
		return player1_odds;
	}
	public void setPlayer1_odds(int player1_odds) {
		this.player1_odds = player1_odds;
	}
	public int getPlayer2_odds() {
		return player2_odds;
	}
	public void setPlayer2_odds(int player2_odds) {
		this.player2_odds = player2_odds;
	}

	public int getPlayer1_odds_nojuice() {
		return player1_odds_nojuice;
	}

	public void setPlayer1_odds_nojuice(int player1_odds_nojuice) {
		this.player1_odds_nojuice = player1_odds_nojuice;
	}

	public int getPlayer2_odds_nojuice() {
		return player2_odds_nojuice;
	}

	public void setPlayer2_odds_nojuice(int player2_odds_nojuice) {
		this.player2_odds_nojuice = player2_odds_nojuice;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((period == null) ? 0 : period.hashCode());
		result = prime * result + player1_odds;
		result = prime * result + player1_odds_nojuice;
		result = prime * result + player2_odds;
		result = prime * result + player2_odds_nojuice;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TennisOdds other = (TennisOdds) obj;
		if (period != other.period)
			return false;
		if (player1_odds != other.player1_odds)
			return false;
		if (player1_odds_nojuice != other.player1_odds_nojuice)
			return false;
		if (player2_odds != other.player2_odds)
			return false;
		if (player2_odds_nojuice != other.player2_odds_nojuice)
			return false;
		return true;
	}

}
