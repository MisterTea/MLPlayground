package com.github.mistertea.cascading;

import java.util.Properties;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.operation.aggregator.Count;
import cascading.pipe.Each;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.property.AppProps;
import cascading.scheme.hadoop.TextDelimited;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;

public class WordCountCascading {
	public static void main(String[] args) {
		String docPath = "/Users/jgauci/textData-01666";
		String wcPath = "cascadingout/wc";

		Properties properties = new Properties();
		AppProps.setApplicationJarClass(properties, WordCountCascading.class);
		HadoopFlowConnector flowConnector = new HadoopFlowConnector(properties);

		// create source and sink taps
		Tap docTap = new Hfs(new HadoopSequenceFile(new Fields("URL",
				"PageText")), docPath);
		Tap wcTap = new Hfs(new TextDelimited(false, "\t"), wcPath);

		// specify a regex operation to split the "document" text lines into a
		// token stream
		Fields token = new Fields("token");
		Fields text = new Fields("PageText");
		RegexNonEmptySplitGenerator splitter = new RegexNonEmptySplitGenerator(
				token, "\\W+");
		// only returns "token"
		Pipe docPipe = new Each("token", text, splitter, Fields.RESULTS);

		// determine the word counts
		Pipe wcPipe = new Pipe("wc", docPipe);
		wcPipe = new GroupBy(wcPipe, token);
		wcPipe = new Every(wcPipe, Fields.ALL, new Count(), Fields.ALL);

		// connect the taps, pipes, etc., into a flow
		FlowDef flowDef = FlowDef.flowDef().setName("wc")
				.addSource(docPipe, docTap).addTailSink(wcPipe, wcTap);

		// write a DOT file and run the flow
		Flow wcFlow = flowConnector.connect(flowDef);
		wcFlow.writeDOT("dot/wc.dot");
		wcFlow.complete();
	}
}
