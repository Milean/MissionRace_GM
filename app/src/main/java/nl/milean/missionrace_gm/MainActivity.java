package nl.milean.missionrace_gm;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import nl.milean.missionrace_gm.messages.QuickstartPreferences;
import nl.milean.missionrace_gm.messages.RegistrationIntentService;
import nl.milean.missionrace_gm.missiondata.Mission;
import nl.milean.missionrace_gm.missiondata.MissionParams;

public class MainActivity extends AppCompatActivity {

    //For GCM
    protected GoogleApiClient mGoogleApiClient;
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private BroadcastReceiver mMessageReceiver;

    private List<EditText> teamNames;
    private List<TextView> teamProgress;
    private List<TextView> teamLastUpdateTimers;
    private List<Button> teamApprovalButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setupMessaging();

        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("gcm_message_event"));

    }

    @Override
    protected void onResume(){
        super.onResume();
        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedpreferences), Context.MODE_PRIVATE);

        teamNames = new ArrayList<>();
        teamProgress = new ArrayList<>();
        teamLastUpdateTimers = new ArrayList<>();
        teamApprovalButtons = new ArrayList<>();

        for(int i = 0; i < 12; i++){

            int teamNameResourceID = getResources().getIdentifier("teamname" + i, "id", getPackageName());
            EditText teamnameText = (EditText) findViewById(teamNameResourceID);
            teamNames.add(teamnameText);

            int missionResourceID = getResources().getIdentifier("mission" + i, "id", getPackageName());
            TextView teamprogressText = (TextView) findViewById(missionResourceID);
            teamProgress.add(teamprogressText);

            int timetrackerResourceID = getResources().getIdentifier("timeTracker" + i, "id", getPackageName());
            TextView timetracker = (TextView) findViewById(timetrackerResourceID);
            teamLastUpdateTimers.add(timetracker);

            int buttonResourceID = getResources().getIdentifier("approve" + i, "id", getPackageName());
            Button approveButton = (Button) findViewById(buttonResourceID);
            teamApprovalButtons.add(approveButton);

            if(sharedPref.contains("teamname"+i)){
                String name = sharedPref.getString("teamname" + i, "");
                String mission = sharedPref.getString("mission" + i, "0");

                teamNames.get(i).setText(name);
                teamProgress.get(i).setText("(" + mission + ") " + getNameForMissionNumber(mission));
            }
        }
        updateTeamTimeouts();
    }

    @Override
    protected void onPause() {
        super.onPause();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedpreferences), Context.MODE_PRIVATE);
        for(int i = 0; i < 12; i++){


            if(teamNames.size() >= i && teamNames.get(i) != null) {
                String name = teamNames.get(i).getText().toString().trim();
                if(name.length() > 0){
                    sharedPref.edit().putString("teamname" + i, name)
                            .apply();
                }
                else{
                    sharedPref.edit()
                            .remove("teamname" + i)
                            .remove("lastlat"+i)
                            .remove("lastlon"+i)
                            .remove("mission"+i)
                            .remove("updatetime"+i)
                            .apply();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_map:
                loadMap();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    public void loadMap(){
        ArrayList<String> teams = new ArrayList<>();
        ArrayList<Double> lats = new ArrayList<>();
        ArrayList<Double> lons = new ArrayList<>();
        ArrayList<String> missions = new ArrayList<>();
        ArrayList<Long> updatetimes = new ArrayList<>();

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedpreferences), Context.MODE_PRIVATE);
        for(int i = 0; i < 12; i++){

            if(sharedPref.contains("teamname" + i)
                    && sharedPref.contains("lastlat" + i)
                    && sharedPref.contains("lastlon" + i)){

                String latString = sharedPref.getString("lastlat" + i, "0");
                String lonString = sharedPref.getString("lastlon" + i, "0");
                if(latString.length()>1 && lonString.length()>1){
                    teams.add(sharedPref.getString("teamname" + i, "-"));
                    lats.add(Double.parseDouble(latString));
                    lons.add(Double.parseDouble(lonString));
                    missions.add(sharedPref.getString("mission" + i, "-"));
                    updatetimes.add(sharedPref.getLong("updatetime" + i, 0l));
                }
            }
        }

        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("teamnames", teams);
        intent.putExtra("teamlat", lats);
        intent.putExtra("teamlon", lons);
        intent.putExtra("teammissions", missions);
        intent.putExtra("teamupdatetimes", updatetimes);
        startActivity(intent);
    }

    private void updateInfo(String receivedTeam, int receivedMission, String lat, String lon){
        Log.i("GMAPP", "Updating info for team " + receivedTeam);
        int teamno = findTeamIDByName(receivedTeam);
        if(teamno < 0){
            Log.i("GMAPP", "Teamname not found yet. Finding first free position.");
            teamno = firstFreeTeam();
            if(teamno < 0){
                Log.i("GMAPP", "No space left!.");
                showMessage("Please make room for team "+receivedTeam);
                return;
            }
            else{
                Log.i("GMAPP", "New team will be given position "+teamno);
                teamNames.get(teamno).setText(receivedTeam);
            }
        }

        teamProgress.get(teamno).setText("(" + receivedMission + ") " + getNameForMissionNumber(receivedMission));
        CallBackInterface.getInstance().pushUpdate(receivedTeam, receivedMission, lat, lon);

        Log.i("SENDLOCATION", "Processing team update for team " + receivedTeam);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedpreferences), Context.MODE_PRIVATE);
        sharedPref.edit().putString("teamname" + teamno, receivedTeam)
                .putString("mission"+teamno,""+receivedMission)
                .putLong("updatetime" + teamno, new Date().getTime())
                .apply();

        if(lat.length()>0 && lon.length()>0){
            sharedPref.edit()
                    .putString("lastlat" + teamno, lat)
                    .putString("lastlon" + teamno, lon)
                    .apply();
            Log.i("SENDLOCATION", "Location received and stored in the preferences.");
        }
        updateTeamTimeouts();
    }

    private void updateTeamTimeouts(){
        for(int i = 0; i < 12; i++) {
            if (teamNames.size() >= i && teamNames.get(i) != null) {
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedpreferences), Context.MODE_PRIVATE);
                Long updateTime = sharedPref.getLong("updatetime" + i, 0l);
                if(updateTime == 0l){
                    teamLastUpdateTimers.get(i).setText("never");
                }
                else{
                    Long currenttime = new Date().getTime();
                    Long timeElapsed = currenttime - updateTime;
                    Long minutesElapsed = TimeUnit.MILLISECONDS.toMinutes(timeElapsed);
                    teamLastUpdateTimers.get(i).setText(""+minutesElapsed+" mins");
                }
            }
        }
    }

    private int findTeamIDByName(String teamname){
        for(int i = 0; i < 12; i++){
            if(teamNames.size() >= i && teamNames.get(i) != null) {
                String name = teamNames.get(i).getText().toString();
                Log.i("GMAPP", "Comparing ("+i+") name '"+name+"' to '"+teamname+"' is: "+(name != null && name.trim().equalsIgnoreCase(teamname.trim())));
                if (name != null && name.trim().equalsIgnoreCase(teamname.trim())) {
                    Log.i("GMAPP", "Returning " + i);
                    return i;
                }
            }
        }
        return -1;
    }
    private int firstFreeTeam(){
        for(int i = 0; i < 12; i++){
            if(teamNames.size() >= i && teamNames.get(i) != null) {
                String name = teamNames.get(i).getText().toString();
                if (name == null || name.length()==0) {
                    Log.i("GMAPP", "Returning that field "+i+" is available because the string is empty.");
                    return i;
                }
            }
            else{
                Log.i("GMAPP", "Error: Returning that field "+i+" is available because it does not exist? Weird.");
                return i;
            }
        }
        return -1;
    }

    public void actionApproveTeam0(View view){ approveTeam(0); }
    public void actionApproveTeam1(View view){ approveTeam(1); }
    public void actionApproveTeam2(View view){ approveTeam(2); }
    public void actionApproveTeam3(View view){ approveTeam(3); }
    public void actionApproveTeam4(View view){ approveTeam(4); }
    public void actionApproveTeam5(View view){ approveTeam(5); }
    public void actionApproveTeam6(View view){ approveTeam(6); }
    public void actionApproveTeam7(View view){ approveTeam(7); }
    public void actionApproveTeam8(View view){ approveTeam(8); }
    public void actionApproveTeam9(View view){ approveTeam(9); }
    public void actionApproveTeam10(View view){ approveTeam(10); }
    public void actionApproveTeam11(View view){ approveTeam(11); }

    private void approveTeam(int teamNum){


        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {

            SharedPreferences sharedPref = getSharedPreferences(getString(R.string.sharedpreferences), Context.MODE_PRIVATE);
            final String missionString = sharedPref.getString("mission" + teamNum, "0");
            int mission = Integer.parseInt(missionString);

            if (mission + 1 >= MissionParams.getInstance().size()){
                new AlertDialog.Builder(this)
                        .setTitle("Not possible")
                        .setMessage("You cannot approve someone who has already reached the last mission.")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setNegativeButton(android.R.string.cancel, null).show();
                return;
            }

            if(teamNames.size() >= teamNum && teamNames.get(teamNum) != null) {
                final String name = teamNames.get(teamNum).getText().toString().trim();
                if (name.length() > 0) {

                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Approval")
                            .setMessage("Do you really want to give team '"+name+"' approval for assignment ("+missionString+") "+ getNameForMissionNumber(missionString)+"?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new SendApprovalTask().execute(name, missionString);
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }
                else{
                    showMessage("Enter a tean name first.");
                }
            }
        } else {
            showMessage("No network connectivity.");
        }

    }

    private String getNameForMissionNumber(String number){
        try{
            int num = Integer.parseInt(number);
            return getNameForMissionNumber(num);
        }
        catch(NumberFormatException ex){
            //do nothing
        }
        return "";
    }

    private String getNameForMissionNumber(int number){
        Mission m = MissionParams.getInstance().getMission(number);
        if(m != null && m.containsData("title")){
            return (String) m.getData("title");
        }
        return "";
    }

    private class SendApprovalTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... teamnames){
            if (teamnames.length != 2){
                return null;
            }
            else {
                String teamname = teamnames[0];
                String missionString = teamnames[1];

                try {
                    JSONObject jGcmData = new JSONObject();
                    jGcmData.put("to", "/topics/global");

                    JSONObject data = new JSONObject();
                    data.put("message", "Approved");
                    data.put("team", teamname);
                    data.put("assignment", missionString);
                    Log.i("GMAPP", "Sending approval to team '"+teamname+"'");

                    jGcmData.put("data", data);

                    // Create connection to send GCM Message request.
                    URL url = new URL("https://android.googleapis.com/gcm/send");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("Authorization", "key="+getString(R.string.GCM_Client_API_KEY));
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);

                    Log.i("GMAPP", "About to Approve player " + teamname + ".");

                    // Send GCM message content.
                    OutputStream outputStream = conn.getOutputStream();
                    outputStream.write(jGcmData.toString().getBytes());

                    Log.i("GMAPP", "GCM Message sent.");

                    // Read GCM response.
                    InputStream inputStream = conn.getInputStream();
                    Scanner s = new Scanner(inputStream).useDelimiter("\\A");
                    if (s.hasNext()) {
                        String resp = s.next();
                        s.close();
                        return resp;
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        }
        protected void onPostExecute(String result) {
            Log.i("Message", "GCM Response: " + result);
        }
    }

    /**
     * Start of GCM block
     */

    protected void setupMessaging(){
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences.getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
//                    showMessage("Ready to receive messages.");
                } else {
                    showMessage("Error occurred while registering for Google Cloud Messaging. Try stopping/starting the app.");
                }
            }
        };
        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String receivedTeam = intent.getStringExtra("team");
                Log.i("GMAPP", "Received team: '"+receivedTeam+"'");
                int receivedMission = intent.getIntExtra("mission", 0);
                String lat = intent.getStringExtra("lat");
                String lon = intent.getStringExtra("lon");

                updateInfo(receivedTeam, receivedMission, lat, lon);

//                showMessage("Team update: "+receivedTeam+" | mission: "+receivedMission+" | location: "+lat+","+lon);
            }
        };

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    protected void showMessage(String message){
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    /**
     * End of GCM block
     */

}
