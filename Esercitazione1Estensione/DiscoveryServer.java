package main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

public class DiscoveryServer {

	/*
	 * DiscoveryServer portaDiscoveryServer
	 */

	public static void main(String[] args) {
		System.out.println("DiscoveryServer: avviato");

		DatagramSocket socket = null;
		DatagramPacket packet = null;
		StringJoiner SJ = null;
		int port = -1;
		int portaSwapServer;
		byte[] buf = new byte[256];
		List<DSEntry> files = null;
		ByteArrayInputStream biStream = null;
		DataInputStream diStream = null;
		ByteArrayOutputStream boStream = null;
		DataOutputStream doStream = null;
		String packetPayload;
		String[] splitPayload;
		byte[] data = null;
		int collisioni;
		boolean giaRegistratoRS = false;

		// Controllo argomenti --------------------------------------------------
		if (args.length == 1) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.out.print("Usage: java DiscoveryServer [serverPort>1024] ...");
				System.exit(1);
			}

			if (port < 1024 || port > 65535) {
				System.out.println("Usage: java DiscoveryServer [serverPort>1024] ...");
				System.exit(1);
			}
		}

		else {
			System.out.println("Numero argomenti non corretto!");
			System.exit(1);
		}

		// creo la lista per memorizzare i RowSwapServer
		files = new ArrayList<DSEntry>();
		/*
		 * Formattazione Pacchetti
		 * Pacchetto Registrazione RS: REG nomefileDaRegistrare (Ip e porta vengono estratti dal pacchetto) 
		 * Pacchetto Deregistrazione: DEL 
		 * Pacchetto Richiesta Catalogo: CAT 
		 * Pacchetto Richesta Indirizzo RS: IRS nomefileRichiesto
		 */

		// Creazione socket
		try {
			socket = new DatagramSocket(port);
			packet = new DatagramPacket(buf, buf.length);
			System.out.println("Creata la socket: " + socket);
		} catch (SocketException e) {
			System.out.println("Problemi nella creazione della socket: ");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Socket Aperta...");
		
		//Creo lo stream di output per la creazione dei pacchetti
		boStream = new ByteArrayOutputStream();
		doStream = new DataOutputStream(boStream);

//DEMONE DISCOVERY SERVER ----------------------------------------------------------		
		while (true) {
			try {
				// Ricezione pacchetto
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
				// VALIDAZIONE PACCHETTO DATAGRAM ----------------------------------
				try {
					biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
					diStream = new DataInputStream(biStream);
					packetPayload = diStream.readUTF();
					splitPayload = packetPayload.split(" ");
					// Questo switch controlla che il payload del pacchetto sia conforme al
					// protocollo
					switch (splitPayload[0]) {
					case "REG": {
						if (splitPayload.length != 2) {
							System.out.println("Ricevuto pacchetto REG invalido");
							continue;
						} else {
							System.out.println("Ricevuto pacchetto REG valido");
							break;
						}
					}
					case "DEL": {

						if (splitPayload.length != 1) {
							System.out.println("Ricevuto pacchetto DEL invalido");
							continue;
						} else {
							System.out.println("Ricevuto pacchetto DEL valido");
							break;
						}
					}
					case "CAT": {

						if (splitPayload.length != 1) {
							System.out.println("Ricevuto pacchetto CAT invalido");
							continue;
						} else {
							System.out.println("Ricevuto pacchetto CAT valido");
							break;
						}

					}
					case "IRS": {
						
						if (splitPayload.length != 2) {
							System.out.println("Ricevuto pacchetto IRS invalido");
							continue;
							}
						else {
							System.out.println("Ricevuto pacchetto IRS valido");
							break;}
					}
					default: {
						System.out.println("DS: scarto pacchetto invalido");
						continue;
					}
					}

				} catch (Exception e) {
					System.err.println("Problemi nella lettura della richiesta");
					e.printStackTrace();
					continue;
					// il server continua a fornire il servizio ricominciando dall'inizio
					// del ciclo
				}
				// REGISTRAZIONE RowSwapServer ----------------------------------------
				if (splitPayload[0].equals("REG")) {
					String nomeFile = splitPayload[1];
					String ip = packet.getAddress().getHostAddress();
					portaSwapServer = packet.getPort();
					collisioni = 0;
					/*Controllo che non ci sia un file con lo stesso nome già registrato 
					 * oppure un file con lo stesso endpoin
					 * */
					for (DSEntry entry : files) {
						if (entry.getFile().equals(nomeFile)
								|| entry.getIp().equals(ip) && entry.getPort() == portaSwapServer) {
							if (entry.getFile().equals(nomeFile) && entry.getIp().equals(ip)
									&& entry.getPort() == portaSwapServer) {
								giaRegistratoRS = true;
							} else {
								collisioni++;
							}
							break;
						}
					}
					/* Se nomeFile e endpoint combaciano con quelli già registrati probabilmente il
					 * pacchetto di conferma è andato perso. Il DS effettua quindi una ritrasmissione
					 * della avvenuta registrazione
					 */

					if (giaRegistratoRS) {
						boStream.reset();
						doStream.writeUTF("Successo");
						data = boStream.toByteArray();
						packet.setData(data, 0, data.length);
						socket.send(packet);
						giaRegistratoRS = false;
						continue;

					}
					//Se c'è una collissione comunico il fallimento al RSServer
					if (collisioni != 0) {
						boStream.reset();
						doStream.writeUTF("Fallimento");
						data = boStream.toByteArray();
						packet.setData(data, 0, data.length);
						socket.send(packet);
						continue;
					} 
					// Nessuna collissione comunico il successo della registrazione
					else { 
						System.out.println("E' avvenuta una registrazione");
						files.add(new DSEntry(nomeFile, ip, portaSwapServer));
						boStream.reset();
						doStream.writeUTF("Successo");
						data = boStream.toByteArray();
						packet.setData(data, 0, data.length);
						socket.send(packet);
						continue;
					}
				}
// DEREGISTRAZIONE RowSwapServer -------------------------------------
				else if (splitPayload[0].equals("DEL")) {
					//Dal pacchetto estraggo l'endpoint che identifica univocamente il file da rimuovere
					String ip = packet.getAddress().getHostAddress();
					int porta = packet.getPort();
					DSEntry temp = null;
					for (DSEntry file : files) {
						if (file.getIp().equals(ip) && file.getPort() == porta) {
							temp = file;
							break;
						}
					}
					//Se il file esiste comunico la deregistrazione
					if (temp != null) {
						files.remove(temp);
						System.out.println("Deregistrazione avvenuta");
						boStream.reset();
						doStream.writeUTF("Successo");
						data = boStream.toByteArray();
						packet.setData(data, 0, data.length);
						socket.send(packet);
					}
					
					continue;
				}
// RICHIESTA DETERMINATO FILE-----------------------------------------------------
//  non richiesta dalle specifiche ma implementata per una futura estensione
				else if (splitPayload[0].equals("IRS")) {
					String nomeFile = splitPayload[1];
					for (DSEntry entry : files) {
						if (entry.getFile() == splitPayload[1]) {
							boStream.reset();
							doStream.writeUTF(entry.getEndpoint());
							data = boStream.toByteArray();
							packet.setData(data, 0, data.length);
							socket.send(packet);
							break;
						}
					}
					continue;

				}
// RICHIESTA CATALOGO -------------------------------------------
				/*Il catalogo e una serie di stringhe con il formato: 
				 * "fileName ip port" separate da ';'
				 * */
				else {
					boStream.reset();
					SJ = new StringJoiner(";");
					for (DSEntry file : files) {
						SJ.add(file.toString());
					}
					doStream.writeUTF(SJ.toString());
					data = boStream.toByteArray();
					packet.setData(data, 0, data.length);
					socket.send(packet);
					continue;

				}
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

		}
	}

}
/*
 * Modifiche da fare L'ip e la porta del RSServer anche nelle richieste di
 * registrazione possono esssere dedotte dal pacchetto Caso perdita pacchetto di
 * conferma registrazione avvenuta
 */
