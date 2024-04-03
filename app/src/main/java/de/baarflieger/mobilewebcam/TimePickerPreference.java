/* Copyright 2012 Michael Haar

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package de.baarflieger.mobilewebcam;

import android.content.Context;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;
 
/**
 * A preference type that allows a user to choose a time
 */
public class TimePickerPreference extends DialogPreference
{ 
	/**
	 * The validation expression for this preference
	 */
	private static final String VALIDATION_EXPRESSION = "[0-2]*[0-9]:[0-5]*[0-9]";
 
	/**
	 * The default value for this preference
	 */
	private String defaultValue;
 
	/**
	 * @param context
	 * @param attrs
	 */
	public TimePickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}
 
	/**
	 * @param context
	 * @param attrs
	 * @param defStyle
	 */
	public TimePickerPreference(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
 
	/**
	 * Initialize this preference
	 */
	private void initialize() {
		setPersistent(true);
	}
 
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.DialogPreference#onCreateDialogView()
	 */
	@Override
	protected View onCreateDialogView()
	{
 		mTP = new TimePicker(getContext());
 
		boolean is24hour = DateFormat.is24HourFormat(mTP.getContext());
		try
		{
			mTP.setIs24HourView(is24hour);
			
			int h = getHour();
			int m = getMinute();
			if (h >= 0 && h <= (is24hour ? 24 : 12))
				mTP.setCurrentHour(h);
			if (m >= 0 && m <= 60)
				mTP.setCurrentMinute(m);
		}
		catch(IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		
		return mTP;
	}
 
	private TimePicker mTP = null; //to get this dialog from other functions

	@Override
	protected void onDialogClosed (boolean positiveResult)
	{
		if (positiveResult && mTP != null)
		{
			mTP.clearFocus();
			int hour = mTP.getCurrentHour();
			int minute = mTP.getCurrentMinute();
			String result = hour + ":" + minute;
			persistString(result);
			callChangeListener(result);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.preference.Preference#setDefaultValue(java.lang.Object)
	 */
	@Override
	public void setDefaultValue(Object defaultValue) {
		// BUG this method is never called if you use the 'android:defaultValue' attribute in your XML preference file, not sure why it isn't		
 
		super.setDefaultValue(defaultValue);
 
		if (!(defaultValue instanceof String)) {
			return;
		}
 
		if (!((String) defaultValue).matches(VALIDATION_EXPRESSION)) {
			return;
		}
 
		this.defaultValue = (String) defaultValue;
	}
 
	/**
	 * Get the hour value (in 24 hour time)
	 * 
	 * @return The hour value, will be 0 to 23 (inclusive)
	 */
	private int getHour() {
		String time = getPersistedString(this.defaultValue);
		if (time == null || !time.matches(VALIDATION_EXPRESSION)) {
			return -1;
		}
 
		return Integer.valueOf(time.split(":")[0]);
	}
 
	/**
	 * Get the minute value
	 * 
	 * @return the minute value, will be 0 to 59 (inclusive)
	 */
	private int getMinute() {
		String time = getPersistedString(this.defaultValue);
		if (time == null || !time.matches(VALIDATION_EXPRESSION)) {
			return -1;
		}
 
		return Integer.valueOf(time.split(":")[1]);
	}
}