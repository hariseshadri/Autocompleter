package com.repuhire.user;

import com.google.common.base.Objects;
import com.repuhire.common.Common.User;

/***
 * StoredUser encompasses the User model, and is to
 * be used when application logic state needs to be maintained
 * by the user but should be put into common.proto.
 */
public class StoredUser {

	//User object (from Common) backing this
	//stored user
	private User user;

	//Immutable uid (even on update, the uid stays constant)
	private final long uid;

	//Memoized user data
	private Domain userDomain;
	//First space last
	//E.g
	//John Smith
	private String fullName;

	/***
	 * Constructs a stored user from a fully instantiated
	 * model
	 *
	 * @param user The Common User model
	 * @throws IllegalArgumentException if the given
	 * user is null
	 */
	public StoredUser(User user) {

		if(user == null) {
			throw new IllegalArgumentException("Cannot construct a null user.");
		}

		this.user = user;
		this.uid = user.getUid();
	}

	//-------
	//Setters
	//-------

	/***
	 * Updates the given user model to the new one
	 *
	 * @param updated The new user's information
	 * @throws IllegalArgumentException if the new user has
	 * a different User UID than the current user.
	 */
	public void update(User updated) {
		invalidateCache();

		if(updated.getUid() != user.getUid()) {
			throw new IllegalArgumentException("Attempt to update a stored user " +
					"with the model of another stored user. " +
					"Original UID = " + user.getUid() +
					", updater's UID = " + updated.getUid());
		}

		this.user = updated;
	}

	/***
	 * Invalidates memoized data
	 */
	private void invalidateCache() {
		this.userDomain = null;
		this.fullName = null;
	}

	//------
	//Getters
	//-------
	public String getFullName() {
		if(fullName == null) {
			fullName = computeFullName();
		}

		return fullName;
	}

	public String getLastName() {
		//Not memoized
		return user.getLastName();
	}

	public String getFirstName() {
		//Not memoized
		return user.getFirstName();
	}

	public String getEmail() {
		//Not memoized
		return user.getEmail();
	}

	public long getTimesRecommended() {
		//Not memoized
		return user.getTimesRecommended();
	}

	public long getUid() {
		//Not memoized
		return user.getUid();
	}

	public Domain getDomain() {
		if(userDomain == null) {
			userDomain = new Domain(user.getDomain());
		}

		return userDomain;
	}

	/***
	 * Computes the "full name" of the user
	 * @return The user's full name
	 */
	private String computeFullName() {
		return getFirstName() + " " + getLastName();
	}

	//---------
	//Overrides
	//---------

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof StoredUser) {
			StoredUser other = (StoredUser) obj;
			return Objects.equal(uid, other.uid);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(user.getUid());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getFullName());
		sb.append(": ");
		sb.append(getEmail());
		sb.append("; recommended ");
		sb.append(getTimesRecommended());
		sb.append(" times");
		return sb.toString();
	}
}
