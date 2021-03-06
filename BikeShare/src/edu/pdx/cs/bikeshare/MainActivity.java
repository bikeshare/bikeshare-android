package edu.pdx.cs.bikeshare;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends ActionBarActivity {
	MyItemizedOverlay myItemizedOverlay = null;
	public static final String apiUrl = "http://api.bikeshare.cs.pdx.edu";
	public static final String tag = "MainActivity";
	public static Drawable bikeMarker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ResourceProxy resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());

		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		MapTileProviderBasic tileProvider = new MapTileProviderBasic(getApplicationContext());
		ITileSource tileSource = new XYTileSource("BikeShare PDX", null, 11, 19, 256, ".png", new String [] {"http://bikeshare.cs.pdx.edu/osm/"});
		tileProvider.setTileSource(tileSource);
		TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this.getBaseContext());
		mapView.getOverlays().add(tilesOverlay);

		IMapController myMapController = mapView.getController();
		myMapController.setZoom(13);
		myMapController.setCenter(new GeoPoint(45.55, -122.70));

		Drawable marker=getResources().getDrawable(R.drawable.ic_location_marker);
		int markerWidth = marker.getIntrinsicWidth();
		int markerHeight = marker.getIntrinsicHeight();
		marker.setBounds(0, markerHeight, markerWidth, 0);

		Drawable bikeMarker = getResources().getDrawable(R.drawable.ic_launcher);
		int bikeMarkerWidth = bikeMarker.getIntrinsicWidth();
		int bikeMarkerHeight = bikeMarker.getIntrinsicHeight();
		bikeMarker.setBounds(0, bikeMarkerHeight, bikeMarkerWidth, 0);

		myItemizedOverlay = new MyItemizedOverlay(marker, resourceProxy, this, mapView);
		mapView.getOverlays().add(myItemizedOverlay);

		new ShowAllStations().execute();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}


	private class ShowAllStations extends AsyncTask<Void, Void, String> {
		private final static String route = "/REST/1.0/stations/all";
		private final static String tag = "ShowAllStations";

		@Override
		protected String doInBackground(Void... params) {
			// Call REST API to find locations of BikeShare stations.
			HttpClient web = new DefaultHttpClient();
			String apiData = null;
			try {
				HttpResponse resp = web.execute(new HttpGet(apiUrl + route));
				StatusLine status = resp.getStatusLine();
				if (status.getStatusCode() == HttpStatus.SC_OK) {
					// REST API returned success.
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					resp.getEntity().writeTo(out);
					out.close();
					apiData = out.toString();
				} else {
					// REST API returned an error.
					resp.getEntity().getContent().close();
					Log.e(tag,"REST API returned error: " + status.getReasonPhrase());
				}
			} catch (ClientProtocolException e) {
				// HTTP barfed.
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return apiData;
		}

		@Override
		protected void onPostExecute(String result) {
			// If we received data from the API, parse the JSON.
			if (result != null) {
				try {
					JSONObject jApiData = new JSONObject(result);
					JSONArray jStations = jApiData.getJSONArray("stations");
					for (int i = 0; i < jStations.length(); i++) {
						JSONObject jStation = jStations.getJSONObject(i);

						// Construct a station object from the data.
						double latitude = jStation.getDouble("LATITUDE");
						double longitude = jStation.getDouble("LONGITUDE");
						String name = jStation.getString("STATION_NAME");
						String addr = jStation.getString("STREET_ADDRESS");
						Station s = new Station(name, addr, new GeoPoint(latitude,longitude));
						s.station_id = jStation.getInt("STATION_ID");
						s.station_name = name;
						s.street_address = addr;
						s.latitude = latitude;
						s.longitude = longitude;

						// Put station pin on the map.
						myItemizedOverlay.addItem(s);
					}
				} catch (JSONException e) {
					// Failed to parse the JSON.
					e.printStackTrace();
				}
			} else {
				// Failed to get data from the API.
			}
		}
	}
}
