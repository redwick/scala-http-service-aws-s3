## Scala Http Service sample using Pekko/Akka Framework with AWS S3 Integration
- ### AWS S3 Features
- - #### Upload File
- - #### Download File
- - #### Share File

- ### Also includes
- - #### Server routes for complete File Cloud
- - - ##### Directory `Create/Rename/Delete`
- - - ##### File `Create/Rename/Delete`
- - - ##### Files `List/Search`
- - #### PostgreSQL HikariCP Slick
- - #### Json Circe.IO


## Installation and running

>Compile and run this project using `sbt run`

## Docker Building

>This project contains file `project/plugins.sbt` which installs native plugin to build docker image

>Modify docker name in `build.sbt`

>Build docker with `sbt docker:publishLocal` or `sbt docker:publish` to deploy it to your local or remote machine