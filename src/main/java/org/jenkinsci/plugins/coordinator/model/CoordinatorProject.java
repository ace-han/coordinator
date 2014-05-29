package org.jenkinsci.plugins.coordinator.model;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.Project;
import jenkins.model.Jenkins;

@SuppressWarnings({ "unchecked" })
public class CoordinatorProject extends
		Project<CoordinatorProject, CoordinatorBuild> implements TopLevelItem {
	
	public CoordinatorProject(ItemGroup<?> parent, String name) {
		super(parent, name);
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(getClass());
	}

	@Override
	protected Class<CoordinatorBuild> getBuildClass() {
		return CoordinatorBuild.class;
	}
	
	@Restricted(NoExternalUse.class)
	@Extension(ordinal = 1000)
	public static class DescriptorImpl extends AbstractProjectDescriptor {

		/**
		 * it shows like when create a new job the title line
		 */
		@Override
		public String getDisplayName() {
			return "Coordinator Project";
		}

		@Override
		public CoordinatorProject newInstance(
				@SuppressWarnings("rawtypes") ItemGroup parent, String name) {
			// it's invoked by Jenkins#createProject#newInstance, parent=Hudson,
			// name=whatever Job Name on the create interfaces
			return new CoordinatorProject(parent, name);
		}
	}


}
