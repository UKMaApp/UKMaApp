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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.MenuInflater;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.widget.SearchView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements LocationListener, LocationSource, SearchView.OnQueryTextListener {
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private LocationSource.OnLocationChangedListener mListener;
    private LocationManager locationManager;
    private HashMap<String, Building> mBuildingHash = new HashMap<String, Building>();
    private ArrayList<Marker> mMarkerArray = new ArrayList<Marker>();
    private static final int HUGE_STR_DIST = 1000;
    private static final double TOP_LAT_BOUND    =  38.050;
    private static final double BOTTOM_LAT_BOUND =  38.000;
    private static final double LEFT_LNG_BOUND   = -84.515;
    private static final double RIGHT_LNG_BOUND  = -84.495;
    private static final double DEFAULT_LAT = 38.038024;
    private static final double DEFAULT_LNG = -84.504686;
    private static final int DEFAULT_ZOOM = 18;
    private static final float DEFAULT_TILT = 30;
    private static final float INVISIBLE = 0.0f;
    private static final float VISIBLE = 1.0f;
    private boolean resetTilt = false;
    private EditText ET;
    private SearchView searchView;
    private ResultsView resultsView;
    //private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> locationNames;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        setUpMapIfNeeded();
        setUpMarkers();

        //Set up listview to show results
        resultsView = new ResultsView();

        //ET = (EditText) findViewById(R.id.et_location);
        //searchView = (SearchView) (item) findViewById(R.id.searchBar).getAc;
        //searchView.setOnQueryTextListener(this);
        /*
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                search(query.toLowerCase());
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                //search(newText.toLowerCase());
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        */
        /*
        ET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //search();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        */
    }
/*
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        setHasOptionsMenu(true);
    }
  */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        //Display the options menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu,menu);

        //Get the SearchView object from the searchBar menu item
        searchView = (SearchView) menu.findItem(R.id.searchBar).getActionView();

        //Set what happens when the searchBar item is pressed
        menu.findItem(R.id.searchBar).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                //Toggle visibility of listview
                resultsView.toggle();
                return false;
            }
        });

        //Activity implements an OnQueryTextListener so we set it to this
        //  Methods for OnQueryTextListener are
        //      onQueryTextSubmit
        //      onQueryTextChange
        searchView.setOnQueryTextListener(this);

        //Call parent method and return
        return super.onCreateOptionsMenu(menu);
    }

    //Set what happens when a search is submitted
    @Override
    public boolean onQueryTextSubmit(String query) {
        //Update results with query
        resultsView.update(query);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        //search(newText.toLowerCase());
        //View shadow = findViewById(R.id.listViewShadowLeft);
        //shadow.setVisibility(View.VISIBLE);

        //If results aren't shown, show them
        if(!resultsView.showing)
            resultsView.toggle();

        //Update results with query
        resultsView.update(newText);

        return false;
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

    // This function listens to the LatLng position the camera is centered on. If this goes out
    // of bounds in any direction(s) it snaps back to the bound so you can't scroll too far
    // away from UK's campus. Essentially this is confining your view to a box around UK's campus
    // This is called every time the camera moves (or zooms or changes in any way).
    public GoogleMap.OnCameraChangeListener getCameraChangeListener() {
        return new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                // This is the latitude and longitude the camera is centered on.
                double updatedLat = position.target.latitude;
                double updatedLng = position.target.longitude;
                double originalLat = updatedLat;
                double originalLng = updatedLng;

                // Check to make sure you are within the rectangular bounds
                if (position.target.latitude > TOP_LAT_BOUND) {
                    updatedLat = TOP_LAT_BOUND; }
                if (position.target.latitude < BOTTOM_LAT_BOUND) {
                    updatedLat = BOTTOM_LAT_BOUND; }
                if (position.target.longitude > RIGHT_LNG_BOUND) {
                    updatedLng = RIGHT_LNG_BOUND; }
                if (position.target.longitude < LEFT_LNG_BOUND) {
                    updatedLng = LEFT_LNG_BOUND; }

                //only animate camera if update needed, reduces camera lag for unnecessary updates
                if (originalLat != updatedLat || originalLng != updatedLng) {
                    // Set camera zoom and tilt
                    CameraPosition updatedPosition = new CameraPosition.Builder()
                            .target(new LatLng(updatedLat, updatedLng))
                            .zoom(position.zoom)
                            .tilt(position.tilt)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(updatedPosition));
                }
                if (resetTilt) { // this is only called when a filter word is searched
                    // Set camera zoom and tilt
                    CameraPosition updatedPosition = new CameraPosition.Builder()
                            .target(new LatLng(position.target.latitude, position.target.longitude))
                            .zoom(position.zoom)
                            .tilt(DEFAULT_TILT)
                            .build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(updatedPosition));
                    resetTilt = false;
                }
            }
        };
    }

    // Search button listener. When the search button is clicked this is called. If there is
    // text in the search box it will be searched for and the found building will be centered on.
    public void search(String location) {
        // Close keyboard
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);


        // Getting user input location
        Marker closestMarker = null;

        if (location.equals("help")) {
            Toast.makeText(this, "Special searches: help, all, food, buildings, parking, and reset", Toast.LENGTH_LONG).show();
        } else if (location.equals("all")) {
            // if "all" is searched the transparency of all markers is toggled
            float toggle = VISIBLE;
            if (mMarkerArray.get(0).getAlpha() != INVISIBLE) {
                toggle = INVISIBLE;
            }
            for (Marker m : mMarkerArray) {
                m.setAlpha(toggle); // toggle transparency of all markers
            }
        } else if (location.equals("reset")) {
            // if "reset" is searched center map to default
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(DEFAULT_LAT, DEFAULT_LNG))
                    .zoom(DEFAULT_ZOOM)
                    .tilt(DEFAULT_TILT)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else if (location.equals("buildings") || location.equals("food") || location.equals("parking")) {
            // This will make all buildings that match the filter visible and it will zoom to
            // a level that has every building on screen. For this we need to calculate the
            // southwest and northeast corners.
            // ((SW_lat, SW_lng), (NE_lat, NE_lng)) == ((minLat, minLng), (maxLat, maxLng))
            double maxLat = BOTTOM_LAT_BOUND;
            double maxLng = RIGHT_LNG_BOUND;
            double minLat = TOP_LAT_BOUND;
            double minLng = LEFT_LNG_BOUND;

            for (Marker m : mMarkerArray) {
                Building bldg = mBuildingHash.get(m.getTitle());
                if (location.equals("parking")) {
                    if (!bldg.type.equals("buildings") && !bldg.type.equals("food")) {
                        m.setAlpha(VISIBLE);
                        if (bldg.lat > maxLat) {
                            maxLat = bldg.lat; }
                        if (bldg.lat < minLat) {
                            minLat = bldg.lat; }
                        if (bldg.lng > maxLng) {
                            minLng = bldg.lng; }
                        if (bldg.lng < minLng) {
                            minLng = bldg.lng; }
                    } else {
                        m.setAlpha(INVISIBLE);
                    }
                } else {
                    if (bldg.type.equals(location)) {
                        m.setAlpha(VISIBLE);
                        if (bldg.lat > maxLat) {
                            maxLat = bldg.lat; }
                        if (bldg.lat < minLat) {
                            minLat = bldg.lat; }
                        if (bldg.lng > maxLng) {
                            minLng = bldg.lng; }
                        if (bldg.lng < minLng) {
                            minLng = bldg.lng; }
                    } else {
                        m.setAlpha(INVISIBLE);
                    }
                }
            }
            LatLngBounds bounds = new LatLngBounds(new LatLng(minLat, minLng), new LatLng(maxLat, maxLng));
            resetTilt = true;
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10));
        } else if (!location.equals("")) {
            // Initial closest is 1000 since that is huge and all other "distances" will be closer.
            int closestStrDistance = HUGE_STR_DIST;

            for (Marker m : mMarkerArray) {
                m.setAlpha(INVISIBLE); // make all markers transparent
            }

            // NOTE: This whole area may seem confusing. Sorry, it's effective for searching.

            // Loop through all markers. For each marker several comparisons are done to find
            // the closest string match.
            // 1. An exact match check is done and if it is found the rest of the markers
            //    are skipped over.
            // 2. Then a check against the building code (e.g. RGAN)
            // 3. Then a check against the entire building name (e.g. Ralph G. Anderson Building)
            // 4. Then a check against each word in the building name. This is done so a search
            //    for "physics" when compared to the words {"chemistry", "physics", "building"}
            //    will match correctly
            // 5. Then a check against just a substring of the entire building name equal to
            //    the length of the searched text (e.g. compare "ralph" to "ralph" of full name
            //    "ralph g. anderson")
            for (Marker m : mMarkerArray) {
                String markerName = mBuildingHash.get(m.getTitle()).name; // retrieve current marker name
                if(markerName.toLowerCase().contains(location.toLowerCase()))
                    closestMarker = m;
                /*
                // If exact name match we can stop searching the rest of the markers
                if (location.equals(markerName.toLowerCase())) {
                    closestMarker = m;
                    break;
                }

                // Check building code like "RGAN"
                int thisStrDistance = levenshteinDistance(location, mBuildingHash.get(markerName).code);
                if (thisStrDistance < closestStrDistance) { // if this is a closer match than current closest then...
                    closestStrDistance = thisStrDistance; // it is now the closest match
                    closestMarker = m;
                }

                // Check entire name like "Ralph G. Anderson Building"
                thisStrDistance = levenshteinDistance(location, mBuildingHash.get(markerName).name);
                if (thisStrDistance < closestStrDistance) { // if this is a closer match than current closest
                    closestStrDistance = thisStrDistance; // it is now the closest match
                    closestMarker = m;
                }

                // Split each building name by spaces
                // Compare "physics" to {"chemistry", "physics", "building"}
                String[] bldgSplitArray = mBuildingHash.get(markerName).name.split(" |-");
                for (String bldgWord : bldgSplitArray) { // check each word
                    thisStrDistance = levenshteinDistance(location, bldgWord);
                    if (thisStrDistance < closestStrDistance) {
                        closestStrDistance = thisStrDistance;
                        closestMarker = m;
                    }
                }

                // Substring because you want to compare "Chem" to "Chem" instead of
                //   "Chem" to "Chemistry" or "Chemistry Physics Building"
                String subMarkerName = markerName.substring(0, Math.min(markerName.length(), location.length()));
                thisStrDistance = levenshteinDistance(location, subMarkerName);
                if (thisStrDistance < closestStrDistance) {
                    closestStrDistance = thisStrDistance;
                    closestMarker = m;
                }
                */
            }
        }
        if (closestMarker != null) {
            // center on the closest matching marker and show its info window
            mMap.animateCamera(CameraUpdateFactory.newLatLng(closestMarker.getPosition()));
            closestMarker.showInfoWindow();
        }
    }
/*
    // Search button listener. When the search button is clicked this is called. If there is
    // text in the search box it will be searched for and the found building will be centered on.
    public void searchListener(View view) {
        // Close keyboard
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow((null == getCurrentFocus()) ? null : getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        // Getting reference to EditText to get the user input location
        EditText etLocation = (EditText) findViewById(R.id.et_location);

        // Getting user input location
        String location = etLocation.getText().toString().toLowerCase();
        Marker closestMarker = null;

        if (location.equals("help")) {
            Toast.makeText(this, "Special searches: help, all, food, buildings, parking, and reset", Toast.LENGTH_LONG).show();
        } else if (location.equals("all")) {
            // if "all" is searched the transparency of all markers is toggled
            float toggle = VISIBLE;
            if (mMarkerArray.get(0).getAlpha() != INVISIBLE) {
                toggle = INVISIBLE;
            }
            for (Marker m : mMarkerArray) {
                m.setAlpha(toggle); // toggle transparency of all markers
            }
        } else if (location.equals("reset")) {
            // if "reset" is searched center map to default
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(DEFAULT_LAT, DEFAULT_LNG))
                    .zoom(DEFAULT_ZOOM)
                    .tilt(DEFAULT_TILT)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else if (location.equals("buildings") || location.equals("food") || location.equals("parking")) {
            // This will make all buildings that match the filter visible and it will zoom to
            // a level that has every building on screen. For this we need to calculate the
            // southwest and northeast corners.
            // ((SW_lat, SW_lng), (NE_lat, NE_lng)) == ((minLat, minLng), (maxLat, maxLng))
            double maxLat = BOTTOM_LAT_BOUND;
            double maxLng = RIGHT_LNG_BOUND;
            double minLat = TOP_LAT_BOUND;
            double minLng = LEFT_LNG_BOUND;

            for (Marker m : mMarkerArray) {
                Building bldg = mBuildingHash.get(m.getTitle());
                if (location.equals("parking")) {
                    if (!bldg.type.equals("buildings") && !bldg.type.equals("food")) {
                        m.setAlpha(VISIBLE);
                        if (bldg.lat > maxLat) {
                            maxLat = bldg.lat; }
                        if (bldg.lat < minLat) {
                            minLat = bldg.lat; }
                        if (bldg.lng > maxLng) {
                            minLng = bldg.lng; }
                        if (bldg.lng < minLng) {
                            minLng = bldg.lng; }
                    } else {
                        m.setAlpha(INVISIBLE);
                    }
                } else {
                    if (bldg.type.equals(location)) {
                        m.setAlpha(VISIBLE);
                        if (bldg.lat > maxLat) {
                            maxLat = bldg.lat; }
                        if (bldg.lat < minLat) {
                            minLat = bldg.lat; }
                        if (bldg.lng > maxLng) {
                            minLng = bldg.lng; }
                        if (bldg.lng < minLng) {
                            minLng = bldg.lng; }
                    } else {
                        m.setAlpha(INVISIBLE);
                    }
                }
            }
            LatLngBounds bounds = new LatLngBounds(new LatLng(minLat, minLng), new LatLng(maxLat, maxLng));
            resetTilt = true;
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10));
        } else if (!location.equals("")) {
            // Initial closest is 1000 since that is huge and all other "distances" will be closer.
            int closestStrDistance = HUGE_STR_DIST;

            for (Marker m : mMarkerArray) {
                m.setAlpha(INVISIBLE); // make all markers transparent
            }

            // NOTE: This whole area may seem confusing. Sorry, it's effective for searching.

            // Loop through all markers. For each marker several comparisons are done to find
            // the closest string match.
            // 1. An exact match check is done and if it is found the rest of the markers
            //    are skipped over.
            // 2. Then a check against the building code (e.g. RGAN)
            // 3. Then a check against the entire building name (e.g. Ralph G. Anderson Building)
            // 4. Then a check against each word in the building name. This is done so a search
            //    for "physics" when compared to the words {"chemistry", "physics", "building"}
            //    will match correctly
            // 5. Then a check against just a substring of the entire building name equal to
            //    the length of the searched text (e.g. compare "ralph" to "ralph" of full name
            //    "ralph g. anderson")
            for (Marker m : mMarkerArray) {
                String markerName = mBuildingHash.get(m.getTitle()).name; // retrieve current marker name
                if(markerName.toLowerCase().contains(location.toLowerCase()))
                    closestMarker = m;
                /*
                // If exact name match we can stop searching the rest of the markers
                if (location.equals(markerName.toLowerCase())) {
                    closestMarker = m;
                    break;
                }

                // Check building code like "RGAN"
                int thisStrDistance = levenshteinDistance(location, mBuildingHash.get(markerName).code);
                if (thisStrDistance < closestStrDistance) { // if this is a closer match than current closest then...
                    closestStrDistance = thisStrDistance; // it is now the closest match
                    closestMarker = m;
                }

                // Check entire name like "Ralph G. Anderson Building"
                thisStrDistance = levenshteinDistance(location, mBuildingHash.get(markerName).name);
                if (thisStrDistance < closestStrDistance) { // if this is a closer match than current closest
                    closestStrDistance = thisStrDistance; // it is now the closest match
                    closestMarker = m;
                }

                // Split each building name by spaces
                // Compare "physics" to {"chemistry", "physics", "building"}
                String[] bldgSplitArray = mBuildingHash.get(markerName).name.split(" |-");
                for (String bldgWord : bldgSplitArray) { // check each word
                    thisStrDistance = levenshteinDistance(location, bldgWord);
                    if (thisStrDistance < closestStrDistance) {
                        closestStrDistance = thisStrDistance;
                        closestMarker = m;
                    }
                }

                // Substring because you want to compare "Chem" to "Chem" instead of
                //   "Chem" to "Chemistry" or "Chemistry Physics Building"
                String subMarkerName = markerName.substring(0, Math.min(markerName.length(), location.length()));
                thisStrDistance = levenshteinDistance(location, subMarkerName);
                if (thisStrDistance < closestStrDistance) {
                    closestStrDistance = thisStrDistance;
                    closestMarker = m;
                }

            }
        }
        if (closestMarker != null) {
            // center on the closest matching marker and show its info window
            mMap.animateCamera(CameraUpdateFactory.newLatLng(closestMarker.getPosition()));
            closestMarker.showInfoWindow();
        }
    }
*/
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

    // A "Building" holds all of the information a building has. This information is linked to a
    // marker which is placed at the building's location.
    //  Need to update this for phone numbers and new hours format
    public class Building {
        public String name = "error"; // White Hall Classroom Building
        public String code = "";      // CB
        public double lat = 0.0;      // 38.7
        public double lng = 0.0;      // -87.5
        public String url = "";       // www.whitehall.com
        public String type = "";      // buildings
        public String hours = "";     // ""
        public String phone = "8592579000"; //Hardcoded for now, will be changed when csv is updated

        /* This constructor will fill the fields based on the incoming array "tokens" which
           represents a row from the table where each index is a column.

           For example:
           tokens[x] = {            name, code,  lat,   lng,   url,      type,                 hours}
           tokens[y] = {      White Hall,   CB, 38.7, -84.5, x.com, buildings,                      }
           tokens[z] = {Parking Garage 6,  PG6, 39.0, -84.9, p.com,         E, 5:30 a.m. - 3:30 p.m.}

         * NOTE: Buildings need to have at least 4 tokens
         * NOTE: It will only hash the object if lat and lng are given and numeric
         */
        Building(String[] tokens) {
            // Every "if" statement is in place to prevent seg faults. Handle with care.
            if (tokens.length > 3) {
                if (isNumeric(tokens[2]) && isNumeric(tokens[3])) {
                    name = tokens[0];
                    code = tokens[1];
                    lat = Double.parseDouble(tokens[2]);
                    lng = Double.parseDouble(tokens[3]);
                    if (tokens.length > 4) {
                        url = tokens[4]; }
                    if (tokens.length > 5) {
                        type = tokens[5]; }
                    if (tokens.length > 6) {
                        hours = tokens[6]; }
                }
            }

            /* name is declared to be "error" so if anything goes wrong in the construction of
               the building object and name is never set to a different string the building is
               assumed to be broken and will not be hashed.

             * Buildings are hashed by their names so search via: buildingHash.get("White Hall")
             */
            if (!name.equals("error")) {
                mBuildingHash.put(name, this);
            }
        }
    }

    //Handles searching and displaying results
    public class ResultsView
    {
        public ListView listView; //listView to populate/change
        public boolean showing; //true if results are being shown
        public int maxHeight; //Max height of listView in pixels

        ResultsView()
        {
            //Get the listview
            listView = (ListView) findViewById(R.id.listView);
            //Copy all layout parameters
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) listView.getLayoutParams();
            //Starting height is max height + 5 so retrieve that from layout parameters
            maxHeight = params.height-5;
            //Update listview with all locations
            update("");
            //it is not showing initially
            showing = false;
            //Set what happens when a location is clicked in the list view
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    //Get the name of the location that was clicked
                    String item = (String) listView.getItemAtPosition(position);
                    //Iterate through all markers looking for matching name
                    //Need to look into storing more info in each item such as index
                    //  so we won't need to iterate through all markers
                    for(Marker m: mMarkerArray)
                    {
                        if(item.equals(mBuildingHash.get(m.getTitle()).name))
                        {
                            //Collapse the search bar
                            searchView.onActionViewCollapsed();
                            //Hide the listview
                            toggle();
                            //Move map to selected location
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(m.getPosition()));
                            //Show window with location information
                            m.showInfoWindow();
                            break;
                        }
                    }

                    System.out.println(item);
                }
            });
        }

        //Updates the listview with locations matching the query
        //Need to update this to accept list of filters once theyre implemented
        public void update(String query)
        {
            //Holds all matching results
            locationNames = new ArrayList<String>();
            //Iterate through locations
            for (Marker m : mMarkerArray) {
                String markerName = mBuildingHash.get(m.getTitle()).name; // retrieve current marker name
                String markerCode = mBuildingHash.get(m.getTitle()).code; // retrieve current marker code
                //If query is a substring of location name or location code
                //  add location name to results
                if(markerName.toLowerCase().contains(query.toLowerCase()) || markerCode.toLowerCase().contains(query.toLowerCase()) )
                    locationNames.add(markerName);
            }
            //Update height of the listview to expand/contract
            updateHeight(locationNames.size());
            System.out.println(locationNames.size());
            //Create an adapter to fill the listview
            adapter = new ArrayAdapter<String>(getApplicationContext(),
                    R.layout.listitem, R.id.name, locationNames);
            //Assign the adapter to the listview
            listView.setAdapter(adapter);
        }

        //Changes height of listview to accomodate number of results
        public void updateHeight(int entries)
        {
            //ScaleAnimation anim = new ScaleAnimation(1,1,1,0);

            //Calculate new height
            int newHeight = entries*maxHeight/5;
            //If new height is greater than the max, set new height equal to max
            if(newHeight > maxHeight)
                newHeight = maxHeight;
            //RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.listViewLayout).getLayoutParams();
            //params.height = newHeight+5;
            //findViewById(R.id.listViewLayout).setLayoutParams(params);

            //if(newHeight == 0)
            //    findViewById(R.id.listViewShadowBottom).startAnimation(anim);
            //params = (RelativeLayout.LayoutParams) (findViewById(R.id.listViewShadowLayout)).getLayoutParams();
            //params.height = newHeight + shadowHeight;
            //findViewById(R.id.listViewShadowLayout).setLayoutParams(params);

            //Get the layout parameters of the listview
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) listView.getLayoutParams();
            //Set the height
            params.height = newHeight;
            //Adding 5 pixels for drop shadow unless there aren't any results
            if(newHeight != 0)
                params.height += 5;
            //Update layout parameters
            listView.setLayoutParams(params);

            /*
            params = (LinearLayout.LayoutParams) findViewById(R.id.listViewShadowLeft).getLayoutParams();
            params.height = newHeight;
            findViewById(R.id.listViewShadowLeft).setLayoutParams(params);
            params = (LinearLayout.LayoutParams) findViewById(R.id.listViewShadowRight).getLayoutParams();
            params.height = newHeight;

            findViewById(R.id.listViewShadowRight).setLayoutParams(params);
            */

        }

        //Toggles visibility of the list view
        public void toggle()
        {
            if(showing)
            {
                showing = false;
                listView.setVisibility(View.GONE);
            }
            else
            {
                showing = true;
                listView.setVisibility(View.VISIBLE);
            }

        }

    }

    // This parses the Buildings.csv file and populates the map with markers for buildings.
    private void setUpMarkers() {
        Toast.makeText(this, "Special searches: help, all, food, buildings, parking, and reset", Toast.LENGTH_LONG).show();
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
                            .alpha(INVISIBLE)); // 0.0 (invisible) - 1.0 (fully visible)

                    if ((bldg.type.equals("buildings") || bldg.type.equals("food")) && !bldg.code.equals("")) {
                        mMarker.setSnippet(bldg.code);
                    } else if (!bldg.hours.equals("")) {
                        mMarker.setSnippet(bldg.hours + " (" + bldg.type + " lot)");
                    }
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
                Building tempBldg = mBuildingHash.get(marker.getTitle());
                // Set intent strings for location
                final String url = tempBldg.url;
                final String phone = "tel:" + tempBldg.phone;
                final String navigate = "google.navigation:q="+tempBldg.lat+","+tempBldg.lng;

                //Create popup menu
                // Need to work on positioning
                PopupMenu popupMenu = new PopupMenu(MapsActivity.this, findViewById(R.id.popupAnchor));
                popupMenu.getMenuInflater().inflate(R.menu.popup_menu,popupMenu.getMenu());

                //Set what happens when an selection is made
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent newIntent;
                        //If user selected call, website, or navigate
                        //  set specific intent
                        if(item.getItemId() == R.id.call)
                        {
                            newIntent = new Intent(Intent.ACTION_DIAL, Uri.parse(phone));
                        }
                        else if(item.getItemId() == R.id.website)
                        {
                            newIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        }
                        else
                        {
                            newIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(navigate));
                        }

                        //Start the intent in a new activity
                        startActivity(newIntent);
                        return false;
                    }
                });

                //Show the popup menu
                popupMenu.show();

                /*
                if (url != null) {
                    // visit the url using the phone's browser
                    Intent openUrl = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(openUrl);
                }
                */
            }
        });
    }

    // Takes a string and return true if it is a double, else false
    public static boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    // This is where we can add markers or lines, add listeners or move the camera.
    // This should only be called once and when we are sure that {@link #mMap} is not null.
    private void setUpMap() {
        // For ping location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            boolean gpsIsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean networkIsEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (gpsIsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10F, this);
            } else if (networkIsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 10F, this);
            } else {
                //Show some kind of message that tells the user that the GPS is disabled.
                Toast.makeText(this, "GPS is disabled.", Toast.LENGTH_SHORT).show();
            }
        } else {
            //Show some generic error dialog because something must have gone wrong with location manager.
            Log.e("setUpMap()", "Location Manager error");
        }

        // Set camera zoom and tilt to default position in center of campus
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(DEFAULT_LAT, DEFAULT_LNG))
                .zoom(DEFAULT_ZOOM)
                .tilt(DEFAULT_TILT)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        // Added for locator button
        mMap.setMyLocationEnabled(true);
        mMap.setOnCameraChangeListener(getCameraChangeListener());
    }

    /* Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
   installed) and the map has not already been instantiated.. This will ensure that we only ever
   call {@link #setUpMap()} once when {@link #mMap} is not null.

 * If it isn't installed {@link SupportMapFragment} (and
   {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
   install/update the Google Play services APK on their device.

 * A user can return to this FragmentActivity after following the prompt and correctly
   installing/updating/enabling the Google Play services. Since the FragmentActivity may not
   have been completely destroyed during this process (it is likely that it would only be
   stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
   method in {@link #onResume()} to guarantee that it will be called.
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
            //mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
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
        //Toast.makeText(this, "status changed", Toast.LENGTH_SHORT).show();
    }
}
