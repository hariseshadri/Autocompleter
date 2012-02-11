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

				int firstNameIndexOf = firstName.indexOf(token), lastNameIndexOf = lastName.indexOf(token);

				//No match
				if(firstNameIndexOf == -1 && lastNameIndexOf == -1) {
					foundMatchForAllTokens = false;
					break;
				}

				if(firstNameIndexOf != -1) {
					HighlightIndices.Builder highlightIndexBuilder = HighlightIndices.newBuilder();
					highlightIndexBuilder.setStart(firstNameIndexOf);
					highlightIndexBuilder.setEnd(firstNameIndexOf + token.length());
					firstNameHighlightIndices.add(highlightIndexBuilder.build());
				}

				if(lastNameIndexOf != -1) {
					HighlightIndices.Builder highlightIndexBuilder = HighlightIndices.newBuilder();
					highlightIndexBuilder.setStart(lastNameIndexOf);
					highlightIndexBuilder.setEnd(lastNameIndexOf + token.length());
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

		List<HighlightIndices> retVal = Lists.newArrayList();

		//Sort so it's ascending on start value
		Collections.sort(indices, new Comparator<HighlightIndices>(){
			@Override
			public int compare(HighlightIndices arg0, HighlightIndices arg1) {
				return (new Integer(arg0.getStart())).compareTo(new Integer(arg1.getStart()));
			}
		});

		while(!indices.isEmpty()) {

			HighlightIndices toAdd = indices.remove(0);

			for(HighlightIndices whatsLeft : Lists.newArrayList(indices)) {

				//Totally consumed by toAdd, forget about this guy
				if(toAdd.getStart() <= whatsLeft.getStart() && toAdd.getEnd() >= whatsLeft.getEnd()) {
					indices.remove(whatsLeft);
				}

				//Totally consumes toAdd, modify toAdd and forget about toAdd
				else if(whatsLeft.getStart() <= toAdd.getStart() && whatsLeft.getEnd() >= toAdd.getEnd()) {
					indices.remove(whatsLeft);
					toAdd = whatsLeft;
				}

				//toAdd starts lower and partially consumed by toAdd.. modify toAdd and forget about this guy
				else if(toAdd.getStart() <= whatsLeft.getStart() && toAdd.getEnd() <= whatsLeft.getEnd()) {
					HighlightIndices.Builder builder = HighlightIndices.newBuilder();
					builder.setStart(toAdd.getStart());
					builder.setEnd(whatsLeft.getEnd());
					indices.remove(whatsLeft);
					toAdd = builder.build();
				}

				//toAdd starts higher and partially consumed by whatsLeft.. modify toAdd and forget about this guy
				else if(whatsLeft.getStart() <= toAdd.getStart() && whatsLeft.getEnd() <= toAdd.getEnd()) {
					HighlightIndices.Builder builder = HighlightIndices.newBuilder();
					builder.setStart(whatsLeft.getStart());
					builder.setEnd(toAdd.getEnd());
					indices.remove(whatsLeft);
					toAdd = builder.build();
				}

			}

			retVal.add(toAdd);
		}

		indices.addAll(retVal);
	}
}
