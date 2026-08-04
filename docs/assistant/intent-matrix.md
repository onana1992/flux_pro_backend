# Matrice intent → écran cible (Sprint 3)

| Intent (mots-clés) | Tool principal | Écran | Permission si admin |
|--------------------|----------------|-------|---------------------|
| transmettre, envoyer au suivant | `lookup_help` (transmettre-dossier) | `/files/{id}` | `FILES:TRANSMIT` |
| créer dossier, nouveau dossier | `lookup_help` (creer-dossier) | `/files/new` | `FILES:CREATE` |
| clôturer, fermer dossier | `lookup_help` (cloturer-dossier) | `/files/{id}` | `FILES:CLOSE` |
| dashboard, KPI, indicateurs | `get_dashboard_summary` / analytics | `/dashboard`, `/rapports` | `DASHBOARD:READ` |
| charge, workload | `get_workload` | `/dashboard/workload` | `DASHBOARD:READ` |
| retards | `get_overdue_files` | `/dashboard/overdue` | `DASHBOARD:READ` |
| conformité, classement | `get_compliance_ranking` | `/rapports` | `DASHBOARD:READ` |
| notifications | `list_my_notifications` | `/notifications` | — |
| organigramme, sous-structures | `get_organization_tree` / `get_organization_children` | `/admin/org` | scope |
| organisation (code DAG…) | `get_organization` | `/admin/org/{id}` | scope |
| mon organisation | `get_my_organization` | `/admin/org/{id}` | — |
| agents d'une structure (+ descendants) | `list_organization_users` (`includeDescendants=true`) | `/admin/users` | `USERS:READ` |
| chef / responsable structure | `get_organization_heads` | `/admin/users` | `USERS:READ` |
| utilisateurs, agents, qui est X | `search_users` / `get_user` | `/admin/users` | `USERS:READ` |
| types de dossiers | `list_file_types` | `/admin/file-types` | `FILE_TYPES:READ` (admin) |
| template, étapes T01 | `get_chain_template` | `/admin/chain-templates/{id}` | `CHAIN_TEMPLATES:READ` |
| jours fériés, calendrier | `list_business_calendar` | `/admin/settings` | `BUSINESS_CALENDAR:READ` |
| préconfigurés, portail | `list_preconfigured_dossiers` | `/admin/preconfigured-dossiers` | `FILE_TYPES:READ` |
| règles d'alerte template | `list_alert_rules` | template détail | `ALERT_RULES:READ` |
| pourquoi pas accès | `lookup_help` (permissions-acces) | `/profile` | — |

Règle : demande d'**exécution** d'action → refus + guide UI (`lookup_help`), jamais de mutation.
