package history;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.history.Wager;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;
import scanner.scanner.util.history.WAGER_RESULT;

public class Analyze {

	WagerService wagerService;

	public static void main(String[] args) {

		Analyze anal = new Analyze();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		anal.wagerService = ws;

		
		anal.sendWagersToCsvFile();
		
		
		Calendar cal = Calendar.getInstance();
        String dateString = "2024-12-31T04:00:00Z";
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 
/*
		try {
			Date begin = formatter.parse("2025-09-28T04:00:00Z");
			Date end   = formatter.parse("2025-09-28T04:00:00Z");
			List<Wager> wagersn = ws.getWagers(begin, end);
			for(Wager w : wagersn) {
				System.out.println(w);
			}
		} catch(Exception e) {
			
		}
*/
		
		// Write out headers
		System.out.println("Date, Session Winnings, Session Losses");
		Date start = null;
		Date stop = null;
		int year = 2025;
		try {
			start = formatter.parse(dateString);
	        cal.setTime(start);
	        cal.add(Calendar.DATE, 1); 
	        stop = cal.getTime();
		} catch(Exception e) {
			
		}

		double totalWinnings = 0.0;
		double totalLosses = 0.0;
		
		do {

			// Get the wagers for the day, sum up values
			List<Wager> wagers = ws.getWagers(start, stop);
			double wagered = 0.0;
			double won = 0.0;
			
			for(Wager w : wagers) {
				if(w.isBonus() == false) {
					wagered += w.getStake();
				}
				won += w.getTotalReturn();

			}
			double netForDay = won - wagered;
			if(netForDay > 0 ) {
				System.out.println(start + "," + String.format("%.2f",  netForDay) + ",,");
				totalWinnings += netForDay;
			} else {
				System.out.println(start + ",," + String.format("%.2f",  netForDay));
				totalLosses += -netForDay;
			}

//			String s = String.format("%s %2d  %7.2f", start, wagers.size(), netForDay);
//			System.out.println(s);
	
			
			// go to next days
	        cal.setTime(start);
	        cal.add(Calendar.DATE, 1); 
	        start = cal.getTime();
			year = cal.get(Calendar.YEAR);
	        cal.add(Calendar.DATE, 1); 
	        stop = cal.getTime();
			
		} while(year == 2025);
		
		
		System.out.println("," + String.format("%.2f", totalWinnings) + "," + String.format("%.2f", totalLosses));
		
	}

	private void sendWagersToCsvFile() {

		List<Wager> wagers = wagerService.getWagers();
		
		
		try {
			BufferedWriter writer = 
				new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/wagers_2025.csv"));

			// Write the header
			writer.write("BetDate,EventDate,PayoutDate,BetNumber,BetType,Result,OriginalOdds,BoostedOdds,"
					+ "IsBonusBet,IsRiskFree,stake,TotalReturn,Sport,League,State,Book\n");

			for(Wager w : wagers) {
				writer.write(
//						System.out.println(
						w.getBetTimestamp() + "," +
						w.getEventTimestamp() + "," +
						w.getPayoutTimestamp() + "," +
//						w.getEventDesc().replace("\n", " ") + "," +
						w.getBetNumber() + "," +
						w.getBetType() + "," +
						w.getResult() + "," +
						w.getOriginal_odds() + "," +
						w.getBoosted_odds() + "," +
						w.isBonus() + "," +
						w.isRiskFree() + "," +
						w.getStake() + "," +
						w.getTotalReturn() + "," +
						w.getSport() + "," +
						w.getLeague() + "," +
						w.getState() + "," +
						w.getBook() + "\n"
						);
				
			}

			writer.close();
		} catch(Exception e) {
			System.out.println("Error writing out downtime: " + e.getMessage());
		}

		
	}

}
