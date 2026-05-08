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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.Team;
import scanner.scanner.model.OU;
import scanner.scanner.model.Odds;
import scanner.scanner.model.Player;
import scanner.scanner.model.Spread;
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
public class Espn extends Book {

	Random random = new Random(System.currentTimeMillis());

	public Espn(boolean useTheDriver) {
		super(Sportsbook.ESPN, useTheDriver);
	}

	public Espn() {
		super(Sportsbook.ESPN, true);
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
//				urls.add("https://sportsbook.thescore.bet/sport/baseball/organization/global/competition/world-baseball-classic#lines");
//				urls.add("https://sportsbook.thescore.bet/sport/baseball/organization/united-states/competition/mlb-spring-training#lines");
				urls.add("https://sportsbook.thescore.bet/sport/baseball/organization/united-states/competition/mlb#lines");
				break;
			case MLB_STATS:
				urls.add("https://sportsbook.thescore.bet/sport/baseball/organization/united-states/competition/mlb#lines");
				break;
			case NBA:
				urls.add("https://sportsbook.thescore.bet/sport/basketball/organization/united-states/competition/nba#lines");
				break;
			case WNBA:
				urls.add("https://sportsbook.thescore.bet/sport/basketball/organization/united-states/competition/wnba#lines");
				break;
			case NCAAM:
				urls.add("https://sportsbook.thescore.bet/sport/basketball/organization/united-states/competition/ncaab#lines");
				break;
			case NCAAW:
				urls.add("https://sportsbook.thescore.bet/sport/basketball/organization/united-states/competition/wncaab#lines");
				break;
			case NCAAF:
				urls.add("https://sportsbook.thescore.bet/sport/football/organization/united-states/competition/ncaaf#lines");
				break;
			case NFL:
				urls.add("https://sportsbook.thescore.bet/sport/football/organization/united-states/competition/nfl#lines");
				break;
			case NHL:
				urls.add("https://sportsbook.thescore.bet/sport/hockey/organization/united-states/competition/nhl#lines");
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

		refresh(Sport.TENNIS, "https://sportsbook.thescore.bet/");
		
		List<String> rtn = new ArrayList<>();
		
		try {
		
			WebElement tennis = driver.findElement(By.xpath("//span[starts-with(@id, 'Tennis')]"));
			
			if(tennis != null) {

				// Traverse up to the containing li
				WebElement parent = tennis.findElement(By.xpath("..")).findElement(By.xpath(".."));
				if(parent != null) {
					System.out.println("Parent tag: " + parent.getTagName());
				}
				if((parent == null) || (parent.getTagName().contentEquals("li") == false)) {
					System.out.println("Failed to find the containing list item for Tennis");
					return rtn;
				}
				
				//System.out.println(parent.getText());

				@SuppressWarnings("deprecation")
				String theDom = parent.getAttribute("outerHTML");
				
				Document doc = null;
				try {
					doc = Jsoup.parse(theDom);
				} catch(Exception e) {
					System.out.println("Error reading the dom: " + e.getMessage());
					return rtn;
				}
				
				Elements links = doc.select("a[href]");
				for(Element link : links) {
					//System.out.println(link.attr("href"));
					
					String l = link.attr("href");
					if(l.contains("/atp-") || l.contains("/wta-")) {
						if(!l.contains("doubles") && (!l.contains("specials")) && (!l.contains("challenger"))) {
							rtn.add("https://sportsbook.thescore.bet" + l);
						}
					}
				}


			} else {
				System.out.println("Failed to find the Tennis list element");
				return rtn;
			}

		} catch(Exception e) {
			System.out.println("Did not find tennis: " + e.getLocalizedMessage());
			
		}

		return rtn;
	}

	private List<Odds> getMatchups(Sport sport, String url) throws IOException, OddsException {

		if(useDriver) {
			
			refresh(sport, url);
			if(sport == Sport.MLB_STATS) {
				List<Odds> list = parseMlbStats(url);
				quitDriver();
				return list;
			}

			try {

				WebElement scroll = driver.findElement(By.cssSelector("main"));

				Actions actions = new Actions(driver);

				// Pull up context menu
				actions.contextClick(scroll).build().perform();

				Robot robot = new Robot();
				
				// Select the debug window from the context menu
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_Q);
				robot.keyPress(KeyEvent.VK_Q);
				robot.keyRelease(KeyEvent.VK_Q);
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

				// moves to the html tag ...
				robot.mouseMove(200, 900);
				Thread.sleep(100);

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
			System.out.print("Copy sport " + sport + " from Espn to the clipboard, then return");
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
				case WNBA:
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

		if(list != null) {
			for(Odds odds : list) {
				persistOdds(odds, "odds" + "_" + sport);
			}
		}

		return list;
	}
	
	private List<Odds> parseMlbStats(String url) {

		List<Odds> oddsList = new ArrayList<>();

		WebElement container = driver.findElement(By.tagName("main"));
		
		List<WebElement> games = null;
		int cntr = 0;
		do {
			games = container.findElements(By.tagName("article"));
			if(games.size() > 0) break;
			cntr++;
			try {Thread.sleep(200L);} catch (InterruptedException e) {}

		} while(cntr < 50);
		//System.out.println("Cntr is " + cntr);
		
		int numGames = games.size();
		
		for(int gameNum = 0; gameNum < numGames; ++gameNum) {
			
			try {
				
				// Refresh list of games
				container = driver.findElement(By.tagName("main"));
				games = container.findElements(By.tagName("article"));

				List<WebElement> dateTimeOfGame = games.get(gameNum).findElements(By.cssSelector("div.flex-wrap"));
				List<WebElement> participants   = games.get(gameNum).findElements(By.cssSelector("div.flex-row"));
				if((dateTimeOfGame == null) || (dateTimeOfGame.size() != 1)) {
					continue;
				}
				if((participants == null) || (participants.size() != 2)) {
					continue;
				}
				
				WebElement away = participants.get(0).findElement(By.cssSelector("div.text-primary"));
				if(away == null) {
					System.out.println("Failed to find the string for the away team");
					continue;
				}
				String awayTeam = removeParens(away.getText().toUpperCase());
				
				WebElement home = participants.get(1).findElement(By.cssSelector("div.text-primary"));
				if(home == null) {
					System.out.println("Failed to find the string for the home team");
					continue;
				}
				String homeTeam = removeParens(home.getText().toUpperCase());

				Team aTeam = null;
				Team hTeam = null;
				
				boolean failed = false;
				try {
					aTeam = getTeam(this.sportsbook, Sport.MLB_STATS, awayTeam, true);
				} catch(Exception e3) {
					failed = true;
				}
				try {
					hTeam = getTeam(this.sportsbook, Sport.MLB_STATS, homeTeam, true);
				} catch(Exception e3) {
					failed = true;
				}

				if(failed) {
					continue;
				}
				
				// Look for LIVE indicator
				try {
					dateTimeOfGame.get(0).findElement(By.cssSelector("div[data-testid='live-indicator']"));
					continue; // if we're here the game is live, so skip it
				} catch(Exception e) {
					// Exception indications no live block, so it's pregame and we want those. Drop through
				}

				WebElement dateTime = dateTimeOfGame.get(0).findElement(By.tagName("span"));
				Date startingTime = getStartingDate(dateTime.getText());
				
				// click on the game to get the stats
				boolean clickResult = waitForClick(away);
				if(clickResult == false) {
					System.out.println("Failed to click on game " + awayTeam + " at " + homeTeam);
					continue;
				}
				try {Thread.sleep(1000L);} catch (InterruptedException e) {}
				
				processMLBGame(awayTeam, aTeam, homeTeam, hTeam, oddsList, startingTime);

				if(oddsList != null) {
					for(Odds odds : oddsList) {
						persistOdds(odds, "odds" + "_" + Sport.MLB_STATS);
					}
				}
				
				oddsList.clear();

				//System.out.println("Start of refresh: " + new Date());
				refresh(Sport.MLB_STATS, url);
				//System.out.println("End of refresh: " + new Date());
		
			} catch(Exception ert) {
				System.out.println("Exception processing game: " + ert.getMessage());
				ert.printStackTrace();
			}
		}

		return oddsList;
	}

	private void processMLBGame(
			String awayTeam, Team aTeam, 
			String homeTeam, Team hTeam, 
			List<Odds> oddsList, Date startingTime) {

		// Get all the header buttons
		WebElement buttonBar = null;
		List<WebElement> hdrButtons = null;
		try {
			buttonBar = driver.findElement(By.cssSelector("div.ao--carousel-slide-container"));
			hdrButtons = buttonBar.findElements(By.tagName("a"));
		} catch(Exception e) {
			System.out.println("Failed to get header buttons for " + awayTeam + " at " + homeTeam);
			return;
		}

		for(WebElement button : hdrButtons) {
			String buttonName = button.getText();
			if((buttonName.contentEquals("Popular") == false) && (buttonName.contentEquals("Batter Props") == false)) {
				continue;
			}
			waitForClick(button);

			waitForSteadyList(driver, "details", buttonName);

			// For each header get all the topics
			List<WebElement> topics = driver.findElements(By.tagName("details"));
			for(WebElement topic : topics) {
				String name = topic.findElement(By.tagName("h2")).getText();
				switch(name) {

					case "Alternate Run Line":
						processAltRunLine(awayTeam, aTeam, homeTeam, hTeam, oddsList);
						break;
					case "Alternate Total Runs":
						processAltTotals(awayTeam, aTeam, homeTeam, hTeam, oddsList);
						break;
					case "Home Runs (O/U)":
						processOU(
								awayTeam, aTeam, homeTeam, hTeam, 
								MLB_STAT.HR, "drawer-Home Runs (O/U)",
								oddsList);
						break;
					case "Total Bases (O/U)":
						processOU(
								awayTeam, aTeam, homeTeam, hTeam, 
								MLB_STAT.BASES, "drawer-Total Bases (O/U)",
								oddsList);
						break;
					case "Hits (O/U)":
						processOU(
								awayTeam, aTeam, homeTeam, hTeam, 
								MLB_STAT.HITS, "drawer-Hits (O/U)",
								oddsList);
						break;
					case "RBIs (O/U)":
						processOU(
								awayTeam, aTeam, homeTeam, hTeam, 
								MLB_STAT.RBI, "drawer-RBIs (O/U)",
								oddsList);
						break;
					case "Runs (O/U)":
						processOU(
								awayTeam, aTeam, homeTeam, hTeam, 
								MLB_STAT.RUNS, "drawer-Runs (O/U)",
								oddsList);
						break;
					case "Singles (O/U)":
						processOU(
								awayTeam, aTeam, homeTeam, hTeam, 
								MLB_STAT.SINGLES, "drawer-Singles (O/U)",
								oddsList);
						break;
					case "Doubles (O/U)":
						processOU(
								awayTeam, aTeam, homeTeam, hTeam, 
								MLB_STAT.DOUBLES, "drawer-Doubles (O/U)",
								oddsList);
						break;
					case "Hits + Runs + RBIs (O/U)":
						processOU(
								awayTeam, aTeam, homeTeam, hTeam, 
								MLB_STAT.H_R_RBI, "drawer-Hits + Runs + RBIs (O/U)",
								oddsList);
						break;
					default:
						break; // do nothing
				}
			}
		}
	}

	private void waitForSteadyList(WebElement we, String tagName, String selector) {
		int cntr = 0;
		List<WebElement> topics = null;
		do {
			topics = we.findElements(By.tagName(tagName));
			if(topics.size() > 0) {
				break;
			} else {
				try {Thread.sleep(10);} catch(Exception ee) {}
				cntr++;
			}
		} while(cntr < 200);

		if(topics.size() == 0) {
			System.out.println("Warning: No items found for selector " + selector);
		}
	}

	private void waitForSteadyList(WebDriver driver, String tagName, String selector) {
		int cntr = 0;
		List<WebElement> topics = null;
		do {
			topics = driver.findElements(By.tagName(tagName));
			if(topics.size() > 0) {
				break;
			} else {
				try {Thread.sleep(10);} catch(Exception ee) {}
				cntr++;
			}
		} while(cntr < 200);

		if(topics.size() == 0) {
			System.out.println("Warning-drv: No items found for selector " + selector);
		}
	}

	@SuppressWarnings("deprecation")
	private void processOU(
			String awayTeam, Team aTeam, String homeTeam, Team hTeam, 
			MLB_STAT mlbStat,
			String selector,
			List<Odds> oddsList) {
		
		WebElement container = driver.findElement(By.tagName("main"));

		try {
			WebElement ovUnder = container.findElement(By.cssSelector("details[data-testid='" + selector + "']"));
			
			// Open the tab, if necessary
			if(ovUnder.getAttribute("open") == null) {
				waitForClick(ovUnder);
			}
			
			waitForSteadyList(ovUnder, "article", selector);
			
			List<WebElement> articles = ovUnder.findElements(By.tagName("article"));
			for(WebElement article : articles) {

				WebElement playerName = article.findElement(By.tagName("header"));
				String playerAsString = playerName.getText().toUpperCase();
				
				List<WebElement> buttons = article.findElements(By.tagName("button"));

				List<WebElement> over_spans  = buttons.get(0).findElements(By.tagName("span"));
				List<WebElement> under_spans = buttons.get(1).findElements(By.tagName("span"));
				String o_pts = over_spans.get(1).getText().replace("O", "").trim();
				String o_ml  = over_spans.get(2).getText().replace("Even", "+100").trim();
				String u_ml  = under_spans.get(2).getText().replace("Even", "+100").trim();

				if((o_pts == null) || (o_pts.length() == 0)) {
					continue;
				}
				if((o_ml == null) || (o_ml.length() == 0)) {
					continue;
				}
				if((u_ml == null) || (u_ml.length() == 0)) {
					continue;
				}

				Odds odds = new Odds();
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(Period.GAME); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(mlbStat);
				
				Player pl = null;
				Team theTeam = null;

				try {
					Object res = getPlayer(Arrays.asList(aTeam, hTeam), playerAsString);
					if(res != null) {
						if(res instanceof Player) {
							odds.setPlayer1((Player)res);
							odds.setPlayer2((Player)res);
							theTeam = ((Player)res).getTeam();
						} else if(res instanceof Team) {
							try {
								theTeam = (Team)res;
								Team t = null;
								if(hTeam.getCommonName().contentEquals(theTeam.getCommonName())) {
									t = hTeam;
								} else if(aTeam.getCommonName().contentEquals(theTeam.getCommonName())) {
									t = aTeam;
								} else {
									System.out.println("Team returned isn't either team, this shouldn't happen");
									continue;
								}
								pl = getPlayer(t, playerAsString);
								odds.setPlayer1(pl);
								odds.setPlayer2(pl);
							} catch(Exception e2) {
								System.out.println("Failed to find player: " + playerAsString);
								continue;
							}
						}
					}
				} catch(Exception ee) {
					System.out.println("Failed to find player: " + playerAsString);
					continue;
				}
				
				try {
					Double overPoints      = Double.parseDouble(o_pts.trim());
					Integer overmoneyline  = Integer.parseInt(o_ml);
					Integer undermoneyline = Integer.parseInt(u_ml);
					OU ou = new OU();
					ou.setPoints(overPoints);
					ou.setOver(overmoneyline);
					ou.setUnder(undermoneyline);
					ou.setPeriod(Period.GAME);

					odds.setTimeStamp(new Date());
					odds.setBook(this.sportsbook);
					odds.setSport(Sport.MLB_STATS);
					odds.setPeriod(Period.GAME); 
					odds.setStatus(Status.SCHEDULED);
					odds.setMlbStat(mlbStat);
					odds.setOu(ou);
					odds.setHome(theTeam);
					odds.setAway(theTeam);
						
					oddsList.add(odds);
				} catch(Exception e3) {
					System.out.println("Exception processing OU: " + e3.getMessage());
					System.out.println("Player: " + playerAsString);
					if(theTeam != null) {
						System.out.println("Team: " + theTeam);
					}
					System.out.println("MLBStat: " + mlbStat);
					e3.printStackTrace();
				}

			}
			
		} catch(Exception e) {
			System.out.println("Exception get HR data: " + e.getMessage());
		}
	}

	private void processAltTotals(
			String awayTeam, Team aTeam, 
			String homeTeam, Team hTeam, 
			List<Odds> oddsList) {
		
		WebElement container = driver.findElement(By.tagName("main"));

		try {
			WebElement totalRuns = container.findElement(By.cssSelector("details[data-testid='drawer-Alternate Total Runs']"));
			boolean clickResult = waitForClick(totalRuns);
			if(clickResult == false) {
				System.out.println("Failed to click on detail for alt totals ");
			} else {
				
				List<WebElement> buttons = getButtons(container, "drawer-Alternate Total Runs");

				for(int index = 0; index < buttons.size(); index+=2) {
					List<WebElement> over_spans  = buttons.get(index).findElements(By.tagName("span"));
					List<WebElement> under_spans = buttons.get(index+1).findElements(By.tagName("span"));
					String o_pts = over_spans.get(1).getText().replace("O", "").trim();
					String o_ml  = over_spans.get(2).getText().replace("Even", "+100").trim();
					String u_ml  = under_spans.get(2).getText().replace("Even", "+100").trim();

					try {
						Double overPoints      = Double.parseDouble(o_pts.trim());
						Integer overmoneyline  = Integer.parseInt(o_ml);
						Integer undermoneyline = Integer.parseInt(u_ml);
						OU ou = new OU();
						ou.setPoints(overPoints);
						ou.setOver(overmoneyline);
						ou.setUnder(undermoneyline);
						ou.setPeriod(Period.GAME);

						Odds odds = new Odds();
						odds.setTimeStamp(new Date());
						odds.setBook(this.sportsbook);
						odds.setSport(Sport.MLB_STATS);
						odds.setPeriod(Period.GAME); 
						odds.setStatus(Status.SCHEDULED);
						odds.setMlbStat(MLB_STAT.TOTALS);
						odds.setOu(ou);
						odds.setHome(hTeam);
						odds.setAway(aTeam);
							
						oddsList.add(odds);
					} catch(Exception e3) {
						System.out.println("Exception processing spread: " + e3.getMessage());
						e3.printStackTrace();
					}
				} // for all lines
				
				// Close the modal
				WebElement closeButton = driver.findElement(By.id("modal-close-button"));
				closeButton.click();
			} // else
		} catch(Exception e) {
			System.out.println("Failed to find alt totals");
		}
		
	}

	private void processAltRunLine(
			String awayTeam, Team aTeam, 
			String homeTeam, Team hTeam, 
			List<Odds> oddsList) {

		WebElement container = driver.findElement(By.tagName("main"));
		
		try {
			WebElement runLines = container.findElement(By.cssSelector("details[data-testid='drawer-Alternate Run Line']"));
			boolean clickResult = waitForClick(runLines);
			if(clickResult == false) {
				System.out.println("Failed to click on detail for alt run line");
			} else {
				
				List<WebElement> buttons = getButtons(container, "drawer-Alternate Run Line");

				for(int index = 0; index < buttons.size(); index+=2) {
					List<WebElement> away_spans = buttons.get(index).findElements(By.tagName("span"));
					List<WebElement> home_spans = buttons.get(index+1).findElements(By.tagName("span"));
					String a_rl = away_spans.get(1).getText().substring(awayTeam.length()).trim();
					String a_ml = away_spans.get(2).getText().replace("Even", "+100").trim();
					String h_rl = home_spans.get(1).getText().substring(homeTeam.length()).trim();
					String h_ml = home_spans.get(2).getText().replace("Even", "+100").trim();

					try {
						Double awaySpreadPoints = Double.parseDouble(a_rl.trim());
						Integer awaymoneyline = Integer.parseInt(a_ml);
						Double homeSpreadPoints = Double.parseDouble(h_rl.trim());
						Integer homemoneyline = Integer.parseInt(h_ml);
						Spread s = new Spread();
						s.setAwayPoints(awaySpreadPoints);
						s.setAwayPrice(awaymoneyline);
						s.setHomePoints(homeSpreadPoints);
						s.setHomePrice(homemoneyline);
						s.setPeriod(Period.GAME);

						Odds odds = new Odds();
						odds.setTimeStamp(new Date());
						odds.setBook(this.sportsbook);
						odds.setSport(Sport.MLB_STATS);
						odds.setPeriod(Period.GAME); 
						odds.setStatus(Status.SCHEDULED);
						odds.setMlbStat(MLB_STAT.SPREAD);
						odds.setSpread(s);
						odds.setHome(hTeam);
						odds.setAway(aTeam);
							
						oddsList.add(odds);
					} catch(Exception e3) {
						System.out.println("Exception processing spread: " + e3.getMessage());
						e3.printStackTrace();
					}
				} // for all lines
				
				// Close the modal
				WebElement closeButton = driver.findElement(By.id("modal-close-button"));
				closeButton.click();
			} // else
		} catch(Exception e) {
			//e.printStackTrace();
			System.out.println("Failed to find alt run lines");
		}
		
	}

	private List<WebElement> getButtons(WebElement container, String selector) {
		
		int cntr = 0;

		do {

			// Process the content
			// get all buttons, find one with See All Lines
			WebElement runLines = container.findElement(By.cssSelector("details[data-testid='" + selector + "']"));
			List<WebElement> buttons = null;
			boolean found = false;
			try {
				buttons = runLines.findElements(By.tagName("button"));
				for(WebElement button : buttons) {
					if(button.getText().contains("See All Lines")) {
						if(waitForClick(button)) {
							found = true;
							try {Thread.sleep(2000);} catch(Exception ee) {}
						}
						break;
					}
				}
				if(found) {
					List<WebElement> cont = driver.findElements(By.tagName("article"));
					return cont.get(cont.size()-1).findElements(By.tagName("button"));
				} else {
					//System.out.println("Waiting for the See All Lines button, cntr is " + cntr);
					cntr++;
					try {Thread.sleep(100);} catch(Exception ee) {}
				}
			} catch(Exception e) {
				
			}
			
		} while(cntr < 20);
		
		return null;
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
		
//		Elements container = doc.select("section[data-testid=marketplace-shelf-]");
		Elements container = doc.select("main");
		Elements games = container.select("article");
		for(Element game : games) {
			processEventTeam(game, list, sport);
		}
		
		System.out.println("Number of games read in:   " + games.size());
		System.out.println("Number of games persisted: " + list.size());

		return list;
	}


@SuppressWarnings("unused")
private List<Odds> parseTennis(String file, Sport sport) {

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
		
		Elements eventGroups = doc.select("ms-event-group");
		String currTournament = null;
		for(Element eventGroup : eventGroups) {
			for(Element child : eventGroup.children()) {
				if(child.tag().getName().contentEquals("div") && child.hasClass("header-wrapper")) {
					Elements divs = child.select("div");
					for(Element divChildren : divs) {
						if(divChildren.hasClass("title")) {
							Elements spans = divChildren.select("span");
							for(Element span : spans) {
								currTournament = span.ownText();
							}
						}
					}
				}
				else if (child.tag().getName().contentEquals("ms-event")) {
					processEventSingle(child, list, currTournament, sport);
				}
			}
		}
		
		return list;
	}

	private void processEventSingle(Element e, List<Odds> list, String tournament, Sport sport) {
		
		Calendar c = Calendar.getInstance();

		c.setTime(new Date());
		Odds odds = new Odds();
		odds.setTimeStamp(new Date());
		odds.setBook(this.sportsbook);
		odds.setSport(sport);

		Element link = e.select("a.grid-info-wrapper").first();
		String url = link.attr("href");
		String urlParts[] = url.split("-");
		odds.setUrl(url);
		odds.setGameNumber(urlParts[urlParts.length - 1]);
		
		
		// Get event status
		Elements timer = e.select("ms-event-timer");
		Elements liveTimer = timer.select("ms-live-timer");
		if(liveTimer.size() <= 0) {
			Elements preMatchTimer = timer.select("ms-prematch-timer");
			if(preMatchTimer.size() > 0) {
				odds.setPeriod(Period.GAME); 
				odds.setStatus(Status.SCHEDULED);
				String dateString = preMatchTimer.text();
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
					String[] dmy = parts[0].split("/");
					month = Integer.parseInt(dmy[0]);
					day   = Integer.parseInt(dmy[1]);
					year  = Integer.parseInt(dmy[2]) + 2000;
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
				}
				// set the starting time
				try {
					odds.setGameDateTime(
							new SimpleDateFormat("yyyy-MM-dd HH:mm")
								.parse(String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute)));
				} catch (ParseException e1) {
					e1.printStackTrace();
				}
			} 
				
			
		} else {
			System.out.println("Match in progress, will not process: " + url);
			return; // don't want matches in progress
		}
		
		// Get participants
		Elements participants = e.select("div.participant");
		Elements doubles = participants.select("div.second-participant");
		if(doubles.size() > 0) {
			odds.setDoubles(true);
			return;
		} else {
			odds.setDoubles(false);
		}
		if(participants.size() == 2) {
			String p1 = participants.get(0).text().toUpperCase().trim();
			String p2 = participants.get(1).text().toUpperCase().trim();
			Team p1Team = null;
			Team p2Team = null;
			try {
				p1Team = getTeam(this.sportsbook, sport, "Tennis", true);
				p2Team = getTeam(this.sportsbook, sport, "Tennis", true);
				
			} catch(Exception e3) {
				return;
			}
			Player player1 = null;
			Player player2 = null;
        	try {
        		player1 = getPlayer(p1Team, p1);
        		player2 = getPlayer(p2Team, p2);
        		odds.setPlayer1(player1);
        		odds.setPlayer2(player2);
        	} catch(Exception e2) {
        		return;
        	}

		} else if(participants.size() != 0) {
			System.out.println("Dont have two particpants: " + e);
			//continue;
		}
		
		// Find the current odds
		Elements oddsList = e.select("div.grid-group-container");
		if(oddsList.size() > 0) {
			Elements optionGroups = oddsList.select("ms-option-group");
			if(optionGroups.size() > 0) {
				for(int i = 0; i < optionGroups.size(); ++i) {
					Elements options = optionGroups.get(i).select("ms-option");
					if(options.size() == 2) {
						Elements optName = options.get(0).select("div.option-name");
						Elements optValue = options.get(0).select("div.option-value");
						Spread spread = new Spread();
						spread.setAwayPoints(0.0);
						spread.setHomePoints(0.0);
						spread.setPeriod(getPeriod(optName.text().trim()));
						try {
							spread.setAwayPrice(Integer.parseInt(optValue.text()));
							optValue = options.get(1).select("div.option-value");
							spread.setHomePrice(Integer.parseInt(optValue.text()));
						} catch(Exception e3) {
							// do nothing
						}
						odds.setMl(spread);
					} else if(options.size() != 0) {
						System.out.println("Did not get two options: " + e);
						continue;
					} else {
						continue; // no options, so dont add
					}
				} // for
			} else {
				//System.out.println("No odds for the match");
			}
		} else {
			System.out.println("No posted odds");
		}
		if((odds.getPlayer1() != null) && (odds.getPlayer2() != null) && (odds.getDoubles() == false)) {
			list.add(odds);
		} else {
			System.out.println("Not persisting: " + odds);
		}
		
	}

	private void processEventTeam(Element e, List<Odds> list, Sport sport) {
		
		Odds odds = new Odds();
		odds.setTimeStamp(new Date());
		odds.setBook(this.sportsbook);
		odds.setSport(sport);
		odds.setPeriod(Period.GAME); 
		
		Elements dateTimeOfGame = e.select("div.flex-wrap");
		Elements participants = e.select("div.flex-row");
		if((dateTimeOfGame == null) || (dateTimeOfGame.size() != 1)) {
			return;
		}
		if((participants == null) || (participants.size() != 2)) {
			return;
		}

		Element away = participants.get(0).select("div.text-primary").first();
		if(away == null) {
			return;
		}
		String awayTeam = removeParens(away.text().toUpperCase());
		
		Element home = participants.get(1).select("div.text-primary").first();
		if(home == null) {
			return;
		}
		String homeTeam = removeParens(home.text().toUpperCase());

		boolean failed = false;
		try {
			odds.setAway(getTeam(this.sportsbook, sport, awayTeam, true));
		} catch(Exception e3) {
			failed = true;
		}
		try {
			odds.setHome(getTeam(this.sportsbook, sport, homeTeam, true));
		} catch(Exception e3) {
			failed = true;
		}

		if(failed) {
			return;
		}
		
		// Look for LIVE indicator
		Elements isLive = dateTimeOfGame.get(0).select("div[data-testid='live-indicator']");
		if(isLive.size() > 0) {
			return;
		}

		Element dateTime = dateTimeOfGame.get(0).select("span").first();
		odds.setStatus(Status.SCHEDULED);
		odds.setGameDateTime(getStartingDate(dateTime.text()));

		String awaySpreadPts = null;
		String awaySpreadLine = null;
		String homeSpreadPts = null;
		String homeSpreadLine = null;
		String overUnderPts = null;
		String overLine = null;
		String underLine = null;
		String awayMl = null;
		String homeMl = null;

		Elements line = participants.get(0).select("button");
		// should be up to 4 buttons --
		// #1 is the team
		// if only 2 then 
		//  #2 is the ML
		// if 4 buttons then 
		//  #2 is away spread
		//  #3 is over points and line
		//  #4 is ml away

		if(line.size() == 4) {
			
			Elements scans = line.get(1).select("span");
			// second scan contains for line (o/u points or spread points)
			// third line is the spread ml, or o/u line
			awaySpreadPts = scans.get(1).text();
			awaySpreadLine = scans.get(2).text().replace("Even", "+100");
				
			scans = line.get(2).select("span");
			// second scan contains fir line (o/u points or spread points)
			// third line is the spread ml, or o/u line
			overUnderPts = scans.get(1).text().replace("O", "");
			overLine = scans.get(2).text().replace("Even", "+100");
			
			scans = line.get(3).select("span");
			awayMl = scans.get(2).text().replace("Even", "+100");
		} else {
			Elements scans = line.get(1).select("span");
			awayMl = scans.get(2).text().replace("Even", "+100");
		}
 
		line = participants.get(1).select("button");

		if(line.size() == 4) {
			
			Elements scans = line.get(1).select("span");
			// second scan contains for line (o/u points or spread points)
			// third line is the spread ml, or o/u line
			homeSpreadPts = scans.get(1).text();
			homeSpreadLine = scans.get(2).text().replace("Even", "+100");
				
			scans = line.get(2).select("span");
			// second scan contains fir line (o/u points or spread points)
			// third line is the spread ml, or o/u line
			overUnderPts = scans.get(1).text().replace("U", "");
			underLine = scans.get(2).text().replace("Even", "+100");
			
			scans = line.get(3).select("span");
			homeMl = scans.get(2).text().replace("Even", "+100");
		} else {
			Elements scans = line.get(1).select("span");
			homeMl = scans.get(2).text().replace("Even", "+100");
		}

		if(awaySpreadPts != null) {
			Spread spread = new Spread();
			spread.setPeriod(Period.GAME);
			try {
				if(!awaySpreadPts.contentEquals("--") && !homeSpreadPts.contentEquals("--")) {
					spread.setAwayPoints(Double.parseDouble(awaySpreadPts));
					spread.setHomePoints(Double.parseDouble(homeSpreadPts));
					spread.setAwayPrice(Integer.parseInt(awaySpreadLine));
					spread.setHomePrice(Integer.parseInt(homeSpreadLine));
				}
			} catch(Exception e3) {
				System.out.println("Failed to parse Spread odds: " 
						+ awaySpreadPts + " " + awaySpreadLine + " " + homeSpreadPts + " " + homeSpreadLine);
			}
			odds.setSpread(spread);
		}

		if(overUnderPts != null) {
			OU ou = new OU();
			ou.setPeriod(Period.GAME);
			try {
				if(!overUnderPts.contentEquals("--")) {
					ou.setPoints(Double.parseDouble(overUnderPts.trim()));
					ou.setOver(Integer.parseInt(overLine));
					ou.setUnder(Integer.parseInt(underLine));
				}
			} catch(Exception e3) {
				System.out.println("Failed to parse OU odds: " 
						+ overUnderPts + " " + overLine + " " + underLine);
			}
			odds.setOu(ou);
		}
		
		if((awayMl.length() > 0) && (homeMl.length() > 0)) {
			Spread ml = new Spread();
			ml.setAwayPoints(0.0);
			ml.setHomePoints(0.0);
			ml.setPeriod(Period.GAME);
			try {
				ml.setAwayPrice(Integer.parseInt(awayMl));
				ml.setHomePrice(Integer.parseInt(homeMl));
			} catch(Exception e3) {
				System.out.println("Failed to parse ML odds: " 
						+ awayMl + " " + homeMl);
			}
			odds.setMl(ml);
		}

		
		if((odds.getAway() != null) && (odds.getHome() != null)) {
			list.add(odds);
		} else {
			System.out.println("Not persisting: " + odds);
		}
	}

	private String removeParens(String team) {
		StringBuilder sb = new StringBuilder();
		sb.append(team.trim());
		if(sb.toString().startsWith("(")) {
			int opening = sb.toString().indexOf("(");
			int closing = sb.toString().indexOf(")");
			for(int index = opening; index <= closing; ++index) {
				sb.setCharAt(index, ' ');
			}
		}
		return sb.toString().trim();
	}

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

	private Period getPeriod(String text) {
		switch(text) {
			case "Set 1": return Period.SET1; 
			case "Set 2": return Period.SET2; 
			case "Set 3": return Period.SET3; 
			case "Set 4": return Period.SET4; 
			case "Set 5": return Period.SET5;
		}
		return Period.MATCH;
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

		//System.out.println("Start of refresh: " + new Date());
		try {
			getWindowHandle(sport, url);
		} catch (OddsException e) {
			return;
		}

		//System.out.println("After window handle: " + new Date());
		
		WebElement popup = null;
		int cntr = 0;
		while(popup == null) {
			try {
				popup = driver.findElement(By.cssSelector("button[aria-label='Got it']"));
			} catch(Exception e) {
				
			}
			try {Thread.sleep(100L);} catch (InterruptedException e) {}
			if(cntr++ >= 10) {
				break;
			}
		}
		//System.out.println("Counter is " + cntr);
		if(popup == null) {
			//System.out.println(this.sportsbook + ": Failed to get app start up");
			return;
		}
		//System.out.println("Clicking off the popup");
		popup.click();
		try {Thread.sleep(500L);} catch (InterruptedException e) {}
		
		// give an extra 5 seconds to populate
		if(sport == Sport.NCAAM) {
			try {Thread.sleep(5000L);} catch (InterruptedException e) {}
		}

	//	System.out.println("end of refresh: " + new Date());
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
		}
					
		try {
			Robot robot2 = new Robot();
			robot2.keyPress(KeyEvent.VK_ALT);
			try {Thread.sleep(500);} catch(Exception ee) {}
			robot2.keyPress(KeyEvent.VK_A);
			try {Thread.sleep(500);} catch(Exception ee) {}
			robot2.keyRelease(KeyEvent.VK_ALT);
			try {Thread.sleep(500);} catch(Exception ee) {}
			robot2.keyRelease(KeyEvent.VK_A);
			try {Thread.sleep(500);} catch(Exception ee) {}

		} catch (Exception e) {
			System.out.println("Issue when trying to close Allow window");
			return;
		}


		// Make sure the panel is active
		boolean found = false;
		for(int i = 0; i < 100; ++i) {
			try {
				driver.findElement(By.cssSelector("main"));
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

		System.out.println(new Date() + ": Processing ESPN");
		
		if(args.length < 2) {
			System.out.println("Requires two args: sport and delete odds flag, along with optional useDriver flag");
			return;
		}
		Sport sport = null;
		switch(args[0].toUpperCase()) {
			case "NHL":       sport = Sport.NHL;       break;
			case "TENNIS":    sport = Sport.TENNIS;    break;
			case "NBA":       sport = Sport.NBA;       break;
			case "WNBA":      sport = Sport.WNBA;      break;
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

		Espn mgm = new Espn(useTheDriver);
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
		
		System.out.println(new Date() + ": Done Processing ESPN");
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
