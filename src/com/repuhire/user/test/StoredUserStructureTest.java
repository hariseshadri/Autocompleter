package com.repuhire.user.test;

import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.repuhire.common.Common.MatchedUser.HighlightIndices;
import com.repuhire.datastructures.Pair;
import com.repuhire.user.StoredUserStructure;

/***
 * Tests the StoredUserStructure module
 */
public class StoredUserStructureTest {

	private static StoredUserStructure structure;

	@BeforeClass
	public static void init() {
		structure = new StoredUserStructure();
	}

	@Test
	public void testConsolidateHighlights1() {

		//0 - 4, 1 - 2, 16 - 28, 3 - 7 could be consolidated into just
		//0 - 7, 16 - 28

		List<Pair<Integer, Integer>> input = Lists.newArrayList();
		input.add(Pair.of(0, 4));
		input.add(Pair.of(1, 2));
		input.add(Pair.of(16, 28));
		input.add(Pair.of(3, 7));

		List<Pair<Integer, Integer>> output = Lists.newArrayList();
		output.add(Pair.of(0, 7));
		output.add(Pair.of(16, 28));

		assertConsolidateWorked(input, output);
	}

	@Test
	public void testConsolidateHighlights2() {

		//1 - 2, 3 - 4,

		List<Pair<Integer, Integer>> input = Lists.newArrayList();
		input.add(Pair.of(1, 2));
		input.add(Pair.of(3, 4));

		assertConsolidateWorked(input, input);
	}

	private void assertConsolidateWorked(List<Pair<Integer, Integer>> input, List<Pair<Integer, Integer>> output) {

		List<HighlightIndices> transformedInput = Lists.newArrayList();

		for(Pair<Integer, Integer> inPair : input) {
			HighlightIndices.Builder hiBuilder = HighlightIndices.newBuilder();
			hiBuilder.setStart(inPair.getFirst());
			hiBuilder.setEnd(inPair.getSecond());
			transformedInput.add(hiBuilder.build());
		}

		StoredUserStructure.consolidateHighlightIndices(transformedInput);
		Assert.assertEquals(output.size(), transformedInput.size());

		int i = 0;
		for(HighlightIndices index : transformedInput) {
			Assert.assertEquals((int) output.get(i).getFirst(), index.getStart());
			Assert.assertEquals((int) output.get(i).getSecond(), index.getEnd());
			i++;
		}
	}

}
