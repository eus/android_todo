package com.euscomputerclub.android.todo;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.widget.DatePicker;

/**
 * Class Deadline wraps a todo deadline.
 * 
 * @author eus
 */
public class Deadline {

	protected String dateFmt = "yyyy-MM-dd";
	protected int year;
	protected int month;
	protected int dayOfMonth;

	/**
	 * Constructs a deadline using the values represented by a DatePicker.
	 *
	 * @param dp the DatePicker object whose values are to be taken
	 */
	public Deadline(DatePicker dp) {

		year = dp.getYear();
		month = dp.getMonth();
		dayOfMonth = dp.getDayOfMonth();
	}

	/**
	 * Constructs a deadline using the specified values.
	 *
	 * @param year the deadline's year
	 * @param month the deadline's month
	 * @param dayOfMonth the deadline's day of month
	 */ 
	public Deadline(int year, int month, int dayOfMonth) {

		this.year = year;
		this.month = month;
		this.dayOfMonth = dayOfMonth;
	}

	/**
	 * Constructs a deadline using the specified date string (YYYY-MM-DD).
	 *
	 * @param s the string representing the deadline's date
	 *
	 * @throws NullPointerException if the given date string is incorrect.
	 */
	public Deadline(String s) {

		SimpleDateFormat fmt = new SimpleDateFormat(dateFmt);
		Date d = fmt.parse(s, new ParsePosition(0));

		if (d == null) {

			throw new NullPointerException("Incorrect date string");
		}

		Calendar c = new GregorianCalendar(); 
		c.setTime(d);
		year = c.get(Calendar.YEAR);
		month = c.get(Calendar.MONTH) - 1;
		dayOfMonth = c.get(Calendar.DAY_OF_MONTH);
	}

	/** Returns the deadline's year. */
	public int getYear() {

		return year;
	}

	/** Returns the deadline's month (January is 0). */
	public int getMonth() {

		return month;
	}

	/** Returns the deadline's day of month. */
	public int getDayOfMonth() {

		return dayOfMonth;
	}

	/** Returns the deadline's date string (January is "01"). */
	public String toString() {

		return (String.valueOf(year)
				+ "-"
				+ String.valueOf(month + 1)
				+ "-"
				+ String.valueOf(dayOfMonth));
	}
}