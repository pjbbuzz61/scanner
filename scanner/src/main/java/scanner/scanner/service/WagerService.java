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
	
	public List<Wager> getWagers(String collection) {
		return repo.findAll(collection);
	}

	public List<Wager> getWagers() {
		return getWagers("wagers");
	}

	public List<Wager> getWagers(Date start, Date stop, String collection) {
		return repo.find(start, stop, collection);
	}

	public List<Wager> getWagers(Date start, Date stop) {
		return getWagers(start, stop, "wagers");
	}

	public void insert(Wager wager) {
		insert(wager, "wagers");
	}

	public boolean insert(Wager wager, String collection) {
		List<Wager> wagers = repo.find(wager.getBetNumber(), collection);
		if((wagers != null) && (wagers.size() > 0)) {
			System.out.println("Looks like this bet id already exists: BetNumber: " + wager.getBetNumber());
			return false;
		} else {
			repo.insert(wager, collection);
			return true;
		}
	}

	public void setWagerRepo(WagerRepo wr) {
		repo = wr;
	}
}
