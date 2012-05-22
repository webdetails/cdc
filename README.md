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
* Supports multiple pentaho servers using the same cluster (eg: stage and production)
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

1. Install CDC using either the installer (soon to be available) or
   [ctools-installer](http://pedroalves-bi.blogspot.com/2011/06/ctools-installer-making-things-fast.html).
   If you do a manual install, be sure to copy the contents of
   _solution/system/cdc/pentaho/lib_ to server's _WEB-INF/lib_

2. Download the [standalone cache
   node](http://ci.analytical-labs.com/job/Webdetails-CDC/lastSuccessfulBuild/artifact/dist/cdc-redist-SNAPSHOT.zip)

3. Execute the standalone cache node in the same machine as pentaho or in the
   same internal network (_launch-hazelcast.sh_), optionally editing the file
   and changing the memory settings (defaults to 1Gb, increase at will). You
   can launch as many nodes as you want.

4. Launch pentaho and click on the CDC button:

![CDC main screen](http://www.webdetails.pt/cdc/cdc-usage.png)

5. Enable CDA and CDC

6. Restart pentaho server 

7. Check if the settings screen are satisfactory. Usually the defaults work
   fine.

![CDC settings](http://www.webdetails.pt/cdc/cdc-settings.png)



Open analyzer, jpivot or a CDE dashboard that uses CDA and you should see the cache being populated



Cluster status
--------------



Clean cache
-----------



Result
------

The result, after declaring this new datasource and registring the cube in
mondrian, is a new cube that we can use.



Issues, bugs and feature requests
---------------------------------


In order to report bugs, issues or feature requests, please use the [Webdetails CDC Project Page](http://redmine.webdetails.org/projects/cdc/issues)


License
-------

CDC is licensed under the [MPLv2](http://www.mozilla.org/MPL/2.0/) license.


