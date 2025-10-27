package scanner.scanner.repo;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import scanner.scanner.model.Odds;
import scanner.scanner.util.Period;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;


@Repository
public class OddsRepo {

    @Autowired
    private MongoTemplate mongoTemplate;
    
    public Odds save(Odds odds) {
    	mongoTemplate.save(odds);
    	return odds;
    }

    public Odds save(Odds odds, String collection) {
    	mongoTemplate.save(odds, collection);
    	return odds;
    }

    public List<Odds> getAll() {
       return mongoTemplate.findAll(Odds.class);
    }

	public void removeAll() {
		mongoTemplate.remove(new Query(), Odds.class);
	}

	public void removeAll(Sport sport) {
		mongoTemplate.remove(new Query(), Odds.class, "odds" + "_" + sport);
	}

	public List<Odds> getOdds(Sport sport, Period period, String collectionName) {
		return mongoTemplate.find(
				new Query().addCriteria(
						Criteria.where("sport").is(sport)
						.and("period").is(period)), 
				Odds.class,
				collectionName);
	}

	public Long getCount(Sportsbook sportsbook) {
		return mongoTemplate.count(new Query().addCriteria(Criteria.where("book").is(sportsbook)), Odds.class);
	}

	public Long getCount() {
		return mongoTemplate.count(new Query(), Odds.class);
	}

	public List<Odds> getOdds(Odds odds) {
		if(odds.getSport() == Sport.MLB) {
			return mongoTemplate.find(
					new Query().addCriteria(
							Criteria.where("sport").is(odds.getSport())
							.and("away.commonName").is(odds.getAway().getCommonName())
							.and("home.commonName").is(odds.getHome().getCommonName())
							.and("awayPitcher.commonName").is(odds.getAwayPitcher().getCommonName())
							.and("homePitcher.commonName").is(odds.getHomePitcher().getCommonName())
							),Odds.class);
		} else {
			return mongoTemplate.find(
					new Query().addCriteria(
							Criteria.where("sport").is(odds.getSport())
							.and("away.commonName").is(odds.getAway().getCommonName())
							.and("home.commonName").is(odds.getHome().getCommonName())
							),Odds.class);
		}
	}

	public List<Odds> getNextGame() {
    	return mongoTemplate.find(
    			new Query(Criteria.where("gameDateTime").gt(new Date()))
    			.with(Sort.by(Sort.Direction.ASC, "gameDateTime" ))
    			.limit(1),
    			Odds.class);
	}

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

}
