package com.repuhire.server.test;

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
import com.repuhire.common.Common.MatchedUsers;
import com.repuhire.common.Common.Status;
import com.repuhire.common.Common.User;
import com.repuhire.server.Server;

public class AutocompleterTest {

	static BlockingInterface service;
	static SocketRpcController rpcController;
	static RpcCallback<Status> mustNotFailCallback;

	//Initialization
	@BeforeClass
	public static void init() {

		// Create channel
		RpcConnectionFactory connectionFactory = SocketRpcConnectionFactories
		    .createRpcConnectionFactory("localhost", Server.port);
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
	public void updateUserTest() {

	}

	@Test
	public void highlightIndexTest() {

	}

	@Test
	public void clearTest() {

	}

	@Test
	public void testEmailAutocomplete() {

	}

	@Test
	public void testTimesRecommendedWorking() {

	}

	@Test
	public void cannotAddTwoUsersOfSameUID() {

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
		User.Builder userBuilder = User.newBuilder();

		userBuilder.setUid(uid);
		userBuilder.setFirstName(firstName);
		userBuilder.setLastName(lastName);
		userBuilder.setDomain(domain);
		userBuilder.setEmail(email);
		userBuilder.setTimesRecommended(timesRecommended);

		Assert.assertEquals("User was not added successfully", 200, service.addUser(rpcController, userBuilder.build()).getStatusCode());
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

}
