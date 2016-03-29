package org.jenkinsci.plugins.coordinator.model;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;


import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.coordinator.test.PlatformUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.CommandInterpreter;

public class AppTest {
  @Rule 
  public JenkinsRule j = new JenkinsRule();
  
  @Test 
  public void first() throws Exception {
    FreeStyleProject project = j.createFreeStyleProject();
    CommandInterpreter commandShell = PlatformUtils.getCommandShellQuietly("echo hello");
    project.getBuildersList().add(commandShell);
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    System.out.println(build.getDisplayName() + " completed");
    // TODO: change this to use HtmlUnit
    String s = FileUtils.readFileToString(build.getLogFile());
    assertThat(s, containsString("echo hello"));
  }
}