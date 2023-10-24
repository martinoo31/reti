//package prova;

import java.io.*;
import java.net.*;

public class ServerSeq {

	public static final int PORT=54321;
	
	public static void main(String[] args) {
		int port = -1;
		
		try {
			if (args.length == 1) {
				port = Integer.parseInt(args[0]);
				// controllo che la porta sia nel range consentito 1024-65535
				if (port < 1024 || port > 65535) {
					System.out.println("Numero di porta non consentito!");
					System.exit(1);
				}
			} else if (args.length == 0) {
				port = PORT;
			} else {
				System.out
					.println("Usage: java ServerSeq or java ServerSeq port");
				System.exit(1);
			}
		} //try
		catch (Exception e) {
			System.out.println("Problemi, i seguenti: ");
			e.printStackTrace();
			System.out
				.println("Usage: java ServerSeq or java ServerSeq port");
			System.exit(1);
		}
		
		ServerSocket serverSocket = null;
		//creazione serverSocket
		try {
			serverSocket = new ServerSocket(port);
			serverSocket.setReuseAddress(true);
			System.out.println("ServerSeq: avviato ");
			System.out.println("Creata la server socket: " + serverSocket);
		}
		catch (Exception e) {
			System.err.println("Problemi nella creazione della server socket: "
					+ e.getMessage());
			e.printStackTrace();
			System.exit(2);
		}
		
		
		
		try {
			while(true) {//deamon
				Socket clientSocket = null;
				DataInputStream inSock = null;
				DataOutputStream outSock = null;
				
				System.out.println("\nIn attesa di richieste...");
				//accettazione richiesta di connessione
				try {
					clientSocket = serverSocket.accept();
					clientSocket.setSoTimeout(30000); //timeout altrimenti server sequenziale si sospende
					System.out.println("Connessione accettata: " + clientSocket + "\n");
				}
				catch (SocketTimeoutException te) {
					System.err
						.println("Non ho ricevuto nulla dal client per 30 sec., interrompo "
								+ "la comunicazione e accetto nuove richieste.");
					// il server continua a fornire il servizio ricominciando dall'inizio
					continue;
				}
				catch (Exception e) {
					System.err.println("Problemi nella accettazione della connessione: "
							+ e.getMessage());
					e.printStackTrace();
					continue;
				}
				
               
				//creazione degli stream IO sulla connessione
				try {
					inSock = new DataInputStream(clientSocket.getInputStream());
					outSock = new DataOutputStream(clientSocket.getOutputStream());
					
		        }
				catch(SocketTimeoutException ste){
					System.out.println("Timeout scattato: ");
					ste.printStackTrace();
					clientSocket.close();
					System.out
						.print("\n^D(Unix)/^Z(Win)+invio per uscire, solo invio per continuare: ");
					// il client continua l'esecuzione riprendendo dall'inizio del ciclo
					continue;          
				}
				catch (IOException e) {
		        	System.out
		        		.println("Problemi nella creazione degli stream di input/output "
		        			+ "su socket: ");
		        	e.printStackTrace();
		        	continue;
		        }
				
                String nomeFile;
				int numBytes = 0;
                FileOutputStream outFile = null;
				DataOutputStream dest = null;
				
				try {
                    while ((nomeFile = inSock.readUTF())  != null) {
                        File curFile = new File(nomeFile);
                        if(curFile.exists()) {
                            outSock.writeUTF("Salta File");
                        }
                        else {
                            //se non esiste lo prendo insieme al numBytes
                            outSock.writeUTF("Attiva");
                            numBytes = inSock.readInt();
                            System.out.println("File: " + nomeFile + " numBytes: " + numBytes );
                            
                            outFile = new FileOutputStream(nomeFile);
                            dest = new DataOutputStream(outFile);
                            int buffer=0;
                            try{
                            
                             while((buffer = inSock.read())>=0) {
                                dest.write(buffer);
                             }
                             System.out.println("Numero di byte trasferiti: " + buffer); 
                             dest.flush();
                            }catch (EOFException e) {
                                System.out.println("Problemi, i seguenti: ");
                                e.printStackTrace();
                            }
                           outFile.close();
                        }
					
						
					}
				}catch (EOFException eof) {
	                System.out.println("Raggiunta la fine delle ricezioni, chiudo...");
	                clientSocket.close();
	                System.out.println("PutFileServer: termino...");
	                System.exit(0);
				}
			}//while
		}catch (Exception e) {
			e.printStackTrace();
			// chiusura di stream e socket
			System.out.println("Errore irreversibile, PutFileServerSeq: termino...");
			System.exit(3);
		}
		
	}//main
}//class ServerSeq
