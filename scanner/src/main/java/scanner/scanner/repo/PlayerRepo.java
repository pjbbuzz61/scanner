package scanner.scanner.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import scanner.scanner.model.Player;
import scanner.scanner.model.Team;

@Repository
public class PlayerRepo {

    @Autowired
    private MongoTemplate mongoTemplate;
    
    public void insert(Player pitcher) {
    	mongoTemplate.insert(pitcher);
    }

    public List<Player> find(Team team, String nameSbSpecific) {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("team.id").is(team.getId()));
    	q.addCriteria(Criteria.where("nameSbSpecific").is(nameSbSpecific));
    	return mongoTemplate.find(q, Player.class);
    }

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Player> findAll() {
		return mongoTemplate.findAll(Player.class);
	}

	public List<Player> findAllForTeam(String teamName) {
		return mongoTemplate.find(new Query().addCriteria(Criteria.where("team.commonName").is(teamName)), Player.class);
	}

	public Player getExistingPlayer(Team team, String commonName) {
    	Query q = new Query();
//    	q.addCriteria(Criteria.where("team.id").is(team.getId()));
    	q.addCriteria(Criteria.where("team.commonName").is(team.getCommonName()));
    	q.addCriteria(Criteria.where("commonName").is(commonName));
    	return mongoTemplate.findOne(q, Player.class);
	}

}
