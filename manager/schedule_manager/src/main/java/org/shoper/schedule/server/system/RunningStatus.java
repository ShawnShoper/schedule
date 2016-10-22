package org.shoper.schedule.server.system;

import java.util.concurrent.atomic.AtomicLong;
/**
 * provider running status..
 * 
 * @author ShawnShoper
 *
 */
public class RunningStatus
{
	public static int port = 0;
	public static AtomicLong serviceTimes = new AtomicLong(0);
	public static AtomicLong updateCount = new AtomicLong(0);
	public static AtomicLong addCount = new AtomicLong(0);
	public static AtomicLong timeConsuming = new AtomicLong(0);
}
