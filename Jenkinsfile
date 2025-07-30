pipeline {
    agent any
    environment {
        DOCKERHUB_USER = 'songchih'
        DOCKERHUB_PASS = credentials('dockerhub-pass')
    }
    triggers {
        githubPush()
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Build & Dockerize') {
            steps {
                sh '''
                ./gradlew clean build -x test
                echo $DOCKERHUB_PASS | docker login -u $DOCKERHUB_USER --password-stdin
                docker build -t $DOCKERHUB_USER/fairplay-backend:latest .
                docker push $DOCKERHUB_USER/fairplay-backend:latest
                '''
            }
        }
        stage('Deploy') {
            steps {
                sh '''
                cd /home/ubuntu/fairplay-deploy
                docker compose pull fairplay-backend
                docker compose up -d fairplay-backend
                '''
            }
        }
    }
}
