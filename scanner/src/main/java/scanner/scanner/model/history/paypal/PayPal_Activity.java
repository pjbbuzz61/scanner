package scanner.scanner.model.history.paypal;

import java.util.List;

public class PayPal_Activity {
	private List<PayPal_Transactions> transactions;

	public List<PayPal_Transactions> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<PayPal_Transactions> transactions) {
		this.transactions = transactions;
	}
}
