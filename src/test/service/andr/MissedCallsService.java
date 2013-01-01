package test.service.andr;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;


public class MissedCallsService extends Service {

    //use wakeLock to keep the CPU running so service can continue notifying
    PowerManager.WakeLock wakeLock;
    //continue notifications until ListMissingActivity is shown and it sends stopNotifications
    Boolean continueNotifications = false;
    //keep datetime of last missed call
    long lastCallDate = getCurrentTimeMs();
    //keep datetime of previous last missed call, to keep a range of missed calls
    long oldLastCallDate = getCurrentTimeMs();
    //we'll use the TelephonyManager to monitor to changes in phone call state 
	TelephonyManager telephonyManager;
	//notificationManager controls interactions with the notificaton area
	private NotificationManager notificationManager;

	//newInstance failed no <init>() without a default constructor
	public MissedCallsService()
	{
		super();
		
	    String startDate = (String) DateFormat.format("MM/dd/yy hh:mm", lastCallDate);
	    Log.v("Missed", "Service: startup" + startDate);
	}
	
    @Override
    public void onCreate() {
        super.onCreate();

        lastCallDate = getCurrentTimeMs();
        
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, MissedCallsService.class.getName());
    }
    
    public long getCurrentTimeMs()
    {
    	Time now = new Time();
    	now.setToNow();
    	return now.toMillis(false);
    }

    public void createNotificaton() {
        Notification notification = new Notification(R.drawable.ic_launcher, getText(R.string.notificationTickerText),
                System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, test.service.andr.ListMissingActivity.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.service),
                getText(R.string.notificationContextText), contentIntent);
        
        //add LED flashing
        notification.ledARGB = 0xff00ff00;
        notification.ledOnMS = 300;
        notification.ledOffMS = 1000;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.defaults = Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;

        //wake up the device if needed
        if (false == wakeLock.isHeld())
        	wakeLock.acquire();
        
        // Send the notification.
        notificationManager.notify(R.string.service, notification);
        
        Log.v("Missed", "Service: createNotification");
    }
    
    public Boolean checkCallLog() {
        Cursor cursor = getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI,
                null, "TYPE="+android.provider.CallLog.Calls.MISSED_TYPE, null,
                android.provider.CallLog.Calls.DATE + " ASC");
        
        int numberId = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER);
        int dateId = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE);
        int numcallTypeId = cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE);

        ArrayList<String> callList = new ArrayList<String>();
        Log.v("Missed", "Service: checkLog enter");

        if (cursor.moveToFirst()) {
        	 do {

	        	String callDate = (String) DateFormat.format("MM/dd/yy hh:mm", cursor.getLong(dateId));  
	        	String numType = cursor.getString(numcallTypeId);
	        	String callerPhoneNumber = cursor.getString(numcallTypeId);

        		Log.v("Missed", "last saved: " + lastCallDate + " " + DateFormat.format("MM/dd/yy hh:mm", lastCallDate));
        		Log.v("Missed", "last on list: " + cursor.getLong(dateId) + " " + callDate);

	        	if (cursor.getInt(numcallTypeId) == android.provider.CallLog.Calls.MISSED_TYPE)
	        	{
	        		if (lastCallDate <= cursor.getLong(dateId))
	        		{
	        			Log.v("Missed", "Service: checkLog found calls");
		        		lastCallDate = cursor.getLong(dateId);
		        		callList.add("Date: " + callDate);
						continueNotifications = true;//there really are new missed calls
	        		}
	        	}

        	} while (cursor.moveToNext());
        }
        cursor.close();
        
        Log.v("Missed", "Service: checkLog exit");
        
        return continueNotifications || callList.size() > 0;
    }
    

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        notificationManager.cancel(R.string.service);
        
        //disable the timed tasks
        handler.removeCallbacks(updateTimeTask);

        // Tell the user we stopped.
        Toast.makeText(this, getText(R.string.serviceStopped), Toast.LENGTH_SHORT).show();
        
        // and allow the device to go to sleep again 
        if (wakeLock != null && wakeLock.isHeld())
        	wakeLock.release();
        
        telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return binder;
	}
	
	private final MissedCallsRemoteInterface.Stub binder = new MissedCallsRemoteInterface.Stub() {
		public void stopCurrentNotifications() {
			continueNotifications = false;
			oldLastCallDate = lastCallDate;
			lastCallDate = getCurrentTimeMs();
			
	        // and allow the device to go to sleep again
			if (wakeLock != null && wakeLock.isHeld())
				wakeLock.release();
		}
		
		//ListViewActivity needs to know the latest missed calls
		public long getLastCallDate() {
			return oldLastCallDate;
		}
	};
	
	@Override
	public void onStart(Intent intent, int startId) {
        Toast.makeText(this, getText(R.string.serviceStarted), Toast.LENGTH_SHORT).show();
	}
	
	//post notification every minute
    private Handler handler = new Handler();
    private Runnable updateTimeTask = new Runnable() {
		public void run() {
			Log.v("Missed", "Service: updateTask enter");
			handler.removeCallbacks(this);
			if (checkCallLog())
			{
				Log.v("Missed", "Service: updateTask checkLog ok");
				createNotificaton();
				handler.postDelayed(this, 60000);
			}
		}
	};
	
	static Boolean incomingCall = false;
    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);

            switch (state) {
            case TelephonyManager.CALL_STATE_OFFHOOK:
                         break;
            case TelephonyManager.CALL_STATE_RINGING:
            			 Log.v("Missed", "Service: ringing");
            			 incomingCall = true;
                         break;
            case TelephonyManager.CALL_STATE_IDLE:
            			Log.v("Missed", "Service: idle");
            	         if (incomingCall)
            	         {
            	        	 Log.v("Missed", "Service: idle, has previos incoming");
	   			 			 incomingCall = false;
				   			 handler.removeCallbacks(updateTimeTask);
				   			 handler.postDelayed(updateTimeTask, 5000);//well, i the end it turns out we have to wait a bit longer for the content provider to sync
            	         }
                         break;
            }
        }

    };
}
