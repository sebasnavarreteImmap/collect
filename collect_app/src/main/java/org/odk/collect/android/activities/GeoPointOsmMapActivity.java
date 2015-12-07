/*
 * Copyright (C) 2009 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.odk.collect.android.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.MarkerOptions;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.utilities.InfoLogger;
import org.odk.collect.android.widgets.GeoPointWidget;
import org.osmdroid.bonuspack.overlays.MapEventsOverlay;
import org.osmdroid.bonuspack.overlays.MapEventsReceiver;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Marker.OnMarkerDragListener;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.text.DecimalFormat;
import java.util.List;

//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
//import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;


public class GeoPointOsmMapActivity extends FragmentActivity implements LocationListener, OnMarkerDragListener, MapEventsReceiver {

	private SharedPreferences sharedPreferences;
	private String basemap;

	private static final String MAPQUEST_MAP_STREETS = "mapquest_streets";
	private static final String MAPQUEST_MAP_SATELLITE = "mapquest_satellite";

	private static final String LOCATION_COUNT = "locationCount";

	//private GoogleMap mMap;
	private MapView mMap;

	private Handler handler = new Handler();
	private MarkerOptions mMarkerOption;
	private Marker mMarker;

	private GeoPoint mLatLng;

	private TextView mLocationStatus;

	private LocationManager mLocationManager;
	private MapEventsOverlay overlayEventos;

	private Location mLocation;
	private Button mAcceptLocation;
	private Button mCancelLocation;
	private Button mReloadLocation;

	private boolean mCaptureLocation = true;
	private boolean mRefreshLocation = true;
	private boolean mIsDragged = false;
	private Button mShowLocation;

	private boolean mGPSOn = false;
	private boolean mNetworkOn = false;

	private double mLocationAccuracy;
	private int mLocationCount = 0;

	private boolean mZoomed = false;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		try {
				setContentView(R.layout.geopoint_osm_layout);
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			Toast.makeText(getBaseContext(), getString(R.string.google_play_services_error_occured), Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		if ( savedInstanceState != null ) {
			mLocationCount = savedInstanceState.getInt(LOCATION_COUNT);
		}

		mMap = (MapView) findViewById(R.id.omap);
		mMap.setMultiTouchControls(true);
		mMap.setBuiltInZoomControls(true);
		mMarker = new Marker(mMap);
		mMarker.setDraggable(true);
		mMarker.setOnMarkerDragListener(this);
//		mMap.getOverlays().add(mMarker);
		overlayEventos = new MapEventsOverlay(getBaseContext(), this);

		mMap.getOverlays().add(overlayEventos);

		handler.postDelayed(new Runnable() {
			public void run() {
				//Do something after 100ms
				GeoPoint point = new GeoPoint(34.08145, -39.85007);
				mMap.getController().setZoom(4);
				mMap.getController().setCenter(point);
			}
		}, 100);


		Intent intent = getIntent();

		mLocationAccuracy = GeoPointWidget.DEFAULT_LOCATION_ACCURACY;
		if (intent != null && intent.getExtras() != null) {
			if ( intent.hasExtra(GeoPointWidget.LOCATION) ) {
				double[] location = intent.getDoubleArrayExtra(GeoPointWidget.LOCATION);
				mLatLng = new GeoPoint(location[0], location[1]);

			}
			if ( intent.hasExtra(GeoPointWidget.ACCURACY_THRESHOLD) ) {
				mLocationAccuracy = intent.getDoubleExtra(GeoPointWidget.ACCURACY_THRESHOLD, GeoPointWidget.DEFAULT_LOCATION_ACCURACY);
			}
			mCaptureLocation = !intent.getBooleanExtra(GeoPointWidget.READ_ONLY, false);
			mRefreshLocation = mCaptureLocation;
		}
		/* Set up the map and the marker */
//		mMarkerOption = new MarkerOptions();


		mLocationStatus = (TextView) findViewById(R.id.location_status);

//		/*Zoom only if there's a previous location*/
		if (mLatLng != null){
			mLocationStatus.setVisibility(View.GONE);
//			mMarkerOption.position(mLatLng);
			mMarker.setPosition(mLatLng);
			mMap.invalidate();
			mRefreshLocation = false; // just show this position; don't change it...
			mZoomed = true;
		}

		mCancelLocation = (Button) findViewById(R.id.cancel_location);
		mCancelLocation.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger().logInstanceAction(this, "cancelLocation", "cancel");
				finish();
			}
		});

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


		// make sure we have a good location provider before continuing
		List<String> providers = mLocationManager.getProviders(true);
		for (String provider : providers) {
			if (provider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
				mGPSOn = true;
			}
			if (provider.equalsIgnoreCase(LocationManager.NETWORK_PROVIDER)) {
				mNetworkOn = true;
			}
		}
		if (!mGPSOn && !mNetworkOn) {
			Toast.makeText(getBaseContext(), getString(R.string.provider_disabled_error),
					Toast.LENGTH_SHORT).show();
			finish();
		}

		if ( mGPSOn ) {
			Location loc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if ( loc != null ) {
				InfoLogger.geolog("GeoPointMapActivity: " + System.currentTimeMillis() +
						" lastKnownLocation(GPS) lat: " +
						loc.getLatitude() + " long: " +
						loc.getLongitude() + " acc: " +
						loc.getAccuracy());
			} else {
				InfoLogger.geolog("GeoPointMapActivity: " + System.currentTimeMillis() +
						" lastKnownLocation(GPS) null location");
			}
		}

		if ( mNetworkOn ) {
			Location loc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if ( loc != null ) {
				InfoLogger.geolog("GeoPointMapActivity: " + System.currentTimeMillis() +
						" lastKnownLocation(Network) lat: " +
						loc.getLatitude() + " long: " +
						loc.getLongitude() + " acc: " +
						loc.getAccuracy() );
			} else {
				InfoLogger.geolog("GeoPointMapActivity: " + System.currentTimeMillis() +
						" lastKnownLocation(Network) null location");
			}
		}


		mAcceptLocation = (Button) findViewById(R.id.accept_location);
		if (mCaptureLocation){
			mAcceptLocation.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Collect.getInstance().getActivityLogger().logInstanceAction(this, "acceptLocation", "OK");
					returnLocation();
				}
			});
		}else{
			mAcceptLocation.setVisibility(View.GONE);
		}

		mReloadLocation = (Button) findViewById(R.id.reload_location);
		if (mCaptureLocation) {
			mReloadLocation.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mRefreshLocation = true;
					mReloadLocation.setVisibility(View.GONE);
					mLocationStatus.setVisibility(View.VISIBLE);
					if (mGPSOn) {
						mLocationManager.requestLocationUpdates(
								LocationManager.GPS_PROVIDER, 0, 0, GeoPointOsmMapActivity.this);
					}
					if (mNetworkOn) {
						mLocationManager.requestLocationUpdates(
								LocationManager.NETWORK_PROVIDER, 0, 0, GeoPointOsmMapActivity.this);
					}
				}

			});
			mReloadLocation.setVisibility(!mRefreshLocation ? View.VISIBLE : View.GONE);
		} else {
			mReloadLocation.setVisibility(View.GONE);
		}

		// Focuses on marked location
		mShowLocation = ((Button) findViewById(R.id.show_location));
		mShowLocation.setVisibility(View.VISIBLE);
		mShowLocation.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logInstanceAction(this, "showLocation", "onClick");
				zoomTo();
				mMap.invalidate();
			}
		});

		// not clickable until we have a marker set....
		mShowLocation.setClickable(false);




	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(LOCATION_COUNT, mLocationCount);
	}

	@Override
	protected void onPause() {
		super.onPause();

		// stops the GPS. Note that this will turn off the GPS if the screen goes to sleep.
		mLocationManager.removeUpdates(this);
	}


	@Override
	protected void onResume() {
		super.onResume();


		basemap = sharedPreferences.getString(PreferencesActivity.KEY_MAP_BASEMAP, MAPQUEST_MAP_STREETS);

		if (basemap.equals(MAPQUEST_MAP_STREETS)) {
			mMap.setTileSource(TileSourceFactory.MAPQUESTOSM);
		}else if(basemap.equals(MAPQUEST_MAP_SATELLITE)){
			mMap.setTileSource(TileSourceFactory.MAPQUESTAERIAL);
		}else{
			mMap.setTileSource(TileSourceFactory.MAPQUESTOSM);
		}

		if ( mRefreshLocation ) {
			mLocationStatus.setVisibility(View.VISIBLE);
			if (mGPSOn) {
				mLocationManager.requestLocationUpdates(
						LocationManager.GPS_PROVIDER, 0, 0, this);
			}
			if (mNetworkOn) {
				mLocationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 0, 0, this);
			}
			mShowLocation.setClickable(mMarker != null);
		}else{

			if(mLatLng != null){
				mMap.getOverlays().add(mMarker);
				mMarker.setPosition(mLatLng);
				zoomTo();
//				mMap.getController().setZoom(16);
//				mMap.getController().setCenter(mLatLng);
//				mMap.invalidate();

			}
			mShowLocation.setClickable(mMarker != null);

		}
	}

	private void zoomTo(){
		handler.postDelayed(new Runnable() {
			public void run() {
				//Do something after 100ms
				mMap.getController().setZoom(16);
				mMap.getController().setCenter(mLatLng);
				mMap.invalidate();
			}
		}, 200);

	}
	

	@Override
	protected void onStart() {
		super.onStart();
		Collect.getInstance().getActivityLogger().logOnStart(this);
	}

	@Override
	protected void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(this);
		super.onStop();
	}

	/**
	 * Sets up the look and actions for the progress dialog while the GPS is searching.
	 */



	private void returnLocation() {
		if (mIsDragged){
			Log.i(getClass().getName(), "IsDragged !!!");
			Intent i = new Intent();
			i.putExtra(
					FormEntryActivity.LOCATION_RESULT,
					mLatLng.getLatitude() + " " + mLatLng.getLongitude() + " "
							+ 0 + " " + 0);
			setResult(RESULT_OK, i);
		} else if (mLocation != null) {
			Log.i(getClass().getName(), "IsNotDragged !!!");
			Intent i = new Intent();
			i.putExtra(
					FormEntryActivity.LOCATION_RESULT,
					mLocation.getLatitude() + " " + mLocation.getLongitude() + " "
							+ mLocation.getAltitude() + " " + mLocation.getAccuracy());
			setResult(RESULT_OK, i);
		}
		finish();
	}

	private String truncateFloat(float f) {
		return new DecimalFormat("#.##").format(f);
	}

	private void stopGeolocating() {
		mRefreshLocation = false;
		mReloadLocation.setVisibility(View.VISIBLE);
		mLocationManager.removeUpdates(this);
		mMarker.setDraggable(true);
		mLocationStatus.setVisibility(View.GONE);
	}

	@Override
	public void onLocationChanged(Location location) {
		if (mRefreshLocation) {
			mLocation = location;
			if (mLocation != null) {
				// Bug report: cached GeoPoint is being returned as the first value.
				// Wait for the 2nd value to be returned, which is hopefully not cached?
				++mLocationCount;
				InfoLogger.geolog("GeoPointMapActivity: " + System.currentTimeMillis() +
						" onLocationChanged(" + mLocationCount + ") lat: " +
						mLocation.getLatitude() + " long: " +
						mLocation.getLongitude() + " acc: " +
						mLocation.getAccuracy() );

				if (mLocationCount > 1) {
					mLocationStatus.setText(getString(R.string.location_provider_accuracy,
							mLocation.getProvider(), truncateFloat(mLocation.getAccuracy())));
					mLatLng = new GeoPoint(mLocation.getLatitude(),mLocation.getLongitude());
					if ( !mZoomed ) {
						mZoomed = true;
						zoomTo();
						mMap.invalidate();

					} else {
						mMap.getController().animateTo(mLatLng);
						mMap.getController().setZoom(mMap.getZoomLevel());

					}

					// create a marker on the map or move the existing marker to the
					// new location
					if (mMarker == null) {
						mMarker = new Marker(mMap);
						mMap.getOverlays().add(mMarker);
						mMarker.setPosition(mLatLng);
						mShowLocation.setClickable(true);
					} else {
						mMap.getOverlays().add(mMarker);
						mMarker.setPosition(mLatLng);
					}

					//If location is accurate enough, stop updating position and make the marker draggable
					if (mLocation.getAccuracy() <= mLocationAccuracy) {
						stopGeolocating();
					}



				}

			} else {
				InfoLogger.geolog("GeoPointMapActivity: " + System.currentTimeMillis() +
						" onLocationChanged(" + mLocationCount + ") null location");
			}
		}
	}


	@Override
	public void onProviderDisabled(String provider) {

	}

	@Override
	public void onMarkerDrag(Marker arg0) {

	}

	@Override
	public void onMarkerDragEnd(Marker marker) {
		mLatLng = marker.getPosition();
		mAcceptLocation.setClickable(true);
		mIsDragged = true;
		mMap.getController().animateTo(mLatLng);
		mMap.getController().setZoom(mMap.getZoomLevel());



		//mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mLatLng, mMap.getCameraPosition().zoom));
	}

	@Override
	public void onMarkerDragStart(Marker arg0) {
		stopGeolocating();
	}


	@Override
	public void onProviderEnabled(String provider) {

	}


	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}


	@Override
	public boolean singleTapConfirmedHelper(GeoPoint geoPoint) {
		return false;
	}

	@Override
	public boolean longPressHelper(GeoPoint geoPoint) {
		if (mMarker == null) {
			mMarker.setPosition(geoPoint);
			//mMarker = mMap.addMarker(mMarkerOption);
			mShowLocation.setClickable(true);
		} else {
			mMarker.setPosition(geoPoint);
			//mMarker.setPosition(latLng);
		}
		mMap.invalidate();
		mLatLng=geoPoint;
		mIsDragged = true;
		stopGeolocating();
		mMarker.setDraggable(true);
		return false;
	}
}
