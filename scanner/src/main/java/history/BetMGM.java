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
import scanner.scanner.model.history.betmgm.BetMGM_Bet;
import scanner.scanner.model.history.betmgm.BetMGM_BetSlip;
import scanner.scanner.model.history.betmgm.BetMGM_Events;
import scanner.scanner.model.history.betmgm.BetMGM_InfoItem;
import scanner.scanner.model.history.betmgm.BetMGM_PromoToken;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.history.STATES;
import scanner.scanner.util.history.WAGER_RESULT;
import scanner.scanner.util.history.WAGER_TYPE;

public class BetMGM {

	WagerService wagerService;
	
	private void processJsonFile(String file) {

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

//        BetSlipsTest rtn = gson.fromJson(sb.toString(), BetSlipsTest.class);
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
        				if(betNum > 0) {
        					desc.append("\t");
        				}
        				desc.append(
        						bet.getFixture().getName() + "|" + 
        						bet.getMarket().getName() + " " + bet.getOption().getName() + "|" + 
        						bet.getOdds().getAmerican() + "|" + 
        						bet.getState() + "|" + "Outcome: " + bet.getOutcome());
        				if(betNum < (bs.getBets().size()-1)) {
        					desc.append("\n");
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
	
        System.out.println("Number of wagers: " + wagers.size());
        for(Wager w : wagers) {
        	wagerService.insert(w);
        }
	}

	public static void main(String[] args) {

		BetMGM bm = new BetMGM();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		bm.wagerService = ws;
		
		List<String> files = List.of(
//				"/home/pat/2025/betmgm/settled_ks_1.json", 
//				"/home/pat/2025/betmgm/settled_ks_2.json",
//				"/home/pat/2025/betmgm/settled_va_1.json", 
//				"/home/pat/2025/betmgm/settled_va_2.json", 
//				"/home/pat/2025/betmgm/settled_va_3.json", 
//				"/home/pat/2025/betmgm/settled_va_4.json", 
//				"/home/pat/2025/betmgm/settled_va_5.json", 
//				"/home/pat/2025/betmgm/settled_va_6.json", 
//				"/home/pat/2025/betmgm/settled_va_7.json", 
//				"/home/pat/2025/betmgm/settled_va_8.json", 
//				"/home/pat/2025/betmgm/settled_va_9.json", 
//				"/home/pat/2025/betmgm/settled_va_10.json", 
//				"/home/pat/2025/betmgm/settled_md_1.json", 
//				"/home/pat/2025/betmgm/settled_md_2.json", 
//				"/home/pat/2025/betmgm/settled_md_3.json", 
//				"/home/pat/2025/betmgm/settled_md_4.json", 
//				"/home/pat/2025/betmgm/settled_md_5.json", 
//				"/home/pat/2025/betmgm/settled_md_6.json", 
//				"/home/pat/2025/betmgm/settled_md_7.json", 
//				"/home/pat/2025/betmgm/settled_md_8.json", 
//				"/home/pat/2025/betmgm/settled_md_9.json", 
//				"/home/pat/2025/betmgm/settled_md_10.json", 
//				"/home/pat/2025/betmgm/settled_md_11.json", 
//				"/home/pat/2025/betmgm/settled_md_12.json", 
//				"/home/pat/2025/betmgm/settled_md_13.json", 
//				"/home/pat/2025/betmgm/settled_md_14.json", 
//				"/home/pat/2025/betmgm/settled_md_15.json", 
//				"/home/pat/2025/betmgm/settled_md_16.json", 
//				"/home/pat/2025/betmgm/settled_md_17.json", 
//				"/home/pat/2025/betmgm/settled_md_18.json", 
//				"/home/pat/2025/betmgm/settled_md_19.json", 
//				"/home/pat/2025/betmgm/settled_md_20.json", 
//				"/home/pat/2025/betmgm/settled_md_21.json", 
//				"/home/pat/2025/betmgm/settled_md_22.json", 
//				"/home/pat/2025/betmgm/settled_md_23.json", 
//				"/home/pat/2025/betmgm/settled_md_24.json", 
//				"/home/pat/2025/betmgm/settled_md_25.json", 
//				"/home/pat/2025/betmgm/settled_md_26.json", 
//				"/home/pat/2025/betmgm/settled_md_27.json", 
//				"/home/pat/2025/betmgm/settled_md_28.json", 
//				"/home/pat/2025/betmgm/settled_md_29.json",
//				"/home/pat/2025/betmgm/settled_md_30.json",
//				"/home/pat/2025/betmgm/settled_md_31.json",
//				"/home/pat/2025/betmgm/settled_md_32.json",
//				"/home/pat/2025/betmgm/settled_md_33.json",
//				"/home/pat/2025/betmgm/settled_md_34.json",
//				"/home/pat/2025/betmgm/settled_md_35.json",
//				"/home/pat/2025/betmgm/settled_md_36.json",
//				"/home/pat/2025/betmgm/settled_md_37.json",
//				"/home/pat/2025/betmgm/settled_md_38.json",
//				"/home/pat/2025/betmgm/settled_md_39.json",
//				"/home/pat/2025/betmgm/settled_md_40.json",
//				"/home/pat/2025/betmgm/settled_md_41.json",
//				"/home/pat/2025/betmgm/settled_md_42.json",
//				"/home/pat/2025/betmgm/settled_md_43.json",
//				"/home/pat/2025/betmgm/settled_md_44.json",
//				"/home/pat/2025/betmgm/settled_md_45.json",
//				"/home/pat/2025/betmgm/settled_md_46.json",
//				"/home/pat/2025/betmgm/settled_md_47.json",
//				"/home/pat/2025/betmgm/settled_md_48.json",
//				"/home/pat/2025/betmgm/settled_md_49.json",
//				"/home/pat/2025/betmgm/settled_md_50.json",
//				"/home/pat/2025/betmgm/settled_md_51.json",
//				"/home/pat/2025/betmgm/settled_md_52.json",
//				"/home/pat/2025/betmgm/settled_md_53.json",
//				"/home/pat/2025/betmgm/settled_md_54.json",
//				"/home/pat/2025/betmgm/settled_md_55.json",
//				"/home/pat/2025/betmgm/settled_md_56.json",
//				"/home/pat/2025/betmgm/settled_md_57.json"
//				"/home/pat/2025/betmgm/settled_md_58.json",
//				"/home/pat/2025/betmgm/settled_md_59.json",
//				"/home/pat/2025/betmgm/settled_md_60.json",
//				"/home/pat/2025/betmgm/settled_md_61.json",
//				"/home/pat/2025/betmgm/settled_md_62.json",
//				"/home/pat/2025/betmgm/settled_md_63.json",
//				"/home/pat/2025/betmgm/settled_md_64.json",
//				"/home/pat/2025/betmgm/settled_md_65.json"
//				"/home/pat/2025/betmgm/settled_md_66.json",
//				"/home/pat/2025/betmgm/settled_md_67.json",
//				"/home/pat/2025/betmgm/settled_md_68.json",
//				"/home/pat/2025/betmgm/settled_md_69.json",
//				"/home/pat/2025/betmgm/settled_md_70.json",
//				"/home/pat/2025/betmgm/settled_md_71.json",
//				"/home/pat/2025/betmgm/settled_md_72.json",
//				"/home/pat/2025/betmgm/settled_md_73.json",
//				"/home/pat/2025/betmgm/settled_md_74.json",
//				"/home/pat/2025/betmgm/settled_md_75.json",
//				"/home/pat/2025/betmgm/settled_md_76.json",
//				"/home/pat/2025/betmgm/settled_md_77.json",
//				"/home/pat/2025/betmgm/settled_md_78.json",
//				"/home/pat/2025/betmgm/settled_md_79.json",
//				"/home/pat/2025/betmgm/settled_md_80.json",
//				"/home/pat/2025/betmgm/settled_md_81.json",
//				"/home/pat/2025/betmgm/settled_md_82.json",
//				"/home/pat/2025/betmgm/settled_md_83.json",
//				"/home/pat/2025/betmgm/settled_md_84.json"
//				"/home/pat/2025/betmgm/settled_md_85.json",
//				"/home/pat/2025/betmgm/settled_md_86.json",
//				"/home/pat/2025/betmgm/settled_md_87.json",
//				"/home/pat/2025/betmgm/settled_md_88.json",
//				"/home/pat/2025/betmgm/settled_md_89.json",
//				"/home/pat/2025/betmgm/settled_md_90.json",
//				"/home/pat/2025/betmgm/settled_md_91.json",
//				"/home/pat/2025/betmgm/settled_md_92.json",
//				"/home/pat/2025/betmgm/settled_md_93.json",
//				"/home/pat/2025/betmgm/settled_md_94.json",
//				"/home/pat/2025/betmgm/settled_md_95.json",
//				"/home/pat/2025/betmgm/settled_md_96.json",
//				"/home/pat/2025/betmgm/settled_md_97.json",
//				"/home/pat/2025/betmgm/settled_md_98.json",
//				"/home/pat/2025/betmgm/settled_md_99.json",
//				"/home/pat/2025/betmgm/settled_md_100.json",
//				"/home/pat/2025/betmgm/settled_md_101.json"
//				"/home/pat/2025/betmgm/settled_md_102.json",
//				"/home/pat/2025/betmgm/settled_md_103.json",
//				"/home/pat/2025/betmgm/settled_md_104.json",
//				"/home/pat/2025/betmgm/settled_md_105.json",
//				"/home/pat/2025/betmgm/settled_md_106.json",
//				"/home/pat/2025/betmgm/settled_md_107.json",
//				"/home/pat/2025/betmgm/settled_md_108.json",
//				"/home/pat/2025/betmgm/settled_md_109.json",
//				"/home/pat/2025/betmgm/settled_md_110.json",
//				"/home/pat/2025/betmgm/settled_md_111.json",
//				"/home/pat/2025/betmgm/settled_md_112.json",
//				"/home/pat/2025/betmgm/settled_md_113.json",
//				"/home/pat/2025/betmgm/settled_md_114.json",
//				"/home/pat/2025/betmgm/settled_md_115.json"
//				"/home/pat/2025/betmgm/settled_md_116.json",
//				"/home/pat/2025/betmgm/settled_md_117.json",
//				"/home/pat/2025/betmgm/settled_md_118.json",
//				"/home/pat/2025/betmgm/settled_md_119.json",
//				"/home/pat/2025/betmgm/settled_md_120.json",
//				"/home/pat/2025/betmgm/settled_md_121.json",
//				"/home/pat/2025/betmgm/settled_md_122.json",
//				"/home/pat/2025/betmgm/settled_md_123.json",
//				"/home/pat/2025/betmgm/settled_md_124.json",
//				"/home/pat/2025/betmgm/settled_md_125.json",
//				"/home/pat/2025/betmgm/settled_md_126.json",
//				"/home/pat/2025/betmgm/settled_md_127.json",
//				"/home/pat/2025/betmgm/settled_md_128.json",
//				"/home/pat/2025/betmgm/settled_md_129.json",
//				"/home/pat/2025/betmgm/settled_md_130.json",
//				"/home/pat/2025/betmgm/settled_md_131.json",
//				"/home/pat/2025/betmgm/settled_md_132.json",
//				"/home/pat/2025/betmgm/settled_md_133.json"
		);

		
		for(String file : files) {
			bm.processJsonFile(file);
		}
		
		List<Wager> all = ws.getWagers();
		double wagered = 0.0;
		double won = 0.0;
		
		System.out.println("Num wagers: " + all.size());
		for(Wager w : all) {
//			if(w.getState() == STATES.MD) {
				//if(w.isBonus() == false) {
					wagered += w.getStake();
				//}
				won += w.getTotalReturn();
//			}
		}
		System.out.println("Won: " + won);
		System.out.println("Wagered: " + wagered);
		System.out.println("Diff: " + (won-wagered));
	}



}
