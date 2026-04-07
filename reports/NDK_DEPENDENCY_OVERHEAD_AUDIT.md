<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# Auditoria NDK + dependências externas (foco em menor overhead/GC)

## 1) Stack nativa atual (fonte de verdade)
- **NDK**: `27.2.12479018`.
- **CMake**: `3.22.1`.
- **Compile API**: resolvida no Gradle raiz com validações de compatibilidade por task.
- **ABIs oficiais**: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` (com política de distribuição priorizando arm64).

Referências:
- `gradle.properties` e `build.gradle` (resolução de versões + validações).
- `app/build.gradle` (integração `externalNativeBuild`, flags e matriz de ABI).

---

## 2) Dependências externas Java/Kotlin em uso no app
Inventário atual do módulo `app` (escopo runtime):

### AndroidX / Google
- `androidx.appcompat:appcompat:1.7.1`
- `com.google.android.material:material:1.13.0`
- `androidx.annotation:annotation:1.9.1`
- `androidx.core:core-ktx:1.13.1`
- `androidx.drawerlayout:drawerlayout:1.2.0`
- `androidx.preference:preference-ktx:1.2.1`
- `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`
- `androidx.viewpager:viewpager:1.1.0`
- `androidx.window:window:1.5.1`
- `androidx.activity:activity-ktx:1.9.2`
- `androidx.constraintlayout:constraintlayout:2.2.1`
- `androidx.documentfile:documentfile:1.1.0`
- `androidx.work:work-runtime:2.9.1`

### Rede / serialização / utilidades
- `com.google.code.gson:gson:2.13.2`
- `com.squareup.okhttp3:okhttp:4.12.0`
- `org.apache.commons:commons-compress:1.28.0`
- `com.github.bumptech.glide:glide:4.16.0` (+ annotation processor)

### Teste
- `junit:junit:4.13.2`
- `org.robolectric:robolectric:4.14.1`
- `androidx.test:core:1.6.1`
- `org.mockito:mockito-core:5.12.0`
- `org.mockito:mockito-inline:5.2.0`
- `androidx.test.ext:junit:1.3.0`
- `androidx.test.espresso:espresso-core:3.7.0`

---

## 3) Pontos de redução de overhead, GC e abstrações (prioridade prática)

## P0 (alto impacto, baixo risco)
1. **Mover parsing crítico de JSON de `gson` para parser nativo autoral (JNI C/C++)**
   - Aplicar em paths de telemetria/controle de VM com payload previsível.
   - Estratégia: parser por estado + buffers fixos/reutilizáveis.
   - Resultado esperado: menos alocações Java e menor pressão no GC.

2. **Substituir operações de compressão quentes em `commons-compress` por caminho nativo dedicado**
   - Manter compatibilidade apenas onde necessário no app layer.
   - Para fluxos de extração recorrentes: usar buffer pool nativo e pipeline determinístico.

3. **Isolar I/O de rede crítica (okhttp) em bridge JNI para cópia mínima**
   - Evitar criação de objetos intermediários em loops frequentes.
   - Padronizar chunk size e reutilização de `byte[]` no lado Java.

## P1 (impacto médio)
4. **Reduzir custo de imagem/UI com Glide apenas onde realmente necessário**
   - Paths não críticos podem usar `BitmapFactory` com amostragem explícita.
   - Evitar transformações em cadeia quando não agregam valor visual.

5. **Revisar uso de `work-runtime` para tarefas que exigem latência previsível**
   - WorkManager é robusto, porém mais abstrato; para caminhos de execução imediata e controlada, usar serviço/processo nativo orquestrado localmente.

## P2 (governança de performance)
6. **Criar baseline de métricas determinísticas por variante**
   - `alloc count`, pausas de GC, p95/p99 de operações I/O e JNI.
   - Integrar em task Gradle de verificação para impedir regressões.

---

## 4) Ajustes nativos já alinhados no projeto (bons sinais)
- Flags JNI já orientadas a menor overhead: `-O2`, `-fno-exceptions`, `-fno-rtti`, `-fno-fast-math` e baseline arm64 explícito `-march=armv8-a`.
- Compatibilidade de toolchain reforçada por validação de NDK r23+ e matriz oficial de ABI.

---

## 5) Sequência recomendada para execução incremental
1. Medir baseline atual (debug/release/perfRelease) com foco em alocação e GC.
2. Migrar 1 caminho crítico JSON para parser JNI autoral e revalidar.
3. Migrar 1 caminho crítico de compressão para pipeline nativo.
4. Reavaliar dependências externas após cada migração (evitar remoção prematura).

> Observação: o objetivo é **reduzir abstrações nos hot paths**, sem quebrar compatibilidade de UI e ciclo de release Android.
