package com.repuhire.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcChannel;
import com.googlecode.protobuf.socketrpc.RpcChannels;
import com.googlecode.protobuf.socketrpc.RpcConnectionFactory;
import com.googlecode.protobuf.socketrpc.SocketRpcConnectionFactories;
import com.googlecode.protobuf.socketrpc.SocketRpcController;
import com.repuhire.common.Common.Autocompleter;
import com.repuhire.server.Server;

public class NonBlockingClient {

	public static void main(String[] args) {
		// Create a thread pool
		ExecutorService threadPool = Executors.newFixedThreadPool(1);

		// Create channel
		RpcConnectionFactory connectionFactory = SocketRpcConnectionFactories
		    .createRpcConnectionFactory("localhost", Server.port);
		RpcChannel channel = RpcChannels.newRpcChannel(connectionFactory, threadPool);

		// Call service
		Autocompleter myService = Autocompleter.newStub(channel);
		SocketRpcController rpcController = new SocketRpcController();

		Foo.Builder fooBuilder = Foo.newBuilder();
		fooBuilder.setBar(123);
		fooBuilder.setBaz("baz val");
		fooBuilder.addNames("name1");
		fooBuilder.addNames("name2");

		myService.doFoo(rpcController, fooBuilder.build(),
		    new RpcCallback<FooResponse>() {
		      @Override
			public void run(FooResponse myResponse) {
		        System.out.println("Received Response: " + myResponse);
		      }
		    });

		// Check success
		if (rpcController.failed()) {
		  System.err.println(String.format("Rpc failed %s : %s", rpcController.errorReason(),
		      rpcController.errorText()));
		}
	}

}
