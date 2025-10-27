package scanner.scanner.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import scanner.scanner.model.Odds;
import scanner.scanner.repo.OddsRepo;
import scanner.scanner.util.Period;
import scanner.scanner.util.Sport;


@Component
public class OddsService {

    @Autowired
	private OddsRepo repo;


    public List<Odds> getOdds(Sport sport, Period period) {
		return repo.getOdds(sport, period, "odds" + "_" + sport);
	}

	public void persistOdds(Odds odds) {

		odds.setId(String.format("%d", odds.hashCode()));
		try {
			repo.save(odds);
		} catch(Exception e) {
			System.out.println("Exception persisting odds: " + e.getMessage());
		}
	}

	public void persistOdds(Odds odds, String collection) {

		odds.setId(String.format("%d", odds.hashCode()));
		try {
			repo.save(odds, collection);
		} catch(Exception e) {
			System.out.println("Exception persisting odds: " + e.getMessage());
		}
	}

	public void removeAll() {
		repo.removeAll();
	}

	public void removeAll(Sport sport) {
		repo.removeAll(sport);
	}

	public static void main(String args[]) {
		
		
		
	} //main

	public void setRepo(OddsRepo oRepo) {
		this.repo = oRepo;
	}

}
