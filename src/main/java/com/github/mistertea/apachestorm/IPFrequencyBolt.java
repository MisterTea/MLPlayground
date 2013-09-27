package com.github.mistertea.apachestorm;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class IPFrequencyBolt extends BaseRichBolt {
	OutputCollector _collector;
	Map<String, Map<String, Integer>> dateFrequency = new HashMap();
	Map<String, Integer> dateSums = new HashMap();

	@Override
	public void prepare(Map conf, TopologyContext context,
			OutputCollector collector) {
		_collector = collector;
	}

	@Override
	public void execute(Tuple tuple) {
		String ipAddress = tuple.getString(0);
		String date = tuple.getString(1);

		Map<String, Integer> ipFrequency = dateFrequency.get(date);
		if (ipFrequency == null) {
			ipFrequency = new HashMap<String, Integer>();
			dateSums.put(date, 1);
			dateFrequency.put(date, ipFrequency);
		} else {
			dateSums.put(date, dateSums.get(date) + 1);
		}
		Integer v = ipFrequency.get(ipAddress);
		if (v == null) {
			v = 1;
			ipFrequency.put(ipAddress, v);
		} else {
			v++;
			ipFrequency.put(ipAddress, v);
		}
		int numEntries = dateSums.get(date);
		if (numEntries % 10000 == 0) {
			// Recompute CDF for this day and send the top nth.
			TreeMap<Integer, Integer> histogram = new TreeMap<Integer, Integer>();
			int sum = numEntries;
			for (Integer i : ipFrequency.values()) {
				Integer count = histogram.get(i);
				if (count == null) {
					histogram.put(i, 1);
				} else {
					histogram.put(i, 1 + count);
				}
			}
			int sum50 = (sum * 50) / 100;
			int rollingSum = 0;
			int threshold = -1;
			for (Map.Entry<Integer, Integer> histogramEntry : histogram
					.entrySet()) {
				rollingSum += histogramEntry.getKey()
						* histogramEntry.getValue();
				if (rollingSum >= sum50) {
					threshold = histogramEntry.getKey();
					break;
				}
			}
			for (Map.Entry<String, Integer> ipFrequencyPair : ipFrequency
					.entrySet()) {
				if (ipFrequencyPair.getValue() >= threshold) {
					_collector.emit(new Values(date, ipFrequencyPair.getKey(),
							ipFrequencyPair.getValue()));
				}
			}
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("Date", "IPAddress", "Count"));
	}


}
