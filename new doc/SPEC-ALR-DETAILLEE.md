# Spécification détaillée — Module Alertes et escalade (ALR)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique
**Module :** Alertes et escalade (ALR)
**Version :** 1.0
**Date :** 14 juillet 2026
**Source :** Rétro-documentation à partir du code source réellement implémenté (`entity/Alert`, `entity/AlertRule`, `entity/AlertType`, `service/AlertEngineService`, `service/AlertRuleService`, `service/AlertRuleSeedProfileService`, `service/AlertTypeService`, `service/NotificationService`, `service/EmailService`, `service/DelaiService`, `job/AlertSchedulerJob`, `job/AlertDigestJob`, `controller/AlertRuleController`, `controller/AlertTypeController`, `controller/NotificationController` du backend Spring Boot ; `NotificationBell`, pages `/notifications` et `/admin/alert-types`, composant `AlertRulesPanel` du frontend Next.js)

---

## Table des matières

1. [Objectif et périmètre](#1-objectif-et-périmètre)
2. [Modèle de données](#2-modèle-de-données)
3. [Cas d'utilisation](#3-cas-dutilisation)
4. [Règles de gestion](#4-règles-de-gestion)

---

## 1. Objectif et périmètre

### 1.1. Objectif

Le module Alertes et escalade a pour objectif de :

- **Transformer** les échéances calculées par CHN-PASS (`FilePassage.dueAt`) en **alertes proactives** (rappel avant échéance, signalement au dépassement, escalade hiérarchique si le retard persiste) ;
- **Paramétrer** entièrement le comportement par **template de chaîne** (seuils, type d'alerte, destinataire, scope de priorité) — aucune matrice ni rôle destinataire n'est codé en dur dans le moteur ;
- **Administrer** un catalogue de **types d'alerte** (libellés, gabarits email) indépendant de toute énumération Java figée ;
- **Diffuser** chaque alerte sur les canaux actifs (`IN_APP`, `EMAIL`, et `SMS` si activé) de façon **idempotente** ;
- **Exposer** un centre de notifications in-app (badge, liste, marquage lu) et l'historique des alertes d'un dossier ;
- **Synthétiser** quotidiennement les retards à destination d'un rôle configurable (digest email, ALR-08).

### 1.2. Périmètre

**Inclus dans le module :**

| Bloc | Contenu |
|---|---|
| Catalogue des types | CRUD `alert_types` (codes seed `REMINDER`, `OVERDUE`, `ESCALATION`, `DAILY_DIGEST` + types métier personnalisés) |
| Règles par template | CRUD imbriqué sous `/api/admin/chain-templates/{id}/alert-rules` ; activation / désactivation ; application du profil de seed CDC §10.2 |
| Moteur d'évaluation | Job CRON (`AlertSchedulerJob`) → `AlertEngineService.evaluateAll()` : calcul des seuils à partir de `dueAt`, résolution destinataire, création/dispatch |
| Notifications | Canal in-app (= lignes `alerts` avec `channel = IN_APP`) ; email (`EmailService`) ; SMS stub (`SmsGatewayService`) |
| Centre de notifications | Liste, compteur non-lues, marquage lu / tout lu (`/api/notifications`) |
| Historique dossier | `GET /api/files/{fileId}/alerts` |
| Digest quotidien | Job CRON (`AlertDigestJob`) → email de synthèse par périmètre organisationnel |
| Garde technique | Permissions RBAC `ALERT_TYPES:*` et `ALERT_RULES:*` ; accès notifications limité au destinataire |

**Explicitement hors périmètre du module (traité ailleurs ou non livré) :**

- Calcul des échéances et calendrier ouvrés — module **DEL** (`DelaiService`) ; ce module **consomme** `dueAt` et `applyOffset` ;
- Cycle de vie des maillons (transmission, suspension, reprise) — module **CHN-PASS** ;
- Indication UI « en retard » sur le circuit (`PassageMapper.overdue`) — CHN-PASS / front dossier, distincte des alertes envoyées ;
- Journal d'audit transversal `ALERT_SENT` (AUD, Sprint 5) — non branché dans les services ALR à ce jour ;
- SMS opérateur local (ALR-09) — stub uniquement (`SMS_GATEWAY_NOT_IMPLEMENTED`) ; canal désactivé par défaut ;
- Persistance d'une ligne `Alert` pour le digest quotidien — le digest envoie un email agrégé **sans** créer d'enregistrements `alerts`.

---

## 2. Modèle de données

### 2.1. Entités

Toutes les entités héritent de `BaseEntity` :

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `id` | `UUID` | généré automatiquement | Identifiant technique unique (stockage `BINARY(16)`) |
| `createdAt` | `Instant` | date/heure courante à la création | Horodatage de création (non modifiable) |
| `updatedAt` | `Instant` | date/heure courante | Horodatage de dernière modification |

#### 2.1.1. `AlertType` (table `alert_types`)

Catalogue administrable des natures d'alerte. **Ce n'est pas une énumération Java** : `REMINDER`, `OVERDUE`, `ESCALATION`, `DAILY_DIGEST` sont des lignes seed (`systemDefined = true`) ; l'admin métier peut en créer d'autres sans redéploiement.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `code` | `String(30)` | — | Code unique ; normalisé en majuscules à la création ; **immuable** après création |
| `label` | `String(100)` | — | Libellé affiché (UI, objet email) |
| `description` | `String` (TEXT) | `null` | Description libre |
| `emailTemplateCode` | `String(100)` | `null` | Identifiant de gabarit email (réservé / extension) |
| `systemDefined` | `boolean` | `false` | `true` = type seed système : non supprimable |
| `active` | `boolean` | `true` | Statut ; un type inactif n'est plus proposé dans les listes actives |

#### 2.1.2. `AlertRule` (table `alert_rules`)

Règle de déclenchement **toujours rattachée à un `ChainTemplate`**. Un template sans règle active ne génère **aucune** alerte. Aucune règle globale / implicite n'existe dans le moteur.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `chainTemplate` | FK → `ChainTemplate` | — | Template parent (obligatoire) |
| `chainStepTemplate` | FK → `ChainStepTemplate` | `null` | Si renseigné : règle limitée à ce maillon ; `null` = tous les maillons du template |
| `thresholdCode` | `String(20)` | — | Libellé métier du seuil (ex. `J_MINUS_2`, `J_PLUS_3`) ; normalisé en majuscules ; **informatif** — le moteur utilise `offsetValue` / `offsetUnit` |
| `offsetValue` | `int` | — | Décalage par rapport à `FilePassage.dueAt` (négatif = avant, 0 = à l'échéance, positif = après) |
| `offsetUnit` | `DelayUnit` | `WORKING_DAYS` | Unité du décalage (`WORKING_DAYS` ou `WORKING_HOURS`) |
| `alertType` | FK → `AlertType` | — | Type d'alerte à émettre |
| `escalationLevel` | `Integer` | `null` | Numéro de palier (1, 2, 3…) sans signification métier propre ; sert surtout à l'affichage / email |
| `targetMode` | `AlertTargetMode` | `ROLE` | Mode de résolution du destinataire |
| `targetRole` | `UserRole` | `null` | Requis si `targetMode = ROLE` ; n'importe quelle valeur de l'enum (jamais fixée dans le moteur) |
| `priorityScope` | `String(20)` | `null` | `null` = toutes priorités ; `URGENT_PLUS` = dossiers `URGENT` ou `VERY_URGENT` uniquement |
| `active` | `boolean` | `true` | Seules les règles actives sont évaluées par le moteur |

#### 2.1.3. `Alert` (table `alerts`)

Alerte effectivement générée / envoyée. Les notifications in-app **sont** les lignes `Alert` avec `channel = IN_APP` (pas d'entité `Notification` séparée).

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `file` | FK → `FileEntity` | `null` | Dossier concerné (nullable en schéma pour digests futurs ; toujours renseigné par le moteur de seuils) |
| `filePassage` | FK → `FilePassage` | `null` | Maillon concerné |
| `alertRule` | FK → `AlertRule` | `null` | Règle à l'origine ; `ON DELETE SET NULL` en SQL |
| `alertType` | FK → `AlertType` | — | Type d'alerte (obligatoire) |
| `escalationLevel` | `Integer` | `null` | Copie du niveau de la règle au moment de l'émission |
| `channel` | `AlertChannel` | — | Canal utilisé (`IN_APP`, `EMAIL`, `SMS`) |
| `recipient` | FK → `User` | — | Destinataire (obligatoire) |
| `status` | `AlertStatus` | `PENDING` | Cycle de vie d'envoi / lecture |
| `sentAt` | `Instant` | `null` | Horodatage d'envoi réussi |
| `readAt` | `Instant` | `null` | Horodatage de lecture (canal in-app) |
| `errorMessage` | `String(500)` | `null` | Message tronqué si `status = FAILED` |

**Contrainte d'idempotence (SQL) :** unique `(file_passage_id, alert_rule_id, channel)` — un même couple (maillon, règle, canal) ne peut être généré qu'une fois.

#### 2.1.4. Entités consommées (hors propriété ALR)

| Entité | Usage par ALR |
|---|---|
| `FilePassage` | Candidats : statut `IN_PROGRESS`, `dueAt` non null ; échéance de référence des offsets |
| `FileEntity` | Statut `IN_PROGRESS` ; priorité pour `priorityScope` ; organisation pour le digest |
| `ChainTemplate` / `ChainStepTemplate` | Portée des règles ; libellé d'étape dans emails / digest |
| `User` / `Organization` | Destinataire ; résolution hiérarchique ; sous-arbre du digest |
| `DelaiService` | `applyOffset(dueAt, offset, unit)`, `countWorkingDays`, fuseau `Africa/Douala` |

### 2.2. Énumérations

#### `AlertStatus`

| Valeur | Signification |
|---|---|
| `PENDING` | Enregistrement créé, envoi en cours / à traiter |
| `SENT` | Envoi réussi (in-app immédiatement ; email/SMS après envoi) |
| `FAILED` | Échec d'envoi (SMTP, email manquant, SMS non implémenté…) |
| `READ` | Notification in-app marquée lue |

#### `AlertChannel`

| Valeur | Signification |
|---|---|
| `IN_APP` | Notification centre FluxPro (toujours actif) |
| `EMAIL` | Email SMTP (toujours actif côté liste de canaux ; peut aboutir à `FAILED` si SMTP mal configuré) |
| `SMS` | Inclus seulement si `fluxpro.alerts.sms.enabled=true` (stub phase 2) |

#### `AlertTargetMode`

| Valeur | Résolution du destinataire |
|---|---|
| `CURRENT_RESPONSIBLE` | `FilePassage.responsibleUser` |
| `ROLE` | `ResponsibleUserResolver.resolve(file, targetRole)` (remontée organisationnelle jusqu'à un utilisateur actif du rôle demandé) |

#### `DelayUnit` (partagé avec DEL / CHN)

| Valeur | Usage ALR |
|---|---|
| `WORKING_DAYS` | Offsets en jours ouvrés (calendrier CM + week-ends) |
| `WORKING_HOURS` | Offsets en heures ouvrées |

### 2.3. Profil de seed CDC §10.2 (`AlertRuleSeedProfileService`)

Profil **jamais** appliqué implicitement. Copié uniquement via `POST .../apply-default-profile`.

| `thresholdCode` | Offset | Type | Destinataire | Notes |
|---|---|---|---|---|
| `J_MINUS_2` | −2 j.o. | `REMINDER` | Responsable actuel | Rappel avant échéance |
| `J_PLUS_0` | 0 | `OVERDUE` | Responsable actuel | Dépassement |
| `J_PLUS_0` | 0 | `OVERDUE` | Rôle `SERVICE_HEAD` | Dépassement (chef de service) |
| `J_PLUS_3` | +3 j.o. | `ESCALATION` L1 | Rôle `DIRECTOR` | Escalade directeur |
| `J_PLUS_7` | +7 j.o. | `ESCALATION` L2 | Rôle `SECRETARY_GENERAL` | Escalade SG |
| `J_PLUS_15` | +15 j.o. | `ESCALATION` L3 | Rôle `EXECUTIVE_OFFICE` | Uniquement `priorityScope = URGENT_PLUS` |

### 2.4. Configuration runtime (`application.properties`)

| Propriété | Défaut | Rôle |
|---|---|---|
| `fluxpro.alerts.scheduler.cron` | `0 0/30 7-18 * * MON-FRI` | Fréquence d'évaluation (zone `Africa/Douala`) |
| `fluxpro.alerts.digest.cron` | `0 30 7 * * MON-FRI` | Digest quotidien 07:30 ouvrés |
| `fluxpro.alerts.digest.target-role` | `DIRECTOR` | Rôle destinataire du digest (toute valeur `UserRole`) |
| `fluxpro.alerts.from-address` | `alertes@mintp.cm` | Expéditeur email |
| `fluxpro.alerts.sms.enabled` | `false` | Active le canal SMS dans `activeChannels()` |
| `spring.mail.*` | (selon environnement) | SMTP ; en local, envoi peut échouer → statut `FAILED` |

### 2.5. Permissions RBAC

| Permission | Usage |
|---|---|
| `ALERT_TYPES:READ` / `CREATE` / `UPDATE` / `DELETE` | Administration du catalogue des types |
| `ALERT_RULES:READ` / `CREATE` / `UPDATE` / `DELETE` | Administration des règles d'un template |
| `FILES:READ` | Lecture de l'historique d'alertes d'un dossier |

Attribution seed typique : CRUD complet pour `SUPER_ADMIN` / `BUSINESS_ADMIN` ; lecture pour directions / SG / Cabinet. Les endpoints `/api/notifications*` n'utilisent **pas** de permission `NOTIFICATIONS:*` : l'accès est borné au destinataire connecté.

---

## 3. Cas d'utilisation

### UC-01 — Lister les types d'alerte actifs (référentiel applicatif)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur authentifié |
| Objectif | Obtenir les types utilisables (formulaires de règles, affichage) |
| Préconditions | Session valide |
| Déroulement nominal | 1. `GET /api/alert-types` (sans permission admin dédiée).<br>2. Retourne les types `active = true`, triés par libellé. |
| Exemple | Réponse : `[{ "code": "REMINDER", "label": "Rappel", ... }, ...]` |
| Règles (code) | — `AlertTypeService.listActive()` |

### UC-02 — Administrer le catalogue des types d'alerte

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permissions `ALERT_TYPES:*` |
| Objectif | Créer, modifier, activer/désactiver, supprimer des types |
| Préconditions | Pour suppression : type non système et non référencé par une règle ou une alerte |
| Déroulement nominal | 1. `GET/POST/PUT/PATCH/DELETE /api/admin/alert-types[/{id}]` (+ `/activate`, `/deactivate`).<br>2. Création : unicité du `code` (insensible à la casse), normalisation majuscules, `systemDefined = false`.<br>3. Mise à jour : refuse tout changement de `code`.<br>4. Suppression : refuse si `systemDefined` ou encore référencé. |
| Exemple | Suppression d'un type seed → `409 ALERT_TYPE_SYSTEM_DEFINED` ; type utilisé → `409 ALERT_TYPE_IN_USE` |
| Règles (code) | RG-ALR-01, RG-ALR-02 — `AlertTypeService` |
| UI | `/admin/alert-types` |

### UC-03 — Consulter / créer / modifier les règles d'un template

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `ALERT_RULES:READ` / `CREATE` / `UPDATE` |
| Objectif | Paramétrer les seuils d'un template précis (ALR-06) |
| Préconditions | Template existant ; si `chainStepTemplateId` fourni, le maillon appartient au template ; si `targetMode = ROLE`, `targetRole` obligatoire |
| Déroulement nominal | 1. `GET /api/admin/chain-templates/{chainTemplateId}/alert-rules`.<br>2. `POST` / `PUT /{ruleId}` avec `AlertRuleRequest` (offset, type, mode cible, priorité, actif…).<br>3. Normalise `thresholdCode` et `priorityScope` en majuscules ; efface `targetRole` si mode `CURRENT_RESPONSIBLE`. |
| Exemple | `targetMode = ROLE` sans `targetRole` → `400 ALERT_RULE_TARGET_ROLE_REQUIRED` ; maillon d'un autre template → `400 ALERT_RULE_STEP_INVALID` |
| Règles (code) | RG-ALR-03, RG-ALR-04, RG-ALR-05 — `AlertRuleService` |
| UI | `AlertRulesPanel` sur la fiche admin du template |

### UC-04 — Activer / désactiver / supprimer une règle

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `ALERT_RULES:UPDATE` ou `ALERT_RULES:DELETE` |
| Objectif | Couper une règle sans la supprimer, ou la retirer définitivement |
| Préconditions | Règle existante pour le template donné |
| Déroulement nominal | 1. `PATCH .../{ruleId}/activate` ou `/deactivate` → bascule `active`.<br>2. `DELETE .../{ruleId}` → suppression ; les alertes déjà émises restent (FK règle mise à null en SQL selon script). |
| Exemple | Règle absente pour ce template → `404 ALERT_RULE_NOT_FOUND` |
| Règles (code) | RG-ALR-03 — `AlertRuleService.activate/deactivate/delete()` |

### UC-05 — Appliquer le profil de seed CDC sur un template

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `ALERT_RULES:CREATE` |
| Objectif | Copier la matrice §10.2 comme point de départ éditable (ALR-F18) |
| Préconditions | Types seed présents en base (`REMINDER`, `OVERDUE`, `ESCALATION`) |
| Déroulement nominal | 1. `POST .../alert-rules/apply-default-profile?overwriteExisting={bool}`.<br>2. Si `overwriteExisting=true`, supprime toutes les règles du template puis recrée le profil.<br>3. Sinon, crée uniquement les lignes absentes (clé logique : `thresholdCode` + `targetMode` + `targetRole`). |
| Exemple | Types seed manquants → `409 ALERT_TYPE_SEED_MISSING` |
| Règles (code) | RG-ALR-06 — `AlertRuleSeedProfileService.apply()` |

### UC-06 — Évaluation périodique des seuils (job moteur)

| Champ | Détail |
|---|---|
| Acteur | Système (`AlertSchedulerJob`) |
| Objectif | Détecter les seuils franchis et déclencher les notifications (ALR-01 à ALR-04) |
| Préconditions | Application démarrée ; scheduling activé ; tables présentes |
| Déroulement nominal | 1. Cron (défaut : toutes les 30 min, 7h–18h, lun–ven, zone Douala).<br>2. `evaluateAll()` charge les candidats : dossier `IN_PROGRESS`, passage `IN_PROGRESS`, `dueAt IS NOT NULL`.<br>3. Pour chaque passage : charge les règles **actives** du template lié.<br>4. Filtre par maillon et `priorityScope` ; calcule `threshold = applyOffset(dueAt, offset, unit)` ; déclenche si `now >= threshold`.<br>5. Résout le destinataire ; pour chaque canal actif, crée une `Alert` `PENDING` si le triplet n'existe pas encore, puis `dispatch`. |
| Exemple | Maillon à délai 0 (`dueAt = null`) → jamais candidat ; dossier `ON_HOLD` ou passage `SUSPENDED` → exclus |
| Règles (code) | RG-ALR-07 à RG-ALR-13 — `AlertEngineService`, `FilePassageRepository.findActiveCandidatesForAlerts()` |

### UC-07 — Diffuser une alerte (in-app / email / SMS)

| Champ | Détail |
|---|---|
| Acteur | Système (`NotificationService`) |
| Objectif | Faire aboutir l'alerte sur le canal demandé (ALR-05) |
| Préconditions | Ligne `Alert` créée avec destinataire |
| Déroulement nominal | 1. `IN_APP` : passe immédiatement à `SENT` + `sentAt`.<br>2. `EMAIL` : envoi SMTP via `EmailService` ; succès → `SENT`, échec → `FAILED` + `errorMessage`.<br>3. `SMS` : appel gateway ; stub → échec typique `SMS_GATEWAY_NOT_IMPLEMENTED`. |
| Exemple | SMTP non configuré en local → alerte email `FAILED`, in-app tout de même `SENT` |
| Règles (code) | RG-ALR-14, RG-ALR-15 — `NotificationService.dispatch()` |

### UC-08 — Consulter mes notifications in-app

| Champ | Détail |
|---|---|
| Acteur | Utilisateur authentifié (destinataire) |
| Objectif | Voir ses alertes et le badge de non-lues (ALR-05) |
| Préconditions | Session valide |
| Déroulement nominal | 1. `GET /api/notifications?unreadOnly=` (pagination).<br>2. `GET /api/notifications/unread-count` → `{ "count": n }`.<br>3. Filtre strict : `recipient = utilisateur courant` et `channel = IN_APP`. |
| Exemple | Badge cloche dans l'AppShell alimenté par le compteur |
| Règles (code) | RG-ALR-16 — `NotificationService.listForUser()` / `countUnread()` |
| UI | `NotificationBell`, page `/notifications` |

### UC-09 — Marquer une notification (ou toutes) comme lue

| Champ | Détail |
|---|---|
| Acteur | Destinataire de la notification |
| Objectif | Accuser lecture (ALR-F12) |
| Préconditions | Alerte existante appartenant à l'acteur |
| Déroulement nominal | 1. `PATCH /api/notifications/{id}/read` : pose `readAt` ; si statut `SENT` → `READ`.<br>2. `PATCH /api/notifications/read-all` : marque toutes les in-app non lues de l'utilisateur. |
| Exemple | Tentative sur une alerte d'un autre utilisateur → `403 ALERT_ACCESS_DENIED` |
| Règles (code) | RG-ALR-16 — `NotificationService.markRead()` / `markAllRead()` |

### UC-10 — Consulter l'historique des alertes d'un dossier

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:READ` et droit d'accès au dossier |
| Objectif | Auditer les alertes émises sur le dossier (tous canaux) |
| Préconditions | `assertCanAccessFile` satisfait |
| Déroulement nominal | 1. `GET /api/files/{fileId}/alerts`.<br>2. Retourne les alertes du dossier, ordre chronologique décroissant. |
| Exemple | Visible dans la fiche dossier (`AlertHistoryCard`) |
| Règles (code) | RG-ALR-17 — `NotificationService.listForFile()` + `AccessControlService` |

### UC-11 — Digest quotidien des retards (ALR-08)

| Champ | Détail |
|---|---|
| Acteur | Système (`AlertDigestJob`) |
| Objectif | Envoyer à chaque utilisateur actif du rôle configuré un email listant les retards de **son sous-arbre organisationnel** |
| Préconditions | Cron digest ; passages en retard (`dueAt < now`, dossier et passage `IN_PROGRESS`) |
| Déroulement nominal | 1. `runDailyDigest(UserRole.valueOf(digest.target-role))`.<br>2. Charge les retards globaux, filtre par sous-arbre de l'organisation du destinataire.<br>3. Si la liste filtrée est non vide, envoie un email texte (référence, objet, étape, échéance Douala, jours ouvrés de retard).<br>4. **Ne crée pas** de ligne `Alert` / n'utilise pas le type `DAILY_DIGEST` en persistance. |
| Exemple | Défaut : tous les `DIRECTOR` actifs reçoivent à 07:30 les retards de leur direction |
| Règles (code) | RG-ALR-18, RG-ALR-19 — `AlertEngineService.runDailyDigest()` |

### UC-12 — Reprise après suspension / attente externe (ALR-07)

| Champ | Détail |
|---|---|
| Acteur | Système (induit par CHN-PASS) |
| Objectif | Ne pas alerter pendant `ON_HOLD` / `SUSPENDED`, et ne pas « rattraper » en rafale les seuils manqués à la reprise |
| Préconditions | Dossier ou maillon sorti de suspension / hold |
| Déroulement nominal | 1. Pendant la suspension : le maillon n'apparaît plus dans `findActiveCandidatesForAlerts`.<br>2. À la reprise : le prochain passage du job compare `now` aux seuils ; seuls les seuils encore non émis (idempotence) peuvent partir ; ceux déjà envoyés avant suspension ne sont pas recréés. |
| Exemple | Seuil J−2 franchi pendant une suspension n'est pas « rattrapé » si, à la reprise, l'alerte n'avait jamais été créée — comportement accepté : pas de catch-up historique |
| Règles (code) | RG-ALR-08, RG-ALR-12 — requêtes candidats + unicité |

---

## 4. Règles de gestion

| ID | Règle | Composant (code) |
|---|---|---|
| RG-ALR-01 | Le code d'un type d'alerte est unique (insensible à la casse), normalisé en majuscules à la création, et **immuable** ensuite. | `AlertTypeService.create()` / `update()` |
| RG-ALR-02 | Un type `systemDefined` ne peut pas être supprimé ; un type encore référencé par une règle ou une alerte ne peut pas être supprimé (`ALERT_TYPE_IN_USE`). | `AlertTypeService.delete()` |
| RG-ALR-03 | Toute règle d'alerte appartient obligatoirement à un `ChainTemplate` ; le moteur n'évalue que les règles `active = true` de ce template. Un template sans règle ne produit aucune alerte. | `AlertRule` FK, `AlertEngineService.evaluatePassage()` |
| RG-ALR-04 | Si `targetMode = ROLE`, `targetRole` est obligatoire ; si `targetMode = CURRENT_RESPONSIBLE`, `targetRole` est forcé à `null`. | `AlertRuleService.applyRequest()` |
| RG-ALR-05 | Un `chainStepTemplateId` optionnel doit appartenir au même template que la règle ; sinon `ALERT_RULE_STEP_INVALID`. | `AlertRuleService.applyRequest()` |
| RG-ALR-06 | Le profil CDC §10.2 n'est jamais appliqué implicitement : uniquement via `apply-default-profile`, avec option d'écrasement. | `AlertRuleSeedProfileService` |
| RG-ALR-07 | Les candidats au moteur sont strictement : dossier `IN_PROGRESS` **et** passage `IN_PROGRESS` **et** `dueAt IS NOT NULL`. | `FilePassageRepository.findActiveCandidatesForAlerts()` |
| RG-ALR-08 | Aucune alerte n'est générée pour un dossier `ON_HOLD` ni pour un maillon `SUSPENDED` (exclusion via RG-ALR-07). | `AlertSchedulerJob` → candidats |
| RG-ALR-09 | Un maillon sans échéance (`dueAt = null`, typiquement délai 0 / clôture) n'entre jamais dans le moteur et n'est jamais « en retard » côté alertes. | `DelaiService.calculateDueDate` (délai ≤ 0 → null) + filtre candidats |
| RG-ALR-10 | Le seuil d'une règle est toujours calculé par `DelaiService.applyOffset(dueAt, offsetValue, offsetUnit)` (fuseau `Africa/Douala`, jours/heures ouvrés), **jamais** à partir de `receivedAt`. | `AlertEngineService.evaluatePassage()` |
| RG-ALR-11 | Une règle s'applique au maillon si `chainStepTemplate` est null ou identique ; `priorityScope = URGENT_PLUS` n'accepte que les dossiers dont la priorité ≠ `NORMAL`. | `appliesToStep` / `appliesToPriority` |
| RG-ALR-12 | Idempotence : au plus une alerte par triplet `(filePassage, alertRule, channel)` (contrôle applicatif + contrainte unique SQL). | `AlertRepository.exists…`, `uk_alert_idempotence` |
| RG-ALR-13 | Aucun rôle destinataire n'est codé en dur dans le moteur : résolution via `CURRENT_RESPONSIBLE` ou `ResponsibleUserResolver.resolve(file, targetRole)`. Si aucun destinataire n'est trouvé, l'alerte n'est pas créée (warning journalisé). | `AlertEngineService.resolveRecipient()` |
| RG-ALR-14 | Canaux actifs : toujours `IN_APP` + `EMAIL` ; `SMS` uniquement si `fluxpro.alerts.sms.enabled=true`. | `NotificationService.activeChannels()` |
| RG-ALR-15 | Échec d'envoi email/SMS → statut `FAILED` + message tronqué (≤ 500) ; l'échec d'un canal n'empêche pas la création / envoi des autres canaux du même déclenchement. | `NotificationService.sendVia()` |
| RG-ALR-16 | Une notification in-app n'est lisible / marquable que par son destinataire (`ALERT_ACCESS_DENIED` sinon). | `NotificationService.markRead()` |
| RG-ALR-17 | L'historique dossier exige `FILES:READ` **et** le contrôle d'accès dossier (`assertCanAccessFile`). | `NotificationController` |
| RG-ALR-18 | Le digest quotidien cible le rôle configuré (`fluxpro.alerts.digest.target-role`), filtre les retards au sous-arbre organisationnel du destinataire, et n'envoie un email que si au moins un retard est dans ce périmètre. | `AlertEngineService.runDailyDigest()` |
| RG-ALR-19 | Le digest n'écrit pas dans `alerts` ; il n'impacte pas l'idempotence des alertes de seuil. | `runDailyDigest()` + `EmailService.sendDigest()` |
| RG-ALR-20 | Les scripts de schéma / permissions ALR sont manuels (`docs/sql/2026-07-04_alert_*.sql`, `2026-07-04_alr_rbac_permissions.sql`) ; `ddl-auto` reste `none`. | Convention projet + initializers |

---

## Annexes

### A. Cartographie endpoints

| Méthode | Chemin | Permission / garde |
|---|---|---|
| `GET` | `/api/alert-types` | Authentifié |
| `GET` | `/api/admin/alert-types` | `ALERT_TYPES:READ` |
| `GET` | `/api/admin/alert-types/{id}` | `ALERT_TYPES:READ` |
| `POST` | `/api/admin/alert-types` | `ALERT_TYPES:CREATE` |
| `PUT` | `/api/admin/alert-types/{id}` | `ALERT_TYPES:UPDATE` |
| `PATCH` | `/api/admin/alert-types/{id}/activate` | `ALERT_TYPES:UPDATE` |
| `PATCH` | `/api/admin/alert-types/{id}/deactivate` | `ALERT_TYPES:UPDATE` |
| `DELETE` | `/api/admin/alert-types/{id}` | `ALERT_TYPES:DELETE` |
| `GET` | `/api/admin/chain-templates/{id}/alert-rules` | `ALERT_RULES:READ` |
| `GET` | `/api/admin/chain-templates/{id}/alert-rules/{ruleId}` | `ALERT_RULES:READ` |
| `POST` | `/api/admin/chain-templates/{id}/alert-rules` | `ALERT_RULES:CREATE` |
| `PUT` | `/api/admin/chain-templates/{id}/alert-rules/{ruleId}` | `ALERT_RULES:UPDATE` |
| `PATCH` | `.../activate` / `.../deactivate` | `ALERT_RULES:UPDATE` |
| `DELETE` | `.../alert-rules/{ruleId}` | `ALERT_RULES:DELETE` |
| `POST` | `.../alert-rules/apply-default-profile` | `ALERT_RULES:CREATE` |
| `GET` | `/api/notifications` | Destinataire courant |
| `GET` | `/api/notifications/unread-count` | Destinataire courant |
| `PATCH` | `/api/notifications/{id}/read` | Destinataire courant |
| `PATCH` | `/api/notifications/read-all` | Destinataire courant |
| `GET` | `/api/files/{fileId}/alerts` | `FILES:READ` + accès dossier |

### B. Scripts SQL associés

| Fichier | Objet |
|---|---|
| `docs/sql/2026-07-04_alert_types.sql` | Création `alert_types` |
| `docs/sql/2026-07-04_alert_rules.sql` | Création `alert_rules` |
| `docs/sql/2026-07-04_alerts.sql` | Création `alerts` + unicité d'idempotence |
| `docs/sql/2026-07-04_alr_rbac_permissions.sql` | Permissions et rattachements de rôles |

### C. Correspondance CDC

| Exigence CDC | Couverture dans ce module |
|---|---|
| ALR-01 (J−2) | Profil seed + moteur (offset négatif) |
| ALR-02 (J+0) | Profil seed double destinataire (responsable + chef de service) |
| ALR-03 (J+3) | Profil seed escalade L1 `DIRECTOR` |
| ALR-04 (J+7) | Profil seed escalade L2 `SECRETARY_GENERAL` |
| ALR-05 | Canaux `IN_APP` + `EMAIL` + centre de notifications |
| ALR-06 | Règles par template + catalogue de types |
| ALR-07 | Exclusion `ON_HOLD` / `SUSPENDED` via candidats |
| ALR-08 | `AlertDigestJob` |
| ALR-09 | Stub SMS, désactivé par défaut |
