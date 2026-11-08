# Lot 2B — Validation intégrée contrôlée du CRM

Date : 2 novembre 2026. Branche : `publication-clean`.

## Verdict : BLOQUÉ en phase 1

La procédure s'arrête avant préparation des binaires et images. Deux précontrôles empêchent de continuer : mémoire disponible insuffisante et moteur Docker inaccessible. Aucun démarrage de Docker Desktop, MySQL, microservice ou frontend, aucune migration et aucun test intégré n'ont été exécutés. Le CRM n'est ni validé ni déployé.

## Documents et périmètre

Consultation des documents obligatoires : [préparation Lot 2A](LOT_2A_PREPARATION_VALIDATION.md), [scénarios Lot 2B](LOT_2B_SCENARIOS_VALIDATION.md) et [protocole Lot 1C](LOT_1C_VALIDATION_INTEGREE.md).

Campagne prévue, non démarrée : `crm-lot2b-20261101-140100`, modèle `target/crm-lot2b-20261101-140100/compose.json`. La validation statique antérieure ne démontre pas la disponibilité des ressources réelles.

## Résultats effectivement obtenus

| Précontrôle | Statut | Observation et limite |
|---|---|---|
| Mémoire physique | **BLOQUÉ** | 15,32 GiB au total, **3,99 GiB libres** à la mesure. Le Lot 2A prévoit 12 GiB libres pour la campagne API, dont 8 GiB de limites cumulées de conteneurs. La mémoire libre actuelle est même inférieure à ces 8 GiB. Aucun processus utilisateur n'a été arrêté et aucun budget réduit pour contourner ce contrôle. |
| Disque C: | **VALIDÉ** | **289,47 GiB disponibles**, au-dessus de la marge de préparation de 20 GiB. L'espace interne de la VM/disque Docker n'a pas pu être vérifié. |
| Ports TCP hôte | **VALIDÉ** | Aucun listener TCP sur **18080, 18761 et 14200** à la mesure. Ce constat devra être renouvelé avant un démarrage ultérieur. |
| CLI Docker / Compose | **VALIDÉ** | Exécutable Docker trouvé ; `docker compose version --short` retourne **5.5.0**. Cela ne valide ni le moteur ni le modèle Compose. |
| Contexte Docker sélectionné | **VALIDÉ** | `docker context show` retourne **desktop-linux**. Les ressources et l'identité effective du moteur n'ont pas pu être inspectées. |
| Moteur et mémoire Docker | **BLOQUÉ** | `docker info --format '{{.MemTotal}}'` échoue à joindre le pipe `dockerDesktopLinuxEngine`, introuvable. Le moteur est inaccessible ; l'allocation mémoire Docker est **inconnue**. Le `0` produit lors de cet échec n'est pas une mesure de RAM attribuée. Docker Desktop n'a pas été lancé pour contourner le blocage. |
| Isolation réelle des volumes/réseaux | **NON TESTÉ** | Inspection du moteur impossible. Les garanties statiques du Lot 2A restent historiques ; aucun volume existant n'a été monté, parcouru, modifié ou supprimé. |
| Images disponibles et dépendances | **NON TESTÉ** | Arrêt avant inspection des images et contrôle du cache de dépendances ; aucune image historique utilisée. |
| Secrets de test | **NON TESTÉ** | Aucun chemin de fichier externe n'a été fourni dans cette demande. Aucune recherche large, lecture ou impression de secrets ; aucune valeur générée. Leur absence sur la machine n'est pas affirmée. |
| Certificats et clés HTTPS | **NON TESTÉ** | Aucun chemin de matériel externe fourni ; présence, dates, SAN, chaîne, alias, correspondance RSA et ACL non vérifiés. Aucun certificat ni clé privée lu ou affiché. |
| Configuration Compose résolue | **NON TESTÉ** | Pas de `config` résolu avec secrets et chemins approuvés ; arrêt des précontrôles après les blocages. Le checker statique du Lot 2A n'a pas été présenté comme une validation Compose réelle. |
| Packaging / JAR / migrations embarquées | **NON TESTÉ** | Phase 2 non engagée, conformément à la condition de réussite des précontrôles. |
| Construction des images | **NON TESTÉ** | Aucune image construite, aucun téléchargement lancé. |
| MySQL, Flyway, Eureka, services, TLS, JWT, parcours Angular et quatre rôles | **NON TESTÉ** | Phase 4 non engagée. Aucun résultat d'intégration déduit des tests ciblés des lots précédents. |

Les statuts VALIDÉ concernent uniquement les observations décrites, à l'instant de mesure. Aucun cas d'intégration n'a été exécuté et aucun résultat fonctionnel ne reçoit le statut ÉCHEC : les obstacles rencontrés sont des préconditions BLOQUÉES.

## Commandes et limites des mesures

Les commandes opérationnelles ont été uniquement en lecture :

```powershell
Get-CimInstance Win32_OperatingSystem
[System.IO.DriveInfo]::new('C:\')
Get-NetTCPConnection -State Listen
docker context show
docker info --format '{{.MemTotal}}'
docker compose version --short
```

Seuls les totaux de mémoire, espace libre, ports sélectionnés et métadonnées CLI nécessaires ont été affichés. Pas de `docker inspect` complet, configuration résolue, environnement, logs de conteneur ou valeur secrète dans les sorties.

Le premier accès CIM dans le sandbox a été refusé et la mesure disque n'y était pas exploitable. Les contrôles ont donc été repris avec l'accès en lecture nécessaire ; les chiffres du tableau proviennent de cette seconde exécution. Le refus initial de CIM n'est pas une preuve d'insuffisance mémoire. L'échec de connexion Docker, lui, a été observé pendant cette seconde exécution.

## Préservation et état des actions

Branche conservée : `publication-clean`. Seul ce nouveau rapport est ajouté au périmètre versionnable du Lot 2B ; les **158 fichiers modifiés/non suivis préexistants** ont été comparés par SHA-256 : aucun changement ni disparition. La photographie est conservée sous `target/lot2b-preflight/`, ignoré par Git. `git diff --check` réussi. Aucun fichier métier, configuration des lots précédents ou migration historique n'a été modifié. Aucun staging, commit, push, changement de branche, suppression de données ou volumes.

## Conditions de reprise

1. Rendre disponibles les ressources mémoire prévues, ou convenir explicitement d'un plan réduit distinct ; ne pas présenter ce dernier comme une validation de la pile complète.
2. Rendre le moteur Docker local accessible, puis vérifier son allocation mémoire, son disque, le contexte réel, l'absence de ressources préexistantes de campagne et les ports. Aucun lancement de Docker Desktop n'a été effectué dans cette tentative.
3. Fournir les chemins absolus externes approuvés du fichier de secrets de test et du répertoire contenant les cinq fichiers HTTPS, puis contrôler leurs contrats sans afficher de valeur sensible.
4. Rejouer l'intégralité de la phase 1, y compris modèle Compose résolu, isolation réelle, images et dépendances. Poursuivre seulement si tous les précontrôles sont satisfaisants.
5. Préparer alors les JAR et images séquentiellement, puis appliquer le **point d'arrêt obligatoire de phase 3** : présenter les preuves et demander explicitement l'autorisation avant le premier démarrage de MySQL ou d'un microservice.

L'autorisation de démarrage n'est pas demandée prématurément : les précontrôles ont échoué et les images ne sont pas préparées.

**BLOQUÉ — mémoire libre insuffisante (3,99 GiB) et moteur Docker inaccessible.**
