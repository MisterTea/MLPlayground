/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.mistertea.spark;

import scala.Tuple2;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaWordCount {
  public static void main(String[] args) throws Exception {
    if (args.length < 2) {
      System.err.println("Usage: JavaWordCount <master> <file>");
      System.exit(1);
    }

    JavaSparkContext ctx = new JavaSparkContext(
    args[0], "JavaWordCount",
    System.getenv("SPARK_HOME"),
    new String[]{});

    JavaRDD<Text> lines = ctx.sequenceFile(args[1], Text.class, Text.class).values();

    JavaRDD<String> words = lines.flatMap(new FlatMapFunction<Text, String>() {
      public Iterable<String> call(Text s) {
      List<String> tokens = new ArrayList<String>();
      for(String token : s.toString().split(" ")) {
    	  token = token
    			  	.replaceFirst("^[^a-zA-Z0-9]+", "")
    			  	.replaceAll("[^a-zA-Z0-9]+$", "");
    	  if (!token.isEmpty() && StringUtils.isAsciiPrintable(token)) {
    		  tokens.add(token);
    	  }
      }
      return tokens;
      }
    });
    
    JavaPairRDD<String, Integer> ones = words.map(new PairFunction<String, String, Integer>() {
      public Tuple2<String, Integer> call(String s) {
        return new Tuple2<String, Integer>(s, 1);
      }
    });
    
    JavaPairRDD<String, Integer> counts = ones.reduceByKey(new Function2<Integer, Integer, Integer>() {
      public Integer call(Integer i1, Integer i2) {
        return i1 + i2;
      }
    });

    JavaPairRDD<String, Integer> thresholdCounts = counts.filter(new Function<Tuple2<String,Integer>, Boolean>() {
    	public Boolean call(Tuple2<String, Integer> count) {
    		return count._2() >= 100;
    	}
    });

    List<Tuple2<String, Integer>> output = thresholdCounts.collect();
    for (Tuple2 tuple : output) {
      System.out.println(tuple._1 + ": " + tuple._2);
    }
    System.exit(0);
  }
}
