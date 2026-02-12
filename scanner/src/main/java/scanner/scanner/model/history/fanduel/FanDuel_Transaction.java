package scanner.scanner.model.history.fanduel;

import java.util.Date;

public class FanDuel_Transaction {
	private String  id;
	private String  currency;    // USD for real bets, PBT for token accounting (ignore)
	private Double  amount;
	private String  description;
	private Date    date_raised;
	private Date    date_completed;
	private String  transaction_type; 
		// BET: a wager
		// BONUS: still just looks like a wager
		// WINNINGS: proceeds from bets won 
		// WALLET_TRANSFER: moving of moneys, ignore
		// DEPOSIT: ignore
	
	private String  account_type;   
		// USER_FREE_BET: bonus bet
		// USER_PROFIT_BOOST: ignore these
		// USER_SPORTSBOOK_CASH: wager i make with my money
		// USER_SPORTSBOOK_DEPOSIT: ignore
	
	// NOTE: Bonus bets are handled by adding the bonus to my account, then removing it to make the bet.
	//  Effect is I have a bet with nothing taken out. Question is how is the winning bet handled? Guessing it
	//  has to have the stake removed
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Date getDate_raised() {
		return date_raised;
	}
	public void setDate_raised(Date date_raised) {
		this.date_raised = date_raised;
	}
	public Date getDate_completed() {
		return date_completed;
	}
	public void setDate_completed(Date date_completed) {
		this.date_completed = date_completed;
	}
	public String getTransaction_type() {
		return transaction_type;
	}
	public void setTransaction_type(String transaction_type) {
		this.transaction_type = transaction_type;
	}
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getAccount_type() {
		return account_type;
	}
	public void setAccount_type(String account_type) {
		this.account_type = account_type;
	}
	
}
