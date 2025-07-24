pipeline {
    agent any

    stages {
        stage('Clone') {
            steps {
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh '''
                                   export $(cat .env | xargs)
                                   ./gradlew clean build
                               '''
            }
        }
    }
}
