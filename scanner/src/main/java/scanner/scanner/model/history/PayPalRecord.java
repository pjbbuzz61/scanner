package scanner.scanner.model.history;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "paypal")
public class PayPalRecord {

	@Id
	private String id;
	
	private String site;
	private String date;
	private String type;
	private Double amount;
	
	@Override
	public String toString() {
	
		StringBuilder sb = new StringBuilder();
		sb.append("ID:     " + id + "\n");
		sb.append("Site:   " + site + "\n");
		sb.append("Date:   " + date + "\n");
		sb.append("Type:   " + type + "\n");
		sb.append("Amount: " + amount);
		return sb.toString();
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getSite() {
		return site;
	}
	public void setSite(String site) {
		this.site = site;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	
}
