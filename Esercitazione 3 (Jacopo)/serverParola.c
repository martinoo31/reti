/*  *** author: Jacopo
    *** prima parte dell'esercitazione 3
    *** socket senza connessione
    *** client chiede all'utente il nome di un file, 
    *** il server dovrà trovare la parola più lunga e
    *** restituire il numero di caratteri
*/



#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#define DIM_BUFF 256



int main(int argc, char** argv){

    int                sd, port, len, ris;
    const int          on = 1;
    struct sockaddr_in cliaddr, servaddr;
    struct hostent    *clienthost;
    char fileName[DIM_BUFF];

    /* CONTROLLO ARGOMENTI ---------------------------------- */
    if (argc != 2) {
        printf("Error: %s port\n", argv[0]);
        exit(1);
    } else {
        port = atoi(argv[1]);
        if (port < 1024 || port > 65535) {
            printf("Error: %s port\n", argv[0]);
            printf("1024 <= port <= 65535\n");
            exit(2);
        }
    }


    /* INIZIALIZZAZIONE INDIRIZZO SERVER ---------------------------------- */
    memset((char *)&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family      = AF_INET;
    servaddr.sin_addr.s_addr = INADDR_ANY;
    servaddr.sin_port        = htons(port);

    /* CREAZIONE, SETAGGIO OPZIONI E CONNESSIONE SOCKET -------------------- */
    sd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sd < 0) {
        perror("creazione socket ");
        exit(1);
    }
    printf("Server: creata la socket, sd=%d\n", sd);

    if (setsockopt(sd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) < 0) {
        perror("set opzioni socket ");
        exit(1);
    }
    printf("Server: set opzioni socket ok\n");

    if (bind(sd, (struct sockaddr_in *)&servaddr, sizeof(servaddr)) < 0) {
        perror("bind socket ");
        exit(1);
    }
    printf("Server: bind socket ok\n");

    // ciclo infinito server
    while(1){
        len = sizeof(struct sockaddr_in);
        if (recvfrom(sd,fileName, sizeof(fileName), 0, (struct sockaddr_in *)&cliaddr, &len) < 0) {
            perror("recvfrom error\n ");
            continue; 
        }

        printf("richiesto file %s\n", fileName);

        clienthost = gethostbyaddr((char *)&cliaddr.sin_addr, sizeof(cliaddr.sin_addr), AF_INET);
        if (clienthost == NULL)
            printf("client host information not found\n");
        else
            printf("Operazione richiesta da: %s %i\n", clienthost->h_name,
                   (unsigned)ntohs(cliaddr.sin_port));

        //ricerco il file

        int count = 0;
        int maxLength = 0;
        int fd = open(fileName, O_RDONLY);
        if(fd < 0){
            printf("errore apertura %s\n", fileName);
        }
        char c;
        int rd;
        int i = 0;
        while((rd = read(fd, &c, sizeof(char))) != EOF){
            if(c != ' ' && c!= '\n') count++;
            else{
                if(count > maxLength) maxLength = count;
                count = 0;
            }
           // printf("conteggio %d\n", ++i);
        }


        printf("conteggio finito: risultato = %d", maxLength);
        close(fd);

        //fine ricerca. mando risultato
        if (sendto(sd, &maxLength, sizeof(maxLength), 0, (struct sockaddr_in *)&cliaddr, len) < 0) {
            perror("sendto ");
            continue;
        }

    }//fine ciclo infinito


}