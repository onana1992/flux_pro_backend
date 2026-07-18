# Spécification détaillée — Portail de soumission (PORTAL)

**Projet :** FluxPro — Gestion électronique des dossiers et suivi par chaîne de passation  
**Module :** Portail de soumission (PORTAL)  
**Version :** 0.1 (cible produit — non implémenté)  
**Date :** 18 juillet 2026  
**Statut :** Spécification cible  
**Références :** [DESCRIPTION-PRODUIT-ECARTS-TODO](../docs/DESCRIPTION-PRODUIT-ECARTS-TODO.md) · [SPEC-DOS-DETAILLEE](./SPEC-DOS-DETAILLEE.md) · [SPEC-CHN-TPL-DETAILLEE](./SPEC-CHN-TPL-DETAILLEE.md)

---

## Table des matières

1. [Objectif et périmètre](#1-objectif-et-périmètre)
2. [Concepts et acteurs](#2-concepts-et-acteurs)
3. [Modèle de données cible](#3-modèle-de-données-cible)
4. [Cycle de vie d'une soumission](#4-cycle-de-vie-dune-soumission)
5. [Formulaires préconfigurés](#5-formulaires-préconfigurés)
6. [API portail (cible)](#6-api-portail-cible)
7. [Règles de gestion](#7-règles-de-gestion)
8. [Sécurité](#8-sécurité)
9. [Exemples concrets](#9-exemples-concrets)
10. [Écarts avec le code actuel](#10-écarts-avec-le-code-actuel)
11. [Décisions ouvertes](#11-décisions-ouvertes)
12. [Historique](#12-historique)

---

## 1. Objectif et périmètre

### 1.1. Objectif

Le module Portail de soumission a pour objectif de :

- **Ouvrir** une porte d'entrée aux dossiers pour des personnes qui **ne sont pas des utilisateurs métier FluxPro** (employé sans compte système, usager, professionnel externe, établissement) ;
- **Préconfigurer** des types de dossiers avec un **formulaire structuré** (champs + pièces jointes obligatoires) et une **chaîne de passation** déjà associée ;
- **Créer et soumettre** automatiquement un dossier à la validation du formulaire, puis **enclencher le circuit interne** (2ᵉ maillon / premier traitant métier) sans intervention manuelle du bureau d'ordre ;
- **Permettre** au demandeur de suivre l'avancement de sa demande (lecture seule) et/ou de recevoir des notifications aux étapes clés ;
- **Réutiliser** sans modification le cœur existant : cycle de vie dossier, passages, délais, alertes, dashboard, pièces jointes.

### 1.2. Périmètre

**Inclus dans le module :**

| Bloc | Contenu |
|---|---|
| Utilisateur du portail | Référentiel `PortalUser` distinct du `User` métier ; authentification / realm dédiés |
| Dossiers préconfigurés | Association `FileType` + schéma de formulaire + `ChainTemplate` + responsable par défaut du 1er maillon interne |
| Formulaire dynamique | Définition des champs (types, contraintes, options) ; validation serveur à la soumission |
| Soumission | Création dossier + PJ + `submit` + initialisation automatique de la chaîne |
| Suivi demandeur | Consultation lecture seule (statut, maillon courant, référence) |
| Front portail | UI publique / semi-publique distincte de l'application métier |

**Explicitement hors périmètre :**

- Remplacement du moteur de chaînes (BPM) — les templates restent le modèle ;
- GED documentaire (versioning, plan de classement) — les PJ restent au service du dossier ;
- Signature électronique du demandeur ;
- Remplissage du circuit interne par le demandeur (il n'agit que sur la soumission) ;
- SSO / LDAP du portail (peut être une évolution ultérieure).

---

## 2. Concepts et acteurs

### 2.1. Clarification terminologique

| Terme | Signification |
|---|---|
| **Utilisateur du portail** | Personne (interne non-système ou externe) qui remplit et soumet un formulaire. **N'est pas** un `User` FluxPro métier. |
| **Utilisateur système / métier** | Compte `User` avec rôle (`AGENT`, `DIRECTOR`, etc.) et RBAC ; agit sur les maillons du circuit. |
| **Dossier préconfiguré** | `FileType` ouvert au portail, muni d'un schéma de formulaire et d'une chaîne associée. |
| **1er maillon (soumission)** | Point d'entrée portail : saisie formulaire + PJ. Peut être **visible** dans le circuit (maillon spécial auto-complété) ou **invisible** (la chaîne démarre au premier traitant interne). |
| **2ᵉ maillon (1er traitant interne)** | Premier maillon du circuit métier activé automatiquement à la soumission. |

### 2.2. Acteurs

| Acteur | Rôle |
|---|---|
| **Demandeur portail** | Remplit le formulaire, joint les pièces, soumet, consulte le suivi |
| **Admin métier** | Configure les types ouverts au portail, le schéma de formulaire, la chaîne et le responsable par défaut |
| **Traitant interne** | Traite le dossier via le circuit existant (transmit / return / suspend…) |
| **Système** | Valide le formulaire, crée le dossier, initialise la chaîne, calcule les délais, déclenche les alertes |

---

## 3. Modèle de données cible

> Toute évolution de schéma devra être livrée via un **script SQL manuel** dans `docs/sql/` (`ddl-auto` reste `none`).

### 3.1. `PortalUser` (nouvelle table `portal_users`)

Référentiel des utilisateurs du portail, **séparé** de `users`.

| Champ | Type | Description |
|---|---|---|
| `id` | `UUID` | Identifiant technique |
| `email` | `String` | Identifiant de contact / login (unique) |
| `firstName`, `lastName` | `String` | Identité |
| `phone` | `String` | Optionnel |
| `portalUserType` | enum | `INTERNAL_EMPLOYEE` \| `EXTERNAL` |
| `staffNumber` | `String` | Optionnel — matricule si employé interne |
| `organizationId` | `UUID` | Optionnel — rattachement org si employé |
| `passwordHash` | `String` | Nullable si auth sans mot de passe (OTP / magic-link) |
| `active` | `boolean` | Compte actif |
| `emailVerifiedAt` | `Instant` | Vérification email |

### 3.2. Schéma de formulaire (`FileFormSchema` ou JSON sur `FileType`)

Attachement recommandé : **sur le `FileType`** (un formulaire par type de dossier préconfiguré).

| Champ | Type | Description |
|---|---|---|
| `fileTypeCode` | `String` | Type de dossier concerné |
| `portalEnabled` | `boolean` | Type ouvert au portail |
| `chainTemplateId` | `UUID` | Chaîne à initialiser à la soumission |
| `defaultFirstStepResponsibleUserId` | `UUID` | User métier assigné au 1er maillon interne (stage 1 ou 2 selon option) |
| `formSchema` | `JSON` | Définition des champs (voir §5) |
| `requiredAttachmentKeys` | `JSON` | Liste des clés de PJ obligatoires |

### 3.3. Impacts sur les entités existantes

| Entité / champ | Impact |
|---|---|
| `FileEntity.createdBy` | Aujourd'hui `User` obligatoire — à assouplir (nullable, ou créateur technique « portail », ou lien `portalUserId`) |
| `FileEntity.senderOrBeneficiary` | Rempli avec le nom du `PortalUser` |
| `FileEntity.metadata` | Contient les réponses du formulaire, **validées** contre le schéma |
| `FileEntity.chainTemplate` | Renseigné automatiquement à la soumission |
| `FileAttachment.uploader` | Aujourd'hui `User` — à adapter pour un uploader portail / technique |
| `ChainStepTemplate` | Optionnel : `stepType = PORTAL_SUBMISSION` si le 1er maillon est visible dans le circuit |

---

## 4. Cycle de vie d'une soumission

### 4.1. Option recommandée (B) — soumission hors circuit, démarrage au 1er traitant

```
PortalUser
  │
  ├─ 1. Choisit un type de dossier préconfiguré
  ├─ 2. Remplit le formulaire + pièces jointes
  └─ 3. Soumet
        │
        ▼
   Backend PORTAL
        ├─ Valide formData contre formSchema
        ├─ Crée FileEntity (DRAFT) + metadata + senderOrBeneficiary
        ├─ Attache les pièces jointes
        ├─ submit() → IN_PROGRESS + référence générée
        └─ initialise la chaîne automatiquement
              └─ active le stage 1 (1er traitant interne)
                    └─ dueAt calculé, alertes applicables
```

Dans cette option, le « 1er maillon » côté produit = **l'acte de soumission** ; le circuit affiché commence au **premier traitant interne**.

### 4.2. Option alternative (A) — maillon « Soumission portail » visible

- Le `ChainTemplate` comporte un maillon `stepOrder = 1` de type `PORTAL_SUBMISSION`.
- À la soumission : le passage 1 est créé puis immédiatement `COMPLETED` (porteur = `PortalUser` / libellé demandeur).
- Le stage 2 (traitants internes) est activé.
- Avantage : traçabilité visuelle complète dans le circuit.

### 4.3. Suite du cycle (inchangée)

Une fois le dossier `IN_PROGRESS` et la chaîne initialisée, le comportement existant s'applique :

- transmission / retour / suspension / reprise / réaffectation ;
- clôture via le maillon de clôture ;
- alertes et digest ;
- dashboard et exports.

Le demandeur **ne traite pas** les maillons internes.

---

## 5. Formulaires préconfigurés

### 5.1. Structure du `formSchema`

```json
{
  "fields": [
    {
      "key": "dateDebut",
      "label": "Date de début",
      "type": "DATE",
      "required": true
    },
    {
      "key": "dateFin",
      "label": "Date de fin",
      "type": "DATE",
      "required": true
    },
    {
      "key": "typeConge",
      "label": "Type de congé",
      "type": "ENUM",
      "required": true,
      "options": ["ANNUEL", "MALADIE", "SANS_SOLDE"]
    },
    {
      "key": "motif",
      "label": "Motif",
      "type": "TEXT",
      "required": false,
      "maxLength": 500
    }
  ],
  "requiredAttachments": [
    { "key": "justificatif", "label": "Justificatif", "minCount": 0 }
  ]
}
```

### 5.2. Types de champs supportés (cible v1)

| Type | Description |
|---|---|
| `TEXT` / `TEXTAREA` | Texte court / long |
| `NUMBER` | Nombre |
| `DATE` / `DATETIME` | Date / date-heure |
| `ENUM` / `MULTI_ENUM` | Liste à choix |
| `BOOLEAN` | Oui / non |
| `EMAIL` / `PHONE` | Contacts avec validation format |

### 5.3. Validation

- Côté client : UX (champs requis, formats).
- Côté serveur : **source de vérité** — rejet si champ requis manquant, type incorrect, option hors liste, PJ obligatoire absente.
- Les réponses validées sont stockées dans `FileEntity.metadata`.

---

## 6. API portail (cible)

Base proposée : `/api/portal/**` (filtre de sécurité distinct de `/api/**` métier).

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/api/portal/form-types` | Liste des types de dossiers ouverts au portail |
| `GET` | `/api/portal/form-types/{code}/schema` | Schéma de formulaire + règles PJ |
| `POST` | `/api/portal/auth/login` ou `/otp/request` | Authentification portail (selon décision §11) |
| `POST` | `/api/portal/submissions` | Crée + soumet le dossier (formData + métadonnées) |
| `POST` | `/api/portal/submissions/{id}/attachments` | Upload PJ avant / pendant soumission |
| `GET` | `/api/portal/submissions` | Liste des soumissions du demandeur connecté |
| `GET` | `/api/portal/submissions/{ref}` | Suivi lecture seule (statut, maillon courant) |

Côté admin métier (extension des APIs existantes) :

| Méthode | Endpoint | Description |
|---|---|---|
| `PUT` | `/api/admin/file-types/{code}/portal` | Activer le portail, lier chaîne, schéma, responsable défaut |
| `PUT` | `/api/admin/file-types/{code}/form-schema` | CRUD du schéma de formulaire |

---

## 7. Règles de gestion

| ID | Règle |
|---|---|
| PORTAL-01 | Seuls les `FileType` avec `portalEnabled = true` sont exposés sur le portail. |
| PORTAL-02 | Une soumission exige un schéma de formulaire valide et une chaîne active associée. |
| PORTAL-03 | `formData` doit passer la validation serveur avant toute création de dossier. |
| PORTAL-04 | À la soumission réussie : dossier `IN_PROGRESS`, référence générée, chaîne initialisée, 1er traitant interne activé. |
| PORTAL-05 | Le demandeur portail ne peut **pas** transmettre, retourner, suspendre ni réaffecter un maillon. |
| PORTAL-06 | Le suivi portail est **lecture seule** (statut dossier, maillon courant, éventuellement historique anonymisé des étapes). |
| PORTAL-07 | Les pièces jointes respectent les contraintes existantes (taille max ~20 Mo, types MIME autorisés). |
| PORTAL-08 | Le `senderOrBeneficiary` du dossier reflète l'identité du `PortalUser`. |
| PORTAL-09 | Les alertes et SLA du circuit interne s'appliquent dès l'activation du 1er traitant (moteur ALR existant). |
| PORTAL-10 | Un type de dossier peut être retiré du portail (`portalEnabled = false`) sans supprimer les dossiers déjà soumis. |

---

## 8. Sécurité

- **Realm séparé** : tokens / sessions portail distincts des JWT métier ; pas d'accès aux endpoints admin / passages.
- **Isolation des données** : un `PortalUser` ne voit que ses propres soumissions.
- **Pas de RBAC métier** sur le portail : droits limités à soumettre / lister / suivre / uploader.
- **Anti-abus** : rate limiting sur soumission et OTP ; vérification email recommandée.
- **PJ** : scan / whitelist MIME ; stockage isolé par organisation / année / dossier (comme l'existant).

---

## 9. Exemples concrets

### 9.1. Administration / RH — Demande de congé

| Élément | Valeur |
|---|---|
| Demandeur | Employé sans compte FluxPro (`INTERNAL_EMPLOYEE`) |
| Formulaire | dates, type de congé, motif |
| PJ | justificatif (optionnel / obligatoire selon type) |
| Circuit | Chef de service → RH → SG (visa) → clôture |

### 9.2. Santé — Déclaration d'événement indésirable

| Élément | Valeur |
|---|---|
| Demandeur | Infirmière / soignant sans compte FluxPro |
| Formulaire | date, unité, gravité, description |
| PJ | photos, compte-rendu |
| Circuit | Cellule qualité → chef de pôle → CME (si gravité élevée) → plan d'action → clôture |
| Valeur ajoutée | Signalement immédiat + SLA courts + traçabilité pour audit qualité |

### 9.3. Santé — Autorisation d'ouverture d'établissement

| Élément | Valeur |
|---|---|
| Demandeur | Promoteur / clinique (`EXTERNAL`) |
| Formulaire | localisation, type d'établissement, capacité |
| PJ | plans, titres, CV du personnel |
| Circuit | Bureau d'ordre → inspection technique → commission → SG (autorisation) → clôture |

### 9.4. Santé — Autres dossiers types

| Dossier | Demandeur | Circuit (indicatif) |
|---|---|---|
| Accès au dossier médical | Patient | Archives → médecin référent → remise |
| Réclamation patient | Usager | Qualité → chef de service → réponse |
| Matériovigilance | Soignant | Biomédical → Direction |
| Agrément officine | Pharmacien | Service pharmacie → inspection → Direction |
| Pharmacovigilance | Professionnel / labo | Centre de pharmacovigilance → évaluation |
| Maladie à notification obligatoire | Médecin / labo | Surveillance épidémiologique → alerte régionale |

### 9.5. Déroulé type (événement indésirable)

```
1. Soignant ouvre le portail (sans compte métier)
2. Choisit « Déclaration d'événement indésirable »
3. Remplit le formulaire + joint les pièces
4. Soumet → dossier créé, validé, référence générée, IN_PROGRESS
5. Circuit interne activé au 1er traitant (qualité)
6. Alertes / délais existants s'appliquent
7. Demandeur suit le statut en lecture seule (ou reçoit un email aux étapes clés)
```

---

## 10. Écarts avec le code actuel

| Élément | État actuel | Cible PORTAL |
|---|---|---|
| Utilisateur externe / non-système | Texte `senderOrBeneficiary` uniquement | Entité `PortalUser` + auth dédiée |
| Formulaire par type | Absent (`metadata` JSON libre non validé) | `formSchema` + validation serveur |
| Création dossier | `createdBy` = `User` obligatoire | Création depuis le portail |
| Init chaîne | Appel API manuel + affectations users | Auto à la soumission + responsable par défaut |
| Stage 1 | Affectation `User` obligatoire | 1er traitant interne préconfiguré |
| Uploader PJ | `User` obligatoire | Adapter pour portail |
| Front portail | Absent | Application / routes dédiées |
| Suivi demandeur | Absent | Endpoints lecture seule |

---

## 11. Décisions ouvertes

À trancher avant implémentation :

| Sujet | Options | Recommandation provisoire |
|---|---|---|
| Authentification portail | Compte email/mdp · Magic-link / OTP · Anonyme · Mixte | OTP / magic-link pour externe ; compte léger pour employé interne |
| Visibilité du 1er maillon | Option A (visible) · Option B (hors circuit) | **Option B** (plus simple, moins d'impact sur CHN) |
| Emplacement du schéma | Sur `FileType` · Sur `ChainTemplate` · Éditeur drag & drop | **Sur `FileType`** ; éditeur UI admin en P1 |
| Suivi après soumission | Page suivi · Emails seuls · Aucun retour | Page suivi lecture seule + emails aux étapes clés |

---

## 12. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-18 | Équipe produit / cadrage | Création de la spécification cible PORTAL (portail de soumission, formulaires préconfigurés, utilisateurs non-système) |
