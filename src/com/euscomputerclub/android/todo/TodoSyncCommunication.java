package com.euscomputerclub.android.todo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Class TodoSyncIO handles the synchronization communication between the
 * local database and the remote database.
 *
 * @author Tadeus Prastowo
 */
public class TodoSyncCommunication
{
	/** The socket address of the sync server. */
	protected SocketAddress daddr;
	/** The communication socket. */
	protected DatagramSocket sock;

	/** The type of a register packet. */
	protected static final byte REGISTER_TYPE = 1;
	/** The size of a register packet. */
	protected static final int REGISTER_LEN = 40;
	/** The type of a register packet. */
	protected static final byte REGISTER_ACK_TYPE = 2;
	/** The size of a register acknowledgement packet. */
	protected static final int REGISTER_ACK_LEN = 8;
	/** The registration timeout in millisecond. */
	protected static final int REGISTER_TIMEOUT = 1000;
	/** The maximum number of registration retries. */
	protected static final int MAX_REGISTER_RETRY = 5;

	/** The type of a server-to-client sync request packet. */
	protected static final byte SERVER_CLIENT_SYNC_TYPE = 3;
	/** The size of a server-to-client sync request packet. */
	protected static final int SERVER_CLIENT_SYNC_LEN = 8;
	/** The server-to-client sync request timeout in millisecond. */
	protected static final int SERVER_CLIENT_SYNC_TIMEOUT = 1000;

	/** The type of a server-to-client sync response packet. */
	protected static final byte SERVER_CLIENT_RESP_TYPE = 4;
	/** The size of a server-to-client sync response packet. */
	protected static final int SERVER_CLIENT_RESP_LEN = 40;

	/** The type of a start server-to-client sync packet. */
	protected static final byte SERVER_CLIENT_RESP_ACK_TYPE = 5;
	/** The size of a start server-to-client sync packet. */
	protected static final int SERVER_CLIENT_RESP_ACK_LEN = 8;
	/** The start server-to-client sync timeout in millisecond. */
	protected static final int SERVER_CLIENT_RESP_ACK_TIMEOUT = 10000;

	/** The type of a client-to-server sync request packet. */
	protected static final byte CLIENT_SERVER_SYNC_TYPE = 6;
	/** The size of a client-to-server sync request packet. */
	protected static final int CLIENT_SERVER_SYNC_LEN = 40;
	/** The client-to-server sync request timeout in millisecond. */
	protected static final int CLIENT_SERVER_SYNC_TIMEOUT = 1000;

	/** The type of a start client-to-server sync response packet. */
	protected static final byte CLIENT_SERVER_RESP_TYPE = 7;
	/** The size of a client-to-server sync response packet. */
	protected static final int CLIENT_SERVER_RESP_LEN = 8;

	/** The type of a start client-to-server sync complete packet. */
	protected static final byte CLIENT_SERVER_RESP_ACK_TYPE = 8;
	/** The size of a client-to-server sync complete packet. */
	protected static final int CLIENT_SERVER_RESP_ACK_LEN = 8;
	/** The client-to-server response acknowledgement timeout in millisecond. */
	protected static final int CLIENT_SERVER_RESP_ACK_TIMEOUT = 10000;

	/** The type of a reset packet. */
	protected static final byte RESET_TYPE = 9;
	/** The size of a reset packet. */
	protected static final int RESET_LEN = 8;
	/** The type of a reset acknowledgement packet. */
	protected static final byte RESET_ACK_TYPE = 10;
	/** The size of a reset acknowledgement packet. */
	protected static final int RESET_ACK_LEN = 8;
	/** The reset timeout in millisecond. */
	protected static final int RESET_TIMEOUT = 1000;

	/** The type of a server-to-client data packet. */
	protected static final byte SERVER_CLIENT_DATA_TYPE = 11;
	/** The type of a client-to-server data packet. */
	protected static final byte CLIENT_SERVER_DATA_TYPE = 12;

	/** The size of a chunk. */
	protected static final int CHUNK_LEN = 24;
	/** The type of a todo chunk. */
	protected static final int CHUNK_TODO_TYPE = 4;

	/**
	 * ClientServerSyncData represents the todo sync data to be transfered
	 * to the sync server.
	 */
	public class ClientServerSyncData
	{
		final ByteBuffer buffer;

		public ClientServerSyncData(int capacity) {

			buffer = ByteBuffer.allocate(capacity + 1).put(CLIENT_SERVER_DATA_TYPE);
		}

		public ClientServerSyncData(byte[] array) {

			byte[] b = new byte[array.length + 1];
			System.arraycopy(array, 0, b, 1, array.length);
			buffer = ByteBuffer.wrap(b).put(CLIENT_SERVER_DATA_TYPE);
		}

		public ClientServerSyncData(byte[] array, int start, int len) {

			byte[] b = new byte[len + 1];
			System.arraycopy(array, start, b, 1, len);
			buffer = ByteBuffer.wrap(b).put(CLIENT_SERVER_DATA_TYPE);
		}
	}

	/** Constructs the sync communication. */
	public TodoSyncCommunication() throws SocketException {

		daddr = new InetSocketAddress("10.0.2.2", 50001);
		sock = new DatagramSocket();
		sock.connect(daddr);
	}

	/**
	 * Returns a byte buffer with a certain size whose order is in network
	 * byte order.
	 */
	protected ByteBuffer getBuffer(int length) {

		return ByteBuffer.allocate(length);
	}

	/**
	 * Authenticates AndroidTodo to the sync server based on the user ID.
	 *
	 * @param clientId the ID of the user
	 *
	 * @return true if the registration is successful or false if it is not
	 *              because the server does not respond after certain number
	 *              of retries indicating that the server does not want to
	 *              maintain a new user
	 */
	public boolean register(int clientId) throws SocketException, IOException {

		ByteBuffer b = getBuffer(REGISTER_LEN);
		b.put(REGISTER_TYPE);
		b.putInt(clientId);

		ByteBuffer a = null;
		for (int i = 0; i < MAX_REGISTER_RETRY && (a == null || a.get(0) != REGISTER_ACK_TYPE); i++) {

			a = sendAndReceive(b, REGISTER_TIMEOUT, REGISTER_ACK_LEN);
		}

		return a.get(0) == REGISTER_ACK_TYPE;
	}

	/** Requests the sync server to send the todo items and returns them. */
	public ByteBuffer serverClientSync() throws SocketException, IOException {

		ByteBuffer b = getBuffer(SERVER_CLIENT_SYNC_LEN);
		b.put(SERVER_CLIENT_SYNC_TYPE);

		ByteBuffer a = null;
		while (a == null || a.get() != SERVER_CLIENT_RESP_TYPE) {

			a = sendAndReceive(b, SERVER_CLIENT_SYNC_TIMEOUT, SERVER_CLIENT_RESP_LEN);
		}

		int dataLen = a.getInt();
		a = null;
		b = getBuffer(SERVER_CLIENT_RESP_ACK_LEN);
		b.put(SERVER_CLIENT_RESP_ACK_TYPE);
		while (a == null || a.get() != SERVER_CLIENT_DATA_TYPE) {

			a = sendAndReceive(b, SERVER_CLIENT_RESP_ACK_TIMEOUT, dataLen);
		}

		return a;
	}

	/** Updates the sync server. */
	public void clientServerSync(ClientServerSyncData data) throws SocketException, IOException {

		ByteBuffer b = getBuffer(CLIENT_SERVER_SYNC_LEN);
		b.put(CLIENT_SERVER_SYNC_TYPE);
		int dataLen = data.buffer.array().length;
		b.putInt(dataLen);

		ByteBuffer a = null;
		while (a == null || a.get() != CLIENT_SERVER_RESP_TYPE) {

			a = sendAndReceive(b, CLIENT_SERVER_SYNC_TIMEOUT, CLIENT_SERVER_RESP_LEN);
		}

		a = null;
		while (a == null || a.get() != CLIENT_SERVER_RESP_ACK_TYPE) {

			a = sendAndReceive(data.buffer, CLIENT_SERVER_RESP_ACK_TIMEOUT, CLIENT_SERVER_RESP_ACK_LEN);
		}
	}

	/** Tears down the connection to the sync server. */
	public void close() throws SocketException, IOException {

		ByteBuffer b = getBuffer(RESET_LEN);
		b.put(RESET_TYPE);

		ByteBuffer a = null;
		while (a == null || a.get() != RESET_ACK_TYPE) {

			a = sendAndReceive(b, RESET_TIMEOUT, RESET_ACK_LEN);
		}

		sock.close();
	}

	/** Sends the given data and returns the received data. */
	protected ByteBuffer sendAndReceive(ByteBuffer data, int timeout, int receivedDataLen) throws SocketException, IOException {

		if (timeout != 0) {
			sock.setSoTimeout(timeout);
		}
		if (data != null) {

			byte[] b = data.array();

			sock.send(new DatagramPacket(b, b.length, daddr));
		}
		DatagramPacket d = new DatagramPacket(new byte[receivedDataLen], receivedDataLen);
		while (d.getLength() != receivedDataLen) {

			sock.receive(d);
		}
		if (timeout != 0) {
			sock.setSoTimeout(0);
		}

		return ByteBuffer.wrap(d.getData());
	}
}
