# Twitch Chat Enhanced — Minecraft 26.3 Fabric

Código-fonte de uma reconstrução independente a partir da funcionalidade pública do mod enviado. **Ainda não compilado/testado em Minecraft 26.3.**

## Compilar
Instala JDK 25 e Gradle 9.7.1. Na pasta do projeto executa `gradle build` (ou cria o Gradle Wrapper e executa `gradlew.bat build`). O JAR ficará em `build/libs/` se a compilação tiver sucesso. Necessita de acesso aos repositórios Maven do Fabric.

## Comandos
- `/twitch join canal` — liga ao canal Twitch
- `/twitch leave` — desliga
- `/twitch on` e `/twitch off` — ativa/desativa
- `/twitch mentions on|off` — notificações de menção
- `/twitch nick nome` — nome a detetar nas menções
- `/twitch status` — estado

TLS na porta 6697, utilizador anónimo; não pede OAuth nem recolhe passwords. Configuração em `config/twitch-chat-enhanced.properties`.

Limitações: sem badges/cor de utilizador, apenas receção de mensagens. Confirmar compatibilidade da API e testar em cliente real antes de distribuir.
