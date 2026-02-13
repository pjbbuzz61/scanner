package history;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.history.Wager;
import scanner.scanner.model.history.draftkings.DraftKings_Events;
import scanner.scanner.model.history.draftkings.DraftKings_Transaction;
import scanner.scanner.model.history.draftkings.DraftKings_TransactionDetail;
import scanner.scanner.model.history.espn.Espn_Container;
import scanner.scanner.model.history.espn.Espn_Events;
import scanner.scanner.model.history.espn.Espn_datanode;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;

public class DraftKings {

	WagerService wagerService;

	private void processJsonFile(String file) {

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
	        DraftKings_Events rtn = gson.fromJson(line, DraftKings_Events.class);

	        List<Wager> wagers = new ArrayList<>();
	        
	        if(rtn != null) { 
	        	
	        	for(DraftKings_Transaction trans : rtn.getTransactions()) {
	        		
	        		for(DraftKings_TransactionDetail detail : trans.getTransactionDetails()) {

	        			if(detail.getDescription().contains("Gaming Deposit")) {
	        				continue;
	        			}
	        			if(detail.getDescription().contains("My Reward")) {
	        				continue;
	        			}
	        			if(detail.getDescription().contains("Crowns Redemption")) {
	        				continue;
	        			}
	        			if(detail.getDescription().contains("Bag Builder")) {
	        				continue;
	        			}
	        			if(detail.getDescription().contains("Withdrawal")) {
	        				continue;
	        			}

	        			Calendar c = Calendar.getInstance();
	    	        	c.setTime(detail.getCreateDate());
	    	        	if(c.get(Calendar.YEAR) != 2025) {
	    	        		//System.out.println("Play is not within 2025");
	    	        		continue;
	    	        	}

	        			Wager w = new Wager();

	        			if(detail.getLocationCode() == null) {
	        				System.out.println("here");
	        			}
	        			switch(detail.getLocationCode()) {
	        				case "US-MD":
	        					w.setState(STATES.MD);
	        					break;
	        				case "US-VA":
	        					w.setState(STATES.VA);
	        					break;
	        				case "US-KS":
	        					w.setState(STATES.KS);
	        					break;
	        				case "US-DC":
	        					w.setState(STATES.DC);
	        					break;
	        				default:
	        					System.out.println("Unknown state: " + detail.getLocationCode());
        			}
        			w.setBook(Sportsbook.DRAFTKINGS);

        			w.setBetNumber(trans.getPublicTransactionKey());
        			w.setBetTimestamp(detail.getCreateDate());
        			w.setPayoutTimestamp(detail.getCreateDate());
        			w.setEventTimestamp(detail.getCreateDate());

        			w.setEventDesc(detail.getDescription());

        			
        			switch(detail.getDescription()) {
        				case "Sportsbook wager":
        					w.setStake(-detail.getAmount());
        					w.setTotalReturn(0.0);
        					break;
        				case "Sportsbetting win payout":
        					w.setTotalReturn(detail.getAmount());
        					w.setStake(0.0);
        					break;
        				default:
        					System.out.println("New label: " + detail.getDescription());
        			}

        			wagers.add(w);
        			
	        		} // for all details
	        	}	        	
	        }
		
	        System.out.println("Number of wagers: " + wagers.size());
	        for(Wager w : wagers) {
	        	wagerService.insert(w);
//	        	System.out.println(w);
	        }

		
		}
	}

	
	public static void main(String[] args) {
		
		DraftKings dk = new DraftKings();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		dk.wagerService = ws;
		
		List<String> files = List.of(
				"/home/pat/2025/draftkings/all_trans.json"
		);

		
		for(String file : files) {
			dk.processJsonFile(file);
		}

		
		List<Wager> all = ws.getWagers();
		double wagered = 0.0;
		double won = 0.0;
		
		System.out.println("Num wagers: " + all.size());
		for(Wager w : all) {
			if(w.getBook() == Sportsbook.DRAFTKINGS) {
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
