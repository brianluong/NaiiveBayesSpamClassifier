package main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.Classifier.Results;

public class Training {
	
	private static final int EVAL_EMAILS_COUNT = 20;
	
	public static final int LAPLACE_SOOTHING_CONSTANT = 25; // vary between 1 - 50
	public static final int K = 3; // vary this as well
	public static final int N_GRAM = 1; // vary this between 1 - 5
	
	private List<Email> spamEmails;
	private List<Email> hamEmails;
	private Set<String> features;
	
	private double priorSpam;
	private double priorHam;
	
	private List<Email> evalSpamEmails;
	private List<Email> evalHamEmails;
	private int evaluatedK;
	private int evaluatedLaplace;

	public Training(String spamTrainingEmailsFilePath, String hamTrainingEmailsFilePath) throws IOException {
		spamEmails = FileIOHelper.getEmailsFromList(spamTrainingEmailsFilePath, Classification.SPAM, N_GRAM);
		hamEmails = FileIOHelper.getEmailsFromList(hamTrainingEmailsFilePath, Classification.HAM, N_GRAM);
	}

	/**
	 * @param k, to generate lexicon consisting of the set of unique words occurring more than k times in the training document collection
	 */
	public Map<String, Parameter> trainWithoutEvaluation() {
		computeFeatures(K, spamEmails, hamEmails);
		computePriors();
		return estimateParameters(spamEmails, hamEmails, LAPLACE_SOOTHING_CONSTANT);
	}
	
	public Map<String, Parameter> trainWithEvaluation() {
		priorSpam = .5;
		priorHam = .5;
		evaluatedK = 0;
		evaluatedLaplace = 0;
		getEvaluationEmails();
		Map<String, Parameter> bestParameters = new HashMap<>();
		int bestCorrectlyClassified = 0;
		for (int k = 0; k <= 75; k+=5) {
			for (int laplace = 0; laplace < 75; laplace += 5) {
				List<Email> spamTrainingEmails = spamEmails.subList(EVAL_EMAILS_COUNT, spamEmails.size());
				List<Email> hamTrainingEmails = hamEmails.subList(EVAL_EMAILS_COUNT, hamEmails.size());
				
				computeFeatures(k, spamTrainingEmails, hamTrainingEmails);
				Map<String, Parameter> model = estimateParameters(spamTrainingEmails, hamTrainingEmails, laplace);
				
				Classifier classifier = new Classifier();
				classifier.addTrainer(this);
				classifier.addTestEmails(evalSpamEmails, evalHamEmails);
				classifier.setModel(model);
				
				Results results = classifier.classify();
				int correctlyClassified = results.computeCorrectlyPredicted(Classification.HAM) + 
						results.computeCorrectlyPredicted(Classification.SPAM);
				System.out.println("k:" + k + " laplace: " + laplace);
				
				System.out.println("spam correct: " + results.computeCorrectlyPredicted(Classification.SPAM)
						+ " ham correct: " + results.computeCorrectlyPredicted(Classification.HAM));
				if (correctlyClassified > bestCorrectlyClassified) {
					bestCorrectlyClassified = correctlyClassified;
					bestParameters = model; 
					evaluatedK = k;
					evaluatedLaplace = laplace;
				}
				System.out.println("----------------------------------");
			}
		}
		return bestParameters;
	}
	
	public int getEvaluatedK() {
		return evaluatedK;
	}
	
	public int getEvaluatedLaplace() {
		return evaluatedLaplace;
	}
	
	private void getEvaluationEmails() {
		evalSpamEmails = new ArrayList<>();
		evalHamEmails = new ArrayList<>();
		for (int i = 0; i < EVAL_EMAILS_COUNT; i++) {
			evalSpamEmails.add(spamEmails.get(i));
			evalHamEmails.add(hamEmails.get(i));
		}
	}
	
	// P(class) as the empirical frequencies of the
	// ￼classes in the training set (ie percentage of spam and ham documents)
	private void computePriors() {
		priorSpam = spamEmails.size() * 1.0 / (spamEmails.size() + hamEmails.size());
		priorHam = hamEmails.size() * 1.0 / (spamEmails.size() + hamEmails.size());
	}
	
	/**
	 * P(Word = w | class) = (# of times word w occurs in training examples from this class) / (Total # of words in
		￼training examples from this class).
	 * @return parameters
	 */
	private Map<String, Parameter> estimateParameters(List<Email> spamEmails, List<Email> hamEmails, int laPlaceSoothingConstant) {
		Map<String, Parameter> parameters = new HashMap<>();
		int totalWordsInSpam = computeTotalWords(spamEmails, laPlaceSoothingConstant);
		int totalWordsInHam = computeTotalWords(hamEmails, laPlaceSoothingConstant);
		for (String word : features) {
			double likelihoodSpam = computeOccurences(word, spamEmails, laPlaceSoothingConstant) * 1.0 / totalWordsInSpam;
			double likelihoodHam = computeOccurences(word, hamEmails, laPlaceSoothingConstant) * 1.0 / totalWordsInHam;
			Parameter parameter = new Parameter(word, likelihoodSpam, likelihoodHam);
			parameters.put(word, parameter);
		}
		return parameters;
	}
	
	//	Laplace smoothing is a very simple method that increases the observation count of every value ‘w’ by some
	//	constant m. This corresponds to adding m to the numerator above, and m*V to the denominator (where V
	//	is the number of words in your lexicon).
	private int computeOccurences(String word, List<Email> emails, int laplaceSoothingConstant) {
		int count = 0;
		for (Email email : emails) {
			count = email.getWordMap().get(word) != null ? count + email.getWordMap().get(word) : count;
		}
		return count + laplaceSoothingConstant;
	}
	
	private int computeTotalWords(List<Email> emails, int laplaceSoothingConstant) {
		int count = 0;
		for (Email email : emails) {
			Map<String, Integer> wordMap = email.getWordMap();
			for (String word : wordMap.keySet()) {
				count += wordMap.get(word);
			}
		}
		return count + (laplaceSoothingConstant * features.size());
	}
	
	private void computeFeatures(int k, List<Email> spamEmails, List<Email> hamEmails) {
		features = new HashSet<>();
		Map<String, Integer> wordCounts = computeTotalWordCount(spamEmails, hamEmails);
		for (String word : wordCounts.keySet()) {
			if (wordCounts.get(word) > k) {
				features.add(word);
			}
		}
	}
	
	private Map<String, Integer> computeTotalWordCount(List<Email> spamEmails, List<Email> hamEmails) {
		Map<String, Integer> wordCounts = new HashMap<>();
		computeWordCounts(spamEmails, wordCounts);
		computeWordCounts(hamEmails, wordCounts);
		return wordCounts;
	}

	private void computeWordCounts(List<Email> emailList, Map<String, Integer> totalWordsMap) {
		for (Email email : emailList) {
			Map<String, Integer> wordMap = email.getWordMap();
			for (String word : wordMap.keySet()) {
				if (totalWordsMap.containsKey(word)) {
					totalWordsMap.put(word, totalWordsMap.get(word) + wordMap.get(word));
				} else {
					totalWordsMap.put(word, wordMap.get(word));
				}
			}
		}
	}
	
	public double getPriorSpam() {
		return priorSpam;
	}
	
	public double getPriorHam() {
		return priorHam;
	}
	
	public List<Email> getSpamEmails() {
		return spamEmails;
	}
	
	public List<Email> getHamEmails() {
		return hamEmails;
	}
	
	public Set<String> getFeatures() {
		return features;
	}
	
	public class Parameter {
		String word;
		private double likelihoodHam;
		private double likelihoodSpam;
		
		public Parameter(String word, double likelihoodSpam, double likelihoodHam){
			this.word = word;
			this.likelihoodSpam = likelihoodSpam;
			this.likelihoodHam = likelihoodHam;
		}
		
		public double getLikelihoodHam() {
			return likelihoodHam;
		}
		
		public double getLikelihoodSpam() {
			return likelihoodSpam;
		}
		
	}
}
