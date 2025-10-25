package scanner.scanner.model;

import scanner.scanner.util.Period;

public class OU {
	
	private Period period;
	private Double points;
	private Integer over;
	private Integer under;
	private Integer push;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("period: " + period + ", ");
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
		result = prime * result + ((period == null) ? 0 : period.hashCode());
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
		OU other = (OU) obj;
		if (period == null) {
			if (other.period != null)
				return false;
		} else if (!period.equals(other.period))
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


	public boolean isValid() {
		return (points != null) && (over != null) && (under != null);
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

	public Integer getPush() {
		return push;
	}

	public void setPush(Integer push) {
		this.push = push;
	}

	public Period getPeriod() {
		return period;
	}

	public void setPeriod(Period period) {
		this.period = period;
	}

}
