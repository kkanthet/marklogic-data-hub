@Library('shared-libraries') _
def gitDataHubRepo="https://github.com/SameeraPriyathamTadikonda/marklogic-data-hub.git"
def JAVA_HOME="~/java/jdk1.8.0_72"
def GRADLE_USER_HOME="/.gradle"
def MAVEN_HOME="/usr/local/maven"
pipeline{
	agent none;
	options {
  	checkoutToSubdirectory 'data-hub'
	}
	stages{
		stage('Build-datahub'){
		agent { label 'dhfLinuxAgent'}
			steps{
				script{
				if(env.CHANGE_TITLE){
				def transitionInput =[transition: [id: '41']]
				jiraTransitionIssue idOrKey: env.CHANGE_TITLE, input: transitionInput, site: 'JIRA'
				}
				}
				println(BRANCH_NAME)
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew build -x test -Pskipui=true;'
				archiveArtifacts artifacts: 'data-hub/marklogic-data-hub/build/libs/* , data-hub/ml-data-hub-plugin/build/libs/* , data-hub/quick-start/build/libs/', onlyIfSuccessful: true			}
		}
		stage('Unit-Tests'){
		agent { label 'dhfLinuxAgent'}
			steps{
				copyRPM 'Latest'
				setUpML '$WORKSPACE/xdmp/src/Mark*.rpm'
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew clean;./gradlew :marklogic-data-hub:test --tests com.marklogic.hub.flow.* -Pskipui=true'
				junit '**/TEST-*.xml'
				script{
				if(env.CHANGE_TITLE){
				jiraAddComment comment: 'Jenkins Unit Test Results For PR Available', idOrKey: env.CHANGE_TITLE, site: 'JIRA'
				}
				}
			}
			post{
                  success {
                    println("Unit Tests Completed")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Unit Tests for PR $CHANGE_ID Passed'
                   }
                   failure {
                      println("Unit Tests Failed")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Unit Tests for PR $CHANGE_ID Failed'
                  }
                  }
		}
		stage('code-review'){
		when {
  			 allOf {
    changeRequest author: '', authorDisplayName: '', authorEmail: '', branch: '', fork: '', id: '', target: '', title: '', url: ''
  }
  			beforeAgent true
		}
		agent none;
		steps{
		sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Waiting for code review PR $CHANGE_ID '
			script{
			try{
			 timeout(time:5, unit:'MINUTES') {
            input message:'Review Done?'
        }
        }catch(err){
        currentBuild.result = "SUCCESS"
        }
        }
		}
		}
		stage('PR'){
		when {
  			changeRequest()
  			beforeAgent true
		}
		agent {label 'master'};
			steps{
			retry(5){
				withCredentials([usernameColonPassword(credentialsId: 'a0ec09aa-f339-44de-87c4-1a4936df44f5', variable: 'Credentials')]) {
				script{
				
    				def response = sh (returnStdout: true, script:'''curl -u $Credentials  --header "application/vnd.github.merge-info-preview+json" "https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/$CHANGE_ID" | grep '"mergeable_state":' | cut -d ':' -f2 | cut -d ',' -f1 | tr -d '"' ''')
    				response=response.trim();
    				println(response)
    				if(response.equals("clean")){
    					println("merging can be done")
    					sh "curl -o - -s -w \"\n%{http_code}\n\" -X PUT -d '{\"commit_title\": \"Merge pull request\"}' -u $Credentials  https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/$CHANGE_ID/merge | tail -1 > mergeResult.txt"
    					def mergeResult = readFile('mergeResult.txt').trim()
    					if(mergeResult==200){
    						println("Merge successful")
    					}else{
    						println("Merge Failed")
    					}
    				}else if(response.equals("blocked")){
    					println("retry blocked");
    					sleep time: 1, unit: 'MINUTES'
    					throw new Exception("Waiting for all the status checks to pass");
    				}else if(response.equals("unstable")){
    					println("retry unstable")
    					sh "curl -o - -s -w \"\n%{http_code}\n\" -X PUT -d '{\"commit_title\": \"Merge pull request\"}' -u $Credentials  https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/$CHANGE_ID/merge | tail -1 > mergeResult.txt"
    					def mergeResult = readFile('mergeResult.txt').trim()
    					println("Result is"+ mergeResult)
    				}else{
    					println("merging not possible")
    					currentBuild.result = "FAILURE"
    					sh 'exit 1';
    				}
				}
				}
				}
			}
			post{
                  success {
                    println("Merge Successful")
                    script{
                    if(env.CHANGE_TITLE){
						def transitionInput =[transition: [id: '31']]
						jiraTransitionIssue idOrKey: env.CHANGE_TITLE, input: transitionInput, site: 'JIRA'
					}
					sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,' PR $CHANGE_ID is Merged'
					}
                   }
                   failure {
                      println("Retried 5times")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'PR $CHANGE_ID Cannot be Merged'
                  }
                  }
		}
		stage('Integration Tests'){
			agent { label 'dhfLinuxAgent'}
			steps{
				copyRPM 'Latest'
				setUpML '$WORKSPACE/xdmp/src/Mark*.rpm'
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew clean;./gradlew :marklogic-data-hub:test --tests com.marklogic.hub.flow.* -Pskipui=true'
				junit '**/TEST-*.xml'
				script{
				if(env.CHANGE_TITLE){
				jiraAddComment comment: 'Jenkins End-End Unit Test Results For PR Available', idOrKey: env.CHANGE_TITLE, site: 'JIRA'
				}
				}
			}
			post{
                  success {
                    println("End-End Tests Completed")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'End-End Tests for PR $CHANGE_ID Passed'
                   }
                   failure {
                      println("End-End Tests Failed")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'End-End Tests for PR $CHANGE_ID Failed'
                  }
                  }
		}
		stage('Create PR For Integration Branch'){
		when {
  			changeRequest target: 'FeatureBranch'
  			beforeAgent true
		}
		agent {label 'master'}
		steps{
		withCredentials([usernameColonPassword(credentialsId: 'a0ec09aa-f339-44de-87c4-1a4936df44f5', variable: 'Credentials')]) {
		script{
			sh "curl -u $Credentials  -X POST -H 'Content-Type:application/json' -d '{\"title\": \"${CHANGE_TITLE} Automated PR for Integration Branch\" , \"head\": \"FeatureBranch\" , \"base\": \"IntegrationBranch\" }' https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls"
			}
			}
		}
		post{
                  success {
                    println("Automated PR For Integration branch created")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Integration Tests for PR $CHANGE_ID Passed'
                   }
                   failure {
                      println("Creation of Automated PR Failed")
                     
                  }
                  }
		}
		stage('Upgrade Tests'){
			agent { label 'dhfLinuxAgent'}
			steps{
				copyRPM 'Latest'
				setUpML '$WORKSPACE/xdmp/src/Mark*.rpm'
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew clean;./gradlew :marklogic-data-hub:test --tests com.marklogic.hub.flow.* -Pskipui=true'
				junit '**/TEST-*.xml'
				script{
				if(env.CHANGE_TITLE){
				jiraAddComment comment: 'Jenkins Upgrade Test Results For PR Available', idOrKey: env.CHANGE_TITLE, site: 'JIRA'
				}
				}
			}
			post{
                  success {
                    println("Upgrade Tests Completed")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'End-End Tests for PR $CHANGE_ID Passed'
                   }
                   failure {
                      println("Upgrade Tests Failed")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'End-End Tests for PR $CHANGE_ID Failed'
                  }
                  }
		}
		stage('Create PR For Release Branch'){
		when {
  			  changeRequest comparator: 'REGEXP', target: 'Integration*'
  			beforeAgent true
		}
		agent {label 'master'}
		steps{
		withCredentials([usernameColonPassword(credentialsId: 'a0ec09aa-f339-44de-87c4-1a4936df44f5', variable: 'Credentials')]) {
		script{
			sh "curl -u $Credentials  -X POST -H 'Content-Type:application/json' -d '{\"title\": \"${CHANGE_TITLE} Automated PR for Release Branch\" , \"head\": \"IntegrationBranch\" , \"base\": \"ReleaseBranch\" }' https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls"
			}
			}
		}
		post{
                  success {
                    println("Automated PR For Release branch created")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Integration Tests for PR $CHANGE_ID Passed'
                   }
                   failure {
                      println("Creation of Automated PR Failed")
                  }
                  }
		}
	}
}