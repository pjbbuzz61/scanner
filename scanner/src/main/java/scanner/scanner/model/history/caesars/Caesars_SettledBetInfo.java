package scanner.scanner.model.history.caesars;

import java.util.Date;

public class Caesars_SettledBetInfo {

	private Date    settledAt;
	private Boolean paidOut;
	private Integer payout;
	private Boolean showVoidedAdjustmentMessage;
	private String  result;
	
	
	public Date getSettledAt() {
		return settledAt;
	}
	public void setSettledAt(Date settledAt) {
		this.settledAt = settledAt;
	}
	public Boolean getPaidOut() {
		return paidOut;
	}
	public void setPaidOut(Boolean paidOut) {
		this.paidOut = paidOut;
	}
	public Integer getPayout() {
		return payout;
	}
	public void setPayout(Integer payout) {
		this.payout = payout;
	}
	public Boolean getShowVoidedAdjustmentMessage() {
		return showVoidedAdjustmentMessage;
	}
	public void setShowVoidedAdjustmentMessage(Boolean showVoidedAdjustmentMessage) {
		this.showVoidedAdjustmentMessage = showVoidedAdjustmentMessage;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	
}
