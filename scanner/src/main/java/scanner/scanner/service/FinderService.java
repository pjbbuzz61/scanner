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
import scanner.scanner.model.Spread;
import scanner.scanner.model.Team;
import scanner.scanner.repo.OddsRepo;
import scanner.scanner.util.MLB_STAT;
import scanner.scanner.util.Period;
import scanner.scanner.util.PlayType;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

@Component
public class FinderService {

	@Autowired
	private OddsService oddsService;
	
	private List<Play> playList = new ArrayList<>();
	private int playListLimit = 30;
	
	
	private List<Play> getBestPlays(
			Sport sport, boolean isBonus, 
			Sportsbook book1, double amt1, Double pct1, String part1,
			Sportsbook book2, double amt2, Double pct2, String part2,
			Integer minSrc, Integer maxSrc) {

		List<Odds> oddsList = oddsService.getOdds(sport, Period.GAME);
		List<Odds> source = new ArrayList<>();
		List<Odds> target = new ArrayList<>();
		
		// Make list of books we're looking from to all others
		for(Odds o : oddsList) {
			if(part1 != null) {
				if(
						(o.getAway().getCommonName().contentEquals(part1) == false) 
							&&
						(o.getHome().getCommonName().contentEquals(part1) == false)
						) {
					continue;
					
				}
			}
			if(part2 != null) {
				if(
						(o.getAway().getCommonName().contentEquals(part2) == false) 
							&&
						(o.getHome().getCommonName().contentEquals(part2) == false)
						) {
					continue;
					
				}
			}

			if(book1 == Sportsbook.ANY) {
				source.add(o);
				target.add(o);
			} else {
				if(o.getBook() == book1) {
					source.add(o);
				} else if(book2 != null) { // only add to target list from the specified book, o/w add all
					if(o.getBook() == book2) {
						target.add(o);
					}
				} else {
					target.add(o);
				}

			}
		}

		if(source.size() == 0) {
			System.out.println("No entries for book " + book1);
			return playList;
		}
		if(target.size() == 0) {
			System.out.println("No entries for all other books");
			return playList;
		}

		for(Odds src : source) {
			for(Odds tgt : target) {
				
				if(src.getBook() == tgt.getBook()) {
					continue;
				}

				// If MLB make sure the game times are within one hour
				if(sport == Sport.MLB) {
					if(gameTimesWithinOneHour(src, tgt) == false) {
						continue;
					}
				}

				// swap home and away for target if they match
				if
				(
						src.getHome().getCommonName().contentEquals(tgt.getAway().getCommonName()) 
							&&
						src.getAway().getCommonName().contentEquals(tgt.getHome().getCommonName()) 
				) {
					swapHomeAndAway(tgt);
				}
	
				if(src.getPeriod() != tgt.getPeriod()) {
					continue;
				}
				
				// if this record is MLB stats then the stats must match
				if((src.getMlbStat() != null) && (tgt.getMlbStat() != null)) {
					if(src.getMlbStat() != tgt.getMlbStat()) {
						continue;
					}
					if((src.getPlayer1() != null) && (tgt.getPlayer1() != null)) {
						if(src.getPlayer1().getCommonName().contentEquals(tgt.getPlayer1().getCommonName()) == false) {
							continue;
						}
					}
					if(src.getMlbStat() != MLB_STAT.HR) {
//						continue;
					}
				}
				
				// Check same participants
				if(
						(
							src.getHome().getCommonName().contentEquals(tgt.getHome().getCommonName()) 
								&&
							src.getAway().getCommonName().contentEquals(tgt.getAway().getCommonName()) 
						)
							
					) {
					
					double tgtAmt = 0;
					Double tgtPct = null;

					if(book2 != null) {
						tgtAmt = amt2;
						tgtPct = pct2;
					}

					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.AWAY_MONEYLINE, PlayType.HOME_MONEYLINE, 
									amt1, pct1, tgtAmt, tgtPct, isBonus), minSrc, maxSrc);
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.HOME_MONEYLINE, PlayType.AWAY_MONEYLINE, 
									amt1, pct1, tgtAmt, tgtPct, isBonus), minSrc, maxSrc);
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.HOME_SPREAD, PlayType.AWAY_SPREAD, 
									amt1, pct1, tgtAmt, tgtPct, isBonus), minSrc, maxSrc);
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.AWAY_SPREAD, PlayType.HOME_SPREAD, 
									amt1, pct1, tgtAmt, tgtPct, isBonus), minSrc, maxSrc);
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.OVER, PlayType.UNDER, 
									amt1, pct1, tgtAmt, tgtPct, isBonus), minSrc, maxSrc);
					addToPlayList(
							getPerformance(
									src, tgt, 
									PlayType.UNDER, PlayType.OVER, 
									amt1, pct1, tgtAmt, tgtPct, isBonus), minSrc, maxSrc);
				}
				
			}
		}
		
		return playList;
	}

	private boolean gameTimesWithinOneHour(Odds src, Odds tgt) {
		if(src.getGameDateTime() == null) return false;
		if(tgt.getGameDateTime() == null) return false;
		Long srcTime = src.getGameDateTime().getTime();
		Long tgtTime = tgt.getGameDateTime().getTime();
		long diff = Math.abs(srcTime-tgtTime);
		if(diff < 3600000) {
			return true;
		}
		return false;
	}

	private void swapHomeAndAway(Odds tgt) {

		Team newHome = tgt.getAway();
		Team newAway = tgt.getHome();
		tgt.setHome(newHome);
		tgt.setAway(newAway);
		
		if(tgt.getMl() != null) {
			Spread ml = new Spread();
			ml.setAwayPoints(0.0);
			ml.setHomePoints(0.0);
			ml.setAwayPrice(tgt.getMl().getHomePrice());
			ml.setHomePrice(tgt.getMl().getAwayPrice());
			tgt.setMl(ml);
		}
		
		if(tgt.getSpread() != null) {
			Spread spread = new Spread();
			spread.setAwayPoints(tgt.getSpread().getHomePoints());
			spread.setHomePoints(tgt.getSpread().getAwayPoints());
			spread.setAwayPrice(tgt.getSpread().getHomePrice());
			spread.setHomePrice(tgt.getSpread().getAwayPrice());
			tgt.setSpread(spread);
		}

	}
	

	private void addToPlayList(Play play, Integer minSrc, Integer maxSrc) {

		if(play == null) {
			return;
		}
		if((minSrc != null) && (play.getSrcML() < minSrc)) {
			return;
		}
		if((maxSrc != null) && (play.getSrcML() > maxSrc)) {
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

	private Play getPerformance(Odds src, Odds tgt, PlayType srcPlay, PlayType tgtPlay, 
			double amtSrc, Double pctSrc, double amtTgt, Double pctTgt, boolean isBonus) {

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
		
//		if(src.getPlayer1() != null) {
//			if(src.getPlayer1().getCommonName().contentEquals("JUSTIN CRAWFORD")) {
//				System.out.println("Here");
//			}
//		}
				
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
			
			case AWAY_MONEYLINE: exRtn = getEx(awayML,  awayPts,  amtSrc, 0.0, pctSrc, isBonus, true); break;
			case AWAY_SPREAD:    exRtn = getEx(awayML,  awayPts,  amtSrc, 0.0, pctSrc, isBonus, true); break;
			case HOME_MONEYLINE: exRtn = getEx(homeML,  homePts,  amtSrc, 0.0, pctSrc, isBonus, true); break;
			case HOME_SPREAD:    exRtn = getEx(homeML,  homePts,  amtSrc, 0.0, pctSrc, isBonus, true); break;
			case OVER:           exRtn = getEx(overML,  overPts,  amtSrc, 0.0, pctSrc, isBonus, true); break;
			case UNDER:          exRtn = getEx(underML, underPts, amtSrc, 0.0, pctSrc, isBonus, true); break;
			default:
				System.out.println("WTF src: " + srcPlay);
				return play;
		}
		
		play.setSrcBetAmt(amtSrc);
		play.setSrcRtnAmt(exRtn);
		play.setSrcPlayType(srcPlay);
		
		double exBetAmt = 0.0;
		switch(tgtPlay) {
		
			case AWAY_MONEYLINE: exBetAmt = getEx(awayML,  awayPts,  amtTgt, exRtn, pctTgt, isBonus, false); break;
			case AWAY_SPREAD:    exBetAmt = getEx(awayML,  awayPts,  amtTgt, exRtn, pctTgt, isBonus, false); break;
			case HOME_MONEYLINE: exBetAmt = getEx(homeML,  homePts,  amtTgt, exRtn, pctTgt, isBonus, false); break;
			case HOME_SPREAD:    exBetAmt = getEx(homeML,  homePts,  amtTgt, exRtn, pctTgt, isBonus, false); break;
			case OVER:           exBetAmt = getEx(overML,  overPts,  amtTgt, exRtn, pctTgt, isBonus, false); break;
			case UNDER:          exBetAmt = getEx(underML, underPts, amtTgt, exRtn, pctTgt, isBonus, false); break;
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

	private double getEx(int ml, double pts, double amt, double rtn, Double pct, boolean isBonus, boolean isSrc) {
		
		double actML = (double)ml/100.0;
		if(ml < 0) {
			actML = -100.0/ml;
		}
		
		// Apply power boost if not a bonus and not the other side of the boost play (amt is 0 for other side)
		if((isBonus == false) && (amt > 0.0)) {
			double p = pct/100;
			actML *= (1.0 + pct/100);
		}
		
		double r = 0.0;
		if(isSrc) { // power boost or bonus side (src)
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

		Sport      sport    = null;
		double     amt1     = 0;
		double     amt2     = 0;
		Sportsbook book1    = null;
		Sportsbook book2    = null;
		Double     pct1     = null;
		Double     pct2     = null;
		boolean    isBonus  = true;
		String     part1    = null;
		String     part2    = null;
		Integer    minSrc   = null;
		Integer    maxSrc   = null;
		
		
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

				case "amt1":
					try {
						amt1 = Double.valueOf(parts[1]);
					} catch (Exception e) {
			            System.out.println("Failed to convert amt value of : " + parts[1]);
			            return;
			        }
					break;

				case "amt2":
					try {
						amt2 = Double.valueOf(parts[1]);
					} catch (Exception e) {
			            System.out.println("Failed to convert amt value of : " + parts[1]);
			            return;
			        }
					break;

				case "book1":
					try {
						if(parts[1].contentEquals("DK")) {
							book1 = Sportsbook.DRAFTKINGS;
						} else if(parts[1].contentEquals("FD")) {
							book1 = Sportsbook.FANDUEL;
						} else if(parts[1].contentEquals("BM")) {
							book1 = Sportsbook.BETMGM;
						} else if(parts[1].contentEquals("CS")) {
							book1 = Sportsbook.CAESARS;
						} else if(parts[1].contentEquals("BR")) {
							book1 = Sportsbook.BETRIVERS;
						} else {
							book1 = Sportsbook.valueOf(parts[1]);
						}
					} catch (IllegalArgumentException e) {
			            System.out.println("No Sportsbook enum constant for : " + parts[1]);
			            return;
			        }
					break;
			
				case "book2":
					try {
						if(parts[1].contentEquals("DK")) {
							book2 = Sportsbook.DRAFTKINGS;
						} else if(parts[1].contentEquals("FD")) {
							book2 = Sportsbook.FANDUEL;
						} else if(parts[1].contentEquals("BM")) {
							book2 = Sportsbook.BETMGM;
						} else if(parts[1].contentEquals("CS")) {
							book2 = Sportsbook.CAESARS;
						} else if(parts[1].contentEquals("BR")) {
							book2 = Sportsbook.BETRIVERS;
						} else {
							book2 = Sportsbook.valueOf(parts[1]);
						}
					} catch (IllegalArgumentException e) {
			            System.out.println("No Sportsbook enum constant for : " + parts[1]);
			            return;
			        }
					break;
			
				case "part1":
					part1 = parts[1];
					break;
					
				case "part2":
					part2 = parts[1];
					break;

				case "minSrc":
					minSrc = Integer.parseInt(parts[1]);
					break;

				case "maxSrc":
					maxSrc = Integer.parseInt(parts[1]);
					break;

				case "pct1":
				case "pct":
					try {
						pct1 = Double.valueOf(parts[1]);
						isBonus = false;
					} catch (Exception e) {
			            System.out.println("Failed to convert pct value of : " + parts[1]);
			            return;
			        }
					break;

				case "pct2":
					try {
						pct2 = Double.valueOf(parts[1]);
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
		
		List<Play> bestPlays = service.getBestPlays(
				sport, isBonus,
				book1, amt1, pct1, part1,
				book2, amt2, pct2, part2,
				minSrc, maxSrc);
		for(Play p : bestPlays) {
			System.out.println(p);
		}
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
