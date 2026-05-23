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
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;

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

import scanner.scanner.model.Team;
import scanner.scanner.model.mlbStats.UpcomingGame;
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
public class FanDuel extends Book {

	Random random = new Random(System.currentTimeMillis());
	int numPersisted = 0;

	public FanDuel(boolean useTheDriver) {
		super(Sportsbook.FANDUEL, useTheDriver);
	}

	public FanDuel() {
		super(Sportsbook.FANDUEL, true);
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

		if(useDriver) {
			
			refresh(sport);
			if(sport == Sport.MLB_STATS) {
				parseMlbStats();
				quitDriver();
				return null;
			}
			
			try {

				Actions actions = new Actions(driver);

				WebElement body = driver.findElement(By.tagName("body"));
				List<WebElement> links = body.findElements(By.tagName("li"));
				for(WebElement link : links) {
					if(
							link.getText().contentEquals(sport.toString() + " Odds")
								||
							((sport == Sport.NCAAF) && link.getText().contains("College Football Games"))
								||
							((sport == Sport.NCAAF) && link.getText().contains("College Football Odds"))
								||
							((sport == Sport.NCAAM) && link.getText().contains("College Basketball"))
								||
							((sport == Sport.NCAAW) && link.getText().contains("College Basketball"))
								||
							((sport == Sport.TENNIS) && link.getText().contains("Matches"))
							
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
			System.out.print("Copy sport " + sport + " from FanDuel to the clipboard, then return");
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
					list = parseTeamEvent(filename, sport);
					break;
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
	
	private void parseMlbStats() {
		
		List<WebElement> uls = driver.findElements(By.tagName("ul"));
		List<WebElement> lis = null;
		WebElement ul = null;

		int ulsSize = uls.size();
		for(int ulIndex = 0; ulIndex < ulsSize; ++ulIndex) {
			ul = uls.get(ulIndex);
			if(ul.getText().contains("MLB Odds")) {
				// This (might be/is) the one we want
				// Now grab the li with "More wagers"
				lis = ul.findElements(By.tagName("li"));
				int liSize = lis.size(); 
				for(int liIndex = 0; liIndex < liSize; ++liIndex) {
					WebElement li = lis.get(liIndex);
					if(li.getText().contains("More wagers")) {
						
						// See if game is live. If so, ignore
						try {
							li.findElement(By.cssSelector("svg[aria-label='live event']"));
							continue;
						} catch(Exception e) {
							// do nothing, event is pre-match
						}

						// Get the game time
						WebElement time = li.findElement(By.tagName("time"));
						@SuppressWarnings("deprecation")
						String dt = time.getAttribute("datetime");
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'"); 
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
						Date gameDateTime = null;
						try {
							gameDateTime = sdf.parse(dt);
						} catch (ParseException e) {
							System.out.println("Failed to parse the game time: " + dt);
							continue;
						}

						List<WebElement> anchors = li.findElements(By.tagName("a"));
						boolean found = false;
						for(WebElement anchor : anchors) {
							if(anchor.getText().contentEquals("More wagers")) {
								waitForClick(anchor);
								found = true;
								break;
							}
						}
						if(found) {
							
							// process the game
							List<Odds> oddsList = processMlbGame(gameDateTime);
							
							persistOddsForMlbStats(oddsList);
							
							driver.navigate().back();

							// doing this just to make it look good :)
							try {Thread.sleep(1000);} catch(Exception ee) {}

							// reload all the objects from the original screen
							uls = driver.findElements(By.tagName("ul"));
							ul = uls.get(ulIndex);
							lis = ul.findElements(By.tagName("li"));
						}
					}
				}
			}
		}

		
		return;
	}

	private List<Odds> processMlbGame(Date gameDateTime) {

		List<Odds> oddsList = new ArrayList<>();
		
		WebElement breadcrumbs = waitForElement(By.cssSelector("nav[aria-label=Breadcrumbs]"));

		// Find all the nav tags so I can hit the Batter Props
		List<WebElement> navs = driver.findElements(By.tagName("nav"));
		boolean found = false;
		for(WebElement nav : navs) {
			if(nav.getText().contains("Batter Props")) {
				List<WebElement> anchors = nav.findElements(By.tagName("a"));
				for(WebElement anchor : anchors) {
					if(anchor.getText().contains("Batter Props")) {
						waitForClick(anchor);
						found = true;
						break;
					}
				}
				if(found) {
					// sleep a little before going back
					try {Thread.sleep(2000);} catch(Exception ee) {}
				}
			}
			if(found) {
				break;
			}
		}

		// Get the Teams and the date and time
		breadcrumbs = waitForElement(By.cssSelector("nav[aria-label=Breadcrumbs]"));
		String brd = breadcrumbs.getText();
		String parts[] = brd.split("/");
		String teamParts[] = parts[2].replace("Odds", "").split("@");
		String awayTeam = teamParts[0].trim();
		String homeTeam = teamParts[1].trim();
		boolean failed = false;
		Team away = null;
		Team home = null;
		try {
			away = getTeam(this.sportsbook, Sport.MLB_STATS, awayTeam, true);
		} catch(Exception e3) {
			failed = true;
		}
		try {
			home = getTeam(this.sportsbook, Sport.MLB_STATS, homeTeam, true);
		} catch(Exception e3) {
			failed = true;
		}
		if(failed) {
			return oddsList;
		}

		// Find all the topics I want and expand/Show More on them so I only have to read in the data once
		WebElement main = driver.findElement(By.id("main"));
		List<WebElement> uls = main.findElements(By.tagName("ul"));
		for(WebElement ul : uls) {
			if(ul.getText().contains("To Hit A Single")) {
				List<WebElement> lis = ul.findElements(By.tagName("li"));
				for(WebElement li : lis) {
					try {
						WebElement button = li.findElement(By.cssSelector("div[aria-expanded]"));
						if(topicOfInterest(button.getText())) {
							expandTopic(li);
						}
					} catch(Exception e) {} // do nothing, not an li we're looking for
				}
				
			}
		}
		
		// Batter props are up -- process them
		main = driver.findElement(By.id("main"));
		uls = main.findElements(By.tagName("ul"));
		for(WebElement ul : uls) {
			if(ul.getText().contains("To Hit A Single")) {
				List<WebElement> lis = ul.findElements(By.tagName("li"));
				for(WebElement li : lis) {
					try {
						li.findElement(By.cssSelector("div[aria-expanded]"));
						processBatterProp(li, away, home, gameDateTime, oddsList);
					} catch(Exception e) {} // do nothing, not an li we're looking for
				}
				
			}
		}
		
		return oddsList;
	}

	private void expandTopic(WebElement element) {
		WebElement button = element.findElement(By.cssSelector("div[aria-expanded]"));
		@SuppressWarnings("deprecation")
		String expanded = button.getAttribute("aria-expanded");
		if(expanded.contentEquals("false")) {
			waitForClick(button);
		}

		try {
			WebElement showMore = element.findElement(By.cssSelector("div[aria-label='Show more']"));
			waitForClick(showMore);
		} catch(Exception e) {}

	
	}

	private boolean topicOfInterest(String title) {

		switch(title) {
			case "To Hit A Home Run":
			case "To Record A Hit":
			case "To Record 2+ Hits":
			case "To Record A Stolen Base":
			case "To Record A Run":
			case "To Record An RBI":
			case "Player To Record 1+ Hits + Runs + RBIs":
			case "Player To Record 2+ Hits + Runs + RBIs":
			case "To Record 2+ Total Bases":
			case "To Record 3+ Total Bases":
			case "To Hit A Single":
			case "To Hit A Double":
			case "To Hit A Triple":
				return true;
			case "Player to Hit a Home Run in First Plate Appearance":
			case "To Hit 2+ Home Runs":
			case "Home Run / Moneyline Parlay":
			case "To Record 3+ Hits":
			case "To Record 4+ Hits":
			case "To Record 2+ Stolen Bases":
			case "To Record 2+ Runs":
			case "To Record 2+ RBIs":
			case "Player To Record 3+ Hits + Runs + RBIs":
			case "Player To Record 4+ Hits + Runs + RBIs":
			case "To Record 4+ Total Bases":
			case "To Record 5+ Total Bases":
			case "To Record 3+ Runs":
			case "To Record 3+ RBIs":
			case "To Record 4+ RBIs":
			case "To Hit a Laser (110+ MPH)":
				return false;
			default:
				System.out.println("Unknown Title for section: " + title);
				return false;
		}
	}

	private void processBatterProp(WebElement element, Team away, Team home, Date gameDateTime, List<Odds> oddsList) {

		MLB_STAT mlbStat = null;
		Double overUnderPoints = null;
		boolean process = true;
		
		WebElement button = element.findElement(By.cssSelector("div[aria-expanded]"));

		String title = button.getText();
		switch(title) {
			
			case "To Hit A Home Run":
				mlbStat = MLB_STAT.HR;
				overUnderPoints = 0.5;
				break;

			case "To Record A Hit":
				mlbStat = MLB_STAT.HITS;
				overUnderPoints = 0.5;
				break;

			case "To Record 2+ Hits":
				mlbStat = MLB_STAT.HITS;
				overUnderPoints = 1.5;
				break;

			case "To Record A Stolen Base":
				mlbStat = MLB_STAT.SB;
				overUnderPoints = 0.5;
				break;

			case "To Record A Run":
				mlbStat = MLB_STAT.RUNS;
				overUnderPoints = 0.5;
				break;

			case "To Record An RBI":
				mlbStat = MLB_STAT.RBI;
				overUnderPoints = 0.5;
				break;

			case "Player To Record 1+ Hits + Runs + RBIs":
				mlbStat = MLB_STAT.H_R_RBI;
				overUnderPoints = 0.5;
				break;

			case "Player To Record 2+ Hits + Runs + RBIs":
				mlbStat = MLB_STAT.H_R_RBI;
				overUnderPoints = 1.5;
				break;

			case "To Record 2+ Total Bases":
				mlbStat = MLB_STAT.BASES;
				overUnderPoints = 1.5;
				break;

			case "To Hit A Single":
				mlbStat = MLB_STAT.SINGLES;
				overUnderPoints = 0.5;
				break;

			case "To Hit A Double":
				mlbStat = MLB_STAT.DOUBLES;
				overUnderPoints = 0.5;
				break;

			case "To Hit A Triple":
				mlbStat = MLB_STAT.TRIPLES;
				overUnderPoints = 0.5;
				break;

			case "To Record 3+ Total Bases":
			case "Player to Hit a Home Run in First Plate Appearance":
			case "To Hit 2+ Home Runs":
			case "Home Run / Moneyline Parlay":
			case "To Record 3+ Hits":
			case "To Record 4+ Hits":
			case "To Record 2+ Stolen Bases":
			case "To Record 2+ Runs":
			case "To Record 3+ Runs":
			case "To Record 2+ RBIs":
			case "To Record 3+ RBIs":
			case "To Record 4+ RBIs":
			case "Player To Record 3+ Hits + Runs + RBIs":
			case "Player To Record 4+ Hits + Runs + RBIs":
			case "To Record 4+ Total Bases":
			case "To Record 5+ Total Bases":
			case "To Hit a Laser (110+ MPH)":
				process = false;
				break;
				
			default:
				System.out.println("Unknown Title for section: " + title);
				process = false;
				break;
		}

		if(process) {
			
			WebElement firstDiv  = element.findElement(By.xpath("./*"));
			WebElement secondDiv = firstDiv.findElement(By.xpath("./*"));
			WebElement thirdDiv  = secondDiv.findElement(By.xpath("./*"));
			List<WebElement> divList = thirdDiv.findElements(By.xpath("./*"));

			for(int index = 2; index < divList.size(); ++index) {
				WebElement currDiv = divList.get(index);
				WebElement currDiv2 = currDiv.findElement(By.xpath("./*"));
				WebElement currDiv3 = currDiv2.findElement(By.xpath("./*"));
				WebElement currDiv4 = currDiv3.findElement(By.xpath("./*"));
				WebElement currDiv5 = currDiv4.findElement(By.xpath("./*"));
				List<WebElement> divListForLine = currDiv5.findElements(By.xpath("./*"));
				for(WebElement theDiv : divListForLine) {
					
					List<WebElement> spans = theDiv.findElements(By.tagName("span"));
					String currentPlayer = null;
					for(int spanIndex = 0; spanIndex < spans.size(); spanIndex+=2) {

						currentPlayer = spans.get(spanIndex).getText();

						Player pl = null;
						Team theTeam = null;
							
						// Find the player first
						try {
							Object res = getPlayer(Arrays.asList(away, home), currentPlayer);
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
										pl = getPlayer(t, currentPlayer);
									} catch(Exception e2) {
										System.out.println("Failed to find player: " + currentPlayer);
										continue;
									}
								}
							}
						} catch(Exception ee) {
							System.out.println("Failed to find player: " + currentPlayer);
							continue;
						}

						try {
							Integer ml = Integer.parseInt(spans.get(spanIndex+1).getText());
							Odds odds = new Odds();
							odds.setTimeStamp(new Date());
							odds.setBook(this.sportsbook);
							odds.setSport(Sport.MLB_STATS);
							odds.setPeriod(Period.GAME); 
							odds.setStatus(Status.SCHEDULED);
							odds.setMlbStat(mlbStat);
							odds.setGameDateTime(gameDateTime);
								
							OU ou = new OU();
							ou.setPoints(overUnderPoints);
							ou.setOver(ml);
								
							odds.setOu(ou);
							odds.setHome(home);
							odds.setAway(away);
							odds.setPlayer1(pl);
							odds.setPlayer2(pl);
							odds.setHome(theTeam);
							odds.setAway(theTeam);

							oddsList.add(odds);
							
						} catch(Exception e) {
							System.out.println("Failed to parse ml of " + spans.get(spanIndex+1).getText());
							currentPlayer = null; // reset
							continue;
						}

					} // for each span in the current div
				}
				
				
			} // for all divs with player info

		}
		
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
		
		Elements container = null;
		if(sport == Sport.TENNIS) {
			container = doc.select("main > div > div > div > div > div:nth-child(2) > div:nth-child(2) > ul");
		} else {
			container = doc.select("main > div > div > div > div:nth-child(2) > div:nth-child(3) > ul");
		}
		Elements games = container.select("li");
		int numGames = 0;
		for(Element game : games) {
			Elements hdrs = game.select("h2[role=heading]");
			if(hdrs.size() > 0) {
				continue;
			}
			hdrs = game.select("h3[role=heading]");
			if(hdrs.size() > 0) {
				continue;
			}
//			if(game.text().contains(sport + " Odds")) {
//				continue;
//			}
//			if(game.text().contains("Spread Money Total")) {
//				continue;
//			}
			numGames++;
			processEventTeam(game, list, sport);
		}
		
		System.out.println("Number of games read in:   " + numGames);
		System.out.println("Number of games persisted: " + numPersisted);
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

		String away = null;
		String home = null;
		
		try {
			Elements hrefs = e.select("a[href]");
			if(hrefs != null) {
				String url = hrefs.get(0).attr("href");
				odds.setUrl(url);
			}
			Elements anchor = e.select("div:nth-child(1) > div:nth-child(1) > div:nth-child(1) > a:nth-child(1)");
			Elements spans = anchor.select("span[aria-label]");
			
			boolean failed = false;
			try {
				away = spans.get(0).text().trim();
				if(sport == Sport.TENNIS) {
					if(away.contains("/")) {
						return; // a doubles match
					}
				}
				odds.setAway(getTeam(this.sportsbook, sport, away, true));
			} catch(Exception e3) {
				failed = true;
			}
			try {
				home = spans.get(1).text().trim();
				if(sport == Sport.TENNIS) {
					if(home.contains("/")) {
						return; // a doubles match
					}
				}
				odds.setHome(getTeam(this.sportsbook, sport, home, true));
			} catch(Exception e3) {
				failed = true;
			}
			if(failed) {
				System.out.println("Failed to look up both teams, not persisted game: " + e.text());
				return;
			}

			// Look for live event marker
			Elements liveBlock = e.select("div > div > a");
			Elements live = liveBlock.select("svg[aria-label='live event']");
			if((live != null) && (live.size() > 0)) {
				System.out.println(away + " at " + home + " is in progress, will not process");
				return;
			}

			try {
				Elements gameTime = e.select("time");
				// Ex: Sun 1:36pm ET or 1:36pm ET if same day
				String[] parts = gameTime.text().split(" ");
				Calendar c = Calendar.getInstance();
				c.setTime(new Date());
				int month=0, day=0, year=0;
				int hour=0, minute=0;
				if(parts.length == 2) { // Today
					month = c.get(Calendar.MONTH) + 1;
					day = c.get(Calendar.DAY_OF_MONTH);
					year = c.get(Calendar.YEAR);
					String dateStr = parts[0];
					String[] hm = dateStr.split(":");
					hour = Integer.parseInt(hm[0]);
					minute = Integer.parseInt(hm[1].replace("am", "").replace("pm", ""));
					if(dateStr.contains("pm")) {
						if(hour != 12) {
							hour +=12;
						}
					} else {
						if(hour == 12) {
							hour = 0;
						}
					}
				} else { // beyond today
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
						minute = Integer.parseInt(hm[1].replace("am", "").replace("pm", ""));
						if(dateStr.contains("pm")) {
							if(hour != 12) {
								hour +=12;
							}
						} else {
							if(hour == 12) {
								hour = 0;
							}
						}
					}
				}
				odds.setGameDateTime(
						new SimpleDateFormat("yyyy-MM-dd HH:mm")
							.parse(String.format("%04d-%02d-%02d %02d:%02d", year, month, day, hour, minute)));
				System.out.println("Game Time: " + odds.getGameDateTime());

			} catch(Exception eee) {
				System.out.println("Exception getting game start: " + eee.getMessage());
				eee.printStackTrace();
			}

		} catch(Exception e2) {
			System.out.println("Failed to get teams, live marker, or time: " + e2.getMessage());
			return;
		}
		
		Elements spreadPts = null;
		Elements spreadML = null;
		Elements moneylines = null;
		Elements ouPts = null;
		Elements ouML = null;

		Spread spread = new Spread();
		spread.setPeriod(Period.GAME);
		try {
			String label = "Spread";
			if(sport == Sport.NHL) {
				label = "Puck Line";
			} else if(sport == Sport.MLB) {
				label = "Run Line";
			}
			spreadPts = e.select("div[aria-label^=" + label + "] > span:nth-child(1)");
			spreadML  = e.select("div[aria-label^=" + label + "] > span:nth-child(2)");
			if((spreadPts == null) || (spreadML == null) || (spreadPts.size() < 2) || (spreadML.size() < 2)) {
				if(sport != Sport.TENNIS) {
					System.out.println("Failed to parse spread: " + away + " at " + home);
				}
			} else {
				spread.setAwayPoints(Double.parseDouble(spreadPts.get(0).text()));
				spread.setHomePoints(Double.parseDouble(spreadPts.get(1).text()));
				spread.setAwayPrice(Integer.parseInt(spreadML.get(0).text()));
				spread.setHomePrice(Integer.parseInt(spreadML.get(1).text()));
			}
		} catch(Exception e3) {
			System.out.println("Failed to parse Spread odds: " 
					+ spreadPts.get(0).text() + " " + spreadML.get(0).text() + " " 
					+ spreadPts.get(1).text() + " " + spreadML.get(1).text());
			
		}
		odds.setSpread(spread);

		Spread ml = new Spread();
		ml.setAwayPoints(0.0);
		ml.setHomePoints(0.0);
		ml.setPeriod(Period.GAME);
		try {
			if(sport != Sport.TENNIS) {
				moneylines = e.select("div[aria-label^=Money] > span:nth-child(1)");
				if((moneylines == null) || (moneylines.size() < 2)) {
					//System.out.println("Failed to parse moneylines: " + away + " at " + home);
				} else {
					ml.setAwayPrice(Integer.parseInt(moneylines.get(0).text()));
					ml.setHomePrice(Integer.parseInt(moneylines.get(1).text()));
				}
			} else {
				// for tennis
				moneylines = e.select("div[aria-label*=to win] > span:nth-child(1)");
				if((moneylines == null) || (moneylines.size() < 2)) {
					//System.out.println("Failed to parse moneylines: " + away + " at " + home);
				} else {
					ml.setAwayPrice(Integer.parseInt(moneylines.get(0).text()));
					ml.setHomePrice(Integer.parseInt(moneylines.get(1).text()));
				}
			}

		} catch(Exception e3) {
			System.out.println("Failed to parse ML odds: " 
					+ moneylines.get(0).text() + " " + moneylines.get(1).text());
		}
		odds.setMl(ml);

		OU ou = new OU();
		ou.setPeriod(Period.GAME);
		try {
			ouPts = e.select("div[aria-label^=Total] > span:nth-child(1)");
			ouML = e.select("div[aria-label^=Total] > span:nth-child(2)");
			if((ouPts == null) || (ouML == null) || (ouML.size() < 2) || (ouPts.size() < 1)) {
				if(sport != Sport.TENNIS) {
					System.out.println("Failed to parse totals: " + away + " at " + home);
				}
			} else {
				ou.setPoints(Double.parseDouble(ouPts.get(0).text().replace("O", "").trim()));
				ou.setOver(Integer.parseInt(ouML.get(0).text()));
				ou.setUnder(Integer.parseInt(ouML.get(1).text()));
			}
		} catch(Exception e3) {
			System.out.println("Failed to parse OU odds: " 
					+ ouPts.get(0).text().replace("O", "").trim() + " " + ouML.get(0).text() + " " + ouML.get(1).text());
		}
		odds.setOu(ou);

		if((odds.getAway() != null) && (odds.getHome() != null)) {
			numPersisted++;
			list.add(odds);
		} else {
			System.out.println("Not persisting: " + odds);
		}
	}
/*
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
*/
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

	public void refresh(Sport sport) {

		List<String> url = new ArrayList<>();
		switch(sport) {
			case MLB:
				url.add("https://sportsbook.fanduel.com/navigation/mlb");
				break;
			case MLB_STATS:
				url.add("https://sportsbook.fanduel.com/navigation/mlb");
				break;
			case NBA:
				url.add("https://sportsbook.fanduel.com/navigation/nba");
				break;
			case WNBA:
				url.add("https://sportsbook.fanduel.com/navigation/wnba");
				break;
			case NCAAM:
				url.add("https://sportsbook.fanduel.com/navigation/ncaab");
				break;
			case NCAAF:
				url.add("https://sportsbook.fanduel.com/navigation/ncaaf");
				break;
			case NCAAW:
				url.add("https://sportsbook.fanduel.com/navigation/ncaaw");
				break;
			case NFL:
				url.add("https://sportsbook.fanduel.com/navigation/nfl");
				break;
			case NHL:
				url.add("https://sportsbook.fanduel.com/navigation/nhl");
				break;
			case TENNIS:
				url.add("https://sportsbook.fanduel.com/tennis");
				
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

		/*
		// Based on their security we're on the main page right now, we need to navigate by their buttons to the 
		//  sport we want
		// Make sure the panel is active
		boolean found = false;
		WebElement root = null;
		for(int i = 0; i < 100; ++i) {
			try {
				root = driver.findElement(By.cssSelector("main[id='main']"));
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
		WebElement body = driver.findElement(By.tagName("body"));
		List<WebElement> links = body.findElements(By.tagName("li"));
		for(WebElement link : links) {
			if(link.getText().contentEquals("More " + sport.toString())) {
				System.out.println(link.getText());
				link.click();
				break;
			}
		}
*/		
		driver.manage().window().maximize();

		return;
	}




	
	public static void main(String args[]) {

		System.out.println(new Date() + ": Processing FANDUEL");

		// Wait at least one minute between tries
		String filePath = System.getProperty("user.home") + "/lastFanduelAccess.txt";
		try {
			String content = Files.readString(Path.of(filePath));
			Long lastAccessTime = Long.parseLong(content);
			long diff = System.currentTimeMillis() - lastAccessTime;
			if(diff < 60000) {
				System.out.println("Too soon to try again: Last time was " + diff + " ms ago.");
				System.exit(0);
			}
		} catch (IOException e) {
			System.out.println("Access file does not exist. First time?");
		}

		// Write out current access time
		Path path = Path.of(filePath);
		String content = String.format("%d", System.currentTimeMillis());
		try {
			Files.writeString(path, content);
		} catch (IOException e) {
			System.out.println("Failed to write out access file");
			System.exit(0);
		}
		
		
		
		if(args.length < 2) {
			System.out.println("Requires two args: sport and delete odds flag, along with optional useDriver flag");
			return;
		}
		Sport sport = null;
		switch(args[0].toUpperCase()) {
			case "NHL":       sport = Sport.NHL;        break;
			case "TENNIS":    sport = Sport.TENNIS;     break;
			case "NBA":       sport = Sport.NBA;        break;
			case "WNBA":      sport = Sport.WNBA;       break;
			case "NFL":       sport = Sport.NFL;        break;
			case "NCAAF":     sport = Sport.NCAAF;      break;
			case "NCAAM":     sport = Sport.NCAAM;      break;
			case "NCAAW":     sport = Sport.NCAAW;      break;
			case "MLB":       sport = Sport.MLB;        break;
			case "MLB_STATS": sport = Sport.MLB_STATS;  break;
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
		
		
		FanDuel fd = new FanDuel(useTheDriver);
		fd.setUpServices();
		
		if(deleteOdds) {
			fd.getOddsService().removeAll(sport);
		}
		try {
			fd.acquire(sport);
		} catch(Exception e) {
			System.out.println("Exception from acquire: " + e);
			e.printStackTrace();
		}

		System.out.println(new Date() + ": Done Processing FANDUEL");

	}

	public void setUpServices() {

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
		setTeamService(tSrv);
		
		PlayerService ps = new PlayerService();
		PlayerRepo pRepo = new PlayerRepo();
		pRepo.setMongoTemplate(mt);
		ps.setRepo(pRepo);
		ps.setUpdateService(uSrv);
		setPlayerService(ps);

		OddsService os = new OddsService();
		OddsRepo oRepo = new OddsRepo();
		oRepo.setMongoTemplate(mt);
		os.setRepo(oRepo);
		setOddsService(os);
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}
	private OddsService getOddsService() {
		return this.oddsService;
	}

	public List<UpcomingGame> getUpcomingGames() throws Exception {
		
		List<UpcomingGame> listOfGames = new ArrayList<>();

		List<WebElement> uls = driver.findElements(By.tagName("ul"));
		List<WebElement> lis = null;
		WebElement ul = null;

		int ulsSize = uls.size();
		for(int ulIndex = 0; ulIndex < ulsSize; ++ulIndex) {
			ul = uls.get(ulIndex);
			if(ul.getText().contains("MLB Odds")) {
				// This (might be/is) the one we want
				// Now grab the li with "More wagers"
				lis = ul.findElements(By.tagName("li"));
				int liSize = lis.size(); 
				for(int liIndex = 0; liIndex < liSize; ++liIndex) {
					WebElement li = lis.get(liIndex);
					if(li.getText().contains("More wagers")) {
						
						// See if game is live. If so, ignore
						try {
							li.findElement(By.cssSelector("svg[aria-label='live event']"));
							continue;
						} catch(Exception e) {
							// do nothing, event is pre-match
						}

						// Get the game time
						WebElement time = li.findElement(By.tagName("time"));
						@SuppressWarnings("deprecation")
						String dt = time.getAttribute("datetime");
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'"); 
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
						Date gameDateTime = null;
						try {
							gameDateTime = sdf.parse(dt);
						} catch (ParseException e) {
							System.out.println("Failed to parse the game time: " + dt);
							continue;
						}

						List<WebElement> anchors = li.findElements(By.tagName("a"));
						WebElement link = null;
						boolean found = false;
						for(WebElement anchor : anchors) {
							if(anchor.getText().contentEquals("More wagers")) {
								link = anchor;
								found = true;
								break;
							}
						}
						
						if(found) {
							List<WebElement> teams = li.findElements(By.cssSelector("span[aria-label][role='text']"));
							if(teams.size() != 2) {
								System.out.println("Failed to find two teams");
								continue;
							}
							
							boolean failed = false;
							Team away = null;
							Team home = null;
							try {
								away = getTeam(this.sportsbook, Sport.MLB_STATS, teams.get(0).getText(), true);
							} catch(Exception e3) {
								failed = true;
							}
							try {
								home = getTeam(this.sportsbook, Sport.MLB_STATS, teams.get(1).getText(), true);
							} catch(Exception e3) {
								failed = true;
							}
							if(failed) {
								continue;
							}
							
							UpcomingGame upGame = new UpcomingGame();
							upGame.setBook(this.sportsbook);
							upGame.setAway(away);
							upGame.setHome(home);
							upGame.setGameTime(gameDateTime);
							upGame.setLink(link);

							listOfGames.add(upGame);

						}
					}
				}
			}
		}

		return listOfGames;
	}

	public void acquireMlbStats(UpcomingGame game) throws Exception {
		
		if(game == null) {
			System.out.println(this.sportsbook + ": No game returned");
			return;
		}

		System.out.println(this.sportsbook + ": Processing game: " + 
				game.getAway().getCommonName() + " at " + game.getHome().getCommonName() + " " + new Date());

		if(game.getLink() == null) {
			System.out.println(this.sportsbook + ": Link is null: " + game);
			return;
		}

		waitForClick(game.getLink());

		// process the game
		List<Odds> oddsList = processMlbGame(game.getGameTime());
	
		persistOddsForMlbStats(oddsList);
	
		driver.navigate().back();
		
		System.out.println(this.sportsbook + ": DONE processing game: " + 
				game.getAway().getCommonName() + " at " + game.getHome().getCommonName() + " " + new Date());

	}

}
