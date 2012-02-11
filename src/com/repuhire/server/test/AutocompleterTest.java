package com.repuhire.server.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.protobuf.BlockingRpcChannel;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.ServiceException;
import com.googlecode.protobuf.socketrpc.RpcChannels;
import com.googlecode.protobuf.socketrpc.RpcConnectionFactory;
import com.googlecode.protobuf.socketrpc.SocketRpcConnectionFactories;
import com.googlecode.protobuf.socketrpc.SocketRpcController;
import com.repuhire.common.Common.AutocompleteRequest;
import com.repuhire.common.Common.Autocompleter;
import com.repuhire.common.Common.Autocompleter.BlockingInterface;
import com.repuhire.common.Common.ClearRequest;
import com.repuhire.common.Common.DeleteRequest;
import com.repuhire.common.Common.MatchedUser;
import com.repuhire.common.Common.MatchedUser.HighlightIndices;
import com.repuhire.common.Common.MatchedUsers;
import com.repuhire.common.Common.Status;
import com.repuhire.common.Common.User;
import com.repuhire.datastructures.Pair;
import com.repuhire.server.Server;

public class AutocompleterTest {

	static BlockingInterface service;
	static SocketRpcController rpcController;
	static RpcCallback<Status> mustNotFailCallback;

	//Initialization
	@BeforeClass
	public static void init() {

		final int port = 12345;

		//Start the server.. this is an absurd hack
		Thread t = new Thread() {
			@Override
			public void run() {
				Server.start(port, 10);
			};
		};

		t.start();
		try {
			Thread.sleep(300);
		} catch (InterruptedException ie) {

		}

		// Create channel
		RpcConnectionFactory connectionFactory = SocketRpcConnectionFactories
		    .createRpcConnectionFactory("localhost", port);
		BlockingRpcChannel channel = RpcChannels.newBlockingRpcChannel(connectionFactory);

		// Call service
		service = Autocompleter.newBlockingStub(channel);
		rpcController = new SocketRpcController();
	}

	@Test
	public void domainSeparationTest() throws ServiceException{

		//Clear the domains we'll be using
		clearUsersFromDomains("domain1", "domain2");

		//Add "Joe Doe", "John Smith", and "John Tackie" to domain1
		addUserToDomain("domain1", "Joe", "Doe", "joe@joe.com", 0, 1);
		addUserToDomain("domain1", "John", "Smith", "johnsmith@johnsmith.com", 0, 2);
		addUserToDomain("domain1", "John", "Tackie", "johntackie@johntackie.com", 0, 3);

		//Add "John White" and "Joe Black" to domain2
		addUserToDomain("domain2", "John", "White", "email@white.com", 0, 4);
		addUserToDomain("domain2", "Joe", "Black", "email@black.com", 0, 5);

		//After typing in "j" into domain1, you should see only
		//see domain1's guys
		MatchedUsers result = service.autocomplete(rpcController, getAutocompleteRequest("domain1", "j", 5));
		Assert.assertEquals("Did not get the correct number of entries back in autocomplete", 3, result.getMatchedUsersCount());

		result = service.autocomplete(rpcController, getAutocompleteRequest("domain1", "joh", 5));
		Assert.assertEquals("Did not get the correct number of entries back in autocomplete", 2, result.getMatchedUsersCount());

		//Make sure that if I only ask for 2, I only get 2
		result = service.autocomplete(rpcController, getAutocompleteRequest("domain1", "j", 2));
		Assert.assertEquals("Did not get the correct number of entries back in autocomplete", 2, result.getMatchedUsersCount());

	}

	@Test
	public void updateUserTest() throws ServiceException {

		//Clear the domains we'll be using
		clearUsersFromDomains("domain1", "domain2");

		//Add "Joe Doe", "John Smith", and "John Tackie" to domain1
		addUserToDomain("domain1", "Joe", "Doe", "joe@joe.com", 0, 1);
		addUserToDomain("domain1", "John", "Smith", "johnsmith@johnsmith.com", 0, 2);

		//After typing in "j" into domain1, you should see both
		MatchedUsers result = service.autocomplete(rpcController, getAutocompleteRequest("domain1", "j", 5));
		Assert.assertEquals("Did not get the correct number of entries back in autocomplete", 2, result.getMatchedUsersCount());

		//Make sure we have a joe doe 1 and a john smith 2
		Set<String> returnedUsers = new HashSet<String>();
		for(MatchedUser matchedUser : result.getMatchedUsersList()) {
			User user = matchedUser.getUser();
			returnedUsers.add(user.getFirstName() +  " " + user.getLastName() + " " + user.getUid() + " " + user.getEmail());
		}
		Assert.assertTrue(returnedUsers.contains("Joe Doe 1 joe@joe.com"));
		Assert.assertTrue(returnedUsers.contains("John Smith 2 johnsmith@johnsmith.com"));

		//Update Joe to be Moe
		updateUser("domain1", "Moe", "Doe", "joe@joe.com", 0, 1);

		//Try everything again
		//After typing in "j" into domain1, you should see both
		result = service.autocomplete(rpcController, getAutocompleteRequest("domain1", "j", 5));
		Assert.assertEquals("Did not get the correct number of entries back in autocomplete", 1, result.getMatchedUsersCount());

		//Make sure we have ONLY and a john smith 2
		returnedUsers = new HashSet<String>();
		for(MatchedUser matchedUser : result.getMatchedUsersList()) {
			returnedUsers.add(toString(matchedUser.getUser()));
		}
		Assert.assertTrue(returnedUsers.contains("John Smith 2 johnsmith@johnsmith.com"));

	}

	@Test
	public void highlightIndexTest() throws ServiceException {

		//Only using domain 1
		clearUsersFromDomains("domain1");

		addUserToDomain("domain1", "SriHari", "eSeshadri", "hseshadri@gmail.com", 0, 1);
		addUserToDomain("domain1", "Hari", "Seshadri", "hari.seshadri@gmail.com", 0, 2);
		addUserToDomain("domain1", "Harmony", "Ling", "hling@gmail.com", 0, 3);

		String firstUserToString = "SriHari eSeshadri 1 hseshadri@gmail.com";
		String secondUserToString = "Hari Seshadri 2 hari.seshadri@gmail.com";
		String thirdUserToString = "Harmony Ling 3 hling@gmail.com";

		//----------------
		//Typing "s"
		//----------------

		{
			//Typing "s" should match srihari's first name, hari's last name
			MatchedUsers result = service.autocomplete(rpcController, getAutocompleteRequest("domain1", "s", 5));
			Assert.assertEquals("Did not get the correct number of entries back in autocomplete", 2, result.getMatchedUsersCount());

			Map<String, Pair<List<HighlightIndices>, List<HighlightIndices>>> results = new HashMap<String, Pair<List<HighlightIndices>, List<HighlightIndices>>>();
			for(MatchedUser matchedUser : result.getMatchedUsersList()) {
				results.put(toString(matchedUser.getUser()), Pair.of(matchedUser.getFirstNameHighlightsList(), matchedUser.getLastNameHighlightsList()));
			}

			//The first letter of the first name & second letter of last name should be selected for srihari
			Pair<List<HighlightIndices>, List<HighlightIndices>> firstAndLastMatches = results.get(firstUserToString);
			List<HighlightIndices> srihariFirstNameMatches = firstAndLastMatches.getFirst();
			List<HighlightIndices> srihariLastNameMatches = firstAndLastMatches.getSecond();
			Assert.assertEquals(1, srihariFirstNameMatches.size());
			Assert.assertEquals(2, srihariLastNameMatches.size());

			//First letter of first name should be highlighted
			HighlightIndices firstNameMatch = srihariFirstNameMatches.get(0);
			Assert.assertEquals(Pair.of(0, 1), Pair.of(firstNameMatch.getStart(), firstNameMatch.getEnd()));

			//Second letter of last name should be highlighted
			HighlightIndices lastNameMatch1 = srihariLastNameMatches.get(0);
			HighlightIndices lastNameMatch2 = srihariLastNameMatches.get(1);
			Assert.assertEquals(Pair.of(1, 2), Pair.of(lastNameMatch1.getStart(), lastNameMatch1.getEnd()));
			Assert.assertEquals(Pair.of(3, 4), Pair.of(lastNameMatch2.getStart(), lastNameMatch2.getEnd()));


			//The first letter of last name should be highlighted for hari
			firstAndLastMatches = results.get(secondUserToString);
			List<HighlightIndices> hariFirstNameMatches = firstAndLastMatches.getFirst();
			List<HighlightIndices> hariLastNameMatches = firstAndLastMatches.getSecond();
			Assert.assertEquals(0, hariFirstNameMatches.size());
			Assert.assertEquals(2, hariLastNameMatches.size());

			//First letter of last name should be highlighted
			lastNameMatch1 = hariLastNameMatches.get(0);
			lastNameMatch2 = hariLastNameMatches.get(1);
			Assert.assertEquals(Pair.of(0, 1), Pair.of(lastNameMatch1.getStart(), lastNameMatch1.getEnd()));
			Assert.assertEquals(Pair.of(2, 3), Pair.of(lastNameMatch2.getStart(), lastNameMatch2.getEnd()));

		}

		//------------------
		//Typing "hAr"
		//------------------
		{
			MatchedUsers result = service.autocomplete(rpcController, getAutocompleteRequest("domain1", "hAr", 5));
			Assert.assertEquals("Did not get the correct number of entries back in autocomplete", 3, result.getMatchedUsersCount());

			Map<String, Pair<List<HighlightIndices>, List<HighlightIndices>>> results = new HashMap<String, Pair<List<HighlightIndices>, List<HighlightIndices>>>();
			for(MatchedUser matchedUser : result.getMatchedUsersList()) {
				results.put(toString(matchedUser.getUser()), Pair.of(matchedUser.getFirstNameHighlightsList(), matchedUser.getLastNameHighlightsList()));
			}

			//The first name should have "har" selected in it for srihari
			Pair<List<HighlightIndices>, List<HighlightIndices>> firstAndLastMatches = results.get(firstUserToString);
			List<HighlightIndices> srihariFirstNameMatches = firstAndLastMatches.getFirst();
			List<HighlightIndices> srihariLastNameMatches = firstAndLastMatches.getSecond();
			Assert.assertEquals(1, srihariFirstNameMatches.size());
			Assert.assertEquals(0, srihariLastNameMatches.size());

			//First letter of first name should be highlighted
			HighlightIndices firstNameMatch = srihariFirstNameMatches.get(0);
			Assert.assertEquals(Pair.of(3, 6), Pair.of(firstNameMatch.getStart(), firstNameMatch.getEnd()));

			//The first name should have "har" selected in it for hari
			firstAndLastMatches = results.get(secondUserToString);
			List<HighlightIndices> hariFirstNameMatches = firstAndLastMatches.getFirst();
			List<HighlightIndices> hariLastNameMatches = firstAndLastMatches.getSecond();
			Assert.assertEquals(1, hariFirstNameMatches.size());
			Assert.assertEquals(0, hariLastNameMatches.size());

			//First letter of first name should be highlighted
			firstNameMatch = hariFirstNameMatches.get(0);
			Assert.assertEquals(Pair.of(0, 3), Pair.of(firstNameMatch.getStart(), firstNameMatch.getEnd()));

			//The first name should have "har" selected in it for harmony
			firstAndLastMatches = results.get(thirdUserToString);
			List<HighlightIndices> harmonyFirstNameMatches = firstAndLastMatches.getFirst();
			List<HighlightIndices> harmonyLastNameMatches = firstAndLastMatches.getSecond();
			Assert.assertEquals(1, harmonyFirstNameMatches.size());
			Assert.assertEquals(0, harmonyLastNameMatches.size());

			//First letter of first name should be highlighted
			firstNameMatch = harmonyFirstNameMatches.get(0);
			Assert.assertEquals(Pair.of(0, 3), Pair.of(firstNameMatch.getStart(), firstNameMatch.getEnd()));
		}

		//------------------
		//Typing "ri ri"
		//------------------
		{
			//Typing "ri ri" should match srihari's first/last name, hari's first/last name
			MatchedUsers result = service.autocomplete(rpcController, getAutocompleteRequest("domain1", "ri ri", 2));
			Assert.assertEquals("Did not get the correct number of entries back in autocomplete", 2, result.getMatchedUsersCount());

			Map<String, Pair<List<HighlightIndices>, List<HighlightIndices>>> results = new HashMap<String, Pair<List<HighlightIndices>, List<HighlightIndices>>>();
			for(MatchedUser matchedUser : result.getMatchedUsersList()) {
				results.put(toString(matchedUser.getUser()), Pair.of(matchedUser.getFirstNameHighlightsList(), matchedUser.getLastNameHighlightsList()));
			}

			Pair<List<HighlightIndices>, List<HighlightIndices>> firstAndLastMatches = results.get(firstUserToString);
			List<HighlightIndices> srihariFirstNameMatches = firstAndLastMatches.getFirst();
			List<HighlightIndices> srihariLastNameMatches = firstAndLastMatches.getSecond();
			Assert.assertEquals(2, srihariFirstNameMatches.size());
			Assert.assertEquals(1, srihariLastNameMatches.size());

			HighlightIndices firstNameMatch1 = srihariFirstNameMatches.get(0);
			HighlightIndices firstNameMatch2 = srihariFirstNameMatches.get(1);
			Assert.assertEquals(Pair.of(1, 3), Pair.of(firstNameMatch1.getStart(), firstNameMatch1.getEnd()));
			Assert.assertEquals(Pair.of(5, 7), Pair.of(firstNameMatch2.getStart(), firstNameMatch2.getEnd()));

			HighlightIndices lastNameMatch = srihariLastNameMatches.get(0);
			Assert.assertEquals(Pair.of(7, 9), Pair.of(lastNameMatch.getStart(), lastNameMatch.getEnd()));

			firstAndLastMatches = results.get(secondUserToString);
			List<HighlightIndices> hariFirstNameMatches = firstAndLastMatches.getFirst();
			List<HighlightIndices> hariLastNameMatches = firstAndLastMatches.getSecond();
			Assert.assertEquals(1, hariFirstNameMatches.size());
			Assert.assertEquals(1, hariLastNameMatches.size());

			firstNameMatch1 = hariFirstNameMatches.get(0);
			Assert.assertEquals(Pair.of(2, 4), Pair.of(firstNameMatch1.getStart(), firstNameMatch1.getEnd()));

			lastNameMatch = hariLastNameMatches.get(0);
			Assert.assertEquals(Pair.of(6, 8), Pair.of(lastNameMatch.getStart(), lastNameMatch.getEnd()));
		}
	}

	@Test
	public void testEmailAutocomplete() {

	}

	@Test
	public void testTimesRecommendedWorking() {

	}

	@Test
	public void cannotAddTwoUsersOfSameUID() throws ServiceException {
		clearUsersFromDomains("domain1");
		addUserToDomain("domain1", "Joe", "Doe", "joe@joe.com", 0, 1);
		User clone = makeUser("domain1", "Joe", "Doe", "joe@joe.com", 0, 1);
		Assert.assertNotSame(200, service.addUser(rpcController, clone).getStatusCode());
	}

	@Test
	public void cannotUpdateNonExistentUser() throws ServiceException {
		clearUsersFromDomains("domain1");
		User user = makeUser("domain1", "Joe", "Doe", "joe@joe.com", 0, 1);
		Assert.assertNotSame(200, service.update(rpcController, user).getStatusCode());
	}

	@Test
	public void cannotRemoveNonExistentUser() throws ServiceException {
		clearUsersFromDomains("domain1");
		DeleteRequest.Builder deleteBuilder = DeleteRequest.newBuilder();
		deleteBuilder.setDomain("domain1");
		deleteBuilder.setUid(11);
		Assert.assertNotSame(200, service.delete(rpcController, deleteBuilder.build()).getStatusCode());

		addUserToDomain("domain1", "SriHari", "eSeshadri", "hseshadri@gmail.com", 0, 1);
		Assert.assertNotSame(200, service.delete(rpcController, deleteBuilder.build()).getStatusCode());

		addUserToDomain("domain1", "SriHari", "eSeshadri", "hseshadri@gmail.com", 0, 11);
		Assert.assertEquals(200, service.delete(rpcController, deleteBuilder.build()).getStatusCode());

	}

	@Test
	public void testBatchAdd() {

	}

	/***
	 * Helper to generate an autocomplete request used in RPC
	 *
	 * @param domain The domain for the autocomplete
	 * @param typedIn The string which is going to get autocompleted
	 * @param numResponses The number of responses you want back
	 * @return The built autocomplete request
	 */
	private AutocompleteRequest getAutocompleteRequest(String domain, String typedIn, int numResponses) {
		AutocompleteRequest.Builder builder = AutocompleteRequest.newBuilder();
		builder.setDomain(domain);
		builder.setTyped(typedIn);
		builder.setNumResponses(numResponses);

		return builder.build();
	}

	/***
	 * Helper which creates a user
	 *
	 * @param domain The domain of user
	 * @param firstName The first name of the user
	 * @param lastName The last name of the user
	 * @param email The email address of the user
	 * @param timesRecommended The number of times this user has been recommended
	 * @param uid The UID of the user
	 */
	private User makeUser(String domain, String firstName, String lastName, String email, int timesRecommended, long uid){
		User.Builder userBuilder = User.newBuilder();

		userBuilder.setUid(uid);
		userBuilder.setFirstName(firstName);
		userBuilder.setLastName(lastName);
		userBuilder.setDomain(domain);
		userBuilder.setEmail(email);
		userBuilder.setTimesRecommended(timesRecommended);

		return userBuilder.build();
	}


	/***
	 * Helper which adds a user to the server via RPC
	 *
	 * @param domain The domain to add the user to
	 * @param firstName The first name of the user
	 * @param lastName The last name of the user
	 * @param email The email address of the user
	 * @param timesRecommended The number of times this user has been recommended
	 * @param uid The UID of the user
	 *
	 * @throws ServiceException If the call fails
	 */
	private void addUserToDomain(String domain, String firstName, String lastName, String email, int timesRecommended, long uid) throws ServiceException {
		User user = makeUser(domain, firstName, lastName, email, timesRecommended, uid);
		Assert.assertEquals("User was not added successfully", 200, service.addUser(rpcController, user).getStatusCode());
	}

	/***
	 * Helper which updates a user via RPC
	 *
	 * @param domain The domain to which the user lies in
	 * @param firstName The new first name of the user
	 * @param lastName The new last name of the user
	 * @param email The new email address of the user
	 * @param timesRecommended The new number of times this user has been recommended
	 * @param uid The UID of the user
	 *
	 * @throws ServiceException If the call fails
	 */
	private void updateUser(String domain, String firstName, String lastName, String email, int timesRecommended, long uid) throws ServiceException {
		User.Builder userBuilder = User.newBuilder();

		userBuilder.setUid(uid);
		userBuilder.setFirstName(firstName);
		userBuilder.setLastName(lastName);
		userBuilder.setDomain(domain);
		userBuilder.setEmail(email);
		userBuilder.setTimesRecommended(timesRecommended);

		Assert.assertEquals("User was not updated successfully", 200, service.update(rpcController, userBuilder.build()).getStatusCode());
	}
	/***
	 * Clears all of the users associated with the given domains
	 *
	 * @param domains The domains to clear the users from
	 * @throws ServiceException If the RPC fails
	 */
	private void clearUsersFromDomains(final String... domains) throws ServiceException {
		ClearRequest.Builder clearBuilder = ClearRequest.newBuilder();

		for(String domain : domains) {
			clearBuilder.addDomains(domain);
		}

		Assert.assertEquals("Could not clear domains: " + Lists.newArrayList(domains), 200, service.clearUsers(rpcController, clearBuilder.build()).getStatusCode());
	}

	/***
	 * Helper to toString a user.
	 * Remember that user is a generated class so can't
	 * add an intelligent tostring there
	 *
	 * @param user The user to serialize
	 * @return String representation of the user
	 */
	private String toString(User user) {
		return user.getFirstName() +  " " + user.getLastName() + " " + user.getUid() + " " + user.getEmail();
	}

}
