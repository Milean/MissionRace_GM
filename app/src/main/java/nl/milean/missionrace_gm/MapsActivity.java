package nl.milean.missionrace_gm;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private List<String> teamnames;
    private List<Double> teamlat;
    private List<Double> teamlon;
    private List<String> teammissions;
    private List<Long> teamupdatetimes;
    HashMap<String, Marker> markers;

    float[] hues = {0, 33, 66, 99, 132, 165, 198, 231, 264, 297, 330};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Intent intent = getIntent();
        teamnames = (List<String>) intent.getSerializableExtra("teamnames");
        teamlat = (List<Double>) intent.getSerializableExtra("teamlat");
        teamlon = (List<Double>) intent.getSerializableExtra("teamlon");
        teammissions = (List<String>) intent.getSerializableExtra("teammissions");
        teamupdatetimes = (List<Long>) intent.getSerializableExtra("teamupdatetimes");
        markers = new HashMap<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        CallBackInterface.getInstance().setListener(this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        CallBackInterface.getInstance().setListener(null);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        for(int i = 0; i<teamnames.size(); i++){
            String name = teamnames.get(i);
            Double lat = teamlat.get(i);
            Double lon = teamlon.get(i);
            String mission = teammissions.get(i);
            Long updatetime = teamupdatetimes.get(i);
            Long currenttime = new Date().getTime();
            Long timeElapsed = currenttime - updatetime;
            Long minutesElapsed = TimeUnit.MILLISECONDS.toMinutes(timeElapsed);

            LatLng loc = new LatLng(lat,lon);
            Marker curTeamMarker = mMap.addMarker(new MarkerOptions().position(loc).title(""+name+" ("+mission+") - "+minutesElapsed+" mins"));
            curTeamMarker.setIcon(BitmapDescriptorFactory.defaultMarker(hues[i%hues.length]));
            markers.put(name, curTeamMarker);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.7269, 5.4613), 9.5f));
    }

    protected void showMessage(String message){
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public void processUpdate(String name, int mission, String latString, String lonString){
        Double lat = null;
        Double lon = null;
        try{
            lat = Double.parseDouble(latString);
            lon = Double.parseDouble(lonString);
        }
        catch(NumberFormatException ex){
            Log.i("GMAPP", "Wrong latitude/longitude format!");
        }

        if(markers != null && markers.containsKey(name)){
            Marker toUpdate = markers.get(name);
            if(toUpdate != null){
                toUpdate.setPosition(new LatLng(lat, lon));
                toUpdate.setTitle(""+name+" ("+mission+") - 0 mins");
            }
        }
    }
}
