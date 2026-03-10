package dk.frankbille.iou

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class IouApiApplication

fun main(args: Array<String>) {
	runApplication<IouApiApplication>(*args)
}
