package com.example.taxiepic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    int PUERTO;
    String TimeVar;
    Handler handler = new Handler();
    UdpClientThread udpClientThread;
    TCPClientThread tcpClientThread;
    private boolean Status = false;
    private final int delay = 5000;
    Switch ProtocolSwitch;
    InetAddress IPaddress;
    ToggleButton BtSend;
    android.widget.TextView Time, Coords;
    String CoordendasTxt;
    Switch TaxiID;
    String ID;

    BluetoothHeadset bluetoothHeadset;

    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BtSend = (ToggleButton) findViewById(R.id.BtSend);
        Coords = (TextView) findViewById(R.id.Coords_TV);
        Time = (TextView) findViewById(R.id.Hora_TV);
        TaxiID = (Switch) findViewById(R.id.TaxiSwitch);
        BtSend.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                GetLocation();
                if (CoordendasTxt != null) {
                    if (BtSend.isChecked()) {
                        Status = true;
                    } else {
                        Status = false;
                    }
                    Send_Data_UDP();
                } else {
                    BtSend.setChecked(false);
                    Toast.makeText(MainActivity.this, "No hay coordenadas para enviar", Toast.LENGTH_SHORT).show();
                }
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.INTERNET}, 1000);
            }
        });

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            GetLocation();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }

    private void GetLocation() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                CoordendasTxt = String.valueOf(location.getLatitude()) +", "+String.valueOf(location.getLongitude());
                Coords.setText(String.valueOf("Coordenadas: \n"+ CoordendasTxt));
                TimeVar = new java.text.SimpleDateFormat("yyyy/MM/dd, HH:mm:ss.SSS").format(location.getTime());
                Time.setText(String.valueOf("Hora: \n" + TimeVar));
            }
        };
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            long MIN_TIME = 1000;
            long MIN_DIST = 5;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DIST, locationListener);
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    public void Send_Data_UDP(){
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    PUERTO = 10000;
                    if(TaxiID.isChecked()){
                        ID = "2";
                    }else{
                        ID = "1";
                    }
                    String Mensaje =  String.valueOf(CoordendasTxt + ", "+TimeVar + ", " + ID);

                    IPaddress = InetAddress.getByName("18.223.199.20");
                    udpClientThread = new UdpClientThread(PUERTO, Mensaje, IPaddress);
                    udpClientThread.start();

                    IPaddress = InetAddress.getByName("52.71.214.41");
                    udpClientThread = new UdpClientThread(PUERTO, Mensaje, IPaddress);
                    udpClientThread.start();

                    IPaddress = InetAddress.getByName("34.200.4.183");
                    udpClientThread = new UdpClientThread(PUERTO, Mensaje, IPaddress);
                    udpClientThread.start();

                    IPaddress = InetAddress.getByName("34.235.195.32");
                    udpClientThread = new UdpClientThread(PUERTO, Mensaje, IPaddress);
                    udpClientThread.start();

                    if(Status) {
                        handler.postDelayed(this, delay);
                        Toast.makeText(MainActivity.this, "Enviando", Toast.LENGTH_SHORT).show();
                    } else {
                        handler.getLooper();
                    }

                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }

            }}
            , delay);
    }

}