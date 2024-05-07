package com.cesar.pruebamapa;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.here.sdk.core.Color;
import com.here.sdk.core.GeoCircle;
import com.here.sdk.core.GeoCoordinates;
import com.here.sdk.core.GeoCoordinatesUpdate;
import com.here.sdk.core.GeoOrientationUpdate;
import com.here.sdk.core.GeoPolygon;
import com.here.sdk.core.GeoPolyline;
import com.here.sdk.core.LanguageCode;
import com.here.sdk.core.engine.SDKNativeEngine;
import com.here.sdk.core.engine.SDKOptions;
import com.here.sdk.core.errors.InstantiationErrorException;
import com.here.sdk.mapview.LineCap;
import com.here.sdk.mapview.LocationIndicator;
import com.here.sdk.mapview.MapCamera;
import com.here.sdk.mapview.MapCameraAnimation;
import com.here.sdk.mapview.MapCameraAnimationFactory;
import com.here.sdk.mapview.MapError;
import com.here.sdk.mapview.MapMarker;
import com.here.sdk.mapview.MapMeasure;
import com.here.sdk.mapview.MapMeasureDependentRenderSize;
import com.here.sdk.mapview.MapPolygon;
import com.here.sdk.mapview.MapPolyline;
import com.here.sdk.mapview.MapScene;
import com.here.sdk.mapview.MapScheme;
import com.here.sdk.mapview.MapView;
import com.here.sdk.mapview.RenderSize;
import com.here.sdk.search.Address;
import com.here.sdk.search.AddressQuery;
import com.here.sdk.search.Place;
import com.here.sdk.search.SearchCallback;
import com.here.sdk.search.SearchEngine;
import com.here.sdk.search.SearchError;
import com.here.sdk.search.SearchOptions;
import com.here.time.Duration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PlatformPositioningProvider.PlatformLocationListener {

    private FloatingActionButton ubicacion,radio,busqueda,ruta;
    private static final int REQUEST_INTERNET_PERMISSION = 100;
    private static final int REQUEST_LOCATION_PERMISSION = 101;
    private MapCamera mapCamera;
    private SearchEngine searchEngine;
    private MapScene mapScene;
    private MapPolygon mapCircle;
    private EditText input_radio,input_busqueda,input_coordenada1,input_coordenada2;
    private ImageButton boton_radio,boton_busqueda,boton_ruta;
    private SearchExample searchExample;
    private MapView mapView;
    private final List<MapMarker> mapMarkerList = new ArrayList<>();
    private PlatformPositioningProvider positioningProvider;
    private LocationIndicator currentLocationIndicator;
    private RoutingExample routingExample;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Siempre se debe inicializar el sdk antes del contentView si no la app peta
        initializeHERESDK();
        setContentView(R.layout.activity_main);

        // Creamos la instancia del mapa
        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapCamera = mapView.getCamera();
        mapScene = mapView.getMapScene();
        try {
            searchEngine = new SearchEngine();
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of SearchEngine failed: " + e.error.name());
        }
        searchExample = new SearchExample(MainActivity.this, mapView);
        loadMapScene();

        // Solicitar permisos de internet y de localización
        requestInternetPermission();
        requestLocationPermission();

        // Initialize positioning provider
        positioningProvider = new PlatformPositioningProvider(this);

        input_radio = findViewById(R.id.radio_input);
        boton_radio = findViewById(R.id.btn_input_radio);

        input_busqueda = findViewById(R.id.ciudad_input);
        boton_busqueda = findViewById(R.id.btn_input_ciudad);

        input_coordenada1 = findViewById(R.id.coordendas_iniciales_input);
        input_coordenada2 = findViewById(R.id.coordendas_finales_input);
        boton_ruta = findViewById(R.id.btn_input_coordenadas);

        ubicacion = findViewById(R.id.btn_ubicacion);
        ubicacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener las coordenadas actuales del teléfono
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        GeoCoordinates userCoordinates = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        // Llamar al método flyTo con las coordenadas actuales
                        input_radio.setVisibility(View.GONE);
                        boton_radio.setVisibility(View.GONE);
                        input_busqueda.setVisibility(View.GONE);
                        boton_busqueda.setVisibility(View.GONE);
                        input_coordenada1.setVisibility(View.GONE);
                        input_coordenada2.setVisibility(View.GONE);
                        boton_ruta.setVisibility(View.GONE);
                        flyTo(userCoordinates);
                        getAddressForCoordinates(userCoordinates);
                    }
                }
            }
        });

        radio = findViewById(R.id.btn_radio);
        radio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input_radio.setVisibility(View.VISIBLE);
                boton_radio.setVisibility(View.VISIBLE);
                input_busqueda.setVisibility(View.GONE);
                boton_busqueda.setVisibility(View.GONE);
                input_coordenada1.setVisibility(View.GONE);
                input_coordenada2.setVisibility(View.GONE);
                boton_ruta.setVisibility(View.GONE);
                input_radio.setText("");
                boton_radio.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Obtener el valor del radio en kilómetros del EditText
                        String radioString = input_radio.getText().toString();
                        if (!radioString.isEmpty()) {
                            double radio = Double.parseDouble(radioString);
                            // Obtener las coordenadas actuales del teléfono
                            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                            if (locationManager != null && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (lastKnownLocation != null) {
                                    GeoCoordinates userCoordinates = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                                    // Llamar al método showMapCircle con las coordenadas actuales y el radio
                                    showMapCircle(userCoordinates, (float) radio * 1000); // Convertir el radio a metros
                                }
                            }
                        } else {
                            // Manejar el caso en que el EditText esté vacío
                            Toast.makeText(MainActivity.this, "Por favor ingrese un valor de radio.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        busqueda = findViewById(R.id.btn_busqueda);

        busqueda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input_busqueda.setVisibility(View.VISIBLE);
                boton_busqueda.setVisibility(View.VISIBLE);
                input_radio.setVisibility(View.GONE);
                boton_radio.setVisibility(View.GONE);
                input_coordenada1.setVisibility(View.GONE);
                input_coordenada2.setVisibility(View.GONE);
                boton_ruta.setVisibility(View.GONE);
                input_busqueda.setText("");
                boton_busqueda.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String direccion = input_busqueda.getText().toString();
                        if(!direccion.isEmpty()){
                            // Llamar al método de búsqueda en la clase SearchExample
                            searchExample.geocodeAddressAtLocation(direccion, mapView.getCamera().getState().targetCoordinates);
                        }else{
                            Toast.makeText(MainActivity.this, "Por favor, ingrese una dirección", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        ruta = findViewById(R.id.btn_ruta);
        ruta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener las coordenadas actuales del teléfono
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        GeoCoordinates userCoordinates = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        // Llamar al método flyTo con las coordenadas actuales
                        input_coordenada1.setVisibility(View.VISIBLE);
                        input_coordenada2.setVisibility(View.VISIBLE);
                        boton_ruta.setVisibility(View.VISIBLE);
                        input_radio.setVisibility(View.GONE);
                        boton_radio.setVisibility(View.GONE);
                        input_busqueda.setVisibility(View.GONE);
                        boton_busqueda.setVisibility(View.GONE);
                        input_coordenada1.setText("");
                        input_coordenada2.setText("");
                        getAddressForCoordinatess(userCoordinates);
                    }
                }
                boton_ruta.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                });
            }
        });



        //FloatingActionButton floatingButton = findViewById(R.id.floating);
        /*floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Obtener las coordenadas actuales del teléfono
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastKnownLocation != null) {
                        GeoCoordinates userCoordinates = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                        // Llamar al método flyTo con las coordenadas actuales
                        flyTo(userCoordinates);
                        getAddressForCoordinates(userCoordinates);
                        showMapCircle(userCoordinates);
                    }
                }
            }
        });*/


    }

    public void searchExampleButtonClicked(View view) {
        searchExample.onSearchButtonClicked();
    }

    // Método que se pasa al oncreate
    private void initializeHERESDK() {
        // Ingresamos las credenciales que nos da HERE
        String accessKeyID = "fGLXo2jToDFFBWr2LiqErg";
        String accessKeySecret = "4QQKkB8QAd08HePdpq-Fc6RPomrsK0IK0xSM2hiGy6cNfzVjIzIrMMaOZmQnvGCESk-VaElTQ3oYxWHu2SmbPQ";
        SDKOptions options = new SDKOptions(accessKeyID, accessKeySecret);
        try {
            Context context = this;
            SDKNativeEngine.makeSharedInstance(context, options);
        } catch (InstantiationErrorException e) {
            throw new RuntimeException("Initialization of HERE SDK failed: " + e.error.name());
        }
    }


    private void disposeHERESDK() {
        // Free HERE SDK resources before the application shuts down.
        // Usually, this should be called only on application termination.
        // Afterwards, the HERE SDK is no longer usable unless it is initialized again.
        SDKNativeEngine sdkNativeEngine = SDKNativeEngine.getSharedInstance();
        if (sdkNativeEngine != null) {
            sdkNativeEngine.dispose();
            // For safety reasons, we explicitly set the shared instance to null to avoid situations,
            // where a disposed instance is accidentally reused.
            SDKNativeEngine.setSharedInstance(null);
        }
    }

    private void loadMapScene() {
        // Verifica si tienes permisos para acceder a la ubicación del usuario
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Obtén la última ubicación conocida del usuario
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocation != null) {
                // Si se encuentra una ubicación conocida, mueve la cámara del mapa a esa ubicación
                GeoCoordinates userCoordinates = new GeoCoordinates(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                routingExample = new RoutingExample(MainActivity.this, mapView,userCoordinates);
                //mapView.getCamera().lookAt(userCoordinates);
            }
        }
        // Verifica si es después de las 8:00 p.m. y antes de las 6:00 a.m.
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 20 || hour < 6) {
            // Carga la escena del mapa con el esquema MapScheme.NORMAL_NIGHT
            mapView.getMapScene().loadScene(MapScheme.NORMAL_NIGHT, new MapScene.LoadSceneCallback() {
                @Override
                public void onLoadScene(@Nullable MapError mapError) {
                    if (mapError == null) {
                        // No se produjo ningún error al cargar la escena del mapa
                    } else {
                        Log.d("loadMapScene()", "Loading map failed: mapError: " + mapError.name());
                    }
                }
            });
        } else {
            // Carga la escena del mapa con el esquema MapScheme.NORMAL_DAY
            mapView.getMapScene().loadScene(MapScheme.NORMAL_DAY, new MapScene.LoadSceneCallback() {
                @Override
                public void onLoadScene(@Nullable MapError mapError) {
                    if (mapError == null) {
                        // No se produjo ningún error al cargar la escena del mapa
                    } else {
                        Log.d("loadMapScene()", "Loading map failed: mapError: " + mapError.name());
                    }
                }
            });
        }
    }

    private void clearMap() {
        for (MapMarker mapMarker : mapMarkerList) {
            mapView.getMapScene().removeMapMarker(mapMarker);
        }
        mapMarkerList.clear();
    }

    public void addRouteButtonClicked(GeoCoordinates primera_coordenada,GeoCoordinates segunda_coordenada) {
        routingExample.addRoute(primera_coordenada,segunda_coordenada);
    }



    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        // Stop location updates when the activity pauses
        stopLocationUpdates();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        // Start location updates when the activity resumes
        startLocationUpdates();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        disposeHERESDK();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        mapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    // Método para solicitar el permiso de Internet
    private void requestInternetPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_INTERNET_PERMISSION);
        } else {
            // El permiso ya está concedido
        }
    }

    // Método para solicitar el permiso de localización
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            // El permiso ya está concedido
        }
    }

    // Método para manejar las respuestas de las solicitudes de permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_INTERNET_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El permiso de Internet fue concedido
                } else {
                    // El permiso de Internet fue denegado
                }
                break;
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // El permiso de localización fue concedido
                } else {
                    // El permiso de localización fue denegado
                }
                break;
        }
    }

    private void startLocationUpdates() {
        // Start location updates
        positioningProvider.startLocating(this);
    }

    private void stopLocationUpdates() {
        // Stop location updates
        positioningProvider.stopLocating();
    }

    @Override
    public void onLocationUpdated(Location location) {
        // Actualiza la posición del usuario en el mapa
        //updateMapUserLocation(location.getLatitude(), location.getLongitude());

        // Agrega un nuevo indicador de ubicación en las nuevas coordenadas
        GeoCoordinates userCoordinates = new GeoCoordinates(location.getLatitude(), location.getLongitude());
        addLocationIndicator(userCoordinates, LocationIndicator.IndicatorStyle.PEDESTRIAN);
    }


    /*private void updateMapUserLocation(double latitude, double longitude) {
        GeoCoordinates userCoordinates = new GeoCoordinates(latitude, longitude);
        mapView.getCamera().lookAt(userCoordinates);
    }*/

    private void addLocationIndicator(GeoCoordinates geoCoordinates,
                                      LocationIndicator.IndicatorStyle indicatorStyle) {
        // Elimina el indicador de ubicación actual, si existe
        removeLocationIndicator();

        // Crea un nuevo indicador de ubicación
        LocationIndicator locationIndicator = new LocationIndicator();
        locationIndicator.setLocationIndicatorStyle(indicatorStyle);

        // A LocationIndicator is intended to mark the user's current location,
        // including a bearing direction.
        com.here.sdk.core.Location location = new com.here.sdk.core.Location(geoCoordinates);
        location.time = new Date();
        location.bearingInDegrees = getRandom(0, 360);

        locationIndicator.updateLocation(location);

        // Show the indicator on the map view.
        locationIndicator.enable(mapView);

        // Asigna la referencia al nuevo indicador de ubicación
        currentLocationIndicator = locationIndicator;
    }

    private MapPolygon createMapCircle(GeoCoordinates centerCoordinates, float radiusInMeters) {
        GeoCircle geoCircle = new GeoCircle(centerCoordinates, radiusInMeters);

        GeoPolygon geoPolygon = new GeoPolygon(geoCircle);
        Color fillColor = Color.valueOf(240, 128, 0.50f, 0.45f); // RGBA
        MapPolygon mapPolygon = new MapPolygon(geoPolygon, fillColor);

        return mapPolygon;
    }


    public void showMapCircle(GeoCoordinates centerCoordinates, float radius) {
        // Primero verifica si hay un MapPolygon existente y lo elimina
        if (mapCircle != null) {
            mapScene.removeMapPolygon(mapCircle);
        }

        // Crea el nuevo MapPolygon
        mapCircle = createMapCircle(centerCoordinates, radius);

        // Agrega el nuevo MapPolygon al mapScene
        mapScene.addMapPolygon(mapCircle);
    }



    private void removeLocationIndicator() {
        // Verifica si hay un indicador de ubicación actual mostrándose y lo elimina
        if (currentLocationIndicator != null) {
            currentLocationIndicator.disable();
            currentLocationIndicator = null;
        }
    }

    private void getAddressForCoordinates(GeoCoordinates geoCoordinates) {
        SearchOptions reverseGeocodingOptions = new SearchOptions();
        reverseGeocodingOptions.languageCode = LanguageCode.EN_GB;
        reverseGeocodingOptions.maxItems = 1;

        searchEngine.search(geoCoordinates, reverseGeocodingOptions, addressSearchCallback);
    }

    private final SearchCallback addressSearchCallback = new SearchCallback() {
        @Override
        public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
            if (searchError != null) {
                showDialog("Reverse geocoding", "Error: " + searchError.toString());
                return;
            }

            // If error is null, list is guaranteed to be not empty.
            showDialog("Información de la ubicación:", list.get(0).getAddress().addressText);
        }
    };

    private void showDialog(String title, String message) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    private void getAddressForCoordinatess(GeoCoordinates geoCoordinates) {
        SearchOptions reverseGeocodingOptions = new SearchOptions();
        reverseGeocodingOptions.languageCode = LanguageCode.EN_GB;
        reverseGeocodingOptions.maxItems = 1;

        searchEngine.search(geoCoordinates, reverseGeocodingOptions, addresssSearchCallback);
    }

    private final SearchCallback addresssSearchCallback = new SearchCallback() {
        @Override
        public void onSearchCompleted(@Nullable SearchError searchError, @Nullable List<Place> list) {
            if (searchError != null) {
                showDialog("Reverse geocoding", "Error: " + searchError.toString());
                return;
            }

            // If error is null, list is guaranteed to be not empty.
            input_coordenada1.setText(list.get(0).getAddress().addressText);
        }
    };


    private double getRandom(double min, double max) {
        return min + Math.random() * (max - min);
    }

    private void flyTo(GeoCoordinates geoCoordinates) {
        GeoCoordinatesUpdate geoCoordinatesUpdate = new GeoCoordinatesUpdate(geoCoordinates);
        double bowFactor = 1;
        MapCameraAnimation animation =
                MapCameraAnimationFactory.flyTo(geoCoordinatesUpdate, bowFactor, Duration.ofSeconds(3));
        mapCamera.startAnimation(animation);
    }


    private void tiltMap() {
        double bearing = mapView.getCamera().getState().orientationAtTarget.bearing;
        double tilt =  60;
        mapView.getCamera().setOrientationAtTarget(new GeoOrientationUpdate(bearing, tilt));
    }

}