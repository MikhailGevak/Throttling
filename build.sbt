organization  := "com.example"

version       := "0.1"

scalaVersion  := "2.11.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

EclipseKeys.withSource := true

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    "io.spray"            %%  "spray-caching" 		% sprayV,
    "io.spray"            %%  "spray-can"		    % sprayV,
    "io.spray"            %%  "spray-routing"		% sprayV,
    "com.typesafe.akka"   %%  "akka-actor"        	% akkaV,
    "io.spray"            %%  "spray-testkit" 		% sprayV   % "test",
    "com.typesafe.akka"   %%  "akka-testkit"  		% akkaV    % "test",
    "org.specs2"          %%  "specs2-core"   		% "2.3.11" % "test",
    "org.scalatest" 	  %%  "scalatest"   	  	% "3.0.1"  % "test",
    "junit" 			  %   "junit"		        % "4.12"   % "test",
    "org.mockito"		  %   "mockito-core" 		% "2.7.9"  % "test"
  )
}

Revolver.settings
