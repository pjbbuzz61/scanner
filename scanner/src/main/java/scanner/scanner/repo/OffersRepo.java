package scanner.scanner.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import scanner.scanner.model.Offer;
import scanner.scanner.util.Action;
import scanner.scanner.util.Period;

@Repository
public class OffersRepo {

    @Autowired
    private MongoTemplate mongoTemplate;

    public Offer save(Offer offer) {
    	offer.setId(String.format("%d", offer.uniqueKey()));
		try {
	    	mongoTemplate.save(offer);
		} catch(Exception e) {
			System.out.println("Exception persisting odds: " + e.getMessage());
		}

    	return offer;
    }

    public List<Offer> getAll() {
       return mongoTemplate.findAll(Offer.class);
    }

	public void removeAll() {
		mongoTemplate.remove(new Query(), Offer.class);
	}

	// get all distinct gameId values (distinct games)
	public List<Integer> distinctGames(String collectionName) {
		return mongoTemplate.findDistinct(new Query(), "gameId", collectionName, Offer.class, Integer.class);
	}
	
	// get all distinct points based on a gameId, period and an offer type
	public List<Double> distinctPoints(Integer gameId, Period period, Action type, String collectionName) {
    	Query query = new Query();
    	query.addCriteria(Criteria.where("gameId").is(gameId));
    	query.addCriteria(Criteria.where("type").is(type));
    	query.addCriteria(Criteria.where("period").is(period));
		return mongoTemplate.findDistinct(query, "points", collectionName, Offer.class, Double.class);
	}
	
	public List<Offer> getOffers(Integer gameId, Period period, Action type, double points, String collectionName) {
		Query query = new Query();
    	query.addCriteria(Criteria.where("gameId").is(gameId));
    	query.addCriteria(Criteria.where("type").is(type));
    	query.addCriteria(Criteria.where("period").is(period));
    	query.addCriteria(Criteria.where("points").is(points));
    	return mongoTemplate.find(
    			query.with(Sort.by(Sort.Direction.DESC, "decimalOdds")),
    			Offer.class,
    			collectionName);
	}

	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	public List<Offer> getOffers(Action type, String collectionName) {

		Query query = new Query();
    	query.addCriteria(Criteria.where("type").is(type));
    	return mongoTemplate.find(
    			query.with(Sort.by(Sort.Direction.DESC, "decimalOdds")),
    			Offer.class,
    			collectionName);
	}


}
