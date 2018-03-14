/**
 * Copyright 2017 Dishant Langayan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.solace.demo;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.SpringJCSMPFactory;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageProducer;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;

@Component
public class TwitterSolaceProducer {
    private static final Logger logger = LoggerFactory.getLogger(TwitterSolaceProducer.class);

    private BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(100000);

    private Client hosebirdClient = null;

    @Autowired
    private SpringJCSMPFactory solaceFactory;
    @Autowired
    private TwitterConfigProperties twitterProperties;
    @Autowired
    private SolaceTwitterStreamConfigProperties appProperties;

    private JCSMPSession session = null;
    private XMLMessageProducer producer = null;
    private Topic topic = null;
    private TextMessage solaceMsg = null;

    @PostConstruct
    public void activate() throws Exception {

        try {
            // Setup Twitter HBC
            setupHoseBirdClient();

            // Setup Solace
            setupSolacePubSub();

            // Connect Solace
            session.connect();

            // Connect HBC
            hosebirdClient.connect();

            logger.info("Solace Twitter Streaming has started");

            while (!hosebirdClient.isDone()) {
                String twitterMsg = msgQueue.take();

                // Reset the Solace msg as we are reusing it
                solaceMsg.reset();

                // Set payload
                solaceMsg.setText(twitterMsg);

                // Publish the msg
                producer.send(solaceMsg, topic);
                
                logger.debug("Msg sent: " + twitterMsg);
            }

        } catch (JCSMPException e) {
            logger.error("Error while setting up Solace PubSub", e);
        } catch (Exception e) {
            logger.error("Error while activating Solace Twitter Streaming", e);
        }
    }

    @PreDestroy
    public void deactivate() throws Exception {
        logger.info("Stopping Solace Twitter Streaming...");

        // Cleanup
        if (hosebirdClient != null) {
            hosebirdClient.stop();
        }

        if (session != null) {
            session.closeSession();
        }
        logger.info("Solace Twitter Streaming has stopped");
    }

    private void setupSolacePubSub() throws JCSMPException {
        logger.info("Setting up Solace PubSub...");

        // Solace session
        session = solaceFactory.createSession();

        // Producer
        producer = session.getMessageProducer(new SolacePubEventHandler());

        // Our topic destination does not change so we can just create it once here
        topic = JCSMPFactory.onlyInstance().createTopic(appProperties.getTopic());

        // Similarly we can reuse a Solace message so we just create it once here
        solaceMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);

        logger.info("Setting up Solace PubSub... Done");
    }

    private void setupHoseBirdClient() {
        logger.info("Setting up Twitter Hosebird Client...");
        /**
         * Declare the host you want to connect to, the endpoint, and authentication
         * (basic auth or oauth)
         */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
        // Optional: set up some followings and track terms
        //List<Long> followings = Lists.newArrayList(1234L, 566788L);
        List<String> terms = appProperties.getTrackTerms(); //Lists.newArrayList("twitter", "api");
        //hosebirdEndpoint.followings(followings);
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(twitterProperties.getConsumerKey(),
                twitterProperties.getConsumerSecret(), twitterProperties.getToken(), twitterProperties.getSecret());

        ClientBuilder builder = new ClientBuilder().name("Hosebird-Client-01") // optional: mainly for the logs
                .hosts(hosebirdHosts).authentication(hosebirdAuth).endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        hosebirdClient = builder.build();

        logger.info("Setting up Twitter Hosebird Client... Done");
    }
}
