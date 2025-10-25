package scanner.scanner.model;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import scanner.scanner.util.Action;
import scanner.scanner.util.Period;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

@Document(collection = "offers")
public class Offer {

    @Id
    public String id;

    // This block should uniquely identify a game
	private Sport      sport;
	private String     away;
	private String     home;
	private String     awayPitcher;
	private String     homePitcher;
	private String     gameDay;      // for the game -- use PST so all games I care about are on the same day (MM/dd/yyyy)
	private Boolean    gameTwo;

	private Integer    gameId;
	
	//
	private Sportsbook book;
	private Date       gameDateTime; 
	private Date       entryTime;
	private Period     period;
	
	private Action     type;
	private Double     points;
	private Integer    americanOdds;
	private Double     decimalOdds;
	
	
	@Override
	public String toString() {
		return String.format("%-5s %-12s %20s at %-20s %-15s %-20s %5.1f %5d %5.2f %s", 
				sport, period, away, home, book, type, points, americanOdds, decimalOdds,gameDateTime);
	}

	public Offer () {}
	
	public Offer (Odds odds) {
		this.sport = odds.getSport();
		this.away = odds.getAway().getCommonName();
		this.home = odds.getHome().getCommonName();
		if(odds.getGameNum() == 2) {
			this.gameTwo = true;
		}
		if(odds.getAwayPitcher() != null) {
			this.awayPitcher = odds.getAwayPitcher().getCommonName();
		}
		if(odds.getHomePitcher() != null) {
			this.homePitcher = odds.getHomePitcher().getCommonName();
		}
		if(odds.getGameDateTime() != null) {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
			cal.setTime(odds.getGameDateTime());
			this.gameDay = String.format("%02d/%02d/%04d", cal.get(Calendar.MONTH)+1, Calendar.DAY_OF_MONTH, cal.get(Calendar.YEAR));
			this.gameDateTime = odds.getGameDateTime();
		}
		this.gameId = hashCode();
		this.book = odds.getBook();
		this.entryTime = new Date();
		this.period = odds.getPeriod();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sport == null) ? 0 : sport.hashCode());
		result = prime * result + ((away == null) ? 0 : away.hashCode());
		result = prime * result + ((home == null) ? 0 : home.hashCode());
		result = prime * result + ((awayPitcher == null) ? 0 : awayPitcher.hashCode());
		result = prime * result + ((homePitcher == null) ? 0 : homePitcher.hashCode());
		result = prime * result + ((gameDay == null) ? 0 : gameDay.hashCode());
		result = prime * result + ((gameTwo == null) ? 0 : gameTwo.hashCode());
		return result;
	}

	public int uniqueKey() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sport == null) ? 0 : sport.hashCode());
		result = prime * result + ((period == null) ? 0 : period.hashCode());
		result = prime * result + ((book == null) ? 0 : book.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((points == null) ? 0 : points.hashCode());
		result = prime * result + ((decimalOdds == null) ? 0 : decimalOdds.hashCode());
		result = prime * result + ((away == null) ? 0 : away.hashCode());
		result = prime * result + ((home == null) ? 0 : home.hashCode());
		result = prime * result + ((awayPitcher == null) ? 0 : awayPitcher.hashCode());
		result = prime * result + ((homePitcher == null) ? 0 : homePitcher.hashCode());
		result = prime * result + ((gameDay == null) ? 0 : gameDay.hashCode());
		result = prime * result + ((gameTwo == null) ? 0 : gameTwo.hashCode());
		return result;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Sport getSport() {
		return sport;
	}
	public void setSport(Sport sport) {
		this.sport = sport;
	}
	public String getAway() {
		return away;
	}
	public void setAway(String away) {
		this.away = away;
	}
	public String getHome() {
		return home;
	}
	public void setHome(String home) {
		this.home = home;
	}
	public String getAwayPitcher() {
		return awayPitcher;
	}
	public void setAwayPitcher(String awayPitcher) {
		this.awayPitcher = awayPitcher;
	}
	public String getHomePitcher() {
		return homePitcher;
	}
	public void setHomePitcher(String homePitcher) {
		this.homePitcher = homePitcher;
	}
	public Sportsbook getBook() {
		return book;
	}
	public void setBook(Sportsbook book) {
		this.book = book;
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
	public Period getPeriod() {
		return period;
	}
	public void setPeriod(Period period) {
		this.period = period;
	}
	public Action getType() {
		return type;
	}
	public void setType(Action type) {
		this.type = type;
	}
	public Double getPoints() {
		return points;
	}
	public void setPoints(Double points) {
		this.points = points;
	}
	public Integer getAmericanOdds() {
		return americanOdds;
	}
	public void setAmericanOdds(Integer americanOdds) {
		this.americanOdds = americanOdds;
	}
	public Double getDecimalOdds() {
		return decimalOdds;
	}
	public void setDecimalOdds(Double decimalOdds) {
		this.decimalOdds = decimalOdds;
	}
	public String getGameDay() {
		return gameDay;
	}
	public void setGameDay(String gameDay) {
		this.gameDay = gameDay;
	}
	public Integer getGameId() {
		return gameId;
	}
	public void setGameId(Integer gameId) {
		this.gameId = gameId;
	}

	public Boolean getGameTwo() {
		return gameTwo;
	}

	public void setGameTwo(Boolean gameTwo) {
		this.gameTwo = gameTwo;
	}
}
