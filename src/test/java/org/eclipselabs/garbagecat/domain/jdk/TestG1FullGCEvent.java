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
 * @author James Livingston
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 */
public class TestG1FullGCEvent extends TestCase {

    public void testLogLineTriggerSystemGC() {
        String logLine = "1302.524: [Full GC (System.gc()) 653M->586M(979M), 1.6364900 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.G1_FULL_GC.toString() + ".",
                G1FullGCEvent.match(logLine));
        G1FullGCEvent event = new G1FullGCEvent(logLine);
        Assert.assertTrue("Trigger not parsed correctly.", event.getTrigger().matches(JdkRegEx.TRIGGER_SYSTEM_GC));
        Assert.assertEquals("Time stamp not parsed correctly.", 1302524, event.getTimestamp());
        Assert.assertEquals("Combined begin size not parsed correctly.", 653 * 1024, event.getCombinedOccupancyInit());
        Assert.assertEquals("Combined end size not parsed correctly.", 586 * 1024, event.getCombinedOccupancyEnd());
        Assert.assertEquals("Combined available size not parsed correctly.", 979 * 1024, event.getCombinedSpace());
        Assert.assertEquals("Perm gen begin size not parsed correctly.", 0, event.getPermOccupancyInit());
        Assert.assertEquals("Perm gen end size not parsed correctly.", 0, event.getPermOccupancyEnd());
        Assert.assertEquals("Perm gen allocation size not parsed correctly.", 0, event.getPermSpace());
        Assert.assertEquals("Duration not parsed correctly.", 1636, event.getDuration());
    }

    public void testLogLinePreprocessedDetailsTriggerToSpaceExhausted() {
        String logLine = "105.151: [Full GC (System.gc()) 5820M->1381M(30G), 5.5390169 secs]"
                + "[Eden: 80.0M(112.0M)->0.0B(128.0M) Survivors: 16.0M->0.0B Heap: 5820.3M(30.0G)->1381.9M(30.0G)]"
                + " [Times: user=5.76 sys=1.00, real=5.53 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.G1_FULL_GC.toString() + ".",
                G1FullGCEvent.match(logLine));
        G1FullGCEvent event = new G1FullGCEvent(logLine);
        Assert.assertTrue("Trigger not parsed correctly.", event.getTrigger().matches(JdkRegEx.TRIGGER_SYSTEM_GC));
        Assert.assertEquals("Time stamp not parsed correctly.", 105151, event.getTimestamp());
        Assert.assertEquals("Combined begin size not parsed correctly.", 5820 * 1024, event.getCombinedOccupancyInit());
        Assert.assertEquals("Combined end size not parsed correctly.", 1381 * 1024, event.getCombinedOccupancyEnd());
        Assert.assertEquals("Combined available size not parsed correctly.", 30 * 1024 * 1024,
                event.getCombinedSpace());
        Assert.assertEquals("Perm gen begin size not parsed correctly.", 0, event.getPermOccupancyInit());
        Assert.assertEquals("Perm gen end size not parsed correctly.", 0, event.getPermOccupancyEnd());
        Assert.assertEquals("Perm gen allocation size not parsed correctly.", 0, event.getPermSpace());
        Assert.assertEquals("Duration not parsed correctly.", 5539, event.getDuration());
    }

    public void testLogLinePreprocessedDetailsNoTriggerPerm() {
        String logLine = "178.892: [Full GC 999M->691M(3072M), 3.4262061 secs]"
                + "[Eden: 143.0M(1624.0M)->0.0B(1843.0M) Survivors: 219.0M->0.0B "
                + "Heap: 999.5M(3072.0M)->691.1M(3072.0M)], [Perm: 175031K->175031K(175104K)]"
                + " [Times: user=4.43 sys=0.05, real=3.44 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.G1_FULL_GC.toString() + ".",
                G1FullGCEvent.match(logLine));
        G1FullGCEvent event = new G1FullGCEvent(logLine);
        Assert.assertTrue("Trigger not parsed correctly.", event.getTrigger() == null);
        Assert.assertEquals("Time stamp not parsed correctly.", 178892, event.getTimestamp());
        Assert.assertEquals("Combined begin size not parsed correctly.", 999 * 1024, event.getCombinedOccupancyInit());
        Assert.assertEquals("Combined end size not parsed correctly.", 691 * 1024, event.getCombinedOccupancyEnd());
        Assert.assertEquals("Combined available size not parsed correctly.", 3072 * 1024, event.getCombinedSpace());
        Assert.assertEquals("Perm gen begin size not parsed correctly.", 175031, event.getPermOccupancyInit());
        Assert.assertEquals("Perm gen end size not parsed correctly.", 175031, event.getPermOccupancyEnd());
        Assert.assertEquals("Perm gen allocation size not parsed correctly.", 175104, event.getPermSpace());
        Assert.assertEquals("Duration not parsed correctly.", 3426, event.getDuration());
    }

    public void testLogLinePreprocessedDetailsTriggerMetadatGcThresholdMetaspace() {
        String logLine = "188.123: [Full GC (Metadata GC Threshold) 1831M->1213M(5120M), 5.1353878 secs]"
                + "[Eden: 0.0B(1522.0M)->0.0B(2758.0M) Survivors: 244.0M->0.0B "
                + "Heap: 1831.0M(5120.0M)->1213.5M(5120.0M)], [Metaspace: 396834K->324903K(1511424K)]"
                + " [Times: user=7.15 sys=0.04, real=5.14 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.G1_FULL_GC.toString() + ".",
                G1FullGCEvent.match(logLine));
        G1FullGCEvent event = new G1FullGCEvent(logLine);
        Assert.assertEquals("Trigger not parsed correctly.", JdkRegEx.TRIGGER_METADATA_GC_THRESHOLD,
                event.getTrigger());
        Assert.assertEquals("Time stamp not parsed correctly.", 188123, event.getTimestamp());
        Assert.assertEquals("Combined begin size not parsed correctly.", 1831 * 1024, event.getCombinedOccupancyInit());
        Assert.assertEquals("Combined end size not parsed correctly.", 1213 * 1024, event.getCombinedOccupancyEnd());
        Assert.assertEquals("Combined available size not parsed correctly.", 5120 * 1024, event.getCombinedSpace());
        Assert.assertEquals("Perm gen begin size not parsed correctly.", 396834, event.getPermOccupancyInit());
        Assert.assertEquals("Perm gen end size not parsed correctly.", 324903, event.getPermOccupancyEnd());
        Assert.assertEquals("Perm gen allocation size not parsed correctly.", 1511424, event.getPermSpace());
        Assert.assertEquals("Duration not parsed correctly.", 5135, event.getDuration());
    }

    public void testLogLinePreprocessedDetailsTriggerLastDitchCollection2SpacesAfterTrigger() {
        String logLine = "98.150: [Full GC (Last ditch collection)  1196M->1118M(5120M), 4.4628626 secs]"
                + "[Eden: 0.0B(3072.0M)->0.0B(3072.0M) Survivors: 0.0B->0.0B "
                + "Heap: 1196.3M(5120.0M)->1118.8M(5120.0M)], [Metaspace: 324984K->323866K(1511424K)] "
                + "[Times: user=6.37 sys=0.00, real=4.46 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.G1_FULL_GC.toString() + ".",
                G1FullGCEvent.match(logLine));
        G1FullGCEvent event = new G1FullGCEvent(logLine);
        Assert.assertEquals("Trigger not parsed correctly.", JdkRegEx.TRIGGER_LAST_DITCH_COLLECTION,
                event.getTrigger());
        Assert.assertEquals("Time stamp not parsed correctly.", 98150, event.getTimestamp());
        Assert.assertEquals("Combined begin size not parsed correctly.", 1196 * 1024, event.getCombinedOccupancyInit());
        Assert.assertEquals("Combined end size not parsed correctly.", 1118 * 1024, event.getCombinedOccupancyEnd());
        Assert.assertEquals("Combined available size not parsed correctly.", 5120 * 1024, event.getCombinedSpace());
        Assert.assertEquals("Perm gen begin size not parsed correctly.", 324984, event.getPermOccupancyInit());
        Assert.assertEquals("Perm gen end size not parsed correctly.", 323866, event.getPermOccupancyEnd());
        Assert.assertEquals("Perm gen allocation size not parsed correctly.", 1511424, event.getPermSpace());
        Assert.assertEquals("Duration not parsed correctly.", 4462, event.getDuration());
    }

    public void testLogLinePreprocessedDetailsTriggerJvmTi() {
        String logLine = "102.621: [Full GC (JvmtiEnv ForceGarbageCollection)  1124M->1118M(5120M), 3.8954775 secs]"
                + "[Eden: 6144.0K(3072.0M)->0.0B(3072.0M) Survivors: 0.0B->0.0B "
                + "Heap: 1124.8M(5120.0M)->1118.9M(5120.0M)], [Metaspace: 323874K->323874K(1511424K)]"
                + " [Times: user=5.87 sys=0.01, real=3.89 secs]";
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.G1_FULL_GC.toString() + ".",
                G1FullGCEvent.match(logLine));
        G1FullGCEvent event = new G1FullGCEvent(logLine);
        Assert.assertEquals("Trigger not parsed correctly.", JdkRegEx.TRIGGER_JVM_TI_FORCED_GAREBAGE_COLLECTION,
                event.getTrigger());
        Assert.assertEquals("Time stamp not parsed correctly.", 102621, event.getTimestamp());
        Assert.assertEquals("Combined begin size not parsed correctly.", 1124 * 1024, event.getCombinedOccupancyInit());
        Assert.assertEquals("Combined end size not parsed correctly.", 1118 * 1024, event.getCombinedOccupancyEnd());
        Assert.assertEquals("Combined available size not parsed correctly.", 5120 * 1024, event.getCombinedSpace());
        Assert.assertEquals("Perm gen begin size not parsed correctly.", 323874, event.getPermOccupancyInit());
        Assert.assertEquals("Perm gen end size not parsed correctly.", 323874, event.getPermOccupancyEnd());
        Assert.assertEquals("Perm gen allocation size not parsed correctly.", 1511424, event.getPermSpace());
        Assert.assertEquals("Duration not parsed correctly.", 3895, event.getDuration());
    }
}
