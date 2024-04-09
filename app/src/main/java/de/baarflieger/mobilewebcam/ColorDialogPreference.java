package de.baarflieger.mobilewebcam;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.content.DialogInterface;
import android.graphics.Color;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

public class ColorDialogPreference extends DialogPreference {
	private String color = "#FFFFFFFF"; // Default color as string
	private Context mContext;

	public ColorDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		setPersistent(true);
	}

	public ColorDialogPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		setPersistent(true);
	}

	@Override
	protected void onClick() {
		int initialColor = convertColorStringToInt(color); // Convert stored string color to integer

		ColorPickerDialogBuilder
				.with(mContext)
				.setTitle("Choose color")
				.initialColor(initialColor)
				.wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
				.density(12)
				.setOnColorSelectedListener(new OnColorSelectedListener() {
					@Override
					public void onColorSelected(int selectedColor) {
						// Optional: Handle live color selection
					}
				})
				.setPositiveButton("ok", new ColorPickerClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
						color = String.format("#%08X", selectedColor);
						persistString(color); // Persist the selected color as string
						callChangeListener(color); // Notify the change listener
					}
				})
				.setNegativeButton("cancel", (dialog, which) -> {
					// Handle cancellation
				})
				.build()
				.show();
	}

	private int convertColorStringToInt(String colorStr) {
		try {
			return Color.parseColor(colorStr);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return 0xFFFFFFFF; // Default color in case of error
		}
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
		if (restorePersistedValue) {
			// Restore existing state
			color = getPersistedString("#FFFFFFFF"); // Default to white
		} else {
			// Set default state from the XML attribute
			color = (String) defaultValue;
			persistString(color);
		}
	}
}