package wesleycoelho.cursoudemy.arcgisapi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.ArcGISFeatureTable;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.internal.jni.CoreArcGISFeatureTable;
import com.esri.arcgisruntime.internal.jni.CoreRequest;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionManeuver;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFutureTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class MainActivity extends AppCompatActivity {
    private MapView mapView;
    private FloatingActionButton fab;
    private ImageView btnActivityAddress;
    private boolean fabButton = false;
    private final Activity activity = this;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String[] permissoesNecessarias = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private SearchView searchView;
    private final GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
    private LocatorTask locatorTask = new LocatorTask("https://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer");
    //variaveis rota
    private ListView listView;
    private List<String> directionList = new ArrayList<String>();
    private  List<Stop> routeStops = new ArrayList<>();
    private  GraphicsOverlay graphicsOverlayRoute = new GraphicsOverlay();
    private ArrayAdapter<String> arrayAdapter;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // this.getSupportActionBar().hide();

        //valida permissões
         Permissao.validaPermissoes(permissoesNecessarias, activity, 1);

        mapView = findViewById(R.id.mapView);
        searchView = findViewById(R.id.searchTxt);
        listView = findViewById(R.id.listView);
        directionList.add("Tap to add two points to the map to find a route between them.");
        arrayAdapter = new ArrayAdapter<String>(
                getApplicationContext(),
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                directionList
        );


        //objeto responsavel por gerenciar a localização do ususário
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        /*locationListener = new LocationListener() {

            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                mapView.setViewpoint(new Viewpoint(latitude, longitude, 10000.0));
                //locationManager.removeUpdates(locationListener);
            }

            @Override
            public void onProviderEnabled(@NonNull String provider) {
                LocationListener.super.onProviderEnabled(provider);

            }


        };*/

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               if(fabButton == false){
                   fabButton = true;
                   mapView.setMap(new ArcGISMap(BasemapStyle.ARCGIS_IMAGERY));
                   fab.setImageResource(R.drawable.ic_topografic);
               }else{
                   fabButton = false;
                   fab.setImageResource(R.drawable.ic_imagery);
                   mapView.setMap(new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC));
               }

            }
        });
        listView.setAdapter(arrayAdapter);
        setApiKeyForApp();
        setupMap();
        setupSearchViewListener();

    }//fim método onCreate

    private void addStops(Stop stop){
        routeStops.add(stop);
        SimpleMarkerSymbol stopMarker = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.BLUE, 20f);
        Point routeStopGeometry = stop.getGeometry();
        graphicsOverlayRoute.getGraphics().add(new Graphic(routeStopGeometry, stopMarker));
    }

    private void clear() {
        routeStops.clear();
        graphicsOverlayRoute.getGraphics().clear();
        directionList.clear();
        directionList.add("Tap to add two points to the map to find a route between them.");
        arrayAdapter.notifyDataSetChanged();
    }




    private void findRoute(){
        RouteTask routeTask = new RouteTask(this,
                "https://route-api.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World");

        ListenableFuture<RouteParameters> routeParameterFuture = routeTask.createDefaultParametersAsync();
        routeParameterFuture.addDoneListener(
                new Runnable() {
                     @Override
                     public void run() {
                         RouteParameters routeParameters = null;
                         try {
                             routeParameters = routeParameterFuture.get();
                             routeParameters.setStops(routeStops);
                             routeParameters.setReturnStops(true);
                             routeParameters.setReturnRoutes(true);
                             routeParameters.setReturnDirections(true);
                             ListenableFuture<RouteResult> routeResult = routeTask.solveRouteAsync(routeParameters);
                             routeResult.addDoneListener(new Runnable() {
                                 @Override
                                 public void run() {
                                     RouteResult result = null;
                                     try {
                                         result = routeResult.get();
                                         List<Route> routes = result.getRoutes();
                                         if( !routes.isEmpty()){
                                             Route route = routes.get(0);

                                             Polyline shape = route.getRouteGeometry();
                                             Graphic routeGraphic = new Graphic(shape, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID,Color.BLUE, 2f));
                                             graphicsOverlayRoute.getGraphics().add(routeGraphic);

                                             directionList.clear();
                                             for ( DirectionManeuver direction :  route.getDirectionManeuvers()) {
                                                 directionList.add(direction.getDirectionText());
                                             }
                                             arrayAdapter.notifyDataSetChanged();
                                         }
                                     } catch (ExecutionException | InterruptedException e) {
                                         e.printStackTrace();
                                     }
                                 }
                             });
                         } catch (ExecutionException | InterruptedException e) {
                             e.printStackTrace();
                         }
                     }
                }
        );

    }

    private void performGeocode(String query){
    /*    Toast.makeText(getApplicationContext(), "No results found.", Toast.LENGTH_LONG).show();
        GeocodeParameters geocodeParameters = new GeocodeParameters();
        geocodeParameters.getResultAttributeNames().add("*");
        geocodeParameters.setMaxResults(1);
        geocodeParameters.setOutputSpatialReference(mapView.getSpatialReference());*/
        try {
            ListenableFuture<List<GeocodeResult>> geocodeResult = locatorTask.geocodeAsync(query);
             displayResult(geocodeResult.get().get(0));
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void displayResult(GeocodeResult geocodeResult){
        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
        graphicsOverlay.getGraphics().clear();
        TextSymbol textSymbol= new TextSymbol(
                12f,
                geocodeResult.getLabel(),
                Color.BLACK,
                TextSymbol.HorizontalAlignment.CENTER,
                TextSymbol.VerticalAlignment.BOTTOM
                );
        Graphic textGraphic = new Graphic(geocodeResult.getDisplayLocation(), textSymbol);
        graphicsOverlay.getGraphics().add(textGraphic);

        SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.SQUARE, Color.RED, 12.0f);
        Graphic markerGraphic = new Graphic(geocodeResult.getDisplayLocation(), geocodeResult.getAttributes(), simpleMarkerSymbol);
        graphicsOverlay.getGraphics().add(markerGraphic);
        mapView.getGraphicsOverlays().add(graphicsOverlay);
        mapView.setViewpointAsync(new Viewpoint(geocodeResult.getDisplayLocation(),25000.0 ));



    }


    private void setupSearchViewListener(){
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performGeocode(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                return false;
            }
        });
    }




    @SuppressLint("ClickableViewAccessibility")
    private void setupMap() {
        ArcGISMap map = new ArcGISMap(BasemapStyle.ARCGIS_STREETS);
        mapView.setMap(map);
        mapView.setViewpoint(new Viewpoint(34.0539, -118.2453, 144447.638572));
        mapView.getGraphicsOverlays().add(graphicsOverlayRoute);
        mapView.setOnTouchListener(new DefaultMapViewOnTouchListener(getApplicationContext(), mapView){
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                android.graphics.Point screenPoint =  new android.graphics.Point(Math.round(e.getX()) ,Math.round(e.getY()));
                switch(routeStops.size()){
                    case 0 :{
                        addStops(new Stop(mapView.screenToLocation(screenPoint)));
                        Toast.makeText(getApplicationContext(), "Ponto : " + routeStops.size(), Toast.LENGTH_SHORT).show();

                    }break;
                    case 1 :{
                        addStops(new Stop(mapView.screenToLocation(screenPoint)));
                        findRoute();
                        Toast.makeText(getApplicationContext(), "Calculating route", Toast.LENGTH_SHORT).show();
                    }break;
                    default:{
                        clear();
                        addStops(new Stop(mapView.screenToLocation(screenPoint)));
                    }
                }

                return super.onSingleTapConfirmed(e);
            }
        });
    }

    private void setApiKeyForApp() {
        ArcGISRuntimeEnvironment.setApiKey("COLE_AQUI_SUA_API_KEY");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.dispose();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int permissaoResultado : grantResults) {
            if (permissaoResultado == PackageManager.PERMISSION_DENIED) {
                //alerta
                alertaValidacaoPermissao();
            } else if ( (permissaoResultado == PackageManager.PERMISSION_GRANTED)) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                  Toast.makeText(getApplicationContext(), "onRequestPermissionsResult", Toast.LENGTH_SHORT).show();
                    //  Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                   // mapView.setViewpoint(new Viewpoint(location.getLatitude(), location.getLongitude(), 10000.0));
                   // addPonto(location.getLatitude(), location.getLongitude());
                }

            }
        }
    }
    private void addPonto(double latitude, double longitude){
        GraphicsOverlay graphicssOverlay = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(graphicssOverlay);
        //cria ponto
        Point ponto = new Point(longitude, latitude, SpatialReferences.getWgs84());
        //cria círculo vermelho
        SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, -0xa8cd, 10f);
        //borda  azul
        SimpleLineSymbol blueOutlineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID,-0xff9c01, 2f);
        //atribui ao círculo a borda azul
        simpleMarkerSymbol.setOutline(blueOutlineSymbol);
        //cria layer gráfico
        Graphic pontoGrafico = new Graphic(ponto, simpleMarkerSymbol);
        //adiciona ponto
        graphicssOverlay.getGraphics().add(pontoGrafico);

    }

    private void alertaValidacaoPermissao(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissoes negadas!");
        builder.setCancelable(false);
        builder.setMessage("Para utilizar o app é necessário aceitar as permissões!");
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


}