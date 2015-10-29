package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FileIOHelper {

	
	// todo: use stringbuffer to make reading faster
	public static List<Email> getEmailsFromList(String emailFilesList, Classification classification, int ngram) throws IOException {
		InputStream inputStream = FileIOHelper.class.getResourceAsStream(emailFilesList);
		BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
		
		String emailFilePath = "";
		List<Email> emails = new ArrayList<>();
		
		while ((emailFilePath = br.readLine()) != null) {
			emailFilePath = "emails/" + emailFilePath;
			BufferedReader br2 = new BufferedReader(new FileReader(new File(emailFilePath)));
			String emailLine = "";
			String emailText = "";
			while ((emailLine = br2.readLine()) != null) {
				emailText += emailLine;
			}
			Email email = new Email(classification, emailFilePath, emailText);
			emails.add(email);
		}
		return emails;
	}
}
