# LOWBASIC ABI + Bootloader/Bootstrap Hotfix (RafaelIA/Vectra)

## Objetivo
Aplicar um caminho low-level para integração entre módulos com foco em:
- zero dependência de libc no contrato de handoff;
- execução branchless no passo crítico de processamento;
- alinhamento ABI entre bootloader, bootstrap e kernel.

Arquivo principal: `tools/baremetal/rafcode_phi/include/rafcode_phi_lowbasic.h`.

## O que foi implementado
1. **Contrato ABI mínimo de boot**
   - `rafphi_boot_handoff_t` com campos de magic/version/arch/flags e ponteiros IN/OUT.
   - `RAFPHI_BOOT_MAGIC` + `RAFPHI_BOOT_VERSION` para handshake determinístico.

2. **Flags de estado sem fluxo alto nível**
   - `RAFPHI_F_BOOT_OK`, `RETRY`, `DENY`, `ABI_MISM`, `CRC_FAIL`, `PTR_INVALID`.

3. **Passo lowbasic AArch64**
   - macro `RAFPHI_LOWBASIC_A64_STEP()` com prefetch, load vetorial, FMA, CRC32 HW e máscara branchless.

4. **Passo lowbasic x86_64**
   - macro `RAFPHI_LOWBASIC_X64_STEP()` com SSE load/store e CRC32 intrínseco de instrução.

5. **Validação mínima de handoff (hotfix ABI)**
   - `rafphi_boot_handoff_validate(...)` retorna estado por máscara em vez de pipeline de controle complexo.

## Semântica operacional
- Entrada e saída são tratadas como fluxo contínuo (organismo vivo): o estado segue por máscara.
- O hotfix evita acoplamento em runtime userspace e reduz superfície de inconsistência ABI.

## Diff lógico esperado no sistema
- **Antes**: bootstrap e módulos dependiam de contratos implícitos/documentação dispersa.
- **Depois**: existe um cabeçalho único de contrato lowbasic para passagem de estado entre estágios.

## O que ainda falta
1. **Wire-up direto no boot path real**
   - conectar `rafphi_boot_handoff_validate` no ponto de entrada do bootstrap nativo.
2. **Teste integrado por ABI policy matrix**
   - cobrir `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` com fixture de handoff inválido/valido.
3. **Medição formal de overhead**
   - benchmark de latência por bloco (64B) comparando caminho anterior vs macro lowbasic.
4. **Fallback explícito riscv64**
   - ausência de macro lowbasic específica para `riscv64`.
5. **Assinatura criptográfica do handoff**
   - atualmente há validação estrutural + CRC parcial; falta assinatura/autenticidade.

## Escopo
Este hotfix é incremental: prepara a infraestrutura de ABI/bootstrap para integração progressiva no sistema todo, sem quebrar contrato existente.

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
