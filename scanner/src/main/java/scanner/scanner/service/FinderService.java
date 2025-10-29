package scanner.scanner.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoClients;

import scanner.scanner.model.Odds;
import scanner.scanner.model.Play;
import scanner.scanner.repo.OddsRepo;
import scanner.scanner.util.Period;
import scanner.scanner.util.PlayType;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

@Component
public class FinderService {

	@Autowired
	private OddsService oddsService;
	
	private List<Play> playList = new ArrayList<>();
	private int playListLimit = 15;
	
	
	private List<Play> getBestPlays(Sportsbook book, double amt, Sport sport, Double pct, boolean isBonus) {
		
		List<Odds> oddsList = oddsService.getOdds(sport, Period.GAME);
		List<Odds> source = new ArrayList<>();
		List<Odds> target = new ArrayList<>();
		
		// Make list of books we're looking from to all others
		for(Odds o : oddsList) {
			if(o.getBook() == book) {
				source.add(o);
			} else {
				target.add(o);
			}
		}

		if(source.size() == 0) {
			System.out.println("No entries for book " + book);
			return playList;
		}
		if(target.size() == 0) {
			System.out.println("No entries for all other books");
			return playList;
		}

		for(Odds src : source) {
			for(Odds tgt : target) {
				// Check same participants
				if(
						src.getHome().getCommonName().contentEquals(tgt.getHome().getCommonName()) &&
						src.getAway().getCommonName().contentEquals(tgt.getAway().getCommonName()) 
								) {
					
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.AWAY_MONEYLINE, PlayType.HOME_MONEYLINE, 
									amt, pct, isBonus));
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.HOME_MONEYLINE, PlayType.AWAY_MONEYLINE, 
									amt, pct, isBonus));
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.HOME_SPREAD, PlayType.AWAY_SPREAD, 
									amt, pct, isBonus));
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.AWAY_SPREAD, PlayType.HOME_SPREAD, 
									amt, pct, isBonus));
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.OVER, PlayType.UNDER, 
									amt, pct, isBonus));
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.UNDER, PlayType.OVER, 
									amt, pct, isBonus));
				}
				
			}
		}
		
		return playList;
	}

	private void addToPlayList(Play play) {

		if(play == null) {
			return;
		}
		if(playList.size() < playListLimit) {
			playList.add(play);
		} else {
			if(play.getPerformance() > playList.get(playListLimit - 1).getPerformance()) {
				playList.remove(playListLimit - 1);
				playList.add(play);
			}
		}

		playList.sort(Comparator.comparing(Play::getPerformance).reversed());
	}

	private Play getPerformance(Odds src, Odds tgt, PlayType srcPlay, PlayType tgtPlay, double amt,
			Double pct, boolean isBonus) {

		double homePts = 0.0, awayPts  = 0.0;
		int    awayML  = 0,   homeML   = 0;
		double overPts = 0.0, underPts = 0.0;
		int    overML  = 0,   underML  = 0;
		
		boolean noPlay = false;
		
		Play play = null;
		
		play = new Play();
		play.setSrc(src);
		play.setTgt(tgt);
		play.setSrcBook(src.getBook());
		play.setTgtBook(tgt.getBook());

		switch(srcPlay) {

			case AWAY_MONEYLINE:
				awayPts = 0.0;
				play.setSrcPts(awayPts);
				if(src.getMl() == null) return null;
				if(src.getMl().getAwayPrice() != null) {
					awayML = src.getMl().getAwayPrice();
					play.setSrcML(awayML);
				} else {
					noPlay = true;
				}
				break;
			
			case AWAY_SPREAD:
				if(src.getSpread() == null) return null;
				if(src.getSpread().getAwayPoints() != null) {
					awayPts = src.getSpread().getAwayPoints();
					play.setSrcPts(awayPts);
				} else {
					noPlay = true;
				}
				if(src.getSpread().getAwayPrice() != null) {
					awayML = src.getSpread().getAwayPrice();
					play.setSrcML(awayML);
				} else {
					noPlay = true;
				}
				break;

			case HOME_MONEYLINE:
				homePts = 0.0;
				play.setSrcPts(homePts);
				if(src.getMl() == null) return null;
				if(src.getMl().getHomePrice() != null) {
					homeML = src.getMl().getHomePrice();
					play.setSrcML(homeML);
				} else {
					noPlay = true;
				}
				break;

			case HOME_SPREAD:
				if(src.getSpread() == null) return null;
				if(src.getSpread().getHomePoints() != null) {
					homePts = src.getSpread().getHomePoints();
					play.setSrcPts(homePts);
				} else {
					noPlay = true;
				}
				if(src.getSpread().getHomePrice() != null) {
					homeML = src.getSpread().getHomePrice();
					play.setSrcML(homeML);
				} else {
					noPlay = true;
				}
				break;

			case OVER:
				if(src.getOu() == null) return null;
				if(src.getOu().getPoints() != null) {
					overPts = src.getOu().getPoints();
					play.setSrcPts(overPts);
				} else {
					noPlay = true;
				}
				if(src.getOu().getOver() != null) {
					overML = src.getOu().getOver();
					play.setSrcML(overML);
				} else {
					noPlay = true;
				}
				break;

			case UNDER:
				if(src.getOu() == null) return null;
				if(src.getOu().getPoints() != null) {
					underPts = src.getOu().getPoints();
					play.setSrcPts(underPts);
				} else {
					noPlay = true;
				}
				if(src.getOu().getUnder() != null) {
					underML = src.getOu().getUnder();
					play.setSrcML(underML);
				} else {
					noPlay = true;
				}
				break;

			default:
				noPlay = true;
				break;
		}

		if(noPlay) {
			return null;
		}

		switch(tgtPlay) {

			case AWAY_MONEYLINE:
				awayPts = 0.0;
				play.setTgtPts(awayPts);
				if(tgt.getMl() == null) return null;
				if(tgt.getMl().getAwayPrice() != null) {
					awayML = tgt.getMl().getAwayPrice();
					play.setTgtML(awayML);
				} else {
					noPlay = true;
				}
				break;
				
			case AWAY_SPREAD:
				if(tgt.getSpread() == null) return null;
				if(tgt.getSpread().getAwayPoints() != null) {
					awayPts = tgt.getSpread().getAwayPoints();
					play.setTgtPts(awayPts);
				} else {
					noPlay = true;
				}
				if(tgt.getSpread().getAwayPrice() != null) {
					awayML = tgt.getSpread().getAwayPrice();
					play.setTgtML(awayML);
				} else {
					noPlay = true;
				}
				break;

			case HOME_MONEYLINE:
				homePts = 0.0;
				play.setTgtPts(homePts);
				if(tgt.getMl() == null) return null;
				if(tgt.getMl().getHomePrice() != null) {
					homeML = tgt.getMl().getHomePrice();
					play.setTgtML(homeML);
				} else {
					noPlay = true;
				}
				break;

			case HOME_SPREAD:
				if(tgt.getSpread() == null) return null;
				if(tgt.getSpread().getHomePoints() != null) {
					homePts = tgt.getSpread().getHomePoints();
					play.setTgtPts(homePts);
				} else {
					noPlay = true;
				}
				if(tgt.getSpread().getHomePrice() != null) {
					homeML = tgt.getSpread().getHomePrice();
					play.setTgtML(homeML);
				} else {
					noPlay = true;
				}
				break;

			case OVER:
				if(tgt.getOu() == null) return null;
				if(tgt.getOu().getPoints() != null) {
					overPts = tgt.getOu().getPoints();
					play.setTgtPts(overPts);
				} else {
					noPlay = true;
				}
				if(tgt.getOu().getOver() != null) {
					overML = tgt.getOu().getOver();
					play.setTgtML(overML);
				} else {
					noPlay = true;
				}
				break;

			case UNDER:
				if(tgt.getOu() == null) return null;
				if(tgt.getOu().getPoints() != null) {
					underPts = tgt.getOu().getPoints();
					play.setTgtPts(underPts);
				} else {
					noPlay = true;
				}
				if(tgt.getOu().getUnder() != null) {
					underML = tgt.getOu().getUnder();
					play.setTgtML(underML);
				} else {
					noPlay = true;
				}
				break;

			default:
				noPlay = true;
				break;
		}

		if(noPlay) {
			return null;
		}
		
		// Points must match. Unused are preset to 0.0 so they can be checked too
		if(awayPts != -homePts) {
			return null;
		}
		if(overPts != underPts) {
			return null;
		}

		// Expected return for src
		double exRtn = 0.0;
		switch(srcPlay) {
			
			case AWAY_MONEYLINE: exRtn = getEx(awayML,  awayPts,  amt, 0.0, pct, isBonus); break;
			case AWAY_SPREAD:    exRtn = getEx(awayML,  awayPts,  amt, 0.0, pct, isBonus); break;
			case HOME_MONEYLINE: exRtn = getEx(homeML,  homePts,  amt, 0.0, pct, isBonus); break;
			case HOME_SPREAD:    exRtn = getEx(homeML,  homePts,  amt, 0.0, pct, isBonus); break;
			case OVER:           exRtn = getEx(overML,  overPts,  amt, 0.0, pct, isBonus); break;
			case UNDER:          exRtn = getEx(underML, underPts, amt, 0.0, pct, isBonus); break;
			default:
				System.out.println("WTF src: " + srcPlay);
				return play;
		}
		
		play.setSrcBetAmt(amt);
		play.setSrcRtnAmt(exRtn);
		play.setSrcPlayType(srcPlay);
		
		double exBetAmt = 0.0;
		switch(tgtPlay) {
		
			case AWAY_MONEYLINE: exBetAmt = getEx(awayML,  awayPts,  0.0, exRtn, pct, isBonus); break;
			case AWAY_SPREAD:    exBetAmt = getEx(awayML,  awayPts,  0.0, exRtn, pct, isBonus); break;
			case HOME_MONEYLINE: exBetAmt = getEx(homeML,  homePts,  0.0, exRtn, pct, isBonus); break;
			case HOME_SPREAD:    exBetAmt = getEx(homeML,  homePts,  0.0, exRtn, pct, isBonus); break;
			case OVER:           exBetAmt = getEx(overML,  overPts,  0.0, exRtn, pct, isBonus); break;
			case UNDER:          exBetAmt = getEx(underML, underPts, 0.0, exRtn, pct, isBonus); break;
			default:
				System.out.println("WTF tgt: " + tgtPlay);
				return play;
	}
		play.setTgtBetAmt(exBetAmt);
		play.setTgtRtnAmt(exRtn);
		play.setTgtPlayType(tgtPlay);

		if(isBonus) {
			play.setPerformance(exRtn - play.getTgtBetAmt());
		} else {
			play.setPerformance(exRtn - play.getSrcBetAmt() - play.getTgtBetAmt());
		}

		return play;
	}

	private double getEx(int ml, double pts, double amt, double rtn, Double pct, boolean isBonus) {
		
		double actML = (double)ml/100.0;
		if(ml < 0) {
			actML = -100.0/ml;
		}
		
		// Apply power boost if not a bonus and noth the other side of the boost play (amt is 0 for other side)
		if((isBonus == false) && (amt > 0.0)) {
			actML *= (1.0 + pct/100);
		}
		
		double r = 0.0;
		if(amt > 0) { // power boost or bonus side (src)
			r = amt * actML;
			if(isBonus == false) {
				r += amt;
			}
		} else { // tgt side - calc the bet amt to match rtn
			r = rtn/(1+actML);
		}
		return r;
	}

	public static void main(String[] args) {

		Sport      sport   = null;
		Double     amt     = null;
		Sportsbook book    = null;
		Double     pct     = null;
		boolean    isBonus = true;
		
		// Handle input args
		if(args.length == 0) {
			System.out.println("Need input args");
			return;
		}
		for(String s : args) {
			String parts[] = s.split("=");
			if(parts.length != 2) {
				System.out.println("Can not handle input arg of " + s);
				return;
			}

			switch(parts[0]) {

				case "sport":
					try {
						sport = Sport.valueOf(parts[1]);
					} catch (IllegalArgumentException e) {
			            System.out.println("No SPORT enum constant for : " + parts[1]);
			            return;
			        }
					break;

				case "amt":
					try {
						amt = Double.valueOf(parts[1]);
					} catch (Exception e) {
			            System.out.println("Failed to convert amt value of : " + parts[1]);
			            return;
			        }
					break;

				case "book":
					try {
						book = Sportsbook.valueOf(parts[1]);
					} catch (IllegalArgumentException e) {
			            System.out.println("No Sportsbook enum constant for : " + parts[1]);
			            return;
			        }
					break;
			
				case "pct":
					try {
						pct = Double.valueOf(parts[1]);
						isBonus = false;
					} catch (Exception e) {
			            System.out.println("Failed to convert pct value of : " + parts[1]);
			            return;
			        }
					break;
			}
		}
		
		
		FinderService service = new FinderService();
		
		MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "scanner");

		OddsService os = new OddsService();
		OddsRepo oRepo = new OddsRepo();
		oRepo.setMongoTemplate(mongoTemplate);
		os.setRepo(oRepo);
		service.setOddsService(os);
		
		List<Play> bestPlays = service.getBestPlays(book, amt, sport, pct, isBonus);
		for(Play p : bestPlays) {
			System.out.println(p);
		}
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
