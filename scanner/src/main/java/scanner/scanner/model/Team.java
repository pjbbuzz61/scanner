package scanner.scanner.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

@Document(collection = "teams")
public class Team {

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commonName == null) ? 0 : commonName.hashCode());
		result = prime * result + ((sport == null) ? 0 : sport.hashCode());
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
		Team other = (Team) obj;
		if (commonName == null) {
			if (other.commonName != null)
				return false;
		} else if (!commonName.equals(other.commonName))
			return false;
		if (sport != other.sport)
			return false;
		return true;
	}
	@Id
    private String id;

    private Sportsbook book;
	private Sport      sport;
	private String     commonName;
	private String     nameSbSpecific;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("id: " + id + ", ");
		sb.append("Book: " + book + ", ");
		sb.append("sport: " + sport + ", ");
		sb.append("commonName: " + commonName + ", ");
		sb.append("nameSbSpecific: " + nameSbSpecific + "\n");
		return sb.toString();
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Sportsbook getBook() {
		return book;
	}
	public void setBook(Sportsbook book) {
		this.book = book;
	}
	public Sport getSport() {
		return sport;
	}
	public void setSport(Sport sport) {
		this.sport = sport;
	}
	public String getNameSbSpecific() {
		return nameSbSpecific;
	}
	public void setNameSbSpecific(String nameSbSpecific) {
		this.nameSbSpecific = nameSbSpecific;
	}
	public String getCommonName() {
		return commonName;
	}
	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}
	
	
	
}
