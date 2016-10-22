package org.shoper.schedule.provider.handle.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
@Component
@Aspect
public class HandleInterceptor
{
	Logger log = LoggerFactory.getLogger(HandleInterceptor.class);
	@Pointcut("execution(* com.daqsoft.schedule.provider.handle.*.*(..))")
	private void anyMethod()
	{
	}
	// 定义一个切入点

	/*@Before("anyMethod() && args(name)")
	public void doAccessCheck(String name)
	{
		System.out.println(name);
		System.out.println("前置通知");
	}

	@AfterReturning("anyMethod()")
	public void doAfter()
	{
		System.out.println("后置通知");
	}

	@After("anyMethod()")
	public void after()
	{
		System.out.println("最终通知");
	}

	@AfterThrowing("anyMethod()")
	public void doAfterThrow()
	{
		System.out.println("例外通知");
	}*/

	@Around("anyMethod()")
	public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable
	{
		long start = System.currentTimeMillis();
		Object object = pjp.proceed();// 执行该方法
		log.info("{}.{} spend time {} ms",pjp.getSignature().getDeclaringType().getName(),pjp.getSignature().getName(),System.currentTimeMillis()-start);
		return object;
	}
}
