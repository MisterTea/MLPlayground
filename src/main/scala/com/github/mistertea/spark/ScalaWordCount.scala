package com.github.mistertea.spark

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.io.Text;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.PairRDDFunctions

import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import scala.collection.mutable.ListBuffer

object ScalaWordCount {

  def main(args: Array[String]): Unit = {
    if (args.length < 3) {
      System.err.println("Usage: ScalaWordCount <master> <file> <output_file>");
      System.exit(1);
    }

    val ctx = new SparkContext(
    args(0), "ScalaWordCount",
    System.getenv("SPARK_HOME"),
    Seq(), Map(), Map());
    val lines = ctx.textFile(args(1))

    val words = lines.flatMap( (s:String) => {
      val tokens = new ListBuffer[String]();
      var first = true;
      for(token <- s.toLowerCase().split(" ")) {
        if (first) {
          first = false;
        } else {
    	    val cleanedToken = token
    			  .replaceFirst("^[^a-zA-Z0-9]+", "")
    			  .replaceAll("[^a-zA-Z0-9]+$", "");
    	    if (!cleanedToken.isEmpty() && StringUtils.isAsciiPrintable(cleanedToken)) {
    		    tokens += cleanedToken;
    	    }
        }
      }
      tokens;
      }
    );
    
    val ones = words.map((s) => {
        (s, 1);
      }
    );
    
    val counts = ones.reduceByKey((i1, i2) => {
        i1 + i2;
      }
    );

    val thresholdCounts = counts.filter( (wordCountPair) => {
    		wordCountPair._2 >= 100;
    	}
    );

    thresholdCounts.saveAsTextFile(args(2))
    /*
    val output = thresholdCounts.collect();
    val bw = new BufferedWriter(new FileWriter("WikipediaWords.txt"));
    for (tuple <- output) {
      //System.out.println(tuple._1 + ": " + tuple._2);
      bw.write(tuple._1 + ": " + tuple._2);
    }
    bw.close();
    */
    System.exit(0);
  }

}
