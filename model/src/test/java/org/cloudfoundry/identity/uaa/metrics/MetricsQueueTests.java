/*
 * ****************************************************************************
 *     Cloud Foundry
 *     Copyright (c) [2009-2017] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 * ****************************************************************************
 */

package org.cloudfoundry.identity.uaa.metrics;

import com.fasterxml.jackson.core.type.TypeReference;
import org.cloudfoundry.identity.uaa.util.JsonUtils;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class MetricsQueueTests {

    private MetricsQueue queue;

    @Before
    public void setup() throws Exception {
        queue = new MetricsQueue();
        RequestMetric metric = RequestMetric.start("uri",0);
        metric.addQuery(new QueryMetric("query1", 0, 2, true));
        metric.stop(200, 2);
        queue.offer(metric);
        metric = RequestMetric.start("uri",0);
        metric.addQuery(new QueryMetric("query1", 0, 5, true));
        metric.stop(200, MetricsQueue.MAX_TIME+1);
        queue.offer(metric);
        metric = RequestMetric.start("uri",0);
        metric.addQuery(new QueryMetric("query1", 0, 2, false));
        metric.stop(500, 5);
        queue.offer(metric);
    }

    @Test
    public void summary() throws Exception {
        validateMetricsQueue(queue);
    }

    @Test
    public void totals() throws Exception {
        RequestMetricSummary summary = queue.getTotals();
        assertNotNull(summary);
        assertEquals(3, summary.getCount());
        assertEquals(1, summary.getIntolerableCount());
        assertEquals(MetricsQueue.MAX_TIME+3+5, summary.getTotalTime());
        assertEquals(MetricsQueue.MAX_TIME+1, summary.getIntolerableTime());
        assertEquals(3, summary.getDatabaseQueryCount());
        assertEquals(9, summary.getDatabaseQueryTime());
        assertEquals(1, summary.getDatabaseFailedQueryCount());
        assertEquals(2, summary.getDatabaseFailedQueryTime());

    }

    public void validateMetricsQueue(MetricsQueue queue) {
        Map<Integer, RequestMetricSummary> summary = queue.getSummary();
        assertNotNull(summary);
        assertEquals(2, summary.size());
        RequestMetricSummary twoHundredResponses = summary.get(200);
        assertNotNull(twoHundredResponses);
        assertEquals(2, twoHundredResponses.getCount());
        assertEquals(1, twoHundredResponses.getIntolerableCount());
        assertEquals(MetricsQueue.MAX_TIME+3, twoHundredResponses.getTotalTime());
        assertEquals(MetricsQueue.MAX_TIME+1, twoHundredResponses.getIntolerableTime());
        assertEquals(2, twoHundredResponses.getDatabaseQueryCount());
        assertEquals(7, twoHundredResponses.getDatabaseQueryTime());

        RequestMetricSummary fiveHundredResponses = summary.get(500);
        assertNotNull(fiveHundredResponses);
        assertEquals(1, fiveHundredResponses.getCount());
        assertEquals(0, fiveHundredResponses.getIntolerableCount());
        assertEquals(5, fiveHundredResponses.getTotalTime());
        assertEquals(0, fiveHundredResponses.getIntolerableTime());
        assertEquals(1, fiveHundredResponses.getDatabaseQueryCount());
        assertEquals(2, fiveHundredResponses.getDatabaseQueryTime());
        assertEquals(1, fiveHundredResponses.getDatabaseFailedQueryCount());
        assertEquals(2, fiveHundredResponses.getDatabaseFailedQueryTime());

        assertEquals(3, queue.getLastRequests().size());
    }

    @Test
    public void json_serialize() throws Exception {
        String json = JsonUtils.writeValueAsString(queue);
        Map<String,Object> object = JsonUtils.readValue(json, new TypeReference<Map<String, Object>>() {});
        assertEquals(2, object.size());
        MetricsQueue deserialized = JsonUtils.readValue(json, MetricsQueue.class);
        validateMetricsQueue(deserialized);
    }

    @Test
    public void overflow_limit_respected() throws Exception {
        RequestMetric metric = RequestMetric.start("uri",0);
        metric.addQuery(new QueryMetric("query1", 0, 2, true));
        metric.stop(200, 2);
        Runnable add10Metrics = () -> {
            for (int i=0; i<10; i++) {
                queue.offer(metric);
            }
        };
        Thread[] threads = new Thread[5];
        for (int i=0; i<threads.length; i++) {
            threads[i] = new Thread(add10Metrics);
        }
        for (int i=0; i<threads.length; i++) {
            threads[i].start();
        }
        for (int i=0; i<threads.length; i++) {
            threads[i].join();
        }
        assertThat(queue.getLastRequests().size(), Matchers.lessThanOrEqualTo(MetricsQueue.MAX_ENTRIES));

    }

}