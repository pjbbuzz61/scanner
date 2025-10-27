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
		return String.format(
				"%-16s at %-16s: %-8s: %-14s at %5.1f/%-4d for $%-7.2f vs %-8s: %-14s at %5.1f/%-4d for $%-7.2f to win %-7.2f",  
				src.getAway().getCommonName(), src.getHome().getCommonName(),
				srcBook, srcPlayType, srcPts, srcML, srcBetAmt, 
				tgtBook, tgtPlayType, tgtPts, tgtML, tgtBetAmt,
				performance);
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
