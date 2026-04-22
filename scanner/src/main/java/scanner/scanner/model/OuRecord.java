package scanner.scanner.model;


public class OuRecord {

	private String name;
	private Double points;
	private Integer ml;
	private OU ou;
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public OU getOu() {
		return ou;
	}
	public void setOu(OU ou) {
		this.ou = ou;
	}
	public Double getPoints() {
		return points;
	}
	public void setPoints(Double points) {
		this.points = points;
	}
	public Integer getMl() {
		return ml;
	}
	public void setMl(Integer ml) {
		this.ml = ml;
	}
	
}
