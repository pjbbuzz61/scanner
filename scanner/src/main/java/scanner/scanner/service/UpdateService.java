package scanner.scanner.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mongodb.client.MongoClients;

import scanner.scanner.model.Player;
import scanner.scanner.model.Team;
import scanner.scanner.model.Update;
import scanner.scanner.repo.PlayerRepo;
import scanner.scanner.repo.TeamRepo;
import scanner.scanner.repo.UpdateRepo;
import uk.ac.shef.wit.simmetrics.similaritymetrics.CosineSimilarity;
import uk.ac.shef.wit.simmetrics.similaritymetrics.MongeElkan;
import uk.ac.shef.wit.simmetrics.similaritymetrics.OverlapCoefficient;
import uk.ac.shef.wit.simmetrics.similaritymetrics.SmithWaterman;

@Component
public class UpdateService {

	private static final Logger logger = LogManager.getLogger(UpdateService.class);
	
	@SuppressWarnings("unused")
	private static final Logger rootLogger = LogManager.getLogger("org.mongodb.driver");

	@Autowired
	protected UpdateRepo updateRepo;

	@Autowired
	protected TeamRepo teamRepo;

	@Autowired
	protected PlayerRepo playerRepo;

	
	public void insert(Update update) {
		List<Update> list = find(update);
		if((list == null) || (list.size() == 0)) {
			updateRepo.insert(update);
		}
	}

	public List<Update> find(Update update) {
		return updateRepo.find(update);
	}

	public static void main (String args[] ) {

		logger.info("Starting ....");
		
		
		//Date d = new Date(1568487746038L);
		//System.out.println(d);
		//System.exit(0);
		UpdateService service = new UpdateService();
		
		MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "scanner");
		
		UpdateRepo r = new UpdateRepo();
		r.setMongoTemplate(mongoTemplate);
		service.updateRepo = r;

		TeamRepo t = new TeamRepo();
		t.setMongoTemplate(mongoTemplate);
		service.teamRepo = t;

		PlayerRepo p = new PlayerRepo();
		p.setMongoTemplate(mongoTemplate);
		service.playerRepo = p;

		List<Update> updatesAll = service.updateRepo.findAll();
		List<Update> updates = new ArrayList<Update>();
		if((updatesAll != null) && (updatesAll.size() > 0)) {
			for(Update u : updatesAll) {
				updates.add(u);
			}
		}
	    Scanner scanner = new Scanner(System.in);
	    int count = updates.size();
		System.out.println("Count: " + count);
		for(Update u : updates) {

			System.out.println("Count: " + count);
			if(u.getType().contentEquals("team")) {

				// Get all teams on the system
				List<Team> allTeams = t.findAllForSport(u.getSport());

				if(u.getTeamName() == null) {
					System.out.println("No Team Name!! " + u);
					service.updateRepo.remove(u);
	    			continue;
				}

				List<String> closestMatches = getClosestTeamMatches(u.getTeamName().toUpperCase().trim(), allTeams);
				
				System.out.println(u);
				System.out.println("Closest Matches:");
				int i = 1;
				for(String s : closestMatches) {
					System.out.println(i + ". " + s);
					i++;
				}
				System.out.print("Enter number or the team name (return to use the one above, 0 to remove): ");
			    String teamName = scanner.nextLine();
			    if(teamName.trim().length() == 0) {
			    	teamName = u.getTeamName().trim().toUpperCase();
			    } else {
			    	int choice = -1;
			    	try {
			    		choice = Integer.parseInt(teamName);
			    		System.out.println("Choice is " + choice);
			    		if((choice >= 1) && (choice <= closestMatches.size())) {
			    			teamName = closestMatches.get(choice-1);
			    		} else if(choice == 0) {
						    service.updateRepo.remove(u);
			    			continue;
			    		} else {
			    			System.out.println("Choice out of range: " + choice + ", number of choices is " + closestMatches.size());
			    			continue;
			    		}
			    	} catch(Exception e) {
			    		teamName = teamName.toUpperCase();
			    		if(teamName.contentEquals("SKIP")) {
						    service.updateRepo.remove(u);
			    			continue;
			    		}
			    	}
			    }
			    System.out.println("Team is: " + teamName.trim().toUpperCase()); 
			    
			    Team team = new Team();
			    team.setBook(u.getBook());
			    team.setCommonName(teamName.trim().toUpperCase());
			    team.setNameSbSpecific(u.getTeamName());
			    team.setSport(u.getSport());

			    service.teamRepo.insert(team);
			    service.updateRepo.remove(u);

			} else if(u.getType().contentEquals("players")) {
			    
				// Get all pitchers on the system
				List<Player> allPlayers = p.findAllForTeam(u.getTeam().getCommonName());
				
				List<String> closestMatches = getClosestPlayerMatches(u.getPlayer().toUpperCase().trim(), allPlayers);
				
				System.out.println(u);
				System.out.println("Closest Matches:");
				int i = 1;
				for(String s : closestMatches) {
					System.out.println(i + ". " + s);
					i++;
				}
				System.out.print("Enter number or the player name (return to use the one above, 0 to remove): ");
			    String pitcher = scanner.nextLine();
			    if(pitcher.trim().length() == 0) {
			    	pitcher = u.getPlayer().trim().toUpperCase();
			    } else {
			    	int choice = -1;
			    	try {
			    		choice = Integer.parseInt(pitcher);
			    		System.out.println("Choice is " + choice);
			    		if((choice >= 1) && (choice <= closestMatches.size())) {
			    			pitcher = closestMatches.get(choice-1);
			    		} else if(choice == 0) {
						    service.updateRepo.remove(u);
			    			continue;
			    		} else {
			    			System.out.println("Choice out of range: " + choice + ", number of choices is " + closestMatches.size());
			    			continue;
			    		}
			    	} catch(Exception e) {
			    		pitcher = pitcher.toUpperCase();
			    		if(pitcher.contentEquals("SKIP")) {
						    service.updateRepo.remove(u);
			    			continue;
			    		}
			    	}
			    }
			    System.out.println("Player is: " + pitcher.trim().toUpperCase()); 
			    
			    Player newPlayer = new Player();
			    newPlayer.setTeam(u.getTeam());
			    newPlayer.setNameSbSpecific(u.getPlayer());
			    newPlayer.setCommonName(pitcher.trim().toUpperCase());
			    service.playerRepo.insert(newPlayer);
			    service.updateRepo.remove(u);
			}
			count--;
		}
		scanner.close();
	}

	private static List<String> getClosestPlayerMatches(String pitcher, List<Player> allPlayers) {
		
		List<String> choices = new ArrayList<String>();
		Map<String, Double> map = new HashMap<String, Double>();
		for(Player p : allPlayers) {
	        map.put(p.getCommonName(), (double)getWeight(pitcher, p.getCommonName()));
		}
		if(map.size() == 0) return choices;
		int count = 0;
		do {

			String top = null;
			Double val = 0.0;
	        for (Map.Entry<String,Double> entry : map.entrySet()) {
	        	if(entry.getValue() > val) {
	        		val = entry.getValue();
	        		top = entry.getKey();
	        	}
	        }
	        choices.add(top);
	        map.remove(top);
	        if(map.size() == 0) break;

			count++;
		} while(count < 10);

		return choices;
	}

	private static float getWeight(String specName, String commonName) {
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

	private static List<String> getClosestTeamMatches(String team, List<Team> allTeams) {
		List<String> choices = new ArrayList<String>();
		Map<String, Double> map = new HashMap<String, Double>();
		for(Team t : allTeams) {
	        map.put(t.getCommonName(), (double)getWeight(team, t.getCommonName()));
		}
		if(map.size() == 0) return choices;
		int count = 0;
		do {

			String top = null;
			Double val = 0.0;
	        for (Map.Entry<String,Double> entry : map.entrySet()) {
	        	if(entry.getValue() > val) {
	        		val = entry.getValue();
	        		top = entry.getKey();
	        	}
	        }
	        choices.add(top);
	        map.remove(top);
	        if(map.size() == 0) break;

			count++;
		} while(count < 8);

		return choices;
	}

	public Long getCount() {
		return updateRepo.getCount();
	}

	public void setUpdateRepo(UpdateRepo repo) {
		this.updateRepo = repo;
	}

	public void setTeamRepo(TeamRepo repo) {
		this.teamRepo = repo;
	}
}
