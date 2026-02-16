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
import scanner.scanner.model.history.hardrock.HardRock_Bet;
import scanner.scanner.model.history.hardrock.HardRock_BetPart;
import scanner.scanner.model.history.hardrock.HardRock_Events;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;
import scanner.scanner.util.history.WAGER_RESULT;

public class HardRock {

	WagerService wagerService;

	
    private int convertDecimalToAmerican(double decimalOdds) {

    	double trueOdds = (double)(decimalOdds);
        if (trueOdds >= 2.0) {
        	return (int)Math.round((trueOdds - 1) * 100);
        } else {
            return (int)Math.round(-100 / (trueOdds - 1));
        }
    }

    private void processJsonFile(String file) {

		STATES state = STATES.FL;
		
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

        HardRock_Events rtn = gson.fromJson(sb.toString(), HardRock_Events.class);

        List<Wager> wagers = new ArrayList<>();
        
        if((rtn != null) && (rtn.getBets() != null) && (rtn.getBets().getBet() != null)) {
        	for(HardRock_Bet bet : rtn.getBets().getBet()) {
        		
        		Wager w = new Wager();
    			w.setState(state);
    			w.setBook(Sportsbook.HARDROCK);
    			
    			w.setBetNumber(bet.getId());
    			w.setBetTimestamp(new Date(bet.getBetTime()));
    			w.setPayoutTimestamp(new Date(bet.getSettlementTime()));
    			w.setEventTimestamp(new Date(bet.getParts().getBetPart().get(0).getEventTime()));

    			Date first = null;
    			StringBuilder desc = new StringBuilder();
    			int betNum = 0;
    			for(HardRock_BetPart part : bet.getParts().getBetPart()) {
    				
					if((first == null) || (new Date(part.getEventTime()).before(first))) {
						first = new Date(part.getEventTime());
					}

    				if(betNum > 0) {
    					desc.append("\t");
    				}
					
    				String score = null;
    				String sport = null;
    				if(part.getEventResult() != null) {
    					sport = part.getEventResult().getSport();
    					score = part.getEventResult().getScore();
    				}
    				
					desc.append(
							part.getEvent().getName() + "|" + 
    						part.getSelection().getName() + " " + part.getMarket().getName() + "|" + 
    						part.getOdds().getMoneyline() + "|" + 
    						part.getResultType() + "|" + "Outcome: " + score);

					if(betNum < (bet.getParts().getBetPart().size()-1)) {
    					desc.append("\n");
    				}

    				w.setSport(sport);
    				w.setLeague(part.getCompetition().getName());

    				betNum++;	
    			}
    			
    			w.setEventTimestamp(first);
    			w.setEventDesc(desc.toString());

    			w.setBonus(bet.getFreeBet());
    			
    			//w.setBetType(bet.getType());

    			switch(bet.getDisplayStatus()) {
					case "LOSE":        w.setResult(WAGER_RESULT.LOSS);        break;
					case "WIN":         w.setResult(WAGER_RESULT.WIN);         break;
//				case "VOID":        w.setResult(WAGER_RESULT.VOIDED);      break;
					case "CASHED":  w.setResult(WAGER_RESULT.CASHED_OUT);  break;
					default: 
						System.out.println("New state: " + bet.getDisplayStatus());
    			}

    			w.setOriginal_odds(convertDecimalToAmerican(bet.getTotalPrice()));
				w.setBoosted_odds(convertDecimalToAmerican(bet.getTotalPrice())); // default boosted to original until we find an update

    			w.setStake(bet.getStake().getAmount());
    			w.setTotalReturn(bet.getTotalPayout());
    			
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
		
		HardRock hr = new HardRock();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		hr.wagerService = ws;
		
		List<String> files = List.of(
//				"/home/pat/2025/hard_rock/all_bets_2025.json"
		);

		
		for(String file : files) {
			hr.processJsonFile(file);
		}
		
		List<Wager> all = ws.getWagers();
		double wagered = 0.0;
		double won = 0.0;
		
		System.out.println("Num wagers: " + all.size());
		for(Wager w : all) {
			//if(w.getBook() == Sportsbook.HARDROCK) {
				if(w.isBonus() == false) {
					wagered += w.getStake();
				}
				won += w.getTotalReturn();
			//}
		}
		System.out.println("Won: " + won);
		System.out.println("Wagered: " + wagered);
		System.out.println("Diff: " + (won-wagered));
	}


}
