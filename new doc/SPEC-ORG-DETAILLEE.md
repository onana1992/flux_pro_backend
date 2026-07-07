# Spécification détaillée — Module Organisation (ORG)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique
**Module :** Référentiel organisationnel (ORG) — types d'organisation et hiérarchie des organisations
**Version :** 1.0
**Date :** 5 juillet 2026
**Source :** Rétro-documentation à partir du code source réellement implémenté (`entity/`, `repository/`, `service/`, `controller/`, `security/OrganizationScopeService.java`, `config/DataInitializer.java` du backend Spring Boot)

---

## Table des matières

1. [Objectif et périmètre](#1-objectif-et-périmètre)
2. [Modèle de données](#2-modèle-de-données)
3. [Cas d'utilisation](#3-cas-dutilisation)
4. [Règles de gestion](#4-règles-de-gestion)

---

## 1. Objectif et périmètre

### 1.1. Objectif

Le module Organisation a pour objectif de :

- **Modéliser** l'arbre hiérarchique institutionnel du ministère (ministère → directions → divisions → services, ou branche régionale DRTP), auto-référencé par un lien parent/enfant ;
- **Typologiser** les organisations via un référentiel de types **stocké en base** (et non un enum Java figé), administrable sans redéploiement ;
- **Servir de support** au module Authentification/RBAC pour l'affectation obligatoire d'un utilisateur à une organisation et pour le calcul du périmètre organisationnel (isolation des branches régionales DRTP, sous-arbres) ;
- **Administrer** le référentiel (organisations et types) via une API dédiée, réservée aux profils habilités (`SUPER_ADMIN`, `BUSINESS_ADMIN`) ;
- **Initialiser** rapidement un déploiement via import CSV en masse et via un amorçage automatique (seed) au démarrage de l'application.

### 1.2. Périmètre

**Inclus dans le module :**

| Bloc | Contenu |
|---|---|
| Référentiel des types d'organisation | CRUD complet (`organization_type`), activation/désactivation logique, suppression physique conditionnelle |
| Hiérarchie des organisations | CRUD complet (`organization`), arbre parent/enfant, activation logique, suppression physique conditionnelle |
| Consultation scopée | Arbre et détail d'une organisation filtrés par le périmètre RBAC de l'utilisateur connecté |
| Import en masse | Import CSV (upsert par `code`), résolution du type et du parent par code |
| Amorçage (seed) | Types MINTP par défaut, organisation racine, 10 DRTP, une direction, un premier compte `SUPER_ADMIN`, exécutés automatiquement et de façon idempotente au démarrage |
| Garde technique | Permissions RBAC dédiées (`ORGANIZATIONS:*`, `ORGANIZATION_TYPES:*`), `OrganizationScopeService` (isolation DRTP, sous-arbres) |

**Explicitement hors périmètre du module (non implémenté ou traité ailleurs) :**

- Détection de cycle lors du changement de parent d'une organisation (aucun contrôle actuellement) ;
- Blocage de la désactivation logique d'une organisation ayant des utilisateurs actifs ou des sous-organisations actives rattachés (seule la **suppression physique** applique ce contrôle) ;
- Réactivation d'une organisation désactivée (aucun endpoint dédié) ;
- Export CSV du référentiel ;
- Historique des affectations organisationnelles, gestion d'intérim/suppléant ;
- Sélecteur hiérarchique dédié dans le formulaire de création/modification d'un utilisateur (le frontend n'expose qu'une visualisation en graphe, pas un sélecteur d'arbre pour l'affectation) ;
- Gestion des utilisateurs elle-même (module AUTH/RBAC, qui **consomme** `organization_id` et le périmètre calculé ici, mais n'est pas décrit dans ce document).

---

## 2. Modèle de données

### 2.1. Entités

#### 2.1.1. `OrganizationType` (table `organization_type`)

Référentiel paramétrable des types d'organisation (remplace un ancien enum Java figé). N'hérite **pas** de `BaseEntity` : il porte ses propres champs techniques, avec un identifiant explicitement mappé en `CHAR(36)` (`@JdbcTypeCode(SqlTypes.VARCHAR)`), par différence avec le `BINARY(16)` utilisé par défaut pour les entités héritant de `BaseEntity` (voir RG-DATA-02).

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `id` | `UUID` (mappé `CHAR(36)`) | généré automatiquement | Identifiant technique unique |
| `createdAt` | `Instant` | date/heure courante à la création | Horodatage de création |
| `updatedAt` | `Instant` | date/heure courante | Horodatage de dernière modification |
| `code` | `String(32)` | — | Code stable unique (ex. `MINISTRY`, `REGIONAL_DIRECTORATE`) ; utilisé par l'import CSV et la logique métier ; **immuable après création** |
| `name` | `String` | — | Libellé (français) |
| `nameEn` | `String` | `null` | Libellé anglais (optionnel) |
| `description` | `String` (TEXT) | `null` | Description libre (aide contextuelle admin) |
| `color` | `String(20)` | `null` | Couleur d'affichage (badge du graphe organisationnel) |
| `sortOrder` | `int` | `0` | Ordre d'affichage dans les listes déroulantes |
| `allowsRoot` | `boolean` | `false` | `true` si une organisation de ce type peut être racine (sans parent) |
| `regionalScope` (`is_regional_scope`) | `boolean` | `false` | `true` pour les types déclenchant l'isolation régionale (DRTP) ; utilisé par `OrganizationScopeService` pour borner le périmètre d'un `REGIONAL_DIRECTOR` |
| `active` | `boolean` | `true` | Statut ; `false` = type désactivé (non assignable aux nouvelles organisations) |

#### 2.1.2. `Organization` (table `organization`)

Nœud de l'arbre hiérarchique organisationnel (siège, directions, services, DRTP…), support du périmètre de données du module AUTH/RBAC. Hérite de `BaseEntity` (`id`, `createdAt`, `updatedAt`).

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `code` | `String(32)` | — | Identifiant métier stable ; unique |
| `name` | `String` | — | Libellé complet |
| `organizationType` | FK → `OrganizationType` (`organization_type_id`) | — | Type de l'organisation ; obligatoire ; mappé en `VARCHAR` pour rester compatible avec l'identifiant `CHAR(36)` de `OrganizationType` |
| `parent` | FK → `Organization` (`parent_id`) | `null` | Organisation parente ; `null` = racine de l'arbre |
| `active` | `boolean` | `true` | Statut ; `false` = désactivée (désactivation logique, sans suppression) |

### 2.2. Relations

| Relation | Description |
|---|---|
| `organization_type` (1) ↔ (N) `organization` | Chaque organisation référence exactement un type actif via `organization_type_id` |
| `organization` (1) ↔ (N) `organization` | Auto-référence via `parent_id` ; forme l'arbre hiérarchique (racine si `parent_id IS NULL`) |
| `organization` (1) ↔ (N) `users` | Chaque utilisateur est rattaché à exactement une organisation (`users.organization_id`, `NOT NULL`) — module AUTH/RBAC, consommateur de ce module |

> Contrairement au module AUTH/RBAC, aucune table de jointure n'existe dans ce module : les relations sont des clés étrangères directes (many-to-one).

---

## 3. Cas d'utilisation

### UC-01 — Consulter l'organigramme (arbre scopé)

| Champ | Détail |
|---|---|
| Acteur | Tout utilisateur authentifié |
| Objectif | Visualiser la hiérarchie organisationnelle dans les limites de son périmètre |
| Préconditions | Authentification valide (aucune permission RBAC spécifique requise) |
| Déroulement nominal | 1. `GET /api/organizations/tree`.<br>2. Pour `SUPER_ADMIN`/`BUSINESS_ADMIN` : charge **toutes** les organisations (actives et inactives) et retourne l'arbre complet.<br>3. Pour `SECRETARY_GENERAL`/`EXECUTIVE_OFFICE` (portée globale également) : charge uniquement les organisations actives et retourne l'arbre complet actif.<br>4. Pour les autres rôles : charge les organisations actives, les filtre selon `OrganizationScopeService.canAccess()` (sous-arbre de l'organisation d'affectation + ses ancêtres, ou branche régionale homologue pour `REGIONAL_DIRECTOR`), puis reconstruit l'arbre à partir du sous-ensemble accessible. |
| Exemple | Un `REGIONAL_DIRECTOR` de DRTP-Centre ne voit que `MINTP` (ancêtre) et la branche `DRTP-C` dans l'arbre ; jamais `DRTP-LITTORAL` |
| Règles (code) | RG-ORG-01, RG-ORG-02 — `OrganizationService.getTree()`, `OrganizationScopeService.canAccess()` |

### UC-02 — Consulter le détail d'une organisation

| Champ | Détail |
|---|---|
| Acteur | Tout utilisateur authentifié |
| Objectif | Voir le détail (code, nom, type, parent) d'un nœud |
| Préconditions | Le nœud doit être dans le périmètre de l'acteur (sauf portée globale) |
| Déroulement nominal | 1. `GET /api/organizations/{id}`.<br>2. Charge l'organisation avec son type et son parent (`findByIdWithDetails`) ; `404 ORGANIZATION_NOT_FOUND` si absente.<br>3. Vérifie `OrganizationScopeService.canAccess()` ; `403 ACCESS_DENIED_ORGANIZATION` sinon.<br>4. Retourne `OrganizationDetailResponse`. |
| Exemple | Un `REGIONAL_DIRECTOR` de DRTP-Centre consultant l'`id` d'un nœud de DRTP-Littoral → `403 ACCESS_DENIED_ORGANIZATION` |
| Règles (code) | RG-ORG-01 — `OrganizationService.getById()` / `getDetailById()` |

### UC-03 — Créer une organisation

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATIONS:CREATE` (`SUPER_ADMIN`, `BUSINESS_ADMIN` dans la matrice par défaut) |
| Objectif | Ajouter un nœud (direction, division, service, DRTP…) à la hiérarchie |
| Préconditions | `code` unique ; `typeId` référence un type existant et actif ; si `parentId` est absent, le type doit avoir `allowsRoot=true` ; si `parentId` est fourni, l'organisation parente doit exister |
| Déroulement nominal | 1. `POST /api/organizations` avec `{code, name, typeId, parentId, active}`.<br>2. Vérifie l'unicité du `code` (`400 ORGANIZATION_CODE_IN_USE`).<br>3. Résout le type (`404` si absent), vérifie qu'il est actif (`400 ORGANIZATION_TYPE_INACTIVE`) et, si racine, qu'il autorise `allowsRoot` (`400 ORGANIZATION_TYPE_CANNOT_BE_ROOT`).<br>4. Résout le parent si fourni (`404 ORGANIZATION_PARENT_NOT_FOUND` sinon).<br>5. Sauvegarde et retourne `201` `OrganizationSummaryResponse`. |
| Exemple | `POST` avec `typeId` = `SERVICE` (`allowsRoot=false`) et `parentId=null` → `400 ORGANIZATION_TYPE_CANNOT_BE_ROOT` |
| Règles (code) | RG-ORG-03, RG-ORG-04, RG-ORG-05, RG-ORG-06 — `OrganizationService.create()` / `applyRequest()` |

### UC-04 — Modifier une organisation

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATIONS:UPDATE` |
| Objectif | Mettre à jour le nom, le type, le parent ou le statut d'une organisation |
| Préconditions | Mêmes contraintes que la création (code unique hors elle-même, type actif, racine autorisée) |
| Déroulement nominal | 1. `PUT /api/organizations/{id}` avec le même corps que la création.<br>2. Vérifie l'unicité du `code` si modifié.<br>3. Réapplique les mêmes contrôles de type/racine/parent que UC-03.<br>4. Sauvegarde et retourne `200` `OrganizationSummaryResponse`. |
| Exemple | Déplacer un nœud `DAG` sous l'un de ses propres descendants **n'est pas empêché** : aucune détection de cycle n'est appliquée par l'API |
| Règles (code) | RG-ORG-03, RG-ORG-04, RG-ORG-05, RG-ORG-06, RG-ORG-07 — `OrganizationService.update()` |

### UC-05 — Désactiver une organisation

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATIONS:UPDATE` |
| Objectif | Marquer une organisation comme inactive (désactivation logique) |
| Préconditions | Aucune — ni les utilisateurs actifs rattachés, ni les sous-organisations actives ne bloquent l'opération |
| Déroulement nominal | 1. `PATCH /api/organizations/{id}/deactivate`.<br>2. Passe `active = false` et sauvegarde ; aucun effet de cascade sur les descendants ou sur les comptes utilisateurs déjà rattachés. |
| Exemple | Désactiver `DAG-ARCHIVES` alors qu'un utilisateur actif y est toujours rattaché → `200`, sans contrôle ni avertissement ; l'utilisateur reste actif, seule l'organisation devient inactive (les **futures** créations/modifications d'utilisateurs sur ce nœud seront, elles, bloquées par le module AUTH/RBAC — `ORGANIZATION_INACTIVE`) |
| Règles (code) | RG-ORG-08 — `OrganizationService.deactivate()` |

### UC-06 — Supprimer définitivement une organisation

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATIONS:DELETE` |
| Objectif | Retirer physiquement un nœud du référentiel (seule suppression physique du module ; l'entité `Organization` privilégie autrement la désactivation logique) |
| Préconditions | Aucun enfant direct ; aucun utilisateur rattaché (actif ou non) |
| Déroulement nominal | 1. `DELETE /api/organizations/{id}`.<br>2. `404 ORGANIZATION_NOT_FOUND` si absente.<br>3. `409 ORGANIZATION_HAS_CHILDREN` si au moins un enfant existe.<br>4. `409 ORGANIZATION_HAS_USERS` si au moins un utilisateur y est rattaché.<br>5. Sinon, suppression physique et `204 No Content`. |
| Exemple | Tentative de suppression de `MINTP` (possède des enfants) → `409 ORGANIZATION_HAS_CHILDREN` |
| Règles (code) | RG-ORG-09 — `OrganizationService.delete()` |

### UC-07 — Importer des organisations en masse (CSV)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATIONS:IMPORT` (`SUPER_ADMIN` exclusivement dans la matrice par défaut — `BUSINESS_ADMIN` n'a pas cette permission) |
| Objectif | Peupler ou mettre à jour en masse le référentiel des organisations |
| Préconditions | Fichier CSV `;`-séparé, encodage UTF-8, en-tête `code;nom;type;parent_code;actif` |
| Déroulement nominal | 1. `POST /api/organizations/import` (multipart, champ `file`).<br>2. Pour chaque ligne : `code` obligatoire ; `type` (code) obligatoire, doit exister et être actif ; upsert de l'organisation par `code` (cache mémoire puis BDD) ; `parent_code` résolu si fourni, sinon le type doit autoriser `allowsRoot` ; `actif` par défaut `true`.<br>3. Toute erreur de ligne est collectée sans interrompre le traitement des lignes suivantes.<br>4. Retourne `{ created, updated, errors[] }`. |
| Exemple | Ligne avec `type=AGENCE` inconnu → ajoutée à `errors: ["Line 5: unknown type AGENCE"]`, les autres lignes sont traitées normalement |
| Règles (code) | RG-ORG-10, RG-ORG-11 — `OrganizationImportService.importCsv()` |

### UC-08 — Lister les types d'organisation actifs

| Champ | Détail |
|---|---|
| Acteur | Tout utilisateur authentifié |
| Objectif | Alimenter les listes déroulantes (formulaire organisation) avec les types utilisables |
| Préconditions | Aucune permission spécifique |
| Déroulement nominal | 1. `GET /api/organization-types`.<br>2. Retourne les types actifs triés par `sortOrder`, dédupliqués par `code` (protection applicative contre d'éventuels doublons résiduels en base — cf. script `2026-07-01_dedupe_organization_types.sql`). |
| Exemple | Retourne les 5 types MINTP : `MINISTRY`, `DIRECTORATE`, `DIVISION`, `SERVICE`, `REGIONAL_DIRECTORATE` |
| Règles (code) | RG-ORG-15 — `OrganizationTypeService.listActive()` |

### UC-09 — Lister tous les types d'organisation (y compris inactifs)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATION_TYPES:READ` (`SUPER_ADMIN`, `BUSINESS_ADMIN`) |
| Objectif | Administrer le référentiel complet des types, y compris désactivés |
| Préconditions | Permission `ORGANIZATION_TYPES:READ` |
| Déroulement nominal | 1. `GET /api/organization-types/all`.<br>2. Retourne la liste complète triée par `sortOrder`, sans déduplication. |
| Exemple | Un `SUPER_ADMIN` retrouve un type désactivé par erreur pour le réactiver via `PUT` |
| Règles (code) | — `OrganizationTypeService.listAll()` |

### UC-10 — Consulter un type d'organisation

| Champ | Détail |
|---|---|
| Acteur | Tout utilisateur authentifié |
| Objectif | Voir le détail d'un type (couleur, flags, description) |
| Préconditions | Aucune permission spécifique |
| Déroulement nominal | 1. `GET /api/organization-types/{id}`.<br>2. `404 ORGANIZATION_TYPE_NOT_FOUND` si absent, sinon retourne `OrganizationTypeResponse`. |
| Exemple | Consultation du type `REGIONAL_DIRECTORATE` pour vérifier son flag `isRegionalScope=true` |
| Règles (code) | — `OrganizationTypeService.getById()` |

### UC-11 — Créer un type d'organisation

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATION_TYPES:CREATE` (`SUPER_ADMIN`, `BUSINESS_ADMIN`) |
| Objectif | Étendre la typologie sans modification de code (adaptation à un autre client institutionnel) |
| Préconditions | `code` unique |
| Déroulement nominal | 1. `POST /api/organization-types` avec `{code, name, nameEn, description, color, sortOrder, allowsRoot, isRegionalScope, active}`.<br>2. Vérifie l'unicité du `code` (`400 ORGANIZATION_TYPE_CODE_IN_USE`).<br>3. Crée le type et retourne `201`. |
| Exemple | Création d'un type `AGENCY` pour un futur déploiement non-MINTP, disponible immédiatement dans les listes de sélection |
| Règles (code) | RG-ORG-12 — `OrganizationTypeService.create()` |

### UC-12 — Modifier un type d'organisation

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATION_TYPES:UPDATE` |
| Objectif | Ajuster le libellé, la couleur, l'ordre d'affichage ou les indicateurs (`allowsRoot`, `isRegionalScope`) d'un type existant |
| Préconditions | Le `code` ne peut pas être modifié |
| Déroulement nominal | 1. `PUT /api/organization-types/{id}`.<br>2. Si `request.code` diffère du code actuel → `400 ORGANIZATION_TYPE_CODE_IMMUTABLE`.<br>3. Sinon, met à jour tous les autres champs et retourne `200`. |
| Exemple | Tentative de renommer le code `REGIONAL_DIRECTORATE` en `DRTP_NEW` → `400 ORGANIZATION_TYPE_CODE_IMMUTABLE` |
| Règles (code) | RG-ORG-12 — `OrganizationTypeService.update()` |

### UC-13 — Désactiver un type d'organisation

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATION_TYPES:UPDATE` |
| Objectif | Retirer un type de la circulation sans le supprimer |
| Préconditions | Aucune organisation **active** ne doit utiliser ce type |
| Déroulement nominal | 1. `PATCH /api/organization-types/{id}/deactivate`.<br>2. Vérifie `existsByOrganizationTypeIdAndActiveTrue` ; `409 ORGANIZATION_TYPE_IN_USE_ACTIVE` si au moins une organisation active l'utilise.<br>3. Sinon, passe `active = false`. |
| Exemple | Tentative de désactivation de `DIRECTORATE` alors que `DAG` (active) l'utilise → `409 ORGANIZATION_TYPE_IN_USE_ACTIVE` |
| Règles (code) | RG-ORG-13 — `OrganizationTypeService.deactivate()` |

### UC-14 — Supprimer un type d'organisation

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ORGANIZATION_TYPES:DELETE` |
| Objectif | Retirer physiquement un type inutilisé du référentiel |
| Préconditions | Aucune organisation, **active ou non**, ne doit référencer ce type |
| Déroulement nominal | 1. `DELETE /api/organization-types/{id}`.<br>2. Vérifie `existsByOrganizationTypeId` (toutes organisations, actives ou non) ; `409 ORGANIZATION_TYPE_IN_USE` si utilisé.<br>3. Sinon, suppression physique et `204`. |
| Exemple | Suppression du type `SERVICE` refusée alors qu'une organisation **inactive** l'utilise encore → `409` (contrainte plus stricte que la désactivation, qui ne considère que les organisations actives) |
| Règles (code) | RG-ORG-13 — `OrganizationTypeService.delete()` |

### UC-15 — Amorçage du référentiel organisationnel au démarrage

| Champ | Détail |
|---|---|
| Acteur | Système (`DataInitializer`, `CommandLineRunner` exécuté à chaque démarrage de l'application) |
| Objectif | Garantir la présence d'un référentiel minimal exploitable sans intervention manuelle ni script SQL de données |
| Préconditions | Aucune — le traitement est **idempotent** (ne recrée rien si déjà présent) |
| Déroulement nominal | 1. Amorce les 5 types MINTP à UUID fixes (`MINISTRY`, `DIRECTORATE`, `DIVISION`, `SERVICE`, `REGIONAL_DIRECTORATE`) si absents par `code` ou par `id`.<br>2. Crée l'organisation racine `MINTP` (type `MINISTRY`) si absente.<br>3. Crée les 10 DRTP (type `REGIONAL_DIRECTORATE`, parent `MINTP`) si absentes.<br>4. Crée la direction `DSI` (type `DIRECTORATE`, parent `MINTP`) si absente.<br>5. Crée un premier compte `SUPER_ADMIN` rattaché à `DSI` si absent (détail du compte hors périmètre — voir module AUTH/RBAC). |
| Exemple | Sur une base neuve, le démarrage de l'application crée automatiquement 5 types, `MINTP`, 10 DRTP, `DSI` et 1 compte `SUPER_ADMIN`, sans exécution manuelle de script de données |
| Règles (code) | RG-ORG-14, RG-ORG-17 — `DataInitializer.run()` |

---

## 4. Règles de gestion

| ID | Règle | Composant (code) |
|---|---|---|
| RG-ORG-01 | Toute lecture (arbre ou détail) d'une organisation applique le périmètre organisationnel de l'acteur : portée globale pour `SUPER_ADMIN`/`BUSINESS_ADMIN`/`SECRETARY_GENERAL`/`EXECUTIVE_OFFICE`, branche régionale homologue (même racine `is_regional_scope=true`) pour `REGIONAL_DIRECTOR`, sous-arbre + ancêtres de l'organisation d'affectation pour les autres rôles. | `OrganizationScopeService.canAccess()` / `resolveScopeFilter()` |
| RG-ORG-02 | `SUPER_ADMIN` et `BUSINESS_ADMIN` voient l'arbre complet y compris les organisations inactives ; `SECRETARY_GENERAL` et `EXECUTIVE_OFFICE` (portée globale également) ne voient que les organisations actives. | `OrganizationService.getTree()` |
| RG-ORG-03 | Le `code` d'une organisation est unique dans tout le référentiel (vérifié à la création et à la modification, hors comparaison avec elle-même). | `OrganizationService.create()/update()`, contrainte BDD unique |
| RG-ORG-04 | Le type référencé par une organisation doit exister et être actif au moment de la création ou de la modification (`400 ORGANIZATION_TYPE_INACTIVE` sinon). | `OrganizationService.applyRequest()` |
| RG-ORG-05 | Une organisation sans parent (racine) ne peut être créée ou modifiée qu'avec un type dont `allowsRoot=true` (`400 ORGANIZATION_TYPE_CANNOT_BE_ROOT` sinon). | `OrganizationService.applyRequest()` |
| RG-ORG-06 | La création et la modification d'une organisation **ne vérifient pas** le périmètre organisationnel de l'acteur : un titulaire de `ORGANIZATIONS:CREATE`/`UPDATE` peut agir sur n'importe quel nœud de l'arbre, même hors de sa propre branche (à la différence du module AUTH/RBAC, qui applique `AccessControlService.assertOrganizationWritable()` sur les utilisateurs). | `OrganizationService.create()/update()` (absence de contrôle constatée) |
| RG-ORG-07 | Aucune détection de cycle n'est appliquée lors du changement de parent d'une organisation ; il est possible en l'état de créer une boucle dans la hiérarchie via l'API d'écriture. | `OrganizationService.update()/applyRequest()` (absence de contrôle constatée) |
| RG-ORG-08 | La désactivation d'une organisation (`active=false`) est une opération logique sans contrôle : elle n'est bloquée ni par la présence d'utilisateurs actifs, ni par des sous-organisations actives, et n'a aucun effet de cascade sur les descendants ou les comptes déjà rattachés. | `OrganizationService.deactivate()` |
| RG-ORG-09 | La suppression physique (`DELETE`) d'une organisation est refusée si elle possède au moins un enfant direct (`409 ORGANIZATION_HAS_CHILDREN`) ou au moins un utilisateur rattaché, actif ou non (`409 ORGANIZATION_HAS_USERS`). | `OrganizationService.delete()` |
| RG-ORG-10 | L'import CSV traite chaque ligne indépendamment (upsert par `code`) : une erreur sur une ligne (code/type manquant, type inconnu ou inactif, parent introuvable, parent requis pour un type non-racine) est ajoutée au rapport `errors[]` sans annuler le traitement des lignes suivantes ni des lignes déjà traitées. | `OrganizationImportService.importCsv()` |
| RG-ORG-11 | Une organisation importée sans `parent_code` doit référencer un type avec `allowsRoot=true`, sinon la ligne est rejetée. | `OrganizationImportService.importCsv()` |
| RG-ORG-12 | Le `code` d'un type d'organisation est unique et **immuable** après création ; toute tentative de modification du `code` via `PUT` est refusée (`400 ORGANIZATION_TYPE_CODE_IMMUTABLE`). | `OrganizationTypeService.create()/update()` |
| RG-ORG-13 | Un type d'organisation ne peut être désactivé que si aucune organisation **active** ne l'utilise (`409 ORGANIZATION_TYPE_IN_USE_ACTIVE`) ; il ne peut être supprimé physiquement que si aucune organisation, **active ou non**, ne l'utilise (`409 ORGANIZATION_TYPE_IN_USE`) — la contrainte de suppression est strictement plus large que celle de désactivation. | `OrganizationTypeService.deactivate()/delete()` |
| RG-ORG-14 | Un type marqué `is_regional_scope=true` détermine la racine de l'isolation régionale (DRTP) : le périmètre d'un `REGIONAL_DIRECTOR` est borné à la branche dont le premier ancêtre (ou lui-même) porte un type à `is_regional_scope=true` ; aucun accès inter-DRTP n'est possible. | `OrganizationScopeService.findRegionalRootCode()` |
| RG-ORG-15 | La liste des types actifs exposée par `GET /api/organization-types` est dédupliquée par `code` (protection applicative contre d'éventuels doublons résiduels en base) ; `GET /api/organization-types/all` ne l'est pas. | `OrganizationTypeService.listActive()/listAll()` |
| RG-ORG-16 | Les endpoints de lecture (arbre, détail d'une organisation, détail/liste active des types) ne requièrent qu'une authentification valide, sans permission RBAC dédiée ; seules les opérations d'écriture (`create`/`update`/`deactivate`/`delete`/`import`) sont protégées par une permission `RESOURCE:ACTION` explicite via `@RequiresPermission`. | Absence de `@RequiresPermission` sur les endpoints `GET` (`OrganizationController`, `OrganizationTypeController`) |
| RG-ORG-17 | Au démarrage de l'application, un jeu minimal de données est amorcé de façon idempotente (5 types MINTP à UUID fixes, organisation racine `MINTP`, 10 DRTP, direction `DSI`, premier compte `SUPER_ADMIN`) afin que l'application soit exploitable sans script SQL de données initial. | `DataInitializer.run()` |
| RG-DATA-01 | Le schéma de base de données est géré exclusivement par scripts SQL manuels (`docs/sql/`) ; `spring.jpa.hibernate.ddl-auto` reste à `none` en toutes circonstances. La migration de l'ancien champ enum `organization.type` vers la FK `organization_type_id` a été livrée via `docs/sql/2026-07-01_organization_types_table.sql`. | Règle projet transverse |
| RG-DATA-02 | L'identifiant technique de `OrganizationType` est stocké en `CHAR(36)` (mapping `VARCHAR` explicite via `@JdbcTypeCode`), par différence avec le `BINARY(16)` utilisé par défaut pour les entités héritant de `BaseEntity` ; la FK `organization.organization_type_id` reprend le même mapping `VARCHAR` pour rester compatible. | `OrganizationType.java`, `Organization.java` |

---

*Document généré à partir de l'analyse du code source du module (`entity/`, `repository/`, `service/OrganizationService.java`, `service/OrganizationTypeService.java`, `service/OrganizationImportService.java`, `controller/OrganizationController.java`, `controller/OrganizationTypeController.java`, `security/OrganizationScopeService.java`, `config/DataInitializer.java`) — à maintenir en cohérence avec toute évolution du code.*
