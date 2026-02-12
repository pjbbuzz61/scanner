package scanner.scanner.model.history.betmgm;


public class BetMGM_PromoToken {

	private String         tokenType;
	private String         payoutType;
	private BetMGM_AddInfo additionalInformation;
	
	
	public String getTokenType() {
		return tokenType;
	}
	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}
	public String getPayoutType() {
		return payoutType;
	}
	public void setPayoutType(String payoutType) {
		this.payoutType = payoutType;
	}
	public BetMGM_AddInfo getAdditionalInformation() {
		return additionalInformation;
	}
	public void setAdditionalInformation(BetMGM_AddInfo additionalInformation) {
		this.additionalInformation = additionalInformation;
	}
}
