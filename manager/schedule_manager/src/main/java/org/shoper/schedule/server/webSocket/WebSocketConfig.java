package org.shoper.schedule.server.webSocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig extends WebMvcConfigurerAdapter
		implements
			WebSocketConfigurer
{
	@Autowired
	SystemPushHandler systemWebSocketHandler;
	@Autowired
	MyHandshakeInterceptor myHandshakeInterceptor;
	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry)
	{
		registry.addHandler(systemWebSocketHandler, "/webSocketServer")
				.addInterceptors(myHandshakeInterceptor);
		registry.addHandler(systemWebSocketHandler, "/sockjs/webSocketServer")
				.addInterceptors(myHandshakeInterceptor).withSockJS();

	}
}