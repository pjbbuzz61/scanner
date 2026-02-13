package scanner.scanner.model.history.draftkings;

import java.util.List;

public class DraftKings_Transaction {

	private String PublicTransactionKey;
	private List<DraftKings_TransactionDetail> TransactionDetails;

	
	public List<DraftKings_TransactionDetail> getTransactionDetails() {
		return TransactionDetails;
	}

	public void setTransactionDetails(List<DraftKings_TransactionDetail> transactionDetails) {
		TransactionDetails = transactionDetails;
	}

	public String getPublicTransactionKey() {
		return PublicTransactionKey;
	}

	public void setPublicTransactionKey(String publicTransactionKey) {
		PublicTransactionKey = publicTransactionKey;
	}
}
