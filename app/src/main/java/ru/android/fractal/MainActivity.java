package ru.android.fractal;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ZoomControls;

public class MainActivity extends Activity implements View.OnClickListener {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		getButtonStart().setOnClickListener(this);

		MasterContext mc = (MasterContext) this.findViewById(R.id.surfaceView1);
		
		((Button)this.findViewById(R.id.button2)).setOnClickListener(new ZoomOnClickListener(mc, true));
		((Button)this.findViewById(R.id.Button01)).setOnClickListener(new ZoomOnClickListener(mc, false));
	}

	private class ZoomOnClickListener implements View.OnClickListener {
		MasterContext masterContext;
		boolean zoomIn = true;

		public ZoomOnClickListener(MasterContext masterContext, boolean zoomIn) {
			super();
			this.masterContext = masterContext;
			this.zoomIn = zoomIn;
		}

		public void onClick(View v) {
			masterContext.zoom(zoomIn, 8);
		}
	}

	@Override
	public void onClick(View v) {
		MasterContext mc = (MasterContext) this.findViewById(R.id.surfaceView1);
		if (v == getButtonStart()) {
			if (mc.isInProgress()) {
				mc.stop();
			} else {
				mc.start();
			}
		}
	}

	public Button getButtonStart() {
		return (Button) this.findViewById(R.id.button1);
	}

	public ZoomControls getButtonZoom() {
		return null;//(ZoomControls) this.findViewById(R.id.zoomControls1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);

		// menu.add(Menu.NONE, 0, 0, "Show current settings");
		// return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item1:
			startActivity(new Intent(this, ShowSettingsActivity.class));
			return true;
		}
		return false;
	}
}