// Declarative pipeline for ClearFund.
// Assumes the agent has JDK 21 and Maven on PATH (or configure them via
// Jenkins "Global Tool Configuration" and a tools { } block).
pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 20, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -DskipTests clean compile'
            }
        }

        stage('Unit tests') {
            // The Testcontainers integration test auto-skips when the agent
            // has no Docker, so this stage stays green on lightweight agents.
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('Static analysis') {
            // Placeholder. Wire in SpotBugs / Checkstyle / SonarQube here,
            // e.g.  sh 'mvn -B spotbugs:check checkstyle:check'
            steps {
                echo 'Static analysis placeholder - no analyzers configured yet.'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B -DskipTests package'
                archiveArtifacts artifacts: 'target/clearfund-*.jar', fingerprint: true
            }
        }
    }

    post {
        success {
            echo 'Pipeline succeeded.'
        }
        failure {
            echo 'Pipeline failed - check the stage logs and test reports.'
        }
    }
}
