import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import main.RowSwapServer;

public class DiscoveryServer {

	/*
	 * DiscoveryServer portaDiscoveryServer
	 * nomefile1 port1... (tutte coppie di argomenti file e porta)
	 * nomefileN portN
	 */

	public static void main(String[] args) {
		System.out.println("DiscoveryServer: avviato");

		DatagramSocket socket = null;
		DatagramPacket packet = null;
		int port = -1;
		byte[] buf = new byte[256];
		// numero di thread : numero di coppie file-porta passati in args
		RowSwapServer[] rowServers = new RowSwapServer[(args.length - 1) / 2];

		// controllo argomenti
		if (args.length >= 3 && (args.length % 2) == 1) { // numero di argomenti deve essere dispari
			try {
				port = Integer.parseInt(args[0]); // porta del DiscoveryServer
			} catch (NumberFormatException e) {
				System.out.println("Wrong port. Use format:[65535<serverPort>1024]");
			}

			if (port < 1024 || port > 65535) {
				System.out.println("Wrong port. Use format:[65535<serverPort>1024]");
				System.exit(1);
			}

			// controllo unicità nomeFile e porta + controllo formato (utilizzando args[])
			try {
				for (int i = 1; i < args.length - 1; i++) {
					if (i % 2 == 1) { // dispari -> nomefile
						for (int j = 1; j < args.length; j += 2) { // controllo che il nomefile non ci sia gia
							if (j != i) // controllo argomenti tranne se stesso
								if (args[i].equals(args[j])) {
									System.out.println("Errore : nomeFile gia inserito!");
									System.exit(2);
								}
						}
					}
					if (i % 2 == 0) { // pari -> numeroPorta
						for (int j = 0; j < args.length; j += 2) {
							int check1 = Integer.parseInt(args[i]);
							if (j != i) { // controllo argomenti tranne se stesso

								int check2 = Integer.parseInt(args[j]);
								// controllo validita porte passate
								if (check1 < 1024 || check1 > 65535 || check2 < 1024 || check2 > 65535) {
									System.out.println("Wrong port. Use format:[65535<serverPort>1024]");
									System.exit(3);
								}
								if (check1 == check2) {
									System.out.println("Errore : numero di porta gia inserito!");
									System.exit(4);
								}

							}
						}
					}
				}
			} catch (NumberFormatException e) {
				System.out.println("Wrong port. Use format:[65535<serverPort>1024]");
				System.exit(5);
			}

		} // fine controllo
		else {
			System.out.println("Numero argomenti non corretto!");
			System.exit(6);
		}

		// creo i processi figli:
		int j = 0;
		for (int i = 1; i < args.length - 1; i += 2) {
			try {
				rowServers[j] = new RowSwapServer(Integer.parseInt(args[i + 1]), args[i]);
				rowServers[j].start();
				j++;
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			socket = new DatagramSocket(port);
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("Creata la socket: " + socket);
		} catch (SocketException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.exit(7);
		}

		try {

			String nomeFile = null;
			int numLinea = -1;
			String nomeFileRichiesto = null;
			ByteArrayInputStream biStream = null;
			DataInputStream diStream = null;
			ByteArrayOutputStream boStream = null;
			DataOutputStream doStream = null;
			String portaDaRitornare = "";
			byte[] data = null;

			while (true) {
				System.out.println("\nIn attesa di richieste...");

				try {
					packet.setData(buf);
					socket.receive(packet);
				} catch (IOException e) {
					System.err.println("Problemi nella ricezione del datagramma: "
							+ e.getMessage());
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}
				try {
					biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
					diStream = new DataInputStream(biStream);
					nomeFileRichiesto = diStream.readUTF();
					System.out.println("Richiesta file " + nomeFileRichiesto);
				} catch (Exception e) {
					System.err.println("Problemi nella lettura della richiesta: "
							+ nomeFile + " " + numLinea);
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}

				try {
					// trovare e mandare la porta giusta:
					for (int i = 1; i < args.length; i += 2) {
						if (args[i].equals(nomeFileRichiesto)) {
							portaDaRitornare = args[i + 1];
						}
					}

					if (portaDaRitornare.isBlank()) {
						portaDaRitornare = "File non trovato";
					}

					boStream = new ByteArrayOutputStream();
					doStream = new DataOutputStream(boStream);
					doStream.writeUTF(portaDaRitornare);
					data = boStream.toByteArray();
					packet.setData(data, 0, data.length);
					socket.send(packet);
				} catch (IOException e) {
					System.err.println("Problemi nell'invio della risposta: "
							+ e.getMessage());
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}

			} // fine di while

			// qui catturo le eccezioni non catturate all'interno del while
			// in seguito alle quali il server termina l'esecuzione
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("LineServer: termino...");
		socket.close();
	}

}
