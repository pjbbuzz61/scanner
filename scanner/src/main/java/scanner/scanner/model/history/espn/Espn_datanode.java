package scanner.scanner.model.history.espn;

import java.util.Date;
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
