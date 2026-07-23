# Guide admin — Onboarding hors MINTP  
## Type de dossier → template de chaîne → règles d’alerte

**Public :** administrateur métier (`BUSINESS_ADMIN` / `SUPER_ADMIN`)  
**Objectif :** rendre un **nouveau circuit de dossier** utilisable de bout en bout, sans s’appuyer sur les seeds MINTP (COUR-STD, T01–T05, etc.).  
**Durée indicative :** 15–30 min pour un circuit simple.

**Références :**
- [Décision templates vs BPM](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md)
- [Description produit / TODO](./DESCRIPTION-PRODUIT-ECARTS-TODO.md)
- [Guide SMTP](./GUIDE-CONFIGURATION-SMTP.md) — pour recevoir les e-mails d’alerte
- Screens UI : `/admin/file-types` · `/admin/chain-templates` · fiche template (panneau alertes)

---

## 1. Principe

À la **soumission** d’un dossier, FluxPro résout le circuit ainsi :

1. Le dossier porte un `fileTypeCode` (ex. `RECLAMATION`).
2. Le système cherche le **premier template actif** lié à ce code (`fileTypeCode` du template).
3. La chaîne de passation est initialisée ; les **règles d’alerte du template** s’appliquent ensuite (moteur ALR).

```
Type de dossier (catalogue)
        │  même code
        ▼
Template de chaîne (circuit + délais)
        │  règles rattachées au template
        ▼
Règles d’alerte (seuils J-2, J+0, escalades…)
```

Sans type **actif** + template **actif** lié → la soumission échoue (`FILE_TEMPLATE_NOT_FOUND_BY_TYPE`).

> **Exception MINTP uniquement :** courrier `COUR-STD` en priorité `VERY_URGENT` peut basculer sur un template dédié (T04). Hors MINTP, ignorez ce cas : un type = un template actif suffit.

---

## 2. Prérequis

| Prérequis | Où | Pourquoi |
|-----------|-----|----------|
| Compte avec droits admin | Rôles / permissions | Voir §2.1 |
| Organigramme + utilisateurs avec les **rôles** des maillons | `/admin/org`, `/admin/users` | Sinon personne ne peut être responsable d’une étape |
| Types d’alerte seedés (`REMINDER`, `OVERDUE`, `ESCALATION`…) | `/admin/alert-types` | Requis pour le profil standard et les règles manuelles |
| Calendrier ouvrés (au moins jours fériés du pays) | `/admin/settings` | SLA et offsets d’alerte en jours ouvrés |
| Paramètres tenant (timezone, préfixe référence) | `/admin/settings` | Numérotation et horodatage cohérents |
| SMTP (optionnel pour le smoke test UI) | [Guide SMTP](./GUIDE-CONFIGURATION-SMTP.md) | E-mails d’alerte |

### 2.1 Permissions utiles

| Action | Permissions |
|--------|-------------|
| Types de dossier | `FILE_TYPES:READ` + `CREATE` / `UPDATE` |
| Templates | `CHAIN_TEMPLATES:READ` + `CREATE` / `UPDATE` |
| Règles d’alerte | `ALERT_RULES:READ` + `CREATE` / `UPDATE` |
| Créer / soumettre un dossier de test | droits dossiers habituels (`FILES:*`) |

---

## 3. Étape A — Créer le type de dossier

**UI :** Administration → **Types de dossier** → `/admin/file-types` → bouton de création.

| Champ | Consignes |
|-------|-----------|
| **Code** | Unique, majuscules, stable (ex. `RECLAMATION`, `AUTH-TRVX`). Sera saisi tel quel sur les dossiers et sur le template. |
| **Nom** / **Nom EN** | Libellés affichables |
| **Description** | Optionnel |
| **Code direction** | Optionnel (filtre / rattachement organisationnel) |
| **Ordre** | Affichage dans les listes |
| **Actif** | Doit rester **coché** pour être sélectionnable à la création de dossier |

**Contrôle :** le nouveau code apparaît dans la liste, badge **Actif**.

**Astuce hors MINTP :** ne recyclez pas les codes seed (`COUR-STD`, `MARCHE-SMP`…) sauf si vous réutilisez volontairement ces circuits.

---

## 4. Étape B — Créer le template de chaîne

**UI :** Administration → **Templates de chaîne** → `/admin/chain-templates/new`.

### 4.1 En-tête

| Champ | Consignes |
|-------|-----------|
| **Code** | Unique (ex. `T-RECLAM`). Évitez d’écraser T01–T05 système. |
| **Nom** / **Description** | Clairs pour les agents |
| **Type de dossier** | Sélectionner le **même code** qu’à l’étape A |
| **Délai total** | Budget global en jours (ou heures) ouvrés — la somme des maxima par étape (hors clôture) ne doit pas le dépasser |
| **Unité** | `WORKING_DAYS` (recommandé) ou `WORKING_HOURS` |

### 4.2 Maillons (circuit)

Ajoutez les étapes dans l’ordre du traitement réel.

Pour chaque maillon :

| Champ | Consignes |
|-------|-----------|
| **Ordre d’étape** (`stepOrder`) | 1, 2, 3… — **même numéro** = étape **parallèle** (visas simultanés, join ET) |
| **Libellé** | Ex. « Instruction », « Visa juridique » |
| **Rôle responsable** | Rôle RBAC présent chez vos utilisateurs (ex. `AGENT`, `SERVICE_HEAD`) |
| **Délai** | SLA du maillon |
| **Action attendue** | Optionnel (visa, instruction…) |
| **Optionnel** | Maillon non bloquant si coché (selon règles produit) |
| **Clôture** | Exactement **un** maillon de clôture, **seul** en dernière étape, délai 0 |

**Parallèle :** bouton « Maillon parallèle » sur une ligne → duplique le `stepOrder`. Tous les maillons de l’étape doivent être terminés avant la suite ([décision BPM](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md)).

**Exemple minimal (séquentiel) :**

| Étape | Libellé | Rôle | Délai | Clôture |
|-------|---------|------|-------|---------|
| 1 | Réception / instruction | `AGENT` | 2 j.o. | non |
| 2 | Validation chef | `SERVICE_HEAD` | 2 j.o. | non |
| 3 | Clôture | `SERVICE_HEAD` | 0 | **oui** |

Enregistrez, puis ouvrez la **fiche détail** du template (`/admin/chain-templates/{id}`).

### 4.3 Activer le template

Sur la fiche : **Activer** si le badge est inactif.  
Seul un template **actif** lié au type est pris à la soumission.

> S’il existe déjà un autre template actif pour le même `fileTypeCode`, le moteur prend le **premier** trouvé. Gardez **un seul template actif par type** hors cas urgents MINTP.

---

## 5. Étape C — Configurer les règles d’alerte

**UI :** même fiche template → panneau **Règles d’alerte** (bas de page).

Les règles sont **toujours rattachées à un template** : pas de matrice globale magique.

### 5.1 Option rapide — profil standard

Si aucune règle n’existe encore :

1. Cliquez **Ajouter les règles standard**.
2. Confirmez. Le système **copie** le profil CDC (éditable ensuite) :

| Seuil | Offset | Type | Destinataire | Notes |
|-------|--------|------|--------------|-------|
| `J_MINUS_2` | −2 j.o. | `REMINDER` | Responsable actuel | Avant échéance |
| `J_PLUS_0` | 0 | `OVERDUE` | Responsable actuel | À l’échéance |
| `J_PLUS_0` | 0 | `OVERDUE` | Rôle `SERVICE_HEAD` | Info chef |
| `J_PLUS_3` | +3 | `ESCALATION` niv. 1 | `DIRECTOR` | |
| `J_PLUS_7` | +7 | `ESCALATION` niv. 2 | `SECRETARY_GENERAL` | |
| `J_PLUS_15` | +15 | `ESCALATION` niv. 3 | `EXECUTIVE_OFFICE` | Scope `URGENT_PLUS` |

Les offsets sont relatifs à `due_at` du passage (jours/heures **ouvrés**).

**Hors MINTP :** après application, **adaptez les rôles cibles** à votre organigramme (ex. remplacez `SECRETARY_GENERAL` / `EXECUTIVE_OFFICE` si ces rôles n’existent pas chez vous).

### 5.2 Option manuelle — règle par règle

**Ajouter une règle** :

| Champ | Signification |
|-------|----------------|
| **Code seuil** | Identifiant libre en majuscules (ex. `J_PLUS_3`) |
| **Offset** + unité | Décalage vs échéance du maillon (`WORKING_DAYS` / `WORKING_HOURS`) |
| **Type d’alerte** | Parmi les types actifs (`/admin/alert-types`) |
| **Maillon** | Un maillon précis, ou toutes les étapes |
| **Mode cible** | `CURRENT_RESPONSIBLE` ou `ROLE` (+ rôle) |
| **Portée priorité** | Optionnel (ex. dossiers urgents seulement) |
| **Actif** | Doit être coché pour être évalué |

Activez / désactivez ou supprimez une règle depuis le tableau.

---

## 6. Smoke test (validation)

1. **Créer un dossier** (`/files/new`) avec le nouveau type.
2. **Enregistrer et soumettre** (ou soumettre depuis la fiche brouillon).
3. Vérifier :
   - numéro de référence généré ;
   - statut **En cours** ;
   - circuit affiché avec le premier maillon actif ;
   - responsable cohérent avec le rôle du maillon.
4. (Optionnel) Transmettre / retourner une étape pour valider la passation.
5. (Optionnel) Avec horloge de test + SMTP configuré, avancer le temps pour déclencher une alerte `J_MINUS_2` / `J_PLUS_0`.

### Erreurs fréquentes

| Symptôme | Cause probable | Correctif |
|----------|----------------|-----------|
| Soumission : template introuvable | Pas de template **actif** avec ce `fileTypeCode` | Lier le type sur le template + activer |
| Type absent à la création | Type inactif ou non créé | Réactiver / créer le type |
| Personne assignable | Aucun user avec le `responsibleRole` dans le périmètre org | Créer / rattacher des users |
| Profil standard échoue | Types d’alerte manquants | Vérifier `/admin/alert-types` (seed SQL alert types) |
| Pas d’e-mail | SMTP / redirect | [Guide SMTP](./GUIDE-CONFIGURATION-SMTP.md) |
| Délais incohérents | Calendrier / timezone | Paramètres + calendrier ouvrés |

---

## 7. Checklist onboarding (hors MINTP)

- [ ] Type de dossier créé, **actif**, code stable
- [ ] Template créé, `fileTypeCode` = code du type
- [ ] Maillons valides (clôture seule en dernière étape ; parallèles OK si besoin)
- [ ] Template **activé** (un seul actif par type)
- [ ] Règles d’alerte : profil standard appliqué **ou** règles manuelles
- [ ] Rôles cibles d’escalade adaptés à l’organisation
- [ ] Utilisateurs présents pour chaque rôle de maillon
- [ ] Smoke test : création → soumission → circuit visible
- [ ] (Prod) SMTP + timezone + préfixe référence

---

## 8. Variantes utiles

| Besoin | Approche |
|--------|----------|
| Reprendre un circuit existant | Fiche template → **Dupliquer** → changer code + type + maillons |
| Visas simultanés | Même `stepOrder` / bouton maillon parallèle |
| Circuit sans alertes e-mail | Laisser règles vides ou inactives (suivi UI seulement) |
| Plusieurs organisations | Même type/template ; les responsables se résolvent via l’org du dossier |
| Branding / timezone non MINTP | `/admin/settings` (tenant lite) |

---

## 9. Ce que ce guide ne couvre pas

- Import massif de templates (CSV) — hors périmètre actuel  
- Moteur BPM — [refusé](./DECISION-TEMPLATES-PARALLELES-VS-BPM.md)  
- Création de rôles RBAC custom avancée — `/admin/roles`  
- Politique d’archivage / rétention  

---

*Guide admin FluxPro — onboarding circuit hors seeds MINTP — 22 juillet 2026*
