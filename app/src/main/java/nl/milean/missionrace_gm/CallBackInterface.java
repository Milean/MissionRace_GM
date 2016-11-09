package nl.milean.missionrace_gm;

import android.app.Activity;
import android.telecom.Call;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Michiel on 18-6-2016.
 */
public class CallBackInterface {
    private static CallBackInterface instance = new CallBackInterface();
    public static CallBackInterface getInstance(){
        return instance;
    }

    MapsActivity listener;
    public CallBackInterface(){
        listener = null;
    }

    public void setListener(MapsActivity map){
        listener = map;
    }

    public void pushUpdate(String name, int mission, String lat, String lon){
        if(listener != null){
            listener.processUpdate(name, mission, lat, lon);
        }
    }

}
