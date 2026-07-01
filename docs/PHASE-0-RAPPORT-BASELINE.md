# Phase 0 — Rapport baseline (30 dossiers / direction)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique  
**Cas pilote :** Ministère des Travaux Publics du Cameroun (MINTP)  
**Activité :** Semaine 3 (S3) — Phase 0 Cadrage  
**Responsable :** Référents métier DAG, DIER, DRTP Centre  
**Livrable :** Rapport baseline + jeux de données CSV  
**Période mesurée :** Janvier — décembre 2024 (dossiers clôturés)  
**Date du rapport :** 30 juin 2025

**Références :** [Cahier des charges](./CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md) · [Roadmap](./ROADMAP-IMPLEMENTATION-CHAINEFLUX.md) · [Inventaire types de dossiers](./PHASE-0-INVENTAIRE-TYPES-DOSSIERS.md)

> **Données fictives** reconstituées à partir de pratiques observées (registres papier, cahiers de transmission, Excel). Elles servent de référence pour le pilote FluxPro et la comparaison KPI en Phase 4.

---

## 1. Objectif

Mesurer l'**état des lieux avant déploiement** de FluxPro sur **90 dossiers clôturés** :

- **30 dossiers** — Direction des Affaires Générales (DAG) — courriers entrants
- **30 dossiers** — Direction DIER — marchés publics simplifiés
- **30 dossiers** — DRTP Centre — autorisations de travaux

Ces mesures constituent la **baseline** pour les KPI opérationnels du pilote (objectif −30 % sur les délais).

---

## 2. Méthodologie

### 2.1 Échantillon

| Direction | Type de dossier | Période | Critère de sélection |
|-----------|-----------------|---------|----------------------|
| DAG | Courrier entrant (`COUR-STD`) | 2024 | 30 derniers courriers clôturés, toutes priorités |
| DIER | Marché public simplifié (`MARCHE-SMP`) | 2024 | 30 dossiers passation &lt; seuil de publicité |
| DRTP Centre | Autorisation travaux (`AUTH-TRAV`) | 2024 | 30 autorisations délivrées ou refusées |

### 2.2 Indicateurs mesurés

| Indicateur | Définition |
|------------|------------|
| **Délai total** | Jours calendaires entre date de réception et date de clôture (pratique actuelle) |
| **Délai cible** | Délai interne cible selon template FluxPro (jours ouvrés) — référence future |
| **Responsable identifié** | Le registre permet d'identifier qui détenait le dossier au moment du blocage principal |
| **Respect délai interne** | Délai total ≤ délai cible (comparaison indicative) |
| **Maillon de blocage** | Étape où le dossier est resté le plus longtemps (reconstitution manuelle) |
| **Source des données** | Registre papier, cahier de transmission, Excel, email |

### 2.3 Limites connues

- Délais en **jours calendaires** (pas de calcul jours ouvrés — contrairement à FluxPro)
- **~40 %** des dossiers sans responsable clairement identifié dans les registres
- Circuits souvent **incomplets** sur le papier (retours non tracés, visas manquants)
- Pas d'alerte automatique : les retards sont découverts **a posteriori**

---

## 3. Synthèse des résultats

### 3.1 Délais moyens par direction

| Direction | Type | n | Délai moyen | Médiane | Min | Max | Cible pilote | Écart vs cible |
|-----------|------|---|-------------|---------|-----|-----|--------------|----------------|
| **DAG** | Courrier entrant | 30 | **24,9 j** | 25 j | 14 j | 35 j | ≤ 15 j | +9,9 j (+66 %) |
| **DIER** | Marché simplifié | 30 | **44,9 j** | 47 j | 20 j | 65 j | ≤ 25 j | +19,9 j (+80 %) |
| **DRTP Centre** | Autorisation travaux | 30 | **34,9 j** | 36 j | 18 j | 49 j | ≤ 20 j | +14,9 j (+75 %) |

Les moyennes confirment les ordres de grandeur du cahier des charges (25 j / 45 j / 35 j).

### 3.2 KPI transversaux (état actuel)

| KPI | Baseline mesurée | Cible pilote FluxPro | Écart |
|-----|------------------|----------------------|-------|
| Délai moyen courrier entrant | 24,9 j | ≤ 15 j | −40 % attendu |
| Délai moyen marché simplifié | 44,9 j | ≤ 25 j | −44 % attendu |
| Délai moyen autorisation travaux | 34,9 j | ≤ 20 j | −43 % attendu |
| Dossiers avec responsable identifié | **33 %** (30/90) | 100 % | +67 pts |
| Respect délais internes | **48 %** (43/90) | ≥ 75 % | +27 pts |
| Alertes déclenchées à temps | **0 %** | ≥ 95 % | N/A (inexistant) |

### 3.3 Répartition par direction — responsable identifié

| Direction | Responsable identifié | Respect délai interne | Dossiers hors délai cible |
|-----------|----------------------|------------------------|---------------------------|
| DAG | 30 % (9/30) | 57 % (17/30) | 100 % (30/30) |
| DIER | 30 % (9/30) | 37 % (11/30) | 100 % (30/30) |
| DRTP Centre | 37 % (11/30) | 50 % (15/30) | 97 % (29/30) |

---

## 4. Analyse par direction

### 4.1 DAG — Courriers entrants

**Constats :**

- Délai moyen **24,9 jours** pour une cible interne de **11 j.o.** (template T01)
- Aucun dossier de l'échantillon n'atteint le délai cible FluxPro en jours calendaires
- **70 %** des courriers en priorité « Normal », **22 %** « Urgent », **8 %** « Très urgent »

**Maillons les plus bloquants** (reconstitution sur registres) :

| Rang | Maillon | Occurrences |
|------|---------|-------------|
| 1 | Orientation chef courrier | 7 |
| 2 | Réception DAG | 7 |
| 3 | Expédition réponse | 5 |

**Causes récurrentes :** circuit incomplet sur registre, absence de visa intermédiaire, orientation tardive vers la direction destinataire.

**Recommandation FluxPro :** template T01/T02, alertes J-2 et escalade J+3 au directeur concerné.

---

### 4.2 DIER — Marchés publics simplifiés

**Constats :**

- Délai moyen **44,9 jours** — le plus élevé des trois familles
- Seulement **37 %** de respect des délais internes
- Blocages fréquents aux étapes transverses (SG, archivage)

**Maillons les plus bloquants :**

| Rang | Maillon | Occurrences |
|------|---------|-------------|
| 1 | Avis SG | 7 |
| 2 | Archivage | 5 |
| 3 | Validation directeur | 5 |

**Causes récurrentes :** attente visa budget, dossiers incomplets relancés sans traçabilité, files d'attente au Secrétariat Général.

**Recommandation FluxPro :** template T03, statut « En attente pièce externe » pour suspension alertes, visibilité directeur sur stagnation &gt; 5 jours (UC-04).

---

### 4.3 DRTP Centre — Autorisations de travaux

**Constats :**

- Délai moyen **34,9 jours** pour une cible de **18 j.o.** (template T04)
- Meilleur taux de responsable identifié des trois directions (**37 %**)
- Visite terrain et délivrance = principaux goulets

**Maillons les plus bloquants :**

| Rang | Maillon | Occurrences |
|------|---------|-------------|
| 1 | Délivrance autorisation | 8 |
| 2 | Validation directeur DRTP | 7 |
| 3 | Visite terrain | 6 |

**Causes récurrentes :** coordination visite terrain, plans complémentaires entreprise non tracés, délai de délivrance après validation.

**Recommandation FluxPro :** template T04, suspension alertes en « En attente pièce externe » (maillon 3), isolation périmètre DRTP Centre.

---

## 5. Sources de données actuelles

Répartition des sources utilisées pour reconstituer les 90 dossiers :

| Source | DAG | DIER | DRTP-C | Total |
|--------|-----|------|--------|-------|
| Registre papier courrier | ~35 % | ~15 % | ~25 % | ~25 % |
| Cahier de transmission | ~30 % | ~30 % | ~30 % | ~30 % |
| Classeur Excel partagé | ~25 % | ~40 % | ~25 % | ~30 % |
| Email sans suivi centralisé | ~10 % | ~15 % | ~20 % | ~15 % |

**Impact :** aucune source ne permet une mesure fiable en temps réel — d'où la valeur de FluxPro pour le suivi continu.

---

## 6. Fichiers de données

| Fichier | Contenu |
|---------|---------|
| [`data/baseline-mintp.csv`](./data/baseline-mintp.csv) | **90 dossiers** — fichier consolidé |
| [`data/baseline-dag.csv`](./data/baseline-dag.csv) | 30 courriers entrants DAG |
| [`data/baseline-dier.csv`](./data/baseline-dier.csv) | 30 marchés simplifiés DIER |
| [`data/baseline-drtp-centre.csv`](./data/baseline-drtp-centre.csv) | 30 autorisations DRTP Centre |

### Colonnes CSV

| Colonne | Description |
|---------|-------------|
| `ref_registre` | Référence registre papier (ex. `REG-DAG-2024-0012`) |
| `direction` | `DAG`, `DIER`, `DRTP-C` |
| `type_dossier` | Code type (`COUR-STD`, `MARCHE-SMP`, `AUTH-TRAV`) |
| `type_libelle` | Libellé métier |
| `objet` | Objet du dossier |
| `priorite` | Normal / Urgent / Très urgent |
| `date_reception` | Date d'enregistrement |
| `date_cloture` | Date de clôture |
| `delai_total_jours` | Délai mesuré (jours calendaires) |
| `delai_cible_jo` | Cible template FluxPro (j.o.) |
| `responsable_identifie` | `oui` / `non` |
| `respect_delai_interne` | `oui` / `non` |
| `maillon_blocage` | Étape principale de stagnation |
| `jours_au_blocage` | Durée estimée au maillon bloquant |
| `source_donnee` | Origine de la reconstitution |
| `observations` | Anomalies constatées |

**Séparateur :** `;` · **Encodage :** UTF-8

---

## 7. Objectifs chiffrés du pilote (rappel)

À comparer en **Phase 4** (semaines 22–36) avec les mesures FluxPro :

| KPI | Baseline (ce rapport) | Cible pilote | Réduction attendue |
|-----|----------------------|--------------|-------------------|
| Courrier entrant | 24,9 j | ≤ 15 j | ~40 % |
| Marché simplifié | 44,9 j | ≤ 25 j | ~44 % |
| Autorisation travaux | 34,9 j | ≤ 20 j | ~43 % |
| Responsable identifié | 33 % | 100 % | — |
| Respect délais internes | 48 % | ≥ 75 % | — |
| Alertes à temps | 0 % | ≥ 95 % | — |

---

## 8. Conclusions et actions

### Conclusions

1. Les délais actuels **dépassent systématiquement** les cibles internes sur les trois familles pilotes.
2. La **traçabilité du responsable** est insuffisante (~2/3 des dossiers sans identification fiable).
3. Les **goulets d'étranglement** diffèrent par direction : orientation courrier (DAG), visa SG (DIER), visite terrain / délivrance (DRTP).
4. L'absence d'**alertes proactives** explique la découverte tardive des retards (6 semaines ou plus pour la DIER).

### Actions Phase 0 → Phase 1

| # | Action | Responsable | Échéance |
|---|--------|-------------|----------|
| 1 | Valider ce rapport en comité de pilotage | SG + Directeurs | Fin S3 |
| 2 | Aligner templates T01–T04 sur maillons réels | Référents + PO | S4 |
| 3 | Intégrer baseline comme référence KPI dans FluxPro | Équipe dev | Sprint 5 |
| 4 | Prévoir comparaison automatique baseline vs FluxPro | PO + DSI | Phase 4 |

---

## 9. Validation

| Rôle | Nom | Date | Signature |
|------|-----|------|-----------|
| Référent métier DAG | | | |
| Référent métier DIER | | | |
| Référent métier DRTP Centre | | | |
| Chef de projet FluxPro | | | |
| Secrétaire Général | | | |

---

*Rapport baseline v1.0 — Phase 0 FluxPro — Données de démonstration.*
