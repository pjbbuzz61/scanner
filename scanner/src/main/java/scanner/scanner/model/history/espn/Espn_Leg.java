package scanner.scanner.model.history.espn;

public class Espn_Leg {

	private String competitionName;
	private Espn_Odds odds;
	private Espn_Type market;
	private Espn_FallbackEvent fallbackEvent;
	private String outcome;
	private String marketSelectionName;
	private String marketSelectionType;
	public String getCompetitionName() {
		return competitionName;
	}
	public void setCompetitionName(String competitionName) {
		this.competitionName = competitionName;
	}
	public Espn_Odds getOdds() {
		return odds;
	}
	public void setOdds(Espn_Odds odds) {
		this.odds = odds;
	}
	public Espn_Type getMarket() {
		return market;
	}
	public void setMarket(Espn_Type market) {
		this.market = market;
	}
	public Espn_FallbackEvent getFallbackEvent() {
		return fallbackEvent;
	}
	public void setFallbackEvent(Espn_FallbackEvent fallbackEvent) {
		this.fallbackEvent = fallbackEvent;
	}
	public String getOutcome() {
		return outcome;
	}
	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}
	public String getMarketSelectionName() {
		return marketSelectionName;
	}
	public void setMarketSelectionName(String marketSelectionName) {
		this.marketSelectionName = marketSelectionName;
	}
	public String getMarketSelectionType() {
		return marketSelectionType;
	}
	public void setMarketSelectionType(String marketSelectionType) {
		this.marketSelectionType = marketSelectionType;
	}

}
