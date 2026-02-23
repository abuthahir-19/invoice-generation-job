pipeline {
    agent any

    environment {
        // === Basic App Info ===
        APP_NAME          = 'invoice-generation-job'
        IMAGE_TAG         = "${env.BUILD_NUMBER}"   // Use build number for versioning (recommended)

        // === GCP Configuration (used in later stages) ===
        GCP_PROJECT       = 'gcp-learning-acnt'
        REGION            = 'us-central1'
        ARTIFACT_REGISTRY = 'us-central1-docker.pkg.dev'
        REPO_NAME         = 'my-batch-repo'
        CLOUD_RUN_JOB     = 'invoice-generation-job'

        // === Control Flags (Change these for local testing) ===
        SKIP_GCP_PUSH     = 'false'   // Set to 'true' during initial local testing
        SKIP_CLOUD_RUN    = 'false'   // Set to 'true' during initial local testing
    }

    tools {
        maven 'Maven3'
        jdk 'JDK21'
    }

    stages {

        stage('Checkout Code') {
            steps {
                checkout scm
                echo "✅ Code checked out successfully"
            }
        }

        stage('Build & Run Tests') {
            steps {
                sh 'mvn clean package'
                echo "✅ Maven build and tests completed successfully"
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    try {
                        sh "docker build -t ${APP_NAME}:${IMAGE_TAG} ."
                        echo "✅ Docker image built locally: ${APP_NAME}:${IMAGE_TAG}"
                    } catch (err) {
                        error "❌ Docker build failed: ${err}"
                    }
                }
            }
        }

        stage('Push Image to Artifact Registry') {
            when {
                expression { SKIP_GCP_PUSH == 'false' }
            }
            steps {
                withCredentials([file(credentialsId: 'gcp-secret-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    sh '''
                        echo "🔐 Authenticating with GCP using service account..."
                        gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                        gcloud auth configure-docker ${ARTIFACT_REGISTRY}

                        echo "🚀 Pushing image to Artifact Registry..."
                        docker tag ${APP_NAME}:${IMAGE_TAG} ${ARTIFACT_REGISTRY}/${GCP_PROJECT}/${REPO_NAME}/${APP_NAME}:${IMAGE_TAG}
                        docker push ${ARTIFACT_REGISTRY}/${GCP_PROJECT}/${REPO_NAME}/${APP_NAME}:${IMAGE_TAG}
                    '''
                }
                echo "✅ Image successfully pushed to Artifact Registry"
            }
        }

        stage('Deploy to Cloud Run Job') {
            when {
                expression { SKIP_CLOUD_RUN == 'false' }
            }
            steps {
                withCredentials([file(credentialsId: 'gcp-secret-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    sh '''
                        gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS
                        gcloud run jobs update ${CLOUD_RUN_JOB} \
                          --image ${ARTIFACT_REGISTRY}/${GCP_PROJECT}/${REPO_NAME}/${APP_NAME}:${IMAGE_TAG} \
                          --region ${REGION}
                    '''
                }
                echo "✅ Cloud Run Job updated with new image"
            }
        }
    }

    post {
        success {
            echo '🎉 Pipeline completed successfully!'
        }
        failure {
            echo '❌ Pipeline failed. Check logs above.'
        }
    }
}