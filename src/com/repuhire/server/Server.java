package com.repuhire.server;

import java.util.concurrent.Executors;

import com.googlecode.protobuf.socketrpc.RpcServer;
import com.googlecode.protobuf.socketrpc.ServerRpcConnectionFactory;
import com.googlecode.protobuf.socketrpc.SocketRpcConnectionFactories;
import com.repuhire.autocompleter.AutocompleterService;
import com.repuhire.common.Common.Autocompleter;

public class Server {

	public static int port = 12345;
	public static int threadPoolSize = 10;

	public static void main(String[] args) {
		ServerRpcConnectionFactory rpcConnectionFactory = SocketRpcConnectionFactories.createServerRpcConnectionFactory(port);
		RpcServer server = new RpcServer(rpcConnectionFactory, Executors.newFixedThreadPool(threadPoolSize), true);

		//TODO why do I have to register two services?
		//Should I only have to put the logic in once (say the blocking version)
		//and when invoked the the client can choose to run it blocking or non?
//		server.registerService(new AutocompleterService()); // For non-blocking impl
		server.registerBlockingService(Autocompleter.newReflectiveBlockingService(AutocompleterService.getInstance())); // For blocking impl
		server.run();
	}

}
