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
package org.eclipselabs.garbagecat.domain;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipselabs.garbagecat.service.GcManager;
import org.eclipselabs.garbagecat.util.Constants;
import org.eclipselabs.garbagecat.util.jdk.Analysis;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil;
import org.eclipselabs.garbagecat.util.jdk.JdkUtil.LogEventType;
import org.eclipselabs.garbagecat.util.jdk.Jvm;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author <a href="mailto:mmillson@redhat.com">Mike Millson</a>
 * 
 */
public class TestJvmRun extends TestCase {

    public void testSummaryStatsParallel() {

        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset1.txt");
        GcManager jvmManager = new GcManager();
        jvmManager.store(testFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);

        Assert.assertEquals("Max heap space not calculated correctly.", 1034624, jvmRun.getMaxHeapSpace());
        Assert.assertEquals("Max heap occupancy not calculated correctly.", 1013058, jvmRun.getMaxHeapOccupancy());
        Assert.assertEquals("Max pause not calculated correctly.", 2782, jvmRun.getMaxGcPause());
        Assert.assertEquals("Max perm gen space not calculated correctly.", 159936, jvmRun.getMaxPermSpace());
        Assert.assertEquals("Max perm gen occupancy not calculated correctly.", 76972, jvmRun.getMaxPermOccupancy());
        Assert.assertEquals("Total GC duration not calculated correctly.", 5614, jvmRun.getTotalGcPause());
        Assert.assertEquals("GC Event count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.PARALLEL_SCAVENGE));
        Assert.assertTrue(JdkUtil.LogEventType.PARALLEL_SERIAL_OLD.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.PARALLEL_SERIAL_OLD));
        Assert.assertTrue(Analysis.KEY_APPLICATION_STOPPED_TIME_MISSING + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_APPLICATION_STOPPED_TIME_MISSING));
        Assert.assertTrue(Analysis.KEY_SERIAL_GC_THROUGHPUT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_SERIAL_GC_THROUGHPUT));
    }

    public void testSummaryStatsParNew() {

        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset2.txt");
        GcManager jvmManager = new GcManager();
        jvmManager.store(testFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);

        Assert.assertEquals("Max heap space not calculated correctly.", 1048256, jvmRun.getMaxHeapSpace());
        Assert.assertEquals("Max heap occupancy not calculated correctly.", 424192, jvmRun.getMaxHeapOccupancy());
        Assert.assertEquals("Max pause not calculated correctly.", 1070, jvmRun.getMaxGcPause());
        Assert.assertEquals("Max perm gen space not calculated correctly.", 99804, jvmRun.getMaxPermSpace());
        Assert.assertEquals("Max perm gen occupancy not calculated correctly.", 60155, jvmRun.getMaxPermOccupancy());
        Assert.assertEquals("Total GC duration not calculated correctly.", 1282, jvmRun.getTotalGcPause());
        Assert.assertEquals("GC Event count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(JdkUtil.LogEventType.PAR_NEW.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.PAR_NEW));
        Assert.assertTrue(JdkUtil.LogEventType.CMS_SERIAL_OLD.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.CMS_SERIAL_OLD));
        Assert.assertTrue(Analysis.KEY_SERIAL_GC_CMS + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_SERIAL_GC_CMS));
    }

    public void testLastTimestampNoEvents() {
        GcManager jvmManager = new GcManager();
        jvmManager.store(null, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Last timestamp not correct.", 0, jvmRun.getLastTimestamp());
    }

    /**
     * Test parsing logging with -XX:+PrintGCApplicationConcurrentTime and -XX:+PrintGCApplicationStoppedTime output.
     */
    public void testParseLoggingWithApplicationTime() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset3.txt");
        GcManager jvmManager = new GcManager();
        jvmManager.store(testFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event count not correct.", 3, jvmRun.getEventTypes().size());
        Assert.assertFalse(JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertEquals("Should not be any unidentified log lines.", 0, jvmRun.getUnidentifiedLogLines().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
        Assert.assertTrue(
                "Log line not recognized as " + JdkUtil.LogEventType.APPLICATION_STOPPED_TIME.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.APPLICATION_STOPPED_TIME));
        Assert.assertTrue(
                "Log line not recognized as " + JdkUtil.LogEventType.APPLICATION_CONCURRENT_TIME.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.APPLICATION_CONCURRENT_TIME));
        Assert.assertTrue(Analysis.KEY_PRINT_GC_APPLICATION_CONCURRENT_TIME + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_GC_APPLICATION_CONCURRENT_TIME));
    }

    /**
     * Test preprocessing a split <code>ParNewPromotionFailedCmsConcurrentModeFailureEvent</code>.
     */
    public void testSplitParNewPromotionFailedCmsConcurrentModeFailure() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset5.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 3, jvmRun.getEventTypes().size());
        Assert.assertFalse(JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue(
                "Log line not recognized as "
                        + JdkUtil.LogEventType.PAR_NEW_PROMOTION_FAILED_CMS_CONCURRENT_MODE_FAILURE.toString() + ".",
                jvmRun.getEventTypes()
                        .contains(JdkUtil.LogEventType.PAR_NEW_PROMOTION_FAILED_CMS_CONCURRENT_MODE_FAILURE));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_CONCURRENT));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_INITIAL_MARK.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_INITIAL_MARK));
        Assert.assertTrue(Analysis.KEY_FIRST_TIMESTAMP_THRESHOLD_EXCEEDED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_FIRST_TIMESTAMP_THRESHOLD_EXCEEDED));
    }

    /**
     * Test preprocessing <code>GcTimeLimitExceededEvent</code> with underlying <code>ParallelOldCompactingEvent</code>
     * .
     */
    public void testSplitParallelOldCompactingEventLogging() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset28.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_OLD_COMPACTING.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PARALLEL_OLD_COMPACTING));
        Assert.assertTrue(Analysis.KEY_GC_OVERHEAD_LIMIT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_GC_OVERHEAD_LIMIT));
        Assert.assertTrue(Analysis.KEY_GC_OVERHEAD_LIMIT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_GC_OVERHEAD_LIMIT));
    }

    /**
     * Test preprocessing a split <code>ParNewPromotionFailedCmsConcurrentModeFailurePermDataEvent</code>.
     */
    public void testSplitParNewPromotionFailedCmsConcurrentModeFailurePermData() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset12.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as "
                + JdkUtil.LogEventType.PAR_NEW_PROMOTION_FAILED_CMS_CONCURRENT_MODE_FAILURE_PERM_DATA.toString() + ".",
                jvmRun.getEventTypes()
                        .contains(JdkUtil.LogEventType.PAR_NEW_PROMOTION_FAILED_CMS_CONCURRENT_MODE_FAILURE_PERM_DATA));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test preprocessing a split <code>ParNewCmsConcurrentModeFailurePermDataEvent</code>.
     */
    public void testSplitParNewCmsConcurrentModeFailurePermData() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset13.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(
                "Log line not recognized as "
                        + JdkUtil.LogEventType.PAR_NEW_CONCURRENT_MODE_FAILURE_PERM_DATA.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW_CONCURRENT_MODE_FAILURE_PERM_DATA));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test preprocessing <code>ParNewPromotionFailedCmsConcurrentModeFailurePermDataEvent</code> split over 3 lines.
     */
    public void testSplit3LinesParNewPromotionFailedCmsConcurrentModeFailurePermDataEventMarkLogging() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset16.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertFalse(JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertTrue("Log line not recognized as "
                + JdkUtil.LogEventType.PAR_NEW_PROMOTION_FAILED_CMS_CONCURRENT_MODE_FAILURE_PERM_DATA.toString() + ".",
                jvmRun.getEventTypes()
                        .contains(JdkUtil.LogEventType.PAR_NEW_PROMOTION_FAILED_CMS_CONCURRENT_MODE_FAILURE_PERM_DATA));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_CONCURRENT));
    }

    /**
     * Test preprocessing <code>PrintTenuringDistributionPreprocessAction</code> with underlying
     * <code>ParallelScavengeEvent</code>.
     */
    public void testSplitParallelScavengeEventLogging() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset30.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PARALLEL_SCAVENGE));
    }

    /**
     * Test preprocessing a combined <code>CmsConcurrentEvent</code> and <code>ApplicationConcurrentTimeEvent</code>
     * split across 2 lines.
     */
    public void testCombinedCmsConcurrentApplicationConcurrentTimeLogging() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset19.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(
                "Log line not recognized as " + JdkUtil.LogEventType.APPLICATION_CONCURRENT_TIME.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.APPLICATION_CONCURRENT_TIME));
        Assert.assertTrue(Analysis.KEY_PRINT_GC_APPLICATION_CONCURRENT_TIME + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_GC_APPLICATION_CONCURRENT_TIME));
    }

    /**
     * Test preprocessing a combined <code>CmsConcurrentEvent</code> and <code>ApplicationStoppedTimeEvent</code> split
     * across 2 lines.
     */
    public void testCombinedCmsConcurrentApplicationStoppedTimeLogging() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset27.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.CMS_CONCURRENT.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.CMS_CONCURRENT));
        Assert.assertTrue(
                "Log line not recognized as " + JdkUtil.LogEventType.APPLICATION_STOPPED_TIME.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.APPLICATION_STOPPED_TIME));
    }

    public void testRemoveBlankLines() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset20.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(
                "Log line not recognized as " + JdkUtil.LogEventType.APPLICATION_CONCURRENT_TIME.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.APPLICATION_CONCURRENT_TIME));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
        Assert.assertTrue(Analysis.KEY_PRINT_GC_APPLICATION_CONCURRENT_TIME + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_GC_APPLICATION_CONCURRENT_TIME));

    }

    /**
     * Test to make sure a <code>ParNewPromotionFailedTruncatedEvent</code> is not mistakenly preprocessed as a
     * <code>ParNewCmsConcurrentPreprocessAction</code>.
     */
    public void testParNewPromotionFailedTruncatedEventLogging() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset23.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(
                "Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW_PROMOTION_FAILED_TRUNCATED.toString()
                        + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW_PROMOTION_FAILED_TRUNCATED));
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
        Assert.assertTrue(Analysis.KEY_CMS_PROMOTION_FAILED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_PROMOTION_FAILED));
    }

    /**
     * Test <code>DateStampPreprocessAction</code>.
     */
    public void testDateStampPreprocessActionLogging() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset25.txt");
        GcManager jvmManager = new GcManager();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, Calendar.FEBRUARY);
        calendar.set(Calendar.DAY_OF_MONTH, 26);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        File preprocessedFile = jvmManager.preprocess(testFile, calendar.getTime());
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
    }

    /**
     * Test <code>DateStampPrefixPreprocessAction</code>.
     */
    public void testDateStampPrefixPreprocessAction() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset26.txt");
        GcManager jvmManager = new GcManager();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, Calendar.APRIL);
        calendar.set(Calendar.DAY_OF_MONTH, 16);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        File preprocessedFile = jvmManager.preprocess(testFile, calendar.getTime());
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
    }

    /**
     * Test <code>DateStampPrefixPreprocessAction</code> with multiple datestamps.
     */
    public void testDateStampPrefixPreprocessActionMultiple() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset33.txt");
        GcManager jvmManager = new GcManager();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, Calendar.APRIL);
        calendar.set(Calendar.DAY_OF_MONTH, 16);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        File preprocessedFile = jvmManager.preprocess(testFile, calendar.getTime());
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
    }

    public void testSummaryStatsStoppedTime() {

        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset41.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Max heap space not calculated correctly.", 31457280, jvmRun.getMaxHeapSpace());
        Assert.assertEquals("Max heap occupancy not calculated correctly.", 140186, jvmRun.getMaxHeapOccupancy());
        Assert.assertEquals("Max GC pause not calculated correctly.", 41, jvmRun.getMaxGcPause());
        Assert.assertEquals("Max perm gen space not calculated correctly.", 0, jvmRun.getMaxPermSpace());
        Assert.assertEquals("Max perm gen occupancy not calculated correctly.", 0, jvmRun.getMaxPermOccupancy());
        Assert.assertEquals("Total GC pause time not calculated correctly.", 61, jvmRun.getTotalGcPause());
        Assert.assertEquals("GC last timestamp not calculated correctly.", 2847, jvmRun.getLastTimestamp());
        Assert.assertEquals("GC throughput not calculated correctly.", 98, jvmRun.getGcThroughput());
        Assert.assertEquals("GC Event count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(JdkUtil.LogEventType.G1_YOUNG_PAUSE.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.G1_YOUNG_PAUSE));
        Assert.assertTrue(JdkUtil.LogEventType.APPLICATION_STOPPED_TIME.toString() + " not identified.",
                jvmRun.getEventTypes().contains(LogEventType.APPLICATION_STOPPED_TIME));
        Assert.assertEquals("Stopped Time event count not correct.", 6, jvmRun.getStoppedTimeEventCount());
        Assert.assertEquals("Max stopped time not calculated correctly.", 1000, jvmRun.getMaxStoppedTime());
        Assert.assertEquals("Total stopped time not calculated correctly.", 1064, jvmRun.getTotalStoppedTime());
        Assert.assertEquals("Stopped time throughput not calculated correctly.", 63, jvmRun.getStoppedTimeThroughput());
        Assert.assertEquals("GC/Stopped ratio not calculated correctly.", 6, jvmRun.getGcStoppedRatio());
        Assert.assertTrue(Analysis.KEY_GC_STOPPED_RATIO + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_GC_STOPPED_RATIO));
    }

    public void testHeaderLogging() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset42.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(JdkUtil.LogEventType.HEADER_COMMAND_LINE_FLAGS.toString() + " information not identified.",
                jvmRun.getEventTypes().contains(LogEventType.HEADER_COMMAND_LINE_FLAGS));
        Assert.assertTrue(JdkUtil.LogEventType.HEADER_MEMORY.toString() + " information not identified.",
                jvmRun.getEventTypes().contains(LogEventType.HEADER_MEMORY));
        Assert.assertTrue(JdkUtil.LogEventType.HEADER_VERSION.toString() + " information not identified.",
                jvmRun.getEventTypes().contains(LogEventType.HEADER_VERSION));
        Assert.assertTrue(Analysis.KEY_THREAD_STACK_SIZE_NOT_SET + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_THREAD_STACK_SIZE_NOT_SET));
    }

    /**
     * Test <code>G1PreprocessAction</code> for mixed G1_YOUNG_PAUSE and G1_CONCURRENT with ergonomics.
     * 
     */
    public void testExplicitGcAnalsysisParallelSerialOld() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset56.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 2, jvmRun.getEventTypes().size());
        Assert.assertTrue(JdkUtil.LogEventType.PARALLEL_SCAVENGE.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.PARALLEL_SCAVENGE));
        Assert.assertTrue(JdkUtil.LogEventType.PARALLEL_SERIAL_OLD.toString() + " collector not identified.",
                jvmRun.getEventTypes().contains(LogEventType.PARALLEL_SERIAL_OLD));
        Assert.assertTrue(Analysis.KEY_EXPLICIT_GC_UNNECESSARY + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_EXPLICIT_GC_UNNECESSARY));
    }

    /**
     * Test JVM Header parsing.
     * 
     */
    public void testHeaders() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset59.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertFalse(JdkUtil.LogEventType.UNKNOWN.toString() + " collector identified.",
                jvmRun.getEventTypes().contains(LogEventType.UNKNOWN));
        Assert.assertEquals("Event type count not correct.", 3, jvmRun.getEventTypes().size());
        Assert.assertTrue(JdkUtil.LogEventType.HEADER_COMMAND_LINE_FLAGS.toString() + " not identified.",
                jvmRun.getEventTypes().contains(LogEventType.HEADER_COMMAND_LINE_FLAGS));
        Assert.assertTrue(JdkUtil.LogEventType.HEADER_MEMORY.toString() + " not identified.",
                jvmRun.getEventTypes().contains(LogEventType.HEADER_MEMORY));
        Assert.assertTrue(JdkUtil.LogEventType.HEADER_VERSION.toString() + " not identified.",
                jvmRun.getEventTypes().contains(LogEventType.HEADER_VERSION));
        Assert.assertTrue(Analysis.KEY_EXPLICIT_GC_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_EXPLICIT_GC_DISABLED));
    }

    /**
     * Test analysis perm gen or metaspace size not set.
     * 
     */
    public void testAnalysisPermMetaspaceNotSet() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset60.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_PERM_METASPACE_NOT_SET + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PERM_METASPACE_NOT_SET));
        Assert.assertFalse(Analysis.KEY_EXPLICIT_GC_NOT_CONCURRENT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_EXPLICIT_GC_NOT_CONCURRENT));
    }

    /**
     * Test passing JVM options on the command line.
     * 
     */
    public void testJvmOptionsPassedInOnCommandLine() {
        String options = "MGM was here!";
        GcManager jvmManager = new GcManager();
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(options, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue("JVM options passed in are missing or have changed.",
                jvmRun.getJvm().getOptions().equals(options));
    }

    /**
     * Test passing JVM options on the command line.
     */
    public void testAnalysisThreadStackSizeLarge() {
        String options = "-o \"-Xss1024k\"";
        GcManager jvmManager = new GcManager();
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(options, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_THREAD_STACK_SIZE_LARGE + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_THREAD_STACK_SIZE_LARGE));
    }

    /**
     * Test DGC not managed analysis.
     */
    public void testAnalysisDgcNotManaged() {
        String jvmOptions = "MGM";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_RMI_DGC_NOT_MANAGED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_RMI_DGC_NOT_MANAGED));
    }

    /**
     * Test DGC redundant options analysis.
     */
    public void testAnalysisDgcRedundantOptions() {
        String jvmOptions = "-XX:+DisableExplicitGC -Dsun.rmi.dgc.client.gcInterval=14400000 "
                + "-Dsun.rmi.dgc.server.gcInterval=24400000";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_RMI_DGC_CLIENT_GCINTERVAL_REDUNDANT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_RMI_DGC_CLIENT_GCINTERVAL_REDUNDANT));
        Assert.assertTrue(Analysis.KEY_RMI_DGC_SERVER_GCINTERVAL_REDUNDANT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_RMI_DGC_SERVER_GCINTERVAL_REDUNDANT));
    }

    /**
     * Test analysis not small DGC intervals.
     */
    public void testAnalysisDgcNotSmallIntervals() {
        String jvmOptions = "-Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertFalse(Analysis.KEY_RMI_DGC_CLIENT_GCINTERVAL_SMALL + " analysis identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_RMI_DGC_CLIENT_GCINTERVAL_SMALL));
        Assert.assertFalse(Analysis.KEY_RMI_DGC_SERVER_GCINTERVAL_SMALL + " analysis identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_RMI_DGC_SERVER_GCINTERVAL_SMALL));
    }

    /**
     * Test analysis small DGC intervals
     */
    public void testAnalysisDgcSmallIntervals() {
        String jvmOptions = "-Dsun.rmi.dgc.client.gcInterval=3599999 -Dsun.rmi.dgc.server.gcInterval=3599999";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_RMI_DGC_CLIENT_GCINTERVAL_SMALL + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_RMI_DGC_CLIENT_GCINTERVAL_SMALL));
        Assert.assertTrue(Analysis.KEY_RMI_DGC_SERVER_GCINTERVAL_SMALL + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_RMI_DGC_SERVER_GCINTERVAL_SMALL));
    }

    /**
     * Test analysis if heap dump on OOME enabled.
     */
    public void testAnalysisHeapDumpOnOutOfMemoryError() {
        String jvmOptions = "MGM";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_HEAP_DUMP_ON_OOME_MISSING + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_HEAP_DUMP_ON_OOME_MISSING));
    }

    /**
     * Test analysis if instrumentation being used.
     */
    public void testAnalysisInstrumentation() {
        String jvmOptions = "Xss128k -Xms2048M -javaagent:byteman.jar=script:kill-3.btm,boot:byteman.jar -Xmx2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_INSTRUMENTATION + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_INSTRUMENTATION));
    }

    /**
     * Test analysis if native library being used.
     */
    public void testAnalysisNative() {
        String jvmOptions = "Xss128k -Xms2048M -agentpath:/path/to/agent.so -Xmx2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_NATIVE + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_NATIVE));
    }

    /**
     * Test analysis background compilation disabled.
     */
    public void testAnalysisBackgroundCompilationDisabled() {
        String jvmOptions = "Xss128k -XX:-BackgroundCompilation -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_BYTECODE_BACKGROUND_COMPILE_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_BYTECODE_BACKGROUND_COMPILE_DISABLED));
    }

    /**
     * Test analysis background compilation disabled.
     */
    public void testAnalysisBackgroundCompilationDisabledXBatch() {
        String jvmOptions = "Xss128k -Xbatch -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_BYTECODE_BACKGROUND_COMPILE_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_BYTECODE_BACKGROUND_COMPILE_DISABLED));
    }

    /**
     * Test analysis compilation on first invocation enabled.
     */
    public void testAnalysisCompilationOnFirstInvocation() {
        String jvmOptions = "Xss128k -Xcomp-Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_BYTECODE_COMPILE_FIRST_INVOCATION + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_BYTECODE_COMPILE_FIRST_INVOCATION));
    }

    /**
     * Test analysis just in time (JIT) compiler disabled.
     */
    public void testAnalysisCompilationDisabled() {
        String jvmOptions = "Xss128k -Xint -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_BYTECODE_COMPILE_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_BYTECODE_COMPILE_DISABLED));
    }

    /**
     * Test analysis explicit GC not concurrent.
     */
    public void testAnalysisExplicitGcNotConcurrentG1() {
        String jvmOptions = "MGM";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        List<LogEventType> eventTypes = new ArrayList<LogEventType>();
        eventTypes.add(LogEventType.G1_FULL_GC);
        jvmRun.setEventTypes(eventTypes);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_EXPLICIT_GC_NOT_CONCURRENT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_EXPLICIT_GC_NOT_CONCURRENT));
    }

    /**
     * Test analysis explicit GC not concurrent.
     */
    public void testAnalysisExplicitGcNotConcurrentCms() {
        String jvmOptions = "MGM";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        List<LogEventType> eventTypes = new ArrayList<LogEventType>();
        eventTypes.add(LogEventType.CMS_CONCURRENT);
        jvmRun.setEventTypes(eventTypes);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_EXPLICIT_GC_NOT_CONCURRENT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_EXPLICIT_GC_NOT_CONCURRENT));
    }

    /**
     * Test DisableExplicitGC in combination with ExplicitGCInvokesConcurrent.
     */
    public void testAnalysisDisableExplictGcWithConcurrentHandling() {
        String jvmOptions = "Xss128k -XX:+DisableExplicitGC -XX:+ExplicitGCInvokesConcurrent -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_EXPLICIT_GC_DISABLED_CONCURRENT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_EXPLICIT_GC_DISABLED_CONCURRENT));
    }

    /**
     * Test HeapDumpOnOutOfMemoryError disabled.
     */
    public void testAnalysisHeapDumpOnOutOfMemoryErrorDisabled() {
        String jvmOptions = "Xss128k -XX:-HeapDumpOnOutOfMemoryError -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_HEAP_DUMP_ON_OOME_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_HEAP_DUMP_ON_OOME_DISABLED));
    }

    /**
     * Test PrintCommandLineFlags missing.
     */
    public void testAnalysisPrintCommandlineFlagsMissing() {
        String jvmOptions = "MGM";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_PRINT_COMMANDLINE_FLAGS + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_COMMANDLINE_FLAGS));
    }

    /**
     * Test PrintCommandLineFlags not missing.
     */
    public void testAnalysisPrintCommandlineFlagsNotMissing() {
        String jvmOptions = "Xss128k -XX:+PrintCommandLineFlags -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertFalse(Analysis.KEY_PRINT_COMMANDLINE_FLAGS + " analysis identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_COMMANDLINE_FLAGS));
    }

    /**
     * Test PrintGCDetails missing.
     */
    public void testAnalysisPrintGCDetailsMissing() {
        String jvmOptions = "Xss128k -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_PRINT_GC_DETAILS_MISSING + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_GC_DETAILS_MISSING));
    }

    /**
     * Test PrintGCDetails not missing.
     */
    public void testAnalysisPrintGCDetailsNotMissing() {
        String jvmOptions = "Xss128k -XX:+PrintGCDetails -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertFalse(Analysis.KEY_PRINT_GC_DETAILS_MISSING + " analysis identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_GC_DETAILS_MISSING));
    }

    /**
     * Test CMS not being used to collect old generation.
     */
    public void testAnalysisCmsYoungSerialOld() {
        String jvmOptions = "Xss128k -XX:+UseParNewGC -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_CMS_NEW_SERIAL_OLD + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_NEW_SERIAL_OLD));
    }

    /**
     * Test CMS being used to collect old generation.
     */
    public void testAnalysisCmsYoungCmsOld() {
        String jvmOptions = "Xss128k -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertFalse(Analysis.KEY_CMS_NEW_SERIAL_OLD + " analysis identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_NEW_SERIAL_OLD));
    }

    /**
     * Test CMS being used to collect old generation.
     */
    public void testAnalysisCMSClassUnloadingEnabledMissing() {
        String jvmOptions = "MGM";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        List<LogEventType> eventTypes = new ArrayList<LogEventType>();
        eventTypes.add(LogEventType.CMS_CONCURRENT);
        jvmRun.setEventTypes(eventTypes);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_CMS_CLASS_UNLOADING_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_CLASS_UNLOADING_DISABLED));
    }

    /**
     * Test CMS handling perm/metaspace collections.
     */
    public void testAnalysisCMSClassUnloadingEnabledMissingButJDK8EnabledByDefault() {
        String jvmOptions = "MGM";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        List<LogEventType> eventTypes = new ArrayList<LogEventType>();
        eventTypes.add(LogEventType.CMS_REMARK_WITH_CLASS_UNLOADING);
        jvmRun.setEventTypes(eventTypes);
        jvmRun.doAnalysis();
        Assert.assertFalse(Analysis.KEY_CMS_CLASS_UNLOADING_DISABLED + " analysis identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_CLASS_UNLOADING_DISABLED));
    }

    /**
     * Test CMS handling perm/metaspace collections.
     */
    public void testAnalysisCMSClassUnloadingEnabledMissingButNotCms() {
        String jvmOptions = "MGM";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertFalse(Analysis.KEY_CMS_CLASS_UNLOADING_DISABLED + " analysis identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_CLASS_UNLOADING_DISABLED));
    }

    /**
     * Test CMS handling perm/metaspace collections.
     */
    public void testAnalysisCMSClassUnloadingEnabledMissingCollector() {
        String jvmOptions = null;
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        List<LogEventType> eventTypes = new ArrayList<LogEventType>();
        eventTypes.add(LogEventType.CMS_REMARK);
        jvmRun.setEventTypes(eventTypes);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_CMS_CLASS_UNLOADING_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_CLASS_UNLOADING_DISABLED));
    }

    /**
     * Test if -XX:+PrintReferenceGC enabled by inspecting logging events.
     */
    public void testPrintReferenceGCByLogging() {
        String jvmOptions = null;
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        List<LogEventType> eventTypes = new ArrayList<LogEventType>();
        eventTypes.add(LogEventType.PRINT_REFERENCE_GC);
        jvmRun.setEventTypes(eventTypes);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_PRINT_REFERENCE_GC_ENABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_REFERENCE_GC_ENABLED));
    }

    /**
     * Test if -XX:+PrintReferenceGC enabled by inspecting jvm options.
     */
    public void testPrintReferenceGCByOptions() {
        String jvmOptions = "Xss128k -XX:+PrintReferenceGC -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_PRINT_REFERENCE_GC_ENABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_REFERENCE_GC_ENABLED));
    }

    /**
     * Test if -XX:+PrintStringDeduplicationStatistics enabled by inspecting jvm options.
     */
    public void testPrintStringDeduplicationStatistics() {
        String jvmOptions = "Xss128k -XX:+PrintStringDeduplicationStatistics -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_PRINT_STRING_DEDUP_STATS_ENABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_STRING_DEDUP_STATS_ENABLED));
    }

    /**
     * Test if PrintGCDetails disabled with -XX:-PrintGCDetails.
     */
    public void testPrintGCDetailsDisabled() {
        String jvmOptions = "Xss128k -XX:-PrintGCDetails -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_PRINT_GC_DETAILS_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_GC_DETAILS_DISABLED));
        Assert.assertFalse(Analysis.KEY_PRINT_GC_DETAILS_MISSING + " analysis incorrectly identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_GC_DETAILS_MISSING));
    }

    /**
     * Test <code>PrintTenuringDistributionPreprocessAction</code> with no space after "GC".
     * 
     */
    public void testPrintTenuringDistributionPreprocessActionNoSpaceAfterGc() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset66.txt");
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(new Jvm(null, null), Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertEquals("Event type count not correct.", 1, jvmRun.getEventTypes().size());
        Assert.assertTrue("Log line not recognized as " + JdkUtil.LogEventType.PAR_NEW.toString() + ".",
                jvmRun.getEventTypes().contains(JdkUtil.LogEventType.PAR_NEW));
    }

    /**
     * Test identifying <code>ParNewEvent</code> running in incremental mode.
     */
    public void testCmsIncrementalModeAnalysis() {
        // TODO: Create File in platform independent way.
        File testFile = new File("src/test/data/dataset68.txt");
        String jvmOptions = "Xss128k -XX:+CMSIncrementalMode -XX:CMSInitiatingOccupancyFraction=70 -Xms2048M";
        Jvm jvm = new Jvm(jvmOptions, null);
        GcManager jvmManager = new GcManager();
        File preprocessedFile = jvmManager.preprocess(testFile, null);
        jvmManager.store(preprocessedFile, false);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        Assert.assertTrue(Analysis.KEY_CMS_INCREMENTAL_MODE + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_INCREMENTAL_MODE));
        Assert.assertTrue(Analysis.KEY_CMS_INC_MODE_INIT_OCCUP_FRACT_CONFLICT + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_CMS_INC_MODE_INIT_OCCUP_FRACT_CONFLICT));
    }

    /**
     * Test if biased locking disabled with -XX:-UseBiasedLocking.
     */
    public void testBisasedLockingDisabled() {
        String jvmOptions = "Xss128k -XX:-UseBiasedLocking -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_BIASED_LOCKING_DISABLED + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_BIASED_LOCKING_DISABLED));
    }

    /**
     * Test if biased locking disabled with -XX:-UseBiasedLocking.
     */
    public void testPrintClassHistogramEnabled() {
        String jvmOptions = "Xss128k -XX:+PrintClassHistogram -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_PRINT_CLASS_HISTOGRAM + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_CLASS_HISTOGRAM));
    }

    /**
     * Test if biased locking disabled with -XX:-UseBiasedLocking.
     */
    public void testPrintApplicationConcurrentTime() {
        String jvmOptions = "Xss128k -XX:+PrintGCApplicationConcurrentTime -Xms2048M";
        GcManager jvmManager = new GcManager();
        Jvm jvm = new Jvm(jvmOptions, null);
        JvmRun jvmRun = jvmManager.getJvmRun(jvm, Constants.DEFAULT_BOTTLENECK_THROUGHPUT_THRESHOLD);
        jvmRun.doAnalysis();
        Assert.assertTrue(Analysis.KEY_PRINT_GC_APPLICATION_CONCURRENT_TIME + " analysis not identified.",
                jvmRun.getAnalysisKeys().contains(Analysis.KEY_PRINT_GC_APPLICATION_CONCURRENT_TIME));
    }
}
