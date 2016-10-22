package org.shoper.schedule.exception;

public class RPCConnectionException extends Exception
{
	private static final long serialVersionUID = 1L;

	public RPCConnectionException()
	{
		super();
	}

	public RPCConnectionException(String message)
	{
		super(message);
	}

	public RPCConnectionException(Throwable cause)
	{
		super(cause);
	}

	public RPCConnectionException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
