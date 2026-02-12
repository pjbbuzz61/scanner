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
import scanner.scanner.model.history.fanduel.FanDuel_Events;
import scanner.scanner.model.history.fanduel.FanDuel_Transaction;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;

public class FanDuel {

	WagerService wagerService;

	
	private void processJsonFile(String file) {

		STATES state = STATES.MD;
		if(file.contains("all_trans_va")) {
			state = STATES.VA;
		}
		if(file.contains("all_trans_ks")) {
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

		double deposits = 0.0;
		double withdrawals = 0.0;
		for(String line : lines) {
	        FanDuel_Events rtn = gson.fromJson(line, FanDuel_Events.class);

	        List<Wager> wagers = new ArrayList<>();
	        
	        if((rtn != null) && (rtn.getTransactions() != null)) {
	        	for(FanDuel_Transaction ft : rtn.getTransactions()) {

	        		switch(ft.getCurrency()) {
	        			case "PBT":
	        				continue;
	        			case "USD":
	        				// do nothing, just fall through
	        				break;
	        			default:
	        				System.out.println("Dont know the currency: " + ft.getCurrency());
	        				continue;
	        		}
	        		
	        		switch(ft.getTransaction_type()) {
	        			case "BET":
	        			case "BONUS":
	        			case "WINNINGS":
	        				break;
	        			case "WALLET_TRANSFER":
	        			case "DEPOSIT":
	        				//System.out.println("Deposit: " + ft.getAmount());
	        				deposits += ft.getAmount();
	        				continue;
	        			case "WITHDRAWAL":
	        				System.out.println("Withdrawal: " + ft.getAmount());
	        				//withdrawals += ft.getAmount();
	        				continue;
	        			default:
	        				System.out.println("New Trans Type: " + ft.getTransaction_type());
	        				continue;
	        		}

	        		
	        		switch(ft.getAccount_type()) {
	        			case "USER_FREE_BET":
	        			case "USER_PROFIT_BOOST":
	        			case "USER_SPORTSBOOK_CASH":
	        				break;
	        			case "USER_SPORTSBOOK_DEPOSIT":
	        				break;
	        			default:
	        				System.out.println("New acct type: " + ft.getAccount_type());
	        				continue;
	        		}
        			Wager w = new Wager();
        			w.setState(state);
        			w.setBook(Sportsbook.FANDUEL);

        			w.setBetNumber(ft.getId());
        			w.setBetTimestamp(ft.getDate_raised());
        			w.setPayoutTimestamp(ft.getDate_completed());
        			w.setEventTimestamp(ft.getDate_completed());  // don't have actual contest start, unfortunately
        			
//        			Calendar c = Calendar.getInstance();
//        			c.setTime(ft.getDate_raised());
//        			if(c.get(Calendar.YEAR) != 2025) {
//        				//System.out.println("Play is not within 2025");
//        				continue;
//        			}

        			w.setEventDesc(ft.getDescription());

        			if(ft.getTransaction_type().contentEquals("WINNINGS")) {
            			w.setTotalReturn(ft.getAmount());
            			w.setStake(0.0);
        			} else {
        				//System.out.println("Wager: " + ft.getAmount() + " at " + ft.getDate_raised() + " " + ft.getDescription());
            			w.setStake(-ft.getAmount());
            			w.setTotalReturn(0.0);
        			}

        			wagers.add(w);

	        	}	
	        }

//	        System.out.println("Number of wagers: " + wagers.size());
	        for(Wager w : wagers) {
	        	wagerService.insert(w);
//	        	System.out.println(w);
	        }

		}
		//System.out.println("Deposits: " + deposits);
		//System.out.println("Withdrawals: " + withdrawals);
	}

	
	public static void main(String[] args) {

		FanDuel fd = new FanDuel();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		fd.wagerService = ws;
		
		List<String> files = List.of(
				"/home/pat/2025/fanduel/all_trans_ks.json",
				"/home/pat/2025/fanduel/all_trans_va.json",
				"/home/pat/2025/fanduel/all_trans_md.json"
//				"/home/pat/2025/fanduel/jan_2026_wagers.json"
		);

		
		for(String file : files) {
			fd.processJsonFile(file);
		}

		
		List<Wager> all = ws.getWagers();
		double wagered = 0.0;
		double won = 0.0;
		
		System.out.println("Num wagers: " + all.size());
		for(Wager w : all) {
			if(w.getBook() == Sportsbook.FANDUEL) {
				wagered += w.getStake();
				won += w.getTotalReturn();
			}
		}
		System.out.println("Won: " + won);
		System.out.println("Wagered: " + wagered);
		System.out.println("Diff: " + (won-wagered));

	}

}
