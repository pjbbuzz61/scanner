package scanner.scanner.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Tourney {

	public static void main(String[] args) {

		
		String file = System.getProperty("user.home") + "/teams.html";

        List<String> lines = new ArrayList<>();
        
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
		} catch(Exception e) {
			System.out.println("Exception reading html file: " + e.getMessage());
		}
		
		List<String> teamList = new ArrayList<>();
		

		for(String line : lines) {
			
			Document doc = null;
			try {
				doc = Jsoup.parse(line);
			} catch(Exception e) {
				System.out.println("Exception parsing file: " + e);
				e.printStackTrace();
				continue;
			}

			List<Element> teams = doc.select("div.TeamCell");
			for(Element team : teams) {
				teamList.add(team.text());
				System.out.println(team.text());
			}
		}
		

		file = System.getProperty("user.home") + "/bpi.html";

		lines = new ArrayList<>();
        
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
			reader.close();
		} catch(Exception e) {
			System.out.println("Exception reading html file: " + e.getMessage());
		}
		
		List<Double> bpiList = new ArrayList<>();
		

		for(String line : lines) {
			
			Document doc = null;
			try {
				doc = Jsoup.parse(line);
			} catch(Exception e) {
				System.out.println("Exception parsing file: " + e);
				e.printStackTrace();
				continue;
			}

			Elements linesForTeam = doc.select("tbody > tr");
			for(Element lineForTeam : linesForTeam) {
				Element bpi = lineForTeam.selectFirst("tr > td:nth-child(2)");
				bpiList.add(Double.parseDouble(bpi.text()));
				System.out.println(bpi.text());
			}
		}
		
		
		
	}

}
