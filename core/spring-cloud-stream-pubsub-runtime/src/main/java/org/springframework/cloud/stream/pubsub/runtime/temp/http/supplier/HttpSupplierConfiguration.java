//package org.springframework.cloud.stream.pubsub.runtime.temp.http.supplier;
//
//import java.util.function.Supplier;
//
//import org.reactivestreams.Publisher;
//import org.springframework.integration.dsl.IntegrationFlow;
//import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
//import org.springframework.integration.webflux.dsl.WebFlux;
//import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;
//import reactor.core.publisher.Flux;
//
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.codec.ServerCodecConfigurer;
//import org.springframework.integration.expression.ValueExpression;
//import org.springframework.integration.mapping.HeaderMapper;
//import org.springframework.messaging.Message;
//import org.springframework.messaging.MessageHeaders;
//
///**
// * Configuration for the HTTP Supplier.
// *
// * @author Artem Bilan
// */
//@EnableConfigurationProperties(HttpSupplierProperties.class)
//@Configuration
//public class HttpSupplierConfiguration {
//
//	@Bean
//	@ConditionalOnMissingBean
//	public HeaderMapper<HttpHeaders> httpHeaderMapper(HttpSupplierProperties httpSupplierProperties) {
//		DefaultHttpHeaderMapper defaultHttpHeaderMapper = DefaultHttpHeaderMapper.inboundMapper();
//		defaultHttpHeaderMapper.setInboundHeaderNames(httpSupplierProperties.getMappedRequestHeaders());
//		return defaultHttpHeaderMapper;
//	}
//
//	@Bean
//	public Publisher<Message<byte[]>> httpSupplierFlow(HttpSupplierProperties httpSupplierProperties,
//													   HeaderMapper<HttpHeaders> httpHeaderMapper,
//													   ServerCodecConfigurer serverCodecConfigurer) {
//
//		return IntegrationFlow.from(
//				WebFlux.inboundChannelAdapter(httpSupplierProperties.getPathPattern())
//					.requestPayloadType(byte[].class)
//					.statusCodeExpression(new ValueExpression<>(HttpStatus.ACCEPTED))
//					.headerMapper(httpHeaderMapper)
//					.codecConfigurer(serverCodecConfigurer)
//					.crossOrigin(crossOrigin ->
//						crossOrigin.origin(httpSupplierProperties.getCors().getAllowedOrigins())
//							.allowedHeaders(httpSupplierProperties.getCors().getAllowedHeaders())
//							.allowCredentials(httpSupplierProperties.getCors().getAllowCredentials()))
//					.autoStartup(false))
//			.enrichHeaders((headers) ->
//				headers.headerFunction(MessageHeaders.CONTENT_TYPE,
//					(message) ->
//						(MediaType.APPLICATION_FORM_URLENCODED.equals(
//							message.getHeaders().get(MessageHeaders.CONTENT_TYPE, MediaType.class)))
//							? MediaType.APPLICATION_JSON
//							: null,
//					true))
//			.toReactivePublisher();
//	}
//
//	@Bean
//	public Supplier<Flux<Message<byte[]>>> httpSupplier(
//		Publisher<Message<byte[]>> httpRequestPublisher,
//		WebFluxInboundEndpoint webFluxInboundEndpoint) {
//
//		return () -> Flux.from(httpRequestPublisher)
//			.doOnSubscribe((subscription) -> webFluxInboundEndpoint.start())
//			.doOnTerminate(webFluxInboundEndpoint::stop);
//	}
//
//}
