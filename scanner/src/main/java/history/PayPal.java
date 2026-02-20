package history;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClients;

import scanner.scanner.model.history.PayPalRecord;
import scanner.scanner.model.history.paypal.PayPal_Events;
import scanner.scanner.model.history.paypal.PayPal_FtsSearchInfo;
import scanner.scanner.model.history.paypal.PayPal_Transactions;
import scanner.scanner.repo.WagerRepo;
import scanner.scanner.service.WagerService;

public class PayPal {

	WagerService wagerService;

	
	private void processJsonFile(String file) {

        Gson gson = (new GsonBuilder()).create();
        
        List<String> lines = new ArrayList<>();

        try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = reader.readLine()) != null) {
			    lines.add(line);
			}
			reader.close();
		} catch(Exception e) {
			System.out.println("Exception reading json file: " + e.getMessage());
		}

        List<PayPalRecord> paypal = new ArrayList<>();

        for(String line : lines) {
			
            PayPal_Events rtn = gson.fromJson(line, PayPal_Events.class);
            
            if(rtn != null) {
            	for(PayPal_Transactions trans : rtn.getData().getData().getActivity().getTransactions()) {
            	PayPal_FtsSearchInfo info = trans.getFtsSearchInfo();
            		PayPalRecord record = new PayPalRecord();
            		String[] d = info.getDate().getWithFullYear().split("/");
            		String dateField = String.format("%02d/%02d/%04d", 
            				Integer.parseInt(d[0]), 
            				Integer.parseInt(d[1]), 
            				Integer.parseInt(d[2])); 
            		
            		record.setId(info.getId());
            		record.setType(info.getDisplayType());
            		record.setDate(dateField);
            		Double amt = Double.parseDouble(info.getDisplayAmount().replace("$", ""));

            		switch(info.getDisplayType()) {
            			case "Payment":                    record.setAmount(-amt); break;
            			case "Payment Received":           record.setAmount( amt); break;
            			case "Transfer to Bank":           record.setAmount(-amt); break;
            			case "Instant transfer to Bank":   record.setAmount(-amt); break;
            			case "Transfer from Bank":         record.setAmount( amt); break;
            			case "Transfer":                   record.setAmount( amt); break;
            			default: System.out.println("New type: " + info.getDisplayType());
            		}

            		record.setSite(info.getCounterparty());

            		paypal.add(record);
            	}
            }
		}
	
//        System.out.println("Number of paypal records: " + paypal.size());
        try {
            BufferedWriter writer = 
    				new BufferedWriter(new FileWriter(System.getProperty("user.home") + "/paypal_2025.csv"));

			// Write the header
			writer.write("ID,Date,Source,Type,Amount\n");
			System.out.println("ID,Date,Source,Type,Amount\n");

	        for(PayPalRecord ppr : paypal) {
	        	writer.write(ppr.getId() + "," + ppr.getDate() + "," + 
	                         ppr.getSite().replace(",", " ") + "," + 
	        			     ppr.getType() + "," + ppr.getAmount() + "\n");
	        	System.out.println(ppr.getId() + "," + ppr.getDate() + "," + 
                        ppr.getSite().replace(",", " ") + "," + 
       			     ppr.getType() + "," + ppr.getAmount() + "\n");
	        }
	        writer.close();
	        
		} catch(Exception e) {
			System.out.println("Error writing out paypal: " + e.getMessage());
		}

	}

	
	public static void main(String[] args) {

		PayPal pp = new PayPal();
		
		ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/scanner");
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
	          .applyConnectionString(connectionString)
	          .build();

	    MongoTemplate mt = 	new MongoTemplate(MongoClients.create(mongoClientSettings), "scanner");

		WagerRepo wr = new WagerRepo();
		wr.setMongoTemplate(mt);
		WagerService ws = new WagerService();
		ws.setWagerRepo(wr);
		pp.wagerService = ws;
		
		List<String> files = List.of(
				"/home/pat/2025/paypal/paypal_2025.json"
		);

		
		for(String file : files) {
			pp.processJsonFile(file);
		}

	}

}
