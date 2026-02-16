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
import java.util.Date;
import java.util.List;

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
		

	}

    private int convertDecimalToAmerican(int decimalOdds) {

    	double trueOdds = (double)(decimalOdds) / 1000.0;
        if (trueOdds >= 2.0) {
        	return (int)Math.round((trueOdds - 1) * 100);
        } else {
            return (int)Math.round(-100 / (trueOdds - 1));
        }
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
		        					for(String s : outcome.getSettledInfo().getResult().getCorrect()) {
		        						cor.append(s + " ");
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

					Element teamOfPlay      = play.select("span[data-test-id^=bet-details-title]").first();
					Element displayOdds     = play.select("span[data-test-id^=bet-details-displayOdds]").first();
					Element origDispOdds    = play.select("span[data-test-id^=bet-details-original-displayOdds]").first();
					Element boostDispOdds   = play.select("span[data-test-id^=bet-details-boosted-displayOdds]").first();
					Element playMade        = play.select("span[data-test-id^=bet-details-subtitle]").first();
					Element status          = play.select("div[data-test-id^=bet-details-status]").first();
					
					Element stake           = play.select("span[data-test-id^=bet-stake]").first();
					Element returns         = play.select("span[data-test-id^=bet-returns]").first();
					
					Element bonus           = play.select("button[data-test-id^=playerBonus]").first();

					List<Element> team1           = play.select("span[data-test-id^=event-team-name-1]");
					List<Element> team2           = play.select("span[data-test-id^=event-team-name-2]");
					List<Element> event           = play.select("div[data-test-id^=event-displayName]");

					List<Element> eventRefs = play.select("span[data-test-id^=event-reference]");
					List<Element> betRefs   = play.select("span[data-test-id^=bet-reference]");
					
					List<Element> selTitles = play.select("div[data-test-id^=bet-selection-title]");
					List<Element> selOdds   = play.select("div[data-test-id^=bet-selection-displayOdds]");
					List<Element> selSubTitles   = play.select("div[data-test-id^=bet-selection-subtitle]");

/*					
					String s = teamOfPlay != null ? teamOfPlay.text() : "";
					System.out.println("teamOfPlay: " + s);

					s = displayOdds != null ? displayOdds.text() : "";
					System.out.println("displayOdds: " + s);
					
					s = origDispOdds != null ? origDispOdds.text() : "";
					System.out.println("origDispOdds: " + s);
					
					s = boostDispOdds != null ? boostDispOdds.text() : "";
					System.out.println("boostDispOdds: " + s);

					s = playMade != null ? playMade.text() : "";
					System.out.println("playMade: " + s);
					
					s = status != null ? status.text() : "";
					System.out.println("status: " + s);

					s = stake != null ? stake.text() : "";
					System.out.println("stake: " + s);
					
					s = returns != null ? returns.text() : "";
					System.out.println("returns: " + s);

					s = bonus != null ? bonus.text() : "";
					System.out.println("bonus: " + s);

					

					if(team1 != null) {
						for(Element e : team1) {System.out.println("team1: " + e.text()); }
					}
					if(team2 != null) {
						for(Element e : team2) {System.out.println("team2: " + e.text()); }
					}
					if(event != null) {
						for(Element e : event) {System.out.println("event: " + e.text()); }
					}
					if(eventRefs != null) {
						for(Element e : eventRefs) {System.out.println("eventRefs: " + e.text()); }
					}
					if(betRefs != null) {
						for(Element e : betRefs) {System.out.println("betRefs: " + e.text()); }
					}
					if(selTitles != null) {
						for(Element e : selTitles) {System.out.println("selTitles: " + e.text()); }
					}
					if(selOdds != null) {
						for(Element e : selOdds) {System.out.println("selOdds: " + e.text()); }
					}
					if(selSubTitles != null) {
						for(Element e : selSubTitles) {System.out.println("selSubTitles: " + e.text()); }
					}	
					
					System.out.println();
*/					
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

					try {
						Date d = sdf.parse(eventRefs.get(1).text());
						w.setEventTimestamp(d);
					} catch (Exception e) {
						System.out.println("Exception parsing event date: " + e.getMessage());
						System.out.println("Error parsing date: " + betRefs.get(0).text());
						System.exit(0);
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
	    				case "Lost":       w.setResult(WAGER_RESULT.LOSS);       break;
	    				case "Won":        w.setResult(WAGER_RESULT.WIN);        break;
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
		        
		        if((rtn != null) && (rtn.getData().getNode().getItems().getEdges() != null)) {
		        	
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

	        			FanDuel_Part firstPart = fnb.getLegs().get(0).getParts().get(0);
	        			
	        			w.setBetTimestamp(fnb.getPlacedDate());
	        			w.setEventTimestamp(firstPart.getStartTime());
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
	        			} else {
	        				System.out.println("Unknown wager type for FanDuel: " + fnb.getBetType());
	        			}

	        			switch(fnb.getResult()) {
	        				case "LOST":       w.setResult(WAGER_RESULT.LOSS);       break;
	        				case "WON":        w.setResult(WAGER_RESULT.WIN);        break;
	        				case "CASHED_OUT": w.setResult(WAGER_RESULT.CASHED_OUT); break;
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

		        			// if no event time, put out a message and continue on
		        			if(w.getEventTimestamp() == null) {
		        				System.out.println("Failed to find an event time for event " + cb.getId());
		        				continue;
		        			}
		        			if(cb.getSelectionMetadata() != null) {
			        			w.setEventDesc(cb.getBetSubtitle() + "|" + cb.getSelectionMetadata().getSelectionName());
		        			} else { // parlay
			        			w.setEventDesc(cb.getBetSubtitle());
		        			}

		        			switch(cb.getBetType()) {
				        		case "parlay":
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
			if(file.contains("settled_va")) {
				state = STATES.VA;
			}
			if(file.contains("settled_ks")) {
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
	        				w.setLeague(bet.getCompetition().getName());
	        				
	        				if(bet.getFixture().getDate().before(first)) {
	        					first = bet.getFixture().getDate();
	        				}
	        				betNum++;
	        			}
	        			w.setEventDesc(desc.toString());

	        			w.setEventTimestamp(first);

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
	        			
	        			w.setBonus(bs.getIsFreeBet());
	        			w.setStake(bs.getStake().getValue());
	        			w.setTotalReturn(bs.getPayout().getValue());
	        			
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
	        }

	        moveFile(f, baseDir + "processed/" + f.getName());
	        
		} // for all files
		
		System.out.println(filesToProcess.size() + " files were processed for BetMGM. Inserted: " 
				+ wagersInserted + " Repeats: " + wagerRepeats);

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
	


}
