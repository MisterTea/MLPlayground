package com.github.mistertea.cascading;

import java.util.regex.Pattern;

import cascading.flow.FlowProcess;
import cascading.operation.FunctionCall;
import cascading.operation.regex.RegexSplitGenerator;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.util.Pair;

public class RegexNonEmptySplitGenerator extends RegexSplitGenerator {

	public RegexNonEmptySplitGenerator(Fields fieldDeclaration,
			String patternString) {
		super(fieldDeclaration, patternString);
	}

	public RegexNonEmptySplitGenerator(String patternString) {
		super(patternString);
	}

	@Override
	public void operate(FlowProcess flowProcess,
			FunctionCall<Pair<Pattern, Tuple>> functionCall) {
		String value = functionCall.getArguments().getString(0);

		if (value == null)
			value = "";

		String[] split = functionCall.getContext().getLhs().split(value);

		for (String string : split) {
			if (string.isEmpty()) {
				continue;
			}

			functionCall.getContext().getRhs().set(0, string);
			functionCall.getOutputCollector().add(
					functionCall.getContext().getRhs());
		}
	}
}
