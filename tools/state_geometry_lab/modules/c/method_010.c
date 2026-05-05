#include <stdio.h>
int main(int argc,char**argv){const char*seed=argc>1?argv[1]:"rmr/rrr/Rafael_Rafael_semente.txt";int v=argc>2?atoi(argv[2]):10;printf("{\"method\":\"c_010\",\"seed\":\"%s\",\"value\":%d}\n",seed,v);return 0;}
