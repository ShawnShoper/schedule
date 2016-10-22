package org.shoper.schedule.exception;
/**
 * 业务异常
 * 
 * @author ShawnShoper
 *
 */
public class SystemException extends Exception
{

	private static final long serialVersionUID = 1L;

	public SystemException()
	{
		super();
	}

	public SystemException(String message)
	{
		super(message);
	}

	public SystemException(Throwable cause)
	{
		super(cause);
	}

	public SystemException(String message, Throwable cause)
	{
		super(message, cause);
	}
}