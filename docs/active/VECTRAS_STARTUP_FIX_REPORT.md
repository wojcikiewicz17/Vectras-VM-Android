# VECTRAS STARTUP FIX REPORT

## F_ok
- Startup path auditado e documentado com origem/contrato/falha/fallback.
- Correção confirmada de política de porta SPICE dinâmica com placeholder.
- Correção de mensagem VNC/SPICE para evitar mistura de conceitos.

## F_gap
- Cobertura de testes de integração do startup completo (preflight→service→runtime).
- Testes multi-VM concorrentes para estados globais estáticos.
- Testes de quoting avançado em `finalextra` com content URI/path com espaços.

## F_noise
- Divergência doc/código detectada e corrigida no trecho SPICE.

## F_error
- Bug real corrigido: comando SPICE fixava `6999` e ignorava placeholder esperado pelo reservador de porta.
- Bug real corrigido: branch VNC external reportava erro de SPICE.

## F_next
1. Expandir testes unit/integration para abort contract (`VmFlowTracker ERROR`, `lastStartError`, ledger e poller stop).
2. Rodar CI Android com SDK/NDK completos para validação final de assemble/test.
3. Validar smoke runtime em arm64 e armv7.
