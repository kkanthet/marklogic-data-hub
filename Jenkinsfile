@Library('shared-libraries') _
import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic
def gitDataHubRepo="https://github.com/SameeraPriyathamTadikonda/marklogic-data-hub.git"
def JAVA_HOME="~/java/jdk1.8.0_72"
def GRADLE_USER_HOME="/.gradle"
def MAVEN_HOME="/usr/local/maven"
def JIRA_ID="";
def commitMessage="";
def prResponse="";
def prNumber;
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
				sh 'printenv'
				if(env.CHANGE_TITLE){
				JIRA_ID=env.CHANGE_TITLE.split(':')[0];
				def transitionInput =[transition: [id: '41']]
				jiraTransitionIssue idOrKey: JIRA_ID, input: transitionInput, site: 'JIRA'
				}
				}
				println(BRANCH_NAME)
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+' export '+MAVEN_HOME+' export PATH=$WORKSPACE/data-hub'+GRADLE_USER_HOME+':$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew build -x test -Pskipui=true;'
				archiveArtifacts artifacts: 'data-hub/marklogic-data-hub/build/libs/* , data-hub/ml-data-hub-plugin/build/libs/* , data-hub/quick-start/build/libs/', onlyIfSuccessful: true			}
		}
		stage('Unit-Tests'){
		agent { label 'dhfLinuxAgent'}
			steps{
				copyRPM 'Latest'
				//setUpML '$WORKSPACE/xdmp/src/Mark*.rpm'
				setUpML '/space/Mark*.rpm'
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$WORKSPACE/data-hub'+GRADLE_USER_HOME+':$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew clean;./gradlew :marklogic-data-hub:test --tests com.marklogic.hub.processes.ProcessManagerTest -Pskipui=true'
				junit '**/TEST-*.xml'
				script{
				if(env.CHANGE_TITLE){
				JIRA_ID=env.CHANGE_TITLE.split(':')[0]
				jiraAddComment comment: 'Jenkins Unit Test Results For PR Available', idOrKey: JIRA_ID, site: 'JIRA'
				}
				}
			}
			post{
                  success {
                    println("Unit Tests Completed")
                    script{
                    println("author"+ env.CHANGE_AUTHOR.toString().trim().toLowerCase())
                    def email=getEmailFromGITUser '$CHANGE_AUTHOR.toString().trim().toLowerCase()' 
                    println(email)
                    sendMail email,'Check: ${BUILD_URL}/console',false,'Unit Tests for  $BRANCH_NAME Passed'
                    }
                   }
                   failure {
                      println("Unit Tests Failed")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Unit Tests for $BRANCH_NAME Failed'
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
		agent {label 'master'};
		steps{
		script{
			if(env.CHANGE_TITLE.split(':')[1].contains("Automated PR")){
				println("Automated PR")
				sh 'exit 0'
			}else{
		
		sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Waiting for code review $BRANCH_NAME '
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
					JIRA_ID=env.CHANGE_TITLE.split(':')[0]
    				def response = sh (returnStdout: true, script:'''curl -u $Credentials  --header "application/vnd.github.merge-info-preview+json" "https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/$CHANGE_ID" | grep '"mergeable_state":' | cut -d ':' -f2 | cut -d ',' -f1 | tr -d '"' ''')
    				response=response.trim();
    				println(response)
    				if(response.equals("clean")){
    					println("merging can be done")
    					sh "curl -o - -s -w \"\n%{http_code}\n\" -X PUT -d '{\"commit_title\": \"$JIRA_ID: merging PR\"}' -u $Credentials  https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/$CHANGE_ID/merge | tail -1 > mergeResult.txt"
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
    					sh "curl -o - -s -w \"\n%{http_code}\n\" -X PUT -d '{\"commit_title\": \"$JIRA_ID: merging PR\"}' -u $Credentials  https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/$CHANGE_ID/merge | tail -1 > mergeResult.txt"
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
					sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'  $BRANCH_NAME is Merged'
					}
                   }
                   failure {
                      println("Retried 5times")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,' $BRANCH_NAME Cannot be Merged'
                  }
                  }
		}
		stage('E2E Single Tests'){
		when {
  			branch 'FeatureBranch'
			}
			agent { label 'dhfLinuxAgent'}
			steps{
				copyRPM 'Latest'
				//setUpML '$WORKSPACE/xdmp/src/Mark*.rpm'
				setUpML '/space/Mark*.rpm'
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$WORKSPACE/data-hub'+GRADLE_USER_HOME+':$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew clean;./gradlew :marklogic-data-hub:test --tests com.marklogic.hub.processes.ProcessManagerTest -Pskipui=true'
				junit '**/TEST-*.xml'
				script{
				 commitMessage = sh (returnStdout: true, script:'''
			curl -u $Credentials -X GET "https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/git/commits/${GIT_COMMIT}" ''')
			def slurper = new JsonSlurperClassic().parseText(commitMessage.toString().trim())
				def commit=slurper.message.toString().trim();
				JIRA_ID=commit.split(("\\n"))[0].split(':')[0].trim();
				JIRA_ID=JIRA_ID.split(" ")[0];
				commitMessage=null;
				jiraAddComment comment: 'Jenkins End-End Unit Test Results For PR Available', idOrKey: JIRA_ID, site: 'JIRA'
				}
			}
			post{
                  success {
                    println("End-End Tests Completed")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'End-End Tests for $BRANCH_NAME Passed'
                    
                   }
                   failure {
                      println("End-End Tests Failed")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'End-End Tests for  $BRANCH_NAME Failed'
                  }
                  }
		}
		stage('Merge PR to Integration Branch'){
		when {
  			branch 'FeatureBranch'
  			beforeAgent true
		}
		agent {label 'master'}
		steps{
		withCredentials([usernameColonPassword(credentialsId: 'a0ec09aa-f339-44de-87c4-1a4936df44f5', variable: 'Credentials')]) {
		script{
			//JIRA_ID=env.CHANGE_TITLE.split(':')[0]
			prResponse = sh (returnStdout: true, script:'''
			curl -u $Credentials  -X POST -H 'Content-Type:application/json' -d '{\"title\": \"Automated PR for Integration Branch\" , \"head\": \"FeatureBranch\" , \"base\": \"IntegrationBranch\" }' https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls ''')
			println(prResponse)
			def slurper = new JsonSlurper().parseText(prResponse)
			println(slurper.number)
			prNumber=slurper.number;
			}
			}
			withCredentials([usernameColonPassword(credentialsId: 'rahul-git', variable: 'Credentials')]) {
                    sh "curl -u $Credentials  -X POST  -d '{\"event\": \"APPROVE\"}' https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/${prNumber}/reviews"
                }
             withCredentials([usernameColonPassword(credentialsId: 'a0ec09aa-f339-44de-87c4-1a4936df44f5', variable: 'Credentials')]) {
             script{
             sh "curl -o - -s -w \"\n%{http_code}\n\" -X PUT -d '{\"commit_title\": \"$JIRA_ID: Merge pull request\"}' -u $Credentials  https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/${prNumber}/merge | tail -1 > mergeResult.txt"
    					def mergeResult = readFile('mergeResult.txt').trim()
    					if(mergeResult==200){
    						println("Merge successful")
    					}else{
    						println("Merge Failed")
    					}
    			}
             }

		}
		post{
                  success {
                    println("Automated PR For Integration branch Completed")
                   }
                   failure {
                      println("Creation of Automated PR Failed")
                     
                  }
                  }
		}
		stage('Parallel Execution'){
		parallel{
		stage('Upgrade Tests'){
		when {
  			branch 'IntegrationBranch'
  			beforeAgent true
		}
			agent { label 'dhfLinuxAgent'}
			steps{ 
				copyRPM 'Latest'
				//setUpML '$WORKSPACE/xdmp/src/Mark*.rpm'
				setUpML '/space/Mark*.rpm'
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$WORKSPACE/data-hub'+GRADLE_USER_HOME+':$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew clean;./gradlew :marklogic-data-hub:test --tests com.marklogic.hub.processes.ProcessManagerTest -Pskipui=true'
				junit '**/TEST-*.xml'
					script{
				 commitMessage = sh (returnStdout: true, script:'''
			curl -u $Credentials -X GET "https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/git/commits/${GIT_COMMIT}" ''')
			def slurper = new JsonSlurperClassic().parseText(commitMessage.toString().trim())
				def commit=slurper.message.toString().trim();
				JIRA_ID=commit.split(("\\n"))[0].split(':')[0].trim();
				JIRA_ID=JIRA_ID.split(" ")[0];
				commitMessage=null;
				jiraAddComment comment: 'Jenkins Upgrade Test Results For PR Available', idOrKey: JIRA_ID, site: 'JIRA'
				}
			}
			post{
                  success {
                    println("Upgrade Tests Completed")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Upgrade Tests for $BRANCH_NAME Passed'
                    // sh './gradlew publish'
                   }
                   failure {
                      println("Upgrade Tests Failed")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Upgrade Tests for $BRANCH_NAME Failed'
                  }
                  }
		}
		stage('End2End all Tests'){
		when {
  			branch 'IntegrationBranch'
  			beforeAgent true
		}
			agent { label 'dhfWindowsAgent'}
			steps{ 
				copyRPM 'Latest'
				//setUpML '$WORKSPACE/xdmp/src/Mark*.rpm'
				setUpML '/space/Mark*.rpm'
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$WORKSPACE/data-hub'+GRADLE_USER_HOME+':$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew clean;./gradlew :marklogic-data-hub:test --tests com.marklogic.hub.processes.ProcessManagerTest -Pskipui=true'
				junit '**/TEST-*.xml'
					script{
				 commitMessage = sh (returnStdout: true, script:'''
			curl -u $Credentials -X GET "https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/git/commits/${GIT_COMMIT}" ''')
			def slurper = new JsonSlurperClassic().parseText(commitMessage.toString().trim())
				def commit=slurper.message.toString().trim();
				JIRA_ID=commit.split(("\\n"))[0].split(':')[0].trim();
				JIRA_ID=JIRA_ID.split(" ")[0];
				commitMessage=null;
				jiraAddComment comment: 'Jenkins End-End All Test Results For PR Available', idOrKey: JIRA_ID, site: 'JIRA'
				}
			}
			post{
                  success {
                    println("Upgrade Tests Completed")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'End-End all Tests for $BRANCH_NAME Passed'
                    // sh './gradlew publish'
                   }
                   failure {
                      println("Upgrade Tests Failed")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'End-End All Tests for $BRANCH_NAME Failed'
                  }
                  }
		}
		}
		}
		stage('Merge PR to Release Branch'){
		when {
  			branch 'IntegrationBranch'
  			beforeAgent true
		}
		agent {label 'master'}
		steps{
		withCredentials([usernameColonPassword(credentialsId: 'a0ec09aa-f339-44de-87c4-1a4936df44f5', variable: 'Credentials')]) {
		script{
			//JIRA_ID=env.CHANGE_TITLE.split(':')[0]
			prResponse = sh (returnStdout: true, script:'''
			curl -u $Credentials  -X POST -H 'Content-Type:application/json' -d '{\"title\": \"Automated PR for Integration Branch\" , \"head\": \"IntegrationBranch\" , \"base\": \"ReleaseBranch\" }' https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls ''')
			println(prResponse)
			def slurper = new JsonSlurper().parseText(prResponse)
			println(slurper.number)
			prNumber=slurper.number;
			}
			}
			withCredentials([usernameColonPassword(credentialsId: 'rahul-git', variable: 'Credentials')]) {
                    sh "curl -u $Credentials  -X POST  -d '{\"event\": \"APPROVE\"}' https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/${prNumber}/reviews"
                }
                withCredentials([usernameColonPassword(credentialsId: 'a0ec09aa-f339-44de-87c4-1a4936df44f5', variable: 'Credentials')]) {
              script{
             sh "curl -o - -s -w \"\n%{http_code}\n\" -X PUT -d '{\"commit_title\": \"$JIRA_ID: Merge pull request\"}' -u $Credentials  https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/pulls/${prNumber}/merge | tail -1 > mergeResult.txt"
    					def mergeResult = readFile('mergeResult.txt').trim()
    					if(mergeResult==200){
    						println("Merge successful")
    					}else{
    						println("Merge Failed")
    					}
    			}
             }

		}
		post{
                  success {
                    println("Automated PR For Release branch created")
           
                   }
                   failure {
                      println("Creation of Automated PR Failed")
                  }
                  }
		}
		stage('Sanity Tests'){
			when {
  			branch 'ReleaseBranch'
  			beforeAgent true
		}
			agent { label 'dhfLinuxAgent'}
			steps{
				copyRPM 'Latest'
				//setUpML '$WORKSPACE/xdmp/src/Mark*.rpm'
				setUpML '/space/Mark*.rpm'
				sh 'echo '+JAVA_HOME+'export '+JAVA_HOME+' export $WORKSPACE/data-hub'+GRADLE_USER_HOME+'export '+MAVEN_HOME+'export PATH=$WORKSPACE/data-hub'+GRADLE_USER_HOME+':$PATH:$MAVEN_HOME/bin; cd $WORKSPACE/data-hub;rm -rf $GRADLE_USER_HOME/caches;./gradlew clean;./gradlew clean;./gradlew :marklogic-data-hub:test --tests com.marklogic.hub.processes.ProcessManagerTest -Pskipui=true'
				junit '**/TEST-*.xml'
				script{
				 commitMessage = sh (returnStdout: true, script:'''
			curl -u $Credentials -X GET "https://api.github.com/repos/SameeraPriyathamTadikonda/marklogic-data-hub/git/commits/${GIT_COMMIT}" ''')
			def slurper = new JsonSlurperClassic().parseText(commitMessage.toString().trim())
				def commit=slurper.message.toString().trim();
				JIRA_ID=commit.split(("\\n"))[0].split(':')[0].trim();
				JIRA_ID=JIRA_ID.split(" ")[0];
				commitMessage=null;
				jiraAddComment comment: 'Jenkins Sanity Test Results For PR Available', idOrKey: JIRA_ID, site: 'JIRA'
				}
			}
			post{
                  success {
                    println("Sanity Tests Completed")
                    sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Sanity Tests for $BRANCH_NAME Passed'
                    script{
						def transitionInput =[transition: [id: '31']]
						//JIRA_ID=env.CHANGE_TITLE.split(':')[0]
						jiraTransitionIssue idOrKey: JIRA_ID, input: transitionInput, site: 'JIRA'
						}
                    sendMail 'stadikon@marklogic.com','Click approve to release',false,'Datahub is ready for Release'

                   }
                   failure {
                      println("Sanity Tests Failed")
                      sendMail 'stadikon@marklogic.com','Check: ${BUILD_URL}/console',false,'Sanity Tests for $BRANCH_NAME Failed'
                  }
                  }
		}
	}
}