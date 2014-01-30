package com.github.mistertea.kaggle.stackexchange;

import java.util.Properties;

import net.htmlparser.jericho.Source;

import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PTable;
import org.apache.crunch.Pair;
import org.apache.crunch.Pipeline;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.types.writable.Writables;
import org.apache.hadoop.mapred.JobConf;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.flow.FlowProcess;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.Identity;
import cascading.operation.OperationCall;
import cascading.operation.aggregator.Count;
import cascading.operation.regex.RegexSplitGenerator;
import cascading.pipe.Each;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.property.AppProps;
import cascading.scheme.hadoop.TextDelimited;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.github.mistertea.cascading.HadoopSequenceFile;
import com.github.mistertea.cascading.RegexNonEmptySplitGenerator;
import com.github.mistertea.cascading.WordCountCascading;
import com.github.mistertea.crunch.WordCountCrunch;
import com.github.mistertea.crunch.WordCountCrunch.RemoveInfrequentFilter;

public class SubsetTags {
	public static class ScrubFunction extends BaseOperation implements Function {

		public ScrubFunction(Fields tags) {
			super(1, tags);
		}

		@Override
		public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
			String outputString = new Source(functionCall.getArguments()
					.getString(0)).getTextExtractor().toString();
			System.out.println(outputString);
			for (String s : outputString.split("\\s+")) {
				if (s.isEmpty()) {
					continue;
				}
				if (s.length() > 80) {
					continue;
				}
				functionCall.getOutputCollector().add(new Tuple(s));
			}
		}
	}

	public static void main(String[] args) {
		String bodyPath = "hdfs://localhost:9000/user/jgauci/stackexchange/Body.txt";
		String wordCountPath = "hdfs://localhost:9000/user/jgauci/stackexchange/wordcount";

		Properties properties = new Properties();
		AppProps.setApplicationJarClass(properties, WordCountCascading.class);
		HadoopFlowConnector flowConnector = new HadoopFlowConnector(properties);

		// create source and sink taps
		Tap bodyTap = new Hfs(
				new TextDelimited(new Fields("Id", "Body"), "\t"), bodyPath);
		Tap wordCountTap = new Hfs(new TextDelimited(true, "\t"), wordCountPath);

		Fields tokens = new Fields("Tokens");
		Fields body = new Fields("Body");

		// only returns "token"
		Pipe pipe = new Each("StripHTMLAndCountWords", body,
				new RegexSplitGenerator(tokens, "\\W+"), Fields.RESULTS);
		// specify a regex operation to split the "document" text lines into a
		// token stream

		// determine the word counts
		pipe = new GroupBy(pipe, tokens);
		pipe = new Every(pipe, Fields.ALL, new Count(), Fields.ALL);

		// connect the taps, pipes, etc., into a flow
		FlowDef flowDef = FlowDef.flowDef().setName("StripHTMLAndCountWords")
				.addSource("StripHTMLAndCountWords", bodyTap).addTail(pipe)
				.addSink("StripHTMLAndCountWords", wordCountTap);

		// write a DOT file and run the flow
		Flow wcFlow = flowConnector.connect(flowDef);
		wcFlow.writeDOT("dot/wc.dot");
		wcFlow.complete();
	}
}
