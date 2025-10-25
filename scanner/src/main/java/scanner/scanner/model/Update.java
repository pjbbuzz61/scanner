package scanner.scanner.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

@Document(collection = "updates")
public class Update {

    @Id
    private String id;

    private String     type; // team or pitcher
    private Sportsbook book;
	private Sport      sport;
	private String     teamName;
	private Team       team;
	private String     player;
	
	public Update() {}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Type: " + type + ", ");
		sb.append("book: " + book + ", ");
		sb.append("sport: " + sport + ", ");
		sb.append("teamName: " + teamName + ", ");
		sb.append("team: " + team + ", ");
		sb.append("player: " + player + ", ");
		sb.append("\n");
		return sb.toString();
	}
	
	public Update(String type, Sportsbook book, Sport sport, String teamName) {
		this.type = type;
		this.book = book;
		this.sport = sport;
		this.teamName = teamName;
	}

	public Update(String type, Sportsbook book, Sport sport, String teamName, String player) {
		this.type = type;
		this.book = book;
		this.sport = sport;
		this.teamName = teamName;
		this.player = player;
	}

	public Update(String type, Team team, String sbSpecificPlayerName) {
		this.type = type;
		this.book = team.getBook();
		this.sport = team.getSport();
		this.teamName = team.getCommonName().toString();
		this.team = team;
		this.player = sbSpecificPlayerName;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
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
	public String getTeamName() {
		return teamName;
	}
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}
	public String getPlayer() {
		return player;
	}
	public void setPlayer(String player) {
		this.player = player;
	}

	public Team getTeam() {
		return team;
	}

	public void setTeam(Team team) {
		this.team = team;
	}

}
