# Vectras VM — Performance Operations (Runbook)

## Resumo
Runbook de operação para medir e ajustar desempenho com rastreabilidade e rollback seguro.

## Escopo
- Coberto:
  - Preparação de ambiente.
  - Coleta de benchmark.
  - Ajuste incremental e validação.
- Não coberto:
  - Números fixos por dispositivo sem artefato anexado.

## Fluxo operacional
1. Preparar ambiente (energia, temperatura, carga em background).
2. Executar benchmark e registrar diagnósticos.
3. Ajustar um parâmetro por vez.
4. Comparar antes/depois com mesmo protocolo.
5. Registrar decisão técnica e rollback.

## Checklist de qualidade
- Mesma variante/build/dispositivo entre cenários.
- Resultado válido com relatório bruto salvo.
- Limitações de ambiente descritas.

## Metadados
- Versão: 1.1
- Última atualização: 2026-02
- Responsável: manutenção documental
- Licença: GPL-2.0
