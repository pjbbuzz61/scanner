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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class BetRivers extends Book {

	Random random = new Random(System.currentTimeMillis());

	String MLB_GROUP_NUMBER = "1000093616";

	public BetRivers(boolean useTheDriver) {
		super(Sportsbook.BETRIVERS, useTheDriver);
	}
	public BetRivers() {
		super(Sportsbook.BETRIVERS, true);
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
				urls.add("https://md.betrivers.com/?page=sportsbook&group=" + MLB_GROUP_NUMBER + "&type=prematch");
//				urls.add("https://md.betrivers.com/?page=sportsbook&group=1000093918&type=matches");
//				urls.add("https://md.betrivers.com/?page=sportsbook&group=2000069864&type=matches");
				break;
			case MLB_STATS:
				urls.add("https://md.betrivers.com/?page=sportsbook&group=1000093616&type=prematch");
				break;
			case NBA:
				urls.add("https://md.betrivers.com/?page=sportsbook&group=1000093652&type=prematch");
				break;
			case NCAAM:
				urls.add("https://md.betrivers.com/?page=sportsbook&group=1000093654&type=prematch");
				break;
			case NCAAW:
				urls.add("https://md.betrivers.com/?page=sportsbook&group=2000054461&type=matches");
				break;
			case NCAAF:
				urls.add("https://md.betrivers.com/?page=sportsbook&group=1000093655&type=prematch");
				break;
			case NFL:
				urls.add("https://md.betrivers.com/?page=sportsbook&group=1000093656&type=prematch");
				break;
			case NHL:
				urls.add("https://md.betrivers.com/?page=sportsbook&group=1000093657&type=prematch");
				break;
			case TENNIS:
				urls.add("https://md.betrivers.com/?page=sportsbook&group=1000093193&type=prematch");
				break;
			default:
				break;
		}
		return urls;
	}

	private List<Odds> getMatchups(Sport sport, String url) throws IOException, OddsException {

		List<String> files = new ArrayList<>();
		if(useDriver) {
			
			refresh(sport, url);
			if(sport == Sport.MLB_STATS) {
				List<Odds> list = parseMlbStats();
				quitDriver();
				return list;
			}

			
			try {

				// expand all expandable headers
				boolean moreEvents = true;
				int cntr = 0;
				
				WebElement mainPage = null;
				Map<String, Boolean> triedMap = new HashMap<>();
				
				while(moreEvents) {
					
					try {
						mainPage = driver.findElement(
								By.cssSelector("div.main-page-view-sportsbook"));
						
						List<WebElement> expandables = mainPage.findElements(
								By.xpath("//button[@aria-expanded='false' and starts-with(@id, 'accordion')]"));

						WebElement expandable = null;
						for(WebElement expand : expandables) {
							String title = expand.getText();
							if(triedMap.get(title) != null) {
								continue;
							} else {
								triedMap.put(title, true);
								expandable = expand;
								break;
							}
						}

						if(expandable == null) {
							moreEvents = false;
							System.out.println("No more expandable areas on the list");
							continue;
						}

						javascriptExecutor.executeScript("arguments[0].scrollIntoView();", expandable);
						String title = expandable.getText();
						if((title == null) || (!title.toUpperCase().contains("DOUBLES"))) {
							if(sport == Sport.TENNIS) {
								if(title.toUpperCase().contains("ATP") || title.toUpperCase().contains("WTA")) {
									expandable.click();
								} else {
									triedMap.put(title, true);
								}
							} else {
								expandable.click();
							}
						} else {
							triedMap.put(title, true);
						}
						Thread.sleep(100);
						
					} catch(Exception er) {
						System.out.println("No more expandable areas");
						moreEvents = false;
//						javascriptExecutor.executeScript("javascript:window.scrollBy(0,-5000)"); 
						javascriptExecutor.executeScript("arguments[0].scrollIntoView();", mainPage);
					}

				}

				moreEvents = true;
				cntr = 0;
				while(moreEvents) {
					WebElement scroll = driver.findElement(
							By.cssSelector("div.main-page-view-sportsbook"));

					Actions actions = new Actions(driver);

					// Pull up context menu
					actions.contextClick(scroll).build().perform();

					Robot robot = new Robot();
					
					// Select the debug window from the context menu
					Thread.sleep(100);
					robot.keyPress(KeyEvent.VK_Q);
					robot.keyRelease(KeyEvent.VK_Q);
					
					// Mouse into the Elements display and click to gain focus
//					robot.mouseMove(500,900);
//					Thread.sleep(100);
//					robot.keyPress(KeyEvent.VK_ENTER);
//					Thread.sleep(100);
//					robot.keyRelease(KeyEvent.VK_ENTER);
//					Thread.sleep(100);

					// Page up enough times to get to the top
					for(int i = 0; i < 50; ++i) {
						Thread.sleep(25);
						robot.keyPress(KeyEvent.VK_PAGE_UP);
						Thread.sleep(25);
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

					String filename = 
							System.getProperty("user.home") + "/" + "SCRAPE_" +
									this.sportsbook + "_" + System.currentTimeMillis() + "_" + cntr + ".html"; 
					cntr++;
					readClipboard(filename);
					files.add(filename);
					
					// See if we have more events
					try {
						
						WebElement moreEventsLabel = null;
						try {
							moreEventsLabel = driver
									.findElement(By.cssSelector("button[data-testid=show-more-events-button]"));
						} catch(Exception e3) {
							moreEventsLabel = driver
									.findElement(By.cssSelector("div[data-testid=show-more-events-button]"));
						}

						int cnt = 0;
						do {
							javascriptExecutor.executeScript("javascript:window.scrollBy(0,200)"); 
							try {
								moreEventsLabel.click();
								break;
							} catch(Exception eee) {
								System.out.println("Not view, try again, Count is " + cnt);
								try {Thread.sleep(50);} catch(Exception ee) {}
								cnt++;
							}
								
						} while(cnt < 100);
							
						
						
//						WebElement moreEventsLabel = driver
//								.findElement(By.cssSelector("button[data-testid=show-more-events-button]"));
//						System.out.println(moreEventsLabel.getLocation());
//						
//						//System.out.println("Clicking for more events ...");
//						javascriptExecutor.executeScript("arguments[0].scrollIntoView();", moreEventsLabel);
//						moreEventsLabel.click();
						try {Thread.sleep(1000);} catch(Exception ee) {}
					} catch(Exception e) {
						//System.out.println("Exception: Didn't find more events: " + e.getMessage());
						moreEvents = false;
					}
				
				} // while more events
			} catch(Exception e) {
				// might not be a visible scrollbar
				System.out.println(e);
				e.printStackTrace();
			}
		} // if useDriver
		else {

			boolean keepGoing = true;
			Scanner scanner = new Scanner(System.in);
			while(keepGoing) {
				int cntr = 0;
				String filename = System.getProperty("user.home") + "/" + "SCRAPE_" +
						this.sportsbook + "_" + System.currentTimeMillis() + "_" + cntr + ".html"; 
				cntr++;
				
				System.out.print("Copy sport " + sport + " from BetRiver to the clipboard, then return (x if done) ");
			    String str = null;
			    try {
			    	str = scanner.nextLine();
			    } catch(Exception er) {
			    	// no nothing - this is just a return with nothing entered
			    }
			    if((str != null) && (str.length() > 0) &&  (str.charAt(0)) == 'x') {
			    	keepGoing = false;
			    } else {
				    readClipboard(filename);
					files.add(filename);
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

		for(Odds odds : list) {
			persistOdds(odds, "odds" + "_" + sport);
		}
		
		return list;
	}
	
	private List<Odds> parseMlbStats() {
		
		List<Odds> oddsList = new ArrayList<>();
		
		showMore(By.cssSelector("button[data-testid=show-more-events-button]"));
		showMore(By.cssSelector("div[data-testid=show-more-events-button]"));
		
		int numGames = 0;
		WebElement container = 
				driver.findElement(By.cssSelector("div[data-testid=listview-group-" + MLB_GROUP_NUMBER + "-events-container]"));
		List<WebElement> games = container.findElements(By.tagName("article"));
		numGames = games.size();

		
		
		for(int gameNum = 0; gameNum < numGames; ++gameNum) {

			showMore(By.cssSelector("button[data-testid=show-more-events-button]"));
			showMore(By.cssSelector("div[data-testid=show-more-events-button]"));
			
			javascriptExecutor.executeScript("javascript:window.scrollTo(0," + gameNum*200 + ")"); 

			try {
				
				// Refresh list of games
				games = refreshListOfGames();
				System.out.println("NumGames: " + numGames);
				if(games.size() < gameNum) {
					System.out.println("Game list is too short");
					continue;
				}

				// See if game is live
				WebElement liveIndicator = games.get(gameNum).findElement(By.cssSelector("div[data-testid^='default-header'"));
				if(liveIndicator.getText().contains("LIVE")) {
					System.out.println("Event is live");
					continue;
				}

				// Get the game time
				Date gameTime = getGameTime(games.get(gameNum));
				
				// Get the two teams
				List<Team> teams = getTeams(games.get(gameNum));
				if(teams.size() != 2) {
					System.out.println("Did not get two teams for game " + gameNum);
					continue;
				}
				System.out.println(teams.get(0).getCommonName() + " at " + teams.get(1).getCommonName());
				
				if(waitForClick(games.get(gameNum)) == false) {
					System.out.println("Failed to click the game");
					continue;
				}

				int numTries = 0;
				boolean success = false;
				WebElement page = null;
				do {
					page = waitForElement(By.cssSelector("main.KambiBC-event-page-microfrontend"));
					if(page == null) {
						System.out.println("Event page didn't show up. NumTries: " + numTries);
						try {Thread.sleep(1000L);} catch (InterruptedException e4) {}
						numTries++;
					} else {
						success = true;
					}
					
				} while((numTries < 3) && (success == false));

				if(success) {
					processMLBGame(oddsList, teams, gameTime);
				} else {
					System.out.println("Failed to load game " + gameNum);
					continue;
				}

				persistOddsForMlbStats(oddsList);
				
				driver.navigate().back();
				
				// Wait for Main Page to show up again
				WebElement mainPage = 
						waitForElement(
								By.cssSelector("div[data-testid=listview-group-" + MLB_GROUP_NUMBER + "-events-container]"));
				if(mainPage == null) {
					System.out.println("Main page didn't show up");
					return oddsList;
				}

			} catch(Exception outerEx) {
				System.out.println("Exception processing game number: " + gameNum);
				System.out.println("Exception: " + outerEx.getMessage());
				outerEx.printStackTrace();
			}

		}
		
		return oddsList;
	}

	private List<Team> getTeams(WebElement game) {
		
		Team aTeam = null;
		Team hTeam = null;
		List<Team> rtn = new ArrayList<>();
		
		List<WebElement> parts = game.findElements(By.cssSelector("div[data-testid=participant-row]"));
		String awayTeam = parts.get(0).getText();
		String awayPitcher = null; 
		try {
			WebElement pitcher = parts.get(0).findElement(By.cssSelector("div[data-testid=pitcher-stat-line]"));
			awayPitcher = pitcher.getText().trim();
		} catch(Exception e) {
			
		}
		
		String homeTeam = parts.get(1).getText();
		String homePitcher = null; 
		try {
			WebElement pitcher = parts.get(1).findElement(By.cssSelector("div[data-testid=pitcher-stat-line]"));
			homePitcher = pitcher.getText().trim();
		} catch(Exception e) {
			
		}
		if(awayPitcher != null) {
			awayTeam = awayTeam.replace(awayPitcher, "").trim();
		}
		if(homePitcher != null) {
			homeTeam = homeTeam.replace(homePitcher, "").trim();
		}
		
		try {
			aTeam = getTeam(this.sportsbook, Sport.MLB_STATS, awayTeam, true);
		} catch(Exception e3) {
		}
		try {
			hTeam = getTeam(this.sportsbook, Sport.MLB_STATS, homeTeam, true);
		} catch(Exception e3) {
		}

		if((aTeam != null) && (hTeam != null)) {
			rtn.add(aTeam);
			rtn.add(hTeam);
		}
		
		return rtn;
	}
	
	private List<WebElement> refreshListOfGames() {
		
		int c = 0;
		WebElement container = null;
		do {
			try {
				container = driver.findElement(By.cssSelector("div[data-testid=listview-group-" + MLB_GROUP_NUMBER + "-events-container]"));
				break;
			} catch(Exception e) {
				//System.out.println("Failed to get container: cnt: " + c);
				try {Thread.sleep(100L);} catch (InterruptedException e4) {}
				c++;
			}
		} while(c < 50);

		if(container == null) {
			System.out.println("failed to get the container for games ");
			return null;
		}

		return getPopulatedList(container, By.tagName("article"));
	}

	private void showMore(By by) {
		
		// See if we have more events
		try {
			WebElement moreEventsLabel = driver.findElement(by);
			int cnt = 0;
			do {
				javascriptExecutor.executeScript("javascript:window.scrollBy(0,200)"); 
				try {
					moreEventsLabel.click();
					break;
				} catch(Exception eee) {
					try {Thread.sleep(50);} catch(Exception ee) {}
					cnt++;
				}
			} while(cnt < 100);
			try {Thread.sleep(1000);} catch(Exception ee) {}
		} catch(Exception e) {
			// do nothing, we should be done
		}
	}
	
	private boolean elementContains(WebElement element, String string) {

		@SuppressWarnings("deprecation")
		String classAttribute = element.getAttribute("class");
        if (classAttribute == null || classAttribute.isEmpty()) {
        	return false;
        }

        // Split the class attribute string by spaces into a list of individual class names
        List<String> classNames = Arrays.asList(classAttribute.split("\\s+"));

        // Check if the specific class name is in the list
        if(classNames.contains(string)) {
        	return true;
        } else {
        	return false;
        }
	}

	private void processMLBGame(List<Odds> oddsList, List<Team> teams, Date gameTime) {

		System.out.println("Processing game: " + teams.get(0).getCommonName() + " at " + teams.get(1).getCommonName());
		WebElement container = null;
		List<WebElement> sections = null;

		// Calling the load of the sections complete after a maximum of 2 seconds
		// Probably a better way but I don't want to overthink this :)
		long start = System.currentTimeMillis();
		boolean loadComplete = false;
		do {
			
			container = waitForElement(By.cssSelector("div.KambiBC-event-page-component__column--1"));
			if(container == null) {
				System.out.println("Failed to find the container");
				return;
			}
			sections = getPopulatedList(container, By.cssSelector("li.KambiBC-bet-offer-category"));

			try {Thread.sleep(10L);} catch (InterruptedException e) {}

			// Done if we get at least 10 sections ...
			// 13 seems to be the number to look for for MLB
			if((sections.size() > 10) || (System.currentTimeMillis()-start) > 2000) {
				loadComplete = true;
			}
			
		} while(loadComplete == false);
		
		for(int sectionNum = 0; sectionNum < sections.size(); ++sectionNum) {

			WebElement section = sections.get(sectionNum);

			// Get the section name
			WebElement button = section.findElement(By.cssSelector("button[id^=bet-offer-category-header]"));
			WebElement label  = button.findElement(By.tagName("h2"));
			String sectionName = label.getText().trim();

			placeElementInView(label);

			switch(sectionName) {
			
				case "Most Popular":
					processMostPopular(section, oddsList, teams, gameTime);
					break;

				case "Batter HRs":
				case "Batter Hits":
				case "Total Bases":
				case "Batter RBIs":
				case "Batter Runs":
				case "Stolen Bases":

					// Expand the section
					if(elementContains(section, "KambiBC-expanded") == false) {
						WebElement h2 = section.findElement(By.tagName("h2"));
						waitForClick(h2);
					}

					// Refresh the objects
					int tries = 0;
					boolean success = false;
					do {
						try {
							container = waitForElement(By.cssSelector("div.KambiBC-event-page-component__column--1"));
							sections = container.findElements(By.cssSelector("li.KambiBC-bet-offer-category"));
							section = sections.get(sectionNum);
							processBatter(section, oddsList, teams);
							success = true;
						} catch(Exception ewok) {
							System.out.println(
									"Exception processing " + teams.get(0).getCommonName() + " at " + teams.get(1).getCommonName() + 
									" for " + sectionName + ": cnt is " + tries + ". Will try up to 5 times");
							tries++;
						}
					} while((tries < 5) && (success == false));
					break;

				case "Team Totals":
					break;

				case "Inning 1":
				case "Pitcher Props":
				case "Innings":
				case "Listed Pitcher":
				case "Game Props":
					break;
				
				default:
					System.out.println("Unknown section label: " + sectionName);
			}		
		}
		
		return;
	}
	
	private void processBatter(WebElement section, List<Odds> oddsList, List<Team> teams) {
		
		MLB_STAT mlbStat = null;

		WebElement button = section.findElement(By.cssSelector("button[id^=bet-offer-category-header]"));
		WebElement label  = button.findElement(By.tagName("h2"));
		String sectionName = label.getText().trim();

		placeElementInView(label);
		
		switch(sectionName) {
			case "Batter HRs":   mlbStat = MLB_STAT.HR;    break;
			case "Batter Hits":  mlbStat = MLB_STAT.HITS;  break;
			case "Total Bases":  mlbStat = MLB_STAT.BASES; break;
			case "Batter RBIs":  mlbStat = MLB_STAT.RBI;   break;
			case "Batter Runs":  mlbStat = MLB_STAT.RUNS;  break;
			case "Stolen Bases": mlbStat = MLB_STAT.SB;    break;
		}

		// Get all the subcategories
		// subcat would represent the grouping for the category (ex: for Batter Hits category
		//  the two subcats would be one for singles and one for doubles)
		List<WebElement> subCats = section.findElements(By.cssSelector("li.KambiBC-bet-offer-subcategory"));
		for(WebElement subCat : subCats) {
			
			// This will have the description of what's here
			// ex: Total Hits by the Player - Including Extra Innings (Listed player must 
			//       be in starting lineup for bets to stand) (Over)
			WebElement hdr  = subCat.findElement(By.cssSelector("div.KambiBC-bet-offer-subcategory__header"));
			
			Double overUnder = getOUFromHeader(hdr);
			mlbStat = updateMlbStatFromHeader(hdr, mlbStat);
			
			// This is the list of offers for it
			// We need to determine the type of offers listing here
			// Seem to come in two flavors - one column and two
			WebElement offers = subCat.findElement(By.cssSelector("div.KambiBC-bet-offer-subcategory__outcomes-list"));
			WebElement outList = offers.findElement(By.cssSelector("div.KambiBC-outcomes-list"));
			@SuppressWarnings("deprecation")
			String classes = outList.getAttribute("class");
			boolean oneColumn   = Arrays.asList(classes.split(" ")).contains("KambiBC-outcomes-list--columns-1");
			boolean twoColumn   = Arrays.asList(classes.split(" ")).contains("KambiBC-outcomes-list--columns-2");
			boolean splitColumn = Arrays.asList(classes.split(" ")).contains("KambiBC-grid-slider");

			if((oneColumn == false) && (twoColumn == false) && (splitColumn == false)) {
				System.out.println("Did not determine if subcategory was one, two or split columns: SectionName: " + sectionName);
				continue;
			}
			if(oneColumn && twoColumn) {
				System.out.println("Subcategory marked as both one and two columns: SectionName: " + sectionName);
				continue;
			}

			List<OuRecord> records = getRecords(offers, oneColumn, overUnder, teams);
				
			for(OuRecord record : records) {
					
				// Convert to Odds struct
				Odds odds = new Odds();

				Player pl = null;
				Team theTeam = null;

				try {
					if(record.getTeam() != null) {
						theTeam = record.getTeam();
						pl = getPlayer(theTeam, record.getName());
						odds.setPlayer1(pl);
						odds.setPlayer2(pl);
					} else {
						Object res = getPlayer(teams, record.getName());
						if(res != null) {
							if(res instanceof Player) {
								odds.setPlayer1((Player)res);
								odds.setPlayer2((Player)res);
								theTeam = ((Player)res).getTeam();
							} else if(res instanceof Team) {
								try {
									theTeam = (Team)res;
									Team t = null;
									if(teams.get(1).getCommonName().contentEquals(theTeam.getCommonName())) {
										t = teams.get(1);
									} else if(teams.get(0).getCommonName().contentEquals(theTeam.getCommonName())) {
										t = teams.get(0);
									} else {
										System.out.println("Team returned isn't either team, this shouldn't happen");
										continue;
									}
									pl = getPlayer(t, record.getName());
									odds.setPlayer1(pl);
									odds.setPlayer2(pl);
								} catch(Exception e2) {
									System.out.println("Failed to find player: " + record.getName());
									continue;
								}
							}
						}
					}
				} catch(Exception ee) {
					System.out.println("Failed to find player: " + record.getName());
					continue;
				}
					
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(Period.GAME); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(mlbStat);
				odds.setOu(record.getOu());
				odds.setHome(theTeam);
				odds.setAway(theTeam);
						
				oddsList.add(odds);
			}
		}
	}

	private List<OuRecord> getRecords(WebElement columnContainer, boolean oneColumn, Double overUnder, List<Team> teams) {

		List<OuRecord> records = new ArrayList<>();

		List<WebElement> columns = columnContainer.findElements(By.cssSelector("div.KambiBC-outcomes-list__column"));
		if(oneColumn && (columns.size() != 1)) {
			System.out.println("Wrong number of columns: should have one but found " + columns.size());
			return records;
		}
		if(!oneColumn && (columns.size() != 2)) {
			System.out.println("Wrong number of columns: should have two but found " + columns.size());
			return records;
		}

		if(oneColumn) {
			
			String currentName = null;
			Team currentTeam = null;
			String points = null;
			String ml = null;
			Double pts = null;
			Integer moneyLine = null;
			
			// Get all the first descendants and process that way
			List<WebElement> descendants = null;
			descendants = columns.get(0).findElements(By.xpath("./*"));
			for(int index = 0; index < descendants.size(); ++index) {
				descendants = columns.get(0).findElements(By.xpath("./*"));
				WebElement desc = descendants.get(index);
				if(desc.getTagName().contentEquals("div")) {
					@SuppressWarnings("deprecation")
					String classes = desc.getAttribute("class");
					boolean participant = Arrays.asList(classes.split(" ")).contains("KambiBC-outcomes-list__row-header--participant");
					if(participant) {
						List<WebElement> spans = desc.findElements(By.tagName("span"));
						if(spans.size() == 2) {
							currentName = spans.get(0).getText().trim().toUpperCase();
							String teamName = spans.get(1).getText();
							if(teams.get(0).getNameSbSpecific().contentEquals(teamName)) {
								currentTeam = teams.get(0);
							} else if(teams.get(1).getNameSbSpecific().contentEquals(teamName)) {
								currentTeam = teams.get(1);
							} else {
								System.out.println("One Column: Team name not found in list of teams: Name: " + teamName);
								System.out.println("AwayTeam: " + teams.get(0));
								System.out.println("HomeTeam: " + teams.get(1));
							}
						} else {
							currentName = desc.getText().trim().toUpperCase();
						}

					} else {
						currentName = null;
					}
				} else if(desc.getTagName().contentEquals("button")) {
					WebElement firstDiv = desc.findElement(By.xpath("./*"));
					List<WebElement> nextTwoDivs = firstDiv.findElements(By.xpath("./*"));
					List<WebElement> nextNextTwoDivs = nextTwoDivs.get(0).findElements(By.xpath("./*"));
					points = nextNextTwoDivs.get(1).getText();
					ml = nextTwoDivs.get(1).getText();

					try {
						pts = Double.parseDouble(points);
						moneyLine = Integer.parseInt(ml);
					} catch(Exception e) {
						System.out.println("Exception parsing either points or moneyline: Points: " + points + ", ML: " + ml); 
						continue;
					}

					// Create a new record
					OuRecord r = new OuRecord();
					r.setName(currentName);
					r.setTeam(currentTeam);
					OU ou = new OU();
					ou.setOver(moneyLine);
					ou.setPeriod(Period.GAME);
					ou.setPoints(pts);
					r.setOu(ou);
					if(ou.getPoints() == null) {
						if(overUnder == null) {
							ou.setPoints(0.5);
						} else {
							ou.setPoints(overUnder);
						}
					}
					records.add(r);
				}
			} // for all descendants
			
		} else { // two column

			Double ovun = overUnder;
			
			List<WebElement> names = null;
			List<WebElement> lines = null;
			names = columns.get(0).findElements(By.xpath("./*"));
			lines = columns.get(1).findElements(By.xpath("./*"));

			if(names.size() != lines.size()) {
				System.out.println("The column lengths do not match: names is " + names.size() + ", lines is " + lines.size());
				return records;
			}

			for(int index = 0; index < names.size(); ++index) {
				
				names = columns.get(0).findElements(By.xpath("./*"));
				lines = columns.get(1).findElements(By.xpath("./*"));

				if(index == 0) {
					String t = lines.get(index).getText();
					if(t != null) {
						if(t.startsWith("O")) {
							ovun = Double.parseDouble(t.substring(1).trim());
						}
					}
					continue;
				}

				// Get name of player and team, if possible
				String playerName = null;
				String teamName = null;
				Team currentTeam = null;
				List<WebElement> spans = names.get(index).findElements(By.tagName("span"));
				if(spans.size() == 2) {
					playerName = spans.get(0).getText();
					teamName = spans.get(1).getText();
					if(teams.get(0).getNameSbSpecific().contentEquals(teamName)) {
						currentTeam = teams.get(0);
					} else if(teams.get(1).getNameSbSpecific().contentEquals(teamName)) {
						currentTeam = teams.get(1);
					} else {
						System.out.println("Team name not found in list of teams: Name: " + teamName);
						System.out.println("AwayTeam: " + teams.get(0));
						System.out.println("HomeTeam: " + teams.get(1));
					}
				} else {
					playerName = names.get(index).getText().toUpperCase().trim();
				}
				
				List<WebElement> buts = null;
				if(lines.get(index).getTagName().contentEquals("button")) {
					buts = Arrays.asList(lines.get(index));
				} else {
					buts = lines.get(index).findElements(By.tagName("button"));
				}
				for(WebElement but : buts) {
					String t = but.getText();
					OU ou = getOU(t.replace("\n", " ").split(" "));
					if(ou != null) {
						
						// Create a new record
						OuRecord r = new OuRecord();
						r.setName(playerName.trim().toUpperCase());
						r.setTeam(currentTeam);
						ou.setPeriod(Period.GAME);
						r.setOu(ou);
						if(ou.getPoints() == null) {
							if(ovun == null) {
								ou.setPoints(0.5);
							} else {
								ou.setPoints(ovun);
							}
						}

						records.add(r);
					}
				}
			}
		}

		return records;
	}

	private OU getOU(String[] parts) {

		OU ou = new OU();
		try {
			
			for(String s : parts) {
				if(s.endsWith("+")) {
					// a points field
					Double pts = Double.parseDouble(s.replace("+", ""));
					ou.setPoints(pts-0.5);
				} else if(s.toUpperCase().contains("OVER")) {
					// do nothing
				} else {
					// must be a money line or points
					String type = getNumericType(s);
					switch(type) {
						case "Integer":
							ou.setOver(Integer.parseInt(s));
							break;
						case "Double":
							ou.setPoints(Double.parseDouble(s));
							break;
						case "Not a number":
							System.out.println("Error: Field is not part of the Ou: " + s);
							break;
					}
				}
			}

		} catch(Exception e) {
			System.out.println("Exception trying to create OU: " + e.getMessage());
			return null;
		}
		
		return ou;
	}
	private MLB_STAT updateMlbStatFromHeader(WebElement hdr, MLB_STAT mlbStat) {

		String headerString = hdr.getText();
		switch(headerString) {
		
			case "Player to Hit a Home Run - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return mlbStat;
			case "Player to hit 2 or more Home Runs - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return mlbStat;
			case "Player to hit 3 or more Home Runs - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return mlbStat;
			case "Total Hits by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Hits by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return mlbStat;
			case "Total Doubles by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Doubles by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return MLB_STAT.DOUBLES;
			case "Total Bases Recorded by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total RBIs by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Runs Scored by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Stolen Bases by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Stolen Bases by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total RBIs by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total Runs Scored by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Batter HRs":
			case "Player to hit X or more Home Runs - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total Bases Recorded by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return mlbStat;
			default:
				System.out.println("Don't have this header string registered1: " + headerString);
		}
		return mlbStat;
	}
	
	private Double getOUFromHeader(WebElement hdr) {

		String headerString = hdr.getText();
		switch(headerString) {
		
			case "Player to Hit a Home Run - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return 0.5;
			case "Player to hit 2 or more Home Runs - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return 1.5;
			case "Player to hit 3 or more Home Runs - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				return 2.5;
			case "Total Hits by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Doubles by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Bases Recorded by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total RBIs by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Runs Scored by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Total Stolen Bases by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand) (Over)":
			case "Batter HRs":
			case "Player to hit X or more Home Runs - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total Bases Recorded by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total Doubles by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total RBIs by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total Runs Scored by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total Stolen Bases by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
			case "Total Hits by the Player - Including Extra Innings (Listed player must be in starting lineup for bets to stand)":
				break; // fall through, return null
			default:
				System.out.println("Don't have this header string registered2: " + headerString);
		}
		return null;
	}

	private void placeElementInView(WebElement element) {
	
		int cnt = 0;
		boolean found = false;
		do {
			javascriptExecutor.executeScript("javascript:window.scrollBy(0,10)"); 
			if(element.isDisplayed()) {
				found = true;
				break;
			} else {
				try {Thread.sleep(5L);} catch (InterruptedException e) {}
			}
		} while(cnt < 100);
		
		//System.out.println("PlaceElementInView: Cnt: " + cnt + ", Name: " + element.getText());
		
		if(found == false) {
			System.out.println("Failed to get element in view: " + element.getText());
		}
	}

	private void processMostPopular(WebElement section, List<Odds> oddsList, List<Team> teams, Date gameTime) {

		// Expand Show Lists
		List<WebElement> showLists = section.findElements(By.cssSelector("button[aria-label^='Show list']"));

		for(WebElement showList : showLists) {
			
			if(waitForClick(showList) == false) {
				System.out.println("Unable to click the Show List");
			}

		}

		try {Thread.sleep(2000L);} catch (InterruptedException e) {}
		
		// Get all the spreads
		String awaySpread = null;
		String homeSpread = null;
		String awayML = null;
		String homeML = null;
		
		WebElement spreadList = waitForElement(section, By.cssSelector("li.KambiBC-bet-offer-subcategory--handicap"));
		if(spreadList == null) {
			System.out.println("Failed to find the spreadlist");
			return;
		}
		
		List<WebElement> lists = spreadList.findElements(By.cssSelector("div.KambiBC-outcomes-list__column"));
		List<WebElement> leftButtons  = lists.get(0).findElements(By.tagName("button"));
		List<WebElement> rightButtons = lists.get(1).findElements(By.tagName("button"));
		for(int i = 0; i < leftButtons.size(); ++i) {

			WebElement firstDiv = leftButtons.get(i).findElement(By.xpath("./*"));
			List<WebElement> nextTwoDivs = firstDiv.findElements(By.xpath("./*"));
			List<WebElement> nextNextTwoDivs = nextTwoDivs.get(0).findElements(By.xpath("./*"));
			awaySpread = nextNextTwoDivs.get(1).getText();
			awayML = nextTwoDivs.get(1).getText();

			firstDiv = rightButtons.get(i).findElement(By.xpath("./*"));
			nextTwoDivs = firstDiv.findElements(By.xpath("./*"));
			nextNextTwoDivs = nextTwoDivs.get(0).findElements(By.xpath("./*"));
			homeSpread = nextNextTwoDivs.get(1).getText();
			homeML     = nextTwoDivs.get(1).getText();
			
			try {
				Double awaySpreadPoints = Double.parseDouble(awaySpread.trim());
				Integer awaymoneyline = Integer.parseInt(awayML);
				Double homeSpreadPoints = Double.parseDouble(homeSpread.trim());
				Integer homemoneyline = Integer.parseInt(homeML);
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
				odds.setHome(teams.get(1));
				odds.setAway(teams.get(0));
				odds.setGameDateTime(gameTime);
					
				oddsList.add(odds);

			} catch(Exception e3) {
				// do nothing
			}
		}
		
		// Get all the totals
		String overPoints  = null;
		String overML  = null;
		String underML = null;
		WebElement ouList = waitForElement(section, By.cssSelector("li.KambiBC-bet-offer-subcategory--overunder"));
		if(ouList == null) {
			System.out.println("Failed to find the over/under list");
			return;
		}

		lists = ouList.findElements(By.cssSelector("div.KambiBC-outcomes-list__column"));
		leftButtons  = lists.get(0).findElements(By.tagName("button"));
		rightButtons = lists.get(1).findElements(By.tagName("button"));
		for(int i = 0; i < leftButtons.size(); ++i) {
			WebElement firstDiv = leftButtons.get(i).findElement(By.xpath("./*"));
			List<WebElement> nextTwoDivs = firstDiv.findElements(By.xpath("./*"));
			List<WebElement> nextNextTwoDivs = nextTwoDivs.get(0).findElements(By.xpath("./*"));
			overPoints = nextNextTwoDivs.get(1).getText();
			overML = nextTwoDivs.get(1).getText();

			firstDiv = rightButtons.get(i).findElement(By.xpath("./*"));
			nextTwoDivs = firstDiv.findElements(By.xpath("./*"));
			nextNextTwoDivs = nextTwoDivs.get(0).findElements(By.xpath("./*"));
			underML     = nextTwoDivs.get(1).getText();

			try {
				Double overPts = Double.parseDouble(overPoints.trim());
				Integer overMoneyLine = Integer.parseInt(overML);
				Integer underMoneyLine = Integer.parseInt(underML);
				OU ou = new OU();
				ou.setPoints(overPts);
				ou.setOver(overMoneyLine);
				ou.setUnder(underMoneyLine);
				ou.setPeriod(Period.GAME);

				Odds odds = new Odds();
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(Period.GAME); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(MLB_STAT.TOTALS);
				odds.setOu(ou);
				odds.setHome(teams.get(1));
				odds.setAway(teams.get(0));
				odds.setGameDateTime(gameTime);

				oddsList.add(odds);

			} catch(Exception e3) {
				// do nothing
			}
		}		
	}

	private List<Odds> parseTeamEvent(List<String> files, Sport sport) {

		List<Odds> list = new ArrayList<>();

		int numGames = 0;
		for(String file : files) {
			
			StringBuilder sb = new StringBuilder();
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

			Elements container = doc.select("div.main-page-view-sportsbook");
			if((container == null) || (container.size() == 0)) {
				System.out.println("Failed to find the main page");
				continue;
			}
			Elements games = container.get(0).select("article");
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


	private boolean processEventTeam(Element match,  List<Odds> list, Sport sport) {
		
		Odds odds = new Odds();
		odds.setTimeStamp(new Date());
		odds.setBook(this.sportsbook);
		odds.setSport(sport);
		odds.setPeriod(Period.GAME); 

		Element awayTeamContainer = match.select("div > div > div > div:nth-child(2) > div > div > div > div").first();
		Element homeTeamContainer = match.select("div > div > div > div:nth-child(2) > div > div > div:nth-child(2) > div").first();

		if(awayTeamContainer == null) {
			return false;
		}
		if(homeTeamContainer == null) {
			return false;
		}
		String awayTeam = awayTeamContainer.text();
		String homeTeam = homeTeamContainer.text();
		if(awayTeamContainer.text().contains(")")) {
			awayTeam = awayTeamContainer.text().substring(awayTeamContainer.text().indexOf(")")+1).trim();
		}
		if(homeTeamContainer.text().contains(")")) {
			homeTeam = homeTeamContainer.text().substring(homeTeamContainer.text().indexOf(")")+1).trim();
		}
		if(homeTeam != null)  {
			if(sport == Sport.TENNIS) {
				if(homeTeam.contains("/")) {
					System.out.println("Bypassing contest - looks like doubles in tennis: Home: " + homeTeam);
					return false;
				}
			}
		}
		if(awayTeam != null)  {
			if(sport == Sport.TENNIS) {
				if(awayTeam.contains("/")) {
					System.out.println("Bypassing contest - looks like doubles in tennis: Away: " + awayTeam);
					return false;
				}
			}
		}
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
			return true;
		}

		// Look for live event marker
		Elements live = match.select("div > div > div > div > div > div > div");
		if(live.size() > 0) {
			if(live.text().contains("Live")) {
				return true;
			}
		}

		Date gameTime = getGameTime(match);
		if(gameTime != null) {
			odds.setGameDateTime(gameTime);
		}

		Elements offersCon = match.select("div > div > div > div:nth-child(3)");
		Elements offerSpans = offersCon.get(0).select("span");
		List<String> offers = new ArrayList<>();
		for(Element sp : offerSpans) {
			offers.add(sp.text().trim().toUpperCase());
		}

		Elements oddsCon = match.select("div > div > div > div:nth-child(4)");
		
		int indx = 0;
		for(String offer : offers) {

			if(
					offer.contentEquals("SPREAD")   || 
					offer.contentEquals("RUN LINE") || 
					offer.contentEquals("PUCK LINE")) {
				Spread spread = new Spread();
				spread.setPeriod(Period.GAME);
				try {
					String awaySpreadPts  = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button              > span").get(0).text();
					String homeSpreadPts  = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button:nth-child(2) > span").get(0).text();
					String awaySpreadLine = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button              >  div").get(0).text();
					String homeSpreadLine = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button:nth-child(2) >  div").get(0).text();
					spread.setAwayPoints(Double.parseDouble(awaySpreadPts));
					spread.setHomePoints(Double.parseDouble(homeSpreadPts));
					spread.setAwayPrice(Integer.parseInt(awaySpreadLine));
					spread.setHomePrice(Integer.parseInt(homeSpreadLine));
				} catch(Exception e3) {
					System.out.println("Failed to parse Spread odds: " 
							+ awayTeam + " at " + homeTeam +  oddsCon.text());
				}
				odds.setSpread(spread);
			}

			else if(offer.contentEquals("WIN")) {
				Spread ml = new Spread();
				ml.setAwayPoints(0.0);
				ml.setHomePoints(0.0);
				ml.setPeriod(Period.GAME);
				try {
					String awayML    = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button").get(0).text();
					String homeML    = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button:nth-child(2)").get(0).text();
					if(awayML.contentEquals("Closed") == false) {
						ml.setAwayPrice(Integer.parseInt(awayML));
					}
					if(homeML.contentEquals("Closed") == false) {
						ml.setHomePrice(Integer.parseInt(homeML));
					}
				} catch(Exception e3) {
					System.out.println("Failed to parse ML odds: " 
							+ awayTeam + " at " + homeTeam +  oddsCon.text());
				}
				odds.setMl(ml);
			}

			else if(
					offer.contentEquals("TOTAL POINTS") || 
					offer.contentEquals("TOTAL RUNS")   || 
					offer.contentEquals("TOTAL GOALS")  || 
					offer.contentEquals("TOTAL")) {
				OU ou = new OU();
				ou.setPeriod(Period.GAME);
				try {
					String overPts   = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button              > span").get(0).text().replace("O", "").trim();
					String overLine  = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button              >  div").get(0).text();
					String underLine = oddsCon.get(0).select("div > div:nth-child(" + (indx+1) + ") > button:nth-child(2) >  div").get(0).text();
					ou.setPoints(Double.parseDouble(overPts.trim()));
					ou.setOver(Integer.parseInt(overLine));
					ou.setUnder(Integer.parseInt(underLine));
				} catch(Exception e3) {
					System.out.println("Failed to parse OU odds: " 
							+ awayTeam + " at " + homeTeam +  oddsCon.text());
				}
				odds.setOu(ou);
			} else {
				System.out.println("Error: don't know the heading: " + offer);
			}

			indx++;
		}

		if((odds.getAway() != null) && (odds.getHome() != null)) {
			list.add(odds);
		} else {
			System.out.println("Not persisting: " + odds);
		}
		
		return true;
	}

	private Date getGameTime(Element match) {
		try {
			Element time = match.select("div > div > div > div > div > div > time").first();
			if(time != null) {
				return getGameTime(time.text());
			}
		} catch(Exception e) {
			System.out.println("Exception finding the time element: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private Date getGameTime(WebElement match) {
		try {
			WebElement time = match.findElement(By.tagName("time"));
			if(time != null) {
				return getGameTime(time.getText());
			}
		} catch(Exception e) {
			System.out.println("Exception finding the time element: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private Date getGameTime(String timeString) {
		Date gameTime = null;
		try {
			if(timeString != null) {
				int month=0, day=0, year=0;
				int hour=0, minute=0;
				Calendar c = Calendar.getInstance();
				c.setTime(new Date());
				String[] parts = timeString.split(" "); // should be Today or Tomorrow or date (ex: 4/11/26), h:mm, then Am/PM
				if(parts[0].compareToIgnoreCase("TODAY") == 0) {
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
				} else if(parts[0].compareToIgnoreCase("TOMORROW") == 0) {
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
				} else if(parts[0].toUpperCase().contains("STARTING")) {
					gameTime = new Date(); // just use right now, close enough
				} else if(Arrays.asList("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").contains(parts[0].toUpperCase())) {
					String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
					int gameDow = 0;
					for(String d : days) {
						if(parts[0].toUpperCase().contentEquals(d)) {
							break;
						}
						gameDow++;
					}
					if((gameDow >= 7)) {
						System.out.println("Failed to find the day for " + parts[0]);
					} else {
						int currDow = c.get(Calendar.DAY_OF_WEEK) - 1;
						if(gameDow < currDow) {
							gameDow += 7;
						}

						c.add(Calendar.DATE, (gameDow-currDow));
						month = c.get(Calendar.MONTH) + 1;
						day = c.get(Calendar.DAY_OF_MONTH);
						year = c.get(Calendar.YEAR);
						String dateStr = parts[1];
						String[] hm = dateStr.split(":");
						hour = Integer.parseInt(hm[0]);
						minute = Integer.parseInt(hm[1].replace("AM", "").replace("PM", ""));
						if(parts[2].contains("PM")) {
							if(hour != 12) {
								hour +=12;
							}
						} else {
							if(hour == 12) {
								hour = 0;
							}
						}
					}
				} else { // date of the form m/d/y h:mm a/p
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
				
				gameTime = new SimpleDateFormat("yyyy-MM-dd HH:mm")
						.parse(String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute));
			}
		} catch(Exception e5) {
			System.out.println("Exception: " + e5.getMessage());
			e5.printStackTrace();
		}
		return gameTime;
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
				driver.findElement(By.tagName("main"));
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

		System.out.println(new Date() + ": Processing BETRIVERS");

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

		BetRivers mgm = new BetRivers(useTheDriver);
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

		System.out.println(new Date() + ": Done Processing BETRIVERS");

	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
