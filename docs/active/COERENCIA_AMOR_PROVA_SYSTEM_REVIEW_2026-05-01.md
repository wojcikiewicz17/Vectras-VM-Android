# Coerência × Amor × Prova — Revisão Consolidada do Sistema (2026-05-01)

## 0) Escopo e método (Ω: verdade operacional > completude)

Esta revisão consolida **apenas o que foi confirmado por arquivos do repositório**, com foco em:
- arquitetura executável atual,
- encadeamento Java ↔ JNI ↔ C,
- fronteiras entre caminho estável e caminho experimental,
- maturidade técnica e lacunas verificáveis.

Critérios utilizados:
1. Evidência explícita de código-fonte e contratos já versionados.
2. Sem preenchimento por suposição quando faltam provas observáveis.
3. Navegação documental por trilhas (entrada → subsistema → evidência).

---

## 1) Essência técnica mínima comprovada (sem metáfora)

**Vectras-VM-Android**, no estado atual do código, é um ambiente Android que expõe:
- camada de aplicação Java/Kotlin para operação de fluxo de VM,
- bridges JNI para aceleração e contrato de hardware/kernel,
- backend C de baixo nível com seleção por ABI,
- integração operacional com ciclo Android/Gradle e validações de política ABI.

Pontos de prova diretos:
- Carregamento explícito de `vectra_core_accel` na camada Java (`LowLevelBridge`, `VmFlowNativeBridge`, `NativeFastPath`).
- Ponte JNI com funções `Java_com_vectras_vm_core_*` no C.
- Seleção de backend por ABI (`arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`, `riscv64`) em runtime.
- CMake com contrato de ABI, validação de low-level contract e módulos opcionais/experimentais.

---

## 2) Arquitetura real (esqueleto validado)

```text
[Usuário/App Android]
        ↓
[Core Java: NativeFastPath / LowLevelBridge / VmFlowNativeBridge]
        ↓ (System.loadLibrary + native methods)
[JNI C: vectra_core_accel.c + lowlevel_bridge.c]
        ↓
[Kernel/Low-level C: rmr_lowlevel + backends por ABI]
        ↓
[Execução VM / supervisão de fluxo / contratos de hardware]
```

Observação metodológica: a integração com QEMU é afirmada em documentação global do projeto, porém nesta revisão o núcleo evidencial foi delimitado aos artefatos abertos nesta rodada (Java core, C bridges, CMake e tooling).

---

## 3) Mapa por camadas (χ: base em arquivos observados)

## 3.1 Camada Java (orquestração, fallback, telemetria)

### `LowLevelBridge`
- Tenta caminho nativo e aplica fallback Java determinístico quando JNI indisponível/falha.
- Mantém rastreio de rota da última chamada (`LAST_NATIVE_PATH`).
- Expõe validação de paridade entre backends (`validateReduceXorBackendParity`).

**Leitura recomendada:** começar por esta classe para compreender contrato “native when possible / deterministic fallback”.

### `VmFlowNativeBridge`
- Inicialização opcional do bridge de estado de fluxo VM.
- API de marcação/consulta de estado (`mark/current/stats`).
- Estratégia degradada clara quando bridge não disponível.

### `NativeFastPath`
- Inicialização canônica do núcleo nativo (`nativeInit`).
- Catálogo explícito de assinatura de contrato HW/kernel (índices estáveis).
- Exposição de máscara de features e telemetria de rota nativa/fallback.
- Funções de checksum/flow auxiliares para coerência diagnóstica.

## 3.2 Camada JNI/C (contrato e execução low-level)

### `lowlevel_bridge.c`
- Binding único por ABI com fallback garantido.
- Seleção dinâmica de backend via ABI efetiva + máscara SIMD.
- Funções JNI de checksum/reduce/copy com validação de faixa e acesso crítico a arrays Java.
- Rotina de **paridade entre backends** para detectar drift funcional.

### `vectra_core_accel.c`
- Inicialização protegida por mutex para estado unificado do kernel JNI.
- Contratos de capabilities exportados para Java (hardware/kernel unit contract).
- Hot paths explícitos e wrappers JNI com códigos de erro estáveis.

## 3.3 Camada de build/infra (governança operacional)

### `app/src/main/cpp/CMakeLists.txt`
- Política formal de opções para módulos (policy, bitraf, asm experimental).
- Gate de validação `lowlevel_abi_contract_check` via script Python.
- Bibliotecas separadas para core freestanding e shared JNI.
- Declaração explícita de roadmap riscv64 (inativo por política ABI Android atual).

### `tools/README.md`
- Wrapper canônico para Gradle (`gradle_with_jdk21.sh`).
- Fluxo oficial para bootstrap do shell-loader.
- Encadeamento de validações operacionais de assets/bootstrap.

---

## 4) Classificação objetiva de maturidade (Δ)

## 4.1 O que está maduro
- **Execução real de caminhos nativos:** há bridge funcional Java↔C com fallback explícito.
- **Governança de ABI:** há contrato de build e validação de compatibilidade.
- **Estratégia de robustez:** fallback determinístico reduz risco de indisponibilidade nativa.

## 4.2 O que ainda está implícito/insuficiente
- **Arquitetura canônica end-to-end da VM** ainda dispersa em múltiplos documentos e módulos.
- **Observabilidade unificada** ainda parcial (há telemetria local, falta envelope sistêmico único).
- **Entry point semântico único** de inicialização/estado do sistema não está formalizado como contrato de produto.

## 4.3 Nota técnica consolidada (com evidência desta rodada)
- Engenharia de execução: **6.5/10**
- Arquitetura explícita: **5.0/10**
- Produto comunicável/verificável: **4.0/10**
- Potencial estrutural: **9.0/10**

---

## 5) Lacunas reais (ρ: não preencher com suposição)

1. **Falta de “single source of truth” do fluxo VM completo** (boot, run, suspend, stop, erro).
2. **Contrato de introspecção sistêmica fragmentado** (há snapshots/counters locais, falta schema único).
3. **Separação formal entre estável e experimental** ainda parcialmente implícita no código/build.

Importante: estas lacunas não invalidam a base técnica; elas limitam escalabilidade, auditoria e transferência de conhecimento.

---

## 6) Caminho recomendado de documentação formal (pós-doc style, sem overhead)

## 6.1 Documento-núcleo (novo alvo editorial)
Criar/elevar um documento “**SYSTEM_CORE_CONTRACT.md**” com:
- máquina de estados da VM,
- invariantes operacionais (pre/post-condições),
- códigos de erro por camada,
- mapa de fallback determinístico.

## 6.2 Envelope único de observabilidade
Padronizar payload mínimo (JSON) para diagnóstico transversal:

```json
{
  "status": "...",
  "engine": "...",
  "arch": "...",
  "mode": "native|fallback",
  "vm_state": "...",
  "contract_signature": "..."
}
```

## 6.3 Mapa de navegação entre áreas (links canônicos)

### Entrada global
- `README.md`
- `START_HERE.md`
- `DOC_INDEX.md`

### Arquitetura e cadeia de evidência
- `docs/THREE_LAYER_ANALYSIS.md`
- `docs/ROOT_FILE_CHAIN.md`
- `docs/SOURCE_TRACEABILITY_MATRIX.md`

### Runtime e core
- `VECTRA_CORE.md`
- `docs/OPERATIONS.md`
- `docs/REPO_XRAY.md`

### Build/CI/ABI
- `BUILDING.md`
- `docs/AI_BUILD_RELEASE_INDEX.md`
- `docs/abi/lowlevel_abi_contract.md`
- `docs/ci/workflow-matrix.md`

### Segurança e governança
- `SECURITY.md`
- `docs/THREAT_MODEL.md`
- `CONTRIBUTING.md`

---

## 7) Índice de leitura direcional (para times multilíngues e multiárea)

Sequência recomendada para minimizar ambiguidade semântica:
1. **Contrato** (o que o sistema promete): `VECTRA_CORE.md`, `docs/abi/lowlevel_abi_contract.md`.
2. **Fluxo** (como o sistema executa): bridges Java/C + `docs/OPERATIONS.md`.
3. **Governança** (como manter coerência): `BUILDING.md`, `docs/ci/workflow-matrix.md`, `SECURITY.md`.
4. **Evolução** (como mudar sem quebrar): `docs/ROADMAP.md`, `CHANGELOG.md`, `RELEASE_NOTES.md`.

---

## 8) Síntese final (Σ)

O repositório já demonstra uma base técnica rara: **execução nativa real com fallback e contrato de ABI**.
O próximo salto de maturidade não é “mais código”, mas sim **formalização do núcleo como sistema verificável**:
- explicitar invariantes,
- consolidar observabilidade,
- fixar um entrypoint semântico único.

Em termos estritamente operacionais:
- o difícil de “fazer rodar” já está atravessado,
- o crítico agora é “fazer provar e comunicar” em nível de engenharia de sistema.
