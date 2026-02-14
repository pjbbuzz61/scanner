package scanner.scanner.model.history.hardrock;

public class HardRock_Bet {

	private HardRock_Amount stake;
	private HardRock_Amount capturedStake;
	private Boolean freeBet;
	private Double totalPrice;
	private Long settlementTime;
	private Long betTime;
	private HardRock_Part parts;
	private String type;
	private String winType;
	private String betRiskGroup;
	private String id;
	private String betSlipId;
	private Double totalPayout;
	private String displayStatus;
	private int numLegs;
	private Boolean oddsBoostBonus;
	private Double oddsBoostBonusPcnt;
	private Double oddsBoostBonusWinnings;
	private String betClassification;
	
	
	public HardRock_Amount getStake() {
		return stake;
	}
	public void setStake(HardRock_Amount stake) {
		this.stake = stake;
	}
	public HardRock_Amount getCapturedStake() {
		return capturedStake;
	}
	public void setCapturedStake(HardRock_Amount capturedStake) {
		this.capturedStake = capturedStake;
	}
	public Boolean getFreeBet() {
		return freeBet;
	}
	public void setFreeBet(Boolean freeBet) {
		this.freeBet = freeBet;
	}
	public Double getTotalPrice() {
		return totalPrice;
	}
	public void setTotalPrice(Double totalPrice) {
		this.totalPrice = totalPrice;
	}
	public Long getSettlementTime() {
		return settlementTime;
	}
	public void setSettlementTime(Long settlementTime) {
		this.settlementTime = settlementTime;
	}
	public Long getBetTime() {
		return betTime;
	}
	public void setBetTime(Long betTime) {
		this.betTime = betTime;
	}
	public HardRock_Part getParts() {
		return parts;
	}
	public void setParts(HardRock_Part parts) {
		this.parts = parts;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getWinType() {
		return winType;
	}
	public void setWinType(String winType) {
		this.winType = winType;
	}
	public String getBetRiskGroup() {
		return betRiskGroup;
	}
	public void setBetRiskGroup(String betRiskGroup) {
		this.betRiskGroup = betRiskGroup;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getBetSlipId() {
		return betSlipId;
	}
	public void setBetSlipId(String betSlipId) {
		this.betSlipId = betSlipId;
	}
	public Double getTotalPayout() {
		return totalPayout;
	}
	public void setTotalPayout(Double totalPayout) {
		this.totalPayout = totalPayout;
	}
	public String getDisplayStatus() {
		return displayStatus;
	}
	public void setDisplayStatus(String displayStatus) {
		this.displayStatus = displayStatus;
	}
	public int getNumLegs() {
		return numLegs;
	}
	public void setNumLegs(int numLegs) {
		this.numLegs = numLegs;
	}
	public Boolean getOddsBoostBonus() {
		return oddsBoostBonus;
	}
	public void setOddsBoostBonus(Boolean oddsBoostBonus) {
		this.oddsBoostBonus = oddsBoostBonus;
	}
	public Double getOddsBoostBonusPcnt() {
		return oddsBoostBonusPcnt;
	}
	public void setOddsBoostBonusPcnt(Double oddsBoostBonusPcnt) {
		this.oddsBoostBonusPcnt = oddsBoostBonusPcnt;
	}
	public Double getOddsBoostBonusWinnings() {
		return oddsBoostBonusWinnings;
	}
	public void setOddsBoostBonusWinnings(Double oddsBoostBonusWinnings) {
		this.oddsBoostBonusWinnings = oddsBoostBonusWinnings;
	}
	public String getBetClassification() {
		return betClassification;
	}
	public void setBetClassification(String betClassification) {
		this.betClassification = betClassification;
	}
	
	
	
}
