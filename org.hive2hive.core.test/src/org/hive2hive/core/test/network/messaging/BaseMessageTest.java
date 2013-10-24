package org.hive2hive.core.test.network.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Random;

import org.hive2hive.core.network.NetworkManager;
import org.hive2hive.core.test.network.NetworkTestUtil;
import org.hive2hive.core.test.network.data.TestDataWrapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseMessageTest extends NetworkJUnitTest {

	private static List<NetworkManager> network;
	private static final int networkSize = 10;
	private static Random random = new Random();

	@BeforeClass
	public static void initTest() throws Exception {
		testClass = BaseMessageTest.class;
		beforeClass();
	}

	@Override
	@Before
	public void beforeMethod() {
		super.beforeMethod();
		network = NetworkTestUtil.createNetwork(networkSize);
	}

	/**
	 * This test checks if a message arrives asynchronously at the right target node (node which is
	 * responsible for the given key). To verify this the receiving node stores the received data into the
	 * DHT. Everything went right when same data is found with the node id of the receiving node as location
	 * key.
	 */
	@Test
	public void testSendingAnAsynchronousMessageWithNoReplyToTargetNode() {
		// select two random nodes
		NetworkManager nodeA = network.get(random.nextInt(networkSize / 2));
		NetworkManager nodeB = network.get(random.nextInt(networkSize / 2) + networkSize / 2);
		// generate random data and content key
		String data = NetworkTestUtil.randomString();
		String contentKey = NetworkTestUtil.randomString();
		// check if selected location is empty
		assertNull(nodeB.getLocal(nodeB.getNodeId(), contentKey));
		// create a message with target node B
		TestMessageOneWay message = new TestMessageOneWay(nodeB.getNodeId(), contentKey, new TestDataWrapper(
				data));
		// send message
		nodeA.send(message);

		// wait till message gets handled
		Waiter w = new Waiter(20);
		Object tmp = null;
		do {
			w.tickASecond();
			tmp = nodeB.getLocal(nodeB.getNodeId(), contentKey);
		} while (tmp == null);

		// verify that data arrived
		String result = ((TestDataWrapper) tmp).getTestString();
		assertNotNull(result);
		assertEquals(data, result);
	}

	/**
	 * This tests sends a message asynchronously to the target node. The message is configured in a way that
	 * it will be blocked on the target node till the maximum allowed numbers of retrying to send the very
	 * message is reached. At this stage the message is accepted and executed on the target node.
	 */
	@Test
	public void testSendingAnAsynchronousMessageWithNoReplyMaxTimesTargetNode() {
		// select two random nodes
		NetworkManager nodeA = network.get(random.nextInt(networkSize / 2));
		NetworkManager nodeB = network.get(random.nextInt(networkSize / 2) + networkSize / 2);
		// generate random data and content key
		String contentKey = NetworkTestUtil.randomString();
		String data = NetworkTestUtil.randomString();
		// check if selected location is empty
		assertNull(nodeB.getLocal(nodeB.getNodeId(), contentKey));
		// create a test message which gets rejected several times
		TestMessageOneWayMaxSending message = new TestMessageOneWayMaxSending(nodeB.getNodeId(), contentKey,
				new TestDataWrapper(data));
		// send message
		nodeA.send(message);

		// wait till message gets handled
		// this might need some time 
		Waiter w = new Waiter(20);
		Object tmp = null;
		do {
			w.tickASecond();
			tmp = nodeB.getLocal(nodeB.getNodeId(), contentKey);
		} while (tmp == null);

		// verify that data arrived
		String result = ((TestDataWrapper) tmp).getTestString();
		assertNotNull(result);
		assertEquals(data, result);
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