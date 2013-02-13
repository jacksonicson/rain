import sys
import subprocess
import time
import re
import os
import simplejson as json
import getopt
from run_manager import RunManager, RainOutputParser

'''Example config
{
    "profilesCreatorClass": "radlab.rain.workload.httptest.BurstUrlProfileCreator",
    "profilesCreatorClassParams": {
         "hostListFile" : "/home/rean/work/rain.git/hostlist.txt",
         "popularHostFraction": 0.2,
         "numHostTargets": 10,
         "meanThinkTime": 5,
         "usersPerPopularHost": 10,
         "usersPerLessPopularHost":1,
         "burstSizePerPopularHost":5,
         "burstSizePerLessPopularHost":2,
         "meanResponseTimeSamplingInterval":123,
         "generatorParameters": {
             "connectionTimeoutMsecs" : 1000,
             "socketTimeoutMsecs" : 1000
             }
    },
    "timing": {
        "rampUp": 10,
        "duration": 60,
        "rampDown": 10
    },
    "pipePort": 7581
}
'''

class BurstUrlGeneratorParameters:
    '''
    Rain configuration object for generator parameters for
    the SpecificUrlGenerator
    '''
    def __init__(self):
        self.connectionTimeoutMsecs = 1000
        self.socketTimeoutMsecs = 1000

    def to_json( self ):
        dict = {}
        dict['connectionTimeoutMsecs'] = self.connectionTimeoutMsecs
        dict['socketTimeoutMsecs'] = self.socketTimeoutMsecs
        return dict

class BurstUrlTestConfig:
    '''
    Rain configuration object for SpecificUrl tests
    '''

    def __init__(self):
        self.generatorParameters = BurstUrlGeneratorParameters()
        # Profile creator class to use
        self.profilesCreatorClass = \
            "radlab.rain.workload.httptest.BurstUrlProfileCreator"
        # Profile creator params
        self.hostListFile = ""
        self.popularHostFraction = 0.2 # Fraction of popular hosts
        self.usersPerPopularHost = 25
        self.usersPerLessPopularHost = 5
        self.meanThinkTime = 5 # seconds
        # Burst size info
        self.burstSizePerPopularHost = 5
        self.burstSizePerLessPopularHost = 2
        # Timing info
        self.rampUp = 10 # seconds
        self.duration = 60 # seconds
        self.rampDown = 10 # seconds
        self.pipePort = 7851 # comm port
        # response time sampling info
        self.meanResponseTimeSamplingInterval = 100

    def to_json( self ):
        dict = {}
        dict['profilesCreatorClass'] = self.profilesCreatorClass
        # sub-map with profile creator parameters
        creatorParams = {}
        # set all the creator params
        creatorParams['hostListFile'] = self.hostListFile
        creatorParams['popularHostFraction'] = self.popularHostFraction
        creatorParams['meanThinkTime'] = self.meanThinkTime
        creatorParams['usersPerPopularHost'] = self.usersPerPopularHost
        creatorParams['usersPerLessPopularHost'] = self.usersPerLessPopularHost
        creatorParams['burstSizePerPopularHost'] = self.burstSizePerPopularHost
        creatorParams['burstSizePerLessPopularHost'] = \
            self.burstSizePerLessPopularHost
        creatorParams['meanResponseTimeSamplingInterval'] = \
            self.meanResponseTimeSamplingInterval

        # add in the generator parameters to the creator parameters
        creatorParams['generatorParameters'] = \
            self.generatorParameters.to_json()

        # Add profile creator params to top-level dictionary
        dict['profilesCreatorClassParams'] = creatorParams
        # sub map with timing info
        timing = {}
        timing['rampUp'] = self.rampUp
        timing['duration'] = self.duration
        timing['rampDown'] = self.rampDown
        # Add timing info to top-level dictionary
        dict['timing'] = timing
        # Add the comm port
        dict['pipePort'] = self.pipePort
        return dict

class BurstUrlTestRunner:
    def create_dir( self, path ):
        if not os.path.exists( path ):
            os.mkdir( path )

    def run( self, hostlist_fname, popular_host_fraction,\
             mean_think_time, users_per_popular_host,\
             users_per_less_popular_host,\
             connection_timeout_msecs, socket_timeout_msecs,\
             burst_size_per_popular_host, burst_size_per_less_popular_host,\
             results_dir="./results", run_duration_secs=60, \
             config_dir="./config", pipe_port=7851, \
             mean_response_time_sample_interval=100 ):

        # Some pre-reqs:
        # 1) create the config_dir if it doesn't exist
        # 2) create the results_dir if it doesn't exist
        self.create_dir( config_dir )
        self.create_dir( results_dir )
        
        num_tests = 1
        for i in range(num_tests):
            # With a single Rain launch, load an entire block of ip's
            config = BurstUrlTestConfig()
            config.hostListFile = hostlist_fname
            config.duration = run_duration_secs
            config.popularHostFraction = popular_host_fraction
            config.usersPerPopularHost = users_per_popular_host
            config.usersPerLessPopularHost = users_per_less_popular_host
            config.meanThinkTime = mean_think_time
            config.pipePort = pipe_port
            config.burstSizePerPopularHost = burst_size_per_popular_host
            config.burstSizePerLessPopularHost = \
                burst_size_per_less_popular_host
            config.meanResponseTimeSamplingInterval = \
                mean_response_time_sample_interval
            # Add in the parameters for the workload generator
            # the operation mixes etc.
            generatorParams = BurstUrlGeneratorParameters()
            generatorParams.connectionTimeoutMsecs = connection_timeout_msecs
            generatorParams.socketTimeoutMsecs = socket_timeout_msecs
            config.generatorParameters = generatorParams
            
            json_data = \
                json.dumps(config, sort_keys='True',\
                               default=BurstUrlTestConfig.to_json)
            # Write this data out to a file, then invoke the run mananger
            # passing in the path to this file
                                  
            print( "[BurstUrlTestRunner] json config: {0}"\
                       .format(json_data) )

            run_classpath=".:rain.jar:workloads/httptest.jar"
            run_config_filename = config_dir + "/" + \
                "run_burst_url_config" + "_nodes.json"
            run_output_filename = results_dir + "/" + \
                "run_burst_url_log" + "_nodes.txt"
            run_results_filename = results_dir + "/" + \
                "run_burst_url_result" + "_nodes.txt"
            
            # write the json data out to the config file
            # invoke the run manager passing the location of the config file
            # collect the results and write them out to the results_dir
         
            print "[BurstUrlTestRunner] Writing config file: {0}"\
                .format( run_config_filename )
            config_file = open( run_config_filename, 'w' )
            config_file.write( json_data )
            config_file.flush()
            config_file.close()
            run_output = RunManager.run_rain( run_config_filename,\
                                               run_classpath )
            #print run_output
            track_results = RainOutputParser.parse_output( run_output )
            # Validate each of the track_results instances
            
            for result in track_results:
                # Set some 90th and 99th pctile thresholds
                result.pct_overhead_ops_threshold=10.0
                result.pct_failed_ops_threshold=5.0
                # Set the desired 90th and 99th percentile thresholds for
                # the 50ms, 100ms, 200ms operations - set everything to
                # 500 ms = 0.5s. Threshold units = seconds
                opNamePopular = "BurstUrl(" + str(burst_size_per_popular_host) + ")"
                opNameLessPopular = "BurstUrl(" + \
                    str(burst_size_per_less_popular_host) + ")"
                
                # A burst of size N means that the base url is requested
                # plus an additional N requests for a total of N+1 requests issued serially.
                # Since the requests are issued serially, scale the desired
                # 90th and 99th percentile response times by N+1
                popular_host_scaling_factor = (burst_size_per_popular_host + 1)
                less_popular_host_scaling_factor = (burst_size_per_less_popular_host + 1)

                # Scale the response times by the number of serial requests
                result.op_response_time_thresholds[opNamePopular]=\
                    ((0.5*popular_host_scaling_factor), (0.5*popular_host_scaling_factor))
                result.op_response_time_thresholds[opNameLessPopular]=\
                    ((0.5*less_popular_host_scaling_factor), \
                         (0.5*less_popular_host_scaling_factor))

            # Write out the run output
            print "[BurstUrlTestRunner] Writing output: {0}"\
                .format( run_output_filename )
            run_output_file = open( run_output_filename, 'w' )
            run_output_file.write( run_output )
            run_output_file.flush()
            run_output_file.close()

            # Write out the run results
            print "[BurstUrlTestRunner] Writing results: {0}"\
                .format( run_results_filename )
            run_results_file = open( run_results_filename, 'w' )
            RainOutputParser.print_results( track_results, run_results_file )
            
            run_results_file.write( "\n" )
            # After writing out the table for all the tracks
            # Spit out the 90th and 99th percentiles
            for result in track_results:
                for k,v in result.op_response_times.items():
                    run_results_file.write( "{0},{1},{2},{3}\n"\
                               .format(result.name, k, v[0], v[1]) )

            run_results_file.flush()
            run_results_file.close()

def usage():
    print( "Usage: {0} [--resultsdir <path>]"\
           " [--duration <seconds to run>] [--configdir <path>]"\
           " [--popularhosts <%popular hosts>]"\
           " [--popularhostusers <users-per-popular-host>]"\
           " [--lesspopularhostusers <users-per-less-popular-host]"\
           " [--popularhostburst <burst size>]"\
           " [--lesspopularhostburst <burst size>]"\
           " [--connectiontimeout <msecs to wait for http connection>]"\
           " [--sockettimeout <msecs to wait for data/server response>]"\
           " [--hostlist <path to file>] [--pipeport <port>]"\
           " [--sampleinterval <interval>]"\
           .format(sys.argv[0]) )

    print "\n"
    print( "defaults: {0} --resultsdir ./results --duration 60"\
           " --configdir ./config --popularhosts 0.2"\
           " --popularhostusers 25 --lesspopularhostusers 5"\
           " --popularhostburst 5 --lesspopularhostburst 2"\
           " --sockettimeout 1000 --connectiontimeout 1000"\
           " --thinktime 5 "\
           " --hostlist /home/rean/work/rain.git/hostlist.txt"\
           " --pipeport 7851 --sampleinterval 10"\
           .format(sys.argv[0]) )


def main(argv):
    results_dir = "./results"
    run_duration = 60
    config_dir = "./config"
    hostlist_fname = ""
    popular_host_fraction = 0.2
    mean_think_time = 5
    users_per_popular_host = 25
    users_per_less_popular_host = 5
    connection_timeout_msecs = 1000
    socket_timeout_msecs = 1000
    pipe_port = 7851
    popular_host_burst = 5
    less_popular_host_burst = 2
    mean_response_time_sampling_interval = 100

    # parse arguments and replace the defaults
    try:
        opts, args = getopt.getopt( argv, "h", ["resultsdir=",\
                                          "duration=", "configdir=",\
                                          "help", "hostlist=", \
                                           "popularhosts=",\
                                           "thinktime=", "popularhostusers=",\
                                           "lesspopularhostusers=",\
                                           "popularhostburst=",\
                                           "lesspopularhostburst=",\
                                           "connectiontimeout=",\
                                           "sockettimeout=", "pipeport=",\
                                           "sampleinterval="] )
    except getopt.GetoptError:
            print sys.exc_info()
            usage()
            sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help" ):
            usage()
            sys.exit()
        elif opt == "--resultsdir":
            results_dir = arg
        elif opt == "--duration":
            run_duration = int(arg)
        elif opt == "--configdir":
            config_dir = arg
        elif opt == "--popularhostfraction":
            popular_host_fraction = float(arg)
        elif opt == "--popularhostusers":
            users_per_popular_host = int(arg)
        elif opt == "--lesspopularhostusers":
            users_per_less_popular_host = int(arg)
        elif opt == "--thinktime":
            mean_think_time = float(arg)
        elif opt == "--connectiontimeout":
            connection_timeout_msecs = int(arg)
        elif opt == "--sockettimeout":
            socket_timeout_msecs = int(arg)
        elif opt == "--hostlist":
            hostlist_fname = arg
        elif opt == "--pipeport":
            pipe_port = int(arg)
        elif opt == "--popularhostburst":
            popular_host_burst = int(arg)
        elif opt == "--lesspopularhostburst":
            less_popular_host_burst = int(arg)
        elif opt == "--sampleinterval":
            mean_response_time_sampling_interval = int(arg)
    
    # launch run
    test_runner = BurstUrlTestRunner()
    test_runner.run( hostlist_fname, popular_host_fraction,\
             mean_think_time, \
             users_per_popular_host,\
             users_per_less_popular_host,\
             connection_timeout_msecs, socket_timeout_msecs,\
             popular_host_burst, less_popular_host_burst,\
             results_dir, run_duration, \
             config_dir, pipe_port, mean_response_time_sampling_interval )

if __name__=='__main__':
    # Pass all the arguments we received except the name of the script
    # argv[0]
    main( sys.argv[1:] )
