#Run Server
To run server use
```
sbt run
```
#Run Load Test
Load test is in the same project im main class *throttling.test.RunTest*. You have to set start parameters (users, rps, duration in seconds and log's file prefix). For exmaple
```
sbt "run-main throttling.test.RunTest 5 10 10 throttling"
```
So, test will run for a 5 users (threads). Each user sends 10 requests per second during 10 seconds. Result will be wrote in file *tests/<file_prefix>_<users>_<rps>_<duration>.log*.
#Config
```
akka {
  loglevel = DEBUG
}

spray.can.server {
  request-timeout = 1s
  
  throttling-port = 9999
  port = 8080
}

throttling {
  grace-rps = 10
  cache {
  	max-capacity = 500
  	initial-capacity = 16, 
  	#time-to-live = 60 //in seconds. Comment for Duration.Inf 
  	#time-to-idle = 60 //in seconds. Comment for Duration.Inf
  }
}
```
#Load Test Result
You can find test results for *5* users, *10* rps and during *10* seconds.
Throttling is on: https://github.com/MikhailGevak/Throttling/blob/master/tests/throttling_5_10_10%20seconds.log
Throttling is off: https://github.com/MikhailGevak/Throttling/blob/master/tests/throttling_5_10_10%20seconds.log

So, you can see that response times for top requests are strange. I think it's a spray-http-client problem :( Program notes startTime, sends request to IO(Http) and notes finish time when response is received. And the problem has to be investigated.
