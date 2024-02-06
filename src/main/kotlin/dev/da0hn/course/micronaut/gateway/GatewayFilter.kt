package dev.da0hn.course.micronaut.gateway

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.discovery.ServiceInstance
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.annotation.Filter
import io.micronaut.http.client.LoadBalancer
import io.micronaut.http.client.ProxyHttpClient
import io.micronaut.http.filter.FilterChain
import io.micronaut.http.filter.HttpFilter
import io.reactivex.Flowable
import jakarta.inject.Inject
import mu.KotlinLogging
import org.reactivestreams.Publisher

@Filter("/**")
class GatewayFilter(
  private val proxyHttpClient: ProxyHttpClient,
) : HttpFilter {

  private val logger = KotlinLogging.logger { }

  @Inject
  lateinit var serviceLoadBalancers: Map<String, LoadBalancer>

  override fun doFilter(request: HttpRequest<*>?, chain: FilterChain?): Publisher<out HttpResponse<*>> {
    val serviceName = request?.path?.replace(Regex("^/([^/]+).*$"), replacement = "$1")
    if (serviceName !in this.serviceLoadBalancers.keys) {
      return Publishers.just(HttpResponse.notFound<Any>())
    }
    val loadBalancer = this.serviceLoadBalancers[serviceName]!!
    return Flowable.fromPublisher(loadBalancer.select())
      .flatMap { targetServiceInstance ->
        val proxiedRequest = this.prepareForRequestTarget(request!!, targetServiceInstance)
        logger.info { "Proxying ${request.path} to ${proxiedRequest.uri} (${targetServiceInstance.instanceId})" }
        Flowable.fromPublisher(this.proxyHttpClient.proxy(proxiedRequest))
      }
  }

  private fun prepareForRequestTarget(request: HttpRequest<*>, serviceInstance: ServiceInstance): MutableHttpRequest<out Any> {
    return request.mutate()
      .uri { uri ->
        val serviceInstanceName = serviceInstance.instanceId
          .map { it.split(":")[0] }
          .orElseThrow { IllegalStateException("Service instance ID is not present") }
        uri.scheme("http")
          .host(serviceInstance.host)
          .port(serviceInstance.port)
          .replacePath(request.path.replace("/$serviceInstanceName", ""))
      }
  }
}
