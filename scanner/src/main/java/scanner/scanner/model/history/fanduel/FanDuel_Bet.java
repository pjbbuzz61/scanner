package scanner.scanner.model.history.fanduel;

import java.util.Date;
import java.util.List;

public class FanDuel_Bet {

	private String betId;
	private String betType;
	private Integer americanBetPrice; // only set for parlays
	private List<FanDuel_Leg> legs;
	private List<FanDuel_OpInfo> operationInfo;
	private Date placedDate;
	private Date settledDate;
	private String result;
	private Double currentSize; // bet amount
	private Double pandl;  // payout
	private FanDuel_Reward rewardUsed;
	
	
	
	public String getBetId() {
		return betId;
	}
	public void setBetId(String betId) {
		this.betId = betId;
	}
	public String getBetType() {
		return betType;
	}
	public void setBetType(String betType) {
		this.betType = betType;
	}
	public List<FanDuel_Leg> getLegs() {
		return legs;
	}
	public void setLegs(List<FanDuel_Leg> legs) {
		this.legs = legs;
	}
	public List<FanDuel_OpInfo> getOperationInfo() {
		return operationInfo;
	}
	public void setOperationInfo(List<FanDuel_OpInfo> operationInfo) {
		this.operationInfo = operationInfo;
	}
	public Date getPlacedDate() {
		return placedDate;
	}
	public void setPlacedDate(Date placedDate) {
		this.placedDate = placedDate;
	}
	public Date getSettledDate() {
		return settledDate;
	}
	public void setSettledDate(Date settledDate) {
		this.settledDate = settledDate;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public Double getPandl() {
		return pandl;
	}
	public void setPandl(Double pandl) {
		this.pandl = pandl;
	}
	public Double getCurrentSize() {
		return currentSize;
	}
	public void setCurrentSize(Double currentSize) {
		this.currentSize = currentSize;
	}
	public Integer getAmericanBetPrice() {
		return americanBetPrice;
	}
	public void setAmericanBetPrice(Integer americanBetPrice) {
		this.americanBetPrice = americanBetPrice;
	}
	public FanDuel_Reward getRewardUsed() {
		return rewardUsed;
	}
	public void setRewardUsed(FanDuel_Reward rewardUsed) {
		this.rewardUsed = rewardUsed;
	}
	
}
