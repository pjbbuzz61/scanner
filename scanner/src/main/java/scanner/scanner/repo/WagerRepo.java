package scanner.scanner.repo;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import scanner.scanner.model.history.Wager;

@Repository
public class WagerRepo {

    @Autowired
    protected MongoTemplate mongoTemplate;

    public Wager insert(Wager wager) {
    	insert(wager, "wagers");
    	return wager;
    }

    public Wager insert(Wager wager, String collection) {
    	mongoTemplate.save(wager, collection);
    	return wager;
    }

	public List<Wager> find(String betNumber) {
		return mongoTemplate.find(
				new Query().addCriteria(
						Criteria.where("betNumber").is(betNumber)), 
				Wager.class);
	}

	public void remove(String betNumber) {
		mongoTemplate.remove(
				new Query().addCriteria(
						Criteria.where("betNumber").is(betNumber)), 
				Wager.class);
	}
	
	public List<Wager> findAll() {
		return mongoTemplate.findAll(Wager.class);
	}

	public void insert(List<Wager> wagers) {
		mongoTemplate.insertAll(wagers);
	}

	public void setMongoTemplate(MongoTemplate mt) {
		mongoTemplate = mt;
	}

	public List<Wager> find(Date start, Date stop) {
		
		Criteria criteria = Criteria.where("betTimestamp") 
                .gte(start)  
                .lte(stop);  

		Query query = new Query(criteria);

		return mongoTemplate.find(query,Wager.class);
	}
}
