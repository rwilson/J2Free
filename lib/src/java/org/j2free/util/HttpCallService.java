/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * HttpCallService.java
 *
 * Copyright (c) 2009 FooBrew, Inc.
 */
package org.j2free.util;

import java.util.concurrent.PriorityBlockingQueue;
import org.apache.commons.httpclient.HttpClient;

/**
 *
 * @author ryan
 */
public class HttpCallService {

    private static final PriorityBlockingQueue<HttpCallFuture> queue = new PriorityBlockingQueue<HttpCallFuture>();

    private static final int HTTP_SOCKET_TIMEOUT = 30000;

    public static boolean enqueue(HttpCallFuture future) {
        return queue.offer(future);
    }

    private class HttpCallWorker {

        private final Log log;
        private final HttpClient client;
        private final Thread worker;

        private HttpCallWorker() {

            log    = LogFactory.getLog(getClass());
            client = new HttpClient();

            Runnable service = new Runnable() {
                public void run() {
                    try {

                        while (true)
                            executeCall(queue.take());

                    } catch (InterruptedException ie) {
                        log.debug("HttpCallWorker service interrupted, re-interrupting to exit...");
                        Thread.currentThread().interrupt();
                    }
                }
            };
            worker = new Thread(service);
            worker.start();
        }

        private String executeCall(HttpCallFuture future) {

            StringBuilder url = new StringBuilder();
            url.append(SERVER_URL)
               .append(method.toString());

            for (Pair param : params) {
                url.append("&" + param.getFirst() + "=" + param.getSecond());
            }

            if (method != Method.login)
                url.append("&apikey=" + apiKey);

            log.debug("Executing API call: " + url.toString());

            HttpMethodParams httpParams = new HttpMethodParams();
            httpParams.setSoTimeout(HTTP_SOCKET_TIMEOUT);

            GetMethod get = new GetMethod(url.toString());
            get.setParams(httpParams);

            String result;

            try {

                if (client == null)
                    client = new HttpClient();

                client.executeMethod(get);
                result = get.getResponseBodyAsString();

            } finally {
                get.releaseConnection();
            }

            log.debug("Received response: " + result);
            return result;
        }
    }
}
