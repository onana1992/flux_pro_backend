# Spécification détaillée — Module Gestion de dossiers (DOS)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique
**Module :** Gestion des dossiers administratifs (DOS) — types de dossier, cycle de vie, pièces jointes, numérotation
**Version :** 1.0
**Date :** 10 juillet 2026
**Source :** Rétro-documentation à partir du code source réellement implémenté (`entity/FileEntity`, `entity/FileAttachment`, `entity/FileType`, `entity/FileNumberSequence`, `service/FileService`, `service/FileAttachmentService`, `service/LocalAttachmentStorageService`, `service/FileTypeService`, `controller/FileController`, `controller/FileTypeController`, `config/FileTypeDataInitializer` du backend Spring Boot ; pages `/files`, `/admin/file-types` et composants `FileFormPage`, `FileDetailPage`, `FilesTable` du frontend Next.js)

---

## Table des matières

1. [Objectif et périmètre](#1-objectif-et-périmètre)
2. [Modèle de données](#2-modèle-de-données)
3. [Cas d'utilisation](#3-cas-dutilisation)
4. [Règles de gestion](#4-règles-de-gestion)

---

## 1. Objectif et périmètre

### 1.1. Objectif

Le module Gestion de dossiers a pour objectif de :

- **Enregistrer** les dossiers administratifs MINTP (objet, expéditeur/bénéficiaire, type, organisation, priorité, date de réception) ;
- **Piloter le cycle de vie** d'un dossier : brouillon → en cours → clôturé → archivé (ou annulé), avec contrôles de statut stricts ;
- **Numéroter** de façon unique et concurrente chaque dossier soumis (`MINTP-{org}-{année}-{####}`) ;
- **Gérer les pièces jointes** (upload, téléchargement, suppression conditionnelle, document de réponse pour la clôture) ;
- **Administrer** le référentiel des types de dossier (`file_types`) ;
- **Isoler** les données selon le périmètre organisationnel RBAC de l'utilisateur ;
- **Servir d'ancrage** aux modules CHN-PASS (initialisation / circulation) et ALR/DSH (alertes, tableaux de bord).

### 1.2. Périmètre

**Inclus dans le module :**

| Bloc | Contenu |
|---|---|
| Cycle de vie dossier | Création (brouillon), modification, soumission, annulation, clôture, archivage, suppression physique (brouillon) |
| Consultation | Liste paginée filtrée, détail par id ou par numéro de référence |
| Numérotation | Séquences par organisation et année (`file_number_sequences`), verrouillage `FOR UPDATE` |
| Pièces jointes | Upload multipart, liste, téléchargement, suppression ; stockage local disque |
| Types de dossier | CRUD admin + liste publique des types actifs |
| Garde technique | Permissions `FILES:*`, `FILE_TYPES:*` ; contrôle de périmètre organisationnel |

**Explicitement hors périmètre du module (traité ailleurs) :**

- Initialisation et circulation de la chaîne de passation (`FilePassage`, transmit / return / suspend) — module **CHN-PASS** ;
- Configuration des templates de chaîne — module **CHN-TPL** (DOS stocke seulement `chain_template_id` après initialisation CHN) ;
- Calcul des échéances et alertes de retard — modules **DEL** / **ALR** ;
- Tableaux de bord et exports — module **DSH** ;
- CRUD des organisations — module **ORG** (DOS consomme `organization_id` et le périmètre calculé).

---

## 2. Modèle de données

### 2.1. Entités

Sauf mention contraire, les entités héritent de `BaseEntity` :

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `id` | `UUID` | généré automatiquement | Identifiant technique unique (`BINARY(16)`) |
| `createdAt` | `Instant` | date/heure courante à la création | Horodatage de création |
| `updatedAt` | `Instant` | date/heure courante | Horodatage de dernière modification |

#### 2.1.1. `FileEntity` (table `files`)

Dossier administratif suivi dans FluxPro.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `referenceNumber` | `String(32)` | `null` | Numéro métier unique ; attribué à la **soumission** uniquement |
| `fileTypeCode` | `String(32)` | — | Code du type de dossier (lien souple vers `file_types.code`) |
| `chainTemplate` | FK → `ChainTemplate` | `null` | Template de chaîne lié ; renseigné à l'**initialisation CHN**, pas à la soumission |
| `organization` | FK → `Organization` | — | Organisation porteuse du dossier (obligatoire) |
| `createdBy` | FK → `User` | — | Créateur du dossier |
| `subject` | `String(500)` | — | Objet du dossier |
| `senderOrBeneficiary` | `String(255)` | — | Expéditeur ou bénéficiaire |
| `receivedAt` | `LocalDate` | — | Date de réception |
| `priority` | `FilePriority` | `NORMAL` | Priorité métier |
| `status` | `FileStatus` | `DRAFT` | Statut du cycle de vie |
| `closureReason` | `String` (TEXT) | `null` | Motif de clôture |
| `closedAt` | `Instant` | `null` | Horodatage de clôture |
| `cancellationReason` | `String` (TEXT) | `null` | Motif d'annulation |
| `cancelledAt` | `Instant` | `null` | Horodatage d'annulation |
| `externalHoldReason` | `String` (TEXT) | `null` | Motif de suspension externe (écrit par CHN-PASS) |
| `externalHoldSince` | `Instant` | `null` | Début de suspension (écrit par CHN-PASS) |
| `metadata` | `Map` (JSON / LONGTEXT) | `null` | Métadonnées libres |
| `attachments` | Liste `FileAttachment` | vide | Pièces jointes du dossier |

#### 2.1.2. `FileAttachment` (table `file_attachments`)

Pièce jointe stockée hors base (fichier disque). N'hérite **pas** de `BaseEntity` : identifiant UUID propre + `createdAt` uniquement.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `id` | `UUID` | généré | Identifiant technique |
| `file` | FK → `FileEntity` | — | Dossier parent ; `ON DELETE CASCADE` |
| `originalFilename` | `String(255)` | — | Nom de fichier d'origine |
| `contentType` | `String(100)` | — | Type MIME |
| `sizeBytes` | `long` | — | Taille en octets |
| `storageBucket` | `String(63)` | `"local"` | Libellé du stockage (implémentation locale) |
| `storageKey` | `String(512)` | — | Chemin relatif de stockage |
| `responseDocument` | `boolean` | `false` | Document de réponse (requis pour clôturer) |
| `uploadedBy` | FK → `User` | — | Utilisateur ayant téléversé |
| `createdAt` | `Instant` | date/heure courante | Horodatage d'upload |

#### 2.1.3. `FileNumberSequence` (table `file_number_sequences`)

Compteur de numérotation par organisation et année civile (fuseau `Africa/Douala`).

| Champ | Type | Description |
|---|---|---|
| `organizationId` | `UUID` (PK composite) | Organisation |
| `year` | `int` (PK composite) | Année |
| `lastSequence` | `int` | Dernier numéro alloué (incrémenté sous verrou `FOR UPDATE`) |

#### 2.1.4. `FileType` (table `file_types`)

Référentiel des types de dossier. Hérite de `BaseEntity`.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `code` | `String(32)` | — | Code unique (ex. `COUR-STD`) ; **immuable** après création |
| `name` | `String` | — | Libellé français |
| `nameEn` | `String` | `null` | Libellé anglais |
| `description` | `String` (TEXT) | `null` | Description libre |
| `directionCode` | `String(32)` | `null` | Direction métier indicative (ex. DAG, DIER) |
| `sortOrder` | `int` | `0` | Ordre d'affichage |
| `active` | `boolean` | `true` | Type assignable aux nouveaux dossiers si actif |

### 2.2. Relations

| Relation | Description |
|---|---|
| `organization` (1) ↔ (N) `files` | Chaque dossier appartient à une organisation |
| `users` (1) ↔ (N) `files` | Créateur (`created_by_user_id`) |
| `chain_templates` (1) ↔ (N) `files` | Template optionnel après init CHN |
| `files` (1) ↔ (N) `file_attachments` | Pièces jointes ; cascade à la suppression du dossier |
| `file_types.code` ↔ `files.file_type_code` | Lien **souple** (pas de FK SQL) |
| `file_number_sequences` | Une ligne par couple `(organization_id, year)` |

### 2.3. Énumérations

#### `FileStatus`

| Valeur | Signification |
|---|---|
| `DRAFT` | Brouillon : éditable, pièces jointes modifiables, pas encore de numéro |
| `IN_PROGRESS` | Soumis : en circulation (ou en attente d'init chaîne) |
| `ON_HOLD` | Suspendu (écrit par CHN-PASS) |
| `CLOSED` | Clôturé avec document de réponse |
| `ARCHIVED` | Archivé (après clôture) |
| `CANCELLED` | Annulé |

#### `FilePriority`

| Valeur |
|---|
| `NORMAL`, `URGENT`, `VERY_URGENT` |

### 2.4. Machine à états (cycle de vie)

```
DRAFT ──submit──► IN_PROGRESS ──close──► CLOSED ──archive──► ARCHIVED
  │                    │
  │                    ├──cancel──► CANCELLED
  └──cancel────────────┘
                       │
                       └──suspend (CHN)──► ON_HOLD ──resume (CHN)──► IN_PROGRESS
```

### 2.5. Schéma SQL

| Script | Rôle |
|---|---|
| `docs/sql/2026-07-02_file_types.sql` | Table `file_types` |
| `docs/sql/2026-07-02_files.sql` | Tables `files`, `file_number_sequences`, `file_attachments` |
| `docs/sql/2026-07-02_seed_files_demo.sql` | Dossiers de démonstration |
| `docs/sql/2026-07-02_drop_file_type_default_template.sql` | Nettoyage colonne obsolète |

---

## 3. Cas d'utilisation

### UC-01 — Lister / rechercher les dossiers

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `FILES:READ` |
| Objectif | Obtenir la liste paginée des dossiers visibles dans son périmètre |
| Préconditions | Authentifié ; périmètre organisationnel non vide (sinon page vide) |
| Déroulement nominal | 1. `GET /api/files?search=&organizationId=&fileTypeCode=&status=&priority=&receivedFrom=&receivedTo=&page=&size=`.<br>2. Filtre par périmètre RBAC + critères optionnels.<br>3. Retourne une page de `FileSummaryResponse` (tri défaut : `receivedAt` DESC). |
| Exemple | Filtrer `status=IN_PROGRESS&fileTypeCode=COUR-STD` |
| Règles (code) | RG-DOS-01, RG-DOS-15 — `FileService.search()`, `FileController.list()` |

### UC-02 — Consulter un dossier

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:READ` |
| Objectif | Voir le détail complet (métadonnées, pièces jointes, template lié) |
| Préconditions | Dossier existant ; organisation dans le périmètre de l'acteur |
| Déroulement nominal | 1. `GET /api/files/{id}` **ou** `GET /api/files/by-reference/{ref}`.<br>2. `assertCanAccessFile`.<br>3. Retourne `FileDetailResponse`. |
| Exemple | Référence inconnue → `404 FILE_NOT_FOUND_BY_REFERENCE` |
| Règles (code) | RG-DOS-01 — `FileService.getById()` / `getByReference()` |

### UC-03 — Créer un dossier (brouillon)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:CREATE` |
| Objectif | Enregistrer un nouveau dossier en statut `DRAFT` |
| Préconditions | Organisation active accessible ; type de dossier actif |
| Déroulement nominal | 1. `POST /api/files` avec `FileCreateRequest` (`submit` généralement `false` côté UI).<br>2. Valide type actif, organisation active, périmètre.<br>3. Persiste sans numéro de référence ; statut `DRAFT`.<br>4. Si `submit=true`, enchaîne UC-05. |
| Exemple | Type inactif → `400 FILE_TYPE_INVALID` |
| Règles (code) | RG-DOS-02, RG-DOS-03, RG-DOS-04 — `FileService.create()` |

### UC-04 — Modifier un dossier brouillon

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:UPDATE` |
| Objectif | Mettre à jour type, organisation, objet, expéditeur, date, priorité, métadonnées |
| Préconditions | Statut strictement `DRAFT` |
| Déroulement nominal | 1. `PUT /api/files/{id}`.<br>2. Refuse si statut ≠ `DRAFT` (`FILE_NOT_EDITABLE`).<br>3. Applique les champs après revalidation type/org. |
| Exemple | Tentative sur un dossier `IN_PROGRESS` → `400 FILE_NOT_EDITABLE` |
| Règles (code) | RG-DOS-05 — `FileService.update()` |

### UC-05 — Soumettre un dossier

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:UPDATE` |
| Objectif | Passer le brouillon en circulation et lui attribuer un numéro |
| Préconditions | Statut `DRAFT` |
| Déroulement nominal | 1. `POST /api/files/{id}/submit` (ou `submit=true` à la création).<br>2. Alloue `referenceNumber` via séquence verrouillée.<br>3. Passe le statut à `IN_PROGRESS`.<br>4. **Ne lie pas** automatiquement un template de chaîne (initialisation CHN séparée). |
| Exemple | Format : `MINTP-DAG-2026-0001` |
| Règles (code) | RG-DOS-06, RG-DOS-07 — `FileService.submitInternal()`, `allocateReferenceNumber()` |

### UC-06 — Annuler un dossier

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:UPDATE` |
| Objectif | Abandonner un dossier non clôturé |
| Préconditions | Statut `DRAFT` ou `IN_PROGRESS` ; motif ≥ 10 caractères |
| Déroulement nominal | 1. `PATCH /api/files/{id}/cancel` avec `FileCancelRequest`.<br>2. Statut → `CANCELLED` ; enregistre motif et horodatage. |
| Exemple | Annulation depuis `CLOSED` → `400 FILE_STATUS_CANCEL_INVALID` |
| Règles (code) | RG-DOS-08 — `FileService.cancel()` |

### UC-07 — Clôturer un dossier

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:CLOSE` |
| Objectif | Clôturer un dossier en cours avec un document de réponse |
| Préconditions | Statut `IN_PROGRESS` ; pièce jointe de réponse existante sur le dossier ; motif ≥ 10 caractères |
| Déroulement nominal | 1. `PATCH /api/files/{id}/close` avec `closureReason` + `responseAttachmentId`.<br>2. Vérifie que la pièce appartient au dossier ; force `responseDocument=true` si besoin.<br>3. Statut → `CLOSED` ; `closedAt` = maintenant. |
| Exemple | Pièce jointe d'un autre dossier → `400 FILE_CLOSURE_INCOMPLETE` |
| Règles (code) | RG-DOS-09 — `FileService.close()` |

### UC-08 — Archiver un dossier

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:ARCHIVE` |
| Objectif | Archiver un dossier déjà clôturé |
| Préconditions | Statut `CLOSED` |
| Déroulement nominal | 1. `PATCH /api/files/{id}/archive`.<br>2. Statut → `ARCHIVED`. |
| Exemple | Archivage depuis `IN_PROGRESS` → `400 FILE_STATUS_ARCHIVE_INVALID` |
| Règles (code) | RG-DOS-10 — `FileService.archive()` |

### UC-09 — Supprimer un brouillon

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:DELETE` (matrice par défaut : `SUPER_ADMIN` uniquement) |
| Objectif | Supprimer physiquement un brouillon |
| Préconditions | Statut `DRAFT` |
| Déroulement nominal | 1. `DELETE /api/files/{id}`.<br>2. Suppression du dossier et des pièces jointes (cascade SQL + fichiers disque selon service). |
| Exemple | Suppression d'un dossier soumis → `400 FILE_DELETE_FORBIDDEN` |
| Règles (code) | RG-DOS-11 — `FileService.delete()` |

### UC-10 — Ajouter une pièce jointe

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:UPDATE` |
| Objectif | Téléverser un fichier sur le dossier |
| Préconditions | `DRAFT` (toute PJ) **ou** `IN_PROGRESS` uniquement si `responseDocument=true` ; type MIME autorisé ; taille ≤ ~20 Mo |
| Déroulement nominal | 1. `POST /api/files/{id}/attachments` (multipart `file`, `responseDocument`).<br>2. Valide type/taille/statut.<br>3. Stocke sur disque local (`fluxpro.attachments.storage-path`) ; persiste métadonnées. |
| Exemple | Upload PDF 25 Mo → `400 FILE_ATTACHMENT_TOO_LARGE` |
| Règles (code) | RG-DOS-12, RG-DOS-13 — `FileAttachmentService.upload()` |

### UC-11 — Lister / télécharger / supprimer une pièce jointe

| Champ | Détail |
|---|---|
| Acteur | Lecture : `FILES:READ` ; suppression : `FILES:UPDATE` |
| Objectif | Consulter ou retirer une pièce jointe |
| Préconditions | Suppression uniquement en `DRAFT` |
| Déroulement nominal | 1. `GET /api/files/{id}/attachments` — liste.<br>2. `GET .../attachments/{aid}/download` — flux binaire.<br>3. `DELETE .../attachments/{aid}` — refuse si dossier déjà soumis (`FILE_ATTACHMENT_DELETE_LOCKED`). |
| Exemple | Suppression après soumission → `400 FILE_ATTACHMENT_DELETE_LOCKED` |
| Règles (code) | RG-DOS-13, RG-DOS-14 — `FileAttachmentService` |

### UC-12 — Administrer les types de dossier

| Champ | Détail |
|---|---|
| Acteur | Permissions `FILE_TYPES:READ` / `CREATE` / `UPDATE` / `DELETE` |
| Objectif | Maintenir le référentiel des types |
| Préconditions | Code unique à la création ; code immuable ensuite ; suppression refusée si un template de chaîne référence le code |
| Déroulement nominal | 1. Liste publique active : `GET /api/file-types` (authentifié, sans permission fine).<br>2. Admin : `GET/POST/PUT/PATCH/DELETE /api/admin/file-types[/{id}]`.<br>3. Désactivation logique via `PATCH .../deactivate`. |
| Exemple | Seed : `COUR-STD`, `COUR-URG`, `MARCHE-SMP`, `AUTH-TRAV` (actifs) ; `COOP-PART` (inactif) |
| Règles (code) | RG-DOS-16, RG-DOS-17 — `FileTypeService`, `FileTypeDataInitializer` |

### UC-13 — Initialiser la chaîne sur un dossier (frontière CHN)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec `FILES:UPDATE` |
| Objectif | Attacher un template et créer les passages (hors cœur DOS, ancré sur le dossier) |
| Préconditions | Dossier `IN_PROGRESS` ou `ON_HOLD` ; chaîne pas déjà initialisée |
| Déroulement nominal | 1. `POST /api/files/{fileId}/chain/initialize` avec template + affectations responsables.<br>2. Pose `files.chain_template_id` ; crée les `file_passages` (CHN-PASS). |
| Exemple | UI : composant `PassageCircuit` sur la fiche dossier |
| Règles (code) | Consommateur CHN — `PassageService.initializeChainForFile()` ; RG-DOS-18 |

---

## 4. Règles de gestion

| ID | Règle | Composant (code) |
|---|---|---|
| RG-DOS-01 | Toute lecture ou écriture sur un dossier exige que son organisation soit dans le périmètre RBAC de l'acteur (pas de filtre « créateur uniquement » au niveau DOS). | `AccessControlService.assertCanAccessFile()`, `OrganizationScopeService` |
| RG-DOS-02 | Un dossier ne peut être créé que sur une organisation **active** et accessible. | `FileService.create()` / `update()` |
| RG-DOS-03 | Le type de dossier doit exister et être **actif** à la création / modification. | `FileService` → `FILE_TYPE_INVALID` |
| RG-DOS-04 | À la création, le statut initial est `DRAFT` et `referenceNumber` reste nul jusqu'à soumission. | `FileService.create()` |
| RG-DOS-05 | Seul un dossier en statut `DRAFT` est modifiable (métadonnées). | `FileService.update()` → `FILE_NOT_EDITABLE` |
| RG-DOS-06 | Seul un dossier `DRAFT` peut être soumis ; la soumission passe le statut à `IN_PROGRESS` et alloue le numéro. | `submitInternal()` → `FILE_STATUS_SUBMIT_INVALID` |
| RG-DOS-07 | Le numéro de référence suit le format `MINTP-{codeOrg}-{année}-{####}` (année `Africa/Douala`) ; allocation atomique via `SELECT … FOR UPDATE` sur `file_number_sequences`. | `allocateReferenceNumber()` |
| RG-DOS-08 | L'annulation n'est possible que depuis `DRAFT` ou `IN_PROGRESS`, avec un motif obligatoire (≥ 10 caractères). | `FileService.cancel()` |
| RG-DOS-09 | La clôture n'est possible que depuis `IN_PROGRESS`, avec motif (≥ 10 car.) et une pièce jointe de réponse appartenant au dossier. | `FileService.close()` |
| RG-DOS-10 | L'archivage n'est possible que depuis `CLOSED`. | `FileService.archive()` |
| RG-DOS-11 | La suppression physique n'est possible que pour un `DRAFT`. | `FileService.delete()` → `FILE_DELETE_FORBIDDEN` |
| RG-DOS-12 | Types MIME autorisés pour les PJ : PDF, DOCX, XLSX, JPEG, PNG ; taille max ≈ 20 Mo (`21_474_836` octets). | `FileAttachmentService` |
| RG-DOS-13 | Upload libre en `DRAFT` ; en `IN_PROGRESS`, upload autorisé uniquement si `responseDocument=true`. Suppression de PJ uniquement en `DRAFT`. | `FileAttachmentService` |
| RG-DOS-14 | Stockage des fichiers : disque local (`LocalAttachmentStorageService`), clé `{orgCode}/{year}/{fileId}/{uuid}_{nom}` ; pas MinIO dans l'implémentation actuelle. | `LocalAttachmentStorageService`, `application.properties` |
| RG-DOS-15 | La liste des dossiers est toujours filtrée par le périmètre organisationnel (global / régional / sous-arbre). | `FileService.search()` |
| RG-DOS-16 | Le code d'un type de dossier est unique et immuable après création. | `FileTypeService` → `FILE_TYPE_CODE_IN_USE` / `FILE_TYPE_CODE_IMMUTABLE` |
| RG-DOS-17 | Un type de dossier lié à au moins un template de chaîne (`file_type_code`) ne peut être supprimé. | `FileTypeService` → `FILE_TYPE_LINKED_TO_CHAIN` |
| RG-DOS-18 | La liaison `chain_template_id` n'est **pas** posée à la soumission ; elle l'est à l'initialisation de chaîne (CHN). La méthode `resolveTemplate` (règle T02 si `COUR-STD` + `VERY_URGENT`) existe en service mais n'est **pas** appelée par le flux de soumission actuel. | `FileService.resolveTemplate()`, `PassageService.initializeChainForFile()` |
| RG-DOS-19 | Les champs `externalHoldReason` / `externalHoldSince` et le statut `ON_HOLD` sont gérés par CHN-PASS (suspend / resume), pas par `FileService`. | `PassageService` |
| RG-DATA-01 | Le schéma est géré exclusivement par scripts SQL manuels (`docs/sql/`) ; `spring.jpa.hibernate.ddl-auto=none`. | Règle projet transverse |

---

### Annexe A — Permissions RBAC

| Permission | Description |
|---|---|
| `FILES:READ` | Lister / consulter dossiers et PJ |
| `FILES:CREATE` | Créer un dossier |
| `FILES:UPDATE` | Modifier brouillon, soumettre, annuler, gérer PJ, init chaîne |
| `FILES:CLOSE` | Clôturer |
| `FILES:ARCHIVE` | Archiver |
| `FILES:DELETE` | Supprimer un brouillon |
| `FILES:TRANSMIT` | Actions de passation (CHN-PASS) |
| `FILE_TYPES:READ` / `CREATE` / `UPDATE` / `DELETE` | Administration des types |

**Matrice indicative (seed) :**

| Rôle | Droits FILES notables |
|---|---|
| `SUPER_ADMIN` | Tous + `DELETE` |
| `BUSINESS_ADMIN`, `DIRECTOR` | READ, CREATE, UPDATE, TRANSMIT, CLOSE, ARCHIVE |
| `REGIONAL_DIRECTOR` | READ, CREATE, UPDATE, TRANSMIT, CLOSE |
| `SERVICE_HEAD`, `AGENT`, `SUPPORT` | READ, CREATE, UPDATE, TRANSMIT |
| `SECRETARY_GENERAL`, `EXECUTIVE_OFFICE`, `READER` | READ |

### Annexe B — Codes d'erreur i18n (`FILE_*`)

| Code | HTTP typique | Signification |
|---|---|---|
| `FILE_NOT_FOUND` / `FILE_NOT_FOUND_BY_REFERENCE` | 404 | Dossier introuvable |
| `FILE_ORGANIZATION_NOT_FOUND` / `FILE_ORGANIZATION_INACTIVE` | 400 | Organisation invalide |
| `FILE_TYPE_INVALID` | 400 | Type introuvable ou inactif |
| `FILE_NOT_EDITABLE` | 400 | Modification hors brouillon |
| `FILE_STATUS_SUBMIT_INVALID` | 400 | Soumission hors brouillon |
| `FILE_REFERENCE_EXISTS` | 400 | Collision de numéro |
| `FILE_STATUS_CANCEL_INVALID` | 400 | Annulation refusée |
| `FILE_STATUS_CLOSE_INVALID` | 400 | Clôture hors `IN_PROGRESS` |
| `FILE_CLOSURE_INCOMPLETE` | 400 | Document de réponse manquant / invalide |
| `FILE_STATUS_ARCHIVE_INVALID` | 400 | Archivage hors `CLOSED` |
| `FILE_DELETE_FORBIDDEN` | 400 | Suppression hors brouillon |
| `FILE_ATTACHMENT_*` | 400 | Erreurs PJ (taille, type, verrou, stockage…) |
| `FILE_TYPE_*` | 400/404 | Erreurs référentiel types |
| `FILE_TEMPLATE_NOT_FOUND_*` | 400 | Échecs de `resolveTemplate` (helper) |

### Annexe C — Endpoints API

Base dossiers : `/api/files`

| Méthode | Chemin | Permission |
|---|---|---|
| `GET` | `/` | `READ` |
| `GET` | `/{id}` | `READ` |
| `GET` | `/by-reference/{ref}` | `READ` |
| `POST` | `/` | `CREATE` |
| `PUT` | `/{id}` | `UPDATE` |
| `POST` | `/{id}/submit` | `UPDATE` |
| `PATCH` | `/{id}/cancel` | `UPDATE` |
| `PATCH` | `/{id}/close` | `CLOSE` |
| `PATCH` | `/{id}/archive` | `ARCHIVE` |
| `DELETE` | `/{id}` | `DELETE` |
| `POST` | `/{id}/attachments` | `UPDATE` |
| `GET` | `/{id}/attachments` | `READ` |
| `GET` | `/{id}/attachments/{aid}/download` | `READ` |
| `DELETE` | `/{id}/attachments/{aid}` | `UPDATE` |

Types : `GET /api/file-types` (actifs) ; admin sous `/api/admin/file-types`.

### Annexe D — Pages frontend

| Route | Rôle |
|---|---|
| `/files` | Liste + filtres |
| `/files/new` | Création brouillon (+ PJ + soumission) |
| `/files/[id]` | Fiche détail, clôture, annulation, archivage, circuit (`PassageCircuit`) |
| `/files/[id]/edit` | Édition brouillon |
| `/admin/file-types` | Administration des types |

### Annexe E — Types de dossier seedés

| Code | Actif | Usage typique |
|---|---|---|
| `COUR-STD` | oui | Courrier entrant standard |
| `COUR-URG` | oui | Courrier urgent |
| `MARCHE-SMP` | oui | Marché public simplifié |
| `AUTH-TRAV` | oui | Autorisation de travaux |
| `COOP-PART` | non | Coopération (hors pilote) |

---

*Document généré à partir de l'analyse du code source du module (`entity/File*.java`, `service/FileService.java`, `service/FileAttachmentService.java`, `service/LocalAttachmentStorageService.java`, `controller/FileController.java`, `controller/FileTypeController.java`, `config/FileTypeDataInitializer.java`, `docs/sql/2026-07-02_files*.sql`, frontend `FileFormPage.tsx` / `FileDetailPage.tsx` / `FilesTable.tsx`) — à maintenir en cohérence avec toute évolution du code.*
