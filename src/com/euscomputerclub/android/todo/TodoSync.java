package com.euscomputerclub.android.todo;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Class TodoSync handles the synchronization between the local database and
 * the remote database.
 *
 * @author Tadeus Prastowo
 */
public class TodoSync extends Thread
{
	/** The user for which the sync will be made. */
	protected int userId;
	/** The sync communication handler. */
	protected TodoSyncCommunication comm;
	/** The activity for displaying the progress dialog. */
	protected Handler handler;
	/** Conflict resolutions. */
	public static enum ConflictResolution {
		UNDECIDED_YET,
		PICK_LOCAL,
		PICK_REMOTE
	}
	/** User's decision regarding a conflict. */
	protected ConflictResolution conflictResolution = ConflictResolution.UNDECIDED_YET;
	/** Bundle key for local todo item. */
	public static final String LOCAL_TODO = "localTodo";
	/** Bundle key for remote todo item. */
	public static final String REMOTE_TODO = "remoteTodo";
	/** Bundle key for synchronization activity. */
	public static final String MESSAGE = "message";
	/** Bundle key for synchronization error. */
	public static final String ERROR_MESSAGE = "error";
	/** Bundle key for synchronization completion. */
	public static final String DONE = "done";
	/** The todo DB. */
	protected TodoDb db;
	/** The kind of porgress that the worker thread has. */
	public static enum SyncState {
		START,
		REGISTER,
		SERVER_CLIENT,
		SYNC,
		CLIENT_SERVER,
		RESET
	};
	/** The progress of the worker thread. */
	public SyncState syncState = SyncState.START;
	/** The total bytes of received todos that need to be parsed. */
	protected int processTodosTotalBytes;
	/** The number of bytes of received todos that has been parsed. */
	protected int processTodosCurrBytes;
	/** The total records of todos that need to be synchronized. */
	protected int syncTotalRecords;
	/** The number of records of todos that has been synchronized. */
	protected int syncCurrRecords;
	/** The total bytes of synchronized todos that need to be created. */
	protected int createDataTotalBytes;
	/** The number of bytes of synchronized todos that has been created. */
	protected int createDataCurrBytes;
	/** The synchronization worker thread. */
	protected final Thread workerThread = new Thread() {

		/** The size of the TLV chunk header. */
		protected final static short SIZE_OF_CHUNK = (short) ((Byte.SIZE + Short.SIZE) / Byte.SIZE);
		/** A new todo item to be stored. */
		protected final static byte CHUNK_NEW_TODO = 1;
		/** An update for a todo item. */
		protected final static byte CHUNK_UPDATE_TODO = 2;
		/** Delete a todo item. */
		protected final static byte CHUNK_DELETE_TODO = 3;
		/** A todo item as stored in the sync server. */
		protected final static byte CHUNK_TODO = 4;
		/** The ID of a todo. */
		protected final static byte CHUNK_TODO_ID = 5;
		/** The title of a todo. */
		protected final static byte CHUNK_TODO_TITLE = 6;
		/** The deadline of a todo. */
		protected final static byte CHUNK_TODO_DEADLINE = 7;
		/** The priority of a todo. */
		protected final static byte CHUNK_TODO_PRIORITY = 8;
		/** The status of a todo. */
		protected final static byte CHUNK_TODO_STATUS = 9;
		/** The description of a todo. */
		protected final static byte CHUNK_TODO_DESCRIPTION = 10;
		/** The revision of a todo. */
		protected final static byte CHUNK_TODO_REVISION = 11;

		@Override
		public void run() {

			try {
				comm = new TodoSyncCommunication();

				syncState = SyncState.REGISTER;
				if (!comm.register(userId)) {
					sendErrorMessage("Cannot register to server");
					return;
				}

				db.recreateSyncTable();
				processTodos(comm.serverClientSync());

				sync();

				comm.clientServerSync(createSyncData());

				syncState = SyncState.RESET;
				comm.close();

				db.dropSync();
			} catch (Exception e) {

				sendErrorMessage("Exception: " + e.getMessage());
			}
		}

		protected void processTodos (ByteBuffer chunks) {

			processTodosTotalBytes = chunks.limit();
			processTodosCurrBytes = 0;
			syncState = SyncState.SERVER_CLIENT;

			while (chunks.remaining() > 0) {

				long id = -1;
				String title = null;
				String deadline = null;
				int priority = -1;
				String status = null;
				String description = null;
				int revision = -1;

				if (chunks.get() != CHUNK_TODO) {

					throw new IllegalStateException("Invalid chunk data: Expected CHUNK_TODO");
				}

				int nextChunkPos = chunks.getShort();
				nextChunkPos += chunks.position();
				short len;
				byte[] s;
				while (chunks.position() < nextChunkPos) {

					switch (chunks.get())
					{
					case CHUNK_TODO_ID:
						processTodosCurrBytes += chunks.getShort();
						id = chunks.getInt();
						break;
					case CHUNK_TODO_TITLE:
						len = chunks.getShort();
						processTodosCurrBytes += len;
						s = new byte[len];
						chunks.get(s);
						title = new String(s);
						break;
					case CHUNK_TODO_DEADLINE:
						len = chunks.getShort();
						processTodosCurrBytes += len;
						s = new byte[len];
						chunks.get(s);
						deadline = new String(s);
						break;
					case CHUNK_TODO_PRIORITY:
						processTodosCurrBytes += chunks.getShort();
						priority = chunks.getInt();
						break;
					case CHUNK_TODO_STATUS:
						len = chunks.getShort();
						processTodosCurrBytes += len;
						s = new byte[len];
						chunks.get(s);
						status = new String(s);
						break;
					case CHUNK_TODO_DESCRIPTION:
						len = chunks.getShort();
						processTodosCurrBytes += len;
						s = new byte[len];
						chunks.get(s);
						description = new String(s);
						break;
					case CHUNK_TODO_REVISION:
						processTodosCurrBytes += chunks.getShort();
						revision = chunks.getInt();
						break;
					default:
						throw new IllegalStateException(
							"Invalid chunk data:"
							+ " Expected CHUNK_TODO_"
							+ "{ID,TITLE,DEADLINE,PRIORITY,STATUS,DESCRIPTION,REVISION}"
						);
					}
				}

				if (id == -1 || title == null
				    || deadline == null || priority == -1
				    || status == null || description == null
				    || revision == -1) {

					throw new IllegalStateException(
						"Incomplete remote todo"
					);
				}
				db.createSyncTodo(id, title, deadline, priority, status, description, revision);
			}
		}

		protected void sync() {

			Cursor c = db.getAllTodoIncludingDeletedOnes();

			syncTotalRecords = c.getCount();
			syncCurrRecords = 0;
			syncState = SyncState.SYNC;

			while (c.moveToNext()) {

				TodoItem localTodo = new TodoItem(c);
				long localId = localTodo.id.longValue();
				int localRev = localTodo.revision.intValue();

				if (localRev == TodoDb.NEW_TODO_REVISION) { // [A1]

					db.insertToSync(localTodo);
					Log.d("TodoSync", "[A1]");
				} else {

					TodoItem remoteTodo = db.getRemoteTodo(localId);

					if (remoteTodo != null) {

						long remoteId = remoteTodo.id.longValue();
						int remoteRev = remoteTodo.revision.intValue();

						if (db.isLocalDeleted (localId)) {

							if (remoteRev > localRev) { // [A10]

								db.replaceTodo(remoteTodo);
								db.removeSync(remoteId);
								Log.d("TodoSync", "[A10]");
							} else if (remoteRev < localRev) { // [A14]

								db.deleteLocal(localId);  
								db.deleteSync(remoteId);
								Log.d("TodoSync", "[A14]");
							} else {

								if (localTodo.equals(remoteTodo)) { // [A11]

									db.deleteLocal(localId);	  
									db.deleteSync(remoteId);
									Log.d("TodoSync", "[A11]");
								} else {

									switch (resolveLocalDeletion(remoteTodo)) {

									case PICK_LOCAL: // [A13]
										db.deleteLocal(localId);		  
										db.deleteSync(remoteId);
										Log.d("TodoSync", "[A13]");
										break;
									case PICK_REMOTE: // [A12]
										db.replaceTodo(remoteTodo);
										db.removeSync(remoteId);
										Log.d("TodoSync", "[A12]");
										break;
									}
								}
							}
						} else {

							if (localRev > remoteRev) {

								if (localTodo.equals(remoteTodo)) { // [A2]

									db.removeSync(remoteId);
									Log.d("TodoSync", "[A2]");
								} else { // [A3]

									db.updateSync(remoteTodo, localTodo);
									db.updateTodoRevision(localId, localRev + 1);
									Log.d("TodoSync", "[A3]");
								}
							} else if (localRev < remoteRev) { // [A4]

								db.replaceTodo(remoteTodo);
								db.removeSync(remoteId);
								Log.d("TodoSync", "[A4]");
							} else {
								if (localTodo.equals(remoteTodo)) { // [A15]

									db.removeSync(remoteId);
									db.updateTodoRevision(localId, localRev + 1);
									Log.d("TodoSync", "[A15]");
								} else {

									switch (resolveTwoItemsConflict (
										localTodo, remoteTodo)) {

									case PICK_LOCAL: // [A5]
										db.updateTodoRevision(
											localId, localRev + 2);
										db.updateSync(remoteTodo, localTodo);
										Log.d("TodoSync", "[A5]");
										break;
									case PICK_REMOTE: // [A6]
										db.replaceTodo(remoteTodo);
										db.removeSync(remoteId);
										Log.d("TodoSync", "[A6]");
										break;
									}
								}
							}
						}
					} else {

						if (db.isLocalDeleted(localId)) { // [A9]

							db.deleteLocal(localId);
							Log.d("TodoSync", "[A9]");
						} else {

							switch (resolveRemoteDeletion (localTodo)) {

							case PICK_LOCAL: // [A8]
								db.insertToSync(localTodo);
								db.updateTodoRevision(localId, localRev + 1);
								Log.d("TodoSync", "[A8]");
								break;
							case PICK_REMOTE: // [A7]
								db.deleteLocal(localId);
								Log.d("TodoSync", "[A7]");
								break;
							}
						}
					}
				}
			}

			db.adjustNewTodoIdsAndRevisions();
			db.importNewTodos(); // [A16]
		}

		protected TodoSyncCommunication.ClientServerSyncData createSyncData() throws UnsupportedEncodingException {

			/* calculate client_server_data_len */
			createDataTotalBytes = 0;

			// new todo length calculation
			Cursor c = db.getAllNewSyncTodo();
			while (c.moveToNext()) {

				int columnCount = c.getColumnCount();

				createDataTotalBytes += SIZE_OF_CHUNK
					+ 6 * SIZE_OF_CHUNK
					+ c.getString(c.getColumnIndex(TodoDb.TITLE_COLUMN)).getBytes("UTF-8").length
					+ c.getString(c.getColumnIndex(TodoDb.DEADLINE_COLUMN)).getBytes("UTF-8").length
					+ c.getString(c.getColumnIndex(TodoDb.STATUS_COLUMN)).getBytes("UTF-8").length
					+ c.getString(c.getColumnIndex(TodoDb.DESCRIPTION_COLUMN)).getBytes("UTF-8").length
					+ 2 * Integer.SIZE / Byte.SIZE;

				if (c.getInt(c.getColumnIndex(TodoDb.REVISION_COLUMN)) != TodoDb.NEW_TODO_REVISION) {

					createDataTotalBytes += SIZE_OF_CHUNK + Integer.SIZE / Byte.SIZE;
				}
			}
			c.close();

			// deleted todo length calculation
			c = db.getAllDeletedSyncTodo();
			while (c.moveToNext()) {

				createDataTotalBytes += 2 * SIZE_OF_CHUNK + Integer.SIZE / Byte.SIZE;
			}
			c.close();

			// updated todo length calculation
			c = db.getAllUpdatedSyncTodo();
			while (c.moveToNext()) {

				int columnCount = c.getColumnCount();

				createDataTotalBytes += SIZE_OF_CHUNK;
				for (int i = 0; i < columnCount; i++) {

					if (!c.isNull(i)) {

						switch (i) {
						case 0:
						case 3:
						case 6:
							createDataTotalBytes += SIZE_OF_CHUNK + Integer.SIZE / Byte.SIZE;
							break;
						case 1:
						case 2:
						case 4:
						case 5:
							createDataTotalBytes += SIZE_OF_CHUNK
								+ c.getString(i).getBytes("UTF-8").length;
							break;
						}
					}
				}
			}
			c.close();

			createDataCurrBytes = 0;
			syncState = SyncState.CLIENT_SERVER;
			TodoSyncCommunication.ClientServerSyncData data
				= new TodoSyncCommunication.ClientServerSyncData(createDataTotalBytes);
			ByteBuffer b = data.buffer;

			// fill in new todos
			c = db.getAllNewSyncTodo();
			while (c.moveToNext()) {

				int columnCount = c.getColumnCount();
				short chunkLen = 0;

				b.put(CHUNK_NEW_TODO);
				int chunkLenPos = b.position();
				b.putShort((short) 0);
				for (int i = 0; i < columnCount; i++) {

					if (i != 6 || c.getInt(i) != TodoDb.NEW_TODO_REVISION) {

						switch (i) {

						case 0:
							b.put(CHUNK_TODO_ID);
							break;
						case 1:
							b.put(CHUNK_TODO_TITLE);
							break;
						case 2:
							b.put(CHUNK_TODO_DEADLINE);
							break;
						case 3:
							b.put(CHUNK_TODO_PRIORITY);
							break;
						case 4:
							b.put(CHUNK_TODO_STATUS);
							break;
						case 5:
							b.put(CHUNK_TODO_DESCRIPTION);
							break;
						case 6:
							b.put(CHUNK_TODO_REVISION);
							break;
						default:
							throw new IllegalStateException(
								"Invalid index for a CHUNK_NEW_TODO");
						}

						switch (i) {

						case 0:
						case 3:
						case 6:
							b.putShort((short) (Integer.SIZE / Byte.SIZE));

							int intVal = c.getInt(i);
							if (i == 6) {

								intVal = TodoDb.NEW_TODO_REVISION * (intVal + 2);
							}
							b.putInt(intVal);

							chunkLen += SIZE_OF_CHUNK + (short) (Integer.SIZE / Byte.SIZE);
							break;
						case 1:
						case 2:
						case 4:
						case 5:
							byte[] blob = c.getString(i).getBytes("UTF-8");
							b.putShort((short) blob.length);

							b.put(blob);

							chunkLen += SIZE_OF_CHUNK + (short) blob.length;
							break;
						}
					}
				}
				b.putShort(chunkLenPos, chunkLen);
			}
			c.close();

			// fill in deleted todo
			c = db.getAllDeletedSyncTodo();
			while (c.moveToNext()) {

				short idLen = (short) (Integer.SIZE / Byte.SIZE);

				b.put(CHUNK_DELETE_TODO);
				b.putShort((short) (SIZE_OF_CHUNK + idLen));
				b.put(CHUNK_TODO_ID);
				b.putShort(idLen);
				b.putInt(c.getInt(0));
			}
			c.close();

			// fill in updated todo
			c = db.getAllUpdatedSyncTodo();
			while (c.moveToNext()) {

				int columnCount = c.getColumnCount();
				short chunkLen = 0;

				b.put(CHUNK_UPDATE_TODO);
				int chunkLenPos = b.position();
				b.putShort((short) 0);
				for (int i = 0; i < columnCount; i++) {

					if (!c.isNull(i)) {

						switch (i) {

						case 0:
							b.put(CHUNK_TODO_ID);
							break;
						case 1:
							b.put(CHUNK_TODO_TITLE);
							break;
						case 2:
							b.put(CHUNK_TODO_DEADLINE);
							break;
						case 3:
							b.put(CHUNK_TODO_PRIORITY);
							break;
						case 4:
							b.put(CHUNK_TODO_STATUS);
							break;
						case 5:
							b.put(CHUNK_TODO_DESCRIPTION);
							break;
						case 6:
							b.put(CHUNK_TODO_REVISION);
							break;
						default:
							throw new IllegalStateException(
								"Invalid index for a CHUNK_UPDATE_TODO");
						}

						switch (i) {

						case 0:
						case 3:
						case 6:
							b.putShort((short) (Integer.SIZE / Byte.SIZE));

							b.putInt(c.getInt(i));

							chunkLen += SIZE_OF_CHUNK + (short) (Integer.SIZE / Byte.SIZE);
							break;
						case 1:
						case 2:
						case 4:
						case 5:
							byte[] blob = c.getString(i).getBytes("UTF-8");
							b.putShort((short) blob.length);

							b.put(blob);

							chunkLen += SIZE_OF_CHUNK + (short) blob.length;
							break;
						}
					}
				}
				b.putShort(chunkLenPos, chunkLen);
    			}
			c.close();

			return data;
		}
	};

	/** Constructs a TodoSync for a user identified by userId. */
	TodoSync(TodoDb db, Handler todoListHandler, int userId) {

		this.userId = userId;
		handler = todoListHandler;
		this.db = db;
	}

	public void setConflictResolution(ConflictResolution resolution) {

		conflictResolution = resolution;
	}

	protected void sendMessageWithHandler(Bundle b) {

		Message m = handler.obtainMessage();
		m.setData(b);
		handler.sendMessage(m);
	}

	protected ConflictResolution waitForResolution(Bundle b) {

		sendMessageWithHandler(b);

		while (conflictResolution == ConflictResolution.UNDECIDED_YET) {

			yield();
		}
		ConflictResolution result = conflictResolution;
		conflictResolution = ConflictResolution.UNDECIDED_YET;

		return result;
	}

	protected ConflictResolution resolveTwoItemsConflict(TodoItem localTodo, TodoItem remoteTodo) {

		Bundle b = new Bundle();
		b.putParcelable(LOCAL_TODO, localTodo);
		b.putParcelable(REMOTE_TODO, remoteTodo);

		return waitForResolution(b);
	}

	protected ConflictResolution resolveRemoteDeletion(TodoItem localTodo) {

		Bundle b = new Bundle();
		b.putParcelable(LOCAL_TODO, localTodo);

		return waitForResolution(b);
	}

	protected ConflictResolution resolveLocalDeletion(TodoItem remoteTodo) {

		Bundle b = new Bundle();
		b.putParcelable(REMOTE_TODO, remoteTodo);

		return waitForResolution(b);
	}

	protected void sendProgressMessage(String msg) {

		Bundle b = new Bundle();
		b.putString(MESSAGE, msg);
		sendMessageWithHandler(b);
	}

	protected void sendErrorMessage(String errMsg) {

		Bundle b = new Bundle();
		b.putString(ERROR_MESSAGE, errMsg);
		sendMessageWithHandler(b);
	}

	protected void sendDismissMessage() {

		Bundle b = new Bundle();
		b.putBoolean(DONE, true);
		sendMessageWithHandler(b);
	}

	public void run() {

		sendProgressMessage("Starting...");
		workerThread.start();
		while (workerThread.getState() != Thread.State.TERMINATED) {

			try {

				switch (syncState) {

				case REGISTER:
					sendProgressMessage("Registering...");
					break;
				case SERVER_CLIENT:
					sendProgressMessage(
						"Server -> Client: "
						+ processTodosCurrBytes
						+ " out of "
						+ processTodosTotalBytes
						+ " bytes"
					);
					break;
				case SYNC:
					sendProgressMessage(
						"Synchronizing "
						+ syncCurrRecords
						+ " out of "
						+ syncTotalRecords
						+ " records"
					);
					break;
				case CLIENT_SERVER:
					sendProgressMessage(
						"Client -> Server: "
						+ createDataCurrBytes
						+ " out of "
						+ createDataTotalBytes
						+ " bytes"
					);
					break;
				case RESET:
					sendProgressMessage("Closing...");
					break;
				}
				Thread.sleep (100);
			} catch (InterruptedException ie) {

				Log.e("com.euscomputerclub.android.todo.TodoSync", "TodoSync is interrupted");
			}
		}

		sendDismissMessage();
	}
}
