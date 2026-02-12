package scanner.scanner.model.history;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.WAGER_RESULT;
import scanner.scanner.util.history.STATES;

@Document(collection = "wagers")
public class Wager {

    @Id
    public String         id;              // make this the unique id from the book


    private Date          betTimestamp;    // when the bet was made
    private Date          eventTimestamp;  // time of the game
    private Date          payoutTimestamp; // when wager was completed

    private String        eventDesc;       // example: New York Knicks at Los Angeles Lakers, o140, -115
    private String        betNumber;       // book specific
    private String        betType;         // Single or Parley

    private WAGER_RESULT  result;          // WIN, LOSS, NO_DEC

    private Integer       original_odds;   // Total odds for the wager
    private Integer       boosted_odds;    // Total odds for the wager with any boost added

    private boolean       isBonus;         // stake is a bonus bet
    private boolean       isRiskFree;      // Bet has a No Sweat Token associated with it
    
    private Double        stake;
    private Double        totalReturn;     // stake plus winnings
  
    private String        sport;           // Football, Baseball, etc
    private String        league;          // NFL, NHL, NCAAF, etc

    private STATES        state;           // state where play was made - MD, VA, KS
    private Sportsbook    book;            // Sportsbook play was made

    
    @Override
    public String toString() {
    	
    	StringBuilder sb = new StringBuilder();

    	sb.append("betTimestamp: "    + betTimestamp + "\n");
    	sb.append("eventTimestamp: "  + eventTimestamp + "\n");
    	sb.append("payoutTimestamp: " + payoutTimestamp + "\n");
    	sb.append("eventDesc: "       + eventDesc + "\n");
    	sb.append("betNumber: "       + betNumber + "\n");
    	sb.append("betType: "         + betType + "\n");
    	sb.append("result: "          + result + "\n");
    	sb.append("original_odds: "   + original_odds + "\n");
    	sb.append("boosted_odds: "    + boosted_odds + "\n");
    	sb.append("isBonus: "         + isBonus + "\n");
    	sb.append("isRiskFree: "      + isRiskFree + "\n");
    	sb.append("stake: "           + stake + "\n");
    	sb.append("totalReturn: "     + totalReturn + "\n");
    	sb.append("sport: "           + sport + "\n");
    	sb.append("league: "          + league + "\n");
    	sb.append("state: "           + state + "\n");
    	sb.append("book: "            + book + "\n");
   
    	return sb.toString();
    }
    
    public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getEventDesc() {
		return eventDesc;
	}
	public void setEventDesc(String eventDesc) {
		this.eventDesc = eventDesc;
	}
	public Date getBetTimestamp() {
		return betTimestamp;
	}
	public void setBetTimestamp(Date betTimestamp) {
		this.betTimestamp = betTimestamp;
	}
	public Date getEventTimestamp() {
		return eventTimestamp;
	}
	public void setEventTimestamp(Date eventTimestamp) {
		this.eventTimestamp = eventTimestamp;
	}
	public Date getPayoutTimestamp() {
		return payoutTimestamp;
	}
	public void setPayoutTimestamp(Date payoutTimestamp) {
		this.payoutTimestamp = payoutTimestamp;
	}
	public String getBetType() {
		return betType;
	}
	public void setBetType(String betType) {
		this.betType = betType;
	}
	public boolean isBonus() {
		return isBonus;
	}
	public void setBonus(boolean isBonus) {
		this.isBonus = isBonus;
	}
	public String getBetNumber() {
		return betNumber;
	}
	public void setBetNumber(String betNumber) {
		this.betNumber = betNumber;
	}
	public WAGER_RESULT getResult() {
		return result;
	}
	public void setResult(WAGER_RESULT payout) {
		this.result = payout;
	}
	public Double getStake() {
		return stake;
	}
	public void setStake(Double stake) {
		this.stake = stake;
	}
	public String getSport() {
		return sport;
	}
	public void setSport(String sport) {
		this.sport = sport;
	}
	public String getLeague() {
		return league;
	}
	public void setLeague(String league) {
		this.league = league;
	}
	public STATES getState() {
		return state;
	}
	public void setState(STATES state) {
		this.state = state;
	}
	public Sportsbook getBook() {
		return book;
	}
	public void setBook(Sportsbook book) {
		this.book = book;
	}
	public Double getTotalReturn() {
		return totalReturn;
	}
	public void setTotalReturn(Double totalReturn) {
		this.totalReturn = totalReturn;
	}
	public Integer getOriginal_odds() {
		return original_odds;
	}
	public void setOriginal_odds(Integer original_odds) {
		this.original_odds = original_odds;
	}
	public Integer getBoosted_odds() {
		return boosted_odds;
	}
	public void setBoosted_odds(Integer boosted_odds) {
		this.boosted_odds = boosted_odds;
	}
	public boolean isRiskFree() {
		return isRiskFree;
	}
	public void setRiskFree(boolean isRiskFree) {
		this.isRiskFree = isRiskFree;
	}
    
    
    
    

}
