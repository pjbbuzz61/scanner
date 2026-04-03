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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.Team;
import scanner.scanner.model.OU;
import scanner.scanner.model.Odds;
import scanner.scanner.model.TeamTotal;
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
@ComponentScan
public class BetMGM extends Book {

	Random random = new Random(System.currentTimeMillis());

	public BetMGM(boolean useTheDriver) {
		super(Sportsbook.BETMGM, useTheDriver);
	}

	public BetMGM() {
		super(Sportsbook.BETMGM, true);
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
				urls.add("https://www.md.betmgm.com/en/sports/baseball-23/betting/usa-9/mlb-75");
//				urls.add("https://www.md.betmgm.com/en/sports/baseball-23/betting/world-6/world-baseball-classic-7405");
				break;
			case MLB_STATS:
				urls.addAll(getMlbUrls());
				break;
			case NBA:
				urls.add("https://www.md.betmgm.com/en/sports/basketball-7/betting/usa-9/nba-6004");
				break;
			case NCAAM:
				urls.add("https://www.md.betmgm.com/en/sports/basketball-7/betting/usa-9/ncaa-264");
				break;
			case NCAAW:
				urls.add("https://www.md.betmgm.com/en/sports/basketball-7/betting/usa-9/ncaaw-5241");
				break;
			case NCAAF:
				urls.add("https://www.md.betmgm.com/en/sports/football-11/betting/usa-9/college-football-211");
				break;
			case NFL:
				urls.add("https://www.md.betmgm.com/en/sports/football-11/betting/usa-9/nfl-35");
				break;
			case NHL:
				urls.add("https://www.md.betmgm.com/en/sports/hockey-12/betting/usa-9/nhl-34");
				break;
			case TENNIS:
				urls.add("https://www.md.betmgm.com/en/sports/tennis-5/betting/atp-6");
				urls.add("https://www.md.betmgm.com/en/sports/tennis-5/betting/wta-7");
				break;
			default:
				break;
		
		}
		return urls;
	}

	private List<String> getMlbUrls() {
		
		List<String> rtn = new ArrayList<>();
		
		// Get MLB urls sitting on the odds_MLB collections
		List<Odds> oddsList = oddsService.getOdds(Sport.MLB, Sportsbook.BETMGM);
		if(oddsList != null) {
			for(Odds ods : oddsList) {
				if(ods.getUrl() != null) {
					rtn.add("https://www.md.betmgm.com" + ods.getUrl());
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
				if(list != null) {
					for(Odds odds : list) {
						persistOdds(odds, "odds" + "_" + sport);
					}
				}
				return list;
			}

			int lastPersisted = 100;
			
			try {
				
				String containerTag = "ms-main-column";
				if(sport == Sport.MLB_STATS) {
					containerTag = "ms-event-details-main";
				}
				WebElement scroll = driver.findElement(By.tagName(containerTag));
				
				Actions actions = new Actions(driver);

				// Pull up context menu
				javascriptExecutor.executeScript("arguments[0].scrollIntoView(true);", scroll);
				actions.contextClick(scroll).build().perform();

				Robot robot = new Robot();
				
				// Select the debug window from the context menu
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_Q);
				robot.keyRelease(KeyEvent.VK_Q);
				
				// Mouse into the Elements display and click to gain focus
				Thread.sleep(1000);
				robot.mouseMove(500,1000);
				Thread.sleep(100);
				robot.keyPress(KeyEvent.VK_ENTER);
				Thread.sleep(100);
				robot.keyRelease(KeyEvent.VK_ENTER);
				Thread.sleep(100);

				// Page up enough times to get to the top
				for(int i = 0; i < 30; ++i) {
					Thread.sleep(100);
					robot.keyPress(KeyEvent.VK_PAGE_UP);
					Thread.sleep(100);
					robot.keyRelease(KeyEvent.VK_PAGE_UP);
				}
					
				int h = 0;
				try {
					WebElement mainView = 
							driver.findElement(By.id("main-view"));
					if(mainView != null) {
						//System.out.println("Main View size: w: " + mainView.getRect().width + ", h: " + mainView.getRect().height);
						h = mainView.getRect().height;
					}
				} catch(Exception ee) {
						
				}

				Robot robot3 = new Robot();
				robot3.mouseMove(1510,400);
				for(int i = 0; i < 5; ++i) {
					Thread.sleep(10);
					robot3.mouseWheel(100);
				}
				int numCycles = 0;
				if(h > 0) {
					numCycles = h/350;
				} else {
					numCycles = lastPersisted * 60 / 100;
					if(numCycles < 35) numCycles = 35;
				}
				if(sport == Sport.MLB_STATS) {
					numCycles = 35;
				}
				//System.out.println("Number of scroll cycles up: " + numCycles);
				for(int i = 0; i < numCycles; ++i) {
					Thread.sleep(150);
					robot3.mouseWheel(-3);
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
			System.out.print("Copy sport " + sport + " from BetMGM to the clipboard, then return");
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
//					list = parseTennis(filename, sport);
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
	
	private List<Odds> parseMlbStats() {

		String homeTeam = null;
		String awayTeam = null;
		
		List<Odds> oddsList = new ArrayList<>();
		
		// Get the names for the home and away teams
		WebElement scoreboard = driver.findElement(By.cssSelector("div.main-score-container"));
		if(scoreboard == null) {
			System.out.println("Failed to look up the team names, outta here");
			return oddsList;
		}
			
		List<WebElement> participants = scoreboard.findElements(By.cssSelector("div.participant"));
		if((participants == null) || (participants.size() != 2)) {
			System.out.println("Failed to find the participants");
			return oddsList;
		}

		for(int i = 0; i < 2; ++i) {
			
			WebElement partName = participants.get(i).findElement(By.cssSelector("div.participant-name-value"));
			if(partName == null) {
				System.out.println("Failed to find one of the participants");
				return oddsList;
			}
			if(i == 0) awayTeam = partName.getText().trim();
			if(i == 1) homeTeam = partName.getText().trim();
		}

		// Select All first
		try {
			WebElement sitemap = driver.findElement(By.tagName("ms-event-details-sitemap"));
			if(sitemap != null) {
				List<WebElement> lis = sitemap.findElements(By.tagName("li"));
				if(lis != null) {
					boolean found = false;
					for(WebElement li : lis) {
						if(li.getText().contentEquals("All")) {
							li.click();
							found = true;
							break;
						}
					}
					if(found == false) {
						System.out.println("Did not find the All button on the sitemap");
					}
				} else {
					System.out.println("Failed to pull any list items from the sitemap");
				}
			}
		} catch(Exception e) {
			System.out.println("Failed to find the sitemap");
			return oddsList;
		}
		
		
		WebElement scroll = driver.findElement(By.tagName("ms-event-details-main"));
		List<WebElement> buttons = scroll.findElements(By.cssSelector("button[aria-label='Open Accordion']"));
		if(buttons != null) {
			for(WebElement e : buttons) {
				
				// Filter here - we want Batter O/Us, Team Totals, and alternate spread and total
				if(isTargetRow(e.getText(), homeTeam, awayTeam) == false) {
					continue;
				}

				int cnt = 0;
				int expandSize = 10;
				do {
					try {
						String cmd = "javascript:window.scrollBy(0," + expandSize + ")";
						javascriptExecutor.executeScript(cmd); 
						e.click();
						try {Thread.sleep(100);} catch(Exception ee) {}
						break;
					} catch(Exception eee) {
						System.out.println("Not view for expand, try again, Count is " + cnt + ", expand size is " + expandSize);
						try {Thread.sleep(100);} catch(Exception ee) {}
						cnt++;
						expandSize *= -1;
						if(expandSize < 0) expandSize -= 10;
						else expandSize += 10;
					}
						
				} while(cnt < 100);
			}
		}
		
		// Now click on all the show more buttons
		scroll = driver.findElement(By.tagName("ms-event-details-main"));
		List<WebElement> showMores = scroll.findElements(By.cssSelector("div.show-more-less-button"));
		if(showMores != null) {
			for(WebElement e : showMores) {
				if(e.getText().contains("Show More")) {
						
					int cnt = 0;
					javascriptExecutor.executeScript("javascript:window.scrollBy(0,-5000)"); 
					do {
						try {
							javascriptExecutor.executeScript("javascript:window.scrollBy(0,10)"); 
							e.click();
							try {Thread.sleep(100);} catch(Exception ee) {}
							break;
						} catch(Exception eee) {
							System.out.println("Not view for more, try again, Count is " + cnt);
							try {Thread.sleep(100);} catch(Exception ee) {}
							cnt++;
						}
							
					} while(cnt < 100);
				}
			}
		}
		

		// Get all the containers
		List<WebElement> groupingContainers = driver.findElements(By.cssSelector("div.team-grouping-container"));

		// Select team one, then team 2
		for(int buttonToSelect = 1; buttonToSelect <= 2; ++buttonToSelect) {
			
			// Select team buttons
			for(WebElement groupingContainer : groupingContainers) {
				List<WebElement> butts = groupingContainer.findElements(By.tagName("button"));
				butts.get(buttonToSelect).click();
			}
			
			List<WebElement> allPanels = driver.findElements(By.tagName("ms-option-panel"));
			for(WebElement panel : allPanels) {

				//System.out.println(panel.getText());
				
				WebElement name = panel.findElement(By.cssSelector("div[slot='title']"));
				if((name != null) && isTargetRow(name.getText(), homeTeam, awayTeam)) {
					boolean nameFound = true;
					MLB_STAT mlbStat = null;
					boolean isBatterStat = false;
					boolean isTeamTotals = false;
					boolean isSpread     = false;
					if(name.getText().contains("Total runs")) {
						mlbStat = MLB_STAT.TT;
						isTeamTotals = true;
					} else {
						switch(name.getText()) {
							case "Batter RBIs O/U":
								mlbStat = MLB_STAT.RBI;
								isBatterStat = true;
								break;
							case "Batter hits O/U":
								mlbStat = MLB_STAT.HITS;
								isBatterStat = true;
								break;
							case "Batter total bases O/U":
								mlbStat = MLB_STAT.BASES;
								isBatterStat = true;
								break;
							case "Batter home runs O/U":
								mlbStat = MLB_STAT.HR;
								isBatterStat = true;
								break;
							case "Batter runs O/U":
								mlbStat = MLB_STAT.RUNS;
								isBatterStat = true;
								break;
							case "Batter H+R+RBIs O/U":
							case "Batter hits +runs + RBIs O/U":
								mlbStat = MLB_STAT.H_R_RBI;
								isBatterStat = true;
								break;
							case "Batter singles O/U":
								mlbStat = MLB_STAT.SINGLES;
								isBatterStat = true;
								break;
							case "Batter doubles O/U":
								mlbStat = MLB_STAT.DOUBLES;
								isBatterStat = true;
								break;
							case "Batter triples O/U":
								mlbStat = MLB_STAT.TRIPLES;
								isBatterStat = true;
								break;
							case "Batter stolen bases O/U":
								mlbStat = MLB_STAT.SB;
								isBatterStat = true;
								break;
							case "Spread":
								mlbStat = MLB_STAT.SPREAD;
								isSpread = true;
								break;
							case "Totals":
								mlbStat = MLB_STAT.TOTALS;
								break;
							default:
								System.out.println("Unknown MLB stat: " + name.getText());
								nameFound = false;
								break;
						}
					}
					if(nameFound == false) {
						continue;
					}
					
					try {
						if(isBatterStat) {
							processBatterStat(panel, name, oddsList, mlbStat, buttonToSelect, homeTeam, awayTeam);
						} else if(isTeamTotals) { 
							processTeamTotals(panel, name, oddsList, mlbStat, homeTeam, awayTeam);
						} else if(isSpread) {
							processSpread(panel, name, oddsList, mlbStat, homeTeam, awayTeam);
						} else { // totals
							processTotals(panel, name, oddsList, mlbStat, homeTeam, awayTeam);
						}
					} catch(Exception e) {
						System.out.println("Exception processing data: " + e.getMessage());
					}
				}
			}
		}
		
		return oddsList;
	}
	
	private void processTeamTotals(WebElement panel, WebElement name, List<Odds> oddsList, MLB_STAT mlbStat,
			String homeTeam, String awayTeam) throws Exception {

		boolean homeTeamData = true;
		if(name.getText().contains(awayTeam)) {
			homeTeamData = false;
		} else if(name.getText().contains(homeTeam)) {
			homeTeamData = true;
		} else {
			System.out.println("Can't determine if this is away or home for team totals: " + name.getText());
			return;
		}

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
			return;
		}

		Period currentPeriod = Period.GAME;

		WebElement rowContainer = null;
		try {
			rowContainer = panel.findElement(By.cssSelector("div.option-group-container"));
		} catch(Exception e) {
			System.out.println("No milestone container for panel name " + name.getText());
			return;
		}

		String overPoints = null;
		String overML = null;
		String underML = null;
		List<WebElement> options = rowContainer.findElements(By.tagName("ms-option"));
			
		for(int index = 0; index < options.size(); index+=2) {
				
			WebElement overOption = options.get(index);
			WebElement underOption = options.get(index+1);
			WebElement overpts = overOption.findElement(By.cssSelector("div.name"));
			WebElement overml  = overOption.findElement(By.cssSelector("div.value"));
			overPoints = overpts.getText();
			overML     = overml.getText();

			WebElement underml  = underOption.findElement(By.cssSelector("div.value"));
			underML     = underml.getText();

			try {
				Double ovPoints = Double.parseDouble(overPoints.replace("O", "").replace("Over", "").trim());
				Integer ovmoneyline = Integer.parseInt(overML);
				Integer unmoneyline = Integer.parseInt(underML);
				TeamTotal tt = new TeamTotal();
				tt.setHome(homeTeamData);
				tt.setOver(ovmoneyline);
				tt.setUnder(unmoneyline);
				tt.setPoints(ovPoints);
				
				Odds odds = new Odds();
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(currentPeriod); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(mlbStat);
				odds.setHome(hTeam);
				odds.setAway(aTeam);
				if(homeTeamData) {
					odds.setTtHome(tt);
				} else {
					odds.setTtAway(tt);
				}
				oddsList.add(odds);
			} catch(Exception e3) {
				// do nothing
			}
		}
	}
	
	private void processTotals(WebElement panel, WebElement name, List<Odds> oddsList, MLB_STAT mlbStat,
			String homeTeam, String awayTeam)  throws Exception {

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
			return;
		}

		WebElement hdrItemsContainer = null;
		boolean noHeader = false;
		try {
			hdrItemsContainer = panel.findElement(By.cssSelector("div.ds-tab-header-items"));
		} catch(Exception e) {
			// No header with different periods, so I'll fake it to have one. Because I can
			noHeader = true;
		}

		// Get list of buttons
		List<WebElement> buttons = null;
		if(noHeader) {
			buttons = new ArrayList<>();
			buttons.add(panel);  // just adding panel since it's a WebElement - it won't be used
		} else {
			buttons = hdrItemsContainer.findElements(By.tagName("button"));
		}
		Period currentPeriod = null;
		for(WebElement button : buttons) {
			String buttonName = null;
			if(noHeader) {
				buttonName = "Full game";
			} else {
				buttonName = button.getText();
			}
			boolean found = true;
			switch(buttonName) {
				case "Full game":
					currentPeriod = Period.GAME;
					break;
				case "First 3 innings":
					currentPeriod = Period.INNING1_3;
					break;
				case "First 5 innings":
					currentPeriod = Period.INNING1_5;
					break;
				case "First 7 innings":
					currentPeriod = Period.INNING1_7;
					break;
				default:
					System.out.println("Unknown period: " + buttonName);
					found = false;
					break;
			}
			if(found == false) {
				continue;
			}

			WebElement rowContainer = null;
			try {
				rowContainer = panel.findElement(By.cssSelector("div.option-group-container"));
			} catch(Exception e) {
				System.out.println("No milestone container for panel name " + name.getText());
				return;
			}

			String points = null;
			String ML = null;
			List<WebElement> options = rowContainer.findElements(By.tagName("ms-option"));
			Map<Double, OU> ouMap = new HashMap<Double, OU>();
			
			for(WebElement option : options) {
				
				WebElement pts = option.findElement(By.cssSelector("div.name"));
				WebElement ml  = option.findElement(By.cssSelector("div.value"));
				points = pts.getText();
				ML     = ml.getText();

				try {
					Double ouPoints = Double.parseDouble(points.replace("Over", "").replace("Under", "").replace("O", "").replace("U", "").trim());
					Integer moneyline = Integer.parseInt(ML);
					OU ou = null;
					if((ou = ouMap.get(ouPoints)) == null) {
						ou = new OU();
						ou.setPeriod(currentPeriod);
						ouMap.put(ouPoints, ou);
					}
					ou.setPoints(ouPoints);
					if(points.contains("O") || points.contains("Over")) {
						ou.setOver(moneyline);
					} else {
						ou.setUnder(moneyline);
					}
				} catch(Exception e3) {
					// do nothing
				}

			}
			
			for (Map.Entry<Double, OU> entry : ouMap.entrySet()) {
				Odds odds = new Odds();
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(currentPeriod); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(mlbStat);
				odds.setOu(entry.getValue());
				odds.setHome(hTeam);
				odds.setAway(aTeam);
				
				oddsList.add(odds);
			}
			
		} // for all periods

	}

	private void processSpread(WebElement panel, WebElement name, List<Odds> oddsList, MLB_STAT mlbStat,
			String homeTeam, String awayTeam)  throws Exception {

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
			return;
		}

		Period currentPeriod = Period.GAME;

		WebElement rowContainer = null;
		try {
			rowContainer = panel.findElement(By.cssSelector("div.option-group-container"));
		} catch(Exception e) {
			System.out.println("No milestone container for panel name " + name.getText());
			return;
		}

		String awayPoints = null;
		String awayML = null;
		String homePoints = null;
		String homeML = null;
		List<WebElement> options = rowContainer.findElements(By.tagName("ms-option"));
			
		for(int index = 0; index < options.size(); index+=2) {
				
			WebElement awayOption = options.get(index);
			WebElement homeOption = options.get(index+1);
			WebElement awaypts = awayOption.findElement(By.cssSelector("div.name"));
			WebElement awayml  = awayOption.findElement(By.cssSelector("div.value"));
			awayPoints = awaypts.getText();
			awayML     = awayml.getText();

			WebElement homepts = homeOption.findElement(By.cssSelector("div.name"));
			WebElement homeml  = homeOption.findElement(By.cssSelector("div.value"));
			homePoints = homepts.getText();
			homeML     = homeml.getText();

			try {
				Double awaySpreadPoints = Double.parseDouble(awayPoints.trim());
				Integer awaymoneyline = Integer.parseInt(awayML);
				Double homeSpreadPoints = Double.parseDouble(homePoints.trim());
				Integer homemoneyline = Integer.parseInt(homeML);
				Spread s = new Spread();
				s.setAwayPoints(awaySpreadPoints);
				s.setAwayPrice(awaymoneyline);
				s.setHomePoints(homeSpreadPoints);
				s.setHomePrice(homemoneyline);
				s.setPeriod(currentPeriod);

				Odds odds = new Odds();
				odds.setTimeStamp(new Date());
				odds.setBook(this.sportsbook);
				odds.setSport(Sport.MLB_STATS);
				odds.setPeriod(currentPeriod); 
				odds.setStatus(Status.SCHEDULED);
				odds.setMlbStat(mlbStat);
				odds.setSpread(s);
				odds.setHome(hTeam);
				odds.setAway(aTeam);
					
				oddsList.add(odds);
			} catch(Exception e3) {
				// do nothing
			}

		}
			
	}

	private void processBatterStat(
			WebElement panel, WebElement name, 
			List<Odds> oddsList, MLB_STAT mlbStat, 
			int buttonToSelect, 
			String homeTeam, String awayTeam)  throws Exception {

		WebElement rowContainer = null;
		try {
			rowContainer = panel.findElement(By.cssSelector("div.option-group-container"));
		} catch(Exception e) {
			System.out.println("No milestone container for panel name " + name.getText());
			return;
		}
		
		List<WebElement> descs = rowContainer.findElements(By.xpath("./*"));
		//System.out.println(name.getText());
		
		Map<WebElement, List<WebElement>> list = new HashMap<WebElement, List<WebElement>>();
		
		WebElement currentPlayer = null;
		for(WebElement des : descs) {
			
			if(elementContains(des, "player-statistics")) {
	        	list.put(des, new ArrayList<WebElement>());
	        	currentPlayer = des;
	        } else if(elementContains(des, "option")) {
	        	List<WebElement> curr = list.get(currentPlayer);
	        	curr.add(des);
	        	list.put(currentPlayer, curr);
	        }

		}
		
		for (Map.Entry<WebElement, List<WebElement>> entry : list.entrySet()) {

			WebElement player      = entry.getKey();
			List<WebElement> options = entry.getValue();

			Odds odds = new Odds();
			odds.setTimeStamp(new Date());
			odds.setBook(this.sportsbook);
			odds.setSport(Sport.MLB_STATS);
			odds.setPeriod(Period.GAME); 
			odds.setStatus(Status.SCHEDULED);
			odds.setMlbStat(mlbStat);
			OU ou = new OU();
			ou.setPeriod(Period.GAME);
			odds.setOu(ou);

			
			Team team = null;
			Player pl = null;
			String playerName = null;

			// get the player name
			WebElement pName = player.findElement(By.cssSelector("span.title"));
			if(pName != null) {
				playerName = pName.getText().trim();
				if(playerName.length() == 0) {
					//System.out.println("Player doesn't exist");
					continue;
				} else {
					//System.out.println("Player: " + playerName);
				}
			}

			// find the img, which will give me the team
			try {
				team = getTeam(this.sportsbook, Sport.MLB_STATS, buttonToSelect==1?awayTeam:homeTeam, true);
				odds.setAway(team);
				odds.setHome(team);
			} catch(Exception e3) {
				//System.out.println("Failed to get team from image: " + teamImage);
				continue;
			}
				
			// get the player name
			try {
				pl = getPlayer(team, playerName);
				odds.setPlayer1(pl);
				odds.setPlayer2(pl);
			} catch(Exception ee) {
				System.out.println("Failed to find player: " + playerName);
				continue;
			}
			
			// Get the over/unders
			String points = null;
			String ML = null;
			for(WebElement op : options) {

				WebElement pts = op.findElement(By.cssSelector("div.name"));
				WebElement ml  = op.findElement(By.cssSelector("div.value"));
				points = pts.getText();
				ML     = ml.getText();

				try {
					ou.setPoints(Double.parseDouble(points.replace("Over", "").replace("Under", "").replace("O", "").replace("U", "").trim()));
					if(points.contains("O") || points.contains("Over")) {
						ou.setOver(Integer.parseInt(ML));
					} else {
						ou.setUnder(Integer.parseInt(ML));
					}
				} catch(Exception e3) {
					// do nothing
				}
			}
				
			oddsList.add(odds);

		} // for each player
	
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

	private boolean isTargetRow(String text, String home, String away) {
		boolean keepIt = false;
		if(text.contains("O/U") && text.contains("Batter"))    keepIt = true;
		if(text.contentEquals("Spread"))                       keepIt = true;
		if(text.contentEquals("Totals"))                       keepIt = true;
		if(text.contains("Total runs") && text.contains(home)) keepIt = true;
		if(text.contains("Total runs") && text.contains(away)) keepIt = true;
		return keepIt;
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
		
		List<String> periods = new ArrayList<>();
		Element headers = doc.select("ms-grid-header").first();
		if(headers != null) {
			Elements header = headers.select("ms-group-selector");
			if(header.size() > 0) {
				for(Element h : header) {
//					Element sel = h.select("span.ng-star-inserted").first();
					Element sel = h.select("span.group-title").first();
					if(sel != null) {
						periods.add(sel.text());
					} else {
						System.out.println("No sel");
					}
				}
			}
		}
		
		
		Elements eventGroups = doc.select("ms-event-group");
		String currTournament = null;
		int numGames = 0;
		for(Element eventGroup : eventGroups) {
			for(Element child : eventGroup.children()) {
				if(
						(child.tag().getName().contentEquals("ms-six-pack-event"))
							||
						(child.tag().getName().contentEquals("ms-event"))
						) {

					numGames++;
					processEventTeam(child, list, currTournament, sport, periods);
				}
			}
		}
		
		System.out.println("Number of games read in:   " + numGames);
		System.out.println("Number of games persisted: " + list.size());

		return list;
	}

	private void processEventTeam(Element e, List<Odds> list, String tournament, Sport sport, List<String> periods) {
		
		Calendar c = Calendar.getInstance();

		c.setTime(new Date());
		Odds odds = new Odds();
		odds.setTimeStamp(new Date());
		odds.setBook(this.sportsbook);
		odds.setSport(sport);

		String url = "URL not set";
		Elements link = e.select("a.grid-info-wrapper");
		if((link != null) && (link.size() > 0)) {
			url = link.first().attr("href");
			if(url != null) {
				String urlParts[] = url.split("-");
				odds.setUrl(url);
				odds.setGameNumber(urlParts[urlParts.length - 1]);
			} else {
				System.out.println("Failed to find href for the url, continuing without it");
			}
		} else {
			System.out.println("Failed to find the game link, continuing without it");
		}
		
		// Get the headers for the offers
		Elements hdrs = e.select("ms-group-header");
		Elements hdr = hdrs.get(0).select("div.grid-group-header");
		List<String> headers = new ArrayList<>();
		for(Element he : hdr) {
			headers.add(he.text().trim());
		}
		
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
		if(participants.size() == 2) {
			
			if(sport == Sport.TENNIS) {
				Elements es = participants.get(0).select("div.second-participant");
				if((es != null) && (es.size() > 0)) {
					return;
				}
			}
			String p1 = participants.get(0).text().toUpperCase().trim();
			String p2 = participants.get(1).text().toUpperCase().trim();
			Team p1Team = null;
			Team p2Team = null;
			boolean failed = false;
			try {
				p1Team = getTeam(this.sportsbook, sport, p1, true);
				odds.setAway(p1Team);
			} catch(Exception e3) {
				failed = true;
			}
			try {
				p2Team = getTeam(this.sportsbook, sport, p2, true);
				odds.setHome(p2Team);
			} catch(Exception e3) {
				failed = true;
			}
			if(failed) {
				return;
			}
		} else if(participants.size() != 0) {
			System.out.println("Dont have two particpants: " + e);
			//continue;
		}
		
		Elements oddsWrapper = null;
		if(sport == Sport.TENNIS) {
			oddsWrapper = e.select("div.grid-group-container");
		} else {
			oddsWrapper = e.select("div.grid-six-pack-wrapper");
		}
		if(oddsWrapper.size() > 0) {
			Elements blocks = null;
			if(sport == Sport.TENNIS) {
				blocks = oddsWrapper.get(0).select("ms-option-group");
				blocks.remove(1);
			} else {
				blocks = oddsWrapper.get(0).select("ms-option-group");
			}
			if(blocks.size() == headers.size()) { // should be three (ml, spread, o/u) -- or just ML for tennis
				int col = 0;
				for(Element grp : blocks) {
					Elements options = grp.select("ms-option.grid-option");
					if(options.size() == 2) {
						Elements ptsAway = options.get(0).select("div.option-name");
						Elements mlAway  = options.get(0).select("div.option-value");
						Elements ptsHome = options.get(1).select("div.option-name");
						Elements mlHome  = options.get(1).select("div.option-value");
						switch(headers.get(col)) {
							case "Spread":
								Spread spread = new Spread();
								spread.setPeriod(Period.GAME);
								try {
									spread.setAwayPoints(Double.parseDouble(ptsAway.text()));
									spread.setHomePoints(Double.parseDouble(ptsHome.text()));
									spread.setAwayPrice(Integer.parseInt(mlAway.text()));
									spread.setHomePrice(Integer.parseInt(mlHome.text()));
								} catch(Exception e3) {
									// do nothing
								}
								odds.setSpread(spread);
								break;
							case "Total":
								OU ou = new OU();
								ou.setPeriod(Period.GAME);
								try {
									ou.setPoints(Double.parseDouble(ptsAway.text().replace("O", "").replace("U", "").trim()));
									ou.setPoints(Double.parseDouble(ptsHome.text().replace("O", "").replace("U", "").trim()));
									ou.setOver(Integer.parseInt(mlAway.text()));
									ou.setUnder(Integer.parseInt(mlHome.text()));
								} catch(Exception e3) {
									// do nothing
								}
								odds.setOu(ou);
								break;
							case "Money":
								Spread ml = new Spread();
								ml.setAwayPoints(0.0);
								ml.setHomePoints(0.0);
								ml.setPeriod(Period.GAME);
								try {
									ml.setAwayPrice(Integer.parseInt(mlAway.text()));
									ml.setHomePrice(Integer.parseInt(mlHome.text()));
								} catch(Exception e3) {
									// do nothing
								}
								odds.setMl(ml);
								break;
							default:
								System.out.println("Don't understand the header value: " + headers.get(col));
								break;
						} // switch
					} else {
						//System.out.println("Didn't find two options for the offer: " + url + " " + headers.get(col));
						// Expected - likely no money line for the game
					}
					col++;
				} // for 3 blocks
					
			} else {
				System.out.println("Failed to find correct number of offers based on header " + url);
			}
		} else {
			System.out.println("Failed to find odds wrapper for " + url);
		}
		
		if((odds.getAway() != null) && (odds.getHome() != null)) {
			list.add(odds);
		} else {
			System.out.println("Not persisting: " + odds);
		}
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

		// This removes popup used for special events
		try {
			try {Thread.sleep(1000L);} catch (InterruptedException e) {}

			WebElement popup = driver.findElement(By.className("fullscreen-promo-banner"));
			if(popup != null) {
				WebElement scan = popup.findElement(By.className("theme-ex"));
				if(scan != null) {
					scan.click();
				}
			}
		} catch (Exception ee) {
			//System.out.println("No popup found");
		}

		// This removes popup used for special events
		try {
			try {Thread.sleep(1000L);} catch (InterruptedException e) {}

			WebElement popup = driver.findElement(By.className("fullsize-overlay-template"));
			if(popup != null) {
				WebElement scan = popup.findElement(By.className("header-ctrl"));
				if(scan != null) {
					scan.click();
				}
			}
		} catch (Exception ee) {
			//System.out.println("No popup found");
		}

		WebElement menu = null;
		int cntr = 0;
		while(menu == null) {
			try {
				menu = driver.findElement(By.id("main-view"));
			} catch(Exception e) {
				
			}
			try {Thread.sleep(100L);} catch (InterruptedException e) {}
			if(cntr++ >= 100) {
				break;
			}
		}
		//System.out.println("Counter is " + cntr);
		if(menu == null) {
			System.out.println("BetMGM: Failed to get app start up");
			return;
		}

//		try {
//			WebElement sortToggle = driver.switchTo().window(handle).findElement(By.xpath("//div[contains(@class, 'sort-toggle-button right-btn')]"));
//			sortToggle.click();processEvent
//		} catch(Exception e) {
//			System.out.println("Failed to find the time toggle");
//		}
		try {Thread.sleep(1000);} catch(Exception ee) {}

		for(int i = 0; i < 15; ++i) {
			try {
				WebElement moreEvents = driver
						.findElement(By.xpath("//div[contains(@class, 'grid-footer ms-active-highlight')]"));
				String t = moreEvents.getText();
				if(t.contentEquals("More Events")) {
					System.out.println("Clicking for more events ...");
					moreEvents.click();
					try {Thread.sleep(1000);} catch(Exception ee) {}
				} else {
//					System.out.println("Didn't find more events text ...");
					break;
				}
			} catch(Exception e) {
//				System.out.println("Exception: Didn't find more events: " + e.getMessage());
				break;
			}
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
				System.out.println("BetMGM: Failed to GET url, try number " + tries);
			}
		}

		if(success == false) {
			throw new OddsException("Failed to get url for BetMGM");
		}
					
		// Make sure the panel is active
		boolean found = false;
		for(int i = 0; i < 100; ++i) {
			try {
				driver.findElement(By.id("sports-nav"));
				found = true;
				break;
			} catch(Exception e) {
			}
			try {Thread.sleep(100L);} catch (InterruptedException e) {}
		}
		if(!found) {
			// In this case there's something wrong with the window, so a refresh is in order
			driver.navigate().refresh();
			System.out.println("BetMGM: Failed to Sports List Menu: Refreshing window to fix");
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

		BetMGM mgm = new BetMGM(useTheDriver);
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
		mgm.closeDriver();
		
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
