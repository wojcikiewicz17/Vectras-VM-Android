# VECTRAS STARTUP INCONSISTENCIES

## I1 — INCONSISTENT_STARTUP_PORT_POLICY (FIXED)
- Evidência: `MainStartVM.reserveSpicePortIfNeeded(...)` espera `StartVM.SPICE_PORT_PLACEHOLDER` enquanto `StartVM.env(...)` usava `port=6999`.
- Correção aplicada: SPICE agora usa `port=__VECTRAS_SPICE_PORT__` e `MainStartVM` substitui porta reservada.

## I2 — VNC error message mismatch (FIXED)
- Evidência: branch VNC external mostrava mensagem de SPICE.
- Correção aplicada: mensagem específica `VNC port in use.`; branch de reserva SPICE usa `SPICE port reservation failed.`.

## I3 — buildCommand token/quote risks
- Estado: NEEDS_TEST
- Observação: `buildCommand` faz trim/join e `finalextra` entra como bloco único; requer regressões para paths com espaço/quotes.

## I4 — RuntimeContract coherence
- Estado: PARCIAL
- Observação: snapshot `prepared` criado em `StartVM.env`, `starting/running` em `MainStartVM`; abort paths precisam cobertura de teste para persistência de `error`.

## I5 — pending launch/global static state
- Estado: NEEDS_TEST
- Observação: `pendingVMID`, `lastVMID`, flags estáticas podem causar risco de corrida multi-launch.

## I6 — NativeFastPath fallback
- Estado: OK/PARCIAL
- Observação: fallback Java existe; falta teste de integração no caminho de startup completo.

## I7 — UI targets headless/X11/VNC/SPICE
- Estado: PARCIAL
- Observação: mapping existe; faltam testes de integração para comando final por target.

## I8 — MainService command handoff
- Estado: PARCIAL
- Observação: `finalCommand` montado uma vez; precisa teste anti-duplicação wrapper/audio/display.
