pipeline {
    agent any
    environment {
        DOCKERHUB_USER = 'songchih'
        DOCKERHUB_PASS = credentials('dockerhub-pass')
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
              docker buildx create --use
              docker buildx build --platform linux/amd64,linux/arm64 -t $DOCKERHUB_USER/fairplay-backend:latest --push .
            '''
          }
        }
    }
}
