<!-- DOC_ORG_SCAN: 2026-04-07 | source-scan: pending-manual-by-domain -->

# addthis — índice operacional RAFAELIA ψ→χ→ρ→Δ→Σ→Ω

Diretório de auditoria aplicada ao estado **real** do repositório `Vectras-VM-Android`.

## Ordem recomendada (sem abstração)
1. Ler `VECTRAS_BUGS.md` (15 bugs críticos/fatais).
2. Ler `VECTRAS_PROBLEMS.md` (42 problemas estruturais/técnicos).
3. Ler `VECTRAS_SOLUTIONS.md` (patches propostos por fase).
4. Ler `VECTRAS_REALITY_DIFF.md` (verificação profunda do que está **aberto** vs **já resolvido** no código atual).
5. Executar `apply_fixes_phase1.sh` somente após revisar `git diff`.

## Arquivos principais
- `VECTRAS_BUGS.md` → inventário de bugs com severidade.
- `VECTRAS_PROBLEMS.md` → problemas de arquitetura/build/coerência.
- `VECTRAS_SOLUTIONS.md` → mudanças recomendadas por arquivo.
- `VECTRAS_REALITY_DIFF.md` → confronto objetivo doc vs árvore atual.
- `apply_fixes_phase1.sh` → automação da fase 1 (CI unblock).
- `rmr_types.h` → proposta de tipagem canônica centralizada.

## Regras de execução
- Executar na raiz do repo.
- Revisar sempre: `git status --short` e `git diff --stat` antes de commit.
- Preservar literais/selos RAFAELIA (⊕ ⊗ ∮ ∫ √ π φ Δ Ω Σ ψ χ ρ ∧).
