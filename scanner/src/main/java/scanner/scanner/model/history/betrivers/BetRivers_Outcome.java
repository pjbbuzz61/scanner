package scanner.scanner.model.history.betrivers;

public class BetRivers_Outcome {

	private Boolean earlySettlement;
	private Long outcomeId;
	private BetRivers_EventInfo eventInfo;
	private String label;
	private BetRivers_SettledInfo settledInfo;
	private String status;
	private Long participantId;
	private BetRivers_BetOffer betOffer;
	private BetRivers_text title;
	
	
	public Boolean getEarlySettlement() {
		return earlySettlement;
	}
	public void setEarlySettlement(Boolean earlySettlement) {
		this.earlySettlement = earlySettlement;
	}
	public Long getOutcomeId() {
		return outcomeId;
	}
	public void setOutcomeId(Long outcomeId) {
		this.outcomeId = outcomeId;
	}
	public BetRivers_EventInfo getEventInfo() {
		return eventInfo;
	}
	public void setEventInfo(BetRivers_EventInfo eventInfo) {
		this.eventInfo = eventInfo;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public BetRivers_SettledInfo getSettledInfo() {
		return settledInfo;
	}
	public void setSettledInfo(BetRivers_SettledInfo settledInfo) {
		this.settledInfo = settledInfo;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Long getParticipantId() {
		return participantId;
	}
	public void setParticipantId(Long participantId) {
		this.participantId = participantId;
	}
	public BetRivers_BetOffer getBetOffer() {
		return betOffer;
	}
	public void setBetOffer(BetRivers_BetOffer betOffer) {
		this.betOffer = betOffer;
	}
	public BetRivers_text getTitle() {
		return title;
	}
	public void setTitle(BetRivers_text title) {
		this.title = title;
	}

}
