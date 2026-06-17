package scanner.scanner.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.exceptions.OddsException;
import scanner.scanner.model.Team;
import scanner.scanner.repo.PlayerRepo;
import scanner.scanner.repo.TeamRepo;
import scanner.scanner.repo.UpdateRepo;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

public class MlbPlayerService {

	WebDriver driver = null;
	private TeamService teamService = null;
	private PlayerService playerService = null;

	public static void main(String[] args) {

		MlbPlayerService service = new MlbPlayerService();
		service.setUpServices();
		
		// Remove existing MLB players
//		service.getPlayerService().removeExistingPlayers(Sportsbook.ESPN_MLB_REF);
		
		service.bringUpDriver();
		String url = "https://www.espn.com/mlb/players";
		service.downloadPage(url);

		service.getTeams(url);
	}
	
	@SuppressWarnings("unused")
	private PlayerService getPlayerService() {
		return playerService;
	}

	private void setUpServices() {

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

	}

	private void setPlayerService(PlayerService ps) {
		this.playerService = ps;
	}

	private void setTeamService(TeamService tSrv) {
		this.teamService = tSrv;
	}

	@SuppressWarnings("deprecation")
	private void getTeams(String mainUrl) {

		List<WebElement> teamLinks = driver.findElements(By.cssSelector("a[href*=mlb\\/teams\\/roster]"));
		int numLinks = teamLinks.size();
		
		for(int linkNum = 0; linkNum < numLinks; ++linkNum) {
			teamLinks = driver.findElements(By.cssSelector("a[href*=mlb\\/teams\\/roster]"));
			WebElement link = teamLinks.get(linkNum);
			processTeam(link.getAttribute("href"), mainUrl);
		}
	}

	private void processTeam(String link, String mainUrl) {
		downloadPage(link);
		WebElement teamName = driver.findElement(By.tagName("h1"));
		String nameOfTeam = teamName.getText().replace("Roster", "").replace("\n", " ").trim();
		System.out.println("Team: " + nameOfTeam);
		Team team = null;
		try {
			team = teamService.getTeam(Sportsbook.ESPN_MLB_REF, Sport.MLB_STATS, nameOfTeam, true);
		} catch(Exception e3) {
			downloadPage(mainUrl);
			return;
		}

		
		List<WebElement> players = driver.findElements(By.cssSelector("a[href*=mlb\\/player]"));
		int playerCount = 0;
		for(WebElement player : players) {
			String name = player.getText().trim();
			if(name.length() > 0) {
				playerCount++;
				System.out.println(" " + name);
				try {
					playerService.getPlayer(team, name);
				} catch (OddsException e) {
					System.out.println("Failed to find player: " + name);
				}

			}
		}
		System.out.println(" NumPlayers: " + playerCount);
		downloadPage(mainUrl);
	}


	
	private void downloadPage(String url) {
		driver.get(url);
		driver.manage().window().maximize();
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

		FirefoxOptions options = new FirefoxOptions()
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
	}


}
