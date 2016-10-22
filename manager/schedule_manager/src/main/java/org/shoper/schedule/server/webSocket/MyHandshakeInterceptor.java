package org.shoper.schedule.server.webSocket;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Component
public class MyHandshakeInterceptor extends HttpSessionHandshakeInterceptor
{

	@Override
	public boolean beforeHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler handler, Map map)
					throws Exception
	{
		/*
		 * if (request instanceof ServletServerHttpRequest) {
		 * ServletServerHttpRequest servletRequest = (ServletServerHttpRequest)
		 * request; HttpSession session = servletRequest.getServletRequest()
		 * .getSession(false); if (session != null) { System.out.println("ok");
		 * // User u = (User) session.getAttribute("U"); // map.put("username",
		 * u.getName()); } }
		 */
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request,
			ServerHttpResponse response, WebSocketHandler wsHandler,
			Exception ex)
	{
	}
}