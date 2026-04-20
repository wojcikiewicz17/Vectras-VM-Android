<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# SURGICAL PATCHSET — RAFAELIA ingestão textual

## Origem e método
- Patchsets recuperados da origem externa via histórico Git (`5924890`), extraídos fora da árvore (`/tmp/rafaelia_patchsets`).
- Integração feita como mudanças textuais versionadas (sem reintroduzir blobs `.zip`).

## Navegação por módulo
- [`engine/rmr`](#module-engine-rmr)
- [`bug/core`](#module-bug-core)
- [`app`](#module-app)

---

## Patchset 1 — `RAFAELIA_VECTRA2_PATCHES.zip`

<a id="module-engine-rmr"></a>
### Módulo `engine/rmr`
- `engine/rmr/include/rafaelia_formulas_core.h` (novo): API C bare-metal para fórmulas RAFAELIA.
- `engine/rmr/src/rafaelia_formulas_core.c` (novo): implementação da API de fórmulas.
- `engine/rmr/include/rmr_math_fabric.h` (alterado): extensão RAFAELIA no Math Fabric.
- `engine/rmr/src/rmr_math_fabric.c` (alterado): implementação dos novos operadores Q16.16.
- `engine/rmr/include/rmr_torus_flow.h` (novo): API pública canônica do kernel toroidal.
- `engine/rmr/src/rmr_torus_flow.c` (novo): implementação determinística em Q16.16 com injeção de gramática/checksum.
- `engine/rmr/src/rmr_bench_suite.c` (alterado): benchmark oficial consumindo `rmr_torus_flow` (`kind=5`).
- `demo_cli/src/rmr_torus_flow_selftest.c` (novo): selftest de determinismo/progressão.
- `CMakeLists.txt` (alterado): adiciona fontes RAFAELIA ao build `rmr`.

#### Símbolos/funções migrados (`engine/rmr`)
- `raf_phi_ethica`
- `raf_kernel_step`
- `raf_vortex_metric`
- `raf_retroalimentar`
- `raf_spiral`
- `raf_toroid_delta_pi_phi`
- `raf_trinity_633`
- `raf_fibonacci_rafael_step`
- `raf_information_bits`
- `raf_logical_capacity`
- `raf_voo_quantico`
- `raf_evolucao_rafaelia`
- `raf_in_fomega_band`
- `raf_synaptic_weight`
- `raf_cycle_step`
- `RmR_MathFabric_RafaeliaExtend`
- `RmR_MathFabric_Spiral`
- `RmR_MathFabric_FibRafaelStep`
- `RmR_TorusFlow_RunDeterministic`

<a id="module-app"></a>
### Módulo `app`
- `app/src/main/java/com/vectras/vm/core/MathUtils.java` (alterado): extensões de fórmulas RAFAELIA.
- `app/src/main/java/com/vectras/vm/rafaelia/RafaeliaKernelV22.java` (alterado): integração estendida do índice RAFAELIA.
- `app/src/main/java/com/vectras/vm/rafaelia/RafaeliaCognitiveLoop.java` (novo): loop cognitivo ψ→χ→ρ→Δ→Σ→Ω.
- `app/src/main/java/com/vectras/vm/rafaelia/RafaeliaFormulas.java` (novo): catálogo de fórmulas/literais.

#### Símbolos/funções migrados (`app`)
- `MathUtils.fibRafaelStep`
- `MathUtils.spiral`
- `MathUtils.toroidDeltaPiPhi`
- `MathUtils.trinity633`
- `MathUtils.logicalCapacity`
- `RafaeliaKernelV22.phiEthica`
- `RafaeliaKernelV22.kernelStep`
- `RafaeliaCognitiveLoop.step`
- `RafaeliaCognitiveLoop.run`

---

## Patchset 2 — `RAFAELIA-CHANGED-FILES.zip`

<a id="module-bug-core"></a>
### Módulo `bug/core`
- `bug/core/bitraf.c` (alterado): diagnóstico determinístico de máscara de rotas.
- `bug/core/zipraf_core_bridge.c` (alterado): utilitário CRC32 IEEE para inspeção ZIP.

#### Símbolos/funções migrados (`bug/core`)
- `bitraf_popcount32`
- `bitraf_route_popcount`
- `zr_crc32_ieee`

> Arquivos auxiliares do zip original (`unified-ci.yml`, `llamaRafaelia-CMakeLists.txt`) não foram importados diretamente por não serem caminhos canônicos deste repo; a integração efetiva foi mapeada para módulos existentes.

---

## Patchset 3 — `ZIPDROP_DELTA_fix.zip`

### Ingestão documental
- `_incoming/INTEGRATION_EVIDENCE.md` (novo/expandido): evidência por arquivo (origem → destino).
- `_incoming/README.md` (alterado): classificação factual (`absorbed`, `backlog`, `archived`).

### Observação
- O workflow de zipdrop já existia no repositório em `.github/workflows/zipdrop.yml`; portanto, não foi necessária alteração adicional desse arquivo nesta aplicação.
