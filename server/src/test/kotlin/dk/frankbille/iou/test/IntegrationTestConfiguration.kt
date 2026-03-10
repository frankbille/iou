package dk.frankbille.iou.test

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.mysql.MySQLContainer

@TestConfiguration(proxyBeanMethods = false)
class IntegrationTestConfiguration {

    @Bean
    @ServiceConnection
    fun mySQLContainer() = MySQLContainer("mysql:8")

}