package com.github.mistertea.apachestorm;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;
import backtype.storm.utils.Utils;

public class ApacheStormMain {

	public static void main(String[] args) throws AlreadyAliveException,
			InvalidTopologyException {
		TopologyBuilder builder = new TopologyBuilder();

		builder.setSpout("ApacheLogReader", new AccessLogSpout(), 10);
		builder.setBolt("IPCounter", new IPFrequencyBolt(), 10).fieldsGrouping(
				"ApacheLogReader", new Fields("IPAddress"));
		builder.setBolt("Writer", new FileWriterBolt(), 1).noneGrouping(
				"IPCounter");

		Config conf = new Config();
		conf.setDebug(true);

		if (args != null && args.length > 0) {
			conf.setNumWorkers(3);

			StormSubmitter.submitTopology(args[0], conf,
					builder.createTopology());
		} else {

			LocalCluster cluster = new LocalCluster();
			cluster.submitTopology("test", conf, builder.createTopology());
			Utils.sleep(1000 * 3600);
			cluster.killTopology("test");
			cluster.shutdown();
		}
	}

}
