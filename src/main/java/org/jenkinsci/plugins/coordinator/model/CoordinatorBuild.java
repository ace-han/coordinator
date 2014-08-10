package org.jenkinsci.plugins.coordinator.model;

import java.io.File;
import java.io.IOException;

import hudson.model.Build;

public class CoordinatorBuild extends Build<CoordinatorProject, CoordinatorBuild> {

	public CoordinatorBuild(CoordinatorProject project) throws IOException {
		super(project);
	}

	public CoordinatorBuild(CoordinatorProject project, File buildDir) throws IOException {
        super(project, buildDir);
    }
}
