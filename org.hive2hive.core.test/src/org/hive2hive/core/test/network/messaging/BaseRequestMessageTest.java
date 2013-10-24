package org.hive2hive.core.test.network.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Random;

import org.hive2hive.core.network.NetworkManager;
import org.hive2hive.core.network.messages.direct.response.ResponseMessage;
import org.hive2hive.core.network.messages.request.IRequestMessage;
import org.hive2hive.core.network.messages.request.callback.ICallBackHandler;
import org.hive2hive.core.test.network.NetworkTestUtil;
import org.hive2hive.core.test.network.data.TestDataWrapper;
import org.hive2hive.core.test.network.messaging.TestMessageWithReply.TestCallBackHandler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests asynchronous request messages where the receiver node has to respond with a response message which
 * gets handled by a callback handler.
 * 
 * @author Seppi
 */
public class BaseRequestMessageTest extends NetworkJUnitTest {

	private static List<NetworkManager> network;
	private static final int networkSize = 10;
	private static Random random = new Random();

	@BeforeClass
	public static void initTest() throws Exception {
		testClass = BaseRequestMessageTest.class;
		beforeClass();
	}

	@Override
	@Before
	public void beforeMethod() {
		super.beforeMethod();
		network = NetworkTestUtil.createNetwork(networkSize);
	}

	/**
	 * Test if a asynchronous message (implementing the {@link IRequestMessage}) interface gets
	 * properly handled. To verify this node A sends to node B a randomly created contentKey. Node B generates
	 * a random secret. The secret gets locally stored (location key is node B's node id) and sent back
	 * with a {@link ResponseMessage} to node A. A callback handler implementing {@link ICallBackHandler} at
	 * node A handles the received response message and also locally puts the received secret into the DHT
	 * (location key is node A's node id). Every think went right when under both location keys the same data
	 * appears.
	 */
	@Test
	public void testSendingAnAsynchronousMessageWithReply() {
		// select two random nodes
		NetworkManager nodeA = network.get(random.nextInt(networkSize / 2));
		NetworkManager nodeB = network.get(random.nextInt(networkSize / 2) + networkSize / 2);
		// generate a random content key
		String contentKey = NetworkTestUtil.randomString();
		// check if selected locations are empty
		assertNull(nodeA.getLocal(nodeA.getNodeId(), contentKey));
		assertNull(nodeB.getLocal(nodeB.getNodeId(), contentKey));
		// create a message with target node B
		TestMessageWithReply message = new TestMessageWithReply(nodeB.getNodeId(), nodeA.getPeerAddress(),
				contentKey);
		// create and add a callback handler
		TestCallBackHandler callBackHandler = message.new TestCallBackHandler(nodeA);
		message.setCallBackHandler(callBackHandler);
		// send message
		nodeA.send(message);

		// wait till callback handler gets executed
		Waiter w = new Waiter(10);
		Object tmp = null;
		do {
			w.tickASecond();
			tmp = nodeA.getLocal(nodeA.getNodeId(), contentKey);
		} while (tmp == null);

		// load and verify if same secret was shared
		String receivedSecret = ((TestDataWrapper) tmp).getTestString();
		String originalSecret = ((TestDataWrapper) nodeB.getLocal(nodeB.getNodeId(), contentKey))
				.getTestString();

		assertEquals(originalSecret, receivedSecret);
	}

	@Override
	@After
	public void afterMethod() {
		NetworkTestUtil.shutdownNetwork(network);
		super.afterMethod();
	}

	@AfterClass
	public static void endTest() {
		afterClass();
	}

}