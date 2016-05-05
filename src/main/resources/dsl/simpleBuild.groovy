package dsl

// See https://github.com/jenkinsci/workflow-plugin/tree/master/cps-global-lib#defining-global-functions

/* sample with all the things turned on:
<code>
simpleBuild {

    enviroment = "DEV"
    version = ""
}
</code>

*/


// The call(body) method in any file in workflowLibs.git/vars is exposed as a
// method with the same name as the file.
def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    /** Run the build scripts */

    try {
            runViaLabel(config)
    }
    catch (Exception rethrow) {
        failureDetail = failureDetail(rethrow)
        throw rethrow
    }
}

/** Execute the scripts on the appropriate label node */
def runViaLabel(config) {
    runScripts(config)
}

/** Run the before/script combination */
def runScripts(config) {
    envList = []
    for ( e in config.env ) {
        envList.add("${e.getKey()}=${e.getValue()}")
    }
    withEnv(envList) {

       if(config.enviroment == "DEV")
       {
       		deployToDev(config)
       }
       else if (config.environment == "QA")
       {
       		deployToQA(config)
       }
       else if (config.environment == "STAGING")
       {
       	    deployToStaging(config)
       }
       else if (config.environment == "PROD")
       {
       		deployToProd(config)
       }
    }
}

/** Deploy to dev **/
def deployToDev(config)
{
	node()
	{
		echo "Deploying to Dev"
		unstash 'war'
		//undeploy previous app 
		sh 'rm -rf /Users/cjp-docker-compose/data/environments/dev/*.war'
		sleep 5
		//deploy app
		sh 'cp target/petclinic.war /Users/cjp-docker-compose/data/environments/dev'
		sleep 5 //wait for initialization
		echo "Deployed to Dev"
	}
}

/** Deploy to QA **/
def deployToQA(config)
{
	stage "QA Approval"
	timeout(time: 10, unit: 'MINUTES')
	{
	   try
	   { 
	      input message: 'Deploy to QA?'
	   } 
	   catch(Exception e)
	   {
	      echo "No input provided, resuming build"
	   } 
	}

	node()
	{
		echo "Deploying to QA"
		sh 'rm -rf /Users/cjp-docker-compose/data/environments/qa/*.war'
		sleep 5
		sh 'cp target/petclinic.war /Users/cjp-docker-compose/data/environments/qa'
		sleep 5
		echo "Deployed to QA"
	}
}

/** Deploy to Staging **/
def deployToStaging(config)
{
	checkpoint "Deployed to QA"
	
	stage 'Staging Deploy'
	timeout(time: 60, unit: 'SECONDS')
	{
	   try
	   {
	    input message: "Deploy to Staging?"
	   } 
	   catch(Exception e)
	   {
	      echo "No input provided, resuming build"
	   } 
	}
	
	node ()
	{	
		echo "Deploying to Staging"
		sh 'rm -rf /Users/cjp-docker-compose/data/environments/staging/*.war'
		sleep 5
		sh 'cp target/petclinic.war /Users/cjp-docker-compose/data/environments/staging'
		sleep 5
		echo "Deployed to Staging"
	}
}

/** Deploy to Prod **/
def deployToProd(config)
{
	checkpoint "Deployed to Staging"
	stage 'Prod Deploy'
	timeout(time: 60, unit: 'SECONDS')
	{
	   input message: "Deploy to Prod?"
	}

	node()
	{		
		echo "Deploying to Prod"
		sh 'rm -rf /Users/cjp-docker-compose/data/environments/prod/*.war'
		sleep 5
		sh 'cp target/petclinic.war /Users/cjp-docker-compose/data/environments/prod'
		sleep 5
		echo "Deployed to Prod"
	}
}

/**
 * Read the detail from the exception to be used in the failure message
 * https://issues.jenkins-ci.org/browse/JENKINS-28119 will give better options.
 */
def failureDetail(exception) {
    /* not allowed to access StringWriter
    def w = new StringWriter()
    exception.printStackTrace(new PrintWriter(w))
    return w.toString();
    */
    return exception.toString()
}
