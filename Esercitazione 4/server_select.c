/*
    autore: jacopo 
    esercitazione n.4 sulla select
    il server offre due servizi: TCP (stream) e UDP (datagram)
    -per il TCP, il server riceve il nome di una directory
    e invia al Client la lista dei nomi dei file contenuti.
    -per l'UDP, riceve il nome di un file e una parola da eliminare dal file,
    il server risponde con il numero di eliminazioni

    ATTENZIONE: l'ordine della struct Datagram va concordato con
    il client, come la terminazione della comunicazione, in questo caso viene
    mandato lo zero binario.
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

#define DIM_BUFF         256
#define LENGTH_FILE_NAME 20
#define max(a, b)        ((a) > (b) ? (a) : (b))

struct Datagram{    
    char fileName[DIM_BUFF];
    char parola[DIM_BUFF];
};

/********************************************************/
void gestore(int signo) {
    int stato;
    printf("esecuzione gestore di SIGCHLD\n");
    wait(&stato);
}
/********************************************************/
void cercaSottoDirettorio(char *name, int sd){
   DIR           *dir, *subDir;
    struct dirent *dd, *subDD;
    int            count = 0;
    char* buffer[256];
    char* subDirPath[256];
    char zero = 0;
    char newLine = '\n';
    char *slash[1];
    slash[1]='\\';
    int lunghezzaNomefile;
    int ce                 = chdir(name);
    if (ce == -1){
        buffer[0]='N';
        buffer[1]=zero;
        write(sd,buffer,2*sizeof(char));        
    }
    else{
        buffer[0]='S';        
        write(sd,buffer,sizeof(char));
        dir = opendir(".");
    while ((dd = readdir(dir)) != NULL) {
        printf("Trovato il file %s\n", dd->d_name);
        if(dd->d_name[0]!='.' && (subDir=opendir(dd->d_name))!=NULL){
            {
                while((subDD =readdir(subDir))!=NULL){
                printf("Trovato il subfile %s\n", subDD->d_name);
                lunghezzaNomefile=strlen(subDD->d_name);
                write(sd,subDD->d_name,lunghezzaNomefile*sizeof(char));
                write(sd, &newLine,sizeof(char));
            }
            close(subDir);
            }}
            
        }
        closedir(dir);
        chdir("..");
    }
   
    write(sd,&zero,sizeof(char));
}
/********************************************************/

int main(int argc, char** argv){

    int                listenfd, connfd, udpfd, fd_dir, nready, maxfdp1;
    const int          on = 1;
    char               buff[DIM_BUFF], nome_dir[LENGTH_FILE_NAME];
    fd_set             rset;
    int                len, nread, nwrite, port;
    struct sockaddr_in cliaddr, servaddr;

    /* CONTROLLO ARGOMENTI ---------------------------------- */
    if (argc != 2) {
        printf("Error: %s port\n", argv[0]);
        exit(1);
    }

    nread = 0;
    while (argv[1][nread] != '\0') {
        if ((argv[1][nread] < '0') || (argv[1][nread] > '9')) {
            printf("Terzo argomento non intero\n");
            exit(2);
        }
        nread++;
    }
    port = atoi(argv[1]);
    if (port < 1024 || port > 65535) {
        printf("Porta scorretta...");
        exit(2);
    }

    /* INIZIALIZZAZIONE INDIRIZZO SERVER E BIND ---------------------------- */
    memset((char *)&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family      = AF_INET;
    servaddr.sin_addr.s_addr = INADDR_ANY;
    servaddr.sin_port        = htons(port);
    printf("Server avviato\n");

    /* CREAZIONE SOCKET TCP ------------------------------------------------ */
    listenfd = socket(AF_INET, SOCK_STREAM, 0);
    if (listenfd < 0) {
        perror("apertura socket TCP ");
        exit(1);
    }
    printf("Creata la socket TCP d'ascolto, fd=%d\n", listenfd);

    if (setsockopt(listenfd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) < 0) {
        perror("set opzioni socket TCP");
        exit(2);
    }
    printf("Set opzioni socket TCP ok\n");

    if (bind(listenfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0) {
        perror("bind socket TCP");
        exit(3);
    }
    printf("Bind socket TCP ok\n");

    if (listen(listenfd, 5) < 0) {
        perror("listen");
        exit(4);
    }
    printf("Listen ok\n\n");

    /* CREAZIONE SOCKET UDP ------------------------------------------------ */
    udpfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (udpfd < 0) {
        perror("apertura socket UDP");
        exit(5);
    }
    printf("Creata la socket UDP, fd=%d\n", udpfd);

    if (setsockopt(udpfd, SOL_SOCKET, SO_REUSEADDR, &on, sizeof(on)) < 0) {
        perror("set opzioni socket UDP");
        exit(6);
    }
    printf("Set opzioni socket UDP ok\n");

    if (bind(udpfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0) {
        perror("bind socket UDP");
        exit(7);
    }
    printf("Bind socket UDP ok\n");

    /* AGGANCIO GESTORE PER EVITARE FIGLI ZOMBIE -------------------------------- */
    signal(SIGCHLD, gestore);

    /* PULIZIA E SETTAGGIO MASCHERA DEI FILE DESCRIPTOR ------------------------- */
    FD_ZERO(&rset);
    maxfdp1 = max(listenfd, udpfd) + 1;

    //inizio ciclo infinito
    while(1){
        FD_SET(listenfd, &rset);
        FD_SET(udpfd, &rset);

        if ((nready = select(maxfdp1, &rset, NULL, NULL, NULL)) < 0) {
            if (errno == EINTR)
                continue;
            else {
                perror("select");
                exit(8);
            }
        }

        /*richiesta di tipo TCP
        il server riceve il nome di un direttorio
        e invia la lista di nomi dei file presenti*/
        if (FD_ISSET(listenfd, &rset)){


            printf("ricevuta richiesta TCP\n");
             len = sizeof(struct sockaddr_in);
            
            if ((connfd = accept(listenfd, (struct sockaddr *)&cliaddr, &len)) < 0) {
                if (errno == EINTR)
                    continue;
                else {
                    perror("accept");
                    exit(9);
                }
            }

            if(fork() == 0){
                //figlio gestisce il servizio
                close(listenfd);
                while(1){
                    
                    printf("Dentro il figlio, pid=%i\n", getpid());

                    if (read(connfd, &nome_dir, sizeof(nome_dir)) <= 0) {
                        perror("read");
                        printf("Figlio %i: termino\n", getpid());
                        shutdown(connfd, 0);
                        shutdown(connfd, 1);
                        close(connfd);
                        exit(0);
                    }

                    if(nome_dir[0] == '\0'){
                        //chiudo tutto se mi arriva lo zero binario dal client
                        printf("Figlio %i: termino\n", getpid());
                        shutdown(connfd, 0);
                        shutdown(connfd, 1);
                        close(connfd);
                        exit(0);
                    }

                    printf("direttorio richiesto: %s\n", nome_dir);

                    cercaSottoDirettorio(nome_dir, connfd);
                }
                

            }
        }//fine gestione TCP


        //inizio gestione UDP
        /*
        server riceve il nome di un file e la parola da eliminare
        il server prende il file, elimina le occorrenze e 
        conta quante volte lo fa. manda poi il conteggio al client
        */
        if (FD_ISSET(udpfd, &rset)){

            struct Datagram datagram;
            printf("Ricevuta richiesta UDP\n");

            len = sizeof(struct sockaddr_in);
            if (recvfrom(udpfd, &datagram, sizeof(datagram), 0, (struct sockaddr *)&cliaddr, &len) < 0) {
                perror("recvfrom");
                continue;
            }

            char * fileName  = datagram.fileName;
            char * parola = datagram.parola;

            int udpFile_fd = open(fileName, O_RDONLY);
            if(udpFile_fd < 0){
                printf("errore apertura file %s\n", fileName);
                int err = -1;
                if (sendto(udpfd, &err, sizeof(err), 0, (struct sockaddr *)&cliaddr, len) < 0) {
                perror("sendto");
                continue;
                }
                continue;
            }else{
                printf("aperto file %s con successo\n", fileName);
            }

            int fileout_fd = open("fileout.txt", O_WRONLY | O_CREAT | O_TRUNC ,0777);
            //operazioni r/w
            char c = "";
            char comp[DIM_BUFF];
            int cread;
            int count = 0;
            int pwrite;
            int res = 0;
            cread = read(udpFile_fd, &c , sizeof(char));
            
            while(cread != 0){

                if(cread < 0){
                    printf("errore lettura file %s\n", fileName);
                    break;
                }
                if(cread > 0){
                   
                    if(c != ' ' && c != '\n'){
                        comp[count] = c;
                        count++;
                    }else{
                        comp[count] = '\0';
                        if((strcmp(comp, parola)) != 0){
                            //scrivo la parola (se non e' uguale non la scrivo quindi n' facc' u cazz')
                            comp[count] = c;
                            comp[count+1] = '\0';
                            if((pwrite = write(fileout_fd, &comp, sizeof(char) * strlen(comp))) < 0){
                                printf("errore write su fileout.txt\n");
                                break;
                            }
                        }else{
                            //parola "eliminata" --> incremento il numero di eliminazioni
                            res++;
                            //printf("SALTATA\n");
                        }
                        count = 0;

                    }
                }
                cread = read(udpFile_fd, &c , sizeof(char));
            }

            close(udpFile_fd);
            close(fileout_fd);
            //rename("fileout.txt", fileName);

            //invio esito al bro
            printf("esito : %d occorrenze della parola '%s' eliminate\n", res, parola);
            printf("creato file fileout.txt senza la parola '%s'\n");
            if (sendto(udpfd, &res, sizeof(res), 0, (struct sockaddr *)&cliaddr, len) < 0) {
                    perror("sendto");
                    continue;
            }
        }    
    }//fine ciclo infinito
    //non arriva mai qui
    exit(0);
}