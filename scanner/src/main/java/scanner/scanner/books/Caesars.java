package scanner.scanner.books;

import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.OU;
import scanner.scanner.model.Odds;
import scanner.scanner.model.Player;
import scanner.scanner.model.Spread;
import scanner.scanner.model.Team;
import scanner.scanner.model.mlbStats.caesars.Caesars_Market;
import scanner.scanner.model.mlbStats.caesars.Caesars_MarketGroup;
import scanner.scanner.model.mlbStats.caesars.Caesars_Selection;
import scanner.scanner.model.mlbStats.caesars.Caesars_Wrapper;
import scanner.scanner.exceptions.OddsException;
import scanner.scanner.repo.OddsRepo;
import scanner.scanner.repo.PlayerRepo;
import scanner.scanner.repo.TeamRepo;
import scanner.scanner.repo.UpdateRepo;
import scanner.scanner.service.OddsService;
import scanner.scanner.service.PlayerService;
import scanner.scanner.service.TeamService;
import scanner.scanner.service.UpdateService;
import scanner.scanner.util.MLB_STAT;
import scanner.scanner.util.Period;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.Status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;



@Component
public class Caesars extends Book {

	Random random = new Random(System.currentTimeMillis());

	public Caesars(boolean useTheDriver) {
		super(Sportsbook.CAESARS, useTheDriver);
	}

	public Caesars() {
		super(Sportsbook.CAESARS, true);
	}
	
	@Override
	public void acquire(Sport sport) {
		
		try {
			getMatchups(sport);
		} catch (OddsException | IOException e) {
			System.out.println("Exception getting matchups for " + this.sportsbook + ": " + e.getMessage());
		}
		
	}

	private List<Odds> getMatchups(Sport sport) throws IOException, OddsException {

		List<String> files = new ArrayList<>();

		if(sport == Sport.MLB_STATS) {
			List<Odds> list = parseMlbStats();
			return list;
		}

		if(useDriver) {
			
			refresh(sport);
			
			try {

				//WebElement scroll = driver.findElement(By.tagName("body"));

				Actions actions = new Actions(driver);

				WebElement body = driver.findElement(By.tagName("body"));
				List<WebElement> links = body.findElements(By.tagName("li"));
				for(WebElement link : links) {
					if(
							link.getText().contentEquals(sport.toString() + " Odds")
								||
							((sport == Sport.NCAAF) && link.getText().contains("College Football Games"))
							) {
						System.out.println(link.getText());
						javascriptExecutor.executeScript("arguments[0].scrollIntoView(true);", link);
						actions.contextClick(link).build().perform();
						break;
					}
				}

				// Pull up context menu
//				actions.contextClick(scroll).build().perform();

				Robot robot = new Robot();
				
				// Select the debug window from the context menu
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_Q);
				robot.keyRelease(KeyEvent.VK_Q);
				
				// Mouse into the Elements display and click to gain focus
				robot.mouseMove(500,1000);
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_ENTER);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_ENTER);
				Thread.sleep(100);

				// Page up enough times to get to the top
				for(int i = 0; i < 25; ++i) {
					Thread.sleep(100);
					robot.keyPress(KeyEvent.VK_PAGE_UP);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_PAGE_UP);
				}
					
				// move to a spot off the first line
				Thread.sleep(100);
				robot.mouseMove(500,1010);
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_ENTER);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_ENTER);

				// select the first line of the elements output (the body of the html)
				Thread.sleep(100);
				robot.mouseMove(500,910);
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_ENTER);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_ENTER);
					
				// bring up the context menu
				Thread.sleep(100);
				robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
				Thread.sleep(100);
				robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);

				// Go to copy option
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_UP);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_UP);
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_UP);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_UP);
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_UP);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_UP);
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_UP);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_UP);

				// Bring up copy options
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_RIGHT);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_RIGHT);

				// Select copy inner html
				Thread.sleep(1000);
				robot.keyPress(KeyEvent.VK_ENTER);
				Thread.sleep(500);
				robot.keyRelease(KeyEvent.VK_ENTER);
				Thread.sleep(500);
				robot.keyPress(KeyEvent.VK_ENTER);
				Thread.sleep(500);
				robot.keyRelease(KeyEvent.VK_ENTER);
					
			} catch(Exception eee) {
				eee.printStackTrace();
			}
		}
		
		String filename = null;
		if(useDriver) {
			filename = 
					System.getProperty("user.home") + "/" + "SCRAPE_" + 
							this.sportsbook + "_" + System.currentTimeMillis() + ".html"; 
			readClipboard(filename);
		} else {
			
			boolean keepGoing = true;
			Scanner scanner = new Scanner(System.in);
			while(keepGoing) {
				int cntr = 0;
				String fname = System.getProperty("user.home") + "/" + "SCRAPE_" +
						this.sportsbook + "_" + System.currentTimeMillis() + "_" + cntr + ".html"; 
				cntr++;
				
				System.out.print("Copy sport " + sport + " from Caesars to the clipboard, then return (x if done) ");
			    String str = null;
			    try {
			    	str = scanner.nextLine();
			    } catch(Exception er) {
			    	// no nothing - this is just a return with nothing entered
			    }
			    if((str != null) && (str.length() > 0) &&  (str.charAt(0)) == 'x') {
			    	keepGoing = false;
			    } else {
				    readClipboard(fname);
					files.add(fname);
			    }
			}
		    scanner.close();
		}
			

		List<Odds> list = null;
		try {
			switch(sport) {
				case MLB:
					list = parseTeamEvent(files, sport);
					break;
				case NBA:
					list = parseTeamEvent(files, sport);
					break;
				case NCAAM:
					list = parseTeamEvent(files, sport);
					break;
				case NCAAW:
					list = parseTeamEvent(files, sport);
					break;
				case NCAAF:
					list = parseTeamEvent(files, sport);
					break;
				case NFL:
					list = parseTeamEvent(files, sport);
					break;
				case NHL:
					list = parseTeamEvent(files, sport);
					break;
				case SOCCER_EPL:
					break;
				case TENNIS:
					list = parseTeamEvent(files, sport);
					break;
				default:
					break;
			}
		} catch(Exception eee) {
			eee.printStackTrace();
		}
		
		for(String file : files) {
			
			File fileToDelete = new File(file);

			if (fileToDelete.delete()) {
				System.out.println("File deleted successfully: " + file);
			} else {
				System.out.println("Failed to delete the file: " + file);
			}
		}

		if(list != null) {
			for(Odds odds : list) {
				persistOdds(odds, "odds" + "_" + sport);
			}
		}

		return list;
	}
	
	private List<Odds> parseMlbStats() {

		// Read in all the json data. I'll put it in <home_dir>/caesars_mlb_stats.json
        Path path = Paths.get(System.getProperty("user.home") + "/caesars_mlb_stats.json");
        try {
            List<String> allLines = Files.readAllLines(path);
            for (String game : allLines) {
            	processMlbStatsForGame(game);
            }
        } catch (IOException e) {
        	System.out.println("Failed to read in the mlb stats files");
        	e.printStackTrace();
        }
		
		return null;
	}

	private void processMlbStatsForGame(String game) {

        Gson gson = (new GsonBuilder()).setStrictness(Strictness.LENIENT).create();
		Caesars_Wrapper rtn = null;
		try {
			rtn = gson.fromJson(game, Caesars_Wrapper.class);
			processJson(rtn);
		} catch(Exception e) {
			System.out.println("Exception for game: " + e.getLocalizedMessage());
		}


	}

	private void processJson(Caesars_Wrapper rtn) {
		
		// get team names
		String eventName = rtn.getEvent().getName();
		String parts[] = eventName.split(" at ");
		String homeName = parts[1].trim();
		String awayName = parts[0].trim();
		Date gameTime = rtn.getEvent().getStartTime();
		
		Team away = null;
		Team home = null;
		
		boolean failed = false;
		try {
			away = getTeam(this.sportsbook, Sport.MLB_STATS, awayName, true);
		} catch(Exception e3) {
			failed = true;
		}
		try {
			home = getTeam(this.sportsbook, Sport.MLB_STATS, homeName, true);
		} catch(Exception e3) {
			failed = true;
		}
		if(failed) {
			return;
		}

		for(Caesars_MarketGroup group : rtn.getEvent().getKeyMarketGroups()) {

			MLB_STAT mlbStat = null;
			switch(group.getMarketDisplayGroupDisplayName()) {
				case "Total Bases O/U":
					mlbStat = MLB_STAT.BASES;
					processGroup(group, mlbStat, away, home, gameTime);
					break;
				case "Hits + Runs + RBI O/U":
					mlbStat = MLB_STAT.H_R_RBI;
					processGroup(group, mlbStat, away, home, gameTime);
					break;
				case "Hits O/U":
					mlbStat = MLB_STAT.HITS;
					processGroup(group, mlbStat, away, home, gameTime);
					break;
				case "Singles O/U":
					mlbStat = MLB_STAT.SINGLES;
					processGroup(group, mlbStat, away, home, gameTime);
					break;
				case "Doubles O/U":
					mlbStat = MLB_STAT.DOUBLES;
					processGroup(group, mlbStat, away, home, gameTime);
					break;
				case "RBI O/U":
					mlbStat = MLB_STAT.RBI;
					processGroup(group, mlbStat, away, home, gameTime);
					break;
				case "Batter Runs O/U":
					mlbStat = MLB_STAT.RUNS;
					processGroup(group, mlbStat, away, home, gameTime);
					break;
				default:
					// do nothing but ignore
					break;
			}
		}
	}

	private void processGroup(Caesars_MarketGroup group, MLB_STAT mlbStat, Team away, Team home, Date gameTime) {

		List<Odds> oddsList = new ArrayList<>();
		
		for(Caesars_Market market : group.getMarkets()) {
			
			// get the players
			String player = market.getMetadata().getPlayer();
			Team team = away;
			if(market.getMetadata().getTeam().contentEquals("HOME")) {
				team = home;
			}
			Player plyr = null;
			try {
				plyr = getPlayer(team, player);
			} catch(Exception e) {
				System.out.println("Failed to find player: " + player);
				continue;
			}

			Double line = market.getLine();
			Integer overML = null;
			Integer underML = null;
			for(Caesars_Selection selection : market.getSelections()) {
				if(selection.getType().contentEquals("over")) {
					overML = selection.getPrice().getA();
				} else if(selection.getType().contentEquals("under")) {
					underML = selection.getPrice().getA();
				} else {
					System.out.println("Type is neither over or under: " + selection.getType());
					break;
				}
				if((line != null) && (overML != null) && (underML != null)) {
					Odds odds = new Odds();
					odds.setTimeStamp(new Date());
					odds.setBook(this.sportsbook);
					odds.setSport(Sport.MLB_STATS);
					odds.setPeriod(Period.GAME); 
					odds.setStatus(Status.SCHEDULED);
					odds.setMlbStat(mlbStat);
					odds.setGameDateTime(gameTime);
					
					OU ou = new OU();
					ou.setPoints(line);
					ou.setOver(overML);
					ou.setUnder(underML);
					
					odds.setOu(ou);
					odds.setHome(home);
					odds.setAway(away);
					odds.setPlayer1(plyr);
					odds.setPlayer2(plyr);
					odds.setHome(team);
					odds.setAway(team);

					oddsList.add(odds);

				}
			}
		}

		persistOddsForMlbStats(oddsList);

	}

	private List<Odds> parseTeamEvent(List<String> files, Sport sport) {

		StringBuilder sb = new StringBuilder();
		List<Odds> list = new ArrayList<>();

		int numGames = 0;
		for(String file : files) {
			
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String line;
				while ((line = reader.readLine()) != null) {
				    sb.append(line);
				}
				reader.close();

			} catch (IOException e) {
				e.printStackTrace();
				return list;
			}
			
			Document doc = null;
			try {
				doc = Jsoup.parse(sb.toString());
			} catch(Exception e) {
				System.out.println("Exception parsing file: " + e);
				e.printStackTrace();
				return list;
			}
			
			
			Elements container = doc.select("div[data-testid='sport-comp-page-content']");
			Elements games = container.select("div.EventCard");
			for(Element game : games) {
				boolean valid = processEventTeam(game, list, sport);
				if(valid) {
					numGames++;
				}
			}
			
		}

		System.out.println("Number of games read in:   " + numGames);
		System.out.println("Number of games persisted: " + list.size());

		return list;
	}

	private boolean processEventTeam(Element e, List<Odds> list, Sport sport) {
		
		Odds odds = new Odds();
		odds.setTimeStamp(new Date());
		odds.setBook(this.sportsbook);
		odds.setSport(sport);
		odds.setPeriod(Period.GAME); 

		
		Element anchor = e.select("div.EventCard > div > div > div > div > div:nth-child(2)").first();

		if(anchor == null) {
			return false;
		}
		
		Element awayTeamContainer = anchor.select("div.cui-px-md > div > div > div").first();
		Element homeTeamContainer = anchor.select("div.cui-px-md > div:nth-child(2) > div > div").first();

		if(awayTeamContainer == null) {
			System.out.println("Failed to find the away team in : " + anchor.text());
			return false;
		}
		if(homeTeamContainer == null) {
			System.out.println("Failed to find the home team in : " + anchor.text());
			return false;
		}

		// Logic
		// 1. If spans are present use the last one
		// 2. If the last one is all digits, use next to last
		// 3. If no spans just use text of whole thing
		
		String away = null;
		String home = null;
		try {
			Elements atSpans = awayTeamContainer.select("span");
			if(atSpans.size() > 0) {
				if(allDigits(atSpans.get(atSpans.size()-1))) {
					away = atSpans.get(atSpans.size()-2).text();
				} else {
					if(atSpans.size() > 1) {
						away = atSpans.get(atSpans.size()-2).text();
					} else {
						away = awayTeamContainer.text();
					}
				}
			} else {
				away = awayTeamContainer.text();
			}

			Elements htSpans = homeTeamContainer.select("span");
			if(htSpans.size() > 0) {
				if(allDigits(htSpans.get(htSpans.size()-1))) {
					home = htSpans.get(htSpans.size()-2).text();
				} else {
					if(htSpans.size() > 1) {
						home = htSpans.get(htSpans.size()-2).text();
					} else {
						home = homeTeamContainer.text();
					}
				}
			} else {
				home = homeTeamContainer.text();
			}

			home = stripNumbers(home);
			away = stripNumbers(away);
			
		} catch(Exception eee) {
			System.out.println("Exception trying to get team names: " 
					+ awayTeamContainer.text() 
					+ " and " 
					+ homeTeamContainer.text());
			return false;
		}
		
		if(home.contains("Super Bowl Participant")) {
			System.out.println("Not going to process " + away + " at " + home);
			return false;
		}
		if(away.contains("Super Bowl Participant")) {
			System.out.println("Not going to process " + away + " at " + home);
			return false;
		}
		Element awaySpreadCon = anchor.select("div.cui-px-md > div > div > div:nth-child(2)").first();
		Element awayMLCon     = anchor.select("div.cui-px-md > div > div > div:nth-child(3)").first();
		Element overCon       = anchor.select("div.cui-px-md > div > div > div:nth-child(4)").first();

		Element homeSpreadCon = anchor.select("div.cui-px-md > div:nth-child(2) > div > div:nth-child(2)").first();
		Element homeMLCon     = anchor.select("div.cui-px-md > div:nth-child(2) > div > div:nth-child(3)").first();
		Element undrCon       = anchor.select("div.cui-px-md > div:nth-child(2) > div > div:nth-child(4)").first();
		
		Element liveIndicator = anchor.select("div.cui-px-md > div:nth-child(3)").first();

		boolean failed = false;
		try {
			odds.setAway(getTeam(this.sportsbook, sport, away, true));
		} catch(Exception e3) {
			failed = true;
		}
		try {
			odds.setHome(getTeam(this.sportsbook, sport, home, true));
		} catch(Exception e3) {
			failed = true;
		}
		if(failed) {
			return false;
		}

		if((liveIndicator != null) && liveIndicator.text().contains("Live")) {
			return false;
		}
		
		try {
			Elements spans = liveIndicator.select("span");
			if(spans != null) {
				int month=0, day=0, year=0;
				int hour=0, minute=0;
				if(spans.size() == 3) {
					Calendar c = Calendar.getInstance();
					c.setTime(new Date());
					String dateString = spans.get(2).text();
					String[] parts = dateString.split(" "); // should be h:mm PM
					boolean dateSet = false;
					if(spans.get(1).text().contentEquals("Today")) {
						month = c.get(Calendar.MONTH) + 1;
						day = c.get(Calendar.DAY_OF_MONTH);
						year = c.get(Calendar.YEAR);
						String[] hm = parts[0].split(":");
						hour = Integer.parseInt(hm[0]);
						minute = Integer.parseInt(hm[1]);
						if(parts[1].contentEquals("PM")) {
							if(hour != 12) {
								hour +=12;
							}
						} else {
							if(hour == 12) {
								hour = 0;
							}
						}
						dateSet = true;
					} else if(spans.get(1).text().contentEquals("Tomorrow")) {
						c.add(Calendar.DATE, 1);
						month = c.get(Calendar.MONTH) + 1;
						day = c.get(Calendar.DAY_OF_MONTH);
						year = c.get(Calendar.YEAR);
						String[] hm = parts[0].split(":");
						hour = Integer.parseInt(hm[0]);
						minute = Integer.parseInt(hm[1]);
						if(parts[1].contentEquals("PM")) {
							if(hour != 12) {
								hour +=12;
							}
						} else {
							if(hour == 12) {
								hour = 0;
							}
						}
						dateSet = true;
					} else {
						// game is in the future
					}
					if(dateSet) {
						odds.setGameDateTime(
								new SimpleDateFormat("yyyy-MM-dd HH:mm")
									.parse(String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute)));

					}
				
				} // 3 spans to check
			}
		} catch(Exception e33) {
			System.out.println("Exception: " + e33.getMessage());
			e33.printStackTrace();
		}
		

		// Look for live event marker
//		Elements live = atest.select("span._cui__score-xs_cui-_1");
//		if(live.size() > 0) {
//			return;
//		}
		

//		Elements gameTime = e.select("time");
//		System.out.println(gameTime.text());
		// TODO - set game time
		
		Spread spread = new Spread();
		spread.setPeriod(Period.GAME);
		try {
			if(awaySpreadCon.text().contains("--") == false) {
				spread.setAwayPoints(Double.parseDouble(awaySpreadCon.text().split(" ")[0].replace("PICK", "0.0")));
				spread.setHomePoints(Double.parseDouble(homeSpreadCon.text().split(" ")[0].replace("PICK", "0.0")));
				spread.setAwayPrice(Integer.parseInt(awaySpreadCon.text().split(" ")[1]));
				spread.setHomePrice(Integer.parseInt(homeSpreadCon.text().split(" ")[1]));
			}
		} catch(Exception e3) {
			System.out.println(away + " at " + home + ": Failed to parse Spread odds: " 
					+ ((awaySpreadCon == null) ? null: awaySpreadCon.text()) + " " 
					+ ((homeSpreadCon == null) ? null: homeSpreadCon.text()));
			
		}
		odds.setSpread(spread);

		Spread ml = new Spread();
		ml.setAwayPoints(0.0);
		ml.setHomePoints(0.0);
		ml.setPeriod(Period.GAME);
		try {
			if((awayMLCon.text().contains("--") == false) && (homeMLCon.text().contains("--") == false)) {
				ml.setAwayPrice(Integer.parseInt(awayMLCon.text()));
				ml.setHomePrice(Integer.parseInt(homeMLCon.text()));
			}
		} catch(Exception e3) {
			System.out.println(away + " at " + home + ": Failed to parse ML odds: " 
					+ ((awayMLCon == null) ? null: awayMLCon.text()) + " " 
					+ ((homeMLCon == null) ? null: homeMLCon.text()));
		}
		odds.setMl(ml);

		OU ou = new OU();
		ou.setPeriod(Period.GAME);
		try {
			if(overCon.text().contains("--") == false) {
				ou.setPoints(Double.parseDouble(overCon.text().split(" ")[0].trim()));
				ou.setOver(Integer.parseInt(overCon.text().split(" ")[1].trim()));
				ou.setUnder(Integer.parseInt(undrCon.text().split(" ")[1].trim()));
			}
		} catch(Exception e3) {
			System.out.println(away + " at " + home + ": Failed to parse OU odds: " 
					+ ((overCon == null) ? null: overCon.text()) + " " 
					+ ((undrCon == null) ? null: undrCon.text()));
		}
		odds.setOu(ou);

		if((odds.getAway() != null) && (odds.getHome() != null)) {
			list.add(odds);
		} else {
			System.out.println("Not persisting: " + odds);
			return false;
		}

		return true;
	}

	private String stripNumbers(String str) {
		int start = 0;
		int end = str.length()-1;

		for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
            	break;
            } else {
            	start++;
            }
        }

		for(int i = str.length()-1; i >= 0; --i) {
			char c = str.charAt(i);
			if (!Character.isDigit(c)) {
            	break;
            } else {
            	end--;
            }
		}

		return str.substring(start, end+1).trim();
	}

	private boolean allDigits(Element element) {
		String str = element.text().trim();
		if(str == null) {
			return true; // not all digits but dont want to use it
		}
		for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
		return true;
	}

	@SuppressWarnings("unused")
	private Date getStartingDate(String dateString) {

		// Oct 26, 2025 · 4:25 PM
		// 10/27/25 • 8:15 PM (Bet MGM)
		String mnths[] = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "aug", "Sep", "Oct", "Nov", "Dec"};
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());

		String[] parts = dateString.split(" ");
		int month, day, year;
		int hour, minute;
		if(parts[0].contentEquals("Today")) {
			month = c.get(Calendar.MONTH) + 1;
			day = c.get(Calendar.DAY_OF_MONTH);
			year = c.get(Calendar.YEAR);
			String[] hm = parts[2].split(":");
			hour = Integer.parseInt(hm[0]);
			minute = Integer.parseInt(hm[1]);
			if(parts[3].contentEquals("PM")) {
				if(hour != 12) {
					hour +=12;
				}
			} else {
				if(hour == 12) {
					hour = 0;
				}
			}
		} else if(parts[0].contentEquals("Tomorrow")) {
			c.add(Calendar.DATE, 1);
			month = c.get(Calendar.MONTH) + 1;
			day = c.get(Calendar.DAY_OF_MONTH);
			year = c.get(Calendar.YEAR);
			String[] hm = parts[2].split(":");
			hour = Integer.parseInt(hm[0]);
			minute = Integer.parseInt(hm[1]);
			if(parts[3].contentEquals("PM")) {
				if(hour != 12) {
					hour +=12;
				}
			} else {
				if(hour == 12) {
					hour = 0;
				}
			}
		} else if(parts[0].contentEquals("Starting")) {
			if(parts[1].contentEquals("now") == false) {
				c.add(Calendar.MINUTE, Integer.parseInt(parts[2]));
			}
			month = c.get(Calendar.MONTH) + 1;
			day = c.get(Calendar.DAY_OF_MONTH);
			year = c.get(Calendar.YEAR);
			hour = c.get(Calendar.HOUR_OF_DAY);
			minute = c.get(Calendar.MINUTE);
		} else {
			int m = -1;
			for(int i = 0; i < 12; ++i) {
				if(parts[0].contentEquals(mnths[i])) {
					m = i+1;
					break;
				}
			}
			if(m < 0) {
				System.out.println("Failed to parse timestamp: " + dateString);
				return null;
			}
			
			month = m;
			day   = Integer.parseInt(parts[1].replace(",", ""));
			year  = Integer.parseInt(parts[2]);
			String[] hm = parts[4].split(":");
			hour = Integer.parseInt(hm[0]);
			minute = Integer.parseInt(hm[1]);
			if(parts[5].contentEquals("PM")) {
				if(hour != 12) {
					hour +=12;
				}
			} else {
				if(hour == 12) {
					hour = 0;
				}
			}
		}
		// set the starting time
		Date d = null;
		try {
			d = new SimpleDateFormat("yyyy-MM-dd HH:mm")
						.parse(String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute));
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		return d;
	}

	private String readClipboard (String file) {
		
//	     File testFile = new File(file);
	     
	     // get the system clipboard
	     Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	
	     // get the contents on the clipboard in a transferable object
	     Transferable clipboardContents = systemClipboard.getContents(null);

	     // check if clipboard is empty
	     if (clipboardContents.equals(null)) {
	    	 return null;
	     } else

	    	 try {
	    		 // see if DataFlavor of DataFlavor.stringFlavor is supported
	    		 if (clipboardContents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
	    			 // return text content
	    			 String returnText = (String) clipboardContents.getTransferData(DataFlavor.stringFlavor);

	    			 try {
	    				 BufferedWriter writer = 
	    						 new BufferedWriter(new FileWriter(file));
	    				 writer.write(returnText.toString());
	    				 writer.close();
	    			 } catch(Exception e) {}

	    			 return "good";

	    		 }
	    	 } 

	     catch (UnsupportedFlavorException ufe) {
	    	 ufe.printStackTrace();
	     } 
	     catch (IOException ioe) {
	    	 ioe.printStackTrace();
	     }
	     return null;
	}

	private void refresh(Sport sport) {

		List<String> url = new ArrayList<>();
		switch(sport) {
			case MLB:
				url.add("https://sportsbook.caesars.com/us/md/bet/");
				break;
			case NBA:
//				url.add("https://sportsbook.caesars.com/us/md/bet/");
				url.add("https://sportsbook.caesars.com/us/md/bet/basketball?id=5806c896-4eec-4de1-874f-afed93114b8c");
				break;
			case NCAAM:
				url.add("https://sportsbook.caesars.com/us/md/bet/");
				break;
			case NCAAW:
				url.add("https://sportsbook.caesars.com/us/md/bet/");
				break;
			case NCAAF:
				url.add("https://sportsbook.caesars.com/us/md/bet/");
				break;
			case NFL:
				url.add("https://sportsbook.caesars.com/us/md/bet/");
				break;
			case NHL:
				url.add("https://sportsbook.caesars.com/us/md/bet/");
				break;
			case TENNIS:
				break;
			default:
				break;
		}
		try {
			getWindowHandle(sport, url);
		} catch (OddsException e) {
			return;
		}

		return;
		
	}

	private void getWindowHandle(Sport sport, List<String> urlList) throws OddsException {

		for(String url : urlList) {
			int tries = 0;
			boolean success = false;
			while(!success && (tries < 3)) {

				try {
					driver.get(url);
					driver.manage().window().maximize();
					success = true;
				} catch(Exception e) {
					tries++;
					System.out.println(this.sportsbook + ": Failed to GET url, try number " + tries);
				}
			}

			if(success == false) {
				throw new OddsException("Failed to get url for " + this.sportsbook);
			} else {
				try {Thread.sleep(2000L);} catch (InterruptedException e) {}
			}
		}

		// Find the nav window and click link to sport we want
		boolean found = false;
		WebElement root = null;
		for(int i = 0; i < 100; ++i) {
			try {
//				root = driver.findElement(By.cssSelector("nav[id^='carousel')] > div:nth-child(1) > div:nth-child(2) > div:nth-child(1) > div:nth-child(1)"));
				root = driver.findElement(By.cssSelector("nav[id^='carousel')]"));
				found = true;
				break;
			} catch(Exception e) {
			}
			try {Thread.sleep(100L);} catch (InterruptedException e) {}
		}
		if(!found) {
			// In this case there's something wrong with the window, so a refresh is in order
			driver.navigate().refresh();
			System.out.println(this.sportsbook + ": Failed to Sports List: Refreshing window to fix");
			return;
		}

		// Find all anchors
		List<WebElement> links = root.findElements(By.cssSelector("div[id=swiper-slide]"));
		for(WebElement link : links) {
			if(link.getText().contentEquals("More " + sport.toString())) {
				System.out.println(link.getText());
				link.click();
				break;
			}
		}
	
		driver.manage().window().maximize();

		return;
	}
 
	
	public static void main(String args[]) {

		String db = "localhost";

		if(args.length < 2) {
			System.out.println("Requires two args: sport and delete odds flag, along with optional useDriver flag");
			return;
		}
		Sport sport = null;
		switch(args[0].toUpperCase()) {
			case "NHL":       sport = Sport.NHL;       break;
			case "TENNIS":    sport = Sport.TENNIS;    break;
			case "NBA":       sport = Sport.NBA;       break;
			case "NFL":       sport = Sport.NFL;       break;
			case "NCAAF":     sport = Sport.NCAAF;     break;
			case "NCAAM":     sport = Sport.NCAAM;     break;
			case "NCAAW":     sport = Sport.NCAAW;     break;
			case "MLB":       sport = Sport.MLB;       break;
			case "MLB_STATS": sport = Sport.MLB_STATS; break;
			default: System.out.println("Unknown sport: " + args[0]); return;
		}
		System.out.println("Sport is " + sport);

		boolean deleteOdds = false;
		if(args[1].toUpperCase().contentEquals("TRUE")) {
			deleteOdds = true;
		}
		System.out.println("Delete existing set to " + deleteOdds);
		
		boolean useTheDriver = true;
		if(args.length == 3) {
			if(args[2].toUpperCase().contentEquals("USEDRIVER=FALSE")) {
				useTheDriver = false;
			}
		}
		System.out.println("UseDriver is " + useTheDriver);

		if(args.length == 4) {
			if(args[3].toUpperCase().contentEquals("DB=")) {
				String parts[] = args[3].split("=");
				db = parts[1];
			}
		}

		Caesars mgm = new Caesars(useTheDriver);
		TeamService tSrv = new TeamService();
		TeamRepo tRepo = new TeamRepo();
		
		ConnectionString connectionString = new ConnectionString("mongodb://" + db + ":27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		tRepo.setMongoTemplate(mt);
		tSrv.setTeamRepo(tRepo);
		UpdateService uSrv = new UpdateService();
		uSrv.setTeamRepo(tRepo);
		UpdateRepo uRepo = new UpdateRepo();
		uRepo.setMongoTemplate(mt);
		uSrv.setUpdateRepo(uRepo);
		tSrv.setUpdateService(uSrv);
		mgm.setTeamService(tSrv);
		
		PlayerService ps = new PlayerService();
		PlayerRepo pRepo = new PlayerRepo();
		pRepo.setMongoTemplate(mt);
		ps.setRepo(pRepo);
		ps.setUpdateService(uSrv);
		mgm.setPlayerService(ps);

		OddsService os = new OddsService();
		OddsRepo oRepo = new OddsRepo();
		oRepo.setMongoTemplate(mt);
		os.setRepo(oRepo);
		mgm.setOddsService(os);
		
		if(deleteOdds) {
			os.removeAll(sport);
		}
		try {
			mgm.acquire(sport);
		} catch(Exception e) {
			System.out.println("Exception from acquire: " + e);
			e.printStackTrace();
		}
		
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
