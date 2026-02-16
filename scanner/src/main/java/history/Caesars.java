package history;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.history.Wager;
import scanner.scanner.model.history.caesars.Caesars_Bet;
import scanner.scanner.model.history.caesars.Caesars_Events;
import scanner.scanner.model.history.caesars.Caesars_Leg;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;
import scanner.scanner.util.history.WAGER_RESULT;
import scanner.scanner.util.history.WAGER_TYPE;

public class Caesars {

	WagerService wagerService;

	
	
	private void processJsonFile(String file) {

		STATES state = STATES.MD;
		if(file.contains("all_bets_va")) {
			state = STATES.VA;
		}
		if(file.contains("all_bets_ks")) {
			state = STATES.KS;
		}
		
        Gson gson = (new GsonBuilder()).setStrictness(Strictness.LENIENT).create();

        List<String> lines = new ArrayList<>();
        
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
		} catch(Exception e) {
			System.out.println("Exception reading json file: " + e.getMessage());
		}

		for(String line : lines) {
	        Caesars_Events rtn = gson.fromJson(line, Caesars_Events.class);

	        List<Wager> wagers = new ArrayList<>();
	        
	        if(rtn != null) {
	        	if(rtn.getBets() != null) {
	        		for(Caesars_Bet cb : rtn.getBets()) {
	        			Wager w = new Wager();
	        			w.setState(state);
	        			w.setBook(Sportsbook.CAESARS);

	        			w.setBetNumber(cb.getId());
	        			w.setBetTimestamp(cb.getPlacedAt());
	        			w.setPayoutTimestamp(cb.getSettledAt());
	        			
	        			Calendar c = Calendar.getInstance();
	        			c.setTime(cb.getPlacedAt());
	        			if(c.get(Calendar.YEAR) != 2025) {
	        				//System.out.println("Play is not within 2025");
	        				continue;
	        			}
	        			
	        			// get all event timestamp for the bets (multiple if a parley)
	        			//  use the earliest for the event start
	        			Date first = cb.getLegs().get(0).getEventStartTime();
	        			StringBuilder desc = new StringBuilder();
	        			int betNum = 0;
	        			for(Caesars_Leg leg : cb.getLegs()) {

	            			// Event Desc will be of the form:
	            			//  Somebody @ Somebody, play (ex: o141), odds (ex: +123)
	        				if(betNum > 0) {
	        					desc.append("\t");
	        				}
	        				desc.append(
	        						leg.getEvent().getName().replace("|", "") + "|" + 
	        						leg.getSelection().getName().replace("|", "") + "|" + 
	        						leg.getPrice().getA() + "|" + 
	        						leg.getResult().getType());
	        				if(betNum < (cb.getLegs().size()-1)) {
	        					desc.append("\n");
	        				}
	        				w.setSport(leg.getSport().getName());
	        				w.setLeague(leg.getCompetition().getName());
	        				
	        				if(leg.getEventStartTime().before(first)) {
	        					first = leg.getEventStartTime();
	        				}
	        				betNum++;
	        			}
	        			w.setEventDesc(desc.toString());

	        			w.setEventTimestamp(first);

	        			switch(cb.getBetType()) {
		        		case "parlay":
			        		w.setBetType(WAGER_TYPE.PARLAY);
		        			break;
		        		case "straight":
		        			w.setBetType(WAGER_TYPE.SINGLE);
		        			break;
		       			default:
		       				System.out.println("Unknown wager type for Caesars: " + cb.getBetType());
        			}

	        			if((cb.getCashOut() != null) && cb.getCashOut()) {
	        				w.setResult(WAGER_RESULT.CASHED_OUT);
	        			} else {
		        			switch(cb.getResultIndicator()) {
		        				case "LOST":      w.setResult(WAGER_RESULT.LOSS);       break;
		        				case "WON":       w.setResult(WAGER_RESULT.WIN);        break;
		        				case "VOID":      w.setResult(WAGER_RESULT.CANCELLED);  break;
		        				case "PUSH":      w.setResult(WAGER_RESULT.CANCELLED);  break;
		        				default: 
		        					System.out.println("New state: " + cb.getResultIndicator());
		        			}
	        			}

	        			if(cb.getLegs().size() > 1) {
		        			w.setOriginal_odds(Integer.parseInt(cb.getEstimatedOdds().getA()));
							w.setBoosted_odds(Integer.parseInt(cb.getEstimatedOdds().getA())); // default boosted to original until we find an update
	        			} else {
		        			w.setOriginal_odds(Integer.parseInt(cb.getLegs().get(0).getPrice().getA()));
							w.setBoosted_odds(Integer.parseInt(cb.getLegs().get(0).getPrice().getA())); // default boosted to original until we find an update
	        			}
						
/*
	        			if((bs.getPromoTokens() != null) && (bs.getPromoTokens().size() > 0)) {
	        				for(BetMGM_PromoToken pt : bs.getPromoTokens()) {
	        					switch(pt.getTokenType()) {
	        						case "OddsBoost": 
	        							for(BetMGM_InfoItem ii : pt.getAdditionalInformation().getInformationItems()) {
	        								if(ii.getKey().contentEquals("BoostedOdds.American")) {
	        									w.setBoosted_odds(Integer.parseInt(ii.getValue()));
	        									break;
	        								}
	        							}
	        							break;
	        						case "RiskFree":
	        							w.setRiskFree(true);
	        							break;
	        						default:
	        							System.out.println("New promo token type: " + pt.getTokenType());
	        					}
	        				}
	        			}
*/	        			
	        			if(cb.getFreebetStake() != null) {
		        			w.setBonus(true);
	        			}
	        			w.setStake((double)(cb.getTotalStake())/100.0);
	        			w.setTotalReturn((double)(cb.getPayout())/100.0);
	        			
	        			wagers.add(w);
	        		}
	        	}
	        }
		
	        System.out.println("Number of wagers: " + wagers.size());
	        for(Wager w : wagers) {
//	        	wagerService.insert(w);
//	        	System.out.println(w);
	        }

		
		}
	}
	
	
	
	public static void main(String[] args) {

		Caesars cs = new Caesars();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		cs.wagerService = ws;
		
		List<String> files = List.of(
//				"/home/pat/2025/caesars/all_bets_ks.json",
//				"/home/pat/2025/caesars/all_bets_va.json",
				"/home/pat/2025/caesars/all_bets_md.json"
		);

		
		for(String file : files) {
			cs.processJsonFile(file);
		}

		
		List<Wager> all = ws.getWagers();
		double wagered = 0.0;
		double won = 0.0;
		
		System.out.println("Num wagers: " + all.size());
		for(Wager w : all) {
			if(w.getBook() == Sportsbook.CAESARS) {
				if(w.getState() == STATES.MD) {
					if(w.isBonus() == false) {
						wagered += w.getStake();
					}
					won += w.getTotalReturn();
				}
			}
		}
		System.out.println("Won: " + won);
		System.out.println("Wagered: " + wagered);
		System.out.println("Diff: " + (won-wagered));

	}

}
