# Décision produit — Templates + étapes parallèles remplacent un moteur BPM

**Projet :** FluxPro — Gestion Électronique des Dossiers et suivi par chaîne de passation  
**Type :** Décision d’architecture / cadrage produit (ADR)  
**Statut :** **Acceptée** — applicable immédiatement  
**Date :** 22 juillet 2026  
**Décideurs :** Équipe produit / tech FluxPro  

**Références :**
- [Décision templates vs BPM](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md)
- [Guide admin onboarding type + template + alertes](./GUIDE-ADMIN-ONBOARDING-TYPE-TEMPLATE-ALERTES.md)
- [Description produit, écarts et TODO](./DESCRIPTION-PRODUIT-ECARTS-TODO.md)
- [SPEC-CHN-TPL](./SPEC-CHN-TPL.md) · [SPEC-CHN](./SPEC-CHN.md)
- [SPEC-CHN-TPL détaillée](../new%20doc/SPEC-CHN-TPL-DETAILLEE.md) — étapes parallèles, join AND
- Scripts SQL : `docs/sql/2026-07-08_chain_parallel_stages.sql`, `docs/sql/2026-07-08_seed_chain_template_parallel.sql` (template T06)

---

## 1. Décision (énoncé officiel)

> **FluxPro n’intègre pas et ne prévoit pas de moteur BPM générique** (BPMN, Camunda, Flowable, Activiti, etc.).  
> Les **templates de chaîne de passation** (`ChainTemplate` / `ChainStepTemplate`), y compris les **étapes parallèles** (plusieurs maillons au même `stepOrder`, jonction **ET**), **constituent le mécanisme officiel de configuration et d’exécution des circuits**.

Cette décision est **volontaire et pérenne** tant que les besoins métier restent des circuits administratifs formalisés (séquences + visas simultanés + délais + alertes).

---

## 2. Contexte

Les administrations (cas pilote MINTP) formalisent des **circuits de passation** : une suite de maillons hiérarchiques avec rôle responsable, délai cible et action attendue. Certains types de dossiers exigent des **visas simultanés** (ex. technique + financier + juridique) avant de continuer.

Un moteur BPM générique offrirait graphes BPMN, gateways complexes, compensation, versions de process, etc. — au prix d’une complexité opérationnelle, d’un runtime dédié et d’une courbe d’apprentissage inadaptée au pilote et au cœur produit.

FluxPro a déjà livré :

| Capacité | Mécanisme |
|----------|-----------|
| Circuit configurable par type de dossier | Template lié à un `file_type_code` |
| Maillons ordonnés (rôle, délai, action) | `ChainStepTemplate` |
| Visas / traitements simultanés | Même `stepOrder` → **étape parallèle** |
| Synchronisation avant la suite | **Join AND** : l’étape suivante ne s’active que lorsque **tous** les maillons de l’étape courante sont `COMPLETED` (`PassageService` / `PassageStageHelper`) |
| Délais & alertes | `DelaiService` + règles ALR par template |
| Exécution runtime | CHN-PASS : transmit / return / suspend / reassign |

Exemple de référence : template **T06** (« Marché — visas parallèles ») — étape 2 à plusieurs maillons parallèles.

---

## 3. Conséquences

### 3.1 Ce que l’on affirme (communication & docs)

| On dit | On ne dit pas |
|--------|----------------|
| Circuits / chaînes de passation configurables | « Moteur BPM », « workflow BPMN », « orchestration Camunda » |
| Étapes séquentielles et **parallèles** (join ET) | Gateways XOR/OR génériques, sous-processus BPMN, compensation |
| Modèles de chaîne admin (`/admin/chain-templates`) | Éditeur de process BPMN |

### 3.2 Périmètre couvert sans BPM

- Séquences linéaires de maillons
- Étapes parallèles avec jonction **ET** (tous les visas requis)
- Maillons optionnels (selon règles template existantes)
- Délais par maillon / étape, alertes et escalades paramétrables
- Traçabilité des passages sur le dossier

### 3.3 Hors périmètre volontaire (ne pas compenser par un BPM)

| Besoin BPM typique | Position FluxPro |
|--------------------|------------------|
| Modélisation BPMN / XML process | Non — config template + UI admin |
| Gateway XOR / OR / event-based | Non — join AND uniquement pour le parallèle |
| Boucles / compensation / multi-instance générique | Non — sauf évolution produit explicite hors BPM |
| Runtime externe (Camunda, Flowable…) | **Refusé** |
| Versioning de process type moteur BPM | Non (évolution template = config admin ; pas de moteur de versions BPM) |

### 3.4 Quand revisiter cette décision

Reconsidérer un moteur BPM **uniquement** si un besoin métier **prouvé** dépasse durablement templates + étapes parallèles (ex. graphes conditionnels riches multi-organisations, orchestration de systèmes externes à la BPMN). Toute révision doit être une **nouvelle décision produit** écrite, pas un ajout technique opportuniste.

---

## 4. Règles techniques de référence (rappel)

| Règle | Résumé |
|-------|--------|
| Étape | Ensemble des maillons partageant le même `step_order` / `stepOrder` |
| Parallèle | ≥ 2 maillons avec le même `stepOrder` |
| Join AND | Tous `COMPLETED` avant activation de l’étape suivante |
| Clôture | Un seul maillon `closureStep` en **dernière** étape — **pas** de parallèle sur la clôture |

```
Étape 1 ──► Étape 2 (parallèle) ──► Étape 3 ──► Clôture
              ├─ Visa A
              ├─ Visa B
              └─ Visa C
                    └── Join AND
```

---

## 5. Liens d’implémentation

| Couche | Emplacement indicatif |
|--------|------------------------|
| Modèle | `ChainTemplate`, `ChainStepTemplate` |
| Exécution | `PassageService`, `PassageStageHelper` |
| Admin API | `/api/admin/chain-templates` |
| UI | `/admin/chain-templates` (aperçu circuit / maillons parallèles) |
| Seed parallèle | T06 via SQL `2026-07-08_seed_chain_template_parallel.sql` |

---

## 6. Historique

| Date | Changement |
|------|------------|
| 2026-07-16 | Positionnement produit : BPM écarté ; templates suffisants ([DESCRIPTION-PRODUIT-ECARTS-TODO](./DESCRIPTION-PRODUIT-ECARTS-TODO.md)) |
| 2026-07-22 | **Décision officielle** : templates + étapes parallèles = substitut formalisé d’un moteur BPM |

---

*Document de décision FluxPro — à citer pour pitch, README, specs et onboarding : « pas de BPM ; chaînes + parallèles ». *
