package scanner.scanner.model.history.draftkings;

import java.util.Date;

public class DraftKings_TransactionDetail {

	private String Type;
	private Double EndingBalance;
	private String Description;
	private String LocationCode;
	private Date   CreateDate;
	private Double Amount;
	private DraftKings_KeyValue Category;
	private DraftKings_KeyValue Product;
	
	
	public String getType() {
		return Type;
	}
	public void setType(String type) {
		Type = type;
	}
	public Double getEndingBalance() {
		return EndingBalance;
	}
	public void setEndingBalance(Double endingBalance) {
		EndingBalance = endingBalance;
	}
	public String getDescription() {
		return Description;
	}
	public void setDescription(String description) {
		Description = description;
	}
	public String getLocationCode() {
		return LocationCode;
	}
	public void setLocationCode(String locationCode) {
		LocationCode = locationCode;
	}
	public Date getCreateDate() {
		return CreateDate;
	}
	public void setCreateDate(Date createDate) {
		CreateDate = createDate;
	}
	public Double getAmount() {
		return Amount;
	}
	public void setAmount(Double amount) {
		Amount = amount;
	}
	public DraftKings_KeyValue getCategory() {
		return Category;
	}
	public void setCategory(DraftKings_KeyValue category) {
		Category = category;
	}
	public DraftKings_KeyValue getProduct() {
		return Product;
	}
	public void setProduct(DraftKings_KeyValue product) {
		Product = product;
	}
	
}
