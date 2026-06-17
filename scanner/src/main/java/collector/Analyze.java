package collector;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.history.Wager;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.history.WAGER_TYPE;

public class Analyze {

	boolean noParlays = false;
	WagerService wagerService;
	static String collection = "wagers_2026";
	static int SESSION_DEAD_TIME = 2; // number of hours of no activity to end a session
	

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
        String dateString = "2026-01-01T05:00:00Z";
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		Date startOfYear = null;
		Date endOfYear = null;
		try {
			startOfYear = formatter.parse("2026-01-01T00:00:00Z");
			endOfYear  = formatter.parse("2027-01-01T00:00:00Z");
		} catch(Exception e) {
			System.out.println("Failed to parse the year ends");
			System.exit(0);
		}
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
//		System.out.println("Date, Session Winnings, Session Losses");
		Date start = null;
		Date stop = null;
		int analysisYear = 2026;
		try {
			start = formatter.parse(dateString);
	        cal.setTime(start);
	        cal.add(Calendar.DATE, 1); 
	        stop = cal.getTime();
		} catch(Exception e) {
			
		}

		@SuppressWarnings("unused")
		double totalWinnings = 0.0;
		@SuppressWarnings("unused")
		double totalLosses = 0.0;
		
		do {

			// Adjust dates for limits
			Date actStart = start;
			Date actEnd   = stop;
			
			if(start.before(startOfYear)) {
				actStart = startOfYear;
			}
			if(stop.after(endOfYear)) {
				actEnd = endOfYear;
			}

			// Get the wagers for the day, sum up values
			List<Wager> wagers = ws.getWagers(actStart, actEnd, collection);
			double wagered = 0.0;
			double won = 0.0;
			
			for(Wager w : wagers) {
				if(anal.noParlays) {
					if(w.getBetType() == WAGER_TYPE.PARLAY) {
						continue;
					}
				}

				//System.out.println(w);
				if(w.isBonus() == false) {
					wagered += w.getStake();
				}
				won += w.getTotalReturn();

			}
			double netForDay = won - wagered;
			if(netForDay > 0 ) {
//				System.out.println(start + "," + String.format("%.2f",  netForDay) + ",,");
				totalWinnings += netForDay;
			} else {
//				System.out.println(start + ",," + String.format("%.2f",  netForDay));
				totalLosses += -netForDay;
			}

//			String s = String.format("%s %2d  %7.2f", start, wagers.size(), netForDay);
//			System.out.println(s);
	
			
			// go to next days
	        cal.setTime(start);
	        cal.add(Calendar.DATE, 1); 
	        start = cal.getTime();
	        analysisYear = cal.get(Calendar.YEAR);
	        cal.add(Calendar.DATE, 1); 
	        stop = cal.getTime();
			
		} while(analysisYear == 2026);
		
		
		//System.out.println("," + String.format("%.2f", totalWinnings) + "," + String.format("%.2f", totalLosses));
		//System.out.println("Totals: " + String.format("%.2f", (totalWinnings-totalLosses)));
		
	}

	private void sendWagersToCsvFile() {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); 
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date startOfYear = null;
		try {
			startOfYear = sdf.parse("2026-01-01T05:00:00Z");
		} catch(Exception e) {
			
		}
			
		Map<Integer, Double> sessMap = new HashMap<>();
		
		List<Wager> wagers = wagerService.getWagers(collection);
		
		Collections.sort(wagers, new Comparator<Wager>() {
		    @Override
			public int compare(Wager o1, Wager o2) {
		        if(o1.getBetTimestamp().before(o2.getBetTimestamp())) {
		        	return -1;
		        } else if(o1.getBetTimestamp().after(o2.getBetTimestamp())) {
		        	return 1;
		        } else {
		        	return 0;
		        }
		    }
		});

		try {
			BufferedWriter writer = 
				new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/wagers_2026.csv"));

			// Write the header
			writer.write("EventDate,BetDate,Book,Description,BetType,Result,Odds,Boost,Stake,Return,"
					+ "Bonus,NST,Sport,League,PayoutDate,State,BetNumber,Session\n");

			int session = 1;
			Wager prev = null;
			
			for(Wager w : wagers) {

				if(w.getBetTimestamp().before(startOfYear)) {
					continue;
				}

				if(prev != null) {
					if(w.getBetTimestamp().getTime() > 
					  (prev.getBetTimestamp().getTime() + 1000L * 60L * 60L * SESSION_DEAD_TIME)) {
						session++;
					}
				}

				if(noParlays) {
					if(w.getBetType() == WAGER_TYPE.PARLAY) {
						continue;
					}
				}

				String desc = 
						(w.getEventDesc().length() > 120) 
							? 
									w.getEventDesc().replace(",", "|").substring(0,120)
									:
									w.getEventDesc().replace(",", "|");
				String sport = w.getSport();
				if(sport != null) {
					sport = sport.replace(",", " ");
				}
				String league = w.getLeague();
				if(league != null) {
					league = league.replace(",", " ");
				}
				writer.write(

//						System.out.println(
						format(w.getEventTimestamp()) + "," +
						format(w.getBetTimestamp()) + "," +
						w.getBook() + "," +
						desc.replace(",", " ")	+ "," +
						w.getBetType() + "," +
						w.getResult() + "," +
						w.getOriginal_odds() + "," +
						w.getBoosted_odds() + "," +
						w.getStake() + "," +
						w.getTotalReturn() + "," +
						w.isBonus() + "," +
						w.isRiskFree() + "," +
						sport + "," +
						league + "," +
						format(w.getPayoutTimestamp()) + "," +
						w.getState() + "," +
						w.getBetNumber() + "," +
						session + "\n"
						);
				
				// Add to session map
				if(sessMap.get(session) == null) {
					sessMap.put(session, 0.0);
				}
				if(w.isBonus() == false) {
					sessMap.put(session, sessMap.get(session) - w.getStake());
				}
				sessMap.put(session, sessMap.get(session) + w.getTotalReturn());
				
				prev = w;
			}

			writer.close();
			
			
			// make csv for the session data
			writer = 
					new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/sessions_2026.csv"));

			// Write the header
			writer.write("Session,Totals\n");
			
			double tots = 0.0;
			double ls = 0.0;
			double ws = 0.0;
			for (Map.Entry<Integer, Double> m : sessMap.entrySet()) {
				//if(m.getKey() > 161) continue;
			    System.out.println(m.getKey() + " = " + String.format("%7.2f", m.getValue()));
			    writer.write(m.getKey() + "," + String.format("%7.2f", m.getValue()) + "\n");
			    tots += m.getValue();
			    if(m.getValue() < 0.0) {
			    	ls += m.getValue();
			    } else {
			    	ws += m.getValue();
			    }
			}
			System.out.println("Tots: " + String.format("%7.2f", tots));
			System.out.println("ws: " + String.format("%7.2f", ws));
			System.out.println("ls: " + String.format("%7.2f", ls));
			
			writer.close();

			
		} catch(Exception e) {
			System.out.println("Error writing out downtime: " + e.getMessage());
		}

		
	}

	private String format(Date timestamp) {

		SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy'T'HH:mm:ss'Z'"); 
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		if(timestamp != null) {
			return formatter.format(timestamp);
		} else {
			return "";
		}
	}

}
