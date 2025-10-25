package scanner.scanner.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import scanner.scanner.model.Odds;
import scanner.scanner.repo.OddsRepo;


@Component
public class OddsService {

    @Autowired
	private OddsRepo repo;

	public void persistOdds(Odds odds) {

		odds.setId(String.format("%d", odds.hashCode()));
		try {
			repo.save(odds);
		} catch(Exception e) {
			System.out.println("Exception persisting odds: " + e.getMessage());
		}
	}

	public void removeAll() {
		repo.removeAll();
	}

	public static void main(String args[]) {
		
		
		
	} //main

	public void setRepo(OddsRepo oRepo) {
		this.repo = oRepo;
	}

}
