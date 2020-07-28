pipeline {
  agent any
  stages {
    stage('Minify') {
      steps {
        nodejs(nodeJSInstallationName: 'Node 14.x') {
            sh 'npm install'
            sh 'npm ci'
            sh './minify'
        }
      }
    }
    stage('Build Jars') {
      steps {
        withSonarQubeEnv('SonarQube') {
           sh 'mvn clean install pmd:pmd checkstyle:checkstyle spotbugs:spotbugs sonar:sonar'
        }
      }
    }
  }
}