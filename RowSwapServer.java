package main;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.*;
import java.util.StringTokenizer;

public class RowSwapServer extends Thread{
	private String fileName;
	private DatagramSocket socket = null;
	private DatagramPacket packet = null;
	byte[] buf = new byte[256];
	private int numLinea1;
	private int numLinea2;
	private String line1;
	private String line2;
	private String lineToWrite;
	private PrintWriter f;
	private BufferedReader in;
	private ByteArrayOutputStream boStream;
	private DataOutputStream doStream;
	private byte[] data=null;
	public RowSwapServer(int port, String fileName) throws SocketException {
		this.fileName = fileName;
		socket = new DatagramSocket(port);
		packet = new DatagramPacket(buf, buf.length);
		System.out.println("RSS: " + this.getId()+" socket creta con successo" + socket);
		}
	
	public void run() {
		while(true) {
		try {
			packet.setData(buf);
			socket.receive(packet);
		}
		catch (IOException e) {
			System.err.println("Problemi nella ricezione del datagramma: "
					+ e.getMessage());
			e.printStackTrace();
			continue;
			// il server continua a fornire il servizio ricominciando dall'inizio
			// del ciclo
		}
		try {
			ByteArrayInputStream biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
			DataInputStream diStream = new DataInputStream(biStream);
			String richiesta = diStream.readUTF();
			StringTokenizer st = new StringTokenizer(richiesta);
			numLinea1 = Integer.parseInt(st.nextToken());
			numLinea2 = Integer.parseInt(st.nextToken());
			if(numLinea1>numLinea2) {
				int temp=numLinea1;
				numLinea1=numLinea2;
				numLinea1=temp;
			}
		}catch (Exception e) {
			System.err.println("Problemi nella lettura della richiesta: ");
				e.printStackTrace();
				continue;
				// il server continua a fornire il servizio ricominciando dall'inizio
				// del ciclo
			}
		try {
			line1=LineUtility.getLine(fileName, numLinea1);			
			line2=LineUtility.getLine(fileName, numLinea2);
			if(line1.contains("Linea non trovata") || line2.contains("Linea non trovata")) {
				System.out.println("Failure");
				boStream = new ByteArrayOutputStream();
				doStream = new DataOutputStream(boStream);
				doStream.writeUTF("Failure");
				data = boStream.toByteArray();
				packet.setData(data, 0, data.length);
				socket.send(packet);
				continue;
			}
			File support = new File("support.txt");
			f = new PrintWriter(support);
						try {
				in = new BufferedReader(new FileReader(fileName));
			} catch (FileNotFoundException e) {
				System.out.println("File non trovato: ");
				e.printStackTrace();
			}
			int i=1;
			do {
				lineToWrite = LineUtility.getNextLine(in);
				if(i==numLinea1) {
					f.println(line2);
				}
				else if(i==numLinea2) {
					f.println(line1);
				}
				else if(lineToWrite!="Nessuna linea disponibile")
					f.println(lineToWrite);
				i++;
				}while(lineToWrite!="Nessuna linea disponibile");
			f.flush();
			File dest = new File(fileName);
			support.renameTo(dest);
			System.out.println("Success");
			boStream = new ByteArrayOutputStream();
			doStream = new DataOutputStream(boStream);
			doStream.writeUTF("Success");
			data = boStream.toByteArray();
			packet.setData(data, 0, data.length);
			socket.send(packet);
			
		}catch(Exception e) {
			continue;
		}
		}
		
		
	}
	

}

