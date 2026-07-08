package scanner.scanner.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClients;

import scanner.scanner.books.BetMGM;
import scanner.scanner.books.BetRivers;
import scanner.scanner.books.DraftKings;
import scanner.scanner.books.Espn;
import scanner.scanner.books.FanDuel;
import scanner.scanner.model.Play;
import scanner.scanner.model.PlayMade;
import scanner.scanner.model.mlbStats.UpcomingGame;
import scanner.scanner.repo.PlayMadeRepo;
import scanner.scanner.service.FinderService;
import scanner.scanner.util.EmailSender;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

public class MlbStatsController {

	public static void main(String[] args) {

		EmailSender es = new EmailSender();
		PlayMadeRepo repo = new PlayMadeRepo();
		MongoTemplate mongoTemplate = new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "scanner");

		double MIN_WIN_AMT = 0.75;
		repo = new PlayMadeRepo();
		repo.setMongoTemplate(mongoTemplate);

		es.sendEmailWithAttachmentToSelf(
				"Survey Started",
				"Survey Started at " + new Date(),
				null,
				false);
		
		List<Sportsbook> books = Arrays.asList(
//				Sportsbook.BETMGM,
//				Sportsbook.ESPN,
				Sportsbook.FANDUEL,
				Sportsbook.DRAFTKINGS
//				Sportsbook.BETRIVERS
				);
		BetMGM bm = null;
		DraftKings dk = null;
		BetRivers br = null;
		FanDuel fd = null;
		Espn espn = null;

		if(books.contains(Sportsbook.BETMGM)) {
			bm = new BetMGM();
			bm.setUpServices();
			bm.refresh(Sport.MLB_STATS, "https://www.md.betmgm.com/en/sports/baseball-23/betting/usa-9/mlb-75", false);
		}

		if(books.contains(Sportsbook.DRAFTKINGS)) {
			dk = new DraftKings();
			dk.setUpServices();
			dk.refresh(Sport.MLB_STATS, "https://sportsbook.draftkings.com/leagues/baseball/mlb");
		}

		if(books.contains(Sportsbook.BETRIVERS)) {
			br = new BetRivers();
			br.setUpServices();
			br.refresh(Sport.MLB_STATS, "https://md.betrivers.com/?page=sportsbook&group=1000093616&type=prematch");
		}
		
		if(books.contains(Sportsbook.FANDUEL)) {
			fd = new FanDuel();
			fd.setUpServices();
			fd.refresh(Sport.MLB_STATS);
		}

		if(books.contains(Sportsbook.ESPN)) {
			espn = new Espn();
			espn.setUpServices();
			espn.refresh(Sport.MLB_STATS, "https://sportsbook.thescore.bet/sport/baseball/organization/united-states/competition/mlb#lines");
		}

		System.out.println("Done with set up");
		
		// Map will use a key of AwayTeam_HomeTeam_gameTimeAsMs, and a value of the Date the game was last checked
		Map<String, Date> currMap = new HashMap<>();
		
		List<Integer> sentItems = new ArrayList<>();
		
		if(dk != null) {
			dk.getOddsService().removeAll(Sport.MLB_STATS);
		}
		
		// forever loop - set a counter to zero that will one up for each loop
		List<Play> bestPlays = null;
		while(true) {
	
			// TODO - check consec fails at this point, restart if necessary
			
			
			List<UpcomingGame> upcomingGames = new ArrayList<>();

			// get list of current prematch games
			if(books.contains(Sportsbook.BETMGM)) {
				try {
					upcomingGames.addAll(bm.getUpcomingGames());
				} catch (Exception e) {
					System.out.println("Exception getting upcoming games from BM: " + e.getMessage());
				}
			}

			if(books.contains(Sportsbook.FANDUEL)) {
				try {
					upcomingGames.addAll(fd.getUpcomingGames());
				} catch (Exception e) {
					System.out.println("Exception getting upcoming games from FD: " + e.getMessage());
				}
			}

			if(books.contains(Sportsbook.ESPN)) {
				try {
					upcomingGames.addAll(espn.getUpcomingGames());
				} catch (Exception e) {
					System.out.println("Exception getting upcoming games from ESPN: " + e.getMessage());
				}
			}

			if(books.contains(Sportsbook.DRAFTKINGS)) {
				try {
					upcomingGames.addAll(dk.getUpcomingGames());
				} catch (Exception e) {
					System.out.println("Exception getting upcoming games from DK: " + e.getMessage());
				}
			}

			if(books.contains(Sportsbook.BETRIVERS)) {
				try {
					upcomingGames.addAll(br.getUpcomingGames());
				} catch (Exception e) {
					System.out.println("Exception getting upcoming games from BR: " + e.getMessage());
				}
			}

			if(upcomingGames.size() == 0) {
				System.out.println("No games on the list");
				break;
			}
			
			// Remove any games not being played today
			upcomingGames = removeGamesNotToday(upcomingGames);
			
			// remove any map entries that don't have an upcomingGames entry
			cleanUpTable(currMap, upcomingGames);

			// Update map with any games not on it
			addNewGamesToMap(currMap, upcomingGames);
			
			printMap(currMap);
			
			// find game with the oldest last check time and process that
			String oldest = getOldestMapEntry(currMap);

			// Put a stop in if we've already checked this one recently
			if(oldest != null) {
				System.out.println("Time the oldest was last checked: " + currMap.get(oldest));
				if(currMap.get(oldest).after(new Date(System.currentTimeMillis() - 1000L * 60L * 60L))) {
					System.out.println("All games have been checked, going to exit");
					try {
						if(books.contains(Sportsbook.BETMGM)) {
							bm.quitDriver();
						}
						if(books.contains(Sportsbook.DRAFTKINGS)) {
							dk.quitDriver();
						}
						if(books.contains(Sportsbook.ESPN)) {
							espn.quitDriver();
						}
						if(books.contains(Sportsbook.FANDUEL)) {
							fd.quitDriver();
						}
						if(books.contains(Sportsbook.BETRIVERS)) {
							br.quitDriver();
						}

					} catch(Exception e) {
						System.out.println("Exception trying to quit drivers: " + e.getMessage());
						e.printStackTrace();
					}
					
					es.sendEmailWithAttachmentToSelf(
							"Completed Survey",
							"Survey Completed at " + new Date() + 
							"\n" + 
							((bestPlays != null && bestPlays.get(0) != null) ? (bestPlays.get(0)) : ("Best Plays list is empty"))
							+ "\n" + 
							((bestPlays != null && bestPlays.get(2) != null) ? (bestPlays.get(2)) : ("Best Plays list is empty"))
							+ "\n" + 
							((bestPlays != null && bestPlays.get(4) != null) ? (bestPlays.get(4)) : ("Best Plays list is empty"))
							,
							null,
							false);

					System.exit(0);
				}
			}
			
			// Have the sites process the next map entry
			if(oldest == null) {
				System.out.println("Did not find a game to process");
				try {Thread.sleep(60000L);} catch (InterruptedException e4) {}
				continue;
			}

			String parts[] = oldest.split(":");
			String away = parts[0];
			String home = parts[1];
			Date gameTime = new Date(Long.parseLong(parts[2]));


			// for each site, find the game I want on the list and pass in the link
			if(books.contains(Sportsbook.BETMGM)) {
				try {
					bm.acquireMlbStats(getUpcomingGame(  upcomingGames, Sportsbook.BETMGM,     away, home, gameTime));
				} catch(Exception e) {
					System.out.println("Exception processing game for BETMGM: " + e.getMessage());
				}
			}
			
			if(books.contains(Sportsbook.FANDUEL)) {
				try {
					fd.acquireMlbStats(getUpcomingGame(  upcomingGames, Sportsbook.FANDUEL,    away, home, gameTime));
				} catch(Exception e) {
					System.out.println("Exception processing game for FANDUEL: " + e.getMessage());
				}
			}

			if(books.contains(Sportsbook.ESPN)) {
				try {
					espn.acquireMlbStats(getUpcomingGame(upcomingGames, Sportsbook.ESPN,       away, home, gameTime));
				} catch(Exception e) {
					System.out.println("Exception processing game for ESPN: " + e.getMessage());
				}
			}

			if(books.contains(Sportsbook.DRAFTKINGS)) {
				try {
					dk.acquireMlbStats(getUpcomingGame(  upcomingGames, Sportsbook.DRAFTKINGS, away, home, gameTime));
				} catch(Exception e) {
					System.out.println("Exception processing game for DK: " + e.getMessage());
				}
			}

			if(books.contains(Sportsbook.BETRIVERS)) {
				try {
					br.acquireMlbStats(getUpcomingGame(  upcomingGames, Sportsbook.BETRIVERS,  away, home, gameTime));
				} catch(Exception e) {
					System.out.println("Exception processing game for BETRIVERS: " + e.getMessage());
				}
			}
			
			// Update map entry with the time of the latest data grab
			currMap.put(oldest, new Date());

			FinderService fs = new FinderService();
			fs.setUpServices("localhost");
			bestPlays = fs.getBestPlays(
					Sport.MLB_STATS, 
					false, // isBonus,
					Sportsbook.ANY, // book1, 
					10.0,  // amt1, 
					0.0,   // pct1, 
					null,  // part1,
					null,  // book2, 
					0.0,   // amt2, 
					null,  // pct2, 
					null,  // part2,
					null,  // minSrc, 
					null   // maxSrc
					);
			
			for(Play p : bestPlays) {
				FinderService.adjustBetSizes(p);
			}
			bestPlays.sort(Comparator.comparing(Play::getPerformance).reversed());

			for(Play p : bestPlays) {
				System.out.println(p);
//				if(p.getPerformance() >= (FinderService.betSizeMlbStats/100.0)) {
				if(p.getPerformance() >= MIN_WIN_AMT) {

					try {

						if(sentItems.contains(p.getSrc().hashCode())) {
							continue;
						}
						if(sentItems.contains(p.getTgt().hashCode())) {
							continue;
						}
						sentItems.add(p.getSrc().hashCode());
						sentItems.add(p.getTgt().hashCode());


						PlayMade pm = new PlayMade();
						Calendar c = Calendar.getInstance();
						c.setTime(new Date());
						pm.setJulianDate(c.get(Calendar.DAY_OF_YEAR));
						pm.setMlbStat(p.getSrc().getMlbStat());
						pm.setPlayer(p.getSrc().getPlayer1().getCommonName());
						List<PlayMade> plays = repo.find(pm);
						if((plays == null) || (plays.size() == 0)) {
							repo.save(pm);
							es.sendEmailWithAttachmentToSelf(
									p.toStringForEmailSubject(),
									p.toStringForEmailBody(),
									null,
									false);
							es.sendPlainTextEmail(
									p.toStringForEmailSubject(), 
									p.toStringForTextBody() + " " + new Date(),
									false);

						} else {
							System.out.println("Play already recorded!");
						}
						
					} catch(Exception e) {
						System.out.println("Exception emailing the play: " + e.getMessage());
					}
				}
			}
			
			// end of loop pause
			System.out.println("Time at end of loop: " + new Date());
			
		} // for ever loop
	}
		
	private static List<UpcomingGame> removeGamesNotToday(List<UpcomingGame> upcomingGames) {

		List<UpcomingGame> rtn = new ArrayList<>();
		int day = LocalDate.now().getDayOfMonth();
		
		for(UpcomingGame game : upcomingGames) {
	
			if(game.getGameTime() != null) {
				int dayOfGame = game.getGameTime().toInstant()
			              .atZone(ZoneId.systemDefault())
			              .toLocalDate()
			              .getDayOfMonth();			
				if(dayOfGame == day) {
					rtn.add(game);
				}
			}
		}
		
		return rtn;
	}

	private static void printMap(Map<String, Date> currMap) {
		
        List<Entry<String, Date>> list = new ArrayList<>(currMap.entrySet());
        list.sort(Entry.comparingByValue());

        Map<String, Date> result = new LinkedHashMap<>();
        for (Entry<String, Date> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

		for(Map.Entry<String, Date> entry : result.entrySet()) {
			System.out.println(String.format("%-45s: %s", entry.getKey(), entry.getValue()));
		}
	}

	private static void addNewGamesToMap(
			Map<String, Date>  currMap, 
			List<UpcomingGame> upcomingGames) {
		
		for(UpcomingGame game : upcomingGames) {

			if(game.getAway() == null) continue;
			if(game.getHome() == null) continue;
			if(game.getGameTime() == null) continue;

			String key = game.getAway().getCommonName() + ":" + 
			             game.getHome().getCommonName() + ":" + 
					     game.getGameTime().getTime();
			boolean match = false;
			for(Map.Entry<String, Date> entry : currMap.entrySet()) {
				match = gameMatch(entry.getKey(), game);
				if(match) {
					break;
				}
			}
			if(match == false) { // game is not on the map
				System.out.println("Adding new game to map: " + game);
				currMap.put(key, new Date(game.getGameTime().getTime() - System.currentTimeMillis()));
			}

		} // for all games
	}

	private static void cleanUpTable(
			Map<String, Date>  currMap, 
			List<UpcomingGame> upcomingGames) {

		List<String> removeList = new ArrayList<>();
		for(Map.Entry<String, Date> entry : currMap.entrySet()) {
			boolean match = false;
			for(UpcomingGame game : upcomingGames) {
				match = gameMatch(entry.getKey(), game);
				if(match) {
					break;
				}
			} // for all games
			if(match == false) {
				// Map entry not found in the current list, so game must have started
				String parts[] = entry.getKey().split(":");
				Date gameTime = new Date(Long.parseLong(parts[2]));
				System.out.println(
						"Removing entry from map: " + 
								parts[0] + " at " + parts[1] + 
								", start time: " + gameTime);
				removeList.add(entry.getKey());
			}
		}
		for(String s : removeList) {
			currMap.remove(s);
		}
		
	}




	private static UpcomingGame getUpcomingGame(
			List<UpcomingGame> games, 
			Sportsbook book, 
			String away, String home,
			Date gameTime) {

		for(UpcomingGame game : games) {
			if(game.getBook() == book) {
				if(game.getAway().getCommonName().contentEquals(away)) {
					if(game.getHome().getCommonName().contentEquals(home)) {
						Long keyDate = gameTime.getTime();
						Long gameDate = game.getGameTime().getTime();
						long diff = keyDate - gameDate;
						if(Math.abs(diff) < 60*60*1000L) {
							return game;
						}
					}
				}
			}
		}

		System.out.println("Returning no game");
		return null;
	}




	private static String getOldestMapEntry(Map<String, Date> currMap) {

		String oldestKey = null;
		Date oldestDate = new Date(System.currentTimeMillis());
		for(Map.Entry<String, Date> entry : currMap.entrySet()) {
			if(entry.getValue().before(oldestDate)) {
				oldestDate = entry.getValue();
				oldestKey = entry.getKey();
			}
		}

		return oldestKey;
	}

	private static boolean gameMatch(String key, UpcomingGame game) {

		// Will be a match if team common names match and the start time is within an hour
		String parts[] = key.split(":");
		if(game.getAway().getCommonName().contentEquals(parts[0]) == false) {
			return false;
		}
		if(game.getHome().getCommonName().contentEquals(parts[1]) == false) {
			return false;
		}
		Long keyDate = Long.parseLong(parts[2]);
		Long gameDate = game.getGameTime().getTime();
		long diff = keyDate - gameDate;
		if(Math.abs(diff) > 60*60*1000L) {
			return false;
		}
		
		return true;
	}

}
