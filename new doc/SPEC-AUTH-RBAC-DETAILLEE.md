# Spécification détaillée — Module Authentification & Contrôle d'accès (AUTH / RBAC)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique
**Module :** Authentification (AUTH) + Contrôle d'accès basé sur les rôles et permissions (RBAC)
**Version :** 1.0
**Date :** 5 juillet 2026
**Source :** Rétro-documentation à partir du code source réellement implémenté (`security/`, `service/`, `controller/`, `entity/` du backend Spring Boot) ; le cas UC-02bis documente en complément un comportement du frontend Next.js (`flux-pro-front`)

---

## Table des matières

1. [Objectif et périmètre](#1-objectif-et-périmètre)
2. [Modèle de données](#2-modèle-de-données)
3. [Cas d'utilisation](#3-cas-dutilisation)
4. [Règles de gestion](#4-règles-de-gestion)

---

## 1. Objectif et périmètre

### 1.1. Objectif

Le module Authentification & Contrôle d'accès a pour objectif de :

- **Identifier** de façon fiable chaque utilisateur de FluxPro (agents MINTP) via un compte email/mot de passe ;
- **Sécuriser** l'accès à l'API par un mécanisme de session sans état (JWT access/refresh) avec protection contre les attaques par force brute (verrouillage de compte) ;
- **Autoriser** finement chaque action selon deux axes complémentaires :
  - un axe **fonctionnel** — un système de permissions granulaires (`RESSOURCE:ACTION`) portées par des rôles administrables (rôles système + rôles personnalisés) ;
  - un axe **organisationnel** — un périmètre de données calculé à partir de la position de l'utilisateur dans l'arbre hiérarchique des organisations (ministère → directions → services, ou branche régionale DRTP) ;
- **Tracer** les événements de connexion (succès/échecs) à des fins d'audit et de sécurité ;
- **Administrer** le référentiel RBAC (utilisateurs, rôles, permissions, association aux organisations) via une API dédiée et une interface d'administration.

### 1.2. Périmètre

**Inclus dans le module :**

| Bloc | Contenu |
|---|---|
| Authentification | Connexion, rafraîchissement de session, déconnexion, changement de mot de passe, changement de mot de passe obligatoire, verrouillage/déverrouillage de compte |
| Gestion des utilisateurs | CRUD utilisateur, activation/désactivation, réinitialisation de mot de passe, import CSV en masse, affectation de rôles additionnels |
| RBAC administrable | CRUD des rôles, CRUD des permissions, association rôle ↔ permission(s), association utilisateur ↔ rôle(s) |
| Isolation organisationnelle | Calcul et application du périmètre de données par rôle/organisation |
| Audit | Journal des tentatives de connexion, consultation filtrée |
| Garde technique | Filtre JWT, aspect de validation des permissions, filtre de blocage « mot de passe à changer » |

**Explicitement hors périmètre du module (traité ailleurs ou non implémenté) :**

- Authentification à double facteur (2FA), SSO/LDAP ;
- Gestion du suppléant (`substitute_id` existe en base mais n'est exploité par aucun flux fonctionnel) ;
- Permissions métier fines sur les dossiers/chaînes/alertes (portées par les modules DOS/CHN/ALR, qui **consomment** les permissions RBAC définies ici mais ne sont pas décrites dans ce document) ;
- Gestion des organisations elle-même (CRUD organisation/type d'organisation) — seul l'usage du périmètre organisationnel par ce module est couvert ici.

---

## 2. Modèle de données

### 2.1. Entités

Toutes les entités héritent d'une classe technique commune `BaseEntity` qui porte l'identifiant et les dates d'audit :

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `id` | `UUID` | généré automatiquement | Identifiant technique unique |
| `createdAt` | `Instant` | date/heure courante à la création | Horodatage de création (non modifiable) |
| `updatedAt` | `Instant` | date/heure courante | Horodatage de dernière modification |

#### 2.1.1. `User` (table `users`)

Représente un compte utilisateur (agent MINTP). Porte à la fois le rôle RBAC principal (qui détermine le périmètre organisationnel) et, optionnellement, des rôles additionnels via la table de jointure `user_roles`.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `staffNumber` | `String(32)` | — | Matricule agent ; unique |
| `email` | `String` | — | Adresse email ; unique (insensible à la casse) ; sert d'identifiant de connexion |
| `lastName` | `String(100)` | — | Nom de famille |
| `firstName` | `String(100)` | — | Prénom |
| `phone` | `String(20)` | `null` | Numéro de téléphone |
| `role` | `UserRole` (enum) | — | Rôle RBAC principal ; détermine le périmètre organisationnel (§2.2) |
| `organization` | FK → `Organization` | — | Organisation de rattachement (obligatoire) |
| `jobTitle` | `String(255)` | `null` | Fonction / intitulé de poste |
| `passwordHash` | `String` | — | Hash BCrypt (force 12) du mot de passe |
| `mustChangePassword` | `boolean` | `true` | Si vrai, bloque toutes les routes métier tant que le mot de passe n'a pas été changé |
| `failedLoginAttempts` | `int` | `0` | Compteur d'échecs de connexion consécutifs |
| `lockedUntil` | `Instant` | `null` | Date de fin de verrouillage du compte ; `null` = non verrouillé |
| `active` | `boolean` | `true` | Statut du compte (désactivation logique) |
| `substitute` | FK → `User` | `null` | Suppléant désigné (champ réservé, non exploité fonctionnellement à ce jour) |
| `roles` | Liste FK → `Role` (table `user_roles`) | vide | Rôles RBAC additionnels apportant des permissions supplémentaires |

#### 2.1.2. `Organization` (table `organization`)

Nœud de l'arbre hiérarchique organisationnel du ministère (siège, directions, services, DRTP…), support du périmètre de données.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `code` | `String(32)` | — | Code unique de l'organisation |
| `name` | `String` | — | Libellé |
| `organizationType` | FK → `OrganizationType` | — | Type de l'organisation |
| `parent` | FK → `Organization` | `null` | Organisation parente (racine si `null`) |
| `active` | `boolean` | `true` | Statut ; une organisation inactive ne peut pas recevoir de nouveaux rattachements/écritures |

#### 2.1.3. `OrganizationType` (table `organization_type`)

Référentiel dynamique des types d'organisation (remplace un ancien enum figé), utilisé notamment pour déterminer les racines de périmètre régional.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `code` | `String(32)` | — | Code unique |
| `name` | `String` | — | Libellé (français) |
| `nameEn` | `String` | `null` | Libellé (anglais) |
| `description` | `String` (TEXT) | `null` | Description libre |
| `color` | `String(20)` | `null` | Couleur d'affichage (UI) |
| `sortOrder` | `int` | — | Ordre d'affichage |
| `allowsRoot` | `boolean` | — | Indique si ce type peut être racine d'un arbre d'organisations |
| `regionalScope` (`is_regional_scope`) | `boolean` | — | Marque ce type comme racine de périmètre régional ; utilisé par `OrganizationScopeService` pour borner le périmètre d'un `REGIONAL_DIRECTOR` |
| `active` | `boolean` | `true` | Statut |

#### 2.1.4. `Role` (table `roles`)

Rôle RBAC administrable, porteur d'un ensemble de permissions. Chacune des 10 valeurs de l'enum `UserRole` a un rôle système correspondant, créé/synchronisé automatiquement au démarrage.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `name` | `String(100)` | — | Nom unique du rôle (ex. `SUPER_ADMIN`, ou nom personnalisé) |
| `description` | `String` (TEXT) | `null` | Description libre |
| `systemRole` | `boolean` | `false` | `true` = rôle système : non renommable, non supprimable, resynchronisé au démarrage |
| `permissions` | Liste FK → `Permission` (table `role_permissions`) | vide | Permissions accordées par ce rôle |

#### 2.1.5. `Permission` (table `permissions`)

Permission atomique nommée `RESSOURCE:ACTION` (ex. `USERS:READ`), portée par un ou plusieurs rôles.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `name` | `String(100)` | — | Nom unique, format `RESOURCE:ACTION` |
| `resource` | `String(50)` | — | Ressource ciblée (ex. `USERS`, `FILES`, `DASHBOARD`) |
| `action` | `String(50)` | — | Action autorisée (`READ`, `CREATE`, `UPDATE`, `DELETE`, `IMPORT`, `RESET_PASSWORD`, `UNLOCK`, `EXPORT`, `CLOSE`, `ARCHIVE`, `TRANSMIT`) |
| `description` | `String` (TEXT) | `null` | Description libre |

#### 2.1.6. `RefreshToken` (table `refresh_token`)

Jeton d'authentification longue durée permettant de renouveler l'access token sans ressaisie du mot de passe.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `token` | `String(512)` | — | Valeur du jeton ; unique |
| `user` | FK → `User` | — | Propriétaire du jeton |
| `expiresAt` | `Instant` | — | Date d'expiration (émission + durée configurée) |
| `revoked` | `boolean` | `false` | Jeton révoqué (rotation à l'usage, déconnexion, changement de mot de passe, désactivation du compte) |

#### 2.1.7. `LoginAudit` (table `login_audit`)

Trace immuable de chaque tentative de connexion, réussie ou échouée.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `user` | FK → `User` | `null` | Utilisateur concerné ; `null` si l'email saisi est inconnu |
| `email` | `String` | — | Email saisi lors de la tentative (tel que fourni, normalisé) |
| `success` | `boolean` | — | Résultat de la tentative |
| `ipAddress` | `String(45)` | — | Adresse IP source (en-tête `X-Forwarded-For` sinon adresse distante) |
| `userAgent` | `String` (TEXT) | — | En-tête HTTP `User-Agent` du client |
| `failureReason` | `String(50)` | `null` | Motif d'échec typé (voir §2.2) ; `null` si succès |

#### 2.1.8. Tables de jointure

| Table | Colonnes | Rôle |
|---|---|---|
| `user_roles` | `user_id`, `role_id` | Association many-to-many `User` ↔ `Role` (rôles additionnels) |
| `role_permissions` | `role_id`, `permission_id` | Association many-to-many `Role` ↔ `Permission` |

### 2.2. Énumération

#### `UserRole` (rôle RBAC principal, détermine le périmètre organisationnel)

| Valeur | Portée organisationnelle associée |
|---|---|
| `SUPER_ADMIN` | Globale (tout le ministère) |
| `BUSINESS_ADMIN` | Globale, à l'exception des comptes `SUPER_ADMIN` (jamais gérables) |
| `SECRETARY_GENERAL` | Globale |
| `EXECUTIVE_OFFICE` | Globale |
| `DIRECTOR` | Organisation d'affectation + tous ses descendants |
| `SERVICE_HEAD` | Organisation d'affectation + tous ses descendants |
| `REGIONAL_DIRECTOR` | Toute la branche régionale (racine `OrganizationType.regionalScope = true`) contenant son organisation d'affectation |
| `AGENT` | Organisation d'affectation + descendants |
| `SUPPORT` | Organisation d'affectation + descendants |
| `READER` | Organisation d'affectation + descendants (lecture seule) |

#### Motifs d'échec de connexion (`LoginAudit.failureReason`)

> Valeurs conventionnelles gérées par `AuthService`/`GlobalExceptionHandler` (chaînes libres, pas un enum Java strict).

| Valeur | Déclencheur |
|---|---|
| `UNKNOWN_EMAIL` | Aucun compte ne correspond à l'email saisi |
| `USER_INACTIVE` | Compte désactivé (`active = false`) |
| `ACCOUNT_LOCKED` | Compte temporairement verrouillé (`lockedUntil` futur) |
| `INVALID_PASSWORD` | Mot de passe incorrect |

---

## 3. Cas d'utilisation

### UC-01 — Connexion (login)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur non authentifié (agent MINTP) |
| Objectif | Obtenir une session (access token + refresh token) pour accéder à l'API |
| Préconditions | L'utilisateur possède un compte (`email` connu) |
| Déroulement nominal | 1. L'utilisateur soumet `email` + `password` sur `POST /api/auth/login`.<br>2. Le système normalise l'email (minuscule, trim) et recherche le compte.<br>3. Vérifie que le compte est actif, non verrouillé, et que le mot de passe correspond au hash stocké.<br>4. Réinitialise le compteur d'échecs, génère un access token (JWT, 8h) et un refresh token (7j), les persiste, journalise le succès.<br>5. Retourne `TokenResponse` (tokens + profil utilisateur avec rôles/permissions). |
| Exemple | Requête : `{ "email": "c.essomba@mintp.cm", "password": "Secret@123" }` → Réponse `200` : `{ "accessToken": "...", "refreshToken": "...", "expiresIn": 28800, "user": { "role": "BUSINESS_ADMIN", "permissions": ["USERS:READ", ...] } }` |
| Règles (code) | RG-AUTH-01, RG-AUTH-02, RG-AUTH-03, RG-AUTH-08 — `AuthService.login()` |

### UC-02 — Rafraîchissement de session

| Champ | Détail |
|---|---|
| Acteur | Client applicatif détenant un refresh token valide |
| Objectif | Obtenir un nouvel access token sans ressaisie du mot de passe |
| Préconditions | Refresh token existant, non révoqué |
| Déroulement nominal | 1. `POST /api/auth/refresh` avec `refreshToken`.<br>2. Le système vérifie : jeton trouvé et non révoqué, non expiré, compte actif, compte non verrouillé.<br>3. Révoque l'ancien jeton (rotation), en émet un nouveau, régénère l'access token. |
| Exemple | Refresh token expiré → `401 Invalid refresh token` / `Refresh token expired` |
| Règles (code) | RG-AUTH-04, RG-AUTH-06 — `AuthService.refresh()` |

### UC-02bis — Expiration de session côté client (SPA)

| Champ | Détail |
|---|---|
| Acteur | Client web (frontend Next.js) d'un utilisateur précédemment authentifié |
| Objectif | Réagir proprement lorsque l'access token **et** le refresh token sont tous deux invalides (expirés, révoqués), sans laisser l'utilisateur face à des erreurs techniques silencieuses |
| Préconditions | Un appel API authentifié (avec access token) reçoit `401` |
| Déroulement nominal | 1. `apiFetch` intercepte le `401` et tente un rafraîchissement via `POST /api/auth/refresh` (UC-02).<br>2. Si le rafraîchissement échoue (refresh token absent, expiré ou révoqué → `401`), le client purge immédiatement `localStorage` (access token, refresh token, profil) et notifie `AuthProvider` via un bus d'événements interne.<br>3. `AuthProvider` met l'utilisateur courant à `null` en mémoire, ce qui déclenche la redirection vers `/login` (garde `RequireAuth`, déjà appliquée sur toutes les pages protégées).<br>4. La page de connexion affiche un message « Votre session a expiré. Veuillez vous reconnecter. » une seule fois, puis réinitialise l'indicateur. |
| Exemple | Un utilisateur laisse un onglet ouvert plus de 7 jours (durée de vie du refresh token) ; à sa prochaine action, il est automatiquement ramené sur `/login` avec le message d'expiration, sans avoir à interpréter une erreur réseau brute |
| Règles (code) | RG-AUTH-10 — `api.ts` (`apiFetch`), `session-events.ts`, `AuthProvider.tsx`, `RequireAuth.tsx` (frontend) |

### UC-03 — Déconnexion

| Champ | Détail |
|---|---|
| Acteur | Utilisateur authentifié |
| Objectif | Invalider sa session côté serveur |
| Préconditions | Aucune (best-effort) |
| Déroulement nominal | 1. `POST /api/auth/logout` avec `refreshToken` (optionnel).<br>2. Si le jeton est trouvé et non révoqué, il est marqué `revoked = true`. |
| Exemple | Appel sans corps → aucune erreur, no-op silencieux |
| Règles (code) | — `AuthService.logout()` |

### UC-04 — Changement de mot de passe

| Champ | Détail |
|---|---|
| Acteur | Utilisateur authentifié |
| Objectif | Changer son propre mot de passe |
| Préconditions | Connaître le mot de passe courant |
| Déroulement nominal | 1. `POST /api/auth/change-password` avec `currentPassword` + `newPassword`.<br>2. Valide le nouveau mot de passe selon la politique (RG-AUTH-01).<br>3. Vérifie que `currentPassword` correspond au hash actuel.<br>4. Met à jour le hash, lève `mustChangePassword`, révoque tous les refresh tokens de l'utilisateur. |
| Exemple | Ancien mot de passe erroné → `401 Current password is incorrect` |
| Règles (code) | RG-AUTH-01, RG-AUTH-05, RG-AUTH-07 — `AuthService.changePassword()` |

### UC-05 — Changement de mot de passe obligatoire (première connexion / après reset)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur dont `mustChangePassword = true` |
| Objectif | Empêcher l'usage fonctionnel de l'application tant que le mot de passe temporaire n'a pas été changé |
| Préconditions | Connexion réussie avec un compte à `mustChangePassword = true` |
| Déroulement nominal | 1. Après authentification JWT, `MustChangePasswordFilter` intercepte chaque requête.<br>2. Si le chemin n'est pas dans la liste autorisée (`/api/auth/change-password`, `/api/auth/logout`, `/api/users/me`), la requête est rejetée.<br>3. L'utilisateur doit appeler UC-04 pour lever le blocage. |
| Exemple | `GET /api/users` avec `mustChangePassword=true` → `403` `{ "code": "MUST_CHANGE_PASSWORD" }` |
| Règles (code) | RG-AUTH-05 — `MustChangePasswordFilter` |

### UC-06 — Consulter mon profil

| Champ | Détail |
|---|---|
| Acteur | Tout utilisateur authentifié |
| Objectif | Récupérer ses informations (identité, organisation, rôle, permissions effectives) |
| Préconditions | Être authentifié (aucune permission spécifique requise) |
| Déroulement nominal | 1. `GET /api/users/me`.<br>2. Retourne `UserProfileResponse` avec `roles[]` et `permissions[]` résolus (rôle principal + rôles additionnels). |
| Exemple | Réponse : `{ "id": "...", "role": "DIRECTOR", "roles": ["DIRECTOR"], "permissions": ["USERS:READ","FILES:READ", ...] }` |
| Règles (code) | RG-RBAC-13 — `UserService.getMeProfile()`, `RbacAuthorityService.resolve()` |

### UC-07 — Rechercher / lister les utilisateurs

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `USERS:READ` (typiquement `SUPER_ADMIN`, `BUSINESS_ADMIN`, `DIRECTOR`, `SERVICE_HEAD`, `REGIONAL_DIRECTOR`) |
| Objectif | Obtenir la liste paginée des utilisateurs visibles dans son périmètre |
| Préconditions | Permission `USERS:READ` **et** rôle reconnu par `AccessControlService.canReadUsers()` |
| Déroulement nominal | 1. `GET /api/users?organizationId=&role=&search=&page=&size=`.<br>2. Le système calcule le périmètre de l'acteur (`resolveUserSearchScope`).<br>3. Si `organizationId` est fourni et hors périmètre → refus.<br>4. Filtre les résultats par périmètre + filtres optionnels, retourne une page. |
| Exemple | Un `REGIONAL_DIRECTOR` de DRTP-Centre ne voit jamais les agents de DRTP-Littoral dans les résultats |
| Règles (code) | RG-RBAC-01, RG-RBAC-05, RG-RBAC-07 — `UserService.search()`, `OrganizationScopeService.resolveScopeFilter()` |

### UC-08 — Consulter un utilisateur

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `USERS:READ` |
| Objectif | Voir le détail d'un utilisateur |
| Préconditions | L'organisation de l'utilisateur cible doit être dans le périmètre de l'acteur |
| Déroulement nominal | 1. `GET /api/users/{id}`.<br>2. `AccessControlService.assertCanReadUser()` vérifie permission + périmètre.<br>3. Retourne `UserResponse` si autorisé. |
| Exemple | Cible hors périmètre → `403 Access denied to this user` |
| Règles (code) | RG-RBAC-05 — `UserService.getById()` |

### UC-09 — Créer un utilisateur

| Champ | Détail |
|---|---|
| Acteur | `SUPER_ADMIN` ou `BUSINESS_ADMIN` (permission `USERS:CREATE`) |
| Objectif | Créer un nouveau compte agent |
| Préconditions | Organisation cible active et dans le périmètre de l'acteur ; rôle à attribuer autorisé |
| Déroulement nominal | 1. `POST /api/users` avec les informations agent + `organizationId` + `role`.<br>2. Vérifie permission, périmètre d'écriture, attribution de rôle (pas `SUPER_ADMIN` sauf par `SUPER_ADMIN`), unicité `staffNumber`/`email`.<br>3. Génère (ou utilise) un mot de passe temporaire valide, force `mustChangePassword=true`.<br>4. Synchronise le rôle RBAC principal dans `user_roles`.<br>5. Retourne l'utilisateur créé **et** le mot de passe temporaire en clair (affiché une seule fois). |
| Exemple | Réponse `201` : `{ "user": {...}, "temporaryPassword": "Mintp@x7Kp2m" }` |
| Règles (code) | RG-RBAC-02, RG-RBAC-03, RG-RBAC-05, RG-RBAC-08, RG-RBAC-09 — `UserService.create()`, `AccessControlService.assertCanWriteUser()` |

### UC-10 — Modifier un utilisateur

| Champ | Détail |
|---|---|
| Acteur | `SUPER_ADMIN` ou `BUSINESS_ADMIN` (permission `USERS:UPDATE`) |
| Objectif | Mettre à jour les informations, l'organisation ou le rôle d'un utilisateur |
| Préconditions | Cible dans le périmètre ; cible non `SUPER_ADMIN` sauf si l'acteur l'est |
| Déroulement nominal | 1. `PUT /api/users/{id}`.<br>2. Vérifie permission, droit de gestion de la cible, unicité `staffNumber`/`email` (hors lui-même), organisation active et accessible.<br>3. Applique les modifications, resynchronise le rôle RBAC principal. |
| Exemple | Tentative de promotion en `SUPER_ADMIN` par un `BUSINESS_ADMIN` → `403 Cannot assign SUPER_ADMIN role` |
| Règles (code) | RG-RBAC-02, RG-RBAC-03, RG-RBAC-05, RG-RBAC-08, RG-RBAC-09 — `UserService.update()` |

### UC-11 — Désactiver un utilisateur

| Champ | Détail |
|---|---|
| Acteur | `SUPER_ADMIN` ou `BUSINESS_ADMIN` (permission `USERS:UPDATE`) |
| Objectif | Bloquer l'accès d'un agent (départ, mutation, sanction) |
| Préconditions | Cible dans le périmètre et gérable (pas `SUPER_ADMIN` pour un `BUSINESS_ADMIN`) |
| Déroulement nominal | 1. `PATCH /api/users/{id}/deactivate`.<br>2. Passe `active = false`.<br>3. Révoque immédiatement tous les refresh tokens de l'utilisateur. |
| Exemple | L'utilisateur désactivé, déjà connecté, ne peut plus rafraîchir sa session : prochain `refresh` → `403 Account inactive` |
| Règles (code) | RG-RBAC-10 — `UserService.deactivate()` |

### UC-12 — Réactiver un utilisateur

| Champ | Détail |
|---|---|
| Acteur | `SUPER_ADMIN` ou `BUSINESS_ADMIN` (permission `USERS:UPDATE`) |
| Objectif | Restaurer l'accès d'un compte désactivé |
| Préconditions | L'organisation de rattachement doit être active |
| Déroulement nominal | 1. `PATCH /api/users/{id}/activate`.<br>2. Vérifie que l'organisation est active, sinon refuse.<br>3. Passe `active = true`. |
| Exemple | Organisation entretemps désactivée → `400 Organization is inactive` |
| Règles (code) | RG-RBAC-11 — `UserService.activate()` |

### UC-13 — Déverrouiller un compte

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `USERS:UNLOCK` |
| Objectif | Lever un verrouillage suite à des échecs de connexion répétés, sans attendre les 30 minutes |
| Préconditions | Cible gérable (périmètre + hiérarchie de rôle) |
| Déroulement nominal | 1. `PATCH /api/users/{id}/unlock`.<br>2. Remet `failedLoginAttempts = 0` et `lockedUntil = null`. |
| Exemple | Un agent bloqué peut se reconnecter immédiatement après déverrouillage |
| Règles (code) | RG-AUTH-02 — `UserService.unlock()` |

### UC-14 — Réinitialiser le mot de passe d'un utilisateur

| Champ | Détail |
|---|---|
| Acteur | `SUPER_ADMIN` exclusivement |
| Objectif | Fournir un nouveau mot de passe temporaire à un agent (mot de passe oublié, compte compromis) |
| Préconditions | Rôle strictement `SUPER_ADMIN` (contrôle en dur en plus de la permission) |
| Déroulement nominal | 1. `POST /api/users/{id}/reset-password`.<br>2. Génère un mot de passe temporaire conforme à la politique.<br>3. Force `mustChangePassword=true`, réinitialise le compteur d'échecs et le verrou, révoque tous les refresh tokens.<br>4. Retourne le mot de passe temporaire en clair. |
| Exemple | Appel par un `BUSINESS_ADMIN` → `403 Access denied` (même avec la permission `USERS:RESET_PASSWORD`) |
| Règles (code) | RG-RBAC-04, RG-AUTH-07 — `UserService.resetPassword()` |

### UC-15 — Importer des utilisateurs en masse (CSV)

| Champ | Détail |
|---|---|
| Acteur | `SUPER_ADMIN` exclusivement |
| Objectif | Peupler/mettre à jour en masse le référentiel des agents pilotes |
| Préconditions | Fichier CSV `;`-séparé avec en-tête `matricule;email;nom;prenom;telephone;role;organisation_code;service;fonction;actif` |
| Déroulement nominal | 1. `POST /api/users/import` (multipart, champ `file`).<br>2. Pour chaque ligne : upsert par `email` ; création avec mot de passe par défaut `ChangeMe@MINTP1` et `mustChangePassword=true`, ou mise à jour des champs si l'email existe déjà.<br>3. Toute erreur de ligne (organisation inconnue, rôle invalide) est collectée sans interrompre le traitement des autres lignes.<br>4. Retourne `{ created, updated, errors[] }`. |
| Exemple | Ligne avec `organisation_code` inexistant → ajoutée à `errors: ["Line 12: Organization DRTP-XX"]`, import des autres lignes poursuivi |
| Règles (code) | RG-RBAC-04, RG-RBAC-12 — `UserService.importCsv()` |

### UC-16 — Attribuer un rôle additionnel à un utilisateur

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `USERS:UPDATE` et droit de gestion sur la cible |
| Objectif | Donner à un utilisateur des permissions supplémentaires via un rôle additionnel (sans changer son rôle RBAC principal) |
| Préconditions | Rôle existant, cible gérable |
| Déroulement nominal | 1. `POST /api/users/{id}/roles` avec `roleId`.<br>2. Vérifie le droit de gestion sur l'utilisateur cible.<br>3. Ajoute le rôle à `user_roles` s'il n'est pas déjà présent. |
| Exemple | Attribution du rôle personnalisé « Référent Alertes » à un `AGENT` pour lui donner `ALERT_RULES:CREATE` sans changer son rôle principal |
| Règles (code) | RG-RBAC-05, RG-RBAC-13 — `RoleService.assignRoleToUser()` |

### UC-17 — Révoquer un rôle d'un utilisateur

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `USERS:UPDATE` et droit de gestion sur la cible |
| Objectif | Retirer un rôle additionnel précédemment attribué |
| Préconditions | L'utilisateur doit conserver au moins un rôle après l'opération |
| Déroulement nominal | 1. `DELETE /api/users/{id}/roles/{roleId}`.<br>2. Refuse si c'est le dernier rôle de l'utilisateur.<br>3. Retire l'association sinon. |
| Exemple | Utilisateur avec un seul rôle → `400 User must keep at least one role` |
| Règles (code) | RG-RBAC-14 — `RoleService.revokeRoleFromUser()` |

### UC-18 — Gérer les rôles (CRUD)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permissions `ROLES:READ` / `ROLES:CREATE` / `ROLES:UPDATE` / `ROLES:DELETE` |
| Objectif | Créer des rôles personnalisés, modifier leur description/permissions, supprimer les rôles inutilisés |
| Préconditions | Un rôle système (`systemRole=true`) ne peut être ni renommé, ni supprimé ; un rôle affecté à au moins un utilisateur ne peut être supprimé |
| Déroulement nominal | 1. `GET/POST/PUT/DELETE /api/admin/roles[/{id}]`.<br>2. Création : vérifie l'unicité du nom, associe les permissions fournies.<br>3. Modification : refuse le renommage d'un rôle système, vérifie l'unicité si renommage.<br>4. Suppression : refuse si `systemRole=true` ou si des utilisateurs y sont liés. |
| Exemple | `DELETE /api/admin/roles/{id}` sur un rôle encore assigné → `400 Role is assigned to users` |
| Règles (code) | RG-RBAC-15, RG-RBAC-16 — `RoleService.create/update/delete()` |

### UC-19 — Gérer les permissions (CRUD)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permissions `PERMISSIONS:READ` / `CREATE` / `UPDATE` / `DELETE` |
| Objectif | Administrer le référentiel des permissions atomiques |
| Préconditions | Une permission encore liée à un rôle ne peut être supprimée |
| Déroulement nominal | 1. `GET/POST/PUT/DELETE /api/admin/permissions[/{id}]`, recherche par `resource`.<br>2. Création/modification : vérifie l'unicité du `name`.<br>3. Suppression : refuse si des rôles utilisent encore cette permission. |
| Exemple | `DELETE` sur `USERS:READ` encore utilisée par `DIRECTOR` → `400 Permission is assigned to roles` |
| Règles (code) | RG-RBAC-17 — `PermissionService.create/update/delete()` |

### UC-20 — Associer / retirer des permissions à un rôle

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `ROLES:UPDATE` |
| Objectif | Composer les permissions d'un rôle |
| Préconditions | Rôle et permission(s) existants |
| Déroulement nominal | 1. `POST /api/admin/roles/{id}/permissions` avec une liste de `permissionIds` → ajoute les permissions non déjà présentes.<br>2. `DELETE /api/admin/roles/{id}/permissions/{permissionId}` → retire l'association. |
| Exemple | Ajout de `ALERT_RULES:CREATE` au rôle `SERVICE_HEAD` personnalisé pour élargir ses droits sans toucher au rôle système |
| Règles (code) | RG-RBAC-13 — `RoleService.assignPermissions()/revokePermission()` |

### UC-21 — Consulter le journal des connexions

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `LOGIN_AUDIT:READ` (accordée uniquement à `SUPER_ADMIN` dans la matrice par défaut) |
| Objectif | Auditer les tentatives de connexion (sécurité, investigation) |
| Préconditions | Aucune restriction de périmètre organisationnel (vue globale) |
| Déroulement nominal | 1. `GET /api/admin/login-audit?email=&success=&from=&to=&page=&size=`.<br>2. Retourne la page filtrée de `LoginAuditResponse` (email, succès, IP, user-agent, motif, date). |
| Exemple | Filtrer les échecs (`success=false`) sur les 7 derniers jours pour repérer une attaque par force brute |
| Règles (code) | RG-AUTH-08 — `LoginAuditController.search()` |

---

## 4. Règles de gestion

| ID | Règle | Composant (code) |
|---|---|---|
| RG-AUTH-01 | Le mot de passe doit contenir au moins 8 caractères, une majuscule, un chiffre et un caractère spécial. | `PasswordValidator` |
| RG-AUTH-02 | Après 5 échecs de connexion consécutifs, le compte est verrouillé automatiquement pendant 30 minutes ; le compteur et le verrou sont remis à zéro dès qu'une connexion réussit ou que la durée de verrouillage est dépassée. | `AuthService.login()`, `isLocked()` |
| RG-AUTH-03 | Un compte inactif (`active=false`) ou verrouillé (`lockedUntil` futur) ne peut pas se connecter, avec des codes d'erreur distincts (`403 USER_INACTIVE`, `423 ACCOUNT_LOCKED`). | `AuthService.login()` |
| RG-AUTH-04 | Le refresh token est révoqué et refusé si l'utilisateur associé est devenu inactif ou verrouillé entre l'émission et l'utilisation du jeton. | `AuthService.refresh()` |
| RG-AUTH-05 | Tant que `mustChangePassword=true`, toutes les routes sont bloquées (`403 MUST_CHANGE_PASSWORD`) sauf `POST /api/auth/change-password`, `POST /api/auth/logout` et `GET /api/users/me`. | `MustChangePasswordFilter` |
| RG-AUTH-06 | Chaque utilisation du refresh token le révoque et en émet un nouveau (rotation à usage unique). | `AuthService.refresh()` |
| RG-AUTH-07 | Un changement de mot de passe (volontaire ou réinitialisation admin) révoque immédiatement tous les refresh tokens actifs de l'utilisateur. | `AuthService.changePassword()`, `UserService.resetPassword()` |
| RG-AUTH-08 | Toute tentative de connexion, réussie ou échouée, est journalisée avec l'email saisi, l'IP, le user-agent et le motif d'échec le cas échéant. | `LoginAuditService.log()` |
| RG-AUTH-09 | L'email est systématiquement normalisé (minuscule, sans espaces superflus) avant toute comparaison ou stockage. | `AuthService`, `UserService` |
| RG-AUTH-10 | Côté client (SPA), un `401` non récupérable (access token expiré **et** rafraîchissement impossible) purge immédiatement la session locale, affiche une notification « session expirée » sur `/login` et y redirige l'utilisateur ; ce comportement ne se déclenche que si une requête portait effectivement un token (pas de faux positif sur un appel anonyme). | `api.ts` (`apiFetch`), `session-events.ts`, `AuthProvider.tsx` (frontend) |
| RG-RBAC-01 | Toute action protégée applique une double garde cumulative : la permission portée par le JWT (`@RequiresPermission`) **et** le contrôle de périmètre organisationnel en service. | `RbacValidationAspect`, `AccessControlService` |
| RG-RBAC-02 | Seul un `SUPER_ADMIN` peut créer, modifier ou attribuer le rôle `SUPER_ADMIN` à un utilisateur. | `AccessControlService.assertCanAssignRole()` |
| RG-RBAC-03 | Un `BUSINESS_ADMIN` ne peut jamais lire, modifier, désactiver, réactiver, déverrouiller ou réinitialiser le mot de passe d'un compte `SUPER_ADMIN`, quel que soit le périmètre organisationnel. | `AccessControlService.assertCanManageUser()` |
| RG-RBAC-04 | L'import CSV en masse et la réinitialisation de mot de passe sont réservés strictement au rôle `SUPER_ADMIN`, vérifié explicitement en service indépendamment de la permission accordée par un rôle personnalisé. | `UserService.importCsv()`, `UserService.resetPassword()` |
| RG-RBAC-05 | Toute lecture ou écriture ciblant un utilisateur ou une organisation vérifie que la ressource cible est dans le périmètre organisationnel de l'acteur (organisation d'affectation + descendants, ou branche régionale pour `REGIONAL_DIRECTOR`, ou périmètre global pour `SUPER_ADMIN`/`BUSINESS_ADMIN`/`SECRETARY_GENERAL`/`EXECUTIVE_OFFICE`). | `OrganizationScopeService.canAccess()` |
| RG-RBAC-06 | Seuls `SUPER_ADMIN` et `BUSINESS_ADMIN` disposent d'un droit d'écriture sur les utilisateurs (`canWriteUsers`) ; `DIRECTOR`, `SERVICE_HEAD`, `REGIONAL_DIRECTOR` sont limités à la lecture de leur périmètre. | `AccessControlService.canWriteUsers()` |
| RG-RBAC-07 | Le périmètre d'un `REGIONAL_DIRECTOR` est borné à la branche organisationnelle dont la racine est de type `regionalScope=true` (DRTP) ; aucun accès inter-DRTP n'est possible. | `OrganizationScopeService.findRegionalRootCode()` |
| RG-RBAC-08 | Toute organisation cible d'une création/modification d'utilisateur doit être active ; sinon l'opération est refusée. | `AccessControlService.assertOrganizationWritable()`, `UserService.activate()` |
| RG-RBAC-09 | Le matricule (`staffNumber`) et l'email sont uniques dans tout le référentiel utilisateur (l'email est comparé insensible à la casse). | `UserService.validateUnique()` |
| RG-RBAC-10 | La désactivation d'un utilisateur (`active=false`) entraîne la révocation immédiate de tous ses refresh tokens, le déconnectant de fait de toutes ses sessions actives. | `UserService.deactivate()` |
| RG-RBAC-11 | La réactivation d'un utilisateur est refusée si son organisation de rattachement est elle-même inactive. | `UserService.activate()` |
| RG-RBAC-12 | L'import CSV traite chaque ligne indépendamment : une erreur sur une ligne est ajoutée au rapport `errors[]` sans annuler le traitement des lignes suivantes ni des lignes déjà traitées. | `UserService.importCsv()` |
| RG-RBAC-13 | Les permissions effectives d'un utilisateur sont l'union des permissions portées par son rôle RBAC principal (`user.role`) et par tous ses rôles additionnels (`user_roles`) ; elles sont recalculées à chaque émission de token et exposées via `/api/users/me`. | `RbacAuthorityService.resolve()` |
| RG-RBAC-14 | Un utilisateur doit conserver au moins un rôle RBAC à tout moment ; la révocation du dernier rôle est refusée. | `RoleService.revokeRoleFromUser()` |
| RG-RBAC-15 | Un rôle système (`systemRole=true`, aligné sur l'enum `UserRole`) ne peut être ni renommé, ni supprimé ; il est recréé/resynchronisé automatiquement au démarrage de l'application. | `RoleService.update()/delete()`, `RbacDataInitializer` |
| RG-RBAC-16 | Un rôle encore assigné à au moins un utilisateur ne peut être supprimé. | `RoleService.delete()` |
| RG-RBAC-17 | Une permission encore associée à au moins un rôle ne peut être supprimée. | `PermissionService.delete()` |
| RG-RBAC-18 | La création d'un rôle ou d'une permission personnalisée exige un nom unique dans le référentiel correspondant. | `RoleService.create()`, `PermissionService.create()` |
| RG-DATA-01 | Un utilisateur actif appartient à exactement une organisation active (pas de multi-organisation à ce jour). | `User.organization` (FK `nullable=false`) |
| RG-DATA-02 | Le schéma de base de données est géré exclusivement par scripts SQL manuels (`docs/sql/`) ; `spring.jpa.hibernate.ddl-auto` reste à `none` en toutes circonstances. | Règle projet transverse |

---

*Document généré à partir de l'analyse du code source du module (`security/`, `service/`, `controller/`, `entity/`, `config/RbacDataInitializer.java`) — à maintenir en cohérence avec toute évolution du code.*
