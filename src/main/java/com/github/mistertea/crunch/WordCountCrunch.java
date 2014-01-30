package com.github.mistertea.crunch;

import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.FilterFn;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PTable;
import org.apache.crunch.Pair;
import org.apache.crunch.Pipeline;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.io.From;
import org.apache.crunch.io.To;
import org.apache.crunch.types.writable.Writables;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;

public class WordCountCrunch {

	public static class RemoveInfrequentFilter extends
			FilterFn<Pair<String, Long>> {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean accept(Pair<String, Long> wordFrequencyPair) {
			return wordFrequencyPair.second() >= 100;
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Pipeline pipeline = new MRPipeline(WordCountCrunch.class);
		PCollection<String> rawInput = pipeline
				.readTextFile("/user/jgauci/WikipediaRawText/wiki_00");
		PTable<String,String> table = rawInput.parallelDo("SplitID",
				new MapFn<String, Pair<String,String>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public Pair<String,String> map(String inputLine) {
						String[] tokens = inputLine.split("\t");
						if (tokens.length!=3) {
							throw new RuntimeException("BAD INPUTLINE: " + inputLine);
						}
						return new Pair<String,String>(tokens[0], tokens[1]);
					}
		}, Writables.tableOf(Writables.strings(), Writables.strings()));
		{
			PCollection<String> tokens = table.values().parallelDo("Tokenize",
					new DoFn<String, String>() {
						private static final long serialVersionUID = 1L;

						@Override
						public void process(String inputDocument,
								Emitter<String> emitter) {
							String tokens[] = inputDocument.toLowerCase().split(
									"\\W+");
							for (String token : tokens) {
								if (token.isEmpty()) {
									continue;
								}
								if (token.length() > 100) {
									continue;
								}
								emitter.emit(token);
							}
						}
					}, Writables.strings());
			PTable<String, Long> frequencies = tokens.count().filter(
					"RemoveInfrequent", new RemoveInfrequentFilter());
			pipeline.writeTextFile(frequencies, "/user/jgauci/output/unigrams.txt");
		}
		/*
		{
			PCollection<String> tokens = table.values().parallelDo(
					"TokenizePairs", new DoFn<String, String>() {
						private static final long serialVersionUID = 1L;

						@Override
						public void process(String inputDocument,
								Emitter<String> emitter) {
							String tokens[] = inputDocument.split(
									"\\W+");
							for (int a = 0; a < tokens.length; a++) {
								for (int b = 1; b <= 25; b++) {
									int aShift = a + b;
									if (aShift >= tokens.length) {
										continue;
									}

									String s1 = tokens[a];
									String s2 = tokens[aShift];

									if (s1.isEmpty() || s1.length() > 100
											|| s2.isEmpty()
											|| s2.length() > 100
											|| s1.equals(s2)) {
										continue;
									}

									if (s1.compareTo(s2) > 0) {
										String tmp = s1;
										s1 = s2;
										s2 = tmp;
									}

									emitter.emit(s1 + " " + s2);
								}
							}
						}
					}, Writables.strings());
			PTable<String, Long> frequencies = tokens.count().filter(
					"RemoveInfrequentPairs", new RemoveInfrequentFilter());
			pipeline.writeTextFile(frequencies, "/user/jgauci/wikipedia/pairs.txt");
		}
		*/
		JobConf jobConf = new JobConf(pipeline.getConfiguration());
		jobConf.setNumReduceTasks(50);
		pipeline.done();
	}
}
