# Aetheris Nova 0.1.4 — Sprint 04 Real WebRTC Filter Layer

## Objetivo

Iniciar o motor real de efeitos/filtros em chamadas WebRTC, começando por sites como OmeTV.

## Regra

Não é overlay falso. Para a outra pessoa ver, o filtro precisa entrar no stream real entregue ao site.

## Implementado

- JavaScript injetado no WebView:
  - intercepta `navigator.mediaDevices.getUserMedia`
  - captura stream original
  - processa vídeo em canvas
  - devolve `canvas.captureStream()`
  - mantém áudio original
- Bridge Android/JavaScript:
  - `AetherisVideoBridge`
  - permite selecionar filtro no menu do navegador
- Menu real "Efeitos de vídeo":
  - Desligado
  - Cinza
  - Frio
  - Contraste
  - Orbit leve
- Permissões reais:
  - Câmera
  - Microfone
  - Áudio
- Status honesto:
  - recurso experimental
  - funciona apenas onde o site permitir e onde o hook carregar antes da captura

## Limitações conhecidas

- Alguns sites podem iniciar getUserMedia antes da injeção.
- Alguns sites podem bloquear manipulação do stream.
- Pode exigir recarregar a página após escolher filtro.
- Filtros pesados não foram usados para preservar performance.

## Versão

- versionCode: 6
- versionName: 0.1.4-webrtcfilter-exp
