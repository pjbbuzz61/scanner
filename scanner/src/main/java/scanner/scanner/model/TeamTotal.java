package scanner.scanner.model;

public class TeamTotal {

	private Double  points;
	private Integer over;
	private Integer under;
	private Integer push;
	private Boolean home;
	
	public boolean isValid() {
		return (points != null) && (over != null) && (under != null);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("points: " + points + ", ");
		sb.append("over: " + over + ", ");
		sb.append("under: " + under + ", ");
		if(push != null) sb.append("push: " + push + ", ");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((home == null) ? 0 : home.hashCode());
		result = prime * result + ((over == null) ? 0 : over.hashCode());
		result = prime * result + ((points == null) ? 0 : points.hashCode());
		result = prime * result + ((push == null) ? 0 : push.hashCode());
		result = prime * result + ((under == null) ? 0 : under.hashCode());
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
		TeamTotal other = (TeamTotal) obj;
		if (home == null) {
			if (other.home != null)
				return false;
		} else if (!home.equals(other.home))
			return false;
		if (over == null) {
			if (other.over != null)
				return false;
		} else if (!over.equals(other.over))
			return false;
		if (points == null) {
			if (other.points != null)
				return false;
		} else if (!points.equals(other.points))
			return false;
		if (push == null) {
			if (other.push != null)
				return false;
		} else if (!push.equals(other.push))
			return false;
		if (under == null) {
			if (other.under != null)
				return false;
		} else if (!under.equals(other.under))
			return false;
		return true;
	}

	public Double getPoints() {
		return points;
	}
	public void setPoints(Double points) {
		this.points = points;
	}
	public Integer getOver() {
		return over;
	}
	public void setOver(Integer over) {
		this.over = over;
	}
	public Integer getUnder() {
		return under;
	}
	public void setUnder(Integer under) {
		this.under = under;
	}
	public Boolean getHome() {
		return home;
	}
	public void setHome(Boolean home) {
		this.home = home;
	}

	public Integer getPush() {
		return push;
	}

	public void setPush(Integer push) {
		this.push = push;
	}

}
