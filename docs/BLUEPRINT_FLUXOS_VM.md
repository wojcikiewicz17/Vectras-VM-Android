<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Blueprint Canônico de Fluxos Operacionais de VM

Este blueprint consolida o fluxo operacional **fim-a-fim** para quatro jornadas críticas do app:

1. Criação de VM.
2. Importação de VM (`.cvbi`/`.cvbi.zip`).
3. Execução de VM.
4. Diagnóstico e recuperação.

> Escopo técnico: integração entre UI Android (`com.vectras.vm...`), orquestração de processo/supervisão (`VMManager`, `ProcessSupervisor`) e controle QEMU/QMP (`com.vectras.qemu...`).

---

## 1) Máquina de estados canônica

### 1.1 Estados
- **Idle**: VM cadastrada/parada e sem processo em execução.
- **Creating**: validação de metadados e persistência de `roms-data.json` + `rom-data.json`.
- **Ready**: VM válida e apta a iniciar.
- **Starting**: pré-checks + disparo do comando QEMU/serviço.
- **Running**: processo QEMU ativo e UI conectada (VNC/X11/headless).
- **Stopping**: tentativa de parada graciosa (QMP) e fallback.
- **Stopped**: processo encerrado e supervisor removido.
- **Error**: falha bloqueante em validação, importação, preflight, execução ou parada.
- **Degraded**: operação com degradação (ex.: pressão de logs/flood).

### 1.2 Transições (modelo operacional)
- `Idle -> Creating -> Ready`
- `Ready -> Starting -> Running`
- `Running -> Stopping -> Stopped`
- `Running -> Degraded -> Running`
- `Starting/Running/Stopping -> Error`
- `Error -> Idle` (após correção/recovery)

### 1.3 Transições reais em módulos
- `START -> VERIFY -> RUN -> STOP` e caminho de exceção `DEGRADED`/`FAILOVER` no `ProcessSupervisor`. (`com.vectras.vm.core.ProcessSupervisor`).
- Guardas de estado `STOPPED/STARTING/RUNNING/STOPPING` no `VMManager` para prevenir corrida e duplicação de start/stop.

---

## 2) Fluxo: criação de VM

### 2.1 Pré-condições
- Nome da VM informado (`title` não vazio).
- Diretório de VM criável em `AppConfig.vmFolder + vmID`.
- `roms-data.json` válido (ou recriável).

### 2.2 Passo a passo (Ação -> Resultado esperado)
1. Usuário aciona `startCreateVM()`.
   - **Esperado**: validação de campos mínimos e alerta se houver risco (QEMU params vazios, sem storage).
2. App chama `createNewVM()` na activity.
   - **Esperado**: valida/repara `roms-data.json`; cria entrada com `imgName`, `imgPath`, `imgExtra`, `imgArch`, `vmID`, `qmpPort`.
3. Persistência via `VMManager.createNewVM(...)` + `addToVMList(...)` + `writeToVMConfig(...)`.
   - **Esperado**: VM aparece na listagem e pode ser iniciada.

### 2.3 Falhas e recovery
- **Falha**: nome vazio.
  - **Sinal**: dialog `need_set_name`.
  - **Recovery**: preencher nome e repetir.
- **Falha**: criação de diretório falha.
  - **Sinal**: dialog `unable_to_create_the_directory_to_create_the_vm`.
  - **Recovery**: validar permissão/storage e tentar novamente.
- **Falha**: `roms-data.json` inválido.
  - **Sinal**: fluxo `need_fix_json_before_create`.
  - **Recovery**: rotina de fix (`startFixRomsDataJson`) e reexecutar criação.
- **Falha**: persistência final falha.
  - **Sinal**: `unable_to_save_please_try_again_later`.
  - **Recovery**: revisar filesystem e repetir.

### 2.4 Pontos de integração
- `com.vectras.vm.VMCreatorActivity`
  - `startCreateVM()`, `createVMFolder(...)`, `createNewVM()`.
- `com.vectras.vm.VMManager`
  - `createNewVM(...)`, `editVM(...)`, `writeToVMList(...)`, `writeToVMConfig(...)`, `isRomsDataJsonValid(...)`.

### 2.5 Critérios de validação
- **Inputs aceitos**:
  - `name` obrigatório; `drive`/`cdrom` opcionais (mas storage ausente gera alerta).
  - `params` aceito vazio com confirmação explícita.
- **Mensagens de erro**:
  - `need_set_name`, `unable_to_create_the_directory_to_create_the_vm`, `unable_to_save_please_try_again_later`.
- **Logs esperados**:
  - Em debug, rastros de existência/lookup em `VMManager` durante validação de VMs.

---

## 3) Fluxo: importação de VM (.cvbi/.cvbi.zip)

### 3.1 Pré-condições
- Arquivo com extensão suportada (`.cvbi` ou `.cvbi.zip`).
- URI/path acessível.
- Diretório alvo de VM criado com sucesso.

### 3.2 Passo a passo (Ação -> Resultado esperado)
1. Usuário seleciona artefato para import.
   - **Esperado**: `importRom(...)` valida extensão.
2. App escolhe modo de leitura (URI vs path).
   - **Esperado**: origem válida detectada; caso contrário aborta com erro.
3. App extrai ZIP para `AppConfig.vmFolder + vmID`.
   - **Esperado**: progress dialog + extração concluída.
4. Pós-extração em `afterExtractCVBIFile(...)`.
   - **Esperado**:
     - Se existe `rom-data.json`: hidrata campos (`title`, `drive`, `qemu`, `icon`, `cdrom`).
     - Se não existe: tenta `quickScanDiskFileInFolder(...)` e aplica fallback.

### 3.3 Falhas e recovery
- **Falha**: formato inválido.
  - **Sinal**: `format_not_supported_please_select_file_with_format_cvbi`.
  - **Recovery**: selecionar artefato válido.
- **Falha**: origem inválida.
  - **Sinal**: `error_CR_CVBI1`.
  - **Recovery**: reabrir seletor e garantir permissões.
- **Falha**: extração inválida/conteúdo inconsistente.
  - **Sinal**: `could_not_process_cvbi_file_content`.
  - **Recovery**: validar integridade do pacote e reimportar.
- **Falha**: ausência de `rom-data.json` + sem disco detectado.
  - **Sinal**: `error_CR_CVBI3`.
  - **Recovery**: reconstruir pacote ou importar outro artefato.
- **Falha recuperável**: sem `rom-data.json` mas disco detectado.
  - **Sinal**: `error_CR_CVBI2`.
  - **Recovery**: ajustar metadados manualmente e salvar VM.

### 3.4 Pontos de integração
- `com.vectras.vm.VMCreatorActivity`
  - `importRom(...)`, `afterExtractCVBIFile(...)`, `createVMFolder(...)`.
- `com.vectras.vm.utils.ZipUtils`
  - extração via URI/path.
- `com.vectras.vm.VMManager`
  - `quickScanDiskFileInFolder(...)` para fallback de disco.

### 3.5 Critérios de validação
- **Inputs aceitos**: arquivo `.cvbi`/`.cvbi.zip` por URI ou path.
- **Mensagens de erro**: `error_CR_CVBI1`, `error_CR_CVBI2`, `error_CR_CVBI3`, `could_not_process_cvbi_file_content`.
- **Logs esperados**:
  - `importRom: Extracting from ... to ...` no fluxo nominal.

---

## 4) Fluxo: execução de VM

### 4.1 Pré-condições
- VM em estado `Ready` (metadados persistidos).
- Preflight de binário QEMU ok.
- Comando considerado seguro por `isthiscommandsafe(...)`.
- Restrições de ambiente aprovadas (shared folder, porta VNC, Termux X11 quando aplicável).

### 4.2 Passo a passo (Ação -> Resultado esperado)
1. UI solicita `MainStartVM.startNow(...)`.
   - **Esperado**: definição de modo (X11/VNC/headless), contexto da VM e cache runtime.
2. Preflight (`SetupFeatureCore.runVmStartPreflight`) + validações.
   - **Esperado**: falhas bloqueiam start com feedback de correção.
3. Montagem/composição de comando final QEMU e início de `MainService`.
   - **Esperado**: ledger de launch + serviço ativo.
4. Poller aguarda sinal de VM ativa (`QMP socket` / flags de erro).
   - **Esperado**: transição para `Running` e attach de frontend (VNC/X11) quando não headless.
5. Registro de processo em supervisor.
   - **Esperado**: `VMManager.registerVmProcess(...)` conecta o processo ao `ProcessSupervisor`.

### 4.3 Falhas e recovery
- **Falha**: preflight inválido.
  - **Sinal**: `VM preflight failed` + dialog de reinstalação.
  - **Recovery**: executar fluxo de reinstall setup.
- **Falha**: comando inseguro.
  - **Sinal**: `harmful_command_was_detected` + reason.
  - **Recovery**: corrigir parâmetros QEMU e reiniciar.
- **Falha**: VM já em execução.
  - **Sinal**: toast `This VM is already running.`.
  - **Recovery**: attach na sessão existente.
- **Falha**: porta VNC em uso.
  - **Sinal**: `the_vnc_server_port_you_set_is_currently_in_use_by_other`.
  - **Recovery**: alterar porta em settings.
- **Falha**: parada com erro na execução.
  - **Sinal**: `isQemuStopedWithError=true` + dialog de erro em parser.
  - **Recovery**: usar `startTryAgain(...)` após ajuste.

### 4.4 Pontos de integração
- `com.vectras.vm.main.core.MainStartVM`
  - `startNow(...)`, `LaunchPoller`, `showProgressDialog(...)`.
- `com.vectras.vm.StartVM`
  - geração de args e seleção de binário/arquitetura.
- `com.vectras.vm.MainService`
  - execução do comando QEMU.
- `com.vectras.vm.VMManager`
  - segurança de comando e estado de runtime.

### 4.5 Critérios de validação
- **Inputs aceitos**:
  - `env` com charset seguro e sem operadores proibidos.
  - `vmID` resolvido e diretório de cache criável.
- **Mensagens de erro**:
  - `harmful_command_was_detected`, `vm_cache_dir_failed_to_create_content`, alertas de shared folder/VNC.
- **Logs esperados**:
  - `engine-only mode enabled (headless=true)` (quando aplicável).
  - `Virtual machine running.` ao finalizar attach.
  - dump de `Params:` em `VectrasStatus` e `Log.d("HomeStartVM", ...)`.

---

## 5) Fluxo: diagnóstico e recuperação

### 5.1 Pré-condições
- VM em `Running`, `Degraded` ou `Error`.
- Acesso a logs (runtime/app/QMP/crash).

### 5.2 Passo a passo (Ação -> Resultado esperado)
1. Verificar estado e trilha de supervisão.
   - **Esperado**: `ProcessSupervisor` reporta estado + transições gravadas em `AuditLedger`.
2. Tentar parada graciosa.
   - **Esperado**: `stopGracefully(true)` tenta QMP (`system_powerdown`) e fallback `TERM -> KILL`.
3. Inspecionar logs de execução.
   - **Esperado**: `LoggerFragment`/`LogcatRuntime` exibem eventos recentes.
4. Inspecionar crash persistido.
   - **Esperado**: `CrashHandler` salva em `AppConfig.lastCrashLogPath` e `LastCrashActivity` mostra conteúdo.
5. Aplicar recovery orientado por causa.
   - **Esperado**: retorno a `Idle/Ready` para novo start.

### 5.3 Falhas e recovery
- **Falha**: QMP indisponível / handshake inválido.
  - **Sinal**: `Could not connect to QMP`, `QMP greeting/capabilities contract not satisfied`.
  - **Recovery**: seguir fallback TERM/KILL e validar socket/localização QMP.
- **Falha**: timeout de parada.
  - **Sinal**: transição para `FAILOVER` com `kill_timeout`.
  - **Recovery**: encerrar forçado, remover supervisor e reinicializar VM.
- **Falha**: exceção não capturada no app.
  - **Sinal**: `uncaughtException` + log persistido.
  - **Recovery**: abrir `LastCrashActivity`, coletar stacktrace e reproduzir.

### 5.4 Pontos de integração
- `com.vectras.vm.core.ProcessSupervisor`
  - `onDegraded(...)`, `stopGracefully(...)`, `transition(...)`.
- `com.vectras.vm.VMManager`
  - `stopVmProcess(...)`, `registerVmProcess(...)`.
- `com.vectras.qemu.utils.QmpClient`
  - negociação de capabilities, envio de `system_powerdown`, leitura de respostas.
- `com.vectras.vm.Fragment.LoggerFragment` + `com.vectras.vm.core.LogcatRuntime`.
- `com.vectras.vm.crashtracker.CrashHandler` + `LastCrashActivity`.

### 5.5 Critérios de validação
- **Inputs aceitos**:
  - comandos QMP JSON válidos.
  - `vmId` normalizado (com fallback `unknown-*` quando necessário).
- **Mensagens de erro**:
  - logs QMP (`Could not connect...`, `I/O error...`), dialogs de falha de execução/parada.
- **Logs esperados**:
  - trilha de transições no `AuditLedger` com `from`, `to`, `cause`, `action`, `stallMs`.
  - registros de crash contendo cabeçalho de dispositivo + stacktrace.

---

## 6) Matriz rápida de validação por fluxo

| Fluxo | Entrada mínima | Sinais de sucesso | Erros principais | Recovery padrão |
|---|---|---|---|---|
| Criação | `name`, `vmID` | VM persistida em `roms-data.json` + `rom-data.json` | nome vazio, falha de diretório, JSON inválido | corrigir campo/permissão; fix JSON; repetir |
| Importação | `.cvbi`/`.cvbi.zip` | extração ok + campos hidratados | `CR_CVBI1/2/3`, formato inválido, extração falha | selecionar artefato válido; corrigir pacote |
| Execução | `env` seguro + preflight ok | socket QMP ativo; VM em running | comando inseguro, VNC porta ocupada, preflight falho | ajustar configuração, reinstalar setup, retry |
| Diagnóstico | `vmId` + logs | parada confirmada + evidência coletada | QMP indisponível, kill timeout, crash app | fallback TERM/KILL; coletar crashlog; relançar |

---

## 7) Navegação relacionada
- Arquitetura operacional: [`docs/ARCHITECTURE.md`](ARCHITECTURE.md)
- Operação e práticas: [`docs/OPERATIONS.md`](OPERATIONS.md)
- API e supervisão: [`docs/API.md`](API.md)
- Esferas metodológicas (ponte conceitual): [`docs/ESFERAS_METODOLOGICAS_RAFAELIA.md`](ESFERAS_METODOLOGICAS_RAFAELIA.md)

## Referência canônica de CI Android/Host

- Pipeline oficial Android: `.github/workflows/android-ci.yml` (acionado por wrappers/orquestração).
- Entrada Android: `.github/workflows/android.yml` (wrapper de eventos + delegação).
- Compatibilidade ABI Android: `.github/workflows/compile-matrix.yml` (trilha auxiliar).
- Pipeline oficial Host: `.github/workflows/host-ci.yml`.
- Orquestração e gate final: `.github/workflows/pipeline-orchestrator.yml` + `.github/workflows/quality-gates.yml`.
- Matriz canônica documentada em `docs/ci/workflow-matrix.md`.
