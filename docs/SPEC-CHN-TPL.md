# Spécification détaillée — Module CHN-TPL (Templates de chaîne)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique  
**Cas pilote :** Ministère des Travaux Publics du Cameroun (MINTP)  
**Module :** CHN-TPL — Configuration des modèles de chaînes de passation  
**Version :** 1.0  
**Date :** 1er juillet 2026  
**Statut :** Spécification cible — **non implémenté**

**Références :**
- [Cahier des charges §7.4](./CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md) — CHN-01 à CHN-10
- [SPEC CHN](./SPEC-CHN.md) — module parent chaîne de passation
- [Sprint 3 — Chaînes & passation](./SPRINT-3-SPEC-CHAINES-PASSATION.md) — contexte global
- [Inventaire types de dossiers](./PHASE-0-INVENTAIRE-TYPES-DOSSIERS.md) — catalogue COUR-STD, MARCHE-SMP, …
- [SPEC USR / RBAC](./SPEC-USR-RBAC.md) — rôles `UserRole`, permissions
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
8. [RBAC et sécurité](#8-rbac-et-sécurité)
9. [Frontend](#9-frontend)
10. [Données de référence — Seed MINTP](#10-données-de-référence--seed-mintp)
11. [User stories](#11-user-stories)
12. [Plan de tests techniques](#12-plan-de-tests-techniques)
13. [Recette UAT](#13-recette-uat)
14. [Hors périmètre](#14-hors-périmètre)
15. [Definition of Done](#15-definition-of-done)

---

## 1. Contexte et objectifs

### 1.1 Problème

Chaque type de dossier MINTP (courrier entrant, marché simplifié, autorisation travaux…) suit un **circuit institutionnel** fixe : une séquence de maillons hiérarchiques, chacun avec un rôle responsable, un délai cible et une action attendue.

Sans référentiel paramétrable de **templates de chaîne**, FluxPro ne peut pas :

- appliquer automatiquement le bon circuit à la création d'un dossier (DOS-06) ;
- calculer les échéances par maillon (`DelaiService`) ;
- afficher la timeline de passation sur la fiche dossier.

### 1.2 Objectifs du module CHN-TPL

| Objectif | Description |
|----------|-------------|
| **Définir** | Créer et maintenir des templates T01–T05 (et templates personnalisés) |
| **Structurer** | Décrire les maillons ordonnés (libellé, rôle, délai, action) |
| **Lier** | Associer un template à un type de dossier (`file_type_code`) |
| **Sécuriser** | Protéger les templates système ; contrôler les droits RBAC |
| **Initialiser** | Seed MINTP au démarrage / migration SQL |

### 1.3 Exigences CDC couvertes

| ID | Libellé | Priorité |
|----|---------|----------|
| CHN-01 | Définition d'un template de chaîne par type de dossier | Must |
| CHN-02 | Chaque maillon : libellé, rôle responsable, délai, action attendue | Must |

### 1.4 Principes

- Nommage API et champs en **anglais** (`chain_templates`, `chain_step_templates`)
- UUID stockés en **BINARY(16)** (aligné Hibernate / entités existantes)
- Templates système **non supprimables** (T01–T05)
- Désactivation logique (`active = false`) — pas de suppression physique
- Erreurs API : **RFC 7807** (`ProblemDetail`)

---

## 2. État des lieux

| Composant | Statut juillet 2026 |
|-----------|---------------------|
| Entités `ChainTemplate`, `ChainStepTemplate` | Non implémenté |
| Tables `chain_templates`, `chain_step_templates` | Non créées |
| `ChainTemplateService` / `Controller` | Non implémenté |
| Permissions `CHAIN_TEMPLATES:*` | Non seedées RBAC |
| Pages `/admin/chain-templates` | Non implémentées |
| Lien type dossier → template | Dépend Sprint 2 (`file_types`) |

---

## 3. Périmètre fonctionnel

### 3.1 Inclus (Must)

| ID | Fonctionnalité |
|----|----------------|
| TPL-F01 | Lister les templates (actifs + inactifs pour admin) |
| TPL-F02 | Consulter le détail d'un template avec ses maillons ordonnés |
| TPL-F03 | Créer un template personnalisé (hors système) |
| TPL-F04 | Modifier l'en-tête template (nom, description, délai total) |
| TPL-F05 | Remplacer / réordonner les maillons d'un template |
| TPL-F06 | Désactiver un template (si aucun dossier en cours) |
| TPL-F07 | Seed automatique T01–T05 au démarrage |
| TPL-F08 | Validation métier des maillons (règles TPL-01 à TPL-06) |
| TPL-F09 | Lecture template par tout agent authentifié (consultation) |

### 3.2 Should

| ID | Fonctionnalité |
|----|----------------|
| TPL-F10 | Dupliquer un template existant |
| TPL-F11 | Export JSON d'un template |
| TPL-F12 | Historique des modifications (audit) |

### 3.3 Exclu de CHN-TPL (autres modules)

| Fonctionnalité | Module |
|----------------|--------|
| Instanciation chaîne sur dossier | CHN-PASS |
| Transmission maillon à maillon | CHN-PASS |
| Calcul échéances | DEL (`DelaiService`) |
| Alertes retard | Sprint 4 |

---

## 4. Concepts métier

### 4.1 Template de chaîne (`ChainTemplate`)

Modèle **réutilisable** décrivant le circuit type d'un dossier. Identifié par un code stable (`T01`…`T05`).

```
Template T01 « Courrier entrant standard »
├── Maillon 1 — Réception DAG
├── Maillon 2 — Orientation Chef Courrier
├── …
└── Maillon 7 — Clôture
```

### 4.2 Maillon template (`ChainStepTemplate`)

Étape **abstraite** du circuit. À l'instanciation (CHN-PASS), chaque maillon devient une `FilePassage` avec un responsable concret.

| Attribut | Rôle métier |
|----------|-------------|
| `step_order` | Position dans le circuit (1 = premier) |
| `label` | Libellé affiché (ex. « Instruction technique ») |
| `responsible_role` | Rôle RBAC attendu (`UserRole`) |
| `delay_value` + `delay_unit` | Délai alloué au maillon |
| `expected_action` | Action attendue (texte libre) |
| `optional` | Maillon contournable (ex. visa Ministre) |
| `closure_step` | Dernier maillon — déclenche clôture dossier |

### 4.3 Unités de délai (`DelayUnit`)

| Valeur | Usage |
|--------|-------|
| `WORKING_DAYS` | Défaut — jours ouvrés lun–ven |
| `WORKING_HOURS` | T02 très urgent — plage 08h–17h |

### 4.4 Lien type de dossier

| `file_type_code` | Template par défaut |
|------------------|---------------------|
| `COUR-STD` | T01 |
| `COUR-URG` | T02 |
| `MARCHE-SMP` | T03 |
| `AUTH-TRAV` | T04 |

---

## 5. Modèle de données

### 5.1 Table `chain_templates`

```sql
CREATE TABLE chain_templates (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    code            VARCHAR(10)  NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    description     TEXT         NULL,
    file_type_code  VARCHAR(32)  NULL,
    total_delay_days INT         NOT NULL DEFAULT 0,
    delay_unit      VARCHAR(20)  NOT NULL DEFAULT 'WORKING_DAYS',
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    system_template BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_chain_templates_active (active),
    INDEX idx_chain_templates_file_type (file_type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 5.2 Table `chain_step_templates`

```sql
CREATE TABLE chain_step_templates (
    id                  BINARY(16)  NOT NULL PRIMARY KEY,
    chain_template_id   BINARY(16)  NOT NULL,
    step_order          INT         NOT NULL,
    label               VARCHAR(255) NOT NULL,
    responsible_role    VARCHAR(30) NOT NULL,
    delay_value         INT         NOT NULL DEFAULT 0,
    delay_unit          VARCHAR(20) NOT NULL DEFAULT 'WORKING_DAYS',
    expected_action     VARCHAR(500) NULL,
    optional            BOOLEAN     NOT NULL DEFAULT FALSE,
    closure_step        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_step_template_chain
        FOREIGN KEY (chain_template_id) REFERENCES chain_templates(id) ON DELETE CASCADE,
    UNIQUE KEY uk_chain_step_order (chain_template_id, step_order),
    INDEX idx_step_template_chain (chain_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

> Script complet à placer dans `docs/sql/2026-XX-XX_chain_templates.sql` (avec seed §10).

### 5.3 Entités JPA (cible)

```java
@Entity @Table(name = "chain_templates")
public class ChainTemplate extends BaseEntity {
    private String code;
    private String name;
    private String description;
    private String fileTypeCode;
    private int totalDelayDays;
    @Enumerated(EnumType.STRING) private DelayUnit delayUnit;
    private boolean active = true;
    private boolean systemTemplate = false;
    @OneToMany(mappedBy = "chainTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<ChainStepTemplate> steps = new ArrayList<>();
}

@Entity @Table(name = "chain_step_templates")
public class ChainStepTemplate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chain_template_id")
    private ChainTemplate chainTemplate;
    private int stepOrder;
    private String label;
    @Enumerated(EnumType.STRING) private UserRole responsibleRole;
    private int delayValue;
    @Enumerated(EnumType.STRING) private DelayUnit delayUnit;
    private String expectedAction;
    private boolean optional;
    private boolean closureStep;
}
```

### 5.4 DTOs API

**`ChainTemplateSummaryResponse`**
```json
{
  "id": "uuid",
  "code": "T01",
  "name": "Courrier entrant standard",
  "fileTypeCode": "COUR-STD",
  "totalDelayDays": 11,
  "delayUnit": "WORKING_DAYS",
  "active": true,
  "systemTemplate": true,
  "stepCount": 7
}
```

**`ChainTemplateDetailResponse`** — inclut `steps[]`

**`ChainStepTemplateRequest`**
```json
{
  "stepOrder": 1,
  "label": "Réception DAG",
  "responsibleRole": "SUPPORT",
  "delayValue": 1,
  "delayUnit": "WORKING_DAYS",
  "expectedAction": "Enregistrer et numériser le courrier",
  "optional": false,
  "closureStep": false
}
```

**`ChainTemplateCreateRequest`**
```json
{
  "code": "T06",
  "name": "Circuit personnalisé",
  "description": "…",
  "fileTypeCode": "CUSTOM-01",
  "totalDelayDays": 10,
  "delayUnit": "WORKING_DAYS",
  "steps": [ /* ChainStepTemplateRequest[] */ ]
}
```

---

## 6. Règles métier

| ID | Règle | Validation | Code erreur |
|----|-------|------------|-------------|
| TPL-01 | `code` unique (insensible à la casse) | Service + contrainte BDD | `CHAIN_TEMPLATE_CODE_EXISTS` |
| TPL-02 | `step_order` consécutifs 1..N sans trou | Service | `CHAIN_STEP_ORDER_GAP` |
| TPL-03 | Exactement **un** maillon `closure_step = true` | Service | `CHAIN_CLOSURE_STEP_INVALID` |
| TPL-04 | Σ `delay_value` (j.o.) ≤ `total_delay_days` | Service | `CHAIN_DELAY_SUM_EXCEEDED` |
| TPL-05 | Template `system_template` : **DELETE interdit** | Service | `CHAIN_SYSTEM_TEMPLATE_PROTECTED` |
| TPL-06 | Désactivation interdite si dossiers `IN_PROGRESS` utilisent le template | Requête count | `CHAIN_TEMPLATE_IN_USE` |
| TPL-07 | `responsible_role` doit être une valeur `UserRole` valide | Bean validation | `400` |
| TPL-08 | Maillon clôture : `delay_value = 0` | Service | `CHAIN_CLOSURE_DELAY_INVALID` |
| TPL-09 | Modifier les maillons d'un template système : **autorisé** (TPL-05) | — | — |
| TPL-10 | Template inactif : non proposé à la création dossier | Service S2 | — |

### 6.1 Algorithme validation maillons

```
1. Trier steps par step_order
2. Vérifier step_order == 1..N (TPL-02)
3. Compter closure_step == 1 (TPL-03)
4. Si closure_step : delay_value == 0 (TPL-08)
5. Somme delay_value (convertir heures → fraction j.o. si besoin) ≤ total_delay_days (TPL-04)
6. Chaque responsible_role ∈ UserRole.values() (TPL-07)
```

---

## 7. API REST

Base : `/api/admin/chain-templates`

### 7.1 Endpoints

| Méthode | Route | Permission | Description |
|---------|-------|------------|-------------|
| GET | `/` | `CHAIN_TEMPLATES:READ` | Liste paginée / filtrable |
| GET | `/{id}` | `CHAIN_TEMPLATES:READ` | Détail + maillons |
| GET | `/by-code/{code}` | `CHAIN_TEMPLATES:READ` | Résolution par code `T01` |
| POST | `/` | `CHAIN_TEMPLATES:CREATE` | Créer template + maillons |
| PUT | `/{id}` | `CHAIN_TEMPLATES:UPDATE` | Modifier en-tête |
| PUT | `/{id}/steps` | `CHAIN_TEMPLATES:UPDATE` | Remplacer tous les maillons |
| PATCH | `/{id}/activate` | `CHAIN_TEMPLATES:UPDATE` | Réactiver |
| PATCH | `/{id}/deactivate` | `CHAIN_TEMPLATES:UPDATE` | Désactiver |
| DELETE | `/{id}` | `CHAIN_TEMPLATES:DELETE` | Supprimer (non système uniquement) |
| POST | `/{id}/duplicate` | `CHAIN_TEMPLATES:CREATE` | Dupliquer (Should) |

### 7.2 Paramètres liste GET `/`

| Paramètre | Type | Description |
|-----------|------|-------------|
| `active` | boolean | Filtrer actifs / inactifs |
| `fileTypeCode` | string | Filtrer par type dossier |
| `search` | string | Recherche code ou nom |
| `page`, `size` | int | Pagination |

### 7.3 Exemples réponses erreur

**Code dupliqué (400)**
```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Chain template code already exists",
  "code": "CHAIN_TEMPLATE_CODE_EXISTS"
}
```

**Template système — suppression (403)**
```json
{
  "status": 403,
  "detail": "System chain templates cannot be deleted",
  "code": "CHAIN_SYSTEM_TEMPLATE_PROTECTED"
}
```

### 7.4 Service `ChainTemplateService` (méthodes)

| Méthode | Description |
|---------|-------------|
| `findAll(filter, pageable)` | Liste scopée |
| `findById(id)` | Détail avec steps (fetch join) |
| `findByCode(code)` | Résolution DOS-06 |
| `create(request)` | Création + validation |
| `updateHeader(id, request)` | Métadonnées sans maillons |
| `replaceSteps(id, steps)` | Transaction : delete orphans + insert |
| `deactivate(id)` | TPL-06 |
| `delete(id)` | TPL-05 |
| `validateSteps(template, steps)` | Règles TPL-01 à TPL-08 |

---

## 8. RBAC et sécurité

### 8.1 Permissions

| Permission | Actions |
|------------|---------|
| `CHAIN_TEMPLATES:READ` | GET liste, GET détail, GET by-code |
| `CHAIN_TEMPLATES:CREATE` | POST, duplicate |
| `CHAIN_TEMPLATES:UPDATE` | PUT, PATCH activate/deactivate |
| `CHAIN_TEMPLATES:DELETE` | DELETE (non système) |

### 8.2 Matrice rôles système (seed RBAC)

| Rôle | READ | CREATE | UPDATE | DELETE |
|------|------|--------|--------|--------|
| `SUPER_ADMIN` | ✓ | ✓ | ✓ | ✓ |
| `BUSINESS_ADMIN` | ✓ | ✓ | ✓ | ✗ |
| `DIRECTOR` | ✓ | ✗ | ✗ | ✗ |
| `SERVICE_HEAD` | ✓ | ✗ | ✗ | ✗ |
| `AGENT` | ✓ | ✗ | ✗ | ✗ |
| `READER` | ✓ | ✗ | ✗ | ✗ |
| Autres | ✓ | ✗ | ✗ | ✗ |

> Lecture ouverte à tous les rôles authentifiés pour consultation du circuit sur fiche dossier.

### 8.3 Garde API

- `@RequiresPermission` sur `ChainTemplateController`
- Pas de filtre périmètre org sur les templates (référentiel **national** MINTP)
- Seuls `BUSINESS_ADMIN` et `SUPER_ADMIN` accèdent aux écrans d'édition frontend

---

## 9. Frontend

### 9.1 Écrans

| Route | Composant | Permission |
|-------|-----------|------------|
| `/admin/chain-templates` | `ChainTemplatesListPage` | `CHAIN_TEMPLATES:READ` |
| `/admin/chain-templates/new` | `ChainTemplateFormPage` | `CHAIN_TEMPLATES:CREATE` |
| `/admin/chain-templates/[id]` | `ChainTemplateDetailPage` | `CHAIN_TEMPLATES:READ` |
| `/admin/chain-templates/[id]/edit` | `ChainTemplateFormPage` | `CHAIN_TEMPLATES:UPDATE` |

Navigation : section **Administration** dans `AppShell`, entrée « Templates de chaîne ».

### 9.2 Liste (`/admin/chain-templates`)

| Élément | Description |
|---------|-------------|
| En-tête | Titre + description + bouton « Nouveau template » |
| Compteur | `{count} template(s)` (comme `/admin/org`) |
| Filtres | Recherche, actif/inactif, type dossier |
| Tableau | Code, nom, type dossier, nb maillons, délai total, statut, badge « Système » |
| Actions ligne | Voir / Modifier / Désactiver |

### 9.3 Formulaire création / édition

**Section en-tête**
- Code (immutable après création)
- Nom, description
- Type dossier lié (select)
- Délai total (j.o.)
- Unité de délai par défaut

**Section maillons** (tableau éditable)
- Ordre (drag & drop ou numéro)
- Libellé
- Rôle responsable (select `UserRole`)
- Délai + unité
- Action attendue (textarea)
- Cases : Optionnel, Maillon clôture

**Validations client**
- Au moins 2 maillons
- Un seul maillon clôture coché
- Somme délais ≤ délai total

### 9.4 Détail lecture seule

- Timeline verticale des maillons (aperçu circuit)
- Métadonnées template
- Boutons : Modifier, Dupliquer, Désactiver (selon droits)
- Badge « Template système » pour T01–T05

### 9.5 i18n (clés proposées)

```
admin.chainTemplates.title
admin.chainTemplates.description
admin.chainTemplates.create
admin.chainTemplates.code
admin.chainTemplates.steps
admin.chainTemplates.responsibleRole
admin.chainTemplates.delayValue
admin.chainTemplates.closureStep
admin.chainTemplates.systemTemplate
```

---

## 10. Données de référence — Seed MINTP

### 10.1 T01 — Courrier entrant standard

| # | Libellé | Rôle | Délai | Unité | Action attendue |
|---|---------|------|-------|-------|-----------------|
| 1 | Réception DAG | `SUPPORT` | 1 | j.o. | Enregistrer et numériser |
| 2 | Orientation Chef Courrier | `SERVICE_HEAD` | 1 | j.o. | Orienter vers direction |
| 3 | Directeur destinataire | `DIRECTOR` | 1 | j.o. | Désigner agent traitant |
| 4 | Agent traitant | `AGENT` | 5 | j.o. | Traiter et préparer réponse |
| 5 | Validation Chef Service | `SERVICE_HEAD` | 2 | j.o. | Valider le projet de réponse |
| 6 | Expédition réponse | `SUPPORT` | 1 | j.o. | Expédier et archiver scan |
| 7 | Clôture | `AGENT` | 0 | clôture | Clôturer le dossier |

`code=T01`, `file_type_code=COUR-STD`, `total_delay_days=11`, `system_template=true`

### 10.2 T02 — Courrier très urgent

| # | Libellé | Rôle | Délai | Unité |
|---|---------|------|-------|-------|
| 1 | Réception DAG | `SUPPORT` | 4 | h |
| 2 | Directeur destinataire | `DIRECTOR` | 4 | h |
| 3 | Agent traitant | `AGENT` | 1 | j.o. |
| 4 | Validation | `SERVICE_HEAD` | 4 | h |
| 5 | Expédition | `SUPPORT` | 4 | h |
| 6 | Clôture | `AGENT` | 0 | clôture |

`code=T02`, `file_type_code=COUR-URG`, `total_delay_days=2`, `delay_unit=WORKING_HOURS`

### 10.3 T03 — Marché public simplifié

| # | Libellé | Rôle | Délai | Unité | Optional |
|---|---------|------|-------|-------|----------|
| 1 | Enregistrement et visa SG | `SUPPORT` | 1 | j.o. | |
| 2 | Instruction technique | `AGENT` | 5 | j.o. | |
| 3 | Visa financier | `SERVICE_HEAD` | 3 | j.o. | |
| 4 | Validation Directeur DIER | `DIRECTOR` | 2 | j.o. | |
| 5 | Avis SG | `SECRETARY_GENERAL` | 3 | j.o. | |
| 6 | Visa Ministre (si requis) | `EXECUTIVE_OFFICE` | 5 | j.o. | ✓ |
| 7 | Notification et archivage | `SUPPORT` | 2 | j.o. | |

`code=T03`, `file_type_code=MARCHE-SMP`, `total_delay_days=15`

### 10.4 T04 — Autorisation travaux DRTP

| # | Libellé | Rôle | Délai |
|---|---------|------|-------|
| 1 | Réception demande | `SUPPORT` | 1 j.o. |
| 2 | Instruction technique | `AGENT` | 7 j.o. |
| 3 | Visite terrain | `SERVICE_HEAD` | 5 j.o. |
| 4 | Validation Directeur DRTP | `REGIONAL_DIRECTOR` | 3 j.o. |
| 5 | Délivrance autorisation | `SUPPORT` | 2 j.o. |
| 6 | Clôture | `AGENT` | 0 |

`code=T04`, `file_type_code=AUTH-TRAV`, `total_delay_days=18`

### 10.5 T05 — Coopération / partenariat

Pré-configuré, `active=false`, `system_template=true` — hors pilote actif.

---

## 11. User stories

### US-TPL-01 — Consulter les templates

**En tant qu'** agent authentifié,  
**je veux** voir la liste des templates de chaîne,  
**afin de** comprendre le circuit applicable à mon type de dossier.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | Agent DAG connecté | GET `/api/admin/chain-templates` | `200`, liste contient T01 |
| AC-02 | Filtre `active=true` | GET avec param | Seuls templates actifs |
| AC-03 | READER | GET détail T01 | `200`, 7 maillons ordonnés |

### US-TPL-02 — Créer un template personnalisé

**En tant qu'** administrateur métier,  
**je veux** créer un nouveau template avec ses maillons,  
**afin d'** adapter FluxPro à un nouveau type de dossier.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | BUSINESS_ADMIN | POST template valide 3 maillons | `201`, template retourné |
| AC-02 | Code `T01` existant | POST même code | `400` CHAIN_TEMPLATE_CODE_EXISTS |
| AC-03 | AGENT | POST | `403` |
| AC-04 | Maillons ordre 1,3 (trou) | POST | `400` CHAIN_STEP_ORDER_GAP |

### US-TPL-03 — Modifier les maillons d'un template système

**En tant qu'** administrateur métier,  
**je veux** ajuster les délais d'un template pilote T01,  
**afin de** refléter la réalité terrain validée en atelier.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | T01 système | PUT steps délai maillon 4 = 6j | `200`, somme ≤ total |
| AC-02 | T01 système | DELETE template | `403` CHAIN_SYSTEM_TEMPLATE_PROTECTED |
| AC-03 | Somme délais > total | PUT steps | `400` CHAIN_DELAY_SUM_EXCEEDED |

### US-TPL-04 — Désactiver un template

**En tant qu'** administrateur métier,  
**je veux** désactiver un template obsolète,  
**afin qu'** il ne soit plus proposé à la création de dossiers.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | Template custom sans dossier actif | PATCH deactivate | `active=false` |
| AC-02 | T03 avec 2 dossiers en cours | PATCH deactivate | `409` CHAIN_TEMPLATE_IN_USE |
| AC-03 | Template inactif | Création dossier type lié | Template non appliqué (S2) |

### US-TPL-05 — Résolution template par type dossier

**En tant que** système (DOS-06),  
**je veux** résoudre le template à partir du `file_type_code`,  
**afin d'** initialiser la chaîne automatiquement.

| AC | Given | When | Then |
|----|-------|------|------|
| AC-01 | Type `COUR-STD` | GET `/by-code` ou lookup | Template T01 actif |
| AC-02 | Type `COUR-URG` | Lookup | Template T02 |
| AC-03 | T05 inactif | Lookup | Non retourné |

---

## 12. Plan de tests techniques

### 12.1 Tests unitaires

| ID | Composant | Scénario |
|----|-----------|----------|
| UT-TPL-01 | `ChainTemplateService.validateSteps` | Ordre consécutif 1..N |
| UT-TPL-02 | `ChainTemplateService.validateSteps` | 0 ou 2 maillons clôture → erreur |
| UT-TPL-03 | `ChainTemplateService.validateSteps` | Somme délais > total → erreur |
| UT-TPL-04 | `ChainTemplateService.delete` | system_template → exception |
| UT-TPL-05 | `ChainTemplateService.deactivate` | dossiers en cours → exception |
| UT-TPL-06 | `ChainTemplateService.replaceSteps` | Remplacement atomique (orphan removal) |

### 12.2 Tests d'intégration API

| ID | Scénario | Résultat |
|----|----------|----------|
| IT-TPL-01 | CRUD template custom SUPER_ADMIN | 201 / 200 / 204 |
| IT-TPL-02 | BUSINESS_ADMIN DELETE custom | 204 |
| IT-TPL-03 | AGENT POST create | 403 |
| IT-TPL-04 | Seed T01 — GET by-code | 7 steps, ordre correct |
| IT-TPL-05 | PUT steps T01 — modification délai | 200, persisted |
| IT-TPL-06 | DELETE T01 | 403 |

### 12.3 Tests frontend E2E

| ID | Scénario |
|----|----------|
| E2E-TPL-01 | Admin ouvre liste → voit T01–T04 |
| E2E-TPL-02 | Création template 3 maillons → visible en liste |
| E2E-TPL-03 | Édition T01 maillon 4 → sauvegarde OK |
| E2E-TPL-04 | Agent standard → pas de bouton « Nouveau template » |

---

## 13. Recette UAT

### 13.1 Objectif recette

Valider avec les **référents métier MINTP** (DAG, DIER, DRTP) que les templates configurés correspondent aux circuits réels et que l'administration est utilisable sans assistance technique.

### 13.2 Environnement et prérequis

| Élément | Exigence |
|---------|----------|
| Environnement | Recette / pré-production |
| Script SQL | `chain_templates` + seed T01–T05 exécuté |
| Comptes test | Voir §13.3 |
| Données | Sprint 2 optionnel pour TPL-UAT-08 à 10 |
| Navigateur | Chrome 90+ ou Edge 90+ |
| Langue | Français |

### 13.3 Personas et comptes de test

| Persona | Rôle FluxPro | Organisation | Usage recette |
|---------|--------------|--------------|---------------|
| Admin métier DAG | `BUSINESS_ADMIN` | DAG | Création / modification templates |
| Super admin DSI | `SUPER_ADMIN` | MINTP | Suppression template custom |
| Directeur DAG | `DIRECTOR` | DAG | Lecture seule |
| Agent courrier | `AGENT` | DAG | Lecture liste (consultation) |
| Référent DIER | `BUSINESS_ADMIN` | DIER | Validation circuit T03 |
| Référent DRTP | `BUSINESS_ADMIN` | DRTP-C | Validation circuit T04 |

### 13.4 Matrice de recette UAT

Légende résultat : **OK** / **KO** / **N/A** — colonne « Observations » pour écarts.

#### Bloc A — Consultation (tous profils)

| ID | Scénario | Étapes | Résultat attendu | OK/KO | Obs. |
|----|----------|--------|------------------|-------|------|
| TPL-UAT-01 | Liste templates | 1. Login admin DAG<br>2. Menu Administration → Templates de chaîne | Liste affiche T01, T02, T03, T04 ; T05 absent ou inactif | | |
| TPL-UAT-02 | Détail T01 | 1. Cliquer sur T01 | 7 maillons dans l'ordre ; délai total 11 j.o. ; badge Système | | |
| TPL-UAT-03 | Détail T02 | 1. Ouvrir T02 | 6 maillons ; unités heures sur maillons 1,2,4,5 | | |
| TPL-UAT-04 | Lecture agent | 1. Login agent courrier<br>2. Accéder liste templates | Liste visible ; **pas** de bouton Créer/Modifier | | |
| TPL-UAT-05 | Filtre recherche | 1. Rechercher « marché » | Seul T03 affiché | | |

#### Bloc B — Administration templates (BUSINESS_ADMIN)

| ID | Scénario | Étapes | Résultat attendu | OK/KO | Obs. |
|----|----------|--------|------------------|-------|------|
| TPL-UAT-06 | Créer template custom | 1. Nouveau template `T06-TEST`<br>2. 3 maillons valides<br>3. Enregistrer | Template créé ; visible en liste | | |
| TPL-UAT-07 | Validation ordre maillons | 1. Créer template avec maillons 1 et 3 (sans 2) | Message erreur explicite ; enregistrement refusé | | |
| TPL-UAT-08 | Modifier délai T01 | 1. Éditer T01<br>2. Maillon 4 : 5j → 6j<br>3. Ajuster total à 12j<br>4. Sauvegarder | Modification persistée après rechargement | | |
| TPL-UAT-09 | Maillon clôture unique | 1. Tenter 2 maillons clôture | Erreur validation | | |
| TPL-UAT-10 | Désactiver template custom | 1. Désactiver T06-TEST | Statut inactif ; badge visible | | |
| TPL-UAT-11 | Réactiver template | 1. Réactiver T06-TEST | Statut actif | | |

#### Bloc C — Protection et sécurité

| ID | Scénario | Étapes | Résultat attendu | OK/KO | Obs. |
|----|----------|--------|------------------|-------|------|
| TPL-UAT-12 | Suppression template système | 1. Login SUPER_ADMIN<br>2. Tenter supprimer T01 | Action refusée ; message clair | | |
| TPL-UAT-13 | Suppression template custom | 1. SUPER_ADMIN supprime T06-TEST | Template disparaît de la liste | | |
| TPL-UAT-14 | BUSINESS_ADMIN suppression | 1. Login BUSINESS_ADMIN<br>2. Tenter supprimer template custom | Refusé (403 ou bouton absent) | | |
| TPL-UAT-15 | Accès non authentifié | 1. Ouvrir URL `/admin/chain-templates` sans login | Redirection `/login` | | |

#### Bloc D — Validation métier MINTP (atelier référents)

| ID | Scénario | Validateur | Résultat attendu | OK/KO | Obs. |
|----|----------|------------|------------------|-------|------|
| TPL-UAT-16 | Circuit T01 conforme DAG | Référent DAG | Maillons et délais validés en atelier | | |
| TPL-UAT-17 | Circuit T03 conforme DIER | Référent DIER | 7 maillons cohérents ; visa Ministre optionnel | | |
| TPL-UAT-18 | Circuit T04 conforme DRTP | Référent DRTP | Maillon visite terrain présent ; 18 j.o. | | |
| TPL-UAT-19 | Rôles responsables cohérents | PO + référents | Rôles correspondent à l'organigramme | | |
| TPL-UAT-20 | Point ouvert T03 (15 vs 21 j) | Comité pilotage | Décision documentée | | |

#### Bloc E — Intégration Sprint 2 (si dossiers disponibles)

| ID | Scénario | Étapes | Résultat attendu | OK/KO | Obs. |
|----|----------|--------|------------------|-------|------|
| TPL-UAT-21 | Lien type → template | 1. Créer dossier COUR-STD | Template T01 appliqué automatiquement | | |
| TPL-UAT-22 | Template inactif | 1. Désactiver template lié<br>2. Créer dossier | Erreur ou sélection manuelle — comportement documenté | | |
| TPL-UAT-23 | Désactivation avec dossiers en cours | 1. Dossier en cours sur T03<br>2. Tenter désactiver T03 | Refus avec message « dossiers en cours » | | |

### 13.5 Critères de sortie recette (Go / No-Go)

| Critère | Seuil Go |
|---------|----------|
| Bloc A (consultation) | 100 % OK |
| Bloc B (administration) | ≥ 90 % OK, aucun KO bloquant |
| Bloc C (sécurité) | 100 % OK |
| Bloc D (validation métier) | T01, T03, T04 validés par référents |
| Bloc E (intégration S2) | ≥ 80 % OK si S2 livré ; sinon N/A |
| Régressions Sprint 1 | Aucune régression auth/org/users |

**KO bloquant :** suppression T01 réussie, création template sans validation, agent peut modifier templates, circuits pilote non validés par référents.

### 13.6 Fiche de levée de réserve

| ID KO | Description | Priorité | Correction | Retest | Validé par | Date |
|-------|-------------|----------|------------|--------|------------|------|
| | | P1/P2/P3 | | TPL-UAT-XX | | |

### 13.7 Procès-verbal de recette (modèle)

```
PROCÈS-VERBAL DE RECETTE UAT — Module CHN-TPL
Projet : FluxPro MINTP
Date : __ / __ / 2026
Environnement : RECETTE
Présents : [Référent DAG] [Référent DIER] [Référent DRTP] [PO] [QA]

Synthèse :
- Cas exécutés : __ / 23
- OK : __  |  KO : __  |  N/A : __
- Taux de succès : __ %

Décision :
☐ RECETTE ACCEPTÉE — mise en production autorisée
☐ RECETTE ACCEPTÉE AVEC RÉSERVES — voir fiche réserves
☐ RECETTE REFUSÉE — corrections requises

Signatures :
Référent métier DAG : _______________  Date : _______
Référent métier DIER : ______________  Date : _______
Product Owner : ____________________  Date : _______
```

---

## 14. Hors périmètre

| Sujet | Module / Sprint |
|-------|-----------------|
| **Moteur BPM générique** | **Refusé** — templates + étapes parallèles ([décision](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md)) |
| Exécution passation (transmit/return) | CHN-PASS |
| Calcul échéances runtime | DEL |
| Alertes et escalades | Sprint 4 |
| Import CSV templates | Phase 2 |
| Versioning historique templates | Phase 2 |
| Templates multi-tenant (hors MINTP) | Déploiements futurs |

---

## 15. Definition of Done

### Backend
- [ ] Script SQL `docs/sql/2026-XX-XX_chain_templates.sql` exécuté manuellement
- [ ] Entités JPA + repositories + service + controller
- [ ] Permissions `CHAIN_TEMPLATES:*` seedées dans RBAC
- [ ] `ChainDataInitializer` — seed T01–T05
- [ ] UT-TPL-01 à UT-TPL-06 verts
- [ ] IT-TPL-01 à IT-TPL-06 verts
- [ ] OpenAPI documenté

### Frontend
- [ ] Pages liste / détail / formulaire
- [ ] i18n fr/en
- [ ] Navigation AppShell
- [ ] E2E-TPL-01 à E2E-TPL-04 verts

### Recette
- [ ] UAT Blocs A–D exécutés
- [ ] PV de recette signé
- [ ] Réserves P1 levées

---

*Spécification CHN-TPL v1.0 — FluxPro MINTP — Juillet 2026*
