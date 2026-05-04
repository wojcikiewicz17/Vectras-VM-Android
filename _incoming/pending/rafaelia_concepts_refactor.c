#include <stdint.h>
#include <stddef.h>
#include <math.h>
#include <string.h>
#include <stdio.h>
#include <time.h>

typedef struct {
    double u,v,psi,chi,rho,delta,sigma;
} torus7_t;

typedef struct {
    double coherence;
    double entropy;
    uint64_t hash;
    uint32_t state;
} concept_state_t;

static inline double wrap01(double x){
    double y = x - floor(x);
    return y < 0.0 ? y + 1.0 : y;
}

static torus7_t toroidal_map(const concept_state_t *in){
    const double q = 1.0 / 4294967296.0;
    torus7_t s = {
        wrap01(in->coherence),
        wrap01(in->entropy),
        wrap01((double)((in->hash >> 0)  & 0xFFFF) * q * 65536.0),
        wrap01((double)((in->hash >> 16) & 0xFFFF) * q * 65536.0),
        wrap01((double)((in->hash >> 32) & 0xFFFF) * q * 65536.0),
        wrap01((double)((in->hash >> 48) & 0xFFFF) * q * 65536.0),
        wrap01((double)(in->state % 42u) / 42.0)
    };
    return s;
}

static inline double ema_step(double prev, double in){ return 0.75*prev + 0.25*in; }
static inline double phase_phi(double c, double h){ return (1.0-h)*c; }

static uint64_t fnv1a64(const uint8_t *p, size_t n){
    uint64_t h=1469598103934665603ULL;
    for(size_t i=0;i<n;i++){
        h ^= p[i];
        h *= 0x100000001B3ULL;
    }
    return h;
}

static uint32_t entropy_milli(const uint8_t *buf, size_t len){
    if(!buf || len==0) return 0;
    uint8_t seen[256]={0};
    uint32_t unique=0, transitions=0;
    for(size_t i=0;i<len;i++){
        if(!seen[buf[i]]){ seen[buf[i]]=1; unique++; }
        if(i>0 && buf[i]!=buf[i-1]) transitions++;
    }
    if(len==1) return (unique*6000u)/256u;
    return (unique*6000u)/256u + (transitions*2000u)/(uint32_t)(len-1);
}

static double torus_distance(double a, double b){
    double d = fabs(a-b);
    return d > 0.5 ? 1.0-d : d;
}

int main(void){
    enum { N = 200000 };
    concept_state_t st = {0.5,0.5,0x123456789abcdef0ULL,1};
    uint8_t payload[256];
    for(size_t i=0;i<sizeof(payload);i++) payload[i]=(uint8_t)(i^(i<<1));

    clock_t t0 = clock();
    double acc=0.0;
    for(int i=0;i<N;i++){
        uint64_t h = fnv1a64(payload,sizeof(payload));
        st.hash ^= h + (uint64_t)i;
        st.coherence = ema_step(st.coherence, (double)(h & 0xFFFF)/65535.0);
        st.entropy = ema_step(st.entropy, (double)entropy_milli(payload,sizeof(payload))/8000.0);
        torus7_t s = toroidal_map(&st);
        acc += phase_phi(st.coherence, st.entropy) + torus_distance(s.u,s.v);
        st.state = (st.state + 1u) % 42u;
    }
    clock_t t1 = clock();
    double ms = 1000.0*(double)(t1-t0)/(double)CLOCKS_PER_SEC;

    printf("benchmark.iter=%d\n", N);
    printf("benchmark.time_ms=%.3f\n", ms);
    printf("benchmark.ops_per_sec=%.0f\n", (N/ms)*1000.0);
    printf("state.coherence=%.6f\nstate.entropy=%.6f\nstate.phase=%.6f\n", st.coherence, st.entropy, phase_phi(st.coherence, st.entropy));
    printf("checksum.acc=%.6f\n", acc);
    return 0;
}
