# BETA_EXTERNAL_REFERENCE_MAP

Uso externo apenas como orientação arquitetural (sem dependência de build no Vectras).

## Rafaelia_Private (guia)
- BitStack witness para rastreabilidade de integridade.
- Smart Guard para gates determinísticos.
- ZIPRAF como padrão de empacotamento e validação.
- MVP/specs para contratos explícitos de execução.

## GAIA_phi (guia)
- Núcleo C determinístico e contratos de módulo.
- Manifestos como fonte de verdade de componentes.
- Testes de coerência por camada.
- Separação gaia_core_v2 / gaia_engines_v2 / guard.

## llamaRafaelia (guia)
- Smart Guard pré-geração.
- Witness em cadeia (BitStack).
- Módulo baremetal como isolamento de núcleo crítico.
- Exemplos de testes de integração lowlevel.

## Regra aplicada nesta beta
- Nenhum código/import desses repositórios foi adicionado como dependência obrigatória.
