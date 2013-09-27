package org.commoncrawl.examples;

// Java classes
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.SequenceFileInputFormat;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.lib.LongSumReducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
// Apache Project classes
// Hadoop classes

/**
 * An example showing how to use the Common Crawl 'textData' files to
 * efficiently work with Common Crawl corpus text content.
 * 
 * @author Chris Stephens <chris@commoncrawl.org>
 */
public class ExampleTextWordCount extends Configured implements Tool {

	private static final Logger LOG = Logger
			.getLogger(ExampleTextWordCount.class);

	/**
	 * Perform a simple word count mapping on text data from the Common Crawl
	 * corpus.
	 */
	public static class ExampleTextWordPairCountMapper extends MapReduceBase
			implements Mapper<Text, Text, Text, LongWritable> {

		// create a counter group for Mapper-specific statistics
		private final String _counterGroup = "Custom Mapper Counters";

		public void map(Text key, Text value,
				OutputCollector<Text, LongWritable> output, Reporter reporter)
				throws IOException {

			reporter.incrCounter(this._counterGroup, "Records In", 1);

			try {

				// Get the text content as a string.
				String pageText = value.toString();

				if (pageText == null || pageText.isEmpty()) {
					reporter.incrCounter(this._counterGroup,
							"Skipped - Empty Page Text", 1);
				}

				// Splits by space and outputs to OutputCollector.
				String tokens[] = pageText.toLowerCase().split("\\W+");
				for (int a = 0; a < tokens.length; a++) {
					for (int b = 1; b <= 25; b++) {
						int aShift = a + b;
						if (aShift >= tokens.length) {
							continue;
						}

						String s1 = tokens[a];
						String s2 = tokens[aShift];
						if (s1.compareTo(s2) > 0) {
							String tmp = s1;
							s1 = s2;
							s2 = tmp;
						}

						output.collect(new Text(s1 + " " + s2),
								new LongWritable(1));
					}
				}
			} catch (Exception ex) {
				LOG.error("Caught Exception", ex);
				reporter.incrCounter(this._counterGroup, "Exceptions", 1);
			}
		}
	}

	/**
	 * Perform a simple word count mapping on text data from the Common Crawl
	 * corpus.
	 */
	public static class ExampleTextWordCountMapper extends MapReduceBase
			implements Mapper<Text, Text, Text, LongWritable> {

		// create a counter group for Mapper-specific statistics
		private final String _counterGroup = "Custom Mapper Counters";

		public void map(Text key, Text value,
				OutputCollector<Text, LongWritable> output, Reporter reporter)
				throws IOException {

			reporter.incrCounter(this._counterGroup, "Records In", 1);

			try {

				// Get the text content as a string.
				String pageText = value.toString();

				// Removes all punctuation.
				pageText = pageText.replaceAll("[^a-zA-Z0-9 ]", "");

				// Normalizes whitespace to single spaces.
				pageText = pageText.replaceAll("\\s+", " ");

				if (pageText == null || pageText == "") {
					reporter.incrCounter(this._counterGroup,
							"Skipped - Empty Page Text", 1);
				}

				// Splits by space and outputs to OutputCollector.
				for (String word : pageText.split(" ")) {
					output.collect(new Text(word.toLowerCase()),
							new LongWritable(1));
				}
			} catch (Exception ex) {
				LOG.error("Caught Exception", ex);
				reporter.incrCounter(this._counterGroup, "Exceptions", 1);
			}
		}
	}

	/**
	 * Hadoop FileSystem PathFilter for ARC files, allowing users to limit the
	 * number of files processed.
	 * 
	 * @author Chris Stephens <chris@commoncrawl.org>
	 */
	public static class SampleFilter implements PathFilter {

		private static int count = 0;
		private static int max = 999999999;

		public boolean accept(Path path) {

			if (!path.getName().startsWith("textData-"))
				return false;

			SampleFilter.count++;

			if (SampleFilter.count > SampleFilter.max)
				return false;

			return true;
		}
	}

	/**
	 * A {@link Reducer} that sums long values.
	 */
	public static class LongSumThresholdReducer<K> extends MapReduceBase
			implements
			Reducer<K, LongWritable, K, LongWritable> {

		public void reduce(K key, Iterator<LongWritable> values,
				OutputCollector<K, LongWritable> output, Reporter reporter)
				throws IOException {

			// sum all values for this key
			long sum = 0;
			while (values.hasNext()) {
				sum += values.next().get();
			}

			if (sum >= 100) {
				// output sum
				output.collect(key, new LongWritable(sum));
			}
		}

	}

	/**
	 * Implmentation of Tool.run() method, which builds and runs the Hadoop job.
	 * 
	 * @param args
	 *            command line parameters, less common Hadoop job parameters
	 *            stripped out and interpreted by the Tool class.
	 * @return 0 if the Hadoop job completes successfully, 1 if not.
	 */
	@Override
	public int run(String[] args) throws Exception {

		String outputPath = null;
		String configFile = null;

		// Read the command line arguments.
		if (args.length < 1)
			throw new IllegalArgumentException(
					"Example JAR must be passed an output path.");

		outputPath = args[0];

		if (args.length >= 2)
			configFile = args[1];

		// For this example, only look at a single text file.
		// String inputPath =
		// "s3n://aws-publicdatasets/common-crawl/parse-output/segment/1341690166822/textData-01666";
		String inputPath = "/Users/jgauci/textData-01666";

		// Switch to this if you'd like to look at all text files. May take many
		// minutes just to read the file listing.
		// String inputPath =
		// "s3n://aws-publicdatasets/common-crawl/parse-output/segment/*/textData-*";

		// Read in any additional config parameters.
		if (configFile != null) {
			LOG.info("adding config parameters from '" + configFile + "'");
			this.getConf().addResource(configFile);
		}

		{
			FileSystem fs = FileSystem.get(new URI(outputPath), this.getConf());
			if (fs.exists(new Path(outputPath)))
				fs.delete(new Path(outputPath), true);
		}

		JobClient jc1;
		RunningJob rj1;
		JobConf job1;
		{
			// Creates a new job configuration for this Hadoop job.
			job1 = new JobConf(this.getConf());

			job1.setJarByClass(ExampleTextWordCount.class);

			// Scan the provided input path for ARC files.
			LOG.info("setting input path to '" + inputPath + "'");
			FileInputFormat.addInputPath(job1, new Path(inputPath));
			FileInputFormat.setInputPathFilter(job1, SampleFilter.class);

			// Delete the output path directory if it already exists.
			LOG.info("clearing the output path at '" + outputPath + "'");

			// Set the path where final output 'part' files will be saved.
			LOG.info("setting output path to '" + outputPath + "'");
			FileOutputFormat.setOutputPath(job1, new Path(outputPath
					+ "/wordcount"));
			FileOutputFormat.setCompressOutput(job1, false);

			// Set which InputFormat class to use.
			job1.setInputFormat(SequenceFileInputFormat.class);

			// Set which OutputFormat class to use.
			job1.setOutputFormat(TextOutputFormat.class);

			// Set the output data types.
			job1.setOutputKeyClass(Text.class);
			job1.setOutputValueClass(LongWritable.class);

			// Set which Mapper and Reducer classes to use.
			job1.setMapperClass(ExampleTextWordCount.ExampleTextWordCountMapper.class);
			job1.setCombinerClass(LongSumReducer.class);
			job1.setReducerClass(LongSumThresholdReducer.class);

			jc1 = new JobClient(job1);
			rj1 = jc1.submitJob(job1);
		}

		JobClient jc2;
		RunningJob rj2;
		JobConf job2;
		{
			// Creates a new job configuration for this Hadoop job.
			job2 = new JobConf(this.getConf());

			job2.setJarByClass(ExampleTextWordCount.class);

			// Scan the provided input path for ARC files.
			LOG.info("setting input path to '" + inputPath + "'");
			FileInputFormat.addInputPath(job2, new Path(inputPath));
			FileInputFormat.setInputPathFilter(job2, SampleFilter.class);

			// Delete the output path directory if it already exists.
			LOG.info("clearing the output path at '" + outputPath + "'");

			// Set the path where final output 'part' files will be saved.
			LOG.info("setting output path to '" + outputPath + "'");
			FileOutputFormat.setOutputPath(job2, new Path(outputPath
					+ "/wordpaircount"));
			FileOutputFormat.setCompressOutput(job2, false);

			// Set which InputFormat class to use.
			job2.setInputFormat(SequenceFileInputFormat.class);

			// Set which OutputFormat class to use.
			job2.setOutputFormat(TextOutputFormat.class);

			// Set the output data types.
			job2.setOutputKeyClass(Text.class);
			job2.setOutputValueClass(LongWritable.class);

			// Set which Mapper and Reducer classes to use.
			job2.setMapperClass(ExampleTextWordCount.ExampleTextWordPairCountMapper.class);
			job2.setCombinerClass(LongSumReducer.class);
			job2.setReducerClass(LongSumThresholdReducer.class);

			jc2 = new JobClient(job2);
			rj2 = jc2.submitJob(job2);
		}

		try {
			if (!jc1.monitorAndPrintJob(job1, rj1)) {
				LOG.info("Job Failed: " + rj1.getFailureInfo());
				return 1;
			}
		} catch (InterruptedException ie) {
			return 1;
		}
		try {
			if (!jc2.monitorAndPrintJob(job2, rj2)) {
				LOG.info("Job Failed: " + rj2.getFailureInfo());
				return 1;
			}
		} catch (InterruptedException ie) {
			return 1;
		}

		return 0;
	}

	/**
	 * Main entry point that uses the {@link ToolRunner} class to run the
	 * example Hadoop job.
	 */
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(),
				new ExampleTextWordCount(), args);
		System.exit(res);
	}
}
