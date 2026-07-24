# Spécification détaillée — Portail de soumission (PORTAL)

**Projet :** FluxPro — Gestion électronique des dossiers et suivi par chaîne de passation  
**Module :** Portail de soumission (PORTAL)  
**Version :** 0.2 (cible produit — non implémenté)  
**Date :** 24 juillet 2026  
**Statut :** Spécification cible  
**Références :** [DESCRIPTION-PRODUIT-ECARTS-TODO](../docs/DESCRIPTION-PRODUIT-ECARTS-TODO.md) · [SPEC-DOS-DETAILLEE](./SPEC-DOS-DETAILLEE.md) · [SPEC-CHN-TPL-DETAILLEE](./SPEC-CHN-TPL-DETAILLEE.md) · [ROADMAP-PORTAIL-SOUMISSION](./ROADMAP-PORTAIL-SOUMISSION.md)

---

## Table des matières

1. [Objectif et périmètre](#1-objectif-et-périmètre)
2. [Concepts et acteurs](#2-concepts-et-acteurs)
3. [Deux variantes de portail](#3-deux-variantes-de-portail)
4. [Inscription et authentification](#4-inscription-et-authentification)
5. [Espace demandeur](#5-espace-demandeur)
6. [Modèle de données cible](#6-modèle-de-données-cible)
7. [Cycle de vie d'une soumission](#7-cycle-de-vie-dune-soumission)
8. [Formulaires préconfigurés](#8-formulaires-préconfigurés)
9. [API portail (cible)](#9-api-portail-cible)
10. [Règles de gestion](#10-règles-de-gestion)
11. [Sécurité](#11-sécurité)
12. [Exemples concrets](#12-exemples-concrets)
13. [Écarts avec le code actuel](#13-écarts-avec-le-code-actuel)
14. [Décisions](#14-décisions)
15. [Historique](#15-historique)

---

## 1. Objectif et périmètre

### 1.1. Objectif

Le module Portail de soumission a pour objectif de :

- **Ouvrir** une porte d'entrée aux dossiers pour des personnes qui **ne sont pas des utilisateurs métier FluxPro** (employé sans compte système, usager, professionnel externe, établissement) ;
- **Distinguer** deux audiences : **portail interne** (demandes RH / organisationnelles) et **portail externe** (usagers, partenaires) ;
- **Préconfigurer** des types de dossiers avec un **formulaire structuré** (champs + pièces jointes obligatoires) et une **chaîne de passation** déjà associée ;
- **Créer et soumettre** automatiquement un dossier à la validation du formulaire, puis **enclencher le circuit interne** (2ᵉ maillon / premier traitant métier) sans intervention manuelle du bureau d'ordre ;
- **Permettre** au demandeur de se connecter à son espace, consulter le catalogue de demandes, soumettre, et suivre l'avancement (lecture seule) et/ou recevoir des notifications aux étapes clés ;
- **Réutiliser** sans modification le cœur existant : cycle de vie dossier, passages, délais, alertes, dashboard, pièces jointes.

### 1.2. Périmètre

**Inclus dans le module :**

| Bloc | Contenu |
|---|---|
| Utilisateur du portail | Référentiel `PortalUser` distinct du `User` métier ; authentification / realm dédiés ; type `INTERNAL_EMPLOYEE` ou `EXTERNAL` |
| Deux variantes de portail | Entrées interne / externe ; catalogue filtré par `portalAudience` |
| Inscription interne | Création par admin + mot de passe provisoire + activation à la 1ʳᵉ connexion |
| Inscription externe | Self-service + OTP email à chaque connexion (§4.2) |
| Dossiers préconfigurés | Association `FileType` + schéma de formulaire + `ChainTemplate` + responsable par défaut du 1er maillon interne + audience |
| Formulaire dynamique | Définition des champs (types, contraintes, options) ; validation serveur à la soumission |
| Soumission | Création dossier + PJ + `submit` + initialisation automatique de la chaîne |
| Espace demandeur | Connexion, catalogue, soumission, suivi lecture seule |
| Front portail | UI distincte de l'application métier (routes `/portal/internal` et `/portal/external` ou équivalent) |

**Explicitement hors périmètre :**

- Remplacement du moteur de chaînes (BPM) — les templates restent le modèle ;
- GED documentaire (versioning, plan de classement) — les PJ restent au service du dossier ;
- Signature électronique du demandeur ;
- Remplissage du circuit interne par le demandeur (il n'agit que sur la soumission) ;
- SSO / LDAP du portail (peut être une évolution ultérieure) ;
- Self-service d'inscription pour les employés internes (création réservée à l'admin).

---

## 2. Concepts et acteurs

### 2.1. Clarification terminologique

| Terme | Signification |
|---|---|
| **Utilisateur du portail** | Personne (interne non-système ou externe) qui remplit et soumet un formulaire. **N'est pas** un `User` FluxPro métier. |
| **Portail interne** | Entrée réservée aux employés (`INTERNAL_EMPLOYEE`) pour demandes internes (congé, absence…). |
| **Portail externe** | Entrée réservée aux usagers / partenaires (`EXTERNAL`). |
| **Utilisateur système / métier** | Compte `User` avec rôle (`AGENT`, `DIRECTOR`, etc.) et RBAC ; agit sur les maillons du circuit. |
| **Dossier préconfiguré** | `FileType` ouvert au portail, muni d'un schéma de formulaire, d'une chaîne et d'une `portalAudience`. |
| **1er maillon (soumission)** | Point d'entrée portail : saisie formulaire + PJ. Peut être **visible** dans le circuit (maillon spécial auto-complété) ou **invisible** (la chaîne démarre au premier traitant interne). |
| **2ᵉ maillon (1er traitant interne)** | Premier maillon du circuit métier activé automatiquement à la soumission. |
| **Mot de passe provisoire** | Mot de passe initial posé par l'admin à la création d'un compte interne ; doit être changé à la 1ʳᵉ connexion (activation). |

### 2.2. Acteurs

| Acteur | Rôle |
|---|---|
| **Demandeur portail interne** | Employé : se connecte, active son compte, consulte le catalogue interne, soumet, suit |
| **Demandeur portail externe** | Usager / partenaire : s'inscrit (selon §14), se connecte, soumet, suit |
| **Admin métier** | Configure les types ouverts au portail (audience, schéma, chaîne) ; **crée les comptes portail internes** (mdp provisoire) |
| **Traitant interne** | Traite le dossier via le circuit existant (transmit / return / suspend…) |
| **Système** | Valide le formulaire, crée le dossier, initialise la chaîne, calcule les délais, déclenche les alertes |

---

## 3. Deux variantes de portail

Un seul moteur PORTAL, **deux audiences** isolées (auth, catalogue, branding).

| | Portail interne | Portail externe |
|---|---|---|
| **Qui** | Employés sans compte FluxPro métier | Usagers, partenaires, établissements |
| **`portalUserType`** | `INTERNAL_EMPLOYEE` | `EXTERNAL` |
| **Demandes types** | Congé, absence, demandes RH / org | Déclarations, réclamations, autorisations… |
| **Inscription** | **Création admin + mdp provisoire** (§4.1) | Self-service + OTP (§4.2 / §14) |
| **Catalogue** | Types `portalAudience = INTERNAL` (ou `BOTH`) | Types `portalAudience = EXTERNAL` (ou `BOTH`) |
| **Entrée UI** | `/portal/internal` (ou sous-domaine intranet) | `/portal/external` (ou sous-domaine public) |

**Règle d'isolation :** un utilisateur ne voit et ne soumet que les types compatibles avec son `portalUserType` / l'audience du portail sur lequel il est connecté.

---

## 4. Inscription et authentification

### 4.1. Portail interne — décision figée

**Création exclusivement par l'admin métier**, avec un **mot de passe provisoire**. L'employé **active** son compte à la **première connexion** en changeant ce mot de passe.

```
Admin métier
  │
  ├─ Crée PortalUser (INTERNAL_EMPLOYEE)
  │    email, nom, prénom, matricule (opt.), org (opt.)
  │    + mot de passe provisoire
  │    mustChangePassword = true
  │    active = true (ou pending selon politique)
  │
  └─ Remise du mdp provisoire (décision figée) :
        1. Affichage **one-shot** dans l'UI admin à la création / reset
           (copiable une fois ; non relisible ensuite)
        2. Envoi **email optionnel** concomitante (identifiants + consigne
           de changer le mdp à la 1ʳᵉ connexion)
              │
              ▼
        1ʳᵉ connexion (email + mdp provisoire)
              │
              ├─ Auth OK mais mustChangePassword = true
              │     → pas d'accès catalogue / soumission
              │     → écran obligatoire « Changer le mot de passe »
              │
              ├─ POST /api/portal/auth/activate
              │     (mdp provisoire + nouveau mdp)
              │     → mustChangePassword = false
              │     → passwordChangedAt renseigné
              │
              └─ Session normale → espace demandeur
```

**Règles associées :**

- Pas de self-register interne.
- Tant que `mustChangePassword = true`, seuls login + activation sont autorisés (pas de soumission).
- L'admin peut **réinitialiser** le mot de passe (nouveau provisoire + `mustChangePassword = true`).
- Le mot de passe provisoire ne doit jamais être stocké en clair (hash uniquement).
- **Canal de remise (figé)** : affichage **one-shot** dans l'UI admin à la création / reset (± email optionnel au demandeur). Après fermeture de l'écran, le provisoire n'est plus relisible ; seul un nouveau reset admin peut en générer un autre.

### 4.2. Portail externe — décision figée : OTP

Authentification et vérification par **OTP email** (pas de magic-link, pas de mot de passe côté externe en MVP).

```
Usager / partenaire
  │
  ├─ 1. Inscription self-service
  │     email, prénom, nom, téléphone (opt.), organisation (opt.)
  │     → crée PortalUser (EXTERNAL, active)
  │     → envoie OTP de vérification
  │
  ├─ 2. POST /api/portal/auth/otp/verify (1ʳᵉ fois)
  │     → emailVerifiedAt renseigné
  │     → session portail
  │
  └─ 3. Connexions suivantes
        POST /api/portal/auth/otp/request  (email)
        POST /api/portal/auth/otp/verify   (code)
        → session portail
```

**Règles associées :**

- Pas de `passwordHash` requis pour `EXTERNAL` (nullable).
- Pas de soumission tant que `emailVerifiedAt` est null.
- OTP à **chaque** connexion (pas de mdp alternatif en MVP).
- Rate limiting strict sur `otp/request` et `otp/verify` ; TTL court du code ; invalidation après usage.

### 4.3. Principes communs

- Realm / JWT portail **distinct** du JWT métier.
- Claims utiles : `portalUserId`, `portalUserType`, `mustChangePassword`.
- Un email = un seul `PortalUser` (pas de double compte interne + externe sur le même email en MVP).
- Compte `active = false` → login refusé.

---

## 5. Espace demandeur

Après authentification (et activation si interne), chaque `PortalUser` dispose d'un **espace personnel** :

| Capacité | Description |
|---|---|
| **Se connecter** | Accès à son espace (portail interne ou externe selon son type) |
| **Voir les demandes disponibles** | Catalogue des `FileType` ouverts à son audience |
| **Soumettre une demande** | Formulaire dynamique + PJ → dossier + chaîne |
| **Suivre** | Liste et détail de **ses** soumissions (lecture seule) |

```
Login (+ activation si 1ʳᵉ connexion interne)
        │
        ▼
   Accueil / Mon espace
        │
        ├─ 1. Demandes disponibles → formSchema → soumission → référence
        │
        └─ 2. Mes demandes → détail suivi (statut, maillon courant)
```

Le demandeur **ne traite pas** les maillons internes (pas de transmit / return / suspend / réaffectation).

---

## 6. Modèle de données cible

> Toute évolution de schéma devra être livrée via un **script SQL manuel** dans `docs/sql/` (`ddl-auto` reste `none`).

### 6.1. `PortalUser` (nouvelle table `portal_users`)

Référentiel des utilisateurs du portail, **séparé** de `users`.

| Champ | Type | Description |
|---|---|---|
| `id` | `UUID` | Identifiant technique |
| `email` | `String` | Identifiant de contact / login (**unique**) |
| `firstName`, `lastName` | `String` | Identité |
| `phone` | `String` | Optionnel |
| `portalUserType` | enum | `INTERNAL_EMPLOYEE` \| `EXTERNAL` |
| `staffNumber` | `String` | Optionnel — matricule si employé interne |
| `organizationId` | `UUID` | Optionnel — rattachement org si employé |
| `passwordHash` | `String` | Obligatoire pour interne ; nullable si auth OTP pure (externe) |
| `mustChangePassword` | `boolean` | `true` après création / reset admin (interne) ; forcer activation |
| `passwordChangedAt` | `Instant` | Dernier changement de mot de passe réussi |
| `active` | `boolean` | Compte actif |
| `emailVerifiedAt` | `Instant` | Vérification email (surtout externe) |
| `createdByAdminUserId` | `UUID` | Optionnel — admin métier ayant créé le compte (interne) |
| `createdAt` / `updatedAt` | `Instant` | Audit |

### 6.2. Schéma de formulaire (`FileFormSchema` ou JSON sur `FileType`)

Attachement recommandé : **sur le `FileType`** (un formulaire par type de dossier préconfiguré).

| Champ | Type | Description |
|---|---|---|
| `fileTypeCode` | `String` | Type de dossier concerné |
| `portalEnabled` | `boolean` | Type ouvert au portail |
| `portalAudience` | enum | `INTERNAL` \| `EXTERNAL` \| `BOTH` |
| `chainTemplateId` | `UUID` | Chaîne à initialiser à la soumission |
| `defaultFirstStepResponsibleUserId` | `UUID` | User métier assigné au 1er maillon interne (stage 1 ou 2 selon option) |
| `formSchema` | `JSON` | Définition des champs (voir §8) |
| `requiredAttachmentKeys` | `JSON` | Liste des clés de PJ obligatoires |

### 6.3. Impacts sur les entités existantes

| Entité / champ | Impact |
|---|---|
| `FileEntity.createdBy` | Aujourd'hui `User` obligatoire — à assouplir (nullable, ou créateur technique « portail », ou lien `portalUserId`) |
| `FileEntity.senderOrBeneficiary` | Rempli avec le nom du `PortalUser` |
| `FileEntity.metadata` | Contient les réponses du formulaire, **validées** contre le schéma |
| `FileEntity.chainTemplate` | Renseigné automatiquement à la soumission |
| `FileAttachment.uploader` | Aujourd'hui `User` — à adapter pour un uploader portail / technique |
| `ChainStepTemplate` | Optionnel : `stepType = PORTAL_SUBMISSION` si le 1er maillon est visible dans le circuit |

---

## 7. Cycle de vie d'une soumission

### 7.1. Option recommandée (B) — soumission hors circuit, démarrage au 1er traitant

```
PortalUser (authentifié + activé)
  │
  ├─ 1. Choisit un type de dossier préconfiguré (audience compatible)
  ├─ 2. Remplit le formulaire + pièces jointes
  └─ 3. Soumet
        │
        ▼
   Backend PORTAL
        ├─ Vérifie compatibilité portalUserType ↔ portalAudience
        ├─ Valide formData contre formSchema
        ├─ Crée FileEntity (DRAFT) + metadata + senderOrBeneficiary
        ├─ Attache les pièces jointes
        ├─ submit() → IN_PROGRESS + référence générée
        └─ initialise la chaîne automatiquement
              └─ active le stage 1 (1er traitant interne)
                    └─ dueAt calculé, alertes applicables
```

Dans cette option, le « 1er maillon » côté produit = **l'acte de soumission** ; le circuit affiché commence au **premier traitant interne**.

### 7.2. Option alternative (A) — maillon « Soumission portail » visible

- Le `ChainTemplate` comporte un maillon `stepOrder = 1` de type `PORTAL_SUBMISSION`.
- À la soumission : le passage 1 est créé puis immédiatement `COMPLETED` (porteur = `PortalUser` / libellé demandeur).
- Le stage 2 (traitants internes) est activé.
- Avantage : traçabilité visuelle complète dans le circuit.

### 7.3. Suite du cycle (inchangée)

Une fois le dossier `IN_PROGRESS` et la chaîne initialisée, le comportement existant s'applique :

- transmission / retour / suspension / reprise / réaffectation ;
- clôture via le maillon de clôture ;
- alertes et digest ;
- dashboard et exports.

Le demandeur **ne traite pas** les maillons internes.

---

## 8. Formulaires préconfigurés

### 8.1. Structure du `formSchema`

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

### 8.2. Types de champs supportés (cible v1)

| Type | Description |
|---|---|
| `TEXT` / `TEXTAREA` | Texte court / long |
| `NUMBER` | Nombre |
| `DATE` / `DATETIME` | Date / date-heure |
| `ENUM` / `MULTI_ENUM` | Liste à choix |
| `BOOLEAN` | Oui / non |
| `EMAIL` / `PHONE` | Contacts avec validation format |

### 8.3. Validation

- Côté client : UX (champs requis, formats).
- Côté serveur : **source de vérité** — rejet si champ requis manquant, type incorrect, option hors liste, PJ obligatoire absente.
- Les réponses validées sont stockées dans `FileEntity.metadata`.

---

## 9. API portail (cible)

Base proposée : `/api/portal/**` (filtre de sécurité distinct de `/api/**` métier).

### 9.1. Authentification & activation

| Méthode | Endpoint | Description |
|---|---|---|
| `POST` | `/api/portal/auth/login` | Login email + mdp (**interne uniquement**) |
| `POST` | `/api/portal/auth/activate` | 1ʳᵉ connexion interne : mdp provisoire → nouveau mdp ; pose `mustChangePassword = false` |
| `POST` | `/api/portal/auth/register` | Inscription self-service **externe** → envoi OTP |
| `POST` | `/api/portal/auth/otp/request` | Demande OTP (externe — login ou renvoi) |
| `POST` | `/api/portal/auth/otp/verify` | Vérifie OTP → session (+ pose `emailVerifiedAt` si 1ʳᵉ fois) |

Réponse login si `mustChangePassword = true` : session limitée ou challenge `PASSWORD_CHANGE_REQUIRED` (pas d'accès catalogue / soumission tant que non activé).

### 9.2. Espace demandeur

| Méthode | Endpoint | Description |
|---|---|---|
| `GET` | `/api/portal/form-types` | Types ouverts au portail **filtrés par audience** du demandeur |
| `GET` | `/api/portal/form-types/{code}/schema` | Schéma de formulaire + règles PJ |
| `POST` | `/api/portal/submissions` | Crée + soumet le dossier (formData + métadonnées) |
| `POST` | `/api/portal/submissions/{id}/attachments` | Upload PJ avant / pendant soumission |
| `GET` | `/api/portal/submissions` | Liste des soumissions du demandeur connecté |
| `GET` | `/api/portal/submissions/{ref}` | Suivi lecture seule (statut, maillon courant) |

### 9.3. Admin métier

| Méthode | Endpoint | Description |
|---|---|---|
| `PUT` | `/api/admin/file-types/{code}/portal` | Activer le portail, audience, chaîne, schéma, responsable défaut |
| `PUT` | `/api/admin/file-types/{code}/form-schema` | CRUD du schéma de formulaire |
| `POST` | `/api/admin/portal-users` | **Créer** un `PortalUser` interne (mdp provisoire, `mustChangePassword = true`) ; réponse inclut le provisoire **une seule fois** ; query/body `sendEmail?: boolean` |
| `PATCH` | `/api/admin/portal-users/{id}` | Modifier identité / `active` / org / matricule |
| `POST` | `/api/admin/portal-users/{id}/reset-password` | Nouveau mdp provisoire + `mustChangePassword = true` ; retour one-shot ± `sendEmail` |
| `GET` | `/api/admin/portal-users` | Lister / rechercher les comptes portail |

---

## 10. Règles de gestion

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
| PORTAL-11 | Un `INTERNAL_EMPLOYEE` ne voit / ne soumet que des types `portalAudience` `INTERNAL` ou `BOTH`. |
| PORTAL-12 | Un `EXTERNAL` ne voit / ne soumet que des types `portalAudience` `EXTERNAL` ou `BOTH`. |
| PORTAL-13 | Les comptes internes sont **créés uniquement par l'admin** ; pas de self-register interne. |
| PORTAL-14 | À la création / reset interne : `mustChangePassword = true` ; la 1ʳᵉ connexion **force** le changement de mot de passe (activation) avant tout autre droit. |
| PORTAL-15 | Tant que `mustChangePassword = true`, le demandeur ne peut ni lister le catalogue ni soumettre ni consulter le suivi. |

---

## 11. Sécurité

- **Realm séparé** : tokens / sessions portail distincts des JWT métier ; pas d'accès aux endpoints admin / passages.
- **Isolation des données** : un `PortalUser` ne voit que ses propres soumissions.
- **Isolation d'audience** : catalogue et soumissions filtrés par `portalUserType` / `portalAudience`.
- **Pas de RBAC métier** sur le portail : droits limités à soumettre / lister / suivre / uploader (après activation).
- **Mots de passe** : hash fort ; provisoire communiqué hors bande ; reset admin réarme `mustChangePassword`.
- **Anti-abus** : rate limiting sur soumission, login et OTP ; vérification email recommandée (externe).
- **PJ** : scan / whitelist MIME ; stockage isolé par organisation / année / dossier (comme l'existant).

---

## 12. Exemples concrets

### 12.1. Administration / RH — Demande de congé (portail interne)

| Élément | Valeur |
|---|---|
| Audience | `INTERNAL` |
| Demandeur | Employé créé par admin (`INTERNAL_EMPLOYEE`), compte activé à la 1ʳᵉ connexion |
| Formulaire | dates, type de congé, motif |
| PJ | justificatif (optionnel / obligatoire selon type) |
| Circuit | Chef de service → RH → SG (visa) → clôture |

### 12.2. Santé — Déclaration d'événement indésirable (portail interne)

| Élément | Valeur |
|---|---|
| Audience | `INTERNAL` |
| Demandeur | Infirmière / soignant sans compte FluxPro (`INTERNAL_EMPLOYEE`) |
| Formulaire | date, unité, gravité, description |
| PJ | photos, compte-rendu |
| Circuit | Cellule qualité → chef de pôle → CME (si gravité élevée) → plan d'action → clôture |

### 12.3. Santé — Autorisation d'ouverture d'établissement (portail externe)

| Élément | Valeur |
|---|---|
| Audience | `EXTERNAL` |
| Demandeur | Promoteur / clinique (`EXTERNAL`) |
| Formulaire | localisation, type d'établissement, capacité |
| PJ | plans, titres, CV du personnel |
| Circuit | Bureau d'ordre → inspection technique → commission → SG (autorisation) → clôture |

### 12.4. Déroulé type — employé interne (congé)

```
1. Admin crée le compte portail interne + mot de passe provisoire
2. Employé se connecte la 1ʳᵉ fois → change le mot de passe (activation)
3. Accède à son espace → voit « Demande de congé »
4. Remplit le formulaire + joint les pièces
5. Soumet → dossier créé, référence générée, IN_PROGRESS
6. Circuit interne activé au 1er traitant
7. Employé suit le statut dans « Mes demandes »
```

---

## 13. Écarts avec le code actuel

| Élément | État actuel | Cible PORTAL |
|---|---|---|
| Utilisateur externe / non-système | Texte `senderOrBeneficiary` uniquement | Entité `PortalUser` + auth dédiée |
| Variantes interne / externe | Absent | `portalUserType` + `portalAudience` |
| Inscription interne | Absent | Création admin + mdp provisoire + activation |
| Formulaire par type | Absent (`metadata` JSON libre non validé) | `formSchema` + validation serveur |
| Création dossier | `createdBy` = `User` obligatoire | Création depuis le portail |
| Init chaîne | Appel API manuel + affectations users | Auto à la soumission + responsable par défaut |
| Stage 1 | Affectation `User` obligatoire | 1er traitant interne préconfiguré |
| Uploader PJ | `User` obligatoire | Adapter pour portail |
| Front portail | Absent | Routes interne / externe + espace demandeur |
| Suivi demandeur | Absent | Endpoints lecture seule |

---

## 14. Décisions

### 14.1. Décisions figées

| Sujet | Décision | Date |
|---|---|---|
| Inscription portail **interne** | Création par **admin** + **mot de passe provisoire** + **activation** (changement de mdp) à la 1ʳᵉ connexion | 2026-07-24 |
| Remise du mdp provisoire | Affichage **one-shot** UI admin à la création / reset **± email** optionnel au demandeur ; non relisible ensuite | 2026-07-24 |
| Authentification portail **externe** | **OTP email** à l'inscription et à **chaque** connexion (pas de magic-link / mdp en MVP) | 2026-07-24 |
| Visibilité du 1er maillon | **Option B** (soumission hors circuit) — recommandation maintenue | — |
| Emplacement du schéma | **Sur `FileType`** | — |
| Suivi après soumission | Page suivi lecture seule + emails aux étapes clés | — |
| Variantes de portail | Deux audiences `INTERNAL` / `EXTERNAL` (même moteur) | 2026-07-24 |
| Périmètre pilote MVP | **Au moins 1 type interne** (ex. demande de **congé**) ; portail / type **externe optionnel** pour le MVP | 2026-07-24 |

### 14.2. Décisions encore ouvertes

| Sujet | Options | Recommandation provisoire |
|---|---|---|
| Compte `active` à la création interne | Actif immédiatement · Actif seulement après changement mdp | Actif + `mustChangePassword` (bloque les droits jusqu'à activation) |
| TTL / longueur OTP | Paramétrables | TTL 10 min, 6 chiffres ; max N tentatives |
| Libellé exact du type pilote | Congé · Absence · autre RH | **Demande de congé** (schéma §8.1) |

---

## 15. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-18 | Équipe produit / cadrage | Création de la spécification cible PORTAL |
| 2026-07-24 | Équipe produit / technique | v0.2 — variantes interne/externe ; inscription interne admin + mdp provisoire + activation 1ʳᵉ connexion ; espace demandeur ; `portalAudience` ; règles PORTAL-11…15 |
| 2026-07-24 | Équipe produit / technique | Remise mdp provisoire figée : one-shot UI admin ± email |
| 2026-07-24 | Équipe produit / technique | Auth externe figée : OTP email (chaque connexion) |
| 2026-07-24 | Équipe produit / technique | Pilote MVP figé : ≥1 type interne (congé) ; externe optionnel |
