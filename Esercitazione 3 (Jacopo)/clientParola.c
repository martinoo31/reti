/*  *** author: Jacopo
    *** prima parte dell'esercitazione 3
    *** socket senza connessione
    *** client chiede all'utente il nome di un file, 
    *** il server dovrà trovare la parola più lunga e
    *** restituire il numero di caratteri
*/

#include <netdb.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>

#define LINE_LENGTH 256

int main(int argc, char** argv){

    struct hostent    *host;
    struct sockaddr_in clientaddr, servaddr;
    int                port, sd, len, ris, ok;
    char               okstr[LINE_LENGTH];
    char               c;

    /* CONTROLLO ARGOMENTI ---------------------------------- */
    if (argc != 3) {
        printf("Error:%s serverAddress serverPort\n", argv[0]);
        exit(1);
    }

     /* INIZIALIZZAZIONE INDIRIZZO CLIENT E SERVER --------------------- */
    memset((char *)&clientaddr, 0, sizeof(struct sockaddr_in));
    clientaddr.sin_family      = AF_INET;
    clientaddr.sin_addr.s_addr = INADDR_ANY;
    clientaddr.sin_port = 0;
    memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
    servaddr.sin_family = AF_INET;
    host                = gethostbyname(argv[1]);
    port = atoi(argv[2]);

    /* VERIFICA PORT e HOST */
    if (port < 1024 || port > 65535) {
        printf("%s = porta scorretta...\n", argv[2]);
        exit(2);
    }
    if (host == NULL) {
        printf("%s not found in /etc/hosts\n", argv[1]);
        exit(2);
    } else {
        servaddr.sin_addr.s_addr = ((struct in_addr *)(host->h_addr))->s_addr;
        servaddr.sin_port        = htons(port);
    }


    /* CREAZIONE SOCKET ---------------------------------- */
    sd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sd < 0) {
        perror("apertura socket");
        exit(1);
    }
    printf("Client: creata la socket sd=%d\n", sd);

    /* BIND SOCKET, a una porta scelta dal sistema --------------- */
    if (bind(sd, (struct sockaddr_in *)&clientaddr, sizeof(clientaddr)) < 0) {
        perror("bind socket ");
        exit(1);
    }
    printf("Client: bind socket ok, alla porta %i\n", clientaddr.sin_port);

    char fileName[LINE_LENGTH];

    //lettura infinita dall'utente
    printf("inserire nome del file\n");
    while((ok = (scanf("%s", &fileName))) != EOF){
        if(ok != 1){
             do {
                c = getchar();
                printf("%c ", c);
            } while (c != '\n');
            printf("Inserire il file, EOF per terminare: ");
            continue;
        }

    len = sizeof(servaddr);
    if (sendto(sd, &fileName, sizeof(fileName), 0, (struct sockaddr_in *)&servaddr, len) < 0) {
        perror("sendto");
        continue;
        }

     /* ricezione del risultato */
    printf("Attesa del risultato...\n");
    if (recvfrom(sd, &ris, sizeof(ris), 0, (struct sockaddr_in *)&servaddr, &len) < 0) {
        perror("recvfrom");
        continue;
    }  

    printf("letto : %d\n", ris); 
    printf("inserire nome del file\n"); 
    }//fine while

    

    // CLEAN OUT
    close(sd);
    printf("\nClient: termino...\n");
    exit(0);
}