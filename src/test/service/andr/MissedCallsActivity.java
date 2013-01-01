package test.service.andr;

import test.service.andr.MissedCallsService;
import test.service.andr.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MissedCallsActivity extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mainActivity = this;
        startStopButton = (Button)this.findViewById(R.id.button1);
        startStopButton.setOnClickListener(this);
        
        if (isMyServiceRunning())
        {
        	startStopButton.setText(getText(R.string.serviceStopService));
        }
    }
	
    ComponentName serviceResult;
	public void onClick(View v) {
		 if (startStopButton.getText() == getText(R.string.serviceStopService))
		 {
			 startStopButton.setText(getText(R.string.serviceStartService));
			 Boolean stopped = stopService(new Intent(MissedCallsActivity.this, MissedCallsService.class));
			 if (!stopped)
				 Toast.makeText(mainActivity, getText(R.string.serviceFailedToStop), Toast.LENGTH_LONG).show();
			 return;
		 }
		 startStopButton.setText(getText(R.string.serviceStopService));
		 
		 serviceResult = startService(new Intent(MissedCallsActivity.this, MissedCallsService.class));
		 if (serviceResult == null)
			 Toast.makeText(this, getText(R.string.serviceFailedToStart), Toast.LENGTH_LONG).show();
	}
    
    Button startStopButton;
    Activity mainActivity;

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MissedCallsService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    // Initiating Menu XML file (menu.xml)
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.layout.menu, menu);
        return true;
    }
    
    /**
     * Event Handling for Individual menu item selected
     * Identify single menu item by it's id
     * */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
 
        switch (item.getItemId())
        {
        case R.id.menu_about:
            // Single menu item is selected do something
            // Ex: launching new activity/screen or show alert message
        	showAboutDialog();
            return true;
 
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    public void showAboutDialog()
    {
    	Context mContext = getApplicationContext();
    	
    	final Dialog dialog = new Dialog(MissedCallsActivity.this);
    	dialog.setContentView(R.layout.about_dialog);
    	dialog.setTitle(getText(R.string.menuAboutTitle));
    	
    	TextView text = (TextView) dialog.findViewById(R.id.text);
    	text.setText(getText(R.string.menuAboutText));
    	
    	Button button = (Button) dialog.findViewById(R.id.okbutton);
    	button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.cancel();
            }
        });
    	
//    	ImageView image = (ImageView) dialog.findViewById(R.id.image);
//    	image.setImageResource(R.drawable.android);
    	
    	dialog.show();
    }
}