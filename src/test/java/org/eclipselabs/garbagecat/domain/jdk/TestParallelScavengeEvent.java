/**********************************************************************************************************************
 * garbagecat                                                                                                         *
 *                                                                                                                    *
 * Copyright (c) 2008-2016 Red Hat, Inc.                                                                              *
 *                                                                                                                    * 
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse *
 * Public License v1.0 which accompanies this distribution, and is available at                                       *
 * http://www.eclipse.org/legal/epl-v10.html.                                                                         *
 *                                                                                                                    *
 * Contributors:                                                                                                      *
 *    Red Hat, Inc. - initial API and implementation                                                                  *
 *********************************************************************************************************************/
package org.eclipselabs.garbagecat.domain.jdk;

import org.eclipselabs.garbagecat.util.jdk.JdkRegEx;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
public class TestParallelScavengeEvent extends TestCase {

    public void testLogLine() {
        String logLine = "19810.091: [GC [PSYoungGen: 27808K->632K(28032K)] "
                + "160183K->133159K(585088K), 0.0225213 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + ".",
                ParallelScavengeEvent.match(logLine));
        ParallelScavengeEvent event = new ParallelScavengeEvent(logLine);
        Assert.assertEquals("Time stamp not parsed correctly.", 19810091, event.getTimestamp());
        Assert.assertEquals("Young begin size not parsed correctly.", 27808, event.getYoungOccupancyInit());
        Assert.assertEquals("Young end size not parsed correctly.", 632, event.getYoungOccupancyEnd());
        Assert.assertEquals("Young available size not parsed correctly.", 28032, event.getYoungSpace());
        Assert.assertEquals("Old begin size not parsed correctly.", 132375, event.getOldOccupancyInit());
        Assert.assertEquals("Old end size not parsed correctly.", 132527, event.getOldOccupancyEnd());
        Assert.assertEquals("Old allocation size not parsed correctly.", 557056, event.getOldSpace());
        Assert.assertEquals("Duration not parsed correctly.", 22, event.getDuration());
    }

    public void testLogLineWhitespaceAtEnd() {
        String logLine = "19810.091: [GC [PSYoungGen: 27808K->632K(28032K)] "
                + "160183K->133159K(585088K), 0.0225213 secs]     ";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + ".",
                ParallelScavengeEvent.match(logLine));
    }

    public void testStressedJvmLogLine() {
        String logLine = "14112.691: [GC-- [PSYoungGen: 313864K->313864K(326656K)] "
                + "879670K->1012935K(1025728K), 0.9561947 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + ".",
                ParallelScavengeEvent.match(logLine));
        ParallelScavengeEvent event = new ParallelScavengeEvent(logLine);
        Assert.assertEquals("Time stamp not parsed correctly.", 14112691, event.getTimestamp());
        Assert.assertEquals("Young begin size not parsed correctly.", 313864, event.getYoungOccupancyInit());
        Assert.assertEquals("Young end size not parsed correctly.", 313864, event.getYoungOccupancyEnd());
        Assert.assertEquals("Young available size not parsed correctly.", 326656, event.getYoungSpace());
        Assert.assertEquals("Old begin size not parsed correctly.", 565806, event.getOldOccupancyInit());
        Assert.assertEquals("Old end size not parsed correctly.", 699071, event.getOldOccupancyEnd());
        Assert.assertEquals("Old allocation size not parsed correctly.", 699072, event.getOldSpace());
        Assert.assertEquals("Duration not parsed correctly.", 956, event.getDuration());
    }

    public void testSizeWithNineTensPlacesLogLine() {
        String logLine = "1006.751: [GC [PSYoungGen: 61139904K->20643840K(67413056K)] "
                + "119561147K->80396669K(129092672K), 3.8993460 secs] [Times: user=66.40 sys=3.73, real=3.89 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + ".",
                ParallelScavengeEvent.match(logLine));
        ParallelScavengeEvent event = new ParallelScavengeEvent(logLine);
        Assert.assertEquals("Time stamp not parsed correctly.", 1006751, event.getTimestamp());
        Assert.assertEquals("Young begin size not parsed correctly.", 61139904, event.getYoungOccupancyInit());
        Assert.assertEquals("Young end size not parsed correctly.", 20643840, event.getYoungOccupancyEnd());
        Assert.assertEquals("Young available size not parsed correctly.", 67413056, event.getYoungSpace());
        Assert.assertEquals("Old begin size not parsed correctly.", 119561147 - 61139904, event.getOldOccupancyInit());
        Assert.assertEquals("Old end size not parsed correctly.", 80396669 - 20643840, event.getOldOccupancyEnd());
        Assert.assertEquals("Old allocation size not parsed correctly.", 129092672 - 67413056, event.getOldSpace());
        Assert.assertEquals("Duration not parsed correctly.", 3899, event.getDuration());
    }

    public void testJDK8LogLineWithMetatdataGcThreshholdTrigger() {
        String logLine = "1.219: [GC (Metadata GC Threshold) [PSYoungGen: 1226834K->17779K(1835008K)] "
                + "1226834K->17795K(6029312K), 0.0144911 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + ".",
                ParallelScavengeEvent.match(logLine));
        ParallelScavengeEvent event = new ParallelScavengeEvent(logLine);
        Assert.assertEquals("Time stamp not parsed correctly.", 1219, event.getTimestamp());
        Assert.assertTrue("Trigger not parsed correctly.",
                event.getTrigger().matches(JdkRegEx.TRIGGER_METADATA_GC_THRESHOLD));
        Assert.assertEquals("Young begin size not parsed correctly.", 1226834, event.getYoungOccupancyInit());
        Assert.assertEquals("Young end size not parsed correctly.", 17779, event.getYoungOccupancyEnd());
        Assert.assertEquals("Young available size not parsed correctly.", 1835008, event.getYoungSpace());
        Assert.assertEquals("Old begin size not parsed correctly.", 1226834 - 1226834, event.getOldOccupancyInit());
        Assert.assertEquals("Old end size not parsed correctly.", 17795 - 17779, event.getOldOccupancyEnd());
        Assert.assertEquals("Old allocation size not parsed correctly.", 6029312 - 1835008, event.getOldSpace());
        Assert.assertEquals("Duration not parsed correctly.", 14, event.getDuration());
    }

    public void testJDK8LogLineWithGcLockerInitiateGcTrigger() {
        String logLine = "4.172: [GC (GCLocker Initiated GC) [PSYoungGen: 649034K->114285K(1223168K)] "
                + "673650K->138909K(4019712K), 0.0711412 secs] [Times: user=0.24 sys=0.01, real=0.08 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + ".",
                ParallelScavengeEvent.match(logLine));
        ParallelScavengeEvent event = new ParallelScavengeEvent(logLine);
        Assert.assertEquals("Time stamp not parsed correctly.", 4172, event.getTimestamp());
        Assert.assertTrue("Trigger not parsed correctly.",
                event.getTrigger().matches(JdkRegEx.TRIGGER_GCLOCKER_INITIATED_GC));
        Assert.assertEquals("Young begin size not parsed correctly.", 649034, event.getYoungOccupancyInit());
        Assert.assertEquals("Young end size not parsed correctly.", 114285, event.getYoungOccupancyEnd());
        Assert.assertEquals("Young available size not parsed correctly.", 1223168, event.getYoungSpace());
        Assert.assertEquals("Old begin size not parsed correctly.", 673650 - 649034, event.getOldOccupancyInit());
        Assert.assertEquals("Old end size not parsed correctly.", 138909 - 114285, event.getOldOccupancyEnd());
        Assert.assertEquals("Old allocation size not parsed correctly.", 4019712 - 1223168, event.getOldSpace());
        Assert.assertEquals("Duration not parsed correctly.", 71, event.getDuration());
    }

    public void testJDK8LogLineWithAllocationFailureTrigger() {
        String logLine = "7.682: [GC (Allocation Failure) [PSYoungGen: 1048576K->131690K(1223168K)] "
                + "1118082K->201204K(4019712K), 0.0657426 secs] [Times: user=0.13 sys=0.00, real=0.07 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + ".",
                ParallelScavengeEvent.match(logLine));
        ParallelScavengeEvent event = new ParallelScavengeEvent(logLine);
        Assert.assertEquals("Time stamp not parsed correctly.", 7682, event.getTimestamp());
        Assert.assertTrue("Trigger not parsed correctly.",
                event.getTrigger().matches(JdkRegEx.TRIGGER_ALLOCATION_FAILURE));
        Assert.assertEquals("Young begin size not parsed correctly.", 1048576, event.getYoungOccupancyInit());
        Assert.assertEquals("Young end size not parsed correctly.", 131690, event.getYoungOccupancyEnd());
        Assert.assertEquals("Young available size not parsed correctly.", 1223168, event.getYoungSpace());
        Assert.assertEquals("Old begin size not parsed correctly.", 1118082 - 1048576, event.getOldOccupancyInit());
        Assert.assertEquals("Old end size not parsed correctly.", 201204 - 131690, event.getOldOccupancyEnd());
        Assert.assertEquals("Old allocation size not parsed correctly.", 4019712 - 1223168, event.getOldSpace());
        Assert.assertEquals("Duration not parsed correctly.", 65, event.getDuration());
    }
}
