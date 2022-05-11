package com.example.taxiepic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    int PUERTO;
    String TimeVar;
    Handler handler = new Handler();
    UdpClientThread udpClientThread;
    private boolean Status = false;
    private final int delay = 5000;
    Switch ProtocolSwitch;
    InetAddress IPaddress;
    ToggleButton BtSend;
    android.widget.TextView Time, Coords, Funciona;
    String CoordendasTxt;
    Switch TaxiID;
    String ID;
    Button BTConnect;

    RPMCommand RPM = new RPMCommand();

    Handler BluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarEstadoBT();

        Funciona = (TextView) findViewById(R.id.Funciona);
        BTConnect = (Button) findViewById(R.id.BluetoothConnectTB);
        BtSend = (ToggleButton) findViewById(R.id.BtSend);
        Coords = (TextView) findViewById(R.id.Coords_TV);
        Time = (TextView) findViewById(R.id.hora_TV);
        TaxiID = (Switch) findViewById(R.id.TaxiSwitch);

        BTConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btSocket != null) {
                    try {
                        btSocket.close();
                    } catch (IOException e) {
                        Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_LONG).show();
                    }
                    finish();
                }
            }
        });

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

    private void initialConfigCommands() {
        try {
            new EchoOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
            new LineFeedOffCommand().run(btSocket.getInputStream(), btSocket.getOutputStream());
            new TimeoutCommand(125).run(btSocket.getInputStream(), btSocket.getOutputStream());
            new SelectProtocolCommand(ObdProtocols.AUTO).run(btSocket.getInputStream(), btSocket.getOutputStream());
        }catch (IOException | InterruptedException e) {
            Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_LONG).show();
        }
    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        }
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        address = intent.getStringExtra(DispositivosVinculados.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "No se pudo crear socket", Toast.LENGTH_LONG).show();
        }

        try {
            btSocket.connect();
        }catch (IOException e){
            try {
                btSocket.close();
            }catch (IOException e2){}
        }

    }

    protected void onPause(){
        super.onPause();
        try {
            btSocket.close();
        }catch (IOException e){}
    }

    private void VerificarEstadoBT() {

        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!btAdapter.isEnabled()) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                }else{
                    Intent enableBtintent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtintent, 1);
                }
            }
        }


    }

    private void GetLocation() {
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                CoordendasTxt = String.valueOf(location.getLatitude()) +", "+String.valueOf(location.getLongitude());
                Coords.setText(String.valueOf("Coordenadas: \n"+ CoordendasTxt));
                TimeVar = new java.text.SimpleDateFormat("yyyy/MM/dd, HH:mm:ss.SSS").format(location.getTime());
                Time.setText(String.valueOf("Hora: " + TimeVar));
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

    private String GetBTData(){
        try{
            while (!Thread.currentThread().isInterrupted()){
                initialConfigCommands();
                RPM.run(btSocket.getInputStream(), btSocket.getOutputStream());
                return RPM.getFormattedResult();
            }
        }catch (IOException | InterruptedException e) {
            Toast.makeText(getBaseContext(), "Error", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    public void Send_Data_UDP(){
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    String RPMVar = GetBTData().replace("RPM","");
                    Funciona.setText("RPM: " + RPMVar);
                    PUERTO = 10000;
                    if(TaxiID.isChecked()){
                        ID = "2";
                    }else{
                        ID = "1";
                    }

                    String Mensaje =  String.valueOf(CoordendasTxt + ", "+TimeVar + ", " + ID + ", " + RPMVar);
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