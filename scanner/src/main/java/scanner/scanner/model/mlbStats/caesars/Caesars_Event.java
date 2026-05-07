package scanner.scanner.model.mlbStats.caesars;

import java.util.Date;
import java.util.List;

public class Caesars_Event {

	private String                    id;
	private String                    name;
	private Date                      startTime;
	private List<Caesars_MarketGroup> keyMarketGroups;

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public List<Caesars_MarketGroup> getKeyMarketGroups() {
		return keyMarketGroups;
	}
	public void setKeyMarketGroups(List<Caesars_MarketGroup> keyMarketGroups) {
		this.keyMarketGroups = keyMarketGroups;
	}

}
