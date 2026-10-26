def microservices = [
    'discovery-service', 'gateway-service', 'identity-service',
    'employee-service', 'department-service', 'customer-service',
    'hr-service', 'payroll-service', 'fees-service', 'invoice-service'
]

def runCommand(String command) {
    if (isUnix()) {
        sh command
    } else {
        bat "@call ${command}"
    }
}

def runMaven(String goals) {
    if (isUnix()) {
        sh 'mvn -B -ntp "-Dmaven.repo.local=$CI_MAVEN_REPO" -DfailIfNoTests=true ' + goals
    } else {
        bat '@call mvn -B -ntp "-Dmaven.repo.local=%CI_MAVEN_REPO%" -DfailIfNoTests=true ' + goals
    }
    if (!fileExists('target/site/jacoco/jacoco.xml')) {
        error('Maven succeeded without the required JaCoCo XML report.')
    }
}

pipeline {
    agent any
    options {
        skipDefaultCheckout(true)
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
    }
    environment {
        CI = 'true'
        // Fresh source/report directory per run; no cleanup of a developer workspace.
        CI_SOURCE_DIR = "ci-workspace/build-${env.BUILD_NUMBER}"
        CI_MAVEN_REPO = "${env.WORKSPACE}/.m2-ci"
    }
    stages {
        stage('Checkout publication-clean') {
            steps {
                dir(env.CI_SOURCE_DIR) {
                    script {
                        if (env.BRANCH_NAME && env.BRANCH_NAME != 'publication-clean') {
                            error('Configure this CI job for publication-clean only.')
                        }
                        checkout([
                            $class: 'GitSCM',
                            branches: [[name: '*/publication-clean']],
                            userRemoteConfigs: scm.userRemoteConfigs.collect { remote ->
                                [url: remote.url, credentialsId: remote.credentialsId,
                                 refspec: '+refs/heads/publication-clean:refs/remotes/origin/publication-clean']
                            },
                            extensions: [[$class: 'CloneOption', shallow: true, depth: 1,
                                          noTags: true, honorRefspec: true]]
                        ])
                    }
                }
            }
        }
        stage('Verify Java 21, Maven, Node and browser') {
            steps {
                dir(env.CI_SOURCE_DIR) {
                    script {
                        runCommand('node scripts/ci/check-tools.cjs')
                        runCommand('node --test --test-reporter=junit --test-reporter-destination=ci-tools.xml scripts/ci/check-tools.test.cjs')
                    }
                }
            }
        }
        stage('Build and test common-security') {
            steps {
                dir("${env.CI_SOURCE_DIR}/microservices/common-security") {
                    script {
                        runMaven('clean org.jacoco:jacoco-maven-plugin:0.8.15:prepare-agent install org.jacoco:jacoco-maven-plugin:0.8.15:report')
                    }
                }
            }
        }
        stage('Build and test ten microservices') {
            steps {
                script {
                    microservices.each { service ->
                        dir("${env.CI_SOURCE_DIR}/microservices/${service}") {
                            echo "Compile, test and package: ${service}"
                            runMaven('clean org.jacoco:jacoco-maven-plugin:0.8.15:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.15:report')
                        }
                    }
                }
            }
        }
        stage('Build and test legacy backend') {
            steps {
                dir("${env.CI_SOURCE_DIR}/backend") {
                    script {
                        runMaven('clean org.jacoco:jacoco-maven-plugin:0.8.15:prepare-agent verify org.jacoco:jacoco-maven-plugin:0.8.15:report')
                    }
                }
            }
        }
        stage('Install Angular dependencies') {
            steps {
                dir("${env.CI_SOURCE_DIR}/frontend") {
                    script { runCommand('npm ci') }
                }
            }
        }
        stage('Angular tests, JUnit and coverage') {
            steps {
                dir("${env.CI_SOURCE_DIR}/frontend") {
                    script {
                        runCommand('npm run test:ci')
                        if (!fileExists('test-results/angular.xml') || !fileExists('coverage/frontend/lcov.info')) {
                            error('Angular succeeded without the required JUnit/LCOV reports.')
                        }
                    }
                }
            }
        }
        stage('Angular production build') {
            steps {
                dir("${env.CI_SOURCE_DIR}/frontend") {
                    script {
                        runCommand('npm run build -- --configuration production')
                        if (!fileExists('dist/frontend/browser/index.html')) {
                            error('Angular production output is missing.')
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                def source = env.CI_SOURCE_DIR
                def allowMissing = currentBuild.currentResult != 'SUCCESS'
                archiveArtifacts(
                    artifacts: "${source}/microservices/*/target/*.jar,${source}/backend/target/*.jar," +
                        "${source}/microservices/*/target/site/jacoco/**,${source}/backend/target/site/jacoco/**," +
                        "${source}/microservices/*/target/surefire-reports/TEST-*.xml,${source}/backend/target/surefire-reports/TEST-*.xml," +
                        "${source}/ci-tools.xml,${source}/frontend/test-results/*.xml,${source}/frontend/coverage/frontend/**," +
                        "${source}/frontend/dist/frontend/browser/**",
                    allowEmptyArchive: allowMissing, fingerprint: true
                )
                def tests = junit(
                    testResults: "${source}/microservices/*/target/surefire-reports/TEST-*.xml," +
                        "${source}/backend/target/surefire-reports/TEST-*.xml,${source}/frontend/test-results/*.xml,${source}/ci-tools.xml",
                    allowEmptyResults: allowMissing, skipPublishingChecks: true
                )
                if (tests.failCount > 0) {
                    error("CI failed: ${tests.failCount} failed tests in JUnit reports.")
                }
            }
        }
        success { echo 'CI completed: builds and tests passed. No deployment performed.' }
        failure { echo 'CI failed. Inspect the failed stage and archived test reports.' }
    }
}
