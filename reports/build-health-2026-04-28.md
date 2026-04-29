# Vectras Build Organism Health Report — 2026-04-28

## Causas-raiz encontradas

1. **Drift de contrato ABI no pipeline `android-native-ci`**
   - Sintoma: gate obrigatório `validate_lowlevel_abi.sh` quebrava antes de compilar.
   - Causa estrutural: workflow passava `-PSUPPORTED_ABIS="${{ matrix.abi }}"` literal, fora do contrato canônico que exige resolução por `tools/ci/resolve_abi_profile.py`.
   - Efeito sistêmico: o organismo CI entrava em estado inválido antes do build, mascarando outras falhas reais de compilação.

2. **Fricção/zumbi operacional na raiz do repositório**
   - Evidência: arquivo órfão `1;.jkjjh` fora de qualquer trilha de build/release.
   - Risco: ruído de versionamento, ruído em auditoria de integridade, baixa previsibilidade de bootstrap para novos runners.

3. **Bloqueio de execução local neste ambiente atual**
   - Evidência: ausência de `java` e de Android SDK local padrão.
   - Impacto: compilação local arm32/arm64 assinada e não-assinada não pode ser concluída **neste runner** sem bootstrap adicional.

## Correções aplicadas

- Workflow `.github/workflows/android-native-ci.yml` foi realinhado para resolver ABI por contrato canônico e exportar variáveis de ambiente derivadas de `resolve_abi_profile.py`.
- Invocações Gradle no workflow deixaram de usar valores literais fora do contrato.
- Staging/upload de artefatos foi ajustado por `profile` em vez de filtro por `matrix.abi` literal.
- Gate ABI foi revalidado após correção com sucesso.

## Estado de urgência (organismo vivo)

- **URGENTE (P0):** manter gate ABI verde (corrigido neste patch).
- **ALTO (P1):** limpar artefato zumbi da raiz (`1;.jkjjh`) em PR dedicado de higiene de árvore.
- **ALTO (P1):** provisionar Java + Android SDK no ambiente local/runner para fechar ciclo de compilação arm32+arm64 signed/unsigned.

## Comandos executados e resultados

```bash
./tools/ci/validate_pipeline_directories.sh --profile android
# PASS: Pipeline directory contract is valid for profile=android

./tools/ci/validate_lowlevel_abi.sh
# FAIL (antes): ABI_CONTRACT_DRIFT em android-native-ci.yml por SUPPORTED_ABIS literal

./tools/ci/validate_lowlevel_abi.sh
# PASS (depois): ABI contract drift check: OK + LOWLEVEL_ABI_CONTRACT_OK

test -d /workspace/android-sdk && echo yes || echo no
# no

java -version
# /bin/bash: java: command not found

rg --files | rg '(^|/)1;|\.bak$|~$' || true
# 1;.jkjjh
```

## Bloqueios restantes

- Sem JDK/SDK no runner atual, **não foi possível** gerar APK local arm32/arm64 com e sem assinatura nesta execução.
- O pipeline CI permanece preparado para executar isso quando o ambiente fornecer toolchain Android.
