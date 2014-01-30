package com.github.mistertea.kaggle.stackexchange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.FilterFn;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PTable;
import org.apache.crunch.Pair;
import org.apache.crunch.Pipeline;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.types.writable.Writables;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class PreProcess {

	public static class RemoveInfrequentFilter extends
			FilterFn<Pair<String, Long>> {
		private int threshold;

		public RemoveInfrequentFilter(int threshold) {
			this.threshold = threshold;
		}

		@Override
		public boolean accept(Pair<String, Long> wordFrequencyPair) {
			return wordFrequencyPair.second() >= threshold;
		}
	}

	public static class SplitIdMap extends MapFn<String, Pair<Long, String>> {
		private static final long serialVersionUID = 1L;

		@Override
		public Pair<Long, String> map(String input) {
			String[] tokens = input.split("\t", 2);
			if (tokens.length != 2) {
				throw new RuntimeException("BAD INPUT: " + tokens.length + ": "
						+ input);
			}
			return new Pair<Long, String>(Long.parseLong(tokens[0]), tokens[1]);
		}

	}

	public static class TokenizeBodiesFn extends DoFn<String, String> {
		private transient Analyzer analyzer;

		@Override
		public void initialize() {
			analyzer = new StandardAnalyzer(Version.LUCENE_45);
		}

		@Override
		public void process(String input, Emitter<String> emitter) {
			try {
				TokenStream ts = analyzer.tokenStream("text", input);
				ts.addAttribute(CharTermAttribute.class);
				ts.reset();
				while (ts.incrementToken()) {
					String s = ts.getAttribute(CharTermAttribute.class)
							.toString();
					emitter.emit(s);
				}
				ts.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static class IntegerizeFn
			extends
			MapFn<Pair<String, String>, Pair<Collection<Integer>, Collection<Integer>>> {
		private HashMap<String, Integer> tagMappingLog, bodyMappingLog;
		private transient Analyzer analyzer;

		@Override
		public void initialize() {
			analyzer = new StandardAnalyzer(Version.LUCENE_45);
		}

		public IntegerizeFn(HashMap<String, Integer> tagMappingLog,
				HashMap<String, Integer> bodyMappingLog) {
			this.tagMappingLog = tagMappingLog;
			this.bodyMappingLog = bodyMappingLog;
		}

		@Override
		public Pair<Collection<Integer>, Collection<Integer>> map(
				Pair<String, String> input) {
			Set<Integer> labels = new HashSet<Integer>();
			Set<Integer> features = new HashSet<Integer>();
			String labelString = input.first();
			String featureString = input.second();
			try {
				boolean first = true;
				TokenStream ts = analyzer.tokenStream("text", featureString);
				ts.addAttribute(CharTermAttribute.class);
				ts.reset();
				while (ts.incrementToken()) {
					String s = ts.getAttribute(CharTermAttribute.class)
							.toString();
					Integer i = bodyMappingLog.get(s);
					if (i != null) {
						features.add(i);
					}
				}
				ts.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			String[] labelStrings = labelString.split("\\s+");
			for (String s : labelStrings) {
				if (s.isEmpty())
					continue;
				Integer i = tagMappingLog.get(s);
				if (i != null) {
					labels.add(i);
				}
			}
			return new Pair<Collection<Integer>, Collection<Integer>>(labels,
					features);
		}
	}

	public static class ToSvmFn
			extends
			DoFn<Pair<Long, Pair<Collection<Integer>, Collection<Integer>>>, String> {
		private int negativeCount = 0;
		private int noneLabel;

		public ToSvmFn(int noneLabel) {
			this.noneLabel = noneLabel;
		}

		@Override
		public void process(
				Pair<Long, Pair<Collection<Integer>, Collection<Integer>>> input,
				Emitter<String> emitter) {
			Collection<Integer> tags = input.second().first();
			Collection<Integer> features = input.second().second();
			StringBuffer out = new StringBuffer();
//			boolean positive = tags.contains(positiveLabel);
//			if (!positive) {
//				negativeCount++;
//				if (negativeCount % 10 != 0) {
//					// skip negative examples
//					return;
//				}
//			}
//			if (positive) {
//				out.append("+1 ");
//			} else {
//				out.append("-1 ");
//			}
			if (tags.isEmpty()) {
				return;
				//out.append(noneLabel + ";");
			} else {
				boolean first=true;
				for (Integer tag : tags) {
					if (first) {
						first=false;
					} else {
						out.append(' ');
					}
					out.append(tag);
				}
				out.append(";");
			}
			// TreeSet<Integer> sortedTags = new TreeSet<Integer>(tags);
			// boolean first=true;
			// for (Integer i : sortedTags) {
			// if (first) {
			// first = false;
			// } else {
			// out.append(",");
			// }
			// out.append(i);
			// }
			// Add the bias feature
			out.append(" 1:1 ");
			TreeSet<Integer> sortedFeatures = new TreeSet<Integer>(features);
			for (int i : sortedFeatures) {
				out.append(i);
				out.append(":1 ");
			}
			out.deleteCharAt(out.length() - 1);
			emitter.emit(out.toString());
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Pipeline pipeline = new MRPipeline(PreProcess.class);
		PCollection<String> bodyFile = pipeline
				.readTextFile("hdfs://localhost:9000/user/jgauci/stackexchange/Body.txt");
		PCollection<String> tagFile = pipeline
				.readTextFile("hdfs://localhost:9000/user/jgauci/stackexchange/Tags.txt");
		PTable<Long, String> bodyTable = bodyFile.parallelDo(new SplitIdMap(),
				Writables.tableOf(Writables.longs(), Writables.strings()));
		PTable<Long, String> tagTable = tagFile.parallelDo(new SplitIdMap(),
				Writables.tableOf(Writables.longs(), Writables.strings()));
		/*
		PCollection<String> bodies = bodyTable.values();

		Iterable<String> bodyDictionary = bodies
				.parallelDo(new TokenizeBodiesFn(), Writables.strings())
				.count()
				.parallelDo(
						new RemoveInfrequentFilter(100),
						Writables.tableOf(Writables.strings(),
								Writables.longs())).keys().materialize();

		PCollection<String> tags = tagTable.values();

		Iterable<String> tagDictionary = tags
				.parallelDo(new TokenizeBodiesFn(), Writables.strings())
				.count()
				.parallelDo(
						new RemoveInfrequentFilter(60000),
						Writables.tableOf(Writables.strings(),
								Writables.longs())).keys().materialize();

		saveDictionary("BodyDictionary.txt", bodyDictionary, 2);
		saveDictionary("TagDictionary.txt", tagDictionary, 1);
		*/

		HashMap<String, Integer> bodyMappingLog = loadDictionary("BodyDictionary.txt");
		HashMap<String, Integer> tagMappingLog = loadDictionary("TagDictionary.txt");

		HashMap<Integer, String> reverseTagMappingLog = new HashMap<>();
		for (Entry<String, Integer> entry : tagMappingLog.entrySet()) {
			reverseTagMappingLog.put(entry.getValue(), entry.getKey());
		}

		PCollection<Pair<Long, Pair<Collection<Integer>, Collection<Integer>>>> joinedLogs = tagTable
				.join(bodyTable).mapValues(
						new IntegerizeFn(tagMappingLog, bodyMappingLog),
						Writables.pairs(
								Writables.collections(Writables.ints()),
								Writables.collections(Writables.ints())));

		PCollection<String> libSvmFormat = joinedLogs.parallelDo(new ToSvmFn(tagMappingLog.size()+2),
				Writables.strings());

		Iterable<String> allInputs = libSvmFormat.materialize();
		BufferedWriter trainingWriter = new BufferedWriter(
				new FileWriter(
						"/Users/jgauci/DataMining/Kaggle/StackExchange/libsvm/Training.txt"));
		BufferedWriter testWriter = new BufferedWriter(
				new FileWriter(
						"/Users/jgauci/DataMining/Kaggle/StackExchange/libsvm/Test.txt"));
		BufferedWriter trueLabelWriter = new BufferedWriter(
				new FileWriter(
						"/Users/jgauci/DataMining/Kaggle/StackExchange/libsvm/TrueTestLabels.txt"));
		int count = 0;
		for (String s : allInputs) {
			String[] tokens = s.split(";", 2);
			String multilabel = tokens[0];
			String firstLabel = multilabel.split("\\s+")[0];
			String features = tokens[1];
			String libsvmrow = firstLabel + " " + features + "\n";
			count++;
			if (count % 10 == 0) {
				testWriter.write(libsvmrow);
				trueLabelWriter.write(multilabel + "\n");
			} else {
				trainingWriter.write(libsvmrow);
			}
		}
		trainingWriter.close();
		testWriter.close();
		trueLabelWriter.close();

		pipeline.done();
	}

	private static HashMap<String, Integer> saveDictionary(String filename,
			Iterable<String> dictionary, int startIndex) throws IOException {
		Configuration config = new Configuration();
		FileSystem fs = FileSystem.get(config);
		Path filenamePath = new Path(
				"hdfs://localhost:9000/user/jgauci/stackexchange/" + filename);
		if (fs.exists(filenamePath)) {
			fs.delete(filenamePath, true);
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"/Users/jgauci/DataMining/Kaggle/StackExchange/libsvm/"
						+ filename));

		FSDataOutputStream fin = fs.create(filenamePath);
		HashMap<String, Integer> mappingLog = new HashMap<String, Integer>();
		for (String s : dictionary) {
			int nextIndex = mappingLog.size() + startIndex;
			fin.writeChars(s + "\t" + nextIndex + "\n");
			mappingLog.put(s, nextIndex);
			writer.write(s + "\t" + nextIndex + "\n");
		}
		fin.close();
		writer.close();
		return mappingLog;
	}

	private static HashMap<String, Integer> loadDictionary(String filename)
			throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				"/Users/jgauci/DataMining/Kaggle/StackExchange/libsvm/"
						+ filename));
		HashMap<String, Integer> dictionary = new HashMap<String, Integer>();
		String s;
		while ((s = reader.readLine()) != null) {
			String[] tokens = s.split("\t");
			dictionary.put(tokens[0], Integer.parseInt(tokens[1]));
		}
		return dictionary;
	}
}
