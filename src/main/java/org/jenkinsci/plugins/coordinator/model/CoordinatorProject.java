package org.jenkinsci.plugins.coordinator.model;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Project;
import hudson.tasks.Builder;
import hudson.util.DescribableList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import jenkins.util.TimeDuration;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


@SuppressWarnings({ "unchecked" })
public class CoordinatorProject extends
		Project<CoordinatorProject, CoordinatorBuild> implements TopLevelItem {
	
	private transient List<Integer> rebuildVersions = new CopyOnWriteArrayList<Integer>();
	
	public CoordinatorProject(ItemGroup<?> parent, String name) {
		super(parent, name);
	}

	@Override
	public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
		super.onLoad(parent, name);
		// avoid null after jenkins server restart
		rebuildVersions = new CopyOnWriteArrayList<Integer>();
	}
	
	
	protected synchronized CoordinatorBuild newBuild() throws IOException {
		if (!rebuildVersions.isEmpty()) {
			Integer rebuildVersion = rebuildVersions.remove(0);
			CoordinatorBuild targetBuild = super.getBuildByNumber(rebuildVersion);
			if (targetBuild == null) {
				// only IOException would make it continue like fluent
				throw new IOException("could not retrieve the specific Build by build #: " + rebuildVersion);
			}
			
			targetBuild.addOldActions(targetBuild.getAllActions());
			
			// it will get set again in hudson.model.Executor
			/*
			 * for (Action action: workUnit.context.actions) {
			 * 	((Actionable) executable).addAction(action);
			 * }
			 */
			targetBuild.getActions().clear();
			return targetBuild;
		} else {
			CoordinatorBuild cb = super.newBuild();
			cb.setOriginalExecutionPlan(this.getCoordinatorBuilder().getExecutionPlan());
			return cb;
		}
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
	public boolean isParameterized() {
		// always true since we will take builder's executionPlan into account
		return true;
	}
	
	public void doBuild( StaplerRequest req, StaplerResponse rsp, @QueryParameter TimeDuration delay ) throws IOException, ServletException {
        // hijack the parameter entry form.
        if (!req.getMethod().equals("POST")) {
            req.getView(this, "parameters-index.jelly").forward(req, rsp);
            return;
        }
        
        ParametersDefinitionProperty pdp = super.getProperty(ParametersDefinitionProperty.class);
        boolean emptyPdp = (pdp == null);
        // below will always go to pp._doBuild(xxx), should be quick enough
        synchronized(this.properties){
        	// some patch up
        	if(emptyPdp){
        		final CoordinatorProject cowner = this;
            	pdp = new ParametersDefinitionProperty(new ArrayList<ParameterDefinition>(2)){

            		public void _doBuild(StaplerRequest req, StaplerResponse rsp, 
            				@QueryParameter TimeDuration delay) throws IOException, ServletException {
            			this.owner = cowner;
            			super._doBuild(req, rsp, delay);
            		}
            		
            	};
            	super.properties.add(pdp);
            	
            }
        	List<ParameterDefinition> pds = pdp.getParameterDefinitions();
        	CoordinatorParameterDefinition cpd = new CoordinatorParameterDefinition();
        	pds.add(cpd);
        	
        	super.doBuild(req, rsp, delay);
        	
        	// some clean up
        	pds.remove(cpd);
        	if(emptyPdp){
        		 super.properties.remove(pdp);
        	}
        }
    }

	public CoordinatorBuilder getCoordinatorBuilder() {
		return (CoordinatorBuilder) getBuilders().get(0);
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

	public List<ParameterDefinition> getParameterDefinitions(){
		// for a simple access to parametersDefinition in parameters-index.jelly
		ParametersDefinitionProperty pdp = super.getProperty(ParametersDefinitionProperty.class);
		if(pdp != null){
			return pdp.getParameterDefinitions();
		}
		return null;
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
		
		public JSON doSearchProjectNames(@QueryParameter String q){
			ArrayList<String> result = new ArrayList<String>();
			List<TopLevelItem> items = Jenkins.getInstance().getAllItems(TopLevelItem.class);
			for (TopLevelItem item : items) {
				if (item.hasPermission(Item.READ)
						&& !this.testInstance(item) // exclude the Coordinator Type
						&& item.getFullName().toLowerCase().contains(q.toLowerCase())) {
					result.add(item.getFullName());
				}
			}
			return JSONArray.fromObject(result);
		}
	}

}
