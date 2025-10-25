package scanner.scanner.service;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClients;

import scanner.scanner.exceptions.OddsException;
import scanner.scanner.model.Team;
import scanner.scanner.model.Update;
import scanner.scanner.repo.TeamRepo;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

@Component
public class TeamService {

	@Autowired
	protected TeamRepo repo;

	@Autowired
	protected UpdateService updateService;
	
	public Team getTeam(Sportsbook book, Sport sport, String sbSpecificTeamName, boolean flagToUpdates) throws OddsException {
		List<Team> teams = repo.find(book, sport, sbSpecificTeamName);
		if((teams == null) || (teams.size() == 0)) {
			if(flagToUpdates) {
				updateService.insert(new Update("team", book, sport, sbSpecificTeamName));
			}
			throw new OddsException("Did not find team with name " + sbSpecificTeamName + ", book: " + book + ", sport: " + sport);
		}
		return teams.get(0);
	}

	public List<Team> getTeams(Sport sport) {
		return repo.findAllForSport(sport);
	}
	
	public Team getTeam(Sportsbook book, Sport sport, String commonName) throws OddsException {
		Team t = repo.getTeam(book, sport, commonName);
		if(t == null) throw new OddsException("Failed to find team: Sport: " + sport + ", Name: " + commonName);
		return t; 
	}

	public static void main(String [] args) {
		TeamService srv = new TeamService();
		MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "scanner");

		TeamRepo ur = new TeamRepo();
		ur.setMongoTemplate(mongoTemplate);
		srv.repo = ur;
		
		for(Sport sport : Sport.values()) {
			Map<String, Integer> map = new HashMap<String, Integer>();
			List<Team> teams = srv.getTeams(sport);
			for(Team t : teams) {
				if(map.get(t.getCommonName()) == null) {
					map.put(t.getCommonName(), 0);
				}
				map.put(t.getCommonName(), map.get(t.getCommonName())+1);
			}

	        for (Map.Entry<String,Integer> entry : map.entrySet()) {
	        	if(entry.getValue() < 4) {
	        		System.out.println("Sport: " + sport + ", CommonName: " + entry.getKey() + " Entries: " + entry.getValue());
	        	}
	        }

		}

	}
	
	public void setTeamRepo(TeamRepo repo) {
		this.repo = repo;
	}
	public void setUpdateService(UpdateService srv) {
		this.updateService = srv;
	}
	
}
