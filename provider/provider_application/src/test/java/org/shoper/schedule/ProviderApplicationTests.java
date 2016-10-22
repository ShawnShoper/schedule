package org.shoper.schedule;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.shoper.dynamiccompile.ClassLoaderHandler;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.provider.job.JobCaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.shoper.schedule.SystemContext.getBean;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ProviderApplication.class)
public class ProviderApplicationTests {
	@Autowired
	AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor;
	@Autowired
	ConfigurableListableBeanFactory factory;
	@Autowired
	AnnotationConfigApplicationContext applicationContext;

	@Test
	public void contextLoads () {
	}

	@Test
	public void annoApplicationRegistry () {

	}

	@Test
	public void factoryRexgistryTest () {

		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(A.class).getBeanDefinition();
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) factory;
		defaultListableBeanFactory.registerBeanDefinition("a", beanDefinition);
		A a = defaultListableBeanFactory.getBean("a", A.class);
		a.say();
		defaultListableBeanFactory.destroySingleton("a");
		A b = defaultListableBeanFactory.getBean("a", A.class);
		b.say();
		System.out.println(a == b);
	}

	public JobCaller getSpringBean (String beanName, JobCaller bean) {
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) factory;
		autowiredAnnotationBeanPostProcessor.processInjection(bean);
		factory.registerSingleton(beanName, bean);
		JobCaller jobCaller = factory.getBean(beanName, JobCaller.class);
		defaultListableBeanFactory.destroySingleton(beanName);
		return jobCaller;
	}

	@Test
	public void factoryByRemote () throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, InterruptedException, SystemException {
		Object o1 = null;
		Object o2 = null;
		{
			ClassLoaderHandler classLoaderHandler = ClassLoaderHandler.newInstance();
			Class class1 = classLoaderHandler.getClassFromJavaFile(new File("/Users/ShawnShoper/Desktop/Test.java"));
			TimeUnit.SECONDS.sleep(20);
			{
				JobCaller jobCaller = getSpringBean("id", (JobCaller) class1.newInstance());
				o1 = jobCaller;
				jobCaller.run();
			}
			TimeUnit.SECONDS.sleep(5);
			System.out.println("release .... begin");
			classLoaderHandler.close();
			System.out.println("release .... over");
			TimeUnit.SECONDS.sleep(10);
			{
				JobCaller jobCaller = getSpringBean("id", (JobCaller) class1.newInstance());
				jobCaller.run();
			}
		}
		System.out.println("...第一步完毕....");
		System.out.println("30秒时间修改文件...");
		TimeUnit.SECONDS.sleep(20);
		System.out.println("加载 class");
		{
			ClassLoaderHandler classLoaderHandler = ClassLoaderHandler.newInstance();
			Class class1 = classLoaderHandler.getClassFromJavaFile(new File("/Users/ShawnShoper/Desktop/Test.java"));
			TimeUnit.SECONDS.sleep(20);
			{
				JobCaller jobCaller = getSpringBean("id", (JobCaller) class1.newInstance());
				jobCaller.run();
			}
			TimeUnit.SECONDS.sleep(5);
			System.out.println("release .... begin");
			classLoaderHandler.close();
			System.out.println("release .... over");
			TimeUnit.SECONDS.sleep(10);
			{
				JobCaller jobCaller = getSpringBean("id", (JobCaller) class1.newInstance());
				o2=jobCaller;
				jobCaller.run();
			}
		}
		System.out.println(o1==o2);
	}

	@Test
	public void factoryRexgistryTest2 () {
		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(A.class).setScope("singleton").getBeanDefinition();
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) factory;
		defaultListableBeanFactory.registerBeanDefinition("a", beanDefinition);
		A a = defaultListableBeanFactory.getBean(A.class);
		a.say();
		System.out.println(a);
		defaultListableBeanFactory.destroySingleton("a");
		a = defaultListableBeanFactory.getBean(A.class);
		System.out.println(a);
		a.say();
		System.out.println(a);
	}

	@Test
	public void factoryRegistry2Test () {
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) factory;
		A a = new A();
		autowiredAnnotationBeanPostProcessor.processInjection(a);
		factory.registerSingleton("a", a);
		A a1 = (A) factory.getBean("a");
		a1.say();
		System.out.println(a1);
		defaultListableBeanFactory.destroySingleton("a");
		factory.getBean("a");
	}

	@Test
	public void factoryRegistry4Test () {
		A a = new A();
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) factory;
		autowiredAnnotationBeanPostProcessor.processInjection(a);
		factory.registerSingleton("a", a);
		A c = (A) factory.getSingleton("a");
		System.out.println(c);
		defaultListableBeanFactory.destroySingleton("a");
	}
}
