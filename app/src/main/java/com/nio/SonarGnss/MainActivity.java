package com.nio.SonarGnss;

// All required Android, Google Maps, Bluetooth, UI, and IO imports
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.OnMapReadyCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import androidx.core.view.WindowInsetsControllerCompat;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;

/**
 * This is the MainActivity class and serves as the core screen and controller for the SonarGnss hydrographic surveying application.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // This is the loadingDialog field and references the loading alert dialog shown during Bluetooth connection
    private AlertDialog loadingDialog;
    // This is the tileOverlay field and holds the Google Maps offline tile overlay layer
    TileOverlay tileOverlay;
    // This is the menubut field and references the toolbar menu ImageButton for opening options
    ImageButton menubut;
    // This is the depthChart field and references the custom DepthChartView echogram component
    private DepthChartView depthChart;
    // This is the isprrecording field and tracks whether targeted profile survey recording is currently active
    Boolean isprrecording = false;
    // This is the d1 field and displays formatted date on dashboard
    TextView d1;
    // This is the d2 field and displays formatted latitude value on dashboard
    TextView d2;
    // This is the d3 field and displays formatted longitude value on dashboard
    TextView d3;
    // This is the d4 field and displays formatted time on dashboard
    TextView d4;
    // This is the d5 field and displays formatted speed value on dashboard
    TextView d5;
    // This is the d6 field and displays formatted course over ground (COG) on dashboard
    TextView d6;
    // This is the d7 field and displays formatted depth measurement on dashboard
    TextView d7;
    // This is the d21 field and displays latitude hemisphere (N/S) indicator
    TextView d21;
    // This is the d31 field and displays longitude hemisphere (E/W) indicator
    TextView d31;
    // This is the d8 field and displays altitude or depth indicator on dashboard
    TextView d8;
    // This is the mbtiles field and stores the local file path for bathymetry MBTiles database
    File mbtiles;
    // This is the IMPORT constant and defines request code for bathymetry file chooser intent
    static final int IMPORT = 100;
    // This is the mMap field and holds the active GoogleMap instance
    private GoogleMap mMap;
    // This is the mapFragment field and manages the Google Maps fragment lifecycle and view
    SupportMapFragment mapFragment;
    // This is the originalParams field and stores default layout parameters for map view container
    private ViewGroup.LayoutParams originalParams;
    // This is the loriginalParams field and stores default layout parameters for main vertical container
    private ViewGroup.LayoutParams loriginalParams;
    // This is the toggleFull field and references the fullscreen toggle ImageButton
    ImageButton toggleFull;
    // This is the bitmap field and holds the custom circular icon bitmap used for live map marker
    Bitmap bitmap;
    // This is the pathPoints field and stores list of GPS coordinates forming boat navigation trail
    List<LatLng> pathPoints = new ArrayList<>();
    // This is the marker field and represents the live position pin on the Google Map
    Marker marker;
    // This is the isFullscreen field and tracks whether the map view is expanded to fullscreen
    boolean isFullscreen = false;
    // This is the handler field and schedules recurring map update timer tasks on the main thread
    private final Handler handler = new Handler();
    // This is the count field and tracks sample count interval before adding trail points
    int count = 0;
    // This is the lat field and stores latest latitude string to display on map
    String lat = "";
    // This is the lon field and stores latest longitude string to display on map
    String lon = "";
    // This is the trailPolyline field and draws the continuous navigation trail polyline on the map
    private Polyline trailPolyline;
    // This is the liveOutputStream field and writes continuous raw incoming Bluetooth data to storage
    private OutputStream liveOutputStream;
    // This is the formatedOutputStream field and writes parsed survey data rows to storage
    private OutputStream formatedOutputStream;
    // This is the prOutputStream field and writes targeted profile survey parsed data rows to storage
    private OutputStream prOutputStream;
    // This is the prrawOutputStream field and writes targeted profile survey raw byte stream to storage
    private OutputStream prrawOutputStream;
    // This is the ext field and stores active file extension ("txt" for Echo Sounder, "ubx" for RTK)
    String ext = "";
    // This is the timeStamp field and stores session timestamp string for unique file naming
    String timeStamp;
    // This is the liveFileUri field and stores MediaStore content URI for raw continuous log file
    Uri liveFileUri;
    // This is the FormateFileUri field and stores MediaStore content URI for parsed continuous log file
    Uri FormateFileUri;
    // This is the RawPRFileUri field and stores MediaStore content URI for targeted profile survey raw file
    Uri RawPRFileUri;
    // This is the PRFileUri field and stores MediaStore content URI for targeted profile survey parsed file
    Uri PRFileUri;

    // This is the ifExec field and flags whether device configuration is executed from menu (1) or startup (0)
    int ifExec = 0;

    // This is the shOver field and tracks whether the MBTiles tile overlay is currently visible on the map
    boolean shOver = false;
    // This is the changeFilename field and flags whether to generate a new filename on connection
    boolean changeFilename = true;
    // This is the table field and references the on-screen TableLayout displaying real-time data rows
    TableLayout table;
    // This is the popupMenu field and manages the dropdown options menu
    PopupMenu popupMenu;
    // This is the btnConnect field and references the main Bluetooth connection and exit button
    Button btnConnect;

    // This is the PR field and references the targeted profile recording Start/Stop button
    Button PR;
    // This is the bluetoothAdapter field and provides device Bluetooth hardware management
    BluetoothAdapter bluetoothAdapter;
    // This is the bluetoothSocket field and maintains active RFCOMM communication socket with paired device
    BluetoothSocket bluetoothSocket;
    // This is the deviceAddress field and stores MAC address of selected Bluetooth hardware
    String deviceAddress = "";
    // This is the deviceName field and stores human-readable name of selected Bluetooth hardware
    String deviceName = "";
    // This is the deviceType field and stores selected hardware type ("Echo Sounder" or "RTK Device")
    String deviceType = "";
    // This is the uuid field and defines standard SPP UUID for Bluetooth RFCOMM serial communication
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // This is the txtNmea field and references TextView displaying raw incoming NMEA/UBX stream
    TextView txtNmea;
    // This is the stopWorker field and signals background communication threads to stop recording
    private volatile boolean stopWorker = false;

    // This is the MODE_MAP constant and identifies Map-only visual viewport mode
    public static final int MODE_MAP = 0;
    // This is the MODE_ECHOGRAM constant and identifies Echogram-only visual viewport mode
    public static final int MODE_ECHOGRAM = 1;
    // This is the MODE_SPLIT constant and identifies Split (Map + Echogram) visual viewport mode
    public static final int MODE_SPLIT = 2;
    // This is the currentVisualMode field and tracks currently selected visual viewport mode
    private int currentVisualMode = MODE_MAP;
    // These are tabMap, tabEchogram, and tabSplit fields and reference mode switcher tab buttons
    private TextView tabMap, tabEchogram, tabSplit;

    // This is the fileName field and references TextView displaying active profile recording filename
    TextView fileName;
    // This is the overlay field and references ImageButton toggle for bathymetry overlay
    ImageButton overlay;
    // This is the mapView field and references the ViewGroup container holding the Google Map
    private ViewGroup mapView;

    // This is the sline field and temporarily caches NMEA GNRMC position line before merging with depth line
    String sline;
    // This is the j field and tracks synchronization state between GNRMC position and ES depth sentences
    int j = 0;

    // This is the parser field and holds instance of UbxParser for binary GNSS packets
    UbxParser parser;

    // This is the pairedDevicesAlert field and holds alert dialog listing paired Bluetooth devices
    AlertDialog pairedDevicesAlert;
    // This is the devicetypeAlert field and holds alert dialog selecting device hardware type
    AlertDialog devicetypeAlert;


    // This is the Heads function and creates the header row for the data table view
    TableRow Heads() {
        // This line initializes a new TableRow object taking insights from the activity context
        TableRow header = new TableRow(this);
        // This line sets header layout parameters to match parent width taking insights from TableRow.LayoutParams
        header.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView h1 = new TextView(this);
        // This line formats the DATE header text taking insights from string formatting
        h1.setText(String.format("%-8s", "DATE"));
        h1.setTextSize(20);
        header.addView(h1);

        TextView h2 = new TextView(this);
        // This line formats the TIME header text taking insights from string formatting
        h2.setText(String.format("%-8s", "TIME"));
        h2.setTextSize(20);
        header.addView(h2);

        TextView h3 = new TextView(this);
        // This line formats the LAT header text taking insights from string formatting
        h3.setText(String.format("%-13s", "LAT"));
        h3.setTextSize(20);
        header.addView(h3);

        TextView h4 = new TextView(this);
        // This line formats the LON header text taking insights from string formatting
        h4.setText(String.format("%-13s", "LON"));
        h4.setTextSize(20);
        header.addView(h4);

        TextView h5 = new TextView(this);
        // This line formats the SPEED header text taking insights from string formatting
        h5.setText(String.format("%-8s", "SPEED"));
        h5.setTextSize(20);
        header.addView(h5);

        TextView h6 = new TextView(this);
        // This line formats the DEPTH header text taking insights from string formatting
        h6.setText(String.format("%-8s", "DEPTH"));
        h6.setTextSize(20);
        header.addView(h6);

        TextView h7 = new TextView(this);
        // This line formats the ALTITUDE header text taking insights from string formatting
        h7.setText(String.format("%-8s", "ALTITUDE"));
        h7.setTextSize(20);
        header.addView(h7);

        // This line returns the fully constructed header TableRow taking insights from added child TextViews
        return header;
    }

    // This is the AddRow function and constructs a table row from raw NMEA GNRMC position and ES depth sentences
    TableRow AddRow(String sline, String line) {
        // This line splits the position sentence into tokens taking insights from comma delimiter
        String[] mp = sline.split(",");
        // This line splits the depth sentence into tokens taking insights from comma delimiter
        String[] np = line.split(",");

        // This line updates dashboard metrics widgets taking insights from updateDash function
        updateDash(mp, np);

        // This line creates a new TableRow widget taking insights from context
        TableRow row = new TableRow(this);
        // This line sets row layout parameters taking insights from TableRow.LayoutParams
        row.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

        TextView r1 = new TextView(this);
        // This line sets formatted date taking insights from FDate function and index 9
        r1.setText(String.format("%-8s", FDate(mp[9])));
        r1.setTextSize(18);
        row.addView(r1);

        TextView r2 = new TextView(this);
        // This line sets formatted time taking insights from FTime function and index 1
        r2.setText(String.format("%-8s", FTime(mp[1])));
        r2.setTextSize(18);
        row.addView(r2);

        TextView r3 = new TextView(this);
        // This line sets formatted latitude with hemisphere taking insights from FLat function and index 3 and 4
        r3.setText(String.format("%-13s", FLat(mp[3]) + " " + mp[4]));
        r3.setTextSize(18);
        row.addView(r3);

        TextView r4 = new TextView(this);
        // This line sets formatted longitude with hemisphere taking insights from FLon function and index 5 and 6
        r4.setText(String.format("%-13s", FLon(mp[5]) + " " + mp[6]));
        r4.setTextSize(18);
        row.addView(r4);

        TextView r5 = new TextView(this);
        // This line sets formatted speed taking insights from FSpe function and index 7
        r5.setText(String.format("%-8s", FSpe(mp[7])));
        r5.setTextSize(18);
        row.addView(r5);

        TextView r6 = new TextView(this);
        // This line sets formatted depth taking insights from FDep function and index 1
        r6.setText(String.format("%-8s", FDep(np[1])));
        r6.setTextSize(18);
        row.addView(r6);

        TextView r7 = new TextView(this);
        // This line sets placeholder altitude taking insights from fixed dashes
        r7.setText(String.format("%-8s", "----"));
        r7.setTextSize(18);
        row.addView(r7);

        // This line returns the newly populated TableRow taking insights from populated TextView columns
        return row;
    }

    // This is the FDate function and reformats NMEA date string DDMMYY into DD/MM/20YY format
    String FDate(String Date) {
        // This line checks if input date string has required 6 characters taking insights from Date length
        if (Date.length() >= 6) {
            // This line returns date formatted with slashes taking insights from Date substrings
            return Date.substring(0, 2) + "/" + Date.substring(2, 4) + "/20" + Date.substring(4, 6);
        }
        // This line returns raw date when length is insufficient taking insights from Date
        return Date;
    }

    // This is the FTime function and reformats NMEA time string HHMMSS into HH:MM:SS format
    String FTime(String Time) {
        // This line checks if input time string has required 6 characters taking insights from Time length
        if (Time.length() >= 6) {
            // This line returns time formatted with colons taking insights from Time substrings
            return Time.substring(0, 2) + ":" + Time.substring(2, 4) + ":" + Time.substring(4, 6);
        }
        // This line returns raw time when length is insufficient taking insights from Time
        return Time;
    }

    // This is the FLat function and extracts the leading 8 characters of an NMEA latitude string
    String FLat(String Lat) {
        // This line extracts first 8 characters of latitude string taking insights from Lat length
        if (Lat.length() >= 8) {
            return Lat.substring(0, 8);
        }
        // This line checks if latitude is non-empty taking insights from Lat length
        if (Lat.length() > 0) {
            return Lat;
        }
        // This line returns fallback placeholder when latitude is empty taking insights from dashes
        return "---";
    }

    // This is the FLon function and extracts the leading 8 characters of an NMEA longitude string
    String FLon(String Lon) {
        // This line extracts first 8 characters of longitude string taking insights from Lon length
        if (Lon.length() >= 8) {
            return Lon.substring(0, 8);
        }
        // This line checks if longitude is non-empty taking insights from Lon length
        if (Lon.length() > 0) {
            return Lon;
        }
        // This line returns fallback placeholder when longitude is empty taking insights from dashes
        return "---";
    }

    // This is the FSpe function and extracts the leading 5 characters of an NMEA speed string
    String FSpe(String Spe) {
        // This line extracts first 5 characters of speed taking insights from Spe length
        if (Spe.length() >= 5) {
            return Spe.substring(0, 5);
        }
        // This line returns fallback placeholder when speed is empty taking insights from dashes
        return "---";
    }

    // This is the FDep function and converts millimeter depth string into meters
    String FDep(String Dep) {
        // This line parses millimeters and divides by 1000 to get meters taking insights from Dep
        if (Dep.length() >= 2) {
            return String.valueOf(Float.parseFloat(Dep) / 1000);
        }
        // This line returns fallback placeholder when depth is empty taking insights from dashes
        return "---";
    }

    // This is the FAlt function and returns formatted altitude string
    String FAlt(String Alt) {
        // This line returns altitude string taking insights from Alt
        return Alt;
    }

    // This is the FAltU function and returns formatted altitude units string
    String FAltU(String Altu) {
        // This line returns altitude units taking insights from Altu
        return Altu;
    }

    // This is the FCog function and extracts leading 6 characters of Course Over Ground
    String FCog(String Cog) {
        // This line extracts first 6 characters of course over ground taking insights from Cog length
        if (Cog.length() >= 6) {
            return Cog.substring(0, 6);
        }
        // This line returns fallback placeholder when COG is empty taking insights from dashes
        return "---";
    }

    // This is the rtkempty function and returns data string or fallback dashes if empty
    String rtkempty(String data) {
        // This line checks if data is empty and returns dashes taking insights from data.isEmpty()
        if (data.isEmpty()) {
            return "---";
        }
        // This line returns data when valid taking insights from data
        return data;
    }

    // This is the rtkline function and constructs a fixed-width formatted survey log line from RTK array tokens
    String rtkline(String[] data) {
        String latch = "";
        String nsch = "";
        String lonch = "";
        String ewch = "";
        String cog = "";

        // This line truncates latitude to 8 characters taking insights from data array index 2
        if (data[2].length() >= 8) {
            latch = data[2].substring(0, 8);
        } else {
            latch = data[2];
        }

        // This line extracts north/south indicator taking insights from data array index 3
        if (data[3].length() >= 3) {
            nsch = data[3].substring(2);
        }

        // This line truncates longitude to 8 characters taking insights from data array index 4
        if (data[4].length() >= 8) {
            lonch = data[4].substring(0, 8);
        } else {
            lonch = data[4];
        }

        // This line extracts east/west indicator taking insights from data array index 5
        if (data[5].length() >= 3) {
            ewch = data[5].substring(2);
        }

        // This line truncates course over ground taking insights from data array index 7
        if (data[7].length() >= 6) {
            cog = data[7].substring(0, 6);
        }

        // This line formats the complete RTK line taking insights from formatted variables and data array
        String f = String.format("%-12s", data[0] + ", ")
                + String.format("%-10s", data[1] + ", ")
                + String.format("%-15s", latch + " " + nsch + ", ")
                + String.format("%-15s", lonch + " " + ewch + ", ")
                + String.format("%-10s", ", ")
                + String.format("%-12s", data[6] + ", ")
                + String.format("%-8s", cog + ", ")
                + String.format("%-5s", data[8] + ", ");
        return f;
    }

    @Override
    // This is the onCreate function and initializes the main activity layout, window insets, parser callback, map fragment, and startup delay
    protected void onCreate(Bundle savedInstanceState) {
        // This line calls parent AppCompatActivity onCreate taking insights from savedInstanceState
        super.onCreate(savedInstanceState);
        // This line sets activity user interface layout taking insights from activity_main layout resource
        setContentView(R.layout.activity_main);
        // This line creates window insets controller taking insights from window and decor view
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());

        // This line hides navigation bar bottom buttons taking insights from navigationBars type
        controller.hide(WindowInsetsCompat.Type.navigationBars());

        // This line allows transient swipe gesture to temporarily show bars taking insights from transient bars behavior
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        );

        // This line configures system bar padding listener taking insights from main root view
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // This line calculates safe system bar insets taking insights from systemBars type
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // This line applies padding to view avoiding system bars taking insights from systemBars dimensions
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // This line attaches click listener to raw data cancel button taking insights from rawDatahide function
        findViewById(R.id.can).setOnClickListener((V) -> rawDatahide());

        // This line instantiates UBX binary parser with callback listener taking insights from UbxParser
        parser = new UbxParser(new UbxParser.OnTimeReceivedListener() {
            @Override
            // This is the onTimeReceived callback function and handles parsed GNSS position, time, and altitude data
            public void onTimeReceived(String date,
                                       String time,
                                       String latitude,
                                       String latd,
                                       String longitude,
                                       String lond,
                                       String speed,
                                       String cog,
                                       String altitude) {
                // This line packs received parameters into an array taking insights from incoming callback arguments
                String[] rtk = {date,
                        time,
                        latitude,
                        latd,
                        longitude,
                        lond,
                        speed,
                        cog,
                        altitude};

                // This line saves formatted RTK data string to continuous log file taking insights from rtkline function
                saveFormatedData1(rtkline(rtk));
                // This line checks if profile recording is active taking insights from isprrecording flag
                if (isprrecording) {
                    // This line saves formatted RTK data string to profile recording file taking insights from rtkline function
                    savePRData1(rtkline(rtk));
                }
                // This line updates dashboard UI on the Android main UI thread taking insights from parsed RTK data
                runOnUiThread(() -> {
                    // This line updates map position coordinates taking insights from latitude and longitude
                    updatePoints(latitude, longitude);
                    // This line sets date textview taking insights from rtkempty and date
                    d1.setText(rtkempty(date));
                    // This line checks if latitude string is not empty taking insights from latitude
                    if (!latitude.isEmpty()) {
                        // This line formats latitude display to 8 characters taking insights from latitude string length
                        if (latitude.length() >= 8) {
                            d2.setText(latitude.substring(0, 8));
                        } else {
                            d2.setText(latitude);
                        }
                        // This line sets latitude direction indicator taking insights from latd
                        d21.setText(rtkempty(latd));
                    }
                    // This line checks if longitude string is not empty taking insights from longitude
                    if (!longitude.isEmpty()) {
                        // This line formats longitude display to 8 characters taking insights from longitude string length
                        if (longitude.length() >= 8) {
                            d3.setText(longitude.substring(0, 8));
                        } else {
                            d3.setText(longitude);
                        }
                        // This line sets longitude direction indicator taking insights from lond
                        d31.setText(rtkempty(lond));
                    }
                    // This line sets time textview taking insights from rtkempty and time
                    d4.setText(rtkempty(time));
                    // This line checks if speed string is not empty taking insights from speed
                    if (!speed.isEmpty()) {
                        // This line formats speed display to two decimal places taking insights from parsed float speed
                        d5.setText(String.format("%.2f", Float.parseFloat(speed)));
                    }
                    // This line checks if course over ground is not empty taking insights from cog
                    if (!cog.isEmpty()) {
                        // This line formats course display to two decimal places taking insights from parsed float cog
                        d6.setText(String.format("%.2f", Float.parseFloat(rtkempty(cog))));
                    }
                    // This line checks if altitude string is not empty taking insights from altitude
                    if (!altitude.isEmpty()) {
                        // This line formats altitude display to two decimal places taking insights from parsed float altitude
                        d8.setText(String.format("%.2f", Float.parseFloat(altitude)));
                        // This line appends altitude reading to depth chart echogram taking insights from parsed float altitude
                        depthChart.addDepth(Float.parseFloat(altitude));
                    }
                });
            }
        });

        // This line binds fileName TextView widget taking insights from layout ID fname
        fileName = findViewById(R.id.fname);

        // This line binds dashboard date TextView widget taking insights from layout ID t1
        d1 = findViewById(R.id.t1);
        // This line binds dashboard latitude TextView widget taking insights from layout ID t2
        d2 = findViewById(R.id.t2);
        // This line binds dashboard longitude TextView widget taking insights from layout ID t3
        d3 = findViewById(R.id.t3);
        // This line binds dashboard time TextView widget taking insights from layout ID t4
        d4 = findViewById(R.id.t4);
        // This line binds dashboard speed TextView widget taking insights from layout ID t5
        d5 = findViewById(R.id.t5);
        // This line binds dashboard COG TextView widget taking insights from layout ID t6
        d6 = findViewById(R.id.t6);
        // This line binds dashboard depth TextView widget taking insights from layout ID t7
        d7 = findViewById(R.id.t7);
        // This line binds dashboard latitude direction indicator TextView widget taking insights from layout ID t21
        d21 = findViewById(R.id.t21);
        // This line binds dashboard longitude direction indicator TextView widget taking insights from layout ID t31
        d31 = findViewById(R.id.t31);
        // This line binds dashboard altitude/extra TextView widget taking insights from layout ID t8
        d8 = findViewById(R.id.t8);
        // This line binds custom depth chart view widget taking insights from layout ID depthChart
        depthChart = findViewById(R.id.depthChart);

        // This line checks if support action bar is present taking insights from getSupportActionBar()
        if (getSupportActionBar() != null) {
            // This line hides support action bar for clean full-screen look taking insights from getSupportActionBar()
            getSupportActionBar().hide();
        }

        // This line binds table layout widget taking insights from layout ID table
        table = findViewById(R.id.table);
        // This line adds table header row to table layout taking insights from Heads() function
        table.addView(Heads(), new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        // This line creates custom live marker bitmap icon taking insights from liveMarker function
        liveMarker();
        // This line locates Google Maps SupportMapFragment taking insights from fragment ID map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // This line checks if map fragment was successfully retrieved taking insights from mapFragment
        if (mapFragment != null) {
            // This line registers asynchronous map ready callback taking insights from this activity
            mapFragment.getMapAsync(this);
        }

        // This line binds raw NMEA/UBX stream display TextView taking insights from layout ID txtNmea
        txtNmea = findViewById(R.id.txtNmea);

        // This line binds Bluetooth connect button taking insights from layout ID btnConnect
        btnConnect = findViewById(R.id.btnConnect);
        // This line attaches click listener to connect button taking insights from checkButton function
        btnConnect.setOnClickListener(v -> checkButton());

        // This line binds profile recording button taking insights from layout ID pR
        PR = findViewById(R.id.pR);
        // This line attaches click listener to profile recording button taking insights from handlePR function
        PR.setOnClickListener(v -> {
            handlePR();
        });
        // This line sets default light green background color for profile recording button taking insights from hex color #90EE90
        ((Button) findViewById(R.id.pR))
                .setBackgroundColor(Color.parseColor("#90EE90"));

        // This line retrieves default device Bluetooth adapter taking insights from BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // This line schedules Bluetooth permission request with 5.5 second delay taking insights from requestPermissions function
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::requestPermissions, 5500);

        // This line binds menu ImageButton taking insights from layout ID mbut
        menubut = findViewById(R.id.mbut);
        // This line clears default tint from menu button taking insights from null
        menubut.setBackgroundTintList(null);

        // This line creates popup menu anchored to menu button taking insights from menubut
        popupMenu = new PopupMenu(MainActivity.this, menubut);
        // This line inflates options menu XML layout into popup menu taking insights from options menu resource
        popupMenu.getMenuInflater().inflate(
                R.menu.options,
                popupMenu.getMenu()
        );
        // This line sets initial uncheck state for raw data menu item taking insights from menu_raw_data ID
        popupMenu.getMenu().findItem(R.id.menu_raw_data).setChecked(false);
        // This line sets initial uncheck state for parsed data menu item taking insights from menu_parsed_data ID
        popupMenu.getMenu().findItem(R.id.menu_parsed_data).setChecked(false);
        // This line sets initial checked state for map menu item taking insights from menu_map ID
        popupMenu.getMenu().findItem(R.id.menu_map).setChecked(true);
        // This line sets initial checked state for depth chart menu item taking insights from menu_depth_chart ID
        popupMenu.getMenu().findItem(R.id.menu_depth_chart).setChecked(true);
        // This line updates view visibilities based on initial menu checked states taking insights from handleChange function
        handleChange();
        // This line initializes visual mode switcher tabs taking insights from initVisualModeSelector function
        initVisualModeSelector();
        // This line attaches click listener to open popup menu taking insights from menubut
        menubut.setOnClickListener(new View.OnClickListener() {
            @Override
            // This is the onClick callback function and displays the options popup menu
            public void onClick(View view) {
                // This line shows popup menu on screen taking insights from popupMenu
                popupMenu.show();

                // This line sets menu item selection listener taking insights from popupMenu
                popupMenu.setOnMenuItemClickListener(
                        new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            // This is the onMenuItemClick callback function and handles user options menu choices
                            public boolean onMenuItemClick(MenuItem item) {
                                // This line checks if user clicked Change Prefix taking insights from item title
                                if (item.getTitle().equals("Change Prefix")) {
                                    // This line opens prefix change dialog taking insights from alertChangePrefix function
                                    alertChangePrefix();
                                    return true;
                                }

                                // This line checks if user clicked Upload Bathymetry taking insights from item title
                                if (item.getTitle().equals("Upload Bathymetry")) {
                                    // This line launches file chooser for MBTiles taking insights from uploadBath function
                                    uploadBath();
                                    return true;
                                }

                                // This line checks if user clicked Raw Data taking insights from item title
                                if (item.getTitle().equals("Raw Data")) {
                                    // This line switches view to fullscreen raw data log taking insights from rawDataShow function
                                    rawDataShow();
                                    return true;
                                }

                                // This line checks if user clicked Configure Connection taking insights from item title
                                if (item.getTitle().equals("Configure Connection")) {
                                    // This line marks configuration execution flag taking insights from value 1
                                    ifExec = 1;
                                    // This line opens paired devices selection dialog taking insights from showPairedDevicesDialog function
                                    showPairedDevicesDialog();
                                    return true;
                                }

                                // This line toggles checkbox state for tapped menu item taking insights from current isChecked state
                                item.setChecked(!item.isChecked());

                                // This line synchronizes UI panels with updated checkbox states taking insights from handleChange function
                                handleChange();
                                // This line creates confirmation feedback text taking insights from item title and checked status
                                String text = item.getTitle()
                                        + (item.isChecked()
                                        ? " Selected"
                                        : " Unselected");

                                // This line displays brief toast message confirming menu choice taking insights from text
                                Toast.makeText(
                                        MainActivity.this,
                                        text,
                                        Toast.LENGTH_SHORT
                                ).show();

                                return true;
                            }
                        });

                // This line displays popup menu taking insights from popupMenu
                popupMenu.show();
            }
        });

        // This line registers back navigation dispatcher callback taking insights from OnBackPressedCallback
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    // This is the handleOnBackPressed callback function and safely intercepts back button presses
                    public void handleOnBackPressed() {
                        // This line checks if map is currently in fullscreen mode taking insights from isFullscreen flag
                        if (isFullscreen) {
                            // This line exits fullscreen map mode returning to dashboard taking insights from mapfullScreen function
                            mapfullScreen();
                            return;
                        }

                        // This line checks if recording session is actively running taking insights from stopWorker and btnConnect text
                        if (!stopWorker &&
                                btnConnect.getText().equals("EXIT")) {
                            // This line creates confirmation dialog preventing accidental exit taking insights from AlertDialog.Builder
                            AlertDialog dia = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Exit Recording")
                                    .setMessage("Recording is in progress.\nAre you sure you want to go back?")
                                    .setPositiveButton("Yes", (dialog, which) -> {
                                        // This line stops active survey recording taking insights from stopRecording function
                                        stopRecording();

                                        try {
                                            // This line closes live stream if open taking insights from liveOutputStream
                                            if (liveOutputStream != null) {
                                                liveOutputStream.close();
                                            }
                                            // This line closes formatted stream if open taking insights from formatedOutputStream
                                            if (formatedOutputStream != null) {
                                                formatedOutputStream.close();
                                            }
                                            // This line closes profile stream if open taking insights from prOutputStream
                                            if (prOutputStream != null) {
                                                prOutputStream.close();
                                            }
                                            // This line closes raw profile stream if open taking insights from prrawOutputStream
                                            if (prrawOutputStream != null) {
                                                prrawOutputStream.close();
                                            }
                                        } catch (Exception e) {
                                            // This line catches any stream closing exceptions taking insights from try block
                                        }

                                        // This line closes the activity and exits taking insights from finish()
                                        finish();
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                            // This line sets green text color for positive confirmation button taking insights from green color resource
                            dia.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setTextColor(ContextCompat.getColor(MainActivity.this, R.color.green));
                            // This line sets red text color for negative cancel button taking insights from red color resource
                            dia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(MainActivity.this, R.color.red));

                            return;
                        }

                        // This line closes activity when not recording taking insights from finish()
                        finish();
                    }
                });

        // This line references local bathymetry MBTiles file in external files directory taking insights from bathymetry.mbtiles
        mbtiles =
                new File(
                        getExternalFilesDir(null),
                        "bathymetry.mbtiles"
                );

        // This line checks if local MBTiles database file already exists on device taking insights from mbtiles.exists()
        if (mbtiles.exists()) {
            // This line displays toast confirming MBTiles file was found taking insights from mbtiles absolute path
            Toast.makeText(
                    this,
                    "MBTiles found:\n" + mbtiles.getAbsolutePath(),
                    Toast.LENGTH_LONG
            ).show();
        } else {
            // This line displays toast notification that MBTiles file is absent taking insights from mbtiles
            Toast.makeText(
                    this,
                    "No MBTiles file found",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // This is the t counter field and counts updates to throttle map marker refresh frequency
    int t = 0;
    // This is the updatePoints function and updates current GPS position markers on the map
    private void updatePoints(String latitude, String longitude) {
        // This line throttles map updates to every 4th sample taking insights from counter t
        if (t == 4) {
            // This line checks that latitude and longitude are valid non-empty strings taking insights from latitude and longitude
            if (!latitude.isEmpty() && !longitude.isEmpty()) {
                // This line stores current latitude taking insights from latitude parameter
                lat = latitude;
                // This line stores current longitude taking insights from longitude parameter
                lon = longitude;
            }
            // This line resets counter t taking insights from zero value
            t = 0;
        }
        // This line increments counter t taking insights from current update cycle
        t++;
    }

    // This is the uploadBath function and launches system file picker to select an MBTiles bathymetry file
    private void uploadBath() {
        // This line creates file open intent taking insights from Intent.ACTION_OPEN_DOCUMENT
        Intent i =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        // This line sets MIME type filter to accept any file format taking insights from wildcard "*/*"
        i.setType("*/*");

        // This line requires files to be openable streams taking insights from Intent.CATEGORY_OPENABLE
        i.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        // This line launches document picker activity waiting for result taking insights from i and IMPORT request code
        startActivityForResult(
                i,
                IMPORT
        );
    }

    // This is the loadTiles function and connects to the SQLite MBTiles database and adds tile overlay to Google Maps
    void loadTiles() {
        // This line checks if local MBTiles database file exists taking insights from mbtiles.exists()
        if (!mbtiles.exists()) {
            // This line dims overlay button indicating no map tiles are loaded taking insights from shoverlay function
            shoverlay(overlay);
            return;
        }

        // This line opens SQLite database in read-only mode taking insights from mbtiles path
        SQLiteDatabase db =
                SQLiteDatabase.openDatabase(
                        mbtiles.getPath(),
                        null,
                        SQLiteDatabase.OPEN_READONLY
                );

        // This line queries database schema tables taking insights from sqlite_master query
        Cursor c = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null
        );

        // This line iterates over table query results taking insights from cursor c
        while (c.moveToNext()) {
            // This line logs discovered table name taking insights from column index 0
            Log.d(
                    "MBTILES",
                    "Table: " + c.getString(0)
            );
        }

        // This line closes table schema cursor taking insights from c
        c.close();

        // This line counts total map tiles inside tiles table taking insights from COUNT query
        Cursor t = db.rawQuery(
                "SELECT COUNT(*) FROM tiles",
                null
        );

        // This line checks if count query succeeded taking insights from cursor t
        if (t.moveToFirst()) {
            // This line logs total tile count taking insights from column index 0
            Log.d(
                    "MBTILES",
                    "Tile count: " + t.getInt(0)
            );
        }

        // This line closes tile count cursor taking insights from t
        t.close();

        // This line creates and adds custom tile overlay layer onto Google Maps taking insights from MBTilesProvider
        tileOverlay = mMap.addTileOverlay(
                new TileOverlayOptions()
                        .tileProvider(new MBTilesProvider(db))
        );

        // This line sets overlay visible state flag to true taking insights from successful tile overlay creation
        shOver = true;
        // This line updates overlay toggle button icon style taking insights from shoverlay function
        shoverlay(overlay);
    }

    // This is the onActivityResult function and receives selected MBTiles file from document picker intent
    @Override
    protected void onActivityResult(
            int request,
            int result,
            Intent data) {

        // This line calls parent activity onActivityResult taking insights from request, result, and data
        super.onActivityResult(
                request, result, data
        );

        // This line checks if result matches our MBTiles import request code and succeeded taking insights from request and result
        if (request == IMPORT &&
                result == RESULT_OK) {

            // This line copies selected file from content URI into local app storage taking insights from data.getData()
            copy(data.getData());

            // This line loads copied tiles onto Google Maps taking insights from loadTiles function
            loadTiles();
        }
    }

    // This is the copy function and streams binary bytes from picked content URI into local bathymetry.mbtiles file
    void copy(Uri uri) {
        try {
            // This line opens input stream from chosen document URI taking insights from content resolver
            InputStream in =
                    getContentResolver()
                            .openInputStream(uri);

            // This line opens output stream writing to destination mbtiles file taking insights from mbtiles
            FileOutputStream out =
                    new FileOutputStream(
                            mbtiles
                    );

            // This line creates 8KB temporary transfer byte buffer taking insights from size 8192
            byte[] buffer =
                    new byte[8192];

            // This line stores number of bytes read in each chunk taking insights from in.read
            int len;

            // This line loops reading chunks until stream end taking insights from buffer and len
            while (
                    (len = in.read(buffer)) > 0
            ) {
                // This line writes chunk to destination file taking insights from buffer and len
                out.write(
                        buffer,
                        0,
                        len
                );
            }

            // This line closes input stream taking insights from in
            in.close();

            // This line closes output file stream taking insights from out
            out.close();

            // This line displays toast confirming MBTiles file was saved successfully taking insights from mbtiles path
            Toast.makeText(
                    this,
                    "MBTiles saved:\n" + mbtiles.getAbsolutePath(),
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {
            // This line prints exception details to logcat taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the rawDataShow function and expands raw NMEA/UBX stream viewer to fullscreen hiding other panels
    private void rawDataShow() {
        ViewGroup.LayoutParams params;
        // This line creates matching parent layout parameters taking insights from MATCH_PARENT
        params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        // This line hides connect button taking insights from View.GONE
        btnConnect.setVisibility(View.GONE);
        // This line hides title bar taking insights from View.GONE
        findViewById(R.id.titi).setVisibility(View.GONE);
        // This line hides depth chart view taking insights from View.GONE
        findViewById(R.id.depthChart).setVisibility(View.GONE);
        // This line hides horizontal parsed data scrollview taking insights from View.GONE
        findViewById(R.id.horizontalScrollView).setVisibility(View.GONE);
        // This line hides metrics dashboard taking insights from View.GONE
        findViewById(R.id.dashboard).setVisibility(View.GONE);
        // This line hides map fragment view taking insights from View.GONE
        findViewById(R.id.map).setVisibility(View.GONE);
        // This line sets raw data container to full screen dimensions taking insights from params
        findViewById(R.id.ll2).setLayoutParams(params);
        // This line makes raw data scroll view visible taking insights from View.VISIBLE
        findViewById(R.id.scv).setVisibility(View.VISIBLE);
    }

    // This is the rawDatahide function and exits raw data fullscreen view restoring dashboard panels
    private void rawDatahide() {
        // This line restores connect button visibility taking insights from View.VISIBLE
        btnConnect.setVisibility(View.VISIBLE);
        // This line restores title bar visibility taking insights from View.VISIBLE
        findViewById(R.id.titi).setVisibility(View.VISIBLE);
        // This line restores depth chart visibility taking insights from View.VISIBLE
        findViewById(R.id.depthChart).setVisibility(View.VISIBLE);
        // This line restores horizontal scroll view visibility taking insights from View.VISIBLE
        findViewById(R.id.horizontalScrollView).setVisibility(View.VISIBLE);
        // This line restores dashboard metrics visibility taking insights from View.VISIBLE
        findViewById(R.id.dashboard).setVisibility(View.VISIBLE);
        // This line restores map fragment visibility taking insights from View.VISIBLE
        findViewById(R.id.map).setVisibility(View.VISIBLE);
        // This line restores original layout dimensions taking insights from loriginalParams
        findViewById(R.id.ll2).setLayoutParams(loriginalParams);
        // This line hides raw data scrollview taking insights from View.GONE
        findViewById(R.id.scv).setVisibility(View.GONE);
        // This line applies user menu visibility selections taking insights from handleChange function
        handleChange();
    }

    // This is the handleChange function and adjusts visibility of map, table, echogram, and raw logs based on menu options
    private void handleChange() {
        // This line checks if map menu checkbox is enabled taking insights from menu_map item
        boolean tmap = popupMenu.getMenu().findItem(R.id.menu_map).isChecked();
        // This line checks if raw data menu checkbox is enabled taking insights from menu_raw_data item
        boolean traw = popupMenu.getMenu().findItem(R.id.menu_raw_data).isChecked();
        // This line checks if parsed table menu checkbox is enabled taking insights from menu_parsed_data item
        boolean tpharsed = popupMenu.getMenu().findItem(R.id.menu_parsed_data).isChecked();
        // This line checks if depth chart echogram menu checkbox is enabled taking insights from menu_depth_chart item
        boolean tdchart = popupMenu.getMenu().findItem(R.id.menu_depth_chart).isChecked();

        // This line hides layout gap spacer view if present taking insights from gap view ID
        if (findViewById(R.id.gap) != null) {
            findViewById(R.id.gap).setVisibility(View.GONE);
        }

        // This line toggles map visibility based on menu check state taking insights from tmap flag
        if (tmap) {
            if (findViewById(R.id.map) != null) {
                // This line shows map view container taking insights from View.VISIBLE
                findViewById(R.id.map).setVisibility(View.VISIBLE);
            }
            if (mapFragment != null) {
                // This line shows map fragment taking insights from fragment manager transaction
                getSupportFragmentManager()
                        .beginTransaction()
                        .show(mapFragment)
                        .commitAllowingStateLoss();
            }
        } else {
            if (findViewById(R.id.map) != null) {
                // This line hides map view container taking insights from View.GONE
                findViewById(R.id.map).setVisibility(View.GONE);
            }
            if (mapFragment != null) {
                // This line hides map fragment taking insights from fragment manager transaction
                getSupportFragmentManager()
                        .beginTransaction()
                        .hide(mapFragment)
                        .commitAllowingStateLoss();
            }
        }

        // This line toggles raw data scrollview visibility taking insights from traw flag
        if (traw) {
            findViewById(R.id.scv).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.scv).setVisibility(View.GONE);
        }

        // This line toggles parsed data table horizontal scrollview visibility taking insights from tpharsed flag
        if (tpharsed) {
            findViewById(R.id.horizontalScrollView).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.horizontalScrollView).setVisibility(View.GONE);
        }

        // This line toggles depth chart echogram visibility taking insights from tdchart flag
        if (tdchart) {
            findViewById(R.id.depthChart).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.depthChart).setVisibility(View.GONE);
        }

        // This line synchronizes tab bar highlight styling taking insights from tmap and tdchart flags
        updateTabSelectionUI(tmap, tdchart);
    }

    // This is the updateTabSelectionUI function and updates tab button background colors and text highlights
    private void updateTabSelectionUI(boolean tmap, boolean tdchart) {
        // This line ensures tab views are initialized before styling taking insights from tabMap, tabEchogram, and tabSplit
        if (tabMap == null || tabEchogram == null || tabSplit == null) return;
        // This line clears map tab background taking insights from null
        tabMap.setBackground(null);
        // This line sets default muted slate text color for map tab taking insights from hex color #94A3B8
        tabMap.setTextColor(Color.parseColor("#94A3B8"));
        // This line clears echogram tab background taking insights from null
        tabEchogram.setBackground(null);
        // This line sets default muted slate text color for echogram tab taking insights from hex color #94A3B8
        tabEchogram.setTextColor(Color.parseColor("#94A3B8"));
        // This line clears split tab background taking insights from null
        tabSplit.setBackground(null);
        // This line sets default muted slate text color for split tab taking insights from hex color #94A3B8
        tabSplit.setTextColor(Color.parseColor("#94A3B8"));

        // This line highlights appropriate tab depending on active view mode taking insights from tmap and tdchart
        if (tmap && tdchart) {
            // This line selects Split mode taking insights from MODE_SPLIT
            currentVisualMode = MODE_SPLIT;
            tabSplit.setBackgroundResource(R.drawable.bg_tab_selected);
            tabSplit.setTextColor(Color.WHITE);
        } else if (tmap) {
            // This line selects Map mode taking insights from MODE_MAP
            currentVisualMode = MODE_MAP;
            tabMap.setBackgroundResource(R.drawable.bg_tab_selected);
            tabMap.setTextColor(Color.WHITE);
        } else if (tdchart) {
            // This line selects Echogram mode taking insights from MODE_ECHOGRAM
            currentVisualMode = MODE_ECHOGRAM;
            tabEchogram.setBackgroundResource(R.drawable.bg_tab_selected);
            tabEchogram.setTextColor(Color.WHITE);
        }
    }

    // This is the setVisualMode function and toggles between Map, Echogram, and Split view modes
    public void setVisualMode(int mode) {
        // This line records requested visual mode taking insights from mode parameter
        currentVisualMode = mode;
        // This line executes view updates on the UI thread taking insights from runOnUiThread
        runOnUiThread(() -> {
            if (popupMenu == null || popupMenu.getMenu() == null) return;
            // This line retrieves map menu item taking insights from menu_map ID
            MenuItem miMap = popupMenu.getMenu().findItem(R.id.menu_map);
            // This line retrieves depth chart menu item taking insights from menu_depth_chart ID
            MenuItem miChart = popupMenu.getMenu().findItem(R.id.menu_depth_chart);

            // This line updates menu checkboxes based on selected mode taking insights from mode variable
            if (mode == MODE_MAP) {
                if (miMap != null) miMap.setChecked(true);
                if (miChart != null) miChart.setChecked(false);
            } else if (mode == MODE_ECHOGRAM) {
                if (miMap != null) miMap.setChecked(false);
                if (miChart != null) miChart.setChecked(true);
            } else { // MODE_SPLIT
                if (miMap != null) miMap.setChecked(true);
                if (miChart != null) miChart.setChecked(true);
            }
            // This line applies updated visibility states taking insights from handleChange function
            handleChange();
        });
    }

    // This is the applyAutoVisualMode function and sets default visual mode to Map view
    public void applyAutoVisualMode() {
        // This line applies Map view mode taking insights from MODE_MAP constant
        setVisualMode(MODE_MAP);
    }

    // This is the initVisualModeSelector function and sets up click listeners for Map, Echogram, and Split tabs
    private void initVisualModeSelector() {
        // This line binds Map tab TextView taking insights from tabMap layout ID
        tabMap = findViewById(R.id.tabMap);
        // This line binds Echogram tab TextView taking insights from tabEchogram layout ID
        tabEchogram = findViewById(R.id.tabEchogram);
        // This line binds Split tab TextView taking insights from tabSplit layout ID
        tabSplit = findViewById(R.id.tabSplit);

        // This line attaches click listener to select Map view taking insights from MODE_MAP
        if (tabMap != null) {
            tabMap.setOnClickListener(v -> setVisualMode(MODE_MAP));
        }
        // This line attaches click listener to select Echogram view taking insights from MODE_ECHOGRAM
        if (tabEchogram != null) {
            tabEchogram.setOnClickListener(v -> setVisualMode(MODE_ECHOGRAM));
        }
        // This line attaches click listener to select Split view taking insights from MODE_SPLIT
        if (tabSplit != null) {
            tabSplit.setOnClickListener(v -> setVisualMode(MODE_SPLIT));
        }

        // This line applies initial default visual mode taking insights from applyAutoVisualMode function
        applyAutoVisualMode();
    }


    /// all the function necessary for the map are down below
    // This is the onMapReady function and configures Google Maps settings, overlays, and camera movement
    public void onMapReady(GoogleMap googleMap) {
        // This line stores GoogleMap instance taking insights from googleMap parameter
        mMap = googleMap;
        // This line retrieves map container ViewGroup taking insights from map fragment view
        mapView = (ViewGroup) ((SupportMapFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.map))
                .getView();

        // This line adds map recenter button taking insights from addRecenter function
        addRecenter();
        // This line adds map fullscreen toggle button taking insights from addFull function
        addFull();
        // This line adds bathymetry overlay toggle button taking insights from Overlay function
        Overlay();

        // This line enables on-screen zoom control buttons taking insights from GoogleMap uiSettings
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // This line records original map view layout parameters taking insights from mapView layout params
        originalParams = mapView.getLayoutParams();
        // This line records original root container layout parameters taking insights from ll2 layout params
        loriginalParams = findViewById(R.id.ll2).getLayoutParams();

        // This line initiates periodic map coordinate update timer taking insights from handlingDelay function
        handlingDelay();

        // This line loads offline MBTiles bathymetry overlay if file is available taking insights from loadTiles function
        loadTiles();
    }

    // This is the handlingDelay function and schedules a repeating runnable every 2 seconds to update map position
    private void handlingDelay() {
        // This line schedules initial delayed runnable execution after 1 second taking insights from handler
        handler.postDelayed(new Runnable() {
            @Override
            // This is the run method and periodically parses coordinates and updates map camera and markers
            public void run() {
                // This line checks if latitude and longitude strings are non-empty taking insights from lat and lon
                if (!lat.isEmpty() && !lon.isEmpty()) {
                    // This line checks if device type is Echo Sounder taking insights from getSavedType function
                    if (getSavedType().equals("Echo Sounder")) {
                        // This line checks for valid leading zero in longitude taking insights from lon substring
                        if (lon.substring(0, 1).equals("0")) {
                            // This line converts NMEA coordinates and updates map taking insights from flat and flon
                            updatemap(flat(lat), flon(lon));
                        }
                    } else {
                        // This line parses decimal degrees and updates map taking insights from Double.parseDouble
                        updatemap(Double.parseDouble(lat), Double.parseDouble(lon));
                    }
                }
                // This line reschedules itself to run repeatedly every 2000 milliseconds taking insights from handler
                handler.postDelayed(this, 2000);
            }
        }, 1000);
    }

    // This is the flat function and converts NMEA DDMM.MMMM latitude string into decimal degrees double
    double flat(String lat) {
        // This line extracts the first 2 characters as degrees taking insights from lat substring 0 to 2
        int deg = Integer.parseInt(lat.substring(0, 2));
        // This line extracts remaining characters as minutes taking insights from lat substring 2
        Double min = Double.parseDouble(lat.substring(2));
        // This line converts degrees and minutes to decimal degrees taking insights from deg and min divided by 60
        Double dec = deg + (min / 60);
        return dec;
    }

    // This is the flon function and converts NMEA DDDMM.MMMM longitude string into decimal degrees double
    double flon(String lon) {
        // This line extracts the first 3 characters as degrees taking insights from lon substring 0 to 3
        int deg = Integer.parseInt(lon.substring(0, 3));
        // This line extracts remaining characters as minutes taking insights from lon substring 3
        Double min = Double.parseDouble(lon.substring(3));
        // This line converts degrees and minutes to decimal degrees taking insights from deg and min divided by 60
        Double dec = deg + (min / 60);
        return dec;
    }

    // This is the updatemap function and moves live marker, centers camera, and appends polyline trail points
    public void updatemap(double latitude, double longitude) {
        // This line creates a Google Maps LatLng geographic position taking insights from latitude and longitude
        LatLng location = new LatLng(latitude, longitude);
        // This line checks if marker has not yet been initialized taking insights from marker object
        if (marker == null) {
            // This line adds live marker to map taking insights from location and custom bitmap icon
            marker = mMap.addMarker(new MarkerOptions()
                    .position(location)
                    .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));
            // This line centers and zooms camera to initial location taking insights from location and zoom 15f
            mMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(location, 15f)
            );
            // This line initializes navigation trail polyline on map taking insights from PolylineOptions
            trailPolyline = mMap.addPolyline(
                    new PolylineOptions()
                            .color(Color.BLUE)
                            .width(8f)
            );
        } else {
            // This line moves existing marker to new location taking insights from location
            marker.setPosition(location);
            // This line increments trail interval counter taking insights from count
            count++;
            // This line records a new trail point every 5 updates taking insights from count value 5
            if (count == 5) {
                // This line adds location point to navigation trail history taking insights from pathPoints list
                pathPoints.add(new LatLng(latitude, longitude));
                // This line refreshes polyline vertices on map taking insights from updated pathPoints list
                trailPolyline.setPoints(pathPoints);
                // This line resets interval counter taking insights from value 1
                count = 1;
            }
        }
    }

    // This is the Overlay function and creates the bathymetry MBTiles layer toggle ImageButton on the map
    private void Overlay() {
        // This line creates overlay ImageButton widget taking insights from context
        overlay = new ImageButton(this);
        // This line sets overlay button icon drawable taking insights from overlay drawable resource
        overlay.setImageResource(R.drawable.overlay);
        // This line defines button size 100x100 taking insights from FrameLayout.LayoutParams
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(100, 100);
        // This line positions button at bottom-start of map taking insights from Gravity flags
        params.gravity = Gravity.BOTTOM | Gravity.START;
        // This line applies margins around overlay button taking insights from pixel margin offsets
        params.setMargins(30, 30, 30, 160);
        // This line applies layout parameters to overlay button taking insights from params
        overlay.setLayoutParams(params);
        // This line sets semi-transparent white background color taking insights from hex 0x88FFFFFF
        overlay.setBackgroundColor(0x88FFFFFF);
        // This line attaches click listener to toggle overlay visibility taking insights from shoverlay function
        overlay.setOnClickListener(v -> shoverlay(overlay));
        // This line adds overlay button onto map container taking insights from mapView
        mapView.addView(overlay);
    }

    // This is the shoverlay function and toggles the visibility of the offline bathymetry map tile overlay
    private void shoverlay(ImageButton o) {
        // This line checks if overlay is currently shown taking insights from shOver flag
        if (shOver) {
            // This line applies gray color filter to indicate disabled state taking insights from Color.GRAY
            o.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
            // This line dims button opacity to 50% taking insights from alpha 0.5f
            o.setAlpha(0.5f);
            // This line checks if MBTiles file exists taking insights from mbtiles.exists()
            if (mbtiles.exists()) {
                // This line hides tile overlay layer from map taking insights from tileOverlay
                tileOverlay.setVisible(false);
                // This line sets shOver flag to false taking insights from hidden state
                shOver = false;
            }
        } else {
            // This line clears color filter to indicate enabled state taking insights from clearColorFilter()
            o.clearColorFilter();
            // This line restores full button opacity taking insights from alpha 1.0f
            o.setAlpha(1.0f);

            // This line checks if MBTiles file exists taking insights from mbtiles.exists()
            if (mbtiles.exists()) {
                // This line shows tile overlay layer on map taking insights from tileOverlay
                tileOverlay.setVisible(true);
                // This line sets shOver flag to true taking insights from visible state
                shOver = true;
            } else {
                // This line displays toast informing user that no overlay file is available taking insights from Toast
                Toast.makeText(this, "There is no overlay available, Please upload", Toast.LENGTH_LONG).show();
            }
        }
    }

    // This is the addRecenter function and creates the camera recenter ImageButton on the map view
    private void addRecenter() {
        // This line creates recenter ImageButton widget taking insights from context
        ImageButton reCenter = new ImageButton(this);
        // This line sets recenter button icon drawable taking insights from recenter drawable resource
        reCenter.setImageResource(R.drawable.recenter);
        // This line defines button size 100x100 taking insights from FrameLayout.LayoutParams
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(100, 100);
        // This line positions button at top-end of map taking insights from Gravity flags
        params.gravity = Gravity.TOP | Gravity.END;
        // This line sets 30px margins around button taking insights from pixel margin offsets
        params.setMargins(30, 30, 30, 30);
        // This line applies layout parameters to recenter button taking insights from params
        reCenter.setLayoutParams(params);
        // This line sets semi-transparent white background color taking insights from hex 0x88FFFFFF
        reCenter.setBackgroundColor(0x88FFFFFF);
        // This line attaches click listener to recenter camera taking insights from ReCenter function
        reCenter.setOnClickListener(v -> ReCenter());
        // This line adds recenter button onto map container taking insights from mapView
        mapView.addView(reCenter);
    }

    // This is the ReCenter function and animates the map camera back to the vessel's live GPS coordinates
    public void ReCenter() {
        // This line checks if current coordinates are available taking insights from lat and lon
        if (!lat.isEmpty() && !lon.isEmpty()) {
            // This line checks if device type is Echo Sounder taking insights from getSavedType function
            if (getSavedType().equals("Echo Sounder")) {
                // This line converts NMEA coordinates to LatLng taking insights from flat and flon functions
                LatLng location = new LatLng(flat(lat), flon(lon));
                // This line moves camera to vessel position taking insights from location
                mMap.moveCamera(
                        CameraUpdateFactory.newLatLng(location)
                );
            } else {
                // This line parses decimal degrees to LatLng taking insights from Double.parseDouble
                LatLng location = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
                // This line moves camera to vessel position taking insights from location
                mMap.moveCamera(
                        CameraUpdateFactory.newLatLng(location)
                );
            }
        }
    }

    // This is the addFull function and creates the fullscreen toggle ImageButton on the map view
    private void addFull() {
        // This line creates toggle fullscreen ImageButton widget taking insights from context
        toggleFull = new ImageButton(this);
        // This line defines button size 100x100 taking insights from FrameLayout.LayoutParams
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(100, 100);
        // This line positions button at bottom-start of map taking insights from Gravity flags
        params.gravity = Gravity.BOTTOM | Gravity.START;
        // This line sets 30px margins around button taking insights from pixel margin offsets
        params.setMargins(30, 30, 30, 30);
        // This line applies layout parameters to button taking insights from params
        toggleFull.setLayoutParams(params);
        // This line sets semi-transparent white background color taking insights from hex 0x88FFFFFF
        toggleFull.setBackgroundColor(0x88FFFFFF);
        // This line sets expand fullscreen icon drawable taking insights from gofull drawable resource
        toggleFull.setImageResource(R.drawable.gofull);
        // This line attaches click listener to toggle fullscreen taking insights from mapfullScreen function
        toggleFull.setOnClickListener(v -> mapfullScreen());
        // This line adds fullscreen button onto map container taking insights from mapView
        mapView.addView(toggleFull);
    }

    // This is the mapfullScreen function and toggles the map view between split dashboard mode and fullscreen mode
    private void mapfullScreen() {
        ViewGroup.LayoutParams params;
        // This line checks if map is currently in standard mode taking insights from isFullscreen flag
        if (!isFullscreen) {
            // This line changes icon to exit fullscreen drawable taking insights from exitfull drawable resource
            toggleFull.setImageResource(R.drawable.exitfull);
            // This line creates fullscreen layout parameters taking insights from MATCH_PARENT
            params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            // This line hides connect button taking insights from View.GONE
            btnConnect.setVisibility(View.GONE);

            // This line hides title bar taking insights from View.GONE
            findViewById(R.id.titi).setVisibility(View.GONE);
            // This line hides depth chart taking insights from View.GONE
            findViewById(R.id.depthChart).setVisibility(View.GONE);
            // This line hides horizontal scrollview taking insights from View.GONE
            findViewById(R.id.horizontalScrollView).setVisibility(View.GONE);
            // This line hides dashboard metrics taking insights from View.GONE
            findViewById(R.id.dashboard).setVisibility(View.GONE);
            // This line hides raw data scrollview taking insights from View.GONE
            findViewById(R.id.scv).setVisibility(View.GONE);
            // This line expands container layout to fill screen taking insights from params
            findViewById(R.id.ll2).setLayoutParams(params);

            // This line expands map fragment layout to fill screen taking insights from params
            findViewById(R.id.map).setLayoutParams(params);
            // This line marks fullscreen state as true taking insights from true
            isFullscreen = true;
        } else {
            // This line restores title bar visibility taking insights from View.VISIBLE
            findViewById(R.id.titi).setVisibility(View.VISIBLE);
            // This line restores container layout parameters taking insights from loriginalParams
            findViewById(R.id.ll2).setLayoutParams(loriginalParams);
            // This line restores depth chart visibility taking insights from View.VISIBLE
            findViewById(R.id.depthChart).setVisibility(View.VISIBLE);
            // This line restores dashboard metrics visibility taking insights from View.VISIBLE
            findViewById(R.id.dashboard).setVisibility(View.VISIBLE);
            // This line synchronizes user view options taking insights from handleChange function
            handleChange();
            // This line restores original map container layout parameters taking insights from originalParams
            mapView.setLayoutParams(originalParams);
            // This line restores connect button visibility taking insights from View.VISIBLE
            btnConnect.setVisibility(View.VISIBLE);
            // This line restores enter fullscreen icon drawable taking insights from gofull drawable resource
            toggleFull.setImageResource(R.drawable.gofull);
            // This line marks fullscreen state as false taking insights from false
            isFullscreen = false;
        }
    }

    // This is the liveMarker function and programmatically draws a blue circular bitmap icon for the live GPS marker
    private void liveMarker() {
        // This line creates gradient drawable shape taking insights from GradientDrawable
        GradientDrawable drawable = new GradientDrawable();
        // This line sets shape type to oval/circle taking insights from GradientDrawable.OVAL
        drawable.setShape(GradientDrawable.OVAL);
        // This line sets fill color to blue taking insights from Color.BLUE
        drawable.setColor(Color.BLUE);
        // This line sets circle dimensions to 40x40 pixels taking insights from dimensions 40, 40
        drawable.setSize(40, 40);
        // This line creates blank 40x40 ARGB bitmap taking insights from Bitmap.createBitmap
        bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888);
        // This line creates Canvas to draw onto the bitmap taking insights from bitmap
        Canvas canvas = new Canvas(bitmap);
        // This line sets drawing bounds for circular shape taking insights from dimensions 40, 40
        drawable.setBounds(0, 0, 40, 40);
        // This line renders blue circle onto bitmap canvas taking insights from canvas
        drawable.draw(canvas);
    }


    /// all the functions necessary for the bluetooth connection and handling
    // This is the requestPermissions function and prompts user for runtime Bluetooth connect and scan permissions
    private void requestPermissions() {
        // This line checks if device runs Android 12 (API 31) or newer taking insights from Build.VERSION.SDK_INT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // This line requests BLUETOOTH_CONNECT and BLUETOOTH_SCAN permissions taking insights from ActivityCompat
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    },
                    1
            );
        } else {
            // This line shows paired devices dialog directly on Android 11 and older taking insights from showPairedDevicesDialog
            showPairedDevicesDialog();
        }
    }

    @Override
    // This is the onRequestPermissionsResult function and handles user acceptance or denial of requested permissions
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        // This line calls parent onRequestPermissionsResult taking insights from requestCode, permissions, and grantResults
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // This line checks if response corresponds to our Bluetooth permission request code 1 taking insights from requestCode
        if (requestCode == 1) {
            // This line initializes grant status flag taking insights from true
            boolean granted = true;

            // This line iterates over results to ensure all permissions were granted taking insights from grantResults
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            // This line opens device picker if permissions were granted taking insights from granted flag
            if (granted) {
                showPairedDevicesDialog();
            } else {
                // This line displays toast warning if permissions were denied taking insights from Toast
                Toast.makeText(this,
                        "Permission Denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // This is the handlePR function and toggles targeted profile recording between Start and Stop states
    private void handlePR() {
        // This line executes action on UI thread taking insights from runOnUiThread
        runOnUiThread(() -> {
            // This line checks if button is in Start state taking insights from PR.getText()
            if (PR.getText().equals("Start")) {
                // This line shows confirmation dialog to start profile survey taking insights from alertstartprconfirm
                alertstartprconfirm();
            } else if (PR.getText().equals("Stop")) {
                // This line shows confirmation dialog to stop profile survey taking insights from alertstopprconfirm
                alertstopprconfirm();
            }
        });
    }

    // This is the alertChangePrefix function and displays dialog allowing user to edit file naming prefix
    private void alertChangePrefix() {
        // This line creates text input field for prefix name taking insights from EditText
        EditText editText = new EditText(this);
        // This line sets input field placeholder hint taking insights from string hint
        editText.setHint("Enter File prefix");

        // This line builds prefix dialog taking insights from AlertDialog.Builder
        AlertDialog dia = new AlertDialog.Builder(this)
                .setTitle("Enter Prefix")
                .setView(editText)
                .setCancelable(false)
                .setPositiveButton("Submit", (dialog, which) -> {
                    // This line extracts user typed prefix string taking insights from editText.getText()
                    String text = editText.getText().toString();
                    // This line verifies prefix string is not empty taking insights from text.isEmpty()
                    if (!text.isEmpty()) {
                        // This line saves chosen prefix into SharedPreferences taking insights from PREFIX preference
                        getSharedPreferences("PREFIX", MODE_PRIVATE)
                                .edit()
                                .putString("LAST_PREFIX", text)
                                .apply();
                        // This line closes old active log files taking insights from closecurrentfile function
                        closecurrentfile();
                        // This line creates new output files with updated prefix taking insights from createTextFile function
                        createTextFile();
                        // This line creates fresh profile survey files if recording is currently active taking insights from isprrecording
                        if (isprrecording) {
                            createfresh();
                        }
                        // This line displays success toast taking insights from Toast
                        Toast.makeText(this, "Successfully Changed", Toast.LENGTH_SHORT).show();
                    } else {
                        // This line warns user that prefix cannot be blank taking insights from Toast
                        Toast.makeText(this, "Cannot be Empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        // This line sets green color on positive submit button taking insights from R.color.green
        dia.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.green));
        // This line sets red color on negative cancel button taking insights from R.color.red
        dia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.red));
    }

    // This is the showPairedDevicesDialog function and displays an alert dialog listing all paired Bluetooth devices
    private void showPairedDevicesDialog() {
        // This line checks Bluetooth connect permission on Android 12+ taking insights from checkSelfPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        // This line retrieves paired Bluetooth devices from adapter taking insights from getBondedDevices()
        Set<BluetoothDevice> pairedDevices =
                bluetoothAdapter.getBondedDevices();

        // This line checks if paired device list is empty taking insights from pairedDevices.isEmpty()
        if (pairedDevices.isEmpty()) {
            // This line displays toast informing user to pair hardware first taking insights from Toast
            Toast.makeText(this,
                    "No paired devices found",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // This line allocates string array for device label display taking insights from pairedDevices size
        String[] deviceNames =
                new String[pairedDevices.size()];

        // This line allocates device object array taking insights from pairedDevices size
        BluetoothDevice[] devices =
                new BluetoothDevice[pairedDevices.size()];

        int index = 0;

        // This line populates device names and MAC addresses taking insights from pairedDevices set
        for (BluetoothDevice device : pairedDevices) {
            deviceNames[index] =
                    device.getName() + "\n" +
                            device.getAddress();

            devices[index] = device;

            index++;
        }

        // This line builds paired device chooser dialog taking insights from AlertDialog.Builder
        pairedDevicesAlert = new AlertDialog.Builder(this)
                .setTitle("Select Bluetooth Device")
                .setItems(deviceNames, (dialog, which) -> {
                    // This line retrieves selected Bluetooth device object taking insights from devices array index
                    BluetoothDevice selectedDevice = devices[which];

                    // This line stores chosen device MAC address taking insights from selectedDevice.getAddress()
                    deviceAddress = selectedDevice.getAddress();
                    // This line stores chosen device name taking insights from selectedDevice.getName()
                    deviceName = selectedDevice.getName();
                    // This line opens device hardware type selector dialog taking insights from showDeviceTypeDialog function
                    showDeviceTypeDialog(selectedDevice);
                })
                .setCancelable(true)
                .setNegativeButton("Cancel", (dialogInterface, which) -> dialogInterface.dismiss())
                .create();

        // This line attaches cancel button dismissal handler when reconfiguring connection taking insights from ifExec
        if (ifExec == 1) {
            pairedDevicesAlert.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, which) -> {
                dialogInterface.dismiss();
            });
        }
        // This line displays paired devices dialog on screen taking insights from pairedDevicesAlert
        pairedDevicesAlert.show();

        // This line sets green color on positive dialog button taking insights from R.color.green
        pairedDevicesAlert.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.green));
        // This line sets red color on negative cancel button taking insights from R.color.red
        pairedDevicesAlert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.red));
    }

    // This is the showDeviceTypeDialog function and lets the user select whether the connected device is an Echo Sounder or RTK Device
    private void showDeviceTypeDialog(BluetoothDevice selectedDevice) {
        // This line defines supported hardware device types taking insights from string array options
        String[] deviceTypes = {
                "Echo Sounder", "RTK Device"
        };

        // This line builds device type selection dialog taking insights from AlertDialog.Builder
        devicetypeAlert = new AlertDialog.Builder(this)
                .setTitle("Select Device Type")
                .setItems(deviceTypes, (dialog, which) -> {
                    // This line records chosen device type string taking insights from deviceTypes array index
                    deviceType = deviceTypes[which];

                    // This line saves connection settings into SharedPreferences taking insights from BT_PREF
                    getSharedPreferences("BT_PREF", MODE_PRIVATE)
                            .edit()
                            .putString("LAST_DEVICE", deviceAddress)
                            .putString("DEVICE_TYPE", deviceType)
                            .putString("DEVICE_NAME", deviceName)
                            .apply();

                    // This line resets visual mode to default taking insights from applyAutoVisualMode function
                    applyAutoVisualMode();

                    // This line checks Bluetooth connect permission taking insights from checkSelfPermission
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    // This line retrieves selected device name taking insights from selectedDevice
                    String name = selectedDevice.getName();
                    if (name == null) {
                        name = "Unknown Device";
                    }

                    // This line updates raw log TextView with connection summary taking insights from name, address, and type
                    txtNmea.setText(
                            "Selected:\n" +
                                    name +
                                    "\n" +
                                    deviceAddress +
                                    "\nType: " +
                                    deviceType
                    );

                    // This line checks if address is valid taking insights from deviceAddress
                    if (deviceAddress != null && !deviceAddress.isEmpty()) {
                        // This line establishes connection if launched at startup taking insights from ifExec
                        if (ifExec == 0) {
                            connectBluetooth();
                        } else {
                            // This line cleanly restarts activity when reconfigured from menu taking insights from destroy_on_configure
                            if (destroy_on_configure()) {
                                Intent intent = getPackageManager()
                                        .getLaunchIntentForPackage(getPackageName());

                                if (intent != null) {
                                    intent.addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    );
                                    startActivity(intent);
                                }
                                finishAffinity();
                            }
                        }
                    }
                })
                .setCancelable(true)
                .setNegativeButton("Back", (dialogInterface, which) -> {
                    // This line returns to paired devices dialog on back tap taking insights from showPairedDevicesDialog
                    dialogInterface.dismiss();
                    showPairedDevicesDialog();
                })
                .create();

        // This line shows device type selection dialog taking insights from devicetypeAlert
        devicetypeAlert.show();

        // This line sets red text color on back button taking insights from R.color.red
        if (devicetypeAlert.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            devicetypeAlert.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    // This is the connectBluetooth function and establishes an RFCOMM Bluetooth connection with selected GNSS/sonar hardware
    private void connectBluetooth() {
        // This line displays connecting progress dialog taking insights from showLoading function
        showLoading();
        // This line starts background thread for Bluetooth socket connection taking insights from new Thread
        new Thread(() -> {
            try {
                // This line displays connecting status text on UI thread taking insights from runOnUiThread
                runOnUiThread(() -> txtNmea.setText("Connecting..."));
                // This line gets remote Bluetooth hardware device taking insights from deviceAddress
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

                // This line verifies Bluetooth permissions on Android 12+ taking insights from checkSelfPermission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                }

                // This line cancels Bluetooth discovery to maximize connection bandwidth taking insights from bluetoothAdapter
                bluetoothAdapter.cancelDiscovery();
                // This line creates RFCOMM socket with standard SPP UUID taking insights from uuid
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid);
                // This line connects Bluetooth socket to hardware taking insights from bluetoothSocket.connect()
                bluetoothSocket.connect();
                // This line enables background worker reading loop taking insights from stopWorker flag
                stopWorker = false;
                // This line updates UI upon successful connection taking insights from runOnUiThread
                runOnUiThread(() -> {
                    txtNmea.setText("Connected Successfully\n");

                    // This line generates survey session filenames taking insights from filename function
                    if (changeFilename) {
                        filename();
                    }
                    // This line starts data recording and stream processing taking insights from recordAndSave function
                    recordAndSave();
                    // This line makes profile recording button visible taking insights from View.VISIBLE
                    PR.setVisibility(View.VISIBLE);
                });

            } catch (Exception e) {
                // This line handles connection failure taking insights from alertDisconnected function
                alertDisconnected(0);
            }
        }).start();
    }

    // This is the showLoading function and displays a non-cancelable progress dialog while Bluetooth connects
    private void showLoading() {
        // This line inflates custom loading dialog layout taking insights from R.layout.loading
        View view = getLayoutInflater().inflate(R.layout.loading, null);
        // This line binds device type TextView widget taking insights from layout ID device_type
        TextView device_type = view.findViewById(R.id.device_type);
        // This line displays hardware name and type taking insights from deviceName and deviceType
        if (!deviceName.isEmpty()) {
            device_type.setText(deviceName + " (" + deviceType + ")");
        } else {
            device_type.setText(getSavedDeviceName() + "(" + getSavedType() + ")");
        }
        // This line constructs loading dialog taking insights from AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);
        builder.setTitle("Connecting to GNSS device ");
        builder.setCancelable(false);

        // This line creates and displays loading dialog taking insights from builder
        loadingDialog = builder.create();
        loadingDialog.show();
    }

    // This is the filename function and creates timestamped data files for the survey session
    private void filename() {
        // This line generates current date-time timestamp string taking insights from SimpleDateFormat
        timeStamp = new java.text.SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());
        // This line creates output text files in storage taking insights from createTextFile function
        createTextFile();
        // This line prevents duplicate file recreation taking insights from false
        changeFilename = false;
    }

    // This is the getSavedDeviceName function and reads the last connected device name from SharedPreferences
    private String getSavedDeviceName() {
        // This line references SharedPreferences XML file in app data directory taking insights from BT_PREF.xml
        File sharedPrefsFile = new File(
                getApplicationInfo().dataDir + "/shared_prefs/BT_PREF.xml"
        );

        // This line checks if preference file exists taking insights from sharedPrefsFile.exists()
        if (sharedPrefsFile.exists()) {
            // This line returns stored device name taking insights from DEVICE_NAME preference key
            return getSharedPreferences("BT_PREF", MODE_PRIVATE)
                    .getString("DEVICE_NAME", "");
        }
        return "";
    }

    // This is the getSavedPrefix function and reads the survey file prefix name from SharedPreferences
    private String getSavedPrefix() {
        // This line references SharedPreferences XML file in app data directory taking insights from PREFIX.xml
        File sharedPrefsFile = new File(
                getApplicationInfo().dataDir + "/shared_prefs/PREFIX.xml"
        );

        // This line checks if preference file exists taking insights from sharedPrefsFile.exists()
        if (sharedPrefsFile.exists()) {
            // This line returns stored prefix string taking insights from LAST_PREFIX preference key
            return getSharedPreferences("PREFIX", MODE_PRIVATE)
                    .getString("LAST_PREFIX", "");
        }
        // This line returns default prefix string when no file exists taking insights from fallback "Untitled"
        return "Untitled";
    }

    // This is the getSavedType function and reads the device type from SharedPreferences
    private String getSavedType() {
        // This line references SharedPreferences XML file in app data directory taking insights from BT_PREF.xml
        File sharedPrefsFile = new File(
                getApplicationInfo().dataDir + "/shared_prefs/BT_PREF.xml"
        );

        // This line checks if preference file exists taking insights from sharedPrefsFile.exists()
        if (sharedPrefsFile.exists()) {
            // This line returns stored device type taking insights from DEVICE_TYPE preference key
            return getSharedPreferences("BT_PREF", MODE_PRIVATE)
                    .getString("DEVICE_TYPE", "");
        }

        return "";
    }

    // This is the checkButton function and handles click events for connecting Bluetooth or stopping survey recording
    private void checkButton() {
        // This line checks if button is in Connect state taking insights from btnConnect text
        if (btnConnect.getText().equals("Connect BLUETOOTH")) {
            // This line starts Bluetooth permission and connection flow taking insights from requestPermissions function
            requestPermissions();
        } else {
            // This line shows toast confirmation taking insights from btnConnect text
            Toast.makeText(this, btnConnect.getText(), Toast.LENGTH_LONG).show();
            // This line shows exit recording confirmation dialog taking insights from alertStopConfirm function
            alertStopConfirm();
        }
    }

    // This is the extension function and sets the active file extension based on hardware type
    private void extension() {
        // This line sets extension to txt for Echo Sounder or ubx for RTK taking insights from getSavedType function
        if (getSavedType().equals("Echo Sounder")) {
            ext = "txt";
        } else {
            ext = "ubx";
        }
    }

    // This is the recordAndSave function and starts appropriate background recording thread based on device type
    private void recordAndSave() {
        // This line dismisses loading dialog taking insights from loadingDialog
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }

        // This line launches Echo Sounder or RTK reader taking insights from getSavedType function
        if (getSavedType().equals("Echo Sounder")) {
            ES();
            ext = "txt";
        } else {
            rtk();
            ext = "ubx";
        }
    }

    // This is the rtk function and reads raw binary UBX byte packets from Bluetooth hardware and parses navigation data
    private void rtk() {
        // This line allocates 1KB byte buffer for incoming binary stream chunks taking insights from size 1024
        byte[] bytes = new byte[1024];

        // This line spawns background worker thread for RTK reading taking insights from new Thread
        new Thread(() -> {
            try {
                // This line opens raw byte input stream from Bluetooth socket taking insights from bluetoothSocket
                InputStream inputStream = bluetoothSocket.getInputStream();
                // This line updates connect button to red EXIT button taking insights from runOnUiThread
                runOnUiThread(() -> {
                    btnConnect.setText("EXIT");
                    ((Button) findViewById(R.id.btnConnect))
                            .setBackgroundColor(Color.parseColor("#EF5350"));
                });
                // This line loops continuously while worker is active taking insights from stopWorker flag
                while (!stopWorker) {
                    // This line reads byte chunks from stream into buffer taking insights from inputStream.read
                    int bytesRead = inputStream.read(bytes);

                    // This line processes received chunk taking insights from bytesRead
                    if (bytesRead > 0) {
                        // This line saves chunk to continuous live log taking insights from saveLiveData1 function
                        saveLiveData1(bytes, 0, bytesRead);
                        // This line saves chunk to profile survey raw log if active taking insights from isprrecording flag
                        if (isprrecording) {
                            saveRawPRData1(bytes, 0, bytesRead);
                        }
                        // This line forwards raw bytes into UBX packet parser taking insights from parser.addBytes
                        parser.addBytes(bytes, bytesRead);
                        // This line converts raw bytes to ASCII string for diagnostic log taking insights from US_ASCII charset
                        String nmea = new String(bytes, 0, bytesRead, StandardCharsets.US_ASCII);
                        // This line appends text to raw data log viewer taking insights from runOnUiThread
                        runOnUiThread(() -> {
                            if (txtNmea.getText().length() > 3000) {
                                txtNmea.setText("");
                            }
                            txtNmea.append(nmea);
                        });
                    }
                }
            } catch (IOException e) {
                // This line logs socket disconnection taking insights from bluetoothSocket state
                if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                    Log.d("RTK", "Bluetooth socket disconnected");
                } else {
                    Log.e("RTK", "Bluetooth read error", e);
                }
                // This line alerts user of unexpected disconnection taking insights from alertDisconnected function
                alertDisconnected(1);
            }
        }).start();
    }

    // This is the ES function and reads ASCII NMEA text lines from Echo Sounder Bluetooth hardware and parses depth and GPS
    private void ES() {
        // This line displays syncing toast taking insights from Toast
        Toast.makeText(this, "Syncing...", Toast.LENGTH_SHORT).show();
        // This line spawns background worker thread for Echo Sounder text reading taking insights from new Thread
        new Thread(() -> {
            try {
                // This line wraps socket input stream in BufferedReader taking insights from InputStreamReader
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                bluetoothSocket.getInputStream()
                        )
                );

                // This line updates connect button to red EXIT button taking insights from runOnUiThread
                runOnUiThread(() -> {
                    btnConnect.setText("EXIT");
                    ((Button) findViewById(R.id.btnConnect))
                            .setBackgroundColor(Color.parseColor("#EF5350"));
                });

                int i = 0;

                // This line loops continuously while worker is active taking insights from stopWorker flag
                while (!stopWorker) {
                    // This line reads next text line from Bluetooth stream taking insights from reader.readLine()
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }

                    // This line updates raw log text display taking insights from runOnUiThread
                    runOnUiThread(() -> {
                        if (txtNmea.getText().length() >= 2000) {
                            txtNmea.setText("");
                        }
                        txtNmea.append(line + "\n");
                    });

                    // This line validates NMEA sentence prefix taking insights from dollar sign "$"
                    if (line.startsWith("$")) {
                        // This line saves line to continuous live log taking insights from saveLiveData function
                        saveLiveData(line);
                        // This line saves line to profile survey raw log if active taking insights from isprrecording flag
                        if (isprrecording) {
                            saveRawPRData(line);
                        }
                        // This line updates UI table and dashboard taking insights from runOnUiThread
                        runOnUiThread(() -> {
                            String[] part = line.split(",");
                            // This line checks for valid GNRMC GPS sentence taking insights from sentence identifiers
                            if (part[0].equals("$GNRMC") && part[2].equals("A") && part.length == 14 && line.length() > 20 && j == 0) {
                                sline = line;
                                j = 1;
                            } else if (part[0].equals("$ES") && j == 1) {
                                // This line limits visible table rows to 20 taking insights from table.getChildCount()
                                if (table.getChildCount() > 20) {
                                    table.removeViewAt(1);
                                }
                                // This line saves formatted combined row taking insights from saveFormatedData function
                                saveFormatedData(fline(sline, line));
                                // This line saves formatted row to profile survey if active taking insights from isprrecording flag
                                if (isprrecording) {
                                    savePRData(fline(sline, line));
                                }
                                // This line adds new row to on-screen data table taking insights from AddRow function
                                table.addView(AddRow(sline, line), new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                                j = 0;
                            }
                        });

                        // This line throttles map coordinate updates to every 20th sentence taking insights from counter i
                        if (line.startsWith("$GNRMC")) {
                            i++;
                            if (i == 20) {
                                String[] cord = line.split(",");
                                if (!cord[3].isEmpty()) {
                                    lat = cord[3];
                                }
                                if (!cord[5].isEmpty()) {
                                    lon = cord[5];
                                }
                                i = 0;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // This line displays error toast on UI thread taking insights from exception message
                runOnUiThread(() -> {
                    Toast.makeText(
                            this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
                // This line alerts user of unexpected disconnection taking insights from alertDisconnected function
                alertDisconnected(1);
            }
        }).start();
    }

    // This is the fhead function and returns a formatted column header string for text file survey logs
    private String fhead() {
        // This line formats fixed-width column titles taking insights from String.format
        String f = String.format("%-12s", "DATE")
                + String.format("%-10s", "TIME")
                + String.format("%-15s", "LAT")
                + String.format("%-15s", "LON")
                + String.format("%-10s", "DEPTH(m)")
                + String.format("%-12s", "SPEED(m/s)")
                + String.format("%-8s", "COG")
                + String.format("%-10s", "ALTITUDE");
        return f;
    }

    // This is the fline function and merges NMEA GNRMC position and ES depth sentences into a formatted log row
    private String fline(String m, String n) {
        // This line splits position sentence into tokens taking insights from comma delimiter
        String[] mp = m.split(",");
        // This line splits depth sentence into tokens taking insights from comma delimiter
        String[] np = n.split(",");
        // This line updates dashboard widgets taking insights from updateDash function
        updateDash(mp, np);
        // This line formats columns into a single line taking insights from helper format functions
        String f = String.format("%-12s", FDate(mp[9]) + ", ")
                + String.format("%-10s", FTime(mp[1]) + ", ")
                + String.format("%-15s", FLat(mp[3]) + " " + mp[4] + ", ")
                + String.format("%-15s", FLon(mp[5]) + " " + mp[6] + ", ")
                + String.format("%-10s", FDep(np[1]) + ", ")
                + String.format("%-12s", FSpe(mp[7]) + ", ")
                + String.format("%-8s", FCog(mp[8]) + ", ")
                + String.format("%-10s", ", ");
        return f;
    }

    // This is the updateDash function and updates the dashboard TextViews and depth chart with latest readings
    private void updateDash(String[] mp, String[] np) {
        // This line runs UI updates on the main thread taking insights from runOnUiThread
        runOnUiThread(() -> {
            // This line updates date TextView taking insights from FDate function
            d1.setText(FDate(mp[9]));
            // This line updates latitude TextView taking insights from FLat function
            d2.setText(FLat(mp[3]));
            // This line updates latitude hemisphere indicator taking insights from mp index 4
            d21.setText(" °" + mp[4]);
            // This line updates longitude TextView taking insights from FLon function
            d3.setText(FLon(mp[5]));
            // This line updates longitude hemisphere indicator taking insights from mp index 6
            d31.setText(" °" + mp[6]);
            // This line updates time TextView taking insights from FTime function
            d4.setText(FTime(mp[1]));
            // This line updates speed TextView taking insights from FSpe function
            d5.setText(FSpe(mp[7]));
            // This line updates course over ground TextView taking insights from FCog function
            d6.setText(FCog(mp[8]));
            // This line updates depth TextView taking insights from FDep function
            d7.setText(FDep(np[1]));
            // This line sets placeholder altitude indicator taking insights from dashes
            d8.setText("---");
            try {
                // This line parses depth in millimeters and converts to meters taking insights from np index 1
                if (!np[1].isEmpty()) {
                    float depth = Float.parseFloat(np[1]) / 1000f;
                    // This line appends depth reading to echogram chart taking insights from depthChart.addDepth
                    depthChart.addDepth(depth);
                }
            } catch (Exception ignored) {
                // This line safely ignores number parsing exceptions taking insights from try block
            }
        });
    }

    // This is the stopRecording function and stops background communication worker and closes all active file streams
    private void stopRecording() {
        // This line signals background threads to terminate taking insights from stopWorker flag
        stopWorker = true;
        try {
            // This line closes live raw output stream taking insights from liveOutputStream
            if (liveOutputStream != null) {
                liveOutputStream.close();
                liveOutputStream = null;
            }
            // This line closes formatted parsed output stream taking insights from formatedOutputStream
            if (formatedOutputStream != null) {
                formatedOutputStream.close();
                formatedOutputStream = null;
            }
            // This line closes profile survey output stream taking insights from prOutputStream
            if (prOutputStream != null) {
                prOutputStream.close();
                prOutputStream = null;
            }
            // This line closes raw profile survey output stream taking insights from prrawOutputStream
            if (prrawOutputStream != null) {
                prrawOutputStream.close();
                prrawOutputStream = null;
            }
        } catch (Exception e) {
            // This line catches stream closing exceptions taking insights from try block
        }
        // This line finishes activity taking insights from finish()
        finish();
    }

    // This is the closecurrentfile function and closes active output streams without terminating activity
    private void closecurrentfile() {
        try {
            // This line flushes and closes live raw output stream taking insights from liveOutputStream
            if (liveOutputStream != null) {
                liveOutputStream.close();
                liveOutputStream = null;
            }
            // This line flushes and closes formatted parsed output stream taking insights from formatedOutputStream
            if (formatedOutputStream != null) {
                formatedOutputStream.close();
                formatedOutputStream = null;
            }
            // This line flushes and closes profile survey output stream taking insights from prOutputStream
            if (prOutputStream != null) {
                prOutputStream.close();
                prOutputStream = null;
            }
            // This line flushes and closes raw profile survey output stream taking insights from prrawOutputStream
            if (prrawOutputStream != null) {
                prrawOutputStream.close();
                prrawOutputStream = null;
            }
        } catch (Exception e) {
            // This line catches stream closing exceptions taking insights from try block
        }
    }

    // This is the alertDisconnected function and displays dialog informing user of connection failure or disconnection
    private void alertDisconnected(int x) {
        // This line handles initial connection failure taking insights from parameter x == 0
        if (x == 0) {
            runOnUiThread(() -> {
                // This line dismisses loading dialog taking insights from loadingDialog
                if (loadingDialog != null && loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                // This line builds connection failed dialog taking insights from AlertDialog.Builder
                AlertDialog dia = new AlertDialog.Builder(this)
                        .setTitle("Bluetooth Not Connected")
                        .setMessage("Failed to connect to bluetooth\n \u2022 Check if the device is switch on\n \u2022 Check if the device is in range")
                        .setCancelable(false)
                        .setPositiveButton("Reconnect", (dialog, which) -> {
                            connectBluetooth();
                        })
                        .setNegativeButton("Select Device", (dialog, which) -> {
                            showPairedDevicesDialog();
                        })
                        .show();
                // This line sets green text color on reconnect button taking insights from R.color.green
                dia.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(this, R.color.green));
            });
        } else {
            // This line handles unexpected disconnection during active survey taking insights from parameter x != 0
            runOnUiThread(() -> {
                AlertDialog dia = new AlertDialog.Builder(this)
                        .setTitle("Device disconnected")
                        .setCancelable(false)
                        .setMessage("Device was unexpectedly disconnected\n \u2022 Check if the device is switch on\n \u2022 Check if the device is in range")
                        .setPositiveButton("Reconnect", (dialog, which) -> {
                            connectBluetooth();
                        })
                        .setNegativeButton("Exit", (dialog, which) -> {
                            finish();
                        })
                        .show();
                // This line sets green text color on reconnect button taking insights from R.color.green
                dia.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.green));
                // This line sets red text color on exit button taking insights from R.color.red
                dia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.red));
            });
        }
    }

    // This is the createfresh function and initializes new MediaStore output files for targeted profile recording
    private void createfresh() {
        // This line retrieves active prefix string taking insights from getSavedPrefix function
        String prefix = getSavedPrefix();
        // This line generates timestamp for profile filenames taking insights from SimpleDateFormat
        String tS = new java.text.SimpleDateFormat(
                "yyyyMMdd_HHmmss",
                java.util.Locale.getDefault()
        ).format(new java.util.Date());
        // This line constructs profile parsed text filename taking insights from prefix and timestamp
        String fileNamepr = prefix + "_" + tS + ".txt";
        // This line constructs profile raw stream filename taking insights from prefix, timestamp, and extension
        String fileNameprraw = prefix + "_raw_" + tS + "." + ext;
        try {
            // This line sets metadata for profile parsed text file taking insights from MediaStore
            ContentValues pr_values = new ContentValues();
            pr_values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileNamepr);
            pr_values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            pr_values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/NMEA/" + prefix + "/FINAL"
            );

            // This line sets metadata for profile raw stream file taking insights from MediaStore
            ContentValues pr_raw_values = new ContentValues();
            pr_raw_values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileNameprraw);
            pr_raw_values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            pr_raw_values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/NMEA/" + prefix + "/FINAL"
            );

            // This line inserts parsed profile file into MediaStore taking insights from content resolver
            PRFileUri = getContentResolver().insert(
                    MediaStore.Files.getContentUri("external"),
                    pr_values
            );
            if (PRFileUri != null) {
                // This line opens output stream in write-append mode taking insights from PRFileUri
                prOutputStream =
                        getContentResolver().openOutputStream(
                                PRFileUri,
                                "wa"
                        );

                // This line writes column header row to profile text file taking insights from fhead function
                if (prOutputStream != null) {
                    prOutputStream.write((fhead() + "\n").getBytes());
                    prOutputStream.flush();
                }

                // This line inserts raw profile file into MediaStore taking insights from content resolver
                RawPRFileUri = getContentResolver().insert(
                        MediaStore.Files.getContentUri("external"),
                        pr_raw_values
                );

                // This line opens output stream for raw profile stream taking insights from RawPRFileUri
                prrawOutputStream =
                        getContentResolver().openOutputStream(
                                RawPRFileUri,
                                "wa"
                        );
            }
        } catch (Exception e) {
            // This line wraps checked exceptions in RuntimeException taking insights from exception e
            throw new RuntimeException(e);
        }

        // This line updates UI recording indicators taking insights from runOnUiThread
        runOnUiThread(() -> {
            // This line displays profile filename on UI taking insights from fileNamepr
            fileName.setText(fileNamepr);
            // This line shows recording indicator icon taking insights from View.VISIBLE
            findViewById(R.id.ri).setVisibility(View.VISIBLE);

            // This line changes PR button background color to orange taking insights from hex color #FFA726
            ((Button) findViewById(R.id.pR))
                    .setBackgroundColor(Color.parseColor("#FFA726"));
            // This line changes PR button text to Stop taking insights from string "Stop"
            PR.setText("Stop");
            // This line marks profile recording flag as true taking insights from true
            isprrecording = true;
            // This line displays toast confirming recording started taking insights from Toast
            Toast.makeText(this, "recording...", Toast.LENGTH_SHORT).show();
        });
    }

    // This is the alertstartprconfirm function and asks user confirmation before starting profile recording
    void alertstartprconfirm() {
        // This line builds confirmation alert dialog taking insights from AlertDialog.Builder
        AlertDialog dia = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Start Recording??")
                .setMessage("Are you sure you want to Start the recording?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // This line initializes profile recording files taking insights from createfresh function
                    createfresh();
                })
                .setNegativeButton("No", null)
                .show();

        // This line sets green color on positive confirmation button taking insights from R.color.green
        dia.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.green));
        // This line sets red color on negative cancel button taking insights from R.color.red
        dia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.red));
    }

    // This is the alertstopprconfirm function and asks user confirmation before stopping profile recording
    void alertstopprconfirm() {
        // This line builds stop confirmation alert dialog taking insights from AlertDialog.Builder
        AlertDialog dia = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Stop Recording??")
                .setMessage("Are you sure you want to stop the recording?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // This line flushes and closes profile recording streams taking insights from stopOld function
                    stopOld();
                })
                .setNegativeButton("No", null)
                .show();

        // This line sets green color on positive confirmation button taking insights from R.color.green
        dia.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.green));
        // This line sets red color on negative cancel button taking insights from R.color.red
        dia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.red));
    }

    // This is the stopOld function and flushes and closes targeted profile recording streams and resets button UI
    private void stopOld() {
        try {
            // This line closes profile parsed output stream taking insights from prOutputStream
            if (prOutputStream != null) {
                prOutputStream.close();
                prOutputStream = null;
            }

            // This line closes profile raw output stream taking insights from prrawOutputStream
            if (prrawOutputStream != null) {
                prrawOutputStream.close();
                prrawOutputStream = null;
            }

            // This line marks profile recording flag as false taking insights from false
            isprrecording = false;
            // This line hides recording indicator icon taking insights from View.GONE
            findViewById(R.id.ri).setVisibility(View.GONE);
            // This line resets PR button background color to light green taking insights from hex color #90EE90
            ((Button) findViewById(R.id.pR))
                    .setBackgroundColor(Color.parseColor("#90EE90"));
            // This line resets PR button text to Start taking insights from string "Start"
            PR.setText("Start");

            // This line displays toast confirming recording stopped taking insights from Toast
            Toast.makeText(this, "recording stopped", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            // This line wraps checked IOException in RuntimeException taking insights from exception e
            throw new RuntimeException(e);
        }
    }

    // This is the createTextFile function and initializes MediaStore continuous raw and parsed survey data files
    private void createTextFile() {
        try {
            // This line determines file extension based on device type taking insights from getSavedType function
            if (getSavedType().equals("Echo Sounder")) {
                ext = "txt";
                Log.d("EXT", ext);
            } else {
                ext = "ubx";
                Log.d("EXT", ext);
            }
            // This line retrieves saved prefix string taking insights from getSavedPrefix function
            String prefix = getSavedPrefix();
            // This line constructs continuous raw file display name taking insights from prefix, timestamp, and extension
            String fileNamer = "ALL_Raw_output_" + timeStamp + "." + ext;
            // This line constructs continuous parsed file display name taking insights from prefix and timestamp
            String fileNamef = "ALL_fp_" + timeStamp + ".txt";

            ContentValues r_values = new ContentValues();
            ContentValues f_values = new ContentValues();

            // This line sets metadata for raw continuous data file taking insights from fileNamer
            r_values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileNamer);
            r_values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
            r_values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/NMEA/" + prefix + "/RAW_DATA"
            );

            // This line sets metadata for parsed continuous data file taking insights from fileNamef
            f_values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileNamef);
            f_values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
            f_values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/NMEA/" + prefix + "/PHARSED_DATA"
            );

            // This line inserts raw file record into MediaStore taking insights from content resolver
            liveFileUri = getContentResolver().insert(
                    MediaStore.Files.getContentUri("external"),
                    r_values
            );

            // This line inserts parsed file record into MediaStore taking insights from content resolver
            FormateFileUri = getContentResolver().insert(
                    MediaStore.Files.getContentUri("external"),
                    f_values
            );

            // This line opens raw output stream in write-append mode taking insights from liveFileUri
            if (liveFileUri != null) {
                liveOutputStream =
                        getContentResolver().openOutputStream(
                                liveFileUri,
                                "wa"
                        );
            }

            // This line opens parsed output stream in write-append mode taking insights from FormateFileUri
            if (FormateFileUri != null) {
                formatedOutputStream =
                        getContentResolver().openOutputStream(
                                FormateFileUri,
                                "wa"
                        );

                // This line writes column header titles to parsed file taking insights from fhead function
                if (formatedOutputStream != null) {
                    formatedOutputStream.write((fhead() + "\n").getBytes());
                    formatedOutputStream.flush();
                }
            }

        } catch (Exception e) {
            // This line logs error details taking insights from exception e
            Log.e("UBX_FILE", "Error creating UBX file", e);
        }
    }

    // This is the saveLiveData function and writes an ASCII text line into the continuous raw output file
    private void saveLiveData(String line) {
        try {
            // This line writes text line and newline to raw stream taking insights from liveOutputStream
            if (liveOutputStream != null) {
                liveOutputStream.write((line + "\n").getBytes());
                liveOutputStream.flush();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the saveLiveData1 function and writes binary byte chunks into the continuous raw output file
    private void saveLiveData1(byte[] b, int o, int c) {
        try {
            // This line writes binary byte array chunk to raw stream taking insights from liveOutputStream
            if (liveOutputStream != null) {
                liveOutputStream.write(b, o, c);
                liveOutputStream.flush();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the saveFormatedData function and writes a formatted data line into the continuous parsed output file
    private void saveFormatedData(String line) {
        try {
            // This line writes formatted text line to parsed stream taking insights from formatedOutputStream
            if (formatedOutputStream != null) {
                formatedOutputStream.write((line + "\n").getBytes());
                formatedOutputStream.flush();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the saveFormatedData1 function and writes a formatted RTK data line into the continuous parsed output file
    private void saveFormatedData1(String line) {
        try {
            // This line writes formatted text line to parsed stream taking insights from formatedOutputStream
            if (formatedOutputStream != null) {
                formatedOutputStream.write((line + "\n").getBytes());
                formatedOutputStream.flush();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the savePRData function and writes a formatted survey data line into the targeted profile output file
    private void savePRData(String line) {
        try {
            // This line writes formatted line to profile output stream taking insights from prOutputStream
            if (prOutputStream != null) {
                prOutputStream.write((line + "\n").getBytes());
                prOutputStream.flush();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the savePRData1 function and writes a formatted RTK survey data line into the targeted profile output file
    private void savePRData1(String line) {
        try {
            // This line writes formatted line to profile output stream taking insights from prOutputStream
            if (prOutputStream != null) {
                prOutputStream.write((line + "\n").getBytes());
                prOutputStream.flush();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the saveRawPRData function and writes an ASCII text line into the targeted profile raw output file
    private void saveRawPRData(String line) {
        try {
            // This line writes raw line to profile raw stream taking insights from prrawOutputStream
            if (prrawOutputStream != null) {
                prrawOutputStream.write((line + "\n").getBytes());
                prrawOutputStream.flush();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the saveRawPRData1 function and writes binary byte chunks into the targeted profile raw output file
    private void saveRawPRData1(byte[] b, int o, int c) {
        try {
            // This line writes binary byte chunk to profile raw stream taking insights from prrawOutputStream
            if (prrawOutputStream != null) {
                prrawOutputStream.write(b, o, c);
                prrawOutputStream.flush();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }

    // This is the alertStopConfirm function and displays confirmation dialog before stopping survey session and exiting
    private void alertStopConfirm() {
        // This line builds exit confirmation dialog taking insights from AlertDialog.Builder
        AlertDialog dia = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Stop Recording??")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // This line stops recording worker taking insights from stopRecording function
                    stopRecording();

                    try {
                        // This line closes live stream taking insights from liveOutputStream
                        if (liveOutputStream != null) {
                            liveOutputStream.close();
                        }
                        // This line closes formatted stream taking insights from formatedOutputStream
                        if (formatedOutputStream != null) {
                            formatedOutputStream.close();
                        }
                        // This line closes profile stream taking insights from prOutputStream
                        if (prOutputStream != null) {
                            prOutputStream.close();
                        }
                        // This line closes raw profile stream taking insights from prrawOutputStream
                        if (prrawOutputStream != null) {
                            prrawOutputStream.close();
                        }
                    } catch (Exception e) {
                        // This line prints exception stack trace taking insights from exception e
                        e.printStackTrace();
                    }

                    // This line terminates activity taking insights from finish()
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
        // This line sets green color on positive confirmation button taking insights from R.color.green
        dia.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.green));
        // This line sets red color on negative cancel button taking insights from R.color.red
        dia.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.red));
    }

    @Override
    // This is the onResume function and re-applies full-screen immersive window insets when activity resumes focus
    protected void onResume() {
        // This line calls parent onResume taking insights from super
        super.onResume();

        // This line creates window insets controller taking insights from window and decor view
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(
                        getWindow(),
                        getWindow().getDecorView()
                );

        // This line hides navigation bar bottom buttons taking insights from navigationBars type
        controller.hide(WindowInsetsCompat.Type.navigationBars());
    }

    // This is the destroy_on_configure function and terminates background worker and closes sockets before connection reconfiguration
    private boolean destroy_on_configure() {
        // This line sets stopWorker flag to true taking insights from true
        stopWorker = true;
        try {
            // This line closes live raw output stream taking insights from liveOutputStream
            if (liveOutputStream != null) {
                liveOutputStream.close();
                liveOutputStream = null;
            }

            // This line closes formatted output stream taking insights from formatedOutputStream
            if (formatedOutputStream != null) {
                formatedOutputStream.close();
                formatedOutputStream = null;
            }

            // This line closes profile output stream taking insights from prOutputStream
            if (prOutputStream != null) {
                prOutputStream.close();
                prOutputStream = null;
            }
            // This line closes raw profile output stream taking insights from prrawOutputStream
            if (prrawOutputStream != null) {
                prrawOutputStream.close();
                prrawOutputStream = null;
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }

        try {
            // This line closes Bluetooth socket taking insights from bluetoothSocket
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
        // This line returns true indicating cleanup completed taking insights from true
        return true;
    }

    @Override
    // This is the onDestroy function and performs final activity cleanup closing worker threads, streams, and Bluetooth socket
    protected void onDestroy() {
        // This line calls parent onDestroy taking insights from super
        super.onDestroy();
        // This line signals background threads to terminate taking insights from stopWorker flag
        stopWorker = true;
        try {
            // This line closes live raw output stream taking insights from liveOutputStream
            if (liveOutputStream != null) {
                liveOutputStream.close();
                liveOutputStream = null;
            }

            // This line closes formatted output stream taking insights from formatedOutputStream
            if (formatedOutputStream != null) {
                formatedOutputStream.close();
                formatedOutputStream = null;
            }

            // This line closes profile output stream taking insights from prOutputStream
            if (prOutputStream != null) {
                prOutputStream.close();
                prOutputStream = null;
            }
            // This line closes raw profile output stream taking insights from prrawOutputStream
            if (prrawOutputStream != null) {
                prrawOutputStream.close();
                prrawOutputStream = null;
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }

        try {
            // This line closes Bluetooth socket taking insights from bluetoothSocket
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (Exception e) {
            // This line prints exception stack trace taking insights from exception e
            e.printStackTrace();
        }
    }
}
