package main;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class RSKiller {
		//RSKiller RSPort
	public static void main(String[] args) {
		int RSPort=0;
		InetAddress lbAddress=null;
		DatagramSocket socket=null;
		DatagramPacket packet=null;
		byte[] buf=new byte[128];
		ByteArrayOutputStream boStream=null;
		DataOutputStream doStream=null;
		byte[] data=null;
		
		if(args.length!=1) {
			System.out.println("Numero argomenti errato: RSKiller RSPort");
			System.exit(1);
		}
		try {
		RSPort = Integer.parseInt(args[0]);
		if(RSPort <1024 || RSPort >65536) {
			System.out.println("RSKiller: 1024<RSPort<65536. Termino");
			System.exit(1);
		}
		}catch(NumberFormatException e) {
			System.out.println("Impossibile riconoscere RSPort. Termino");
			e.printStackTrace();
			System.exit(1);
		}
		//Creo la socket e preparo il pacchetto
		try {
			
			socket = new DatagramSocket();
			
		} catch (Exception e) {
			System.out.println("Problemi nella preparazione della socket. Termino");
			e.printStackTrace();
			System.exit(1);
		}
		//Scrivo il messaggio KILL
		try {
			lbAddress=InetAddress.getByName("127.0.0.1");
			packet = new DatagramPacket(buf, buf.length,lbAddress,RSPort);
			boStream = new ByteArrayOutputStream();
			doStream = new DataOutputStream(boStream);
			doStream.writeUTF("KILL");
			data = boStream.toByteArray();
			packet.setData(data);
		}catch(Exception e) {
			System.out.println("Errori nella creazione del pacchetto. Termino");
			e.printStackTrace();
			System.exit(1);
		}
		
		//Invio il messaggio KILL
		try {
			socket.send(packet);
			System.out.println("Pacchetto di Terminazione inviato con successo. Termino");
		}catch(Exception e) {
			System.out.println("Errori nell'invio del pacchetto termino. Termino");
			e.printStackTrace();
			System.exit(1);
		}

	}

}
