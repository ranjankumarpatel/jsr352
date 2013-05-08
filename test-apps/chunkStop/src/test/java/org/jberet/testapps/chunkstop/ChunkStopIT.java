/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jberet.testapps.chunkstop;

import java.util.List;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

import junit.framework.Assert;
import org.jberet.runtime.metric.MetricImpl;
import org.jberet.testapps.common.AbstractIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ChunkStopIT extends AbstractIT {
    protected int dataCount = 30;
    protected static final String jobXml = "chunkStop.xml";

    @Before
    public void before() {
        params.setProperty("data.count", String.valueOf(dataCount));
    }

    @After
    public void after() {
        params.clear();
    }

    @Test
    public void chunkStopRestart() throws Exception {
        params.setProperty("writer.sleep.time", "500");
        startJob(jobXml);
        jobOperator.stop(jobExecutionId);
        awaitTermination(jobExecution);
        Assert.assertEquals(BatchStatus.STOPPED, jobExecution.getBatchStatus());

        List<StepExecution> stepExecutions = jobOperator.getStepExecutions(jobExecutionId);
        StepExecution stepExecution = stepExecutions.get(0);
        Assert.assertEquals(1, stepExecutions.size());
        //since we called stop right after start, and the writer sleeps before writing data, there should only be 1 write and commit
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution, Metric.MetricType.COMMIT_COUNT));

        restartAndWait();

        stepExecution = jobOperator.getStepExecutions(restartJobExecutionId).get(0);
        Assert.assertEquals(BatchStatus.COMPLETED, restartJobExecution.getBatchStatus());
        Assert.assertTrue(MetricImpl.getMetric(stepExecution, Metric.MetricType.READ_COUNT) < dataCount);
    }

    @Test
    public void chunkFailRestart() throws Exception {
        params.setProperty("reader.fail.at", "13");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        StepExecution stepExecution = jobOperator.getStepExecutions(jobExecutionId).get(0);
        Assert.assertEquals(13, MetricImpl.getMetric(stepExecution, Metric.MetricType.READ_COUNT));  //reader.fail.at is 0-based, reader.fail.at 13 means 13 successful read
        Assert.assertEquals(10, MetricImpl.getMetric(stepExecution, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution, Metric.MetricType.COMMIT_COUNT));

        params.setProperty("reader.fail.at", "3");
        restartAndWait();
        Assert.assertEquals(BatchStatus.COMPLETED, restartJobExecution.getBatchStatus());

        stepExecution = jobOperator.getStepExecutions(restartJobExecutionId).get(0);
        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(3, MetricImpl.getMetric(stepExecution, Metric.MetricType.COMMIT_COUNT));
    }

    @Test
    public void chunkWriterFailRestart() throws Exception {
        params.setProperty("writer.fail.at", "13");
        startJobAndWait(jobXml);
        Assert.assertEquals(BatchStatus.FAILED, jobExecution.getBatchStatus());

        StepExecution stepExecution = jobOperator.getStepExecutions(jobExecutionId).get(0);
        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(10, MetricImpl.getMetric(stepExecution, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(1, MetricImpl.getMetric(stepExecution, Metric.MetricType.COMMIT_COUNT));

        params.setProperty("writer.fail.at", "-1");
        restartAndWait();
        Assert.assertEquals(BatchStatus.COMPLETED, restartJobExecution.getBatchStatus());

        stepExecution = jobOperator.getStepExecutions(restartJobExecutionId).get(0);
        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution, Metric.MetricType.READ_COUNT));
        Assert.assertEquals(20, MetricImpl.getMetric(stepExecution, Metric.MetricType.WRITE_COUNT));
        Assert.assertEquals(3, MetricImpl.getMetric(stepExecution, Metric.MetricType.COMMIT_COUNT));
    }
}
