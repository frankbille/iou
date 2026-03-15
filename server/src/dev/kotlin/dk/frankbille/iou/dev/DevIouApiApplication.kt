package dk.frankbille.iou.dev

import org.springframework.boot.SpringApplication
import org.springframework.boot.devtools.restart.RestartScope
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.with
import org.springframework.context.annotation.Bean
import org.testcontainers.mysql.MySQLContainer
import dk.frankbille.iou.main as springMain

@TestConfiguration
class DevIouApiApplication {
    @Bean
    @RestartScope
    @ServiceConnection
    fun mySQLContainer() = MySQLContainer("mysql:8")
}

fun main(args: Array<String>) {
    SpringApplication
        .from(::springMain)
        .with(DevIouApiApplication::class)
        .run(*args)
}
