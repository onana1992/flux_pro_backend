# Matrice d'accès — interface utilisateur

Vue menus + actions (front : `AppShell`, `RequireAuth`, `auth-storage.ts`).  
Source backend des permissions : `RbacDataInitializer.rolePermissionMatrix()`.

**Légende rôles** : **SA** Super Admin · **BA** Business Admin · **DIR** Director · **SH** Service Head · **RD** Regional Director · **SG/EO** Secretary General / Executive Office · **AG/SUP** Agent / Support · **R** Reader  

**Accès** : **L** lecture · **E** écriture (créer / modifier) · **—** non accessible

---

## Tableau récapitulatif — navigation × rôles × actions

| Navigation (route) | SA | BA | DIR | SH | RD | SG/EO | AG/SUP | R | Actions disponibles selon rôle |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|---|
| **Tableau de bord** `/dashboard` | L | L | L | L | L | L | L | L | Consulter KPIs. **Exporter / lien Rapports** : SA, BA, DIR, SH, RD, SG/EO. Liens admin (org, users, audit) : selon admin |
| **Dossiers** `/files` | L+E | L+E | L+E | L+E | L+E | L | L+E | L | **Nouveau** : SA, BA, DIR, SH, RD, AG/SUP. **Éditer brouillon** (liste) : mêmes rôles. SG/EO/R : liste + ouvrir détail seulement |
| **Nouveau dossier** `/files/new` | E | E | E | E | E | — | E | — | Créer un dossier (brouillon) |
| **Détail dossier** `/files/[id]` | L+E | L+E | L+E | L+E* | L+E* | L | L+E* | L | Voir section [Détail dossier](#zoom--détail-dossier-filesid) |
| **Éditer dossier** `/files/[id]/edit` | E | E | E | E | E | — | E | — | Modifier métadonnées (`FILES:UPDATE`) |
| **Rapports** `/rapports` | L | L | L | L | L | L | — | — | Consulter / exporter rapports (`DASHBOARD:EXPORT`) |
| **Notifications** `/notifications` | L | L | L | L | L | L | L | L | Consulter ses notifications (authentifié) |
| **Types de dossiers** `/admin/file-types` | L+E | L+E | L | L | L | L | L | L | **Créer / activer-désactiver / supprimer** : SA, BA. Autres : lecture |
| **Modèles de chaînes** `/admin/chain-templates` | L+E | L+E* | L | L | L | L | L | L | **Créer / modifier / activer** : SA, BA. **Supprimer** : SA seulement. Autres : lecture |
| **Détail modèle** `/admin/chain-templates/[id]` | L+E | L+E* | L | L | L | L | L | L | Éditer / dupliquer : SA, BA. Règles d'alerte liées : lecture DIR…SG/EO ; écriture SA, BA |
| **Types d'alertes** `/admin/alert-types` | L+E | L+E | L | L | L | L | — | — | **CRUD** : SA, BA. Lecture : DIR, SH, RD, SG/EO. Invisible AG/SUP/R |
| **Organigramme** `/admin/org` | L+E | L+E | — | — | — | — | — | — | Créer / modifier / désactiver organisations |
| **Types d'org** `/admin/org/types` | L+E | L+E | — | — | — | — | — | — | CRUD types d'organisation |
| **Utilisateurs** `/admin/users` | L+E | L+E | L | L | L | L | — | L | **Créer / modifier / désactiver** : SA, BA. Lecture liste/détail : DIR, SH, RD (+ SG/EO/R via JWT). Reset MDP / unlock / import : **SA** |
| **Rôles** `/admin/roles` | L+E | L | — | — | — | — | — | — | Lecture BA+SA. **Créer / assigner permissions** : SA (`ROLES:CREATE` / `UPDATE`) |
| **Permissions** `/admin/permissions` | L+E | L | — | — | — | — | — | — | Lecture BA+SA. Écriture catalogue : SA |
| **Journal connexions** `/admin/audit` | L | — | — | — | — | — | — | — | Consulter audit login (SA uniquement) |

\* SH / RD / AG/SUP : écriture dossier sans toutes les actions de clôture / archivage (voir ci-dessous).

---

## Zoom — détail dossier `/files/[id]`

| Action UI | SA | BA | DIR | SH | RD | SG/EO | AG/SUP | R |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| Consulter fiche + pièces + circuit | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Soumettre brouillon | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| Modifier / annuler (selon statut) | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓ | — |
| Lier circuit / affecter responsables | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓¹ | — |
| Marquer effectué / transmettre maillon | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓² | — |
| Retour / suspendre / reprendre | ✓ | ✓ | ✓ | ✓ | ✓ | — | ✓² | — |
| Clôturer | ✓ | ✓ | ✓ | — | ✓ | — | — | — |
| Archiver | ✓ | ✓ | ✓ | — | — | — | — | — |
| Supprimer brouillon | ✓ | — | — | — | — | — | — | — |

¹ Si `FILES:UPDATE`.  
² Si `FILES:TRANSMIT` **et** responsable du maillon (ou supérieur hiérarchique / manager).

---

## Synthèse par rôle

| Rôle | Vues nav principales | Pouvoir typique |
|---|---|---|
| **SA** | Tout | Configuration + dossiers cycle complet |
| **BA** | Tout sauf audit | Admin métier + dossiers jusqu'à archive |
| **DIR** | Métier + users + alertes + rapports | Pilote dossiers jusqu'à archive |
| **RD** | Idem DIR | Comme DIR sans archiver |
| **SH** | Idem DIR | Traite / transmet, pas clôturer |
| **SG / EO** | Dashboard, dossiers, rapports, référentiels, alertes, users (lecture) | Supervision / lecture |
| **AG / SUP** | Dashboard, dossiers, types, chaînes | Opérationnel dossiers |
| **R** | Dashboard, dossiers, types, chaînes (+ users lecture) | Consultation |

---

## Notes techniques

- La **nav** utilise `canSeePermission` (JWT ou fallback rôle).
- Les **gardes de page** et boutons d'action utilisent surtout `hasPermission` (JWT).
- Les données restent filtrées par **périmètre organisationnel** côté API.
- Matrice technique rôle × permission : `RbacDataInitializer` ; spec historique Sprint 1 : `docs/SPEC-USR-RBAC.md` §4.3 (partiellement obsolète).
