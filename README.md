Rain+ workload driver
=====================

Rain+ was designed to generate realistic variable workload on an IaaS cloud testbed environment including dynamic allocation and deallocation of VMs during a benchmark. Rain+ extends the original Rain implementation found [here](https://github.com/yungsters/rain-workload-toolkit). It adds the following functionality: 

* Integration with [Sonar](https://github.com/jacksonicson/sonar) and [Times](https://github.com/jacksonicson/times). Time series of request response times and other systems statistics are stored in Sonar. Times serves as an archive for time series data describing variable workload on a target over time. 
* Thrift RPC interfaces to configure and trigger a Rain+ workload driver instances. This allows to synchronize multiple Rain+ instances. 
* Complete refactoring of the Scoreboard implementation to gather statistics. Percentiles on the operation duration are calculated by the [P-Square](https://github.com/jacksonicson/psquared) algorithm without storing samples. 


## Usage scenario






