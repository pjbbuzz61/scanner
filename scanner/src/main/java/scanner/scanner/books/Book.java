package scanner.scanner.books;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.beans.factory.annotation.Autowired;

import scanner.scanner.exceptions.OddsException;
import scanner.scanner.model.Odds;
import scanner.scanner.model.Player;
import scanner.scanner.model.Team;
import scanner.scanner.service.OddsService;
import scanner.scanner.service.PlayerService;
import scanner.scanner.service.TeamService;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;
import scanner.scanner.util.MLB_STAT;

public abstract class Book {

	protected WebDriver driver = null;
	protected Process chromeProcess = null;
	protected JavascriptExecutor javascriptExecutor = null;
	protected String port;
	protected Boolean useDriver = true;

	
	@Autowired
	protected OddsService oddsService;

	@Autowired
	private TeamService teamService;
	
	@Autowired
	private PlayerService playerService;

	public void setPlayerService(PlayerService playerService) {
		this.playerService = playerService;
	}


	public Sportsbook sportsbook;

	public Book(Sportsbook sportsbook, boolean useDriver) {
		this.sportsbook = sportsbook;
		this.port = "9201";
		this.useDriver = useDriver;
		if(useDriver) {
			bringUpDriver();
		}
	}

	public abstract void acquire(Sport sport);
	
	public Team getTeam(Sportsbook book, Sport sport, String sbSpecificTeamName, boolean flagToUpdates) throws OddsException {
		return teamService.getTeam(book, sport, sbSpecificTeamName, flagToUpdates);
	}

	public Player getPlayer(Team team, String sbSpecificPlayerName) throws OddsException {
		return playerService.getPlayer(team, sbSpecificPlayerName);
	}

	public Object getPlayer(List<Team> teams, String sbSpecificPlayerName) throws OddsException {
		return playerService.getPlayer(teams, sbSpecificPlayerName);
	}

	public String getNumericType(String str) {
	    try {
	        Integer.parseInt(str);
	        return "Integer";
	    } catch (NumberFormatException e1) {
	        try {
	            Double.parseDouble(str);
	            return "Double";
	        } catch (NumberFormatException e2) {
	            return "Not a number";
	        }
	    }
	}
	
	public WebElement waitForElement(WebElement fromElement, By by) {
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

	public List<WebElement> getPopulatedList(WebElement container, By by) {
		return getPopulatedList(container, by, 1);
	}

	public List<WebElement> getPopulatedList(WebElement container, By by, int expectedCount) {
		
		int c = 0;
		List<WebElement> list = null;
		
		do {
			list = container.findElements(by);
			if(list.size() == 0) {
				try {Thread.sleep(10L);} catch (InterruptedException e4) {}
			}
			c++;
			if(c > 500) {
				System.out.println("Waited the full time for a list");
				break;
			}
		} while(list.size() < expectedCount);

		return list;
	}

	public WebElement waitForElement(By by) {
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
		} while(cnt < 50);

		return rtn;
	}

	public List<WebElement> waitForElements(By by) {
		List<WebElement> rtn = null;
		int cnt = 0;
		do {
			try {
				rtn = driver.findElements(by);
				break;
			} catch(Exception e) {
				try {Thread.sleep(100);} catch(Exception ee) {}
				cnt++;
			}
		} while(cnt < 20);

		return rtn;
	}

	public boolean waitForClick(WebElement element) {

		boolean success = false;
		int cnt = 0;
		//String name = element.getText();
		do {
			try {
				javascriptExecutor.executeScript("javascript:window.scrollBy(0,100)"); 
				element.click();
				try {Thread.sleep(100);} catch(Exception ee) {}
				success = true;
				break;
			} catch(Exception eee) {
				try {Thread.sleep(2);} catch(Exception ee) {}
				cnt++;
			}
		} while(cnt < 500);

		//System.out.println("WaitForClick: cnt: " + cnt + ", Name: " + name);
		
		return success;
	}

	public void persistOddsForMlbStats(List<Odds> oddsList) {

		Map<MLB_STAT, Integer> counts = new HashMap<>();
		
		if(oddsList != null) {
			for(Odds odds : oddsList) {
				if(counts.get(odds.getMlbStat()) == null) {
					counts.put(odds.getMlbStat(), 0);
				}
				counts.put(odds.getMlbStat(), counts.get(odds.getMlbStat()) + 1);
				persistOdds(odds, "odds" + "_" + Sport.MLB_STATS);
			}

			System.out.print(this.sportsbook + ": Persisted " + oddsList.size() + " records - ");
			for (Map.Entry<MLB_STAT, Integer> m : counts.entrySet()) {
			    System.out.print(" " + m.getKey() + ":  " + m.getValue());
			}
			System.out.println(); // add line break at the end
			oddsList.clear();
		}
	}

	public void persistOdds(Odds odds) {
		oddsService.persistOdds(odds);
	}

	public void persistOdds(Odds odds, String collection) {
		oddsService.persistOdds(odds, collection);
	}

	public void setTeamService(TeamService srv) {
		teamService = srv;
	}
	
	public Process startBrowser() {
		String cmd = null;
		try {
			if(System.getProperty("os.name").contentEquals("Linux")) {
				cmd = "/opt/google/chrome/chrome --remote-debugging-port=" + port;
			} else {
				cmd = "\"c:\\Program Files\\Google\\Chrome\\Application\\chrome\" --remote-debugging-port=" + port;
			}
			return Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void cycleDriver() {
		closeDriver();
		bringUpDriver();
	}

	public void closeDriver() {
		if(driver != null) {
			driver = null;
		}
	}
	
	public void quitDriver() {
		if(driver != null) {
			driver.quit();
		}
	}

	@SuppressWarnings("deprecation")
	public void bringUpDriver() {
		
		closeDriver();
		System.setProperty(
				"webdriver.gecko.driver", 
				System.getProperty("user.dir") + "/scanner/drivers/geckodriver_34");

//		String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36 OPR/60.0.3255.170";
		FirefoxOptions options = new FirefoxOptions()
//				.addPreference("general.useragent.override",userAgent)
				.setAcceptInsecureCerts(true)
				;
		
		int attempts = 0;
		do {
			try {
				driver = new FirefoxDriver(options);
				break;
			} catch(Exception e) {
				System.out.println("\nFailed to bring up FF driver, waiting and trying again: attempt: " + attempts + "\n");
				try{Thread.sleep(1000);} catch(Exception ee) {}
				attempts++;
			}
		} while(attempts < 10);
		
		driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
		javascriptExecutor = (JavascriptExecutor) driver;
/*
		closeDriver();
		System.setProperty(
				"webdriver.gecko.driver", 
				System.getProperty("user.dir") + "/scanner/drivers/geckodriver_34");

		File f = new File("/home/pat/snap/firefox/common/.cache/mozilla/firefox/pj2cz7pf.default");
		
		FirefoxProfile myProfile = new FirefoxProfile(f);

		FirefoxOptions options = new FirefoxOptions()
				.setProfile(myProfile)
				.setAcceptInsecureCerts(true);
		driver = new FirefoxDriver(options);
		driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
		javascriptExecutor = (JavascriptExecutor) driver;
*/
	}

}
