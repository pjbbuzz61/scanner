package scanner.scanner.model.history.hardrock;

public class HardRock_BetPart {

	private String id;
	private HardRock_Odds odds;
	private HardRock_Name sport;
	private HardRock_Name competition;
	private HardRock_Name event;
	private HardRock_Name market;
	private Double line;
	private HardRock_Name selection;
	private String resultType;
	private String selectionType;
	private Long settlementTime;
	private HardRock_Result eventResult;
	private Double stake;
	private Long eventTime;
	private String winType;
	
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public HardRock_Odds getOdds() {
		return odds;
	}
	public void setOdds(HardRock_Odds odds) {
		this.odds = odds;
	}
	public HardRock_Name getSport() {
		return sport;
	}
	public void setSport(HardRock_Name sport) {
		this.sport = sport;
	}
	public HardRock_Name getCompetition() {
		return competition;
	}
	public void setCompetition(HardRock_Name competition) {
		this.competition = competition;
	}
	public HardRock_Name getEvent() {
		return event;
	}
	public void setEvent(HardRock_Name event) {
		this.event = event;
	}
	public HardRock_Name getMarket() {
		return market;
	}
	public void setMarket(HardRock_Name market) {
		this.market = market;
	}
	public Double getLine() {
		return line;
	}
	public void setLine(Double line) {
		this.line = line;
	}
	public HardRock_Name getSelection() {
		return selection;
	}
	public void setSelection(HardRock_Name selection) {
		this.selection = selection;
	}
	public String getResultType() {
		return resultType;
	}
	public void setResultType(String resultType) {
		this.resultType = resultType;
	}
	public String getSelectionType() {
		return selectionType;
	}
	public void setSelectionType(String selectionType) {
		this.selectionType = selectionType;
	}
	public Long getSettlementTime() {
		return settlementTime;
	}
	public void setSettlementTime(Long settlementTime) {
		this.settlementTime = settlementTime;
	}
	public HardRock_Result getEventResult() {
		return eventResult;
	}
	public void setEventResult(HardRock_Result eventResult) {
		this.eventResult = eventResult;
	}
	public Double getStake() {
		return stake;
	}
	public void setStake(Double stake) {
		this.stake = stake;
	}
	public Long getEventTime() {
		return eventTime;
	}
	public void setEventTime(Long eventTime) {
		this.eventTime = eventTime;
	}
	public String getWinType() {
		return winType;
	}
	public void setWinType(String winType) {
		this.winType = winType;
	}

	

}
