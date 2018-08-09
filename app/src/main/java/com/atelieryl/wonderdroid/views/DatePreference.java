
package com.atelieryl.wonderdroid.views;

import java.text.DateFormat;
import java.util.GregorianCalendar;

import com.atelieryl.wonderdroid.R;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DatePreference extends Preference {
	private LinearLayout layout;

	public DatePreference (Context context) {
		super(context);
	}

	public DatePreference (Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DatePreference (Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private TextView current;
	private final GregorianCalendar cal = new GregorianCalendar();

	private final OnDateSetListener dateSetListener = new OnDateSetListener() {

		public void onDateSet (DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			cal.set(year, monthOfYear, dayOfMonth);
			Editor editor = getSharedPreferences().edit();
			editor.putLong(getKey(), cal.getTimeInMillis());
			editor.commit();
			current.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(cal.getTime()));
		}
	};

	@Override
	protected View onCreateView (ViewGroup parent) {

		LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout layout = (LinearLayout)layoutInflater.inflate(R.layout.datepref, null);

		TextView title = (TextView)layout.getChildAt(0);
		title.setText(getTitle());

		SharedPreferences prefs = getSharedPreferences();
		final long currentDate = prefs.getLong(getKey(), 0);
		current = (TextView)layout.getChildAt(1);
		cal.setTimeInMillis(currentDate);

		current.setText(DateFormat.getDateInstance(DateFormat.SHORT).format(cal.getTime()));

		Button set = (Button)layout.getChildAt(2);

		set.setOnClickListener(new OnClickListener() {

			public void onClick (View v) {
				new DatePickerDialog(getContext(), dateSetListener, cal.get(GregorianCalendar.YEAR),
					cal.get(GregorianCalendar.MONTH), cal.get(GregorianCalendar.DAY_OF_MONTH)).show();
			}
		});

		return layout;
	}

	@Override
	public View getView (View convertView, ViewGroup parent) {
		convertView = this.layout == null ? onCreateView(parent) : this.layout;
		return convertView;
	}

}
