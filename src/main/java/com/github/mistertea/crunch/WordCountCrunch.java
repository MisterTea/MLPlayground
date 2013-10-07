package com.github.mistertea.crunch;

import org.apache.crunch.DoFn;
import org.apache.crunch.Emitter;
import org.apache.crunch.FilterFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.PTable;
import org.apache.crunch.Pair;
import org.apache.crunch.Pipeline;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.io.From;
import org.apache.crunch.types.writable.Writables;
import org.apache.hadoop.io.Text;

public class WordCountCrunch {

public static class RemoveInfrequentFilter extends
                                           FilterFn<Pair<String, Long>> {
private static final long serialVersionUID = 1L;

@Override
public boolean accept(Pair<String, Long> wordFrequencyPair) {
return wordFrequencyPair.second() >= 100;
}
}

/**
 * @param args
 */
public static void main(String[] args) {
Pipeline pipeline = new MRPipeline(WordCountCrunch.class);
PTable<Text, Text> table = pipeline.read(From.sequenceFile(
                                         "/textData-00112", Text.class, Text.class));
{
PCollection<String> tokens = table.values().parallelDo("Tokenize",
                                                       new DoFn<Text, String>() {
                                                       private static final long serialVersionUID = 1L;

                                                       @Override
                                                       public void process(Text inputDocument,
                                                                           Emitter<String> emitter) {
                                                       String tokens[] = inputDocument.toString().split(
                                                       "\\W+");
                                                       for (String token : tokens) {
                                                       if (token.isEmpty()) {
                                                       continue;
                                                       }
                                                       if (token.length() > 100) {
                                                       continue;
                                                       }
                                                       emitter.emit(token);
                                                       }
                                                       }
                                                       }, Writables.strings());
PTable<String, Long> frequencies = tokens.count().filter(
"RemoveInfrequent", new RemoveInfrequentFilter());
pipeline.writeTextFile(frequencies, "crunchout/wordcount");
}
{
PCollection<String> tokens = table.values().parallelDo(
"TokenizePairs", new DoFn<Text, String>() {
private static final long serialVersionUID = 1L;

@Override
public void process(Text inputDocument,
                    Emitter<String> emitter) {
String tokens[] = inputDocument.toString().split(
"\\W+");
for (int a = 0; a < tokens.length; a++) {
for (int b = 1; b <= 25; b++) {
int aShift = a + b;
if (aShift >= tokens.length) {
continue;
}

String s1 = tokens[a];
String s2 = tokens[aShift];

if (s1.isEmpty() || s1.length() > 100
    || s2.isEmpty()
    || s2.length() > 100
    || s1.equals(s2)) {
continue;
}

if (s1.compareTo(s2) > 0) {
String tmp = s1;
s1 = s2;
s2 = tmp;
}

emitter.emit(s1 + " " + s2);
}
}
}
}, Writables.strings());
PTable<String, Long> frequencies = tokens.count().filter(
"RemoveInfrequentPairs", new RemoveInfrequentFilter());
pipeline.writeTextFile(frequencies, "crunchout/wordcountpairs");
}
pipeline.done();
}
}
