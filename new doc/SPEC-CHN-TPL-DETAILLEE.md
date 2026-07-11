# Spécification détaillée — Module Templates de chaîne (CHN-TPL)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique
**Module :** Templates de chaîne de passation (CHN-TPL)
**Version :** 1.0
**Date :** 10 juillet 2026
**Source :** Rétro-documentation à partir du code source réellement implémenté (`entity/ChainTemplate`, `entity/ChainStepTemplate`, `service/ChainTemplateService`, `service/ChainTemplateUsageService`, `service/PassageStageHelper`, `controller/ChainTemplateController`, `config/ChainDataInitializer` du backend Spring Boot ; pages `/admin/chain-templates` et composants `ChainTemplateFormPage`, `ChainCircuitWizard` du frontend Next.js)

---

## Table des matières

1. [Objectif et périmètre](#1-objectif-et-périmètre)
2. [Modèle de données](#2-modèle-de-données)
3. [Cas d'utilisation](#3-cas-dutilisation)
4. [Règles de gestion](#4-règles-de-gestion)

---

## 1. Objectif et périmètre

### 1.1. Objectif

Le module Templates de chaîne a pour objectif de :

- **Définir** des modèles réutilisables de circuits de passation (templates T01–T05 système, templates personnalisés, template parallèle T06) associés à un type de dossier ;
- **Structurer** chaque circuit en **étapes** (`step_order`) composées d'un ou plusieurs **maillons** (libellé, rôle responsable, délai, action attendue, optionnel, clôture) ;
- **Autoriser le parallélisme** : plusieurs maillons peuvent partager le même numéro d'étape ; la jonction entre étapes est de type **AND** (l'étape suivante n'est activée que lorsque tous les maillons de l'étape courante sont terminés — logique consommée par CHN-PASS) ;
- **Valider** la cohérence métier des maillons (ordre d'étapes, clôture unique, budget de délai, rôles) avant toute persistance ;
- **Administrer** le référentiel via une API dédiée et une interface d'administration (`/admin/chain-templates`) ;
- **Initialiser** les templates de référence MINTP au démarrage (seed Java) et via scripts SQL manuels (parallélisme, T06).

### 1.2. Périmètre

**Inclus dans le module :**

| Bloc | Contenu |
|---|---|
| Référentiel des templates | CRUD en-tête (`code`, nom, description, type de dossier, délai total, unité, actif, système) |
| Maillons / étapes | Remplacement atomique de la liste des maillons ; étapes séquentielles ou parallèles (même `step_order`) |
| Consultation | Liste paginée filtrée, détail par id ou par code, aperçu circuit (frontend) |
| Cycle de vie | Activation, désactivation conditionnelle, suppression conditionnelle, duplication |
| Validation métier | Règles TPL sur étapes, clôture, délais (max par étape), rôles |
| Amorçage (seed) | T01–T05 au démarrage (`ChainDataInitializer`) ; T06 parallèle via SQL |
| Garde technique | Permissions RBAC `CHAIN_TEMPLATES:*` |

**Explicitement hors périmètre du module (traité ailleurs) :**

- Instanciation d'une chaîne sur un dossier, transmission, retour, suspension (`FilePassage`) — module **CHN-PASS** (`PassageService`) ;
- Calcul des échéances calendaires — module **DEL** (`DelaiService`) ;
- Règles d'alerte liées à un template — module **ALR** (API imbriquée `/api/admin/chain-templates/{id}/alert-rules`, hors description détaillée ici) ;
- CRUD des types de dossier (`file_types`) — module **DOS** / référentiel types ; ce module ne stocke qu'un `file_type_code` (lien souple, sans FK) ;
- Résolution automatique du template à la soumission d'un dossier (`FileService.resolveTemplate`) — consommateur de ce module, décrit dans DOS/CHN-PASS.

---

## 2. Modèle de données

### 2.1. Entités

Toutes les entités de ce module héritent de `BaseEntity` :

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `id` | `UUID` | généré automatiquement | Identifiant technique unique (stockage `BINARY(16)`) |
| `createdAt` | `Instant` | date/heure courante à la création | Horodatage de création (non modifiable) |
| `updatedAt` | `Instant` | date/heure courante | Horodatage de dernière modification |

#### 2.1.1. `ChainTemplate` (table `chain_templates`)

Modèle réutilisable décrivant le circuit type d'un dossier. Identifié par un code métier stable (ex. `T01`, `T06`).

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `code` | `String(10)` | — | Code unique ; normalisé en majuscules à la création ; immuable après création |
| `name` | `String` | — | Libellé du template |
| `description` | `String` (TEXT) | `null` | Description libre |
| `fileTypeCode` | `String(32)` | `null` | Code du type de dossier associé (lien souple vers `file_types.code`, sans FK) |
| `totalDelayDays` | `int` | — | Budget de délai total du circuit, exprimé en jours ouvrés (référence pour la validation des maillons) |
| `delayUnit` | `DelayUnit` | `WORKING_DAYS` | Unité d'affichage / référence de l'en-tête (les maillons portent leur propre unité) |
| `active` | `boolean` | `true` | Statut ; un template inactif n'est pas proposé à l'instanciation sur dossier |
| `systemTemplate` | `boolean` | `false` | `true` = template système (T01–T05) : non supprimable |
| `steps` | Liste `ChainStepTemplate` | vide | Maillons du circuit ; cascade ALL + orphanRemoval ; ordonnés par `stepOrder` |

#### 2.1.2. `ChainStepTemplate` (table `chain_step_templates`)

Maillon abstrait du circuit. À l'instanciation (CHN-PASS), chaque maillon devient une `FilePassage` avec un responsable concret.

| Champ | Type | Défaut | Description |
|---|---|---|---|
| `chainTemplate` | FK → `ChainTemplate` | — | Template parent (obligatoire) ; suppression en cascade |
| `stepOrder` | `int` | — | **Numéro d'étape** (≥ 1). Plusieurs maillons peuvent partager la même valeur → étape parallèle |
| `label` | `String` | — | Libellé affiché (ex. « Visa technique DIER ») |
| `responsibleRole` | `UserRole` (enum) | — | Rôle RBAC attendu pour le responsable du maillon |
| `delayValue` | `int` | — | Délai alloué au maillon (≥ 0) |
| `delayUnit` | `DelayUnit` | `WORKING_DAYS` | Unité du délai du maillon |
| `expectedAction` | `String(500)` | `null` | Action attendue (aide contextuelle) |
| `optional` | `boolean` | `false` | Maillon optionnel (métier) |
| `closureStep` | `boolean` | `false` | Maillon de clôture du circuit ; exactement un par template |

### 2.2. Relations

| Relation | Description |
|---|---|
| `chain_templates` (1) ↔ (N) `chain_step_templates` | Chaque template possède au moins deux maillons ; suppression du template entraîne celle des maillons |
| `chain_templates` (1) ↔ (N) `files` | Consommateur DOS/CHN-PASS : un dossier peut référencer un template instancié |
| `chain_templates` (1) ↔ (N) `alert_rules` | Consommateur ALR : chaque règle d'alerte appartient à un template |
| `chain_step_templates` (1) ↔ (N) `file_passages` | Consommateur CHN-PASS : instance runtime d'un maillon |

> Aucune table de jointure dans ce module : relation one-to-many directe. Le lien vers le type de dossier est une **chaîne** (`file_type_code`), pas une clé étrangère.

### 2.3. Énumérations

#### `DelayUnit`

| Valeur | Signification |
|---|---|
| `WORKING_DAYS` | Jours ouvrés |
| `WORKING_HOURS` | Heures ouvrées (conversion validation : 9 h = 1 j.o.) |

#### `UserRole` (utilisé comme `responsible_role` d'un maillon)

| Valeur |
|---|
| `SUPER_ADMIN`, `BUSINESS_ADMIN`, `EXECUTIVE_OFFICE`, `SECRETARY_GENERAL`, `DIRECTOR`, `SERVICE_HEAD`, `AGENT`, `SUPPORT`, `READER`, `REGIONAL_DIRECTOR` |

Toute valeur de l'enum est acceptée à la validation template ; le frontend expose la même liste dans le formulaire.

### 2.4. Concepts d'étape et de parallélisme

```
Étape 1 ──► Étape 2 (parallèle) ──► Étape 3 ──► Étape 4 (clôture)
              ├─ Visa technique
              ├─ Visa financier
              └─ Avis juridique
                    │
                    └── Join AND : tous COMPLETED avant d'activer l'étape 3
```

| Concept | Règle |
|---|---|
| Étape | Ensemble des maillons partageant le même `step_order` |
| Séquentiel | Une seule maillon par étape |
| Parallèle | Plusieurs maillons, même `step_order` |
| Join AND | (CHN-PASS) étape suivante activée seulement si tous les maillons de l'étape courante sont `COMPLETED` |
| Clôture | Exactement un maillon `closureStep=true`, seul dans la **dernière** étape, délai 0 |
| Budget délai | Somme des **maxima** de délai (en j.o.) de chaque étape hors clôture ≤ `totalDelayDays` |

### 2.5. Schéma SQL (évolution)

| Script | Rôle |
|---|---|
| `docs/sql/2026-07-02_chain_templates.sql` | Création des tables ; unicité initiale `(chain_template_id, step_order)` |
| `docs/sql/2026-07-08_chain_parallel_stages.sql` | Suppression de cette unicité ; unicité passages `(file_id, chain_step_template_id)` |
| `docs/sql/2026-07-08_seed_chain_template_parallel.sql` | Seed template `T06` avec étape 2 parallèle |

---

## 3. Cas d'utilisation

### UC-01 — Lister / rechercher les templates

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:READ` |
| Objectif | Obtenir la liste paginée des templates (actifs et/ou inactifs) |
| Préconditions | Authentifié avec la permission de lecture |
| Déroulement nominal | 1. `GET /api/admin/chain-templates?active=&fileTypeCode=&search=&page=&size=`.<br>2. Filtre optionnel par statut, type de dossier, recherche textuelle.<br>3. Retourne une page de `ChainTemplateSummaryResponse` (dont `stepCount`). |
| Exemple | Filtrer `active=true&fileTypeCode=COUR-STD` pour ne voir que les circuits courrier actifs |
| Règles (code) | RG-TPL-14 — `ChainTemplateService.findAllSummaries()`, `ChainTemplateController.list()` |

### UC-02 — Consulter un template (détail)

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:READ` |
| Objectif | Voir l'en-tête et tous les maillons ordonnés d'un template |
| Préconditions | Template existant |
| Déroulement nominal | 1. `GET /api/admin/chain-templates/{id}` **ou** `GET /api/admin/chain-templates/by-code/{code}`.<br>2. Retourne `ChainTemplateDetailResponse` avec la liste des `ChainStepTemplateResponse`. |
| Exemple | `GET .../by-code/T06` → détail du circuit « Marché — visas parallèles » avec trois maillons en étape 2 |
| Règles (code) | RG-TPL-15 — `ChainTemplateService.findById()` / `findByCode()` |

### UC-03 — Créer un template personnalisé

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:CREATE` (typiquement `SUPER_ADMIN`, `BUSINESS_ADMIN`) |
| Objectif | Créer un nouveau circuit (hors seed système) |
| Préconditions | Code libre ; au moins deux maillons valides |
| Déroulement nominal | 1. `POST /api/admin/chain-templates` avec en-tête + `steps[]`.<br>2. Vérifie l'unicité du code (insensible à la casse), normalise le code en majuscules.<br>3. Force `systemTemplate=false`, `active=true`.<br>4. Exécute `validateSteps` (RG-TPL-01 à RG-TPL-07).<br>5. Persiste le template et ses maillons ; retourne le détail (`201`). |
| Exemple | Code déjà pris → `400 CHAIN_TEMPLATE_CODE_EXISTS` |
| Règles (code) | RG-TPL-01 à RG-TPL-08, RG-TPL-16 — `ChainTemplateService.create()` |

### UC-04 — Modifier l'en-tête d'un template

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:UPDATE` |
| Objectif | Mettre à jour nom, description, type de dossier, délai total, unité (sans toucher au code ni aux maillons) |
| Préconditions | Template existant |
| Déroulement nominal | 1. `PUT /api/admin/chain-templates/{id}`.<br>2. Applique les champs d'en-tête.<br>3. **Revalide** les maillons existants contre le nouvel en-tête (ex. baisse de `totalDelayDays` pouvant faire échouer RG-TPL-07).<br>4. Persiste. |
| Exemple | Passer `totalDelayDays` de 11 à 2 alors que la somme des max d'étapes vaut 10 → `400 CHAIN_DELAY_SUM_EXCEEDED` |
| Règles (code) | RG-TPL-07, RG-TPL-09 — `ChainTemplateService.updateHeader()` |

### UC-05 — Remplacer les maillons d'un template

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:UPDATE` |
| Objectif | Remplacer atomiquement toute la liste des maillons (réordonnancement, parallélisme, libellés…) |
| Préconditions | Template existant ; nouvelle liste valide |
| Déroulement nominal | 1. `PUT /api/admin/chain-templates/{id}/steps` avec le corps `List<ChainStepTemplateRequest>`.<br>2. `validateSteps`.<br>3. Vide la collection (`clear` + orphanRemoval) et recrée les entités maillons.<br>4. Retourne le détail à jour. |
| Exemple | Deux maillons avec `stepOrder=2` → étape parallèle acceptée si clôture seule en dernière étape |
| Règles (code) | RG-TPL-01 à RG-TPL-07, RG-TPL-10 — `ChainTemplateService.replaceSteps()` |

### UC-06 — Activer un template

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:UPDATE` |
| Objectif | Rendre un template à nouveau sélectionnable pour l'instanciation |
| Préconditions | Template existant |
| Déroulement nominal | 1. `PATCH /api/admin/chain-templates/{id}/activate`.<br>2. Passe `active=true`. |
| Exemple | Réactivation de T05 (seedé inactif) après validation métier |
| Règles (code) | — `ChainTemplateService.activate()` |

### UC-07 — Désactiver un template

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:UPDATE` |
| Objectif | Retirer un template du catalogue actif sans le supprimer |
| Préconditions | Aucun dossier en statut `IN_PROGRESS` ou `ON_HOLD` n'utilise ce template |
| Déroulement nominal | 1. `PATCH /api/admin/chain-templates/{id}/deactivate`.<br>2. Si `ChainTemplateUsageService.hasInProgressFiles` → refus `409`.<br>3. Sinon `active=false`. |
| Exemple | Template encore lié à un dossier en cours → `409 CHAIN_TEMPLATE_IN_USE` |
| Règles (code) | RG-TPL-11 — `ChainTemplateService.deactivate()`, `ChainTemplateUsageService` |

### UC-08 — Supprimer un template

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:DELETE` (matrice par défaut : `SUPER_ADMIN` uniquement) |
| Objectif | Supprimer physiquement un template personnalisé |
| Préconditions | Template non système (`systemTemplate=false`) |
| Déroulement nominal | 1. `DELETE /api/admin/chain-templates/{id}`.<br>2. Si `systemTemplate=true` → `403 CHAIN_SYSTEM_TEMPLATE_PROTECTED`.<br>3. Sinon suppression (cascade maillons) → `204`. |
| Exemple | Tentative de suppression de T01 → `403` |
| Règles (code) | RG-TPL-12 — `ChainTemplateService.delete()` |

### UC-09 — Dupliquer un template

| Champ | Détail |
|---|---|
| Acteur | Utilisateur avec permission `CHAIN_TEMPLATES:CREATE` |
| Objectif | Créer une copie éditable d'un template existant (système ou non) |
| Préconditions | Template source existant |
| Déroulement nominal | 1. `POST /api/admin/chain-templates/{id}/duplicate`.<br>2. Génère un code `{code}-COPY` (puis `-COPY1`, `-COPY2`… si collision).<br>3. Nom = `"{name} (copie)"` ; `active=true` ; `systemTemplate=false`.<br>4. Copie les maillons ; retourne le détail (`201`). |
| Exemple | Duplication de T01 → code `T01-COPY`, template personnalisé modifiable |
| Règles (code) | RG-TPL-13 — `ChainTemplateService.duplicate()` |

### UC-10 — Configurer une étape parallèle (join AND) dans le formulaire admin

| Champ | Détail |
|---|---|
| Acteur | Administrateur métier (`CHAIN_TEMPLATES:CREATE` ou `UPDATE`) via l'UI |
| Objectif | Définir plusieurs maillons sur le même numéro d'étape |
| Préconditions | Formulaire de création ou d'édition ouvert |
| Déroulement nominal | 1. Sur `/admin/chain-templates/new` ou `.../{id}/edit`, l'utilisateur ajoute un maillon (nouvelle étape) ou clique sur « Maillon parallèle » pour dupliquer le `stepOrder` de la ligne courante.<br>2. La validation client (miroir backend) vérifie étapes consécutives, clôture seule en dernière étape, budget délai (max par étape).<br>3. À la soumission : création (UC-03) ou `updateHeader` + `replaceSteps` (UC-04 + UC-05).<br>4. L'aperçu `ChainCircuitWizard` regroupe les maillons par étape et affiche le libellé « Maillons parallèles (jonction ET) ». |
| Exemple | Étape 2 : Visa DIER + Visa DAF + Avis juridique ; délai compté = max(5, 3, 4) = 5 j.o. |
| Règles (code) | RG-TPL-01 à RG-TPL-07 — `ChainTemplateFormPage.tsx`, `ChainCircuitWizard.tsx` |

### UC-11 — Amorçage des templates de référence (seed)

| Champ | Détail |
|---|---|
| Acteur | Système (démarrage applicatif) / administrateur DBA (scripts SQL) |
| Objectif | Garantir la présence des circuits MINTP de référence |
| Préconditions | Tables `chain_templates` / `chain_step_templates` créées ; pour T06, script parallèle déjà exécuté |
| Déroulement nominal | 1. Au démarrage, `ChainDataInitializer` (@Order 20) crée T01–T05 s'ils sont absents (`systemTemplate=true` ; T05 seedé `active=false`).<br>2. Optionnellement, exécution manuelle de `docs/sql/2026-07-08_seed_chain_template_parallel.sql` pour T06 (étape 2 à trois maillons parallèles, `systemTemplate=false`). |
| Exemple | Premier démarrage → T01 Courrier entrant standard (7 maillons séquentiels) disponible |
| Règles (code) | RG-TPL-12, RG-DATA-01 — `ChainDataInitializer`, scripts `docs/sql/` |

---

## 4. Règles de gestion

| ID | Règle | Composant (code) |
|---|---|---|
| RG-TPL-01 | Un template doit comporter **au moins deux** maillons. | `ChainTemplateService.validateSteps()` → `CHAIN_STEP_MIN_COUNT` |
| RG-TPL-02 | Les numéros d'étape distincts (`step_order`) doivent être **consécutifs de 1 à N** (pas de trou). Plusieurs maillons peuvent partager le même numéro (parallélisme). | `validateSteps()` + `PassageStageHelper.distinctStagesFromRequests()` → `CHAIN_STEP_ORDER_GAP` |
| RG-TPL-03 | Exactement **un** maillon de clôture (`closureStep=true`) est requis par template. | `validateSteps()` → `CHAIN_CLOSURE_STEP_INVALID` |
| RG-TPL-04 | Le maillon de clôture doit avoir un **délai nul** (`delayValue=0`). | `validateSteps()` → `CHAIN_CLOSURE_DELAY_INVALID` |
| RG-TPL-05 | Le maillon de clôture doit être le **seul** maillon de la **dernière** étape (pas de parallèle sur la clôture). | `validateSteps()` → `CHAIN_PARALLEL_CLOSURE_INVALID` |
| RG-TPL-06 | Chaque `responsibleRole` doit être une valeur valide de l'enum `UserRole`. | `validateSteps()` → `CHAIN_INVALID_ROLE` |
| RG-TPL-07 | La somme des délais, calculée comme **maximum** des délais (convertis en jours ouvrés) de chaque étape **hors clôture**, ne doit pas dépasser `totalDelayDays`. Conversion : `WORKING_HOURS` → `delayValue / 9`. | `validateSteps()`, `toWorkingDays()` → `CHAIN_DELAY_SUM_EXCEEDED` |
| RG-TPL-08 | Le code template est unique (comparaison insensible à la casse) et stocké en majuscules. | `ChainTemplateService.create()` → `CHAIN_TEMPLATE_CODE_EXISTS` |
| RG-TPL-09 | La modification de l'en-tête **revalide** les maillons existants contre le nouvel en-tête (cohérence du budget de délai). | `ChainTemplateService.updateHeader()` |
| RG-TPL-10 | Le remplacement des maillons est **atomique** : ancienne liste vidée (orphanRemoval), nouvelle liste validée puis persistée. | `ChainTemplateService.replaceSteps()` |
| RG-TPL-11 | La désactivation est refusée si au moins un dossier lié est en statut `IN_PROGRESS` ou `ON_HOLD`. | `ChainTemplateUsageService.hasInProgressFiles()`, `deactivate()` → `CHAIN_TEMPLATE_IN_USE` |
| RG-TPL-12 | Un template système (`systemTemplate=true`) ne peut pas être supprimé. | `ChainTemplateService.delete()` → `CHAIN_SYSTEM_TEMPLATE_PROTECTED` |
| RG-TPL-13 | La duplication produit toujours un template **non système**, actif, avec un code dérivé unique `{code}-COPY[n]` et le suffixe « (copie) » dans le nom. | `ChainTemplateService.duplicate()` |
| RG-TPL-14 | Toute lecture de la liste ou du détail exige la permission `CHAIN_TEMPLATES:READ`. | `@RequiresPermission`, `RbacDataInitializer` |
| RG-TPL-15 | La création / duplication exige `CHAIN_TEMPLATES:CREATE` ; la mise à jour d'en-tête, le remplacement de maillons, l'activation et la désactivation exigent `CHAIN_TEMPLATES:UPDATE` ; la suppression exige `CHAIN_TEMPLATES:DELETE`. | `ChainTemplateController`, matrice RBAC |
| RG-TPL-16 | Matrice par défaut : `SUPER_ADMIN` = toutes les permissions ; `BUSINESS_ADMIN` = READ + CREATE + UPDATE (pas DELETE) ; les autres rôles = READ uniquement. | `RbacDataInitializer` |
| RG-TPL-17 | À l'exécution (CHN-PASS), une étape parallèle est activée en bloc ; l'étape suivante n'est activée que lorsque **tous** les maillons de l'étape courante sont `COMPLETED` (join AND). | `PassageService`, `PassageStageHelper` |
| RG-DATA-01 | Le schéma est géré exclusivement par scripts SQL manuels (`docs/sql/`) ; `spring.jpa.hibernate.ddl-auto` reste à `none`. L'activation du parallélisme nécessite l'exécution de `2026-07-08_chain_parallel_stages.sql` avant tout seed / usage multi-maillons par étape. | Règle projet transverse |
| RG-DATA-02 | Le lien template ↔ type de dossier est un code (`file_type_code`), sans contrainte d'intégrité référentielle en base. | `ChainTemplate.fileTypeCode` |

---

### Annexe A — Permissions RBAC

| Permission | Description |
|---|---|
| `CHAIN_TEMPLATES:READ` | Lister et consulter les templates |
| `CHAIN_TEMPLATES:CREATE` | Créer et dupliquer |
| `CHAIN_TEMPLATES:UPDATE` | Modifier en-tête / maillons, activer, désactiver |
| `CHAIN_TEMPLATES:DELETE` | Supprimer un template non système |

### Annexe B — Codes d'erreur i18n

| Code | HTTP typique | Signification |
|---|---|---|
| `CHAIN_TEMPLATE_NOT_FOUND` | 404 | Template introuvable (id) |
| `CHAIN_TEMPLATE_NOT_FOUND_BY_CODE` | 404 | Template introuvable (code) |
| `CHAIN_TEMPLATE_CODE_EXISTS` | 400 | Code déjà utilisé |
| `CHAIN_TEMPLATE_IN_USE` | 409 | Désactivation impossible (dossiers en cours) |
| `CHAIN_SYSTEM_TEMPLATE_PROTECTED` | 403 | Template système non supprimable |
| `CHAIN_STEP_MIN_COUNT` | 400 | Moins de deux maillons |
| `CHAIN_STEP_ORDER_GAP` | 400 | Étapes non consécutives |
| `CHAIN_CLOSURE_STEP_INVALID` | 400 | Nombre de clôtures ≠ 1 |
| `CHAIN_CLOSURE_DELAY_INVALID` | 400 | Délai de clôture ≠ 0 |
| `CHAIN_PARALLEL_CLOSURE_INVALID` | 400 | Clôture non seule en dernière étape |
| `CHAIN_INVALID_ROLE` | 400 | Rôle responsable invalide |
| `CHAIN_DELAY_SUM_EXCEEDED` | 400 | Budget de délai dépassé |

### Annexe C — Templates de référence (seed)

| Code | Nom | Type dossier | Délai | Système | Actif | Particularité |
|---|---|---|---|---|---|---|
| T01 | Courrier entrant standard | COUR-STD | 11 j.o. | oui | oui | 7 maillons séquentiels |
| T02 | Courrier très urgent | COUR-URG | 3 (unité h) | oui | oui | Circuit accéléré |
| T03 | Marché public simplifié | MARCHE-SMP | 21 j.o. | oui | oui | Maillon optionnel visa Ministre |
| T04 | Autorisation travaux DRTP | AUTH-TRAV | 18 j.o. | oui | oui | Rôle `REGIONAL_DIRECTOR` |
| T05 | Coopération / partenariat | COOP-PART | 10 j.o. | oui | **non** | Hors pilote |
| T06 | Marché — visas parallèles | MARCHE-SMP | 8 j.o. | non | oui | Étape 2 : 3 maillons parallèles (SQL) |

### Annexe D — Endpoints API

Base : `/api/admin/chain-templates`

| Méthode | Chemin | Permission |
|---|---|---|
| `GET` | `/` | `READ` |
| `GET` | `/{id}` | `READ` |
| `GET` | `/by-code/{code}` | `READ` |
| `POST` | `/` | `CREATE` |
| `PUT` | `/{id}` | `UPDATE` |
| `PUT` | `/{id}/steps` | `UPDATE` |
| `PATCH` | `/{id}/activate` | `UPDATE` |
| `PATCH` | `/{id}/deactivate` | `UPDATE` |
| `DELETE` | `/{id}` | `DELETE` |
| `POST` | `/{id}/duplicate` | `CREATE` |

### Annexe E — Pages frontend

| Route | Rôle |
|---|---|
| `/admin/chain-templates` | Liste + filtres + actions |
| `/admin/chain-templates/new` | Création |
| `/admin/chain-templates/[id]` | Détail + aperçu circuit + onglet alertes |
| `/admin/chain-templates/[id]/edit` | Édition en-tête + maillons |

---

*Document généré à partir de l'analyse du code source du module (`entity/`, `service/ChainTemplateService.java`, `service/PassageStageHelper.java`, `controller/ChainTemplateController.java`, `config/ChainDataInitializer.java`, `docs/sql/2026-07-08_*.sql`, frontend `ChainTemplateFormPage.tsx` / `ChainCircuitWizard.tsx`) — à maintenir en cohérence avec toute évolution du code.*
