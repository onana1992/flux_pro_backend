# Base de connaissances aide FluxPro

Sections consultées par l'outil `lookup_help`. Réponses **lecture seule** : explique la marche à suivre dans l'UI, sans exécuter d'action.

---

## transmettre-dossier

**Intent :** comment transmettre / envoyer / passer un dossier au maillon suivant  
**Écran :** `/files/{id}` (onglet passation / actions sur le maillon actif)  
**Permission :** `FILES:TRANSMIT` (et être possessionnaire ou substitut du maillon courant)

### Étapes
1. Ouvrir le dossier depuis **Dossiers** (`/files`) ou via sa référence.
2. Vérifier que vous êtes le **possessionnaire** du maillon actif (sinon l'action n'apparaît pas).
3. Utiliser l'action **Transmettre** sur l'étape en cours.
4. Confirmer le destinataire / maillon suivant selon le template.
5. Le dossier quitte votre charge ; le suivant reçoit une notification.

### Contenu lié
- Retour en arrière : action **Retour** sur le maillon (motif requis selon règles).
- Pièces jointes : ajouter avant transmission si le processus l'exige.

---

## creer-dossier

**Intent :** comment créer / ouvrir / enregistrer un nouveau dossier  
**Écran :** `/files/new` ou Portail (`/portal`) selon le cas  
**Permission :** `FILES:CREATE`

### Étapes (application interne)
1. Aller dans **Dossiers** → **Nouveau** (`/files/new`).
2. Choisir le **type de dossier** (et éventuellement un dossier préconfiguré).
3. Renseigner objet, bénéficiaire/émetteur, organisation, priorité.
4. Enregistrer : le circuit de passation est généré selon le template lié.
5. Ajouter les pièces jointes nécessaires.

### Portail
- Agents externes/internes : `/portal` → formulaire du type préconfiguré autorisé.

---

## cloturer-dossier

**Intent :** comment clôturer / fermer / archiver un dossier  
**Écran :** `/files/{id}`  
**Permission :** `FILES:CLOSE` (et règles métier du maillon de clôture)

### Étapes
1. Ouvrir le dossier.
2. Vérifier que le circuit est au **maillon de clôture** (étape finale du template).
3. Utiliser l'action **Clôturer**.
4. L'archivage éventuel se fait ensuite selon vos procédures (`FILES:ARCHIVE`).

---

## lire-dashboard

**Intent :** où voir indicateurs / KPI / tableau de bord / charge / retards  
**Écrans :**
- Synthèse : `/dashboard`
- Charge agents : `/dashboard/workload`
- Top retards : `/dashboard/overdue`
- Rapports / conformité / tendances : `/rapports`  
**Permission :** `DASHBOARD:READ` (export : `DASHBOARD:EXPORT`)

### Contenu
- Actifs, retards, créés/clôturés du mois.
- Classements de conformité et délais moyens par type dans **Rapports**.

---

## consultations-notifications

**Intent :** notifications / alertes non lues  
**Écran :** `/notifications`  
Les alertes d'un dossier précis : fiche dossier `/files/{id}`.

---

## organisation

**Intent :** organigramme / sous-structures / direction / service / mon organisation / chef  
**Écran :** `/admin/org` et `/admin/org/{id}`  
**Permission :** lecture selon périmètre (souvent `ORGANIZATIONS:READ` pour admin).

### Contenu
- Arborescence : outil `get_organization_tree`
- Détail / code (DAG, DIER…) : `get_organization`
- Sous-structures : `get_organization_children`
- Mon rattachement : `get_my_organization`
- Agents d'une structure (+ sous-structures) : `list_organization_users` avec `includeDescendants=true` (`USERS:READ`)
- Chef d'organisation : `get_organization_heads` (`USERS:READ`)

---

## utilisateurs

**Intent :** chercher un collègue / agent / responsable / fiche  
**Écran :** `/admin/users`  
**Permission :** `USERS:READ`

### Contenu
- Recherche nom/email/matricule : `search_users`
- Fiche détaillée : `get_user`
- Filtres org via `organizationCode` ou `organizationId`

---

## types-dossiers

**Intent :** quels types de dossiers existent  
**Écran public list :** données actives via API ; admin : `/admin/file-types`  
**Permission admin :** `FILE_TYPES:READ`

L'outil `list_file_types` renvoie les types **actifs** pour tout utilisateur authentifié.

---

## templates-circuit

**Intent :** étapes d'un template / chaîne de passation / T01  
**Écran :** `/admin/chain-templates` et `/admin/chain-templates/{id}`  
**Permission :** `CHAIN_TEMPLATES:READ`

Sans cette permission, l'assistant indique clairement le refus et oriente vers un admin métier.

---

## calendrier-ouvrable

**Intent :** jours fériés / calendrier ouvrable / jours non travaillés  
**Écran :** `/admin/settings` (section jours fériés)  
**Permission :** `BUSINESS_CALENDAR:READ`

L'outil `list_business_calendar` filtre par année (ex. 2026) et pays (défaut CM).

---

## dossiers-preconfigures

**Intent :** dossiers préconfigurés / formulaires portail  
**Écran :** `/admin/preconfigured-dossiers`  
**Permission :** `FILE_TYPES:READ`

---

## alertes-referentiel

**Intent :** types d'alertes / règles d'escalade d'un template  
**Écrans :** `/admin/alert-types`, règles sur `/admin/chain-templates/{id}`  
**Permissions :** `ALERT_TYPES:READ`, `ALERT_RULES:READ`

---

## permissions-acces

**Intent :** pourquoi je n'ai pas accès / permission refusée / rôle insuffisant  
**Écran profil :** `/profile` — admin rôles : `/admin/roles`

### Réponse type
1. Vérifier le rôle et les permissions listées sur le profil.
2. Identifier la permission manquante (ex. `CHAIN_TEMPLATES:READ`).
3. Demander à un **administrateur métier** / SUPER_ADMIN d'ajuster le rôle.
4. Se déconnecter / reconnecter après mise à jour RBAC.

---

## assistant-limites

**Intent :** que peut faire l'assistant  
L'assistant FluxPro est **lecture seule** : localisation de dossiers, lecture KPI, explication des écrans.  
Il ne transmet, ne crée, ne clôture ni ne modifie aucune donnée.
