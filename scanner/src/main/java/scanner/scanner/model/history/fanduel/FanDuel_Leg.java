package scanner.scanner.model.history.fanduel;

import java.util.List;

public class FanDuel_Leg {

	private String result;
	private List<FanDuel_Part> parts;
	
	
	public List<FanDuel_Part> getParts() {
		return parts;
	}
	public void setParts(List<FanDuel_Part> parts) {
		this.parts = parts;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	

}
