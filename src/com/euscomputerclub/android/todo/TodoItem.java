package com.euscomputerclub.android.todo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Class TodoItem represents a single todo item.
 *
 * @author Tadeus Prastowo
 */
public class TodoItem implements Parcelable
{
	public Long id;
	public String title;
	public String deadline;
	public Integer priority;
	public String status;
	public String description;
	public Integer revision;

	TodoItem(Long id, String title, String deadline, Integer priority,
		String status, String description, Integer revision) {

		this.id = id;
		this.title = title;
		this.deadline = deadline;
		this.priority = priority;
		this.status = status;
		this.description = description;
		this.revision = revision;
	}

	public void writeToParcel(Parcel out, int flags) {

		out.writeValue(id);
		out.writeValue(title);
		out.writeValue(deadline);
		out.writeValue(priority);
		out.writeValue(status);
		out.writeValue(description);
		out.writeValue(revision);
	}

	private TodoItem(Parcel in) {

		id = (Long) in.readValue(Long.class.getClassLoader());
		title = (String) in.readValue(String.class.getClassLoader());
		deadline = (String) in.readValue(String.class.getClassLoader());
		priority = (Integer) in.readValue(Integer.class.getClassLoader());
		status = (String) in.readValue(String.class.getClassLoader());
		description = (String) in.readValue(String.class.getClassLoader());
		revision = (Integer) in.readValue(Integer.class.getClassLoader());
	}

	public int describeContents() {
		return 0;
	}

	public static final Parcelable.Creator<TodoItem> CREATOR
		= new Parcelable.Creator<TodoItem>() {

		public TodoItem createFromParcel(Parcel in) {
			return new TodoItem(in);
		}

		public TodoItem[] newArray(int size) {
			return new TodoItem[size];
		}
	};
}
