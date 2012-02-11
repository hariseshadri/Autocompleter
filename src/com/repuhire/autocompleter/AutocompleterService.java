package com.repuhire.autocompleter;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.repuhire.common.Common.AutocompleteRequest;
import com.repuhire.common.Common.Autocompleter.BlockingInterface;
import com.repuhire.common.Common.ClearRequest;
import com.repuhire.common.Common.DeleteRequest;
import com.repuhire.common.Common.MatchedUser;
import com.repuhire.common.Common.MatchedUsers;
import com.repuhire.common.Common.Status;
import com.repuhire.common.Common.User;
import com.repuhire.common.Common.Users;
import com.repuhire.user.Domain;
import com.repuhire.user.StoredUser;
import com.repuhire.user.StoredUserStructure;

/***
 * AutocompleterService is the implementation of the Autocompleter
 * prototype specified in common.proto
 *
 * AutocompleterService is a singleton class; the single instance can be
 * accessed via AutocompleterService.getInstance()
 *
 */
public class AutocompleterService implements BlockingInterface {

	//Maps each domain to a collection of users in that domain
	private Map<Domain, StoredUserStructure> userMap = new HashMap<Domain, StoredUserStructure>();

	//Singleton state var
	private static AutocompleterService service = null;

	//Commonly used "valid status" variable
	private static final Status VALID_STATUS;
	static{
		Status.Builder statusBuilder = Status.newBuilder();
		statusBuilder.setStatusCode(200);
		statusBuilder.setMessage("Success");
		VALID_STATUS = statusBuilder.build();
	}

	/***
	 * Prevent instantiation
	 */
	private AutocompleterService() {}

	/***
	 * Singleton accesssor.
	 *
	 * @return The same instance of AutocompleterService everytime.
	 */
	public static AutocompleterService getInstance() {
		if(service == null) {
			service = new AutocompleterService();
		}

		return service;
	}

	@Override
	public Status addUsers(RpcController controller, Users request)
			throws ServiceException {

		try {
			int numUsers = request.getUsersCount();

			//TODO Improve perf by using a batch collection addAll request
			for(int userIndex = 0; userIndex < numUsers; userIndex++) {
				addUser(request.getUsers(userIndex));
			}

			return VALID_STATUS;
		} catch (Exception e) {
			return getInvalidStatus(e, request);
		}
	}

	@Override
	public Status addUser(RpcController controller, User request)
			throws ServiceException {
		try {
			addUser(request);
			return VALID_STATUS;
		} catch (Exception e) {
			return getInvalidStatus(e, request);
		}
	}

	@Override
	public Status update(RpcController controller, User request)
			throws ServiceException {
		try {
			updateUser(request);
			return VALID_STATUS;
		} catch (Exception e) {
			return getInvalidStatus(e, request);
		}
	}

	@Override
	public Status delete(RpcController controller, DeleteRequest request)
			throws ServiceException {
		try {
			deleteUser(request.getDomain(), request.getUid());
			return VALID_STATUS;
		} catch(Exception e) {
			return getInvalidStatus(e, request);
		}
	}

	@Override
	public Status clearUsers(RpcController controller, ClearRequest request)
			throws ServiceException {

		try {
			for(String domain : request.getDomainsList()) {
				clearUsersFromDomain(new Domain(domain));
			}

			return VALID_STATUS;
		} catch (Exception e) {
			return getInvalidStatus(e, request);
		}
	}

	@Override
	public MatchedUsers autocomplete(RpcController controller,
			AutocompleteRequest request) throws ServiceException {
		MatchedUsers.Builder matchedUsers = MatchedUsers.newBuilder();

		try {
			String typedSoFar = request.getTyped();
			Domain domain = new Domain(request.getDomain());
			int numResponses = request.getNumResponses();

			for(MatchedUser matchedUser : userMap.get(domain).autocomplete(typedSoFar, numResponses)) {
				matchedUsers.addMatchedUsers(matchedUser);
			}

			matchedUsers.setStatus(VALID_STATUS);
			return matchedUsers.build();
		} catch (Exception e) {
			matchedUsers.setStatus(getInvalidStatus(e, request));
		}

		return matchedUsers.build();
	}

	private Status getInvalidStatus(Exception e, Object request) {
		Status.Builder status = Status.newBuilder();
		status.setStatusCode(500);
		status.setMessage("RPC failed: " + e.getMessage() + " with request " + request);
		return status.build();
	}

	/***
	 * Adds a given user to the list of maintained users
	 * @param user The user to store
	 * @throws IllegalArgumentException if a user with that
	 * same UID already exists in the domain of that user
	 */
	private void addUser(User user) {

		StoredUser toAdd = new StoredUser(user);
		Domain domain = toAdd.getDomain();
		if(!userMap.containsKey(domain)) {
			userMap.put(domain, new StoredUserStructure());
		}

		StoredUserStructure userStore = userMap.get(domain);

		if(userStore.containsUser(toAdd)) {
			throw new IllegalArgumentException("Attempt to re-add a user with unique identifier " + toAdd.getUid() + " to domain " + toAdd.getDomain());
		}

		userStore.addUser(toAdd);
	}

	/***
	 * Purges a user from the given domain which he/she
	 * is registered in
	 *
	 * @param domainIdentifier The domain which this user belongs to
	 * @param uid The UID of the user to delete
	 */
	private void deleteUser(String domainIdentifier, long uid) {

		Domain domain = new Domain(domainIdentifier);

		if(!userMap.containsKey(domain)) {
			throw new IllegalArgumentException("UID " + uid + " could not be deleted; unknown domain: " + domainIdentifier);
		}

		StoredUserStructure userStore = userMap.get(domain);

		if(!userStore.deleteUser(uid)) {
			throw new IllegalArgumentException("Cannot delete user with UID " + uid + "; no such user exists in domain " + domainIdentifier);
		}

	}

	/***
	 * Updates the information for the given user
	 *
	 * @param user The user whose UID should be updated
	 * with the object's new data
	 */
	private void updateUser(User user) {

		Domain domain = new Domain(user.getDomain());

		if(!userMap.containsKey(domain)) {
			throw new IllegalArgumentException("Cannot update user: " + user + "; from an unknown domain " + domain);
		}

		StoredUserStructure userStore = userMap.get(domain);
		StoredUser storedUser = userStore.getUser(user.getUid());
		storedUser.update(user);
	}

	/***
	 * Clears all of the users from the given domain
	 * @param d The domain to purge users from
	 */
	private void clearUsersFromDomain(Domain d) {
		userMap.remove(d);
	}
}
