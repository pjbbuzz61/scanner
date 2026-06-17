package collector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.history.Wager;
import scanner.scanner.model.history.bet365.Bet365_Events;
import scanner.scanner.model.history.betmgm.BetMGM_Bet;
import scanner.scanner.model.history.betmgm.BetMGM_BetSlip;
import scanner.scanner.model.history.betmgm.BetMGM_Events;
import scanner.scanner.model.history.betmgm.BetMGM_InfoItem;
import scanner.scanner.model.history.betmgm.BetMGM_PromoToken;
import scanner.scanner.model.history.betrivers.BetRivers_Bet;
import scanner.scanner.model.history.betrivers.BetRivers_CouponRow;
import scanner.scanner.model.history.betrivers.BetRivers_EventGroup;
import scanner.scanner.model.history.betrivers.BetRivers_Events;
import scanner.scanner.model.history.betrivers.BetRivers_Item;
import scanner.scanner.model.history.betrivers.BetRivers_Outcome;
import scanner.scanner.model.history.caesars.Caesars_Bet;
import scanner.scanner.model.history.caesars.Caesars_Events;
import scanner.scanner.model.history.espn.Espn_Container;
import scanner.scanner.model.history.espn.Espn_EventGrouping;
import scanner.scanner.model.history.espn.Espn_Events;
import scanner.scanner.model.history.espn.Espn_datanode;
import scanner.scanner.model.history.fanduel.FanDuel_Bet;
import scanner.scanner.model.history.fanduel.FanDuel_Part;
import scanner.scanner.model.history.fanduel.FanDuel_Wagers;
import scanner.scanner.model.history.hardrock.HardRock_Bet;
import scanner.scanner.model.history.hardrock.HardRock_BetPart;
import scanner.scanner.model.history.hardrock.HardRock_Events;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;
import scanner.scanner.util.history.WAGER_RESULT;
import scanner.scanner.util.history.WAGER_TYPE;

public class Collector {

	WagerService wagerService;
	String collectionName = "wagers_2026";
	static String dataHome = "/home/pat/wagers/2026/";
	static int year = 2026;
	
	
	public static void main(String[] args) {
		
		Collector collector = new Collector();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		collector.wagerService = ws;
		
		
		// Go through each book, look for new files to process
		collector.processBetMGM(dataHome + "betmgm/");
		collector.processCaesars(dataHome + "caesars/");
		collector.processFanduel(dataHome + "fanduel/");
		collector.processEspn(dataHome + "espn/");
		collector.processDraftKings(dataHome + "draftkings/");
		collector.processBetRivers(dataHome + "betrivers/");
		collector.processHardRock(dataHome + "hardrock/");
		collector.processBet365(dataHome + "bet365/");
		

	}


	private void processBet365(String baseDir) {
		
		System.out.println("Processing files for Bet365 ...");
		int wagersInserted = 0;
		int wagerRepeats = 0;

		// Get list of files to process
		List<File> filesToProcess = getFiles(baseDir + "files");

        List<Wager> wagers = new ArrayList<>();

		for(File f : filesToProcess ) {
			
			// To types of files ... json, which is just the wager objects, and html
			String extension = getFileExtension(f);
			switch(extension) {
				case "txt":
					wagers.addAll(processBet365Txt(f));
					break;
				case "json":
					wagers.addAll(processBet365JsonFile(f));
					break;
				default:
					System.out.println("Bet365: Unknown file extension: " + extension);
			}

			moveFile(f, baseDir + "processed/" + f.getName());
		}

		for(Wager w : wagers) {
        	if(wagerService.insert(w, collectionName)) {
        		wagersInserted++;
        	} else {
        		wagerRepeats++;
        	}
//			System.out.println(w);
        }

		System.out.println(filesToProcess.size() + " files were processed for Bet365. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);
	}

	
	private List<Wager> processBet365JsonFile(File f) {
		
        Gson gson = (new GsonBuilder()).create();
        StringBuilder sb = new StringBuilder();
        
		try {
			BufferedReader reader = new BufferedReader(new FileReader(f));
			String line;
			while ((line = reader.readLine()) != null) {
			    sb.append(line);
			}
			reader.close();
		} catch(Exception e) {
			System.out.println("Exception reading json file: " + e.getMessage());
		}

        Bet365_Events rtn = gson.fromJson(sb.toString(), Bet365_Events.class);
        
        return rtn.getRecords();
	}


	private List<Wager> processBet365Txt(File f) {
		
		List<Wager> wagers = new ArrayList<>();
		
		String file = f.getAbsolutePath();

        List<String> lines = new ArrayList<>();
        
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
		} catch(Exception e) {
			System.out.println("Exception reading text file: " + e.getMessage());
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		for(String line : lines) {
			
			if(line.length() < 5) {
				continue;
			}

			Wager w = new Wager();
			
			String parlay = getField("SY", line);
			boolean isParlay = false;
			if(parlay.contentEquals("f") || parlay.contentEquals("g")) {
				isParlay = true;
			}

			try {
				w.setBetTimestamp(sdf.parse(getField("DA", line)));
			} catch (Exception e) {
				System.out.println("Failed to parse the bet timestamp of " + getField("DA", line));
				continue;
			}
			
			if(isParlay) { // the actual game time(s) isn't given, so use the time of the bet
				w.setEventTimestamp(w.getBetTimestamp());
			} else {
				try {
					w.setEventTimestamp(sdf.parse(getField("TP", line)));
				} catch (ParseException e) {
					System.out.println("Failed to parse the event timestamp of " + getField("TP", line));
					continue;
				}
			}

			int odds = 0;
			try {
				int lastIndex = line.lastIndexOf("OD");
				odds = getOdds("OD", line, lastIndex);
			} catch (Exception e) {
				System.out.println("failed to parse the odds fraction: " + e.getMessage());
				continue;
			}
			w.setOriginal_odds(odds);
			w.setBoosted_odds(odds);
			
			List<String> fn  = getAllFields("FN", line);
			List<String> na2 = getAllFields("02;NA", line);
			List<String> mn  = getAllFields("MN", line);
			int index  = -1;
			List<String> na3List = new ArrayList<>();
			
			do {
				index = line.indexOf("03;NA", index);
				if(index != -1) {
					na3List.add(getField("03;NA", line, index));
					index++;
				}
				
			} while(index != -1);
			
			StringBuilder sb = new StringBuilder();
			sb.append(fn + "|");
			sb.append(na2 + "|");
			if(na3List.size() > 0) {
				int i = 0;
				for(String s : na3List) {
					sb.append(s);
					if(i < (na3List.size()-1)) {
						sb.append(";");
					}
					i++;
				}
				sb.append("|");
			}
			sb.append(mn + "|");
			sb.append(String.format("%d", odds));
			
			w.setEventDesc(sb.toString().replace(",", " "));

			w.setBetNumber(getField("BR", line));

			if(isParlay) {
				w.setBetType(WAGER_TYPE.PARLAY);
			} else {
				w.setBetType(WAGER_TYPE.SINGLE);
			}
			
			if(getField("FS", line) != null) {
				w.setBonus(true);
			}

			w.setStake(Double.parseDouble(getField("ST", line)));
			w.setTotalReturn(Double.parseDouble(getField("RT", line)));

			if(w.getTotalReturn() > 0.0) {
				w.setResult(WAGER_RESULT.WIN);
			} else {
				w.setResult(WAGER_RESULT.LOSS);
			}

			w.setSport(getField("L3", line).replace(",", " "));
			w.setLeague(getField("L3", line).replace(",", " "));
			
			w.setState(STATES.VA);
			w.setBook(Sportsbook.BET365);

			wagers.add(w);
			
		} // for each line of file
		
		return wagers;
	}

	private List<String> getAllFields(String s, String line) {

		List<String> rtn = new ArrayList<>();
		int index = -1;
		do {
			index = line.indexOf(s, index);
			if(index != -1) {
				rtn.add(getField(s, line, index));
				index++;
			}
			
		} while(index != -1);

		return rtn;
	}


	private int fractionalToAmerican(double numerator, double denominator) {
        double decimal = numerator / denominator;
        double american;

        if (decimal >= 1.0) {
            // Positive odds (Underdog)
            american = decimal * 100;
        } else {
            // Negative odds (Favorite)
            american = -100 / decimal;
        }

        // Round to nearest whole number for typical American odds
        return (int) Math.round(american);
    }

	private int getOdds(String s, String line, int index) throws Exception {
		String oddsField = getField(s, line,index);
		String[] parts = oddsField.split("/");
		if(parts.length != 2) {
			throw new Exception("Failed to split the fraction");
		}
		return fractionalToAmerican(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
	}


	private String getField(String s, String line) {
		return getField(s, line, 0);
	}
	
	private String getField(String s, String line, int startingPos) {
		
		int index = line.indexOf(s+"=", startingPos);
		if(index != -1) {
			String ss = line.substring(index + s.length() + 1);
			int i = ss.indexOf(';');
			if(i != -1) {
				return ss.substring(0, i);
			}
		}
		return null;
	}


	private void processHardRock(String baseDir) {
		
		System.out.println("Processing files for HardRock ...");
		int wagersInserted = 0;
		int wagerRepeats = 0;

		// Get list of files to process
		List<File> filesToProcess = getFiles(baseDir + "files");

		for(File f : filesToProcess ) {

			String file = f.getAbsolutePath();

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

				HardRock_Events rtn = gson.fromJson(line, HardRock_Events.class);

		        List<Wager> wagers = new ArrayList<>();
		        
		        if((rtn != null) && (rtn.getBets() != null) && (rtn.getBets().getBet() != null)) {

		        	for(HardRock_Bet bet : rtn.getBets().getBet()) {

		        		// The state is not known from the data, so I'll just say Florida
		        		// Could be VA as well
		        		STATES state = STATES.FL;
		        		
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
		    						part.getResultType() + "|" + "Outcome " + score);

							if(betNum < (bet.getParts().getBetPart().size()-1)) {
		    					desc.append("||");
		    				}

		    				w.setSport(sport);
		    				w.setLeague(part.getCompetition().getName());

		    				betNum++;	
		    			}
		    			
		    			w.setEventTimestamp(first);
		    			w.setEventDesc(desc.toString());

		    			w.setBonus(bet.getFreeBet());
		    			
		    			switch(bet.getType()) {
		    				case "SINGLE":
		    					w.setBetType(WAGER_TYPE.SINGLE);
		    					break;
		    				case "MULTIPLE":
		    					w.setBetType(WAGER_TYPE.PARLAY);
		    					break;
		    				default:
		    					System.out.println("UNKNOWN BET TYPE at HARDROCK: " + bet.getType());
		    			}
		    			
		    			switch(bet.getDisplayStatus()) {
							case "LOSE":        w.setResult(WAGER_RESULT.LOSS);        break;
							case "WIN":         w.setResult(WAGER_RESULT.WIN);         break;
							case "CASHED":      w.setResult(WAGER_RESULT.CASHED_OUT);  break;
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
			        
		        for(Wager w : wagers) {
		        	
		        	if(wagerService.insert(w, collectionName)) {
		        		wagersInserted++;
		        	} else {
		        		wagerRepeats++;
		        	}

//		        	System.out.println(w);

		        }
			}

			moveFile(f, baseDir + "processed/" + f.getName());

		} // for file
			
		System.out.println(filesToProcess.size() + " files were processed for HardRock. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);
	}

	
	private void processBetRivers(String baseDir) {
		
		System.out.println("Processing files for BetRivers ...");
		int wagersInserted = 0;
		int wagerRepeats = 0;

		// Get list of files to process
		List<File> filesToProcess = getFiles(baseDir + "files");

		for(File f : filesToProcess ) {

			String file = f.getAbsolutePath();

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

				BetRivers_Events rtn = gson.fromJson(line, BetRivers_Events.class);

		        List<Wager> wagers = new ArrayList<>();
		        
		        if((rtn != null) && (rtn.getItems() != null)) {
		        	for(BetRivers_Item bri : rtn.getItems()) {

		        		Wager w = new Wager();
		        		w.setState(STATES.MD);
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
		        					if(outcome.getSettledInfo() != null) {
			        					for(String s : outcome.getSettledInfo().getResult().getCorrect()) {
			        						cor.append(s + " ");
			        					}
		        					}
		        					desc.append(
		        							outcome.getEventInfo().getEventName() + "|" + 
		        									outcome.getBetOffer().getCriterion() + " " + outcome.getLabel() + "|" + 
		        									convertDecimalToAmerican(row.getPlayedOdds()) + "|" + 
		        									row.getStatus() + "|" + "Outcome: " + cor.toString());
    						
		        					if(betNum < (row.getOutcomes().size()-1)) {
		        						desc.append("||");
		        					}

		        					cor = new StringBuilder();
		        					for(BetRivers_EventGroup eg : outcome.getEventInfo().getEventGroups()) {
		        						cor.append(eg.getName() + "|");
		        					}
		        					w.setSport(cor.toString());
		        					w.setLeague(cor.toString());

		        					betNum++;	
		        				}
		        			}
		        		}
    			
		        		w.setEventTimestamp(first);
		        		
		        		if(notTargetYear(first)) {
		        			continue;
		        		}
		        		w.setEventDesc(desc.toString());

		        		if(bri.getRewardType() != null) {
		        			switch(bri.getRewardType()) {
		        				case "SECOND_CHANCE":
		        					w.setRiskFree(true);
		        					break;
		        				case "PROFIT_BOOST":
		        				case "ODDS_BOOST":
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
			        
		        for(Wager w : wagers) {
		        	if(wagerService.insert(w, collectionName)) {
		        		wagersInserted++;
		        	} else {
		        		wagerRepeats++;
		        	}
//		        	System.out.println(w);
		        }

			}

			moveFile(f, baseDir + "processed/" + f.getName());

		} // for file
			
		System.out.println(filesToProcess.size() + " files were processed for BetRivers. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);
	}



	private void processDraftKings(String baseDir) {
		
		System.out.println("Processing files for DraftKings ...");
		int wagersInserted = 0;
		int wagerRepeats = 0;

		// Get list of files to process
		List<File> filesToProcess = getFiles(baseDir + "files");

		for(File f : filesToProcess ) {

			String file = f.getAbsolutePath();

	        List<String> lines = new ArrayList<>();
	        
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
					lines.add(line);
				}
				reader.close();
			} catch(Exception e) {
				System.out.println("Exception reading html file: " + e.getMessage());
			}

			for(String line : lines) {
				
				Document doc = null;
				try {
					doc = Jsoup.parse(line);
				} catch(Exception e) {
					System.out.println("Exception parsing file: " + e);
					e.printStackTrace();
					continue;
				}

		        List<Wager> wagers = new ArrayList<>();

				List<Element> plays = doc.select("div[data-test-id^=bet-card-]");

				for(Element play : plays) {

					Element teamOfPlay         = play.select("span[data-test-id^=bet-details-title]").first();
					Element displayOdds        = play.select("span[data-test-id^=bet-details-displayOdds]").first();
					Element origDispOdds       = play.select("span[data-test-id^=bet-details-original-displayOdds]").first();
					Element boostDispOdds      = play.select("span[data-test-id^=bet-details-boosted-displayOdds]").first();
					Element playMade           = play.select("span[data-test-id^=bet-details-subtitle]").first();
					Element status             = play.select("div[data-test-id^=bet-details-status]").first();
					
					Element stake              = play.select("span[data-test-id^=bet-stake]").first();
					Element returns            = play.select("span[data-test-id^=bet-returns]").first();
					
					Element bonus              = play.select("button[data-test-id^=playerBonus]").first();
					Element bonus2             = play.select("span[data-test-id^=playerBonus]").first();

					List<Element> team1        = play.select("span[data-test-id^=event-team-name-1]");
					List<Element> team2        = play.select("span[data-test-id^=event-team-name-2]");
					List<Element> event        = play.select("div[data-test-id^=event-displayName]");
					
					//System.out.println("Teams: " + team1.get(0).text() + " " + team2.get(0).text());

					List<Element> eventRefs    = play.select("span[data-test-id^=event-reference]");
					List<Element> betRefs      = play.select("span[data-test-id^=bet-reference]");
					
					@SuppressWarnings("unused")
					List<Element> selTitles    = play.select("div[data-test-id^=bet-selection-title]");
					@SuppressWarnings("unused")
					List<Element> selOdds      = play.select("div[data-test-id^=bet-selection-displayOdds]");
					@SuppressWarnings("unused")
					List<Element> selSubTitles = play.select("div[data-test-id^=bet-selection-subtitle]");

					Wager w = new Wager();
					
					w.setState(STATES.MD); // don't seem to give you way to determine the state
					w.setBook(Sportsbook.DRAFTKINGS);
					w.setBetNumber(betRefs.get(1).text());

					SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy, h:mm:ss a");
					try {
						Date d = sdf.parse(betRefs.get(0).text());
						w.setBetTimestamp(d);
					} catch (ParseException e) {
						System.out.println("Exception parsing begin date: " + e.getMessage());
						System.out.println("Error parsing date: " + betRefs.get(0).text());
						System.exit(0);
					}

					int indexForDate = 0;
					String theDate = null;
					try {
						theDate = eventRefs.get(0).text();
						if(theDate.trim().length() == 0) {
							w.setEventTimestamp(w.getBetTimestamp());
						} else {
							if(theDate.contains("Final Score")) {
								indexForDate = 1;
							} else if(theDate.contains("Bet Ended")) {
								indexForDate = 1;
							}
							Date d = sdf.parse(eventRefs.get(indexForDate).text());
							w.setEventTimestamp(d);
						}
					} catch (Exception e) {
						System.out.println("Exception parsing event date: " + e.getMessage());
						System.out.println("Error parsing date: " + eventRefs.get(indexForDate).text());
						System.exit(0);
					}

					if(notTargetYear(w.getEventTimestamp())) {
	        			continue;
	        		}

					
					if(displayOdds != null) { // no boost
						w.setOriginal_odds(Integer.parseInt(displayOdds.text().replace("\u2212", "-")));
						w.setBoosted_odds(Integer.parseInt(displayOdds.text().replace("\u2212", "-")));
					} else if((origDispOdds != null) || (boostDispOdds != null)) {
						w.setOriginal_odds(Integer.parseInt(origDispOdds.text().replace("\u2212", "-")));
						w.setBoosted_odds(Integer.parseInt(boostDispOdds.text().replace("\u2212", "-")));
					} else {
						System.out.println("Both orig and boost odds not set: " + play.text());
						System.exit(0);
					}

        			switch(status.text()) {
	    				case "Lost":        w.setResult(WAGER_RESULT.LOSS);       break;
	    				case "Won":         w.setResult(WAGER_RESULT.WIN);        break;
	    				case "Cancelled":   w.setResult(WAGER_RESULT.CANCELLED);  break;
	    				case "Cashed Out":  w.setResult(WAGER_RESULT.CANCELLED);  break;
	    				case "Voided":      w.setResult(WAGER_RESULT.CANCELLED);  break;
	    				default: 
	    					System.out.println("New state: " + status.text());
	    					System.exit(0);
        			}

        			
        			w.setStake(Double.parseDouble(stake.text().replace("Wager:", "").replace("$", "").trim()));
        			if(returns != null) {
            			w.setTotalReturn(Double.parseDouble(returns.text().replace("Paid:", "").replace("$", "").trim()));
        			} else {
        				w.setTotalReturn(0.0);
        			}

        			if(bonus != null) {
        				if(bonus.text().contains("NO SWEAT")) {
        					w.setRiskFree(true);
        				} else if(bonus.text().contains("Bonus Bet")) {
        					w.setBonus(true);
        				}
        			}

        			if(bonus2 != null) {
        				if(bonus2.text().contains("NO SWEAT")) {
        					w.setRiskFree(true);
        				} else if(bonus2.text().contains("Bonus Bet")) {
        					w.setBonus(true);
        				}
        			}

        			if(teamOfPlay.text().contains("Picks")) {
        				w.setBetType(WAGER_TYPE.PARLAY);
        			} else {
        				w.setBetType(WAGER_TYPE.SINGLE);
        			}
        			
        			StringBuilder desc = new StringBuilder();
        			if(team1 == null) {
        				desc.append(event.get(0).text() + "|");
        			} else {
        				for(int index = 0; index < team1.size(); ++index) {
        					desc.append(team1.get(index).text() + "@" + team2.get(index).text());
        					if(index < (team1.size()-1)) {
        						desc.append(",");
        					}
        				}
        				desc.append("|");
        			}

        			desc.append(playMade.text() + "|");
        			if(boostDispOdds != null) {
        				desc.append(boostDispOdds.text() + "|");
        			} else {
        				desc.append(displayOdds.text() + "|");
        			}
        			desc.append(status.text());
        			
        			w.setEventDesc(desc.toString());

        			wagers.add(w);
				}

				for(Wager w : wagers) {
		        	if(wagerService.insert(w, collectionName)) {
		        		wagersInserted++;
		        	} else {
		        		wagerRepeats++;
		        	}
//		        	System.out.println(w);
		        }
			}
			moveFile(f, baseDir + "processed/" + f.getName());
		}

		System.out.println(filesToProcess.size() + " files were processed for DraftKings. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);
	}



	private void processEspn(String baseDir) {

		System.out.println("Processing files for ESPN ...");
		int wagersInserted = 0;
		int wagerRepeats = 0;

		// Get list of files to process
		List<File> filesToProcess = getFiles(baseDir + "files");

		for(File f : filesToProcess ) {

			String file = f.getAbsolutePath();

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
		        Espn_Events rtn = null;
		        try {
			        rtn = gson.fromJson(line, Espn_Events.class);
		        } catch(Exception e) {
		        	System.out.println(line);
		        }

		        List<Wager> wagers = new ArrayList<>();
		        
		        if((rtn != null) && 
		           (rtn.getData().getNode() != null) &&
		           (rtn.getData().getNode().getItems() != null) &&
		           (rtn.getData().getNode().getItems().getEdges() != null)) {
		        	
	        		for(Espn_Container e : rtn.getData().getNode().getItems().getEdges()) {
	        			
	        			Espn_datanode node = e.getNode();
	        			
	        			Wager w = new Wager();

        				w.setState(STATES.MD); // not sure how to tell what state I used with these guys

        				w.setBook(Sportsbook.ESPN);

	        			w.setBetNumber(node.getRawId());

	        			w.setBetTimestamp(node.getPlacedAt());
	        			w.setPayoutTimestamp(node.getClosedAt());
	        			
	        			Date first = node.getLegEventGroupings().get(0).getLegs().get(0).getFallbackEvent().getStartTime();
	        			StringBuilder desc = new StringBuilder();
	        			int betNum = 0;
	        			for(Espn_EventGrouping grp : node.getLegEventGroupings()) {

	            			// Event Desc will be of the form:
	            			//  Somebody @ Somebody, play (ex: o141), odds (ex: +123)
	        				desc.append(
	        						grp.getLegs().get(0).getFallbackEvent().getName() + "|" + 
	        						grp.getLegs().get(0).getMarketSelectionName() + "|" + 
	        						node.getTotalOdds().getFormattedOdds() + "|" + 
	        						node.getOutcome());
	        				if(betNum < (node.getLegEventGroupings().size()-1)) {
	        					desc.append("||");
	        				}
	        				w.setSport(grp.getLegs().get(0).getFallbackEvent().getSport().getName());
	        				w.setLeague(grp.getLegs().get(0).getFallbackEvent().getCompetition().getName());
	        				
	        				if(grp.getLegs().get(0).getFallbackEvent().getStartTime().before(first)) {
	        					first = grp.getLegs().get(0).getFallbackEvent().getStartTime();
	        				}
	        				betNum++;
	        			}
	        			w.setEventDesc(desc.toString());

	        			w.setEventTimestamp(first);
	        			if(notTargetYear(first)) {
		        			continue;
		        		}

		        		switch(node.getType()) {
		        			case "PARLAY":
		        			case "PARLAY_PLUS":
		        				w.setBetType(WAGER_TYPE.PARLAY);
		        				break;
		        			case "STRAIGHT":
		        				w.setBetType(WAGER_TYPE.SINGLE);
		        				break;
		        			default:
		        				System.out.println("Unknown wager type for ESPN: " + node.getType());
		        		}

	        			switch(node.getOutcome()) {
	        				case "LOSS":       w.setResult(WAGER_RESULT.LOSS);       break;
	        				case "WIN":        w.setResult(WAGER_RESULT.WIN);        break;
	        				case "CASHED_OUT": w.setResult(WAGER_RESULT.CASHED_OUT); break;
	        				case "CASH_OUT":   w.setResult(WAGER_RESULT.CASHED_OUT); break;
	        				case "REFUND":     w.setResult(WAGER_RESULT.CASHED_OUT); break;
	        				default: 
	        					System.out.println("New state: " + node.getOutcome());
	        			}

	        			w.setOriginal_odds(Integer.parseInt(node.getTotalOdds().getFormattedOdds().replace("Even", "100")));
						w.setBoosted_odds(Integer.parseInt(node.getTotalOdds().getFormattedOdds().replace("Even", "100"))); 
					
						if(node.getAmountSourceType().contentEquals("FREE_BET")) {
							w.setBonus(true);
						}

        			w.setStake(Double.parseDouble(node.getBetAmount().getAmountLong())/100.0);
        			w.setTotalReturn(Double.parseDouble(node.getPayoutAmount().getAmountLong())/100.0);
        			
        			wagers.add(w);

	        		}
		        }
		        
		        for(Wager w : wagers) {
		        	if(wagerService.insert(w, collectionName)) {
		        		wagersInserted++;
		        	} else {
		        		wagerRepeats++;
		        	}
//		        	System.out.println(w);
		        }

			}
			moveFile(f, baseDir + "processed/" + f.getName());

		} // for file
		
		System.out.println(filesToProcess.size() + " files were processed for ESPN. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);
	}

	private void processFanduel(String baseDir) {
		
		System.out.println("Processing files for Fanduel ...");
		int wagersInserted = 0;
		int wagerRepeats = 0;

		// Get list of files to process
		List<File> filesToProcess = getFiles(baseDir + "files");

		for(File f : filesToProcess ) {

			String file = f.getAbsolutePath();

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
		        FanDuel_Wagers rtn = null;
		        try {
			        rtn = gson.fromJson(line, FanDuel_Wagers.class);
		        } catch(Exception e) {
		        	System.out.println(line);
		        }

		        List<Wager> wagers = new ArrayList<>();
		        
		        if((rtn != null) && (rtn.getBets() != null)) {
		        	
	        		for(FanDuel_Bet fnb : rtn.getBets()) {
	        			
	        			Wager w = new Wager();

	        			if(file.contains("_va")) {
	        				w.setState(STATES.VA);
	        			} else if(file.contains("_ks")) {
	        				w.setState(STATES.KS);
	        			} else if(file.contains("_md")) {
	        				w.setState(STATES.MD);
	        			} else {
	        				System.out.println("Can not determine the state, exiting");
	        				System.exit(0);
	        			}
	        			w.setBook(Sportsbook.FANDUEL);

	        			w.setBetNumber(fnb.getBetId());

	        			FanDuel_Part firstPart = null;
	        			//System.out.println("trying");
	        			try {
	        				firstPart = fnb.getLegs().get(0).getParts().get(0);
	        			} catch(Exception e) {
	        				System.out.println("Exception getting first part: " + e.getMessage());
	        				System.exit(0);
	        			}
	        			
	        			w.setBetTimestamp(fnb.getPlacedDate());
	        			w.setEventTimestamp(firstPart.getStartTime());

	        			if(notTargetYear(firstPart.getStartTime())) {
		        			continue;
		        		}
	        			w.setPayoutTimestamp(fnb.getSettledDate());
	        			
	        			w.setEventDesc(firstPart.getEventDescription() + "|" 
	        					+ firstPart.getEventMarketDescription() + "|"
	        					+ firstPart.getSelectionName());

	        			w.setSport(firstPart.getCompetitionName());
	        			w.setLeague(firstPart.getCompetitionName());

	        			if(fnb.getBetType().contentEquals("SGL")) {
	        				w.setBetType(WAGER_TYPE.SINGLE);
	        			} else if(fnb.getBetType().contentEquals("TBL")) {
	        				w.setBetType(WAGER_TYPE.PARLAY);
	        			} else if(fnb.getBetType().startsWith("AC")) {
	        				w.setBetType(WAGER_TYPE.PARLAY);
	        			} else {
	        				System.out.println("Unknown wager type for FanDuel: " + fnb.getBetType());
	        			}

	        			switch(fnb.getResult()) {
	        				case "LOST":              w.setResult(WAGER_RESULT.LOSS);       break;
	        				case "WON":               w.setResult(WAGER_RESULT.WIN);        break;
	        				case "CASHED_OUT":        w.setResult(WAGER_RESULT.CASHED_OUT); break;
	        				case "VOID":              w.setResult(WAGER_RESULT.CANCELLED);  break;
	        				case "VOID_WITH_TOKEN":   w.setResult(WAGER_RESULT.LOSS);       break;
	        				default: 
	        					System.out.println("New state: " + fnb.getResult());
	        			}

	        			if(fnb.getAmericanBetPrice() != null) {
		        			w.setOriginal_odds(fnb.getAmericanBetPrice());
							w.setBoosted_odds(fnb.getAmericanBetPrice()); 
	        			} else {
		        			w.setOriginal_odds(firstPart.getAmericanPrice());
							w.setBoosted_odds(firstPart.getAmericanPrice()); 
	        			}
						
	        			if(fnb.getRewardUsed() != null) {
		        			if(fnb.getRewardUsed().getType().contentEquals("BONUS_BET")) {
			        			w.setBonus(true);
		        			} else if(fnb.getRewardUsed().getType().contentEquals("NO_SWEAT")) {
		        				w.setRiskFree(true);
		        			}
	        			}
	        			
	        			w.setStake(fnb.getCurrentSize());
	        			w.setTotalReturn(fnb.getPandl());
	        			
	        			wagers.add(w);
	        		}
		        }

		        for(Wager w : wagers) {
		        	if(wagerService.insert(w, collectionName)) {
		        		wagersInserted++;
		        	} else {
		        		wagerRepeats++;
		        	}
//		        	System.out.println(w);
		        }

			} // for line in file

			moveFile(f, baseDir + "processed/" + f.getName());

		} // for file
		
		System.out.println(filesToProcess.size() + " files were processed for FanDuel. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);
	}


	private void processCaesars(String baseDir) {
		
		System.out.println("Processing files for Caesars ...");
		int wagersInserted = 0;
		int wagerRepeats = 0;

		// Get list of files to process
		List<File> filesToProcess = getFiles(baseDir + "files");

		for(File f : filesToProcess ) {

			String file = f.getAbsolutePath();

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
		        Caesars_Events rtn = null;
		        try {
			        rtn = gson.fromJson(line, Caesars_Events.class);
		        } catch(Exception e) {
		        	System.out.println(line);
		        }

		        List<Wager> wagers = new ArrayList<>();
		        
		        if(rtn != null) {
		        	if(rtn.getBets() != null) {
		        		for(Caesars_Bet cb : rtn.getBets()) {
		        			Wager w = new Wager();
		        			
		        			switch(cb.getUniverse()) {
			        			case "wh-md": w.setState(STATES.MD); break;
			        			case "wh-va": w.setState(STATES.VA); break;
			        			case "wh-ks": w.setState(STATES.KS); break;
			        			case "wh-dc": w.setState(STATES.DC); break;
			        			default:
			        				System.out.println("Unknown label for state: " + cb.getUniverse());
		        			}
		        			w.setBook(Sportsbook.CAESARS);

		        			w.setBetNumber(cb.getId());
		        			
		        			// Set all the times
		        			w.setBetTimestamp(cb.getPlacedAt());
		        			w.setPayoutTimestamp(cb.getSettledBetData().getSettledAt());

		        			// It appears the event data is not populated for parlays
		        			if(cb.getEventMetadata() != null) {
			        			w.setEventTimestamp(cb.getEventMetadata().getStartTime());
			        			w.setSport(cb.getEventMetadata().getSportId());
		        				w.setLeague(cb.getEventMetadata().getCompetitionName());
		        			} else { // if this field doesnt exist it was a parlay, so just use settled time
			        			w.setEventTimestamp(cb.getSettledBetData().getSettledAt());
		        			}

		        			if(w.getEventTimestamp() == null) {
		        				System.out.println("Failed to find an event time for event " + cb.getId());
		        				continue;
		        			}
			        		if(notTargetYear(w.getEventTimestamp())) {
			        			continue;
			        		}

		        			if(cb.getSelectionMetadata() != null) {
			        			w.setEventDesc(cb.getBetSubtitle() + "|" + cb.getSelectionMetadata().getSelectionName());
		        			} else { // parlay
			        			w.setEventDesc(cb.getBetSubtitle());
		        			}

		        			switch(cb.getBetType()) {
				        		case "parlay":
				        		case "superparlay":
				        		case "sgp":
					        		w.setBetType(WAGER_TYPE.PARLAY);
				        			break;
				        		case "straight":
				        			w.setBetType(WAGER_TYPE.SINGLE);
				        			break;
				       			default:
				       				System.out.println("Unknown wager type for Caesars: " + cb.getBetType());
		        			}

		        			switch(cb.getSettledBetData().getResult()) {
		        				case "lost":        w.setResult(WAGER_RESULT.LOSS);       break;
		        				case "won":         w.setResult(WAGER_RESULT.WIN);        break;
		        				case "void":        w.setResult(WAGER_RESULT.CANCELLED);  break;
		        				case "push":        w.setResult(WAGER_RESULT.CANCELLED);  break;
		        				case "cashed out":  w.setResult(WAGER_RESULT.CASHED_OUT); break;
		        				default: 
		        					System.out.println("New settled.result: " + cb.getSettledBetData().getResult());
		        			}

		        			w.setOriginal_odds(Integer.parseInt(cb.getPrice().getAmerican()));
		        			w.setBoosted_odds(Integer.parseInt(cb.getPrice().getAmerican()));
		        			
		        			if(cb.getWagerType().contentEquals("Bonus Bet")) {
			        			w.setBonus(true);
		        			}
	        				
		        			w.setStake((double)(cb.getTotalStake())/100.0);
		        			w.setTotalReturn((double)(cb.getSettledBetData().getPayout())/100.0);
		        			
		        			wagers.add(w);
		        		}
		        	}
		        }
			
		        for(Wager w : wagers) {
		        	if(wagerService.insert(w, collectionName)) {
		        		wagersInserted++;
		        	} else {
		        		wagerRepeats++;
		        	}
//		        	System.out.println(w);
		        }
			

			} // for line in file

			moveFile(f, baseDir + "processed/" + f.getName());
		} // for file
		
		System.out.println(filesToProcess.size() + " files were processed for Caesars. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);
	}

	private void processBetMGM(String baseDir) {

		System.out.println("Processing BetMGM files ...");
		int wagersInserted = 0;
		int wagerRepeats = 0;
		
		// Get list of files to process
		List<File> filesToProcess = getFiles(baseDir + "files");

		for(File f : filesToProcess ) {

			String file = f.getAbsolutePath();
			
			STATES state = STATES.MD;
			if(file.contains("_va")) {
				state = STATES.VA;
			}
			if(file.contains("_ks")) {
				state = STATES.KS;
			}

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

	        BetMGM_Events rtn = gson.fromJson(sb.toString(), BetMGM_Events.class);
		
	        List<Wager> wagers = new ArrayList<>();
	        
	        if(rtn != null) {
	        	if(rtn.getBetSlips() != null) {
	        		for(BetMGM_BetSlip bs : rtn.getBetSlips()) {
	        			Wager w = new Wager();
	        			w.setState(state);
	        			w.setBook(Sportsbook.BETMGM);

	        			w.setBetNumber(bs.getBetSlipNumber());
	        			w.setBetTimestamp(bs.getConclusionDateUtc());
	        			w.setPayoutTimestamp(null);
	        			
	        			// get all event timestamp for the bets (multiple if a parley)
	        			//  use the earliest for the event start
	        			Date first = bs.getBets().get(0).getFixture().getDate();
	        			StringBuilder desc = new StringBuilder();
	        			int betNum = 0;
	        			for(BetMGM_Bet bet : bs.getBets()) {

	            			// Event Desc will be of the form:
	            			//  Somebody @ Somebody, play (ex: o141), odds (ex: +123)
	        				desc.append(
	        						bet.getFixture().getName() + "|" + 
	        						bet.getMarket().getName() + " " + bet.getOption().getName() + "|" + 
	        						bet.getOdds().getAmerican() + "|" + 
	        						bet.getState() + "|" + "Outcome: " + bet.getOutcome());
	        				if(betNum < (bs.getBets().size()-1)) {
	        					desc.append("||");
	        				}
	        				w.setSport(bet.getSport().getName());
	        				w.setLeague(bet.getCompetition().getName().replace(",", " "));
	        				
	        				if(bet.getFixture().getDate().before(first)) {
	        					first = bet.getFixture().getDate();
	        				}
	        				betNum++;
	        			}
	        			w.setEventDesc(desc.toString());

	        			w.setEventTimestamp(first);
		        		if(notTargetYear(first)) {
		        			continue;
		        		}

	        			switch(bs.getType()) {
			        		case "Parlay":
				        		w.setBetType(WAGER_TYPE.PARLAY);
			        			break;
			        		case "Straight":
			        			w.setBetType(WAGER_TYPE.SINGLE);
			        			break;
			       			default:
			       				System.out.println("Unknown wager type for BetMGM: " + bs.getType());
	        			}

	        			
	        			switch(bs.getState()) {
	        				case "Lost":      w.setResult(WAGER_RESULT.LOSS);       break;
	        				case "Won":       w.setResult(WAGER_RESULT.WIN);        break;
	        				case "Canceled":  w.setResult(WAGER_RESULT.CANCELLED);  break;
	        				default: 
	        					System.out.println("New state: " + bs.getState());
	        			}

	        			w.setOriginal_odds(bs.getTotalOdds().getAmerican());
						w.setBoosted_odds(bs.getTotalOdds().getAmerican()); // default boosted to original until we find an update
						
	        			w.setBonus(bs.getIsFreeBet());
	        			w.setStake(bs.getStake().getValue());
	        			w.setTotalReturn(bs.getPayout().getValue());
	        			
	        			if((bs.getPromoTokens() != null) && (bs.getPromoTokens().size() > 0)) {
	        				for(BetMGM_PromoToken pt : bs.getPromoTokens()) {
	        					switch(pt.getTokenType()) {
	        						case "OddsBoost": 
	        							for(BetMGM_InfoItem ii : pt.getAdditionalInformation().getInformationItems()) {
	        								if(ii.getKey().contentEquals("BoostedOdds.American")) {
	        									w.setBoosted_odds(Integer.parseInt(ii.getValue()));
	        								}
	        								if(ii.getKey().contentEquals("BoostedWinnings")) {
	        									w.setTotalReturn(Double.parseDouble(ii.getValue()));
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
	        			wagers.add(w);
	        		}
	        	}
	        }
	        
	        for(Wager w : wagers) {
	        	if(wagerService.insert(w, collectionName)) {
	        		wagersInserted++;
	        	} else {
	        		wagerRepeats++;
	        	}
//	        	System.out.println(w);
	        }

	        moveFile(f, baseDir + "processed/" + f.getName());
	        
		} // for all files
		
		System.out.println(filesToProcess.size() + " files were processed for BetMGM. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);

	}


	private String getFileExtension(File file) {
	    String fileName = file.getName();
	    int dotIndex = fileName.lastIndexOf('.');
	    if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
	        return ""; 
	    }
	    return fileName.substring(dotIndex + 1);
	}

	private void moveFile(File f, String trgt) {
		
        // move file to processed directory
        Path source = Paths.get(f.getAbsolutePath());
        Path target = Paths.get(trgt);

        try {
            // Move the file, replacing the target file if it already exists
            Files.move(source, target, REPLACE_EXISTING);

        } catch (IOException e) {
            // Handle I/O exceptions, such as file not found or permission issues
            System.err.println("Failed to move file. Reason: " + e.getMessage());
            e.printStackTrace();
        }

	}

	private List<File> getFiles(String dir) {
		
		List<File> rtn = new ArrayList<>();
		
		// Collect all files to process
		File directory = new File(dir);
        File[] filesList = directory.listFiles();

        if (filesList != null) {
            for (File file : filesList) {
                if (file.isFile()) {
                	rtn.add(file);
                }
            }
        }

        return rtn;
	}
	
	private boolean notTargetYear(Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		if(c.get(Calendar.YEAR) != Collector.year) {
			return true;
		}

		return false;
	}

    private int convertDecimalToAmerican(int decimalOdds) {

    	double trueOdds = (double)(decimalOdds) / 1000.0;
        if (trueOdds >= 2.0) {
        	return (int)Math.round((trueOdds - 1) * 100);
        } else {
            return (int)Math.round(-100 / (trueOdds - 1));
        }
    }

    private int convertDecimalToAmerican(double decimalOdds) {

    	double trueOdds = (double)(decimalOdds);
        if (trueOdds >= 2.0) {
        	return (int)Math.round((trueOdds - 1) * 100);
        } else {
            return (int)Math.round(-100 / (trueOdds - 1));
        }
    }



}
