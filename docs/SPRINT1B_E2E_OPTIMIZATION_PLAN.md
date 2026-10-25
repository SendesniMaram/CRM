# Sprint 1B — Plan E2E progressif et budget mémoire

> Archive documentaire : les constats, chiffres, anciens commits et autorisations ci-dessous décrivent la campagne datée du rapport. Ils ne constituent pas un état courant ni un plan à réappliquer sur `publication-clean`. Toute opération réelle nécessite une nouvelle vérification et l’autorisation adaptée.

Date : 19 octobre 2026, Africa/Tunis. Phase d'analyse uniquement.

**VERDICT : REQUIRES DEDICATED ENVIRONMENT.** Une campagne séquentielle réelle est techniquement possible, mais son admission sur le moteur partagé n'est pas justifiée par la marge observée. Le protocole ci-dessous permet de la préparer et de réévaluer cette conclusion après mesures. Aucun démarrage, build, E2E, SQL ou changement de limites n'est exécuté.

## 1. Diagnostic actualisé

Références lues : rapports lot F et E2E, générateur `scripts/e2e/prepare-sprint1b.mjs`, Compose principal. Recherche des clients HTTP/Feign et lecture des chemins Employee/Department, HR/Identity/Employee et des routes Gateway.

| Mesure réelle, instantané du contrôle | Valeur |
|---|---:|
| Windows, mémoire physique totale | 16 067 356 Kio, environ 15,32 Gio |
| Windows, mémoire physique libre | 2 711 448 Kio, environ 2,59 Gio |
| Docker, capacité annoncée | 7 965 650 944 octets, environ 7,42 Gio |
| Docker, CPU logiques | 12 |
| Onze conteneurs actifs, somme `docker stats` | 4 771 Mio, environ 4,66 Gio |
| Écart capacité Docker moins conteneurs | Environ 2,76 Gio ; ce n'est pas une mesure de mémoire disponible |

| Conteneur actif préexistant | Mémoire observée, Mio |
|---|---:|
| crm-fees-service-1 | 455,4 |
| crm-customer-service-1 | 422,2 |
| crm-department-service-1 | 450,0 |
| crm-employee-service-1 | 507,4 |
| crm-identity-service-1 | 432,6 |
| crm-mysql-1 | 420,8 |
| crm-invoice-service-1 | 477,4 |
| crm-hr-service-1 | 454,6 |
| crm-payroll-service-1 | 425,5 |
| crm-gateway-service-1 | 374,6 |
| crm-discovery-service-1 | 350,5 |

Les CPU instantanés sont faibles, entre 0,08 % et 0,57 % par conteneur ; cela ne décrit pas les pointes de démarrage. Les onze conteneurs, cinq réseaux et quatre volumes inventoriés sont préexistants. Ports principaux : 8080, 8082–8089, 8761 et MySQL 3307. Aucun accès authentifié aux services actifs n'est réalisé.

La mémoire Windows et celle du moteur virtualisé sont liées : ne pas additionner leurs marges comme deux réserves indépendantes. Il manque une mesure fiable de `MemAvailable`, du swap et de la pression mémoire interne au moteur. Les moyennes des conteneurs actifs ne mesurent ni les pics de démarrage ni les besoins des nouvelles images. Le tmpfs MySQL consomme aussi de la mémoire et compte dans le budget du conteneur ; sa taille maximale n'est pas une réservation.

Commandes de mesure exécutées en lecture seule : `Get-CimInstance Win32_OperatingSystem`, `docker info --format`, `docker stats --no-stream --format`, `docker ps --format`, `docker network ls`, `docker volume ls`. `git status --short`, lectures des références et recherches de sources complètent l'analyse. Aucun test du lot F n'est réexécuté.

## 2. Comparaison des solutions

| Critère | A — Machine ou moteur dédié | B — Sous-ensembles séquentiels | C — Pile complète ultérieure |
|---|---|---|---|
| Données | Base, réseau et identifiants synthétiques indépendants | Même isolation indispensable pour chaque palier | Même isolation indispensable |
| Mémoire | Capacité provisionnée hors environnement actif | Environ 1–3 Gio de plafonds conteneurs selon palier, plus réserves et navigateur | Enveloppe initiale d'environ 6 Gio conteneurs, plus réserves et navigateur |
| Couverture | Tous les parcours et simultanéité possibles | Parcours réels sélectionnés ; pas de validation des sept services simultanés | Couverture simultanée complète possible |
| Complexité | Provisionnement, transfert des artefacts et contrôles d'accès | Orchestration, transitions, surveillance et conservation des fixtures | Orchestration simple après disponibilité des ressources |
| Risque pour l'actif | Faible sur une autre machine ; un second moteur sur le même hôte partage encore CPU/RAM/disque | Résiduel sur le moteur partagé ; plafonds sans garantie contre la contention globale | Faible si capacité dédiée ; sinon même problème qu'aujourd'hui |
| Temps estimé, non mesuré | Provisionnement externe variable ; préparation/campagne 0,5–1 journée hors attente | Préparation du runner et instrumentation 0,5–1 journée ; exécution 2–4 h si tout réussit | Attente de capacité indéterminée ; campagne 2–4 h après préparation |

Les durées sont des estimations de travail, hors téléchargements, incidents et corrections. A est recommandée aujourd'hui. B reste un protocole candidat, surtout utile sur une machine dédiée de capacité limitée. C reste nécessaire pour une mesure représentative de la pile entière. Un contexte Docker distinct sur la même machine ne fournit pas, à lui seul, une isolation de capacité.

## 3. Sous-ensembles minimaux et scénarios

Conserver MySQL temporaire pendant les transitions ; ne jamais arrêter ce conteneur pour simuler une panne Identity, sinon le tmpfs perd les fixtures. Arrêter les seuls services de test devenus inutiles avant le palier suivant. Une seule campagne et un seul groupe métier à la fois.

| Palier | Services Docker réellement nécessaires | Couverture possible après autorisation |
|---|---|---|
| P0 — Calibration Identity | MySQL, Identity | Login direct, uid, validation interne, rotation, signature/expiration/legacy, statut et rôles courants |
| P1 — Consommateur direct | MySQL, Identity, un service métier | Même JWT avant/après disabled/locked/retrait de rôle ; 401/403/503 ; panne/reprise ; écriture refusée inchangée ; répéter pour les sept services |
| P2 — Gateway | MySQL, Identity, Discovery, Gateway | Login/Refresh via Gateway, discovery réel, namespace interne bloqué y compris chemins encodés |
| P3 — Parcours complet simple | MySQL, Identity, Discovery, Gateway, un service métier | Navigateur ou client réel → Gateway → métier → Identity → MySQL ; comparer accès direct ; PDF Invoice/Payroll ; répéter les consommateurs |
| P4 — Employee/Department | MySQL, Identity, Discovery, Employee, Department | Feign réel, Bearer, deux validations de comptes, dépendance refusée sans fallback normal ; Gateway ajouté seulement si budget recalculé |
| P5 — HR/annuaires | MySQL, Identity, Discovery, HR, Employee | Associations via annuaires réels Identity/Employee, refus sans sauvegarde, congés et propriété A/B, PDF ; Department inutile pour la lecture simple Employee ; Gateway ajouté seulement si budget recalculé |

P0/P1 : désactiver uniquement le client Eureka dans la configuration temporaire (`EUREKA_CLIENT_ENABLED=false`) pour éviter une instance Discovery inutile. La validation de compte utilise toujours l'Identity réel et sa DB. Aucun appel nécessitant discovery n'est revendiqué dans ces paliers. C'est une variante de transport de test, à distinguer du parcours complet ; ne pas changer le code ou la politique JWT.

P2–P5 : conserver Eureka réel et les routes `lb://` existantes. Ne pas remplacer les annuaires HR ni Department par un serveur simulé. Les autres services ne présentent pas de client métier sortant effectif identifié par la recherche ; l'annotation EnableFeignClients seule ne prouve pas un appel.

Pour Invoice/Payroll, les références métier peuvent être préparées dans un palier antérieur et conservées dans les bases synthétiques. Cela vérifie les PDF et droits, pas un parcours de création distribué absent du code. Tenir un registre des IDs réels, sans supposer User.id=Employee.id=Customer.id.

Angular : utiliser la copie E2E, une origine API 18080 et un profil navigateur propre. Préconstruire son bundle avant les conteneurs puis servir statiquement le bundle avec support des routes SPA, pour éviter le coût de `ng serve`. Pas de suppression des guards ou de l'intercepteur ; dépendances HTTP absentes d'une page = parcours incomplet à signaler.

## 4. Budget proposé, à calibrer

Ces plafonds sont des hypothèses conservatrices arrondies au-dessus des consommations courantes. Ils ne sont pas des pics mesurés des images candidates et ne constituent pas un dimensionnement validé.

| Composant E2E | Plafond initial proposé | Hypothèse JVM / stockage |
|---|---:|---|
| MySQL | 640 Mio | tmpfs borné à 256 Mio pour fixtures minimales, inclus dans les 640 Mio ; init MySQL à vérifier |
| Identity | 512 Mio | `-Xms64m -Xmx256m` |
| Discovery | 384 Mio | `-Xms64m -Xmx192m` |
| Gateway | 448 Mio | `-Xms64m -Xmx192m`, reste pour mémoire native/directe |
| Employee | 576 Mio | `-Xms64m -Xmx256m` |
| Department, Customer, Payroll, Fees | 512 Mio chacun | `-Xms64m -Xmx256m` |
| HR, Invoice avec PDF | 576 Mio chacun | `-Xms64m -Xmx256m` |

Ne pas fixer des plafonds arbitraires plus petits pour obtenir artificiellement une admission. Heap, metaspace, buffers directs, piles de threads, cache de fichiers et tmpfs ne sont pas interchangeables. Un OOM E2E invalide le palier ; ne pas augmenter automatiquement sa limite sur le moteur partagé.

| Palier | Somme des plafonds conteneurs | Minimum de marge préalable proposée : somme + 1 Gio de réserve |
|---|---:|---:|
| P0 | 1 152 Mio | 2 176 Mio, soit 2,13 Gio |
| P1 | 1 664–1 728 Mio | 2 688–2 752 Mio, soit 2,63–2,69 Gio |
| P2 | 1 984 Mio | 3 008 Mio, soit 2,94 Gio |
| P3 | 2 496–2 560 Mio | 3 520–3 584 Mio, soit 3,44–3,50 Gio |
| P4 | 2 624 Mio | 3 648 Mio, soit 3,56 Gio |
| P5 | 2 688 Mio | 3 712 Mio, soit 3,63 Gio |

Ajouter **1 Gio d'enveloppe hôte provisoire** pour navigateur, runner et serveur statique lorsqu'ils sont nécessaires ; mesurer ensuite leurs pics. Ne pas exécuter npm, Maven ou builds Docker en parallèle des paliers. Pour la pile entière, somme proposée des conteneurs : **5 760 Mio**, avant réserve, navigateur et pics de construction.

Admission : mesurer la marge réellement disponible du moteur et de Windows pendant au moins 60 s ; retenir leurs minima, pas une seule observation. Exiger les deux marges adaptées au palier, sans les additionner. Si la mémoire moteur ne peut pas être déterminée sans interaction avec l'actif, ne pas admettre le palier sur le moteur partagé.

Aujourd'hui P1 dépasse déjà la mémoire Windows libre observée avant navigateur ; P2–P5 dépassent aussi l'écart Docker théorique. P0 pourrait être envisagé après meilleure mesure, mais ne couvre aucun accès métier et la mémoire disponible du moteur n'est pas établie. Il ne justifie pas le verdict READY FOR LIMITED E2E pour l'objectif demandé.

## 5. Isolation et adaptations à préparer

Ne jamais fusionner le Compose principal avec le modèle E2E. Générer un nouveau run avec projet, réseau et tags propres ; interdire les réseaux externes et les noms explicites de ressources existantes. Vérifier que les ports 14200, 18080 et 18082–18089 sont libres, puis les publier uniquement sur loopback. Pas de port MySQL hôte.

Préparer un `compose-limited.json` autonome dérivé du nouveau modèle, sans modifier le générateur ou le Compose principal dans cette phase. Y ajouter les seuls plafonds `mem_limit`, `memswap_limit` égal au plafond pour éviter le swap conteneur, `restart: "no"`, les options JVM de test et le tmpfs borné. L'absence de swap doit être vérifiée à l'exécution ; les plafonds ne réservent pas la RAM. Aucun paramètre de concurrence JWT, timeout, cache, rôle ou fallback ne change.

Le modèle actuel dépend de Discovery même pour Identity ; ses `depends_on` doivent être adaptés au groupe avant démarrage, ou toutes les dépendances doivent déjà être explicitement démarrées et prêtes avec `--no-deps`. Ne jamais lancer `up` sans sélection des services. Ne pas introduire une URL localhost de secours : conserver `IDENTITY_VALIDATION_BASE_URL=http://identity-service:8082`.

Ne générer les secrets E2E qu'après autorisation, en mémoire du processus, sans `.env` existant, sans impression de Compose résolu ou de l'environnement complet. Fixtures `example.test`, mots de passe et clé HMAC de test propres au run. SQL uniquement après vérification des labels du conteneur MySQL et de l'ID enregistré, sur les schémas du run, avec mutations ciblées et commit confirmé.

## 6. Commandes proposées — NON exécutées

Ces extraits décrivent la future campagne. Le fichier `compose-limited.json`, le runner de scénarios et le moniteur de seuils restent à préparer et vérifier ; ce n'est pas un script autonome déjà exécutable.

```powershell
# Après validation, nouveau run ; ne pas réutiliser l'ancien.
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
node scripts/e2e/prepare-sprint1b.mjs $runId
$project = "crm-sprint1b-e2e-$runId"
$dir = Join-Path (Get-Location) "target/$project"
# Préparer/revoir compose-limited.json et générer E2E_* en mémoire.
$argsE2E = @('compose','--project-name',$project,'--project-directory',$dir,
             '-f',(Join-Path $dir 'compose-limited.json'))
& docker @argsE2E config --quiet
if ($LASTEXITCODE -ne 0) { throw 'Configuration E2E invalide' }
# Construire les JAR courants, common-security installé avant consommateurs.
# Construire séquentiellement les seules images candidates requises.
& docker @argsE2E build identity-service
if ($LASTEXITCODE -ne 0) { throw 'Build Identity échoué' }
# Préflight mémoire et labels obligatoire avant chacune des étapes suivantes.
& docker @argsE2E up -d --no-deps mysql
# Attendre health MySQL, puis P0 avec Eureka désactivé dans ce modèle.
& docker @argsE2E up -d --no-deps identity-service
# Attendre readiness réelle ; puis P1, un consommateur à la fois.
& docker @argsE2E up -d --no-deps customer-service
# Runner : scénarios réels, assertions et résultats expurgés.
& docker @argsE2E stop customer-service
```

Ne pas employer le fichier P0/P1 avec Eureka désactivé pour les paliers discovery. Préparer un modèle par groupe ou recréer uniquement les conteneurs E2E concernés avec la bonne configuration, après arrêt contrôlé, MySQL conservé. Pour P4 : Discovery, Identity, Department et Employee réels. Pour P5 : Discovery, Identity, Employee et HR réels. Pour P3 : Discovery, Identity, Gateway puis un consommateur réel. Vérifier les codes de sortie de chaque commande dans le runner définitif.

Avant chaque panne ou arrêt, récupérer l'ID par le Compose du run et contrôler ses labels :

```powershell
$id = (& docker @argsE2E ps -q identity-service).Trim()
if (-not $id) { throw 'Identity E2E absent' }
$label = (& docker inspect --format '{{index .Config.Labels "com.docker.compose.project"}}' $id).Trim()
if ($label -ne $project -or $project -notmatch '^crm-sprint1b-e2e-\d{8}-\d{6}$') {
    throw 'Ressource hors projet E2E'
}
# Vérifier aussi le label service et l'ID dans le registre créé par la campagne.
# Après ces contrôles seulement : docker stop $id ; puis docker start $id.
```

Nettoyage final après vérification de TOUS les IDs et labels du projet :

```powershell
& docker @argsE2E down
# Pas de -v, prune, suppression globale ou --rmi all.
# Retirer seulement les tags d'images candidats enregistrés et absents du baseline,
# si leur suppression est souhaitée ; pas de suppression forcée ni d'image de base.
```

Ne pas utiliser `down --remove-orphans` pour contourner une incohérence d'inventaire. Aucun volume nommé de données n'est nécessaire ; arrêter MySQL détruit seulement ses fixtures en tmpfs. Conserver logs expurgés et résultats ; arrêter le seul navigateur/serveur de test en vérifiant PID et heure de création.

## 7. Readiness, scénarios et instrumentation

MySQL : health puis ouverture authentifiée des seuls schémas de test. Identity : health, Login synthétique, uid exact puis GET interne direct 204. Discovery : enregistrement attendu et absence d'instance du CRM principal. Métier : health, requête autorisée réelle via son port puis via Gateway si présent. Gateway : Login/Refresh et route métier résolue ; service_started ne suffit pas. Délai initial proposé : 120 s par service, un démarrage à la fois ; échec = arrêt du palier, sans boucle de redémarrage.

Pour chaque consommateur : compte actif, JWT faux/expiré/sans uid, disabled/locked et rôle retiré après commit, droit insuffisant, panne puis reprise. HTTP attendu : 401 pour session invalide, 403 pour droit insuffisant après authentification, 503 pour vérification impossible. Comparer comptes, IDs et horodatages métier avant/après les POST/PUT refusés ; confirmer l'absence d'invocation avec traces de test limitées, sans Bearer ni corps personnel. Ne pas déduire cette absence du seul statut HTTP.

Interservices : Employee avec departmentId non nul ; HR association appelant vraiment Identity et Employee ; ne pas provoquer uniquement un refus du filtre d'entrée et prétendre avoir testé un refus de la seconde dépendance. Préparer un mécanisme déterministe, réservé à l'environnement E2E, pour la panne après admission du premier service ; documenter son point d'injection sans modifier les règles JWT. Compter les validations par journaux d'accès Identity expurgés : parcours Employee→Department attendu deux validations internes ; HR→Employee ajoute sa propre validation, en plus des lectures annuaire Identity.

Browser P3 : tester 401→refresh borné, 403 sans élévation, 503 sans refresh/logout injustifié, refresh indisponible après 401 et PDF refusés sans téléchargement. P5 ajoute propriété et PDF congés croisés. Les tests Angular simulés existants ne remplacent aucun de ces parcours réels.

Performance : après calibration, 10 échauffements exclus, 100 requêtes séquentielles par parcours, p50/p95/p99 et population indiquée ; pas de SLA déduit. Saturation : quatre validations retenues dans le seul Identity E2E, cinquième refusée, absence d'écriture puis reprise. Un échec dû à une limite mémoire du test ne prouve pas une panne JWT. Séparer les séries API, navigateur et panne ; ne pas confondre latence de démarrage/Eureka avec validation.

## 8. Critères d'arrêt et maîtrise OOM

Moniteur indépendant du runner, échantillon toutes les 2 s ; toutes les ressources modifiables doivent figurer dans le registre du run. Seuils proposés de procédure, à confirmer avant exécution :

- Pas d'admission si les minima mémoire pendant 60 s ne couvrent pas plafonds + réserves du palier.
- Windows disponible < 1 Gio ou moteur disponible < 768 Mio : arrêter immédiatement la charge et les seuls conteneurs E2E ; aucune action sur l'actif.
- Un conteneur E2E > 85 % de son plafond sur trois mesures : suspendre les nouvelles requêtes et terminer le palier ; > 95 % sur une mesure : arrêt immédiat des seules ressources E2E.
- OOMKilled, OOME Java, compteur cgroup `oom`/`oom_kill` en hausse, redémarrage inattendu ou health dégradée : échec explicite, arrêt, pas d'augmentation automatique de RAM ou de délai.
- Pression mémoire persistante, swap en croissance soutenue ou impossibilité de collecter les métriques : abandonner le palier. L'accès aux métriques cgroup/moteur doit être préparé uniquement pour les ressources de test et l'hôte, sans exec dans les conteneurs CRM actifs.
- Changement d'ID, redémarrage ou disparition d'un conteneur préexistant : arrêter la campagne ; signaler l'événement sans attribuer sa cause sans preuve.

La surveillance et les plafonds réduisent le risque, sans garantir l'absence d'impact avant réaction. Le démarrage séquentiel évite les pics cumulés ; le nettoyage des conteneurs n'annule pas une contention déjà subie. Aucun seuil ne dispense de l'admission préalable.

## 9. Couverture non acquise et conditions de reprise

Aucun scénario E2E n'est exécuté dans cette phase. Le protocole B ne couvre pas les sept services simultanément, une charge de production, une haute disponibilité multi-instance, TLS/mTLS de production, les courses distribuées exhaustives ni un déploiement principal. Une batterie avec serveurs simulés reste un test isolé, distinct de ce plan réel.

Pour reprendre : validation utilisateur de l'exécution, capacité dédiée ou admission mémoire démontrée, modèles limités et runner/moniteur préparés et revus, artefacts reconstruits depuis les sources actuelles, inventaire et ports revérifiés, secrets synthétiques et fixtures explicitement bornées. Aucun besoin de nouvelle autorisation pour les simples lectures et la rédaction de ce plan ; l'autorisation de démarrer n'est pas donnée par cette mission.

Le seul livrable de cette phase est ce document. Aucun fichier préexistant n'est modifié ; aucune limite active, base, secret, .env ou volume n'est touché. Aucun build, test E2E, nouveau conteneur, arrêt, redémarrage, déploiement, commit ou push.

**VERDICT FINAL : REQUIRES DEDICATED ENVIRONMENT. La stratégie séquentielle est définie, mais les groupes nécessaires aux principaux parcours ne disposent pas aujourd'hui d'une marge mémoire démontrée sur le moteur partagé.**
