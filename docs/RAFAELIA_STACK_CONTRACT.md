# RAFAELIA Stack Contract

Este documento define contratos explícitos entre os 4 repositórios da stack RAFAELIA/Vectras.

## 1) qemu_rafaelia

- **Papel:** emulador base para ciclo lógico da VM, integração TCG/icount, IPC RAFAELIA e evolução de mecanismos de digest-chain/TTL quando disponíveis no upstream do fork.
- **Forma de integração em Vectras:** **fonte externa validada por manifesto** (`tools/ci/external_sources.manifest`) e verificação CI (`tools/ci/verify_external_sources.sh`).
- **Não é vendor interno por padrão.**

## 2) Vectras-VM-Android

- **Papel:** runtime Android/VM que integra Gradle + CMake + JNI para empacotamento Android e ponte de execução da VM.
- **Escopo técnico:**
  - build Android (Gradle/AGP);
  - build nativo (CMake/NDK);
  - ponte JNI (`app/src/main/cpp`);
  - assets/empacotamento Android;
  - launch/configuração de execução QEMU no contexto do app.
- **Política ABI:** oficial separada de validações internas (arm64 oficial; arm32+arm64 e matriz ampliada como trilhas controladas de validação interna).

## 3) androidx_RmR

- **Papel:** fork AndroidX modificado/otimizado para requisitos de runtime do projeto (GC behavior, locality/cache e impactos em módulos como Camera/Compose/Room quando aplicável).
- **Forma de integração em Vectras:** **fonte externa validada por manifesto**, sem duplicação vendor automática.

## 4) RafCoder

- **Papel:** microcore determinístico C/ASM para operações lowlevel, snapshot/reentrância, cobertura arquitetural ARMv7/AArch64 e base para benchmarks lowlevel.
- **Integração no Vectras:** componentes lowlevel e primitivas no módulo nativo do app (C/C++), sem obrigatoriedade de clone externo dedicado no fluxo atual.
- **Dependência atual:** operacional/conceitual e de código incorporado no próprio repositório Vectras (não como terceiro clone obrigatório no manifesto atual).

---

## Matriz de integração (fonte de verdade)

| Repositório | Fonte de verdade | Branch contratada | Caminho local esperado | Etapa de validação | Artefatos | Riscos |
|---|---|---|---|---|---|---|
| qemu_rafaelia | `tools/ci/external_sources.manifest` | `master` (até revisão explícita) | `.third_party_forks/qemu_rafaelia` | `tools/ci/verify_external_sources.sh --check-remote` (e `--sync-clone` quando necessário) | Evidência de clone/sync + uso indireto em build/runtime | drift de branch, mudanças incompatíveis de IPC/TCG |
| androidx_RmR | `tools/ci/external_sources.manifest` | `androidx-main` | `.third_party_forks/androidx_RmR` | `tools/ci/verify_external_sources.sh --check-remote` (e `--sync-clone` quando necessário) | Evidência de clone/sync + consumo em integração Android | drift de API AndroidX forkada |
| Vectras-VM-Android | árvore Git deste repositório | `master` (base) | raiz do repositório | workflows canônicos (`host-ci`, `android-ci`, `quality-gates`) | APK/AAB, libs nativas, logs CI, status canônico | divergência entre docs/CI/build real |
| RafCoder | contratos lowlevel dentro do Vectras | n/a (integrado) | `app/src/main/cpp` + `engine/rmr/*` | gate lowlevel (`tools/ci/validate_lowlevel_abi.sh` e validações CMake/CI) | bibliotecas nativas + evidências ABI | regressão asm/c fallback, incompatibilidade ABI |

## Regras de não ambiguidade

1. Fonte externa ≠ vendor: qemu_rafaelia e androidx_RmR são rastreados via manifesto e validação remota.
2. Alterar política de pinagem (branch/SHA) exige atualização de `tools/ci/external_sources.manifest` + registro em estado do projeto.
3. RafCoder não deve ser apresentado como clone externo obrigatório enquanto não entrar explicitamente no manifesto.
4. Build status oficial deve apontar para validação temporal concreta; ausência de execução no commit atual não pode ser mascarada como “build atual aprovado”.
