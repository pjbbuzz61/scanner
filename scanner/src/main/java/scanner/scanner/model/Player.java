package scanner.scanner.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "players")
public class Player {

	@Id
    private String id;

	private Team       team;
	private String     commonName;
	private String     nameSbSpecific;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commonName == null) ? 0 : commonName.hashCode());
		result = prime * result + ((team == null) ? 0 : team.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Player other = (Player) obj;
		if (commonName == null) {
			if (other.commonName != null)
				return false;
		} else if (!commonName.equals(other.commonName))
			return false;
		if (team == null) {
			if (other.team != null)
				return false;
		} else if (!team.equals(other.team))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("team: " + team + ", ");
		sb.append("commonName: " + commonName + ", ");
		sb.append("nameSbSpecific: " + nameSbSpecific + ", ");
		return sb.toString();
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Team getTeam() {
		return team;
	}
	public void setTeam(Team team) {
		this.team = team;
	}
	public String getCommonName() {
		return commonName;
	}
	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}
	public String getNameSbSpecific() {
		return nameSbSpecific;
	}
	public void setNameSbSpecific(String nameSbSpecific) {
		this.nameSbSpecific = nameSbSpecific;
	}

}
