package org.jenkinsci.plugins.coordinator.model;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 
 * For project's builder part configuration
 * @author Ace Han
 *
 */
public class CoordinatorBuilder extends Builder {
	
	@DataBoundConstructor
	public CoordinatorBuilder(String executionPlanJsonString){
		setExecutionPlanJsonString(executionPlanJsonString);
	}
	
	private String executionPlanJsonString;
	
	public String getExecutionPlanJsonString() {
		return executionPlanJsonString;
	}

	public void setExecutionPlanJsonString(String executionPlanJsonString) {
		this.executionPlanJsonString = executionPlanJsonString;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			// Only available to CooradinatorProject
			return CoordinatorProject.class.isAssignableFrom(jobType);
		}
		
		@Override
		public String getDisplayName() {
			return "Execution Plan";
		}
		
	}
}
