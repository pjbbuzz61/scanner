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

	public Book(Sportsbook sportsbook) {
		this.sportsbook = sportsbook;
		this.port = "9201";
		bringUpDriver();
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
	
	@SuppressWarnings("deprecation")
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
	
	public void bringUpDriver() {
		
		closeDriver();
		System.setProperty(
				"webdriver.gecko.driver", 
				System.getProperty("user.dir") + "/scanner/drivers/geckodriver_34");
		FirefoxOptions options = new FirefoxOptions()
				.setAcceptInsecureCerts(true);
		driver = new FirefoxDriver(options);
		driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
		javascriptExecutor = (JavascriptExecutor) driver;
/*
		closeDriver();
		String exe = "chromedriver_v128.exe";
		if(System.getProperty("os.name").contentEquals("Linux")) {
			exe = "chromedriver_141";
		}
		String drv = System.getProperty("user.dir") + 
				File.separator +
				"drivers" +
				File.separator +
				exe;
		System.setProperty(
				"webdriver.chrome.driver", 
				drv);
		driver = new ChromeDriver();
		javascriptExecutor = (JavascriptExecutor) driver;
*/
	}

}
