// 필요한 변수를 선언할 수 있다. (내가 직접 선언하는 변수, 젠킨스 환경변수를 끌고 올 수 있음)
def ecrLoginHelper="docker-credential-ecr-login" // ECR credential helper 이름
def deployHost = "172.31.10.141"    // 배포서버 private ip address

// 젠킨스의 선언형 파이프라인 정의부 시작 (그루비 언어)
pipeline {
    agent any // 어느 젠킨스 서버에서도 실행이 가능
    environment {
        REGION = "ap-northeast-2"
        ECR_URL = "124355678220.dkr.ecr.ap-northeast-2.amazonaws.com"
        SERVICE_DIRS = "config-service,discoveryservice,gateway-service,user-service,ordering-service,product-service"
    }
    stages {
        stage('Pull Codes from Github'){ // 스테이지 제목 (맘대로 써도 됨.)
            steps{
                checkout scm // 젠킨스와 연결된 소스 컨트롤 매니저(git 등)에서 코드를 가져오는 명령어
            }
        }
        //--------------------------------------------
        stage('Detect Changes') {
            steps {
                script {
                    // 변경된 파일 감지
                    def changedFiles = sh(script: "git diff --name-only HEAD~1 HEAD", returnStdout: true)
                        .trim()
                        .split('\n') // 변경된 파일을 줄 단위로 분리

                    // 변경된 파일 출력
                    // user-service/src/main/resources/application.yml
                    // user-service/src/main/java/com/plaudata/userservice/controller/userController.java
                    echo "Changed files: ${changedFiles}"

                    def changedServices = []    // 이 배열에 들어가는 애들만 빌드함.
                    def serviceDirs = env.SERVICE_DIRS.split(",")

                    serviceDirs.each { service ->
                        if (changedFiles.any { it.startsWith(service + "/") }) {
                            changedServices.add(service)
                        }
                    }

                    env.CHANGED_SERVICES = changedServices.join(",")
                    if (env.CHANGED_SERVICES == "") {
                        echo "No changes detected in service directories. Skipping build and deployment."
                        // 성공 상태로 파이프라인 종료
                        currentBuild.result = 'SUCCESS' // 성공으로 표시
                    }
                }
            }
        }
        //--------------------------------------------

//         stage('Build Codes by Gradle') {
//             steps {
//                 script {
//                     def serviceDirs = env.SERVICE_DIRS.split(",")
//                     serviceDirs.each { service ->
//                         sh """
//                         echo "Building ${service}"
//                         cd ${service}
//                         chmod +x ./gradlew
//                         ./gradlew clean build -x test
//                         ls -al ./build/libs
//                         cd ..
//                         """
//                     }
//                 }
//             }
//         }
        stage('Build Changed Services') {
        // 이 스테이지는 빌드되어야 할 서비스가 존재한다면 실행되는 스테이지.
        // 이전 스테이지에서 체팅한 CHANGED_SERVICES라는 환경변수가 비어있지 않아야만 실행.
            when {
                expression { env.CHANGED_SERVICES != "" } // 변경된 서비스가 있을 때만 실행
            }
            steps {
                script {
                    def changedServices = env.CHANGED_SERVICES.split(",")
                    changedServices.each { service ->
                        sh """
                        echo "Building ${service}..."
                        cd ${service}
                        chmod +x ./gradlew
                        ./gradlew clean build -x test
                        ls -al ./build/libs
                        cd ..
                        """
                    }
                }
            }
        }

        //-----------------------------------------------
//         stage('Build Docker Image & Push to AWS ECR') {
//             steps {
//                  script {
//                     // withAWS를 통해 리전과 계정의 access, secret 키를 가져옴.
//                     withAWS(region: "${REGION}", credentials: "aws-key") {
//                         def serviceDirs = env.SERVICE_DIRS.split(",")
//                         serviceDirs.each { service ->
//                             // AWS에 접속해서 ECR을 사용해야 하는데, 젠킨스에는 aws-cli를 설치하지 않았어요.
//                             // aws-cli 없이도 접속을 할 수 있게 도와주는 라이브러리 설치.
//                             // helper가 여러분들 대신 aws에 로그인을 진행. 그리고 그 인증 정보를 json으로 만들어서
//                             // docker에게 세팅해 줍니다. -> docker가 ECR에 push가 가능해짐.
//                             sh """
//                                 curl -O https://amazon-ecr-credential-helper-releases.s3.us-east-2.amazonaws.com/0.4.0/linux-amd64/${ecrLoginHelper}
//                                 chmod +x ${ecrLoginHelper}
//                                 mv ${ecrLoginHelper} /usr/local/bin/
//
//                                 echo '{"credHelpers": {"${ECR_URL}": "ecr-login"}}' > ~/.docker/config.json
//
//                                 # Docker 이미지 빌드(서비스 이름으로)
//                                 docker build -t ${service}:latest ${service}
//
//                                 # ECR 레포지토리로 태깅
//                                 docker tag ${service}:latest ${ECR_URL}/${service}:latest
//
//                                 # ECR 로 푸시
//                                 docker push ${ECR_URL}/${service}:latest
//                             """
//                         }
//                     }
//                 }
//             }
//         }
        stage('Build Docker Image & Push to AWS ECR') {
           when {
               expression { env.CHANGED_SERVICES != "" } // 변경된 서비스가 있을 때만 실행
           }
            steps {
                script {
                    withAWS(region: "${REGION}", credentials: "aws-key") {
                        def changedServices = env.CHANGED_SERVICES.split(",")
                        changedServices.each { service ->
                            sh """
                            curl -O https://amazon-ecr-credential-helper-releases.s3.us-east-2.amazonaws.com/0.4.0/linux-amd64/${ecrLoginHelper}
                            chmod +x ${ecrLoginHelper}
                            mv ${ecrLoginHelper} /usr/local/bin/

                            echo '{"credHelpers": {"${ECR_URL}": "ecr-login"}}' > ~/.docker/config.json

                            docker build -t ${service}:latest ${service}
                            docker tag ${service}:latest ${ECR_URL}/${service}:latest
                            docker push ${ECR_URL}/${service}:latest
                            """
                        }
                    }
                }
            }
        }
        //-------------------------------------------------------------------------
//         stage('Deploy to AWS EC2 VM') {
//             steps {
//                 sshagent(credentials: ["jenkins-ssh-key"]) {
//                     //
//                     sh """
//                     # Jenkins에서 배포 서버로 docker-compose.yml 복사
//                     scp -o StrictHostKeyChecking=no docker-compose.yml ubuntu@${deployHost}:/home/ubuntu/docker-compose.yml
//
//                     ssh -o StrictHostKeyChecking=no ubuntu@${deployHost} '
//
//                     # Docker compose 파일이 있는 경로로 이동
//                     cd /home/ubuntu && \
//
//                     # 기존 컨테이너 중지 및 제거
//                     docker-compose down && \
//
//                     aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL} && \
//
//                     # Docker Compose로 컨테이너 재배포
//                     docker-compose pull && \
//                     docker-compose up -d
//                     '
//                     """
//                 }
//             }
//         }
        stage('Deploy Changed Services to AWS EC2 VM') {
            when {
                expression { env.CHANGED_SERVICES != "" } // 변경된 서비스가 있을 때만 실행
            }

            steps {
                sshagent(credentials: ["jenkins-ssh-key"]) {
                    sh """
                    # Jenkins에서 배포 서버로 docker-compose.yml 복사
                    scp -o StrictHostKeyChecking=no docker-compose.yml ubuntu@${deployHost}:/home/ubuntu/docker-compose.yml

                    ssh -o StrictHostKeyChecking=no ubuntu@${deployHost} '
                    cd /home/ubuntu && \

                    # 기존 컨테이너 중지 및 변경된 컨테이너만 업데이트
                    aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ECR_URL} && \

                    docker-compose pull ${env.CHANGED_SERVICES} && \
                    docker-compose up -d ${env.CHANGED_SERVICES}
                    '
                    """
                }
            }
        }
        //----------------------------------------------------------------
    }
}