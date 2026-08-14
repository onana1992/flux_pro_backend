# ADR — Assistant IA FluxPro (Sprint 0)

**Statut :** Accepté  
**Date :** 2026-08-03  
**Contexte :** Roadmap `docs/ROADMAP-ASSISTANT-IA.md` — Sprint 0 Fondations

## Décision

| Sujet | Choix |
|-------|--------|
| Fournisseur LLM | **OpenAI** (GPT) |
| Intégration Java | **Spring AI 2.0** (`spring-ai-starter-model-openai`) |
| Modèle par défaut | `gpt-4o-mini` (surchargeable via `FLUXPRO_ASSISTANT_MODEL`) |
| Streaming | **Non** en S0 (réponse sync `POST /api/assistant/chat`) — SSE prévu S2 |
| Hébergement clé | Variable d’env `OPENAI_API_KEY` / `application-secrets.properties` (hors git) |

## Conséquences

- Kill-switch : `fluxpro.assistant.enabled` (`FLUXPRO_ASSISTANT_ENABLED`)
- Auto-config modèle : `spring.ai.model.chat` (`FLUXPRO_ASSISTANT_CHAT=none|openai`) — `none` permet de démarrer sans clé
- Tools S0 : `get_current_user`, `get_file_by_reference` (lecture seule, JWT + RBAC)
- Tables : `docs/sql/2026-08-03_assistant_permission_and_tables.sql` (exécution manuelle)

## Alternatives écartées (S0)

- Anthropic / Azure OpenAI — possibles plus tard sans changer le contrat API
- LangChain4j — non retenu (alignement Spring Boot 4.1 + Spring AI 2.0)
- Streaming SSE — reporté après smoke sync

## Conformité

Clarifier avec la DSI si l’envoi de prompts / résultats d’outils vers OpenAI (cloud) est autorisé pour le pilote MINTP. En cas de refus, basculer vers Azure OpenAI région France ou modèle on-prem (ADR à mettre à jour).
