package com.github.mistertea.commoncrawl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.FilterFn;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PObject;
import org.apache.crunch.PTable;
import org.apache.crunch.Pair;
import org.apache.crunch.Pipeline;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.io.From;
import org.apache.crunch.types.writable.Writables;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import com.google.common.collect.ImmutableSet;

public class WordCoOccurance {
	public static class Tokenize<Input> extends DoFn<Input, String> {
		private transient Analyzer analyzer;

		@Override
		public void initialize() {
			analyzer = new SnowballAnalyzer(Version.LUCENE_45, "English");
		}

		@Override
		public void process(Input input, Emitter<String> emitter) {
			try {
				TokenStream ts = analyzer.tokenStream("text", input.toString()
						.toLowerCase().replaceAll("_", " "));
				ts.addAttribute(CharTermAttribute.class);
				ts.reset();
				Set<String> pageWords = new HashSet<String>();
				while (ts.incrementToken()) {
					String s = ts.getAttribute(CharTermAttribute.class)
							.toString();
					if (s.length() > 100 || s.length() < 3
							|| s.matches(".*\\d.*")) {
						continue;
					}
					boolean skip = false;
					for (int a = 0; a < s.length(); a++) {
						if (Character.UnicodeBlock.of(s.charAt(a)) != Character.UnicodeBlock.BASIC_LATIN) {
							skip = true;
							break;
						}
					}
					if (!skip) {
						pageWords.add(s);
					}
				}
				for (String s : pageWords) {
					emitter.emit(s);
				}
				ts.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class ComputeIdf extends
			MapFn<Pair<String, Long>, Pair<String, Double>> {
		private double numDocuments;

		public ComputeIdf(long numDocuments) {
			this.numDocuments = numDocuments;
		}

		@Override
		public Pair<String, Double> map(Pair<String, Long> input) {
			return new Pair<String, Double>(input.first(),
					Math.log(numDocuments / (1 + input.second())));
		}
	}

	public static class ComputeCoOccurance<Input> extends
			DoFn<Input, Pair<String, String>> {
		private transient Analyzer analyzer;
		private Set<Set<String>> sourceDictionary;
		private Set<String> targetDictionary;
		private Map<Set<String>, String> sourceDictionaryTokenLineMap;

		public ComputeCoOccurance(Set<Set<String>> sourceDictionary,
				Map<Set<String>, String> sourceDictionaryTokenLineMap,
				Set<String> targetDictionary) {
			this.sourceDictionary = sourceDictionary;
			this.sourceDictionaryTokenLineMap = sourceDictionaryTokenLineMap;
			this.targetDictionary = targetDictionary;
		}

		@Override
		public void initialize() {
			analyzer = new SnowballAnalyzer(Version.LUCENE_45, "English");
		}

		@Override
		public void process(Input input, Emitter<Pair<String, String>> emitter) {
			try {
				TokenStream ts = analyzer.tokenStream("text", input.toString()
						.toLowerCase().replaceAll("_", " "));
				ts.addAttribute(CharTermAttribute.class);
				ts.reset();
				Set<String> pageWords = new HashSet<String>();
				while (ts.incrementToken()) {
					String s = ts.getAttribute(CharTermAttribute.class)
							.toString();
					if (s.length() > 100 || s.length() < 3
							|| s.matches(".*\\d.*")) {
						continue;
					}
					boolean skip = false;
					for (int a = 0; a < s.length(); a++) {
						if (Character.UnicodeBlock.of(s.charAt(a)) != Character.UnicodeBlock.BASIC_LATIN) {
							skip = true;
							break;
						}
					}
					if (!skip) {
						pageWords.add(s);
					}
				}
				for (Set<String> s1 : sourceDictionary) {
					if (pageWords.containsAll(s1)) {
						for (String s2 : targetDictionary) {
							if (pageWords.contains(s2)) {
								emitter.emit(new Pair<String, String>(
										sourceDictionaryTokenLineMap.get(s1),
										s2));
							}
						}
					}
				}
				ts.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Set<String> sourceDictionaryLines = new HashSet<String>();
		URL fileURL = WordCoOccurance.class.getResource("/BuildingTypes.txt");
		File buildingFile = new File(fileURL.getPath());
		sourceDictionaryLines.addAll(FileUtils.readLines(buildingFile));
		Set<Set<String>> sourceDictionary = new HashSet<>();
		Map<Set<String>, String> sourceDictionaryLineTokenMap = new HashMap<>();
		for (String s : sourceDictionaryLines) {
			HashSet<String> tokens = new HashSet<String>(Arrays.asList(s
					.split("_")));
			sourceDictionary.add(tokens);
			sourceDictionaryLineTokenMap.put(tokens, s);
		}

		Pipeline pipeline = new MRPipeline(WordCoOccurance.class);
		PTable<Text, Text> urlInputMap = pipeline.read(From.sequenceFile(
				"/user/jgauci/CommonCrawl/textData*", Text.class, Text.class));
		PObject<Long> numDocuments = urlInputMap.length();
		Map<String, Double> tokenInverseDocumentFrequency = urlInputMap
				.values()
				.parallelDo("Tokenize", new Tokenize<Text>(),
						Writables.strings())
				.count()
				.filter("RemoveInfrequent", new RemoveInfrequentFilter(100))
				.parallelDo(
						new ComputeIdf(numDocuments.getValue()),
						Writables.tableOf(Writables.strings(),
								Writables.doubles())).materializeToMap();

		Set<String> targetDictionary = new HashSet<>();
		targetDictionary.add("flare");
		targetDictionary.add("grenade");
		targetDictionary.add("crowbar");
		targetDictionary.add("lockpick");
		targetDictionary.add("medkit");
		targetDictionary.add("food");
		targetDictionary.add("antidote");
		targetDictionary.add("rope");
		targetDictionary.add("armor");

		{
			PCollection<Pair<String, String>> tokens = urlInputMap.values()
					.parallelDo(
							"ComputeCoOccurance",
							new ComputeCoOccurance<Text>(sourceDictionary,
									sourceDictionaryLineTokenMap,
									targetDictionary),
							Writables.pairs(Writables.strings(),
									Writables.strings()));
			PTable<Pair<String, String>, Long> frequencies = tokens.count()
					.filter("RemoveInfrequent",
							new RemoveInfrequentFilter<Pair<String, String>>(
									100));
			for (Pair<Pair<String, String>, Long> entry : frequencies
					.materialize()) {
				String nonDictionaryTerm = entry.first().second();
				if (!tokenInverseDocumentFrequency
						.containsKey(nonDictionaryTerm)) {
					System.out.println("SKIPPING: " + nonDictionaryTerm);
					continue;
				}
				System.out
						.println(entry.first()
								+ " : "
								+ (((double) entry.second()) * tokenInverseDocumentFrequency
										.get(nonDictionaryTerm)));
			}
		}
		pipeline.done();
	}
}
