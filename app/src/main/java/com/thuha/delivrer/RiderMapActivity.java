package com.thuha.delivrer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.LocationCallback;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;

public class RiderMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
   private GoogleApiClient ngoogleApiClient;
    Location nLastLocation;
    LocationRequest nLocationRequest;
    private Button nLogout;
    private  String clientId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        nLogout = findViewById(R.id.logout);
        nLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(RiderMapActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
        getAssignedClient();
    }

    private void getAssignedClient(){
        String driverId = FirebaseAuth.getInstance().getUid();
        DatabaseReference assignedClientRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Clients").child(driverId);
        assignedClientRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("clientRideId")!=null){
                        clientId = map.get("clientRideId").toString();
                        getAssignedClientPickupLocation();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }
    private void getAssignedClientPickupLocation(){
        DatabaseReference assignedClientPickupLocationRef = FirebaseDatabase.getInstance().getReference().child("RequestRider").child(clientId).child("1");
        assignedClientPickupLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    List<Object> map =  (List<Object>) dataSnapshot.getValue();
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
                    mMap.addMarker(new MarkerOptions().position(riderLatLong).title("Pickup Location"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

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

        final String userId = FirebaseAuth.getInstance().getUid();
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("AvailableRiders");
        final GeoFire geoFire = new GeoFire(ref);
        geoFire.getLocation(userId, new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                if (location!=null){
                    ref.child("location").setValue(location.latitude);
                    ref.child("longitude").setValue(location.longitude);
                }
                else{
                    ref.child("location").setValue(nLastLocation.getLatitude());
                    ref.child("longitude").setValue(nLastLocation.getLongitude());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast theError = Toast.makeText(RiderMapActivity.this, "Network Error", Toast.LENGTH_LONG);
                theError.show();
            }
        });
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
        final String userId = FirebaseAuth.getInstance().getUid();
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("AvailableRiders");
        final GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }
}
