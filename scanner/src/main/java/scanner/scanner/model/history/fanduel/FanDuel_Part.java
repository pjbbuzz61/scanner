package scanner.scanner.model.history.fanduel;

import java.util.Date;

public class FanDuel_Part {

	private String eventId;
	private Integer americanPrice;
	private Integer originalAmericanPrice;
	private String competitionName;
	private String eventDescription;
	private String eventMarketDescription;
	private String result;
	private String selectionName;
	private Date startTime;
	
	
	public String getEventId() {
		return eventId;
	}
	public void setEventId(String eventId) {
		this.eventId = eventId;
	}
	public Integer getAmericanPrice() {
		return americanPrice;
	}
	public void setAmericanPrice(Integer americanPrice) {
		this.americanPrice = americanPrice;
	}
	public Integer getOriginalAmericanPrice() {
		return originalAmericanPrice;
	}
	public void setOriginalAmericanPrice(Integer originalAmericanPrice) {
		this.originalAmericanPrice = originalAmericanPrice;
	}
	public String getCompetitionName() {
		return competitionName;
	}
	public void setCompetitionName(String competitionName) {
		this.competitionName = competitionName;
	}
	public String getEventDescription() {
		return eventDescription;
	}
	public void setEventDescription(String eventDescription) {
		this.eventDescription = eventDescription;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getSelectionName() {
		return selectionName;
	}
	public void setSelectionName(String selectionName) {
		this.selectionName = selectionName;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public String getEventMarketDescription() {
		return eventMarketDescription;
	}
	public void setEventMarketDescription(String eventMarketDescription) {
		this.eventMarketDescription = eventMarketDescription;
	}
	
}
