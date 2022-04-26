package com.example.taxiepic;

import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TCPClientThread extends Thread{

    int PUERTO;
    InetAddress IP;
    PrintWriter printWriter;
    Socket socketTCP;
    String ubicacion;
    DataOutputStream DataOS;


    public TCPClientThread(int puerto_servidor, String toString, InetAddress IPad) {
        super();
        PUERTO = puerto_servidor;
        ubicacion = toString;
        IP = IPad;

    }

    @Override
    public void run() {
        boolean running = true;
        try{

            socketTCP = new Socket(IP,PUERTO);
            printWriter = new PrintWriter(socketTCP.getOutputStream());
            printWriter.write(ubicacion);
            printWriter.flush();
            printWriter.close();
            socketTCP.close();

        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
