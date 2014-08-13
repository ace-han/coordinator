package org.jenkinsci.plugins.coordinator.model;

import hudson.Util;
import hudson.model.Action;
import hudson.model.Build;
import hudson.model.CauseAction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

public class CoordinatorBuild extends Build<CoordinatorProject, CoordinatorBuild> {
	
	private List<List<? extends Action>> oldActions = new ArrayList<List<? extends Action>>();

	private transient PerformExecutor performExecutor;
	
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
	
	
	
}
