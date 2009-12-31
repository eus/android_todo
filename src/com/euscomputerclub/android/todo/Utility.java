package com.euscomputerclub.android.todo;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * Class Utility provides static utility functions for other classes in this package.
 * @author eus
 */
public class Utility {

    
    /**
     * Populates a spinner with values specified in the given array of strings.
     * 
     * @param activity the activity that has the view
     * @param spinnerResourceId the spinner to be populated
     * @param arrayStringResourceId the array of strings as the populating values
     */
    static void populateSpinner(Activity activity, int spinnerResourceId, int arrayStringResourceId) {

        Spinner s = (Spinner) activity.findViewById(spinnerResourceId);
        ArrayAdapter<CharSequence> a = ArrayAdapter.createFromResource(
        		activity,
        		arrayStringResourceId,
        		android.R.layout.simple_spinner_item);
        
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
    }

    /** Returns the integer value of a priority string. */
    static int getPriorityValue(String priority) {

       return Integer.parseInt(priority.substring(0, 1));
    }
}
