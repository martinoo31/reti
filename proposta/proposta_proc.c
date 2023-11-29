/* operazioni_proc.c
 * 	+implementazione delle procedure remote: "somma" e "moltiplicazione".
 *	+include operazioni.h.
 */

#include "proposta.h"
#include <string.h>
#include <rpc/rpc.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <dirent.h>
#include <unistd.h>

void cercaSottoDirettorio(char *name, Scansione *ris, int soglia);

Risultato *file_scan_1_svc(Stringa *str, struct svc_req *rp)
{  
    printf("Ricevuta richiesta file scan\n");
     char *filename =str->l;
    static Risultato *ris;
    //free(ris);
    ris = (Risultato *)malloc(sizeof(Risultato));
   

    int fd = open(filename, O_RDONLY);
    if (fd < 0)
    {
        ris->caratteri = -1;
    }
    else
    {
        char buffer;
        ris->caratteri = 0;
        ris->parole = 0;
        ris->linee = 0;
        while ((read(fd, &buffer, sizeof(char))) == 1)
        {
            ris->caratteri++;
            switch (buffer)
            {
            case '\n':
                ris->parole++;
                ris->linee++;
                break;
            case ' ':
                ris->parole++;
                break;
            default:
                break;
            }
        }
        if (buffer != ' ' && buffer != '\n')
        {
            ris->parole++;
            ris->linee++;
        }
    }
    return (ris);
}

Scansione *dir_scan_1_svc(Dir *direttorio, struct svc_req *rp)
{
    printf("Ricevuta richiesta dir scan\n");
    static Scansione *ris;
    free(ris);
    ris = (Scansione *)malloc(sizeof(Scansione));

    int soglia = direttorio->soglia;
    char* name = direttorio->dir.l;
    cercaSottoDirettorio(name,ris,soglia);
    printf("Invio risposta dirScan\n");
    return (ris);
}

void cercaSottoDirettorio(char *name, Scansione *ris, int soglia)
{
    DIR *dir;
    struct dirent *dd;
    int ce = chdir(name);
    int fd;
    if (ce == -1)
    {
        ris->numeroFile = -1;
        return;
    }
    else
    {
        ris->numeroFile = 0;
        dir = opendir(".");
        while ((dd = readdir(dir)) != NULL)
        {
            printf("Trovato il file %s\n", dd->d_name);
            if (dd->d_name[0] != '.' && (fd = open(dd->d_name, O_RDONLY)) > 0)
            {
                int byte = lseek(fd, 0, SEEK_END);
                if (byte >= soglia){
                    strcpy(ris->files[ris->numeroFile].l,dd->d_name);
                    ris->numeroFile++;
                    }
            }
        }
        closedir(dir);
        chdir("..");
    }
}