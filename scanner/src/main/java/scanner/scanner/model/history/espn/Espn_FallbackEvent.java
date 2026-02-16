package scanner.scanner.model.history.espn;

import java.util.Date;

public class Espn_FallbackEvent {

	private String name;
	private Date startTime;
	private Espn_Type competition;
	private Espn_Type sport;
	private Espn_Type organization;
	
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Espn_Type getCompetition() {
		return competition;
	}
	public void setCompetition(Espn_Type competition) {
		this.competition = competition;
	}
	public Espn_Type getSport() {
		return sport;
	}
	public void setSport(Espn_Type sport) {
		this.sport = sport;
	}
	public Espn_Type getOrganization() {
		return organization;
	}
	public void setOrganization(Espn_Type organization) {
		this.organization = organization;
	}
	

}
