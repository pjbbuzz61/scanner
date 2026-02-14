package scanner.scanner.model.history.betrivers;

import java.util.Date;
import java.util.List;

public class BetRivers_EventInfo {

	private Long eventId;
	private String sport;
	private String homeName;
	private String awayName;
	private String eventName;
	private List<BetRivers_EventGroup> eventGroups;
	private Date eventStartDate;
	
	
	public Long getEventId() {
		return eventId;
	}
	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}
	public String getSport() {
		return sport;
	}
	public void setSport(String sport) {
		this.sport = sport;
	}
	public String getHomeName() {
		return homeName;
	}
	public void setHomeName(String homeName) {
		this.homeName = homeName;
	}
	public String getAwayName() {
		return awayName;
	}
	public void setAwayName(String awayName) {
		this.awayName = awayName;
	}
	public String getEventName() {
		return eventName;
	}
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}
	public List<BetRivers_EventGroup> getEventGroups() {
		return eventGroups;
	}
	public void setEventGroups(List<BetRivers_EventGroup> eventGroups) {
		this.eventGroups = eventGroups;
	}
	public Date getEventStartDate() {
		return eventStartDate;
	}
	public void setEventStartDate(Date eventStartDate) {
		this.eventStartDate = eventStartDate;
	}
	
}
