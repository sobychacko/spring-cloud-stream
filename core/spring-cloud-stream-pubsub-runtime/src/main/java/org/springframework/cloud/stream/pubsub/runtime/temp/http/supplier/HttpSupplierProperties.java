//package org.springframework.cloud.stream.pubsub.runtime.temp.http.supplier;
//
//import jakarta.validation.constraints.NotEmpty;
//
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.cors.CorsConfiguration;
//
///**
// * Configuration properties for the HTTP Supplier.
// *
// * @author Artem Bilan
// */
//@ConfigurationProperties("http")
//@Validated
//public class HttpSupplierProperties {
//
//	/**
//	 * HTTP endpoint path mapping.
//	 */
//	private String pathPattern = "/";
//
//	/**
//	 * Headers that will be mapped.
//	 */
//	private String[] mappedRequestHeaders = {DefaultHttpHeaderMapper.HTTP_REQUEST_HEADER_NAME_PATTERN};
//
//	/**
//	 * CORS properties.
//	 */
//	private Cors cors = new Cors();
//
//	@NotEmpty
//	public String getPathPattern() {
//		return this.pathPattern;
//	}
//
//	public void setPathPattern(String pathPattern) {
//		this.pathPattern = pathPattern;
//	}
//
//	public String[] getMappedRequestHeaders() {
//		return this.mappedRequestHeaders;
//	}
//
//	public void setMappedRequestHeaders(String[] mappedRequestHeaders) {
//		this.mappedRequestHeaders = mappedRequestHeaders;
//	}
//
//	public Cors getCors() {
//		return this.cors;
//	}
//
//	public void setCors(Cors cors) {
//		this.cors = cors;
//	}
//
//	public static class Cors {
//
//		/**
//		 * List of allowed origins, e.g. https://domain1.com.
//		 */
//		private String[] allowedOrigins = {CorsConfiguration.ALL};
//
//		/**
//		 * List of request headers that can be used during the actual request.
//		 */
//		private String[] allowedHeaders = {CorsConfiguration.ALL};
//
//		/**
//		 * Whether the browser should include any cookies associated with the domain of the request being annotated.
//		 */
//		private Boolean allowCredentials;
//
//		@NotEmpty
//		public String[] getAllowedOrigins() {
//			return this.allowedOrigins;
//		}
//
//		public void setAllowedOrigins(String[] allowedOrigins) {
//			this.allowedOrigins = allowedOrigins;
//		}
//
//		@NotEmpty
//		public String[] getAllowedHeaders() {
//			return this.allowedHeaders;
//		}
//
//		public void setAllowedHeaders(String[] allowedHeaders) {
//			this.allowedHeaders = allowedHeaders;
//		}
//
//		public Boolean getAllowCredentials() {
//			return allowCredentials;
//		}
//
//		public void setAllowCredentials(Boolean allowCredentials) {
//			this.allowCredentials = allowCredentials;
//		}
//
//	}
//
//}
