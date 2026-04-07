<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# BUILD FAILURE AUDIT (local run)

## Ambiente validado
- `sdk.dir=/workspace/android-sdk` em `local.properties` (não versionado).
- SDK instalado: `platforms;android-35`, `build-tools;35.0.0`, `ndk;27.2.12479018`, `cmake;3.22.1`.
- Java runtime local: OpenJDK 17 instalado para `sdkmanager`; Gradle executou com JVM 21 (aceita pelo projeto).

## Ajuste técnico aplicado
- Corrigido bloqueio de compilação em `TerminalEmulator`:
  - removida duplicidade da constante `DECSET_BIT_REVERSE_WRAPAROUND`.
  - removido `case 45` duplicado no `mapDecSetBitToInternalBit`.

## Resultado dos comandos principais
1. `:terminal-emulator:compileDebugJavaWithJavac`
   - **Antes**: falhava por símbolo e case duplicados em `TerminalEmulator`.
   - **Depois**: compila.

2. `:terminal-emulator:testDebugUnitTest`
   - Executados 157 testes; **5 falhas**:
     - `CursorAndScreenTest.testInsertMode`
     - `CursorAndScreenTest.testBackspaceAcrossWrappedLines`
     - `DecSetTest.testReverseWrapAroundMode`
     - `DeviceControlStringTest.testHighCardinalityCapabilityItems`
     - `TerminalRowTest.testIssue135`

3. `:app:assembleDebug` / `:app:assembleRelease` (`--continue`)
   - Falhas Java (símbolos ausentes/duplicações) em módulos de app.
   - Falhas JNI/C no `vectra_core_accel.c` por funções libc sem include adequado.
   - Falha de gate de release Firebase (`google-services.json` ausente).
   - Falha de gate de assinatura release (signingConfig release ausente).

## Firebase (configuração prática “low level” no fluxo atual)
- O projeto já possui gate explícito em `app/build.gradle`:
  - release exige `app/google-services.json` válido com `project_info.project_id` real.
  - bloqueia `project_id` placeholder.
- Para debug local:
  - não é obrigatório `google-services.json` (conforme `app/FIREBASE.md`).
- Para release CI:
  - injetar `google-services.json` real via segredo base64 (não versionar no Git).

## Principais classes de falha observadas
1. **Compilação Java (app core)**: APIs referenciadas que não existem mais (`SlotToken`, `Snapshot`, campos onboarding, binding ids, etc.).
2. **Duplicação de método**: `PermissionUtils` com assinaturas repetidas.
3. **Compilação JNI/C**: uso de `popen/fgets/pclose/malloc/memcpy/free` sem includes necessários (`stdio.h`, `stdlib.h`, `string.h`).
4. **Gates de release corretos porém bloqueantes no local**:
   - Firebase release sem arquivo real.
   - Signing release não configurado.
5. **Regressões funcionais no terminal-emulator**: 5 testes unitários falhando em comportamento de terminal (wrap/reverse-wrap/insert mode/DCS/row).

## Dependências e estrutura: confirmação objetiva
- Cadeia Android validada no host (SDK/NDK/CMake) está funcional.
- Estrutura Gradle e módulos (`terminal-emulator`, `terminal-view`, `app`) carregam e avançam até compilação.
- Os bloqueios atuais são majoritariamente de **lógica/código-fonte** e **gates de release**, não de ausência de toolchain.
