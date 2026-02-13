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
import scanner.scanner.model.history.espn.Espn_Container;
import scanner.scanner.model.history.espn.Espn_Events;
import scanner.scanner.model.history.espn.Espn_datanode;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;

public class Espn {

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
	        Espn_Events rtn = gson.fromJson(line, Espn_Events.class);

	        List<Wager> wagers = new ArrayList<>();
	        
	        if((rtn != null) && 
	           (rtn.getData() != null) &&
	           (rtn.getData().getNode() != null) &&
	           (rtn.getData().getNode().getItems() != null) &&
	           (rtn.getData().getNode().getItems().getEdges() != null) &&
	           (rtn.getData().getNode().getItems().getEdges() != null)
	           ) {

	        	for(Espn_Container cont : rtn.getData().getNode().getItems().getEdges()) {
	        		Espn_datanode node = cont.getNode();
	        		
        			Calendar c = Calendar.getInstance();
        			c.setTime(node.getDate());
        			if(c.get(Calendar.YEAR) != 2025) {
        				//System.out.println("Play is not within 2025");
        				continue;
        			}

        			Wager w = new Wager();
        			switch(node.getRegion()) {
        				case "MD, US":
        					w.setState(STATES.MD);
        					break;
        				case "VA, US":
        					w.setState(STATES.VA);
        					break;
        				case "KS, US":
        					w.setState(STATES.KS);
        					break;
        				case "DC, US":
        					w.setState(STATES.DC);
        					break;
        				default:
        					System.out.println("Unknown state: " + node.getRegion());
        			}
        			w.setBook(Sportsbook.ESPN);

        			w.setBetNumber(node.getTransactionId());
        			w.setBetTimestamp(node.getDate());
        			w.setPayoutTimestamp(node.getDate());
        			w.setEventTimestamp(node.getDate());

        			w.setEventDesc(node.getLabel());
        			
        			switch(node.getLabel()) {
        				case "Bet Placed":
        					w.setStake(-Double.parseDouble(node.getAmount().getAmountLong())/100.0);
        					w.setTotalReturn(0.0);
        					break;
        				case "Bonus Bet Placed":
        				case "Bonus Bet Refund":
        					// no change to stake
        					w.setTotalReturn(0.0);
        					w.setStake(0.0);
        					break;
        				case "Bet Payout":
        				case "Bonus Bet Payout":
        				case "Bet Cancelled":
        				case "Bet Cashed Out":
        				case "Bet Resettled":
        					w.setTotalReturn(Double.parseDouble(node.getAmount().getAmountLong())/100.0);
        					w.setStake(0.0);
        					break;
        				default:
        					System.out.println("New label: " + node.getLabel());
        			}

        			if(w.getStake() == null) {
        				System.out.println("no stake");
        			}
        			wagers.add(w);

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

		Espn espn = new Espn();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		espn.wagerService = ws;
		
		List<String> files = List.of(
//				"/home/pat/2025/espn/all_bets.json"
		);

		
		for(String file : files) {
			espn.processJsonFile(file);
		}

		
		List<Wager> all = ws.getWagers();
		double wagered = 0.0;
		double won = 0.0;
		
		System.out.println("Num wagers: " + all.size());
		for(Wager w : all) {
		//	if(w.getBook() == Sportsbook.ESPN) {
				if(w.isBonus() == false) {
					wagered += w.getStake();
				}
				won += w.getTotalReturn();
		//	}
		}
		System.out.println("Won: " + won);
		System.out.println("Wagered: " + wagered);
		System.out.println("Diff: " + (won-wagered));

	}

}
