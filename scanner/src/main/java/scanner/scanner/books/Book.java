package scanner.scanner.books;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
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
	
	@SuppressWarnings("deprecation")
	public void bringUpDriver() {
		
		closeDriver();
		System.setProperty(
				"webdriver.gecko.driver", 
				System.getProperty("user.dir") + "/scanner/drivers/geckodriver_34");

		String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36 OPR/60.0.3255.170";
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
		} while(attempts < 3);
		
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
