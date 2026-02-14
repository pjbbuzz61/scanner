package scanner.scanner.model.history.betrivers;

import java.util.List;

public class BetRivers_CouponRow {

	private Long outcomeId;
	private String status;
	private Integer playedOdds;
	private List<BetRivers_Outcome> outcomes;
	
	
	public Long getOutcomeId() {
		return outcomeId;
	}
	public void setOutcomeId(Long outcomeId) {
		this.outcomeId = outcomeId;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Integer getPlayedOdds() {
		return playedOdds;
	}
	public void setPlayedOdds(Integer playedOdds) {
		this.playedOdds = playedOdds;
	}
	public List<BetRivers_Outcome> getOutcomes() {
		return outcomes;
	}
	public void setOutcomes(List<BetRivers_Outcome> outcomes) {
		this.outcomes = outcomes;
	}
	
}
