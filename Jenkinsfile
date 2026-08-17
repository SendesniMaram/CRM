pipeline {
    // -------------------------------------------------------------------------
    // agent : exécute le pipeline sur un agent Jenkins (n'importe lequel).
    // V1 : aucun label particulier requis tant que Maven n'est pas utilisé.
    // -------------------------------------------------------------------------
    agent any

    // -------------------------------------------------------------------------
    // options : contrôle global du comportement du pipeline.
    //   - timestamps()    ajoute un horodatage à chaque ligne de log.
    //   - disableConcurrentBuilds() empêche deux builds simultanés
    //     sur le même travail (évite les conflits sur le workspace).
    //   - timeout(...)    limite la durée totale du pipeline (60 min).
    // -------------------------------------------------------------------------
    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
    }

    // -------------------------------------------------------------------------
    // environment : variables d'environnement accessible dans tout le pipeline.
    //   - WORKSPACE  emplacement du workspace de l'agent (fourni par Jenkins).
    //   - PROJECT_NAME identifie le projet pour les futurs logs / artefacts.
    // -------------------------------------------------------------------------
environment {
        PROJECT_NAME = 'crm'
        WORKSPACE    = "${env.WORKSPACE}"

// -----------------------------------------------------------------
        // Docker Registry (Docker Hub) - utilisé par le stage 'Docker Push'.
        // Ces variables sont définies comme vides par défaut : aucune
        // supposition de compte / registre. Elles doivent être configurées
        // dans le job Jenkins (PARAMÈTRE de l'utilisateur Docker Hub).
        //
        //   - DOCKER_REGISTRY   : URL du registre. Pour Docker Hub : 'docker.io'
        //   - DOCKER_IMAGE_BASE : nom d'utilisateur Docker Hub (sans /crm).
        //      Les images finales seront :
        //          <username>/crm-discovery-service:build-${BUILD_NUMBER}
        //          <username>/crm-discovery-service:latest
        // -----------------------------------------------------------------
        DOCKER_REGISTRY   = ''
        DOCKER_IMAGE_BASE = ''
    }

    stages {
                // =====================================================================
        // STAGE 0 (TEMPORAIRE) : Audit de l'environnement Jenkins
        // ---------------------------------------------------------------------
        // Vérifie la présence et les versions de Java, Maven et Docker.
        // Ce stage est uniquement destiné à l'audit de l'agent Jenkins.
        // Il ne bloque pas le pipeline si un outil est absent.
        // =====================================================================
        stage('Environment Audit') {
            steps {
                script {
                    echo '================ ENVIRONMENT AUDIT ================'

                    // ---- 1. Java ----
                    echo '--- Java ---'
                    sh 'java -version 2>&1 || echo "JAVA ABSENT"'
                    sh 'javac -version 2>&1 || echo "JDK (javac) ABSENT"'
                    echo "JAVA_HOME = ${env.JAVA_HOME ?: 'non défini'}"

                    // ---- 2. Maven ----
                    echo '--- Maven ---'
                    sh 'command -v mvn || echo "MAVEN ABSENT (commande mvn introuvable)"'
                    sh 'mvn -v 2>&1 || echo "MAVEN NON EXECUTABLE"'

                    // ---- 3. Docker ----
                    echo '--- Docker ---'
                    sh 'command -v docker || echo "DOCKER ABSENT (commande docker introuvable)"'
                    sh 'docker --version 2>&1 || echo "DOCKER NON EXECUTABLE"'
                    sh 'docker compose version 2>&1 || echo "DOCKER COMPOSE v2 ABSENT"'

                    echo '--- FIN AUDIT ---'
                    echo '==================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 1 : Checkout du dépôt
        // ---------------------------------------------------------------------
        // Récupère le code source depuis le dépôt Git configuré dans le job
        // Jenkins (SCM). Le branche et le dépôt sont définis dans la
        // configuration du job, ce qui évite de les coder en dur ici.
        // =====================================================================
        stage('Checkout') {
            steps {
                // Effectue le checkout du code source dans le workspace.
                checkout scm
                echo "Checkout terminé sur la branche : ${env.BRANCH_NAME}"
            }
        }

        // =====================================================================
        // STAGE 2 : Informations du build
        // ---------------------------------------------------------------------
        // Affiche les principales informations du build en cours pour faciliter
        // le suivi et le diagnostic dans la console de Jenkins.
        // =====================================================================
        stage('Build Information') {
            steps {
                echo '================ BUILD INFORMATION ================'
                echo "Job name        : ${env.JOB_NAME}"
                echo "Build number    : ${env.BUILD_NUMBER}"
                echo "Build ID        : ${env.BUILD_ID}"
                echo "Branch          : ${env.BRANCH_NAME}"
                echo "Commit          : ${env.GIT_COMMIT}"
                echo "Workspace       : ${WORKSPACE}"
                echo "Build URL       : ${env.BUILD_URL}"
                echo "Node name       : ${env.NODE_NAME}"
                echo '===================================================='
            }
        }

        // =====================================================================
        // STAGE 3 : Contenu du workspace
        // ---------------------------------------------------------------------
        // Liste le contenu du workspace après le checkout afin de vérifier
        // visuellement que les fichiers attendus sont bien présents.
        // =====================================================================
        stage('Workspace Content') {
            steps {
                script {
                    echo '================ WORKSPACE CONTENT ================'
                    // Liste récursive simplifiée des 2 premiers niveaux
                    // pour avoir une vue d'ensemble sans surcharger les logs.
                    sh 'ls -la'
                    echo '----------------------------------------------------'
                    echo 'Contenu du dossier microservices :'
                    sh 'ls -la microservices 2>/dev/null || echo "microservices absent"'
                    echo '===================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 4 : Vérification de docker-compose.yml
        // ---------------------------------------------------------------------
        // Vérifie que le fichier docker-compose.yml existe à la racine du
        // dépôt. Il est indispensable pour orchestrer toute l'architecture.
        // =====================================================================
        stage('Verify docker-compose.yml') {
            steps {
                // Interrompt le pipeline (SUCCESS != true) si le fichier manque.
                script {
                    if (fileExists('docker-compose.yml')) {
                        echo 'OK : docker-compose.yml est présent.'
                    } else {
                        error 'ERREUR : docker-compose.yml est introuvable à la racine du dépôt.'
                    }
                }
            }
        }

        // =====================================================================
        // STAGE 5 : Vérification du dossier microservices
        // ---------------------------------------------------------------------
        // Vérifie que le dossier microservices existe. Il contient tous les
        // microservices Spring Cloud (discovery, gateway, identity, etc.).
        // =====================================================================
        stage('Verify microservices/') {
            steps {
                script {
                    if (fileExists('microservices')) {
                        echo 'OK : le dossier microservices est présent.'
                    } else {
                        error 'ERREUR : le dossier microservices est introuvable.'
                    }
                }
            }
        }

        // =====================================================================
        // STAGE 6 : Vérification du dossier backend
        // ---------------------------------------------------------------------
        // Vérifie que le dossier backend existe. Il contient le monolithe
        // Spring Boot du projet.
        // =====================================================================
        stage('Verify backend/') {
            steps {
                script {
                    if (fileExists('backend')) {
                        echo 'OK : le dossier backend est présent.'
                    } else {
                        error 'ERREUR : le dossier backend est introuvable.'
                    }
                }
            }
        }

        // =====================================================================
        // STAGE 7 : Compilation Maven
        // ---------------------------------------------------------------------
        // Compile exclusivement les modules Maven du dossier microservices,
        // sans lancer les tests ni construire les images Docker.
        // =====================================================================
        stage('Compilation Maven') {
            steps {
                script {
                    echo '================ COMPILATION MAVEN ================'
                    sh 'cd microservices/common-security && mvn clean install -DskipTests'
                    sh 'cd microservices/discovery-service && mvn clean compile'
                    sh 'cd microservices/gateway-service && mvn clean compile'
                    sh 'cd microservices/identity-service && mvn clean compile'
                    sh 'cd microservices/customer-service && mvn clean compile'
                    sh 'cd microservices/department-service && mvn clean compile'
                    sh 'cd microservices/employee-service && mvn clean compile'
                    sh 'cd microservices/fees-service && mvn clean compile'
                    sh 'cd microservices/hr-service && mvn clean compile'
                    sh 'cd microservices/invoice-service && mvn clean compile'
                    sh 'cd microservices/payroll-service && mvn clean compile'
                    echo '===================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 8 : Tests Maven
        // ---------------------------------------------------------------------
        // Exécute la suite de tests unitaires et d'intégration de chaque
        // microservice via la commande `mvn test`.
        //
        // Points importants :
        //   - `common-security` a déjà été installé dans le dépôt Maven local
        //     à l'étape 'Compilation Maven' (`mvn clean install -DskipTests`),
        //     les autres services peuvent donc résoudre cette dépendance
        //     pendant leurs propres tests.
        //   - `mvn test` se termine en SUCCESS même si un module ne contient
        //     aucun test (comportement par défaut de Surefire), ce qui
        //     garantit que le pipeline ne casse pas si un service n'a pas
        //     encore de tests ou si ses tests sont désactivés.
        //   - Ce stage s'exécute APRÈS la compilation, conformément à
        //     l'objectif : les tests doivent être exécutés après le build.
        //   - On n'utilise PAS `clean` ici : la compilation a déjà été
        //     effectuée au stage 'Compilation Maven'. `mvn test` réutilise
        //     les classes déjà compilées (target/classes) et ne recompile
        //     que le code de test (target/test-classes), ce qui évite une
        //     recompilation inutile de tout le projet.
        // =====================================================================
        stage('Tests Maven') {
            steps {
                script {
                    echo '==================== TESTS MAVEN ===================='
                    sh 'cd microservices/common-security && mvn test'
                    sh 'cd microservices/customer-service && mvn test'
                    sh 'cd microservices/department-service && mvn test'
                    sh 'cd microservices/employee-service && mvn test'
                    sh 'cd microservices/fees-service && mvn test'
                    sh 'cd microservices/hr-service && mvn test'
                    sh 'cd microservices/invoice-service && mvn test'
                    sh 'cd microservices/payroll-service && mvn test'
                    sh 'cd microservices/discovery-service && mvn test'
                    sh 'cd microservices/gateway-service && mvn test'
                    sh 'cd microservices/identity-service && mvn test'
                    echo '======================================================'
                }
            }
        }

        // =====================================================================
        // STAGE 9 : Packaging Maven
        // ---------------------------------------------------------------------
        // Produit le fichier JAR de chaque microservice via Maven.
        //
        // Points importants :
        //   - `mvn package` empaquète le projet : pour les services Spring Boot,
        //     le spring-boot-maven-plugin génère un JAR "exécutable" (fat jar)
        //     contenant les classes (BOOT-INF/classes) et les dépendances
        //     (BOOT-INF/lib). Pour common-security (module de type bibliothèque),
        //     il produit un JAR simple réutilisé comme dépendance par les
        //     services métier.
        //   - L'option `-DskipTests` évite de relancer les tests, déjà exécutés
        //     avec succès au stage 'Tests Maven'. Le code est donc déjà validé
        //     et le packaging est plus rapide (pas de recompilation des tests).
        //   - Les commandes `sh` sont exécutées séquentiellement : si un
        //     `mvn package` échoue (code retour non nul), la step échoue et
        //     Jenkins interrompt immédiatement le pipeline. Un seul service
        //     défaillant empêche la production des JAR suivants.
        // =====================================================================
        stage('Packaging Maven') {
            steps {
                script {
                    echo '==================== PACKAGING MAVEN ===================='
                    sh 'cd microservices/common-security && mvn package -DskipTests'
                    sh 'cd microservices/discovery-service && mvn package -DskipTests'
                    sh 'cd microservices/gateway-service && mvn package -DskipTests'
                    sh 'cd microservices/identity-service && mvn package -DskipTests'
                    sh 'cd microservices/customer-service && mvn package -DskipTests'
                    sh 'cd microservices/department-service && mvn package -DskipTests'
                    sh 'cd microservices/employee-service && mvn package -DskipTests'
                    sh 'cd microservices/fees-service && mvn package -DskipTests'
                    sh 'cd microservices/hr-service && mvn package -DskipTests'
                    sh 'cd microservices/invoice-service && mvn package -DskipTests'
                    sh 'cd microservices/payroll-service && mvn package -DskipTests'
                    echo '========================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 9b : Construction des images Docker (Docker Build)
        // ---------------------------------------------------------------------
        // Première évolution du pipeline V1 vers un vrai pipeline CI/CD.
        //
        // OBJECTIF (uniquement CI, aucune livraison) :
        //   - Vérifier que Docker est disponible sur l'agent.
        //   - Construire l'image Docker de CHAQUE microservice qui possède
        //     réellement un Dockerfile dans le dépôt.
        //   - Afficher clairement dans les logs quelle image est construite.
        //   - Page bloquante : si UNE construction Docker échoue, le pipeline
        //     s'arrête immédiatement.
        //
        // CONTRAINTES RESPECTÉES :
        //   - Aucun docker push (pas de registry Docker ajouté).
        //   - Aucun déploiement / aucune utilisation de docker-compose.
        //   - Aucune modification des Dockerfiles existants.
        //   - Aucun credential Jenkins ajouté.
        //   - Ne construit QUE les services ayant un Dockerfile présent.
        //     => common-security est UNIQUEMENT une bibliothèque Maven partagée
        //        (pas un service exécutable) : il n'a PAS de Dockerfile et n'est
        //        donc PAS conteneurisé ici.
        //
        // Tags utilisés : <crm>-<service>:latest (simples et cohérents).
        // =====================================================================
        stage('Docker Build') {
            steps {
                script {
                    echo '==================== DOCKER BUILD ===================='
                    // 1. Vérification de la disponibilité de Docker.
                    // Si la commande échoue (docker absent ou non exécutable),
                    // le pipeline s'arrête immédiatement.
                    sh 'docker --version'

// 2. Construction des images Docker des 10 microservices
                    //    disposant d'un Dockerfile. Ces commandes sont
                    //    séquentielles et BLOQUANTES : un `docker build` qui
                    //    échoue interrompt le pipeline (comportement par défaut
                    //    de `sh` sous Jenkins).
                    //
                    //    Chaque image reçoit DEUX tags :
                    //      - build-${BUILD_NUMBER}  -> traçabilité du build courant
                    //      - latest                 -> conserve le comportement actuel
                    //    Aucun docker push n'est effectué ici.

                    // Tag unique propre au build Jenkins courant.
                    def buildTag = "build-${BUILD_NUMBER}"

                    echo "Construction de l'image discovery-service (${buildTag})"
                    sh "docker build -t crm/discovery-service:${buildTag} -t crm/discovery-service:latest ./microservices/discovery-service"

                    echo "Construction de l'image gateway-service (${buildTag})"
                    sh "docker build -t crm/gateway-service:${buildTag} -t crm/gateway-service:latest ./microservices/gateway-service"

                    echo "Construction de l'image identity-service (${buildTag})"
                    sh "docker build -t crm/identity-service:${buildTag} -t crm/identity-service:latest ./microservices/identity-service"

                    echo "Construction de l'image customer-service (${buildTag})"
                    sh "docker build -t crm/customer-service:${buildTag} -t crm/customer-service:latest ./microservices/customer-service"

                    echo "Construction de l'image department-service (${buildTag})"
                    sh "docker build -t crm/department-service:${buildTag} -t crm/department-service:latest ./microservices/department-service"

                    echo "Construction de l'image employee-service (${buildTag})"
                    sh "docker build -t crm/employee-service:${buildTag} -t crm/employee-service:latest ./microservices/employee-service"

                    echo "Construction de l'image fees-service (${buildTag})"
                    sh "docker build -t crm/fees-service:${buildTag} -t crm/fees-service:latest ./microservices/fees-service"

                    echo "Construction de l'image hr-service (${buildTag})"
                    sh "docker build -t crm/hr-service:${buildTag} -t crm/hr-service:latest ./microservices/hr-service"

                    echo "Construction de l'image invoice-service (${buildTag})"
                    sh "docker build -t crm/invoice-service:${buildTag} -t crm/invoice-service:latest ./microservices/invoice-service"

                    echo "Construction de l'image payroll-service (${buildTag})"
                    sh "docker build -t crm/payroll-service:${buildTag} -t crm/payroll-service:latest ./microservices/payroll-service"

                    echo '======================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 9c : Validation de la configuration Docker Compose
        // ---------------------------------------------------------------------
        // Deuxième évolution du pipeline V1 vers un vrai pipeline CI/CD.
        //
        // OBJECTIF (uniquement CI, aucune livraison) :
        //   - Vérifier que le plugin Docker Compose v2 est disponible.
        //   - Vérifier que le fichier docker-compose.yml est présent.
        //   - Valider la syntaxe et la configuration du fichier avec
        //     `docker compose config`.
        //
        // COMPORTEMENT :
        //   - Stage BLOQUANT : si une commande échoue (docker compose absent,
        //     fichier absent, ou `docker compose config` invalide), le pipeline
        //     s'arrête immédiatement.
        //   - N'exécute PAS `docker compose up` ni `docker compose down`.
        //   - Aucun docker push, aucun registry, aucun credential Jenkins.
        //   - Aucune modification des Dockerfiles, pom.xml ou code métier.
        // =====================================================================
        stage('Docker Compose Validate') {
            steps {
                script {
                    echo '============ DOCKER COMPOSE VALIDATE =============='

                    // 1. Vérification de la disponibilité de Docker Compose.
                    //    Si `docker compose` est absent, le pipeline s'arrête.
                    sh 'docker compose version'

                    // 2. Vérification de la présence du fichier docker-compose.yml.
                    //    Obligatoire : le pipeline s'arrête s'il est introuvable.
                    if (fileExists('docker-compose.yml')) {
                        echo 'OK : docker-compose.yml est présent à la racine.'
                    } else {
                        error 'ERREUR : docker-compose.yml est introuvable à la racine du dépôt.'
                    }

                    // 3. Validation de la syntaxe / configuration.
                    //    BLOQUANT : si `docker compose config` échoue (YAML ou
                    //    configuration invalide), le pipeline s'arrête.
                    sh 'docker compose config'

                    echo 'OK : la configuration Docker Compose est valide.'
                    echo '==================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 9d : Test d'intégration de l'architecture conteneurisée
        //            (Docker Compose Integration Test)
        // ---------------------------------------------------------------------
        // Troisième évolution du pipeline V1 vers un vrai pipeline CI/CD.
        //
        // OBJECTIF : effectuer un VRAI test d'intégration de l'architecture
        // conteneurisée, SANS déploiement permanent :
        //   1. Vérifier que Docker Compose est disponible.
        //   2. Démarrer l'environnement complet (docker compose up -d).
        //   3. Afficher l'état des conteneurs (docker compose ps).
        //   4. Laisser quelques secondes aux services pour démarrer (sleep).
        //   5. Afficher les logs récents (docker compose logs --tail=50).
        //   6. Vérifier que les conteneurs sont bien démarrés.
        //
        // COMPORTEMENT :
        //   - Stage BLOQUANT : si docker compose up -d échoue, le pipeline
        //     échoue immédiatement.
        //   - Un bloc try/finally garantit que `docker compose down` est
        //     TOUJOURS exécuté, même si le test échoue (nettoyage propre,
        //     aucune architecture laissée en place après le test).
        //   - N'utilise PAS docker compose down AVANT le test, ni immédiatement
        //     après up sans avoir effectué les vérifications.
        //   - Aucun docker push, aucun registry, aucun credential Jenkins,
        //     aucun déploiement permanent/production.
        //   - N'invente AUCUN service : utilise uniquement les 11 services
        //     réellement définis dans docker-compose.yml.
        //   - Aucune modification de docker-compose.yml ni des Dockerfiles.
        // =====================================================================
stage('Docker Compose Integration Test') {
            steps {
                script {
                    echo '========= DOCKER COMPOSE INTEGRATION TEST =========='

                    // try/finally : `docker compose down` S'EXECUTE TOUJOURS
                    // même si le test échoue, pour nettoyer l'environnement.
                    try {
                        // 1. Vérification de la disponibilité de Docker Compose.
                        sh 'docker compose version'

                        // 2. Démarrage de l'architecture conteneurisée.
                        //    BLOQUANT : si cette commande échoue, une exception
                        //    est levée -> le pipeline échoue (puis finally nettoie).
                        echo "Démarrage de l'environnement avec docker compose up -d ..."
                        sh 'docker compose up -d'

                        // 3. Affichage de l'état des conteneurs (démarrés ou non).
                        echo 'État des conteneurs (docker compose ps) :'
                        sh 'docker compose ps'

                        // ==========================================================
                        // 4. VÉRIFICATION RÉELLE DE SANTÉ APPLICATIVE (retry)
                        // ==========================================================
                        // On remplace le simple "sleep 45" par un véritable contrôle
                        // de santé : on interroge un endpoint HTTP réellement
                        // confirmé dans le dépôt, avec plusieurs tentatives.
                        //
                        // Endpoints utilisés (origine confirmée dans le dépôt) :
                        //   - discovery-service : endpoint REST natif Eureka
                        //     http://localhost:8761/eureka/apps
                        //     (le discovery-service est un serveur Eureka, son
                        //     pom.xml ne contient PAS d'Actuator)
                        //   - gateway-service : /actuator/health
                        //     (management.endpoints.web.exposure.include = health,info,metrics)
                        //   - services métier : /actuator/health
                        //     (management.endpoints.web.exposure.include = health,info)
                        //
                        // Mécanisme de retry :
                        //   - plusieurs tentatives ;
                        //   - attente entre chaque tentative ;
                        //   - arrêt en FAILURE si un service ne devient jamais
                        //     disponible après le nombre maximal de tentatives.
                        // ==========================================================

                        // Paramètres du mécanisme de retry.
                        def maxRetries    = 30   // nombre maximal de tentatives par service
                        def retryDelaySec = 5    // attente entre 2 tentatives (secondes)
                        def connectTo     = 5    // timeout de connexion curl (secondes)

                        // Liste des contrôles : [nom, port_hôte, endpoint, code_attendu].
                        // Les ports sont ceux publiés par docker-compose.yml sur le host.
                        def healthChecks = [
                            ['discovery-service',  8761, '/eureka/apps',       '200'],
                            ['gateway-service',    8080, '/actuator/health',   '200'],
                            ['identity-service',   8082, '/actuator/health',   '200'],
                            ['employee-service',   8083, '/actuator/health',   '200'],
                            ['department-service', 8084, '/actuator/health',   '200'],
                            ['customer-service',   8085, '/actuator/health',   '200'],
                            ['hr-service',         8086, '/actuator/health',   '200'],
                            ['payroll-service',    8087, '/actuator/health',   '200'],
                            ['fees-service',       8088, '/actuator/health',   '200'],
                            ['invoice-service',    8089, '/actuator/health',   '200']
                        ]

                        // Suivi global : true si TOUS les services sont sains.
                        def allHealthy = true

                        // Boucle sur chaque service à contrôler.
                        for (def c : healthChecks) {
                            def name     = c[0]
                            def port     = c[1]
                            def endpoint = c[2]
                            def url      = "http://localhost:${port}${endpoint}"
                            def healthy  = false

                            echo "  -> Contrôle de santé de ${name} via ${url}"

                            // Boucle de retry jusqu'à maxRetries tentatives.
                            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                                // curl silencieux, on ne récupère que le code HTTP.
                                // Si la connexion échoue, on renvoie '000'.
                                def code = sh(
                                    script: "curl -s -o /dev/null -w '%{http_code}' --connect-timeout ${connectTo} --max-time 10 ${url} || echo '000'",
                                    returnStdout: true
                                ).trim()

                                if (code == '200') {
                                    echo "  ✓ ${name} est sain (HTTP ${code}) après ${attempt} tentative(s)."
                                    healthy = true
                                    break
                                } else {
                                    echo "  ... ${name} : HTTP ${code} (tentative ${attempt}/${maxRetries}). Nouvel essai dans ${retryDelaySec}s."
                                    if (attempt < maxRetries) {
                                        sleep(retryDelaySec)
                                    }
                                }
                            }

                            if (!healthy) {
                                allHealthy = false
                                echo "  ✗ ÉCHEC : ${name} n'est PAS devenu disponible sur ${url} après ${maxRetries} tentatives."
                            }
                        }

                        // ==========================================================
                        // 5. Traitement du résultat des contrôles de santé
                        // ==========================================================
                        // Si au moins un service est en échec : on affiche l'état
                        // des conteneurs et les logs AVANT de faire échouer le
                        // pipeline (le finally exécutera ensuite docker compose down).
                        if (!allHealthy) {
                            echo 'Un ou plusieurs services ne sont pas sains. Diagnostic :'
                            sh 'docker compose ps'
                            sh 'docker compose logs --tail=100'
                            // Stage BLOQUANT : le pipeline termine en FAILURE.
                            error 'ERREUR : un ou plusieurs services ne sont pas devenus opérationnels. Pipeline en FAILURE.'
                        }

                        // Si tout est sain : affichage des logs récents (résumé)
                        // pour le diagnostic, sans bloquer le pipeline.
                        echo 'Tous les services sont opérationnels. Logs récents (résumé) :'
                        sh 'docker compose logs --tail=50'

                        echo "OK : le test d'intégration Docker Compose est terminé (tous les services sains)."
                    } finally {
                        // Nettoyage OBLIGATOIRE de l'environnement, exécuté
                        // que le test réussisse ou échoue.
                        echo "Nettoyage de l'environnement (docker compose down) ..."
                        sh 'docker compose down'
                        echo 'Environnement arrêté et nettoyé.'
                    }
                    echo '==================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 9e : Publication des images Docker vers le Registry (Docker Push)
        // ---------------------------------------------------------------------
        // Cinquième évolution du pipeline V1 vers un vrai pipeline CI/CD : la
        // partie CD (livraison) vers un Docker Registry (Docker Hub par défaut).
        //
        // OBJECTIF :
        //   1. Vérifier que Docker est disponible.
        //   2. Vérifier que DOCKER_REGISTRY et DOCKER_IMAGE_BASE sont définis.
        //   3. Effectuer un `docker login` SÉCURISÉ via withCredentials
        //      (le mot de passe ne transite JAMAIS en clair dans les logs).
        //   4. Publier les 10 images construites au stage 'Docker Build' avec
        //      chacune DEUX tags : build-${BUILD_NUMBER} et latest.
        //   5. common-security n'est PAS publié (bibliothèque Maven, pas de
        //      Dockerfile).
        //
        // SÉCURITÉ :
        //   - Aucun username / password en dur dans le Jenkinsfile.
        //   - Utilise le credential Jenkins Username/Password dont l'ID est :
        //         docker-registry-credentials
        //   - Le bloc withCredentials est LE SEUL endroit où le credential est
        //     utilisé (docker login). Il n'est pas utilisé ailleurs.
        //   - Le credential N'EST PAS créé par ce code : il doit être créé
        //     manuellement dans Jenkins (voir rubrique configuration).
        //
        // COMPORTEMENT :
        //   - Stage BLOQUANT : si docker login ou un docker push échoue,
        //     le pipeline devient FAILURE.
        //   - Aucun docker compose up/down, aucun déploiement, aucun registre
        //     supplémentaire dans ce stage.
        // =====================================================================
        stage('Docker Push') {
            steps {
                script {
                    echo '==================== DOCKER PUSH ===================='

                    // 1. Vérification de la disponibilité de Docker.
                    sh 'docker --version'

                    // 2. Vérification de DOCKER_REGISTRY et DOCKER_IMAGE_BASE.
                    //    Obligatoires : sans eux, impossible de taguer/pousser.
                    if (!env.DOCKER_REGISTRY || !env.DOCKER_IMAGE_BASE) {
                        error 'ERREUR : DOCKER_REGISTRY ou DOCKER_IMAGE_BASE non défini. Configurez ces variables dans le job Jenkins.'
                    }
                    echo "Registry cible : ${DOCKER_REGISTRY}"
                    echo "Base image     : ${DOCKER_IMAGE_BASE}"

                    // Tag unique propre au build Jenkins courant.
                    def buildTag = "build-${BUILD_NUMBER}"

                    // Liste ordonnée des 10 microservices à publier.
                    // common-security est volontairement EXCLU (bibliothèque).
                    def services = [
                        'discovery-service',
                        'gateway-service',
                        'identity-service',
                        'customer-service',
                        'department-service',
                        'employee-service',
                        'fees-service',
                        'hr-service',
                        'invoice-service',
                        'payroll-service'
                    ]

                    // 3. docker login sécurisé via withCredentials.
                    //    Seul docker login est entouré par le credential.
                    //    Le mot de passe n'apparaît jamais en clair dans les logs.
withCredentials([usernamePassword(
                        credentialsId: 'docker-registry-credentials',
                        usernameVariable: 'DOCKER_REGISTRY_USER',
                        passwordVariable: 'DOCKER_REGISTRY_PASSWORD'
                    )]) {
                        echo 'Connexion au Docker Registry (docker login) ...'
                        // Méthode SÉCURISÉE : le mot de passe est envoyé via
                        // stdin (--password-stdin) et n'apparaît JAMAIS en clair
                        // dans les logs ni dans la ligne de commande.
                        sh "echo '${DOCKER_REGISTRY_PASSWORD}' | docker login '${DOCKER_REGISTRY}' -u '${DOCKER_REGISTRY_USER}' --password-stdin"
                        echo 'docker login effectué avec succès.'
                    }

                    // 4. Tag + push des 10 images (2 tags chacune).
                    for (String svc : services) {
                        // Nom local de l'image construit au stage 'Docker Build'.
                        def localImage = "crm/${svc}"

                        // Nom complet sur Docker Hub :
                        //   <username>/crm-<service>:build-${BUILD_NUMBER}
                        //   <username>/crm-<service>:latest
                        // (DOCKER_IMAGE_BASE est l'utilisateur Docker Hub, SANS /crm)
                        def registryImage = "${DOCKER_IMAGE_BASE}/crm-${svc}"

                        // Tag build-${BUILD_NUMBER}
                        echo "Tag + push de ${registryImage}:${buildTag}"
                        sh "docker tag ${localImage}:${buildTag} ${registryImage}:${buildTag}"
                        sh "docker push ${registryImage}:${buildTag}"

                        // Tag latest
                        echo "Tag + push de ${registryImage}:latest"
                        sh "docker tag ${localImage}:latest ${registryImage}:latest"
                        sh "docker push ${registryImage}:latest"
                    }

                    echo 'OK : les 10 images Docker ont été publiées avec succès.'
                    echo '======================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 9f : Archivage des artefacts de build (Archive Artifacts)
        // ---------------------------------------------------------------------
        // Quatrième évolution du pipeline V1 vers un vrai pipeline CI/CD.
        //
        // OBJECTIF : conserver les artefacts produits par les stages précédents
        // (SANS relancer compilation / tests / packaging / docker) :
        //   1. Archiver les JAR produits par Maven (microservices + common-security).
        //   2. Archiver les rapports Surefire XML des tests Maven.
        //   3. N'archiver QUE les fichiers ciblés (JAR + rapports) pour éviter
        //      les sources, logs ou autres fichiers du workspace.
        //   4. Utiliser la fonctionnalité native Jenkins `archiveArtifacts`.
        //   5. Publier les rapports de tests via `junit` (méthode Jenkins
        //      appropriée), sans faire échouer le pipeline si un module ne
        //      possède pas de rapport (testResults critère échoue si aucun,
        //      donc on utilise allowEmptyResults).
        //
        // IMPORTANT :
        //   - Reste bloquant pour les vrais problèmes de génération d'artefacts
        //     (aucun JAR trouvé -> erreur explicite).
        //   - Ne fait PAS échouer le pipeline simplement parce qu'un module
        //     ne possède aucun rapport de test.
        //   - N'exécute aucune compilation, aucun test, aucun packaging,
        //     aucun docker build, aucun docker compose.
        //   - Aucun registry, aucun docker push, aucun déploiement.
        // =====================================================================
        stage('Archive Artifacts') {
            steps {
                script {
                    echo '================ ARCHIVE ARTIFACTS ================'

// 1. Message : l'archivage commence.
                    echo "Démarrage de l'archivage des artefacts du build."

                    // Liste des JAR des microservices (target/*.jar).
                    // Le glob "microservices/*/target/*.jar" couvre aussi
                    // common-security (présent sous microservices/common-security).
                    def jarArtifacts = 'microservices/*/target/*.jar'

                    // Les rapports de tests Surefire (XML).
                    def testArtifacts = 'microservices/**/target/surefire-reports/*.xml'

                    // 2. Message : types d'artefacts archivés.
                    echo 'Types d'artefacts à archiver :'
                    echo "  - JAR          : ${jarArtifacts}"
                    echo "  - Rapports tests XML : ${testArtifacts}"

                    // 3. Vérification préalable : existerait-il des JAR ?
                    //    On compte les fichiers matches via le workspace.
                    //    (findFileInWorkspace est disponible via la pipeline utility steps,
                    //    mais ici on utilise une approche shell simple et robuste.)
                    def hasJar = sh(
                        script: "ls ${jarArtifacts} >/dev/null 2>&1",
                        returnStatus: true
                    ) == 0

                    if (hasJar) {
                        echo 'OK : des JAR ont été trouvés dans microservices/*/target/.'

                        // 4. Archivage des JAR (fonctionnalité native Jenkins).
                        echo 'Archivage des JAR en cours (archiveArtifacts) ...'
                        archiveArtifacts artifacts: jarArtifacts, fingerprint: true, allowEmptyArchive: false
                        echo 'OK : JAR archivés.'
                    } else {
                        // Bloquant : aucun JAR produit -> vrai problème.
                        error 'ERREUR : aucun JAR trouvé dans microservices/*/target/. La génération des artefacts a échoué.'
                    }

                    // 5. Message : traitement des rapports de tests.
                    echo 'Traitement des rapports de tests Surefire ...'

                    // Publication JUnit : Jenkins publie les graphiques/trends.
                    // allowEmptyResults: true => ne fait PAS échouer le pipeline
                    // si un module ne possède aucun rapport de test.
                    // (junit échoue seulement en cas d'échec réel de test, pas
                    // en l'absence de rapports).
                    junit testResults: testArtifacts,
                         allowEmptyResults: true,
                         skipPublishingChecks: true

                    echo 'OK : rapports de tests Surefire publiés (junit).'

                    // 6. Message : archivage terminé.
                    echo 'Archivage des artefacts terminé.'
                    echo '====================================================='
                }
            }
        }

        // =====================================================================
        // STAGE 10 : Résumé final
        // ---------------------------------------------------------------------
        // Affiche un récapitulatif de toutes les vérifications effectuées.
        // Ce stage n'atteint ce point que si toutes les vérifications
        // précédentes ont réussi. Le pipeline se termine alors en SUCCESS.
        // =====================================================================
        stage('Result') {
            steps {
                echo '================ PIPELINE RESULT ===================='
                echo 'Toutes les vérifications structurelles sont OK :'
                echo '  - Checkout du dépôt           : OK'
                echo '  - Informations du build       : OK'
                echo '  - Contenu du workspace        : OK'
                echo '  - docker-compose.yml présent  : OK'
                echo '  - microservices/ présent      : OK'
                echo '  - backend/ présent            : OK'
                echo '  - Compilation Maven           : OK'
                echo '  - Tests Maven                 : OK'
                echo '  - Packaging Maven             : OK'
                echo 'Le pipeline V1 se termine en SUCCESS.'
                echo '======================================================'
            }
        }
    }

    // -------------------------------------------------------------------------
    // post : actions exécutées après les stages, quel que soit le résultat.
    //   - always : affiche un message final de fin de pipeline.
    //   - success : message explicite quand tout est vert.
    // Il sera enrichi plus tard (archivage d'artefacts, notifications, etc.).
    // -------------------------------------------------------------------------
    post {
        always {
            echo "Fin du pipeline ${PROJECT_NAME} (build #${env.BUILD_NUMBER})."
        }
        success {
            echo 'SUCCESS : le pipeline V1 s est terminé correctement.'
        }
    }
}
