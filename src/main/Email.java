package main;

import java.util.HashMap;
import java.util.Map;

public class Email {
	
	private final Classification classification;
	private final String emailFilePath;
	private final String originalEmailString;
	private Classification predictedClassification; // should be null for the test documents
	
	private Map<String, Integer> wordMap;
	
	public Email(Classification classification, String emailFilePath, String emailString) {
		this.classification = classification;
		this.emailFilePath = emailFilePath;
		this.originalEmailString = emailString;
		calculateWordMap(Training.N_GRAM);
	}
	
	private void calculateWordMap(int n) {
		// gets words, excludes punctuation & numbers,
		String[] words = this.originalEmailString.split("[^a-zA-Z]+");
		wordMap = new HashMap<String, Integer>();
		for(int i = 0; i < words.length - n + 1; i++) {
			String phrase = "";
			for(int j = 0; j < n; j++) {
				if(j == n - 1) {
					phrase += words[i + j];
				} else {
					phrase += words[i + j] + " ";
				}
			}
			
			if(!wordMap.containsKey(phrase)) {
				wordMap.put(phrase, 1);
			} else {
				wordMap.put(phrase, wordMap.get(phrase) + 1);
			}
		}
	}
	
	public Map<String, Integer> getWordMap() {
		return wordMap;
	}
	
	public String getEmailFilePath() {
		return emailFilePath;
	}
	
	public String getOriginalEmailString() {
		return originalEmailString;
	}
	
	public Classification getClassification() {
		return classification;
	}
	
	public void setPredictedClassification(Classification c) {
		this.predictedClassification = c;
	}
	
	public Classification getPredictedClassification() {
		return predictedClassification;
	}
}
