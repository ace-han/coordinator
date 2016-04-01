package org.jenkinsci.plugins.coordinator.test;

import java.io.File;
import java.net.URLConnection;

import org.jvnet.hudson.test.JenkinsRule;

import hudson.Functions;

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
}