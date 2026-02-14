package scanner.scanner.model.history.betrivers;

import java.util.Date;
import java.util.List;

public class BetRivers_Item {

	private String couponExternalRef;
	private Long couponRef;
	private Date placedDate;
	private String couponType;
	private String rewardType;
	private List<BetRivers_Bet> bets;
	private BetRivers_text title;
	private List<BetRivers_text> description;
	
	
	public String getCouponExternalRef() {
		return couponExternalRef;
	}
	public void setCouponExternalRef(String couponExternalRef) {
		this.couponExternalRef = couponExternalRef;
	}
	public Long getCouponRef() {
		return couponRef;
	}
	public void setCouponRef(Long couponRef) {
		this.couponRef = couponRef;
	}
	public Date getPlacedDate() {
		return placedDate;
	}
	public void setPlacedDate(Date placedDate) {
		this.placedDate = placedDate;
	}
	public List<BetRivers_Bet> getBets() {
		return bets;
	}
	public void setBets(List<BetRivers_Bet> bets) {
		this.bets = bets;
	}
	public BetRivers_text getTitle() {
		return title;
	}
	public void setTitle(BetRivers_text title) {
		this.title = title;
	}
	public List<BetRivers_text> getDescription() {
		return description;
	}
	public void setDescription(List<BetRivers_text> description) {
		this.description = description;
	}
	public String getRewardType() {
		return rewardType;
	}
	public void setRewardType(String rewardType) {
		this.rewardType = rewardType;
	}
	public String getCouponType() {
		return couponType;
	}
	public void setCouponType(String couponType) {
		this.couponType = couponType;
	}
	
}
