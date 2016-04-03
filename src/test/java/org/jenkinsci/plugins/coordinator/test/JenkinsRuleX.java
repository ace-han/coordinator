package org.jenkinsci.plugins.coordinator.test;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.jvnet.hudson.test.FailureBuilder;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.SleepBuilder;

import hudson.Functions;
import hudson.model.FreeStyleProject;

/**
 * Please refer to 
 * https://wiki.jenkins-ci.org/display/JENKINS/Unit+Test+on+Windows#UnitTestonWindows-temporarydirectorygrowsup
 * @author Ace
 *
 */
public class JenkinsRuleX extends JenkinsRule {
	private boolean origDefaultUseCache = true;
    
    @Override
    public void before() throws Throwable {
        if(Functions.isWindows()) {
            // To avoid JENKINS-4409.
            // URLConnection caches handles to jar files by default,
            // and it prevents delete temporary directories.
            // Disable caching here.
            // Though defaultUseCache is a static field,
            // its setter and getter are provided as instance methods.
            URLConnection aConnection = new File(".").toURI().toURL().openConnection();
            origDefaultUseCache = aConnection.getDefaultUseCaches();
            aConnection.setDefaultUseCaches(false);
        }
        super.before();
    }
    
    @Override
    public void after() throws Exception {
        super.after();
        if(Functions.isWindows()) {
            URLConnection aConnection = new File(".").toURI().toURL().openConnection();
            aConnection.setDefaultUseCaches(origDefaultUseCache);
        }
    }
    
    
    protected List<FreeStyleProject> prepareSleepJobs(String[] jobNames, long milliseconds) throws Throwable  {
		List<FreeStyleProject> result = new ArrayList<FreeStyleProject>();
//		Jenkins jenkins = this.getInstance();
		for(String name: jobNames){
			FreeStyleProject job = this.createFreeStyleProject(name);
			job.getBuildersList().replace(new SleepBuilder(milliseconds));
//			job = spy(job);
//			jenkins.putItem(job);
			result.add(job);
		}
		return result;
	}

	protected List<FreeStyleProject> prepareFailureJobs(String[] jobNames) throws Throwable  {
		List<FreeStyleProject> result = new ArrayList<FreeStyleProject>();
//		Jenkins jenkins = this.getInstance();
		for(String name: jobNames){
			FreeStyleProject job = this.createFreeStyleProject(name);
			job.getBuildersList().replace(new FailureBuilder());
//			job = spy(job);
//			jenkins.putItem(job);
			result.add(job);
		}
		return result;
	}
}