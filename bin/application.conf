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