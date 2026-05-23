package scanner.scanner.model.mlbStats;

import java.util.Date;

import org.openqa.selenium.WebElement;

import scanner.scanner.model.Team;
import scanner.scanner.util.Sportsbook;

public class UpcomingGame {

	private Sportsbook book;
	private Team       away;
	private Team       home;
	private Date       gameTime;
	private int        gameNum;
	private WebElement link;
	
	@Override
	public String toString() {
	
		StringBuilder sb = new StringBuilder();

		sb.append("Book:  " + book + "\n");
		sb.append("Away:  " + away);
		sb.append("Home:  " + home);
		sb.append("Time:  " + gameTime + "\n");
		sb.append("GmNum: " + gameNum + "\n");
//		sb.append("Link:  " + link);
		return sb.toString();
	}

	public Team getAway() {
		return away;
	}
	public void setAway(Team away) {
		this.away = away;
	}
	public Team getHome() {
		return home;
	}
	public void setHome(Team home) {
		this.home = home;
	}
	public Date getGameTime() {
		return gameTime;
	}
	public void setGameTime(Date gameTime) {
		this.gameTime = gameTime;
	}
	public int getGameNum() {
		return gameNum;
	}
	public void setGameNum(int gameNum) {
		this.gameNum = gameNum;
	}
	public WebElement getLink() {
		return link;
	}
	public void setLink(WebElement link) {
		this.link = link;
	}
	public Sportsbook getBook() {
		return book;
	}
	public void setBook(Sportsbook book) {
		this.book = book;
	}
	
}
