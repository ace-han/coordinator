package org.jenkinsci.plugins.coordinator.model;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BatchFile;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * 
 * For project's builder part configuration
 * @author Ace Han
 *
 */
public class CoordinatorBuilder extends Builder {
	
	private TreeNode executionPlan;
	
	public CoordinatorBuilder(){
		setExecutionPlan(new TreeNode());
	}
	
	@DataBoundConstructor
	public CoordinatorBuilder(TreeNode executionPlan){
		setExecutionPlan(executionPlan);
	}
	
	public TreeNode getExecutionPlan() {
		return executionPlan;
	}

	public void setExecutionPlan(TreeNode executionPlan) {
		this.executionPlan = executionPlan;
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

		@Override
		public Builder newInstance(StaplerRequest req, JSONObject formData)
				throws FormException {
			JSONObject jsonObject = JSONObject.fromObject(
											formData.getString("executionPlan"), 
											TreeNode.JSON_CONFIG);
			TreeNode executionPlan = (TreeNode)JSONObject.toBean(jsonObject, TreeNode.JSON_CONFIG);
			return new CoordinatorBuilder(executionPlan);
		}
		
		
		
	}
}
