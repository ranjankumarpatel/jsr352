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

package org.jberet.testapps.loopback;

import org.junit.Test;
import org.jberet.testapps.common.AbstractIT;

/**
 * Verifies step loopbacks are detected and failed.
 */
public class LoopbackIT extends AbstractIT {
    public LoopbackIT() {
        params.setProperty("job-param", "job-param");
    }

    /**
     * step1's next attribute is itself.
     */
    @Test
    public void selfNextAttribute() throws Exception {
        startJobAndWait("self-next-attribute.xml");
    }

    /**
     * step1's next element points to itself.
     */
    @Test
    public void selfNextElement() throws Exception {
        startJobAndWait("self-next-element.xml");
    }

    /**
     * step1->step2->step3, transitioning with either next attribute or next element.
     */
    @Test
    public void loopbackAttributeElement() throws Exception {
        startJobAndWait("loopback-attribute-element.xml");
    }

    /**
     * same as loopbackAttributeElement, but within a flow, still a loopback error.
     */
    @Test
    public void loopbackInFlow() throws Exception {
        startJobAndWait("loopback-in-flow.xml");
    }

    /**
     * flow1 (step1 -> step2) => step1 is not loopback.  The job should run successfully.
     * @throws Exception
     */
    @Test
    public void notLoopbackAcrossFlow() throws Exception {
        startJobAndWait("not-loopback-across-flow.xml");
    }

    /**
     * flow1 (step1) => flow2 (step1 -> step2) => flow1 is a loopback at the last transition,
     * not at flow1.step1 -> flow2.step1.
     * @throws Exception
     */
    @Test
    public void loopbackFlowToFlow() throws Exception {
        startJobAndWait("loopback-flow-to-flow.xml");
    }

    /**
     * split1 (flow1 (step1) | flow2 (step2)) => self is a loopback.
     */
    @Test
    public void loopbackSplitSelf() throws Exception {
        startJobAndWait("loopback-split-self.xml");
    }

    /**
     * step0 => split1 (flow1 (step1) | flow2 (step2)) => step0 is a loopback.
     */
    @Test
    public void loopbackStepSplit() throws Exception {
        startJobAndWait("loopback-step-split.xml");
    }
}
