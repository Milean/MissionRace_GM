/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.milean.missionrace_gm.messages;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class GCMBroadcastReceiver extends GcmListenerService {

    private static final String TAG = "GMAPP-Broadcast";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String team = data.getString("team");
        Log.i("GMAPP", "Received team '"+team+"'");
        String mission = data.getString("mission");
        String lat = data.getString("lat");
        String lon = data.getString("lon");

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "About: " + team + " at mission: " + mission + " on location: " + lat + "," + lon);

        Intent messageIntent = new Intent("gcm_message_event");
        messageIntent.putExtra("team", team);

        try{
            int missionInt = Integer.parseInt(mission);
            messageIntent.putExtra("mission", missionInt);
        }
        catch(NumberFormatException ex){/*Do nothing*/};

        messageIntent.putExtra("lat", lat);
        messageIntent.putExtra("lon", lon);
//
//
//        if(lat.length()>0 && lon.length()>0){
//            Log.i("SENDLOCATION", "Real location received!");
//
//            try {
//                Location targetLocation = new Location("");
//                targetLocation.setLatitude(Double.parseDouble(lat));
//                targetLocation.setLongitude(Double.parseDouble(lon));
//                messageIntent.putExtra("location", targetLocation);
//                Log.i("SENDLOCATION", "Real location processed: "+targetLocation.toString());
//            }
//            catch(Exception ex){
//                Log.i("SENDLOCATION", "Numberformatexception");
//                //Unable to parse location. Aborting.
//            }
//
//        }

        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    }

}
