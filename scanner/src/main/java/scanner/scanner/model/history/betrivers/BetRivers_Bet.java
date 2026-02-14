package scanner.scanner.model.history.betrivers;

import java.util.List;

public class BetRivers_Bet {

	private Integer betOdds;
	private Integer playedOdds;
	private Integer stake;
	private Integer payout;
	private String betStatus;
	private List<BetRivers_CouponRow> couponRows;
	
	
	public Integer getBetOdds() {
		return betOdds;
	}
	public void setBetOdds(Integer betOdds) {
		this.betOdds = betOdds;
	}
	public Integer getPlayedOdds() {
		return playedOdds;
	}
	public void setPlayedOdds(Integer playedOdds) {
		this.playedOdds = playedOdds;
	}
	public Integer getStake() {
		return stake;
	}
	public void setStake(Integer stake) {
		this.stake = stake;
	}
	public Integer getPayout() {
		return payout;
	}
	public void setPayout(Integer payout) {
		this.payout = payout;
	}
	public List<BetRivers_CouponRow> getCouponRows() {
		return couponRows;
	}
	public void setCouponRows(List<BetRivers_CouponRow> couponRows) {
		this.couponRows = couponRows;
	}
	public String getBetStatus() {
		return betStatus;
	}
	public void setBetStatus(String betStatus) {
		this.betStatus = betStatus;
	}
	
}
