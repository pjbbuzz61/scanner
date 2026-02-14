package scanner.scanner.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import scanner.scanner.model.history.Wager;
import scanner.scanner.repo.WagerRepo;

@Component
public class WagerService {

	@Autowired
	protected WagerRepo repo;
	
	public List<Wager> getWagers() {
		return repo.findAll();
	}

	public List<Wager> getWagers(Date start, Date stop) {
		return repo.find(start, stop);
	}

	public void insert(List<Wager> wagers) {
		repo.insert(wagers);
	}

	public void insert(Wager wager) {
		List<Wager> wagers = repo.find(wager.getBetNumber());
		if((wagers != null) && (wagers.size() > 0)) {
			System.out.println("Looks like this bet id already exists:");
			System.out.println("Bet trying to add: \n" + wager);
			System.out.println("Wager(s) already in the database: ");
			for(Wager w : wagers) {
				System.out.println(w);
			}
			return;
		} else {
			repo.insert(wager);
		}
	}

	public void setWagerRepo(WagerRepo wr) {
		repo = wr;
	}
}
