package com.github.mistertea.cascading;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.RecordReader;

import cascading.flow.FlowProcess;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.scheme.hadoop.SequenceFile;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

public class HadoopSequenceFile extends SequenceFile {
	private static final long serialVersionUID = 1L;

	public HadoopSequenceFile(Fields fields) {
		super(fields);
	}

	@Override
	public boolean source(FlowProcess<JobConf> flowProcess,
			SourceCall<Object[], RecordReader> sourceCall) throws IOException {
		Text key = (Text) sourceCall.getContext()[0];
		Text value = (Text) sourceCall.getContext()[1];
		boolean result = sourceCall.getInput().next(key, value);

		if (!result)
			return false;

		Tuple tuple = sourceCall.getIncomingEntry().getTuple();
		if (tuple.size() != 2) {
			throw new IOException(
					"Invalid number of tuples to a hadoop sequencefile source: "
							+ tuple.size());
		}

		// key is always null/empty, so don't bother
		if (sourceCall.getIncomingEntry().getFields().isDefined()) {
			tuple.set(0, key);
			tuple.set(1, value);
		} else {
			tuple.clear();
			tuple.set(0, key);
			tuple.set(1, value);
		}

		return true;
	}

	@Override
	public void sink(FlowProcess<JobConf> flowProcess,
			SinkCall<Void, OutputCollector> sinkCall) throws IOException {
		throw new IOException("sink not supported");
	}
}