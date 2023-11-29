struct Stringa{
    char l[50];
};

struct Risultato{
	int caratteri;
	int parole;
    int linee;
};

struct Scansione{
    int numeroFile;
    Stringa files[8];
};

struct Dir{
    Stringa dir;
    int soglia;
};




program PROPOSTAPROG {
	version PROPOSTAVERS {
		Risultato FILE_SCAN(Stringa) = 1;
		Scansione DIR_SCAN(Dir) = 2;
	} = 1;
} = 0x200000AA;
