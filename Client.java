//package provaclient;

import java.net.*;
import java.io.*;

public class Client{
    public static void main(String[] args) {
        InetAddress addr = null;
        int port = -1;
        int minSize = 0;
     
        try{
        //controllo argomenti
            if(args.length == 3){
                addr = InetAddress.getByName(args[0]);
                port = Integer.parseInt(args[1]);
                minSize = Integer.parseInt(args[2]);
            }
            else {
                System.out.println("Usage: java Client serverAddr serverPort minSize");
                System.exit(1);
            }    
        }catch (Exception e) {
        System.out.println("Problemi, i seguenti: ");
        e.printStackTrace();
        System.exit(1);
        }

        Socket socket = null;
        DataInputStream inSock = null;
        DataOutputStream outSock = null;

        try {
        //creazione socket
            socket = new Socket(addr, port);
            socket.setSoTimeout(30000);
            System.out.println("Creata la socket: " + socket);

        //creazione stream di input e output
            inSock = new DataInputStream(socket.getInputStream());
            outSock = new DataOutputStream(socket.getOutputStream());
        }catch (IOException ioe) {
            System.out.println("Problemi nella creazione degli stream su socket: ");
            ioe.printStackTrace();
            System.out.print("\n^D(Unix)/^Z(Win)+invio per uscire, solo invio per continuare: ");
            System.exit(1);
        }

        //interazione con utente
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
        String nomeDir = null;

        System.out.print("Inserisci il nome del direttorio oppure ^D(Unix)|^Z(Win)+invio per uscire: ");

        try {
			while((nomeDir = stdIn.readLine()) != null){
			    File dir = new File(nomeDir);
			    if(dir.exists()){
			        File[] allFiles = dir.listFiles();
			        for (int i = 0; i<allFiles.length; i++) {
			            //controllo su minSize
			            if(allFiles[i].length() > minSize){
			                //invio nomeFile al server
			                outSock.writeUTF(allFiles[i].getName());
			                System.out.println("Nome del file da trasferire : " + allFiles[i].getName() );
			                
			                //lettura risposta del server
			                String esito = inSock.readUTF();
			                if(esito.equalsIgnoreCase("Attiva")){
			                    System.out.println("Attivo il trasferimento del file " + allFiles[i].getName() + " di dimensione " + allFiles[i].length());
			                    
			                    try{
			                      /*  // FileUtility.trasferisci_a_byte_file_binario(allFiles[i], outSock);
			                        int buffer=0;//NPException
			                        try (DataInputStream src = new DataInputStream(new FileInputStream(allFiles[i]))) {
										while((buffer = src.read()) >=0){
										    outSock.write(buffer);//trasferisce numero di byte letti 
										}
										System.out.println("Numero totale di byte:" + buffer);
									}*/
                                DataInputStream src = new DataInputStream(new FileInputStream(allFiles[i]));     
				try{
					   
                                        int count = src.available();//numero totale di byte del file
                                        byte[] buffer = new byte[count]; 
                                        int numBytes = src.read(buffer);//numero di byte letti 
                                        outSock.write(buffer, 0, numBytes);//invio numbytes al server
                                        System.out.println("Numero di byte trasferiti: " + numBytes); 

                                    }catch (EOFException e) {
                                        System.out.println("Problemi, i seguenti: ");
                                        e.printStackTrace();
                                    }
			         outSock.flush();
			                    }catch(IOException io){
			                        System.out.println("Non sono riuscito a trasferire il file!");
			                        io.printStackTrace();
			                   }
			                }
			                else if(esito.equalsIgnoreCase("Salta")){
			                    System.out.println("File " + allFiles[i].getName() + " esistente, salta!");
			                }    
			            }
			        }//for
			    }//if
			}//while
		} catch (IOException e) {
			System.out.println("Non sono riuscito ad effettuare i trasferimenti");
			e.printStackTrace();
		}
    }//main
}//Client
