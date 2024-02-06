package dev.da0hn.course.micronaut.gateway

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.http.client.LoadBalancer
import io.micronaut.http.client.loadbalance.DiscoveryClientLoadBalancerFactory

@Factory
class GatewayLoadBalancersFactory {

  @Bean
  fun serviceLoadBalancers(
    gatewayProperties: GatewayProperties,
    factory: DiscoveryClientLoadBalancerFactory,
  ): Map<String, LoadBalancer> {
    val loadBalancers = mutableMapOf<String, LoadBalancer>()
    gatewayProperties.services.forEach { serviceName -> loadBalancers[serviceName] = factory.create(serviceName) }
    return loadBalancers
  }

}
