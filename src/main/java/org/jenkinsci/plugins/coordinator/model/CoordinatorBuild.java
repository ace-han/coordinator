package org.jenkinsci.plugins.coordinator.model;

import hudson.Util;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.CauseAction;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSON;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.apache.commons.jelly.JellyContext;
import org.apache.commons.jelly.JellyException;
import org.apache.commons.jelly.Script;
import org.apache.commons.jelly.XMLOutput;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.MetaClass;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.WebApp;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.jelly.JellyClassTearOff;


public class CoordinatorBuild extends Build<CoordinatorProject, CoordinatorBuild> {
	
	private List<List<? extends Action>> oldActions = new ArrayList<List<? extends Action>>();

	private transient PerformExecutor performExecutor;
	
	private TreeNode originalExecutionPlan;
	
	private transient Map<String, Integer> tableRowIndexMap;
	
	/*package*/ TreeNode getOriginalExecutionPlan() {
		return originalExecutionPlan;
	}

	public void setOriginalExecutionPlan(TreeNode originalExecutionPlan) {
		this.originalExecutionPlan = originalExecutionPlan;
	}

	public CoordinatorBuild(CoordinatorProject project) throws IOException {
		super(project);
	}

	public CoordinatorBuild(CoordinatorProject project, File buildDir) throws IOException {
        super(project, buildDir);
    }

	public void addOldActions(List<? extends Action> oldActions) {
		this.oldActions.add(oldActions);
	}
	
	public List<List<? extends Action>> getOldActions(){
		return this.oldActions;
	}

	@Override
	public void run() {
		if (!this.oldActions.isEmpty()){
			foldCauseIntoOne();
		}
		super.run();
	}

	private void foldCauseIntoOne() {
		CauseAction curCauseAction = getAction(CauseAction.class);
		if(curCauseAction != null ){
			CauseAction oldCauseAction = Util.filter(this.oldActions.get(this.oldActions.size()-1), 
														CauseAction.class).get(0);
			
			curCauseAction.getCauses().addAll(oldCauseAction.getCauses());
			super.replaceAction(curCauseAction);
		}
	}

	@Override
	@RequirePOST
	public synchronized HttpResponse doStop() throws IOException,
			ServletException {
		if(performExecutor != null){
			performExecutor.shutdown();
		}
		return super.doStop();
	}

	public void setPerformExecutor(PerformExecutor performExecutor) {
		// just for a clean stop
		this.performExecutor = performExecutor;
	}
	
	public AtomicBuildInfo getExecutionPlanInfo(){
		prepareTableRowIndexMap();
		return prepareAtomicBuildInfo(this.originalExecutionPlan, this.tableRowIndexMap, false);
	}

	private void prepareTableRowIndexMap() {
		if(this.tableRowIndexMap == null){
			this.tableRowIndexMap = new HashMap<String, Integer>();
			List<TreeNode> list = this.originalExecutionPlan.getFlatNodes(true);
			for(int i=0; i<list.size(); i++){
				TreeNode node = list.get(i);
				this.tableRowIndexMap.put(node.getId(), i);
			}
		}
	}

	public JSON doPollActiveAtomicBuildStatus(StaplerRequest req) {
		if(this.performExecutor == null){
			// no build or refresh or reload from disk
			return JSONNull.getInstance();
		}
		Set<Entry<String, AbstractBuild<?, ?>>> entrySet = this.performExecutor.getActiveBuildMap().entrySet();
		Map<String, String> result = new HashMap<String, String>(entrySet.size()*2 + 3);
		TreeNode dummyNode = prepareDummyTreeNode();
		for(Map.Entry<String, AbstractBuild<?, ?>> entry: entrySet){
			AbstractBuild<?, ?> build = entry.getValue();
			AtomicBuildInfo abi = new AtomicBuildInfo();
			abi.build = build;
			abi.treeNode = dummyNode;	// just taking advantage of tableRow.jelly
			abi.tableRowIndex = this.tableRowIndexMap.get(entry.getKey());
			result.put(entry.getKey(), getBuildInfoScriptAsString(abi));
		}
		return JSONObject.fromObject(result);
	}
	
	public String doAtomicBuildResultTableRowHtml(@QueryParameter String nodeId, 
			 @QueryParameter String jobName, @QueryParameter int buildNumber){
		prepareTableRowIndexMap();
		AtomicBuildInfo abi = new AtomicBuildInfo();
		abi.build = retrieveTargetBuild(jobName, buildNumber);
		abi.treeNode = prepareDummyTreeNode();
		abi.tableRowIndex = this.tableRowIndexMap.get(nodeId);
		return getBuildInfoScriptAsString(abi);
	}

	private TreeNode prepareDummyTreeNode() {
		TreeNode dummyNode = new TreeNode();
		dummyNode.setText("Dummy for Polling Active Atomic Build Info");
		return dummyNode;
	}
	
	protected String getBuildInfoScriptAsString(AtomicBuildInfo abi) {
		JellyContext context = new JellyContext();
        // let Jelly see the whole classes
        WebApp webapp = WebApp.getCurrent();
		context.setClassLoader(webapp.getClassLoader());
        context.setVariable("it", abi);
        context.setVariable("fromClazz", this.getClass());
        MetaClass mc = webapp.getMetaClass(this.getClass());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(mc.classLoader.loader);
        try {
        	XMLOutput output = XMLOutput.createXMLOutput(baos);
			Script script = mc.loadTearOff(JellyClassTearOff.class).findScript("tableRow.jelly");
			script.run(context, output);
			return baos.toString("UTF-8");
		} catch (JellyException e) {
			LOGGER.warning("Exception in tableRow.jelly:\n"+e);
			return prepareBuildStatusErrorMessage(e);
		} catch (UnsupportedEncodingException e) {
			LOGGER.warning("Could not resolve tableRow.jelly in the specific charset:\n" + e);
			return prepareBuildStatusErrorMessage(e);
		} finally {
			Thread.currentThread().setContextClassLoader(old);
		}

	}

	private String prepareBuildStatusErrorMessage(Exception e) {
		return "<div class='jstree-wholerow jstree-table-row' style='background-color:#ffebeb;'>"
				+"<div class='jstree-table-col jobStatus'>check log for error details</div></div>";
	}

	protected AtomicBuildInfo prepareAtomicBuildInfo(TreeNode node, Map<String, Integer> tableRowIndexMap, boolean simpleMode){
		if(simpleMode && !node.getState().checked){
			return null; // save the time
		}
		AtomicBuildInfo abi = new AtomicBuildInfo();
		abi.treeNode = node;
		abi.tableRowIndex = tableRowIndexMap.get(node.getId());
		List<TreeNode> children = node.getChildren();
		if(children.isEmpty()){
			abi.build = retrieveTargetBuild(node.getText(), node.getBuildNumber());
		} 
		abi.children = new ArrayList<AtomicBuildInfo>(children.size());
		for(TreeNode child: children){
			AtomicBuildInfo abiChild = prepareAtomicBuildInfo(child, tableRowIndexMap, simpleMode);
			if(abiChild != null){
				abi.children.add(abiChild);
			}
		}
		return abi;
	}

	private AbstractBuild<?, ?> retrieveTargetBuild(String projectName,
			int buildNumber){
		AbstractProject<?, ?> project = (AbstractProject<?, ?>)Jenkins.getInstance().getItem(projectName);
		if(project == null){
			return null;
		}
		return (AbstractBuild<?, ?>)project.getBuildByNumber(buildNumber);
	}
	
	/**
	 * For display purpose
	 * @author Ace Han
	 *
	 */
	public static class AtomicBuildInfo {
		public TreeNode treeNode;
		public int tableRowIndex;
		public AbstractBuild<?, ?> build;
		
		public List<AtomicBuildInfo> children;
	}
	
	private static final Logger LOGGER = Logger.getLogger(CoordinatorBuild.class.getName());
}
