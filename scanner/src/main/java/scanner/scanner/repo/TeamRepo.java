package scanner.scanner.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import scanner.scanner.model.Team;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

@Repository
public class TeamRepo {

    @Autowired
    private MongoTemplate mongoTemplate;
    
    public void insert(Team team) {
    	mongoTemplate.insert(team);
    }

    public List<Team> find(Sportsbook book, Sport sport, String nameSbSpecific) {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("book").is(book));
    	q.addCriteria(Criteria.where("sport").is(sport));
    	q.addCriteria(Criteria.where("nameSbSpecific").is(nameSbSpecific));
    	return mongoTemplate.find(q, Team.class);
    }

    public Team getTeam(Sportsbook book, Sport sport, String commonName) {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("book").is(book));
    	q.addCriteria(Criteria.where("sport").is(sport));
    	q.addCriteria(Criteria.where("commonName").is(commonName));
    	return mongoTemplate.findOne(q, Team.class);
    }

    public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Team> findAllForSport(Sport sport) {
		return mongoTemplate.find(new Query().addCriteria(Criteria.where("sport").is(sport)), Team.class);
	}
	
}
