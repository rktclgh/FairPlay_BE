pipeline {
    agent any
    environment {
        DOCKERHUB_USER = 'songchih'
        DOCKERHUB_PASS = credentials('dockerhub-pass')
    }
    stages {
        stage('Checkout Backend') {
            steps {
                checkout scm
            }
        }
        stage('Checkout Frontend') {
            steps {
                sh 'git clone https://github.com/Fairing-15th/FairPlay_FE.git fairplay-fe'
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
                sh 'rm -rf fairplay/src/main/resources/static/*'
                sh 'cp -R fairplay-fe/dist/* fairplay/src/main/resources/static/'
            }
        }
        stage('Backend Build & Dockerize') {
            steps {
                dir('fairplay') {
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
}
