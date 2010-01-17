package com.euscomputerclub.android.todo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
	public static final String LOCAL_TODO = "local_todo";
	/** Bundle key for remote todo item. */
	public static final String REMOTE_TODO = "remote_todo";
	/** Bundle key for synchronization activity. */
	public static final String MESSAGE = "message";
	/** Bundle key for synchronization completion. */
	public static final String DONE = "done";

	/** Constructs a TodoSync for a user identified by userId. */
	TodoSync(Handler todoListHandler, int userId) {

		this.userId = userId;
		handler = todoListHandler;
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

	protected void sendDismissMessage(String msg) {

		Bundle b = new Bundle();
		b.putBoolean(DONE, true);
		sendMessageWithHandler(b);
	}

	public void run() {

		for (int i = 0; i <= 100; i++) {

			try {

				sendProgressMessage("Synchronization " + i + " out of 100");
				Thread.sleep (100);
			} catch (InterruptedException ie) {

				Log.e("ERROR", "TodoSync is interrupted");
			}
		}

		sendDismissMessage(DONE);
	}
}
