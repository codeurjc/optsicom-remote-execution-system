optsicom-remote-execution-system
================================

Executes Java programs remotely directly from Eclipse IDE

Optsicom RES stands for Optsicom Remote Experiment System. It is a system that allows to execute aplications from Eclipse in remote machines. This is very useful in some situations like executing in a virtual machine, in a cluster, etc.

Optsicom Remote Experiment System (Optsicom RES) is a set of tools aimed to execute and debug Java programs developed in Eclipse in another machine. Specifically, Optsicom RES offers the following functionality:

* Execute or debug a concrete version of a program in the current machine (local execution) within Eclipse
* Execute or debug a concrete version of a program in another machine (remote execution) within Eclipse

In both cases, in the current implementation, Optsicom RES creates a .zip file containing source and binary files. In the case of remote execution/debug, Optsicom RES send this files to remote machine. To do this, the remote machine has to be executing Optsicom RES Server.

Optsicom RES is composed by two pieces:

* Optsicom RES Server: It is a small Java program reponsible to receive and execute Java programas. It also save output from executed programs to further view.
* Optsicom RES Client: It is a Eclipse plugin that allow to launch Java projects in a local or remote way. In both cases, a .zip is generated with the code that is being executed.


Installation
------------

In order to be able to use Optsicom RES, at least one machine must have the Optsicom RES service running. To do this, download the server from here, unzip, and follow the instructions on the README file.

**Setting the password for the service**

The Optsicom RES service is password-protected. Service password can be set (and changed) issuing the following command:

  java -jar es.optsicom.res.server-*.jar <password>

This is the password that must be entered in the Optsicom RES Eclipse dialog.

**Running the service**

The service is started as follows:

  java -jar es.optsicom.res.server-<version>.jar ip rmi_port port2 port3 path_to_jre_bin_folder

The key here are the two first parameters passed to the jar application. These parameters need to be specified later on the client (the Eclipse plugin), so that it can connect to the service. The IP parameter must be an ip address accesible from the client. You can also specify a hostname instead. The other two ports are discovered dynamically by the client. The path to the jre bin folder is used to know which jvm version to use. In Ubuntu systems, it is usually placed under /usr/lib/jvm/java-6-sun/jre/bin. This last parameter was introduced in version 0.9.17 to force the user choose a jvm version explicitly.

Installing the Eclipse plugin
-----------------------------

Optsicom RES has been tested succesfully with Eclipse 3.5 and above (including Eclipse 4.x). There is no reason why it shouldn't work with prior Eclipse 3.x versions, however, we have not tested Optsicom RES with them.

To install the Eclipse plugin just:

* Go to Help > Install new software... 
* Copy and paste this URL in the work with field: <http://code.sidelab.es/public/optsicomsuite/res/releases/> 
* Select Optsicom Remote Experiment System
* Click Finish.

**Eclipse plugin usage**

Using the Optsicom RES Eclipse plugin is really easy: it works just like the standard way of launching Java applications. When you select a Java file, and press the right mouse button, in the Run As... option of the popup menu, a couple of new items appear:

* Local Java Versioned Application: this option runs the application locally, but it compresses the project and its dependencies for later recovery
* Remote Java Versioned Applciation: this option allows the user to launch the Java program in a remote machine

