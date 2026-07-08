package scanner.scanner.model;

import scanner.scanner.util.PlayType;
import scanner.scanner.util.Sportsbook;

public class Play {

	private Sportsbook srcBook;
	private Sportsbook tgtBook;
	private Double     srcBetAmt;
	private Double     srcRtnAmt;
	private Double     tgtBetAmt;
	private Double     tgtRtnAmt;
	private int        srcML;
	private double     srcPts;
	private int        tgtML;
	private double     tgtPts;
	private PlayType   srcPlayType;
	private PlayType   tgtPlayType;
	private double     performance;
	private Odds       src;
	private Odds       tgt;
	

	@Override
	public String toString() {

		String srcPlay = srcPlayType.toString()
				.replace("HOME_SPREAD",    "H_SPD")
				.replace("HOME_MONEYLINE", "H_ML")
				.replace("AWAY_SPREAD",    "A_SPD")
				.replace("AWAY_MONEYLINE", "A_ML")
				.replace("OVER",           "OVER")
				.replace("UNDER",          "UNDER");
		String tgtPlay = tgtPlayType.toString()
				.replace("HOME_SPREAD",    "H_SPD")
				.replace("HOME_MONEYLINE", "H_ML")
				.replace("AWAY_SPREAD",    "A_SPD")
				.replace("AWAY_MONEYLINE", "A_ML")
				.replace("OVER",           "OVER")
				.replace("UNDER",          "UNDER");
		String sBook = srcBook.toString()
				.replace("DRAFTKINGS", "DRAFT")
				.replace("BETMGM",     "MGM")
				.replace("CAESARS",    "CAES")
				.replace("FANDUEL",    "FAND")
				.replace("ESPN",       "ESPN")
				.replace("BETRIVERS",  "RIVERS");
		String tBook = tgtBook.toString()
				.replace("DRAFTKINGS", "DRAFT")
				.replace("BETMGM",     "MGM")
				.replace("CAESARS",    "CAES")
				.replace("FANDUEL",    "FAND")
				.replace("ESPN",       "ESPN")
				.replace("BETRIVERS",  "RIVERS");
		
		return String.format(
				"%-28s at %-28s: %-6s: %-5s at %5.1f/%-6d for $%-7.2f vs %-6s: %-5s at %5.1f/%-6d for $%-7.2f to win %-7.2f %7s %s",  
				src.getAway().getCommonName(), src.getHome().getCommonName(),
				sBook, srcPlay, srcPts, srcML, srcBetAmt, 
				tBook, tgtPlay, tgtPts, tgtML, tgtBetAmt,
				performance, 
				src.getMlbStat()==null?" ":src.getMlbStat(),
				src.getPlayer1()==null?" ":src.getPlayer1().getCommonName()
				);
//				100.0*performance/(srcBetAmt+tgtBetAmt));
	}

	public String toStringForEmailSubject() {
		return 
				src.getPlayer1().getCommonName() + ", " +  
				src.getAway().getCommonName();
	}

	public String toStringForEmailBody() {

		String srcPlay = srcPlayType.toString()
				.replace("HOME_SPREAD",    "H_SPD")
				.replace("HOME_MONEYLINE", "H_ML")
				.replace("AWAY_SPREAD",    "A_SPD")
				.replace("AWAY_MONEYLINE", "A_ML")
				.replace("OVER",           "OVER")
				.replace("UNDER",          "UNDER");
		String tgtPlay = tgtPlayType.toString()
				.replace("HOME_SPREAD",    "H_SPD")
				.replace("HOME_MONEYLINE", "H_ML")
				.replace("AWAY_SPREAD",    "A_SPD")
				.replace("AWAY_MONEYLINE", "A_ML")
				.replace("OVER",           "OVER")
				.replace("UNDER",          "UNDER");
		String sBook = srcBook.toString()
				.replace("DRAFTKINGS", "DK")
				.replace("BETMGM",     "BM")
				.replace("CAESARS",    "CAES")
				.replace("FANDUEL",    "FD")
				.replace("ESPN",       "ESPN")
				.replace("BETRIVERS",  "BR");
		String tBook = tgtBook.toString()
				.replace("DRAFTKINGS", "DK")
				.replace("BETMGM",     "BM")
				.replace("CAESARS",    "CAES")
				.replace("FANDUEL",    "FD")
				.replace("ESPN",       "ESPN")
				.replace("BETRIVERS",  "BR");

		return String.format(
				"%-6s: %-5s @ %5.1f/%-6d: $%-7.2f\n%-6s: %-5s @ %5.1f/%-6d: $%-7.2f\nWin: %-7.2f\n%7s",  
				sBook, srcPlay.replace("OVER", "OV").replace("UNDER", "UN"), srcPts, srcML, srcBetAmt, 
				tBook, tgtPlay.replace("OVER", "OV").replace("UNDER", "UN"), tgtPts, tgtML, tgtBetAmt,
				performance, 
				src.getMlbStat()==null?" ":src.getMlbStat()
				);
	}

	public String toStringForTextBody() {

		String srcPlay = srcPlayType.toString()
				.replace("HOME_SPREAD",    "H_SPD")
				.replace("HOME_MONEYLINE", "H_ML")
				.replace("AWAY_SPREAD",    "A_SPD")
				.replace("AWAY_MONEYLINE", "A_ML")
				.replace("OVER",           "OVER")
				.replace("UNDER",          "UNDER");
		String tgtPlay = tgtPlayType.toString()
				.replace("HOME_SPREAD",    "H_SPD")
				.replace("HOME_MONEYLINE", "H_ML")
				.replace("AWAY_SPREAD",    "A_SPD")
				.replace("AWAY_MONEYLINE", "A_ML")
				.replace("OVER",           "OVER")
				.replace("UNDER",          "UNDER");
		String sBook = srcBook.toString()
				.replace("DRAFTKINGS", "DK")
				.replace("BETMGM",     "BM")
				.replace("CAESARS",    "CAES")
				.replace("FANDUEL",    "FD")
				.replace("ESPN",       "ESPN")
				.replace("BETRIVERS",  "BR");
		String tBook = tgtBook.toString()
				.replace("DRAFTKINGS", "DK")
				.replace("BETMGM",     "BM")
				.replace("CAESARS",    "CAES")
				.replace("FANDUEL",    "FD")
				.replace("ESPN",       "ESPN")
				.replace("BETRIVERS",  "BR");

		return String.format(
				"%s:%s@%.1f/%d:$%.2f%s:%s@%.1f/%d:$%.2f::Win:%.2f%s",  
				sBook, srcPlay.replace("OVER", "OV").replace("UNDER", "UN"), srcPts, srcML, srcBetAmt, 
				tBook, tgtPlay.replace("OVER", "OV").replace("UNDER", "UN"), tgtPts, tgtML, tgtBetAmt,
				performance, 
				src.getMlbStat()==null?" ":src.getMlbStat()
				);
	}

	public Odds getSrc() {
		return src;
	}

	public void setSrc(Odds src) {
		this.src = src;
	}

	public Odds getTgt() {
		return tgt;
	}

	public void setTgt(Odds tgt) {
		this.tgt = tgt;
	}

	public Double getSrcBetAmt() {
		return srcBetAmt;
	}

	public void setSrcBetAmt(Double srcBetAmt) {
		this.srcBetAmt = srcBetAmt;
	}

	public Double getSrcRtnAmt() {
		return srcRtnAmt;
	}

	public void setSrcRtnAmt(Double srcRtnAmt) {
		this.srcRtnAmt = srcRtnAmt;
	}

	public Double getTgtBetAmt() {
		return tgtBetAmt;
	}

	public void setTgtBetAmt(Double tgtBetAmt) {
		this.tgtBetAmt = tgtBetAmt;
	}

	public Double getTgtRtnAmt() {
		return tgtRtnAmt;
	}

	public void setTgtRtnAmt(Double tgtRtnAmt) {
		this.tgtRtnAmt = tgtRtnAmt;
	}

	public PlayType getSrcPlayType() {
		return srcPlayType;
	}

	public void setSrcPlayType(PlayType srcPlayType) {
		this.srcPlayType = srcPlayType;
	}

	public PlayType getTgtPlayType() {
		return tgtPlayType;
	}

	public void setTgtPlayType(PlayType tgtPlayType) {
		this.tgtPlayType = tgtPlayType;
	}

	public double getPerformance() {
		return performance;
	}

	public void setPerformance(double performance) {
		this.performance = performance;
	}

	public Sportsbook getSrcBook() {
		return srcBook;
	}

	public void setSrcBook(Sportsbook srcBook) {
		this.srcBook = srcBook;
	}

	public Sportsbook getTgtBook() {
		return tgtBook;
	}

	public void setTgtBook(Sportsbook tgtBook) {
		this.tgtBook = tgtBook;
	}

	public int getSrcML() {
		return srcML;
	}

	public void setSrcML(int srcML) {
		this.srcML = srcML;
	}

	public double getSrcPts() {
		return srcPts;
	}

	public void setSrcPts(double srcPts) {
		this.srcPts = srcPts;
	}

	public int getTgtML() {
		return tgtML;
	}

	public void setTgtML(int tgtML) {
		this.tgtML = tgtML;
	}

	public double getTgtPts() {
		return tgtPts;
	}

	public void setTgtPts(double tgtPts) {
		this.tgtPts = tgtPts;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
