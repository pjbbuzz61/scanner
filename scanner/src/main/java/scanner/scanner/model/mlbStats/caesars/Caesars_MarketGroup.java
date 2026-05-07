package scanner.scanner.model.mlbStats.caesars;

import java.util.List;

public class Caesars_MarketGroup {

	private String               marketDisplayGroupDisplayName;
	private List<Caesars_Market> markets;
	private List<String>         teams;
	
	
	
	public String getMarketDisplayGroupDisplayName() {
		return marketDisplayGroupDisplayName;
	}
	public void setMarketDisplayGroupDisplayName(String marketDisplayGroupDisplayName) {
		this.marketDisplayGroupDisplayName = marketDisplayGroupDisplayName;
	}
	public List<Caesars_Market> getMarkets() {
		return markets;
	}
	public void setMarkets(List<Caesars_Market> markets) {
		this.markets = markets;
	}
	public List<String> getTeams() {
		return teams;
	}
	public void setTeams(List<String> teams) {
		this.teams = teams;
	}
	
	
}
