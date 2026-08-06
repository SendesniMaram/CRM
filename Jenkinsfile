// =============================================================================
// Jenkinsfile — CRM Spring Boot Microservices (Version 1)
// -----------------------------------------------------------------------------
// Pipeline déclaratif Jenkins moderne pour un projet multi-modules :
//   - backend/                      -> monolithe Spring Boot
//   - microservices/                -> architecture microservices Spring Cloud
//   - docker-compose.yml            -> orchestration Docker Compose
//
// NOTE IMPORTANTE (V1) :
//   Maven n'est PAS encore installé dans le conteneur Jenkins.
//   Ce premier pipeline NE lance donc ni compilation, ni tests,
//   ni build Docker, ni docker-compose.
//   Il sert uniquement de base évolutive : il vérifie que la structure
//   du dépôt est correcte et que le workspace contient tous les éléments
//   nécessaires avant d'ajouter plus tard les étapes de build.
//
// Le pipeline se termine en SUCCESS uniquement si toutes les vérifications
// structurelles passent.
// =============================================================================

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
    }

    stages {

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
                    sh 'cd microservices/common-security && mvn clean compile'
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
        // STAGE 8 : Résumé final
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
