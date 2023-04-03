/*
 * Copyright(c) 2022, ZWSOFT Co., LTD. (Guangzhou)ALL Rights Reserved.
 */

package com.example.springtest;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import java.security.Principal;
import java.util.List;

/**
 * @author Ponzio
 * @create 2022-03-09 10:53
 * @description
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Log4j2
public class WebsocketConfigure implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/acan").setAllowedOrigins("*");
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // ws 传输数据的时候，数据过大有时候会接收不到，所以在此处设置bufferSize
        container.setMaxTextMessageBufferSize(512000);
        container.setMaxBinaryMessageBufferSize(512000);
        container.setMaxSessionIdleTimeout(15 * 60000L);
        return container;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {

            @Override
            public boolean preReceive(MessageChannel channel) {
                log.info("preReceive channel .........");
                return true;
            }

            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                log.info("inbound channel .........");
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> header = accessor.getNativeHeader("token");
                    if (header == null || header.isEmpty()) {
                        log.info("inbound channel header is null or empty.......");
                        return null;
                    }
                    String token = header.get(0);
                    log.info("inbound channel token is {}", token);
                    if (token.isEmpty()) {
                        return null;
                    }
                    if (!validToken(token)) {
                        return null;
                    }
                    Principal principal = () -> "111111" + "@" + accessor.getSessionId();
                    accessor.setUser(principal);
                    return message;
                }
                return message;
            }
        });
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(1024 * 1024 * 1000);
        registry.setSendBufferSizeLimit(1024 * 1024 * 1000);
    }


    //TODO token校验有待实现
    private boolean validToken(String token) {
        return true;
    }
}
