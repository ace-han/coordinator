# Jenkins Coordinator Plugin

## About
Coordinator is a Jenkins plugin to let the user create _master_ job to include other ordinary jobs as build steps.

## Rationale

Jenkins has various ways to configure jobs execution order, such as built-in trigger _Build after other projects are built_, post-build action _Build other projects_ and plenty of plugins. However, if you want to leverage Jenkins not only as a CI tool, but a sophisticated deployment platform, you will still miss the fine-grained build steps control seen in other product such as BuildForge. 

## Getting started
1. Divide the whole deployment process into several parts by its nature, create separate jobs respectively, such as Maven build and packaging, transfer via SSH, database script run, static content update, some Redis commands;
1. Create a new job of type _Coordinator Project_. ,include those jobs defined in step 1, specify the execution order, group some of them under same tree node to parallel run; 
1. Trigger the master job, select which steps to run and start. Then you can monitor the over status within the single page.

## Demo
http://jenkins.unendedquest.com/view/Coordinators/

## Author
[Ace Han](https://github.com/ace-han)

## Development

## Licence
MIT

## References
https://wiki.jenkins-ci.org/display/JENKINS/Coordinator

