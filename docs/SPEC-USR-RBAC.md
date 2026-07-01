# Spécification — Module gestion des utilisateurs & contrôle d'accès (USR / RBAC)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique  
**Cas pilote :** Ministère des Travaux Publics du Cameroun (MINTP)  
**Module :** USR (utilisateurs) + RBAC (contrôle d'accès basé sur les rôles)  
**Version :** 1.0  
**Date :** 30 juin 2026  
**Statut :** Spécification cible — **non implémenté de bout en bout**

**Références :**
- [Cahier des charges](./CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md) — §5 (rôles), §7.1, §7.2
- [Sprint 1 — Auth & Org](./SPRINT-1-SPEC-AUTH-ORG.md) — sections 6, 7, 8 (base historique)
- [Données pilote](./data/README-AGENTS-MINTP.md)
- Règle projet : `spring.jpa.hibernate.ddl-auto=none` — toute évolution schéma via scripts `docs/sql/`

---

## Table des matières

1. [Contexte et objectifs](#1-contexte-et-objectifs)
2. [État des lieux](#2-état-des-lieux)
3. [Périmètre fonctionnel](#3-périmètre-fonctionnel)
4. [Rôles et hiérarchie des permissions](#4-rôles-et-hiérarchie-des-permissions)
5. [Isolation organisationnelle](#5-isolation-organisationnelle)
6. [Modèle de données](#6-modèle-de-données)
7. [API REST](#7-api-rest)
8. [Règles métier](#8-règles-métier)
9. [Sécurité et authentification](#9-sécurité-et-authentification)
10. [Frontend](#10-frontend)
11. [Import CSV](#11-import-csv)
12. [User stories et critères d'acceptation](#12-user-stories-et-critères-dacceptation)
13. [Plan de tests](#13-plan-de-tests)
14. [Plan d'implémentation](#14-plan-dimplémentation)
15. [Hors périmètre](#15-hors-périmètre)
16. [Definition of Done](#16-definition-of-done)

---

## 1. Contexte et objectifs

### 1.1 Problème

FluxPro doit gérer ~85 agents pilotes répartis sur l'organigramme MINTP (siège + 10 DRTP). Chaque agent :

- s'authentifie avec un compte institutionnel ;
- possède un **rôle RBAC** déterminant ses actions ;
- est rattaché à une **organisation** (direction, service, DRTP…) qui définit son **périmètre de données**.

Sans un module USR/RBAC complet, les modules dossiers (Sprint 2+), alertes et tableaux de bord ne peuvent pas appliquer de filtrage fiable ni d'autorisation fine.

### 1.2 Objectifs du module

| Objectif | Description |
|----------|-------------|
| **Identité** | CRUD utilisateurs, affectation organisation/rôle, cycle de vie du compte |
| **Autorisation** | Matrice rôle × ressource, appliquée **à la fois** en annotation et en service |
| **Périmètre** | Isolation DRTP et sous-arbres organisationnels |
| **Auditabilité** | Journal des connexions consultable par `SUPER_ADMIN` |
| **Exploitabilité** | Import CSV pilote, reset mot de passe, verrouillage compte |

### 1.3 Principes d'architecture

- API REST JSON sous `/api`, erreurs **RFC 7807** (`ProblemDetail`)
- Authentification **stateless JWT** (access 8 h + refresh 7 j)
- Autorisation **à deux niveaux** :
  1. **Méthode** — `@PreAuthorize` sur les contrôleurs (garde grossière par rôle)
  2. **Service** — `OrganizationScopeService` + `AccessControlService` (garde fine par périmètre)
- Schéma BDD géré **manuellement** (`ddl-auto=none`) — scripts dans `docs/sql/`
- Nommage **anglais** pour enums, endpoints et champs API (aligné code actuel)

---

## 2. État des lieux

> Ce tableau décrit l'écart entre le code actuel et la spécification cible. Le module est considéré **non livré** tant que les éléments « Manquant » ne sont pas résolus.

### 2.1 Backend

| Composant | Existant | Manquant / incomplet |
|-----------|----------|----------------------|
| Entité `User` | Table `users`, champs principaux | Script SQL `docs/sql/` versionné ; contraintes CHECK sur `role` |
| `UserController` / `UserService` | Endpoints CRUD, import, reset MDP | **Filtrage périmètre** sur search/get/create/update ; validation rôle assignable ; réactivation ; déverrouillage manuel |
| `OrganizationScopeService` | Logique DRTP + sous-arbre | **Non branché** sur `UserService` ; pas de tests d'intégration |
| `AuthService` | Login, refresh, logout, change-password, verrouillage 5× | Blocage compte verrouillé sur JWT existant ; `mustChangePassword` forcé ; refresh vérifie `active` |
| `LoginAuditController` | Liste paginée filtrable | Export CSV (optionnel phase 2) |
| `SecurityConfig` | JWT filter, routes publiques | Rate limiting login ; politique CORS prod |
| Packages legacy | `utilisateur/`, `organisation/`, `auth/` (doublons FR) | **Suppression** ou fusion — source de conflits de beans |
| Tests | `PasswordValidatorTest`, `contextLoads` | IT lockout, IT isolation DRTP, IT RBAC utilisateurs |

### 2.2 Frontend

| Écran | Existant | Manquant |
|-------|----------|----------|
| `/login` | Connexion fonctionnelle | Redirection si `mustChangePassword` |
| `/admin/users` | Liste, filtres rôle/recherche, import, désactivation | **Création**, **édition**, reset MDP, filtre organisation, périmètre `DIRECTOR` |
| `/admin/org` | Arbre lecture + import | CRUD organisations (créer/modifier/désactiver) |
| `/admin/audit` | Liste paginée | Filtres email/succès/dates |
| Profil | Affichage menu | **Changement mot de passe** ; déconnexion |
| Protection routes | `RequireAuth` client | Middleware Next.js (optionnel) |

### 2.3 Base de données

- `spring.jpa.hibernate.ddl-auto=none` — **aucune migration automatique**
- Schéma cible documenté en §6 ; script initial à créer : `docs/sql/V1__create_users_organizations_auth.sql`
- Données pilote : `docs/data/agents-mintp.csv`, `docs/data/organisations-mintp.csv` (enums anglais)

---

## 3. Périmètre fonctionnel

### 3.1 Inclus (Must)

| ID | Fonctionnalité |
|----|----------------|
| USR-01 | CRUD utilisateur (création avec mot de passe temporaire) |
| USR-02 | Affectation unique à une organisation active |
| USR-03 | Attribution d'un des 10 rôles RBAC |
| USR-04 | Désactivation / réactivation logique (`active`) |
| USR-05 | Reset mot de passe par `SUPER_ADMIN` |
| USR-06 | Import CSV agents pilote (~85 lignes) |
| USR-07 | Liste paginée avec filtres (organisation, rôle, recherche texte) |
| USR-08 | Profil courant (`GET /api/users/me`) |
| RBAC-01 | Matrice permissions Sprint 1 (§4.3) |
| RBAC-02 | Isolation périmètre organisationnel (§5) |
| RBAC-03 | Garde `@PreAuthorize` + contrôle service |
| RBAC-04 | Journal connexions (`login_audit`) — lecture `SUPER_ADMIN` |
| AUTH-01 | Connexion email/MDP, refresh, logout |
| AUTH-02 | Politique MDP (8 car., majuscule, chiffre, spécial) |
| AUTH-03 | Verrouillage après 5 échecs (30 min) |
| AUTH-04 | Session access token 8 h |
| AUTH-05 | Changement mot de passe (utilisateur authentifié) |
| AUTH-06 | `mustChangePassword` — blocage fonctionnel jusqu'au changement |

### 3.2 Inclus (Should — phase 1 bis)

| ID | Fonctionnalité |
|----|----------------|
| USR-09 | Déverrouillage manuel compte par `SUPER_ADMIN` |
| USR-10 | Invalidation refresh tokens à la désactivation |
| FE-01 | Formulaires création/édition utilisateur |
| FE-02 | Modal changement mot de passe obligatoire |

### 3.3 Exclu (phase ultérieure)

- 2FA email (AUTH-05 CDC)
- LDAP/AD MINTP
- Gestion suppléant (`substitute_id`) — ORG-04
- Permissions dossiers / chaînes / alertes (Sprint 2+)
- Excel import (`.xlsx`)

---

## 4. Rôles et hiérarchie des permissions

### 4.1 Énumération `UserRole`

| Code | Libellé métier (FR) | Description |
|------|---------------------|-------------|
| `SUPER_ADMIN` | Super administrateur | DSI — configuration système, tous périmètres |
| `BUSINESS_ADMIN` | Administrateur métier | Référent métier — gestion org/users dans son périmètre |
| `EXECUTIVE_OFFICE` | Cabinet | Vue globale ministère |
| `SECRETARY_GENERAL` | Secrétaire général | Vue transversale, escalades SG |
| `DIRECTOR` | Directeur | Périmètre direction + descendants |
| `SERVICE_HEAD` | Chef de service | Périmètre service/division + descendants |
| `AGENT` | Agent | Opérations dossiers (Sprint 2+) |
| `SUPPORT` | Cadre d'appui | Enregistrement, transmission |
| `READER` | Lecteur | Consultation seule |
| `REGIONAL_DIRECTOR` | Directeur DRTP | Périmètre DRTP régionale uniquement |

### 4.2 Ressources et actions (Sprint 1)

| Ressource | Actions |
|-----------|---------|
| `auth` | `login`, `logout`, `refresh`, `change-password` |
| `organization` | `read`, `create`, `update`, `deactivate`, `import` |
| `user` | `read`, `create`, `update`, `deactivate`, `reactivate`, `reset-password`, `import`, `unlock` |
| `login_audit` | `read` |

### 4.3 Matrice rôle × permission

| Permission | SUPER_ADMIN | BUSINESS_ADMIN | DIRECTOR | SERVICE_HEAD | REGIONAL_DIRECTOR | SECRETARY_GENERAL | EXECUTIVE_OFFICE | AGENT / SUPPORT | READER |
|------------|:-----------:|:--------------:|:--------:|:------------:|:-----------------:|:-----------------:|:----------------:|:---------------:|:------:|
| `organization:read` | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| `organization:write` | ✓ | ✓* | — | — | — | — | — | — | — |
| `organization:import` | ✓ | — | — | — | — | — | — | — | — |
| `user:read` | ✓ | ✓* | ✓* | ✓* | ✓* | — | — | — | — |
| `user:write` | ✓ | ✓* | — | — | — | — | — | — | — |
| `user:import` | ✓ | — | — | — | — | — | — | — | — |
| `user:reset-password` | ✓ | — | — | — | — | — | — | — | — |
| `user:unlock` | ✓ | — | — | — | — | — | — | — | — |
| `login_audit:read` | ✓ | — | — | — | — | — | — | — | — |
| `admin:ui` | ✓ | ✓ | — | — | — | — | — | — | — |

\* Limité au **périmètre organisationnel** (§5). Un `BUSINESS_ADMIN` ne peut pas gérer un `SUPER_ADMIN`.

### 4.4 Règles d'attribution de rôle (USR-R03 étendu)

| Acteur | Peut attribuer |
|--------|----------------|
| `SUPER_ADMIN` | Tous les rôles |
| `BUSINESS_ADMIN` | Tous sauf `SUPER_ADMIN` ; uniquement dans son périmètre |
| Autres | Aucun |

### 4.5 Garde `@PreAuthorize` (contrôleurs)

Mapping minimal côté contrôleur — **ne remplace pas** le contrôle de périmètre en service.

**`UserController` (`/api/users`)**

| Endpoint | `@PreAuthorize` |
|----------|-----------------|
| `GET /me` | Authentifié |
| `GET /`, `GET /{id}` | `SUPER_ADMIN`, `BUSINESS_ADMIN`, `DIRECTOR`, `SERVICE_HEAD`, `REGIONAL_DIRECTOR` |
| `POST /`, `PUT /{id}` | `SUPER_ADMIN`, `BUSINESS_ADMIN` |
| `PATCH /{id}/deactivate`, `PATCH /{id}/activate` | `SUPER_ADMIN`, `BUSINESS_ADMIN` |
| `POST /{id}/reset-password`, `POST /import`, `PATCH /{id}/unlock` | `SUPER_ADMIN` |

**`LoginAuditController`** — classe : `hasRole('SUPER_ADMIN')`

### 4.6 Service `AccessControlService` (à créer)

Centralise les décisions RBAC + périmètre :

```java
public interface AccessControlService {
    void assertCanReadUser(SecurityUser actor, UUID targetUserId);
    void assertCanWriteUser(SecurityUser actor, UserRequest request, UUID targetUserId);
    void assertCanAssignRole(SecurityUser actor, UserRole roleToAssign);
    Set<UUID> resolveReadableOrganizationIds(SecurityUser actor);
}
```

Implémentation : délègue à `OrganizationScopeService.canAccess()` et applique les règles §4.3–4.4.

---

## 5. Isolation organisationnelle

### 5.1 Principe

Un utilisateur ne peut lire ou modifier des ressources (utilisateurs, organisations, et plus tard dossiers) que si l'organisation cible est dans son **périmètre**.

### 5.2 Périmètre par rôle

| Rôle | Périmètre |
|------|-----------|
| `SUPER_ADMIN`, `SECRETARY_GENERAL`, `EXECUTIVE_OFFICE` | Tout le ministère |
| `REGIONAL_DIRECTOR` | Nœud `REGIONAL_DIRECTORATE` racine de sa branche DRTP uniquement |
| `DIRECTOR` | Organisation d'affectation + tous les descendants |
| `SERVICE_HEAD`, `AGENT`, `SUPPORT`, `READER` | Organisation d'affectation + descendants |
| `BUSINESS_ADMIN` | Organisation d'affectation + descendants (extensible en phase 2) |

### 5.3 Algorithme `OrganizationScopeService`

Déjà esquissé dans `security/OrganizationScopeService.java` — à **réutiliser** pour :

- filtrage `GET /api/users` (clause `organization_id IN (:scope)`)
- validation `POST/PUT /api/users` (`organizationId` ∈ périmètre)
- `GET /api/users/{id}` (403 si hors périmètre)
- `GET /api/organizations/tree` (déjà partiellement appliqué)

### 5.4 Test d'acceptation clé (préparation Sprint 2)

> Un agent `REGIONAL_DIRECTOR` affecté à `DRTP-C` reçoit **403** sur toute ressource rattachée à `DRTP-LITTORAL`.

---

## 6. Modèle de données

### 6.1 Tables

> Script à produire : `docs/sql/V1__create_users_organizations_auth.sql`  
> Exécution **manuelle** sur MySQL avant démarrage applicatif.

#### Table `organization`

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `CHAR(36)` | PK |
| `code` | `VARCHAR(32)` | NOT NULL, UNIQUE |
| `name` | `VARCHAR(255)` | NOT NULL |
| `type` | `VARCHAR(30)` | NOT NULL — voir `OrganizationType` |
| `parent_id` | `CHAR(36)` | FK → `organization(id)` |
| `active` | `BOOLEAN` | DEFAULT TRUE |
| `created_at` | `DATETIME(6)` | |
| `updated_at` | `DATETIME(6)` | |

**`OrganizationType` :** `MINISTRY`, `DIRECTORATE`, `DIVISION`, `SERVICE`, `REGIONAL_DIRECTORATE`

#### Table `users`

| Colonne | Type | Contraintes |
|---------|------|-------------|
| `id` | `CHAR(36)` | PK |
| `staff_number` | `VARCHAR(32)` | NOT NULL, UNIQUE |
| `email` | `VARCHAR(255)` | NOT NULL, UNIQUE |
| `last_name` | `VARCHAR(100)` | NOT NULL |
| `first_name` | `VARCHAR(100)` | NOT NULL |
| `phone` | `VARCHAR(20)` | NULL |
| `role` | `VARCHAR(30)` | NOT NULL — voir `UserRole` |
| `organization_id` | `CHAR(36)` | NOT NULL, FK → `organization(id)` |
| `job_title` | `VARCHAR(255)` | NULL |
| `password_hash` | `VARCHAR(255)` | NOT NULL |
| `must_change_password` | `BOOLEAN` | DEFAULT TRUE |
| `failed_login_attempts` | `INT` | DEFAULT 0 |
| `locked_until` | `DATETIME(6)` | NULL |
| `active` | `BOOLEAN` | DEFAULT TRUE |
| `substitute_id` | `CHAR(36)` | NULL, FK → `users(id)` — réservé |
| `created_at` | `DATETIME(6)` | |
| `updated_at` | `DATETIME(6)` | |

#### Table `login_audit`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | `CHAR(36)` | PK |
| `user_id` | `CHAR(36)` | FK nullable (échec login inconnu) |
| `email` | `VARCHAR(255)` | Email tenté |
| `success` | `BOOLEAN` | |
| `ip_address` | `VARCHAR(45)` | |
| `user_agent` | `TEXT` | |
| `failure_reason` | `VARCHAR(50)` | `BAD_CREDENTIALS`, `LOCKED`, `INACTIVE` |
| `created_at` | `DATETIME(6)` | |

#### Table `refresh_token`

| Colonne | Type | Description |
|---------|------|-------------|
| `id` | `CHAR(36)` | PK |
| `user_id` | `CHAR(36)` | FK → `users(id)` |
| `token` | `VARCHAR(512)` | UNIQUE |
| `expires_at` | `DATETIME(6)` | |
| `revoked` | `BOOLEAN` | DEFAULT FALSE |

### 6.2 Index recommandés

```sql
CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_organization_parent ON organization(parent_id);
CREATE INDEX idx_login_audit_created ON login_audit(created_at);
```

---

## 7. API REST

**Base :** `/api`  
**Auth :** `Authorization: Bearer <accessToken>`  
**Pagination :** `page`, `size` (défaut 20)  
**Erreurs :** `application/problem+json`

### 7.1 Utilisateurs — `/api/users`

#### `GET /api/users/me`

Profil de l'utilisateur connecté.

**Response 200 :**

```json
{
  "id": "uuid",
  "email": "c.essomba@mintp.cm",
  "lastName": "ESSOMBA",
  "firstName": "Claude",
  "role": "BUSINESS_ADMIN",
  "organization": { "id": "uuid", "code": "DSI", "name": "Direction des Systèmes d'Information" },
  "mustChangePassword": false
}
```

#### `GET /api/users`

**Query :** `organizationId`, `role`, `search` (matricule, nom, email), `page`, `size`

- Résultats **filtrés par périmètre** de l'appelant
- `SUPER_ADMIN` : tous les utilisateurs (filtres optionnels)

#### `GET /api/users/{id}`

- **403** si utilisateur hors périmètre

#### `POST /api/users`

**Request :**

```json
{
  "staffNumber": "MAT-2026-0100",
  "email": "nouveau.agent@mintp.cm",
  "lastName": "DUPONT",
  "firstName": "Jean",
  "phone": "+237 677 00 00 00",
  "role": "AGENT",
  "organizationId": "uuid",
  "jobTitle": "Agent de saisie",
  "active": true
}
```

**Response 201 :**

```json
{
  "user": { "...": "UserResponse" },
  "temporaryPassword": "Mintp@x7Kp2m"
}
```

#### `PUT /api/users/{id}`

Mise à jour — **interdit** de promouvoir en `SUPER_ADMIN` sauf par un `SUPER_ADMIN`.

#### `PATCH /api/users/{id}/deactivate`

`active = false` + révocation des refresh tokens.

#### `PATCH /api/users/{id}/activate`

`active = true` (réservé `SUPER_ADMIN`, `BUSINESS_ADMIN` dans périmètre).

#### `PATCH /api/users/{id}/unlock`

Remet `failedLoginAttempts = 0`, `lockedUntil = null` — `SUPER_ADMIN` uniquement.

#### `POST /api/users/{id}/reset-password`

**Response 200 :** `{ "temporaryPassword": "Mintp@..." }`

#### `POST /api/users/import`

`multipart/form-data`, champ `file` — CSV point-virgule (voir §11).

### 7.2 Authentification — `/api/auth`

| Méthode | Path | Auth | Description |
|---------|------|------|-------------|
| POST | `/api/auth/login` | Public | Retourne tokens + profil |
| POST | `/api/auth/refresh` | Public (refresh token) | Vérifie `active`, non révoqué |
| POST | `/api/auth/logout` | Bearer | Révoque refresh token |
| POST | `/api/auth/change-password` | Bearer | `{ currentPassword, newPassword }` — lève `mustChangePassword` |

**Codes erreur login :**

| HTTP | `failure_reason` | Condition |
|------|------------------|-----------|
| 401 | `BAD_CREDENTIALS` | Email/MDP incorrect |
| 423 | `LOCKED` | `locked_until > now()` |
| 403 | `INACTIVE` | `active = false` |

### 7.3 Journal connexions — `/api/admin/login-audit`

`GET` — query : `email`, `success`, `from`, `to`, `page`, `size` — **`SUPER_ADMIN` uniquement**.

---

## 8. Règles métier

| ID | Règle |
|----|-------|
| USR-R01 | `staff_number` et `email` uniques (insensibles à la casse pour email) |
| USR-R02 | Un utilisateur actif appartient à **exactement une** organisation **active** |
| USR-R03 | Seul `SUPER_ADMIN` peut créer/modifier un utilisateur avec rôle `SUPER_ADMIN` |
| USR-R04 | `BUSINESS_ADMIN` gère les users de son périmètre, jamais les `SUPER_ADMIN` |
| USR-R05 | Désactivation → `active = false` ; refresh tokens révoqués |
| USR-R06 | Reset MDP → mot de passe temporaire + `mustChangePassword = true` |
| USR-R07 | Organisation inactive → refus création utilisateur rattaché |
| USR-R08 | `DIRECTOR` / `SERVICE_HEAD` peuvent **lire** les users de leur périmètre, pas les modifier |
| USR-R09 | Import CSV : upsert par `email` ; erreurs ligne par ligne sans rollback global |
| AUTH-R01 | MDP : min 8 car., 1 majuscule, 1 chiffre, 1 caractère spécial |
| AUTH-R02 | 5 échecs consécutifs → verrouillage 30 minutes |
| AUTH-R03 | Si `mustChangePassword = true`, toutes les routes sauf `change-password` et `logout` → **403** |
| AUTH-R04 | Compte verrouillé → refus login et refus refresh |

---

## 9. Sécurité et authentification

### 9.1 JWT — claims access token

| Claim | Description |
|-------|-------------|
| `sub` | `user.id` |
| `email` | Email |
| `role` | `UserRole.name()` |
| `organizationId` | UUID organisation |
| `organizationCode` | Code org (affichage) |
| `type` | `access` |

### 9.2 Chaîne de filtres

```
Requête → JwtAuthenticationFilter → @PreAuthorize → Controller → AccessControlService → Service → Repository
```

### 9.3 Bonnes pratiques

| Sujet | Décision |
|-------|----------|
| Hachage MDP | BCrypt force 12 |
| Stockage tokens FE | `localStorage` (MVP) ; migration `httpOnly` cookies en phase 2 |
| CORS | Origines configurées `fluxpro.cors.allowed-origins` |
| Rate limiting login | 10 req/min/IP (à implémenter — Bucket4j ou filtre) |
| Swagger | `/swagger-ui.html` — désactivable en prod |

---

## 10. Frontend

### 10.1 Routes

| Route | Garde | Rôles autorisés |
|-------|-------|-----------------|
| `/login` | Public | — |
| `/dashboard` | Authentifié | Tous |
| `/change-password` | Authentifié + `mustChangePassword` | Tous |
| `/admin/users` | Admin | `SUPER_ADMIN`, `BUSINESS_ADMIN` |
| `/admin/users/new` | Admin write | `SUPER_ADMIN`, `BUSINESS_ADMIN` |
| `/admin/users/{id}/edit` | Admin write | `SUPER_ADMIN`, `BUSINESS_ADMIN` |
| `/admin/org` | Admin | `SUPER_ADMIN`, `BUSINESS_ADMIN` |
| `/admin/audit` | Super admin | `SUPER_ADMIN` |

### 10.2 Écran gestion utilisateurs (`/admin/users`)

**Liste :**
- Colonnes : matricule, nom, prénom, email, rôle (badge), organisation, statut, actions
- Filtres : recherche texte, rôle, organisation (arbre ou select)
- Actions : voir, modifier, désactiver, reset MDP (`SUPER_ADMIN`)
- Bouton : importer CSV, créer utilisateur

**Formulaire création/édition :**
- Champs alignés `UserRequest`
- Organisation : select limité au périmètre de l'admin
- Rôle : select — options filtrées selon §4.4
- À la création : affichage **une fois** du mot de passe temporaire (copier)

### 10.3 Flux `mustChangePassword`

```mermaid
flowchart TD
    A[Login réussi] --> B{mustChangePassword?}
    B -->|oui| C[/change-password]
    B -->|non| D[/dashboard]
    C --> E[POST /api/auth/change-password]
    E --> F[/dashboard]
```

### 10.4 Client API (`lib/api.ts`)

Fonctions à exposer :

- `searchUsers`, `getUser`, `createUser`, `updateUser`
- `deactivateUser`, `activateUser`, `resetUserPassword`
- `changePassword`, `importUsers`
- Gestion 403 périmètre → message i18n explicite

---

## 11. Import CSV

### 11.1 Format agents (`agents-mintp.csv`)

Séparateur `;`, encodage UTF-8, en-tête obligatoire :

```
matricule;email;nom;prenom;telephone;role;organisation_code;service;fonction;actif
```

**Valeurs `role` :** enums `UserRole` anglais (ex. `BUSINESS_ADMIN`, `DIRECTOR`).

**Valeurs `organisation_code` :** doit exister dans `organization.code`.

### 11.2 Comportement import

| Cas | Traitement |
|-----|------------|
| Email inconnu | Création, MDP par défaut `ChangeMe@MINTP1`, `mustChangePassword=true` |
| Email existant | Mise à jour champs (sauf `password_hash` sauf option future) |
| Ligne invalide | Ajout dans `errors[]`, continue les autres lignes |
| Response | `{ created, updated, errors[] }` |

---

## 12. User stories et critères d'acceptation

### US-USR-01 — Créer un agent

**En tant que** `BUSINESS_ADMIN`, **je veux** créer un agent dans mon service **afin de** l'intégrer au pilote.

**Critères :**
- [ ] Formulaire + `POST /api/users` fonctionnels
- [ ] Mot de passe temporaire affiché une fois
- [ ] Impossible de créer hors périmètre (403)
- [ ] Impossible d'attribuer `SUPER_ADMIN`

### US-USR-02 — Lister les agents de mon périmètre

**En tant que** `DIRECTOR`, **je veux** voir les agents de ma direction **afin de** superviser mon équipe.

**Critères :**
- [ ] `GET /api/users` retourne uniquement le périmètre
- [ ] Frontend accessible en lecture pour `DIRECTOR` (liste seule, pas boutons write)

### US-USR-03 — Isolation DRTP

**En tant que** `REGIONAL_DIRECTOR` de `DRTP-C`, **je ne dois pas** accéder aux users de `DRTP-LITTORAL`.

**Critères :**
- [ ] `GET /api/users/{id}` → 403
- [ ] Test d'intégration IT-RBAC-04 vert

### US-USR-04 — Désactiver un compte

**En tant que** `BUSINESS_ADMIN`, **je veux** désactiver un agent parti **afin de** bloquer son accès.

**Critères :**
- [ ] `active=false`, refresh révoqués
- [ ] Prochain login → 403 `INACTIVE`

### US-USR-05 — Import pilote 85 agents

**En tant que** `SUPER_ADMIN`, **je veux** importer `agents-mintp.csv` **afin de** peupler la base pilote.

**Critères :**
- [ ] ≥ 80 créations/mises à jour sans erreur bloquante
- [ ] Rapport `created` / `updated` / `errors` affiché

### US-AUTH-01 — Verrouillage compte

**Critères :**
- [ ] 5 échecs → HTTP 423
- [ ] Déverrouillage auto après 30 min OU `PATCH /unlock` par `SUPER_ADMIN`

### US-AUTH-02 — Changement MDP obligatoire

**Critères :**
- [ ] 1er login → redirection `/change-password`
- [ ] API métier bloquée (403) tant que `mustChangePassword=true`

### US-AUDIT-01 — Consulter le journal

**En tant que** `SUPER_ADMIN`, **je veux** filtrer les connexions par date et email.

**Critères :**
- [ ] Filtres FE branchés sur query params API
- [ ] `BUSINESS_ADMIN` → 403

---

## 13. Plan de tests

### 13.1 Tests unitaires

| Classe | Cas |
|--------|-----|
| `PasswordValidator` | MDP valides/invalides |
| `OrganizationScopeService` | DRTP, sous-arbre, rôles globaux |
| `AccessControlService` | Attribution rôle, périmètre write |

### 13.2 Tests d'intégration

| ID | Scénario |
|----|----------|
| IT-RBAC-01 | Login succès + refresh + logout |
| IT-RBAC-02 | 5 échecs → lock → unlock admin |
| IT-RBAC-03 | `mustChangePassword` bloque `GET /api/users` |
| IT-RBAC-04 | DRTP-C n'accède pas DRTP-LITTORAL |
| IT-RBAC-05 | `BUSINESS_ADMIN` crée user hors périmètre → 403 |
| IT-RBAC-06 | Import CSV 85 agents |
| IT-RBAC-07 | Désactivation invalide refresh |

### 13.3 Tests frontend (optionnel MVP)

- Login → dashboard
- Admin crée user → mot de passe temporaire affiché
- `RequireAuth` redirige non authentifié

---

## 14. Plan d'implémentation

### Phase A — Fondations (bloquant)

1. Script SQL `docs/sql/V1__create_users_organizations_auth.sql` + seed DRTP
2. Suppression packages legacy (`utilisateur/`, `organisation/`, `auth/` doublons)
3. `AccessControlService` + branchement `UserService`
4. Corrections `AuthService` (`mustChangePassword`, lock, refresh `active`)

### Phase B — API complète

5. Endpoints `activate`, `unlock`
6. Révocation refresh à la désactivation
7. Tests IT-RBAC-01 à 04

### Phase C — Frontend admin

8. Formulaires création/édition user
9. Reset MDP + change-password flow
10. Filtres audit + organisation sur liste users

### Phase D — Durcissement

11. Rate limiting login
12. Tests IT restants + import pilote bout en bout
13. Documentation OpenAPI à jour

---

## 15. Hors périmètre

- Permissions métier dossiers (`case:read`, `case:write`, …)
- Délégation suppléant
- 2FA, LDAP
- Flyway automatique (scripts manuels uniquement)
- Multi-organisation par utilisateur

---

## 16. Definition of Done

Le module USR/RBAC est considéré **implémenté** lorsque :

- [ ] Schéma SQL exécuté manuellement et documenté dans `docs/sql/`
- [ ] 85 agents importables sans erreur majeure
- [ ] Matrice §4.3 appliquée en service (pas seulement `@PreAuthorize`)
- [ ] Isolation DRTP validée par test d'intégration
- [ ] UI admin : CRUD users + change-password + liste audit filtrable
- [ ] Aucun package legacy en doublon
- [ ] OpenAPI `/swagger-ui.html` reflète les endpoints §7
- [ ] `ddl-auto` reste à `none`

---

*Document maintenu par l'équipe FluxPro — Nanotech*
