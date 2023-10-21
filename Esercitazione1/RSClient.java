
import java.io.*;
import java.net.*;
import java.util.*;

public class RSClient {

    // RSClient IPDS portDS fileName
    public static void main(String args[]) {
        InetAddress adr = null;
        int port = -1;
        String fileName = null;

        try {
            if (args.length == 3) {
                adr = InetAddress.getByName(args[0]);
                port = Integer.parseInt(args[1]);
                fileName = args[2];
            } else {
                System.out.println("Usage : RSClient IPDS portDS fileName");
                System.exit(1);
            }
        } catch (UnknownHostException e) {
            System.out.println("RSClient : Errore creazione argomenti :");
            e.printStackTrace();
            System.exit(2);
        }

        DatagramSocket socket = null;
        DatagramPacket packet = null;
        byte[] buf = new byte[256];

        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(30000);
            packet = new DatagramPacket(buf, buf.length, adr, port);
            System.out.println("RSClient : socket(" + socket + ") e datagram creati");
        } catch (IOException e) {
            System.out.println("RSClient : Errore creazione socket/datagram : ");
            e.printStackTrace();
            System.exit(3);
        }

        System.out.println("Inserisce righe da scambiare");
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        // prima richiesta
        ByteArrayOutputStream boStream = new ByteArrayOutputStream();
        DataOutputStream doStream = new DataOutputStream(boStream);
        try {
            doStream.writeUTF(fileName);
        } catch (IOException e) {

            e.printStackTrace();
        }
        byte[] data = boStream.toByteArray();
        packet.setData(data);
        try {
            socket.send(packet);
        } catch (IOException e) {

            e.printStackTrace();
        }

        // ricezione della porta dove si trova il file
        packet.setData(buf);
        try {
            socket.receive(packet);
        } catch (IOException e) {

            e.printStackTrace();
        }
        String risposta = null;

        ByteArrayInputStream biStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
        DataInputStream diStream = new DataInputStream(biStream);
        try {
            risposta = diStream.readUTF();
        } catch (IOException e) {

            e.printStackTrace();
        }
        System.out.println("Il discovery server mi ha dato questa porta: " + risposta);

        // set nuova porta e socket
        port = Integer.parseInt(risposta);
        packet = new DatagramPacket(buf, buf.length, adr, port);

        try {
            socket = new DatagramSocket();
            System.out.println("RSClient : socket(" + socket + ") e datagram creati con la nuova porta");
        } catch (SocketException e) {

            e.printStackTrace();
        }

        String rd = "";
        StringTokenizer tk = null;
        int riga1, riga2;
        String richiesta = "";
        try {
            System.out.println("Inserisci righe da scambiare di " + fileName);
            while ((rd = stdIn.readLine()) != null) {
                // interazione utente
                tk = new StringTokenizer(rd);
                riga1 = Integer.parseInt(tk.nextToken());
                riga2 = Integer.parseInt(tk.nextToken());
                richiesta = riga1 + " " + riga2;

                // mando richiesta al Discovery
                ByteArrayOutputStream boStream2 = new ByteArrayOutputStream();
                DataOutputStream doStream2 = new DataOutputStream(boStream2);
                doStream2.writeUTF(richiesta);
                byte[] data2 = boStream2.toByteArray();
                packet.setData(data2);
                socket.send(packet);
                System.out.println("Inviata richiesta scambio righe a: " + adr + " " + port);

                try {
                    packet.setData(buf);
                    socket.receive(packet);
                } catch (IOException e) {

                    e.printStackTrace();
                }
                ByteArrayInputStream biStream2 = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
                DataInputStream diStream2 = new DataInputStream(biStream2);
                try {
                    risposta = diStream2.readUTF();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("esito: " + risposta);
                System.out.println("Inserisci righe da scambiare di " + fileName);
            }

        } catch (NumberFormatException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }

        // ricezione esito

        // chiusura
        System.out.println("RSClient : termino");
        socket.close();
    }
}
