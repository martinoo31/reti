/*  *** author: Jacopo
    *** seconda parte dell'esercitazione 3
    *** socket con connessione
    *** client chiede all'utente il nome di un file e un int
    *** che indica il numero della linea da eliminare del file.
    *** Il server riceve il file, elimina la linea e restituisce
    *** un nuovo file. 
    *** SERVER PARALLELO
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
#define FILENAME_MAX 256

int main(int argc, char** argv){

    int  sd, port, fd_sorg, fd_dest, nread;
    char buff[DIM_BUFF];
    char               nome_sorg[FILENAME_MAX + 1], nome_dest[FILENAME_MAX + 1];
    struct hostent    *host;
    struct sockaddr_in servaddr;

    /* CONTROLLO ARGOMENTI ---------------------------------- */
    if (argc != 3) {
        printf("Error:%s serverAddress serverPort\n", argv[0]);
        exit(1);
    }

    /* INIZIALIZZAZIONE INDIRIZZO SERVER -------------------------- */
    memset((char *)&servaddr, 0, sizeof(struct sockaddr_in));
    servaddr.sin_family = AF_INET;
    host                = gethostbyname(argv[1]);

    /*VERIFICA INTERO*/
    nread = 0;
    while (argv[2][nread] != '\0') {
        if ((argv[2][nread] < '0') || (argv[2][nread] > '9')) {
            printf("Secondo argomento non intero\n");
            exit(2);
        }
        nread++;
    }
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

    //inizio corpo
    printf("inserisci il nome del file\n");
    while(gets(nome_sorg)){
        printf("nome file : %s\n", nome_sorg);

        if ((fd_sorg = open(nome_sorg, O_RDONLY)) < 0) {
            perror("open file sorgente");
            printf("Nome del file, EOF per terminare: ");
            continue;
        }

        printf("inserisci il nome del nuovo file\n");
        if(gets(nome_dest) == 0) break; //EOF
        else{
            if((fd_dest = open(nome_dest, O_WRONLY | O-CREAT, 0777)) < 0){
                perror("open file destinatario");
                printf("Nome del file, EOF per terminare: \n");
                continue;
            }
            else printf("file senza la linea: %s\n", nome_dest);
        }

        //leggo l'intero (num riga da togliere)
        while(scanf("%d", &line) != 1){
            do{
                c = getchar();
                printf("%c", &c);
            }while(c != '\n');
            printf("inserire un intero\n");
            continue;
        }

        gets(okstr);
        printf("numero linea = %d", line);

        /* CREAZIONE SOCKET ------------------------------------ */
        sd = socket(AF_INET, SOCK_STREAM, 0);
        if (sd < 0) {
            perror("apertura socket");
            exit(1);
        }
        printf("Client: creata la socket sd=%d\n", sd);

        /* Operazione di BIND implicita nella connect */
        if (connect(sd, (struct sockaddr *)&servaddr, sizeof(struct sockaddr)) < 0) {
            perror("connect");
            exit(1);
        }
        printf("Client: connect ok\n");

        // Invio del numero riga
        printf("Invio il numero riga: %d\n", line);
        write(sd, &line, sizeof(int));

        printf("client : invio file\n");
        while ((nread = read(fd_sorg, buff, DIM_BUFF)) > 0)   write(sd, buff, nread);
        
        // Chiusura socket in spedizione -> invio dell'EOF 
        shutdown(sd, 1);
        printf("Client: file inviato\n");

        /* RICEZIONE File */
        printf("Client: ricevo e stampo file senza la linea\n");
        while ((nread = read(sd, buff, DIM_BUFF)) > 0) {
            write(fd_dest, buff, nread);
            write(1, buff, nread);
        }
        printf("\nTrasferimento terminato\n");
        /* Chiusura socket in ricezione */
        shutdown(sd, 0);

        /* Chiusura file */
        close(fd_sorg);
        close(fd_dest);

        printf("inserire nome del file\n");

    }

    printf("Client : termino...\n");
    exit(0);
}