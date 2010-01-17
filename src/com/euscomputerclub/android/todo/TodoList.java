package com.euscomputerclub.android.todo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;

public class TodoList extends ListActivity {

	/** If you modify this, also update strings, ascStrings, descStrings and colNames. */
	protected int[] sortingButtonIds = {
			R.id.MainTitleButton,
			R.id.MainPriorityButton,
			R.id.MainStatusButton,
			R.id.MainDeadlineButton
	};
	/** The order must correspond to sortingButtonIds. */
	protected int[] strings = {
			R.string.title,
			R.string.priority,
			R.string.status,
			R.string.deadline
	};
	/** The order must correspond to sortingButtonIds. */
	protected int[] ascStrings = {
			R.string.title_asc,
			R.string.priority_asc,
			R.string.status_asc,
			R.string.deadline_asc
	};
	/** The order must correspond to sortingButtonIds. */
	protected int[] descStrings = {
			R.string.title_desc,
			R.string.priority_desc,
			R.string.status_desc,
			R.string.deadline_desc
	};
	/** The order must correspond to sortingButtonIds. */
	protected String[] colNames = {
			TodoDb.TITLE_COLUMN,
			TodoDb.PRIORITY_COLUMN,
			TodoDb.STATUS_COLUMN,
			TodoDb.DEADLINE_COLUMN
	};
	/** The buttons corresponding to sortingButtonIds. */
	protected Button[] sortingButtons;
	/** The DB containing todo items. */
	protected TodoDb db;
	/** The cursor pointing to all todo items sorted in a certain way. */
	protected Cursor c;
	/** The column that is currently used as the sort key. */
	protected String sortByColumn;
	/** The current sorting order. */
	protected boolean isAsc;
	/** The retrieved attributes from each todo item. If you update this, viewIds must also be updated. */
	protected String[] cols = {
			TodoDb.ID_COLUMN,
			TodoDb.TITLE_COLUMN,
			TodoDb.DEADLINE_COLUMN,
			TodoDb.PRIORITY_COLUMN,
			TodoDb.STATUS_COLUMN		
	};
	/** The mapping of cols to views in each row in the ListView. viewIds must correspond to cols. */
	protected int[] viewIds = {
			R.id.RowId,
			R.id.RowTitle,
			R.id.RowDeadline,
			R.id.RowPriority,
			R.id.RowStatus
	};
	/** The adapter that maps columns in each row in the cursor to the corresponding views. */
	protected SimpleCursorAdapter rowAdapter;
	/** The id of the currently selected todo item. */
	protected long curr_selected_id = AdapterView.INVALID_ROW_ID;
	/** The position of the currently selected todo item. */
	protected int curr_selected_position = AdapterView.INVALID_POSITION;
	/** The view of the currently selected todo item. */
	protected LinearLayout curr_selected_row;
	/** The default color of the TextView in the ListView row. */
	protected ColorStateList defaultColors;
	/** The socket used to sync the local DB with the server's DB. */
	protected DatagramSocket syncSocket;
	/** The notification pop-up. */
	protected AlertDialog.Builder alertBuilder;
	/** The shared preferences of this Todo Application. */
	public static final String PREFERENCES_NAME = "com.euscomputerclub.android.todo";
	/** The user ID used for identification with the synchronization server. */
	protected int userId;
	/** The shared preferences' key for the user ID. */
	protected static final String USER_ID = "todo_user_id";
	/** The userId value when the ID is unset. */
	protected static final int NO_USER_ID = -1;
	/** The ID of the user ID selection dialog. */
	protected static final int USER_ID_DIALOG = 0;
	/** The sync progress dialog. */
	protected ProgressDialog syncProgressDialog;
	/** The conflict resolution dialog. */
	protected AlertDialog.Builder conflictDialog;
	/** The priority values. */
	protected String priorityValues[];
	/** The handler of this activity thread for sync. */
	protected final Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			Bundle b = msg.getData();
			TodoItem localTodo = (TodoItem) b.get(TodoSync.LOCAL_TODO);
			TodoItem remoteTodo = (TodoItem) b.get(TodoSync.REMOTE_TODO);
			String message = b.getString(TodoSync.MESSAGE);
			boolean isDone = b.getBoolean(TodoSync.DONE, false);

			if (isDone) {

				syncProgressDialog.dismiss();
				return;
			}

			if (message != null) {

				syncProgressDialog.setMessage(message);
				return;
			}

			if (localTodo != null && remoteTodo != null) {

				String localPriority = priorityValues[localTodo.priority.intValue()];
				String remotePriority = priorityValues[remoteTodo.priority.intValue()];

				conflictDialog.setMessage(
					"[Local item]\n"
					+ "\tTitle: " + localTodo.title + "\n"
					+ "\tDeadline: " + localTodo.deadline + "\n"
					+ "\tPriority: " + localPriority + "\n"
					+ "\tStatus: " + localTodo.status + "\n"
					+ "\tDescription: " + localTodo.description + "\n"
					+ "[Remote item]\n"
					+ "\tTitle: " + remoteTodo.title + "\n"
					+ "\tDeadline: " + remoteTodo.deadline + "\n"
					+ "\tPriority: " + remotePriority + "\n"
					+ "\tStatus: " + remoteTodo.status + "\n"
					+ "\tDescription: " + remoteTodo.description
				);

			} else if (localTodo != null) {

				String localPriority = priorityValues[localTodo.priority.intValue()];

				conflictDialog.setMessage(
					"[Local item]\n"
					+ "\tTitle: " + localTodo.title + "\n"
					+ "\tDeadline: " + localTodo.deadline + "\n"
					+ "\tPriority: " + localPriority + "\n"
					+ "\tStatus: " + localTodo.status + "\n"
					+ "\tDescription: " + localTodo.description + "\n"
					+ "[Remote item is DELETED]"
				);
			} else {

				String remotePriority = priorityValues[remoteTodo.priority.intValue()];

				conflictDialog.setMessage(
					"[Local item is DELETED]\n"
					+ "[Remote item]\n"
					+ "\tTitle: " + remoteTodo.title + "\n"
					+ "\tDeadline: " + remoteTodo.deadline + "\n"
					+ "\tPriority: " + remotePriority + "\n"
					+ "\tStatus: " + remoteTodo.status + "\n"
					+ "\tDescription: " + remoteTodo.description
				);
			}
			
			conflictDialog.show();
		}
	};
	/** The sync thread. */
	protected TodoSync syncThread;

	@Override
	protected Dialog onCreateDialog(int id) {

		final Dialog d;

		switch (id) {
		case USER_ID_DIALOG:
			d = new Dialog(this);
			d.setContentView(R.layout.todo_user);
			d.setTitle("User ID");
			d.setCancelable(false);
			Button b = (Button) d.findViewById(R.id.SelectUserIdSaveButton);
			b.setOnClickListener(new OnClickListener() {

				public void onClick(View v) {

					try {
						TextView t = (TextView) d.findViewById(R.id.SelectUserIdText);
						int id = Integer.parseInt(t.getText().toString());

						if (id < 0) {

							alertBuilder.setTitle("User ID Error");
							alertBuilder.setMessage("User ID must be a natural number");
							alertBuilder.show();
							return;
						}

						if (id != userId) {

							SharedPreferences s = getSharedPreferences(

								PREFERENCES_NAME, Context.MODE_PRIVATE
							);

							SharedPreferences.Editor e = s.edit();
							e.putInt(USER_ID, id);
							userId = id;
							e.commit();
						}
						d.dismiss();
					} catch (NumberFormatException e) {
					
						alertBuilder.setTitle("User ID Error");
						alertBuilder.setMessage("User ID must be a natural number");
						alertBuilder.show();
					}
				}
			});
			break;
		default:
			d = null;
		}

		return d;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {

		switch (id) {
		case USER_ID_DIALOG:
			TextView t = (TextView) dialog.findViewById(R.id.SelectUserIdText);
			t.setText(userId == NO_USER_ID ? "" : String.valueOf(userId));
			break;
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.todo_list);

		alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setNeutralButton("Close", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				dialog.dismiss();
			}
		});

		conflictDialog = new AlertDialog.Builder(this);
		conflictDialog.setTitle("Pick local or remote?");
		conflictDialog.setCancelable(false);
		conflictDialog.setNegativeButton("Local", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				syncThread.setConflictResolution(TodoSync.ConflictResolution.PICK_LOCAL);
				dialog.dismiss();
			}
		});
		conflictDialog.setPositiveButton("Remote", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				syncThread.setConflictResolution(TodoSync.ConflictResolution.PICK_REMOTE);
				dialog.dismiss();
			}
		});

		priorityValues = getResources().getStringArray(R.array.priority_values);

		SharedPreferences s = getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
		userId = s.getInt(USER_ID, NO_USER_ID);
		if (userId == NO_USER_ID) { // only happen once when the application is pristine

			showDialog(USER_ID_DIALOG);
		}

		try {

			syncSocket = new DatagramSocket();
		} catch (SocketException e) {

			alertBuilder.setTitle("TodoList Exception");
			alertBuilder.setMessage(e.toString());
			alertBuilder.show();
			finish();
		}

		OnClickListener sortingButtonListener = new OnClickListener() {

			public void onClick(View v) {
				int i;
				int clickedButtonId = v.getId();

				for (i = 0; i < sortingButtonIds.length; i++) {

					if (sortingButtonIds[i] == clickedButtonId) {

						break;
					}
				}

				sortByColumn = colNames[i];

				String buttonText = sortingButtons[i].getText().toString();
				/* button is not a previous sorting key? Ascending. Otherwise, the opposite ordering */
				isAsc = buttonText.equals(sortByColumn) || !buttonText.equals(getString(ascStrings[i]));

				updateTodoList();
				updateSortStatus();
			}
		};
		sortingButtons = new Button[sortingButtonIds.length];
		for (int i = 0; i < sortingButtonIds.length; i++) {

			sortingButtons[i] = (Button) findViewById(sortingButtonIds[i]);
			sortingButtons[i].setOnClickListener(sortingButtonListener);
		};

		sortByColumn = TodoDb.DEADLINE_COLUMN;
		isAsc = true;
		db = new TodoDb(this);

		rowAdapter = new SimpleCursorAdapter(this, R.layout.todo_list_row, c, cols, viewIds) {
			
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				LinearLayout v = (LinearLayout) super.getView(position, convertView, parent);

				if (position == curr_selected_position) {

					setHighlight(v);
					curr_selected_row = v;
				} else {

					removeHighlight(v);
				}
				
				return v;
			}
		};
		setListAdapter(rowAdapter);
		getListView().setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

				if (curr_selected_row != null) {

					removeHighlight(curr_selected_row);
					curr_selected_row = null;
				}
				curr_selected_position = position;
				curr_selected_id = id;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		updateTodoList();
		updateSortStatus();

		Button b = (Button) findViewById(R.id.MainNewButton);
		b.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				db.close();
				startActivityForResult(new Intent(TodoList.this, TodoEdit.class), TodoEdit.CREATE_REQUEST);
			}
		});

		b = (Button) findViewById(R.id.MainEditButton);
		b.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				Intent i = new Intent(TodoList.this, TodoEdit.class);

				i.putExtra("id", curr_selected_id);
				startActivityForResult(i, TodoEdit.EDIT_REQUEST);
			}
		});

		b = (Button) findViewById(R.id.MainDeleteButton);
		b.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				db.deleteTodo(curr_selected_id);
				updateTodoList();
			}
		});

		b = (Button) findViewById(R.id.MainSyncButton);
		b.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				syncTodo();
			}
		});

		b = (Button) findViewById(R.id.MainUserButton);
		b.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				showDialog(USER_ID_DIALOG);
			}
		});
	}

	/** Synchronize the ToDo items in the DB with the server. */
	protected void syncTodo() {

		// showDialog(SYNC_PROGRESS_DIALOG); Can't use because animation won't restart
		// See http://code.google.com/p/android/issues/detail?id=4266
		syncProgressDialog = new ProgressDialog(this);
		syncProgressDialog.setIndeterminate(true);
		syncProgressDialog.setCancelable(false);
		syncProgressDialog.show();

		if (syncThread == null || syncThread.getState() == Thread.State.TERMINATED) {

			syncThread = new TodoSync(handler, userId);
			syncThread.start();
		}
	}

	/** Sets the background of a ListView's row indicating a selection. */
	protected void setHighlight(LinearLayout v) {

		int childCount = v.getChildCount();
		for (int i = 0; i < childCount; i++) {

			TextView t = (TextView) v.getChildAt(i);
			t.setTextColor(0xFF000000);
		}

		v.setBackgroundDrawable(getResources().getDrawable(

			R.drawable.list_selector_background_focus
		));
	}

	/** Remove the background of a ListView's row indicating an unselected item. */
	protected void removeHighlight(LinearLayout v) {

		int childCount = v.getChildCount();
		for (int i = 0; i < childCount; i++) {

			TextView t = (TextView) v.getChildAt(i);
			
			if (defaultColors == null) {
	
				defaultColors = ((TextView) ((LinearLayout) v).getChildAt(0))
					.getTextColors();
			}
			t.setTextColor(defaultColors);
		}

		v.setBackgroundDrawable(null);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {

			updateTodoList();
		}
	}

	/** Updates the todo list because the data have to be sorted in a different way. */
	protected void updateTodoList() {

		if (c != null) {

			stopManagingCursor(c);
		}

		c = db.getAllTodo(cols, sortByColumn, isAsc);
		startManagingCursor(c);

		rowAdapter.changeCursor(c);
	}

	/** Updates the sort button labels according to the selected sorting criteria. */
	protected void updateSortStatus() {

		for (int i = 0; i < sortingButtonIds.length; i++) {
			if (colNames[i].equals(sortByColumn)) {

				sortingButtons[i].setText(isAsc ? ascStrings[i] : descStrings[i]);
			}
			else {

				sortingButtons[i].setText(strings[i]);
			}
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		super.onListItemClick(l, v, position, id);

		curr_selected_id = id;
		curr_selected_position = position;
	}
}
