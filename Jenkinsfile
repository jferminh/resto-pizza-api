pipeline {
    agent any

    tools {
        maven 'Maven'
    }

    environment {
        DOCKER_IMAGE       = "jferminh/resto-pizza-api"
        DOCKER_CREDENTIALS = 'dockerhub-credentials'
        SONAR_TOKEN        = credentials('sonar-token')
        SONAR_PROJECT_KEY  = 'jferminh_resto-pizza-api'
        SONAR_ORGANIZATION = 'jferminh'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'mvn clean verify --no-transfer-progress'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Allure Report') {
            steps {
                sh 'mvn allure:report --no-transfer-progress'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/site/allure-maven-plugin/**',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('SonarCloud') {
            steps {
                sh """
                    mvn sonar:sonar --no-transfer-progress \\
                        -Dsonar.projectKey=${SONAR_PROJECT_KEY} \\
                        -Dsonar.organization=${SONAR_ORGANIZATION} \\
                        -Dsonar.host.url=https://sonarcloud.io \\
                        -Dsonar.token=${SONAR_TOKEN} \\
                        -Dsonar.java.source=25
                """
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker build -t ${DOCKER_IMAGE}:latest -t ${DOCKER_IMAGE}:${env.GIT_COMMIT[0..6]} ."
            }
        }

        stage('Docker Push') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' }
            }
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKER_CREDENTIALS,
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_TOKEN'
                )]) {
                    sh """
                        echo \$DOCKER_TOKEN | docker login -u \$DOCKER_USER --password-stdin
                        docker push ${DOCKER_IMAGE}:latest
                        docker push ${DOCKER_IMAGE}:${env.GIT_COMMIT[0..6]}
                        docker logout
                    """
                }
            }
        }
    }

    post {
        success {
            echo '✅ Pipeline resto-pizza-api terminé avec succès !'
        }
        failure {
            echo '❌ Pipeline échoué — vérifier les logs ci-dessus.'
        }
    }
}