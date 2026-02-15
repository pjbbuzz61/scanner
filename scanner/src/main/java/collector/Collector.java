package collector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.util.ArrayList;
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
import scanner.scanner.model.history.betmgm.BetMGM_Bet;
import scanner.scanner.model.history.betmgm.BetMGM_BetSlip;
import scanner.scanner.model.history.betmgm.BetMGM_Events;
import scanner.scanner.model.history.betmgm.BetMGM_InfoItem;
import scanner.scanner.model.history.betmgm.BetMGM_PromoToken;
import scanner.scanner.model.history.caesars.Caesars_Bet;
import scanner.scanner.model.history.caesars.Caesars_Events;
import scanner.scanner.model.history.fanduel.FanDuel_Bet;
import scanner.scanner.model.history.fanduel.FanDuel_Part;
import scanner.scanner.model.history.fanduel.FanDuel_Wagers;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;
import scanner.scanner.util.history.WAGER_RESULT;

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
	

	private void processFanduel(String baseDir) {
		
		System.out.println("Processing files for Fanduel ...");
		
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

	        			w.setBetType(fnb.getBetType());  // Single or Parlay

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
		        	wagerService.insert(w, collectionName);
//		        	System.out.println(w);
		        }

			} // for line in file

			moveFile(f, baseDir + "processed/" + f.getName());

		} // for file
		
		System.out.println(filesToProcess.size() + " files were processed for FanDuel");
	}


	private void processCaesars(String baseDir) {
		
		System.out.println("Processing files for Caesars ...");
		
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

		        			if(cb.getSelectionMetadata() != null) {
			        			w.setEventDesc(cb.getBetSubtitle() + "|" + cb.getSelectionMetadata().getSelectionName());
		        			} else { // parlay
			        			w.setEventDesc(cb.getBetSubtitle());
		        			}
		        			w.setBetType(cb.getBetType());  // Single or Parlay

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
		        	wagerService.insert(w, collectionName);
//		        	System.out.println(w);
		        }
			

			} // for line in file

			moveFile(f, baseDir + "processed/" + f.getName());
		} // for file
		
		System.out.println(filesToProcess.size() + " files were processed for Caesars");
	}

	private void processBetMGM(String baseDir) {

		System.out.println("Processing BetMGM files ...");
		
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

	        			w.setBetType(bs.getType());  // Single or Parlay
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
	        	wagerService.insert(w, collectionName);
	        }

	        moveFile(f, baseDir + "processed/" + f.getName());
	        
		} // for all files
		
		System.out.println(filesToProcess.size() + " files were processed for BetMGM");
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

}
