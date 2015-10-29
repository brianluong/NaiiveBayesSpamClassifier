package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import main.Training.Parameter;

public class Classifier {

	private Training trainer;
	private Map<String, Parameter> model;
	private List<Email> testSpam;
	private List<Email> testHam;
	
	private static final String SPAM_TRAINING_EMAILS_FILEPATH = "../emailPaths/spamtraining.txt";
	private static final String HAM_TRAINING_EMAILS_FILEPATH = "../emailPaths/hamtraining.txt";
	private static final String SPAM_TESTING_EMAILS_FILEPATH = "../emailPaths/spamtesting.txt";
	private static final String HAM_TESTING_EMAILS_FILEPATH = "../emailPaths/hamtesting.txt";
	
	public static void main(String[] args) throws IOException {
		Training trainer = new Training(SPAM_TRAINING_EMAILS_FILEPATH, HAM_TRAINING_EMAILS_FILEPATH);
		Classifier classifier = new Classifier(SPAM_TESTING_EMAILS_FILEPATH, HAM_TESTING_EMAILS_FILEPATH, trainer);
		classifier.setModel(trainer.trainWithoutEvaluation());
		Results results = classifier.classify();
		
		System.out.println("The value K used in training is is " + Training.K);
		System.out.println("The Laplace smoothing constant using in training is " + Training.LAPLACE_SOOTHING_CONSTANT);
		System.out.println("The number of HAM emails that were classified correctly: " + results.computeCorrectlyPredicted(Classification.HAM));
		System.out.println("The number of SPAM emails that were classified correctly: " + results.computeCorrectlyPredicted(Classification.SPAM));
		System.out.println("The accuracy of classifying Ham is " + results.computeAccuracy(Classification.HAM) * 100 + "%");
		System.out.println("The accuracy of classifying Spam is " + results.computeAccuracy(Classification.SPAM) * 100 + "%");
		
		System.out.println("An example of HAM email that was classified correctly: " + 
					((results.getCorrectlyPredicted(Classification.HAM).size() > 0) 
					? results.getCorrectlyPredicted(Classification.HAM).get(0).getOriginalEmailString() : ""));
		
		System.out.println("An example of HAM email that was classified incorrectly: " + 
				((results.getIncorrectlyPredicted(Classification.HAM).size() > 0) 
				? results.getIncorrectlyPredicted(Classification.HAM).get(0).getOriginalEmailString() : ""));
		
		System.out.println("An example of SPAM email that was classified correctly: " + 
				((results.getCorrectlyPredicted(Classification.SPAM).size() > 0) 
				? results.getCorrectlyPredicted(Classification.SPAM).get(0).getOriginalEmailString() : ""));
	
		System.out.println("An example of SPAM email that was classified incorrectly: " + 
			((results.getIncorrectlyPredicted(Classification.SPAM).size() > 0) 
			? results.getIncorrectlyPredicted(Classification.SPAM).get(0).getOriginalEmailString() : ""));
		
//		System.out.println("Here are the features");
//		for (String word : classifier.getModel().keySet()) {
//			System.out.println("Word: " + word + "\n     likelihood in ham: " + classifier.getModel().get(word).getLikelihoodHam()
//					+ "\n     likelihood in spam: " + classifier.getModel().get(word).getLikelihoodSpam());
//		}
		
		trainer = new Training(SPAM_TRAINING_EMAILS_FILEPATH, HAM_TRAINING_EMAILS_FILEPATH);
		classifier = new Classifier(SPAM_TESTING_EMAILS_FILEPATH, HAM_TESTING_EMAILS_FILEPATH, trainer);
		classifier.setModel(trainer.trainWithEvaluation());
		
		results = classifier.classify();
		
		System.out.println("The value K used in training is is " + trainer.getEvaluatedK() + " " + trainer.getEvaluatedLaplace());
		System.out.println("The number of HAM emails that were classified correctly: " + results.computeCorrectlyPredicted(Classification.HAM));
		System.out.println("The number of SPAM emails that were classified correctly: " + results.computeCorrectlyPredicted(Classification.SPAM));
		System.out.println("The accuracy of classifying Ham is " + results.computeAccuracy(Classification.HAM) * 100 + "%");
		System.out.println("The accuracy of classifying Spam is " + results.computeAccuracy(Classification.SPAM) * 100 + "%");
		
		// Commit n-gram features.
	}
	
	public Classifier() {
		
	}
	
	/**
	 * @param spam and ham filepath 
	 * @throws IOException
	 */
	public Classifier(String spamTestEmailsPath, String hamTestEmailsPath, Training trainer) throws IOException {
		addTestEmails(spamTestEmailsPath, hamTestEmailsPath);
		this.trainer = trainer;
	}
	
	public void addTestEmails(String spamTestEmailsPath, String hamTestEmailsPath) throws IOException {
		testSpam = FileIOHelper.getEmailsFromList(spamTestEmailsPath, Classification.SPAM);
		testHam = FileIOHelper.getEmailsFromList(hamTestEmailsPath, Classification.HAM);
	}
	
	/**
	 * method is needed to use in Training.java for evaluation 
	 * @param spamTestEmails
	 * @param hamTestEmails
	 */
	public void addTestEmails(List<Email> spamTestEmails, List<Email> hamTestEmails) {
		testSpam = spamTestEmails;
		testHam = hamTestEmails;
	}
	
	public void addTrainer(Training trainer) {
		this.trainer = trainer;
	}
	
	
	public Results classify() {
		classifyEmails(testSpam);
		classifyEmails(testHam);
		return new Results();
	}

	private void classifyEmails(List<Email> emails) {
		for (Email email : emails) {
			Map<String, Integer> wordMap = email.getWordMap();
			// priors, to use MAP classification
			double probSpam = trainer.getPriorSpam();
			double probHam = trainer.getPriorHam();
			for (String word : wordMap.keySet()) {
				if (model.containsKey(word)) {
					// To avoid underflow, you should compute the log of the above quantity: 
					// log P(class) + log P(w1|class) + log P(w2|class) + ... + log P(w200 | class)
					probSpam += Math.log(model.get(word).getLikelihoodSpam());
					probHam += Math.log(model.get(word).getLikelihoodHam());
				}
			}
			email.setPredictedClassification(probSpam > probHam ? Classification.SPAM : Classification.HAM);
		}
	}
	
	public void setModel(Map<String, Parameter> model) {
		this.model = model;
	}
	
	public Map<String, Parameter> getModel() {
		return model;
	}
	
	/**
	 * 
	 * incorrectlyPredictedHam and incorrectlyPredictedSpam are hams that are supposed to be ham, 
	 * but got classified as spam, and vice versa
	 * 
	 * @author brianluong
	 *
	 */
	public class Results {
		List<Email> correctlyPredictedHam;
		List<Email> correctlyPredictedSpam;
		List<Email> incorrectlyPredictedHam; 
		List<Email> incorrectlyPredictedSpam;
		
		public double computeAccuracy(Classification classification) {
			return computeCorrectlyPredicted(classification) * 1.0 / (classification == Classification.SPAM  ? testSpam.size() : testHam.size());
		}
		
		/**
		 * Need to run this method before being able to get the lists of correctly and incorrectly predicted emails
		 * @param classification
		 * @return
		 */
		public int computeCorrectlyPredicted(Classification classification) {
			int count = 0;
			correctlyPredictedHam = new ArrayList<>();
			correctlyPredictedSpam = new ArrayList<>();
			incorrectlyPredictedHam = new ArrayList<>();
			incorrectlyPredictedSpam = new ArrayList<>();
			
			for (Email email : classification == Classification.SPAM ? testSpam : testHam) {
				if (email.getClassification() == email.getPredictedClassification()) {
					count++;
					if (classification == Classification.SPAM) {
						correctlyPredictedSpam.add(email);
					} else {
						correctlyPredictedHam.add(email);
					}
				} else {
					if (classification == Classification.SPAM) {
						incorrectlyPredictedSpam.add(email);
					} else {
						incorrectlyPredictedHam.add(email);
					}
				}
			}
			return count;
		}
		
		public List<Email> getCorrectlyPredicted(Classification classification) {
			return classification == Classification.SPAM ? correctlyPredictedSpam : correctlyPredictedHam;
		}
		
		public List<Email> getIncorrectlyPredicted(Classification classification) {
			return classification == Classification.SPAM ? incorrectlyPredictedSpam : incorrectlyPredictedHam;
		}
		
		public List<Email> getTestSpam() {
			return testSpam;
		}
		
		public List<Email> getTestHam() {
			return testHam;
		}
	}
}