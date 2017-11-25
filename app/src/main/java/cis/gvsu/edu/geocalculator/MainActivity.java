package cis.gvsu.edu.geocalculator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.location.Location;
import java.text.DecimalFormat;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import android.view.inputmethod.InputMethodManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Places;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.parceler.Parcels;

import cis.gvsu.edu.geocalculator.webservice.WeatherService;

import static cis.gvsu.edu.geocalculator.webservice.WeatherService.SHOW_WEATHER;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    public static int SETTINGS_RESULT = 1;
    public static int HISTORY_RESULT = 2;
    public static int SEARCH_RESULT = 3;

    private String bearingUnits = "degrees";
    private String distanceUnits = "kilometers";

    private EditText p1Lat = null;
    private EditText p1Lng = null;
    private EditText p2Lat = null;
    private EditText p2Lng = null;
    private TextView distance = null;
    private TextView bearing = null;

    private ImageView p1Icon = null;
    private ImageView p2Icon = null;

    private TextView p1Temp = null;
    private TextView p1Summary = null;
    private TextView p2Temp = null;
    private TextView p2Summary = null;

    public static List<LocationLookup> allHistory;
    DatabaseReference topRef;

    private static String TAG = "WeatherService";

    private BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent);
            Bundle bundle = intent.getExtras();
            double temp = bundle.getDouble("TEMPERATURE");
            String summary = bundle.getString("SUMMARY");
            String icon = bundle.getString("ICON").replaceAll("-", "_");
            String key = bundle.getString("KEY");
            int resID = getResources().getIdentifier(icon , "drawable", getPackageName());
            setWeatherViews(View.VISIBLE);
            if (key.equals("p1"))  {
                p1Summary.setText(summary);
                p1Temp.setText(Double.toString(temp));
                p1Icon.setImageResource(resID);
            } else {
                p2Summary.setText(summary);
                p2Temp.setText(Double.toString(temp));
                p2Icon.setImageResource(resID);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        allHistory = new ArrayList<LocationLookup>();

        GoogleApiClient apiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        Button clearButton = (Button)this.findViewById(R.id.clear);
        Button calcButton = (Button)this.findViewById(R.id.calc);
        Button searchButton = (Button)this.findViewById(R.id.search_button);

        p1Lat = (EditText) this.findViewById(R.id.p1Lat);
        p1Lng = (EditText) this.findViewById(R.id.p1Lng);
        p2Lat = (EditText) this.findViewById(R.id.p2Lat);
        p2Lng = (EditText) this.findViewById(R.id.p2Lng);
        distance = (TextView) this.findViewById(R.id.distance);
        bearing = (TextView) this.findViewById(R.id.bearing);

        p1Icon = (ImageView) this.findViewById(R.id.p1Icon);
        p2Icon = (ImageView) this.findViewById(R.id.p2Icon);
        p1Temp = (TextView) this.findViewById(R.id.p1Temp);
        p2Temp = (TextView) this.findViewById(R.id.p2Temp);
        p1Summary = (TextView) this.findViewById(R.id.p1Summary);
        p2Summary = (TextView) this.findViewById(R.id.p2Summary);

        clearButton.setOnClickListener(v -> {
            hideKeyboard();
            p1Lat.getText().clear();
            p1Lng.getText().clear();
            p2Lat.getText().clear();
            p2Lng.getText().clear();
            distance.setText("Distance:");
            bearing.setText("Bearing:");
            this.setWeatherViews(View.INVISIBLE);
        });


        calcButton.setOnClickListener(v -> {
            updateScreen();
        });

        searchButton.setOnClickListener(v -> {
            Intent locationSearch = new Intent(MainActivity.this, LocationSearchActivity.class);
            startActivityForResult(locationSearch, SEARCH_RESULT);
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        allHistory.clear();
        topRef = FirebaseDatabase.getInstance().getReference("history");
        topRef.addChildEventListener (chEvListener);
        //topRef.addValueEventListener(valEvListener);
        IntentFilter weatherFilter = new IntentFilter(SHOW_WEATHER);
        LocalBroadcastManager.getInstance(this).registerReceiver(weatherReceiver, weatherFilter);
        this.setWeatherViews(View.INVISIBLE);
    }

    @Override
    public void onPause(){
        super.onPause();
        topRef.removeEventListener(chEvListener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(weatherReceiver);
    }

    private void setWeatherViews(int visible) {
        p1Icon.setVisibility(visible);
        p2Icon.setVisibility(visible);
        p1Summary.setVisibility(visible);
        p2Summary.setVisibility(visible);
        p1Temp.setVisibility(visible);
        p2Temp.setVisibility(visible);
    }

    private void updateScreen()
    {
        try {
            Double lat1 = Double.parseDouble(p1Lat.getText().toString());
            Double lng1 = Double.parseDouble(p1Lng.getText().toString());
            Double lat2 = Double.parseDouble(p2Lat.getText().toString());
            Double lng2 = Double.parseDouble(p2Lng.getText().toString());


            Location p1 = new Location("");//provider name is unecessary
            p1.setLatitude(lat1);//your coords of course
            p1.setLongitude(lng1);

            Location p2 = new Location("");
            p2.setLatitude(lat2);
            p2.setLongitude(lng2);

            double b = p1.bearingTo(p2);
            double d = p1.distanceTo(p2) / 1000.0d;

            if (this.distanceUnits.equals("Miles")) {
                d *= 0.621371;
            }

            if (this.bearingUnits.equals("Mils")) {
                b *= 17.777777778;
            }

            DecimalFormat df = new DecimalFormat("#.##");
            df.setRoundingMode(RoundingMode.CEILING);


            String dStr = "Distance: " + df.format(d) + " " + this.distanceUnits;
            distance.setText(dStr);

            String bStr = "Bearing: " + df.format(b) + " " + this.bearingUnits;
            bearing.setText(bStr);

            // remember the calculation.
//            HistoryContent.HistoryItem item = new HistoryContent.HistoryItem(lat1.toString(),
//                    lng1.toString(), lat2.toString(), lng2.toString(), DateTime.now());
//            HistoryContent.addItem(item);

            long time = System.currentTimeMillis() / 1000;
            WeatherService.startGetWeather(this, Double.toString(lat1), Double.toString(lng1), "p1", time);
            WeatherService.startGetWeather(this, Double.toString(lat2), Double.toString(lng2), "p2", time);

            LocationLookup entry = new LocationLookup();
            entry.setOrigLat(lat1);
            entry.setOrigLng(lng1);
            entry.setDestLat(lat2);
            entry.setDestLng(lng2);
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            entry.setTimestamp(fmt.print(DateTime.now()));
            topRef.push().setValue(entry);



            hideKeyboard();
        } catch (Exception e) {
            return;
        }

    }

    private void hideKeyboard()
    {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            //this.getSystemService(Context.INPUT_METHOD_SERVICE);
            InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SETTINGS_RESULT) {
            this.bearingUnits = data.getStringExtra("bearingUnits");
            this.distanceUnits = data.getStringExtra("distanceUnits");
            updateScreen();
        } else if (requestCode == HISTORY_RESULT) {
            if (data != null && data.hasExtra("item")) {
                Parcelable par = data.getParcelableExtra("item");
                LocationLookup ll = Parcels.unwrap(par);
                this.p1Lat.setText(Double.toString(ll.getOrigLat()));
                this.p1Lng.setText(Double.toString(ll.getOrigLng()));
                this.p2Lat.setText(Double.toString(ll.getDestLat()));
                this.p2Lng.setText(Double.toString(ll.getDestLng()));
                this.updateScreen();  // code that updates the calcs.
            }
        } else if (requestCode == SEARCH_RESULT) {
            if (data != null && data.hasExtra("SEARCH_RESULTS")) {
                Parcelable parcel = data.getParcelableExtra("SEARCH_RESULTS");
                LocationLookup l = Parcels.unwrap(parcel);
                //Log.d("MainACtivity", "New Trip: " + t.name);
                this.p1Lat.setText(String.valueOf(l.origLat));
                this.p1Lng.setText(String.valueOf(l.origLng));
                this.p2Lat.setText(String.valueOf(l.destLat));
                this.p2Lng.setText(String.valueOf(l.destLng));
                this.updateScreen();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_settings) {
            Intent intent = new Intent(MainActivity.this, MySettingsActivity.class);
            startActivityForResult(intent, SETTINGS_RESULT );
            return true;
        } else if(item.getItemId() == R.id.action_history) {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
//            Bundle bundle = new Bundle();
//            //bundle.putParcelableArrayList("items",  allHistory);
//            intent.putExtras(bundle);
            startActivityForResult(intent, HISTORY_RESULT );
            return true;
        }
        return false;
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private ChildEventListener chEvListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            LocationLookup entry = (LocationLookup) dataSnapshot.getValue(LocationLookup.class);
            entry._key = dataSnapshot.getKey();
            allHistory.add(entry);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            LocationLookup entry = (LocationLookup) dataSnapshot.getValue(LocationLookup.class);
            List<LocationLookup> newHistory = new ArrayList<LocationLookup>();
            for (LocationLookup t : allHistory) {
                if (!t._key.equals(dataSnapshot.getKey())) {
                    newHistory.add(t);
                }
            }
            allHistory = newHistory;
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    };
}
