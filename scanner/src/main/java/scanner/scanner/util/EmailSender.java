package scanner.scanner.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class EmailSender {

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

	public static void main(String args[]) {

		EmailSender es = new EmailSender();

		es.sendEmailWithAttachmentToSelf(
				"The player name : Baltimore Orioles : Runs",
				"Message Body", 
				null,
				false);

//		es.sendPlainTextEmail(
//				"Test Message", 
//				" ",
//				false);

	}

	public void sendPlainTextEmail(String subject, String message, boolean debug) {

		Authenticator authenticator = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(USERNAME, PASSWORD);
			}
		};

		Session session = Session.getInstance(PROPERTIES, authenticator);
		session.setDebug(debug);

		try {

			// create a message with headers
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress("ddanoso2018@gmail.com"));
			InternetAddress[] address = {new InternetAddress("4106939535@vtext.com")};
			msg.setRecipients(Message.RecipientType.TO, address);
			msg.setSubject(subject);
			msg.setSentDate(new Date());

			// create message body
			msg.setText(message);

			// send the message
			Transport.send(msg);

		} catch (MessagingException mex) {
			mex.printStackTrace();
			Exception ex = null;
			if ((ex = mex.getNextException()) != null) {
				ex.printStackTrace();
			}
		}
	}

	public void sendEmailWithAttachmentToSelf(String subject, String message, String file, boolean debug) {

		Authenticator authenticator = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(USERNAME, PASSWORD);
			}
		};

		Session session = Session.getInstance(PROPERTIES, authenticator);
		session.setDebug(debug);

		try {

			// create a message with headers
			MimeMessage msg = new MimeMessage(session);

			MimeBodyPart messageBodyPart = new MimeBodyPart(); 
			messageBodyPart.setText(message);
			
			MimeBodyPart attachmentPart = new MimeBodyPart();
			if(file != null) {
				attachmentPart.attachFile(new File(file));
			}

			MimeMultipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			if(file != null) {
				multipart.addBodyPart(attachmentPart);
			}
			
			msg.setFrom(new InternetAddress("ddanoso2018@gmail.com"));
			InternetAddress[] address = {new InternetAddress("ddanoso2018@gmail.com")};
			msg.setRecipients(Message.RecipientType.TO, address);
			msg.setSubject(subject);
			msg.setSentDate(new Date());

			// create message body
			msg.setContent(multipart);

			// send the message
			Transport.send(msg);

		} catch (MessagingException | IOException mex) {
			mex.printStackTrace();
		}
	}

}

