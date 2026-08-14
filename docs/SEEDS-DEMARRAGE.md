# Seeds de données au démarrage

**Projet :** FluxPro — backend Spring Boot  
**Date :** août 2026  
**Périmètre :** initializers exécutés après le démarrage du contexte (`CommandLineRunner` / `ApplicationRunner`)

Ce document décrit **en détail** ce que le backend écrit en base (ou en cache) au boot.  
Le schéma SQL n’est **jamais** modifié automatiquement (`spring.jpa.hibernate.ddl-auto=none`) : les tables doivent déjà exister via les scripts `docs/sql/`.

---

## Table des matières

1. [Règles générales](#1-règles-générales)
2. [Ordre d’exécution](#2-ordre-dexécution)
3. [TenantSettingsDataInitializer](#3-tenantsettingsdatainitializer)
4. [DataInitializer (org + admin)](#4-datainitializer-org--admin)
5. [RbacDataInitializer](#5-rbacdatainitializer)
6. [ChainDataInitializer](#6-chaindatainitializer)
7. [FileTypeDataInitializer](#7-filetypedatainitializer)
8. [AlertTypeDataInitializer](#8-alerttypedatainitializer)
9. [AlertDigestRecipientRoleDataInitializer](#9-alertdigestrecipientroledatainitializer)
10. [Ce qui n’est pas seedé](#10-ce-qui-nest-pas-seedé)
11. [Points d’attention](#11-points-dattention)
12. [Fichiers sources](#12-fichiers-sources)

---

## 1. Règles générales

| Comportement | Seeds concernés |
|--------------|-----------------|
| **Insert si absent** (`findByCode` / `count() == 0`) | Tenant, org / types d’org, types de dossiers, templates (par code), types d’alertes, rôles digest |
| **Réécrit à chaque démarrage** | Matrice permissions des rôles RBAC + sync du rôle primaire de **tous** les users |
| **En cas d’erreur SQL / table manquante** | La plupart loguent un `warn` et skippent (indiquent le script SQL à exécuter) |

**Hors seeds data (mais au boot) :** `@PostConstruct` sur horloge, jobs cron, templates e-mail Thymeleaf, KB assistant (`help-kb.md`) — voir aussi le comportement runtime dans le code.

---

## 2. Ordre d’exécution

Ordre approximatif (basé sur `@Order` et le type de runner) :

| Ordre | Composant | Type |
|------:|-----------|------|
| 5 | `TenantSettingsDataInitializer` | `ApplicationRunner` |
| (défaut) | `DataInitializer` | `CommandLineRunner` |
| (défaut) | `RbacDataInitializer` | `CommandLineRunner` |
| 20 | `ChainDataInitializer` | `CommandLineRunner` |
| 21 | `FileTypeDataInitializer` | `CommandLineRunner` |
| 22 | `AlertTypeDataInitializer` | `CommandLineRunner` |
| 40 | `AlertDigestRecipientRoleDataInitializer` | `ApplicationRunner` |

> Les runners sans `@Order` s’exécutent selon l’ordre d’enregistrement Spring (souvent avant les `@Order(20+)`).  
> Les chaînes résolvent des organisations déjà seedées (ou retombent sur `MINTP`).

---

## 3. TenantSettingsDataInitializer

**Classe :** `config/TenantSettingsDataInitializer.java`  
**Délègue à :** `TenantSettingsService.ensureSeeded()`

| Si | Alors |
|----|--------|
| `tenant_settings` a déjà ≥ 1 ligne | recharge le cache, ne recrée rien |
| Table vide | crée **1** ligne depuis les propriétés `fluxpro.tenant.*` |

### Valeurs par défaut (`application.properties`)

| Propriété | Défaut typique |
|-----------|----------------|
| `fluxpro.tenant.name` | `MINTP Cameroun` |
| `fluxpro.tenant.product-name` | `FluxPro` |
| `fluxpro.tenant.timezone` | `Africa/Douala` |
| `fluxpro.tenant.country-code` | `CM` |
| `fluxpro.tenant.reference-prefix` | `MINTP` |
| `fluxpro.tenant.badge` | Déploiement pilote · MINTP Cameroun |
| From-address / redirect e-mail | selon propriétés associées |

**SQL de prérequis (si table absente) :** `docs/sql/2026-07-22_tenant_settings.sql` (ou équivalent).

---

## 4. DataInitializer (org + admin)

**Classe :** `config/DataInitializer.java`

### 4.1 Types d’organisation

Insert si le code (ou l’UUID seed) est absent :

| Code | Libellé FR | Libellé EN | Particularité |
|------|------------|------------|---------------|
| `MINISTRY` | Ministère | Ministry | `allowsRoot = true` |
| `DIRECTORATE` | Direction | Directorate | |
| `DIVISION` | Division | Division | |
| `SERVICE` | Service | Service | |
| `REGIONAL_DIRECTORATE` | DRTP | Regional directorate | `regionalScope = true` |

### 4.2 Organisations

Créées si le **code** est absent :

**Racine**

- `MINTP` — Ministère des Travaux Publics

**10 DRTP** (enfants de MINTP)

| Code | Nom |
|------|-----|
| `DRTP-ADAMAOUA` | DRTP Adamaoua (Ngaoundéré) |
| `DRTP-C` | DRTP du Centre (Yaoundé) |
| `DRTP-EST` | DRTP Est (Bertoua) |
| `DRTP-EXTN` | DRTP Extrême-Nord (Maroua) |
| `DRTP-LITTORAL` | DRTP Littoral (Douala) |
| `DRTP-NORD` | DRTP Nord (Garoua) |
| `DRTP-NO` | DRTP Nord-Ouest (Bamenda) |
| `DRTP-OUEST` | DRTP Ouest (Bafoussam) |
| `DRTP-SUD` | DRTP Sud (Ebolowa) |
| `DRTP-SO` | DRTP Sud-Ouest (Buea) |

**Directions** (enfants de MINTP)

| Code | Nom |
|------|-----|
| `DSI` | Direction des Systèmes d'Information |
| `MINTP-CABINET` | Cabinet du Ministre |
| `MINTP-SG` | Secrétariat Général |
| `DAG` | Direction des Affaires Générales |
| `DIER` | Direction des Investissements et de l'Entretien Routier |
| `DGTI` | Direction Générale des Travaux d'Infrastructures |
| `DGET` | Direction Générale des Études Techniques |

### 4.3 Utilisateur bootstrap

Si l’e-mail n’existe pas :

| Champ | Valeur |
|-------|--------|
| E-mail | `e.fotso@mintp.cm` |
| Mot de passe | `Mintp@2025` (hashé) |
| Rôle enum | `SUPER_ADMIN` |
| Organisation | `DSI` |
| Matricule | `MAT-2014-0006` |
| `mustChangePassword` | `false` |

> Les codes d’org mentionnés dans les maillons de chaîne (`DAG-COURRIER`, `DIER-TECH`, `DRTP-C-ADMIN`…) **ne sont pas** créés ici.  
> Voir [§11](#11-points-dattention).

---

## 5. RbacDataInitializer

**Classe :** `config/RbacDataInitializer.java`

Trois étapes dans `run()` :

### 5.1 Permissions (insert si `name` absent)

Catalogue `RESSOURCE:ACTION`, notamment :

- `USERS:*` (READ, CREATE, UPDATE, DELETE, IMPORT, RESET_PASSWORD, UNLOCK)
- `ORGANIZATIONS:*`, `ORGANIZATION_TYPES:*`
- `ROLES:*`, `PERMISSIONS:*`
- `LOGIN_AUDIT:READ`, `AUDIT_LOG:READ`
- `CHAIN_TEMPLATES:*`, `FILE_TYPES:*`
- `FILES:*` (READ, CREATE, UPDATE, CLOSE, ARCHIVE, DELETE, TRANSMIT)
- `ALERT_TYPES:*`, `ALERT_RULES:*`
- `BUSINESS_CALENDAR:*`
- `DASHBOARD:READ`, `DASHBOARD:EXPORT`
- `ASSISTANT:USE`

### 5.2 Rôles + matrice (réécrit à chaque boot)

Pour **chaque** valeur de `UserRole` :

1. Crée le rôle système s’il manque (`systemRole = true`).
2. **Remplace** ses permissions par la matrice codée (`rolePermissionMatrix()`).

| Rôle | Niveau typique |
|------|----------------|
| `SUPER_ADMIN` | Toutes les permissions |
| `BUSINESS_ADMIN` | Admin métier (users/org/types/templates/files jusqu’archive, alertes, calendrier) — sans l’intégralité du SA |
| `DIRECTOR` | Dossiers + close/archive, lecture alertes / users / org |
| `REGIONAL_DIRECTOR` | Comme directeur **sans** archive |
| `SERVICE_HEAD` | Créer / mettre à jour / transmettre — pas de close |
| `AGENT` / `SUPPORT` | Créer / update / transmit + lecture référentiels + dashboard lecture |
| `SECRETARY_GENERAL` / `EXECUTIVE_OFFICE` | Lecture dossiers + dashboards / exports |
| `READER` | Lecture (dossiers, users, référentiels) |

### 5.3 Backfill users

Pour **chaque** utilisateur en base : `roleService.syncPrimaryRole(user)` puis `save` — aligne le lien user ↔ rôle système sur l’enum `User.role`.

**SQL de prérequis :** `docs/sql/2026-07-02_rbac_roles_permissions.sql` (si tables absentes).

---

## 6. ChainDataInitializer

**Classe :** `config/ChainDataInitializer.java` — `@Order(20)`

Insert **uniquement** si le **code** template n’existe pas.  
Ne met **pas** à jour T01–T05 déjà présents.

| Code | Type dossier (`fileTypeCode`) | Délai total | Unité | Actif | Contenu résumée |
|------|-------------------------------|-------------|-------|-------|-----------------|
| **T01** | `COUR-STD` | 11 | jours ouvrés | oui | 7 étapes DAG : réception → orientation → directeur → agent → chef → expédition → clôture |
| **T02** | `COUR-URG` | 3 | heures ouvrées | oui | Circuit accéléré (délais mixtes h.o. / j.o.), 6 étapes |
| **T03** | `MARCHE-SMP` | 21 | jours ouvrés | oui | 8 étapes DIER / SG / cabinet ; visa ministre **optionnel** |
| **T04** | `AUTH-TRAV` | 18 | jours ouvrés | oui | 6 étapes DRTP-C |
| **T05** | `COOP-PART` | 10 | jours ouvrés | **non** | Circuit hors pilote, 3 étapes (`active = false`) |

Chaque maillon porte : `stepOrder`, libellé, `responsibleRole`, organisation (lookup par code → sinon **`MINTP`**), délai, action attendue, flags `optional` / `closureStep`.

**SQL de prérequis :** `docs/sql/2026-07-02_chain_templates.sql`.

---

## 7. FileTypeDataInitializer

**Classe :** `config/FileTypeDataInitializer.java` — `@Order(21)`

Insert si le code est absent (pas de refresh des libellés existants) :

| Code | Nom | Direction | Actif | Ordre |
|------|-----|-----------|-------|------:|
| `COUR-STD` | Courrier entrant standard | DAG | oui | 10 |
| `COUR-URG` | Courrier très urgent | DAG | oui | 20 |
| `MARCHE-SMP` | Marché public simplifié | DIER | oui | 30 |
| `AUTH-TRAV` | Autorisation travaux domaine public | DRTP-C | oui | 40 |
| `COOP-PART` | Coopération / partenariat | — | **non** | 50 |

**SQL de prérequis :** `docs/sql/2026-07-02_file_types.sql`.

---

## 8. AlertTypeDataInitializer

**Classe :** `config/AlertTypeDataInitializer.java` — `@Order(22)`

Tous `systemDefined = true`, `active = true` :

| Code | Libellé | Template e-mail | Usage |
|------|---------|-----------------|--------|
| `REMINDER` | Rappel avant échéance | `alert-reminder` | Règles ALR programmées |
| `OVERDUE` | Dépassement d’échéance | `alert-overdue` | Règles ALR |
| `ESCALATION` | Escalade hiérarchique | `alert-escalation` | Règles ALR |
| `DAILY_DIGEST` | Récapitulatif quotidien des retards | `alert-daily-digest` | Digest |
| `PASSAGE_ARRIVAL` | Dossier arrivé sur votre maillon | `passage-arrival` | Event passation (hors config règles ALR UI) |
| `PASSAGE_CC` | Copie informée | `passage-cc` | Event passation CC |

**SQL de prérequis :** `docs/sql/2026-07-04_alert_types.sql`.

---

## 9. AlertDigestRecipientRoleDataInitializer

**Classe :** `config/AlertDigestRecipientRoleDataInitializer.java` — `@Order(40)`  
**Délègue à :** `AlertDigestRecipientRoleService.seedDefaultsIfEmpty()`

| Si | Alors |
|----|--------|
| Table destinataires digest **non vide** | no-op |
| Table **vide** | insert le rôle de `fluxpro.alerts.digest.target-role` ; si invalide → **`DIRECTOR`** |

---

## 10. Ce qui n’est pas seedé

Au démarrage, le backend **ne crée pas** :

- les **règles d’alerte** rattachées aux templates (`alert_rules`) — via UI « Ajouter les règles standard » ;
- les **jours fériés** / calendrier métier ;
- des **dossiers**, passages, notifications, pièces ;
- d’autres **utilisateurs métier** que le SUPER_ADMIN bootstrap ;
- les **sous-structures** référencées dans les maillons (`DAG-COURRIER`, `DIER-TECH`, `DRTP-C-ADMIN`, etc.), sauf script SQL / création manuelle.
- les comptes **portail**.

---

## 11. Points d’attention

1. **RBAC réécrit la matrice à chaque boot**  
   Une personnalisation manuelle des permissions d’un rôle `system` en base peut être **écrasée** au prochain démarrage.

2. **Templates / types = create-if-missing**  
   Modifier T01 en base puis redémarrer **ne** réapplique **pas** le Java `buildT01()` ; le seed ignore un code déjà présent.

3. **Org des maillons**  
   `ChainDataInitializer.resolveOrganization(code)` : si le code manque → fallback **`MINTP`**. Les étapes seedées peuvent donc pointer toutes vers MINTP tant que les sous-services ne sont pas créés.

4. **Pas de migration DDL**  
   Si une table manque, corriger via script SQL manuel dans `docs/sql/`, pas via `ddl-auto`.

5. **Compte bootstrap**  
   Mot de passe connu en clair dans le seed de dev/pilote — à changer / désactiver hors environnements contrôlés.

---

## 12. Fichiers sources

| Composant | Chemin |
|-----------|--------|
| Tenant | `src/main/java/.../config/TenantSettingsDataInitializer.java` |
| Org + admin | `src/main/java/.../config/DataInitializer.java` |
| RBAC | `src/main/java/.../config/RbacDataInitializer.java` |
| Chaînes | `src/main/java/.../config/ChainDataInitializer.java` |
| Types dossier | `src/main/java/.../config/FileTypeDataInitializer.java` |
| Types alerte | `src/main/java/.../config/AlertTypeDataInitializer.java` |
| Digest roles | `src/main/java/.../config/AlertDigestRecipientRoleDataInitializer.java` |
| Propriétés tenant | `src/main/resources/application.properties` (`fluxpro.tenant.*`) |

**Voir aussi :** [Manuel d’utilisation](./MANUEL-UTILISATION-FLUXPRO.md) · [Guide admin type → template → alertes](./GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md) · [Matrice d’accès UI](./MATRICE-ACCES-UI.md)
