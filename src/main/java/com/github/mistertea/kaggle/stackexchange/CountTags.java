package com.github.mistertea.kaggle.stackexchange;

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
import org.apache.crunch.types.writable.Writables;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;

public class CountTags {

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
		Pipeline pipeline = new MRPipeline(CountTags.class);
		PCollection<String> rawInput = pipeline
				.readTextFile("hdfs://localhost:9000/user/jgauci/stackexchange/Tags.txt");
		PCollection<String> tags = rawInput.parallelDo("SplitID",
				new DoFn<String, String>() {
					private static final long serialVersionUID = 1L;

					@Override
					public void process(String input, Emitter<String> emitter) {
						String[] tokens = input.split("\\s+");
						for (int a = 1; a < tokens.length; a++) {
							emitter.emit(tokens[a]);
						}
					}
				}, Writables.strings());
		PTable<String, Long> tagCount = tags.count().parallelDo(
				new RemoveInfrequentFilter(),
				Writables.tableOf(Writables.strings(), Writables.longs()));
		pipeline.writeTextFile(tagCount,
				"hdfs://localhost:9000/user/jgauci/wikipedia/tagcounts.txt");
		pipeline.writeTextFile(tagCount.keys(),
				"hdfs://localhost:9000/user/jgauci/wikipedia/PopularTags.txt");
		pipeline.done();
	}
}
