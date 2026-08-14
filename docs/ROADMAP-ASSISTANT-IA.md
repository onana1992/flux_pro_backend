# Roadmap — Assistant conversationnel métier FluxPro

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique  
**Cas pilote :** Ministère des Travaux Publics du Cameroun (MINTP)  
**Livrable :** Assistant IA **lecture seule**, conversationnel, multi-domaines  
**Date :** 29 juillet 2026  
**Statut :** Roadmap d’implémentation (à valider avant développement)  
**Références :** [SPEC fonctionnelle Assistant](./SPEC-ASSISTANT.md), [CDC](./CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md), [SPEC-DOS](./SPEC-DOS.md), [SPEC-CHN](./SPEC-CHN.md), [SPEC-ALR](./SPEC-ALR.md), [SPEC-DSH](./SPEC-DSH.md), [SPEC-USR-RBAC](./SPEC-USR-RBAC.md), [SPEC-ORG](./SPEC-ORG.md)

---

## 1. Vision et principes

### 1.1 Objectif produit

Mettre à disposition de chaque utilisateur authentifié un **assistant conversationnel** capable de répondre à un **large éventail de questions** sur FluxPro : localisation de dossiers, chaînes de passation, retards et alertes, pilotage (KPI), organisation, templates, règles d’alerte, et aide à la navigation — **sans jamais écrire** dans le système métier.

L’assistant remplace la navigation manuelle dans filtres / écrans quand la question est du type :

> « Où est… ? Qui… ? Combien… ? Pourquoi cette alerte… ? Comment fonctionne… ? Où cliquer pour… ? »

### 1.2 Principes non négociables

| # | Principe | Implication |
|---|----------|-------------|
| P1 | **Lecture seule** | Aucun tool de transmission, clôture, création, modification admin |
| P2 | **Même identité JWT** | Chaque tool s’exécute avec `currentUser()` ; scope org + RBAC inchangés |
| P3 | **Grounding obligatoire** | Toute affirmation chiffrée ou factuelle vient d’un tool ; sinon « je ne sais pas » |
| P4 | **Citations** | Réponses avec références dossier, liens UI (`/files/{id}`, `/dashboard`, …) |
| P5 | **Pas de fuite hors scope** | Un agent ne voit pas les dossiers hors de son périmètre organisationnel |
| P6 | **Traçabilité** | Journal des conversations (qui, quand, tools appelés, succès/échec) |
| P7 | **Français métier** | Ton administratif clair, vocabulaire FluxPro (dossier, maillon, possessionnaire, escalade) |

### 1.3 Ce que l’assistant n’est pas

- Pas un agent d’action (pas de « transmets le dossier à X » exécuté automatiquement)
- Pas un lecteur OCR / analyseur de pièces jointes (phase ultérieure éventuelle, hors ce roadmap)
- Pas un chatbot public (hors portail citoyen / non authentifié)
- Pas un remplacement des règles ALR déterministes

---

## 2. Catalogue de questions par domaine

Objectif : couvrir **8 domaines**. Chaque domaine a des **intents**, des **exemples**, et des **tools** associés (voir §4).

### Domaine A — Dossiers (DOS)

| Intent | Exemples de questions |
|--------|------------------------|
| A1 Localiser | « Où est MINTP-DAG-2026-0042 ? » |
| A2 Chercher | « Trouve les courriers urgents reçus cette semaine » |
| A3 Métadonnées | « Quel est le statut / la priorité / l’expéditeur du dossier X ? » |
| A4 Pièces | « Quelles pièces jointes sont attachées au dossier X ? » (métadonnées uniquement) |
| A5 Cycle de vie | « Quand a-t-il été soumis / clôturé / annulé ? » |

### Domaine B — Passation (CHN)

| Intent | Exemples |
|--------|----------|
| B1 Possessionnaire | « Qui traite ce dossier depuis combien de jours ? » |
| B2 Historique | « Montre le journal de passation du dossier X » |
| B3 Maillon courant | « À quelle étape est-on ? Quelle action est attendue ? » |
| B4 Délai maillon | « Quelle est l’échéance du maillon actuel ? Est-il en retard ? » |
| B5 Retours / suspensions | « Y a-t-il eu un retour ou une suspension sur ce dossier ? » |

### Domaine C — Alertes & notifications (ALR)

| Intent | Exemples |
|--------|----------|
| C1 Mes alertes | « Quelles alertes non lues ai-je ? » |
| C2 Alertes d’un dossier | « Quelles alertes ont été déclenchées sur MINTP-… ? » |
| C3 Comprendre une alerte | « Pourquoi ai-je reçu une escalade J+3 ? » |
| C4 Règles (admin) | « Quelles règles d’alerte sont actives sur le template T02 ? » |
| C5 Types d’alerte | « Quels types d’alerte existent (J-2, J+0, …) ? » |

### Domaine D — Pilotage & reporting (DSH)

| Intent | Exemples |
|--------|----------|
| D1 Ma charge | « Combien de dossiers ai-je en cours / en retard ? » |
| D2 Retards équipe | « Top 10 retards de ma direction » |
| D3 KPI synthèse | « Combien d’actifs, retards, clôturés ce mois ? » |
| D4 Charge par agent | « Qui a le plus de dossiers en cours dans mon service ? » |
| D5 Délais par type | « Quel est le délai moyen des courriers vs marchés ? » |
| D6 Classement | « Quelle direction respecte le mieux les délais ? » |
| D7 Analytics | « Tendance des retards sur 90 jours » |

### Domaine E — Organisation & utilisateurs (ORG / USR)

| Intent | Exemples |
|--------|----------|
| E1 Structure | « Quelles sont les sous-structures de la DAG ? » |
| E2 Qui est qui | « Qui est le chef de service du Bureau courrier ? » (si visible via USERS:READ) |
| E3 Mon profil | « Quel est mon rôle et mon organisation ? » |
| E4 Recherche agent | « Liste les agents actifs de la DIER » (selon permission) |

### Domaine F — Référentiels métier (templates, types, calendrier)

| Intent | Exemples |
|--------|----------|
| F1 Templates | « Quelles sont les étapes du template T01 ? » |
| F2 Types de dossier | « Quels types de dossiers sont actifs ? » |
| F3 Dossiers préconfigurés | « À quoi sert le dossier préconfiguré CODE-X ? » |
| F4 Calendrier | « Quels jours fériés sont paramétrés en 2026 ? » |
| F5 Délais template | « Combien de jours ouvrés au total pour T02 ? » |

### Domaine G — Aide produit & navigation (HELP)

| Intent | Exemples |
|--------|----------|
| G1 Comment faire | « Comment transmettre un dossier ? » |
| G2 Où cliquer | « Où voir le classement des directions ? » → lien `/rapports` |
| G3 Glossaire | « Que signifie possessionnaire / maillon / escalade ? » |
| G4 Permissions | « Pourquoi je ne vois pas le menu Admin ? » |

### Domaine H — Audit & conformité (ADMIN, restreint)

| Intent | Exemples |
|--------|----------|
| H1 Journal métier | « Qui a modifié le dossier X ? » (si AUDIT_LOG:READ) |
| H2 Connexions | « Y a-t-il des échecs de login récents ? » (LOGIN_AUDIT:READ) |
| H3 Paramétrage tenant | « Quelle est la config publique du tenant ? » (admin) |

### Synthèse couverture

| Domaine | Public cible principal | Priorité roadmap |
|---------|------------------------|------------------|
| A DOS | Tous | P0 |
| B CHN | Tous | P0 |
| C ALR | Tous (+ admin règles) | P0 / P1 |
| D DSH | Agent → Directeur → SG | P0 / P1 |
| E ORG/USR | Tous / managers | P1 |
| F Référentiels | Admin métier + agents curieux | P1 |
| G HELP | Tous | P1 |
| H Audit | Super-admin / DSI | P2 |

---

## 3. Architecture cible

```
┌─────────────────────────────────────────────────────────────┐
│  Frontend Next.js                                            │
│  AssistantChatPanel (drawer) + suggestions + citations       │
│  POST /api/assistant/chat  (Bearer JWT)                      │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│  Backend Spring Boot                                         │
│  AssistantController                                         │
│    → AssistantOrchestrator (LLM + tool-calling loop)         │
│    → AssistantToolRegistry (tools lecture seule)             │
│    → délègue aux *Service existants (File, Passage, …)       │
│    → AssistantConversationService (persist historique)       │
│    → AssistantGuardrails (refus écriture, injection, quota)  │
└────────────────────────────┬────────────────────────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
         FileService   DashboardService  … services métier
         PassageService NotificationService  (RBAC déjà en place)
```

### 3.1 Choix techniques recommandés

| Sujet | Recommandation | Alternative |
|-------|----------------|-------------|
| Intégration LLM Java | **Spring AI** (ChatClient + function/tool calling) | LangChain4j |
| Fournisseur LLM | OpenAI GPT-4.1 / GPT-4o ou Anthropic Claude (qualité FR + tools) | Azure OpenAI (si contrainte souveraineté cloud MINTP) |
| Streaming | SSE `text/event-stream` pour tokens + events `tool_start` / `tool_end` | Réponse bloquante v0 uniquement |
| Mémoire | Historique conversation en BDD (N derniers tours) + résumé optionnel | Session Redis si volume élevé |
| Knowledge HELP | Fichier Markdown versionné `docs/assistant/help-kb.md` injecté / RAG léger | RAG vectoriel (P2) |
| Secrets | `OPENAI_API_KEY` / `ANTHROPIC_API_KEY` hors git (comme secrets actuels) | — |

### 3.2 Contrat API (cible)

```http
POST /api/assistant/chat
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "conversationId": "uuid|null",
  "message": "Où est MINTP-DAG-2026-0042 ?",
  "locale": "fr"
}
```

Réponse (non-stream, v0) :

```json
{
  "conversationId": "…",
  "messageId": "…",
  "answer": "Le dossier MINTP-DAG-2026-0042 est chez M. Onana (Bureau courrier) depuis 4 jours ouvrés…",
  "citations": [
    { "type": "file", "id": "…", "label": "MINTP-DAG-2026-0042", "href": "/files/…" }
  ],
  "toolsUsed": ["get_file_by_reference", "get_current_passage"],
  "refused": false
}
```

Streaming (v1) : events `delta`, `citation`, `tool`, `done`, `error`.

Permissions :
- Nouvelle permission `ASSISTANT:USE` (tous les rôles métier actifs par défaut)
- Chaque tool vérifie **en plus** sa permission métier (`FILES:READ`, `DASHBOARD:READ`, …)

---

## 4. Catalogue des tools (lecture seule)

Chaque tool = méthode Java exposée au LLM, qui appelle un service existant.

### 4.1 Pack P0 — Opérationnel dossier + passation + mon activité

| Tool | Entrées | Service / API existante | Domaines |
|------|---------|-------------------------|----------|
| `get_current_user` | — | `GET /api/users/me` | E, G |
| `search_files` | search, status, priority, fileTypeCode, orgId, dates, page | `FileService.findAll` | A |
| `get_file_by_reference` | reference | `FileService.findByReference` | A |
| `get_file_by_id` | fileId | `FileService.findById` | A |
| `list_file_attachments` | fileId | `FileAttachmentService` (métadonnées) | A |
| `list_passages` | fileId | `PassageController` GET | B |
| `get_current_passage` | fileId | GET `…/passages/current` | B |
| `list_file_alerts` | fileId | GET `/api/files/{id}/alerts` | C |
| `list_my_notifications` | unreadOnly, limit | GET `/api/notifications` | C |
| `get_my_activity` | — | GET `/api/dashboard/my-activity` | D |
| `get_overdue_files` | orgId?, limit | GET `/api/dashboard/overdue-files` | D |
| `get_dashboard_summary` | orgId?, fileTypeCode? | GET `/api/dashboard/summary` | D |

### 4.2 Pack P1 — Pilotage étendu + référentiels + org

| Tool | Service / API | Domaines |
|------|---------------|----------|
| `get_workload` | `/api/dashboard/workload` | D |
| `get_delay_by_type` | `/api/dashboard/delay-by-type` | D |
| `get_compliance_ranking` | `/api/dashboard/compliance-ranking` | D |
| `get_dashboard_analytics` | `/api/dashboard/analytics` | D |
| `get_organization_tree` | `/api/organizations/tree` | E |
| `get_organization` | `/api/organizations/{id}` | E |
| `search_users` | `GET /api/users` (filtres) | E |
| `list_file_types` | `/api/file-types` | F |
| `list_chain_templates` | `/api/admin/chain-templates` | F |
| `get_chain_template` | by id ou code | F |
| `list_alert_types` | `/api/alert-types` | C, F |
| `list_alert_rules` | `/api/admin/chain-templates/{id}/alert-rules` | C, F |
| `list_business_calendar` | `/api/admin/business-calendar` | F |
| `list_preconfigured_dossiers` | admin preconfigured | F |
| `lookup_help` | KB locale `help-kb.md` | G |

### 4.3 Pack P2 — Audit / admin avancé

| Tool | Permission requise | Domaines |
|------|-------------------|----------|
| `search_audit_log` | `AUDIT_LOG:READ` | H |
| `search_login_audit` | `LOGIN_AUDIT:READ` | H |
| `get_tenant_settings` | admin tenant | H |
| `explain_permissions` | dérivé de `/me` + matrice UI | G, H |

### 4.4 Tools explicitement interdits

Toute mutation : `transmit`, `return`, `suspend`, `resume`, `reassign`, `create/update/delete` files, users, orgs, templates, alert rules, clock adjust, etc.

Si l’utilisateur demande une action : l’assistant **explique la marche à suivre** (domaine G) et fournit le lien UI, sans exécuter.

---

## 5. Timeline globale

Durée cible : **10 semaines** (5 sprints de 2 semaines), après validation architecture.

```
Sem.  1────2────3────4────5────6────7────8────9────10
      │ S0 fondations │ S1 P0 chat │ S2 P0+ │ S3 P1  │ S4 P2+ │
      │ LLM+API+UI    │ DOS/CHN/ALR│ DSH    │ ORG/F/G│ Audit  │
      │               │            │ élargi │ HELP   │ eval   │
```

| Sprint | Semaines | Objectif | Domaines livrés |
|--------|----------|---------|-----------------|
| **S0** | 1–2 | Socle technique, UI chat, 2–3 tools smoke | A (partiel), G (stub) |
| **S1** | 3–4 | Pack P0 dossiers + passation + notifs | A, B, C (notifs), D (my-activity) |
| **S2** | 5–6 | Pack P0/P1 dashboard complet + citations riches | D élargi, C (alertes dossier) |
| **S3** | 7–8 | Org, templates, calendrier, KB aide | E, F, G |
| **S4** | 9–10 | Audit, garde-fous, eval UAT, prod staging | H + durcissement |

---

## 6. Détail par sprint

### Sprint 0 — Fondations (semaines 1–2)

**Objectifs**
- Décider fournisseur LLM + hébergement clé API
- Brancher Spring AI (ou LangChain4j) dans `flux-pro-backend`
- Créer module `assistant` (controller, orchestrator, DTO, config)
- Permission `ASSISTANT:USE` + script SQL seed rôles
- UI : panneau chat (drawer) dans le layout authentifié
- Smoke : `get_current_user` + `get_file_by_reference` + réponse FR

**Livrables**
- [x] ADR : choix LLM / Spring AI / streaming → `docs/ADR-ASSISTANT-LLM-SPRING-AI.md` (OpenAI + Spring AI 2.0, sync S0)
- [x] `docs/sql/2026-08-03_assistant_permission_and_tables.sql` (permission + tables conversation)
- [x] Endpoint `POST /api/assistant/chat` (sync) + `GET /api/assistant/status`
- [x] Tables `assistant_conversation`, `assistant_message`, `assistant_tool_call` (schéma §7)
- [x] Composant front `AssistantChatPanel` + i18n FR/EN
- [x] Feature flag `fluxpro.assistant.enabled=true|false`
- [x] Variables d’env documentées (Render / local)

**Critères d’acceptation**
- Un utilisateur connecté pose « Qui suis-je ? » → réponse avec nom, rôle, org
- « Où est {réf connue} ? » → réponse grounded ou erreur métier claire
- Feature flag off → 404/403 API + panneau masqué

**Risques**
- Latence LLM → timeout HTTP ; prévoir timeout configurable + message UX
- Souveraineté données MINTP → privilégier Azure FR / option on-prem si exigée

---

### Sprint 1 — Pack opérationnel P0 (semaines 3–4)

**Objectifs**
- Implémenter tools Pack P0 §4.1
- System prompt métier FluxPro + glossaire court
- Boucle tool-calling multi-étapes (max 5 appels / tour)
- Suggestions de démarrage selon rôle (agent vs directeur)

**Questions de recette (échantillon)**
1. Où est le dossier X ?
2. Qui le traite depuis combien de jours ?
3. Liste mes dossiers en retard
4. Quelles sont mes notifications non lues ?
5. Résume mon activité du jour / ma charge
6. Cherche les dossiers urgents « autorisation »
7. Quelles pièces sont liées au dossier X ? (noms/types seulement)

**Livrables**
- [x] Tool registry P0 + tests unitaires (tools mock + rate limit)
- [x] Prompt versionné `docs/assistant/system-prompt.md`
- [x] Limite résultats (ex. max 10 dossiers listés dans la réponse)
- [x] Citations cliquables front
- [x] Rate limit : N messages / utilisateur / heure

**Notes S1 (implémenté)**
- Tools : `get_current_user`, `search_files`, `get_file_by_reference`, `get_file_by_id`,
  `list_file_attachments`, `list_passages`, `get_current_passage`, `list_file_alerts`,
  `list_my_notifications`, `get_my_activity`, `get_overdue_files`, `get_dashboard_summary`
- Limite tool-calls / tour : `fluxpro.assistant.max-tool-calls` (défaut 5)
- Rate limit : `fluxpro.assistant.rate-limit-per-hour` (défaut 30)
- Banque eval : `docs/assistant/eval-bank-p0.md`
- Front : suggestions agent vs manager + citations « Sources »

**Critères d’acceptation**
- ≥ 80 % des 20 questions de banque P0 correctement grounded (eval manuelle)
- Aucune réponse inventant un possessionnaire absent des tools
- Refus poli si demande de transmission

---

### Sprint 2 — Pilotage large (semaines 5–6)

**Objectifs**
- Completer tools dashboard : workload, delay-by-type, ranking, analytics, overdue
- Alertes par dossier
- Streaming SSE (optionnel mais recommandé)
- Comparaisons « ce mois vs période » si données analytics le permettent

**Questions de recette**
1. Combien de dossiers actifs / en retard dans mon périmètre ?
2. Top retards de ma direction
3. Qui a la charge la plus élevée dans mon service ?
4. Délai moyen par type de dossier
5. Classement des directions par respect des délais
6. Tendance sur 90 jours
7. Quelles alertes sur le dossier X ?

**Livrables**
- [x] Tools DSH P1
- [x] Format de réponse « tableau textuel » pour classements
- [x] Export conversation → lien vers `/rapports` ou `/dashboard/overdue`
- [ ] Métriques : latence p50/p95, taux d’erreur tools, tokens

**Notes S2 (implémenté)**
- Tools : `get_workload`, `get_delay_by_type`, `get_compliance_ranking`, `get_dashboard_analytics`
- Réponses avec `tableMarkdown` + citations `/dashboard/*`, `/rapports`
- Streaming SSE et métriques tokens reportés au Sprint 4 / ops

**Critères d’acceptation**
- Un directeur obtient UC-04 CDC en langage naturel (actifs, retards, délai moyen, top retards)
- Scope org respecté (test avec 2 users de directions différentes)

---

### Sprint 3 — Référentiels, org, aide (semaines 7–8)

**Objectifs**
- Tools E + F + `lookup_help`
- Base de connaissances aide : comment transmettre, clôturer, créer un dossier, lire un dashboard
- Deep-links contextuels (`/admin/chain-templates/{id}`, `/files`, `/rapports`, …)
- Détection intent « action demandée » → bascule mode guide UI

**Questions de recette**
1. Quelles étapes contient le template T01 ?
2. Quels types de dossiers sont disponibles ?
3. Quels jours fériés en août 2026 ?
4. Comment transmettre un dossier que je possède ?
5. Où voir le rapport de conformité ?
6. Quelles sous-structures sous la DAG ?
7. Pourquoi je n’ai pas accès aux templates ? (explication permission)

**Livrables**
- [x] `docs/assistant/help-kb.md` (FR, aligné UI réelle)
- [x] Tool `lookup_help` (recherche sections / embeddings légers)
- [x] Matrice intent → écran cible
- [x] Tests : admin métier vs agent (tools admin refusés proprement)

**Notes S3 (implémenté)**
- Tools E/F/G : org tree/detail/code/children/my-org, users search/detail/org-users/heads,
  file types, chain templates, alert types/rules, business calendar, preconfigured, `lookup_help`
- Matrice : `docs/assistant/intent-matrix.md`
- Refus action → guide `lookup_help` ; mode how-to dans l'orchestrateur
- Classe `AssistantCatalogTools` + `AssistantHelpKnowledgeBase`

**Critères d’acceptation**
- Questions « comment faire » répondent avec étapes + lien, sans mutation
- Questions template/règles inaccessibles → message permission explicite

---

### Sprint 4 — Audit, durcissement, mise en production pilote (semaines 9–10)

**Objectifs**
- Tools H (audit) pour rôles autorisés
- Garde-fous anti-injection / jailbreak
- Banque d’évaluation automatisée (golden set ≥ 50 questions)
- Documentation admin + runbook incident LLM
- Activation staging puis feature flag prod pilote (DAG)

**Livrables**
- [ ] Tools audit P2
- [ ] Suite `AssistantEvalIT` ou script d’eval offline
- [ ] Dashboard interne usage assistant (messages/jour, intents, erreurs)
- [ ] Guide utilisateur 2 pages + FAQ
- [ ] Checklist sécurité (clés, PII logs, rétention conversations)

**Critères d’acceptation**
- UAT MINTP : 10 utilisateurs, ≥ 70 % satisfaction « utile au quotidien »
- 0 incident de fuite hors scope sur tests RBAC
- Kill-switch `fluxpro.assistant.enabled=false` opérationnel

---

## 7. Modèle de données (SQL manuel)

> Rappel projet : `spring.jpa.hibernate.ddl-auto=none` — **script à exécuter manuellement** avant déploiement du code.

Schéma cible (à figer en S0) :

```sql
-- Objectif : conversations assistant IA (lecture seule)
-- Tables : assistant_conversation, assistant_message, assistant_tool_call
-- + seed permission ASSISTANT:USE

CREATE TABLE assistant_conversation (
  id CHAR(36) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  title VARCHAR(200) NULL,
  CONSTRAINT fk_asst_conv_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE assistant_message (
  id CHAR(36) PRIMARY KEY,
  conversation_id CHAR(36) NOT NULL,
  role VARCHAR(20) NOT NULL, -- USER | ASSISTANT | SYSTEM
  content MEDIUMTEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  refused BIT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_asst_msg_conv FOREIGN KEY (conversation_id) REFERENCES assistant_conversation(id)
);

CREATE TABLE assistant_tool_call (
  id CHAR(36) PRIMARY KEY,
  message_id CHAR(36) NOT NULL,
  tool_name VARCHAR(80) NOT NULL,
  arguments_json JSON NULL,
  result_summary VARCHAR(500) NULL,
  success BIT(1) NOT NULL,
  duration_ms INT NULL,
  created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_asst_tool_msg FOREIGN KEY (message_id) REFERENCES assistant_message(id)
);
```

Rétention proposée : 90 jours (job de purge), configurable.

---

## 8. Frontend — UX

| Élément | Spec |
|---------|------|
| Entrée | Bouton flottant « Assistant » (layout app authentifiée) |
| Conteneur | Drawer droit ~400 px desktop ; plein écran mobile |
| Suggestions | 4 chips dynamiques selon rôle + domaine |
| Streaming | Indicateur « Recherche dans FluxPro… » pendant tools |
| Citations | Chips cliquables vers fiches / dashboards |
| Historique | Liste des conversations de l’utilisateur |
| Erreurs | Timeout LLM, quota, feature off — messages distincts |
| Accessibilité | Focus trap, clavier, labels i18n |

Hors portail citoyen (`/portal/**`).

---

## 9. Sécurité & conformité

| Contrôle | Mesure |
|----------|--------|
| AuthN | JWT obligatoire |
| AuthZ | `ASSISTANT:USE` + permission par tool |
| Scope | Réutilise `OrganizationScopeService` via services métier |
| Prompt injection | Instructions système prioritaires ; tools = seule source de vérité |
| Sortie | Pas de secrets, pas de dump SQL brut |
| Logs | tools + ids ; pas de contenu pièce jointe |
| Quota | rate limit user + budget tokens / jour |
| Kill-switch | propriété + variable d’env Render |
| Données MINTP | Clarifier avec DSI si envoi prompts vers API cloud est autorisé (ADR) |

---

## 10. Plan d’évaluation (qualité des réponses)

### 10.1 Banque de questions (golden set)

Maintenir `docs/assistant/eval-questions.md` :

| ID | Domaine | Question | Réponse attendue (critères) | Sprint |
|----|---------|----------|------------------------------|--------|
| Q001 | A | Où est {ref} ? | possessionnaire + org + jours | S1 |
| Q002 | B | Historique de {ref} | ≥ N maillons chronologiques | S1 |
| … | … | … | … | … |

Cible : **50 questions** à S4 (couvrant A–H).

### 10.2 Métriques

| Métrique | Cible S2 | Cible S4 |
|----------|----------|----------|
| Exactitude grounded (humain) | ≥ 80 % P0 | ≥ 85 % multi-domaines |
| Hallucination factuelle | ≤ 5 % | ≤ 3 % |
| Refus correct d’écriture | 100 % | 100 % |
| Latence p95 (hors cold start) | ≤ 12 s | ≤ 8 s (streaming perçu) |
| Satisfaction UAT | — | ≥ 70 % |

---

## 11. Organisation projet

| Rôle | Responsabilités |
|------|-----------------|
| Product / Chef projet | Priorisation intents, UAT MINTP |
| Backend | Orchestrateur, tools, SQL, sécurité |
| Frontend | Chat UI, citations, i18n |
| Admin métier | Validation glossaire + help-kb |
| DSI | Validation hébergement LLM / conformité |

Dépendances internes FluxPro : modules DOS, CHN, ALR, DSH, ORG, USR déjà livrés — **pas de nouveau module métier requis** hors assistant.

---

## 12. Estimation effort

| Sprint | Backend | Frontend | Docs / eval | Total indicative |
|--------|---------|----------|-------------|------------------|
| S0 | 5–7 j | 3–4 j | 1 j | ~10 j·h |
| S1 | 6–8 j | 2–3 j | 2 j | ~12 j·h |
| S2 | 5–7 j | 2–3 j | 2 j | ~11 j·h |
| S3 | 5–6 j | 2 j | 3 j (KB) | ~11 j·h |
| S4 | 4–5 j | 1–2 j | 3 j | ~9 j·h |
| **Total** | | | | **~50–55 j·h** |

(1 dev full-stack peut tenir le calendrier 10 semaines ; 2 profils BE+FE accélèrent S0–S1.)

---

## 13. Hors périmètre de ce roadmap

- OCR / résumé de contenu PDF
- Actions métier automatiques (transmit, close, …) même avec confirmation
- Assistant sur le portail public
- Fine-tuning d’un modèle maison
- Remplacement des digests email ALR
- Multi-tenant cross-org « vue SG omnisciente » au-delà des droits déjà accordés

Ces sujets pourront faire l’objet d’un **roadmap v2** après stabilisation de l’assistant lecture seule.

---

## 14. Décisions à trancher avant S0

| # | Décision | Options | Impact |
|---|----------|---------|--------|
| D1 | Fournisseur LLM | OpenAI / Anthropic / Azure OpenAI FR | Coût, latence, conformité |
| D2 | Framework Java | Spring AI vs LangChain4j | Vitesse d’intégration |
| D3 | Streaming | Sync only S0–S1 vs SSE dès S0 | UX |
| D4 | Rétention conversations | 30 / 90 / 365 jours | RGPD / audit MINTP |
| D5 | Feature flag prod | Off par défaut jusqu’à UAT | Risque rollout |

---

## 15. Definition of Done (produit assistant v1)

- [ ] Domaines A–G couverts par tools + eval ≥ 85 % sur golden set
- [ ] Domaine H disponible pour rôles audit uniquement
- [ ] 100 % lecture seule ; refus des mutations testé
- [ ] RBAC + scope org validés (tests automatisés)
- [ ] UI chat en FR, citations, suggestions, kill-switch
- [ ] Script SQL versionné exécuté sur staging/prod
- [ ] Runbook + help-kb + ADR LLM publiés dans `docs/`
- [ ] UAT pilote DAG validée

---

## 16. Prochaines actions immédiates

1. Valider ce roadmap (périmètre multi-domaines + 10 semaines).
2. Trancher D1–D5 (§14).
3. Créer le ticket Epic « Assistant IA FluxPro » découpé en S0–S4.
4. Rédiger le script SQL + ADR dès démarrage S0.
5. Collecter 20 questions réelles auprès d’un agent DAG et d’un directeur pour enrichir le golden set.

---

*Document vivant — à mettre à jour à chaque fin de sprint (outils ajoutés, taux d’eval, décisions ADR).*
