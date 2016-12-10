# Jenkins Coordinator Plugin [![Build Status](https://jenkins.ci.cloudbees.com/buildStatus/icon?job=plugins/coordinator-plugin)](https://jenkins.ci.cloudbees.com/job/plugins/job/coordinator-plugin/)

## About
Coordinator is a Jenkins plugin to let the user create _master_ job to include other ordinary jobs as build steps.

Build steps(job dependencies) could be categorized into `serial/parallel` patterns.
And we believe these two patterns should cover almost every generic scenario along with `breaking/non-breaking` options.

## Rationale

Jenkins has various ways to configure jobs execution order, such as built-in trigger _Build after other projects are built_, post-build action _Build other projects_ and plenty of plugins. However, if you want to leverage Jenkins not only as a CI tool, but a sophisticated deployment platform, you will still miss the fine-grained build steps control seen in other product such as BuildForge. 

## Getting started
1. Divide the whole deployment process into several parts by its nature, create separate jobs respectively, such as Maven build and packaging, transfer via SSH, database script run, static content update, some Redis commands;
1. Create a new job of type _Coordinator Project_. ,include those jobs defined in step 1, specify the execution order, group some of them under same tree node to parallel run; 
1. Trigger the master job, select which steps to run and start. Then you can monitor the over status within the single page.

__Detail configuration walk through__:
http://www.tothenew.com/blog/jenkins-coordinator-plugin/


## Demo
http://jenkins.unendedquest.com/view/Coordinators/

**Serial/Parallel**: Direct children of this kind of node will be executed **sequentially**/**concurrently**.

**Breaking/Non-Breaking**: Any failure on direct children of this kind of node will **break**/**not break** the whole build.

The UI configuration as below

| UI  | Serial | Parallel |
| ------------- | ------------- | ------------- |
| **Breaking**(default)  | <img src="https://raw.githubusercontent.com/jenkinsci/coordinator-plugin/master/src/main/webapp/images/coordinator-serial.ico" width="16">  | <img src="https://raw.githubusercontent.com/jenkinsci/coordinator-plugin/master/src/main/webapp/images/coordinator-parallel.ico" width="16">  |
| **Non-Breaking**  | <img src="https://raw.githubusercontent.com/jenkinsci/coordinator-plugin/develop/src/main/webapp/images/coordinator-non-breaking-serial.ico" width="16">  | <img src="https://raw.githubusercontent.com/jenkinsci/coordinator-plugin/develop/src/main/webapp/images/coordinator-non-breaking-parallel.ico" width="16">  |

<img src="https://cloud.githubusercontent.com/assets/1177332/14269580/935d63ba-fb18-11e5-8d0f-ebf82f71170c.png" width="512">

## Author
[Ace Han](https://github.com/ace-han)

## Development

## Licence
MIT

## References
https://wiki.jenkins-ci.org/display/JENKINS/Coordinator

