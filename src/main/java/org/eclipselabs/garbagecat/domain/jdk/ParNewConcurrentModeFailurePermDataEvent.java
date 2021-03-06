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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipselabs.garbagecat.domain.BlockingEvent;
import org.eclipselabs.garbagecat.domain.OldCollection;
import org.eclipselabs.garbagecat.domain.OldData;
import org.eclipselabs.garbagecat.domain.PermCollection;
import org.eclipselabs.garbagecat.domain.PermData;
import org.eclipselabs.garbagecat.domain.YoungData;
import org.eclipselabs.garbagecat.util.jdk.JdkMath;
import org.eclipselabs.garbagecat.util.jdk.JdkRegEx;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;

/**
 * <p>
 * PAR_NEW_CONCURRENT_MODE_FAILURE_PERM_DATA
 * </p>
 * 
 * <p>
 * A {@link org.eclipselabs.garbagecat.domain.jdk.ParNewEvent} ending with a concurrent mode failure with permanent
 * generation data.
 * </p>
 * 
 * A concurrent mode failure event indicates that the concurrent collection of the old generation did not finish before
 * the old generation became full. The JVM initiates a full GC in an attempt to free up space using the
 * <code>CmsSerialOldEvent</code> old generation collector.
 * 
 * <p>
 * The concurrent low pause collector measures the rate at which the the old generation is filling and the amount of
 * time between collections and uses this historical data to calculate when to start the concurrent collection (plus
 * adds some padding) so that it will finish just in time before the old generation becomes full.
 * </p>
 * 
 * <p>
 * This happens because there is not enough space in the old generation to support the rate of promotion from the young
 * generation. Possible causes:
 * </p>
 * <ol>
 * <li>The heap is too small.</li>
 * <li>There is a change in application behavior (e.g. a load increase) that causes the young promotion rate to exceed
 * historical data. If this is the case, the concurrent mode failures will happen near the change in behavior, then
 * after a few collections the CMS collector will adjust based on the new promotion rate. Performance will suffer for a
 * short period until the CMS collector recalibrates. The <code>-XX:CMSInitiatingOccupancyFraction=NN</code> (default
 * 92) JVM option can be used to handle changes in application behavior; however, the tradeoff is that there will be
 * more collections.</li>
 * <li>The application has large variances in object allocation rates, causing large variances in young generation
 * promotion rates, leading to the CMS collector not being able to accurately predict the time between collections. The
 * <code>-XX:CMSIncrementalSafetyFactor=NN</code> (default 10) JVM option can be used to start the concurrent collection
 * NN% sooner than the calculated time.</li>
 * <li>There is premature promotion from the young to the old generation, causing the old generation to fill up with
 * short-lived objects. The default value for <code>-XX:MaxTenuringThreshold</code> for the CMS collector is 0, meaning
 * that objects surviving a young collection are immediately promoted to the old generation. Add the following JVM
 * option to allow more time for objects to expire in the young generation: <code>-XX:MaxTenuringThreshold=32</code>.
 * </li>
 * <li>If the old generation has available space, the cause is likely fragmentation. Fragmentation can be avoided by
 * increasing the heap size.</li>
 * </ol>
 * 
 * <h3>Example Logging</h3>
 * 
 * <p>
 * 1) {@link org.eclipselabs.garbagecat.domain.jdk.ParNewConcurrentModeFailurePermDataEvent} combined with
 * {@link org.eclipselabs.garbagecat.domain.jdk.CmsConcurrentEvent} across 2 lines:
 * </p>
 * 
 * <pre>
 * 3070.289: [GC 3070.289: [ParNew: 207744K-&gt;207744K(242304K), 0.0000682 secs]3070.289: [CMS3081.621: [CMS-concurrent-mark: 11.907/12.958 secs] [Times: user=45.31 sys=3.93, real=12.96 secs]
 *  (concurrent mode failure): 6010121K-&gt;6014591K(6014592K), 79.0505229 secs] 6217865K-&gt;6028029K(6256896K), [CMS Perm : 206688K-&gt;206662K(262144K)], 79.0509595 secs] [Times: user=104.69 sys=3.63, real=79.05 secs]
 * </pre>
 * 
 * <p>
 * Preprocessed:
 * </p>
 * 
 * <pre>
 * 3070.289: [GC 3070.289: [ParNew: 207744K-&gt;207744K(242304K), 0.0000682 secs]3070.289: [CMS (concurrent mode failure): 6010121K-&gt;6014591K(6014592K), 79.0505229 secs] 6217865K-&gt;6028029K(6256896K), [CMS Perm : 206688K-&gt;206662K(262144K)], 79.0509595 secs] [Times: user=104.69 sys=3.63, real=79.05 secs]
 * 3081.621: [CMS-concurrent-mark: 11.907/12.958 secs] [Times: user=45.31 sys=3.93, real=12.96 secs]
 * </pre>
 * 
 * <p>
 * 2) JDK8 with Metaspace:
 * </p>
 * 
 * <pre>
 * 1202.526: [GC (Allocation Failure) 1202.528: [ParNew: 1355422K-&gt;1355422K(1382400K), 0.0000500 secs]1202.528: [CMS1203.491: [CMS-concurrent-sweep: 1.009/1.060 secs] [Times: user=1.55 sys=0.12, real=1.06 secs]
 *  (concurrent mode failure): 2656311K-&gt;2658289K(2658304K), 9.3575580 secs] 4011734K-&gt;2725109K(4040704K), [Metaspace: 72111K-&gt;72111K(1118208K)], 9.3610080 secs] [Times: user=9.35 sys=0.01, real=9.36 secs]
 * </pre>
 * 
 * <p>
 * Preprocessed:
 * </p>
 * 
 * <pre>
 * 1202.526: [GC (Allocation Failure) 1202.528: [ParNew: 1355422K-&gt;1355422K(1382400K), 0.0000500 secs]1202.528: [CMS (concurrent mode failure): 2656311K-&gt;2658289K(2658304K), 9.3575580 secs] 4011734K-&gt;2725109K(4040704K), [Metaspace: 72111K-&gt;72111K(1118208K)], 9.3610080 secs] [Times: user=9.35 sys=0.01, real=9.36 secs]
 * 1203.491: [CMS-concurrent-sweep: 1.009/1.060 secs] [Times: user=1.55 sys=0.12, real=1.06 secs]
 * </pre>
 * 
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * @author jborelo
 */
public class ParNewConcurrentModeFailurePermDataEvent
        implements BlockingEvent, OldCollection, PermCollection, YoungData, OldData, PermData, CmsCollection {

    /**
     * Regular expressions defining the logging.
     */
    private static final String REGEX = "^" + JdkRegEx.TIMESTAMP + ": \\[GC( \\(" + JdkRegEx.TRIGGER_ALLOCATION_FAILURE
            + "\\))? " + JdkRegEx.TIMESTAMP + ": \\[ParNew( \\(" + JdkRegEx.TRIGGER_PROMOTION_FAILED + "\\))?: "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\]("
            + ClassHistogramEvent.REGEX_PREPROCESSED + ")?(" + JdkRegEx.TIMESTAMP + ": \\[CMS)?( \\("
            + JdkRegEx.TRIGGER_CONCURRENT_MODE_FAILURE + "\\))?: " + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\("
            + JdkRegEx.SIZE + "\\), " + JdkRegEx.DURATION + "\\](" + ClassHistogramEvent.REGEX_PREPROCESSED + ")? "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\), \\[(CMS Perm |Metaspace): "
            + JdkRegEx.SIZE + "->" + JdkRegEx.SIZE + "\\(" + JdkRegEx.SIZE + "\\)\\]" + JdkRegEx.ICMS_DC_BLOCK + "?, "
            + JdkRegEx.DURATION + "\\]" + JdkRegEx.TIMES_BLOCK + "?[ ]*$";

    private static Pattern pattern = Pattern.compile(REGEX);

    /**
     * The log entry for the event. Can be used for debugging purposes.
     */
    private String logEntry;

    /**
     * The elapsed clock time for the GC event in milliseconds (rounded).
     */
    private int duration;

    /**
     * The time when the GC event happened in milliseconds after JVM startup.
     */
    private long timestamp;

    /**
     * Young generation size (kilobytes) at beginning of GC event.
     */
    private int young;

    /**
     * Young generation size (kilobytes) at end of GC event.
     */
    private int youngEnd;

    /**
     * Available space in young generation (kilobytes). Equals young generation allocation minus one survivor space.
     */
    private int youngAvailable;

    /**
     * Old generation size (kilobytes) at beginning of GC event.
     */
    private int old;

    /**
     * Old generation size (kilobytes) at end of GC event.
     */
    private int oldEnd;

    /**
     * Space allocated to old generation (kilobytes).
     */
    private int oldAllocation;

    /**
     * Permanent generation size (kilobytes) at beginning of GC event.
     */
    private int permGen;

    /**
     * Permanent generation size (kilobytes) at end of GC event.
     */
    private int permGenEnd;

    /**
     * Space allocated to permanent generation (kilobytes).
     */
    private int permGenAllocation;

    /**
     * Create event from log entry.
     * 
     * @param logEntry
     *            The log entry for the event.
     */
    public ParNewConcurrentModeFailurePermDataEvent(String logEntry) {
        this.logEntry = logEntry;
        Matcher matcher = pattern.matcher(logEntry);
        if (matcher.find()) {
            timestamp = JdkMath.convertSecsToMillis(matcher.group(1)).longValue();
            old = Integer.parseInt(matcher.group(16));
            oldEnd = Integer.parseInt(matcher.group(17));
            oldAllocation = Integer.parseInt(matcher.group(18));
            int totalBegin = Integer.parseInt(matcher.group(24));
            young = totalBegin - old;
            int totalEnd = Integer.parseInt(matcher.group(25));
            youngEnd = totalEnd - oldEnd;
            int totalAllocation = Integer.parseInt(matcher.group(26));
            youngAvailable = totalAllocation - oldAllocation;
            permGen = Integer.parseInt(matcher.group(28));
            permGenEnd = Integer.parseInt(matcher.group(29));
            permGenAllocation = Integer.parseInt(matcher.group(30));
            duration = JdkMath.convertSecsToMillis(matcher.group(32)).intValue();
        }
    }

    /**
     * Alternate constructor. Create ParNew detail logging event from values.
     * 
     * @param logEntry
     *            The log entry for the event.
     * @param timestamp
     *            The time when the GC event happened in milliseconds after JVM startup.
     * @param duration
     *            The elapsed clock time for the GC event in milliseconds.
     */
    public ParNewConcurrentModeFailurePermDataEvent(String logEntry, long timestamp, int duration) {
        this.logEntry = logEntry;
        this.timestamp = timestamp;
        this.duration = duration;
    }

    public String getLogEntry() {
        return logEntry;
    }

    public int getDuration() {
        return duration;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getYoungOccupancyInit() {
        return young;
    }

    public int getYoungOccupancyEnd() {
        return youngEnd;
    }

    public int getYoungSpace() {
        return youngAvailable;
    }

    public int getOldOccupancyInit() {
        return old;
    }

    public int getOldOccupancyEnd() {
        return oldEnd;
    }

    public int getOldSpace() {
        return oldAllocation;
    }

    public int getPermOccupancyInit() {
        return permGen;
    }

    public int getPermOccupancyEnd() {
        return permGenEnd;
    }

    public int getPermSpace() {
        return permGenAllocation;
    }

    public String getName() {
        return JdkUtil.LogEventType.PAR_NEW_CONCURRENT_MODE_FAILURE_PERM_DATA.toString();
    }

    /**
     * Determine if the logLine matches the logging pattern(s) for this event.
     * 
     * @param logLine
     *            The log line to test.
     * @return true if the log line matches the event pattern, false otherwise.
     */
    public static final boolean match(String logLine) {
        return pattern.matcher(logLine).matches();
    }
}
