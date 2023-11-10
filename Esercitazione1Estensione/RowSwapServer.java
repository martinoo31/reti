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

public class RowSwapServer {

	// RS IPDS portDS portRS nomeFile
	public static void main(String args[]) {
		InetAddress IPDS = null;
		int DSPort = -1;
		int RSPort = -1;
		int numLinea1;
		int numLinea2;
		String line1;
		String line2;
		String lineToWrite;
		PrintWriter f;
		BufferedReader in = null;
		String nomeFile = "";
		File FileDaSwappare = null;
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] buf = new byte[256];
		ByteArrayOutputStream boStream;
		DataOutputStream doStream;
		byte[] data = null;
		int TentativiRegistrazione;
		String risposta = "";
		ByteArrayInputStream biStream = null;
		DataInputStream diStream = null;
		int numLines = 0;

		// CONTROLLO ARGOMENTI
		if (args.length != 4) {
			System.out.println("Numero di argomenti errato: RS IPDS portDS portRS nomeFile ");
			System.exit(1);
		} else {
			try {
				IPDS = InetAddress.getByName(args[0]);
				DSPort = Integer.parseInt(args[1]);
				if (DSPort < 1024 || DSPort > 65536) {
					System.out.println("RSClient: 1024<DSPort<65536. Termino");
					System.exit(1);
				}
				RSPort = Integer.parseInt(args[2]);
				if (RSPort < 1024 || RSPort > 65536) {
					System.out.println("RSClient: 1024<RSPort<65536. Termino");
					System.exit(1);
				}
				nomeFile = args[3];
				FileDaSwappare = new File(nomeFile);
				if (!FileDaSwappare.exists()) {
					System.out.println("RSClient: " + nomeFile + " non esiste. Termino");
					System.exit(1);
				}
			} catch (Exception e) {
				System.out.println("RSClient: Errore nella lettura argomenti. Termino");
				System.exit(1);
			}
		}
		//Creo gli stream per la creazione dei pacchetti
		boStream = new ByteArrayOutputStream();
		doStream = new DataOutputStream(boStream);
		
		// REGISTRAZIONE presso il DS, vengono fatti 3 tentativi di contatto, dopo il
		// terzo termina l'applicazione
		try {
			socket = new DatagramSocket(RSPort);
			packet = new DatagramPacket(buf, buf.length, IPDS, DSPort);
			boStream.reset();
			doStream.writeUTF("REG " + nomeFile);
			data = boStream.toByteArray();
			TentativiRegistrazione = 0;
			socket.setSoTimeout(10000);
			do {
				packet.setData(data);
				socket.send(packet);
				System.out.println("Richiesta inviata a " + IPDS + ", " + DSPort);
				try {
					packet.setData(buf);
					socket.receive(packet);
					biStream = new ByteArrayInputStream(packet.getData());
					diStream = new DataInputStream(biStream);
					risposta = diStream.readUTF();
				} catch (SocketTimeoutException se) {
					System.out.println("RSSwap: tentativo di registrazione fallito causa timeout");
				} catch (IOException e) {
					e.printStackTrace();
				}
				TentativiRegistrazione++;
			} while (!risposta.equals("Successo") && TentativiRegistrazione < 3 && risposta.equals("Fallimento"));
			if (risposta.equals("Fallimento")) {
				System.out.println("RSSwap: registrazione fallita, collisione nel DS. Termino");
				socket.close();
				System.exit(1);
			} else if (risposta.equals("Successo")) {
				System.out.println("RSClient: Registrazione avvenuta con " + risposta);
			} else {
				System.out.println("RSSwap: registrazione fallita, tentativi scaduti. Termino");
				System.exit(1);
			}
		} catch (Exception e) {
			System.out.println("Errore nella registrazione");
			e.printStackTrace();
		}

//CALCOLO DIMENSIONE FILE ----------------------------------------------------
		try {
			BufferedReader reader = new BufferedReader(new FileReader(nomeFile));
			while (reader.readLine() != null)
				numLines++;
			reader.close();
		} catch (IOException e) {
			System.err.println("Errore nel calcolo della dimensione del file");
			e.printStackTrace();
		}

//DEMONE RowSwap -------------------------------------------------------------
		while (true) {
			packet.setData(buf);
			try {
				socket.setSoTimeout(0);
			} catch (SocketException e) {
				e.printStackTrace();
				continue;
			}
			try {
				packet.setData(buf);
				socket.receive(packet);
			} catch (IOException e) {
				System.err.println("Problemi nella ricezione del datagramma: " + e.getMessage());
				e.printStackTrace();
				continue;
				// il server continua a fornire il servizio ricominciando dall'inizio
				// del ciclo
			}
			try {
				biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
				diStream = new DataInputStream(biStream);
				String richiesta = diStream.readUTF();
//PROCEDURA DI TERMINAZIONE ----------------------------------------------------
				if (richiesta.equals("KILL")) {
					if (!packet.getAddress().getHostAddress().equals("127.0.0.1")) {
						System.out.println("RSSwap: ricevuto pacchetto di terminazione malizioso!!");
						continue;
					}
					/*
					 * Dopo aver controllato che il pacchetto arrivi effettivamente dal RSKiller
					 * attraverso, l'indirizzo di loopback termino. Per terminare mando un pacchetto
					 * di deregistrazione al DS e aspetto la sua risposta, se non ricevo la sua
					 * risposta entro 3 tentativi termino
					 */
					else {
						System.out.println("RSSwap: ricevuto pacchetto di terminazione");
						packet = new DatagramPacket(buf, buf.length, IPDS, DSPort);
						packet = new DatagramPacket(buf, buf.length, IPDS, DSPort);
						boStream.reset();
						doStream.writeUTF("DEL");
						data = boStream.toByteArray();
						TentativiRegistrazione = 0;
						socket.setSoTimeout(10000);
						do {
							packet.setData(data);
							socket.send(packet);
							System.out.println("RSSwap: invio pacchetto di deregistrazione");
							try {
								packet.setData(buf);
								socket.receive(packet);
								biStream = new ByteArrayInputStream(packet.getData());
								diStream = new DataInputStream(biStream);
								risposta = diStream.readUTF();
							} catch (SocketTimeoutException se) {
								System.out.println("RSSwap: tentativo di deregistrazione fallito causa timeout");
							} catch (IOException e) {
								e.printStackTrace();
							}
						} while (!risposta.equals("Successo") && TentativiRegistrazione < 3);
						if (risposta.equals("Successo")) {
							System.out.println("RSSWap: deregistrazione avvenuta con successo. Termino");
							socket.close();
							System.exit(0);
						} else {
							System.out.println("RSSwap: deregistrazione fallita. Termino");
							socket.close();
							System.exit(1);
						}

					}
				} // Fine procedura di terminazione

//PROCEDURA SCAMBIO RIGHE ------------------------------------------------------

				StringTokenizer st = new StringTokenizer(richiesta);
				numLinea1 = Integer.parseInt(st.nextToken());
				numLinea2 = Integer.parseInt(st.nextToken());
				if (numLinea1 > numLinea2) {
					int temp = numLinea1;
					numLinea1 = numLinea2;
					numLinea1 = temp;
				}
				// Controllo se le righe da scambiare sono all'interno del file, in caso
				// contrario comunico il fallimento
				if (numLinea2 > numLines || numLinea1 <= 0) {
					boStream.reset();
					doStream.writeUTF("Failure");
					data = boStream.toByteArray();
					packet.setData(data, 0, data.length);
					socket.send(packet);
					continue;

				}
			} catch (Exception e) {
				System.err.println("Problemi nella lettura della richiesta: ");
				e.printStackTrace();
				continue;
				// il server continua a fornire il servizio ricominciando dall'inizio
				// del ciclo
			}
			try {
				// Estraggo dal file le linee da scambiare ed esegue un ulteriore controllo
				// sulla loro esistenza
				line1 = LineUtility.getLine(nomeFile, numLinea1);
				line2 = LineUtility.getLine(nomeFile, numLinea2);
				if (line1.contains("Linea non trovata") || line2.contains("Linea non trovata")) {
					System.out.println("Failure");
					boStream.reset();
					doStream.writeUTF("Failure");
					data = boStream.toByteArray();
					packet.setData(data, 0, data.length);
					socket.send(packet);
					continue;
				}
				// Creo file di supporto su cui scambiare righe
				File support = new File("support.txt");
				f = new PrintWriter(support);
				try {
					in = new BufferedReader(new FileReader(nomeFile));
				} catch (FileNotFoundException e) {
					System.out.println("File non trovato: ");
					e.printStackTrace();
				}
				int i = 1;
				// Ciclo di copiatura su file di supporto, con annesso scambio righe
				do {
					lineToWrite = LineUtility.getNextLine(in);
					if (i == numLinea1) {
						f.println(line2);
					} else if (i == numLinea2) {
						f.println(line1);
					} else if (lineToWrite != "Nessuna linea disponibile")
						f.println(lineToWrite);
					i++;
				} while (lineToWrite != "Nessuna linea disponibile");
				f.flush();
				// Rinomino nome file e comunico il successo al client
				File dest = new File(nomeFile);
				support.renameTo(dest);
				System.out.println("Successo");
				boStream.reset();
				doStream.writeUTF("Successo");
				data = boStream.toByteArray();
				packet.setData(data, 0, data.length);
				socket.send(packet);

			} catch (Exception e) {
				e.printStackTrace();
				continue;

			} // Fine Procedure Scambio Righe
		} // While

	}

}
