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
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.OU;
import scanner.scanner.model.Odds;
import scanner.scanner.model.OuRecord;
import scanner.scanner.model.Player;
import scanner.scanner.model.Spread;
import scanner.scanner.model.Team;
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

@Component
public class DraftKings extends Book {

	Random random = new Random(System.currentTimeMillis());

	public DraftKings(boolean useTheDriver) {
		super(Sportsbook.DRAFTKINGS, useTheDriver);
	}

	public DraftKings() {
		super(Sportsbook.DRAFTKINGS, true);
	}
	
	@Override
	public void acquire(Sport sport) {
		
		List<String> urls = getUrls(sport);
		
		try {
			for(String url : urls) {
				getMatchups(sport, url);
			}
		} catch (OddsException | IOException e) {
			System.out.println("Exception getting matchups for " + this.sportsbook + ": " + e.getMessage());
		}
		
	}

	private List<String> getUrls(Sport sport) {
		
		List<String> urls = new ArrayList<>();
		switch(sport) {
			case MLB:
				urls.add("https://sportsbook.draftkings.com/leagues/baseball/mlb");
//				urls.add("https://sportsbook.draftkings.com/leagues/baseball/mlb-preseason");
//				urls.add("https://sportsbook.draftkings.com/leagues/baseball/world-baseball-classic");
				break;
			case MLB_STATS:
				urls.add("https://sportsbook.draftkings.com/leagues/baseball/mlb");
				break;
			case NBA:
				urls.add("https://sportsbook.draftkings.com/leagues/basketball/nba");
				break;
			case NCAAM:
				urls.add("https://sportsbook.draftkings.com/leagues/basketball/ncaab");
				break;
			case NCAAW:
				urls.add("https://sportsbook.draftkings.com/leagues/basketball/wncaab");
				break;
			case NCAAF:
				urls.add("https://sportsbook.draftkings.com/leagues/football/ncaaf");
				break;
			case NFL:
				urls.add("https://sportsbook.draftkings.com/leagues/football/nfl");
				break;
			case NHL:
				urls.add("https://sportsbook.draftkings.com/leagues/hockey/nhl");
				break;
			case TENNIS:
				urls = getAllTennisUrls();
				break;
			default:
				break;
		
		}
		return urls;
	}

	private List<String> getAllTennisUrls() {

		refresh(Sport.TENNIS, "https://sportsbook.draftkings.com/sports/tennis");
		
		List<String> rtn = new ArrayList<>();

		WebElement tennis = driver.findElement(By.cssSelector("div.sportsbook-a-to-z-sport__content-all"));

		@SuppressWarnings("deprecation")
		String theDom = tennis.getAttribute("outerHTML");
		
		Document doc = null;
		try {
			doc = Jsoup.parse(theDom);
		} catch(Exception e) {
			System.out.println("Error reading the dom: " + e.getMessage());
			return rtn;
		}
		
		Elements links = doc.select("a[href]");
		for(Element link : links) {
			String l = link.attr("href");
			if(l.contains("/atp-") || l.contains("/wta-")) { // || l.contains("/itf-") || l.contains("/challenger-")) {
				if(!l.contains("doubles")) {
					rtn.add("https://sportsbook.draftkings.com" + l);
				}
			}
		}
		
		return rtn;
	}

	private List<Odds> getMatchups(Sport sport, String url) throws IOException, OddsException {

		if(useDriver) {
			
			refresh(sport, url);
			if(sport == Sport.MLB_STATS) {
				List<Odds> list = parseMlbStats();
				quitDriver();
				return list;
			}
			
			try {

				WebElement scroll = driver.findElement(By.cssSelector("section[data-testid='league-page-widget-container']"));

				Actions actions = new Actions(driver);

				// Pull up context menu
				actions.contextClick(scroll).build().perform();

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
			} catch(Exception e) {
				// might not be a visible scrollbar
				System.out.println(e);
				e.printStackTrace();
			}
		}
				
		String filename = null;
		if(useDriver) {
			filename = 
					System.getProperty("user.home") + "/" + "SCRAPE_" + 
							this.sportsbook + "_" + System.currentTimeMillis() + ".html"; 
			readClipboard(filename);
		} else {
			filename = 
					System.getProperty("user.home") + "/" + this.sportsbook + "_" + sport +  ".html"; 
				
			Scanner scanner = new Scanner(System.in);
			System.out.print("Copy sport " + sport + " from Draftkings to the clipboard, then return");
			scanner.nextLine();
			scanner.close();
			readClipboard(filename);
		}

		List<Odds> list = null;
		try {
			switch(sport) {
				case MLB:
					list = parseTeamEvent(filename, sport);
					break;
				case NBA:
					list = parseTeamEvent(filename, sport);
					break;
				case NCAAM:
				case NCAAW:
					list = parseTeamEvent(filename, sport);
					break;
				case NCAAF:
					list = parseTeamEvent(filename, sport);
					break;
				case NFL:
					list = parseTeamEvent(filename, sport);
					break;
				case NHL:
					list = parseTeamEvent(filename, sport);
					break;
				case SOCCER_EPL:
					break;
				case TENNIS:
					list = parseTeamEvent(filename, sport);
					break;
				default:
					break;
			}
		} catch(Exception eee) {
			eee.printStackTrace();
		}
		File fileToDelete = new File(filename);

		if (fileToDelete.delete()) {
			System.out.println("File deleted successfully: " + filename);
		} else {
			System.out.println("Failed to delete the file: " + filename);
		}

		for(Odds odds : list) {
			persistOdds(odds, "odds" + "_" + sport);
		}
	        
		return list;
	}
	
	private List<Odds> parseMlbStats() {
		
		List<Odds> oddsList = new ArrayList<>();
		
		List<WebElement> containers = null;
		WebElement container = null;;
		List<WebElement> games = null;
		
		containers = driver.findElements(By.cssSelector("div[data-testid=marketboard]"));
		int numContainers = containers.size();

		for(int cont = 0; cont < numContainers; ++cont) {

			int numAttempts = 0;
			boolean success = false;
			do {
				try {
					containers = driver.findElements(By.cssSelector("div[data-testid=marketboard]"));
					container = containers.get(cont);
					games = container.findElements(By.cssSelector("div.cb-static-parlay__content--inner"));
					success = true;
				} catch(Exception eee) {
					try {Thread.sleep(200L);} catch (InterruptedException ew) {}
					numAttempts++;
				}
				
			} while((numAttempts < 5) && (success == false));
			
			if(success == false) {
				System.out.println("Failed to load page: Cont: " + cont);
				continue;
			}
			if((games.size() % 3) != 0) {
				System.out.println("Problem: Should be three containers for each game");
				return oddsList;
			}
			
			int len = games.size();
			for(int i = 0; i < len; i+=3) { // for each game

				int tries = 0;
				boolean worked = false;
				do {
					
					try {
						// reload the pages -- do this because we will have reloaded the page below
						containers = driver.findElements(By.cssSelector("div[data-testid=marketboard]"));
						container = containers.get(cont);
						games = container.findElements(By.cssSelector("div.cb-static-parlay__content--inner"));

						processEventTeamMlbStats(games.get(i+0), games.get(i+1), games.get(i+2), oddsList);
						worked = true;
					} catch(Exception e) {
						System.out.println("Failed to process game at DK, trying again. Tries is " + tries + ", Msg: " + e.getMessage());
						try {Thread.sleep(200L);} catch (InterruptedException ew) {}
						tries++;
					}

				} while((tries < 10) && (worked == false));
				
				persistOddsForMlbStats(oddsList);
			}

		}
		
		return oddsList;
	}

	private void processEventTeamMlbStats(WebElement match, WebElement time, WebElement unused, List<Odds> oddsList) {
		
		Team away = null;
		Team home = null;
		String awayName = null;
		String homeName = null;
		
		List<WebElement> teams = match.findElements(By.cssSelector("div.cb-market__label-team-wrapper--col"));
		List<WebElement> spans_away = teams.get(0).findElements(By.tagName("span"));
		List<WebElement> spans_home = teams.get(1).findElements(By.tagName("span"));
		
		boolean failed = false;
		try {
			awayName = spans_away.get(0).getText();
			away = getTeam(this.sportsbook, Sport.MLB_STATS, awayName, true);
		} catch(Exception e3) {
			failed = true;
		}
		try {
			homeName = spans_home.get(0).getText();
			home = getTeam(this.sportsbook, Sport.MLB_STATS, homeName, true);
		} catch(Exception e3) {
			failed = true;
		}
		if(failed) {
			return;
		}

		// See if game is live -- if so, skip it
		List<WebElement> live = time.findElements(By.cssSelector("svg[data-testid=live-badge]"));
		if(live.size() > 0) {
			return;
		}

		WebElement startTime = time.findElement(By.cssSelector("span.cb-event-cell__start-time"));
		Date gameTime = null;
		if(startTime != null) {
			gameTime = getGameTime(startTime.getText());
		}

		// click on the game
		System.out.println("Going to click for game: " + away.getCommonName() + " at " + home.getCommonName());
		WebElement c = match.findElement(By.cssSelector("div.cb-market__label-wrapper"));
		if(waitForClick(c) == false) {
			System.out.println("Unable to click the game");
			return;
		}
		try {Thread.sleep(2000L);} catch (InterruptedException e) {}

		WebElement buttonBar = waitForElement(By.cssSelector("div.tab-switcher-tabs-wrapper"));
		if(buttonBar == null) {
			System.out.println("Failed to get the button bar, outta here");
		} else {
			// Get all the buttons
			List<WebElement> buttons = buttonBar.findElements(By.tagName("a"));
			for(WebElement button : buttons) {
				switch(button.getText().toUpperCase()) {
					case "GAME LINES":
						waitForClick(button);
						processGameLines(oddsList, away, home, gameTime);
						break;
					case "BATTER PROPS":
						waitForClick(button);
						processBatterProps(oddsList, away, home, gameTime);
						break;
					default:
						// do nothing
				}
			}
		}
				
		driver.navigate().back();
		try {Thread.sleep(2000L);} catch (InterruptedException e) {}

		
	}

	private void processBatterProps(List<Odds> oddsList, Team away, Team home, Date gameTime) {
		
		List<WebElement> topics = waitForElements(By.cssSelector("div.cms-expander-container"));
		for(WebElement topic : topics) {
			
			WebElement label = topic.findElement(By.tagName("h2"));
			String name = label.getText();
			switch(name) {
				case "Hits O/U":
					processOu(topic, oddsList, away, home, gameTime, MLB_STAT.HITS);
					break;
				case "Total Bases O/U":
					processOu(topic, oddsList, away, home, gameTime, MLB_STAT.BASES);
					break;
				case "RBIs O/U":
					processOu(topic, oddsList, away, home, gameTime, MLB_STAT.RBI);
					break;
				case "Hits + Runs + RBIs O/U":
					processOu(topic, oddsList, away, home, gameTime, MLB_STAT.H_R_RBI);
					break;
				case "Runs O/U":
					processOu(topic, oddsList, away, home, gameTime, MLB_STAT.RUNS);
					break;
				case "Singles O/U":
					processOu(topic, oddsList, away, home, gameTime, MLB_STAT.SINGLES);
					break;
				case "Doubles O/U":
					processOu(topic, oddsList, away, home, gameTime, MLB_STAT.DOUBLES);
					break;
				case "Stolen Bases O/U":
					processOu(topic, oddsList, away, home, gameTime, MLB_STAT.SB);
					break;
				default:
					//System.out.println("Not processing: " + name);
					break;
			}

		}
	}

	private void processOu(
			WebElement topic, List<Odds> oddsList, 
			Team away, Team home, 
			Date gameTime, MLB_STAT mlbStat) {

		// See if collapsed
		WebElement wrapper = topic.findElement(By.cssSelector("div[data-testid=collapsible-wrapper]"));
		@SuppressWarnings("deprecation")
		String isCollapsed = wrapper.getAttribute("data-collapsed");
		if(isCollapsed.contentEquals("true")) {
			waitForClick(wrapper);
		}

//		List<WebElement> rows = topic.findElements(By.cssSelector("div[data-testid=market-mapping-template-8]"));
		List<WebElement> rows = getPopulatedList(topic, By.cssSelector("div[data-testid=market-mapping-template-8]"));
		for(WebElement row : rows) {
			
			Player pl = null;
			Team theTeam = null;

			try {
				WebElement label = row.findElement(By.cssSelector("a.cb-player-page-link"));
				WebElement name = label.findElement(By.cssSelector("p.cb-market__label--truncate-strings"));
				String playerName = name.getText();

				try {
					Object res = getPlayer(Arrays.asList(away, home), playerName);
					if(res != null) {
						if(res instanceof Player) {
							pl = (Player)res;
							theTeam = pl.getTeam();
						} else if(res instanceof Team) {
							try {
								theTeam = (Team)res;
								Team t = null;
								if(home.getCommonName().contentEquals(theTeam.getCommonName())) {
									t = home;
								} else if(away.getCommonName().contentEquals(theTeam.getCommonName())) {
									t = away;
								} else {
									System.out.println("Team returned isn't either team, this shouldn't happen");
									continue;
								}
								pl = getPlayer(t, playerName);
							} catch(Exception e2) {
								System.out.println("Failed to find player: " + playerName);
								continue;
							}
						}
					}
				} catch(Exception ee) {
					System.out.println("Failed to find player: " + playerName);
					continue;
				}

				List<WebElement> buttons = row.findElements(By.tagName("button"));
				OuRecord recOver = getRecord(buttons.get(0));
				OuRecord recUnder = getRecord(buttons.get(1));

				// Build the odds structure
				Odds odds = new Odds();
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(Period.GAME); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(mlbStat);
				odds.setGameDateTime(gameTime);
				
				OU ou = new OU();
				ou.setPoints(recOver.getPoints());
				ou.setOver(recOver.getMl());
				ou.setUnder(recUnder.getMl());
				
				odds.setOu(ou);
				odds.setHome(home);
				odds.setAway(away);
				odds.setPlayer1(pl);
				odds.setPlayer2(pl);
				odds.setHome(theTeam);
				odds.setAway(theTeam);

				oddsList.add(odds);

				
			} catch(Exception e) {
				
			}

		}
	}

	private void processGameLines(List<Odds> oddsList, Team away, Team home, Date gameTime) {

		List<WebElement> topics = waitForElements(By.cssSelector("div.cms-expander-container"));
		topics = waitForElements(By.cssSelector("div.cms-expander-container"));
		for(WebElement topic : topics) {
			
			WebElement label = topic.findElement(By.tagName("h2"));
			String name = label.getText();
			switch(name) {
				case "Alternate Run Line":
				case "Alternate Run Lines":
					// Expand 
					WebElement expand = waitForElement(topic, By.cssSelector("button.cb-view-more__button"));
					waitForClick(expand);
					processAltRunLines(topic, oddsList, away, home, gameTime);
					break;
				case "Alternate Total Runs":
					// Expand 
					WebElement expand2 = waitForElement(topic, By.cssSelector("button.cb-view-more__button"));
					waitForClick(expand2);
					processAltTotalRuns(topic, oddsList, away, home, gameTime);
					break;
				default:
					break;
			}

		}
	}

	private void processAltTotalRuns(WebElement topic, List<Odds> oddsList, Team away, Team home, Date gameTime) {

		WebElement view = topic.findElement(By.cssSelector("div[data-testid=market-template]"));
		
		// Each button below holds a over/under line
		List<WebElement> buttons = view.findElements(By.tagName("button"));
		for(int index = 0; index < buttons.size(); index+=2) {
			try {
				OuRecord recOver = getRecord(buttons.get(index+0));
				OuRecord recUnder = getRecord(buttons.get(index+1));

				Odds odds = new Odds();
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(Period.GAME); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(MLB_STAT.TOTALS);
				odds.setGameDateTime(gameTime);
				
				OU ou = new OU();
				ou.setPoints(recOver.getPoints());
				ou.setOver(recOver.getMl());
				ou.setUnder(recUnder.getMl());
				
				odds.setOu(ou);
				odds.setHome(home);
				odds.setAway(away);

				oddsList.add(odds);

			} catch(Exception e) {
				System.out.println("Exception getting over/unders: " + e.getMessage());
			}
		}
	}

	private void processAltRunLines(WebElement topic, List<Odds> oddsList, Team away, Team home, Date gameTime) {

		WebElement view = topic.findElement(By.cssSelector("div[data-testid=market-template]"));

		// Each button below holds a spread line
		List<WebElement> buttons = view.findElements(By.tagName("button"));
		for(int index = 0; index < buttons.size(); index+=2) {
			try {
				OuRecord recAway = getRecord(buttons.get(index+0));
				OuRecord recHome = getRecord(buttons.get(index+1));

				Odds odds = new Odds();
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(Period.GAME); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(MLB_STAT.SPREAD);
				odds.setGameDateTime(gameTime);
				
				Spread s = new Spread();
				s.setAwayPoints(recAway.getPoints());
				s.setAwayPrice(recAway.getMl());
				s.setHomePoints(recHome.getPoints());
				s.setHomePrice(recHome.getMl());

				odds.setSpread(s);
				odds.setHome(home);
				odds.setAway(away);

				oddsList.add(odds);

			} catch(Exception e) {
				System.out.println("Exception getting spreads: " + e.getMessage());
			}
		}
	}

	private OuRecord getRecord(WebElement element) throws Exception {

		OuRecord rec = new OuRecord();

		String name = element.findElement(By.cssSelector("span[data-testid=button-title-market-board]")).getText();
		String points = fixIt(
				element.findElement(
						By.cssSelector(
								"span[data-testid=button-points-market-board]"
								)
				).getText().replace("pk", "0.0"));
		String ml = fixIt(
				element.findElement(
						By.cssSelector("span[data-testid=button-odds-market-board]")
				).getText());
		
		rec.setName(name);
		rec.setPoints(Double.parseDouble(points));
		rec.setMl(Integer.parseInt(ml));
		
		return rec;
	}

	private List<Odds> parseTeamEvent(String file, Sport sport) {

		StringBuilder sb = new StringBuilder();
		List<Odds> list = new ArrayList<>();

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
		
		Elements containers = doc.select("div[data-testid=marketboard]");
		int numGames = 0;
		for(Element container : containers) {
			Elements games = container.select("div.cb-static-parlay__content--inner");
			numGames += games.size()/3;
			if((games.size() % 3) != 0) {
				System.out.println("Problem: Should be three containers for each game");
				return list;
			}
			int len = games.size();
			for(int i = 0; i < len; i+=3) {
				processEventTeam(games.get(i+0), games.get(i+1), games.get(i+2), list, sport);
			}
		}

		System.out.println("Number of games read in:   " + numGames);
		System.out.println("Number of games persisted: " + list.size());
	
		return list;
	}


	private void processEventTeam(Element match, Element time, Element unused, List<Odds> list, Sport sport) {
		
		Odds odds = new Odds();
		odds.setTimeStamp(new Date());
		odds.setBook(this.sportsbook);
		odds.setSport(sport);
		odds.setPeriod(Period.GAME); 

		Elements teams = match.select("div.cb-market__label-team-wrapper--col");
		Elements spans_away = teams.get(0).select("span");
		Elements spans_home = teams.get(1).select("span");
		
		
		boolean failed = false;
		try {
			odds.setAway(getTeam(this.sportsbook, sport, spans_away.get(0).text(), true));
		} catch(Exception e3) {
			failed = true;
		}
		try {
			odds.setHome(getTeam(this.sportsbook, sport, spans_home.get(0).text(), true));
		} catch(Exception e3) {
			failed = true;
		}
		if(failed) {
			return;
		}

		// Look for live event marker
		// TODO - test against live events
		Elements live = time.select("svg[data-testid=live-badge]");
		if(live.size() > 0) {
			return;
		}
		
		try {
			Element startTime = time.selectFirst("span.cb-event-cell__start-time");
			if(startTime != null) {
				Date gameTime = getGameTime(startTime.text());
				odds.setGameDateTime(gameTime);
				System.out.println("Game Time: " + odds.getGameDateTime());
			}
		} catch(Exception e55) {
			System.out.println("Exception gettting the start time for the event: " + e55.getMessage());
			e55.printStackTrace();
		}
		

		

		Elements oddsCon = match.select("div.cb-side-column__right");
		Elements buttons = oddsCon.get(0).select(".cb-market__button");
		if(sport != Sport.TENNIS) {
			if(buttons.size() != 6) {
				System.out.println("Not all odds populated, will ignore this contest");
				return;
			}
		} else {
			if(buttons.size() != 2) {
				System.out.println("Not all odds populated, will ignore this contest");
				return;
			}
		}

		if(sport != Sport.TENNIS) {

			Spread spread = new Spread();
			spread.setPeriod(Period.GAME);
			try {
				if(
						(buttons.get(0).tag().getName().contentEquals("button"))
							&&
						(buttons.get(3).tag().getName().contentEquals("button"))
						) {
					String awaySpreadPts  = buttons.get(0).text().split(" ")[0].replace("pk", "0.0");
					String homeSpreadPts  = buttons.get(3).text().split(" ")[0].replace("pk", "0.0");
					String awaySpreadLine = fixIt(buttons.get(0).text().split(" ")[1]);
					String homeSpreadLine = fixIt(buttons.get(3).text().split(" ")[1]);
					spread.setAwayPoints(Double.parseDouble(awaySpreadPts));
					spread.setHomePoints(Double.parseDouble(homeSpreadPts));
					spread.setAwayPrice(Integer.parseInt(awaySpreadLine));
					spread.setHomePrice(Integer.parseInt(homeSpreadLine));
				}
			} catch(Exception e3) {
				System.out.println("Failed to parse Spread odds: " + teams.get(0).text() + " at " + teams.get(1).text());
				System.out.println("Odds Container: " + oddsCon.text());
			}
			odds.setSpread(spread);

			OU ou = new OU();
			ou.setPeriod(Period.GAME);
			try {
				if(
						(buttons.get(1).tag().getName().contentEquals("button"))
							&&
						(buttons.get(4).tag().getName().contentEquals("button"))
						) {

					String overPts   = buttons.get(1).text().split(" ")[0].replace("O", "");
					String overLine  = fixIt(buttons.get(1).text().split(" ")[1]);
					String underLine = fixIt(buttons.get(4).text().split(" ")[1]);
					ou.setPoints(Double.parseDouble(overPts.trim()));
					ou.setOver(Integer.parseInt(overLine));
					ou.setUnder(Integer.parseInt(underLine));
				}
			} catch(Exception e3) {
				System.out.println("Failed to parse Totals odds: " + teams.get(0).text() + " at " + teams.get(1).text());
				System.out.println("Odds Container: " + oddsCon.text());
			}
			odds.setOu(ou);
		}

		int awayMLIndex = 2;
		int homeMLIndex = 5;
		
		if(sport == Sport.TENNIS) {
			awayMLIndex = 0;
			homeMLIndex = 1;
		}
		Spread ml = new Spread();
		ml.setAwayPoints(0.0);
		ml.setHomePoints(0.0);
		ml.setPeriod(Period.GAME);
		try {
			if(
					(buttons.get(awayMLIndex).tag().getName().contentEquals("button"))
						&&
					(buttons.get(homeMLIndex).tag().getName().contentEquals("button"))
					) {
				String awayML    = fixIt(buttons.get(awayMLIndex).text());
				String homeML    = fixIt(buttons.get(homeMLIndex).text());		
				ml.setAwayPrice(Integer.parseInt(awayML));
				ml.setHomePrice(Integer.parseInt(homeML));
			}
		} catch(Exception e3) {
			System.out.println("Failed to parse Moneyline odds: " + teams.get(0).text() + " at " + teams.get(1).text());
			System.out.println("Odds Container: " + oddsCon.text());
		}
		odds.setMl(ml);


		if((odds.getAway() != null) && (odds.getHome() != null)) {
			list.add(odds);
		} else {
			System.out.println("Not persisting: " + odds);
		}

	}

	private Date getGameTime(String start) {
		
		int month=0, day=0, year=0;
		int hour=0, minute=0;
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		String[] parts = start.split(" "); // should be Today or Tomorrow or date (ex: 4/11/26), h:mm, then Am/PM
		if(parts[0].contentEquals("Today")) {
			month = c.get(Calendar.MONTH) + 1;
			day = c.get(Calendar.DAY_OF_MONTH);
			year = c.get(Calendar.YEAR);
			String[] hm = parts[1].split(":");
			hour = Integer.parseInt(hm[0]);
			minute = Integer.parseInt(hm[1]);
			if(parts[2].contentEquals("PM")) {
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
			String[] hm = parts[1].split(":");
			hour = Integer.parseInt(hm[0]);
			minute = Integer.parseInt(hm[1]);
			if(parts[2].contentEquals("PM")) {
				if(hour != 12) {
					hour +=12;
				}
			} else {
				if(hour == 12) {
					hour = 0;
				}
			}
		} else if(fieldIsDow(parts[0])) { // Sat Apr 18th 1:10 PM 
			String[] months = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};
			int monthNum = 0;
			for(String m : months) {
				if(parts[1].toUpperCase().contentEquals(m)) {
					break;
				}
				monthNum++;
			}
			if((monthNum >= 12)) {
				System.out.println("Failed to find the month for " + parts[1]);
			} else {
				month = monthNum+1;

				String dayStr = parts[2].replace("st", "").replace("nd", "").replace("rd", "").replace("th", "").trim();
				day = Integer.parseInt(dayStr);
				year = c.get(Calendar.YEAR);
				String[] hm = parts[3].split(":");
				hour = Integer.parseInt(hm[0]);
				minute = Integer.parseInt(hm[1]);
				if(parts[4].contentEquals("PM")) {
					if(hour != 12) {
						hour +=12;
					}
				} else {
					if(hour == 12) {
						hour = 0;
					}
				}
			}

		} else { // date of the form m/d/y
			String[] mdy = parts[0].split("/");
			month = Integer.parseInt(mdy[0]);
			day = Integer.parseInt(mdy[1]);
			year = Integer.parseInt(mdy[2]) + 2000;
			String[] hm = parts[1].split(":");
			hour = Integer.parseInt(hm[0]);
			minute = Integer.parseInt(hm[1]);
			if(parts[2].contentEquals("PM")) {
				if(hour != 12) {
					hour +=12;
				}
			} else {
				if(hour == 12) {
					hour = 0;
				}
			}
		}
		
		Date ret = null;
		try {
			ret = new SimpleDateFormat("yyyy-MM-dd HH:mm")
					.parse(String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute));
		} catch(Exception e) {
			System.out.println("Exception converting date: " + e.getMessage());
			e.printStackTrace();
		}

		return ret;
	}

	private boolean fieldIsDow(String dow) {
		String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
		for(String d : days) {
			if(dow.toUpperCase().contentEquals(d)) {
				return true;
			}
		}
		return false;
	}

	private String fixIt(String string) {

		byte[] test  = string.getBytes(StandardCharsets.UTF_8);

		int lenOut = test.length;
		if((test[0] == -30) && (test[1] == -120) && (test[2] == -110)) {
			lenOut -= 2;
		}
		
		byte[] out = new byte[lenOut];
		int inPtr = 0;
		int outPtr = 0;
		if((test[0] == -30) && (test[1] == -120) && (test[2] == -110)) {
			out[0] = 45;
			outPtr = 1;
			inPtr = 3;
		} else {
			return string;
		}

		while(inPtr < test.length) {
			out[outPtr++] = test[inPtr++];
		}
		
		return(new String(out, StandardCharsets.UTF_8));
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

	private void refresh(Sport sport, String url) {

		try {
			getWindowHandle(sport, url);
		} catch (OddsException e) {
			return;
		}

		return;
		
	}

	private void getWindowHandle(Sport sport, String url) throws OddsException {

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

		// Make sure the panel is active
		boolean found = false;
		for(int i = 0; i < 100; ++i) {
			try {
				try {
					driver.findElement(By.cssSelector("section[data-testid='league-page-widget-container']"));
					found = true;
					break;
				} catch(Exception e1) {
					// do nothing, continue to next test (for tennis window)
				}
				
				// this looks for the tennis set up
				driver.findElement(By.className("sportsbook-a-to-z-sport__content"));
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

		driver.manage().window().maximize();

		return;
	}




	
	public static void main(String args[]) {

		System.out.println(new Date() + ": Processing DRAFTKINGS");

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

		DraftKings mgm = new DraftKings(useTheDriver);
		TeamService tSrv = new TeamService();
		TeamRepo tRepo = new TeamRepo();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
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

		System.out.println(new Date() + ": Done Processing DRAFTKINGS");
		
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
