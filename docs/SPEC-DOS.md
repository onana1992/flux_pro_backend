# Spécification détaillée — Module DOS (Gestion des dossiers)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique  
**Cas pilote :** Ministère des Travaux Publics du Cameroun (MINTP)  
**Module :** DOS — Gestion des dossiers administratifs  
**Version :** 1.0  
**Date :** 1er juillet 2026  
**Statut :** Spécification cible — **non implémenté** (prérequis partiels livrés)

**Références :**
- [Cahier des charges §7.3](./CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md) — DOS-01 à DOS-11
- [Roadmap — Sprint 2](./ROADMAP-IMPLEMENTATION-CHAINEFLUX.md) — périmètre semaines 9–10
- [Inventaire types de dossiers](./PHASE-0-INVENTAIRE-TYPES-DOSSIERS.md) — catalogue COUR-STD, MARCHE-SMP, …
- [SPEC CHN](./SPEC-CHN.md) — module chaîne de passation (CDC §7.4)
- [SPEC CHN-TPL](./SPEC-CHN-TPL.md) — templates de chaîne, lien `file_type_code`
- [Sprint 3 — Chaînes & passation](./SPRINT-3-SPEC-CHAINES-PASSATION.md) — DOS-06 instanciation, DOS-10 maillons
- [SPEC USR / RBAC](./SPEC-USR-RBAC.md) — permissions, périmètre org
- [SPEC ORG](./SPEC-ORG.md) — `OrganizationScopeService`
- Règle projet : `spring.jpa.hibernate.ddl-auto=none` — scripts dans `docs/sql/`

---

## Table des matières

1. [Contexte et objectifs](#1-contexte-et-objectifs)
2. [État des lieux](#2-état-des-lieux)
3. [Périmètre fonctionnel](#3-périmètre-fonctionnel)
4. [Concepts métier](#4-concepts-métier)
5. [Modèle de données](#5-modèle-de-données)
6. [Règles métier](#6-règles-métier)
7. [API REST](#7-api-rest)
8. [RBAC et périmètre organisationnel](#8-rbac-et-périmètre-organisationnel)
9. [Stockage pièces jointes (MinIO)](#9-stockage-pièces-jointes-minio)
10. [Frontend](#10-frontend)
11. [User stories](#11-user-stories)
12. [Plan de tests techniques](#12-plan-de-tests-techniques)
13. [Recette UAT](#13-recette-uat)
14. [Hors périmètre et dépendances](#14-hors-périmètre-et-dépendances)
15. [Definition of Done](#15-definition-of-done)

---

## 1. Contexte et objectifs

### 1.1 Problème

Au MINTP, les dossiers administratifs (courriers, marchés simplifiés, autorisations de travaux) circulent sans traçabilité fiable. Les questions récurrentes — *Où est le dossier ? Qui le traite ? Depuis combien de temps ?* — ne trouvent pas de réponse immédiate.

Le module **DOS** est le **cœur documentaire** de FluxPro : il matérialise chaque dossier dans le système, lui attribue une identité unique, enregistre ses métadonnées et ses pièces, et sert de point d’ancrage aux modules aval (passation CHN, alertes ALR, tableaux de bord DSH).

### 1.2 Objectifs du module DOS

| Objectif | Description |
|----------|-------------|
| **Enregistrer** | Créer un dossier avec numéro unique et métadonnées obligatoires (DOS-01 à DOS-04) |
| **Joindre** | Attacher des documents numériques (DOS-05) |
| **Lier** | Associer type de dossier et template de chaîne (DOS-06) |
| **Piloter** | Suivre le statut de vie du dossier (DOS-07) |
| **Retrouver** | Rechercher et filtrer rapidement (DOS-08, DOS-09 — PERF-02) |
| **Clôturer** | Archiver avec motif et pièce de réponse (DOS-11) |
| **Isoler** | Restreindre la visibilité au périmère organisationnel (hérité Sprint 1) |

### 1.3 Exigences CDC couvertes

| ID | Libellé | Priorité | Sprint cible |
|----|---------|----------|--------------|
| DOS-01 | Création avec numéro unique auto-généré | Must | S2 |
| DOS-02 | Format `MINTP-{DIR}-{ANNÉE}-{SÉQUENCE}` | Must | S2 |
| DOS-03 | Champs obligatoires : type, objet, expéditeur/bénéficiaire, date réception, priorité | Must | S2 |
| DOS-04 | Priorités Normal / Urgent / Très urgent | Must | S2 |
| DOS-05 | Pièces jointes PDF, DOCX, XLSX, JPG, PNG (max 20 Mo) | Must | S2 |
| DOS-06 | Application auto modèle de chaîne selon le type | Must | S2 (résolution) + S3 (instanciation) |
| DOS-07 | Statuts Brouillon, En cours, En attente, Clôturé, Archivé, Annulé | Must | S2 |
| DOS-08 | Recherche full-text objet, numéro, expéditeur | Must | S2 |
| DOS-09 | Filtres direction, statut, retard, responsable, période | Must | S2 (partiel) / S3 (retard, responsable) |
| DOS-10 | Commentaires internes par maillon | Must | S3 (CHN-PASS) |
| DOS-11 | Clôture avec motif et pièce de réponse | Must | S2 (champs) + S3 (workflow clôture) |

### 1.4 Principes

- Nommage API et tables en **anglais** (`files`, `file_attachments`)
- UUID en **BINARY(16)** (aligné entités existantes)
- Numéro dossier **immuable** après création (`reference_number`)
- Suppression physique interdite pour dossiers ayant circulé — **annulation logique** (AUD-01)
- Erreurs API : **RFC 7807** (`ProblemDetail`)
- Performance recherche : **< 2 s** sur 1 000 dossiers (PERF-02)

---

## 2. État des lieux

| Composant | Statut juillet 2026 |
|-----------|---------------------|
| Table `files` / entité `File` | Non implémenté |
| Table `file_attachments` | Non implémenté |
| Numérotation séquentielle | Non implémenté |
| `FileService` / `FileController` | Non implémenté |
| Permissions `FILES:*` | Non seedées RBAC |
| Pages `/files`, `/files/new`, `/files/[id]` | Non implémentées |
| Intégration MinIO | Non implémentée |
| Référentiel `file_types` | **Implémenté** (`/admin/file-types`, seed COUR-STD…) |
| Templates chaîne `chain_templates` | **Implémenté** (CHN-TPL, lien `file_type_code`) |
| `OrganizationScopeService` | **Implémenté** (Sprint 1 — à brancher sur endpoints dossiers) |
| Passation / commentaires maillon (DOS-10) | Non implémenté (Sprint 3) |
| Filtre « en retard » / responsable actuel (DOS-09) | Non implémenté (dépend `file_passages`, Sprint 3) |

---

## 3. Périmètre fonctionnel

### 3.1 Inclus Sprint 2 — Must

| ID | Fonctionnalité |
|----|----------------|
| DOS-F01 | Créer un dossier (brouillon ou soumis directement) |
| DOS-F02 | Générer le numéro `MINTP-{DIR}-{ANNÉE}-{SÉQUENCE}` à la validation |
| DOS-F03 | Saisir les champs obligatoires DOS-03 |
| DOS-F04 | Sélectionner la priorité (Normal, Urgent, Très urgent) |
| DOS-F05 | Uploader / lister / supprimer pièces jointes (brouillon uniquement) |
| DOS-F06 | Résoudre le template de chaîne à partir du type (DOS-06) |
| DOS-F07 | Transitions de statut autorisées (DOS-07) |
| DOS-F08 | Recherche texte sur numéro, objet, expéditeur |
| DOS-F09 | Filtres : direction (org), type, statut, priorité, période réception |
| DOS-F10 | Consulter fiche dossier (métadonnées + pièces + template associé) |
| DOS-F11 | Modifier dossier en statut `DRAFT` |
| DOS-F12 | Annuler un dossier (motif obligatoire) |
| DOS-F13 | Liste paginée scopée au périmètre organisationnel |

### 3.2 Should (Sprint 2 ou S2+)

| ID | Fonctionnalité |
|----|----------------|
| DOS-F14 | Champs spécifiques par type (montant marché, localisation travaux…) — JSON `metadata` |
| DOS-F15 | Dupliquer un dossier brouillon |
| DOS-F16 | Export CSV liste dossiers filtrée |
| DOS-F17 | Historique des modifications métadonnées (audit) |

### 3.3 Livré en Sprint 3+ (référencé ici, implémenté ailleurs)

| ID | Fonctionnalité | Module |
|----|----------------|--------|
| DOS-F18 | Instanciation chaîne (`file_passages`) | CHN-PASS |
| DOS-F19 | Commentaires internes par maillon (DOS-10) | CHN-PASS |
| DOS-F20 | Clôture workflow avec scan réponse (DOS-11 complet) | CHN-PASS + DOS |
| DOS-F21 | Filtres retard / responsable actuel (DOS-09) | CHN-PASS + DEL |
| DOS-F22 | Statut « En attente pièce externe » (RM-05) | CHN-PASS |

---

## 4. Concepts métier

### 4.1 Dossier (`File`)

Unité de travail suivie dans FluxPro. Identifié par :

- **`reference_number`** — ex. `MINTP-DAG-2026-0042` (DOS-02)
- **`file_type_code`** — ex. `COUR-STD` (référentiel `file_types`)
- **`organization_id`** — direction propriétaire (numérotation et périmètre)
- **`status`** — cycle de vie (DOS-07)
- **`priority`** — impact délais / template (DOS-04, DOS-06c)

### 4.2 Type de dossier

Référentiel national (`file_types`). Le type détermine :

- Les champs métier attendus (catalogue Phase 0)
- Le **template de chaîne** via lookup : `chain_templates.file_type_code = file_types.code` (DOS-06)

> Le type de dossier **ne porte pas** de template par défaut en propre — le lien est porté par le template (CHN-TPL).

### 4.3 Priorité (`FilePriority`)

| Valeur | Usage |
|--------|-------|
| `NORMAL` | Défaut |
| `URGENT` | Accélération alertes (Sprint 4) |
| `VERY_URGENT` | Peut forcer template T02 si type `COUR-STD` (DOS-06c) |

### 4.4 Statut dossier (`FileStatus`)

| Statut | Code | Description |
|--------|------|-------------|
| Brouillon | `DRAFT` | Création en cours ; modifiable ; pas de numéro définitif |
| En cours | `IN_PROGRESS` | Chaîne active ; numéro attribué |
| En attente | `ON_HOLD` | Suspension externe (RM-05) — alertes off |
| Clôturé | `CLOSED` | Traitement terminé (DOS-11) |
| Archivé | `ARCHIVED` | Conservation long terme |
| Annulé | `CANCELLED` | Abandon tracé ; lecture seule |

### 4.5 Pièce jointe (`FileAttachment`)

Fichier binaire stocké dans **MinIO** ; métadonnées en base. Une pièce peut être marquée **`response_document = true`** lors de la clôture (DOS-11).

### 4.6 Numérotation

Séquence **par direction et année civile** :

```
MINTP-DAG-2026-0001
MINTP-DAG-2026-0002
MINTP-DIER-2026-0001
```

- `{DIR}` = code organisation **racine direction** (ex. `DAG`, `DIER`, `DRTP-C`)
- `{SÉQUENCE}` = entier sur 4 chiffres minimum (zero-padded), unique par `(organization_id, year)`

---

## 5. Modèle de données

### 5.1 Table `files`

```sql
CREATE TABLE files (
    id                      BINARY(16)   NOT NULL PRIMARY KEY,
    reference_number        VARCHAR(32)  NULL UNIQUE,
    file_type_code          VARCHAR(32)  NOT NULL,
    chain_template_id       BINARY(16)   NULL,
    organization_id         BINARY(16)   NOT NULL,
    created_by_user_id      BINARY(16)   NOT NULL,
    subject                 VARCHAR(500) NOT NULL,
    sender_or_beneficiary   VARCHAR(255) NOT NULL,
    received_at             DATE         NOT NULL,
    priority                VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    closure_reason          TEXT         NULL,
    closed_at               DATETIME(6)  NULL,
    cancelled_at            DATETIME(6)  NULL,
    cancellation_reason     TEXT         NULL,
    external_hold_reason    TEXT         NULL,
    external_hold_since     DATETIME(6)  NULL,
    metadata                JSON         NULL,
    created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_file_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_file_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_file_chain_template
        FOREIGN KEY (chain_template_id) REFERENCES chain_templates(id),
    INDEX idx_files_org (organization_id),
    INDEX idx_files_status (status),
    INDEX idx_files_type (file_type_code),
    INDEX idx_files_received (received_at),
    INDEX idx_files_ref (reference_number),
    FULLTEXT INDEX ft_files_search (reference_number, subject, sender_or_beneficiary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

> Script complet à placer dans `docs/sql/2026-XX-XX_files.sql`.

### 5.2 Table `file_number_sequences`

```sql
CREATE TABLE file_number_sequences (
    organization_id BINARY(16) NOT NULL,
    year            INT         NOT NULL,
    last_sequence   INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (organization_id, year),
    CONSTRAINT fk_seq_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

Allocation transactionnelle : `SELECT … FOR UPDATE` puis incrément.

### 5.3 Table `file_attachments`

```sql
CREATE TABLE file_attachments (
    id                BINARY(16)   NOT NULL PRIMARY KEY,
    file_id           BINARY(16)   NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    storage_bucket    VARCHAR(63)  NOT NULL,
    storage_key       VARCHAR(512) NOT NULL,
    response_document BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_by_id    BINARY(16)   NOT NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_attachment_file
        FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_attachment_user
        FOREIGN KEY (uploaded_by_id) REFERENCES users(id),
    INDEX idx_attachments_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.4 Entités JPA (cible)

```java
@Entity @Table(name = "files")
public class File extends BaseEntity {
    private String referenceNumber;
    private String fileTypeCode;
    @ManyToOne private ChainTemplate chainTemplate;
    @ManyToOne private Organization organization;
    @ManyToOne private User createdBy;
    private String subject;
    private String senderOrBeneficiary;
    private LocalDate receivedAt;
    @Enumerated(EnumType.STRING) private FilePriority priority;
    @Enumerated(EnumType.STRING) private FileStatus status;
    private String closureReason;
    private Instant closedAt;
    // … cancellation, external hold, metadata JSON
    @OneToMany(mappedBy = "file") private List<FileAttachment> attachments;
}

public enum FilePriority { NORMAL, URGENT, VERY_URGENT }

public enum FileStatus {
    DRAFT, IN_PROGRESS, ON_HOLD, CLOSED, ARCHIVED, CANCELLED
}
```

### 5.5 DTOs API

**`FileSummaryResponse`**
```json
{
  "id": "uuid",
  "referenceNumber": "MINTP-DAG-2026-0042",
  "fileTypeCode": "COUR-STD",
  "subject": "Demande de renseignements",
  "priority": "NORMAL",
  "status": "IN_PROGRESS",
  "receivedAt": "2026-06-15",
  "organizationCode": "DAG",
  "organizationName": "Direction des Affaires Générales",
  "chainTemplateCode": "T01",
  "createdAt": "2026-06-15T08:00:00Z"
}
```

**`FileDetailResponse`** — inclut métadonnées complètes, template, pièces, indicateurs passation (S3)

**`FileCreateRequest`**
```json
{
  "fileTypeCode": "COUR-STD",
  "organizationId": "uuid",
  "subject": "Courrier ministère Travaux Publics",
  "senderOrBeneficiary": "Ministère des Finances",
  "receivedAt": "2026-06-15",
  "priority": "NORMAL",
  "submit": true
}
```

**`FileCloseRequest`** (DOS-11)
```json
{
  "closureReason": "Réponse signée et expédiée",
  "responseAttachmentId": "uuid"
}
```

---

## 6. Règles métier

| ID | Règle | Validation | Code erreur |
|----|-------|------------|-------------|
| DOS-R01 | Numéro généré uniquement à la soumission (`submit=true`) | Service | `FILE_NUMBER_ON_SUBMIT` |
| DOS-R02 | Format numéro conforme DOS-02 | Service | — |
| DOS-R03 | Unicité `reference_number` | Contrainte BDD | `FILE_REFERENCE_EXISTS` |
| DOS-R04 | Type dossier actif et existant | Service | `FILE_TYPE_INVALID` |
| DOS-R05 | Template résolu : actif + `file_type_code` correspondant | Service | `FILE_TEMPLATE_NOT_FOUND` |
| DOS-R06 | DOS-06c : `COUR-STD` + `VERY_URGENT` → template T02 | Service | — |
| DOS-R07 | Modification interdite si statut ≠ `DRAFT` (sauf champs S3) | Service | `FILE_NOT_EDITABLE` |
| DOS-R08 | Pièces : formats autorisés, max 20 Mo (DOS-05) | Service | `FILE_ATTACHMENT_INVALID` |
| DOS-R09 | Suppression pièce : uniquement en `DRAFT` | Service | `FILE_ATTACHMENT_LOCKED` |
| DOS-R10 | Annulation : motif ≥ 10 caractères | Bean validation | `400` |
| DOS-R11 | Clôture : motif + pièce réponse obligatoires (DOS-11) | Service | `FILE_CLOSURE_INCOMPLETE` |
| DOS-R12 | Transition statut conforme au diagramme §6.2 | Service | `FILE_STATUS_TRANSITION_INVALID` |
| DOS-R13 | Accès dossier : périmètre org (`OrganizationScopeService`) | Service | `403` |
| DOS-R14 | Pas de DELETE HTTP sur dossier soumis | Service | `FILE_DELETE_FORBIDDEN` |

### 6.1 Diagramme transitions de statut

```
DRAFT ──submit──► IN_PROGRESS ──hold──► ON_HOLD
                      │                    │
                      │◄────resume─────────┘
                      │
                      ├──close──► CLOSED ──archive──► ARCHIVED
                      │
                      └──cancel──► CANCELLED

DRAFT ──cancel──► CANCELLED
```

### 6.2 Algorithme numérotation (DOS-01 / DOS-02)

```
1. Déterminer organization_id (direction propriétaire)
2. Extraire code direction DIR depuis organizations.code (ex. DAG)
3. year = année courante (timezone Africa/Douala)
4. BEGIN TRANSACTION
5.   UPSERT file_number_sequences FOR (org_id, year) FOR UPDATE
6.   seq = last_sequence + 1 ; UPDATE last_sequence
7.   reference = "MINTP-" + DIR + "-" + year + "-" + lpad(seq, 4, '0')
8. COMMIT
9. Persister files.reference_number
```

### 6.3 Résolution template (DOS-06)

```
1. Charger file_types par code ; vérifier active
2. Si priority = VERY_URGENT AND file_type = COUR-STD :
       template = chain_templates WHERE code = 'T02' AND active
   Sinon :
       template = chain_templates WHERE file_type_code = :type AND active
3. Si aucun template actif → erreur FILE_TEMPLATE_NOT_FOUND
4. Stocker chain_template_id sur le dossier
5. (Sprint 3) Appeler ChainPassageService.initialize(file, template)
```

### 6.4 Recherche (DOS-08)

- Full-text MySQL sur `reference_number`, `subject`, `sender_or_beneficiary`
- Fallback `LIKE` si FULLTEXT indisponible en dev
- Tri par défaut : `received_at DESC`
- Objectif PERF-02 : < 2 s pour 1 000 dossiers (index + pagination)

---

## 7. API REST

Base : `/api/files`

### 7.1 Endpoints Sprint 2

| Méthode | Route | Permission | Description |
|---------|-------|------------|-------------|
| GET | `/` | `FILES:READ` | Liste paginée / filtrable |
| GET | `/{id}` | `FILES:READ` | Détail dossier |
| GET | `/by-reference/{ref}` | `FILES:READ` | Résolution par numéro |
| POST | `/` | `FILES:CREATE` | Créer (brouillon ou soumis) |
| PUT | `/{id}` | `FILES:UPDATE` | Modifier (DRAFT uniquement) |
| POST | `/{id}/submit` | `FILES:UPDATE` | Brouillon → En cours + numéro + template |
| PATCH | `/{id}/cancel` | `FILES:UPDATE` | Annuler |
| PATCH | `/{id}/close` | `FILES:CLOSE` | Clôturer (DOS-11) |
| PATCH | `/{id}/archive` | `FILES:ARCHIVE` | Archiver |
| POST | `/{id}/attachments` | `FILES:UPDATE` | Upload pièce jointe |
| GET | `/{id}/attachments` | `FILES:READ` | Liste pièces |
| GET | `/{id}/attachments/{aid}/download` | `FILES:READ` | Téléchargement (URL signée ou stream) |
| DELETE | `/{id}/attachments/{aid}` | `FILES:UPDATE` | Supprimer pièce (DRAFT) |

### 7.2 Paramètres liste GET `/`

| Paramètre | Type | Description |
|-----------|------|-------------|
| `search` | string | Full-text numéro, objet, expéditeur |
| `organizationId` | uuid | Filtrer direction |
| `fileTypeCode` | string | Type dossier |
| `status` | enum | Statut |
| `priority` | enum | Priorité |
| `receivedFrom` | date | Période réception (début) |
| `receivedTo` | date | Période réception (fin) |
| `overdue` | boolean | En retard (Sprint 3 — jointure passages) |
| `responsibleUserId` | uuid | Responsable actuel (Sprint 3) |
| `page`, `size` | int | Pagination |

### 7.3 Exemples réponses erreur

**Type dossier invalide (400)**
```json
{
  "status": 400,
  "detail": "File type not found or inactive: UNKNOWN",
  "code": "FILE_TYPE_INVALID"
}
```

**Modification interdite (409)**
```json
{
  "status": 409,
  "detail": "File cannot be edited in status IN_PROGRESS",
  "code": "FILE_NOT_EDITABLE"
}
```

**Hors périmètre (403)**
```json
{
  "status": 403,
  "detail": "Access denied to organization scope"
}
```

### 7.4 Service `FileService` (méthodes)

| Méthode | Description |
|---------|-------------|
| `findAll(filter, pageable, actor)` | Liste scopée org |
| `findById(id, actor)` | Détail + contrôle périmètre |
| `findByReference(ref, actor)` | Lookup numéro |
| `create(request, actor)` | Création ; numérotation si submit |
| `update(id, request, actor)` | Métadonnées DRAFT |
| `submit(id, actor)` | DRAFT → IN_PROGRESS |
| `cancel(id, reason, actor)` | Annulation |
| `close(id, request, actor)` | Clôture DOS-11 |
| `archive(id, actor)` | Archivage |
| `resolveTemplate(fileType, priority)` | DOS-06 |
| `allocateReferenceNumber(orgId)` | DOS-01 / DOS-02 |

---

## 8. RBAC et périmètre organisationnel

### 8.1 Permissions

| Permission | Actions |
|------------|---------|
| `FILES:READ` | GET liste, détail, téléchargement pièces |
| `FILES:CREATE` | POST création |
| `FILES:UPDATE` | PUT, submit, cancel, upload/delete pièces (DRAFT) |
| `FILES:CLOSE` | PATCH close |
| `FILES:ARCHIVE` | PATCH archive |
| `FILES:DELETE` | Réservé SUPER_ADMIN — dossiers DRAFT abandonnés uniquement |
| `FILES:TRANSMIT` | Sprint 3 — passation maillons |

### 8.2 Matrice rôles système (seed RBAC)

| Rôle | READ | CREATE | UPDATE | CLOSE | ARCHIVE |
|------|------|--------|--------|-------|---------|
| `SUPER_ADMIN` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `BUSINESS_ADMIN` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `DIRECTOR` | ✓* | ✓* | ✓* | ✓* | ✓* |
| `SERVICE_HEAD` | ✓* | ✓* | ✓* | ✓* | — |
| `AGENT` | ✓* | ✓* | ✓* | — | — |
| `SUPPORT` | ✓* | ✓* | ✓* | — | — |
| `READER` | ✓* | — | — | — | — |
| `SECRETARY_GENERAL` | ✓ | — | — | — | — |
| `EXECUTIVE_OFFICE` | ✓ | — | — | — | — |
| `REGIONAL_DIRECTOR` | ✓* | ✓* | ✓* | ✓* | — |

\* Périmètre limité via `OrganizationScopeService` (direction + descendants ; DRTP isolée si `is_regional_scope`).

### 8.3 Garde API

- `@RequiresPermission` sur `FileController`
- Chaque lecture/écriture : `accessControlService.assertCanAccessFile(actor, file)`
- Rôles « vision ministère » (`SUPER_ADMIN`, `SECRETARY_GENERAL`, `EXECUTIVE_OFFICE`) : accès transversal lecture

---

## 9. Stockage pièces jointes (MinIO)

### 9.1 Configuration

| Paramètre | Valeur |
|-----------|--------|
| Bucket | `fluxpro-attachments` (dev/staging/prod) |
| Clé objet | `{org_code}/{year}/{file_id}/{uuid}_{filename}` |
| Taille max | 20 Mo (21 474 836 octets) |
| MIME autorisés | `application/pdf`, `application/vnd.openxmlformats-officedocument.*`, `image/jpeg`, `image/png` |

### 9.2 Flux upload

```
1. Client POST multipart /api/files/{id}/attachments
2. FileService vérifie statut DRAFT ou droit clôture (response doc)
3. Validation taille + extension + MIME
4. MinioClient.putObject(bucket, key, stream, size, contentType)
5. INSERT file_attachments
6. Retour FileAttachmentResponse
```

### 9.3 Téléchargement

- URL pré-signée (15 min) ou proxy stream via backend
- Journal audit (AUD-01) : `FILE_ATTACHMENT_DOWNLOADED`

---

## 10. Frontend

### 10.1 Écrans

| Route | Composant | Permission |
|-------|-----------|------------|
| `/files` | `FilesListPage` | `FILES:READ` |
| `/files/new` | `FileFormPage` | `FILES:CREATE` |
| `/files/[id]` | `FileDetailPage` | `FILES:READ` |
| `/files/[id]/edit` | `FileFormPage` | `FILES:UPDATE` (DRAFT) |

Navigation : section **Principal** ou **Dossiers** dans `AppShell`.

### 10.2 Liste (`/files`)

| Élément | Description |
|---------|-------------|
| En-tête | Titre + bouton « Nouveau dossier » |
| Compteur | `{filtered} / {total} dossier(s)` |
| Filtres | Recherche, direction, type, statut, priorité, période |
| Tableau | Numéro, objet, type, priorité, statut, date réception, direction |
| Badge retard | Sprint 3 — si maillon actif en dépassement |
| Actions | Voir / Modifier (si DRAFT) |

### 10.3 Formulaire création / édition

**Section identité**
- Type dossier (select `file_types` actifs)
- Direction (select org — défaut : org utilisateur)
- Objet, expéditeur/bénéficiaire
- Date réception, priorité

**Section pièces jointes**
- Zone drag & drop
- Liste fichiers avec taille, suppression (DRAFT)
- Validation 20 Mo côté client + serveur

**Actions**
- « Enregistrer brouillon » → statut `DRAFT`
- « Enregistrer et soumettre » → numéro + template + `IN_PROGRESS`

### 10.4 Fiche dossier (`/files/[id]`)

**Onglets**
- **Résumé** — métadonnées, statut, template associé
- **Pièces jointes** — liste + téléchargement
- **Circuit** — Sprint 3 (`PassageCircuit`)
- **Historique** — Sprint 5 (audit)

**Actions contextuelles**
- Modifier (DRAFT)
- Annuler (motif)
- Clôturer (motif + pièce réponse) — si autorisé

### 10.5 i18n (clés proposées)

```
files.title
files.create
files.referenceNumber
files.subject
files.senderOrBeneficiary
files.receivedAt
files.priority
files.status
files.attachments
files.submit
files.saveDraft
files.close
files.cancel
```

---

## 11. User stories

### US-DOS-01 — Enregistrer un courrier entrant (UC-01)

**En tant qu'** agent SUPPORT (DAG),  
**je veux** créer un dossier courrier avec scan PDF,  
**afin de** lancer le circuit de traitement institutionnel.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | Agent DAG authentifié | POST dossier COUR-STD complet + submit | `201`, numéro `MINTP-DAG-2026-XXXX` |
| AC-02 | Type COUR-STD | Soumission | `chainTemplateCode = T01` |
| AC-03 | Priorité Très urgent + COUR-STD | Soumission | Template T02 (DOS-06c) |
| AC-04 | Champs manquants | POST | `400` validation |

### US-DOS-02 — Consulter mes dossiers

**En tant qu'** agent,  
**je veux** voir la liste des dossiers de mon périmètre,  
**afin de** prioriser mon travail.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | Agent DRTP-C | GET `/api/files` | Uniquement dossiers DRTP-C |
| AC-02 | Agent DIER | GET dossier DRTP-C par ID | `403` |
| AC-03 | Recherche « Finances » | GET avec `search` | Résultats sur objet/expéditeur |

### US-DOS-03 — Pièces jointes

**En tant qu'** agent,  
**je veux** joindre un PDF de 15 Mo,  
**afin de** numériser le courrier entrant.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | Dossier DRAFT | Upload PDF 15 Mo | `201`, fichier listé |
| AC-02 | Fichier 21 Mo | Upload | `400` FILE_ATTACHMENT_INVALID |
| AC-03 | Dossier IN_PROGRESS | DELETE pièce | `409` FILE_ATTACHMENT_LOCKED |

### US-DOS-04 — Clôturer un dossier (DOS-11)

**En tant qu'** agent traitant,  
**je veux** clôturer avec motif et scan de réponse,  
**afin de** finaliser le dossier.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | Dossier IN_PROGRESS, chaîne complète (S3) | PATCH close avec motif + pièce | `status = CLOSED` |
| AC-02 | Sans pièce réponse | PATCH close | `400` FILE_CLOSURE_INCOMPLETE |

---

## 12. Plan de tests techniques

### 12.1 Tests unitaires

| ID | Composant | Scénario |
|----|-----------|----------|
| UT-DOS-01 | `FileService.allocateReferenceNumber` | Séquence consécutive même org/année |
| UT-DOS-02 | `FileService.allocateReferenceNumber` | Deux orgs → séquences indépendantes |
| UT-DOS-03 | `FileService.resolveTemplate` | COUR-STD → T01 |
| UT-DOS-04 | `FileService.resolveTemplate` | COUR-STD + VERY_URGENT → T02 |
| UT-DOS-05 | `FileService.update` | IN_PROGRESS → FILE_NOT_EDITABLE |
| UT-DOS-06 | `FileAttachmentService` | Rejet 21 Mo |
| UT-DOS-07 | `OrganizationScopeService` + File | DRTP isolation |

### 12.2 Tests d'intégration API

| ID | Scénario | Résultat |
|----|----------|----------|
| IT-DOS-01 | CRUD brouillon complet | 201 / 200 |
| IT-DOS-02 | Submit → numéro unique | 200, format MINTP-* |
| IT-DOS-03 | Double submit même org | Numéros distincts |
| IT-DOS-04 | AGENT hors périmètre | 403 |
| IT-DOS-05 | Upload + download pièce | 201 / 200 |
| IT-DOS-06 | Recherche full-text | 200, < 2 s (jeu 1000) |

### 12.3 Tests frontend E2E

| ID | Scénario |
|----|----------|
| E2E-DOS-01 | Création courrier + brouillon → liste |
| E2E-DOS-02 | Soumission affiche numéro |
| E2E-DOS-03 | Upload rejeté > 20 Mo |
| E2E-DOS-04 | Agent DRTP ne voit pas dossiers DIER |

---

## 13. Recette UAT

### 13.1 Objectif

Valider avec les référents DAG, DIER et DRTP que l'enregistrement dossier est utilisable en conditions réelles (UC-01, UC-02, UC-03 — phase enregistrement).

### 13.2 Prérequis

| Élément | Exigence |
|---------|----------|
| Scripts SQL | `files`, `file_attachments`, `file_number_sequences` |
| Référentiels | `file_types` + `chain_templates` seedés |
| MinIO | Bucket configuré |
| Comptes | SUPPORT DAG, AGENT DIER, AGENT DRTP-C |

### 13.3 Matrice UAT (extrait)

| ID | Scénario | Résultat attendu | OK/KO |
|----|----------|------------------|-------|
| DOS-UAT-01 | Créer courrier COUR-STD + PDF | Numéro MINTP-DAG-…, statut En cours | |
| DOS-UAT-02 | Rechercher par numéro | Dossier retrouvé < 2 s | |
| DOS-UAT-03 | Agent DRTP accès dossier DIER | Refus 403 | |
| DOS-UAT-04 | Courrier très urgent | Template T02 appliqué | |
| DOS-UAT-05 | Upload 21 Mo | Message erreur clair | |
| DOS-UAT-06 | Modifier dossier soumis | Refus modification | |

---

## 14. Hors périmètre et dépendances

### 14.1 Hors périmètre DOS (autres modules)

| Fonctionnalité | Module | Sprint |
|----------------|--------|--------|
| Transmission maillon à maillon | CHN-PASS | S3 |
| Commentaires par maillon (DOS-10) | CHN-PASS | S3 |
| Calcul échéances / retard | DEL | S3 |
| Alertes J-2, J+0 | ALR | S4 |
| Dashboard « mes dossiers » | DSH | S5 |
| Fiche circulation PDF | AUD | S5 |
| Workflows parallèles | Post-pilote | — |

### 14.2 Dépendances amont (livré)

| Module | Apport |
|--------|--------|
| AUTH | JWT, sessions |
| ORG | Arbre organisations, périmètre |
| USR/RBAC | Rôles, permissions |
| `file_types` | Catalogue types dossiers |
| CHN-TPL | Templates + `file_type_code` |

### 14.3 Dépendances aval

| Module | Besoin DOS |
|--------|------------|
| CHN-PASS | Entité `File` + statut `IN_PROGRESS` |
| ALR | `file_id`, statut, maillon actif |
| DSH | Agrégations sur `files` |

---

## 15. Definition of Done

### 15.1 Backend

- [ ] Script SQL `docs/sql/2026-XX-XX_files.sql` exécuté manuellement
- [ ] Entités `File`, `FileAttachment`, `FileNumberSequence`
- [ ] `FileService`, `FileAttachmentService`, `FileController`
- [ ] Permissions `FILES:*` seedées RBAC
- [ ] Intégration MinIO configurable (`application.properties`)
- [ ] Tests UT-DOS-01 à UT-DOS-07 verts
- [ ] Tests IT-DOS-01 à IT-DOS-06 verts
- [ ] OpenAPI documenté

### 15.2 Frontend

- [ ] Pages `/files`, `/files/new`, `/files/[id]`
- [ ] i18n FR/EN
- [ ] Navigation AppShell
- [ ] E2E-DOS-01 à E2E-DOS-04 verts

### 15.3 Recette

- [ ] DOS-UAT-01 à DOS-UAT-06 validés par référent métier
- [ ] PERF-02 mesuré sur jeu 1 000 dossiers
- [ ] Test isolation DRTP vs DIER (R03 roadmap)

### 15.4 Documentation

- [ ] Mise à jour statut §2 après implémentation
- [ ] Guide admin : paramétrage MinIO
- [ ] Lien spec CHN-PASS pour DOS-06 instanciation et DOS-10

---

*Document généré pour le Sprint 2 FluxPro — aligné CDC §7.3 et roadmap MINTP.*
