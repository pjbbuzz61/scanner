package scanner.scanner.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import scanner.scanner.model.Update;
import scanner.scanner.util.Sport;
import scanner.scanner.util.Sportsbook;

@Repository
public class UpdateRepo {

    @Autowired
    protected MongoTemplate mongoTemplate;
    
    public void insert(Update update) {
    	List<Update> list = find(update.getType(), update.getBook(), update.getSport(), update.getTeamName(), update.getPlayer());
    	if((list == null) ||(list.size() == 0)) {
        	mongoTemplate.insert(update);
    	}
    }

    public void remove(Update update) {
    	mongoTemplate.remove(new Query().addCriteria(Criteria.where("id").is(update.getId())), Update.class);
    }

    public List<Update> find(String type, Sportsbook book, Sport sport, String teamName, String pitcher) {
    	Query q = new Query();
    	q.addCriteria(Criteria.where("type").is(type));
    	q.addCriteria(Criteria.where("book").is(book));
    	q.addCriteria(Criteria.where("sport").is(sport));
    	q.addCriteria(Criteria.where("teamName").is(teamName));
    	if(pitcher != null) {
        	q.addCriteria(Criteria.where("pitcher").is(pitcher));
    	}
    	return mongoTemplate.find(q, Update.class);

    }

    public List<Update> findAll() {
    	Query q = new Query();
    	return mongoTemplate.find(q, Update.class);

    }

    public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Update> find(Update update) {
		return mongoTemplate.find(new Query().addCriteria(
				Criteria.where("sport").is(update.getSport())
				.and("type").is(update.getType())
				.and("book").is(update.getBook())
				.and("teamName").is(update.getTeamName())
				.and("player").is(update.getPlayer())
				), Update.class);
	}

	public Long getCount() {
		return mongoTemplate.count(new Query(), Update.class);
	}
}
