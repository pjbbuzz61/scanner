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
import java.util.ArrayList;
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
				break;
			case NBA:
				urls.add("https://sportsbook.draftkings.com/leagues/basketball/nba");
				break;
			case NCAAM:
				urls.add("https://sportsbook.draftkings.com/leagues/basketball/ncaab");
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
				urls.add("https://sportsbook.draftkings.com/leagues/tennis/australian-open-men");
				urls.add("https://sportsbook.draftkings.com/leagues/tennis/australian-open-women");
				break;
			default:
				break;
		
		}
		return urls;
	}

	private List<Odds> getMatchups(Sport sport, String url) throws IOException, OddsException {

		if(useDriver) {
			
			refresh(sport, url);
			
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
		
		boolean failed = false;
		try {
			odds.setAway(getTeam(this.sportsbook, sport, teams.get(0).text(), true));
		} catch(Exception e3) {
			failed = true;
		}
		try {
			odds.setHome(getTeam(this.sportsbook, sport, teams.get(1).text(), true));
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

		
		// TODO - set game time
//		Elements gameTime = e.select("time");
//		System.out.println(gameTime.text());

		Elements oddsCon = match.select("div.cb-side-column__right");
//		Elements buttons = oddsCon.get(0).select("button");
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
				driver.findElement(By.cssSelector("section[data-testid='league-page-widget-container']"));
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
			case "NHL":    sport = Sport.NHL;    break;
			case "TENNIS": sport = Sport.TENNIS; break;
			case "NBA":    sport = Sport.NBA;    break;
			case "NFL":    sport = Sport.NFL;    break;
			case "NCAAF":  sport = Sport.NCAAF;  break;
			case "NCAAM":  sport = Sport.NCAAM;  break;
			case "MLB":    sport = Sport.MLB;    break;
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
		
	}

	private void setOddsService(OddsService os) {
		this.oddsService = os;
	}

}
