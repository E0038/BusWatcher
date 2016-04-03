package org.e38.android.buswatcher;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.e38.android.buswatcher.model.BusPoint;
import org.e38.android.buswatcher.model.Buss;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final int GLOBAL_UPDATE_INTERVAL_DELAY = 2000 * 60;
    public static final int CONNECT_TIMEOUT = 10000;
    public static final int RESPONSE_OK = 200;
    private static final String webServiceHost = "192.168.1.11";
    private static final int webServicePort = 39284;
    private static final String remotePath = "/BussesWebExternal/webresources/api.lbuses";
    private static final String webServiceURL = "http://" + webServiceHost + ":" + webServicePort + remotePath;
    private final Handler mapsHandeler = new Handler();
    private final Object matriculasLocker = new Object();
    private final AtomicBoolean atomicStop = new AtomicBoolean(false);
    private final AtomicBoolean atomicGetAll = new AtomicBoolean(true);
    private final Object postLocker = new Object();
    private final List<String> matriculas = Collections.synchronizedList(new ArrayList<String>());
    private Thread watchThread = new Thread();
    private GoogleMap googleMap;
    private final Runnable BUS_UPDATER = new Runnable() {
        @Override
        public void run() {
            while (!atomicStop.get()) {
                try {
                    final List<PolylineOptions> recoridos = busesRecordido();
//                    synchronized (postLocker) {
                    mapsHandeler.post(new Runnable() {
                        @Override
                        public void run() {
                            clearMarkers();
                            processLines();
//                            Toast.makeText(MainActivity.this, "updateLines", Toast.LENGTH_SHORT).show();
                        }

                        private void processLines() {
                            for (PolylineOptions line : recoridos) {
                                LatLng inicio = line.getPoints().get(0);
                                googleMap.addPolyline(line);
                                googleMap.moveCamera(CameraUpdateFactory.newLatLng(inicio));
                                googleMap.addMarker(new MarkerOptions().title("Inicio Recordido").position(inicio));
                            }
                        }
                    });

                    Thread.sleep(GLOBAL_UPDATE_INTERVAL_DELAY);
                } catch (InterruptedException | ExecutionException | IOException | JSONException e) {
                    Log.d(MainActivity.class.getName(), e.toString(), e);
                }
            }
        }
    };

    private void clearMarkers() {
        googleMap.clear();
    }

    /**
     * calcula el recorido de los buses , no se recomienda usar en el hilo de la aplicacion
     */
    private List<PolylineOptions> busesRecordido() throws ExecutionException, InterruptedException, IOException, JSONException {
        List<PolylineOptions> lines = new ArrayList<>();
        Map<String, List<BusPoint>> busesPointMap = new HashMap<>();

        JSONArray jsonArray = getAllBusses();
        if (jsonArray.length() == 0) throw new IOException("error empty data");
        List<BusPoint> points = BusPoint.fromJsonArray(jsonArray);
        //map points
        for (BusPoint point : points) {
            if (matriculas.contains(point.getMatricula())) {
                if (busesPointMap.containsKey(point.getMatricula())) {
                    busesPointMap.get(point.getMatricula()).add(point);
                } else {
                    busesPointMap.put(point.getMatricula(), new ArrayList<>(Collections.singletonList(point)));
                }
            }
        }

        for (String matricula : busesPointMap.keySet()) {
            PolylineOptions bussLine = new PolylineOptions();
            List<BusPoint> pointList = busesPointMap.get(matricula);
            sortBussPointsByTime(pointList);
            for (int i = 0; i < pointList.size(); i++) {
                bussLine.add(new LatLng(pointList.get(i).getLatitut(), pointList.get(i).getLongitut()));
            }
            lines.add(bussLine);
        }
        return lines;
    }

    private JSONArray getAllBusses() throws InterruptedException, ExecutionException {
        return new AsyncTask<Void, Void, JSONArray>() {
            @Override
            protected JSONArray doInBackground(Void... params) {
                try {
                    JSONArray array = new JSONArray("[]");
                    HttpClient httpClient = new DefaultHttpClient();
                    HttpGet del = new HttpGet(webServiceURL);
                    del.setHeader("content-type", "application/json");
                    HttpResponse response = httpClient.execute(del);
                    if (response.getStatusLine().getStatusCode() == RESPONSE_OK) {
                        String respStr = EntityUtils.toString(response.getEntity());
                        array = new JSONArray(respStr);
                    }
                    return array;
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                    return new JSONArray();
                }
            }
        }.execute().get();
    }

    private void sortBussPointsByTime(List<BusPoint> points) {
        Collections.sort(points, new Comparator<BusPoint>() {
            @Override
            public int compare(BusPoint lhs, BusPoint rhs) {
                return (int) (lhs.getTime() - rhs.getTime());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
//        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.frag_bus_map);
        mapFragment.getMapAsync(this);
        //noinspection ConstantConditions
        findViewById(R.id.menu_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchAction(view);
            }
        });
    }

    private void searchAction(View view) {
        final boolean modeAll = atomicGetAll.get();
        Snackbar.make(view, R.string.filterSearchLabel, Snackbar.LENGTH_SHORT)
                .setAction(modeAll ? getString(R.string.showMatricula) : getString(R.string.showAll), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "ChangeMode", Toast.LENGTH_LONG).show();
                        if (modeAll) {
                            searchButtonModeFilter();
                        } else {
                            atomicGetAll.set(true);
                            updateMatricules();
                        }
                    }
                }).show();
    }

    private void searchButtonModeFilter() {
        final EditText input = new EditText(MainActivity.this);
        new AlertDialog.Builder(MainActivity.this)
                .setCancelable(true)
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (input.getText() == null || input.getText().toString().isEmpty())
                            dialog.cancel();
                        else {
                            String matricula = input.getText().toString();
                            if (checkMatricula(matricula)) {//not implemented
                                matriculas.clear();
                                matriculas.add(matricula);
                                atomicGetAll.set(false);
                            } else {
                                Toast.makeText(MainActivity.this, "Not implemented", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    private boolean checkMatricula(String matricula) {
        return false;//TODO write
    }

    private void updateMatricules() {
        if (checkHostAcces()) {
            synchronized (matriculasLocker) {
                matriculas.clear();
                matriculas.addAll(getAllMatriculas());
            }
        }
    }

    private boolean checkHostAcces() {
        boolean ck;
        if (ck = AndroidUtils.isNetWorkAvailable(this)) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL("http://" + webServiceHost + ":" + webServicePort).openConnection();
                connection.setRequestProperty("User-Agent", "Firefox 40.0: Mozilla/5.0 (X11; Linux x86_64; rv:40.0) Gecko/20100101 Firefox/40.0Linux");
                connection.setRequestProperty("Connection", "close");
                connection.setConnectTimeout(CONNECT_TIMEOUT);
                //android no deja realizar opereciones de red en el hilo principal aunque sea un service lanzara execption: NetworkOnMainThread
                ck = new AsyncTask<HttpURLConnection, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(HttpURLConnection... params) {
                        try {
                            params[0].connect();
                            return params[0].getResponseCode() == RESPONSE_OK;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                }.execute(connection).get();
            } catch (IOException | InterruptedException | ExecutionException e) {
                Log.e(getClass().getName(), "error checking conection", e);
                ck = false;
            }
        }
        return ck;
    }

    private Collection<? extends String> getAllMatriculas() {
        List<String> tmp_matricules = new ArrayList<>();
        try {
            List<BusPoint> busPoints;
            JSONArray jsonArray = getAllBusses();
            if (jsonArray.length() > 0) {
                busPoints = BusPoint.fromJsonArray(jsonArray);
                for (BusPoint point : busPoints) {
                    if (!tmp_matricules.contains(point.getMatricula())) tmp_matricules.add(point.getMatricula());
                }
            }
        } catch (InterruptedException | ExecutionException | JSONException e) {
            Log.e(MainActivity.class.getName(), "webservice error", e);
        }
        return tmp_matricules;
    }

    @Override
    protected void onDestroy() {
        atomicStop.set(true);
        try {
            watchThread.join();
        } catch (InterruptedException e) {
            Log.e(MainActivity.class.getName(), e.toString());
        }
        mapsHandeler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onMapReady(final GoogleMap map) {
        this.googleMap = map;
        this.googleMap.setTrafficEnabled(true);
        this.googleMap.getUiSettings().setZoomControlsEnabled(true);//para poder hacer zoom con el emulador sin los gestos de zoom
        this.googleMap.getUiSettings().setAllGesturesEnabled(true);
        updateMatricules();
        watchThread = new Thread(BUS_UPDATER, "Watcher-Thread");
        watchThread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

}
