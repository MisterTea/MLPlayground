package com.github.mistertea.kaggle.stackexchange;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections15.buffer.CircularFifoBuffer;
import org.apache.commons.math.util.MathUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.sgd.L1;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;
import org.apache.mahout.vectorizer.encoders.Dictionary;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;

public class TrainStackExchange {
	private static final int FEATURES = 10000;
	private static Map<String, BetaClassifier> stopWordProbabilities = new HashMap<>();

	public static class BetaClassifier {
		public CircularFifoBuffer<Boolean> pastTrials = new CircularFifoBuffer<>(
				1000);

		public void addTrial(boolean result) {
			pastTrials.add(result);
		}

		public double probabilityOfTrue() {
			return probabilityOfTrue(0.25);
		}

		public double probabilityOfTrue(double margin) {
			int numTrue = 0;
			for (boolean b : pastTrials) {
				if (b) {
					numTrue++;
				}
			}
			int numFalse = pastTrials.size() - numTrue;
			double factor = MathUtils.factorialDouble(pastTrials.size() + 1);
			factor /= (MathUtils.factorialDouble(numTrue) * MathUtils
					.factorialDouble(numFalse));

			double betaIntegral = 0;
			double marginFloor = (1.0 - margin);
			// We need to integrate r^TRUE * (1-r)^FALSE
			for (int a=0;a<=1000;a++) {
				double r = marginFloor + (((double)margin)*a)/1000.0;
				double y = Math.pow(r, numTrue) * Math.pow(1.0 - r, numFalse);
				if (a!=0 && a!=1000) {
					betaIntegral += 2 * y;
				} else {
					betaIntegral += y;
				}
			}
			betaIntegral *= (margin - 0.0) / (2.0 * 1000.0);

			return factor * betaIntegral;
		}
	}

	public static void main(String[] args) throws IOException {
		/*
		BetaClassifier bc = new BetaClassifier();
		for (int a = 0; a < 150; a++) {
			System.out.println(bc.probabilityOfTrue());
			if (a%20==0) bc.addTrial(false);
			bc.addTrial(true);
		}

		boolean b = false;

		if (!b)
			return;
			*/

		File base = new File(args[0]);

		Map<String, Set<Integer>> traceDictionary = new TreeMap<String, Set<Integer>>();
		FeatureVectorEncoder encoder = new StaticWordValueEncoder("body");
		encoder.setProbes(2);
		encoder.setTraceDictionary(traceDictionary);
		FeatureVectorEncoder bias = new ConstantValueEncoder("Intercept");
		bias.setTraceDictionary(traceDictionary);
		FeatureVectorEncoder lines = new ConstantValueEncoder("Lines");
		lines.setTraceDictionary(traceDictionary);
		FeatureVectorEncoder logLines = new ConstantValueEncoder("LogLines");
		logLines.setTraceDictionary(traceDictionary);
		Dictionary newsGroups = new Dictionary();

		OnlineLogisticRegression learningAlgorithm = new OnlineLogisticRegression(
				20, FEATURES, new L1()).alpha(1).stepOffset(1000)
				.decayExponent(0.9).lambda(3.0e-5).learningRate(20);

		List<File> files = new ArrayList<File>();
		for (File newsgroup : base.listFiles()) {
			newsGroups.intern(newsgroup.getName());
			files.addAll(Arrays.asList(newsgroup.listFiles()));
		}

		Collections.shuffle(files);
		System.out.printf("%d training files\n", files.size());

		for (int a = 0; a < 100; a++) {
			double averageLL = 0.0;
			double averageCorrect = 0.0;
			double averageLineCount = 0.0;
			int k = 0;
			double step = 0.0;
			int[] bumps = new int[] { 1, 2, 5 };
			double lineCount = 0;

			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_45);

			for (File file : files) {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				String ng = file.getParentFile().getName();
				int actual = newsGroups.intern(ng);
				Multiset<String> words = ConcurrentHashMultiset.create();

				String line = reader.readLine();
				while (line != null && line.length() > 0) {
					if (line.startsWith("Lines:")) {
						String count = line.split(":")[1];
						try {
							lineCount = Integer.parseInt(count);
							averageLineCount += (lineCount - averageLineCount)
									/ Math.min(k + 1, 1000);
						} catch (NumberFormatException e) {
							lineCount = averageLineCount;
						}
					}
					boolean countHeader = (line.startsWith("From:")
							|| line.startsWith("Subject:")
							|| line.startsWith("Keywords:") || line
							.startsWith("Summary:"));
					do {
						StringReader in = new StringReader(line);
						if (countHeader) {
							countWords(analyzer, words, in);
						}
						line = reader.readLine();
					} while (line.startsWith(" "));
				}
				countWords(analyzer, words, reader);
				
				for (String s : words) {
					BetaClassifier probabiltyClassifier = stopWordProbabilities.get(s);
					if (probabiltyClassifier == null) {
						stopWordProbabilities.put(s, new BetaClassifier());
					}
				}
				
				for (Entry<String, BetaClassifier> entry : stopWordProbabilities.entrySet()) {
					BetaClassifier classifier = entry.getValue();
					classifier.addTrial(words.contains(entry.getKey()));
					if (classifier.probabilityOfTrue() > 0.95) {
						//System.out.println("STOP WORD: " + entry.getKey() + " " + words.count(entry.getKey()));
						words.remove(entry.getKey(), 1000000);
					}
				}

				Vector v = new RandomAccessSparseVector(FEATURES);
				bias.addToVector((String) null, 1, v);
				lines.addToVector((String) null, lineCount / 30, v);
				logLines.addToVector((String) null, Math.log(lineCount + 1), v);
				for (String word : words.elementSet()) {
					encoder.addToVector(word, Math.log(1 + words.count(word)),
							v);
				}

				double mu = Math.min(k + 1, 200);
				double ll = learningAlgorithm.logLikelihood(actual, v);
				averageLL = averageLL + (ll - averageLL) / mu;

				Vector p = new DenseVector(20);
				learningAlgorithm.classifyFull(p, v);
				int estimated = p.maxValueIndex();

				int correct = (estimated == actual ? 1 : 0);
				averageCorrect = averageCorrect + (correct - averageCorrect)
						/ mu;

				learningAlgorithm.train(actual, v);
				k++;
				int bump = bumps[(int) Math.floor(step) % bumps.length];
				int scale = (int) Math.pow(10, Math.floor(step / bumps.length));
				if (k % (bump * scale) == 0) {
					step += 0.25;
					System.out.printf("%10d %10.3f %10.3f %10.2f %s %s\n", k,
							ll, averageLL, averageCorrect * 100, ng, newsGroups
									.values().get(estimated));
				}
				learningAlgorithm.close();

				reader.close();
			}
		}

	}

	private static void countWords(Analyzer analyzer, Collection<String> words,
			Reader in) throws IOException {
		TokenStream ts = analyzer.tokenStream("text", in);
		ts.addAttribute(CharTermAttribute.class);
		ts.reset();
		String lastLast = null;
		String last = null;
		while (ts.incrementToken()) {
			String s = ts.getAttribute(CharTermAttribute.class).toString();
			words.add(s);
			if (last != null) {
				// words.add(last + " " + s);
				if (lastLast != null) {
					// words.add(lastLast + " " + last + " " + s);
				}
			}
			lastLast = last;
			last = s;
		}
		ts.close();
		/* overallCounts.addAll(words); */
	}
}