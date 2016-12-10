package org.jenkinsci.plugins.coordinator.model;

import com.infradna.tool.bridge_method_injector.WithBridgeMethods;
import hudson.Extension;
import hudson.model.*;
import hudson.model.Cause.LegacyCodeCause;
import hudson.model.queue.QueueTaskFuture;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import hudson.util.FormApply;
import hudson.util.FormValidation;
import hudson.util.QuotedStringTokenizer;
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

import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.logging.Logger;


@SuppressWarnings({ "unchecked" })
public class CoordinatorProject extends
		Project<CoordinatorProject, CoordinatorBuild> implements TopLevelItem {
	
	private transient List<Integer> rebuildVersions = new CopyOnWriteArrayList<Integer>();
	
	private static final Logger LOGGER = Logger.getLogger(CoordinatorProject.class.getName());
	
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
			reset(targetBuild);
			return targetBuild;
		} else {
			CoordinatorBuild cb = super.newBuild();
			TreeNode brandNewExecutionPlan = this.getCoordinatorBuilder().getExecutionPlan().clone(true);
			cb.setOriginalExecutionPlan(brandNewExecutionPlan);
			return cb;
		}
	}
	
	protected void reset(CoordinatorBuild targetBuild) {
		Object notStarted = extractNotStartEnumConstant();
		setField(targetBuild, "state", notStarted);
		setField(targetBuild, "result", null);
		targetBuild.getActions().clear();
	}

	private Object extractNotStartEnumConstant() {
		try {
			Object[] constants = Run.class.getDeclaredField("state").getType()
					.getEnumConstants();
			Object notStarted = constants[0];
			return notStarted;
		} catch (Exception e) {
			LOGGER.warning("could not extract field: state in Class hudson.model.Run\nException: " + e);
			return null;
		}
	}

	private void setField(CoordinatorBuild target, String fieldName, Object value) {
		try {
			Field field = Run.class.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		} catch (Exception e) {
			LOGGER.warning("could force to set up field: " + fieldName + " value: " + value);
		}
	}
	
	@Override
	public DescriptorImpl getDescriptor() {
		Jenkins instance = Jenkins.getInstance();
		if(instance == null) {
			throw new IllegalStateException("Jenkins is not ready.");
		}
		return (DescriptorImpl) instance.getDescriptorOrDie(getClass());
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
        
        final StaplerResponse originalResponse = rsp; 
        if (FormApply.isApply(req)) {
        	try {
        		Integer version = Integer.valueOf(req.getParameter("version"));
        		this.rebuildVersions.add(version);
        	} catch (NumberFormatException e){
        		throw FormValidation.error("invalid version in the request");
        	}
        	
        	// make rsp as an Response that won't do anything as sendRedirect is invoked...
        	rsp = (StaplerResponse) Proxy.newProxyInstance(rsp.getClass().getClassLoader(), rsp.getClass().getInterfaces(), 
        			new InvocationHandler(){

						@Override
						public Object invoke(Object proxy, Method method,
								Object[] args) throws Throwable {
							if(method.getName().equals("sendRedirect")){
								return null;
							}
							Object result = method.invoke(originalResponse, args);
							return result;
						}
        			});
        }
        
        
        ParametersDefinitionProperty pdp = super.getProperty(ParametersDefinitionProperty.class);
        boolean emptyPdp = (pdp == null);
        // below will always goes to pp._doBuild(xxx), should be quick enough
        synchronized(super.properties){
        	// some patch up
        	if(emptyPdp){
        		final CoordinatorProject cowner = this;
            	pdp = new ParametersDefinitionProperty(new ArrayList<ParameterDefinition>(2)){

            		public void _doBuild(StaplerRequest req, StaplerResponse rsp, 
            				@QueryParameter TimeDuration delay) throws IOException, ServletException {
            			this.owner = cowner;	// avoid empty owner on ParametersDefinitionProperty
            			super._doBuild(req, rsp, delay);
            		}
            		
            	};
            	// since it would finally get to AbstractProject.getProperty(ParametersDefinitionProperty.class)
            	// so super.properties.add(pdp) is necessary
            	super.properties.add(pdp);
            	
            }
        	List<ParameterDefinition> pds = pdp.getParameterDefinitions();
        	CoordinatorParameterDefinition cpd = new CoordinatorParameterDefinition(
        										getCoordinatorBuilder().getExecutionPlan().clone(true));
        	pds.add(cpd);
        	
        	super.doBuild(req, rsp, delay);
        	
        	// it's always a best practice to do some clean up after hijacking
        	pds.remove(cpd);
        	if(emptyPdp){
        		 super.properties.remove(pdp);
        	}
        }
        if (FormApply.isApply(req)) {
        	FormApply.applyResponse("notificationBar.show(" + QuotedStringTokenizer.quote("Submission Accepted.") + ",notificationBar.OK)")
        		.generateResponse(req, originalResponse, null);
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
			Jenkins instance = Jenkins.getInstance();
			if(instance == null) {
				throw new IllegalStateException("Jenkins is not ready.");
			}

			Set<String> projectNames = instance.getItemMap().keySet();
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

			final Jenkins instance = Jenkins.getInstance();
			if(instance == null) {
				throw new IllegalStateException("Jenkins is not ready.");
			}

			List<TopLevelItem> items = instance.getAllItems(TopLevelItem.class);
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

	/**
	 * For TimerTrigger(Schedule Job) Supported
	 */
	@Override
	public boolean scheduleBuild(int quietPeriod, Cause c) {
		List<ParameterValue> values = getDefaultParameterValues(true);
		return scheduleBuild2(quietPeriod, c, new ParametersAction(values))!=null;
	}
	
	/**
	 * For Test Case
	 */
	@SuppressWarnings("deprecation")
    @WithBridgeMethods(Future.class)
    public QueueTaskFuture<CoordinatorBuild> scheduleBuild2(int quietPeriod) {
		LegacyCodeCause cause = new LegacyCodeCause();
        return scheduleBuild2(quietPeriod, cause);
    }
	
	/**
     * For Test Case
     */
    @WithBridgeMethods(Future.class)
    public QueueTaskFuture<CoordinatorBuild> scheduleBuild2(int quietPeriod, Cause c) {
    	List<ParameterValue> values = getDefaultParameterValues(true);
        return scheduleBuild2(quietPeriod, c, new ParametersAction(values));
    }
	/**
	 * 
	 * @return defaultParameterValues for this project
	 */
	protected List<ParameterValue> getDefaultParameterValues(boolean withCoordinatorParameterValue) {
		ArrayList<ParameterValue> values;
		ParametersDefinitionProperty pp = getProperty(ParametersDefinitionProperty.class);
		if(pp == null){
			values = new ArrayList<ParameterValue>(2);
		} else{
			List<ParameterDefinition> pds = pp.getParameterDefinitions();
			values = new ArrayList<ParameterValue>(pds.size() + 1);
			for(ParameterDefinition pd: pds){
				values.add(pd.getDefaultParameterValue());
			}
		}
		if (withCoordinatorParameterValue){
			CoordinatorParameterDefinition cpd = new CoordinatorParameterDefinition(
					getCoordinatorBuilder().getExecutionPlan().clone(true));
			values.add(cpd.getDefaultParameterValue());
		}
		return values;
	}
}
