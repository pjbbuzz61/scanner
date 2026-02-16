package history;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.history.Wager;
import scanner.scanner.model.history.betrivers.BetRivers_Bet;
import scanner.scanner.model.history.betrivers.BetRivers_CouponRow;
import scanner.scanner.model.history.betrivers.BetRivers_EventGroup;
import scanner.scanner.model.history.betrivers.BetRivers_Events;
import scanner.scanner.model.history.betrivers.BetRivers_Item;
import scanner.scanner.model.history.betrivers.BetRivers_Outcome;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;
import scanner.scanner.util.history.WAGER_RESULT;
import scanner.scanner.util.history.WAGER_TYPE;

public class BetRivers {

	WagerService wagerService;
	
	
    private int convertDecimalToAmerican(int decimalOdds) {

    	double trueOdds = (double)(decimalOdds) / 1000.0;
        if (trueOdds >= 2.0) {
        	return (int)Math.round((trueOdds - 1) * 100);
        } else {
            return (int)Math.round(-100 / (trueOdds - 1));
        }
    }

    private void processJsonFile(String file) {

		STATES state = STATES.MD;
		
        Gson gson = (new GsonBuilder()).create();
        StringBuilder sb = new StringBuilder();
        
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
			    sb.append(line);
			}
			reader.close();
		} catch(Exception e) {
			System.out.println("Exception reading json file: " + e.getMessage());
		}

        BetRivers_Events rtn = gson.fromJson(sb.toString(), BetRivers_Events.class);

        List<Wager> wagers = new ArrayList<>();
        
        if((rtn != null) && (rtn.getItems() != null)) {
        	for(BetRivers_Item bri : rtn.getItems()) {

        		Wager w = new Wager();
    			w.setState(state);
    			w.setBook(Sportsbook.BETRIVERS);

    			w.setBetNumber(bri.getCouponExternalRef());
    			w.setBetTimestamp(bri.getPlacedDate());
    			w.setPayoutTimestamp(null);
    			
    			// get all event timestamp for the bets (multiple if a parley)
    			//  use the earliest for the event start
    			Date first = null;
    			StringBuilder desc = new StringBuilder();
    			int betNum = 0;
    			
    			for(BetRivers_Bet bet : bri.getBets()) {
    				for(BetRivers_CouponRow row : bet.getCouponRows()) {
    					for(BetRivers_Outcome outcome : row.getOutcomes()) {
    						if((first == null) || (outcome.getEventInfo().getEventStartDate().before(first))) {
    							first = outcome.getEventInfo().getEventStartDate();
    						}
    						
    						StringBuilder cor = new StringBuilder();
    						for(String s : outcome.getSettledInfo().getResult().getCorrect()) {
    							cor.append(s + " ");
    						}
            				if(betNum > 0) {
            					desc.append("\t");
            				}
    						desc.append(
    								outcome.getEventInfo().getEventName() + "|" + 
    	    						outcome.getBetOffer().getCriterion() + " " + outcome.getLabel() + "|" + 
    	    						convertDecimalToAmerican(row.getPlayedOdds()) + "|" + 
    	    						row.getStatus() + "|" + "Outcome: " + cor.toString());
    						
            				if(betNum < (row.getOutcomes().size()-1)) {
            					desc.append("\n");
            				}

            				cor = new StringBuilder();
            				for(BetRivers_EventGroup eg : outcome.getEventInfo().getEventGroups()) {
            					cor.append(eg.getName() + ",");
            				}
            				w.setSport(cor.toString());
            				w.setLeague(cor.toString());

            				betNum++;	
    					}
    				}
    			}
    			
    			w.setEventTimestamp(first);
    			w.setEventDesc(desc.toString());

    			if(bri.getRewardType() != null) {
        			switch(bri.getRewardType()) {
	    			case "SECOND_CHANCE":
	    				w.setRiskFree(true);
	    				break;
	    			case "PROFIT_BOOST":
	    				break;
	    			case "FREE_BET":
	    				w.setBonus(true);
	    				break;
	    			default:
	    				System.out.println("New reward type: " + bri.getRewardType());
        			}
    			}
    			
        		switch(bri.getCouponType()) {
        		case "PARLAY":
        		case "SAME_GAME_PARLAY":
	        		w.setBetType(WAGER_TYPE.PARLAY);
        			break;
        		case "SINGLE":
        			w.setBetType(WAGER_TYPE.SINGLE);
        			break;
       			default:
       				System.out.println("Unknown wager type for Betrivers: " + bri.getCouponType());
    		}

    			switch(bri.getBets().get(0).getBetStatus()) {
    				case "LOST":        w.setResult(WAGER_RESULT.LOSS);        break;
    				case "WON":         w.setResult(WAGER_RESULT.WIN);         break;
    				case "VOID":        w.setResult(WAGER_RESULT.VOIDED);      break;
    				case "CASHED_OUT":  w.setResult(WAGER_RESULT.CASHED_OUT);  break;
    				default: 
    					System.out.println("New state: " + bri.getBets().get(0).getBetStatus());
    			}

    			w.setOriginal_odds(convertDecimalToAmerican(bri.getBets().get(0).getPlayedOdds()));
				w.setBoosted_odds(convertDecimalToAmerican(bri.getBets().get(0).getPlayedOdds())); // default boosted to original until we find an update

    			w.setStake((double)(bri.getBets().get(0).getStake())/1000.0);
    			w.setTotalReturn((double)(bri.getBets().get(0).getPayout())/1000.0);
    			
    			wagers.add(w);

        	}
        }
	
        System.out.println("Number of wagers: " + wagers.size());
        for(Wager w : wagers) {
        	wagerService.insert(w);
//        	System.out.println(w);
        }
	}

	public static void main(String[] args) {

		BetRivers br = new BetRivers();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		br.wagerService = ws;
		
		List<String> files = List.of(
//				"/home/pat/2025/betrivers/all_bets_md.json"
		);

		
		for(String file : files) {
			br.processJsonFile(file);
		}
		
		List<Wager> all = ws.getWagers();
		double wagered = 0.0;
		double won = 0.0;
		
		System.out.println("Num wagers: " + all.size());
		for(Wager w : all) {
			if(w.getBook() == Sportsbook.BETRIVERS) {
				if(w.isBonus() == false) {
					wagered += w.getStake();
				}
				won += w.getTotalReturn();
			}
		}
		System.out.println("Won: " + won);
		System.out.println("Wagered: " + wagered);
		System.out.println("Diff: " + (won-wagered));
	}


}
