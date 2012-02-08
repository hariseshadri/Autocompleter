package com.repuhire.user;

import java.util.HashMap;

/***
 * Data structure in charge of storing all of the users.
 * Optimized for quick prefix lookup.
 */

//TODO make better
public class StoredUserStructure extends HashMap<Long, StoredUser>{

	private static final long serialVersionUID = -6751388005945265976L;

	public boolean deleteUser(long uid) {
		return this.remove(uid) != null;
	}

	public boolean containsUser(StoredUser user) {
		return this.containsUser(user.getUid());
	}

	public boolean containsUser(long uid) {
		return this.containsKey(uid);
	}

	public void addUser(StoredUser toAdd) {

		if(this.containsKey(toAdd.getUid())) {
			throw new IllegalArgumentException("Attempted to add a user with UID " + toAdd.getUid() + " when one already existed.");
		}

		this.put(toAdd.getUid(), toAdd);
	}

	public StoredUser getUser(long uid) {
		if(containsUser(uid)) {
			return this.get(uid);
		} else {
			throw new IllegalArgumentException("No user exists with UID = " + uid);
		}

	}
}
