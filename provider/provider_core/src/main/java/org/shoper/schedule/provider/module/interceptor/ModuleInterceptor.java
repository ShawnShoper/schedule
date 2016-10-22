package org.shoper.schedule.provider.module.interceptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
/**
 * 这里必须开启cglib方式.不能使用 jdk proxy 的方式。<br>
 * 因为 JDKproxy 的方式是通过 interface<br>
 * 因为 ThriftStart 类构成问题...<br>
 * 所以需要使用EnableAspectJAutoProxy注解指定 proxyTargetClass = true来启用 cglib<br>
 * 
 * @author ShawnShoper
 */
@Aspect
@Component
// @EnableAspectJAutoProxy(proxyTargetClass = true)
public class ModuleInterceptor
{
	Logger log = LoggerFactory.getLogger(ModuleInterceptor.class);
	// @Pointcut("execution(* com.daqsoft.schedule.provider.module.*.*(..))")
	private void anyMethod()
	{
	}
	// 定义一个切入点

	/*
	 * @Before("anyMethod() && args(name)") public void doAccessCheck(String
	 * name) { System.out.println(name); System.out.println("前置通知"); }
	 * 
	 * @AfterReturning("anyMethod()") public void doAfter() {
	 * System.out.println("后置通知"); }
	 * 
	 * @After("anyMethod()") public void after() { System.out.println("最终通知"); }
	 * 
	 * @AfterThrowing("anyMethod()") public void doAfterThrow() {
	 * System.out.println("例外通知"); }
	 */

	// @Around("anyMethod()")
	public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable
	{
		long start = System.currentTimeMillis();
		Object object = pjp.proceed();// 执行该方法
		log.info("{}.{} spend time {} ms",
				pjp.getSignature().getDeclaringType().getName(),
				pjp.getSignature().getName(),
				System.currentTimeMillis() - start);
		return object;
	}
}
