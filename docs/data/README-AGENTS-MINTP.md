# Annuaire agents MINTP — Données pilote FluxPro

**Livrable Phase 0 (S2)** — Export annuaire agents pour import initial  
**Fichiers :**
- [`agents-mintp.csv`](./agents-mintp.csv) — 85 utilisateurs pilotes
- [`organisations-mintp.csv`](./organisations-mintp.csv) — Référentiel organisationnel associé

> **Données fictives** à vocation de développement, recette et démonstration. Noms, emails et numéros ne correspondent pas à des personnes réelles.

---

## Répartition (conforme CDC §5.2)

| Entité | Code(s) org | Effectif |
|--------|-------------|----------|
| Cabinet / SG | `MINTP-CABINET`, `MINTP-SG` | 5 |
| DSI / Admin | `DSI` | 5 |
| DAG | `DAG`, `DAG-COURRIER`, `DAG-ARCHIVES` | 25 |
| DIER | `DIER`, `DIER-MARCHES`, `DIER-TECH`, `DIER-BUDGET`, `DIER-PROG` | 30 |
| DRTP Centre | `DRTP-C`, `DRTP-C-AUTH`, `DRTP-C-ROUTES` | 20 |
| **Total** | | **85** |

---

## Format `agents-mintp.csv`

| Colonne | Type | Description |
|---------|------|-------------|
| `matricule` | string | Identifiant RH (ex. `MAT-2015-0001`) |
| `email` | string | Email institutionnel `@mintp.cm` (login AUTH-01) |
| `nom` | string | Nom de famille (majuscules) |
| `prenom` | string | Prénom(s) |
| `telephone` | string | Mobile Cameroun `+237 6XX XX XX XX` |
| `role` | enum | Rôle RBAC (voir ci-dessous) |
| `organisation_code` | string | Code org — jointure avec `organisations-mintp.csv` |
| `service` | string | Libellé service (affichage) |
| `fonction` | string | Intitulé de poste |
| `actif` | boolean | `true` / `false` |

**Séparateur :** point-virgule (`;`) — compatible Excel français  
**Encodage :** UTF-8

### Rôles RBAC utilisés

`SUPER_ADMIN` · `ADMIN_METIER` · `CABINET` · `SG` · `DIRECTEUR` · `CHEF_SERVICE` · `AGENT` · `APPUI` · `LECTEUR` · `DRTP`

---

## Répartition des rôles par entité

| Entité | DIRECTEUR / DRTP | CHEF_SERVICE | ADMIN_METIER | APPUI | AGENT | LECTEUR | Autres |
|--------|------------------|--------------|--------------|-------|-------|---------|--------|
| Cabinet/SG | — | — | — | 2 | 1 | — | CABINET×1, SG×1 |
| DSI | — | — | 1 | — | 1 | 1 | SUPER_ADMIN×2 |
| DAG | 1 | 3 | 1 | 4 | 14 | 2 | — |
| DIER | 1 | 4 | — | 3 | 20 | 2 | — |
| DRTP Centre | 1 (DRTP) | 3 | — | 3 | 12 | 1 | — |

---

## Import prévu (Sprint 1)

Ordre recommandé :

1. Importer `organisations-mintp.csv` (arbre hiérarchique)
2. Importer `agents-mintp.csv` (utilisateurs + affectation org + rôle)
3. Vérifier l'isolation : un agent `DRTP-C` ne voit pas les dossiers `DIER`

Mot de passe initial : à définir par la DSI (non inclus dans ce fichier).

---

## Données baseline (Phase 0 S3)

| Fichier | Contenu |
|---------|---------|
| [`baseline-mintp.csv`](./baseline-mintp.csv) | 90 dossiers consolidés (30 × 3 directions) |
| [`baseline-dag.csv`](./baseline-dag.csv) | Courriers entrants |
| [`baseline-dier.csv`](./baseline-dier.csv) | Marchés simplifiés |
| [`baseline-drtp-centre.csv`](./baseline-drtp-centre.csv) | Autorisations travaux |

Rapport : [PHASE-0-RAPPORT-BASELINE.md](../PHASE-0-RAPPORT-BASELINE.md)

---

## Références

- [Cahier des charges](../CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md) — §5, §14.1
- [Roadmap](../ROADMAP-IMPLEMENTATION-CHAINEFLUX.md) — Phase 0 S2, Sprint 1
- [Inventaire types de dossiers](../PHASE-0-INVENTAIRE-TYPES-DOSSIERS.md)
- [Rapport baseline](../PHASE-0-RAPPORT-BASELINE.md)
