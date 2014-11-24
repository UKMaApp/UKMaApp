package com.weebly.cs499fall14.ukmaapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements LocationListener, LocationSource {
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationSource.OnLocationChangedListener mListener;
    private LocationManager locationManager;
    private HashMap<String, Building> mBuildingHash = new HashMap<String, Building>();
    private ArrayList<Marker> mMarkerArray = new ArrayList<Marker>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        setUpMapIfNeeded();
        setUpMarkers();
    }

    @Override
    public void onPause() {
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        if (locationManager != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    // Change markers button listener
    public void changeMarkersListener(View view) {
        float toggle = 1.0f;
        if (mMarkerArray.get(0).getAlpha() != 0.0f) {
            toggle = 0.0f;
        }
        for (Marker m : mMarkerArray) {
            m.setAlpha(toggle); // toggle transparency
        }
    }

    // Search button listener
    public void searchListener(View view) {
        // Close keyboard
        InputMethodManager inputManager = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null :
                getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        // Getting reference to EditText to get the user input location
        EditText etLocation = (EditText) findViewById(R.id.et_location);

        // Getting user input location
        String location = etLocation.getText().toString().toLowerCase();
        Marker closestMarker = null;

        if (!location.equals("")) {
            int closestDistance = 1000;
            for (Marker m : mMarkerArray) {
                String markerName = mBuildingHash.get(m.getTitle()).name;
                m.setAlpha(0.0f); // make all markers transparent

                if (location.equals(markerName.toLowerCase())) {
                    closestMarker = m;
                    break;
                }

                // Check building code like "RGAN"
                int thisDistance = levenshteinDistance(location, mBuildingHash.get(markerName).code);
                if (thisDistance < closestDistance) {
                    closestDistance = thisDistance;
                    closestMarker = m;
                }

                // Check actual name like "Ralph G. Anderson Building"
                // Substring because you want to compare "Ralph" to "Ralph" instead of
                //   "Ralph" to "Ralph G. Anderson" with the Levenshtein algorithm
                String subMarkerName = markerName.substring(0, Math.min(markerName.length(), location.length()));
                thisDistance = levenshteinDistance(location, subMarkerName);
                if (thisDistance < closestDistance) {
                    closestDistance = thisDistance;
                    closestMarker = m;
                }
            }
        }
        if (closestMarker != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(closestMarker.getPosition()));
            closestMarker.showInfoWindow();
            closestMarker.setAlpha(1.0f); // Make matched marker full visible
        }
    }

    // Algorithm implementation from http://rosettacode.org/wiki/Levenshtein_distance#Java
    // It finds the Levenshtein distance between two string (an int) and returns it
    public static int levenshteinDistance(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();

        int[] costs = new int[b.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;

            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    public class Building {
        public String name;
        public String code;
        public double lat;
        public double lng;
        public String url;

        /* Constuctors are cool. This one will fill the fields based on the
           incoming array "tokens" which represents a row from the table where
           each index is a column. example
           tokens[x] = {White Hall, CB, 38.038079, -84.503844, http://www.whitehall.com/}

         * Once the fields are filled the Building is hashed by its name so it can be
           searched for later like this: buildingHash.get("White Hall")

         * NOTE: It will only hash the object if lat and lng are given and numeric */
        Building(String[] tokens) {
            name = "Error";
            switch (tokens.length) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    if (isNumeric(tokens[2]) && isNumeric(tokens[3])) {
                        name = tokens[0];
                        code = tokens[1];
                        lat = Double.parseDouble(tokens[2]);
                        lng = Double.parseDouble(tokens[3]);
                    }
                    break;
                case 5:
                    if (isNumeric(tokens[2]) && isNumeric(tokens[3])) {
                        name = tokens[0];
                        code = tokens[1];
                        lat = Double.parseDouble(tokens[2]);
                        lng = Double.parseDouble(tokens[3]);
                        url = tokens[4];
                    }
                    break;
                default:
                    break;
            }
            if (!name.equals("Error")) {
                mBuildingHash.put(name, this);
            }
        }
    }

    private void setUpMarkers() {
        BufferedReader reader = null;
        try {
            // Open the .csv (comma separated value) file
            reader = new BufferedReader(new InputStreamReader(getAssets().open("Buildings.csv")));

            String mLine = reader.readLine(); // read first line
            while (mLine != null) {
                // Split lines (by commas) into tokens, example
                // tokens[x] = {White Hall,CB,38.038079,-84.503844,http://www.whitehall.com/}
                String[] tokens = mLine.split(",");

                // The constructor fills up the Building and hashes it by its name
                Building bldg = new Building(tokens);

                // Create a marker for the current Building and add it to the map
                if (!bldg.name.equals("Error")) {
                    Marker mMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(bldg.lat, bldg.lng))
                            .title(bldg.name) // this is what we search clicked markers by so beware
                            .alpha(1.0f) // 0.0 (invisible) - 1.0 (fully visible)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)) // make pin blue
                            .snippet(bldg.code + " Hours: 8-5")); // We will just add a new column for hours of operation
                    mMarkerArray.add(mMarker); // This is so we can loop through markers later
                }
                mLine = reader.readLine(); // increment line
            }
        } catch (IOException e) {
            //error opening file probably
            Log.e("setUpMarkers", "Error opening file");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //error closing file probably
                    Log.e("setUpMarkers()", "Error closing file");
                }
            }
        }

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // Search hash by marker title (which is a name) then take the object's url
                String url = mBuildingHash.get(marker.getTitle()).url;

                if (url != null) {
                    Intent openUrl = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(openUrl);
                }
            }
        });
    }

    // Takes a string and return true if it is a double, else false
    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
            d = d + d;
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
                mMap.setLocationSource(this);
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        // For ping location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            boolean gpsIsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkIsEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

/*            if (gpsIsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10F, this);
            } else if (networkIsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 10F, this);
            } else {
                //Show some kind of message that tells the user that the GPS is disabled.
                Log.e("setUpMap()","GPS is disabled");
            }*/
        } else {
            //Show some generic error dialog because something must have gone wrong with location manager.
            Log.e("setUpMap()", "Location Manager error");
        }

        // Set zoom and tilt
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(38.038024, -84.504686))
                .zoom(18)
                .tilt(30) // Sets the tilt of the camera to 30 degrees
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        /*// Add Ground Overlay
        GroundOverlayOptions campusMap = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher))
                .position(new LatLng(38.038024, -84.504686), 8600f, 6500f);
        mMap.addGroundOverlay(campusMap);

        // Change to Satellite to get rid of labels
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);*/

        // Added for locator button
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
    }

    @Override
    public void deactivate() {
        mListener = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (mListener != null) {
            mListener.onLocationChanged(location);
            mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        Toast.makeText(this, "provider disabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        Toast.makeText(this, "provider enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        Toast.makeText(this, "status changed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
