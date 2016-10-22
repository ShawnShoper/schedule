package org.shoper.schedule.provider;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Unit test for simple App.
 */
public class AppTest
{
	volatile static int count = 0;

	final static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();

	public static void main(String args[]) throws Exception
	{
		// InitQueue();
		// InitQueue();
		// InitQueue();
		// InitQueue();
		// ComitQueue();
		double a = 0.0d;
		System.out.println(a == 0x0);

	}
	public static void ComitQueue()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				int i = 0;
				for (;;)
				{
					new Thread(new Runnable() {
						@Override
						public void run()
						{
							String val = queue.poll();
							count--;
							System.out.println(
									"消费" + val + "-size :" + queue.size());
						}
					}, "消费线程子线程" + i).start();
					i++;
				}
			}
		}, "消费主线程线程").start();
	}

	public static void InitQueue()
	{
		new Thread(new Runnable() {
			@Override
			public void run()
			{
				int i = 0;
				for (;;)
				{
					new Thread(new Runnable() {
						@Override
						public void run()
						{
							String uuid = Math.random() + ""
									+ System.currentTimeMillis();
							System.out.println(queue.offer(uuid) + "- size : "
									+ queue.size());
							count++;
						}
					}, "生产子线程" + i).start();
					i++;
				}
			}
		}, "生产主线程").start();;
	}
}
