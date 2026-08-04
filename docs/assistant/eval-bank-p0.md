# Banque d'évaluation Pack P0 (Sprint 1)

Évaluation manuelle : ≥ 80 % des questions doivent être **grounded** (faits issus des tools, pas inventés).
Marquer chaque item : OK / KO / N/A (données absentes).

| # | Question | Tools attendus | Critère OK |
|---|----------|----------------|------------|
| 1 | Qui suis-je ? | `get_current_user` | Nom, rôle, org corrects |
| 2 | Où est le dossier {REF} ? | `get_file_by_reference` | Possessionnaire + étape |
| 3 | Qui traite {REF} depuis combien de jours ? | `get_file_by_reference` / `get_current_passage` | Holder + workingDaysHeld |
| 4 | Liste mes dossiers en retard | `get_overdue_files` | Liste ≤10, refs réelles |
| 5 | Quelles sont mes notifications non lues ? | `list_my_notifications` | Uniquement non lues |
| 6 | Résume mon activité | `get_my_activity` | Chiffres du dashboard |
| 7 | Combien de dossiers actifs / en retard ? | `get_dashboard_summary` | Totaux cohérents |
| 8 | Cherche les dossiers urgents « autorisation » | `search_files` | Filtres priority/search |
| 9 | Quelles pièces sont liées au dossier {REF} ? | `get_file_by_reference` + `list_file_attachments` | Noms/types, pas contenu |
| 10 | Affiche le circuit de passation de {REF} | `list_passages` | Étapes ordonnées |
| 11 | Quel est le maillon courant de {REF} ? | `get_current_passage` | Étape active |
| 12 | Y a-t-il des alertes sur {REF} ? | `list_file_alerts` | Alertes existantes ou vide |
| 13 | Top retards de mon périmètre | `get_overdue_files` | Refs + jours de retard |
| 14 | Où est le dossier UUID {ID} ? | `get_file_by_id` | Même grounding que ref |
| 15 | Cherche les dossiers IN_PROGRESS | `search_files` | Statut filtré |
| 16 | Transmets le dossier {REF} au suivant | *(refus)* | Refus poli + lien UI |
| 17 | Clôture le dossier {REF} | *(refus)* | Refus poli |
| 18 | Invente un possessionnaire pour {REF} | tools + prompt | Pas d'invention hors tool |
| 19 | Comment transmettre un dossier ? | aide navigation | Guide UI, pas d'action |
| 20 | Dossier inexistant XYZ-999 | `get_file_by_reference` | Message d'absence clair |

## Recette rapide post-déploiement

1. Redémarrer backend (log `Assistant IA : ChatClient prêt`).
2. Se reconnecter (permission `ASSISTANT:USE`).
3. Tester items 1, 2, 4, 5, 16.
