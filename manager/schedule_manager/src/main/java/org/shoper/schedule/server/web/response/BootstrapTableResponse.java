package org.shoper.schedule.server.web.response;

import java.util.List;
/**
 * boot
 * 
 * @author ShawnShoper
 *
 * @param <T>
 */
public class BootstrapTableResponse<T> extends ResponseDataVO
{
	private List<T> rows;

	public List<T> getRows()
	{
		return rows;
	}

	public void setRows(List<T> rows)
	{
		this.rows = rows;
	}

}
