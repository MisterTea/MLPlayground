package com.github.mistertea.apachestorm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;

public class AccessLogSpout extends BaseRichSpout {
	public static Logger LOG = LoggerFactory.getLogger(AccessLogSpout.class);

	private SpoutOutputCollector collector_;
	private Queue<File> fileQueue = new LinkedList();
	private Scanner scanner = null;

	@Override
	public void open(Map conf, TopologyContext context,
			SpoutOutputCollector collector) {
		collector_ = collector;

		List<Integer> sortedTaskIds = new ArrayList(
				context.getThisWorkerTasks());
		Collections.sort(sortedTaskIds);
		int taskIndex = sortedTaskIds.indexOf(context.getThisTaskId());
		int numTasks = sortedTaskIds.size();

		List<File> files = new ArrayList<File>();
		int a = 0;
		for (File file : new File("inputs").listFiles()) {
			if (a % numTasks == taskIndex) {
				LOG.info("Adding " + file + " to the queue");
				fileQueue.add(file);
			}
			a++;
		}
	}

	@Override
	public void nextTuple() {
		if (scanner == null || !scanner.hasNext()) {
			// open the next file
			try {
				if (fileQueue.isEmpty()) {
					LOG.info("FINISHED PROCESSING FILE");
					this.close();
					Thread.sleep(1000);
					return;
				}
				scanner = new Scanner(new GZIPInputStream(new FileInputStream(
						fileQueue.poll())));
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}

		List<Object> emit = new ArrayList();
		emit.add(scanner.next());
		scanner.next();
		scanner.next();
		String date = scanner.next().substring(1);
		date = date.substring(0, date.indexOf(':'));
		emit.add(date);

		scanner.nextLine();

		System.out.println("GOT IP " + emit.get(0) + " ON DAY " + date);
		collector_.emit(emit);
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("IPAddress", "Time"));
	}
}