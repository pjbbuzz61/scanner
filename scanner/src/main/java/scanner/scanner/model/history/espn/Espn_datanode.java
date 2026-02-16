package scanner.scanner.model.history.espn;

import java.util.Date;
import java.util.List;
//u
public class Espn_datanode {

	private String   transactionId;
	private String   label;
	private Date     date;
	private String   region;
	private Espn_Amt amount;
	private String   type;
	private String   awardType;
	private String   awardDescription;
	
	private String rawId;
	private String outcome;
	private Date placedAt;
	private Date closedAt;
	private String amountSourceType;
	private Espn_Amt betAmount;
	private Espn_Amt payoutAmount;
	private Espn_Odds totalOdds;
	private List<Espn_EventGrouping> legEventGroupings;
	
	
	
	
	
	public String getRawId() {
		return rawId;
	}
	public void setRawId(String rawId) {
		this.rawId = rawId;
	}
	public String getOutcome() {
		return outcome;
	}
	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}
	public Date getPlacedAt() {
		return placedAt;
	}
	public void setPlacedAt(Date placedAt) {
		this.placedAt = placedAt;
	}
	public Date getClosedAt() {
		return closedAt;
	}
	public void setClosedAt(Date closedAt) {
		this.closedAt = closedAt;
	}
	public String getAmountSourceType() {
		return amountSourceType;
	}
	public void setAmountSourceType(String amountSourceType) {
		this.amountSourceType = amountSourceType;
	}
	public Espn_Amt getBetAmount() {
		return betAmount;
	}
	public void setBetAmount(Espn_Amt betAmount) {
		this.betAmount = betAmount;
	}
	public Espn_Amt getPayoutAmount() {
		return payoutAmount;
	}
	public void setPayoutAmount(Espn_Amt payoutAmount) {
		this.payoutAmount = payoutAmount;
	}
	public Espn_Odds getTotalOdds() {
		return totalOdds;
	}
	public void setTotalOdds(Espn_Odds totalOdds) {
		this.totalOdds = totalOdds;
	}
	public List<Espn_EventGrouping> getLegEventGroupings() {
		return legEventGroupings;
	}
	public void setLegEventGroupings(List<Espn_EventGrouping> legEventGroupings) {
		this.legEventGroupings = legEventGroupings;
	}
	public String getTransactionId() {
		return transactionId;
	}
	public void setTransactionId(String transactionId) {
		this.transactionId = transactionId;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getRegion() {
		return region;
	}
	public void setRegion(String region) {
		this.region = region;
	}
	public Espn_Amt getAmount() {
		return amount;
	}
	public void setAmount(Espn_Amt amount) {
		this.amount = amount;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getAwardType() {
		return awardType;
	}
	public void setAwardType(String awardType) {
		this.awardType = awardType;
	}
	public String getAwardDescription() {
		return awardDescription;
	}
	public void setAwardDescription(String awardDescription) {
		this.awardDescription = awardDescription;
	}
	
}
