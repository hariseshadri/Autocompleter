package com.repuhire.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.repuhire.common.Common.MatchedUser;
import com.repuhire.common.Common.MatchedUser.HighlightIndices;

/***
 * Data structure in charge of storing all of the users.
 * Optimized for quick prefix lookup.
 */
public class StoredUserStructure extends HashMap<Long, StoredUser>{

	private static final long serialVersionUID = -6751388005945265976L;

	/***
	 * Removes a user from the structure
	 *
	 * @param uid The UID of the user to remove
	 * @return True iff a user was removed
	 */
	public boolean deleteUser(long uid) {
		return this.remove(uid) != null;
	}

	/***
	 * Determines whether the structure is aware of a given user
	 *
	 * @param user The user to check contains on
	 * @return True iff this contains the user
	 */
	public boolean containsUser(StoredUser user) {
		return this.containsUser(user.getUid());
	}

	/***
	 * Determines whether the structure is aware of a given user
	 *
	 * @param uid The UID of the user to check
	 * @return True iff this contains the user
	 */
	public boolean containsUser(long uid) {
		return this.containsKey(uid);
	}

	/***
	 * Adds a user to this structure
	 * @param toAdd The user to add to the data store
	 * @throws IllegalArgumentException If this data
	 * store already contains a user of the same UID
	 */
	public void addUser(StoredUser toAdd) {

		if(this.containsKey(toAdd.getUid())) {
			throw new IllegalArgumentException("Attempted to add a user with UID " + toAdd.getUid() + " when one already existed.");
		}

		this.put(toAdd.getUid(), toAdd);
	}

	/***
	 * Fetches a user from the UID
	 *
	 * @param uid The UID of the user to fetch
	 * @return The user corresponding to this UID
	 */
	public StoredUser getUser(long uid) {
		if(containsUser(uid)) {
			return this.get(uid);
		} else {
			throw new IllegalArgumentException("No user exists with UID = " + uid);
		}
	}

	/***
	 * Finds a set of matching users given a typed string
	 *
	 * @param typedSoFar The string typed so far to "match" on
	 * @param numResponses The maximum number of desired matches
	 * @return A collection of users who match what was typed in
	 */
	public Collection<MatchedUser> autocomplete(String typedSoFar, int numResponses) {

		String[] tokenized = typedSoFar.trim().split(" ");
		List<String> tokens = Lists.newArrayList();
		ArrayList<MatchedUser> retVal = Lists.newArrayList();

		for(String tok : tokenized) {
			if(tok.trim().length() > 0) {
				tokens.add(tok.trim().toLowerCase());
			}
		}

		//If nothing is typed, we don't autocomplete
		if(tokens.isEmpty() || numResponses <= 0) {
			return retVal;
		}

		//TODO beef this up
		for(StoredUser user : values()) {

			String firstName = user.getFirstName().toLowerCase();
			String lastName = user.getLastName().toLowerCase();

			List<HighlightIndices> firstNameHighlightIndices = Lists.newArrayList();
			List<HighlightIndices> lastNameHighlightIndices = Lists.newArrayList();
			boolean foundMatchForAllTokens = true;

			for(String token : tokens) {

				List<Integer> firstNameIndices = allIndicesOf(token, firstName);
				List<Integer> lastNameIndices = allIndicesOf(token, lastName);

				//No match
				if(firstNameIndices.isEmpty() && lastNameIndices.isEmpty()) {
					foundMatchForAllTokens = false;
					break;
				}

				for(Integer firstNameIndex : firstNameIndices) {
					HighlightIndices.Builder highlightIndexBuilder = HighlightIndices.newBuilder();
					highlightIndexBuilder.setStart(firstNameIndex);
					highlightIndexBuilder.setEnd(firstNameIndex + token.length());
					firstNameHighlightIndices.add(highlightIndexBuilder.build());
				}

				for(Integer lastNameIndex : lastNameIndices) {
					HighlightIndices.Builder highlightIndexBuilder = HighlightIndices.newBuilder();
					highlightIndexBuilder.setStart(lastNameIndex);
					highlightIndexBuilder.setEnd(lastNameIndex + token.length());
					lastNameHighlightIndices.add(highlightIndexBuilder.build());
				}
			}

			if(!foundMatchForAllTokens) {
				continue;
			}

			MatchedUser.Builder userBuilder = MatchedUser.newBuilder();

			//Add in first name highlights
			consolidateHighlightIndices(firstNameHighlightIndices);
			for(HighlightIndices hi : firstNameHighlightIndices) {
				userBuilder.addFirstNameHighlights(hi);
			}

			//Add in last name highlights
			consolidateHighlightIndices(lastNameHighlightIndices);
			for(HighlightIndices hi : lastNameHighlightIndices) {
				userBuilder.addLastNameHighlights(hi);
			}

			userBuilder.setUser(user.getUser());
			userBuilder.setScore(1);
			retVal.add(userBuilder.build());

			if(numResponses == retVal.size()) {
				return retVal;
			}

		}

		return retVal;
	}

	/***
	 * Finds all the indices of the needle in the given haystack.
	 *
	 * @param needle The substring to find
	 * @param haystack The string to search through
	 * @return A list of 0-based indices of the needle in the haystacks
	 */
	public static List<Integer> allIndicesOf(String needle, String haystack) {
		List<Integer> retVal = Lists.newArrayList();

		int indexOf  = haystack.indexOf(needle);
		while(indexOf >= 0) {
			retVal.add(indexOf);
			indexOf = haystack.indexOf(needle, indexOf + needle.length());
		}

		return retVal;
	}

	/***
	 * Consolidates a collection of highlight indices into a collection
	 * of the fewest possible highlight indices which still highlight the
	 * same thing. Example:
	 *
	 * 0 - 4, 1 - 2, 16 - 28, 3 - 7 could be consolidated into just
	 * 0 - 7, 16 - 28
	 *
	 * @param indices
	 */
	public static void consolidateHighlightIndices(List<HighlightIndices> indices) {

		if(indices.isEmpty()) {
			return;
		}

		//Sort so it's ascending on start value
		Collections.sort(indices, new Comparator<HighlightIndices>(){
			@Override
			public int compare(HighlightIndices arg0, HighlightIndices arg1) {
				return (new Integer(arg0.getStart())).compareTo(new Integer(arg1.getStart()));
			}
		});

		//Take the left-most guy
		HighlightIndices leftMost = indices.remove(0);

		//Walk through indices
		ArrayList<HighlightIndices> indicesCopy = Lists.newArrayList(indices);
		for(HighlightIndices hi : indicesCopy) {

			int leftMostStart = leftMost.getStart();
			int leftMostEnd = leftMost.getEnd();

			int hiStart = hi.getStart();
			int hiEnd = hi.getEnd();

			//If we're in the range..
			if(leftMostStart <= hiStart && hiStart <= leftMostEnd) {
				indices.remove(hi);
				HighlightIndices.Builder hiBuilder = HighlightIndices.newBuilder();
				hiBuilder.setStart(leftMostStart);
				hiBuilder.setEnd(Math.max(hiEnd, leftMostEnd));
				leftMost = hiBuilder.build();
			} else {
				indices.add(leftMost);
				leftMost = hi;
			}
		}

		if(!indices.contains(leftMost)) {
			indices.add(leftMost);
		}

		//Sort so it's ascending on start value
		Collections.sort(indices, new Comparator<HighlightIndices>(){
			@Override
			public int compare(HighlightIndices arg0, HighlightIndices arg1) {
				return (new Integer(arg0.getStart())).compareTo(new Integer(arg1.getStart()));
			}
		});
	}
}
