package org.jenkinsci.plugins.coordinator.test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
//import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.jenkinsci.plugins.coordinator.model.CoordinatorBuild;
import org.jenkinsci.plugins.coordinator.model.CoordinatorProject;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import jenkins.model.Jenkins;

public class JobSequenceTest {
	// It had better to renew a JenkinsRule everytime for a clean setup 
	// for tmp folders clean up plz refer to 
	// https://wiki.jenkins-ci.org/display/JENKINS/Unit+Test+on+Windows#UnitTestonWindows-temporarydirectorygrowsup
	@Rule
	public JenkinsRule r = new JenkinsRuleX(){
        
        @Override
        public void before() throws Throwable {
            super.before();
            // setup jenkins for test
 			String[] twoSecNames = {"12_L_2s", "21_L_2s", "211_L_2s", "213_L_2s", "23_L_2s", "3_L_2s"};
 			String[] fourSecNames = {"11_L_4s", "212_L_4s"};
 			String[] failureNames = {"211_L_Failure", "21_L_Failure"};

 			prepareSleepJobs(twoSecNames, 2000L);
 			prepareSleepJobs(fourSecNames, 4000L);
 			prepareFailureJobs(failureNames);
        }
	};

	private FreeStyleBuild retrieveFreeStyleProjectLastBuild(String projectName) {
		return ((FreeStyleProject)r.jenkins.getItem(projectName)).getLastBuild();
	}
	
	@LocalData
	@Test
	public void happy() throws Exception {
//		  Root
//		  |-- 1_P
//		  |   |-- 11_L_4s
//		  |   |__ 12_L_2s
//		  |-- 2_S
//		  |   |-- 21_L_2s
//		  |   |-- 22_P
//		  |   |   |-- 211_L_2s
//		  |   |   |-- 212_L_4s
//		  |   |   |__ 213_L_2s
//		  |   |__ 23_L_2s
//		  |__ 3_L_2s
//		  
//		  starttime_21_L_2s>endtime_11_L_4s && starttime_21_L_2s>endtime_12_L_2s for parallel run checking
//		  starttime_3_L_2s_starttime>endtime_23_L_2s for serial run checking

		Jenkins jenkins = r.getInstance();
		assertEquals("Num of Executors should be 6", 6, jenkins.getNumExecutors());
		CoordinatorProject coordinatorProject = (CoordinatorProject) jenkins.getItem("test");
		QueueTaskFuture<CoordinatorBuild> future = coordinatorProject.scheduleBuild2(0);
		CoordinatorBuild build = future.get(60, TimeUnit.SECONDS);
		
		StringBuilder reason = new StringBuilder("Coordinator build should not be a failure.\n");
		for(String log: build.getLog(1000)){
			reason.append(log).append("\n");
		}
		assertEquals(reason.toString(), Result.SUCCESS, build.getResult());
		
		FreeStyleBuild build_21_L_2s = retrieveFreeStyleProjectLastBuild("21_L_2s");
		FreeStyleBuild build_11_L_4s = retrieveFreeStyleProjectLastBuild("11_L_4s");
		FreeStyleBuild build_12_L_2s = retrieveFreeStyleProjectLastBuild("12_L_2s");
		FreeStyleBuild build_3_L_2s = retrieveFreeStyleProjectLastBuild("3_L_2s");
		FreeStyleBuild build_23_L_2s = retrieveFreeStyleProjectLastBuild("23_L_2s");
		
		long starttime_21_L_2s = build_21_L_2s.getStartTimeInMillis();
		long endtime_11_L_4s = build_11_L_4s.getStartTimeInMillis() + build_11_L_4s.getDuration();
		long endtime_12_L_2s = build_12_L_2s.getStartTimeInMillis() + build_12_L_2s.getDuration();
		long starttime_3_L_2s = build_3_L_2s.getStartTimeInMillis();
		long endtime_23_L_2s = build_23_L_2s.getStartTimeInMillis() + build_23_L_2s.getDuration();
		
		assertThat("parallel run 11_L_4s should take longer than 12_L_2s to finish", endtime_11_L_4s, greaterThan(endtime_12_L_2s));

		assertThat("starttime_21_L_2s>endtime_11_L_4s && starttime_21_L_2s>endtime_12_L_2s for parallel run checking", 
				starttime_21_L_2s, is( allOf( 
											greaterThan(endtime_11_L_4s), 
											greaterThan(endtime_12_L_2s)
											)
										)
				);
		
		assertThat("starttime_3_L_2s_starttime>endtime_23_L_2s for serial run checking", 
				starttime_3_L_2s,  greaterThan(endtime_23_L_2s)
				);
	}
	
	@LocalData
	@Test 
	public void serialConfiguredJobsAfterFailureVerification() throws Exception {
//		  Root
//		  |-- 1_P
//		  |   |-- 11_L_4s
//		  |   |__ 12_L_2s
//		  |-- 2_S
//		  |   |-- 21_L_Failure
//		  |   |-- 22_P
//		  |   |   |-- 211_L_2s
//		  |   |   |-- 212_L_4s
//		  |   |   |__ 213_L_2s
//		  |   |__ 23_L_2s
//		  |__ 3_L_2s
//		  
//		  211_L_2s, 212_L_4s, 213_L_2s, 23_L_2s, 3_L_2s are never triggered ( AbstractProject.createExecutable() )
		Jenkins jenkins = r.getInstance();
		CoordinatorProject coordinatorProject = (CoordinatorProject) jenkins.getItem("test");
		QueueTaskFuture<CoordinatorBuild> future = coordinatorProject.scheduleBuild2(0);
		CoordinatorBuild build = future.get(60, TimeUnit.SECONDS);

		assertEquals("Coordinator build should be failure.", Result.FAILURE, build.getResult());
		
		String[] notTriggeredProjectNames = {"211_L_2s", "212_L_4s", "213_L_2s", "23_L_2s", "3_L_2s"};
		StringBuilder reason = new StringBuilder("All these projects should not be triggered.\n");
		ArrayList<FreeStyleBuild> builds = new ArrayList<FreeStyleBuild>();
		for(String projectName: notTriggeredProjectNames){
			builds.add(retrieveFreeStyleProjectLastBuild(projectName));
			reason.append(projectName).append(" ");
		}
		assertThat(reason.toString(), builds, everyItem( nullValue(FreeStyleBuild.class) ) );
	}
	
	@LocalData
	@Test 
	public void parallelConfiguredJobsAfterFailureVerification() throws Exception {
//		  Root
//		  |-- 1_P
//		  |   |-- 11_L_4s
//		  |   |__ 12_L_2s
//		  |-- 2_S
//		  |   |-- 21_L_2s
//		  |   |-- 22_P
//		  |   |   |-- 211_L_Failure
//		  |   |   |-- 212_L_4s
//		  |   |   |__ 213_L_2s
//		  |   |__ 23_L_2s
//		  |__ 3_L_2s
//		  
//		  212_L_4s, 213_L_2s are triggered ( AbstractProject.createExecutable() )
//		  23_L_2s, 3_L_2s never triggered ( AbstractProject.createExecutable() )
		
		Jenkins jenkins = r.getInstance();
		CoordinatorProject coordinatorProject = (CoordinatorProject) jenkins.getItem("test");
		QueueTaskFuture<CoordinatorBuild> future = coordinatorProject.scheduleBuild2(0);
		CoordinatorBuild build = future.get(60, TimeUnit.SECONDS);

		assertEquals("Coordinator build should be failure.", Result.FAILURE, build.getResult());
		
		String[] triggeredProjectNames = {"212_L_4s", "213_L_2s"};
		StringBuilder reason = new StringBuilder("All this project should be triggered.\n");
		ArrayList<FreeStyleBuild> builds = new ArrayList<FreeStyleBuild>();
		for(String projectName: triggeredProjectNames){
			builds.add(retrieveFreeStyleProjectLastBuild(projectName));
			reason.append(projectName).append(" ");
		}
		assertThat(reason.toString(), builds, everyItem( notNullValue(FreeStyleBuild.class) ) );
		
		String[] notTriggeredProjectNames = {"23_L_2s", "3_L_2s"};
		reason = new StringBuilder("All this project should not be triggered.\n");
		builds = new ArrayList<FreeStyleBuild>();
		for(String projectName: notTriggeredProjectNames){
			builds.add(retrieveFreeStyleProjectLastBuild(projectName));
			reason.append(projectName).append(" ");
		}
		assertThat(reason.toString(), builds, everyItem( nullValue(FreeStyleBuild.class) ) );
	}
}