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
import scanner.scanner.util.Period;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.Status;

@Component
public class Espn extends Book {

	Random random = new Random(System.currentTimeMillis());

	public Espn() {
		super(Sportsbook.ESPN);
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

		refresh(sport);
		
		try {

			WebElement scroll = driver.findElement(By.cssSelector("section[data-testid='marketplace-shelf-']"));

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
				
/*
			int h = 0;
			try {
				WebElement mainView = 
						driver.findElement(By.id("main-view"));
				if(mainView != null) {
					System.out.println("Main View size: w: " + mainView.getRect().width + ", h: " + mainView.getRect().height);
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
			System.out.println("Number of scroll cycles up: " + numCycles);
			for(int i = 0; i < numCycles; ++i) {
				Thread.sleep(150);
				robot3.mouseWheel(-3);
			}
*/
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
					System.getProperty("user.home") + "/" + 
							this.sportsbook + "_" + System.currentTimeMillis() + ".html"; 
			
			readClipboard(filename);

			List<Odds> list = null;
			try {
				switch(sport) {
					case MLB:
						break;
					case NBA:
						list = parseNcaaf(filename, sport);
						break;
					case NCAAB:
						break;
					case NCAAF:
						list = parseNcaaf(filename, sport);
						break;
					case NFL:
						list = parseNcaaf(filename, sport);
						break;
					case NHL:
						list = parseNcaaf(filename, sport);
						break;
					case SOCCER_EPL:
						break;
					case TENNIS:
						list = parseTennis(filename, sport);
						break;
					default:
						break;
				}
			} catch(Exception eee) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/crash.txt"));
				writer.write("parse crashed: " + eee);
				writer.close();
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
		} catch(Exception e) {
			// might not be a visible scrollbar
			System.out.println(e);
			e.printStackTrace();
		}

		return null;
	}
	
	private List<Odds> parseNcaaf(String file, Sport sport) {

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
		
		Elements container = doc.select("section[data-testid=marketplace-shelf-]");
		Elements games = container.select("article");
		System.out.println(games.size());
		for(Element game : games) {
			processEventTeam(game, list, sport);
		}
		
		return list;
	}


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

		Elements containers = e.select("div.bg-card-primary > div.relative > div");
		if(containers.size() != 4) {
			System.out.println("Failed to get the expected containers for data");
			return;
		}
		
		Element away = containers.get(2).select("div.text-primary").first();
		String awayTeam = away.text().toUpperCase();
		if(awayTeam.contains(")")) {
			awayTeam = awayTeam.substring(awayTeam.indexOf(")")+1).trim();
		}
		Element home = containers.get(3).select("div.text-primary").first();
		String homeTeam = home.text().toUpperCase();
		if(homeTeam.contains(")")) {
			homeTeam = homeTeam.substring(homeTeam.indexOf(")")+1).trim();
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
			return;
		}
		
		// Look for LIVE indicator
		Elements isLive = containers.get(1).select("div[data-testid='live-indicator']");
		if(isLive.size() > 0) {
			return;
		}

		Element dateTime = containers.get(1).select("span").first();
		odds.setPeriod(Period.NONE); 
		odds.setStatus(Status.SCHEDULED);
		odds.setGameDateTime(getStartingDate(dateTime.text()));

		Elements line = containers.get(2).select("button");
		// should be 4 buttons --
		// #2 is away spread
		// #3 is over points and line
		// #4 is ml away

		Elements scans = line.get(1).select("span");
		// second scan contains fir line (o/u points or spread points)
		// third line is the spread ml, or o/u line
		String awaySpreadPts = scans.get(1).text();
		String awaySpreadLine = scans.get(2).text().replace("Even", "+100");
		
		scans = line.get(2).select("span");
		// second scan contains fir line (o/u points or spread points)
		// third line is the spread ml, or o/u line
		String overUnderPts = scans.get(1).text().replace("O", "");
		String overLine = scans.get(2).text().replace("Even", "+100");

		scans = line.get(3).select("span");
		String awayMl = scans.get(2).text().replace("Even", "+100");

		line = containers.get(3).select("button");

		scans = line.get(1).select("span");
		String homeSpreadPts = scans.get(1).text();
		String homeSpreadLine = scans.get(2).text().replace("Even", "+100");
		
		scans = line.get(2).select("span");
		String underLine = scans.get(2).text().replace("Even", "+100");

		scans = line.get(3).select("span");
		String homeMl = scans.get(2).text().replace("Even", "+100");

		Spread spread = new Spread();
		spread.setPeriod(Period.GAME);
		try {
			spread.setAwayPoints(Double.parseDouble(awaySpreadPts));
			spread.setHomePoints(Double.parseDouble(homeSpreadPts));
			spread.setAwayPrice(Integer.parseInt(awaySpreadLine));
			spread.setHomePrice(Integer.parseInt(homeSpreadLine));
		} catch(Exception e3) {
			System.out.println("Failed to parse Spread odds: " 
					+ awaySpreadPts + " " + awaySpreadLine + " " + homeSpreadPts + " " + homeSpreadLine);
			
		}
		odds.setSpread(spread);
		
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

		OU ou = new OU();
		ou.setPeriod(Period.GAME);
		try {
			ou.setPoints(Double.parseDouble(overUnderPts.trim()));
			ou.setOver(Integer.parseInt(overLine));
			ou.setUnder(Integer.parseInt(underLine));
		} catch(Exception e3) {
			System.out.println("Failed to parse OU odds: " 
					+ overUnderPts + " " + overLine + " " + underLine);
		}
		odds.setOu(ou);

		
		if((odds.getAway() != null) && (odds.getHome() != null)) {
			list.add(odds);
		} else {
			System.out.println("Not persisting: " + odds);
		}
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

	private void refresh(Sport sport) {

		String url = null;
		switch(sport) {
			case MLB:
				break;
			case NBA:
				url = "https://espnbet.com/sport/basketball/organization/united-states/competition/nba";
				break;
			case NCAAB:
				break;
			case NCAAF:
				url = "https://espnbet.com/sport/football/organization/united-states/competition/ncaaf";
				break;
			case NFL:
				url = "https://espnbet.com/sport/football/organization/united-states/competition/nfl";
				break;
			case NHL:
				url = "https://espnbet.com/sport/hockey/organization/united-states/competition/nhl";
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

		
		WebElement popup = null;
		int cntr = 0;
		while(popup == null) {
			try {
				popup = driver.findElement(By.cssSelector("button[id='onetrust-accept-btn-handler']"));
			} catch(Exception e) {
				
			}
			try {Thread.sleep(100L);} catch (InterruptedException e) {}
			if(cntr++ >= 100) {
				break;
			}
		}
		System.out.println("Counter is " + cntr);
		if(popup == null) {
			System.out.println(this.sportsbook + ": Failed to get app start up");
			return;
		}
		System.out.println("Clicking off the popup");
		popup.click();
		try {Thread.sleep(500L);} catch (InterruptedException e) {}
		
		WebElement modalClose = null;
		cntr = 0;
		while(modalClose == null) {
			try {
				modalClose = driver.findElement(By.cssSelector("button[id='modal-close-button']"));
			} catch(Exception e) {
				
			}
			try {Thread.sleep(100L);} catch (InterruptedException e) {}
			if(cntr++ >= 100) {
				break;
			}
		}
		System.out.println("Counter is " + cntr);
		if(modalClose == null) {
			System.out.println(this.sportsbook + ": Failed to get modal close");
			return;
		}
		System.out.println("Clicking off the init offer");
		modalClose.click();
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
					
		// Make sure the panel is active
		boolean found = false;
		for(int i = 0; i < 100; ++i) {
			try {
				driver.findElement(By.cssSelector("section[data-testid='marketplace-shelf-']"));
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

		if(args.length != 2) {
			System.out.println("Requires two args: sport and delete odds flag");
			return;
		}
		Sport sport = null;
		switch(args[0].toUpperCase()) {
			case "NHL":    sport = Sport.NHL;    break;
			case "TENNIS": sport = Sport.TENNIS; break;
			case "NBA":    sport = Sport.NBA;    break;
			case "NFL":    sport = Sport.NFL;    break;
			case "NCAAF":  sport = Sport.NCAAF;  break;
			case "NCAAB":  sport = Sport.NCAAB;  break;
			case "MLB":    sport = Sport.MLB;    break;
			default: System.out.println("Unknown sport: " + args[0]); return;
		}
		System.out.println("Sport is " + sport);

		boolean deleteOdds = false;
		if(args[1].toUpperCase().contentEquals("TRUE")) {
			deleteOdds = true;
		}
		System.out.println("Delete existing set to " + deleteOdds);
		
		Espn mgm = new Espn();
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
