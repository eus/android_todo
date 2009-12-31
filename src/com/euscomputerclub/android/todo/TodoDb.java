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
	protected static String TODO_TABLE = "todo";
	/** The ID column name. */
	protected static String ID_COLUMN = "_id";
	/** The title column name. */
	public static String TITLE_COLUMN = "title";
	/** The deadline column name. */
	public static String DEADLINE_COLUMN = "deadline";
	/** The priority column name. */
	public static String PRIORITY_COLUMN = "priority";
	/** The status column name. */
	public static String STATUS_COLUMN = "status";
	/** The description column name. */
	public static String DESCRIPTION_COLUMN = "description";

	/**
	 * Class TodoDbOpenHelper is a convenience to access the DB.
	 * 
	 * @author eus
	 */
	protected static class TodoDbOpenHelper extends SQLiteOpenHelper {

		/** The SQL statement to create todo table. */
		protected static String CREATE_TODO_TABLE = ("create table " + TODO_TABLE +" ("
				+ ID_COLUMN + " integer primary key autoincrement, "
				+ TITLE_COLUMN + " text not null, "
				+ DEADLINE_COLUMN + " text not null, "
				+ PRIORITY_COLUMN + " integer not null, "
				+ STATUS_COLUMN + " text not null, "
				+ DESCRIPTION_COLUMN + " text, "
				+ "UNIQUE (" + TITLE_COLUMN + ", " + DEADLINE_COLUMN + "));");
		/** The DB name. */
		protected static String DB_NAME = "todo";
		/** The DB version. */
		protected static int DB_VERSION = 1;

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
		public void onCreate(SQLiteDatabase arg0) {
			
			arg0.execSQL(CREATE_TODO_TABLE);
		}

		/* (non-Javadoc)
		 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			
			Log.e(this.getClass().getName(), "No DB upgrade is allowed yet");
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
		db = helper.getWritableDatabase();
	}

	/** Closes DB and frees up unused resources. */
	public void close() {

		helper.close();
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
	 * @return the row ID of the newly inserted row, -1 if an error occurred, -2 if a todo already exists.
	 */
	public long createTodo(String title, Deadline deadline, int priority, String status, String description) {
		
		Cursor c = getTodo(title, deadline);
		int rowCount = c.getCount();
		c.close();
		
		if (rowCount != 0) {
			return -2;
		}

		ContentValues v = new ContentValues();
		v.put(TITLE_COLUMN, title);
		v.put(DEADLINE_COLUMN, deadline.toString());
		v.put(PRIORITY_COLUMN, priority);
		v.put(STATUS_COLUMN, status);
		v.put(DESCRIPTION_COLUMN, description);
		
		return db.insert(TODO_TABLE, null, v);
	}

	/**
	 * Update a todo item.
	 * 
	 * @param id the todo's id
	 * @param isModified specifies whether a particular supplied argument needs to be updated or not
	 * @param title the todo's title
	 * @param deadline the todo's deadline
	 * @param priority the todo's priority
	 * @param status the todo's status
	 * @param description the todo's description
	 * 
	 * @return the number of updated todo items.
	 */
	public int updateTodo(long id, boolean[] isModified, String title, Deadline deadline, Integer priority, String status, String description) {

		ContentValues v = new ContentValues();
		if (isModified[0]) {
			
			v.put(TITLE_COLUMN, title == null ? "null" : title);
		}
		if (isModified[1]) {
			
			v.put(DEADLINE_COLUMN, deadline == null ? "null" : deadline.toString());
		}
		if (isModified[2]) {
			
			v.put(PRIORITY_COLUMN, priority == null ? "null" : priority.toString());
		}
		if (isModified[3]) {

			v.put(STATUS_COLUMN, status == null ? "null" : status.toString());	
		}
		if (isModified[4]) {

			v.put(DESCRIPTION_COLUMN, description == null ? "null" : description.toString());	
		}
		
		return db.update(TODO_TABLE, v, ID_COLUMN + " = ?", new String[] {String.valueOf(id)});
	}
	
	/**
	 * Deletes a todo item.
	 * 
	 * @param title the title of the todo item.
	 * @param deadline the deadline of the todo item.
	 * 
	 * @return the number of deleted todo items.
	 */
	public int deleteTodo(String title, Deadline deadline) {
		
		return db.delete(
				TODO_TABLE,
				TITLE_COLUMN + " = ? and " + DEADLINE_COLUMN + " = ?",
				new String[] {title, deadline.toString()}
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
		
		return db.delete(
				TODO_TABLE,
				ID_COLUMN + " = ?",
				new String[] {String.valueOf(id)}
		);
	}
	
	/**
	 * Returns a todo item.
	 * 
	 * @param title the title of the todo to be retrieved.
	 * @param deadline the deadline of the todo to be retrieved.
	 * 
	 * @return a cursor containing the desired todo item if it exists.
	 */
	public Cursor getTodo(String title, Deadline deadline) {

		return db.query(
				TODO_TABLE,
				null,
				TITLE_COLUMN + " = ? and " + DEADLINE_COLUMN + " = ?",
				new String[] {title, deadline.toString()},
				null,
				null,
				null
		);
	}
	
	/**
	 * Returns a todo item.
	 * 
	 * @param id the id of the todo to be retrieved.
	 * 
	 * @return a cursor containing the desired todo item if it exists.
	 */
	public Cursor getTodo(long id) {

		return db.query(
				TODO_TABLE,
				null,
				ID_COLUMN + " = ?",
				new String[] {String.valueOf(id)},
				null,
				null,
				null
		);
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
		
		return db.query(
				TODO_TABLE,
				desiredColumns,
				null,
				null,
				null,
				null,
				sortByColumn + " " + (isAsc ? "asc" : "desc")
		);
	}
}
