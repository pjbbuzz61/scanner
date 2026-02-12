package scanner.scanner.model.history.caesars;

import java.util.Date;

public class Caesars_Leg {
	private String             id;
	private String             type;
	private Caesars_identifier event;
	private Caesars_identifier market;
	private Caesars_identifier selection;
	private Caesars_identifier competition;
	private Caesars_identifier sport;
	private Caesars_Odds       price;
	private Caesars_Result     result;
	private Date               eventStartTime;
	
	
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
	public Caesars_identifier getEvent() {
		return event;
	}
	public void setEvent(Caesars_identifier event) {
		this.event = event;
	}
	public Caesars_identifier getMarket() {
		return market;
	}
	public void setMarket(Caesars_identifier market) {
		this.market = market;
	}
	public Caesars_identifier getSelection() {
		return selection;
	}
	public void setSelection(Caesars_identifier selection) {
		this.selection = selection;
	}
	public Caesars_identifier getCompetition() {
		return competition;
	}
	public void setCompetition(Caesars_identifier competition) {
		this.competition = competition;
	}
	public Caesars_identifier getSport() {
		return sport;
	}
	public void setSport(Caesars_identifier sport) {
		this.sport = sport;
	}
	public Caesars_Odds getPrice() {
		return price;
	}
	public void setPrice(Caesars_Odds price) {
		this.price = price;
	}
	public Caesars_Result getResult() {
		return result;
	}
	public void setResult(Caesars_Result result) {
		this.result = result;
	}
	public Date getEventStartTime() {
		return eventStartTime;
	}
	public void setEventStartTime(Date eventStartTime) {
		this.eventStartTime = eventStartTime;
	}
	
	
	
}
