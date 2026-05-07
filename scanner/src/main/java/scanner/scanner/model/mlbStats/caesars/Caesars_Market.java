package scanner.scanner.model.mlbStats.caesars;

import java.util.List;

public class Caesars_Market {

	private Double                  line;
	private List<Caesars_Selection> selections;
	private Caesars_Metadata        metadata;
	
	
	public Double getLine() {
		return line;
	}
	public void setLine(Double line) {
		this.line = line;
	}
	public List<Caesars_Selection> getSelections() {
		return selections;
	}
	public void setSelections(List<Caesars_Selection> selections) {
		this.selections = selections;
	}
	public Caesars_Metadata getMetadata() {
		return metadata;
	}
	public void setMetadata(Caesars_Metadata metadata) {
		this.metadata = metadata;
	}
	
	
	
}
