pipeline {
    agent any
    environment {
        DOCKERHUB_USER = 'songchih'
        DOCKERHUB_PASS = credentials('dockerhub-pass')
        FRONTEND_ENV = credentials('fairplay-fe-env')
    }
    stages {
        stage('Checkout Backend') {
            steps {
                checkout scm
            }
        }
        stage('Checkout Frontend') {
            steps {
                sh 'rm -rf fairplay-fe'
                sh 'git clone https://github.com/rktclgh/FairPlay_FE.git fairplay-fe'
            }
        }
        stage('Prepare Frontend Environment') {
            steps {
                dir('fairplay-fe') {
                    withCredentials([file(credentialsId: 'fairplay-fe-env', variable: 'ENV_FILE')]) {
                        sh 'cp $ENV_FILE .env'
                    }
                }
            }
        }
        stage('Frontend Build') {
            steps {
                dir('fairplay-fe') {
                    sh 'npm install'
                    sh 'npm run build'
                }
            }
        }
        stage('Copy Frontend to Backend') {
            steps {
                sh '''
                    mkdir -p src/main/resources/static
                    rm -rf src/main/resources/static/*
                    cp -R fairplay-fe/dist/* src/main/resources/static/
                '''
            }
        }
        stage('Backend Build & Dockerize') {
            steps {
                sh '''
                    ./gradlew clean build -x test
                    echo $DOCKERHUB_PASS | docker login -u $DOCKERHUB_USER --password-stdin
                    docker buildx create --use || true
                    docker buildx inspect --bootstrap
                    docker buildx build --platform linux/amd64,linux/arm64 -t $DOCKERHUB_USER/fairplay-backend:latest --push .
                '''
            }
        }
    }
}
