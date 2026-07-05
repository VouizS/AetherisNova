# Aetheris Nova 0.1.1 — Sprint 01 Glass Orbit Fix R1

## Problema

A build da Sprint 01 falhou em `:app:processDebugResources` por erro AAPT de cores ausentes em drawables herdados da fundação:

- `aetheris_nebula`
- `aetheris_panel_high`
- `aetheris_panel`

## Correção

Foram adicionadas de volta ao `colors.xml` as cores/aliases usados por drawables antigos e novos.

## Motivo

A Sprint 01 substituiu a paleta visual, mas alguns XMLs antigos ainda referenciavam nomes da paleta anterior.

## Resultado esperado

- Resources processam corretamente.
- APK compila.
- Interface Glass Orbit continua aplicada.
