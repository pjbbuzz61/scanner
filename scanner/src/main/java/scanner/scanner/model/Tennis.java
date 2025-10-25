package scanner.scanner.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

public class Tennis implements Serializable {

	private static final long serialVersionUID = 7072519172816008290L;

	@Id
	private ObjectId id;
	
	private Date matchTime;
	
	private Date timeStamp;
	private String url;
	private String gameNumber;
	private Boolean doubles;
	private Boolean male;
	
	
	private String player1;
	private String player2;
	
	private TENNIS_STATUS status;
	private TENNIS_PERIOD period;
	
	private int server;
	
	private String player1_points;
	private int player1_games;
	private int player1_sets;
	
	private String player2_points;
	private int player2_games;
	private int player2_sets;
	
	private List<TennisOdds> odds;
	private Boolean suspect; // odds are suspect
	private String tournament;
	private String betMGMTournamentName;
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(player1 + " vs " + player2 + " @" + matchTime + ": " + status + "\n");
		sb.append("Time Stamp: " + timeStamp + "\n");
		sb.append("Game Number: " + gameNumber + "\n");
		sb.append("URL: " + url + "\n");
		sb.append("Doubles: " + doubles + "\n");
		sb.append(String.format("%s %3s %d %d", server==1?"*":" ", player1_points, player1_games, player1_sets) + "\n");
		sb.append(String.format("%s %3s %d %d", server==2?"*":" ", player2_points, player2_games, player2_sets) + "\n");
		if(odds != null) {
			for(TennisOdds odds : odds) {
				sb.append(odds + "\n");
			}
		}
		sb.append("Suspect: " + suspect + "\n");
		sb.append("Tournament: " + tournament + "\n");
		sb.append("BetMGM Tournament: " + betMGMTournamentName + "\n");
		return sb.toString();
	}
	
	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getPlayer1() {
		return player1;
	}

	public void setPlayer1(String player1) {
		this.player1 = player1;
	}

	public String getPlayer2() {
		return player2;
	}

	public void setPlayer2(String player2) {
		this.player2 = player2;
	}

	public TENNIS_STATUS getStatus() {
		return status;
	}

	public void setStatus(TENNIS_STATUS status) {
		this.status = status;
	}

	public int getServer() {
		return server;
	}

	public void setServer(int server) {
		this.server = server;
	}

	public String getPlayer1_points() {
		return player1_points;
	}

	public void setPlayer1_points(String player1_points) {
		this.player1_points = player1_points;
	}

	public int getPlayer1_games() {
		return player1_games;
	}

	public void setPlayer1_games(int player1_games) {
		this.player1_games = player1_games;
	}

	public int getPlayer1_sets() {
		return player1_sets;
	}

	public void setPlayer1_sets(int player1_sets) {
		this.player1_sets = player1_sets;
	}

	public String getPlayer2_points() {
		return player2_points;
	}

	public void setPlayer2_points(String player2_points) {
		this.player2_points = player2_points;
	}

	public int getPlayer2_games() {
		return player2_games;
	}

	public void setPlayer2_games(int player2_games) {
		this.player2_games = player2_games;
	}

	public int getPlayer2_sets() {
		return player2_sets;
	}

	public void setPlayer2_sets(int player2_sets) {
		this.player2_sets = player2_sets;
	}

	public List<TennisOdds> getOdds() {
		return odds;
	}

	public void setOdds(List<TennisOdds> odds) {
		this.odds = odds;
	}

	public TENNIS_PERIOD getPeriod() {
		return period;
	}

	public void setPeriod(TENNIS_PERIOD period) {
		this.period = period;
	}

	public Date getMatchTime() {
		return matchTime;
	}

	public void setMatchTime(Date matchTime) {
		this.matchTime = matchTime;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getGameNumber() {
		return gameNumber;
	}

	public void setGameNumber(String gameNumber) {
		this.gameNumber = gameNumber;
	}

	public Boolean getDoubles() {
		return doubles;
	}

	public void setDoubles(Boolean doubles) {
		this.doubles = doubles;
	}

	public Boolean getMale() {
		return male;
	}

	public void setMale(Boolean male) {
		this.male = male;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((doubles == null) ? 0 : doubles.hashCode());
		result = prime * result + ((gameNumber == null) ? 0 : gameNumber.hashCode());
		result = prime * result + ((male == null) ? 0 : male.hashCode());
		if(odds != null) {
			for(TennisOdds to : odds) {
				result = prime * result + ((to == null) ? 0 : to.hashCode());
			}
			result = prime * result + ((odds == null) ? 0 : odds.hashCode());
		}
		result = prime * result + ((period == null) ? 0 : period.hashCode());
		result = prime * result + ((player1 == null) ? 0 : player1.hashCode());
		result = prime * result + player1_games;
		result = prime * result + ((player1_points == null) ? 0 : player1_points.hashCode());
		result = prime * result + player1_sets;
		result = prime * result + ((player2 == null) ? 0 : player2.hashCode());
		result = prime * result + player2_games;
		result = prime * result + ((player2_points == null) ? 0 : player2_points.hashCode());
		result = prime * result + player2_sets;
		result = prime * result + server;
		result = prime * result + ((status == null) ? 0 : status.hashCode());
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
		Tennis other = (Tennis) obj;
		if (doubles == null) {
			if (other.doubles != null)
				return false;
		} else if (!doubles.equals(other.doubles))
			return false;
		if (male == null) {
			if (other.male != null)
				return false;
		} else if (!male.equals(other.male))
			return false;
		if (odds == null) {
			if (other.odds != null)
				return false;
		} else if ((odds != null) && (other.odds == null)) {
			return false;
		} else if (odds.size() != other.odds.size()) {
			return false;
		} else {
			for(int i = 0; i < odds.size(); ++i) {
				if(!odds.get(i).equals(other.odds.get(i))) {
					return false;
				}
			}
		}
		if (period != other.period)
			return false;
		if (player1 == null) {
			if (other.player1 != null)
				return false;
		} else if (!player1.equals(other.player1))
			return false;
		if (player1_games != other.player1_games)
			return false;
		if (player1_points == null) {
			if (other.player1_points != null)
				return false;
		} else if (!player1_points.equals(other.player1_points))
			return false;
		if (player1_sets != other.player1_sets)
			return false;
		if (player2 == null) {
			if (other.player2 != null)
				return false;
		} else if (!player2.equals(other.player2))
			return false;
		if (player2_games != other.player2_games)
			return false;
		if (player2_points == null) {
			if (other.player2_points != null)
				return false;
		} else if (!player2_points.equals(other.player2_points))
			return false;
		if (player2_sets != other.player2_sets)
			return false;
		if (server != other.server)
			return false;
		if (status != other.status)
			return false;
		return true;
	}

	public void printScores() {
		StringBuilder sb = new StringBuilder();
		sb.append(player1 + " vs " + player2 + " @" + matchTime + ": " + status + "\n");
		sb.append("Time Stamp: " + timeStamp + "\n");
		sb.append(String.format("%s %3s %d %d", server==1?"*":" ", player1_points, player1_games, player1_sets) + "\n");
		sb.append(String.format("%s %3s %d %d", server==2?"*":" ", player2_points, player2_games, player2_sets) + "\n");
		System.out.println(sb.toString());
	}

	public Boolean getSuspect() {
		return suspect;
	}

	public void setSuspect(Boolean suspect) {
		this.suspect = suspect;
	}

	public String getTournament() {
		return tournament;
	}

	public void setTournament(String tournament) {
		this.tournament = tournament;
	}

	public String getBetMGMTournamentName() {
		return betMGMTournamentName;
	}

	public void setBetMGMTournamentName(String betMGMTournamentName) {
		this.betMGMTournamentName = betMGMTournamentName;
	}


	
}
