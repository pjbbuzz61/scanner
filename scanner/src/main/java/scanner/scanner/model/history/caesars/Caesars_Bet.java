package scanner.scanner.model.history.caesars;

import java.util.Date;
import java.util.List;

public class Caesars_Bet {

	private String                  id;
	private String                  betTitle;
	private String                  betType;
	private String                  betSubtitle;
	private String                  type;
	private String                  typeCategory; // straights or parlays
	private Date                    placedAt;
	private Date                    settledAt;
	private Integer                 totalStake; // in hundreds, so $5.00 is 500
	private Integer                 payout;
	private Caesars_Odds            price;
	private Integer                 operatorPayout; // hundreds
	private List<Caesars_Leg>       legs;
	private String                  resultIndicator;
	private String                  typeName;
	private Integer                 freebetStake;
	private Boolean                 cashOut;
	private Integer                 preBonusPotentialReturns;
	private Caesars_Odds            estimatedOdds;
	private String                  universe;
	private Caesars_EventInfo       eventMetadata;
	private Caesars_SelectionInfo   selectionMetadata;
	private Caesars_SettledBetInfo  settledBetData;
	private String                  wagerType;
	
	
	
	
	public String getBetTitle() {
		return betTitle;
	}
	public void setBetTitle(String betTitle) {
		this.betTitle = betTitle;
	}
	public String getBetType() {
		return betType;
	}
	public void setBetType(String betType) {
		this.betType = betType;
	}
	public String getBetSubtitle() {
		return betSubtitle;
	}
	public void setBetSubtitle(String betSubtitle) {
		this.betSubtitle = betSubtitle;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Caesars_Odds getPrice() {
		return price;
	}
	public void setPrice(Caesars_Odds price) {
		this.price = price;
	}
	public String getUniverse() {
		return universe;
	}
	public void setUniverse(String universe) {
		this.universe = universe;
	}
	public Caesars_EventInfo getEventMetadata() {
		return eventMetadata;
	}
	public void setEventMetadata(Caesars_EventInfo eventMetadata) {
		this.eventMetadata = eventMetadata;
	}
	public Caesars_SelectionInfo getSelectionMetadata() {
		return selectionMetadata;
	}
	public void setSelectionMetadata(Caesars_SelectionInfo selectionMetadata) {
		this.selectionMetadata = selectionMetadata;
	}
	public Caesars_SettledBetInfo getSettledBetData() {
		return settledBetData;
	}
	public void setSettledBetData(Caesars_SettledBetInfo settledBetData) {
		this.settledBetData = settledBetData;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTypeCategory() {
		return typeCategory;
	}
	public void setTypeCategory(String typeCategory) {
		this.typeCategory = typeCategory;
	}
	public Date getPlacedAt() {
		return placedAt;
	}
	public void setPlacedAt(Date placedAt) {
		this.placedAt = placedAt;
	}
	public Date getSettledAt() {
		return settledAt;
	}
	public void setSettledAt(Date settledAt) {
		this.settledAt = settledAt;
	}
	public Integer getTotalStake() {
		return totalStake;
	}
	public void setTotalStake(Integer totalStake) {
		this.totalStake = totalStake;
	}
	public Integer getPayout() {
		return payout;
	}
	public void setPayout(Integer payout) {
		this.payout = payout;
	}
	public Integer getOperatorPayout() {
		return operatorPayout;
	}
	public void setOperatorPayout(Integer operatorPayout) {
		this.operatorPayout = operatorPayout;
	}
	public List<Caesars_Leg> getLegs() {
		return legs;
	}
	public void setLegs(List<Caesars_Leg> legs) {
		this.legs = legs;
	}
	public String getResultIndicator() {
		return resultIndicator;
	}
	public void setResultIndicator(String resultIndicator) {
		this.resultIndicator = resultIndicator;
	}
	public Integer getPreBonusPotentialReturns() {
		return preBonusPotentialReturns;
	}
	public void setPreBonusPotentialReturns(Integer preBonusPotentialReturns) {
		this.preBonusPotentialReturns = preBonusPotentialReturns;
	}
	public Caesars_Odds getEstimatedOdds() {
		return estimatedOdds;
	}
	public void setEstimatedOdds(Caesars_Odds estimatedOdds) {
		this.estimatedOdds = estimatedOdds;
	}
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	public Integer getFreebetStake() {
		return freebetStake;
	}
	public void setFreebetStake(Integer freebetStake) {
		this.freebetStake = freebetStake;
	}
	public Boolean getCashOut() {
		return cashOut;
	}
	public void setCashOut(Boolean cashOut) {
		this.cashOut = cashOut;
	}
	public String getWagerType() {
		return wagerType;
	}
	public void setWagerType(String wagerType) {
		this.wagerType = wagerType;
	}
	
}
