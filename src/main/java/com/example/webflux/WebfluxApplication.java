package com.example.webflux;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.context.annotation.Bean;

import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Date;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class WebfluxApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(WebfluxApplication.class);
		// prevent SpringBoot from starting a web server
		app.setWebApplicationType(WebApplicationType.NONE);
		app.run(args);
	}

	@Bean
  	public CommandLineRunner myCommandLineRunner() throws URISyntaxException, MalformedURLException{
		return args -> {
			URI streamdataUri = new URI("ws://localhost:9000/api/activity/Fred/live");

	    		EmitterProcessor<String> output = EmitterProcessor.create();

			WebSocketClient webSocketClient = new ReactorNettyWebSocketClient();
			Mono<Void> sessionMono = webSocketClient.execute(streamdataUri, wsession ->
				wsession.send(
						Flux.<WebSocketMessage>generate(s -> s.next(wsession.pingMessage(f -> f.wrap("PING".getBytes()))))
                                            	.log()
                                            	.delayElements(Duration.ofSeconds(2)))						
					.thenMany(
				         	wsession.receive()
					 	.filter(msg -> msg.getType() == WebSocketMessage.Type.TEXT)
			           	 	.map(WebSocketMessage::getPayloadAsText)
						.log()
			           		.subscribeWith(output)
					   	.then())
					.then()
			);

	    		Flux<String> msgs = output.doOnSubscribe(s->sessionMono.subscribe());
			msgs.subscribe();
	  	};
  	}
}
