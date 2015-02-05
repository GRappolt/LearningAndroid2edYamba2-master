package com.marakana.android.yamba;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	// Called to inflate the menu resource into the given Menu object
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu); // clk: not just return true, following
		// https://developer.android.com/training/basics/actionbar/adding-buttons.html
	}

	// Called every time user clicks on an action
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		case R.id.action_tweet:
			startActivity(new Intent("com.marakana.android.yamba.action.tweet"));
			return true;
		case R.id.action_refresh:
			startService(new Intent(this, RefreshService.class));
			return true;
		case R.id.action_purge:
			int rows = getContentResolver().delete(StatusContract.CONTENT_URI, null, null);
			Toast.makeText(this, "Deleted "+rows+" rows", Toast.LENGTH_LONG).show();
			return true;
		default:
			return false;
		}
	}
}
