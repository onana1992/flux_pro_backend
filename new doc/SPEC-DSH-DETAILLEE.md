# Spécification détaillée — Module Tableaux de bord et reporting (DSH)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique
**Module :** Tableaux de bord et reporting (DSH) — widgets de pilotage, compteurs, classements, export CSV
**Version :** 1.0
**Date :** 16 juillet 2026
**Source :** Rétro-documentation à partir du code source réellement implémenté (`service/DashboardService`, `service/DashboardExportService`, `controller/DashboardController`, `enumeration/DashboardScopeWidth`, `dto/response/Dashboard*`, `dto/response/MyActivity*`, `dto/response/WorkloadEntryResponse`, `dto/response/OverdueFileResponse`, `dto/response/DelayByTypeResponse`, `dto/response/OrganizationRankingResponse`, `common/DashboardException`, requêtes DSH de `FileRepository` / `FilePassageRepository`, `docs/sql/2026-07-04_dsh_rbac_permissions.sql` du backend Spring Boot ; pages `/dashboard`, `/rapports` et composants `DashboardWidgets` du frontend Next.js)

**Référence cible :** [docs/SPEC-DSH.md](../docs/SPEC-DSH.md) (spécification produit CDC §7.6 / §11) — le présent document décrit **uniquement** ce qui est livré dans le code.

---

## Table des matières

1. [Objectif et périmètre](#1-objectif-et-périmètre)
2. [Modèle de données](#2-modèle-de-données)
3. [Cas d'utilisation](#3-cas-dutilisation)
4. [Règles de gestion](#4-règles-de-gestion)

---

## 1. Objectif et périmètre

### 1.1. Objectif

Le module Tableaux de bord et reporting a pour objectif de :

- **Agréger** à la lecture les données déjà produites par DOS / CHN-PASS / DEL / ALR (dossiers, maillons, échéances) en **indicateurs consultables** ;
- **Contextualiser** automatiquement la largeur des données visibles via `OrganizationScopeService` — **aucune vue codée en dur par nom de rôle** ;
- **Exposer** des widgets indépendants (mon activité, compteurs, charge par agent, top retards, délai moyen par type, classement de conformité) ;
- **Indiquer** au frontend la largeur de périmètre (`scopeWidth`) pour composer un unique écran `/dashboard` ;
- **Exporter** en CSV les mêmes jeux de données que les widgets (sans logique métier dupliquée) ;
- **Réutiliser** strictement la définition du retard ALR (`dueAt` dépassé, maillon et dossier `IN_PROGRESS`) et le calendrier `DelaiService` / horloge métier `ClockService`.

### 1.2. Périmètre

**Inclus dans le module :**

| Bloc | Contenu |
|---|---|
| Compteurs de périmètre | Actifs, en retard, clôturés / créés du mois civil (`Africa/Douala`) — DSH-05 |
| Mon activité | Maillons dont l'appelant est responsable + transmissions récentes 30 j — DSH-01 |
| Charge par agent | Répartition des maillons actifs par responsable dans le périmètre — DSH-02 |
| Top retards | Liste ordonnée des maillons en retard (même population que le digest ALR-08) — DSH-01/02/03 |
| Délai moyen par type | Moyenne en jours ouvrés sur dossiers clôturés, fenêtre glissante — DSH-06 |
| Classement conformité | Taux de respect des délais regroupé par niveau d'`OrganizationType` — DSH-07 |
| Export CSV | Datasets `overdue-files`, `workload`, `compliance-ranking`, `delay-by-type` — DSH-08 |
| Largeur de périmètre | Enum informatif `DashboardScopeWidth` (`SELF` / `SUBTREE` / `REGIONAL` / `GLOBAL`) |
| Garde technique | Permissions `DASHBOARD:READ` / `DASHBOARD:EXPORT` ; filtre org via scope RBAC |

**Explicitement hors périmètre du module (non livré ou traité ailleurs) :**

- Cycle de vie des dossiers et pièces jointes — module **DOS** ;
- Circulation des maillons (transmit / return / suspend) — module **CHN-PASS** ;
- Calcul des échéances et jours ouvrés — module **DEL** (`DelaiService`) ; DSH **consomme** uniquement ;
- Moteur d'alertes / digest email — module **ALR** (DSH partage la **définition** du retard, pas le dispatch) ;
- Export **PDF**, rapport mensuel automatique §11.1, table `dashboard_daily_snapshot` — prévus dans `SPEC-DSH.md`, **non implémentés** ;
- Heatmap transversale dédiée (DSH-04) — non livrée ; le classement + jauge frontend en tiennent lieu partiellement pour les périmètres `GLOBAL` / `REGIONAL` ;
- Cache Redis / stats nationales XLSX — Phase 2.

---

## 2. Modèle de données

### 2.1. Persistance

Le module DSH **ne possède aucune table métier propre**. Tous les indicateurs sont calculés à la lecture sur :

| Source | Usage DSH |
|---|---|
| `files` (`FileEntity`) | Compteurs actifs / créés / clôturés ; délai réel ; conformité |
| `file_passages` (`FilePassage`) | Activité personnelle, charge, retards |
| `chain_templates` | Délai cible (`totalDelayDays` + `delayUnit`) |
| `file_types` | Libellé du type pour DSH-06 |
| `organizations` / `organization_types` | Périmètre RBAC ; regroupement du classement |
| `users` | Identité des responsables (charge, top retards) |

Horodatage de référence : `ClockService.now()` (horloge système ou horloge de test). Fuseau métier : `DelaiService.BUSINESS_ZONE` = `Africa/Douala`.

### 2.2. Énumération `DashboardScopeWidth`

Indicateur **informatif** renvoyé dans `DashboardSummaryResponse` pour que le frontend décide quels widgets charger. Ce n'est **pas** un filtre SQL autonome : le filtre réel reste `OrganizationScopeService.ScopeFilter`.

| Valeur | Signification | Calcul (`DashboardService.computeScopeWidth`) |
|---|---|---|
| `SELF` | Périmètre personnel — une seule organisation, hors directeur régional | `!allOrgs` et `organizationIds.size() ≤ 1` |
| `SUBTREE` | Périmètre équipe / direction (plusieurs orgs descendantes) | `organizationIds.size() > 1` |
| `REGIONAL` | Périmètre DRTP | Rôle `REGIONAL_DIRECTOR` (même si sous-arbre) |
| `GLOBAL` | Périmètre national / transversal | `scope.allOrganizations() = true` |

Libellés UI (i18n) : « Périmètre personnel », « Périmètre : équipe », « Périmètre régional », « Périmètre national ».

### 2.3. DTOs de réponse (contrats API)

#### 2.3.1. `DashboardSummaryResponse`

| Champ | Type | Description |
|---|---|---|
| `organizationId` | `UUID?` | Filtre org optionnel passé en query (sinon `null`) |
| `organizationCode` | `String?` | Code org de l'acteur, ou de l'org filtrée si c'est la sienne |
| `scopeWidth` | `DashboardScopeWidth` | Largeur informative (§2.2) |
| `activeFiles` | `long` | Dossiers `IN_PROGRESS` dans le périmètre |
| `overdueFiles` | `long` | Nombre de **dossiers distincts** ayant ≥ 1 maillon en retard |
| `closedThisMonth` | `long` | Clôturés depuis le 1er du mois civil jusqu'à `now` |
| `createdThisMonth` | `long` | `receivedAt` dans le mois civil courant |

#### 2.3.2. `MyActivityResponse` / `MyActivityItemResponse`

| Champ | Type | Description |
|---|---|---|
| `activeCount` | `long` | Maillons `IN_PROGRESS` dont `responsibleUser = moi` |
| `overdueCount` | `long` | Sous-ensemble en retard (`DelaiService.isOverdue`) |
| `transmittedRecentCount` | `long` | Maillons `COMPLETED`/`RETURNED` avec `transmittedAt` ≥ now − 30 j |
| `items[]` | liste | Détail des maillons actifs (tri `dueAt` ASC) |
| `items[].passageId` / `fileId` | `UUID` | Identifiants |
| `items[].fileReferenceNumber` / `fileSubject` | `String?` | Métadonnées dossier |
| `items[].stepLabel` | `String` | Libellé du maillon template |
| `items[].receivedAt` / `dueAt` | `Instant?` | Horodatages |
| `items[].overdue` | `boolean` | Retard calculé à `now` |

#### 2.3.3. `WorkloadEntryResponse`

| Champ | Type | Description |
|---|---|---|
| `userId` | `UUID` | Responsable |
| `firstName` / `lastName` | `String` | Identité |
| `organizationCode` | `String?` | Org du responsable |
| `activeCount` | `long` | Maillons actifs |
| `overdueCount` | `long` | Dont en retard |

Tri service : `overdueCount` DESC, puis `activeCount` DESC.

#### 2.3.4. `OverdueFileResponse`

| Champ | Type | Description |
|---|---|---|
| `fileId` | `UUID` | Dossier |
| `referenceNumber` / `subject` / `fileTypeCode` | `String?` | Métadonnées |
| `organizationCode` | `String` | Org du dossier |
| `stepLabel` | `String` | Maillon en retard |
| `responsibleUserName` | `String?` | Prénom + nom |
| `dueAt` | `Instant` | Échéance dépassée |
| `daysOverdue` | `int` | Jours ouvrés entre `dueAt` et `now` (`DelaiService.countWorkingDays`) |

#### 2.3.5. `DelayByTypeResponse`

| Champ | Type | Description |
|---|---|---|
| `fileTypeCode` / `fileTypeLabel` | `String` | Code + libellé (`file_types.name` ou code) |
| `closedCount` | `long` | Dossiers clôturés dans la fenêtre |
| `averageDelayDays` | `double` | Moyenne des délais réels (2 décimales) |
| `targetDelayDays` | `Integer?` | Cible commune si tous partagent le même `chainTemplate` ; sinon `null` |

#### 2.3.6. `OrganizationRankingResponse`

| Champ | Type | Description |
|---|---|---|
| `organizationId` / `organizationCode` / `organizationName` | — | Ancêtre de regroupement |
| `closedCount` | `long` | Dossiers clôturés dans la fenêtre |
| `compliantCount` | `long` | Dont délai réel ≤ délai cible du template |
| `complianceRate` | `double` | Pourcentage (2 décimales) |

Tri : `complianceRate` DESC.

### 2.4. Relations logiques (lecture seule)

| Relation | Description |
|---|---|
| Acteur → `ScopeFilter` | Résolu une fois par requête (sauf `my-activity`) |
| Dossier → org ∈ scope | Filtre des compteurs / retards / délais / classement |
| Maillon → `responsibleUser.org` ∈ scope | Filtre spécifique de la **charge** (pas l'org du dossier) |
| Dossier → `chainTemplate` | Nécessaire pour la conformité (sinon non conforme) |
| Dossier → ancêtre `OrganizationType` | Regroupement du classement (`resolveGroupAncestor`) |

### 2.5. Schéma SQL

| Script | Rôle |
|---|---|
| `docs/sql/2026-07-04_dsh_rbac_permissions.sql` | Permissions `DASHBOARD:READ` / `EXPORT` + matrices rôles (idempotent) |

Aucune migration de table métier DSH. Règle projet : `spring.jpa.hibernate.ddl-auto=none`.

---

## 3. Cas d'utilisation

### UC-01 — Consulter les compteurs de périmètre (summary)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `DASHBOARD:READ` |
| Objectif | Obtenir KPI actifs / retards / clôturés / créés du mois + `scopeWidth` |
| Préconditions | Authentifié ; périmètre org non vide (sinon compteurs à 0) |
| Déroulement nominal | 1. `GET /api/dashboard/summary?organizationId=&fileTypeCode=`.<br>2. Résout le scope ; si `organizationId` fourni, `assertCanAccessOrganization`.<br>3. Compte actifs (`FileRepository.countActiveByScope`) ; retards distincts (`findOverdueForScope`) ; clôturés / créés du mois civil Douala.<br>4. Retourne `DashboardSummaryResponse`. |
| Exemple | Agent → `scopeWidth=SELF`, compteurs limités à son org |
| Règles (code) | RG-DSH-01, RG-DSH-02, RG-DSH-03, RG-DSH-04 — `DashboardService.summary()` |

### UC-02 — Consulter mon activité

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `DASHBOARD:READ` |
| Objectif | Voir mes maillons en cours, mes retards, mes transmissions récentes |
| Préconditions | Aucune — **non filtré** par périmètre organisationnel |
| Déroulement nominal | 1. `GET /api/dashboard/my-activity`.<br>2. Charge maillons actifs dont `responsibleUser = acteur`.<br>3. Compte transmissions `COMPLETED`/`RETURNED` sur 30 jours glissants.<br>4. Retourne `MyActivityResponse` (items triés par `dueAt`). |
| Exemple | Un directeur voit aussi ses propres maillons, indépendamment de sa direction |
| Règles (code) | RG-DSH-05 — `DashboardService.myActivity()` |

### UC-03 — Consulter la charge par agent

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `DASHBOARD:READ` (UI : si `scopeWidth ≠ SELF`) |
| Objectif | Répartition des maillons actifs par responsable dans le périmètre |
| Préconditions | Scope non vide |
| Déroulement nominal | 1. `GET /api/dashboard/workload?organizationId=`.<br>2. `findActiveForWorkload` (filtre sur **org du responsable**).<br>3. Agrège actifs / retards par user ; tri retards DESC. |
| Exemple | Chef de service : liste des agents de son sous-arbre |
| Règles (code) | RG-DSH-01, RG-DSH-06 — `DashboardService.workload()` |

### UC-04 — Consulter le top des dossiers en retard

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `DASHBOARD:READ` (UI : si `scopeWidth ≠ SELF`) |
| Objectif | Lister les maillons les plus en retard (échéance la plus ancienne d'abord) |
| Préconditions | Scope non vide |
| Déroulement nominal | 1. `GET /api/dashboard/overdue-files?organizationId=&limit=` (défaut `limit=10`).<br>2. `findOverdueForScope` (définition = digest ALR-08 + filtre org dossier).<br>3. Borne `limit` dans `[1, 100]` ; mappe `daysOverdue` en jours ouvrés. |
| Exemple | UI dashboard demande `limit=8` |
| Règles (code) | RG-DSH-01, RG-DSH-03, RG-DSH-07 — `DashboardService.overdueFiles()` |

### UC-05 — Consulter le délai moyen par type de dossier

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `DASHBOARD:READ` |
| Objectif | Comparer délai réel moyen vs cible par `fileTypeCode` |
| Préconditions | `windowDays` ∈ [1, 365] ; scope non vide |
| Déroulement nominal | 1. `GET /api/dashboard/delay-by-type?organizationId=&windowDays=` (défaut 30).<br>2. Charge dossiers `CLOSED` depuis `now − windowDays`.<br>3. Groupe par type ; moyenne `countWorkingDays(receivedAt→closedAt)` ; cible consensus si un seul template. |
| Exemple | `windowDays=0` → `400 DASHBOARD_WINDOW_INVALID` |
| Règles (code) | RG-DSH-01, RG-DSH-08, RG-DSH-09 — `DashboardService.delayByType()` |

### UC-06 — Consulter le classement de conformité

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `DASHBOARD:READ` (UI : si `scopeWidth` ∈ {`GLOBAL`,`REGIONAL`}) |
| Objectif | Classer les organisations (niveau de type paramétrable) par taux de respect des délais |
| Préconditions | `groupByTypeCode` existant dans `organization_types` ; `windowDays` valide |
| Déroulement nominal | 1. `GET /api/dashboard/compliance-ranking?groupByTypeCode=&windowDays=` (défauts `DIRECTORATE`, 90).<br>2. Valide le type ; charge clôturés du scope ; remonte chaque dossier à l'ancêtre du type demandé.<br>3. Conformité = délai réel ≤ délai cible du `chainTemplate` (conversion heures→jours si `WORKING_HOURS`, 9 h/j). |
| Exemple | Type inconnu → `400 DASHBOARD_GROUP_TYPE_INVALID` |
| Règles (code) | RG-DSH-01, RG-DSH-08, RG-DSH-10, RG-DSH-11 — `DashboardService.complianceRanking()` |

### UC-07 — Exporter un jeu de données CSV

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `DASHBOARD:EXPORT` |
| Objectif | Télécharger un CSV (BOM UTF-8, séparateur `;`) aligné sur un widget |
| Préconditions | `dataset` ∈ {`overdue-files`,`workload`,`compliance-ranking`,`delay-by-type`} ; `format=csv` |
| Déroulement nominal | 1. `GET /api/dashboard/export?dataset=&format=csv&organizationId=&windowDays=&groupByTypeCode=`.<br>2. `DashboardExportService` délègue aux mêmes méthodes que les widgets (pas de recalcul parallèle).<br>3. Réponse `text/csv` + `Content-Disposition` `dashboard-{dataset}-{date}.csv`. |
| Exemple | `format=pdf` → `400 DASHBOARD_FORMAT_UNSUPPORTED` ; dataset inconnu → `DASHBOARD_DATASET_INVALID` |
| Règles (code) | RG-DSH-12, RG-DSH-13 — `DashboardController.export()`, `DashboardExportService` |

### UC-08 — Composer le tableau de bord (frontend)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur authentifié (lecture via `DASHBOARD:READ` côté API) |
| Objectif | Afficher un unique `/dashboard` adapté à la largeur de périmètre |
| Préconditions | Session JWT valide |
| Déroulement nominal | 1. Charge `summary` puis, en parallèle : `my-activity`, `delay-by-type` (30 j).<br>2. Si `scopeWidth ≠ SELF` : charge `workload` + `overdue-files`.<br>3. Si `GLOBAL` ou `REGIONAL` : charge `compliance-ranking`.<br>4. Affiche badge i18n `dashboard.scope.{scopeWidth}` + jauge dérivée du ranking (ou approximation actifs/retards). |
| Exemple | Agent : mon activité + compteurs SELF ; pas de charge ni classement |
| Règles (code) | RG-DSH-14 — `src/app/dashboard/page.tsx`, `DashboardWidgets.tsx` |

### UC-09 — Accéder à la page Rapports

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `DASHBOARD:EXPORT` |
| Objectif | Choisir dataset / fenêtre / niveau de regroupement et télécharger le CSV |
| Préconditions | Permission export ; types d'organisation chargés pour le select `groupByTypeCode` |
| Déroulement nominal | 1. Route `/rapports` (`RequireAuth permission="DASHBOARD:EXPORT"`).<br>2. Appelle `downloadDashboardExport` → `GET /api/dashboard/export`. |
| Exemple | Lien « Rapports » masqué dans `AppShell` si pas `DASHBOARD:EXPORT` |
| Règles (code) | RG-DSH-12 — `src/app/rapports/page.tsx` |

---

## 4. Règles de gestion

| ID | Règle | Composant (code) |
|---|---|---|
| RG-DSH-01 | Toute requête DSH (sauf `my-activity`) passe par `OrganizationScopeService.resolveScopeFilter` ; un `organizationId` optionnel est validé par `AccessControlService.assertCanAccessOrganization` (403 hors périmètre). | `DashboardService.resolveScope()` |
| RG-DSH-02 | `scopeWidth` est purement informatif pour l'UI ; il ne remplace jamais le filtre SQL. Calcul : `GLOBAL` si `allOrganizations` ; sinon `REGIONAL` si rôle `REGIONAL_DIRECTOR` ; sinon `SUBTREE` si > 1 org ; sinon `SELF`. | `computeScopeWidth()` |
| RG-DSH-03 | Un dossier / maillon est « en retard » ssi maillon `IN_PROGRESS`, dossier `IN_PROGRESS`, `dueAt IS NOT NULL` et `dueAt < now` — même définition que `findOverdueForDigest` (ALR-08), restreinte au scope. | `FilePassageRepository.findOverdueForScope` |
| RG-DSH-04 | « Ce mois » = du 1er jour du mois civil (`Africa/Douala`) à `ClockService.now()` ; clôturés via `closedAt`, créés via `receivedAt` (date). | `DashboardService.summary()` |
| RG-DSH-05 | `my-activity` ignore le périmètre organisationnel : uniquement `responsibleUser = acteur`. Transmissions récentes = 30 jours glissants, statuts `COMPLETED` ou `RETURNED`. | `myActivity()` |
| RG-DSH-06 | La charge filtre sur l'organisation du **responsable** du maillon (pas celle du dossier). | `findActiveForWorkload` |
| RG-DSH-07 | `limit` des top retards est borné à `[1, 100]` ; l'export overdue utilise `limit=1000` en interne. | `overdueFiles()`, `DashboardExportService` |
| RG-DSH-08 | `windowDays` doit être dans `[1, 365]` sinon `DASHBOARD_WINDOW_INVALID`. | `validateWindowDays()` |
| RG-DSH-09 | Délai réel d'un dossier clôturé = `DelaiService.countWorkingDays(début receivedAt Douala, closedAt)`. La cible affichée (DSH-06) n'est renseignée que si tous les dossiers du type partagent le même `chainTemplate`. | `actualDelayDays()`, `resolveConsensusTargetDays()` |
| RG-DSH-10 | Un dossier clôturé est conforme ssi un `chainTemplate` est lié et délai réel ≤ cible. Sans template / dates → non conforme. Cible en `WORKING_HOURS` convertie en jours via plafond(`totalDelayDays` / 9). | `isCompliant()`, `targetDelayDays()` |
| RG-DSH-11 | Le regroupement du classement remonte l'arbre parent jusqu'au premier nœud dont `organizationType.code` = `groupByTypeCode` ; type inconnu → `DASHBOARD_GROUP_TYPE_INVALID`. | `resolveGroupAncestor()`, `organizationTypeRepository.existsByCode` |
| RG-DSH-12 | L'export réutilise strictement `DashboardService` (mêmes chiffres que l'écran). Seul `format=csv` est supporté ; PDF → `DASHBOARD_FORMAT_UNSUPPORTED`. Datasets hors liste → `DASHBOARD_DATASET_INVALID`. | `DashboardExportService`, `DashboardController` |
| RG-DSH-13 | CSV : BOM UTF-8 `\uFEFF`, séparateur `;`, échappement des champs contenant `;` / `"` / saut de ligne. | `DashboardExportService.csv()` |
| RG-DSH-14 | Le frontend compose **un** dashboard : widgets équipe si `scopeWidth ≠ SELF` ; classement si `GLOBAL` ou `REGIONAL` ; jamais de branchement sur le nom du rôle. | `dashboard/page.tsx` |
| RG-DSH-15 | L'instant de référence métier est `ClockService.now()` (permet horloge de test). Les jours ouvrés / fériés restent exclusifs à `DelaiService`. | `DashboardService`, `ClockService` |
| RG-DATA-01 | Le schéma est géré exclusivement par scripts SQL manuels (`docs/sql/`) ; `spring.jpa.hibernate.ddl-auto=none`. | Règle projet transverse |

---

### Annexe A — Permissions RBAC

| Permission | Description |
|---|---|
| `DASHBOARD:READ` | Consulter summary, my-activity, workload, overdue-files, delay-by-type, compliance-ranking |
| `DASHBOARD:EXPORT` | Déclencher `GET /api/dashboard/export` et accéder à `/rapports` |

**Matrice indicative (seed `RbacDataInitializer` + script SQL) :**

| Rôle | READ | EXPORT |
|---|---|---|
| `SUPER_ADMIN`, `BUSINESS_ADMIN` | ✅ | ✅ |
| `DIRECTOR`, `SERVICE_HEAD`, `REGIONAL_DIRECTOR` | ✅ | ✅ |
| `SECRETARY_GENERAL`, `EXECUTIVE_OFFICE` | ✅ | ✅ |
| `AGENT`, `SUPPORT`, `READER` | ✅ | ❌ |

> Les agents voient de fait surtout « Mon activité » + compteurs `SELF` ; l'absence d'EXPORT n'empêche pas la lecture.

### Annexe B — Codes d'erreur i18n (`DASHBOARD_*`)

| Code | HTTP | Signification |
|---|---|---|
| `DASHBOARD_GROUP_TYPE_INVALID` | 400 | `groupByTypeCode` inconnu |
| `DASHBOARD_WINDOW_INVALID` | 400 | `windowDays` hors [1, 365] |
| `DASHBOARD_DATASET_INVALID` | 400 | Dataset d'export inconnu |
| `DASHBOARD_FORMAT_UNSUPPORTED` | 400 | Format ≠ `csv` (PDF non livré) |
| Accès org hors scope | 403 | Via `AccessControlService` / messages org (`ACCESS_DENIED_*`) — pas un code `DASHBOARD_*` dédié |

### Annexe C — Endpoints API

Base : `/api/dashboard`

| Méthode | Chemin | Permission | Params notables |
|---|---|---|---|
| `GET` | `/summary` | `READ` | `organizationId?`, `fileTypeCode?` |
| `GET` | `/my-activity` | `READ` | — |
| `GET` | `/workload` | `READ` | `organizationId?` |
| `GET` | `/overdue-files` | `READ` | `organizationId?`, `limit=10` |
| `GET` | `/delay-by-type` | `READ` | `organizationId?`, `windowDays=30` |
| `GET` | `/compliance-ranking` | `READ` | `groupByTypeCode=DIRECTORATE`, `windowDays=90` |
| `GET` | `/export` | `EXPORT` | `dataset` (requis), `format=csv`, `organizationId?`, `windowDays=90`, `groupByTypeCode=DIRECTORATE` |

### Annexe D — Colonnes CSV par dataset

| Dataset | En-têtes |
|---|---|
| `overdue-files` | `reference;objet;type;organisation;maillon;responsable;echeance;jours_de_retard` |
| `workload` | `agent;organisation;dossiers_actifs;dossiers_en_retard` |
| `compliance-ranking` | `organisation;code;dossiers_clotures;dossiers_conformes;taux_de_respect_pct` |
| `delay-by-type` | `type_de_dossier;dossiers_clotures;delai_moyen_jours;delai_cible_jours` |

### Annexe E — Pages frontend

| Route | Rôle |
|---|---|
| `/dashboard` | Accueil pilotage : welcome + badge périmètre + KPI + widgets conditionnels |
| `/rapports` | Exports CSV (`DASHBOARD:EXPORT`) |

Composants : `src/components/dashboard/DashboardWidgets.tsx` (`MyActivityWidget`, `WorkloadWidget`, `OverdueFilesWidget`, `DelayByTypeWidget`, `ComplianceRankingWidget`).

### Annexe F — Composition UI par `scopeWidth`

| Widget | `SELF` | `SUBTREE` | `REGIONAL` | `GLOBAL` |
|---|---|---|---|---|
| Compteurs (summary) | ✅ | ✅ | ✅ | ✅ |
| Mon activité | ✅ | ✅ | ✅ | ✅ |
| Délai moyen par type | ✅ (chargé) | ✅ | ✅ | ✅ |
| Charge par agent | ❌ | ✅ | ✅ | ✅ |
| Top retards | ❌ | ✅ | ✅ | ✅ |
| Classement conformité | ❌ | ❌ | ✅ | ✅ |

### Annexe G — Non livré (écart vs SPEC-DSH.md cible)

| Élément | Statut code |
|---|---|
| Export PDF | Refusé explicitement (`DASHBOARD_FORMAT_UNSUPPORTED`) |
| Rapport mensuel automatique §11.1 | Absent |
| Table / job `dashboard_daily_snapshot` | Absent |
| Heatmap DSH-04 dédiée | Absent (classement + jauge partielle) |
| Filtre mois calendaire alternatif à la fenêtre glissante | Absent (hors « ce mois » du summary) |

---

*Document généré à partir de l'analyse du code source du module (`DashboardService.java`, `DashboardExportService.java`, `DashboardController.java`, `DashboardScopeWidth.java`, DTOs `dto/response/*` DSH, `FileRepository` / `FilePassageRepository` requêtes DSH, `docs/sql/2026-07-04_dsh_rbac_permissions.sql`, frontend `dashboard/page.tsx`, `rapports/page.tsx`, `DashboardWidgets.tsx`) — à maintenir en cohérence avec toute évolution du code.*
