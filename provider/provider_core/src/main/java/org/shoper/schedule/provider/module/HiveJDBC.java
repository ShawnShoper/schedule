package org.shoper.schedule.provider.module;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.PostConstruct;

import org.shoper.commons.StringUtil;
import org.shoper.schedule.exception.SystemException;
import org.shoper.schedule.provider.common.HiveInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Hive JDBC util
 * 
 * @author ShawnShoper
 *
 */
public class HiveJDBC
{
	private static final Logger log = LoggerFactory.getLogger(HiveJDBC.class);

	@Autowired
	HiveInfo hiveInfo;
	private static String driverName = "org.apache.hive.jdbc.HiveDriver";
	private static String url = "jdbc:hive2://192.168.0.92:10000/public_sentiment";
	private static String user = "cloudera";
	private static String password = "cloudera";
	private static String sql = "";
	private static final String protcol = "jdbc:hive2://";
	private static boolean initFlag = true;
	@PostConstruct
	public void init()
	{
		try
		{
			Class.forName(driverName);
			checkArgs();
			if (!initFlag)
			{
				driverName = hiveInfo.getDriverClass();
				url = protcol + hiveInfo.getHost() + ":" + hiveInfo.getPort()
						+ "/" + hiveInfo.getDbName();
				user = hiveInfo.getUser();
				password = hiveInfo.getPassword();
			}
		} catch (Exception e)
		{
			log.error("Initialize hive JDBC util failed ......", e);
			initFlag = false;
		}

	}
	/**
	 * get Connection
	 * 
	 * @return
	 * @throws SQLException
	 */
	public static Connection getConnections() throws SystemException
	{
		if (!initFlag)
			throw new SystemException("Hive JDBC initialize failed....");
		Connection conn = null;
		try
		{
			conn = DriverManager.getConnection(url, user, password);
		} catch (Exception e)
		{
			throw new SystemException(e);
		}
		return conn;
	}
	/**
	 * Check hive JDBC connector args
	 * 
	 * @throws SystemException
	 */
	private void checkArgs() throws SystemException
	{
		int port = hiveInfo.getPort();
		if (!StringUtil.isEmpty(hiveInfo.getDbName())
				&& !StringUtil.isEmpty(hiveInfo.getDriverClass())
				&& !StringUtil.isEmpty(hiveInfo.getHost())
				&& !StringUtil.isEmpty(hiveInfo.getPassword())
				&& !StringUtil.isEmpty(hiveInfo.getUser())
				&& (port <= 0 && port > 65535))
			throw new SystemException("Invalid args for hive jdbc connector..");
	}
	public static void main(String[] args) throws Exception
	{
		Class.forName(driverName);
		Connection conn = DriverManager.getConnection(url, user, password);

		Statement stmt = conn.createStatement();

		// 创建的表名
		String tableName = "news_base";
		/** 第一步:存在就先删除 **/
		sql = "select count(1) from " + tableName;
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next())
		{
			System.out.println(rs.getLong(1));
		}
	}
}
