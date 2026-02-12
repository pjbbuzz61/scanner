package scanner.scanner.model.history.betmgm;

import java.util.Date;
import java.util.List;

public class BetMGM_BetSlip {

	private String                  betSlipNumber;
	private String                  type;
	private Date                    conclusionDateUtc;
	private BetMGM_Currency         stake;
	private BetMGM_Currency         payout;
	private String                  state;
	private List<BetMGM_Bet>        bets;
	private BetMGM_odds             totalOdds;
	private Boolean                 isFreeBet;
	private Boolean                 isEarlyPayout;
	private String                  slipType;
	private BetMGM_Currency         grossPayout;
	private List<BetMGM_PromoToken> promoTokens;
	
	
	public String getBetSlipNumber() {
		return betSlipNumber;
	}
	public void setBetSlipNumber(String betSlipNumber) {
		this.betSlipNumber = betSlipNumber;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Date getConclusionDateUtc() {
		return conclusionDateUtc;
	}
	public void setConclusionDateUtc(Date conclusionDateUtc) {
		this.conclusionDateUtc = conclusionDateUtc;
	}
	public BetMGM_Currency getStake() {
		return stake;
	}
	public void setStake(BetMGM_Currency stake) {
		this.stake = stake;
	}
	public BetMGM_Currency getPayout() {
		return payout;
	}
	public void setPayout(BetMGM_Currency payout) {
		this.payout = payout;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public List<BetMGM_Bet> getBets() {
		return bets;
	}
	public void setBets(List<BetMGM_Bet> bets) {
		this.bets = bets;
	}
	public BetMGM_odds getTotalOdds() {
		return totalOdds;
	}
	public void setTotalOdds(BetMGM_odds totalOdds) {
		this.totalOdds = totalOdds;
	}
	public Boolean getIsFreeBet() {
		return isFreeBet;
	}
	public void setIsFreeBet(Boolean isFreeBet) {
		this.isFreeBet = isFreeBet;
	}
	public Boolean getIsEarlyPayout() {
		return isEarlyPayout;
	}
	public void setIsEarlyPayout(Boolean isEarlyPayout) {
		this.isEarlyPayout = isEarlyPayout;
	}
	public String getSlipType() {
		return slipType;
	}
	public void setSlipType(String slipType) {
		this.slipType = slipType;
	}
	public BetMGM_Currency getGrossPayout() {
		return grossPayout;
	}
	public void setGrossPayout(BetMGM_Currency grossPayout) {
		this.grossPayout = grossPayout;
	}
	public List<BetMGM_PromoToken> getPromoTokens() {
		return promoTokens;
	}
	public void setPromoTokens(List<BetMGM_PromoToken> promoTokens) {
		this.promoTokens = promoTokens;
	}

	

}
