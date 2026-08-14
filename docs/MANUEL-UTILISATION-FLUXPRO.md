# Manuel d’utilisation FluxPro

**Public :** agents, responsables, superviseurs, administrateurs métier et utilisateurs du portail  
**Niveau :** débutant → avancé  
**Date :** août 2026  

Ce manuel vous guide pas à pas. Vous pouvez le lire dans l’ordre (parcours pédagogique) ou aller directement à la section qui vous concerne via la table des matières.

---

## Table des matières

1. [Bienvenue dans FluxPro](#1-bienvenue-dans-fluxpro)
2. [Les 6 concepts essentiels de FluxPro](#2-les-6-concepts-essentiels-de-fluxpro)
3. [Premiers pas](#3-premiers-pas)
4. [L’interface en 5 minutes](#4-linterface-en-5-minutes)
5. [Gérer un dossier — parcours agent](#5-gérer-un-dossier--parcours-agent)
6. [La passation — le cœur du métier](#6-la-passation--le-cœur-du-métier)
7. [Piloter l’activité — responsables & managers](#7-piloter-lactivité--responsables--managers)
8. [Notifications et alertes](#8-notifications-et-alertes)
9. [L’assistant FluxPro](#9-lassistant-fluxpro)
10. [Le portail (dépôt externe / interne)](#10-le-portail-dépôt-externe--interne)
11. [Administration — mettre FluxPro en ordre de marche](#11-administration--mettre-fluxpro-en-ordre-de-marche)
12. [Qui peut faire quoi ?](#12-qui-peut-faire-quoi-)
13. [Fiches pratiques rapides](#13-fiches-pratiques-rapides)
14. [Questions fréquentes et dépannage](#14-questions-fréquentes-et-dépannage)
15. [Glossaire](#15-glossaire)

---

## 1. Bienvenue dans FluxPro

### 1.1 À quoi sert FluxPro ?

FluxPro est une **plateforme de gestion électronique des dossiers** et de **suivi des circuits de passation**.

En une phrase :  
> Chaque dossier administratif a un **responsable nommé**, un **délai**, un **historique**, et le système **alerte** quand ça traîne.

Imaginez le cahier de transmission papier… mais avec :

- une recherche instantanée (« où est le dossier X ? ») ;
- un responsable visible à chaque étape ;
- des délais mesurés en **jours ouvrés** ;
- des alertes avant et après l’échéance ;
- un tableau de bord pour piloter la charge et les retards.

### 1.2 Principales fonctionnalités

| Fonctionnalité | Description |
|----------------|-------------|
| Créer, rechercher, suivre et clôturer un **dossier** | Enregistre l’affaire administrative, lui donne un numéro et un statut tout au long du traitement |
| Joindre des **pièces** au dossier | Ajoute et télécharge les documents utiles au traitement (support du dossier) |
| Circuit de passation par **template** | Définit les étapes, rôles responsables et délais selon le type de dossier |
| Transmission, retour, suspension, réaffectation | Fait circuler le dossier d’un maillon à l’autre avec traçabilité |
| SLA en **jours / heures ouvrés** + alertes / escalades | Mesure les délais et notifie avant / après l’échéance (in-app et e-mail) |
| Tableau de bord, retards, charge, exports CSV | Donne une vue de pilotage pour anticiper les goulots et suivre la conformité |
| Organigramme, utilisateurs, rôles, permissions | Contrôle qui voit quoi et qui peut agir, selon la structure |
| Portail de dépôt et suivi de soumissions | Permet de déposer certaines demandes et de suivre leur référence |

En résumé : FluxPro pilote le **cycle de vie du dossier** et la **chaîne de responsabilités** ; les documents servent de support, pas de produit principal.

### 1.3 À qui s’adresse FluxPro ? (profils et besoins)

| Profil | Besoin typique |
|--------|----------------|
| **Agent** | Créer, instruire, transmettre un dossier |
| **Chef de service / directeur** | Valider, superviser, clôturer, voir la charge |
| **Secrétariat / lecture** | Consulter, suivre sans modifier |
| **Administrateur métier** | Types de dossiers, templates, alertes, utilisateurs |
| **Usager portail** | Déposer une demande et suivre sa référence |

---

## 2. Les 6 concepts essentiels de FluxPro

Prenez 3 minutes pour assimiler ces notions. Tout le reste du manuel découle de là.

### Concept 1 — Le dossier

C’est l’unité de travail : courrier, marché simplifié, autorisation, réclamation…  
Il a : un **numéro de référence**, un **type**, un **objet**, une **priorité**, un **statut**, des **pièces**, et une **chaîne**.

### Concept 2 — Le type de dossier

C’est la « catégorie » (ex. courrier standard, marché simplifié).  
Le type détermine quel **circuit** (template) sera appliqué à la soumission.

### Concept 3 — Le template de chaîne

C’est le **itinéraire officiel** du dossier : étape 1 → étape 2 → … → clôture.  
Chaque étape (maillon) a un **rôle responsable** et un **délai**.

```
Réception → Instruction → Visa chef → Clôture
   │            │              │           │
 Agent        Agent      Chef service   Chef
  2 j.o.       3 j.o.         2 j.o.      —
```

### Concept 4 — Le maillon (passage)

À un instant T, le dossier est **chez quelqu’un** (possessionnaire du maillon actif).  
C’est cette personne qui peut **transmettre**, **retourner**, **suspendre**, etc.

### Concept 5 — Le délai ouvré

Les délais se comptent en **jours (ou heures) ouvrés**, selon le calendrier métier (week-ends, jours fériés exclus selon configuration).

### Concept 6 — L’alerte

Quand l’échéance approche ou est dépassée, FluxPro envoie des **notifications** (in-app et, si configuré, e-mail) selon les **règles d’alerte** du template.

---

## 3. Premiers pas

### 3.1 Se connecter

1. Ouvrez l’adresse de FluxPro fournie par votre administration.
2. Saisissez votre **identifiant** (e-mail) et votre **mot de passe**.
3. Validez.

**Conseil :** si c’est votre première connexion et qu’un changement de mot de passe est exigé, suivez l’écran `/change-password`.

### 3.2 Vérifier son profil

Allez dans **Profil** (`/profile`) :

- Nom, organisation de rattachement ;
- **Rôle** et **permissions** ;
- Suppléant éventuel (intérim).

> Si un bouton ou un menu manque, commencez toujours par regarder vos permissions sur le profil. Dans 80 % des cas, « je n’ai pas accès » = permission ou rôle insuffisant.

### 3.3 Changer la langue / le thème

- **Langue** : sélecteur dans l’en-tête.
- **Thème clair / sombre** : icône soleil / lune dans l’en-tête.

---

## 4. L’interface en 5 minutes

### 4.1 Structure de l’écran

| Zone | Rôle |
|------|------|
| **Barre latérale (gauche)** | Navigation : tableau de bord, dossiers, rapports, administration |
| **En-tête** | Recherche globale, notifications, profil, assistant |
| **Zone centrale** | Contenu de la page |

### 4.2 Menus principaux

#### Métier

| Menu | Route | À quoi ça sert |
|------|-------|----------------|
| Tableau de bord | `/dashboard` | Vue d’ensemble (actifs, retards, activité) |
| Dossiers | `/files` | Liste, création, ouverture des dossiers |
| Rapports | `/rapports` | Conformité, tendances, exports |

#### Référentiels (selon droits)

| Menu | Route |
|------|-------|
| Types de dossiers | `/admin/file-types` |
| Dossiers préconfigurés | `/admin/preconfigured-dossiers` |
| Modèles de chaînes | `/admin/chain-templates` |
| Types d’alertes | `/admin/alert-types` |
| Paramètres | `/admin/settings` |

#### Organisation & sécurité (admin)

| Menu | Route |
|------|-------|
| Organigramme | `/admin/org` |
| Utilisateurs | `/admin/users` |
| Utilisateurs portail | `/admin/portal-users` |
| Rôles / Permissions | `/admin/roles`, `/admin/permissions` |
| Journal de connexions | `/admin/audit` |

### 4.3 La recherche

Dans l’en-tête, la **barre de recherche** permet de retrouver rapidement un dossier par :

- numéro de référence ;
- objet ;
- expéditeur.

Sur la liste `/files`, utilisez aussi les **filtres** (statut, type, priorité, texte).

---

## 5. Gérer un dossier — parcours agent

Objectif pédagogique de ce chapitre : créer un dossier, le soumettre, le retrouver, y joindre des pièces.

### 5.1 Créer un dossier

**Prérequis :** permission `FILES:CREATE`.

1. Menu **Dossiers** → bouton **Nouveau** (`/files/new`).
2. Choisissez le **type de dossier** (ex. courrier standard).
3. Renseignez au minimum :
   - **Objet** ;
   - **Émetteur / bénéficiaire** selon le formulaire ;
   - **Organisation** de rattachement si proposée ;
   - **Priorité** (Normal / Urgent / Très urgent).
4. Enregistrez.

À ce stade, le dossier est souvent en **brouillon** : vous pouvez encore le modifier facilement et ajouter des pièces.

**Astuce :** certains contextes proposent un **dossier préconfiguré** (formulaire déjà préparé pour un cas récurrent). Utilisez-le si votre service l’a mis en place.

### 5.2 Ajouter des pièces jointes

Sur la fiche du dossier (`/files/{id}`) :

1. Ouvrez la section pièces jointes.
2. Ajoutez les fichiers attendus (PDF, images, Office selon politique).
3. Vérifiez que les pièces nécessaires sont présentes **avant** de transmettre.

### 5.3 Soumettre le dossier (passer en circuit)

Tant que le dossier est brouillon, il n’a pas encore de circulation réelle.

1. Ouvrez le dossier.
2. Utilisez l’action **Soumettre** (libellé selon votre version UI).
3. FluxPro :
   - génère / confirme le **numéro de référence** ;
   - résout le **template** lié au type ;
   - crée la **chaîne de passation** ;
   - passe le statut à **En cours** ;
   - active le **premier maillon**.

**Si la soumission échoue** avec « template introuvable » : le type n’a pas de modèle de chaîne **actif**. Contactez un administrateur métier.

### 5.4 Retrouver et ouvrir un dossier

Quatre chemins classiques :

| Chemin | Quand l’utiliser |
|--------|------------------|
| Liste **Dossiers** + filtres | Vue de travail quotidienne |
| Recherche en-tête | Vous avez le numéro ou un bout d’objet |
| Notifications | On vous a transmis un dossier |
| **Assistant FluxPro** | Vous décrivez ce que vous cherchez (référence, objet, statut…) et suivez le lien proposé |

### 5.5 Modifier, annuler, clôturer, archiver

Ces actions se font depuis la **fiche dossier** (`/files/{id}`), sauf l’édition du brouillon.

| Situation | Action | Statuts autorisés | Permission | Conditions |
|-----------|--------|-------------------|------------|------------|
| Brouillon à corriger | **Éditer** → `/files/{id}/edit` | `DRAFT` uniquement | `FILES:UPDATE` | Objet, priorité, organisation, type, pièces… Après **soumission**, plus d’édition des métadonnées |
| Brouillon inutile | **Supprimer** (si proposé) | `DRAFT` | `FILES:DELETE` | Suppression physique ; hors brouillon → refusée |
| Abandon avant fin | **Annuler** | `DRAFT` ou `IN_PROGRESS` | `FILES:UPDATE` | **Motif obligatoire** (≥ 10 caractères). Impossible depuis `CLOSED` / `ARCHIVED` / déjà `CANCELLED` |
| Traitement terminé | **Clôturer** | `IN_PROGRESS` | `FILES:CLOSE` | Circuit au **maillon de clôture** ; **motif** (≥ 10 car.) ; **pièce de réponse** jointe au dossier. Voir aussi §6.6 |
| Conservation / sortie du quotidien | **Archiver** | `CLOSED` uniquement | `FILES:ARCHIVE` | Bouton sur fiche quand le dossier est déjà clôturé |

**Qui en pratique (matrice indicative) :**

| Profil typique | Éditer / Annuler | Clôturer | Archiver |
|----------------|------------------|----------|----------|
| Agent, Chef de service | Oui (`UPDATE`) | Non (sauf rôle enrichi) | Non |
| Directeur régional | Oui | Oui | Souvent non |
| Directeur / Admin métier / Super Admin | Oui | Oui | Oui |

**Enchaînement des statuts :**

```text
DRAFT ──soumettre──► IN_PROGRESS ──clôturer──► CLOSED ──archiver──► ARCHIVED
  │                       │
  └── annuler ◄───────────┘  → CANCELLED
```

**À retenir :**

- Annuler ≠ clôturer : annulation = abandon tracé ; clôture = fin de traitement avec réponse.
- Archivage = étape **après** clôture, pas un raccourci depuis « en cours ».
- Suspendre / reprendre (attente externe) est une autre action (§6.4), distincte de l’annulation.

---

## 6. La passation — le cœur du métier

C’est le chapitre le plus important pour le travail quotidien.

### 6.1 Lire la fiche dossier comme un pro

Sur `/files/{id}`, identifiez :

1. **Statut** du dossier (brouillon, en cours, clôturé…).
2. **Maillon actif** : étape en cours + responsable.
3. **Échéance** du maillon.
4. **Historique** des transmissions (qui a fait quoi, quand).
5. **Pièces** et commentaires éventuels.

Question clé à se poser à chaque ouverture :  
> « Est-ce **moi** le possessionnaire du maillon actif ? »

Si non, vous consultez ; si oui, vous agissez.

### 6.2 Transmettre au maillon suivant

**Permission :** `FILES:TRANSMIT` **et** être possessionnaire (ou suppléant) du maillon courant.

1. Ouvrir le dossier.
2. Sur le maillon actif, cliquer **Transmettre**.
3. Confirmer le destinataire / l’étape suivante selon le template.
4. Valider.

**Effet :** le dossier quitte votre charge ; le suivant reçoit une notification ; l’historique conserve la trace.

### 6.3 Retourner en arrière

Quand une pièce manque ou qu’une correction est nécessaire :

1. Action **Retour** sur le maillon.
2. Saisir le **motif** (souvent obligatoire).
3. Valider.

Le dossier repart vers l’étape prévue par les règles métier.

### 6.4 Suspendre / reprendre

- **Suspendre** : le traitement est mis en pause (attente d’info externe, pièce manquante longue…).
- **Reprendre** : le maillon redevient actif et les délais reprennent selon les règles configurées.

### 6.5 Réaffecter

Si le responsable prévu est absent ou inadapté, une **réaffectation** peut être proposée (selon vos droits).  
Dans le cas d’un intérim planifié, le **suppléant** configuré sur l’utilisateur peut agir à sa place.

### 6.6 Clôturer et archiver

1. Le circuit doit être au **maillon de clôture** (étape finale du template).
2. Action **Clôturer** (permission `FILES:CLOSE` + rôle autorisé).
3. Puis, selon procédure, **Archiver** (`FILES:ARCHIVE`) pour sortir le dossier du quotidien opérationnel.

### 6.7 Étapes parallèles (à connaître)

Certains templates ont plusieurs maillons avec le **même numéro d’ordre** (`stepOrder`) : ce sont des **étapes parallèles** (ex. visa technique + visa financier en même temps).

**Jointure ET :** la suite du circuit ne s’active que lorsque **tous** les maillons de cette étape sont terminés. Il ne suffit pas qu’un seul visa soit donné.

**Exemple (étape 2 en parallèle) :**

| Ordre | Libellé | Qui agit |
|------:|---------|----------|
| 1 | Instruction | Agent |
| **2** | Visa technique | Responsable A |
| **2** | Visa financier | Responsable B |
| 3 | Validation chef | Chef de service |
| 4 | Clôture | … |

Tant que le visa technique **ou** le visa financier manque, l’étape 3 reste en attente.

**Pour vous au quotidien :**

- Vous ne voyez / traitez que **votre** maillon (possessionnaire ou suppléant).
- Vos collègues des autres branches parallèles traitent **en même temps**, chacun de leur côté.
- Un dossier peut sembler « bloqué » alors qu’un autre visa de la même étape n’est pas encore fait — regardez le circuit sur la fiche.
- Chaque maillon parallèle a son **propre délai** et peut générer ses **propres alertes**.

**Ce qui n’existe pas :** un « OU » (un seul visa suffit) ou un choix exclusif de branche. Le parallèle FluxPro, c’est uniquement le **ET**.

**Côté admin :** pour configurer, même `stepOrder` ou bouton « Maillon parallèle » sur le template (§11.5).

---

## 7. Piloter l’activité — responsables & managers

### 7.1 Tableau de bord (`/dashboard`)

Vue synthétique typique :

- dossiers **actifs** ;
- dossiers **en retard** ;
- créés / clôturés sur la période ;
- accès rapides vers la charge et les retards.

### 7.2 Charge des agents (`/dashboard/workload`)

Pour répondre à : *qui est saturé ?*  
Utile pour répartir le travail ou anticiper un goulot d’étranglement.

### 7.3 Top retards (`/dashboard/overdue`)

Pour répondre à : *quels dossiers dépassent, et de combien ?*  
Point de départ des revues hebdomadaires de service.

### 7.4 Rapports (`/rapports`)

Conformité, délais moyens par type, tendances.  
Export possible selon permission `DASHBOARD:EXPORT`.

**Rituel conseillé (hebdo) :**

1. Ouvrir les **retards**.
2. Traiter / escalader les plus anciens.
3. Regarder la **charge** avant d’accepter de nouveaux flux.
4. Exporter un extrait pour la réunion de direction si besoin.

---

## 8. Notifications et alertes

### 8.1 Notifications in-app

- Icône cloche dans l’en-tête.
- Page `/notifications` pour l’historique / les non lues.
- Une alerte liée à un dossier précis se consulte aussi depuis la fiche `/files/{id}`.

### 8.2 Types d’alertes courants

| Type | Signification pédagogique |
|------|---------------------------|
| **Rappel** (`REMINDER`) | L’échéance approche (ex. J−2 jours ouvrés) |
| **Retard** (`OVERDUE`) | L’échéance est atteinte / dépassée |
| **Escalade** (`ESCALATION`) | Le retard s’aggrave ; niveaux hiérarchiques alertés |

Les seuils (J−2, J+0, J+3…) sont définis par les **règles d’alerte du template**, pas « magiquement » au global.

### 8.3 Comment bien réagir à une alerte

1. Ouvrir le dossier concerné.
2. Vérifier si vous êtes bien le responsible.
3. Soit **traiter et transmettre**, soit **retourner** avec motif, soit **suspendre** si blocage externe.
4. Ne laissez pas une alerte « pour plus tard » sans action : c’est le principal source de chaines d’escalade.

---

## 9. L’assistant FluxPro

FluxPro inclut un **assistant conversationnel** (bouton / panneau de chat dans l’application).  
Il répond dans le **périmètre de vos droits** : il ne voit que ce que votre compte peut déjà consulter.

### 9.1 Ouvrir et utiliser le panneau

1. Cliquer sur l’icône **Assistant** (souvent en bas à droite).
2. Poser une question en langage courant.
3. Suivre les **liens** ou **citations** vers le dossier / l’écran proposé.
4. Vous pouvez **réduire** le panneau (historique conservé) ou le **fermer** ; rouvrir ne repart pas forcément d’une conversation vide selon l’usage de la session.

Astuce : pour retrouver un dossier, c’est l’un des quatre chemins de §5.4.

### 9.2 Ce qu’il sait faire

| Besoin | Exemple de demande |
|--------|-------------------|
| Localiser un dossier | « Où en est MINTP-DAG-2026-00042 ? » |
| Lire l’état / historique | « Quel est le maillon actuel et qui est responsable ? » |
| Vue activité / retards | « Quels sont mes dossiers en retard ? » |
| KPI / charge | « Quelle est la charge du service X ? » |
| Aide UI | « Comment transmettre un dossier ? » |
| Organigramme / agents | « Qui sont les agents du service Y ? » |
| Orienter vers un écran | « Où configurer un suppléant ? » |

### 9.3 Ce qu’il ne fait pas

L’assistant est **lecture seule**. Il :

- ne transmet, ne retourne ni ne suspend ;
- ne crée, ne modifie, ne clôture ni n’archive ;
- ne change ni un suppléant ni un rôle.

Si vous dites « transmets le dossier X », il **explique** les étapes et pointe vers `/files/{id}` — **vous** faites l’action.

### 9.4 Bien formuler

- Préférez une **référence** ou un critère précis (service, type, retard).
- Pour une procédure : commencez par **« Comment… »**.
- Une question à la fois donne de meilleures réponses.
- Si la réponse cite un dossier, ouvrez le lien pour vérifier dans la fiche.

### 9.5 Exemples de bonnes questions

- « Où en est le dossier MINTP-DAG-2026-00042 ? »
- « Quels sont mes dossiers en retard ? »
- « Comment transmettre un dossier ? »
- « Qui sont les agents du service X ? »
- « Quel est le délai moyen sur ce type de dossier ? »
- « Comment affecter un suppléant ? »

### 9.6 Si quelque chose bloque

| Symptôme | Piste |
|----------|--------|
| L’assistant refuse d’agir | Normal : lecture seule — suivez le lien UI |
| « Accès / aucun résultat » | Hors de votre périmètre ou droits insuffisants |
| Panneau indisponible | Assistant désactivé côté déploiement — voir un admin |
| Réponse trop générale | Reformulez avec une référence ou un écran précis |

---

## 10. Le portail (dépôt externe / interne)

Le portail permet de **déposer** certains types de demandes sans passer par l’application métier complète.

### 10.1 Accès

| Public | Entrée typique |
|--------|----------------|
| Agents internes (portail) | `/portal/internal/login` |
| Usagers externes | `/portal/external/login` |
| Hub portail | `/portal` |

### 10.2 Déposer une demande

1. Se connecter au portail correspondant.
2. Choisir le **formulaire / dossier préconfiguré** autorisé.
3. Remplir les champs obligatoires et joindre les pièces.
4. Soumettre.
5. **Conserver la référence** affichée : c’est votre ticket de suivi.

### 10.3 Suivre ses soumissions

- Internes : `/portal/internal/.../submissions`
- Externes : `/portal/external/.../submissions`

Ouvrez une soumission par sa **référence** pour voir l’état communiqué.

### 10.4 Administration des comptes portail

Les administrateurs gèrent les utilisateurs portail via `/admin/portal-users` (création, reset, activation selon processus).

La **configuration des formulaires** de dépôt (dossiers préconfigurés) se fait dans l’administration métier : voir **§11.7**.

---

## 11. Administration — mettre FluxPro en ordre de marche

Les chapitres précédents couvrent le travail quotidien sur les dossiers ; celui-ci s’adresse aux **administrateurs métier** et **super-administrateurs** (`BUSINESS_ADMIN` / `SUPER_ADMIN`) qui préparent le terrain : organigramme, utilisateurs, types de dossiers, templates de chaîne, alertes, dossiers préconfigurés (portail) et paramètres tenant (fuseau, calendrier ouvrés). Sans ce socle, les agents ne peuvent ni créer, ni soumettre, ni transmettre correctement — FluxPro n’invente pas les circuits : vous les configurez. La **carte** ci-dessous résume les écrans ; le **fil conducteur** §11.1 donne l’ordre recommandé. Guide pas à pas type → template → alertes : [GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md](./GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md). Détail des droits : [MATRICE-ACCES-UI.md](./MATRICE-ACCES-UI.md).

### Carte des référentiels métier

| Référentiel | Route | Rôle |
|-------------|-------|------|
| Organigramme | `/admin/org` | Structures + périmètre de visibilité |
| Utilisateurs | `/admin/users` | Comptes, rôles des maillons, suppléance |
| Types de dossiers | `/admin/file-types` | Catalogue des affaires créables |
| Templates de chaîne | `/admin/chain-templates` | Circuit + délais liés à un type |
| Types d’alertes | `/admin/alert-types` | Catalogue (RAPPEL, RETARD, ESCALADE…) |
| Règles d’alerte | Fiche d’un template | Seuils et destinataires par circuit |
| Dossiers préconfigurés | `/admin/preconfigured-dossiers` | Formulaires portail / modèles de dépôt |
| Paramètres & calendrier | `/admin/settings` | Fuseau, préfixe, jours ouvrés |
| Rôles / Permissions | `/admin/roles`, `/admin/permissions` | RBAC |
| Journal de connexions | `/admin/audit` | Traçabilité auth (souvent Super Admin) |

Sans **type actif** + **template actif** liés par le même code, la soumission d’un dossier échoue.

### 11.1 Le fil conducteur (à mémoriser)

Pour qu’un nouveau type de dossier fonctionne de bout en bout :

```
1. Organigramme + utilisateurs (rôles des maillons)
           ↓
2. Type de dossier (catalogue, actif)
           ↓
3. Template de chaîne (lié au même code type, actif)
           ↓
4. Règles d’alerte sur le template
           ↓
5. Calendrier ouvrés + paramètres tenant
           ↓
6. Smoke test : créer → soumettre → transmettre
```

**Ordre conseillé :** ne créez pas le template avant d’avoir des utilisateurs portant les **rôles** des maillons ; ne activez pas les alertes avant le calendrier ouvrés (sinon les délais / offsets seront faux).

### 11.2 Organigramme (`/admin/org`)

**Objectif :** représenter la structure institutionnelle réelle (ministère → directions → services / DRTP…).  
Cet arbre n’est pas décoratif : il fixe le **périmètre de données** (dossiers, utilisateurs, KPI) et intervient dans la **numérotation** des dossiers (code org dans la référence).

#### Où travailler

| Écran | Usage |
|-------|--------|
| `/admin/org` | Liste ou graphe, recherche, filtres, création / édition / import |
| `/admin/org/{id}` | Fiche détail d’une organisation |
| `/admin/org/types` | Types d’organisation (référentiel : ministère, direction, service…) — selon droits |

Vues utiles sur `/admin/org` : **liste** (filtres code / type / actif / parent) ou **graphe** (vision d’ensemble).

#### Créer ou modifier une organisation

1. Ouvrir **Administration → Organigramme**.
2. **Créer** (ou ouvrir une fiche / éditer une ligne existante).
3. Renseigner notamment :
   - **Code** — identifiant métier stable, unique (`DAG`, `DRCE`…) ; évitez de le changer après mise en production ;
   - **Nom** — libellé affiché ;
   - **Type** — nature de la structure ;
   - **Parent** — rattachement dans l’arbre (sauf racine, ex. ministère) ;
   - **Actif** — une org inactive ne doit plus servir de rattachement opérationnel courant.
4. **Enregistrer**.
5. Contrôler dans la liste / le graphe que la hiérarchie est correcte.

**Import :** un import CSV (upsert par `code`) peut charger un organigramme initial — utile en démarrage de site ; vérifiez un échantillon après import.

#### Impacts métier

| Domaine | Effet |
|---------|--------|
| Utilisateurs | Chaque compte est rattaché à une org → périmètre de lecture / action |
| Dossiers | Visibilité filtrée selon le scope de l’acteur (soi / sous-arbre / régional / global selon rôle) |
| Passation | Les responsables de maillons doivent être dans le bon périmètre pour être assignables |
| Références | Le code org entre souvent dans le numéro de dossier |
| Pilotage (§7) | Tableaux de bord et rapports s’appuient sur la même hiérarchie |

#### Bonnes pratiques

| Faire | Éviter |
|-------|--------|
| Codes courts, uniques, stables | Renommer un code utilisé en production |
| Aligner l’arbre sur l’organigramme officiel | Créer des « faux » services pour contourner les droits |
| **Désactiver** une structure obsolète | Supprimer alors que des dossiers / users y sont liés |
| Créer d’abord le parent, puis les enfants | Laisser des orphelins ou des cycles parent/enfant |

Voir aussi §7 (pilotage) et §11.3 (rattachement des utilisateurs).

### 11.3 Utilisateurs (`/admin/users`)

Pour chaque agent :

1. Compte actif ;
2. Rattachement à une organisation ;
3. **Rôle RBAC** cohérent avec les maillons des templates ;
4. Suppléant éventuel (voir ci-dessous).

**Checklist « utilisateur opérationnel » :**
- [ ] Peut se connecter  
- [ ] Est dans la bonne structure  
- [ ] A le bon rôle (ex. Agent, Chef de service)  
- [ ] Apparaît comme assignable sur les maillons concernés  
- [ ] Suppléant renseigné si un intérim est prévu  

#### Affecter un suppléant

**Qui :** administrateur disposant de la permission `USERS:UPDATE`.  
**Où :** fiche utilisateur du **titulaire** (celui qui sera remplacé en intérim).

Tant qu’un suppléant est désigné :

- il peut traiter les maillons / dossiers du titulaire ;
- il reçoit les alertes du titulaire.

**Procédure :**

1. Ouvrir **Administration → Utilisateurs** (`/admin/users`).
2. Ouvrir la **fiche** du titulaire.
3. Cliquer **Modifier** (depuis la fiche ou la carte **Suppléance**).
4. Dans le formulaire, section **Suppléant** :
   - **Organisation du suppléant** — structure où se trouve le remplaçant ;
   - **Utilisateur suppléant** — personne désignée (ou **Aucun** pour retirer).
5. **Enregistrer**.

**Vérification :**

- Sur la fiche du **titulaire**, carte **Suppléance** → sous-carte **Suppléant** : la personne désignée.
- Sur la fiche du **suppléant**, sous-carte **En suppléance de** : le titulaire doit y figurer.

**Retirer un suppléant :** même chemin d’édition → **Utilisateur suppléant** = **Aucun** → enregistrer.

| Point | Détail |
|-------|--------|
| Création de compte | Le champ n’apparaît qu’en **édition**, pas à la création |
| Organisation | Le suppléant peut appartenir à une **autre** organisation que le titulaire |
| Compte cible | Seuls les utilisateurs **actifs** (et distincts du titulaire) sont proposés |
| Suite opérationnelle | Vérifier connexion + rôle du suppléant (checklist ci-dessus) |

### 11.4 Types de dossiers (`/admin/file-types`)

Chaque dossier créé porte un **type** ; ce code relie ensuite le **template** de circuit.

| Champ | Conseil |
|-------|---------|
| **Code** | Unique, stable, majuscules (`RECLAMATION`, `COUR-STD`…). Ne le renommez pas à la légère une fois en prod |
| **Nom** (FR / EN) | Libellé affiché aux agents |
| **Actif** | Doit être coché pour apparaître à la création de dossier |
| **Ordre** | Position dans les listes déroulantes |
| **Code direction** | Optionnel (rattachement / filtre métier) |

**Contrôle :** le type apparaît dans `/files/new` avec un badge actif.  
**Attention :** un type lié à au moins un template ne doit pas être supprimé à la légère — désactivez-le plutôt.

### 11.5 Templates de chaîne (`/admin/chain-templates`)

Le template définit le **circuit** (maillons, rôles, délais) pour un type de dossier.

**Procédure :**

1. Créer le template avec le **même code type** que le type de dossier (`fileTypeCode`).
2. Renseigner code template, nom, délai total, unité (`WORKING_DAYS` recommandé).
3. Ajouter les **maillons** dans l’ordre réel du traitement :
   - libellé, rôle responsable, délai, action attendue ;
   - **même `stepOrder`** = étape parallèle (join ET — §6.7) ;
   - bouton « Maillon parallèle » pour dupliquer un ordre.
4. Prévoir **exactement un maillon de clôture**, seul en **dernière** étape, délai **0**.
5. Enregistrer, ouvrir la fiche, **Activer** le template.
6. Garder **un seul template actif par type** (sinon le moteur prend le premier trouvé).

**Exemple minimal :**

| Étape | Libellé | Rôle | Délai | Clôture |
|------:|---------|------|------:|:-------:|
| 1 | Instruction | `AGENT` | 2 j.o. | non |
| 2 | Validation chef | `SERVICE_HEAD` | 2 j.o. | non |
| 3 | Clôture | `SERVICE_HEAD` | 0 | **oui** |

**Astuce :** pour un circuit voisin, **Dupliquer** un template existant puis adapter code, type et maillons.

### 11.6 Types d’alertes et règles d’alerte

#### Types d’alertes (`/admin/alert-types`)

Catalogue des natures d’alerte (ex. `REMINDER`, `OVERDUE`, `ESCALATION`).  
En général **seedés** à l’installation : vérifiez qu’ils sont présents et actifs avant de créer des règles. Sans types, le profil standard d’alertes échoue.

#### Règles d’alerte (fiche template)

Les règles sont **toujours rattachées à un template** (pas de matrice globale magique).

Sur la fiche `/admin/chain-templates/{id}` :

1. **Ajouter les règles standard** (démarrage rapide) — copie un profil type (J−2 rappel, J+0 retard, escalades…) ; **ou**
2. **Ajouter une règle** manuellement : seuil, offset (jours/heures ouvrés), type d’alerte, maillon (ou toutes les étapes), mode cible (`CURRENT_RESPONSIBLE` ou `ROLE`), actif.

**Hors cas MINTP :** après le profil standard, **adaptez les rôles d’escalade** à votre organigramme (remplacez SG / cabinet si absents).  
Les offsets se calculent par rapport à l’échéance du maillon (`due_at`), en jours **ouvrés** — d’où l’importance du calendrier (§11.8).

### 11.7 Dossiers préconfigurés (`/admin/preconfigured-dossiers`)

Un **dossier préconfiguré** est un bundle catalogue : **type de dossier** + **formulaire dédié** + **circuit de passation** + **responsables des maillons**.  
Il sert surtout au **portail** (§10) : les usagers internes ou externes déposent une demande via ce formulaire, qui rejoint ensuite le métier. Le formulaire est **propriétaire** (1 formulaire = 1 préconfiguré, non réutilisable ailleurs).

**Permissions** (mêmes droits que les types de dossier) :

| Action | Permission |
|--------|------------|
| Consulter | `FILE_TYPES:READ` |
| Créer / modifier / désactiver | `FILE_TYPES:CREATE` ou `FILE_TYPES:UPDATE` |
| Supprimer | `FILE_TYPES:DELETE` |

#### Prérequis

Avant de créer ou d’activer un préconfiguré :

1. Type de dossier **actif** (`/admin/file-types`) ;
2. Template de chaîne **actif** lié à ce type (`/admin/chain-templates`) ;
3. Utilisateurs actifs portant les **rôles** des maillons, dans la bonne **organisation** ;
4. (Portail) comptes portail si besoin (`/admin/portal-users`).

#### Consulter la liste

Écran `/admin/preconfigured-dossiers` : code, libellé, type, circuit, responsables renseignés, portail, statut.

| Action | Effet |
|--------|--------|
| **Configurer** | Ouvre la fiche d’édition |
| **Désactiver** | Retire le préconfiguré sans le supprimer |
| **Supprimer** | Suppression définitive (confirmation) — à utiliser avec prudence |

#### Créer un dossier préconfiguré

1. Bouton **Nouveau dossier préconfiguré** → `/admin/preconfigured-dossiers/new`.
2. Remplir les blocs ci-dessous.
3. **Enregistrer**.

**Identité**

| Champ | Consignes |
|-------|-----------|
| **Code** | Unique, majuscules, stable (ex. `RH-CONGE`) |
| **Libellé** / **Libellé EN** | Noms affichés |
| **Description** | Optionnel |
| **Type de dossier** | Type **actif** du catalogue |
| **Circuit de passation** | Template **actif** cohérent avec le type |
| **Direction** / **Ordre** | Classement / filtre d’affichage |
| **Actif** | Disponible dans le référentiel |

**Responsables des maillons**

Après choix du circuit, FluxPro liste les étapes du template :

- l’**organisation** du maillon vient du template ;
- choisir un agent **du bon rôle** dans cette organisation ;
- le **1er maillon est obligatoire** ; les suivants sont optionnels.

Sans responsable sur le 1er maillon, le dépôt portail ne pourra pas démarrer correctement le circuit.

**Portail**

| Champ | Effet |
|-------|--------|
| **Visible sur le portail** | Le formulaire apparaît pour dépôt |
| **Audience** | Interne (agents) / Externe (citoyens, partenaires) / Les deux |

N’activez le portail que lorsque le **formulaire** et le **circuit** sont prêts — sinon risque de dépôts orphelins.

**Formulaire de demande**

1. **Ajouter un champ** : clé technique, libellé, type, obligatoire éventuel.
2. Types disponibles : `TEXT`, `TEXTAREA`, `NUMBER`, `DATE`, `DATETIME`, `ENUM`, `MULTI_ENUM`, `BOOLEAN`, `EMAIL`, `PHONE`.
3. Pour `ENUM` / `MULTI_ENUM` : options séparées par des virgules.

**Pièces jointes requises**

**Ajouter une PJ** : clé + libellé (ex. clé `justificatif`, libellé « Justificatif »). Ces pièces seront exigées à la soumission portail.

#### Modifier

1. Liste → **Configurer** sur la ligne concernée (`/admin/preconfigured-dossiers/{id}`).
2. Ajuster identité, responsables, portail, formulaire ou PJ.
3. **Enregistrer**.

#### Tester un dépôt bout en bout

1. Se connecter au portail correspondant à l’audience (interne ou externe) — §10.
2. Choisir le formulaire / dossier préconfiguré.
3. Remplir les champs obligatoires et joindre les pièces.
4. Soumettre → **conserver la référence**.
5. Vérifier le suivi côté portail **et** l’apparition du dossier côté métier (`/files`).

#### Erreurs fréquentes (préconfigurés)

| Symptôme | Cause probable | Correctif |
|----------|----------------|-----------|
| Formulaire absent du portail | Inactif, portail non activé, ou mauvaise audience | Activer + `Visible sur le portail` + audience |
| Impossible d’assigner un responsable | Aucun user du rôle dans l’org du maillon | Créer / rattacher des agents (§11.2–11.3) |
| Dépôt sans circuit / template introuvable | Type ou template inactif / non lié | Aligner type + template actif |
| Menu introuvable | Permission manquante | `FILE_TYPES:READ` (ou rôle admin métier) |

### 11.8 Paramètres & calendrier (`/admin/settings`)

| Paramètre | Effet |
|-----------|--------|
| Fuseau horaire | Horodatage, bascule d’année pour la numérotation |
| Préfixe / règles de référence | Format des numéros de dossier |
| Branding léger | Habillage tenant (selon config) |
| **Jours fériés / calendrier ouvrable** | SLA, délais de maillons et offsets d’alerte |

Sans calendrier crédible, les retards et alertes seront **faux**.

### 11.9 Rôles et permissions

- Catalogue : `/admin/roles`, `/admin/permissions`.
- Les **rôles des maillons** du template doivent exister et être portés par des utilisateurs dans le bon périmètre org.
- Création / modification fine des rôles : surtout **SUPER_ADMIN**.
- Après un changement de permissions : l’utilisateur doit **se reconnecter**.

Permissions utiles pour les référentiels :

| Domaine | Permissions typiques |
|---------|----------------------|
| Types de dossier | `FILE_TYPES:READ` / `CREATE` / `UPDATE` |
| Dossiers préconfigurés | mêmes droits `FILE_TYPES:*` (§11.7) |
| Templates | `CHAIN_TEMPLATES:READ` / `CREATE` / `UPDATE` |
| Règles d’alerte | `ALERT_RULES:READ` / `CREATE` / `UPDATE` |
| Utilisateurs | `USERS:READ` / `CREATE` / `UPDATE` |

### 11.10 Journal de connexions

`/admin/audit` : consultation des authentifications (souvent réservé Super Admin) — utile en investigation sécurité.

### 11.11 Checklist « référentiel opérationnel »

- [ ] Organigramme aligné sur la réalité  
- [ ] Utilisateurs actifs avec les bons rôles de maillons  
- [ ] Type de dossier créé et **actif**  
- [ ] Template lié au même code, maillons + clôture OK, **activé** (un seul actif / type)  
- [ ] Types d’alertes présents ; règles sur le template (standard adaptées ou manuelles)  
- [ ] Calendrier ouvrés + fuseau / préfixe  
- [ ] Smoke test : `/files/new` → soumettre → circuit visible → transmettre  
- [ ] (Portail) dossier préconfiguré testé si besoin  

**Erreurs fréquentes :**

| Symptôme | Cause probable | Correctif |
|----------|----------------|-----------|
| Soumission : template introuvable | Pas de template **actif** pour ce type | Lier le type + activer |
| Type absent à la création | Type inactif | Réactiver le type |
| Personne assignable | Aucun user avec le rôle du maillon dans le périmètre | Rattacher / corriger rôles |
| Délais / alertes incohérents | Calendrier ou fuseau | `/admin/settings` |
| Profil standard d’alertes échoue | Types d’alertes manquants | Vérifier `/admin/alert-types` |
| Formulaire portail absent / dépôt KO | Préconfiguré inactif ou mal lié | §11.7 |

---

## 12. Qui peut faire quoi ?

Synthèse pédagogique (détail : [MATRICE-ACCES-UI.md](./MATRICE-ACCES-UI.md)).

| Rôle | Capacités typiques |
|------|--------------------|
| **Super Admin** | Tout : config + dossiers cycle complet + audit |
| **Admin métier** | Config métier + dossiers jusqu’à archive |
| **Directeur / Dir. régional** | Pilotage dossiers, clôture (archivage selon rôle) |
| **Chef de service** | Traite / transmet ; souvent sans clôture |
| **SG / Cabinet** | Supervision / lecture + rapports |
| **Agent / Support** | Création, instruction, transmission sur ses maillons |
| **Lecteur** | Consultation |

**Règle d’or sur la fiche dossier :**  
même avec `FILES:TRANSMIT`, les boutons de passation n’apparaissent que si vous êtes **possessionnaire** (ou suppléant) du maillon actif — sauf profils admin globaux selon règles techniques.

---

## 13. Fiches pratiques rapides

### Fiche A — « On m’a donné un dossier »

1. Ouvrir la notification ou chercher le numéro.  
2. Vérifier que je suis possessionnaire.  
3. Lire objet + pièces + historique.  
4. Traiter.  
5. **Transmettre** (ou retourner avec motif).  

### Fiche B — « Créer un courrier interne »

1. `/files/new` → type courrier.  
2. Objet + priorité + pièces.  
3. Enregistrer → soumettre.  
4. Vérifier le premier maillon et le numéro.  

### Fiche C — « Revue hebdo chef de service »

1. `/dashboard/overdue` — retards.  
2. `/dashboard/workload` — charge.  
3. Traiter les blocages (réaffectation / rappel verbal + alerte).  
4. `/rapports` — tendance conformité.  

### Fiche D — « Nouveau circuit métier (admin) »

1. Type actif.  
2. Template lié + maillons + clôture.  
3. Activer template.  
4. Règles d’alerte.  
5. Compte test : créer → soumettre → transmettre.  

### Fiche E — « Je n’ai pas le bouton Transmettre »

Vérifier dans l’ordre :

1. Suis-je connecté avec le bon compte ?  
2. Suis-je bien le possessionnaire du maillon ?  
3. Ai-je `FILES:TRANSMIT` (profil) ?  
4. Le dossier est-il bien **en cours** (pas brouillon / clôturé / suspendu) ?  
5. Sinon → administrateur métier.  

---

## 14. Questions fréquentes et dépannage

### « Je ne vois pas le menu Administration »

Votre rôle n’inclut pas les permissions admin. C’est normal pour un agent. Demandez à un admin métier uniquement si votre mission l’exige.

### « Soumission impossible : template introuvable »

Aucun template **actif** n’est lié au type choisi.  
→ `/admin/chain-templates` : lier le type, activer, un seul actif par type.

### « Personne n’est assignable sur l’étape »

Aucun utilisateur avec le **rôle du maillon** dans le périmètre organisationnel.  
→ Créer / rattacher / corriger le rôle des users.

### « Les délais semblent faux »

Vérifier calendrier ouvrés + fuseau dans `/admin/settings`.

### « Mon formulaire n’apparaît pas sur le portail »

Le dossier préconfiguré n’est pas **actif**, le portail n’est pas coché, ou l’**audience** ne correspond pas (interne vs externe).  
→ `/admin/preconfigured-dossiers` — détail §11.7.

### « Je ne reçois pas les e-mails d’alerte »

Les notifications in-app peuvent fonctionner sans SMTP. Pour l’e-mail, voir le guide SMTP de votre déploiement et confirmer l’adresse du compte.

### « L’assistant refuse d’agir »

Normal : il est lecture seule. Suivez le lien UI qu’il propose et faites l’action vous-même.

### « Après changement de rôle, rien n’a changé »

Déconnexion / reconnexion pour rafraîchir le jeton de permissions.

---

## 15. Glossaire

| Terme | Définition simple |
|-------|-------------------|
| **Dossier** | Affaire administrative suivie dans FluxPro |
| **Référence** | Numéro unique du dossier |
| **Type de dossier** | Catégorie métier qui oriente le circuit |
| **Template / modèle de chaîne** | Itinéraire d’étapes + délais |
| **Maillon / passage** | Une étape du circuit pour un dossier donné |
| **Possessionnaire** | Responsable actuel du maillon actif |
| **SLA** | Délai cible du maillon / du dossier |
| **Jour ouvré** | Jour travaillé selon calendrier métier |
| **Alerte** | Notification automatique liée à un seuil de délai |
| **Escalade** | Alerte hiérarchique quand le retard s’aggrave |
| **RBAC** | Contrôle d’accès par rôles et permissions |
| **Périmètre org** | Filtre de visibilité selon l’organigramme |
| **Portail** | Espace de dépôt / suivi pour formulaires préconfigurés |
| **Dossier préconfiguré** | Bundle type + formulaire + circuit + responsables (souvent portail) |
| **Suppléant** | Personne qui agit à la place du titulaire |

---

## Annexes — documents liés

| Document | Usage |
|----------|--------|
| [Description produit](./DESCRIPTION-PRODUIT-ECARTS-TODO.md) | Positionnement et périmètre |
| [Matrice d’accès UI](./MATRICE-ACCES-UI.md) | Détail rôles × écrans × actions |
| [Guide admin type → template → alertes](./GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md) | Onboarding d’un nouveau circuit |
| [Aide assistant (KB)](./assistant/help-kb.md) | Textes d’aide « comment faire » |
| [Cahier des charges](./CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md) | Cadrage institutionnel pilote |

---

*Fin du manuel. Pour une question précise sur un écran, ouvrez aussi l’assistant FluxPro et demandez « comment faire… » — il vous renverra vers la marche à suivre dans l’interface.*
