package com.example.sos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.sos.models.PlaceInfo;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePredictionBufferResponse;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady:map is ready");
        mMap = googleMap;
        if (mLocationPermissionsGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            init();


        }

    }

    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION= Manifest.permission.ACCESS_COARSE_LOCATION;

    //widgets
    private AutoCompleteTextView mSearchText;
    private ImageView mGps,mInfo,mPlacePicker,mHospital;
    // The entry points to the Places API.

    //var
    private boolean mLocationPermissionsGranted= false;
    private GoogleMap mMap;
    public double latitude;
    public double longitude;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE= 1234;
    private static final float DEFAULT_ZOOM= 15f;
    private static final int PLACE_PICKER_REQUEST = 1;

    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40,-168),new LatLng(71,136));
    private PlaceInfo mPlace;
    private Marker mMarker;
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;
    private int PROXIMITY_RADIUS = 10000;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getLocationPermission();

        mSearchText= (AutoCompleteTextView) findViewById(R.id.input_search);
        mGps = (ImageView) findViewById(R.id.ic_gps);
        mInfo = (ImageView) findViewById(R.id.place_info);
        mPlacePicker=(ImageView) findViewById(R.id.place_picker);
        mHospital=(ImageView) findViewById(R.id.hospital);


    }

    private void init(){
        Log.d(TAG,"init: initializing");

      mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
      mSearchText.setOnItemClickListener(mAutocompleteClickListener);
        // Construct a GeoDataClient.
        mGeoDataClient = Places.getGeoDataClient(this, null);

       mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGeoDataClient , LAT_LNG_BOUNDS,null);


        mSearchText.setAdapter(mPlaceAutocompleteAdapter);


        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent) {
                if(actionID== EditorInfo.IME_ACTION_SEARCH || actionID==EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER){
                    // execute our method for searching
                    geoLocate();

                }
                return false;
            }
        });
        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG,"OnClick: clicked gps icone");
                getDeviceLocation();
            }
        });

        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"onClick: place info clicked");
                try{
                    if(mMarker.isInfoWindowShown()){
                        mMarker.hideInfoWindow();

                    }else {
                        Log.d(TAG,"onClick: place info: "+mPlace.toString() );
                        mMarker.showInfoWindow();
                    }

                }catch (NullPointerException e){
                    Log.d(TAG,"onClick: NullPointerException: " + e.getMessage());


                }
            }
        });
        mPlacePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

                try {
                    startActivityForResult(builder.build(MapActivity.this), PLACE_PICKER_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    Log.e(TAG,"OnClick : GooglePlayServicesRepairableException:"+ e.getMessage());
                } catch (GooglePlayServicesNotAvailableException e) {
                    Log.e(TAG,"OnClick : GooglePlayServicesNotAvailableException:"+ e.getMessage());

                }
            }
        });
        mHospital.setOnClickListener(new View.OnClickListener() {
            String Hospital = "hospital";
            @Override
            public void onClick(View v) {
                mMap.clear();
                if (mMarker != null) {
                    mMarker.remove();
                }
                String url = getUrl(latitude , longitude, Hospital);
                Log.d(TAG,"url : "+ url);
                Object[] DataTransfer = new Object[2];
                DataTransfer[0] = mMap;
                DataTransfer[1] = url;
                Log.d("onClick", url);
                GetNearbyPlacesData getNearbyPlacesData = new GetNearbyPlacesData();
                getNearbyPlacesData.execute(DataTransfer);
                Log.d(TAG,"latitude longitude"+ latitude);

                Toast.makeText(MapActivity.this,"Nearby Hospitals", Toast.LENGTH_LONG).show();
                Log.d(TAG,"nearby place:"+ getNearbyPlacesData.url);
            }
        });



        hideSoftKeyboard();

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient,place.getId());
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            }
        }
    }

   private void geoLocate(){
        Log.d(TAG,"geoLocate: geolocating");
        String searchString = mSearchText.getText().toString();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try{
            list= geocoder.getFromLocationName(searchString,1);

        }catch (IOException e){
            Log.d(TAG,"geoLocate: IOException: "+ e.getMessage());
        }
        if(list.size()>0){
            Address address = list.get(0);
            Log.d(TAG,"geoLocate: Found a location "+ address.toString());
            //Toast.makeText(this,address.toString(),Toast.LENGTH_SHORT).show();
            moveCamera(new LatLng(address.getLatitude(),address.getLongitude()),DEFAULT_ZOOM,address.getAddressLine(0));

        }
    }


    private void getDeviceLocation(){
        Log.d(TAG,"getDeviceLocation: getting the current devices location");
        mFusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);
        try{
            if(mLocationPermissionsGranted){
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG,"onComplete: Found location!");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),DEFAULT_ZOOM,"My Location");

                        }
                        else{
                            Log.d(TAG,"onComplete: current location is null");
                            Toast.makeText(MapActivity.this,"unable to get current location",Toast.LENGTH_SHORT).show();
                        }


                    }
                });
            }

        }catch (SecurityException e){
            Log.e(TAG,"getDeviceLocation: SecurityException" + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng , float zoom , PlaceInfo placeInfo){
        Log.d(TAG,"moveCamera: moving the camera to: lat " + latLng.latitude + ", lng: "+ latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
        mMap.clear();
        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(MapActivity.this));
        if(placeInfo!=null){
            try{
                String snippet = "Address :" + placeInfo.getAddress() + "\n" +
                        "Phone Number :" + placeInfo.getPhoneNumber() + "\n" +
                        "Website :" + placeInfo.getWebsiteUri() + "\n" +
                        "Rating :" + placeInfo.getRating() + "\n" ;
                MarkerOptions options = new MarkerOptions().position(latLng).title(placeInfo.getName()).snippet(snippet);
                mMarker=mMap.addMarker(options);


            }catch(NullPointerException e){
                Log.e(TAG,"moveCamera:NullPointerException: "+ e.getMessage());

            }

        }else {
            mMap.addMarker(new MarkerOptions().position(latLng));

        }
        hideSoftKeyboard();

    }


    private void moveCamera(LatLng latLng , float zoom , String title){
        Log.d(TAG,"moveCamera: moving the camera to: lat " + latLng.latitude + ", lng: "+ latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
        if(!title.equals("My Location")){
            MarkerOptions options = new MarkerOptions().position(latLng).title(title);
            mMap.addMarker(options);
        }
        hideSoftKeyboard();

    }

    private void initMap(){
        Log.d(TAG,"initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);

    }



    private void getLocationPermission(){
        Log.d(TAG,"getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted= true;
                initMap();
            }
            else{
                ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,permissions,LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG,"onRequestPermissionsResult:called. ");
        mLocationPermissionsGranted=false;
        switch (requestCode){
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if(grantResults.length>0){
                    for(int i=0;i<grantResults.length;i++){
                        if(grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                            mLocationPermissionsGranted=false;
                            Log.d(TAG,"OnRequestPermissionsResult: permission failed");
                            return;

                        }
                    }
                    Log.d(TAG,"OnRequestPermissionsResult: permission Granted");
                    mLocationPermissionsGranted=true;
                    //initialize map
                    initMap();

                }
            }
        }
    }
    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    /*--------------------------google places api autocomplete suggestions------------------------

     */
    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
            hideSoftKeyboard();
            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId=item.getPlaceId();
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient,placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if(!places.getStatus().isSuccess()){
                Log.d(TAG,"OnResult: Place query did not complete successfuly"+ places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);
            try {

            mPlace= new PlaceInfo();
            mPlace.setName(place.getName().toString());
            mPlace.setAddress(place.getAddress().toString());
            //mPlace.setAttributions(place.getAttributions().toString());
            mPlace.setId(place.getId());
            mPlace.setLatlng(place.getLatLng());
            mPlace.setRating(place.getRating());
            mPlace.setPhoneNumber(place.getPhoneNumber().toString());
            mPlace.setWebsiteUri(place.getWebsiteUri());
            Log.d(TAG,"OnResult:Place "+ mPlace.toString());

            }catch (NullPointerException e){
                Log.e(TAG,"NullPointerException:" + e.getMessage());
            }
            moveCamera(new LatLng(place.getViewport().getCenter().latitude,place.getViewport().getCenter().longitude),DEFAULT_ZOOM,mPlace);

            places.release();



        }
    };

    private String getUrl(double latitude, double longitude, String nearbyPlace) {

        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + latitude + "," + longitude);
        Log.d(TAG,"latitude long  ok"+latitude+longitude);
        googlePlacesUrl.append("&radius=" + PROXIMITY_RADIUS);
        Log.d(TAG,"proximity ok  ok");
        googlePlacesUrl.append("&type=" + nearbyPlace);
        Log.d(TAG,"type  ok"+nearbyPlace );
        googlePlacesUrl.append("&sensor=true");
        Log.d(TAG,"sensor  ok");
        googlePlacesUrl.append("&key=" + "AIzaSyDfcN6Mz4RNFXOhROQIvu113Y2gZtiqwTw");
        Log.d("getUrl", googlePlacesUrl.toString());
        return (googlePlacesUrl.toString());
    }




}
