package com.example;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import static com.example.sqlite.DB.Columns.Employees;

public class MainActivity extends ListActivity {

	private Cursor employees;
	private MyDatabase db;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		db = new MyDatabase(this);
		employees = db.getEmployees(); // you would not typically call this on the main thread

		ListAdapter adapter = new SimpleCursorAdapter(this, 
				android.R.layout.simple_list_item_2,
				employees, 
				new String[] {Employees.Firstname, Employees.Lastname},
				new int[] {android.R.id.text1, android.R.id.text2});

		getListView().setAdapter(adapter);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		employees.close();
		db.close();
	}

}