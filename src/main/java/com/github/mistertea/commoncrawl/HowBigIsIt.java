package com.github.mistertea.commoncrawl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.FilterFn;
import org.apache.crunch.PTable;
import org.apache.crunch.Pair;
import org.apache.crunch.Pipeline;
import org.apache.crunch.Target.WriteMode;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.io.From;
import org.apache.crunch.io.To;
import org.apache.crunch.lib.PTables;
import org.apache.crunch.types.writable.Writables;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class HowBigIsIt {
	public static class GeneratePotentialSizes<Input> extends
			DoFn<Input, Pair<String, Pair<String, Double>>> {
		private transient Analyzer analyzer;
		private Map<String, Double> unitSuffixToMultiplier = new HashMap<>();
		private Map<String, Double> unitNameToMultiplier = new HashMap<>();

		public GeneratePotentialSizes() {
			unitSuffixToMultiplier.put("cm", 1.0);
			unitSuffixToMultiplier.put("in", 2.54);
			unitSuffixToMultiplier.put("in.", 2.54);
			unitSuffixToMultiplier.put("\"", 2.54);
			unitSuffixToMultiplier.put("ft", 30.48);
			unitSuffixToMultiplier.put("ft.", 30.48);
			unitSuffixToMultiplier.put("\'", 30.48);
			unitSuffixToMultiplier.put("m", 1000.0);
			unitSuffixToMultiplier.put("mi", 160934.0);

			unitNameToMultiplier.put("centimeter", 1.0);
			unitNameToMultiplier.put("centimeters", 1.0);
			unitNameToMultiplier.put("inch", 2.54);
			unitNameToMultiplier.put("inches", 2.54);
			unitNameToMultiplier.put("in.", 2.54);
			unitNameToMultiplier.put("feet", 30.48);
			unitNameToMultiplier.put("foot", 30.48);
			unitNameToMultiplier.put("ft.", 30.48);
			unitNameToMultiplier.put("meter", 1000.0);
			unitNameToMultiplier.put("meters", 1000.0);
			unitNameToMultiplier.put("m", 1000.0);
			unitNameToMultiplier.put("miles", 160934.0);
			unitNameToMultiplier.put("mile", 160934.0);
			unitNameToMultiplier.put("mi", 160934.0);

			unitNameToMultiplier.putAll(unitSuffixToMultiplier);
		}

		@Override
		public void initialize() {
			analyzer = new StandardAnalyzer(Version.LUCENE_45);
		}

		@Override
		public void process(Input input,
				Emitter<Pair<String, Pair<String, Double>>> emitter) {
			try {
				TokenStream ts = analyzer.tokenStream("text", input.toString()
						.toLowerCase().replaceAll("_", " "));
				ts.addAttribute(CharTermAttribute.class);
				ts.reset();
				Set<String> pageWords = new HashSet<String>();
				List<String> pageWordList = new ArrayList<String>();
				while (ts.incrementToken()) {
					String s = ts.getAttribute(CharTermAttribute.class)
							.toString();
					if (s.length() > 100) {
						continue;
					}
					boolean skip = false;
					for (int a = 0; a < s.length(); a++) {
						if (Character.UnicodeBlock.of(s.charAt(a)) != Character.UnicodeBlock.BASIC_LATIN) {
							skip = true;
							break;
						}
					}
					if (skip) {
						continue;
					}
					if (s.length() < 100 && s.length() >= 3
							&& !s.matches(".*\\d.*")) {
						pageWords.add(s);
					}
					pageWordList.add(s);
				}
				ts.close();
				for (int a = 0; a < pageWordList.size() - 1; a++) {
					String currentWord = pageWordList.get(a);
					String nextWord = pageWordList.get(a + 1);
					if (NumberUtils.isNumber(currentWord)) {
						if (a + 1 >= pageWordList.size() - 1) {
							continue;
						}
						String twoWordsAhead = pageWordList.get(a + 2);
						String dimension = null;
						if (twoWordsAhead.equalsIgnoreCase("long")) {
							dimension = "length";
						}
						if (dimension == null) {
							continue;
						}

						// We have a number, see if we can tell the units from
						// the following word
						if (unitNameToMultiplier.containsKey(nextWord)) {
							try {
								double value = NumberUtils
										.createDouble(currentWord)
										* unitNameToMultiplier.get(nextWord);
								for (String s : pageWords) {
									emitter.emit(new Pair<String, Pair<String, Double>>(
											s, new Pair<String, Double>(
													dimension, value)));
								}
							} catch (NumberFormatException nfe) {

							}
						}
					} else {
						// We might have a number with the units appended
						String dimension = null;
						if (nextWord.equalsIgnoreCase("long")) {
							dimension = "length";
						}
						if (dimension == null) {
							continue;
						}
						for (Entry<String, Double> entry : unitSuffixToMultiplier
								.entrySet()) {
							if (currentWord.endsWith(entry.getKey())) {
								String wordWithoutSuffix = currentWord
										.substring(0, currentWord.length()
												- entry.getKey().length());
								if (NumberUtils.isNumber(wordWithoutSuffix)) {
									try {
										double value = NumberUtils
												.createDouble(wordWithoutSuffix)
												* entry.getValue();
										for (String s : pageWords) {
											emitter.emit(new Pair<String, Pair<String, Double>>(
													s,
													new Pair<String, Double>(
															dimension, value)));
										}
									} catch (NumberFormatException nfe) {

									}
								}
							}
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class AggregateSizes
			extends
			DoFn<Pair<String, Collection<Pair<String, Double>>>, Pair<String, Pair<String, Double>>> {

		@Override
		public void process(
				Pair<String, Collection<Pair<String, Double>>> input,
				Emitter<Pair<String, Pair<String, Double>>> emitter) {
			// For now just take the average
			double sum = 0.0;
			int count = input.second().size();
			List<Double> values = new ArrayList<Double>();
			// TODO: Fix when we add more dimensions
			String dimension = null;
			for (Pair<String, Double> pair : input.second()) {
				values.add(pair.second());
				dimension = pair.first();
			}
			Collections.sort(values);
			emitter.emit(new Pair<String, Pair<String, Double>>(input.first(),
					new Pair<String, Double>(dimension, values.get(values
							.size() / 2))));
		}

	}

	public static class RemoveInfrequentFilter<T> extends
			FilterFn<Pair<T, Long>> {
		private int threshold;

		public RemoveInfrequentFilter(int threshold) {
			this.threshold = threshold;
		}

		@Override
		public boolean accept(Pair<T, Long> wordFrequencyPair) {
			return wordFrequencyPair.second() >= threshold;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Pipeline pipeline = new MRPipeline(HowBigIsIt.class);
		PTable<Text, Text> urlInputMap = pipeline.read(From.sequenceFile(
				"/user/jgauci/CommonCrawl/textData-*", Text.class, Text.class));
		PTable<String, Pair<String, Double>> wordDimensionValueTuples = PTables
				.asPTable(
						urlInputMap
								.values()
								.parallelDo(
										"Tokenize",
										new GeneratePotentialSizes<Text>(),
										Writables.tableOf(Writables.strings(),
												Writables.tableOf(
														Writables.strings(),
														Writables.doubles())))
								.count()
								.filter("RemoveInfrequent",
										new RemoveInfrequentFilter<Pair<String, Pair<String, Double>>>(
												50)).keys())
				.collectValues()
				.parallelDo(
						new AggregateSizes(),
						Writables.tableOf(Writables.strings(), Writables
								.tableOf(Writables.strings(),
										Writables.doubles())));
		wordDimensionValueTuples.write(
				To.textFile("/user/jgauci/CommonCrawl/Sizes.txt"),
				WriteMode.OVERWRITE);
		Map<String, Pair<String, Double>> wordDimensionValueMap = new TreeMap<>(
				wordDimensionValueTuples.materializeToMap());
		System.out
				.println("NUBMER OF ENTRIES: " + wordDimensionValueMap.size());
		for (Entry<String, Pair<String, Double>> entry : wordDimensionValueMap
				.entrySet()) {
			String term = entry.getKey();
			// right now only length is supported
			System.out.println(term + " : " + entry.getValue().second());
		}
		pipeline.done();
	}
}
