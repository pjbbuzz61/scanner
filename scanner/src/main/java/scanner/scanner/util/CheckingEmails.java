package scanner.scanner.util;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Store;

public class CheckingEmails {

	private static final Properties PROPERTIES = new Properties();
	private static final String USERNAME = "ddanoso2018@gmail.com";   
	private static final String PASSWORD = "fkjo neol egrk dqwt";  
	private static final String HOST = "smtp.gmail.com";

	static {
		PROPERTIES.put("mail.smtp.host", HOST);
		PROPERTIES.put("mail.smtp.port", "587");
		PROPERTIES.put("mail.smtp.auth", "true");
		PROPERTIES.put("mail.smtp.starttls.enable", "true");
	}


	public long getStopTime() throws MessagingException, IOException {
	
		long stopTime = -1L;

	       Folder emailFolder = null;
	       Store store = null;
	       try {
	           //create properties field
	           Properties properties = new Properties();

	           properties.put("mail.pop3.host", "pop.gmail.com");
	           properties.put("mail.pop3.port", Integer.toString(995));
	           properties.put("mail.pop3.starttls.enable", "true");

	           Session emailSession = Session.getDefaultInstance(properties);

	           //create the POP3 store object and connect with the pop server

	           store = emailSession.getStore("pop3s");

	           store.connect("pop.gmail.com", "ddanoso2018@gmail.com", "fkjo neol egrk dqwt");

	           //create the folder object and open it

	           emailFolder = store.getFolder("INBOX");
	           emailFolder.open(Folder.READ_ONLY);

	           // retrieve the messages from the folder in an array and print it
	           Message[] messages = emailFolder.getMessages();
	           System.out.println("Number of messages: " + messages.length);
	           int index = 1;
	           for (final Message msg : messages) {
	               final String subject = msg.getSubject();
	               if(subject.contains("Nrt2")) {
		               System.out.println(index + ". " + subject);
		               Object c = msg.getContent();
		               if(c instanceof String) {
		            	   SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"); 
		            	   sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		            	   try {
		            		   Date d = sdf.parse((String)c);
		            		   stopTime = d.getTime();
			            	   System.out.println("Stop date is " + d);
			            	   System.out.println("Calculated stop is " + new Date(stopTime));
			            	   break;
		            	   } catch (ParseException e) {
		            		   System.out.println("Parse error");
		            		   stopTime = 0L;
		            	   }
		            	   System.out.println("This is the content: " + c);
		               } else {
		            	   // Since I don't understand the content lets just force the process off
		            	   stopTime = 0L;
		               }
	               } else {
	            	   msg.setFlag(Flags.Flag.SEEN, false);
	               }
	               index++;

	           }
	       }
	       finally {
	           if (emailFolder != null) {
	               emailFolder.close(false);
	           }
	           if (store != null) {
	               store.close();
	           }
	       }

		
		return stopTime;
	}

	public static void main(String[] args) throws MessagingException, IOException {
		CheckingEmails cem = new CheckingEmails();
		cem.getStopTime();

   }
}
