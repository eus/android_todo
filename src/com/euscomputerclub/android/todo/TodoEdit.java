package com.euscomputerclub.android.todo;

import java.util.Arrays;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

public class TodoEdit extends Activity {

	/** A request code to create a todo item. */
	public static final int CREATE_REQUEST = 1;
	/** A request code to edit a todo item. */
	public static final int EDIT_REQUEST = 2;
	/** The todo item to operate on (-1 means a new item). */
	protected long id;
	/** The todo DB. */
	protected TodoDb db;
	/** The todo's title. */
	protected String title;
	/** The title EditText. */
	protected EditText titleEditText;
	/** The todo's deadline. */
	protected Deadline deadline;
	/** The deadline DatePicker. */
	protected DatePicker deadlineDatePicker;
	/** The todo's description. */
	protected String description;
	/** The description EditText. */
	protected EditText descriptionEditText;
	/** The todo's priority. */
	protected int priority;
	/** The priority Spinner. */
	protected Spinner prioritySpinner;
	/** The priority values. */
	protected String[] priorityValues;
	/** The todo's status. */
	protected String status;
	/** The status Spinner. */
	protected Spinner statusSpinner;
	/** The status values. */
	protected String[] statusValues;
	/** The alert dialog builder for this edit screen. */
	protected AlertDialog.Builder alertBuilder;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.todo_edit);

		alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setNeutralButton("Close", new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {

				dialog.dismiss();
			}
		});
		
		titleEditText = (EditText) findViewById(R.id.EditTitleText);
		deadlineDatePicker = (DatePicker) findViewById(R.id.EditDeadlineDatePicker);
		descriptionEditText = (EditText) findViewById(R.id.EditDescriptionText);
		prioritySpinner = (Spinner) findViewById(R.id.EditPrioritySpinner);
		priorityValues = getResources().getStringArray(R.array.priority_values);
		statusSpinner = (Spinner) findViewById(R.id.EditStatusSpinner);
		statusValues = getResources().getStringArray(R.array.status_values);

		Utility.populateSpinner(this, R.id.EditPrioritySpinner, R.array.priority_values);
		Utility.populateSpinner(this, R.id.EditStatusSpinner, R.array.status_values);

		db = new TodoDb(this);
		
		/* initialize fields and buttons */
		Intent i = getIntent();
		id = i.getLongExtra("id", -1);
		if (id != -1) {

			TodoItem todoItem = db.getTodo(id);
			if (todoItem == null) {
				
				id = -1;
			}
			else {
				
				initializeWidgets(todoItem);
			}
		}

		Button b = (Button) findViewById(R.id.EditSaveButton);
		b.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				
				setResult(RESULT_OK);
				if (id == -1) {

					updateWidgetValues();
					long rc = db.createTodo(title, deadline, priority, status, description);
					if (rc < 0) {

						alertBuilder.setTitle("TodoEdit Error");
						if (rc == -1) {

							alertBuilder.setMessage("Cannot create due to DB error");
						}
						alertBuilder.show();
						return;
					}
				}
				else {
					
					updateTodo();
				}
				db.close();
				finish();
			}
		});
		
		b = (Button) findViewById(R.id.EditCancelButton);
		b.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				
				setResult(RESULT_CANCELED);
				db.close();
				finish();
			}
		});
	}
	
	/**
	 * Initializes the values of EditText and Spinner on the UI based on the first record in the Cursor.
	 * 
	 * @param todo the todo with which the UI widgets are to be initialized
	 */
	protected void initializeWidgets(TodoItem todo) {
		
		title = todo.title;
		titleEditText.setText(todo.title);

		deadline = new Deadline(todo.deadline);
		deadlineDatePicker.init(deadline.getYear(), deadline.getMonth(), deadline.getDayOfMonth(), null);

		priority = todo.priority.intValue();
		prioritySpinner.setSelection(priority - 1);

		status = todo.status;
		statusSpinner.setSelection((Arrays.binarySearch(statusValues, status)));

		description = todo.description;
		descriptionEditText.setText(description);
	}

	/** Updates a todo entry in the DB. */
	protected void updateTodo() {
		
		String oldTitle = title;
		String oldDescription = description;
		String oldStatus = status;
		Deadline oldDeadline = deadline;
		int oldPriority = priority;
		
		updateWidgetValues();
		
		db.updateTodo(
			id,
			title.equals(oldTitle) ? null : title,
			deadline.equals(oldDeadline) ? null : deadline,
			priority == oldPriority ? null : new Integer(priority),
			status.equals(oldStatus) ? null : status,
			description.equals(oldDescription) ? null : description
		);
	}

	/** Updates the internal values for the corresponding widgets. */
	protected void updateWidgetValues() {

		title = titleEditText.getText().toString();
		description = descriptionEditText.getText().toString();
		status = statusValues[statusSpinner.getSelectedItemPosition()];
		priority = Utility.getPriorityValue(priorityValues[prioritySpinner.getSelectedItemPosition()]);
		deadline = new Deadline(
			deadlineDatePicker.getYear(),
			deadlineDatePicker.getMonth(),
			deadlineDatePicker.getDayOfMonth()
		);
	}
}
