# Phase 0 — Inventaire des types de dossiers (DAG, DIER, DRTP)

**Projet :** FluxPro — Suivi de dossiers par chaîne hiérarchique  
**Cas pilote :** Ministère des Travaux Publics du Cameroun (MINTP)  
**Activité :** Semaine 2 (S2) — Phase 0 Cadrage  
**Responsable :** Référents métier  
**Livrable :** Catalogue dossiers v1  
**Références :** [Cahier des charges](./CAHIER-DES-CHARGES-CHAINEFLUX-MINTP%20(1).md) · [Roadmap](./ROADMAP-IMPLEMENTATION-CHAINEFLUX.md)

---

## 1. Objectif de l'activité

Recenser et valider les **types de dossiers réellement traités** par les trois entités pilotes du MINTP, avant :

- la mesure de baseline (S3) ;
- le maquettage UX (S3) ;
- le paramétrage des templates de chaîne (Sprint 3 dev) ;
- la priorisation du backlog produit (S4).

L'inventaire ne part pas de zéro : le cahier des charges fixe **trois familles prioritaires** (une par entité). L'atelier S2 doit les **confirmer, détailler et compléter** avec les variantes métier observées sur le terrain.

---

## 2. Cadre projet

| Élément | Détail |
|---------|--------|
| **Quand** | S2 (Phase 0, semaines 1–4) |
| **Qui** | Référents métier DAG, DIER, DRTP Centre |
| **Livrable** | Catalogue dossiers v1 |
| **Critère de sortie Phase 0** | Catalogue validé, baseline alignée, backlog priorisé |

---

## 3. Entités pilotes

| Entité | Signification | Rôle dans le pilote | Utilisateurs estimés |
|--------|---------------|---------------------|----------------------|
| **DAG** | Direction des Affaires Générales (siège Yaoundé) | Courriers entrants et correspondances | ~25 |
| **DIER** | Direction des Investissements et de l'Entretien Routier | Marchés publics simplifiés (&lt; seuil) | ~30 |
| **DRTP Centre** | Délégation Régionale des Travaux Publics — région Centre | Autorisations de travaux sur le domaine public routier | ~20 |

**Total pilote :** ~85 utilisateurs (+ 5 Cabinet/SG, 5 DSI/Admin).

### Organigramme simplifié (périmètre pilote)

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
┌──────────▼─────────┐  ┌─────────▼────────┐
│ Direction Affaires │  │ Direction Invest.  │
│ Générales (DAG)    │  │ & Entretien Routier│
│ ★ PILOTE P1        │  │ (DIER) ★ PILOTE P1 │
└──────────┬─────────┘  └─────────┬──────────┘
           │                      │
    ┌──────▼──────┐        ┌──────▼──────┐
    │ Sous-dir.   │        │ Sous-dir.   │
    │ Courrier    │        │ Marchés     │
    └─────────────┘        └─────────────┘

                        ┌─────────────────────┐
                        │   DRTP CENTRE       │
                        │   ★ PILOTE P2       │
                        └──────────┬──────────┘
                                   │
                        ┌──────────▼──────────┐
                        │ Autorisations travaux│
                        └─────────────────────┘
```

---

## 4. Types de dossiers cibles du MVP

| Priorité | Type de dossier | Entité | Code direction | Template | Cas d'usage | Délai cible |
|----------|-----------------|--------|----------------|----------|-------------|-------------|
| **P1** | Courrier entrant standard | DAG | `DAG` | **T01** | UC-01 | 11 j.o. |
| **P1** | Courrier entrant — Très urgent | DAG | `DAG` | **T02** | UC-01 (variante) | 2 j.o. |
| **P1** | Marché public simplifié | DIER | `DIER` | **T03** | UC-02 | 15 j.o. |
| **P2** | Autorisation travaux domaine public | DRTP Centre | `DRTP-C` | **T04** | UC-03 | 18 j.o. |
| Hors pilote actif | Coopération / partenariat | — | — | **T05** | — | 22 j.o. (pré-configuré) |

**Numérotation :** `MINTP-{DIR}-{ANNÉE}-{SÉQUENCE}` — ex. `MINTP-DAG-2025-0042`

**Priorités dossier :** Normal · Urgent · Très urgent

**Statuts dossier :** Brouillon · En cours · En attente · Clôturé · Archivé · Annulé

---

## 5. Contenu attendu du Catalogue dossiers v1

Pour **chaque type de dossier**, documenter :

### 5.1 Identification

- Libellé métier (ex. « Courrier entrant standard »)
- Code technique (`COUR-STD`, `MARCHE-SMP`, etc.)
- Code direction (`DAG`, `DIER`, `DRTP-C`)
- Priorité pilote (P1 / P2 / hors périmètre)
- Volume mensuel estimé
- Service responsable de l'enregistrement

### 5.2 Champs obligatoires (exigence DOS-03)

| Champ | Obligatoire | Notes |
|-------|-------------|-------|
| Type de dossier | Oui | Détermine le template appliqué (DOS-06) |
| Objet | Oui | Recherche full-text |
| Expéditeur / bénéficiaire | Oui | Selon le type |
| Date de réception | Oui | Point de départ des délais |
| Priorité | Oui | Normal / Urgent / Très urgent |

Champs spécifiques à inventorier par type (ex. montant marché, localisation travaux, référence courrier).

### 5.3 Chaîne de passation

Pour chaque maillon :

- Libellé
- Rôle responsable (ex. `CHEF_SERVICE`, `DIRECTEUR`, `AGENT`)
- Délai (jours ouvrés ou heures ouvrées si Très urgent)
- Action attendue
- Possibilité de retour arrière (motif ≥ 20 caractères)
- Cas de suspension (ex. « En attente pièce externe »)

### 5.4 Pièces jointes typiques

- Formats acceptés : PDF, DOCX, XLSX, JPG, PNG (max 20 Mo/fichier)
- Documents habituellement joints par type de dossier

### 5.5 Règles métier spécifiques

- Conditions d'application du template
- Escalade hiérarchique particulière
- Exceptions (visa Ministre, visite terrain, etc.)

---

## 6. Détail par entité

### 6.1 DAG — Courriers entrants

| Élément | Détail |
|---------|--------|
| **Acteur principal** | Cadre d'appui — Service Courrier |
| **Déclencheur** | Arrivée d'un courrier au secrétariat du MINTP |
| **Templates** | T01 (standard) · T02 (très urgent) |
| **Baseline actuelle** | ~25 jours (cible pilote : ≤ 15 j) |

#### Circuit T01 — Courrier entrant standard (7 maillons, 11 j.o.)

| # | Maillon | Responsable type | Délai |
|---|---------|------------------|-------|
| 1 | Réception DAG | Cadre d'appui / secrétariat | 1 j.o. |
| 2 | Orientation Chef Courrier | Chef Service Courrier | 1 j.o. |
| 3 | Directeur destinataire | Directeur | 1 j.o. |
| 4 | Agent traitant | Agent | 5 j.o. |
| 5 | Validation Chef Service | Chef de service | 2 j.o. |
| 6 | Expédition réponse | Service courrier | 1 j.o. |
| 7 | Clôture | — | — |

#### Circuit T02 — Courrier très urgent (6 maillons, 2 j.o.)

Circuit accéléré : délais en **heures ouvrées** (4 h par maillon sur les étapes critiques).

#### Scénario alternatif — Retard

- À J+1 sans action → alerte email au directeur concerné
- Dashboard : *« Courrier MINTP-DAG-2025-0042 — bloqué chez Direction X depuis 3 jours »*

---

### 6.2 DIER — Marchés publics simplifiés

| Élément | Détail |
|---------|--------|
| **Acteur principal** | Agent — Service Marchés Publics |
| **Contexte** | Dossier &lt; seuil de publicité (consultation simplifiée) |
| **Template** | T03 |
| **Baseline actuelle** | ~45 jours (cible pilote : ≤ 25 j) |
| **Hors périmètre** | Cycle complet marchés (soumission, évaluation, attribution) |

#### Circuit T03 — Marché public simplifié (7 maillons, 15 j.o.)

| # | Maillon | Responsable type | Délai |
|---|---------|------------------|-------|
| 1 | Enregistrement et visa SG | Secrétariat DIER | 1 j.o. |
| 2 | Instruction technique | Ingénieur division | 5 j.o. |
| 3 | Visa financier | Sous-direction budget | 3 j.o. |
| 4 | Validation Directeur DIER | Directeur | 2 j.o. |
| 5 | Avis SG | Secrétariat Général | 3 j.o. |
| 6 | Visa Ministre (si requis) | Cabinet | 5 j.o. |
| 7 | Notification et archivage | Service courrier | 2 j.o. |

> **Note CDC :** le cas d'usage UC-02 décrit un délai total de 21 j.o. ; le template T03 retenu pour le MVP vise 15 j.o. — à valider en atelier S2.

---

### 6.3 DRTP Centre — Autorisations de travaux

| Élément | Détail |
|---------|--------|
| **Acteur principal** | Agent DRTP Centre |
| **Contexte** | Travaux de raccordement / occupation du domaine public routier (région Centre) |
| **Template** | T04 |
| **Baseline actuelle** | ~35 jours (cible pilote : ≤ 20 j) |
| **Spécificité** | Suspension alertes si « En attente pièce externe » |

#### Circuit T04 — Autorisation travaux (5 maillons, 18 j.o.)

| # | Maillon | Responsable type | Délai |
|---|---------|------------------|-------|
| 1 | Réception demande | Secrétariat DRTP | 1 j.o. |
| 2 | Instruction technique | Ingénieur routes | 7 j.o. |
| 3 | Visite terrain | Chef service + agent | 5 j.o. |
| 4 | Validation Directeur DRTP | Directeur DRTP | 3 j.o. |
| 5 | Délivrance autorisation | Service courrier DRTP | 2 j.o. |

> **Maillon 3 :** peut passer en statut « En attente pièce externe » (plan complémentaire entreprise) — alertes suspendues, motif tracé.

---

## 7. Modèle de fiche — Catalogue dossiers v1

### 7.1 Tableau synthétique

| code_type | libelle | direction | template | priorite_pilote | volume_mensuel | delai_cible_jo | statut_atelier |
|-----------|---------|-----------|----------|-----------------|----------------|----------------|----------------|
| COUR-STD | Courrier entrant standard | DAG | T01 | P1 | _à compléter_ | 11 | ☐ Validé |
| COUR-URG | Courrier très urgent | DAG | T02 | P1 | _à compléter_ | 2 | ☐ Validé |
| MARCHE-SMP | Marché public simplifié | DIER | T03 | P1 | _à compléter_ | 15 | ☐ Validé |
| AUTH-TRAV | Autorisation travaux | DRTP-C | T04 | P2 | _à compléter_ | 18 | ☐ Validé |

### 7.2 Fiche détaillée (par type)

```markdown
## [CODE_TYPE] — [Libellé]

**Direction :** DAG | DIER | DRTP-C
**Template FluxPro :** T01 | T02 | T03 | T04
**Priorité pilote :** P1 | P2 | Hors périmètre
**Volume mensuel estimé :**
**Service enregistreur :**

### Champs spécifiques
- ...

### Pièces jointes typiques
- ...

### Maillons (circuit réel terrain)
| # | Libellé | Rôle | Délai | Action attendue |
|---|---------|------|-------|-----------------|
| 1 | ... | ... | ... | ... |

### Écarts vs template CDC
- ...

### Règles / exceptions
- ...

### Validé par
- Nom, fonction, date
```

---

## 8. Questions d'atelier S2

À poser aux référents métier pour chaque direction :

1. Quels sont les **5 à 10 types de dossiers** les plus fréquents dans votre service ?
2. Lesquels sont **déjà couverts** par T01–T04 ? Lesquels sont hors périmètre pilote ?
3. Existe-t-il des **variantes** (urgent, visa Ministre, attente tiers) non modélisées ?
4. Quels **champs** sont systématiquement renseignés aujourd'hui (registre papier / Excel) ?
5. Le **circuit réel** correspond-il au template CDC ? Quels maillons manquent ou sont sautés ?
6. Quels **délais réels** observez-vous en moyenne (pour alimenter la baseline S3) ?
7. Quels **documents** sont joints à chaque type ?
8. Y a-t-il des cas où les **alertes doivent être suspendues** (attente externe, congés, etc.) ?

---

## 9. Participants recommandés

| Rôle | Entité | Rôle FluxPro |
|------|--------|--------------|
| Directeur ou délégué | DAG | Sponsor P1 courrier |
| Référent courrier | DAG | `ADMIN_METIER` |
| Directeur ou délégué | DIER | Sponsor P1 marchés |
| Chef Service Marchés | DIER | Validation circuit |
| Directeur DRTP Centre | DRTP | Sponsor P2 autorisations |
| Référent métier | Transverse | Animation atelier |
| DSI | Transverse | Faisabilité technique, import org |
| Chef de projet | Transverse | Consolidation catalogue |

**Comité de pilotage :** SG (président), Directeurs DAG/DIER/DRTP Centre, DSI, Référent métier.

---

## 10. Liens avec les autres activités Phase 0

| Semaine | Activité | Lien avec l'inventaire |
|---------|----------|------------------------|
| S2 | Inventaire types de dossiers | **Ce document** → Catalogue dossiers v1 |
| S2 | Export annuaire agents (CSV) | Rôles des maillons → affectation utilisateurs |
| S3 | Baseline 30 dossiers/direction | Types inventoriés = échantillon baseline |
| S3 | Maquettage UX | Formulaires création dossier par type |
| S4 | Import référentiel org MINTP | Codes direction (`DAG`, `DIER`, `DRTP-C`) |
| S4 | Backlog priorisé | Epics Sprint 2–3 alimentés par le catalogue |

### Chaîne de dépendances

```
Inventaire S2 (Catalogue v1)
    ├── Baseline S3 (délais par type)
    ├── Maquettes S3 (écrans création / fiche dossier)
    └── Sprint 3 dev (configuration templates T01–T04)
```

---

## 11. KPI pilote associés (post-baseline)

| Type de dossier | Baseline | Cible pilote |
|-----------------|----------|--------------|
| Courrier entrant | 25 j | ≤ 15 j |
| Marché simplifié | 45 j | ≤ 25 j |
| Autorisation travaux | 35 j | ≤ 20 j |

---

## 12. Prochaines étapes

| # | Action | Responsable | Échéance |
|---|--------|-------------|----------|
| 1 | Planifier atelier inventaire (½ journée × 3 directions) | Chef projet | S2 |
| 2 | Compléter le tableau §7.1 avec volumes réels | Référents métier | S2 |
| 3 | Rédiger une fiche §7.2 par type validé | Référents + PO | S2 |
| 4 | Valider le catalogue en comité de pilotage | SG + Directeurs | Fin S2 |
| 5 | Lancer baseline sur les types validés | Référents métier | S3 |
| 6 | Transmettre le catalogue à l'équipe dev (templates Sprint 3) | PO | S4 |

---

*Document v1.0 — Phase 0 FluxPro — À compléter après atelier S2.*
