package org.shoper.schedule.server.web.response;

public class ResponseDataVO extends Response
{
	private long total;
	private int page;
	private int row;

	public long getTotal()
	{
		return total;
	}
	public void setTotal(long total)
	{
		this.total = total;
	}
	public int getPage()
	{
		return page;
	}
	public void setPage(int page)
	{
		this.page = page;
	}
	public int getRow()
	{
		return row;
	}
	public void setRow(int row)
	{
		this.row = row;
	}

}
