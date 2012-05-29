CDC - Community Distributed Cache
=================================


About
-----

CDC stands for Community Distributed Cache and allows for high-performance,
scalable and distributed memory clustering cache based on
[Hazelcast](http://www.hazelcast.com/) for both
[CDA](http://cda.webdetails.org) and [Mondrian](http://mondrian.pentaho.org). 


CDC is a pentaho plugin that provides the following features:

* CDA distributed cache support
* Mondrian distributed cache support
* Ability to switch between default and CDC cache for cda and mondrian
* Gracefully handles adding / removing new cache nodes
* Allows to selectively clear cache of specific [CDE](http://cde.webdetails.org) dashboards
* Allows to selectively clear cache of specific schemas / cubes / dimensions of mondrian cubes
* Provides an API to clean the cache from the outside (eg: after running etl)
* Provides a view over cluster status
* Supports several memory configuration options



Motivation
----------

Performance is a key point not only in business intelligence softwares but
generally in any user interface. The goal of CDC is to give a
[Pentaho](http://pentaho.com) implementation based on Mondrian / CDA a
distributed caching layer that can prevent as much as possible the database to be hit.



One added functionality is the ability to clear the cache of only specific
mondrian cubes. Even though Mondrian has a very complete api to control the
member's cache, Pentaho only exposes a clean all functionality that ends up
being very limited in production environments.


The cache being able to survive server restarts is a design bonus, and
supported by CDA out of the box. It will be supported by Mondrian as soon as
[MONDRIAN-1107](http://jira.pentaho.com/browse/MONDRIAN-1107) is fixed.



Requirements
------------

* Mondrian 3.4 or newer (in Pentaho 4.5)
* CDA 12.05.15



Usage
-----

It's very simple to configure CDC. 

* Install CDC using either the installer (soon to be available) or
   [ctools-installer](http://pedroalves-bi.blogspot.com/2011/06/ctools-installer-making-things-fast.html).
   If you do a manual install, be sure to copy the contents of
   _solution/system/cdc/pentaho/lib_ to server's _WEB-INF/lib_

* Download the [standalone cache
   node](http://ci.analytical-labs.com/job/Webdetails-CDC/lastSuccessfulBuild/artifact/dist/cdc-redist-SNAPSHOT.zip)

* Execute the standalone cache node in the same machine as pentaho or in the
   same internal network (_launch-hazelcast.sh_), optionally editing the file
   and changing the memory settings (defaults to 1Gb, increase at will). You
   can launch as many nodes as you want.

* Launch pentaho and click on the CDC button:

![CDC main screen](http://www.webdetails.pt/cdc/cdc-usage.png)

* Enable cache usage on CDA and Mondrian

* Restart pentaho server 

* Check on settings screen if they are satisfactory. Usually the defaults work
  fine.

![CDC settings](http://www.webdetails.pt/cdc/cdc-settings.png)



Open analyzer, jpivot or a CDE dashboard that uses CDA and you should see the cache being populated



Cluster info
--------------


[Hazelcast](http://www.hazelcast.com) has a very good [Management
Center](http://www.hazelcast.com/products.jsp), so it's outside the scope of
CDC to reimplement that kind of features. However, we do support a simple
cluster information dashboard gives an overview of the state of the nodes.


![CDC cluster info](http://www.webdetails.pt/cdc/cdc-clusterInfo.png)


Note about _lite nodes_: Pentaho server is itself a cache node. However, it's configured in such a way that doesn't hold data, thus the term _lite node_


Clean cache
-----------

With CDC you can selectively control the contents of the cache, allowing you to
clean either specific dashboards or cubes. The business case around this is
simple: We need to clear the cache after new data is available (usually as a
result of a etl job). CDC allows not only to do that but also to do it from within the etl process.


### CDA 

![CDA cache clean](http://www.webdetails.pt/cdc/cdc-cleanCacheCda.png)


CDC offers a solution navigator so that we can select a dashboard. When we
select that dashboard, all the CDA queries used by that dashboard will be
cleaned.

Clicking on the _URL_ button we'll get a url that we can call externally (from an etl job). Be aware that you need to add the user credentials when calling from the outside (eg: _&userid=joe&password=password_)


### Mondrian

![Mondrian cache clean](http://www.webdetails.pt/cdc/cdc-cleanCacheMondrian.png)


This one is very similar to the previous one, but navigates through the
available cubes. One can then either clean the entire schema, a specific cube
or even the individual cell cache for a specific dimension (use this latest one with care).



Issues, bugs and feature requests
---------------------------------


In order to report bugs, issues or feature requests, please use the [Webdetails CDC Project Page](http://redmine.webdetails.org/projects/cdc/issues)

### tcp46 Issue

There is a particularly nasty known issue, either at startup or when attempting to access hazelcast (ie putting elements in cache, accessing ClusterInfo). So far this issue has been confirmed in PCs running MacOS X.

#### diagnosis
If running `netstat -a -n` shows more than one socket on the same hazelcast port (by default they will start at 5701), and these are of different types (ie `tcp4` and `tcp46`), you are likely to have this issue.

#### workaround
Make sure the `-Djava.net.preferIPv4Stack` flag is explicitly set to the same value on both your pentaho JVM (can set it in the `JAVA_OPTS` flag) and the standalone script.

License
-------

CDC is licensed under the [MPLv2](http://www.mozilla.org/MPL/2.0/) license.


