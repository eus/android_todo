package com.euscomputerclub.android.todo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Class TodoDB serves as the central storage for all created todo items.
 * 
 * @author eus
 */
public class TodoDb {

	/** The todo table name. */
	protected static final String TODO_TABLE = "todo";
	/** The sync table name. */
	protected static final String SYNC_TABLE = "sync";
	/** The deleted todo table name. */
	protected static final String DELETED_TODO_TABLE = "deleted_todo";
	/** The ID column name. */
	protected static final String ID_COLUMN = "_id";
	/** The title column name. */
	public static final String TITLE_COLUMN = "title";
	/** The deadline column name. */
	public static final String DEADLINE_COLUMN = "deadline";
	/** The priority column name. */
	public static final String PRIORITY_COLUMN = "priority";
	/** The status column name. */
	public static final String STATUS_COLUMN = "status";
	/** The description column name. */
	public static final String DESCRIPTION_COLUMN = "description";
	/** The revision column name. */
	public static final String REVISION_COLUMN = "revision";
	/** The revision number assigned to a new todo item. */
	public static final int NEW_TODO_REVISION = -1;

	/**
	 * Class TodoDbOpenHelper is a convenience to access the DB.
	 * 
	 * @author eus
	 */
	protected static class TodoDbOpenHelper extends SQLiteOpenHelper {

		/** The SQL statement to create todo table. */
		protected static final String CREATE_TODO_TABLE = ("create table " + TODO_TABLE +" ("
				+ ID_COLUMN + " integer primary key autoincrement, "
				+ TITLE_COLUMN + " text not null, "
				+ DEADLINE_COLUMN + " text not null, "
				+ PRIORITY_COLUMN + " integer not null, "
				+ STATUS_COLUMN + " text not null, "
				+ DESCRIPTION_COLUMN + " text, "
				+ "UNIQUE (" + TITLE_COLUMN + ", " + DEADLINE_COLUMN + "));");
		/** The version 2 of the SQL statement to create todo table. */
		protected static final String CREATE_TODO_TABLE_2 = ("create table ? ("
				+ ID_COLUMN + " integer primary key autoincrement, "
				+ TITLE_COLUMN + " text not null, "
				+ DEADLINE_COLUMN + " text not null, "
				+ PRIORITY_COLUMN + " integer not null, "
				+ STATUS_COLUMN + " text not null, "
				+ DESCRIPTION_COLUMN + " text);");
		/** The version 3 of the SQL statement to create todo table. */
		protected static final String CREATE_TODO_TABLE_3 = ("create table ? ("
				+ ID_COLUMN + " integer not null primary key autoincrement, "
				+ TITLE_COLUMN + " text, "
				+ DEADLINE_COLUMN + " text, "
				+ PRIORITY_COLUMN + " integer, "
				+ STATUS_COLUMN + " text, "
				+ DESCRIPTION_COLUMN + " text);");
		/** The version 4 of the SQL statement to create todo table. */
		protected static final String CREATE_TODO_TABLE_4 = ("create table " + TODO_TABLE +" ("
				+ ID_COLUMN + " integer not null primary key autoincrement, "
				+ TITLE_COLUMN + " text, "
				+ DEADLINE_COLUMN + " text, "
				+ PRIORITY_COLUMN + " integer, "
				+ STATUS_COLUMN + " text, "
				+ DESCRIPTION_COLUMN + " text,"
				+ REVISION_COLUMN + " integer default " + NEW_TODO_REVISION + " not null);");

		/** The SQL statement to create sync table. */
		protected static final String CREATE_SYNC_TABLE = ("create table " + SYNC_TABLE + " ("
			+ ID_COLUMN + " integer not null primary key autoincrement,"
			+ TITLE_COLUMN + " text,"
			+ DEADLINE_COLUMN + " text,"
			+ PRIORITY_COLUMN + " integer,"
			+ STATUS_COLUMN + " text,"
			+ DESCRIPTION_COLUMN + " text,"
			+ REVISION_COLUMN + " integer);"
		);

		/** The SQL statement to create deleted_todo table. */
		protected static final String CREATE_DELETED_TODO_TABLE = ("create table " + DELETED_TODO_TABLE
			+ " (" + ID_COLUMN + " integer not null references " + TODO_TABLE + " (" + ID_COLUMN + ")"
			+ " on delete cascade on update cascade)"
		);

		/** The DB name. */
		protected static final String DB_NAME = "todo";
		/** The DB version. */
		protected static final int DB_VERSION = 4;

		/**
		 * Constructs a TodoDbOpenHelper working on the DB referred by the context.
		 * 
		 * @param context the context in which the DB should be opened/created.
		 */
		public TodoDbOpenHelper(Context context) {

			super(context, DB_NAME, null, DB_VERSION);
		}

		/* (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			
			db.execSQL(CREATE_TODO_TABLE_4);
			db.execSQL(CREATE_SYNC_TABLE);
			db.execSQL(CREATE_DELETED_TODO_TABLE);
		}

		/* (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			for (int upgradeTo = oldVersion + 1; upgradeTo <= newVersion; upgradeTo++) {

				if (upgradeTo == 2) {

					db.execSQL(CREATE_TODO_TABLE_2.replace("?", "TMP_TABLE"));
					db.execSQL("insert into TMP_TABLE select * from " + TODO_TABLE + ";");
					db.execSQL("drop table " + TODO_TABLE + ";");
					db.execSQL(CREATE_TODO_TABLE_2.replace("?", TODO_TABLE));
					db.execSQL("insert into " + TODO_TABLE + " select " + ID_COLUMN + ", " + TITLE_COLUMN + ", " + DEADLINE_COLUMN + ", " + PRIORITY_COLUMN + ", " + STATUS_COLUMN + ", " + DESCRIPTION_COLUMN + " from TMP_TABLE;");
					db.execSQL("drop table TMP_TABLE;");
				}
				if (upgradeTo == 3) {

					db.execSQL(CREATE_TODO_TABLE_3.replace("?", "TMP_TABLE"));
					db.execSQL("insert into TMP_TABLE select * from " + TODO_TABLE + ";");
					db.execSQL("drop table " + TODO_TABLE + ";");
					db.execSQL(CREATE_TODO_TABLE_3.replace("?", TODO_TABLE));
					db.execSQL("insert into " + TODO_TABLE + " select " + ID_COLUMN + ", " + TITLE_COLUMN + ", " + DEADLINE_COLUMN + ", " + PRIORITY_COLUMN + ", " + STATUS_COLUMN + ", " + DESCRIPTION_COLUMN + " from TMP_TABLE;");
					db.execSQL("drop table TMP_TABLE;");
				}
				if (upgradeTo == 4) {

					db.execSQL(
						"alter table " + TODO_TABLE + " add "
						+ REVISION_COLUMN + " integer default "
						+ NEW_TODO_REVISION + " not null;"
					);
					db.execSQL(CREATE_SYNC_TABLE);
					db.execSQL(CREATE_DELETED_TODO_TABLE);
				}
			}
		}
	}

	/** Field helper stores the helper to open the DB. */
	protected TodoDbOpenHelper helper;

	/** Field db stores the writable DB. */
	protected SQLiteDatabase db;

	/**
	 * Constructs and initializes the internal states of the DB.
	 * 
	 * @param c the context on which the DB should be opened or created
	 */
	public TodoDb(Context c) {

		helper = new TodoDbOpenHelper (c);
	}

	/** Closes DB and frees up unused resources. */
	public void close() {

		helper.close();
		db = null;
	}

	/** Drop if necessary and create the sync table again so that the autoincremented field is reset. */
	public void recreateSyncTable() {

		ensureDb();

		db.execSQL("drop table if exists " + SYNC_TABLE + ";");
		db.execSQL(TodoDbOpenHelper.CREATE_SYNC_TABLE);
	}

	/** Drop the sync table to save memory. */
	public void dropSync() {

		ensureDb();

		db.execSQL("drop table " + SYNC_TABLE + ";");
	}

	/**
	 * Creates a new todo item.
	 * 
	 * @param title the todo's title
	 * @param deadline the todo's deadline
	 * @param priority the todo's priority
	 * @param status the todo's status
	 * @param description the todo's description
	 * 
	 * @return the row ID of the newly inserted row or -1 if an error occurred.
	 */
	public long createTodo(String title, Deadline deadline, int priority, String status, String description) {
		
		ensureDb();

		ContentValues v = new ContentValues();
		v.put(TITLE_COLUMN, title);
		v.put(DEADLINE_COLUMN, deadline.toString());
		v.put(PRIORITY_COLUMN, priority);
		v.put(STATUS_COLUMN, status);
		v.put(DESCRIPTION_COLUMN, description);
		
		return db.insert(TODO_TABLE, null, v);
	}

	/**
	 * Creates a new sync todo item
	 */
	public long createSyncTodo(long id, String title, String deadline,
				   int priority, String status, String description,
				   int revision) {
		
		ensureDb();

		ContentValues v = new ContentValues();
		v.put(ID_COLUMN, id);
		v.put(TITLE_COLUMN, title);
		v.put(DEADLINE_COLUMN, deadline);
		v.put(PRIORITY_COLUMN, priority);
		v.put(STATUS_COLUMN, status);
		v.put(DESCRIPTION_COLUMN, description);
		v.put(REVISION_COLUMN, revision);
		
		return db.insert(SYNC_TABLE, null, v);
	}

	/**
	 * Update a todo item.
	 * 
	 * @param id the todo's id
	 * @param title the todo's title (set to NULL to not update this field)
	 * @param deadline the todo's deadline (set to NULL to not update this field)
	 * @param priority the todo's priority (set to NULL to not update this field)
	 * @param status the todo's status (set to NULL to not update this field)
	 * @param description the todo's description (set to NULL to not update this field)
	 * 
	 * @return the number of updated todo items.
	 */
	public int updateTodo(long id, String title, Deadline deadline, Integer priority, String status, String description) {

		ensureDb();

		ContentValues v = new ContentValues();
		if (title != null) {

			v.put(TITLE_COLUMN, title);
		}
		if (deadline != null) {

			v.put(DEADLINE_COLUMN, deadline.toString());
		}
		if (priority != null) {

			v.put(PRIORITY_COLUMN, priority.intValue());
		}
		if (status != null) {

			v.put(STATUS_COLUMN, status);
		}
		if (description != null) {

			v.put(DESCRIPTION_COLUMN, description);
		}
		
		return db.update(TODO_TABLE, v, ID_COLUMN + " = ?", new String[] {String.valueOf(id)});
	}

	/**
	 * Update the revision of a todo item.
	 * 
	 * @param id the todo's id
	 * @param newRevision the new revision for this todo item
	 * 
	 * @return the number of updated todo items.
	 */
	public int updateTodoRevision(long id, int newRevision) {

		ensureDb();

		ContentValues v = new ContentValues();
		v.put(REVISION_COLUMN, newRevision);
		
		return db.update(TODO_TABLE, v, ID_COLUMN + " = ?", new String[] {String.valueOf(id)});
	}

	/**
	 * Update a remote todo item.
	 * 
	 * @param remoteTodo the remote todo to be updated
	 * @param localTodo the local todo whose data are used to update the remote one
	 * 
	 * @return the number of updated todo items.
	 */
	public int updateSync(TodoItem remoteTodo, TodoItem localTodo) {

		ensureDb();

		ContentValues v = new ContentValues();
		if (remoteTodo.title.equals(localTodo.title)) {

			v.putNull(TITLE_COLUMN);
		} else {

			v.put(TITLE_COLUMN, localTodo.title);
		}
		if (remoteTodo.deadline.equals(localTodo.deadline)) {

			v.putNull(DEADLINE_COLUMN);
		} else {

			v.put(DEADLINE_COLUMN, localTodo.deadline);
		}
		if (remoteTodo.priority.equals(localTodo.priority)) {

			v.putNull(PRIORITY_COLUMN);
		} else {

			v.put(PRIORITY_COLUMN, localTodo.priority.intValue());
		}
		if (remoteTodo.status.equals(localTodo.status)) {

			v.putNull(STATUS_COLUMN);
		} else {

			v.put(STATUS_COLUMN, localTodo.status);
		}
		if (remoteTodo.description.equals(localTodo.description)) {

			v.putNull(DESCRIPTION_COLUMN);
		} else {

			v.put(DESCRIPTION_COLUMN, localTodo.description);
		}
		v.putNull(REVISION_COLUMN);
		
		return db.update(SYNC_TABLE, v, ID_COLUMN + " = ?", new String[] {localTodo.id.toString()});
	}

	/**
	 * Replace a local todo item with the remote one.
	 * 
	 * @param remoteTodo the remote todo that will replace the local one
	 * 
	 * @return the number of updated todo items.
	 *
	 * @throw IllegalArgumentException if one of the arguments is null.
	 */
	public int replaceTodo(TodoItem remoteTodo) {

		ensureDb();

		ContentValues v = new ContentValues();
		if (remoteTodo.title == null) {

			throw new IllegalArgumentException("title is null");
		} else {

			v.put(TITLE_COLUMN, remoteTodo.title);
		}
		if (remoteTodo.deadline == null) {

			throw new IllegalArgumentException("deadline is null");
		} else {

			v.put(DEADLINE_COLUMN, remoteTodo.deadline);
		}
		if (remoteTodo.priority == null) {

			throw new IllegalArgumentException("priority is null");
		} else {

			v.put(PRIORITY_COLUMN, remoteTodo.priority.intValue());
		}
		if (remoteTodo.status == null) {

			throw new IllegalArgumentException("status is null");
		} else {

			v.put(STATUS_COLUMN, remoteTodo.status);
		}
		if (remoteTodo.description == null) {

			throw new IllegalArgumentException("description is null");
		} else {

			v.put(DESCRIPTION_COLUMN, remoteTodo.description);
		}
		if (remoteTodo.revision == null) {

			throw new IllegalArgumentException("revision is null");
		} else {

			v.put(REVISION_COLUMN, remoteTodo.revision.intValue() + 1);
		}
		
		return db.update(TODO_TABLE, v, ID_COLUMN + " = ?", new String[] {remoteTodo.id.toString()});
	}

	/**
	 * Creates a new remote todo item.
	 * 
	 * @param localTodo the local todo to be stored in the sync server
	 * 
	 * @return the row ID of the newly inserted row or -1 if an error occurred.
	 */
	public long insertToSync(TodoItem localTodo) {
		
		ensureDb();

		ContentValues v = new ContentValues();
		v.put(ID_COLUMN, localTodo.id);
		v.put(TITLE_COLUMN, localTodo.title);
		v.put(DEADLINE_COLUMN, localTodo.deadline);
		v.put(PRIORITY_COLUMN, localTodo.priority.intValue());
		v.put(STATUS_COLUMN, localTodo.status);
		v.put(DESCRIPTION_COLUMN, localTodo.description);
		int revision = localTodo.revision.intValue();
		v.put(REVISION_COLUMN, (revision == NEW_TODO_REVISION
					? NEW_TODO_REVISION
					: NEW_TODO_REVISION - revision - 1));
		
		return db.insert(SYNC_TABLE, null, v);
	}

	/**
	 * Remove a remote todo item from the synchronization process.
	 * 
	 * @param id the remote todo's ID
	 * 
	 * @return the number of removed todo items.
	 */
	public int removeSync(long id) {
		
		ensureDb();

		return db.delete(
			SYNC_TABLE,
			ID_COLUMN + " = ?",
			new String[] {String.valueOf(id)}
		);
	}

	/**
	 * Delete a local todo item.
	 * 
	 * @param id the local todo's ID
	 * 
	 * @return the number of removed todo items.
	 */
	public int deleteLocal(long id) {
		
		ensureDb();

		return db.delete(
			TODO_TABLE,
			ID_COLUMN + " = ?",
			new String[] {String.valueOf(id)}
		) + db.delete(
			DELETED_TODO_TABLE,
			ID_COLUMN + " = ?",
			new String[] {String.valueOf(id)}
		);
	}

	/**
	 * Delete a remote todo item.
	 * 
	 * @param id the remote todo's ID
	 * 
	 * @return the number of removed todo items.
	 */
	public int deleteSync(long id) {
		
		ensureDb();

		ContentValues v = new ContentValues();
		v.putNull(TITLE_COLUMN);
		v.putNull(DEADLINE_COLUMN);
		v.putNull(PRIORITY_COLUMN);
		v.putNull(STATUS_COLUMN);
		v.putNull(DESCRIPTION_COLUMN);
		v.putNull(REVISION_COLUMN);
	
		return db.update(SYNC_TABLE, v, ID_COLUMN + " = ?", new String[] {String.valueOf(id)});
	}

	/**
	 * Returns true if the user has deleted the local todo, otherwise false.
	 * 
	 * @param id the local todo's ID
	 */
	public boolean isLocalDeleted(long id) {
		
		boolean result = false;
		ensureDb();

		Cursor c = db.query(
			DELETED_TODO_TABLE,
			null,
			ID_COLUMN + " = ?",
			new String[] {String.valueOf(id)},
			null,
			null,
			null
		);

		if (c.getCount() != 0) {

			result = true;
		}

		c.close();
	
		return result;
	}

	/** Imports remote todos that do not exist in the local DB to the DB. */
	public void importNewTodos() {
		
		ensureDb();

		db.execSQL(
			"insert into " + TODO_TABLE
			+ " select "
			+ ID_COLUMN + ", "
			+ TITLE_COLUMN + ", "
			+ DEADLINE_COLUMN + ", "
			+ PRIORITY_COLUMN + ", "
			+ STATUS_COLUMN + ", "
			+ DESCRIPTION_COLUMN + ", "
			+ REVISION_COLUMN + " + 1"
			+ " from " + SYNC_TABLE
			+ " where " + ID_COLUMN + " not in ("
			+ "select " + ID_COLUMN + " from " + TODO_TABLE + ")"
			+ " and " + TITLE_COLUMN + " is not null"
			+ " and " + DEADLINE_COLUMN + " is not null"
			+ " and " + PRIORITY_COLUMN + " is not null"
			+ " and " + STATUS_COLUMN + " is not null"
			+ " and " + DESCRIPTION_COLUMN + " is not null"
			+ " and " + REVISION_COLUMN + " is not null"
		);
	}

	/** Giving new local todo items IDs that correspond with the remote ones. */
	public void adjustNewTodoIdsAndRevisions() {

		ensureDb();

		db.delete(
			TODO_TABLE,
			REVISION_COLUMN + " = ?",
			new String[] {String.valueOf(NEW_TODO_REVISION)}
		);

		db.execSQL(
			"insert into " + TODO_TABLE + " ("
			+ ID_COLUMN + ", "
			+ TITLE_COLUMN + ", "
			+ DEADLINE_COLUMN + ", "
			+ PRIORITY_COLUMN + ", "
			+ STATUS_COLUMN + ", "
			+ DESCRIPTION_COLUMN + ", "
			+ REVISION_COLUMN + ")"
			+ " select "
			+ ID_COLUMN + ", "
			+ TITLE_COLUMN + ", "
			+ DEADLINE_COLUMN + ", "
			+ PRIORITY_COLUMN + ", "
			+ STATUS_COLUMN + ", "
			+ DESCRIPTION_COLUMN + ", "
			+ "1"
			+ " from " + SYNC_TABLE
			+ " where " + REVISION_COLUMN + " = " + NEW_TODO_REVISION
		);
	}
	
	/**
	 * Deletes a todo item.
	 * 
	 * @param id the id of the todo item.
	 * 
	 * @return the number of deleted todo items.
	 */
	public int deleteTodo(long id) {

		ensureDb();
		
		Cursor c = db.query(
			TODO_TABLE,
			new String[] {REVISION_COLUMN},
			ID_COLUMN + " = ?",
			new String[] {String.valueOf(id)},
			null,
			null,
			null
		);
		if (c.getCount() == 0) {

			c.close();

			return 0;
		}
		c.moveToNext();
		int revision = c.getInt(c.getColumnIndex(REVISION_COLUMN));

		c.close();
		if (revision == NEW_TODO_REVISION) {

			return db.delete(
					TODO_TABLE,
					ID_COLUMN + " = ?",
					new String[] {String.valueOf(id)}
			);
		} else {

			ContentValues v = new ContentValues();
			v.put(ID_COLUMN, id);
		
			db.insert(DELETED_TODO_TABLE, null, v);
			return 1;
		}
	}
	
	/**
	 * Returns a todo item that is received from the synchronization server.
	 * 
	 * @param id the id of the todo to be retrieved.
	 * 
	 * @return a cursor containing the desired todo item if it exists.
	 */
	public TodoItem getRemoteTodo(long id) {

		ensureDb();

		return getTodoItemFromCursor(db.query(
			SYNC_TABLE,
			null,
			ID_COLUMN + " = ? ",
			new String[] {String.valueOf(id)},
			null,
			null,
			null
		));
	}

	protected TodoItem getTodoItemFromCursor(Cursor c) {

		TodoItem todoItem = null;

		if (c.getCount() == 0) {

			c.close();

			return todoItem;
		}
		c.moveToNext();
		todoItem = new TodoItem(
			new Long(c.getLong(c.getColumnIndex(ID_COLUMN))),
			c.getString(c.getColumnIndex(TITLE_COLUMN)),
			c.getString(c.getColumnIndex(DEADLINE_COLUMN)),
			new Integer(c.getInt(c.getColumnIndex(PRIORITY_COLUMN))),
			c.getString(c.getColumnIndex(STATUS_COLUMN)),
			c.getString(c.getColumnIndex(DESCRIPTION_COLUMN)),
			new Integer(c.getInt(c.getColumnIndex(REVISION_COLUMN)))
		);
		c.close();

		return todoItem;
	}
	
	/**
	 * Returns a todo item.
	 * 
	 * @param id the id of the todo to be retrieved.
	 * 
	 * @return the desired todo item if it exists, otherwise null.
	 */
	public TodoItem getTodo(long id) {

		ensureDb();

		return getTodoItemFromCursor(db.query(
			TODO_TABLE,
			null,
			ID_COLUMN + " = ? and not exists (select "
			+ ID_COLUMN + " from " + DELETED_TODO_TABLE + " where " + ID_COLUMN + " = ?)",
			new String[] {String.valueOf(id), String.valueOf(id)},
			null,
			null,
			null
		));
	}
	
	/**
	 * Returns all todo items.
	 * 
	 * @param desiredColumns the attributes of every todo items to be retrieved
	 * @param sortByColumn the attribute used to sort the todo items
	 * @param isAsc the sorting order of the todo items
	 * 
	 * @return a cursor containing all todo items sorted in a particular order if any exists.
	 */
	public Cursor getAllTodo(String[] desiredColumns, String sortByColumn, boolean isAsc) {
		
		ensureDb();

		return db.query(
				TODO_TABLE,
				desiredColumns,
				ID_COLUMN + " not in (select "
				+ ID_COLUMN + " from " + DELETED_TODO_TABLE + ")",
				null,
				null,
				null,
				sortByColumn + " " + (isAsc ? "asc" : "desc")
		);
	}
	
	/** Returns all new todo items to be sent to the synchronization server. */
	public Cursor getAllNewSyncTodo() {
		
		ensureDb();

		return db.query(
				SYNC_TABLE,
				new String[] {
					ID_COLUMN,
					TITLE_COLUMN,
					DEADLINE_COLUMN,
					PRIORITY_COLUMN,
					STATUS_COLUMN,
					DESCRIPTION_COLUMN,
					REVISION_COLUMN
				},
				REVISION_COLUMN + " < 0",
				null,
				null,
				null,
				null
		);
	}
	
	/** Returns all updated todo items to be sent to the synchronization server. */
	public Cursor getAllUpdatedSyncTodo() {
		
		ensureDb();

		return db.query(
				SYNC_TABLE,
				new String[] {
					ID_COLUMN,
					TITLE_COLUMN,
					DEADLINE_COLUMN,
					PRIORITY_COLUMN,
					STATUS_COLUMN,
					DESCRIPTION_COLUMN,
					REVISION_COLUMN
				},
				"(" + TITLE_COLUMN + " is not null"
				+ " or " + DEADLINE_COLUMN + " is not null"
				+ " or " + PRIORITY_COLUMN + " is not null"
				+ " or " + STATUS_COLUMN + " is not null"
				+ " or " + DESCRIPTION_COLUMN + " is not null)"
				+ " and revision is null",
				null,
				null,
				null,
				null
		);
	}
	
	/** Returns all deleted todo items to be sent to the synchronization server. */
	public Cursor getAllDeletedSyncTodo() {
		
		ensureDb();

		return db.query(
				SYNC_TABLE,
				new String[] {ID_COLUMN},
				TITLE_COLUMN + " is null"
				+ " and " + DEADLINE_COLUMN + " is null"
				+ " and " + PRIORITY_COLUMN + " is null"
				+ " and " + STATUS_COLUMN + " is null"
				+ " and " + DESCRIPTION_COLUMN + " is null"
				+ " and revision is null",
				null,
				null,
				null,
				null
		);
	}

	/** Ensures that the DB is ready for reading and writing. */
	protected void ensureDb() {

		if (db == null) {

			db = helper.getWritableDatabase();
		}
	}
}
