package com.example.taxiepic;

import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class UdpClientThread extends Thread{
    int dstPort;
    String ubicacion;

    DatagramSocket socket;
    InetAddress address;
    InetAddress IP;

    public UdpClientThread(int puerto_servidor, String toString, InetAddress IPad) {
        super();
        dstPort = puerto_servidor;
        ubicacion = toString;
        IP = IPad;

    }




    @Override
    public void run() {
        boolean running = true;


        try {
            socket = new DatagramSocket();
            address= IP;
            String mensaje = ubicacion;

            byte[] buffer = mensaje.getBytes();

            DatagramPacket Envio = new DatagramPacket(buffer, buffer.length, address, dstPort);

            socket.send(Envio);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
            }
        }

    }
}
