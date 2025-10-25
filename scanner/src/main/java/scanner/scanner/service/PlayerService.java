package scanner.scanner.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import scanner.scanner.exceptions.OddsException;
import scanner.scanner.model.Player;
import scanner.scanner.model.Team;
import scanner.scanner.model.Update;
import scanner.scanner.repo.PlayerRepo;

@Component
public class PlayerService {

	@Autowired
	protected PlayerRepo repo;

	public void setRepo(PlayerRepo repo) {
		this.repo = repo;
	}

	public void setUpdateService(UpdateService updateService) {
		this.updateService = updateService;
	}

	@Autowired
	protected UpdateService updateService;

	public Player getExistingPlayer(Team team, String commonName) throws OddsException {
		Player p = repo.getExistingPlayer(team, commonName);
		if(p == null) {
			throw new OddsException("Failed to find player: Team: " + team + ", Name: " + commonName);
		}
		return p;
	}

	public Player getPlayer(Team team, String sbSpecificPlayerName) throws OddsException {
		List<Player> players = repo.find(team, sbSpecificPlayerName);
		if((players == null) || (players.size() == 0)) {
			updateService.insert(new Update("players", team, sbSpecificPlayerName));
			throw new OddsException("Did not find player with name " + sbSpecificPlayerName + ", team: " + team);
		}
		return players.get(0);
	}

	public void insert(Player player) {
		repo.insert(player);
	}

	public List<Player> find(Team team, String p) {
		return repo.find(team, p);
	}

	
	
}
