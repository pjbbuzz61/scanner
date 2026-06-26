package scanner.scanner.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClients;

import scanner.scanner.exceptions.OddsException;
import scanner.scanner.model.Player;
import scanner.scanner.model.Team;
import scanner.scanner.model.Update;
import scanner.scanner.repo.PlayerRepo;
import scanner.scanner.repo.TeamRepo;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;
import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;
import uk.ac.shef.wit.simmetrics.similaritymetrics.OverlapCoefficient;
import uk.ac.shef.wit.simmetrics.similaritymetrics.SmithWaterman;

@Component
public class PlayerService {

	@Autowired
	protected PlayerRepo repo;

	@Autowired
	protected TeamRepo teamRepo;

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

	// Use this to guess which one of two teams a player is on ...
	// Using this for ESPN, where they don't have the players team in the MLB_STATS data
	public Object getPlayer(List<Team> teams, String sbSpecificPlayerName) throws OddsException {

		for(Team team : teams) {
			List<Player> players = repo.find(team, sbSpecificPlayerName);
			if((players != null) && (players.size() > 0)) {
				return players.get(0);
			}
		}

		// If we're here then player isn't listed on any team on the list
		// Make a list of all players on the list of team and see which one matches closest
		List<Player> allPlayers = new ArrayList<>();
		for(Team team : teams) {
			allPlayers.addAll(repo.find(team));
		}
		
		Player candidate = null;
		float bestScore = 0;
		for(Player p : allPlayers) {
			float score = UpdateService.getWeight(sbSpecificPlayerName, p.getCommonName());
			if(score > bestScore) {
				bestScore = score;
				candidate = p;
			}
		}
		
		return candidate.getTeam();
	}

	public void insert(Player player) {
		repo.insert(player);
	}

	public List<Player> find(Team team, String p) {
		return repo.find(team, p);
	}

	public static void main(String args[]) {
		PlayerService service = new PlayerService();
	    Scanner scanner = new Scanner(System.in);

		MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "scanner");

		PlayerRepo r = new PlayerRepo();
		r.setMongoTemplate(mongoTemplate);
		service.repo = r;

		TeamRepo tr = new TeamRepo();
		tr.setMongoTemplate(mongoTemplate);
		service.teamRepo = tr;

//		service.compareMlbStatsToEspnRef();
//		System.exit(0);
		
		List<Player> refPlayerList = r.getRefPlayerList();
		
		List<Player> players = r.findAll();
		Map<String, List<Player>> theMap = new HashMap<>();
		
		for(Player p : players) {
			if(theMap.get(p.getCommonName()) == null) {
				List<Player> l = new ArrayList<>();
				l.add(p);
				theMap.put(p.getCommonName(), l);
			} else {
				List<Player> l = theMap.get(p.getCommonName());
				l.add(p);
				theMap.put(p.getCommonName(), l);
			}
		} // for all players
		
		List<Team> allTeams = service.teamRepo.findAllForSport(Sport.MLB_STATS, Sportsbook.BETMGM);
		
		int repeatsRemoved = 0;
		for (Map.Entry<String, List<Player>> entry : theMap.entrySet()) {
//		    System.out.println(entry.getKey() + " = " + entry.getValue());
			Player pLeft = null;
			int index = 0;
			while(index < entry.getValue().size()) {
				Player pRight = entry.getValue().get(index);
				if(pLeft == null) {
		    		pLeft = pRight;
		    		index++;
		    	} else {
		    		if(pLeft.getTeam().getCommonName().contentEquals(pRight.getTeam().getCommonName()) == false) {
		    			System.out.println(
		    					entry.getKey() + ": " + 
		    							pLeft.getTeam().getCommonName() + " (" + pLeft.getTeam().getBook() + ")" +  
		    							" and " + 
		    							pRight.getTeam().getCommonName() + " (" + pRight.getTeam().getBook() + ")");
		    			Player best = service.getClosestPlayerMatch(entry.getKey(), refPlayerList);
		    			System.out.println("Best Match from Ref: " + best);
		    			System.out.print("Use 1 or 2, or 3 for neither (return will bypass): ");
					    String teamName = scanner.nextLine();
					    if(teamName.trim().length() == 0) {
					    	continue;
					    }
				    	int choice = -1;
				    	try {
				    		choice = Integer.parseInt(teamName);
				    		System.out.println("Choice is " + choice);
				    		if((choice != 1) && (choice != 2) && (choice != 3)) {
				    			System.out.println("Choice must be 1,2, or 3");
				    			continue;
				    		}
				    		if(choice == 1) { // left team -> right team

				    			Team teamToUse = pLeft.getTeam();
				    			List<Team> allTeamsForBookRight = 
				    					service.teamRepo.findAllForSport(Sport.MLB_STATS, pRight.getTeam().getBook());

				    			Team teamForRight = null;
				    			for(Team t : allTeamsForBookRight) {
				    				if(t.getCommonName().contentEquals(teamToUse.getCommonName())) {
				    					teamForRight = t;
				    					break;
				    				}
				    			}
				    			if(teamForRight == null) {
				    				System.out.println("Failed to find team to replace for Right side");
				    				index++;
				    				continue;
				    			}

				    			service.repo.updateTeam(pRight, teamForRight);
				    			pRight.setTeam(teamForRight);

				    			// go to the next node to check on the list
				    			index++;

				    		} else if(choice == 2) { // right team -> left Team

				    			Team teamToUse = pRight.getTeam();
				    			List<Team> allTeamsForBookLeft = 
				    					service.teamRepo.findAllForSport(Sport.MLB_STATS, pLeft.getTeam().getBook());
				    			Team teamForLeft = null;
				    			for(Team t : allTeamsForBookLeft) {
				    				if(t.getCommonName().contentEquals(teamToUse.getCommonName())) {
				    					teamForLeft = t;
				    					break;
				    				}
				    			}
				    			if(teamForLeft == null) {
				    				System.out.println("Failed to find team to replace for Left side");
				    				continue;
				    			}

				    			service.repo.updateTeam(pLeft, teamForLeft);
				    			pLeft.setTeam(teamForLeft);

				    			// Since I updated the left node I have to compare all in the list to it
				    			index = 0;
				    			pLeft = null;
				    		} else {
				    			System.out.println("Select a team from the list: ");
				    			for(int index2 = 0; index2 <  allTeams.size(); ++index2) {
				    				System.out.println(index2 + ". " + allTeams.get(index2).getCommonName());
				    			}
				    			String teamNum = scanner.nextLine();
				    			if(teamNum.trim().length() == 0) {
				    				continue;
				    			}
				    			choice = Integer.parseInt(teamNum);
				    			System.out.println("Choice is " + choice);
				    			Team teamToUse = allTeams.get(choice);
				    			System.out.println("Team to use is " + teamToUse.getCommonName());

				    			List<Team> allTeamsForBookLeft = 
				    					service.teamRepo.findAllForSport(Sport.MLB_STATS, pLeft.getTeam().getBook());

				    			Team teamForLeft = null;
				    			for(Team t : allTeamsForBookLeft) {
				    				if(t.getCommonName().contentEquals(teamToUse.getCommonName())) {
				    					teamForLeft = t;
				    					break;
				    				}
				    			}
				    			if(teamForLeft == null) {
				    				System.out.println("Failed to find team to replace for left side");
				    				continue;
				    			}
				    			service.repo.updateTeam(pLeft, teamForLeft);
				    			pLeft.setTeam(teamForLeft);

				    			List<Team> allTeamsForBookRight = 
				    					service.teamRepo.findAllForSport(Sport.MLB_STATS, pRight.getTeam().getBook());
				    			Team teamForRight = null;
				    			for(Team t : allTeamsForBookRight) {
				    				if(t.getCommonName().contentEquals(teamToUse.getCommonName())) {
				    					teamForRight = t;
				    					break;
				    				}
				    			}
				    			if(teamForRight == null) {
				    				System.out.println("Failed to find team to replace for right side");
				    				continue;
				    			}
				    			service.repo.updateTeam(pRight, teamForRight);
				    			pRight.setTeam(teamForRight);

				    			// Since I updated the left node I have to compare all in the list to it
				    			index = 0;
				    			pLeft = null;
				    		}
				    	} catch(Exception e) {
				    		System.out.println("Exception updating player team: " + e.getMessage());
				    	}
		    		} else {
		    			index++;
		    		}
		    	}
		    }
			
			// Check for repeats
			List<String> bookPlusSpecificName = new ArrayList<>();
			for(Player player : entry.getValue()) {
				String key = player.getTeam().getBook().toString() + "_"  + player.getNameSbSpecific();
				if(bookPlusSpecificName.contains(key)) {
					System.out.println("Repeat to be removed: " + player);
	    			service.repo.removePlayer(player);
	    			repeatsRemoved++;
				} else {
					bookPlusSpecificName.add(key);
				}
			}

		}
		scanner.close();
		System.out.println("Repeats Removed: " + repeatsRemoved);
	}

	public float getWeight(String specName, String commonName) {
		CosineSimilarity cs = new CosineSimilarity();
		OverlapCoefficient os = new OverlapCoefficient();
		SmithWaterman sw = new SmithWaterman();
		MongeElkan me = new MongeElkan();

		float f1 = cs.getSimilarity(specName,commonName);
		float f2 = os.getSimilarity(specName,commonName);
		float f3 = sw.getSimilarity(specName,commonName);
		float f4 = me.getSimilarity(specName,commonName);
		return f1+f2+f3+f4;
	}

	private Player getClosestPlayerMatch(String pitcher, List<Player> allPlayers) {
		
		double bestWeight = -1;
		Player bestPlayer = null;
		for(Player p : allPlayers) {
	        double w = (double)getWeight(pitcher, p.getCommonName());
	        if(w > bestWeight) {
	        	bestWeight = w;
	        	bestPlayer = p;
	        }
		}

		if(bestWeight > 2.8) {
			return bestPlayer;
		}
		return null;
	}

	private void compareMlbStatsToEspnRef() {
		
		List<Player> refPlayers = repo.findAllForBook(Sportsbook.ESPN_MLB_REF);
		
		List<Player> players = repo.findAllForBook(Sportsbook.BETMGM);
		for(Player player : players) {

			// find best match in the reference data
			Player bestPlayer = getClosestPlayerMatch(player.getCommonName(), refPlayers);
			if(bestPlayer != null) {
				if(bestPlayer.getTeam().getCommonName().contentEquals(player.getTeam().getCommonName()) == false) {
					System.out.println("Existing player does not match ref: " + player);
					System.out.println("Ref match is " + bestPlayer + "\n");
				}
			}
		}
	
	
	}

	public void removeExistingPlayers(Sportsbook book) {
		repo.removeExistingPlayers(book);
	}
	
	
}
