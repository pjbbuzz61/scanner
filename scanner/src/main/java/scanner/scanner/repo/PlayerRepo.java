package scanner.scanner.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import scanner.scanner.model.Player;
import scanner.scanner.model.Team;
import scanner.scanner.util.Sportsbook;

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

    public List<Player> find(Team team) {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("team.commonName").is(team.getCommonName()));
    	q.addCriteria(Criteria.where("team.sport").is(team.getSport()));
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

	public List<Player> findAllForBook(Sportsbook book) {
		return mongoTemplate.find(new Query().addCriteria(Criteria.where("team.book").is(book)), Player.class);
	}

	public Player getExistingPlayer(Team team, String commonName) {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("team.commonName").is(team.getCommonName()));
    	q.addCriteria(Criteria.where("commonName").is(commonName));
    	return mongoTemplate.findOne(q, Player.class);
	}

	public void updateTeam(Player player, Team team) {

    	Query q = new Query();
    	q.addCriteria(Criteria.where("commonName").is(player.getCommonName()));
    	q.addCriteria(Criteria.where("team.book").is(player.getTeam().getBook()));
    	q.addCriteria(Criteria.where("team.commonName").is(player.getTeam().getCommonName()));
    	q.addCriteria(Criteria.where("team.sport").is(player.getTeam().getSport()));
		Update update = new Update();
		update.set("team", team);
		mongoTemplate.updateMulti(q, update, Player.class);
	}

	public void removeExistingPlayers(Sportsbook book) {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("team.book").is(book));
    	mongoTemplate.remove(q, Player.class);
		
	}

	public void removePlayer(Player p) {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("id").is(p.getId()));
    	mongoTemplate.remove(q, Player.class);
	}

	public List<Player> getRefPlayerList() {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("team.book").is(Sportsbook.ESPN_MLB_REF));
    	return mongoTemplate.find(q, Player.class);
	}

}
