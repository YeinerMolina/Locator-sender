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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    Handler BluetoothIn;
    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    private ConnectedThread MyConextionBT;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothIn = new Handler() {
            public void handlerMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String MyCharacter = (String) msg.obj;
                    Funciona.setText(MyCharacter);
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarEstadoBT();

        Funciona = (TextView) findViewById(R.id.Funciona);
        BTConnect = (Button) findViewById(R.id.BluetoothConnectTB);
        BtSend = (ToggleButton) findViewById(R.id.BtSend);
        Coords = (TextView) findViewById(R.id.Coords_TV);
        Time = (TextView) findViewById(R.id.Hora_TV);
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
        MyConextionBT = new ConnectedThread(btSocket);
        MyConextionBT.start();

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

    private void GetBTData(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }else{
            MyConextionBT.write("010D");
        }
    }

    public void Send_Data_UDP(){
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    GetBTData();
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


    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket){
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }catch (IOException e){}
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] byte_in = new byte[1];
            while(true){
                try{
                    mmInStream.read(byte_in);
                    char ch = (char) byte_in[0];
                    BluetoothIn.obtainMessage(handlerState, ch).sendToTarget();
                }catch (IOException e){
                    break;
                }
            }
        }

        public void write(String input){
            try {
                mmOutStream.write(input.getBytes());
            }catch (IOException e2){
                Toast.makeText(getBaseContext(),"La conexión falló", Toast.LENGTH_LONG).show();
                finish();
            }
        }

    }


}