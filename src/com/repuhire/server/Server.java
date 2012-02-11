package com.repuhire.server;

import java.util.concurrent.Executors;

import com.googlecode.protobuf.socketrpc.RpcServer;
import com.googlecode.protobuf.socketrpc.ServerRpcConnectionFactory;
import com.googlecode.protobuf.socketrpc.SocketRpcConnectionFactories;
import com.repuhire.autocompleter.AutocompleterService;
import com.repuhire.common.Common.Autocompleter;

/***
 * Main class which serves
 */
public class Server {

	public static void main(String[] args) {
		int port = 12345;
		int threadPoolSize = 10;

		if(args.length != 0 && args.length != 2) {
			System.out.println("Takes two arguments: <PORT> <THREAD_POOL_SIZE>\nIf no arguments are specified, port default is: " + port + "\nthread pool size default is: " + threadPoolSize);
			return;
		}

		if(args.length == 2) {
			port = Integer.parseInt(args[0]);
			threadPoolSize = Integer.parseInt(args[1]);
		}

		System.out.println("Using port " + port + " with thread pool size " + threadPoolSize);
		start(port, threadPoolSize);

	}

	/***
	 * Starts the server
	 *
	 * @param port The port to listen on
	 * @param threadPoolSize The thread pool size
	 */
	public static void start(int port, int threadPoolSize) {
		ServerRpcConnectionFactory rpcConnectionFactory = SocketRpcConnectionFactories.createServerRpcConnectionFactory(port);
		RpcServer server = new RpcServer(rpcConnectionFactory, Executors.newFixedThreadPool(threadPoolSize), true);
		server.registerBlockingService(Autocompleter.newReflectiveBlockingService(AutocompleterService.getInstance())); // For blocking impl
		server.run();
	}

}
