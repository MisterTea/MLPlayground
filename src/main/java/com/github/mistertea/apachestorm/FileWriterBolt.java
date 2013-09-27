package com.github.mistertea.apachestorm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public class FileWriterBolt extends BaseRichBolt {
	BufferedWriter bw;

	@Override
	public void prepare(Map conf, TopologyContext context,
			OutputCollector collector) {
		try {
			bw = new BufferedWriter(new FileWriter("Results.txt"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void execute(Tuple tuple) {
		try {
			for (int a = 0; a < tuple.size(); a++) {
				if (a > 0) {
					bw.write("\t");
				}
				bw.write(tuple.getValue(a).toString());
			}
			bw.write("\n");
			bw.flush();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
	}

	@Override
	public void cleanup() {
		try {
			bw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		super.cleanup();
	}

}
