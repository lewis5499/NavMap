package com.example.navmap;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay;
import org.osmdroid.wms.WMSTileSource;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public enum pointType {START, END}
    public enum eventType {TAPMAP, ONROUTE}
    private BoundingBox boundingBox;
    private MapView mapView;
    private ScaleBarOverlay scaleBarOverlay;
    private RotationGestureOverlay rotationGestureOverlay;
    private Button btnRoute, btnUserPos;
    private EditText etStartPoint, etEndPoint;
    private Drawable dbStartPoint, dbEndPoint;
    private String textStartPoint, textEndPoint;
    private String routeTask, layerName;
    private GeoPoint startPoint, endPoint;
    private List<GeoPoint> points;
    private SimpleLocationOverlay spLocationOverlay;
    private MyLocationNewOverlay myLocationOverlay;
    private boolean queryState, clickState;  // State Machine
    // [queryState] false: query by touching points; true: query by points' texts
    // [clickState] false: touch the 2nd point;      true: touch the 1st point;
    private AudioManager audioManager;
    private Vibrator vibrator;
    private SensorManager sensorManager;
    private ImageView imgMapCompass, imgPosCompass;
    private float lastCompassRotateDegree;
    private float[] accelerometerValues, magneticValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupMainActivityTheme();
        setupUIComponents();
        setupMapView();

        setupPermissionRequest();

        setupUserLocationListener();
        setupMapViewListener();
        setupEditTextListener();
        setupButtonListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();

        if (myLocationOverlay != null) {
            myLocationOverlay.disableMyLocation(); // Disable my location updates
            myLocationOverlay.disableFollowLocation(); // Disable follow location updates
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        if (myLocationOverlay != null) {
            myLocationOverlay.enableMyLocation(); // Enable my location updates
            myLocationOverlay.enableFollowLocation(); // Enable follow location updates
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(@NonNull SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values.clone();
        }

        float[] R = new float[9];
        float[] values = new float[3];
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticValues);
        SensorManager.getOrientation(R, values);

        float rotateDegree = -(float) Math.toDegrees(values[0]);

        // 创建 ObjectAnimator 实例，指定旋转属性和动画范围
        ObjectAnimator animator = ObjectAnimator.ofFloat(imgPosCompass, "rotation", lastCompassRotateDegree, rotateDegree);
        animator.setDuration(1000); // 设置动画持续时间（单位：毫秒）
        animator.setInterpolator(new AccelerateDecelerateInterpolator()); // 添加插值器
        animator.start(); // 开始动画

        lastCompassRotateDegree = rotateDegree; // 更新上一次旋转角度
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void setupMainActivityTheme() {
        setTheme(R.style.Theme_NavMap);//恢复原有的样式

        View rootView = getWindow().getDecorView();
        ObjectAnimator fadeInAnimator = ObjectAnimator.ofFloat(rootView, "alpha", 0.618f, 1f);
        fadeInAnimator.setDuration(1000); // 设置动画持续时间为1秒
        fadeInAnimator.start(); // 启动动画
    }

    private void setupUIComponents() {
        mapView = null;
        startPoint = new GeoPoint(30.53717,114.3488);
        endPoint   = new GeoPoint(30.53800,114.3718);
        queryState = false;
        clickState = true;
        accelerometerValues = new float[3];
        magneticValues = new float[3];
        textStartPoint = textEndPoint = "";
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        layerName = "whuB07:route";
        routeTask = "http://202.114.122.22:2107/geoserver/whuB07";
        etStartPoint = findViewById(R.id.editText_src);
        etEndPoint = findViewById(R.id.editText_dest);
        mapView = findViewById(R.id.osmMapView);
        btnUserPos = findViewById(R.id.m_coordinate);
        btnRoute = findViewById(R.id.m_btnRoute);
        dbStartPoint = ResourcesCompat.getDrawable(getResources(), R.drawable.img_start, null);
        dbEndPoint = ResourcesCompat.getDrawable(getResources(), R.drawable.img_end, null);
        imgMapCompass = findViewById(R.id.m_compass);
        imgPosCompass = findViewById(R.id.m_pos);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    private void setupMapBoundingBox() {
        GeoPoint centerPoint = new GeoPoint(30.53392, 114.36022);
        // 定义边界框的宽度和高度（单位为度）
        double boundingBoxWidth = 0.5;  // 度
        double boundingBoxHeight = 0.5; // 度
        // 计算边界框的四个角点的经纬度
        double north = centerPoint.getLatitude() + (boundingBoxHeight / 2);
        double south = centerPoint.getLatitude() - (boundingBoxHeight / 2);
        double east = centerPoint.getLongitude() + (boundingBoxWidth / 2);
        double west = centerPoint.getLongitude() - (boundingBoxWidth / 2);
        boundingBox = new BoundingBox(north, east, south, west);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setupMapView() {
        setupMapBoundingBox();

        mapView.setMultiTouchControls(true);
        mapView.setTileSource(new WMSTileSource(
                "OGC:WMS",
                new String[]{"http://202.114.122.22:2107/geoserver/whuB07/wms?service=WMS"},
                "whuB07:whuB07",
                "1.1.1",
                "EPSG:900913",
                "",
                256
        ));
        mapView.getController().setCenter(new GeoPoint(30.538, 114.3618));
        mapView.setMinZoomLevel(13.0);
        mapView.setMaxZoomLevel(22.0);
        mapView.getController().setZoom(15.0);
//        mapView.setTilesScaledToDpi(true);

        // Remove the existing overlay and initialize a new one
        mapView.getOverlays().clear();

        spLocationOverlay = new SimpleLocationOverlay(((BitmapDrawable) Objects.requireNonNull(mapView.getContext().getDrawable(R.drawable.img_null))).getBitmap());
        spLocationOverlay.setLocation(new GeoPoint(30.538, 114.3618));
        mapView.getOverlays().add(spLocationOverlay);

        scaleBarOverlay = new ScaleBarOverlay(mapView);
        scaleBarOverlay.setAlignBottom(true); //底部显示
        scaleBarOverlay.setScaleBarOffset(50, 50);
        scaleBarOverlay.setEnableAdjustLength(true);
        scaleBarOverlay.setLineWidth(4);
        scaleBarOverlay.setMaxLength(1.5F);
        mapView.getOverlays().add(scaleBarOverlay);

        rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);
    }

    private void setupPermissionRequest() {
        ActivityResultLauncher<String[]> locationPermissionRequest =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                            Boolean fineLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                            Boolean coarseLocationGranted = result.getOrDefault(android.Manifest.permission.ACCESS_COARSE_LOCATION,false);
                            if (fineLocationGranted != null && fineLocationGranted) {
                                // Precise location access granted.
                                Toast.makeText(this, "Precise location access granted.", Toast.LENGTH_LONG).show();
                            } else if (coarseLocationGranted != null && coarseLocationGranted) {
                                // Only approximate location access granted.
                                Toast.makeText(this, "Only approximate location access granted.", Toast.LENGTH_LONG).show();
                            } else {
                                // No location access granted.
                                Toast.makeText(this, "No location access granted.", Toast.LENGTH_LONG).show();
                            }
                        }
                );

        locationPermissionRequest.launch(new String[] {
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupMapViewListener() {
        mapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                imgMapCompass.setRotation(mapView.getMapOrientation());
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                imgMapCompass.setRotation(mapView.getMapOrientation());
                return true;
            }

        });

        mapView.getOverlays().add(new Overlay() {
            @SuppressLint("UseCompatLoadingForDrawables")
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                if (mapView != null && mapView.getOverlays() != null) {
                    queryState = false; // 取消文本查询
                    clearLayerResources(mapView, eventType.TAPMAP);
                    pointStateMachine((GeoPoint) mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY()), mapView);
                    return true;
                } else {
                    return super.onSingleTapConfirmed(e, mapView);
                }
            }
        });

        imgMapCompass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runRotationAnimation(mapView.getMapOrientation(),0.0f);
            }
        });
    }

    private void setupUserLocationListener() {
        mapView.getController().animateTo(new GeoPoint(30.538, 114.3618)); // 初始化：将地图中心移动到起始点
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this,"android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            // public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 0.5F, new LocationListener() {
            // 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
            @Override
            public void onLocationChanged(@NonNull Location location) {
                spLocationOverlay.setLocation(new GeoPoint(location.getLatitude(), location.getLongitude()));
            }
        });

        myLocationOverlay = new MyLocationNewOverlay(mapView);
        myLocationOverlay.setDrawAccuracyEnabled(true); // Enable drawing accuracy circle
        myLocationOverlay.setDirectionIcon(BitmapFactory.decodeResource(getResources(), R.drawable.img_pos_nav_blue));
        myLocationOverlay.setDirectionAnchor(0.5f, 0.5f); // Center of the image
        myLocationOverlay.setPersonIcon(BitmapFactory.decodeResource(getResources(), R.drawable.img_pos_still2p));
        myLocationOverlay.setPersonAnchor(0.5f, 0.5f); // Center of the image
        myLocationOverlay.enableMyLocation(); // Enable my location
        myLocationOverlay.enableFollowLocation(); // Enable follow location
        mapView.getOverlays().add(myLocationOverlay);

        // 创建一个线程池
        Executor executor = Executors.newSingleThreadExecutor();
        locationManager.addNmeaListener(executor, new OnNmeaMessageListener() {
            @Override
            public void onNmeaMessage(String message, long timestamp) {
                // 日志输出NMEA语句
                Log.i("log2", message);

                // 判断消息是否是RMC语句
                if (message.startsWith("$GPRMC")) {
                    System.out.println("RMC语句：" + message);
                }
                // 解码RMC语句
                GPRMCData data = GPRMCParser.parse(message);
                if (data != null) {
                    System.out.println("解析结果：" + data.toString());
                }
            }
        });

    }

    private void setupEditTextListener() {
        etStartPoint.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (textStartPoint.isEmpty()) {
                    textStartPoint = s.toString();
                }
            }
        });

        etEndPoint.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (textEndPoint.isEmpty()) {
                    textEndPoint = s.toString();
                }
            }
        });
    }

    private void setupButtonListener() {
        btnUserPos.setOnClickListener(new View.OnClickListener() {
            @SuppressLint({"DefaultLocale", "UseCompatLoadingForDrawables"})
            @Override
            public void onClick(View v) {
                if(myLocationOverlay == null) return;

                GeoPoint userPoint = myLocationOverlay.getMyLocation();

                if(userPoint == null) return;

                myLocationOverlay.disableFollowLocation();
                clearLayerResources(mapView, eventType.TAPMAP);
                pointStateMachine(userPoint, mapView);
                mapView.getController().zoomTo(18.0, 1000L); // 第三个参数表示动画持续时间（单位：毫秒）
                mapView.getController().animateTo(userPoint);
                runRotationAnimation(mapView.getMapOrientation(),0.0f);
                setVibration(30);
            }
        });

        btnUserPos.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(myLocationOverlay == null) return true;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setVibration(50);
                        mapView.getController().zoomTo(20.0, 1000L); // 第三个参数表示动画持续时间（单位：毫秒）
                        Toast.makeText(MainActivity.this, "Enable Location-Following Mode !", Toast.LENGTH_SHORT).show();
                    }
                });
                clearLayerResources(mapView, eventType.ONROUTE);
                myLocationOverlay.enableFollowLocation();

                return true;
            }
        });

        btnRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WhuRouteTask whuRouteTask = new WhuRouteTask(routeTask, layerName, new WhuRouteListner() {
                    @Override
                    public void onRoute(WhuRoute route) {
                        Polyline plRoute = route.getRouteGeometry();

                        clearLayerResources(mapView, eventType.ONROUTE);
                        if (plRoute != null) {
                            mapView.getOverlays().add(plRoute);
                            points = plRoute.getActualPoints();
                            if (!points.isEmpty()) {
                                renewMapMarker(points.get(0), pointType.START, mapView);
                                renewMapMarker(points.get(points.size() - 1), pointType.END, mapView);
                                mapView.invalidate();
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    myLocationOverlay.disableFollowLocation();
                                    // Calculate the bounding box of the polyline
                                    if(plRoute.getDistance() < 1E-5) { // path distance < 0.01mm -> null!
                                        return;
                                    }
                                    BoundingBox boundingBox = plRoute.getBounds();
                                    if (boundingBox != null) {
                                        // Zoom to the bounding box to ensure the entire route is visible
                                        mapView.zoomToBoundingBox(boundingBox, true, 160);
                                    }
                                }
                            });

                        } else {
                            runOnUiThread(new Runnable() { // 必须在主线程中显示Toast消息
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Server connection failed, please try again later.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });
                selectQueryState();
                checkPointsValidity(startPoint, endPoint, boundingBox);
                whuRouteTask.setStops(startPoint, endPoint);
                whuRouteTask.solveRouteAsync();
                runRotationAnimation(mapView.getMapOrientation(),0.0f);
                setVibration(30);
            }
        });

    }

    private void selectQueryState() {
        if (!textStartPoint.isEmpty()) {
            if(!etStartPoint.getText().toString().equals(textStartPoint)) {
                queryState = true;
            }
        } else if (!textEndPoint.isEmpty()) {
            if(!etEndPoint.getText().toString().equals(textEndPoint)) {
                queryState = true;
            }
        }

        if (queryState) {  // 从文本框查询：获取起始-终点点位坐标信息
            getPointsFromText();
        }
        textStartPoint = textEndPoint = "";
    }

    private void setPoints(double srcLat, double srcLon, double destLat, double destLon) {
        startPoint = new GeoPoint(srcLat,srcLon);
        endPoint   = new GeoPoint(destLat,destLon);
    }

    private void setPoints(GeoPoint point, pointType type) {
        if (type == pointType.START) {
            startPoint = point;
        } else {
            endPoint = point;
        }
    }

    private void getPointsFromText() {
        String[] srcGeo = etStartPoint.getText().toString().split(",");
        String[] destGeo = etEndPoint.getText().toString().split(",");

        // Check if srcGeo and destGeo contain exactly two elements each
        if (srcGeo.length != 2 || destGeo.length != 2) {
            // Handle the error, for example, show a toast message or log an error
            Toast.makeText(this, "Invalid input. Please enter latitude and longitude separated by a comma.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Attempt to parse the latitude and longitude values from strings
            double srcLat = Double.parseDouble(srcGeo[0]);
            double srcLon = Double.parseDouble(srcGeo[1]);
            double destLat = Double.parseDouble(destGeo[0]);
            double destLon = Double.parseDouble(destGeo[1]);

            // Set the points if parsing is successful
            setPoints(srcLat, srcLon, destLat, destLon);
        } catch (NumberFormatException e) {
            // Handle the case where parsing fails (e.g., non-numeric input)
            Toast.makeText(this, "Invalid input. Please enter valid numeric values for latitude and longitude", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkPointsValidity(GeoPoint startPoint, GeoPoint endPoint, BoundingBox boundingBox) {
        // 检查 boundingBox 是否为 null
        if (boundingBox == null) {
            Log.e("Error", "BoundingBox is null");
            return;
        }

        // 检查用户输入的起始点和终点坐标是否在地图可见范围内
        if (startPoint != null && !boundingBox.contains(startPoint)) {
            // startPoint 不在可见范围内，进行容错处理，例如将其设置为可见范围内的某个点
            GeoPoint newStartPoint = adjustPointToBounds(startPoint, boundingBox);
            this.startPoint = newStartPoint;
            renewMapMarker(newStartPoint, pointType.START, mapView);
            renewPosText(newStartPoint, pointType.START);
        }

        if (endPoint != null && !boundingBox.contains(endPoint)) {
            // endPoint 不在可见范围内，进行容错处理，例如将其设置为可见范围内的某个点
            GeoPoint newEndPoint = adjustPointToBounds(endPoint, boundingBox);
            this.endPoint = newEndPoint;
            renewMapMarker(newEndPoint, pointType.END, mapView);
            renewPosText(newEndPoint, pointType.END);
        }
    }

    @NonNull
    private GeoPoint adjustPointToBounds(@NonNull GeoPoint point, @NonNull BoundingBox boundingBox) {
        double latitude = point.getLatitude();
        double longitude = point.getLongitude();

        // 将点限制在地图可见范围内
        double adjustedLat = Math.max(boundingBox.getLatSouth(), Math.min(boundingBox.getLatNorth(), latitude));
        double adjustedLon = Math.max(boundingBox.getLonWest(), Math.min(boundingBox.getLonEast(), longitude));

        return new GeoPoint(adjustedLat, adjustedLon);
    }

    private void renewMapMarker(@NonNull GeoPoint point, pointType type, MapView mapView) {
        if (type == pointType.START) {
            Marker startMarker = new Marker(mapView);
            startMarker.setPosition(point);
            startMarker.setIcon(dbStartPoint);
            mapView.getOverlays().add(startMarker);
        } else {
            Marker endMarker = new Marker(mapView);
            endMarker.setPosition(point);
            endMarker.setIcon(dbEndPoint);
            mapView.getOverlays().add(endMarker);
        }
    }

    private void clearLayerResources(@NonNull final MapView mapView, eventType type) {

        if (mapView.getOverlays() == null) {
            Log.e("Error", "MapView or Overlays is null");
            return;
        }

        if (type == eventType.TAPMAP) { // When Tap Map
            // 清除先前绘制的路径
            mapView.getOverlays().removeIf(overlay -> overlay instanceof Polyline);
            // 移除先前的起点和终点标记
            if (clickState) {
                mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker);
                //renewMapMarker(endPoint, pointType.END, mapView);
            } else {
                mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker);
                renewMapMarker(startPoint, pointType.START, mapView);
            }

        } else { // When OnRoute
            mapView.getOverlays().removeIf(overlay -> overlay instanceof Polyline);
            mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker);
        }

    }

    @SuppressLint("DefaultLocale")
    private void renewPosText(@NonNull GeoPoint point, pointType type) {
        String[] str = point.toString().split(",");
        String pointText;
        if (str.length >= 2) {
            double latitude = Double.parseDouble(str[0]);
            double longitude = Double.parseDouble(str[1]);
            pointText = String.format("%.5f,%.5f", latitude, longitude);
        } else {
            pointText = "";
        }
        if (type == pointType.START) {
            etStartPoint.setText(pointText);
        } else {
            etEndPoint.setText(pointText);
        }
    }

    private void pointStateMachine(final GeoPoint point, final MapView mapView) {
        if(clickState) {
            setPoints(point, pointType.START);
            renewMapMarker(point, pointType.START, mapView);
            renewPosText(point, pointType.START);
            mapView.invalidate();
            clickState = false;
        } else {
            setPoints(point, pointType.END);
            renewMapMarker(point, pointType.END, mapView);
            renewPosText(point, pointType.END);
            mapView.invalidate();
            clickState = true; // 重置为第一次点击状态
        }
    }

    private void runRotationAnimation(float currentAngle, float targetAngle) {
        float clockwiseAngle = (targetAngle - currentAngle + 360) % 360;
        float counterClockwiseAngle = (currentAngle - targetAngle + 360) % 360;
        float targetRotation;

        if (clockwiseAngle <= counterClockwiseAngle) {
            targetRotation = (currentAngle + clockwiseAngle) % 360;
        } else {
            targetRotation = (currentAngle - counterClockwiseAngle) % 360;
        }

        long duration = 500;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 创建 mapView 的旋转动画
                ObjectAnimator mapViewAnimator = ObjectAnimator.ofFloat(mapView, "mapOrientation", currentAngle, targetRotation);
                mapViewAnimator.setDuration(duration);
                mapViewAnimator.setInterpolator(new AccelerateDecelerateInterpolator()); // 使用加速减速插值器

                // 创建 imgMapCompass 的旋转动画
                ObjectAnimator compassAnimator = ObjectAnimator.ofFloat(imgMapCompass, "rotation", currentAngle, targetRotation);
                compassAnimator.setDuration(duration);
                compassAnimator.setInterpolator(new DecelerateInterpolator()); // 使用加速减速插值器

                // 同时开始 mapView 和 imgMapCompass 的旋转动画
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(mapViewAnimator,compassAnimator); // 可以添加其他动画一同播放，例如 compassAnimator
                animatorSet.start();
            }
        });
    }

    private void setVibration(long period) {
        if (vibrator == null) return;

        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            vibrator.vibrate(period);
        } else {
            vibrator.cancel();
        }
    }

}
