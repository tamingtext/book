#!/usr/bin/perl

use File::Find;
use Getopt::Long;

#the number of instances to start with
$instances = 2;
#the maximum number of instances
$maxInstances = 8;
#the default step for instances
$step = 2;
#the maximum number of iterations.  One iteration is for all instances from $instances to maxInstances by step
#In other words, it will run each instance selection 5 times
$numIterations = 2;
#the key pair name to use
$keyPair = "";
#where the input lives.  The all files are relative to this path
$input = "s3n://news-vecs";
#the name of the Mahout job file to run, relative to $input
$jobName = "mahout-core-0.4-SNAPSHOT.job";
#the name of the file, relative to input, containing the vector
$vector = "part-out.vec";
$result = &GetOptions("input=s", \$input, "instances=i", \$instances, "iterations=i", \$numIterations, "key=s", \$keyPair, "maxInstances=i", \$maxInstances, "step=i", \$step,
  "jobName=s", \$jobName, "vector=s", \$vector);
if ($keyPair eq ""){
  print "Must specify --key";
  exit();
}
$iter = 0;
$killCmd = "";
for ($iter = 0; $iter < $numIterations; $iter++){

  for ($currInst = $instances; $currInst < $maxInstances; $currInst++){
    $item = $currInst . "_" . $iter;
    $cmd = "elastic-mapreduce --create --alive --log-uri $input-log" . $currInst . " --key-pair $keyPair --num-instances $currInst --name kmeans_" . $item;
    print "\nRunning: $cmd\n";
    $out = `$cmd`;
    #testing
    #$out = "Created job flow $item   \n";
    print $out;
    $job = $out;
    $job =~ s/\s*Created job flow\s*//;
    $job =~ s/\s*//g;
    $killCmd .= "elastic-mapreduce --terminate $job #Kill for job with $currInst instances\n";
    $cmd = "elastic-mapreduce -j $job --jar s3n://news-vecs/$jobName  --main-class org.apache.mahout.driver.MahoutDriver --arg kmeans --arg --input --arg " . $input
      . "/$vector --arg --clusters --arg $input/kmeans_" . $item . "/clusters-run_" . $item . "/ --arg -k --arg 10 --arg --output --arg $input/kmeans_" . $item . "/out-run_$item "
      ." --arg --distanceMeasure --arg  org.apache.mahout.common.distance.CosineDistanceMeasure --arg --convergenceDelta --arg 0.001 --arg --overwrite --arg --maxIter --arg 50 --arg --clustering";
    print "\nRunning: $cmd\n";
    $out = `$cmd`;
    sleep(5);
  }

}
print "\nDON'T FORGET TO TERMINATE JOBs:\n";
print "$killCmd\n";


