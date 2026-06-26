package scanner.scanner.repo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import scanner.scanner.model.PlayMade;
import scanner.scanner.util.MLB_STAT;

@Repository
public class PlayMadeRepo {

    @Autowired
    private MongoTemplate mongoTemplate;
    
    public PlayMade save(PlayMade play) {
    	mongoTemplate.save(play);
    	return play;
    }

    public List<PlayMade> getAll() {
        return mongoTemplate.findAll(PlayMade.class);
     }

 	public void removeAll() {
 		mongoTemplate.remove(new Query(), PlayMade.class);
 	}

 	public List<PlayMade> find(int date, String player, MLB_STAT mlbStat) {
 		
 		return mongoTemplate.find(
 				new Query().addCriteria(
 						Criteria.where("julianDate").is(date)
 						.and("player").is(player)
 						.and("mlbStat").is(mlbStat)
 						), PlayMade.class);
 	}

 	public List<PlayMade> find(PlayMade pm) {
 		
 		return mongoTemplate.find(
 				new Query().addCriteria(
 						Criteria.where("julianDate").is(pm.getJulianDate())
 						.and("player").is(pm.getPlayer())
 						.and("mlbStat").is(pm.getMlbStat())
 						), PlayMade.class);
 	}

 	public void setMongoTemplate(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

}
