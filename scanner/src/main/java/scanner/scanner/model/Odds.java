package scanner.scanner.model;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.Status;
import scanner.scanner.util.Period;
import scanner.scanner.util.Sport;

@Document(collection = "odds")
public class Odds {
	

    @Id
    public String id;

	private Sportsbook book;
	private Sport      sport;
	private Period     period;
	private Date       gameDay;  //12/21/2019
	private int        gameNum;
	private Date       gameDateTime;
	private Date       entryTime;
	private Team       away;
	private Team       home;
	private Player     awayPitcher;
	private Player     homePitcher;
	private OU         ou;
	private Spread     ml;
	private Spread     spread;
	private TeamTotal  ttAway;
	private TeamTotal  ttHome;
	private boolean    inGame;
	
	private String     url;
	private Date       timeStamp;
	private String     gameNumber;
	private Status     status;
	private Player     player1;
	private Player     player2;
	private Boolean    doubles;
	


	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("book: " + book + "\n");
		sb.append("sport: " + sport + "\n");
		sb.append("period: " + period + "\n");
		sb.append("gameDay: " + gameDay + "\n");
		sb.append("gameNum: " + gameNum + "\n");
		sb.append("gameDateTime: " + gameDateTime + "\n");
		sb.append("entryTime: " + entryTime + "\n");
		sb.append("away: " + away );
		sb.append("home: " + home );
		sb.append("awayPitcher: " + awayPitcher + "\n");
		sb.append("homePitcher: " + homePitcher + "\n");
		sb.append("ou: " + ou + "\n");
		sb.append("ml: " + ml + "\n");
		sb.append("spread: " + spread + "\n");
		sb.append("ttHome: " + ttHome + "\n");
		sb.append("ttAway: " + ttAway + "\n");
		sb.append("underway: " + inGame + "\n\n");
		return sb.toString();
	}

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((away == null) ? 0 : away.hashCode());
		result = prime * result + ((awayPitcher == null) ? 0 : awayPitcher.hashCode());
		result = prime * result + ((book == null) ? 0 : book.hashCode());
		result = prime * result + ((gameDateTime == null) ? 0 : gameDateTime.hashCode());
		result = prime * result + ((gameDay == null) ? 0 : gameDay.hashCode());
		result = prime * result + gameNum;
		result = prime * result + ((home == null) ? 0 : home.hashCode());
		result = prime * result + ((homePitcher == null) ? 0 : homePitcher.hashCode());
		result = prime * result + (inGame ? 1231 : 1237);
		result = prime * result + ((ml == null) ? 0 : ml.hashCode());
		result = prime * result + ((ou == null) ? 0 : ou.hashCode());
		result = prime * result + ((period == null) ? 0 : period.hashCode());
		result = prime * result + ((sport == null) ? 0 : sport.hashCode());
		result = prime * result + ((spread == null) ? 0 : spread.hashCode());
		result = prime * result + ((ttAway == null) ? 0 : ttAway.hashCode());
		result = prime * result + ((ttHome == null) ? 0 : ttHome.hashCode());
		return result;
	}
    
    public Odds() {
		inGame = false;
	}
	
	public Sportsbook getBook() {
		return book;
	}
	public void setBook(Sportsbook book) {
		this.book = book;
	}
	public Sport getSport() {
		return sport;
	}
	public void setSport(Sport sport) {
		this.sport = sport;
	}
	public Date getGameDay() {
		return gameDay;
	}
	public void setGameDay(Date gameDay) {
		this.gameDay = gameDay;
	}
	public int getGameNum() {
		return gameNum;
	}
	public void setGameNum(int gameNum) {
		this.gameNum = gameNum;
	}
	public Date getGameDateTime() {
		return gameDateTime;
	}
	public void setGameDateTime(Date gameDateTime) {
		this.gameDateTime = gameDateTime;
	}
	public Date getEntryTime() {
		return entryTime;
	}
	public void setEntryTime(Date entryTime) {
		this.entryTime = entryTime;
	}
	public Team getAway() {
		return away;
	}
	public void setAway(Team away) {
		this.away = away;
	}
	public Team getHome() {
		return home;
	}
	public void setHome(Team home) {
		this.home = home;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Player getAwayPitcher() {
		return awayPitcher;
	}
	public void setAwayPitcher(Player awayPitcher) {
		this.awayPitcher = awayPitcher;
	}
	public Player getHomePitcher() {
		return homePitcher;
	}
	public void setHomePitcher(Player homePitcher) {
		this.homePitcher = homePitcher;
	}
	public OU getOu() {
		return ou;
	}
	public void setOu(OU ou) {
		this.ou = ou;
	}
	public Spread getMl() {
		return ml;
	}
	public void setMl(Spread ml) {
		this.ml = ml;
	}
	public Spread getSpread() {
		return spread;
	}
	public void setSpread(Spread spread) {
		this.spread = spread;
	}
	public Period getPeriod() {
		return period;
	}
	public void setPeriod(Period period) {
		this.period = period;
	}


	public TeamTotal getTtAway() {
		return ttAway;
	}

	public void setTtAway(TeamTotal ttAway) {
		this.ttAway = ttAway;
	}

	public TeamTotal getTtHome() {
		return ttHome;
	}

	public void setTtHome(TeamTotal ttHome) {
		this.ttHome = ttHome;
	}
	public boolean isInGame() {
		return inGame;
	}
	public void setInGame(boolean inGame) {
		this.inGame = inGame;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}

	public String getGameNumber() {
		return gameNumber;
	}

	public void setGameNumber(String gameNumber) {
		this.gameNumber = gameNumber;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Player getPlayer1() {
		return player1;
	}

	public void setPlayer1(Player player1) {
		this.player1 = player1;
	}

	public Player getPlayer2() {
		return player2;
	}

	public void setPlayer2(Player player2) {
		this.player2 = player2;
	}

	public Boolean getDoubles() {
		return doubles;
	}

	public void setDoubles(Boolean doubles) {
		this.doubles = doubles;
	}

}
