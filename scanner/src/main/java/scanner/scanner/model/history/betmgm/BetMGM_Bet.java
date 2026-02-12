package scanner.scanner.model.history.betmgm;

public class BetMGM_Bet {

	private String state;
	private String outcome;
	private BetMGM_odds odds;
	private BetMGM_fixture fixture;
	private BetMGM_name competition;
	private BetMGM_name market;
	private BetMGM_name option;
	private BetMGM_name sport;
	
	
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public String getOutcome() {
		return outcome;
	}
	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}
	public BetMGM_odds getOdds() {
		return odds;
	}
	public void setOdds(BetMGM_odds odds) {
		this.odds = odds;
	}
	public BetMGM_fixture getFixture() {
		return fixture;
	}
	public void setFixture(BetMGM_fixture fixture) {
		this.fixture = fixture;
	}
	public BetMGM_name getCompetition() {
		return competition;
	}
	public void setCompetition(BetMGM_name competition) {
		this.competition = competition;
	}
	public BetMGM_name getMarket() {
		return market;
	}
	public void setMarket(BetMGM_name market) {
		this.market = market;
	}
	public BetMGM_name getOption() {
		return option;
	}
	public void setOption(BetMGM_name option) {
		this.option = option;
	}
	public BetMGM_name getSport() {
		return sport;
	}
	public void setSport(BetMGM_name sport) {
		this.sport = sport;
	}
	
	
}
