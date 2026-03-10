package dk.frankbille.iou

import dk.frankbille.iou.test.IntegrationTestConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(IntegrationTestConfiguration::class)
class IouApiApplicationTests {

    @Test
    fun contextLoads() {
    }

}