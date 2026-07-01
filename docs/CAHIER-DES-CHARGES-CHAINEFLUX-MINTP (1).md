# Cahier des charges — FluxPro

## Application de suivi de dossiers par chaîne hiérarchique

**Version :** 1.0  
**Date :** 29 juin 2025  
**Cas pilote :** Ministère des Travaux Publics du Cameroun (MINTP)  
**Référence légale :** Décret n° 2018/461 du 07 août 2018 portant organisation et fonctionnement du MINTP

---

## Table des matières

1. [Résumé exécutif](#1-résumé-exécutif)
2. [Contexte et justification](#2-contexte-et-justification)
3. [Objectifs du projet](#3-objectifs-du-projet)
4. [Périmètre fonctionnel](#4-périmètre-fonctionnel)
5. [Acteurs et profils utilisateurs](#5-acteurs-et-profils-utilisateurs)
6. [Organisation pilote — MINTP](#6-organisation-pilote--mintp)
7. [Exigences fonctionnelles détaillées](#7-exigences-fonctionnelles-détaillées)
8. [Cas d'usage pilote](#8-cas-dusage-pilote)
9. [Modèles de chaînes de passation](#9-modèles-de-chaînes-de-passation)
10. [Règles métier et alertes](#10-règles-métier-et-alertes)
11. [Tableaux de bord et reporting](#11-tableaux-de-bord-et-reporting)
12. [Exigences non fonctionnelles](#12-exigences-non-fonctionnelles)
13. [Architecture technique](#13-architecture-technique)
14. [Modèle de données](#14-modèle-de-données)
15. [Sécurité et conformité](#15-sécurité-et-conformité)
16. [Intégrations](#16-intégrations)
17. [Plan de déploiement pilote MINTP](#17-plan-de-déploiement-pilote-mintp)
18. [Planning et livrables](#18-planning-et-livrables)
19. [Indicateurs de succès (KPI)](#19-indicateurs-de-succès-kpi)
20. [Estimation budgétaire](#20-estimation-budgétaire)
21. [Annexes](#21-annexes)

---

## 1. Résumé exécutif

### 1.1 Problème

Au sein du MINTP, comme dans la plupart des administrations camerounaises, les dossiers administratifs (courriers, marchés publics, autorisations de travaux, projets d'infrastructure) circulent entre de nombreux niveaux hiérarchiques — du Cabinet du Ministre aux cadres d'appui et aux Délégations Régionales — sans traçabilité fiable des délais et des responsabilités.

**Conséquences observées :**
- Dossiers bloqués plusieurs semaines sans identification du responsable actuel
- Impossibilité de mesurer le temps passé à chaque maillon
- Absence d'alerte précoce avant dépassement des délais réglementaires ou internes
- Difficulté à produire des statistiques de performance par direction ou par agent
- Contentieux internes sans preuve horodatée de passation

### 1.2 Solution proposée

**FluxPro** est une application web de suivi de dossiers par **chaîne de passation hiérarchique**. Chaque dossier suit un circuit prédéfini ; à chaque étape, un responsable est identifié, un délai est fixé, et le système déclenche des alertes en cas de retard.

### 1.3 Cas pilote

Le déploiement pilote couvrira **deux directions centrales** et **une délégation régionale** du MINTP, sur **trois types de dossiers** à fort volume et à retards fréquents :

| Priorité | Type de dossier | Direction pilote |
|----------|-----------------|------------------|
| P1 | Courriers entrants et correspondances officielles | Direction des Affaires Générales (DAG) |
| P1 | Dossiers de passation de marchés publics (< seuil) | Direction des Investissements et de l'Entretien Routier (DIER) |
| P2 | Demandes d'autorisation de travaux sur le domaine public routier | DRTP Centre (délégation régionale) |

### 1.4 Résultat attendu

À l'issue du pilote, le MINTP doit pouvoir répondre en temps réel à :
- *Où se trouve le dossier X ?*
- *Qui est responsable depuis combien de jours ?*
- *Quels dossiers sont en retard et de combien de jours ?*
- *Quelle direction ou quel agent retarde le plus ?*

---

## 2. Contexte et justification

### 2.1 Contexte institutionnel

Le **Ministère des Travaux Publics (MINTP)** est l'ingénieur de l'État camerounais. Il est responsable de :

- La supervision et le contrôle technique de la construction des infrastructures et bâtiments publics
- L'entretien et la protection du patrimoine routier national
- La planification, la programmation et la normalisation des activités du secteur BTP

Le MINTP est structuré autour d'un **siège à Yaoundé** (BP 15406) et de **10 Délégations Régionales des Travaux Publics (DRTP)** couvrant les 10 régions du Cameroun.

### 2.2 Cadre réglementaire applicable

| Texte | Pertinence pour FluxPro |
|-------|---------------------------|
| Décret n° 2018/461 — Organisation du MINTP | Définit la chaîne hiérarchique à modéliser |
| Loi n° 2018/012 — Lutte contre la corruption | Traçabilité et transparence des circuits |
| Décret n° 2018/275 — Marchés publics | Délais de traitement des dossiers de passation |
| Loi n° 2010/012 — Cybersécurité et cybercriminalité | Exigences de sécurité des données |
| Instruction permanente sur la gestion du courrier administratif | Délais de traitement des correspondances |

### 2.3 État des lieux du traitement actuel

| Pratique actuelle | Limite |
|-------------------|--------|
| Registre papier / cahier de transmission | Pas de recherche, pas d'alerte, pertes fréquentes |
| Classeurs Excel partagés | Versions conflictuelles, pas d'horodatage fiable |
| Courrier email interne | Pas de suivi centralisé, pas de SLA |
| WhatsApp / appels téléphoniques | Informel, non auditable |
| GED partielle (si existante) | Stocke les pièces, ne suit pas les délais par acteur |

### 2.4 Benchmark

| Solution | Adéquation MINTP |
|----------|------------------|
| Microsoft SharePoint / Teams | GED générique, pas de chaîne ni SLA natifs |
| Trello / Asana / Monday | Outils projet, pas adaptés à la hiérarchie administrative |
| Alfresco / OpenKM | GED lourde, déploiement complexe |
| Solutions e-gouvernance (GovStack, etc.) | Souvent généralistes, peu orientées « qui retarde » |
| **FluxPro** | Spécialisé passation + responsabilité + alertes |

---

## 3. Objectifs du projet

### 3.1 Objectif général

Mettre en place un système numérique permettant de **suivre chaque dossier administratif** du MINTP à travers sa **chaîne de passation**, avec **identification du responsable actuel**, **mesure des délais** et **alertes automatiques** en cas de dépassement.

### 3.2 Objectifs spécifiques

| # | Objectif | Indicateur cible (pilote 6 mois) |
|---|----------|----------------------------------|
| O1 | Réduire le temps moyen de traitement des dossiers pilotes | −30 % vs baseline |
| O2 | Identifier le responsable actuel de 100 % des dossiers actifs | 100 % |
| O3 | Alerter avant ou au dépassement de l'échéance | 95 % des alertes déclenchées à temps |
| O4 | Produire un rapport mensuel de performance par direction | Rapport automatisé |
| O5 | Garantir l'auditabilité de chaque passation | Journal inaltérable 100 % des actions |
| O6 | Atteindre 80 % d'adoption chez les utilisateurs pilotes | Taux d'utilisation active |

### 3.3 Objectifs hors périmètre (phase 1)

- Signature électronique qualifiée
- Paiement en ligne / trésorerie
- Gestion complète du cycle de marchés publics (soumission, évaluation, attribution)
- Interopérabilité avec le Tresor Public
- Application mobile native (phase 2)

---

## 4. Périmètre fonctionnel

### 4.1 Inclus — MVP pilote

```
┌─────────────────────────────────────────────────────────────┐
│                      FLUXPRO MVP                         │
├─────────────────────────────────────────────────────────────┤
│  ✓ Authentification et gestion des rôles                    │
│  ✓ Référentiel organisationnel MINTP                        │
│  ✓ Création et enregistrement de dossiers                   │
│  ✓ Modèles de chaînes de passation (templates)              │
│  ✓ Passation de maillon à maillon (transmission)            │
│  ✓ Horodatage et journal d'audit                            │
│  ✓ Calcul automatique des délais (jours ouvrés)             │
│  ✓ Alertes email + notification in-app                      │
│  ✓ Escalade hiérarchique configurable                       │
│  ✓ Tableaux de bord (agent, chef de service, directeur)     │
│  ✓ Recherche et filtres de dossiers                         │
│  ✓ Pièces jointes (PDF, images, Word)                       │
│  ✓ Export PDF du journal de passation                         │
│  ✓ Statistiques de performance par direction/agent          │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 Prévu — Phase 2 (post-pilote)

- Notifications SMS (via opérateurs locaux : MTN, Orange Cameroun)
- Application mobile (PWA ou native)
- Intégration Active Directory / LDAP du MINTP
- API ouverte pour interconnexion GED
- Workflows parallèles (visa simultané de plusieurs services)
- Tableau de bord Ministre / Cabinet
- Extension aux 10 DRTP
- Module de gestion des absences et délégation de signature

### 4.3 Exclus

- Rédaction assistée des documents administratifs
- Archivage légal à valeur probante (DLT, blockchain)
- Comptabilité et ordonnancement

---

## 5. Acteurs et profils utilisateurs

### 5.1 Matrice des rôles

| Rôle | Code | Description | Permissions clés |
|------|------|-------------|------------------|
| Super administrateur | `SUPER_ADMIN` | Équipe technique / DSI MINTP | Configuration système, référentiels, tous dossiers |
| Administrateur métier | `ADMIN_METIER` | DAG — Référent courrier | Création templates, paramétrage alertes |
| Ministre / Cabinet | `CABINET` | Cabinet du Ministre | Vue globale, dossiers sensibles, statistiques |
| Secrétaire Général | `SG` | Coordination inter-directions | Vue transversale, escalades niveau SG |
| Directeur | `DIRECTEUR` | Directeur de direction | Dashboard direction, validation, escalades |
| Chef de service / division | `CHEF_SERVICE` | Chef de division ou service | Affectation agents, suivi équipe, validation |
| Cadre / Agent traitant | `AGENT` | Agent opérationnel | Réception, traitement, transmission dossiers |
| Cadre d'appui | `APPUI` | Secrétariat, archiviste, courier | Enregistrement, transmission, classement |
| Lecteur | `LECTEUR` | Audit, contrôle | Consultation seule, export rapports |
| Délégation régionale | `DRTP` | Directeur DRTP | Périmètre régional uniquement |

### 5.2 Volume utilisateurs estimé — Pilote

| Entité | Utilisateurs actifs |
|--------|---------------------|
| Cabinet / SG | 5 |
| DAG (siège) | 25 |
| DIER (siège) | 30 |
| DRTP Centre | 20 |
| DSI / Admin | 5 |
| **Total pilote** | **~85 utilisateurs** |

Extension nationale (phase 2) : **400 à 800 utilisateurs**.

---

## 6. Organisation pilote — MINTP

### 6.1 Organigramme simplifié (siège Yaoundé)

```
                        ┌─────────────────────┐
                        │  CABINET DU MINISTRE │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────┐
                        │  SECRÉTAIRE GÉNÉRAL  │
                        └──────────┬──────────┘
           ┌───────────────────────┼───────────────────────┐
           │                       │                       │
┌──────────▼─────────┐  ┌─────────▼────────┐  ┌──────────▼─────────┐
│ Direction Affaires │  │ Direction Invest.  │  │ Direction Routes   │
│ Générales (DAG)    │  │ & Entretien Routier│  │ Rurales            │
│ ★ PILOTE P1        │  │ (DIER) ★ PILOTE P1 │  │                    │
└──────────┬─────────┘  └─────────┬──────────┘  └────────────────────┘
           │                      │
    ┌──────▼──────┐        ┌──────▼──────┐
    │ Sous-dir.   │        │ Sous-dir.   │
    │ Courrier    │        │ Marchés     │
    │ Archives    │        │ Programmation│
    └─────────────┘        └─────────────┘

                        ┌─────────────────────┐
                        │   DRTP CENTRE       │
                        │   ★ PILOTE P2       │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────┐
                        │ Services régionaux  │
                        │ Autorisations travaux│
                        └─────────────────────┘
```

### 6.2 Périmètre géographique pilote

- **Siège :** Yaoundé — immeuble du MINTP
- **Région :** DRTP du Centre (Yaoundé, Mfoundi, Lekié, etc.)
- **Langue interface :** Français (anglais en phase 2 pour Nord-Ouest / Sud-Ouest)

### 6.3 Connexion et accès

- Accès via navigateur web (Chrome, Firefox, Edge)
- Réseau : intranet MINTP + accès VPN pour DRTP
- Pas de dépendance à une connexion permanente (mode consultation dégradée en phase 2)

---

## 7. Exigences fonctionnelles détaillées

### 7.1 Module — Authentification et sécurité (AUTH)

| ID | Exigence | Priorité |
|----|----------|----------|
| AUTH-01 | Connexion par email institutionnel + mot de passe | Must |
| AUTH-02 | Politique mot de passe : 8 car. min, majuscule, chiffre, caractère spécial | Must |
| AUTH-03 | Verrouillage après 5 tentatives échouées (30 min) | Must |
| AUTH-04 | Session expire après 8 h d'inactivité | Must |
| AUTH-05 | Authentification à deux facteurs (2FA) par email — phase 2 | Should |
| AUTH-06 | Intégration LDAP/AD MINTP — phase 2 | Should |
| AUTH-07 | Journal des connexions (qui, quand, IP) | Must |

### 7.2 Module — Référentiel organisationnel (ORG)

| ID | Exigence | Priorité |
|----|----------|----------|
| ORG-01 | Arbre hiérarchique : Ministère → Direction → Division → Service → Agent | Must |
| ORG-02 | Import initial depuis fichier Excel/CSV | Must |
| ORG-03 | Affectation d'un agent à un poste et une direction | Must |
| ORG-04 | Gestion des intérims (agent absent → suppléant désigné) | Should |
| ORG-05 | Historique des affectations | Should |
| ORG-06 | Référentiel des 10 DRTP pré-configuré | Must |

### 7.3 Module — Gestion des dossiers (DOS)

| ID | Exigence | Priorité |
|----|----------|----------|
| DOS-01 | Création d'un dossier avec numéro unique auto-généré | Must |
| DOS-02 | Format numéro : `MINTP-{DIR}-{ANNÉE}-{SÉQUENCE}` ex. `MINTP-DAG-2025-0042` | Must |
| DOS-03 | Champs obligatoires : type, objet, expéditeur/bénéficiaire, date réception, priorité | Must |
| DOS-04 | Priorités : Normal, Urgent, Très urgent | Must |
| DOS-05 | Pièces jointes : PDF, DOCX, XLSX, JPG, PNG (max 20 Mo/fichier) | Must |
| DOS-06 | Application automatique d'un modèle de chaîne selon le type | Must |
| DOS-07 | Statuts dossier : Brouillon, En cours, En attente, Clôturé, Archivé, Annulé | Must |
| DOS-08 | Recherche full-text sur objet, numéro, expéditeur | Must |
| DOS-09 | Filtres : direction, statut, retard, responsable, période | Must |
| DOS-10 | Commentaires internes par maillon (non visibles extérieur) | Must |
| DOS-11 | Clôture avec motif et pièce de réponse | Must |

### 7.4 Module — Chaîne de passation (CHN)

| ID | Exigence | Priorité |
|----|----------|----------|
| CHN-01 | Définition d'un template de chaîne par type de dossier | Must |
| CHN-02 | Chaque maillon : libellé, rôle responsable, délai (jours ouvrés), action attendue | Must |
| CHN-03 | Transmission : l'agent cède le dossier au maillon suivant | Must |
| CHN-04 | Horodatage automatique à chaque transmission (date/heure, auteur) | Must |
| CHN-05 | Calcul temps passé par maillon (heures ouvrées) | Must |
| CHN-06 | Possibilité de retour au maillon précédent (avec motif obligatoire) | Must |
| CHN-07 | Possessionnaire visible : « Dossier chez M. X depuis 4 jours » | Must |
| CHN-08 | Verrouillage : un seul responsable actif par dossier à la fois | Must |
| CHN-09 | Copie informée (CC) sans responsabilité de traitement | Should |
| CHN-10 | Saut de maillon exceptionnel (avec autorisation chef de service) | Should |

### 7.5 Module — Alertes et escalade (ALR)

| ID | Exigence | Priorité |
|----|----------|----------|
| ALR-01 | Alerte J-2 : rappel au responsable actuel | Must |
| ALR-02 | Alerte J+0 : dépassement — notification responsable + chef de service | Must |
| ALR-03 | Alerte J+3 : escalade au directeur | Must |
| ALR-04 | Alerte J+7 : escalade au Secrétaire Général | Should |
| ALR-05 | Canaux : notification in-app + email | Must |
| ALR-06 | Paramétrage des seuils par template de chaîne | Must |
| ALR-07 | Désactivation alertes si dossier en statut « En attente pièce externe » | Must |
| ALR-08 | Récapitulatif quotidien des retards par directeur (digest email 7h30) | Should |
| ALR-09 | SMS via API opérateur local — phase 2 | Could |

### 7.6 Module — Tableaux de bord (DSH)

| ID | Exigence | Priorité |
|----|----------|----------|
| DSH-01 | Vue agent : mes dossiers en cours, mes retards, mes transmissions | Must |
| DSH-02 | Vue chef de service : dossiers équipe, retards, charge par agent | Must |
| DSH-03 | Vue directeur : KPI direction, top retards, délai moyen | Must |
| DSH-04 | Vue SG/Cabinet : vision transversale, heatmap par direction | Should |
| DSH-05 | Compteur dossiers : total actifs, en retard, clôturés ce mois | Must |
| DSH-06 | Graphique délai moyen par type de dossier (30/90 jours) | Must |
| DSH-07 | Classement directions par taux de respect des délais | Must |
| DSH-08 | Export CSV / PDF des rapports | Must |

### 7.7 Module — Audit et traçabilité (AUD)

| ID | Exigence | Priorité |
|----|----------|----------|
| AUD-01 | Journal inaltérable de toutes les actions (création, transmission, consultation, export) | Must |
| AUD-02 | Export PDF « Fiche de circulation » d'un dossier | Must |
| AUD-03 | Conservation des logs 5 ans minimum | Must |
| AUD-04 | Horodatage serveur (NTP synchronisé) | Must |

---

## 8. Cas d'usage pilote

### 8.1 UC-01 — Enregistrement d'un courrier entrant (DAG)

**Acteur principal :** Cadre d'appui — Service Courrier (DAG)  
**Précondition :** L'agent est authentifié  
**Déclencheur :** Arrivée d'un courrier au secrétariat du MINTP

**Scénario nominal :**

1. Le cadre d'appui crée un dossier type « Courrier entrant »
2. Il saisit : expéditeur, objet, date de réception, priorité, scan PDF
3. Le système génère le numéro `MINTP-DAG-2025-XXXX`
4. Le système applique le template « Courrier entrant standard »
5. Le dossier est transmis au maillon 1 : **Chef du Service Courrier** (délai : 1 jour ouvré)
6. Le chef oriente vers la direction concernée (maillon 2)
7. Le directeur désigne un agent traitant (maillon 3)
8. L'agent traite et prépare la réponse (maillon 4)
9. Validation chef de service → directeur → réponse expédiée
10. Clôture du dossier avec scan de la réponse

**Scénario alternatif — Retard :**

- À J+1 sans action au maillon 2 → alerte email au directeur concerné
- Le dashboard affiche : *« Courrier MINTP-DAG-2025-0042 — bloqué chez Direction X depuis 3 jours »*

---

### 8.2 UC-02 — Passation d'un dossier de marché public (DIER)

**Acteur principal :** Agent — Service Marchés Publics (DIER)  
**Contexte :** Dossier de consultation pour travaux d'entretien routier, montant inférieur au seuil de publicité

**Chaîne type (7 maillons) :**

| # | Maillon | Responsable type | Délai |
|---|---------|------------------|-------|
| 1 | Enregistrement et visa SG | Secrétariat DIER | 1 j.o. |
| 2 | Instruction technique | Ingénieur division | 5 j.o. |
| 3 | Visa financier | Sous-direction budget | 3 j.o. |
| 4 | Validation Directeur DIER | Directeur | 2 j.o. |
| 5 | Avis SG | Secrétariat Général | 3 j.o. |
| 6 | Visa Ministre (si requis) | Cabinet | 5 j.o. |
| 7 | Notification et archivage | Service courrier | 2 j.o. |

**Délai total cible :** 21 jours ouvrés

**Valeur ajoutée :** Si le dossier stagne 10 jours au maillon 3, le Directeur DIER voit immédiatement le blocage et peut intervenir — au lieu de découvrir le retard 6 semaines plus tard.

---

### 8.3 UC-03 — Autorisation de travaux sur domaine public (DRTP Centre)

**Acteur principal :** Agent DRTP Centre  
**Contexte :** Une entreprise demande autorisation pour des travaux de raccordement sur une route nationale dans la région du Centre

**Chaîne type (5 maillons) :**

| # | Maillon | Responsable | Délai |
|---|---------|-------------|-------|
| 1 | Réception demande | Secrétariat DRTP | 1 j.o. |
| 2 | Instruction technique | Ingénieur routes | 7 j.o. |
| 3 | Visite terrain (planification) | Chef service + agent | 5 j.o. |
| 4 | Validation Directeur DRTP | Directeur DRTP | 3 j.o. |
| 5 | Délivrance autorisation | Service courrier DRTP | 2 j.o. |

**Délai total cible :** 18 jours ouvrés (conforme engagement service public)

**Note :** Maillon 3 peut passer en statut « En attente pièce externe » (entreprise doit fournir plan complémentaire) — les alertes sont suspendues avec motif tracé.

---

### 8.4 UC-04 — Consultation du tableau de bord Directeur

**Acteur :** Directeur de la DIER  
**Objectif :** Piloter l'activité de sa direction

**Données affichées :**

- 47 dossiers actifs dont 8 en retard
- Délai moyen de traitement : 14 jours (cible : 21 jours)
- Top 3 agents avec retards : M. A (4 dossiers), Mme B (2), M. C (2)
- Évolution mensuelle du taux de respect des délais (graphique)
- Liste des dossiers en retard > 5 jours (action immédiate)

---

## 9. Modèles de chaînes de passation

### 9.1 Template T01 — Courrier entrant standard

```
[Réception DAG] ──1j──▶ [Orientation Chef Courrier] ──1j──▶
[Directeur destinataire] ──1j──▶ [Agent traitant] ──5j──▶
[Validation Chef Service] ──2j──▶ [Expédition réponse] ──1j──▶ [Clôture]
```

**Délai total :** 11 jours ouvrés

### 9.2 Template T02 — Courrier entrant — Priorité Très Urgent

```
[Réception DAG] ──4h──▶ [Directeur destinataire] ──4h──▶
[Agent traitant] ──1j──▶ [Validation] ──4h──▶ [Expédition] ──4h──▶ [Clôture]
```

**Délai total :** 2 jours ouvrés

### 9.3 Template T03 — Dossier marché public simplifié

```
[Enregistrement] ──1j──▶ [Instruction technique] ──5j──▶ [Visa budget] ──3j──▶
[Directeur] ──2j──▶ [SG] ──3j──▶ [Archivage] ──1j──▶ [Clôture]
```

**Délai total :** 15 jours ouvrés

### 9.4 Template T04 — Autorisation travaux domaine public

```
[Réception DRTP] ──1j──▶ [Instruction] ──7j──▶ [Visite terrain] ──5j──▶
[Directeur DRTP] ──3j──▶ [Délivrance] ──2j──▶ [Clôture]
```

**Délai total :** 18 jours ouvrés

### 9.5 Template T05 — Demande de coopération / partenariat

```
[Réception Division Coopération] ──2j──▶ [Analyse juridique] ──5j──▶
[Directeur technique] ──3j──▶ [SG] ──5j──▶ [Cabinet] ──7j──▶ [Clôture]
```

**Délai total :** 22 jours ouvrés (hors périmètre pilote, pré-configuré)

---

## 10. Règles métier et alertes

### 10.1 Calcul des délais

| Règle | Description |
|-------|-------------|
| RM-01 | Les délais sont exprimés en **jours ouvrés** (lundi–vendredi) |
| RM-02 | Jours fériés camerounais exclus (liste paramétrable : 1er jan, 11 fév, 20 mai, 1er jan, etc.) |
| RM-03 | Le délai démarre à la **réception effective** (horodatage transmission) |
| RM-04 | Les priorités « Très urgent » utilisent des délais en **heures ouvrées** (8h–17h) |
| RM-05 | Un dossier « En attente pièce externe » suspend le compteur (avec date de suspension tracée) |

### 10.2 Matrice d'escalade par défaut

| Seuil | Action | Destinataires |
|-------|--------|---------------|
| J-2 | Rappel préventif | Responsable actuel |
| J+0 (échéance dépassée) | Alerte retard | Responsable + Chef de service |
| J+3 ouvrés | Escalade niveau 1 | Directeur |
| J+7 ouvrés | Escalade niveau 2 | Secrétaire Général |
| J+15 ouvrés | Escalade niveau 3 | Cabinet (dossiers priorité Urgent/Très urgent uniquement) |

### 10.3 Règles de passation

| Règle | Description |
|-------|-------------|
| RP-01 | Une transmission doit inclure un commentaire si le dossier est en retard |
| RP-02 | Un retour en arrière nécessite un motif (minimum 20 caractères) |
| RP-03 | Seul le responsable actuel ou un supérieur hiérarchique peut forcer une réaffectation |
| RP-04 | La clôture nécessite que tous les maillons obligatoires soient validés |
| RP-05 | Un dossier « Annulé » conserve son historique (pas de suppression) |

---

## 11. Tableaux de bord et reporting

### 11.1 Rapport mensuel automatique (PDF)

Envoyé le 1er de chaque mois à chaque Directeur et au SG :

1. **Synthèse :** dossiers créés, clôturés, en cours, en retard
2. **Performance :** taux de respect des délais (%), délai moyen par type
3. **Retards :** top 10 dossiers les plus en retard
4. **Agents :** classement par charge et par respect des délais
5. **Tendances :** comparaison M vs M-1

### 11.2 Exports disponibles

| Export | Format | Destinataire |
|--------|--------|--------------|
| Fiche de circulation d'un dossier | PDF | Tous |
| Liste dossiers en retard | CSV, PDF | Directeurs, SG |
| Rapport mensuel direction | PDF | Directeurs |
| Journal d'audit | CSV | Audit, DSI |
| Statistiques nationales | PDF, XLSX | Cabinet (phase 2) |

---

## 12. Exigences non fonctionnelles

### 12.1 Performance

| ID | Exigence | Cible |
|----|----------|-------|
| PERF-01 | Temps de chargement page dashboard | < 3 secondes |
| PERF-02 | Temps de recherche dossier | < 2 secondes |
| PERF-03 | Utilisateurs simultanés (pilote) | 100 |
| PERF-04 | Utilisateurs simultanés (national) | 500 |
| PERF-05 | Disponibilité | 99,5 % (heures ouvrées) |

### 12.2 Compatibilité

| ID | Exigence |
|----|----------|
| COMP-01 | Chrome 90+, Firefox 90+, Edge 90+ |
| COMP-02 | Responsive tablette (iPad, Android) |
| COMP-03 | Résolution minimale 1280×720 |

### 12.3 Localisation

| ID | Exigence |
|----|----------|
| LOC-01 | Interface en français |
| LOC-02 | Fuseau horaire : Africa/Douala (UTC+1) |
| LOC-03 | Format date : JJ/MM/AAAA |
| LOC-04 | Devise affichée : FCFA (pour dossiers marchés) |

### 12.4 Sauvegarde et continuité

| ID | Exigence |
|----|----------|
| BCP-01 | Sauvegarde automatique quotidienne (rétention 30 jours) |
| BCP-02 | Sauvegarde hebdomadaire (rétention 1 an) |
| BCP-03 | Plan de reprise : RTO 4 h, RPO 24 h |
| BCP-04 | Hébergement avec redondance (minimum 2 zones) |

---

## 13. Architecture technique

### 13.1 Stack recommandée

```
┌─────────────────────────────────────────────────────────────┐
│                        UTILISATEURS                          │
│              (Agents MINTP — Navigateur Web)                 │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTPS
┌─────────────────────────▼───────────────────────────────────┐
│                     FRONTEND (SPA)                           │
│              Next.js 14 + React + TypeScript                 │
│              Tailwind CSS + shadcn/ui                        │
└─────────────────────────┬───────────────────────────────────┘
                          │ REST API / WebSocket
┌─────────────────────────▼───────────────────────────────────┐
│                     BACKEND API                              │
│              Node.js (NestJS) ou Python (FastAPI)            │
│              JWT Auth + RBAC                                 │
├─────────────────────────────────────────────────────────────┤
│  Services :                                                  │
│  • DossierService    • ChaineService    • AlerteService      │
│  • AuditService      • ReportService    • NotificationService│
└────────┬──────────────────────┬─────────────────────────────┘
         │                      │
┌────────▼────────┐    ┌────────▼────────┐    ┌───────────────┐
│   PostgreSQL    │    │      Redis       │    │  MinIO / S3   │
│   (données)     │    │  (cache, jobs)   │    │  (fichiers)   │
└─────────────────┘    └────────┬─────────┘    └───────────────┘
                                │
                       ┌────────▼────────┐
                       │  Worker (CRON)  │
                       │  Alertes, rapports│
                       └────────┬────────┘
                                │
                       ┌────────▼────────┐
                       │  SMTP / Email   │
                       │  (notifications)│
                       └─────────────────┘
```

### 13.2 Hébergement recommandé (Cameroun / souveraineté)

| Option | Fournisseur | Avantage |
|--------|-------------|----------|
| **Option A (recommandée pilote)** | Cloud VPS — OVHcloud ou Scaleway (datacenter EU, proximité) | Rapide à déployer, coût maîtrisé |
| **Option B (souveraineté)** | Cloud Temple ou hébergement ARSAT (Cameroun) | Données au Cameroun |
| **Option C (hybride)** | App on-premise MINTP (serveur DSI) + backup cloud | Contrôle total, accès intranet |

**Recommandation pilote :** Option A pour time-to-market, migration Option B pour généralisation nationale.

### 13.3 Environnements

| Environnement | Usage |
|---------------|-------|
| `dev` | Développement |
| `staging` | Recette MINTP (DSI + 5 utilisateurs test) |
| `production` | Pilote DAG + DIER + DRTP Centre |

---

## 14. Modèle de données

### 14.1 Entités principales

```
Organisation
├── id, nom, type (MINISTERE | DIRECTION | DIVISION | SERVICE | DRTP)
├── parent_id, code (ex: "DAG", "DIER", "DRTP-C")
└── actif

Utilisateur
├── id, email, nom, prenom, telephone
├── role (enum), organisation_id
├── suppleant_id (nullable), actif
└── created_at

TemplateChaine
├── id, code (T01..T05), libelle, description
├── type_dossier, delai_total_jours
└── actif

MaillonTemplate
├── id, template_chaine_id, ordre, libelle
├── role_responsable, delai_jours, delai_heures (nullable)
└── action_attendue

Dossier
├── id, numero (unique), type, objet
├── expediteur, beneficiaire, priorite
├── statut, template_chaine_id
├── organisation_origine_id, created_by
├── date_reception, date_cloture
└── created_at, updated_at

Passation (instance de maillon)
├── id, dossier_id, maillon_template_id, ordre
├── responsable_id, statut (EN_ATTENTE | EN_COURS | VALIDE | RETOURNE)
├── date_reception, date_transmission, date_echeance
├── delai_consomme_heures, commentaire
└── motif_retour (nullable)

PieceJointe
├── id, dossier_id, nom_fichier, mime_type
├── taille, storage_path, uploaded_by
└── created_at

Alerte
├── id, dossier_id, passation_id, type (RAPPEL | RETARD | ESCALADE)
├── destinataire_id, canal (EMAIL | IN_APP | SMS)
├── envoye, date_envoi
└── created_at

AuditLog
├── id, entite, entite_id, action, utilisateur_id
├── details (JSON), ip_address
└── created_at
```

### 14.2 Diagramme simplifié

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Organisation │◄────│  Utilisateur │────►│   Dossier    │
└──────────────┘     └──────────────┘     └──────┬───────┘
                                                  │
                     ┌──────────────┐             │
                     │TemplateChaine│◄────────────┤
                     └──────┬───────┘             │
                            │                     │
                     ┌──────▼───────┐     ┌───────▼──────┐
                     │MaillonTemplate│     │  Passation   │
                     └──────────────┘     └──────┬───────┘
                                                  │
                     ┌──────────────┐     ┌───────▼──────┐
                     │PieceJointe   │◄────│   Alerte     │
                     └──────────────┘     └──────────────┘
```

---

## 15. Sécurité et conformité

### 15.1 Mesures de sécurité

| Mesure | Implémentation |
|--------|----------------|
| Chiffrement transit | TLS 1.3 (HTTPS obligatoire) |
| Chiffrement repos | AES-256 (base + fichiers) |
| Authentification | JWT + refresh token (courte durée) |
| Autorisation | RBAC strict — un agent ne voit que son périmètre |
| Isolation DRTP | Filtrage par organisation_id — un agent DRTP Centre ne voit pas DRTP Littoral |
| Dossiers sensibles | Flag « Confidentiel » — accès restreint Cabinet/SG/Directeur |
| Audit | Log immuable, pas de DELETE sur tables audit |
| Sauvegarde chiffrée | Backups chiffrés AES-256 |

### 15.2 Conformité

- **Loi n° 2010/012** (cybersécurité Cameroun) : notification ANSSI en cas de breach
- **RGPD** : non applicable directement, mais bonnes pratiques (minimisation, droit d'accès)
- **Données personnelles** : les dossiers peuvent contenir des PII — accès journalisé

### 15.3 Classification des dossiers

| Niveau | Accès |
|--------|-------|
| Public interne | Tous agents MINTP |
| Standard | Direction concernée + SG |
| Confidentiel | Directeur + SG + Cabinet |
| Secret (phase 2) | Cabinet uniquement |

---

## 16. Intégrations

### 16.1 Phase 1 (pilote)

| Système | Type | Priorité |
|---------|------|----------|
| Email SMTP MINTP | Envoi alertes et rapports | Must |
| Import CSV référentiel agents | Données initiales | Must |
| Export PDF | Fiches de circulation | Must |

### 16.2 Phase 2

| Système | Type | Priorité |
|---------|------|----------|
| Active Directory / LDAP MINTP | Authentification SSO | Should |
| GED existante (si applicable) | Lien vers documents | Could |
| SMS MTN/Orange API | Alertes mobiles | Should |
| ANTIC / e-gov Cameroun | Interopérabilité | Could |
| Signature électronique | Visa numérique | Could |

---

## 17. Plan de déploiement pilote MINTP

### 17.1 Phase 0 — Cadrage (Semaines 1–4)

| Action | Responsable | Livrable |
|--------|-------------|----------|
| Validation cahier des charges | SG + Directeurs pilotes | CDC signé |
| Nomination comité de pilotage | Ministre / SG | Note de service |
| Inventaire types de dossiers DAG, DIER, DRTP Centre | Référents métier | Catalogue dossiers |
| Mesure baseline (délais actuels sur 30 dossiers) | Référents | Rapport baseline |
| Export annuaire agents (CSV) | DSI + RH | Fichier import |

**Comité de pilotage proposé :**
- Président : Secrétaire Général
- Membres : Directeur DAG, Directeur DIER, Directeur DRTP Centre, DSI, Référent métier

### 17.2 Phase 1 — Développement MVP (Semaines 5–16)

Sprint de 2 semaines, 6 sprints :

| Sprint | Contenu |
|--------|---------|
| S1 | Auth, référentiel org, CRUD utilisateurs |
| S2 | CRUD dossiers, pièces jointes, numérotation |
| S3 | Templates chaînes, passation, calcul délais |
| S4 | Alertes, escalade, notifications email |
| S5 | Dashboards agent/chef/directeur, recherche |
| S6 | Audit, exports PDF, recette, corrections |

### 17.3 Phase 2 — Recette et formation (Semaines 17–20)

| Action | Détail |
|--------|--------|
| Environnement staging | Mise à disposition DSI |
| Tests utilisateurs | 10 agents (3 DAG, 4 DIER, 3 DRTP) |
| Formation référents | 2 sessions × 4 h (1 par direction pilote) |
| Formation agents | 6 sessions × 2 h (groupes de 15) |
| Documentation | Guide utilisateur PDF + vidéos courtes |

### 17.4 Phase 3 — Mise en production pilote (Semaine 21)

- Bascule progressive : DAG d'abord (1 semaine), puis DIER, puis DRTP Centre
- Support hotline interne (DSI + éditeur) pendant 4 semaines
- Point hebdomadaire comité de pilotage

### 17.5 Phase 4 — Évaluation pilote (Semaines 22–36)

- Période d'observation : **3 mois**
- Mesure KPI vs baseline
- Rapport d'évaluation → décision généralisation

---

## 18. Planning et livrables

### 18.1 Timeline globale

```
Semaine  1────4────8────12───16───20───24───28───32───36
         │Cadrage│──── Développement MVP ────│Recette│
         │       │                            │Form.  │
         │       │                            │PROD   │
         │       │                            │─── Évaluation pilote (3 mois) ───│
```

**Durée totale :** 9 mois (cadrage + dev + pilote + évaluation)

### 18.2 Livrables

| # | Livrable | Phase |
|---|----------|-------|
| L1 | Cahier des charges validé | Cadrage |
| L2 | Rapport baseline délais actuels | Cadrage |
| L3 | Application web MVP déployée (staging) | Dev |
| L4 | Guide utilisateur et guide administrateur | Recette |
| L5 | Application en production (pilote) | Prod |
| L6 | Rapport mensuel automatisé (template) | Prod |
| L7 | Rapport d'évaluation pilote + recommandations | Évaluation |
| L8 | Dossier de généralisation (10 DRTP) | Évaluation |

---

## 19. Indicateurs de succès (KPI)

### 19.1 KPI opérationnels

| KPI | Baseline (estimé) | Cible pilote | Mesure |
|-----|-------------------|--------------|--------|
| Délai moyen courrier entrant | 25 jours | ≤ 15 jours | FluxPro |
| Délai moyen dossier marché simplifié | 45 jours | ≤ 25 jours | FluxPro |
| Délai moyen autorisation travaux | 35 jours | ≤ 20 jours | FluxPro |
| % dossiers avec responsable identifié | ~40 % | 100 % | FluxPro |
| % alertes déclenchées à temps | 0 % | ≥ 95 % | FluxPro |
| Taux de respect des délais internes | ~50 % | ≥ 75 % | FluxPro |

### 19.2 KPI adoption

| KPI | Cible |
|-----|-------|
| Taux de connexion hebdomadaire | ≥ 80 % des utilisateurs pilotes |
| Nombre de dossiers créés dans FluxPro vs papier | ≥ 90 % à M+2 |
| Satisfaction utilisateurs (enquête) | ≥ 3,5 / 5 |

### 19.3 KPI techniques

| KPI | Cible |
|-----|-------|
| Disponibilité | ≥ 99,5 % |
| Incidents P1 (> 4 h indisponibilité) | 0 |
| Temps de réponse support | < 4 h ouvrées |

---

## 20. Estimation budgétaire

### 20.1 Coûts de développement (MVP pilote)

| Poste | Estimation FCFA | Estimation EUR |
|-------|-----------------|----------------|
| Cadrage et conception UX | 3 000 000 | ~4 500 € |
| Développement MVP (3 dev × 4 mois) | 24 000 000 | ~36 000 € |
| Tests et recette | 2 000 000 | ~3 000 € |
| Formation et accompagnement | 2 500 000 | ~3 800 € |
| Documentation | 1 000 000 | ~1 500 € |
| **Total développement** | **32 500 000** | **~49 000 €** |

### 20.2 Coûts récurrents (annuels)

| Poste | Estimation FCFA/an | Estimation EUR/an |
|-------|--------------------|--------------------|
| Hébergement cloud + backup | 1 200 000 | ~1 800 € |
| Maintenance corrective et évolutive | 6 000 000 | ~9 000 € |
| Support utilisateurs (0,5 ETP) | 4 800 000 | ~7 200 € |
| Licences (monitoring, email) | 600 000 | ~900 € |
| **Total récurrent** | **12 600 000** | **~19 000 €** |

### 20.3 Modèle de commercialisation (post-pilote)

| Offre | Périmètre | Prix indicatif |
|-------|-----------|----------------|
| **MINTP Pilote** | 3 entités, 85 users | Forfait dev (ci-dessus) |
| **MINTP National** | 10 DRTP + siège, 500 users | 8 000 000 FCFA/mois (~12 000 €) |
| **Ministère type** | 1 ministère, 200 users | 3 500 000 FCFA/mois (~5 300 €) |
| **Collectivité** | Mairie/Département, 50 users | 800 000 FCFA/mois (~1 200 €) |

---

## 21. Annexes

### Annexe A — Glossaire

| Terme | Définition |
|-------|------------|
| Maillon | Étape élémentaire d'une chaîne de passation |
| Passation | Action de transmettre un dossier d'un maillon au suivant |
| SLA | Service Level Agreement — délai cible de traitement |
| j.o. | Jour ouvré (lundi–vendredi, hors fériés) |
| DRTP | Délégation Régionale des Travaux Publics |
| Escalade | Remontée automatique d'une alerte au niveau hiérarchique supérieur |

### Annexe B — Jours fériés Cameroun (paramétrables)

- 1er janvier — Nouvel An
- 11 février — Fête de la Jeunesse
- 1er mai — Fête du Travail
- 20 mai — Fête Nationale
- 15 août — Assomption
- 25 décembre — Noël
- Lundi de Pâques, Ascension, Lundi de Pentecôte (variables)
- Fête du Mouton (Tabaski) — variable

### Annexe C — Maquette écran (description textuelle)

**Écran « Mes dossiers » (Agent) :**

```
┌─────────────────────────────────────────────────────────────┐
│  FluxPro — MINTP          🔔 3 alertes    M. Onana ▼     │
├─────────────────────────────────────────────────────────────┤
│  Mes dossiers                                               │
│  ┌─────────┬─────────┬─────────┐                            │
│  │ En cours│ En retard│ Clôturés│                            │
│  │   12    │    2    │   45    │                            │
│  └─────────┴─────────┴─────────┘                            │
│                                                             │
│  ⚠ EN RETARD                                                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ MINTP-DAG-2025-0042 │ Courrier │ +3 jours │ [Traiter]│   │
│  │ Objet: Demande de...  │ Chez moi depuis 6 jours       │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
│  EN COURS                                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ MINTP-DIER-2025-0118 │ Marché │ J-2 │ [Transmettre]  │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Annexe D — Contacts MINTP (référence)

- **Site web :** https://www.mintp.cm
- **Email courrier :** couriermintp@mintp.cm
- **Adresse :** BP 15406 Yaoundé
- **Secrétariat Ministre :** 222 22 19 18
- **Secrétariat Général :** 222 22 67 73

### Annexe E — Risques et mitigations

| Risque | Impact | Probabilité | Mitigation |
|--------|--------|-------------|------------|
| Résistance au changement | Fort | Élevée | Formation, sponsors SG + Directeurs, double saisie temporaire |
| Connexion internet instable | Moyen | Élevée | Hébergement local/on-premise, mode dégradé phase 2 |
| Données initiales incomplètes | Moyen | Moyenne | Atelier import avec DSI et RH |
| Absence de référent métier | Fort | Moyenne | Nomination formelle dès phase cadrage |
| Dépendance prestataire | Moyen | Moyenne | Code source escrow, documentation technique |
| Sécurité / fuite de dossiers | Fort | Faible | RBAC strict, audit, hébergement sécurisé |

---

## Validation

| Rôle | Nom | Signature | Date |
|------|-----|-----------|------|
| Secrétaire Général MINTP | | | |
| Directeur DAG | | | |
| Directeur DIER | | | |
| Directeur DRTP Centre | | | |
| Chef DSI MINTP | | | |
| Chef de projet FluxPro | | | |

---

*Document rédigé pour le projet FluxPro — Cas pilote Ministère des Travaux Publics du Cameroun.*  
*Prochaine révision prévue après phase de cadrage (Semaine 4).*
