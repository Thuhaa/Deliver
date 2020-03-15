package com.thuha.delivrer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;

public class ClientMapActivity  extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient ngoogleApiClient;
    Location nLastLocation;
    LocationRequest nLocationRequest;
    private Button nLogout, nRequestRider;
    private LatLng pickupLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        nRequestRider = findViewById(R.id.requestrider);
        nLogout = findViewById(R.id.logout);
        nLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(ClientMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        nRequestRider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("RequestRider");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userId, new GeoLocation(nLastLocation.getLatitude(), nLastLocation.getLongitude()));
                pickupLocation = new LatLng(nLastLocation.getLatitude(), nLastLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup Here"));
                nRequestRider.setText("Getting Rider...");
                getClosestRider();
            }
        });
    }

    private void getClosestRider(){
        final int[] radius = {1};
        final Boolean[] riderFound = {false};
        final String[] riderFoundID = new String[1];
        DatabaseReference riderLocation = FirebaseDatabase.getInstance().getReference().child("AvailableRiders");
        GeoFire geoFire = new GeoFire(riderLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(pickupLocation.latitude, pickupLocation.longitude), radius[0]);
        geoQuery.removeAllListeners();
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!riderFound[0]){
                    riderFound[0] = true;
                    riderFoundID[0] = key;
                }
                DatabaseReference riderRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(String.valueOf(riderFoundID));
                String clientID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                HashMap map = new HashMap();
                map.put("clientRideId", clientID);
                riderRef.updateChildren(map);
                getRiderLocation();
                nRequestRider.setText("Getting Rider Location....");

            }
            private Marker nRiderMarker;
            private void getRiderLocation() {
                DatabaseReference riderLocationRef = FirebaseDatabase.getInstance().getReference().child("WorkingRiders").child(String.valueOf(riderFoundID)).child("1");
                riderLocationRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            List<Object> map = (List<Object>) dataSnapshot.getValue();
                            nRequestRider.setText("Rider Found");
                            double locationLat = 0;
                            if (map.get(0) != null) {
                                locationLat = 0;
                                locationLat = Double.parseDouble(map.get(0).toString());
                            }
                            double locationLong = 0;
                            if (map.get(1) != null) {
                                locationLong = 0;
                                locationLong = Double.parseDouble(map.get(1).toString());
                            }
                            LatLng riderLatLong = new LatLng(locationLat, locationLong);
                            if (nRiderMarker!= null){
                                nRiderMarker.remove();
                            }
                            nRiderMarker = mMap.addMarker(new MarkerOptions().position(riderLatLong).title("Your Driver"));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!riderFound[0]){
                    radius[0]++;
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }





    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);
    }

    protected synchronized void buildGoogleApiClient() {
        ngoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        ngoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {
        nLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(13));


    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        nLocationRequest = new LocationRequest();
        nLocationRequest.setInterval(1000);
        nLocationRequest.setFastestInterval(1000);
        nLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationServices.FusedLocationApi.requestLocationUpdates(ngoogleApiClient, nLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();
    }
}
