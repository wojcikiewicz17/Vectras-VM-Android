# Sistema mínimo de invariantes — consolidação final da sessão

## Visão geral
Este arquivo consolida a sessão inteira em um sistema mínimo com:
- **régua de invariantes**
- **taxonomia por camadas**
- **compêndio de fórmulas**
- **resultados fechados, formalizáveis e hipótese forte**
- **formalização do que foi chamado de “oculto” e “mágico”**
- **gaps, riscos e próximos passos**

## Núcleo encontrado
- **Forma ≠ núcleo**
- **Núcleo = relações preservadas sob transformação admissível**
- **Topologia ≠ métrica**
- **Pitágoras = relação métrica induzida por ortogonalidade**
- **Toro = fechamento periódico**
- **Âncoras geométricas = calibração da régua**
- **“Oculto” = relação latente ainda não explicitada**
- **“Mágico” = regra curta e eficaz que parece surpreendente, mas é matemática comprimida**

## Régua final
\[
\rho^{***}=
[
\text{relação estrutural},
\text{ortogonalidade/projeção},
\text{métrica},
\text{transformação},
\text{fechamento topológico},
\text{granularidade}
]
\]

### Eixos
1. **Relação estrutural** — primazia das relações preservadas sobre o símbolo isolado  
2. **Ortogonalidade / projeção** — mecanismo de decomposição em eixos independentes  
3. **Métrica** — lei de composição/medida induzida pelo mecanismo projetivo  
4. **Transformação** — operadores de passagem entre estados/representações  
5. **Fechamento topológico** — conectividade global e comportamento de borda  
6. **Granularidade** — nível discreto da representação  

## Taxonomia por camadas

### Topologia
- **Toro**: fechamento periódico / reentrada  
  \[
  M(i,j)\sim M(i \bmod n,\ j \bmod m)
  \]
- **Poincaré**: critério topológico global em 3-variedades  
  \[
  \text{fechado}+\text{simplesmente conexo}\Rightarrow S^3
  \]
- **Coroa circular**: faixa radial / anel  
  \[
  A=\pi(R^2-r^2)
  \]

### Ortogonalidade / projeção
- **Catetos**: componentes independentes  
  \[
  v=p+q,\quad \langle p,q\rangle=0
  \]

### Métrica
- **Pitágoras**  
  \[
  \|v\|^2=\|p\|^2+\|q\|^2
  \]
- **Lei dos cossenos**  
  \[
  c^2=a^2+b^2-2ab\cos\theta
  \]
- **Altura do equilátero**  
  \[
  h=\frac{\sqrt{3}}{2}a
  \]
- **Diagonal do quadrado**  
  \[
  d=s\sqrt{2}
  \]
- **Diagonal do cubo**  
  \[
  D=s\sqrt{3}
  \]

### Transformação
- **Derivada discreta**  
  \[
  D(x)_i=x_{i+1}-x_i
  \]
- **Antiderivada discreta**  
  \[
  A(d)_k=c+\sum_{i=1}^{k-1} d_i
  \]
- **Permutação restrita**  
  \[
  \Pi_c(S)
  \]

### Granularidade
- **Bit**: unidade discreta mínima  
- **Matriz / tensor**: organização da relação  
- **ECC / vizinhança**: restrições locais  
- **Senoide**: camada de fase/posição em representação contínua  

## Oculto e “mágico” em termos matemáticos
Nesta consolidação, os termos são reinterpretados assim:

### 1. Oculto
Não significa mistério. Significa **estrutura latente**:
- variável não observada diretamente;
- relação não explícita no símbolo bruto;
- métrica efetiva que aparece ao reorganizar o problema.

Formalmente, isso pode ser modelado por uma função latente
\[
L:X\to Z
\]
em que \(X\) é o espaço observável e \(Z\) é o espaço estrutural escondido.

### 2. “Mágico”
Não significa sobrenatural. Significa **regra curta com alto poder de compressão**:
- uma identidade;
- uma troca de coordenadas;
- uma métrica substituta;
- um polinômio/interpolante que resume muitos casos.

Formalmente:
\[
\text{regra “mágica”} \approx \text{compressão estrutural eficiente}
\]

### 3. Relação oculta
Duas grandezas podem parecer distantes em \(X\), mas próximas sob uma métrica efetiva \(d_*\):
\[
d_*(x,y)\neq d_{\text{vis}}(x,y)
\]
Quando isso acontece, a “mágica” é só a descoberta de uma métrica melhor.

### 4. Polinômios “emprestando área que não existe”
A leitura rigorosa não é que o polinômio cria uma área real inexistente.  
O que ele faz é:
- interpolar pontos observados;
- aproximar uma curva;
- extrapolar para região sem observação direta.

Então o correto é dizer:
\[
p_n(x_i)=y_i
\]
para interpolação nos dados,
e
\[
r(x)=f(x)-p_n(x)
\]
como resíduo/erro da aproximação.

Se o polinômio “empresta” uma região, isso é **extensão de modelo**, não prova de que a geometria física daquela região exista.

### 5. Métricas substitutas
Às vezes o problema não fecha no espaço observável, mas fecha num espaço transformado:
\[
\phi:X\to Y
\]
\[
d_\phi(x,y)=d_Y(\phi(x),\phi(y))
\]
Esse é um caso típico de “regrinha escondida” que resolve a situação.

## Fórmulas centrais
\[
M(i,j)\sim M(i \bmod n,\ j \bmod m)
\]

\[
\|v\|^2=\|p\|^2+\|q\|^2
\]

\[
c^2=a^2+b^2
\]

\[
c^2=a^2+b^2-2ab\cos\theta
\]

\[
h=\frac{\sqrt3}{2}a
\]

\[
\sin 60^\circ=\frac{\sqrt3}{2},\qquad
\cos 30^\circ=\frac{\sqrt3}{2},\qquad
\tan 60^\circ=\sqrt3
\]

\[
d=s\sqrt2,\qquad D=s\sqrt3
\]

\[
A=\pi(R^2-r^2)
\]

\[
T(u,v)=((R+r\cos v)\cos u,(R+r\cos v)\sin u,r\sin v)
\]

\[
I=\{k\mid \mathrm{Var}(\phi_k(s_i))<\tau\}
\]

\[
V=\{k\mid \mathrm{Var}(\phi_k(s_i))\ge \tau\}
\]

\[
\rho=\mathrm{StableAxes}(\Pi_c(S))
\]

\[
A_i=\{j\mid |\phi_j(s_i)-\rho_j|>\epsilon\}
\]

\[
\Delta^{(w)}(x,\rho)=W(\Phi(x)-\rho)
\]

\[
\pi_1(X)=0\Rightarrow X\ \text{simplesmente conexo}
\]

\[
L:X\to Z
\]

\[
d_*(x,y)\neq d_{\text{vis}}(x,y)
\]

\[
p_n(x_i)=y_i
\]

\[
r(x)=f(x)-p_n(x)
\]

\[
d_\phi(x,y)=d_Y(\phi(x),\phi(y))
\]

## Resultados

### Fechados
1. **Fechamento toroidal de vizinhança**  
2. **Relação métrica induzida por ortogonalidade**  
3. **Calibração equilátera**  
4. **Calibração por diagonais ortogonais**  

### Formalizáveis
1. **Equivalência estrutural sob transformação admissível**  
2. **Permutação restrita preserva a régua**  
3. **Critério discreto de ajuste/anomalia**  
4. **Régua ponderada com pesos e limiares**  
5. **Métrica latente/substituta para relações ocultas**  
6. **Interpretação polinomial como aproximação e não como ontologia da região extrapolada**  

### Hipótese forte
1. **Acoplamento discreto-contínuo com embedding**  

## Contagem honesta
- **Papers imediatos**: 1  
- **Papers possíveis**: 3  
- **Fórmulas nucleares**: ~25  
- **Resultados fechados**: 4  
- **Proposições formalizáveis**: 6  
- **Problemas abertos clássicos fechados**: 0  

## Gaps
- definir pesos \(W\)  
- fixar limiares \(\tau\) e \(\epsilon\)  
- axiomatizar transformações admissíveis  
- provar equivalência estrutural em casos não triviais  
- separar rigorosamente interpolação, extrapolação e geometria real  
- não misturar topologia, métrica e representação  

## Próximos passos
1. escrever a nota formal do sistema mínimo  
2. transformar a régua em definição com pesos e prova local  
3. escolher um caso aplicado (anomalia, embedding, ECC ou vizinhança)  
4. testar a noção de métrica latente em um conjunto discreto simples  
