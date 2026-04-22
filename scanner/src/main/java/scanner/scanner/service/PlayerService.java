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
		
		for (Map.Entry<String, List<Player>> entry : theMap.entrySet()) {
//		    System.out.println(entry.getKey() + " = " + entry.getValue());
			Team team = null;
			for(Player p : entry.getValue()) {
		    	if(team == null) {
		    		team = p.getTeam();
		    	} else {
		    		if(team.getCommonName().contentEquals(p.getTeam().getCommonName()) == false) {
		    			System.out.println(entry.getKey() + ": " + p.getTeam().getCommonName() + " and " + team.getCommonName());
						System.out.print("Use first or second team, or 3 for neither (return will bypass): ");
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
				    			if(choice == 1) { // replace team with the team from p.team
				    				
						    		for(Player player : players) {
					    				if(player.getCommonName().contentEquals(p.getCommonName()) && 
				    					  (player.getTeam().getCommonName().contentEquals(team.getCommonName()))) {
					    						
					    					// find the team for the sport with the common name
					    					// use that for the team
					    					Team t = service.teamRepo.getTeam(
					    							team.getBook(), 
					    							p.getTeam().getSport(), 
					    							p.getTeam().getCommonName());
					    					if(t == null) {
					    						System.out.println("Failed to find the team " + p.getTeam());
					    						continue;
					    					}
					    					service.repo.updateTeam(player, t);
					    					break;
					    				}
				    				}
				    			} else if(choice == 2) { // choice is 2 -- 
				    				
						    		for(Player player : players) {
					    				if(player.getCommonName().contentEquals(p.getCommonName()) && 
						    			  (player.getTeam().getCommonName().contentEquals(p.getTeam().getCommonName()))) {
	
					    					Team t = service.teamRepo.getTeam(
					    							p.getTeam().getBook(), 
					    							team.getSport(), 
					    							team.getCommonName());
					    					if(t == null) {
					    						System.out.println("Failed to find the team " + team);
					    						continue;
					    					}
					    					service.repo.updateTeam(player, t);
					    					break;
					    				}
				    				}
				    			} else {
				    				System.out.println("Select a team from the list: ");
				    				for(int index = 0; index <  allTeams.size(); ++ index) {
				    					System.out.println(index + ". " + allTeams.get(index).getCommonName());
				    				}
								    String teamNum = scanner.nextLine();
								    if(teamNum.trim().length() == 0) {
								    	continue;
								    }
						    		choice = Integer.parseInt(teamNum);
						    		System.out.println("Choice is " + choice);
						    		Team teamToUse = allTeams.get(choice);
						    		System.out.println("Team to use is " + teamToUse.getCommonName());
						    		for(Player player : players) {
						    			if(
						    					(player.getCommonName().contentEquals(p.getCommonName()) 
						    							&& 
						    					   (
						    							 (player.getTeam().getCommonName().contentEquals(p.getTeam().getCommonName()))
						    							 	||
						    							 (player.getTeam().getCommonName().contentEquals(team.getCommonName()))
						    					   )
						    					)
						    			  ) {
						    				
						    				// need to find the team for this book
					    					Team t = service.teamRepo.getTeam(
					    							player.getTeam().getBook(), 
					    							player.getTeam().getSport(), 
					    							teamToUse.getCommonName());
					    					if(t == null) {
					    						System.out.println("Failed to find the team " + team);
					    						continue;
					    					}
						    				
							    			service.repo.updateTeam(player, t);
						    			}
						    		}
				    			}
				    	} catch(Exception e) {
				    		System.out.println("Exception updating player team: " + e.getMessage());
				    	}
		    		}
		    	}
		    }
		}
	}
	
	
}
