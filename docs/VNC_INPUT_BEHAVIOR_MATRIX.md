# VNC Input Behavior Matrix (androidVNC)

Este documento define o comportamento canônico de consumo de eventos para `VncCanvas`.

## Escopo
- Listener de toque: `VNCOnTouchListener.onTouch(...)`
- Listener de movimento genérico: `VNCGenericMotionListener_API12.onGenericMotion(...)`
- Hover mouse: `processHoverMouse(...)`

Arquivo de implementação: `app/src/main/java/android/androidVNC/VncCanvas.java`.

## Matriz por `source` / `action`

| Source (MotionEvent) | Action | MouseMode | Comportamento | Retorno |
|---|---|---|---|---|
| `SOURCE_MOUSE` | `ACTION_SCROLL` | `External` | Usa `AXIS_VSCROLL`; envia `processPointerEvent(..., ACTION_SCROLL, scrollUp)` quando há delta vertical. | `true` quando enviado; `false` sem delta ou fallback. |
| `SOURCE_MOUSE` | `ACTION_HOVER_MOVE` | `External` | Encaminha histórico (quando habilitado) + amostra atual para `processHoverMouse(...)` e depois `processPointerEvent(...)`. | `true` se ao menos um evento foi consumido; `false` no fallback. |
| `SOURCE_MOUSE` | qualquer outra | `External` | Não há mapeamento de ponteiro neste listener. | `false`. |
| `SOURCE_MOUSE` | qualquer | `Trackpad` | Pass-through explícito para que o pipeline de trackpad/touch do app decida. | `false`. |
| `SOURCE_JOYSTICK` / `SOURCE_GAMEPAD` / `SOURCE_DPAD` | qualquer | qualquer | Não tratado por `VncCanvas` (evita consumo indevido). | `false`. |
| `SOURCE_*` não mouse | qualquer | qualquer | Não suportado neste listener. | `false`. |
| toque (`onTouch`) | `ACTION_DOWN/MOVE/UP/...` | `External` | Encaminha para `processPointerEvent(event, isDown)` com semântica de botão. | retorno de `processPointerEvent`. |
| toque (`onTouch`) | qualquer | `Trackpad` | Pass-through explícito para não conflitar com pipeline de trackpad da activity/input handler. | `false`. |

## Política de logs

- Logs estruturados **somente em fallback** (`input_fallback ...`) para evitar ruído em produção.
- Campos: `handler`, `reason`, `source`, `action`, `mode`, `x`, `y`, `consumed`.
- Nível: `debug` (`Log.d`), protegido por `Log.isLoggable(TAG, Log.DEBUG)`.

## Objetivo de estabilidade

- Padronizar retorno booleano (`consumed` vs `not consumed`) para evitar perdas intermitentes de input.
- Eliminar caminhos mortos/TODO nos fluxos `ACTION_SCROLL` e `ACTION_HOVER_MOVE`.
