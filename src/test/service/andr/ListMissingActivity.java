package test.service.andr;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Contacts;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListMissingActivity extends Activity{

	ListView listView;
	TextView textView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        
        textView = (TextView)this.findViewById(R.id.textView1);
        
        listView = (ListView)this.findViewById(R.id.listView1);
        //clicking items in the listview dials the number
        listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> adapterView, View view, int position,
					long id) {
				
				//initiate the call
				String phoneNumber = (String) ((TextView) view).getText();
				Pattern pattern = Pattern.compile("Number:\\s(\\+?[0-9]*)\n.*");
				Matcher matcher = pattern.matcher(phoneNumber);
				if(matcher.find())
				{
					//Intent intent = new Intent("android.intent.action.DIAL", Uri.parse(matcher.group(1)));
			        Intent intent = new Intent(Intent.ACTION_CALL);
			        intent.setData(Uri.parse("tel:"+matcher.group(1)));
			        startActivity(intent);
				}
				else
				{
					Toast.makeText(ListMissingActivity.this, getText(R.string.couldntResolveNumber), Toast.LENGTH_LONG).show();
				}
			}
		});
               
        //tell the service to stop with missed calls notification as we just checked them
        this.bindService(new Intent(ListMissingActivity.this, test.service.andr.MissedCallsService.class),
                remoteConnection, Context.BIND_AUTO_CREATE);
    }
    
    //as we need data from the remote service and bindService is asynchronous -  
    //this method will go in bindService and be executed when connection to the service has been established
    void processCallsList() {
        Cursor cursor = getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI,
                null, null, null,
                android.provider.CallLog.Calls.DATE + " DESC");
        
        int numberId = cursor.getColumnIndex(android.provider.CallLog.Calls.NUMBER);
        int dateId = cursor.getColumnIndex(android.provider.CallLog.Calls.DATE);
        int numcallTypeId = cursor.getColumnIndex(android.provider.CallLog.Calls.TYPE);
        ArrayList<String> callList = new ArrayList<String>();

        if (cursor.moveToFirst()) {
        	 do {

	        	String callDate = (String) DateFormat.format("MM/dd/yy hh:mm a", cursor.getLong(dateId));  
	        	String numType = cursor.getString(numcallTypeId);
	        	String callerPhoneNumber = cursor.getString(numberId);
	
	        	if (cursor.getInt(numcallTypeId) == android.provider.CallLog.Calls.MISSED_TYPE
	        			&& lastCallDate < cursor.getLong(dateId))
	        	{
	        		//try to match contact name to the missed call number
	        		Uri contactUri = Uri.withAppendedPath(Contacts.Phones.CONTENT_FILTER_URL, Uri.encode(callerPhoneNumber));
	        		Cursor contactsCursor = ListMissingActivity.this.getContentResolver().query(contactUri, 
	        				new String[] {	Contacts.Phones.DISPLAY_NAME,
	                        				Contacts.Phones.NUMBER },
	        				null, null, null);
	        		
	        		String contactName = "";
	                if (contactsCursor.moveToFirst())
	                    contactName = contactsCursor.getString(contactsCursor.getColumnIndex(Contacts.Phones.DISPLAY_NAME));
	        		
	        		callList.add("Number: " + callerPhoneNumber + "\n" + 
	        					 ((contactName.length() > 0) ? ("Contact: " + contactName + "\n") :  "") + 
	        					 "Date: " + callDate);
	        	}

        	} while (cursor.moveToNext());
        }
        cursor.close();
        
        listView.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, callList));
        
		textView.post(new Runnable() {
            public void run() {
            	textView.setText(getText(R.string.listCallsTitle));
            }
          });
        
        NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(R.string.service);
    }
    
    //retrieve from the remote service the datetime limits for missed calls, we want to display only the most recent ones
    long lastCallDate = 0;
    
    //interface to the service
    private MissedCallsRemoteInterface remoteInterface;
    private ServiceConnection remoteConnection = new ServiceConnection()
    {
            public void onServiceConnected(ComponentName className, IBinder service) {
            		remoteInterface = MissedCallsRemoteInterface.Stub.asInterface((IBinder)service);
            		if (remoteInterface != null)
						try {
							lastCallDate = remoteInterface.getLastCallDate();
							processCallsList();
							remoteInterface.stopCurrentNotifications();
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					else
            			Toast.makeText(getApplicationContext(), "connect to service failed", Toast.LENGTH_LONG).show();
            }
     
            public void onServiceDisconnected(ComponentName className) {
            	remoteInterface = null;
            }
    };
}
