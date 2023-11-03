/*  *** author: Jacopo
    *** seconda parte dell'esercitazione 3
    *** socket con connessione
    *** client chiede all'utente il nome di un file e un int
    *** che indica il numero della linea da eliminare del file.
    *** Il server riceve il file, elimina la linea e restituisce
    *** un nuovo file. 
    *** SERVER PARALLELO
*/

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

//gestione processi figli
void gestore(int signo) {
    int stato;
    printf("esecuzione gestore di SIGCHLD\n");
    wait(&stato);
}

//main
int main(int argc, char**argv){

    int                listen_sd, conn_sd;
    int                port, len, num;
    const int          on = 1;
    struct sockaddr_in cliaddr, servaddr;
    struct hostent    *host;

    /* CONTROLLO ARGOMENTI ---------------------------------- */
    if (argc != 2) {
        printf("Error: %s port\n", argv[0]);
        exit(1);
    } else {
        num = 0;
        while (argv[1][num] != '\0') {
            if ((argv[1][num] < '0') || (argv[1][num] > '9')) {
                printf("Secondo argomento non intero\n");
                exit(2);
            }
            num++;
        }
        port = atoi(argv[1]);
        if (port < 1024 || port > 65535) {
            printf("Error: %s port\n", argv[0]);
            printf("1024 <= port <= 65535\n");
            exit(2);
        }
    }

    /* INIZIALIZZAZIONE INDIRIZZO SERVER ----------------------------------------- */
    memset((char *)&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family      = AF_INET;
    servaddr.sin_addr.s_addr = INADDR_ANY;
    servaddr.sin_port        = htons(port);

    /* CREAZIONE E SETTAGGI SOCKET D'ASCOLTO --------------------------------------- */
    listen_sd = socket(AF_INET, SOCK_STREAM, 0);
    if (listen_sd < 0) {
        perror("creazione socket ");
        exit(1);
    }
    printf("Server: creata la socket d'ascolto per le richieste di ordinamento, fd=%d\n",
           listen_sd);

    if (setsockopt(listen_sd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) < 0) {
        perror("set opzioni socket d'ascolto");
        exit(1);
    }
    printf("Server: set opzioni socket d'ascolto ok\n");

    if (bind(listen_sd, (struct sockaddr_in *)&servaddr, sizeof(servaddr)) < 0) {
        perror("bind socket d'ascolto");
        exit(1);
    }
    printf("Server: bind socket d'ascolto ok\n");

    if (listen(listen_sd, 5) < 0) // creazione coda d'ascolto
    {
        perror("listen");
        exit(1);
    }
    printf("Server: listen ok\n");

    //assegnazione di gestore a SIGCHLD
    signal(SIGCHLD, gestore);

    //inizio ciclo infinito del server
    while(1){
        len = sizeof(cliaddr);
        if ((conn_sd = accept(listen_sd, (struct sockaddr_in *)&cliaddr, &len)) < 0) {
            if (errno == EINTR) {
                perror("Forzo la continuazione della accept");
                continue;
            } else
                exit(1);
        }

        if(fork() == 0){
            //processo figlio
            close(listen_sd);
            host = gethostbyaddr((char *)&cliaddr.sin_addr, sizeof(cliaddr.sin_addr), AF_INET);
            if (host == NULL) {
                printf("client host information not found\n");
                continue;
            } else printf("Server (figlio): host client e' %s \n", host->h_name);
            printf("Server (figlio): elimino una riga\n");

            //prima di tutto leggo il numero della riga da togliere
            int line = 0;
            int nread = read(conn_sd, &line, sizeof(int));
            if(nread < 0){
                sprintf(err, "(PID %d) impossibile leggere il numero di linea", getpid());
                perror(err);
                exit(EXIT_FAILURE);
            }

            printf("linea da rimuovere: %d\n");

            //lettura file
            int counter = 1;
            char c = "";
            while((nread = read(conn_sd, &c, sizeof(char))) > 0){
                if( c == '\n'){
                    //linea finita
                    if(counter != line){
                        //non e' la linea che mi interessa
                        write(conn_sd, &c, 1);
                    }
                    counter++;
                }
                if(counter != line){
                    //non e' la linea da togliere, la riscrivo uguale
                    write(conn_sd, &c, sizeof(char));
                }
            }//fine while di controllo/scrittura file

            //chiudo la socket
            close(conn_sd);



        }// fine figlio
    }//fine ciclo infinito

}