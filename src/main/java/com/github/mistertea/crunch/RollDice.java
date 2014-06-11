package com.github.mistertea.crunch;

import org.apache.crunch.MapFn;
import org.apache.crunch.PCollection;
import org.apache.crunch.Pipeline;
import org.apache.crunch.impl.mr.MRPipeline;
import org.apache.crunch.io.SequentialFileNamingScheme;
import org.apache.crunch.io.impl.FileTargetImpl;
import org.apache.crunch.types.PTypes;
import org.apache.hadoop.fs.Path;

import com.github.mistertea.boardgame.core.PlayerServerState;
import com.google.common.collect.ImmutableList;

public class RollDice {
	public static class PlayerServerStateOutputFormat extends
			ThriftCollectionOutputFormat<PlayerServerState> {
		public PlayerServerStateOutputFormat() {
			super(PlayerServerState.class, ImmutableList.of(1, 2, 3));
		}
	}

	public static void main(String[] args) {
		Pipeline pipeline = new MRPipeline(RollDice.class);
		PCollection<String> rawInput = pipeline
				.readTextFile("/user/jgauci/WikipediaRawText/wiki_00");
		PCollection<PlayerServerState> states = rawInput.parallelDo("SplitID",
				new MapFn<String, PlayerServerState>() {
					private static final long serialVersionUID = 1L;

					@Override
					public PlayerServerState map(String inputLine) {
						String[] tokens = inputLine.split("\t");
						if (tokens.length < 3) {
							return new PlayerServerState(tokens[0], tokens[1],
									"");
						}
						if (tokens.length > 3) {
							throw new RuntimeException("BAD INPUTLINE: "
									+ tokens.length + " " + inputLine);
						}
						return new PlayerServerState(tokens[0], tokens[1],
								tokens[2]);
					}
				}, PTypes.thrifts(PlayerServerState.class,
						rawInput.getTypeFamily()));
    // TODO: Fix this old code
		//states.write(new FileTargetImpl(new Path("/user/jgauci/output.txt"),
    //PlayerServerStateOutputFormat.class,
    //new SequentialFileNamingScheme()));

		pipeline.done();
	}

}
