package scanner.scanner.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import scanner.scanner.util.MLB_STAT;

@Document(collection = "playMade")
public class PlayMade {

    @Id
    public String id;

    private int      julianDate;
    private String   player;
    private MLB_STAT mlbStat;
    
    
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public int getJulianDate() {
		return julianDate;
	}
	public void setJulianDate(int julianDate) {
		this.julianDate = julianDate;
	}
	public String getPlayer() {
		return player;
	}
	public void setPlayer(String player) {
		this.player = player;
	}
	public MLB_STAT getMlbStat() {
		return mlbStat;
	}
	public void setMlbStat(MLB_STAT mlbStat) {
		this.mlbStat = mlbStat;
	}
    
    
    
}
