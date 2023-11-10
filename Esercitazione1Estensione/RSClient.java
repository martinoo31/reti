package main;

import java.io.*;
import java.net.*;
import java.util.*;

public class RSClient {

	// RSClient IPDS portDSs
	public static void main(String args[]) {
		InetAddress adr = null;
		int port = -1;
		String fileName = null;
		String[] catalogoString = null;
		ArrayList<DSEntry> catalogo;
		String[] entryString;
		String rd = "";
		StringTokenizer tk = null;
		int riga1, riga2;
		String richiesta = "";
		DatagramSocket socket = null;
		DatagramPacket packet = null;
		byte[] buf = new byte[256];

		// CONTROLLO ARGOMENTI ---------------------------------------------
		try {
			if (args.length == 2) {
				adr = InetAddress.getByName(args[0]);
				port = Integer.parseInt(args[1]);
			} else {
				System.out.println("Usage : RSClient IPDS portDS");
				System.exit(1);
			}
		} catch (UnknownHostException e) {
			System.out.println("RSClient: Errore creazione argomenti :");
			e.printStackTrace();
			System.exit(2);
		}

		// CREAZIONE SOCKET -----------------------------------------------
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(30000);
			packet = new DatagramPacket(buf, buf.length, adr, port);
			System.out.println("RSClient: socket(" + socket + ") e datagram creati");
		} catch (IOException e) {
			System.out.println("RSClient: Errore creazione socket/datagram : ");
			e.printStackTrace();
			System.exit(3);
		}
		ByteArrayOutputStream boStream = new ByteArrayOutputStream();
		DataOutputStream doStream = new DataOutputStream(boStream);
		// ATTIVO FILTRO CLIENTE -----------------------------------------------
			do {

				// RICHIESTA CATALOGO al DS --------------------------------------
				
				try {
					boStream.reset();
					doStream.writeUTF("CAT");
				} catch (IOException e) {

					e.printStackTrace();
				}
				byte[] data = boStream.toByteArray();
				packet = new DatagramPacket(buf, buf.length, adr, port);
				packet.setData(data);
				try {
					socket.send(packet);
				} catch (IOException e) {

					e.printStackTrace();
				}

				// RICEZIONE CATALOGO ------------------------------------------
				packet.setData(buf);
				try {
					socket.setSoTimeout(30000);
				} catch (SocketException e) {
					System.out.println("RSClient: Impossibile comunicare con il DS. Termino");
					System.exit(1);
				}
				try {
					socket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				//Leggo la risposta del DS
				String risposta = null;
				ByteArrayInputStream biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
				DataInputStream diStream = new DataInputStream(biStream);
				try {
					risposta = diStream.readUTF();
					if (risposta.isEmpty()) {
						System.out.println(
								"RSClient: Il DiscoveryServer non ha nessuna registrazione. Termino");
						System.exit(0);
					}
					catalogoString = risposta.split(";");

				} catch (IOException e) {

					e.printStackTrace();
				}

				// Mostra del catalogo all'utente e creazione del database dei file
				System.out.println("RSClient: Questo è il catalogo ricevuto dal Discovery Server:");
				catalogo = new ArrayList<DSEntry>();
				for (int i = 0; i < catalogoString.length; i++) {
					entryString = catalogoString[i].split(" ");
					if (entryString.length == 3) {
						System.out.println(entryString[0]);
						catalogo.add(new DSEntry(entryString[0], entryString[1], Integer.parseInt(entryString[2])));
					}
				}
				// Richiesta file da modificare da parte dell'utente
				System.out.println("RSClient: Inserire file da modificare oppure terminare chiudendo l'input");
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
				
				boolean flag = false;
				DSEntry temp = null;

				try {

					

					do {
						//Lettura nome file da modificare
						fileName = stdIn.readLine();
						if (fileName != null) {
							//Cerco il file da modificare all'interno del catalogo
							for (DSEntry entry : catalogo) {
								if (entry.getFile().equals(fileName)) {
									flag = true;
									temp = entry;
									break;
								}
							}
						}
						//Comunico eventuale errore
						if (!flag && fileName != null)
							System.out.println("RSClient: Il file inserito non esiste nel catalogo,"
									+ " riprovare oppure terminare chiudendo l'input");
					} while (fileName != null && !flag);
					//L'utente ha chiuso l'input termino
					if (fileName == null) {
						socket.close();
						System.out.println("RSClient: Termino");
						System.exit(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
				// Il file selezionato dall'utente esiste
				//CREAZIONE PACCHETTO PER ROW SWAP SERVER ---------------------------------------------
				try {
					packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(temp.getIp()), temp.getPort());
				} catch (UnknownHostException e) {
					System.out.println("RSClient: indirizzo RS errato. Termino");
					e.printStackTrace();
					System.exit(1);
				}
				
				try {
					//INTERAZIONE CON L'UTENTE PER RICHIEDERE LE RIGHE -------------------------------------------------
					System.out.println("RSClient: Inserisci righe da scambiare di " 
					+ fileName + " oppure R per cambiare file, EOF per terminare");
					while ((rd = stdIn.readLine()) != null) {

						// interazione utente
						if (rd.equals("R")) {
							break;
						}
						tk = new StringTokenizer(rd);
						riga1 = Integer.parseInt(tk.nextToken());
						riga2 = Integer.parseInt(tk.nextToken());
						richiesta = riga1 + " " + riga2;

						// Preparazione e invio pacchetto con righe da scambiare
						boStream.reset();
						doStream.writeUTF(richiesta);
						byte[] data2 = boStream.toByteArray();
						packet.setData(data2);
						socket.send(packet);
						System.out.println("RSClient: Inviata richiesta scambio righe a: " + temp.getIp() + " " + temp.getPort());
						
						//Ricezione pacchetto e elaborazione contenuto
						try {
							packet.setData(buf);
							socket.receive(packet);
						} catch (IOException e) {

							e.printStackTrace();
						}
						biStream = new ByteArrayInputStream(packet.getData(), 0,
								packet.getLength());
						diStream = new DataInputStream(biStream);
						try {
							risposta = diStream.readUTF();
						} catch (IOException e) {
							e.printStackTrace();
						}
						System.out.println("RSClient: Esito: " + risposta);
						System.out.println("RSCLient: Inserisci righe da scambiare di " + fileName + " oppure R per cambiare file"
								+ " EOF per terminare");
					}

				} catch (NumberFormatException e) {
					System.out.println("RSClient: numero di riga fornito errato");
					e.printStackTrace();
				} catch (IOException e) {
					System.out.println("RSClient: problemi con le operazione di IO");
					e.printStackTrace();
				}
			} while (rd != null);
		

		// chiusura
		System.out.println("RSClient: input chiuso. Termino");
		socket.close();
	}
}
