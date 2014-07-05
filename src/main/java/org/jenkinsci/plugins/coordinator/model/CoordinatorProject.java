package org.jenkinsci.plugins.coordinator.model;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.Descriptor;
import hudson.model.Project;
import hudson.tasks.Builder;
import hudson.util.DescribableList;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;

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

	@Override
	public DescribableList<Builder, Descriptor<Builder>> getBuildersList() {
		DescribableList<Builder, Descriptor<Builder>> buildersList = super.getBuildersList();
		if (buildersList.size() == 0){
			// since DescribableList is CopyOnWriteList, 
			// keep it simple in comparison via size == 0 not contains(xxx)
			buildersList.add(new CoordinatorBuilder());
		}
		return buildersList;
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
		
		public JSON doCheckProjectExistence(@QueryParameter String idNameMap){
			JSONObject checks = JSONObject.fromObject(idNameMap);
			Set<String> projectNames = Jenkins.getInstance().getItemMap().keySet();
			HashMap<String, String> notExist = new HashMap<String, String>(checks.size()<<1);
			for(Object e: checks.entrySet()){
				Map.Entry<String, String> entry = (Map.Entry<String, String>) e;
				if(!projectNames.contains(entry.getValue())){
					notExist.put(entry.getKey(), entry.getValue());
				}
			}
			return JSONObject.fromObject(notExist);
			
		}
	}


}
