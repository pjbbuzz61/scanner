package scanner.scanner.model;

import scanner.scanner.util.Period;

public class Spread {
	
	private Period  period;
	private Double  homePoints;  // home team or player 2 in tennis
	private Double  awayPoints;  // away team of player 1 in tennis
	private Integer homePrice;
	private Integer awayPrice;
	private Integer pushPrice;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("period:     " + period + ", ");
		sb.append("homePoints: " + homePoints + ", ");
		sb.append("awayPoints: " + awayPoints + ", ");
		sb.append("homePrice:  " + homePrice + ", ");
		sb.append("awayPrice:  " + awayPrice + ", ");
		if(pushPrice != null) sb.append("pushPrice: " + pushPrice + ", ");
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((period == null) ? 0 : period.hashCode());
		result = prime * result + ((awayPoints == null) ? 0 : awayPoints.hashCode());
		result = prime * result + ((awayPrice == null) ? 0 : awayPrice.hashCode());
		result = prime * result + ((homePoints == null) ? 0 : homePoints.hashCode());
		result = prime * result + ((homePrice == null) ? 0 : homePrice.hashCode());
		result = prime * result + ((pushPrice == null) ? 0 : pushPrice.hashCode());
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
		Spread other = (Spread) obj;
		if (period == null) {
			if (other.period != null)
				return false;
		} else if (!period.equals(other.period))
			return false;
		if (awayPoints == null) {
			if (other.awayPoints != null)
				return false;
		} else if (!awayPoints.equals(other.awayPoints))
			return false;
		if (awayPrice == null) {
			if (other.awayPrice != null)
				return false;
		} else if (!awayPrice.equals(other.awayPrice))
			return false;
		if (homePoints == null) {
			if (other.homePoints != null)
				return false;
		} else if (!homePoints.equals(other.homePoints))
			return false;
		if (homePrice == null) {
			if (other.homePrice != null)
				return false;
		} else if (!homePrice.equals(other.homePrice))
			return false;
		if (pushPrice == null) {
			if (other.pushPrice != null)
				return false;
		} else if (!pushPrice.equals(other.pushPrice))
			return false;
		return true;
	}

	public boolean isValid() {
		return (homePoints != null) && (awayPoints != null) && (homePrice != null) && (awayPrice != null);
	}

	public Integer getHomePrice() {
		return homePrice;
	}
	public void setHomePrice(Integer homePrice) {
		this.homePrice = homePrice;
	}
	public Integer getAwayPrice() {
		return awayPrice;
	}
	public void setAwayPrice(Integer awayPrice) {
		this.awayPrice = awayPrice;
	}
	public Double getHomePoints() {
		return homePoints;
	}
	public void setHomePoints(Double homePoints) {
		this.homePoints = homePoints;
	}
	public Double getAwayPoints() {
		return awayPoints;
	}
	public void setAwayPoints(Double awayPoints) {
		this.awayPoints = awayPoints;
	}

	public Integer getPushPrice() {
		return pushPrice;
	}

	public void setPushPrice(Integer pushPrice) {
		this.pushPrice = pushPrice;
	}

	public Period getPeriod() {
		return period;
	}

	public void setPeriod(Period period) {
		this.period = period;
	}

}
