![alt text](https://travis-ci.org/dishantlangayan/solace-twitter-stream.svg?branch=master "Build Status")

# Streaming Twitter Data using Solace PubSub Messaging

## Overview

The solace-twitter-stream application uses the Twitter Streaming API and the 
[Hosebird Client](https://github.com/twitter/hbc) to stream real-time tweets
based on configurable search terms, and publishes them to Solace PubSub+ 
messaging.

The application is built using the Spring Boot Actuactor starter project, and
use Solace's [Spring Boot Auto-Configuration for the Solace Java API](https://github.com/SolaceProducts/solace-java-spring-boot) 
to publish the Twitter stream to Solace PubSub.

You can also filter for specific terms in the Twitter stream.

The stream is published to Solace on the following topic destination:

```
solace/twitter/stream
```

## Usage

To start the solace-twitter-stream app:

	1. Extract the ZIP/tar distribution release package
	2. Copy the sample configuration file in the config folder and rename it 
		to application.yml
	3. Update the configuration to point to your Solace PubSub+ message 
		broker and use your Twitter access keys
	4. From the command line run the following:
		java -jar solace-twitter-stream-0.1.0.jar


## Pivotal Cloud Foundry Deployment

The solace-twitter-stream is a Spring Boot app and is cloud ready. You can also
deploy it to a PCF environment and set the configuration via environment variables.

You need to download the source code first. Either clone this repo, or download 
the ZIP/tar distribution from the Releases. Once extracted, open a command line 
window and navigate to the folder.


To push the app to PCF:

```
# Login to your PCF Org
cf login -a <API_ENDPOINT_URL> -o <ORG_NAME> -u <USERNAME> -p <PASSWORD>

# Compile the app
./gradlew clean assemble

# Push the app
cf push solace-twitter-stream -p build/libs/solace-twitter-stream-0.1.0.jar
```

You will all need to set the configuration through environment variables to 
connect to your twitter account.

```
# For Solace PubSub API to auto-reconnect
cf set-env solace-twitter-stream SOLACE_JAVA_CONNECTRETRIES 1
cf set-env solace-twitter-stream SOLACE_JAVA_RECONNECTRETRIES 5
cf set-env solace-twitter-stream SOLACE_JAVA_CONNECTRETRIESPERHOST 20
cf set-env solace-twitter-stream SOLACE_JAVA_RECONNECTRETRYWAITINMILLIS 3000

# Your Twitter Access Keys
cf set-env solace-twitter-stream TWITTER_CONSUMERKEY <YOUR_CONSUMER_KEY>
cf set-env solace-twitter-stream TWITTER_CONSUMERSECRET <YOUR_CONSUMER_SECRET>
cf set-env solace-twitter-stream TWITTER_TOKEN <YOUR_TWITTER_APP_TOKEN>
cf set-env solace-twitter-stream TWITTER_SECRET <YOUR_TWITTER_TOKEN_SECRET>

# Customizations for the app
cf set-env solace-twitter-stream SOLACESTREAM_TOPIC 'solace/twitter/stream'
cf set-env solace-twitter-stream SOLACESTREAM_TRACKTERMS_0_ twitter
cf set-env solace-twitter-stream SOLACESTREAM_TRACKTERMS_1_ api
```

## Logging

The app uses SLF4J logging framework, so any supported implementation, like 
LogBack or Log4J can be used to redirect logs to a file.

## Health Checks

Spring Boot 2 come with a default set of actuator endpoints that can be used to monitor the app. All endpoints are exposed on port 8080.

So you can try for example:

```
http://127.0.0.1/actuators/health
```

For actuator endpoints and configuration refer to: https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html

## Distribution Package

To generate a binary distribution package run:

```
./gradlew clean fatJarZip
```

## Support

For any support, bugs, and enhancements, please contact:

Dishant Langayan <dishantlangayan@gmail.com>

## License

Copyright 2018 Dishant Langayan

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
