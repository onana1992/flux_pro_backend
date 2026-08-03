# Tutoriel pédagogique — Créer un assistant IA conversationnel

**Public :** développeurs / product owners qui démarrent  
**Niveau :** débutant → intermédiaire  
**Durée de lecture :** ~25–30 min  
**Objectif :** comprendre *comment* construire un assistant utile, fiable et sécurisé — **sans dépendre d’un produit métier précis**.

---

## Table des matières

1. [Qu’est-ce qu’un assistant IA ?](#1-quest-ce-quun-assistant-ia-)
2. [Les 3 architectures possibles](#2-les-3-architectures-possibles)
3. [Les briques indispensables](#3-les-briques-indispensables)
4. [Le cœur : LLM + outils (tool calling)](#4-le-cœur--llm--outils-tool-calling)
5. [Tutoriel pas à pas (mini-projet)](#5-tutoriel-pas-à-pas-mini-projet)
6. [Prompt système : l’art de cadrer le modèle](#6-prompt-système--lart-de-cadrer-le-modèle)
7. [Sécurité et garde-fous](#7-sécurité-et-garde-fous)
8. [Coûts et performance](#8-coûts-et-performance)
9. [Qualité : comment savoir si ça marche](#9-qualité--comment-savoir-si-ça-marche)
10. [Checklist de mise en production](#10-checklist-de-mise-en-production)
11. [Glossaire](#11-glossaire)
12. [Pour aller plus loin](#12-pour-aller-plus-loin)

---

## 1. Qu’est-ce qu’un assistant IA ?

### 1.1 Chatbot classique vs assistant

| | Chatbot classique | Assistant IA |
|---|-------------------|--------------|
| Logique | Arbres de décision / intents figés | Modèle de langage (LLM) |
| Compréhension | Mots-clés, phrases exactes | Langage naturel, reformulations |
| Actions | Souvent limitées | Peut appeler des **outils** (API, BDD…) |
| Maintenance | Beaucoup de règles à écrire | Moins de règles, plus de *cadrage* |

Un **assistant IA** est un système qui :

1. comprend une question en langage naturel ;
2. décide s’il doit **chercher de l’information** ou **agir** ;
3. utilise des **outils** (fonctions) pour obtenir des faits à jour ;
4. formule une réponse claire, fondée sur ces faits.

### 1.2 Analogie simple

Imaginez un stagiaire intelligent :

- il parle bien (le **LLM**) ;
- mais il ne connaît pas votre base de données (il **hallucine** s’il invente) ;
- vous lui donnez un **badge** (auth), un **manuel** (prompt système) et un **téléphone interne** (outils) pour appeler la compta, le stock, etc. ;
- il doit **vérifier** avant de répondre, et **ne pas signer de chèques** sans validation (garde-fous).

### 1.3 Les deux familles d’assistants

| Type | Exemple | Risque |
|------|---------|--------|
| **Lecture seule** (Q&A / copilote) | « Où en est la commande 123 ? » | Faible : il informe |
| **Agent d’action** | « Annule la commande 123 » | Élevé : il modifie le monde |

**Conseil pédagogique :** commencez **toujours** par la lecture seule. Ajoutez les actions plus tard, avec confirmation humaine.

---

## 2. Les 3 architectures possibles

### Architecture A — « LLM nu » (à éviter en prod métier)

```
Utilisateur → LLM → Réponse
```

Le modèle répond uniquement avec sa mémoire d’entraînement.  
**Problème :** dates fausses, données inventées, aucune info privée de votre entreprise.

### Architecture B — RAG (Retrieval-Augmented Generation)

```
Utilisateur → recherche dans documents → morceaux pertinents → LLM → Réponse
```

Idéal pour : FAQ, manuels, politiques internes, documentation.  
Le LLM **ne cherche pas dans une API live** ; il lit des **extraits** de documents indexés.

### Architecture C — Tool calling (agents / assistants métier)

```
Utilisateur → LLM → (décide d’appeler un outil) → API/BDD → résultat → LLM → Réponse
```

Idéal pour : « état d’une commande », « solde », « prochain RDV », « tickets ouverts ».  
C’est l’architecture **la plus adaptée** aux applications métier connectées.

### Combinaison fréquente (recommandée)

```
          ┌── RAG (docs / aide produit)
Utilisateur ── LLM ──┤
          └── Tools (APIs métier live)
```

---

## 3. Les briques indispensables

```
┌──────────────────────────────────────────────────────┐
│  Interface (chat web, Slack, WhatsApp…)              │
└────────────────────────┬─────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────┐
│  Backend orchestrateur                               │
│  - Auth utilisateur                                  │
│  - Historique de conversation                        │
│  - Boucle LLM ↔ tools                                │
│  - Garde-fous / quotas                               │
└───────────┬────────────────────────────┬─────────────┘
            │                            │
     ┌──────▼──────┐              ┌──────▼──────┐
     │  Fournisseur│              │  Vos APIs / │
     │  LLM (API)  │              │  services   │
     └─────────────┘              └─────────────┘
```

| Brique | Rôle |
|--------|------|
| **UI chat** | Saisir, afficher, citations, loading |
| **Orchestrateur** | Envoie le prompt, gère la boucle d’outils |
| **LLM** | Comprend, choisit les tools, rédige |
| **Tools** | Fonctions sûres qui appellent *votre* code |
| **Mémoire** | Historique court de la conversation |
| **Auth / RBAC** | L’utilisateur ne voit que *ses* données |
| **Logs / eval** | Mesurer qualité et coût |

---

## 4. Le cœur : LLM + outils (tool calling)

### 4.1 Idée centrale

Vous **ne demandez pas** au modèle d’inventer le stock.  
Vous lui **déclarez** une fonction :

```text
nom : get_order_status
description : Retourne le statut d'une commande par son numéro
paramètres :
  - order_id (string, obligatoire)
```

Quand l’utilisateur dit « Où en est la commande A-42 ? », le modèle répond souvent ainsi (conceptuellement) :

```json
{
  "tool": "get_order_status",
  "arguments": { "order_id": "A-42" }
}
```

Votre backend exécute la vraie fonction, renvoie le JSON réel, puis le LLM formule :

> « La commande A-42 est en livraison, départ prévu demain. »

### 4.2 Boucle typique (à retenir)

```
1. Utilisateur envoie un message
2. Backend construit : system prompt + historique + message + liste des tools
3. Appel LLM
4. Si le LLM demande un tool :
     a. Backend exécute le tool (avec l’identité de l’utilisateur !)
     b. Backend renvoie le résultat au LLM
     c. Retour à l’étape 3 (max N fois)
5. Sinon : le LLM renvoie la réponse finale → affichage UI
```

**Limite importante :** plafonnez le nombre d’appels d’outils par message (ex. 5). Évite les boucles infinies et la facture qui explose.

### 4.3 Règle d’or du grounding

> Toute affirmation factuelle (chiffre, statut, nom, date) doit venir d’un **résultat d’outil** ou d’un **document RAG**. Sinon le modèle doit dire « Je ne sais pas ».

Sans cette règle, vous construisez un **menteur élégant**.

---

## 5. Tutoriel pas à pas (mini-projet)

Nous construisons un assistant **lecture seule** pour une boutique fictive « DemoShop ».

**Capacités :**
- « Où en est ma commande X ? »
- « Quelles sont mes 5 dernières commandes ? »
- « Comment retourner un article ? » (réponse d’aide, sans outil BDD)

### Étape 0 — Prérequis

- Un compte chez un fournisseur LLM (OpenAI, Anthropic, Azure OpenAI, etc.)
- Une clé API stockée en variable d’environnement (`LLM_API_KEY`) — **jamais dans Git**
- Un backend (n’importe lequel : Node, Python, Java…)
- Une fausse base commandes (tableau en mémoire suffit pour apprendre)

### Étape 1 — Données fictives

```text
Commandes :
  A-41 | Alice | livrée     | 2026-07-01
  A-42 | Alice | en_transit | 2026-07-28
  A-43 | Bob   | préparée   | 2026-07-30
```

### Étape 2 — Définir 2 outils

**Tool 1 — `get_order`**
- Entrée : `order_id`
- Sortie : statut, client, date
- Règle : ne renvoyer la commande que si elle appartient à l’utilisateur connecté

**Tool 2 — `list_my_orders`**
- Entrée : `limit` (défaut 5)
- Sortie : liste des commandes de l’utilisateur

### Étape 3 — Endpoint chat

```http
POST /api/assistant/chat
Authorization: Bearer <token utilisateur>
Content-Type: application/json

{ "message": "Où en est A-42 ?", "conversationId": null }
```

Réponse :

```json
{
  "answer": "Votre commande A-42 est en transit depuis le 28 juillet 2026.",
  "toolsUsed": ["get_order"],
  "citations": [{ "type": "order", "id": "A-42" }]
}
```

### Étape 4 — Pseudo-code orchestrateur

```text
function chat(user, message, conversationId):
  history = loadHistory(conversationId, maxTurns=8)
  messages = [SYSTEM_PROMPT] + history + [userMessage(message)]

  for attempt in 1..5:
    llmResponse = callLLM(messages, tools=[get_order, list_my_orders])

    if llmResponse.isFinalAnswer:
      save(assistantMessage)
      return answer

    if llmResponse.wantsTool:
      result = executeTool(llmResponse.toolName, llmResponse.args, user)
      messages.append(toolResult(result))
      continue

  return "Je n’ai pas pu terminer la recherche. Réessayez."
```

### Étape 5 — Exécution sûre d’un tool

```text
function executeTool(name, args, user):
  if name == "get_order":
    order = db.findOrder(args.order_id)
    if order is null: return { error: "not_found" }
    if order.customerId != user.id: return { error: "forbidden" }
    return { id: order.id, status: order.status, date: order.date }

  if name == "list_my_orders":
    return db.listOrders(user.id, limit=min(args.limit, 10))

  return { error: "unknown_tool" }
```

**Point pédagogique critique :** le LLM **propose** l’appel ; **votre code** décide ce qui est autorisé. Ne faites jamais confiance aux arguments du modèle pour contourner la sécurité.

### Étape 6 — Interface minimale

- Zone de messages
- Champ de saisie
- Indicateur « Recherche en cours… »
- Affichage des citations (ex. lien vers la fiche commande)

Inutile d’avoir une UI parfaite pour valider le concept.

### Étape 7 — Tester à la main (recette)

| Question | Comportement attendu |
|----------|----------------------|
| « Où en est A-42 ? » (Alice) | Statut réel via `get_order` |
| « Où en est A-43 ? » (Alice) | Refus / introuvable (pas la commande de Bob) |
| « Mes dernières commandes » | Liste via `list_my_orders` |
| « Annule A-42 » | Explication + refus (pas d’outil d’écriture) |
| « Quel temps demain à Paris ? » | « Hors périmètre » |

---

## 6. Prompt système : l’art de cadrer le modèle

Le **system prompt** est le « contrat de stage » du modèle.

### Structure recommandée

```text
# Rôle
Tu es l’assistant de DemoShop. Tu aides les clients sur leurs commandes.

# Règles
- Réponds en français, clairement et brièvement.
- N’invente jamais un statut de commande.
- Utilise les outils pour toute info factuelle.
- Si l’outil renvoie forbidden/not_found, dis-le simplement.
- Tu ne peux pas annuler, rembourser ou modifier une commande.
- Si on te demande une action, explique la marche à suivre humaine.

# Style
- Ton professionnel et accessible.
- Cite le numéro de commande dans la réponse.

# Hors périmètre
- Météo, actualité, code source, sujets hors DemoShop.
```

### Conseils

1. **Court et testable** : un prompt de 3 pages est difficile à maintenir.
2. **Versionnez-le** dans Git (`system-prompt.md`).
3. **Changez une chose à la fois**, puis re-testez votre banque de questions.
4. Rappelez le grounding : *faits = outils uniquement*.

---

## 7. Sécurité et garde-fous

| Risque | Contre-mesure |
|--------|----------------|
| Hallucination | Tools + « je ne sais pas » |
| Fuite de données | Auth + filtre par `user.id` dans chaque tool |
| Prompt injection (« ignore tes règles ») | Instructions système fermes + tools comme seule source de vérité |
| Actions dangereuses | Pas d’outils d’écriture au début |
| Abus / coût | Rate limit (ex. 30 messages / heure / user) |
| Boucle infinie tools | Max 5 appels / message |
| Fuite de clé API | Secrets en env / vault, jamais front |
| Logs sensibles | Logger tool + id, pas tout le contenu PII si inutile |

### Anti-pattern dangereux

```text
❌ Tool "run_sql" avec la requête écrite par le LLM
❌ Tool "http_request" vers n’importe quelle URL
❌ Exécuter une action d’écriture parce que le LLM « a l’air sûr »
```

### Pattern sain

```text
✅ Tools étroits, typés, validés (schéma JSON)
✅ Droits vérifiés dans le code métier
✅ Confirmation humaine pour toute mutation
```

---

## 8. Coûts et performance

### Comment on paie

Les fournisseurs facturent en général :

- **tokens d’entrée** (prompt + historique + résultats d’outils)
- **tokens de sortie** (texte généré)

Une question avec 2–3 tools coûte plus cher qu’une question sans tool, car il y a **plusieurs allers-retours** au LLM.

### Leviers d’économie

1. Choisir un modèle **adapté** (petit pour le simple, grand pour le complexe)
2. Limiter l’historique (5–10 tours)
3. Truncate les résultats d’outils (max 10 lignes)
4. Mettre en cache le system prompt si le fournisseur le propose
5. Rate limiting

### Latency UX

Sans streaming, l’utilisateur attend 3–15 s.  
Avec **streaming** (SSE), la réponse apparaît progressivement : la perception est bien meilleure, même si le temps total est proche.

---

## 9. Qualité : comment savoir si ça marche

### Banque de questions (golden set)

Créez un fichier de 20–50 questions avec le résultat attendu :

| ID | Question | Critère de succès |
|----|----------|-------------------|
| Q1 | Statut A-42 (Alice) | Mentionne « en_transit » |
| Q2 | Statut A-43 (Alice) | Ne révèle pas les données de Bob |
| Q3 | Annule A-42 | Refuse d’exécuter |
| Q4 | Comment retourner | Donne la procédure d’aide |

### Métriques simples

| Métrique | Cible débutant |
|----------|----------------|
| Réponses grounded correctes | ≥ 80 % |
| Fuites hors droits | 0 % |
| Refus d’actions non supportées | 100 % |
| Satisfaction testeurs | « Utile » ≥ 70 % |

### Boucle d’amélioration

```
Observer échecs → classer (prompt / tool manquant / bug code / modèle)
              → corriger UNE cause
              → rejouer le golden set
```

---

## 10. Checklist de mise en production

- [ ] Feature flag pour activer / couper l’assistant
- [ ] Auth obligatoire sur l’endpoint chat
- [ ] Chaque tool vérifie les droits
- [ ] Quotas (messages / tokens)
- [ ] Timeout LLM + message d’erreur clair
- [ ] Logs : qui a demandé quoi, quels tools, durée, succès
- [ ] Rétention des conversations définie (30 / 90 jours…)
- [ ] Kill-switch documenté pour l’astreinte
- [ ] Page d’aide « ce que l’assistant peut / ne peut pas faire »
- [ ] Golden set rejoué avant chaque release majeure

---

## 11. Glossaire

| Terme | Définition courte |
|-------|-------------------|
| **LLM** | Large Language Model — modèle qui prédit du texte |
| **Token** | Unité de texte facturée / traitée (~0,75 mot en anglais, variable en FR) |
| **Prompt** | Texte d’instruction / contexte envoyé au modèle |
| **System prompt** | Instructions permanentes de rôle et de règles |
| **Tool / function calling** | Le modèle demande l’exécution d’une fonction déclarée |
| **Orchestrateur** | Code qui gère la boucle LLM ↔ tools |
| **Grounding** | Ancrer la réponse dans des faits récupérés |
| **Hallucination** | Affirmation confiante mais fausse |
| **RAG** | Enrichir le prompt avec des extraits de documents |
| **Agent** | Assistant capable d’enchaîner outils / actions vers un but |
| **RBAC** | Contrôle d’accès par rôles / permissions |

---

## 12. Pour aller plus loin

### Progression pédagogique suggérée

| Niveau | Projet |
|--------|--------|
| 1 | Chat sans tools (FAQ figée dans le prompt) — pour comprendre le LLM |
| 2 | 1–2 tools lecture seule + auth — **ce tutoriel** |
| 3 | Ajouter RAG sur une doc Markdown |
| 4 | Streaming + citations cliquables |
| 5 | Actions avec **confirmation** (« Oui, annuler ») |
| 6 | Multi-agents / planification (avancé) |

### Questions à se poser avant de coder

1. Quelles questions apportent vraiment de la valeur ?
2. Quelles données sont nécessaires, et via quelles APIs ?
3. Lecture seule ou actions ?
4. Qui a le droit de voir quoi ?
5. Où tourne le LLM (cloud public, Azure, on-prem) et est-ce acceptable ?
6. Quel budget mensuel max ?
7. Comment on mesure le succès à J+30 ?

---

## Annexe A — Schéma mental en une image texte

```
                    ┌─────────────┐
                    │  Utilisateur │
                    └──────┬──────┘
                           │ question
                    ┌──────▼──────┐
                    │ Orchestrateur│
                    └──────┬──────┘
                           │
              ┌────────────▼────────────┐
              │         LLM             │
              │  (comprend + rédige)    │
              └────────────┬────────────┘
                           │
              besoin d'un fait ?
                    │ oui
              ┌─────▼─────┐
              │   Tool    │──► API / BDD (droits user)
              └─────┬─────┘
                    │ JSON réel
              ┌─────▼─────┐
              │   LLM     │──► réponse grounded
              └───────────┘
```

## Annexe B — Erreurs classiques des débutants

1. **Laisser le LLM parler à la place de la BDD** → hallucinations.
2. **Mettre la clé API dans le frontend** → clé volée en 5 minutes.
3. **Un seul mega-tool générique** → impossible à sécuriser.
4. **Pas de limite d’historique** → coût et confusion croissants.
5. **Passer aux actions trop tôt** → incidents métier.
6. **Aucun test de non-régression** → chaque changement de prompt casse un cas.
7. **Promettre l’omniscience** → déception utilisateurs ; mieux vaut un périmètre clair.

---

## Conclusion

Créer un assistant IA, ce n’est pas « brancher ChatGPT sur mon site ».  
C’est construire un **système** :

> **Interface + identité utilisateur + LLM cadré + outils étroits + garde-fous + mesure de qualité.**

Maîtrisez d’abord un assistant **lecture seule** avec 2 outils et 20 questions de test.  
Ensuite seulement, élargissez le périmètre.

*Fin du tutoriel.*
