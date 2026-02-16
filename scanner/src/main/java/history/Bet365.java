package history;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.history.Wager;
import scanner.scanner.model.history.bet365.Bet365_Events;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;

public class Bet365 {

	WagerService wagerService;

    private void processJsonFile(String file) {
    	
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

        Bet365_Events rtn = gson.fromJson(sb.toString(), Bet365_Events.class);
        
        List<Wager> wagers = rtn.getRecords();
        
        System.out.println("Number of wagers: " + wagers.size());
        for(Wager w : wagers) {
        	wagerService.insert(w, "wagers_2026");
//        	System.out.println(w);
        }


    }
    
    
	public static void main(String[] args) {

		Bet365 b365 = new Bet365();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		b365.wagerService = ws;
		
		List<String> files = List.of(
//				"/home/pat/2025/bet365/all_from_2025_by_hand.json"
				"/home/pat/wagers/2026/hardrock/files/jan_2026_by_hand.json"
		);

		
		for(String file : files) {
			b365.processJsonFile(file);
		}
		
		List<Wager> all = ws.getWagers();
		double wagered = 0.0;
		double won = 0.0;
		
		System.out.println("Num wagers: " + all.size());
		for(Wager w : all) {
//			if(w.getBook() == Sportsbook.BET365) {
				if(w.isBonus() == false) {
					wagered += w.getStake();
				}
				won += w.getTotalReturn();
//			}
		}
		System.out.println("Won: " + won);
		System.out.println("Wagered: " + wagered);
		System.out.println("Diff: " + (won-wagered));
	}

}
