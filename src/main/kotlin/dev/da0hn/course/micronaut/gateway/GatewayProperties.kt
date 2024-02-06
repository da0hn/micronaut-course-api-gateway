package dev.da0hn.course.micronaut.gateway

import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("gateway")
class GatewayProperties {
  lateinit var services: Set<String>
}
