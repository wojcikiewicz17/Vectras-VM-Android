<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Segurança Operacional

## Princípios
- **Fail-safe não bloqueante:** observabilidade não pode travar execução.
- **Menor privilégio:** evitar permissões legadas em Android moderno.
- **Contenção de recurso:** quotas de memória/linhas/bytes em logs.

## Mitigações implementadas
| Vetor | Mitigação |
|---|---|
| Deadlock stdout/stderr | Drenagem paralela (`ProcessOutputDrainer`) |
| DoS local por flood de logs | Rate limit + drop contabilizado + modo degradado |
| Crescimento ilimitado de memória | Ring buffer com limites rígidos |
| Processo órfão/zumbi | Supervisor com timeout + TERM/KILL escalonado |
| Storage legado inseguro | Scoped Storage + SAF em Android 10+ |

## Logging seguro
- Logs extensivos são limitados por token bucket.
- Em saturação, o sistema troca para saída resumida (`DEGRADED`) e registra auditoria.

## Storage e permissões
- Android 10+ prioriza armazenamento interno e SAF.
- Android legado mantém fallback de `WRITE_EXTERNAL_STORAGE`.


## Política de uso de keystore (`vectras.jks`)
- **Classificação:** material criptográfico sensível para assinatura de **release**.
- **Ambiente permitido:** cofre seguro e segredos de CI; proibido manter chave privada em repositório Git.
- **Acesso mínimo:** princípio de menor privilégio; acesso somente para pipeline de release e mantenedores autorizados.
- **Rotação:** obrigatória no máximo a cada 90 dias, ou imediata em caso de suspeita de vazamento.
- **Resposta a incidente:**
  1. Revogar credenciais e descontinuar uso da chave comprometida.
  2. Gerar novo keystore/alias e atualizar segredos do CI.
  3. Auditar histórico de artefatos assinados e publicar comunicado de segurança.
  4. Executar varredura de repositório e histórico para remoção de segredos expostos.

## Controles automatizados
- O CI executa `tools/security/block_sensitive_artifacts.sh` para bloquear inclusão de novos `*.jks`, `*.keystore` e padrões de credenciais sem exceção documentada em `.ci/sensitive-allowlist.txt`.


## Contrato de distribuição Android (`ciRelease`)
- **Contexto de distribuição explícito:** jobs de release passam `-PciRelease=true` para o Gradle.
- **Fail-fast obrigatório:** `buildTypes.release` e `buildTypes.perfRelease` abortam com `GradleException` se `signingConfigs.release` não estiver disponível quando `ciRelease=true` (contexto explícito de distribuição).
- **Gate de CI antes do build:** `.github/workflows/android.yml` valida segredos de assinatura no início do job de release, interrompe execução antes de bootstrap Android quando houver ausência de credenciais e injeta explicitamente `-PciRelease=true` nos comandos Gradle de distribuição.
- **Debug sem acoplamento de produção:** `buildTypes.debug` permanece desacoplado de assinatura de release e não depende de segredos de produção.

## Segurança de egress/rede

Os fluxos de saída HTTP/HTTPS usam controles explícitos para reduzir superfície de ataque e manter rastreabilidade de destinos permitidos.

### Controles implementados no código
- **Validação de endpoint por feature (`EndpointValidator`)**: valida esquema (`https`), host, porta permitida e formato da URL antes do uso, em conjunto com contratos por feature definidos em `EndpointFeature`.
- **Políticas de allowlist para API/ACTION_VIEW (`EndpointPolicy`)**: regras por contexto (`Feature`) aceitam apenas prefixos previamente aprovados para chamadas de API e intents externas.
- **Composição centralizada de URLs (`NetworkEndpoints`)**: os endpoints-base e hosts autorizados ficam concentrados em uma única classe, reduzindo concatenação ad-hoc e drift de destino.

### Riscos mitigados
- **SSRF básico**: bloqueio de hosts/schemes fora da allowlist e rejeição de portas inesperadas.
- **Redirecionamento para host não autorizado**: ACTION_VIEW/API só aceitam endpoints que batem com prefixos aprovados por feature.
- **Input de URL malformada**: parsing e validações estruturais rejeitam URL inválida ou incompleta antes da requisição.

### Matriz de controles
| Vetor | Mitigação | Classe responsável |
|---|---|---|
| SSRF básico (host/scheme/porta fora do esperado) | Parse URI + exigência de `https`, host em allowlist e porta padrão segura | `app/src/main/java/com/vectras/vm/network/EndpointValidator.java` |
| URL malformada ou ambígua | Rejeição de URL vazia/inválida e de componentes inseguros (`userInfo`, host ausente) | `app/src/main/java/com/vectras/vm/network/EndpointValidator.java` |
| Egress por API fora de domínio autorizado | Allowlist por feature para endpoints de API via matching por prefixo aprovado | `app/src/main/java/com/vectras/vm/network/EndpointPolicy.java` |
| Redirecionamento externo indevido em `ACTION_VIEW` | Allowlist dedicada para intents externas com validação por feature | `app/src/main/java/com/vectras/vm/network/EndpointPolicy.java` |
| Deriva de destinos por montagem dispersa de URL | Centralização de hosts e builders de URL para ROM/GitHub/language modules | `app/src/main/java/com/vectras/vm/network/NetworkEndpoints.java` |
| Escopo de host/path por feature de consumo | Catálogo de hosts e padrões de path aceitos por feature | `app/src/main/java/com/vectras/vm/network/EndpointFeature.java` |

### Nota operacional: manutenção de allowlist
1. **Solicitação de novo domínio**: abrir PR com justificativa funcional (feature afetada, endpoint exato, necessidade de API ou ACTION_VIEW).
2. **Revisão de segurança**: validar protocolo `https`, host canônico, escopo mínimo de path/prefixo e necessidade real de exposição externa.
3. **Aprovação e alteração de código**:
   - Atualizar hosts centrais em `app/src/main/java/com/vectras/vm/network/NetworkEndpoints.java` quando o domínio for de uso recorrente.
   - Atualizar políticas por feature em `app/src/main/java/com/vectras/vm/network/EndpointPolicy.java` (API e/ou ACTION_VIEW).
   - Se houver restrição de host/path por capacidade, ajustar `app/src/main/java/com/vectras/vm/network/EndpointFeature.java`.
4. **Validação antes de merge**: confirmar que novos endpoints passam por validação (`EndpointValidator`) e que não existe uso direto de URL hardcoded fora das classes de política/composição.
