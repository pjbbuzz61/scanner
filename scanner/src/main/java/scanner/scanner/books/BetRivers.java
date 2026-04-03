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
import java.util.ArrayList;
import java.util.Arrays;
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
				if(list != null) {
					for(Odds odds : list) {
						persistOdds(odds, "odds" + "_" + sport);
					}
				}
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
						
						WebElement moreEventsLabel = driver
								.findElement(By.cssSelector("button[data-testid=show-more-events-button]"));

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
		
		// See if we have more events
		try {
			WebElement moreEventsLabel = driver
					.findElement(By.cssSelector("button[data-testid=show-more-events-button]"));
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

		int numGames = 0;
		WebElement container = 
				driver.findElement(By.cssSelector("div[data-testid=listview-group-" + MLB_GROUP_NUMBER + "-events-container]"));
		List<WebElement> games = container.findElements(By.tagName("article"));
		numGames = games.size();
//System.out.println("Number of games: " + numGames);		

		
		
		for(int gameNum = 0; gameNum < numGames; ++gameNum) {

			// See if we have more events
			try {
				WebElement moreEventsLabel = driver
						.findElement(By.cssSelector("button[data-testid=show-more-events-button]"));
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

			javascriptExecutor.executeScript("javascript:window.scrollBy(0,-5000)"); 
			try {Thread.sleep(1000);} catch(Exception ee) {}
			javascriptExecutor.executeScript("javascript:window.scrollBy(0," + gameNum*200 + ")"); 
			try {Thread.sleep(1000);} catch(Exception ee) {}

			try {
				
				// Refresh list of games
				int c = 0;
				container = null;
				do {
					try {
						container = driver.findElement(By.cssSelector("div[data-testid=listview-group-" + MLB_GROUP_NUMBER + "-events-container]"));
						break;
					} catch(Exception e) {
						System.out.println("Failed to get container: cnt: " + c);
						try {Thread.sleep(10L);} catch (InterruptedException e4) {}
						c++;
					}
				} while(c < 500);

				if(container == null) {
					System.out.println("failed to get the container");
					continue;
				}
	// this delay might be important to allow the game list to populate
	//try {Thread.sleep(2000L);} catch (InterruptedException e) {}

				c = 0;
				games = null;
				do {
	//System.out.println("Attempting to get games list again ...");
					games = container.findElements(By.tagName("article"));
	//System.out.println("Number of games: " + games.size() + ", cnt: " + c);
					c++;
				} while(games.size() == 0);

				// See if game is live
				WebElement liveIndicator = games.get(gameNum).findElement(By.cssSelector("div[data-testid^='default-header'"));
				if(liveIndicator.getText().contains("LIVE")) {
					System.out.println("Event is live");
					continue;
				}

				// Get the two teams
				List<WebElement> parts = games.get(gameNum).findElements(By.cssSelector("div[data-testid=participant-row]"));
				String awayTeam = parts.get(0).getText();
				String homeTeam = parts.get(1).getText();
				Team aTeam = null;
				Team hTeam = null;
				boolean foundBoth = true;
				try {
					aTeam = getTeam(this.sportsbook, Sport.MLB_STATS, awayTeam, true);
				} catch(Exception e3) {
					foundBoth = false;
				}
				try {
					hTeam = getTeam(this.sportsbook, Sport.MLB_STATS, homeTeam, true);
				} catch(Exception e3) {
					foundBoth = false;
				}

				if(foundBoth == false) {
					continue;
				}
	System.out.println("Home: " + hTeam.getCommonName());
	System.out.println("Away: " + aTeam.getCommonName());

	try {Thread.sleep(1000L);} catch (InterruptedException e) {}
				if(waitForClick(games.get(gameNum)) == false) {
					System.out.println("Failed to click the game");
					continue;
				}

				processMLBGame(oddsList, aTeam, hTeam);
	//System.out.println("OddsList has " + oddsList.size() + " items");			
				driver.navigate().back();
				try {Thread.sleep(2000L);} catch (InterruptedException e) {}

			} catch(Exception outerEx) {
				System.out.println("Exception processing game number: " + gameNum);
				System.out.println("Exception: " + outerEx.getMessage());
				outerEx.printStackTrace();
			}

		}
		
		return oddsList;
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

	private void processMLBGame(List<Odds> oddsList, Team awayTeam, Team homeTeam) {
		
		WebElement container = waitForElement(By.cssSelector("div.KambiBC-event-page-component__column--1"));
		if(container == null) {
			System.out.println("Failed to find the container");
			return;
		}

try {Thread.sleep(2000L);} catch (InterruptedException e) {}
		
		List<WebElement> selections = container.findElements(By.cssSelector("li.KambiBC-bet-offer-category"));
//System.out.println("Number of sections: " + selections.size());		
		for(WebElement section : selections) {

//System.out.println("Section: " + section.getText());

			// Expand Show Lists
			List<WebElement> showLists = section.findElements(By.cssSelector("button[aria-label^='Show list']"));

//System.out.println("Number of show lists: " + showLists.size());

			for(WebElement showList : showLists) {
				
				if(waitForClick(showList) == false) {
					System.out.println("Unable to click the Show List");
				}

			}
try {Thread.sleep(2000L);} catch (InterruptedException e) {}
			System.out.println();
			
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
					odds.setHome(homeTeam);
					odds.setAway(awayTeam);
						
					oddsList.add(odds);

				} catch(Exception e3) {
					// do nothing
				}
			}
			
			
			// Roll out each one
//			if(elementContains(section, "KambiBC-expanded") == false) {
//				WebElement h2 = section.findElement(By.tagName("h2"));
//				javascriptExecutor.executeScript("arguments[0].scrollIntoView();", h2);
//				try {Thread.sleep(500L);} catch (InterruptedException e) {}
//				h2.click();
//				try {Thread.sleep(500L);} catch (InterruptedException e) {}
//			}
			
			break; // this has us just processing the first section (most popular)
		}
		
//		for(Odds o : oddsList) {
//			System.out.println(o);
//		}
		return;
	}
	
	private boolean waitForClick(WebElement element) {

		boolean success = false;
		int cnt = 0;
		do {
			try {
				javascriptExecutor.executeScript("javascript:window.scrollBy(0,100)"); 
				element.click();
				try {Thread.sleep(100);} catch(Exception ee) {}
				success = true;
				break;
			} catch(Exception eee) {
				try {Thread.sleep(100);} catch(Exception ee) {}
				cnt++;
			}
		} while(cnt < 100);

		return success;
	}

	private WebElement waitForElement(By by) {
		WebElement rtn = null;
		int cnt = 0;
		do {
			try {
				rtn = driver.findElement(by);
				break;
			} catch(Exception e) {
				try {Thread.sleep(100);} catch(Exception ee) {}
				cnt++;
			}
		} while(cnt < 20);

		return rtn;
	}

	private WebElement waitForElement(WebElement fromElement, By by) {
		WebElement rtn = null;
		int cnt = 0;
		do {
			try {
				rtn = fromElement.findElement(by);
				break;
			} catch(Exception e) {
				try {Thread.sleep(100);} catch(Exception ee) {}
				cnt++;
			}
		} while(cnt < 20);

		return rtn;
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

		// TODO - set game time
//		Elements gameTime = e.select("time");
//		System.out.println(gameTime.text());

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
		
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
