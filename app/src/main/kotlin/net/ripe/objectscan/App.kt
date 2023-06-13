/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package net.ripe.objectscan

import io.github.oshai.kotlinlogging.KotlinLogging
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificateParser
import net.ripe.rpki.commons.validation.ValidationResult
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.streams.asStream
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}


class App(val base: String){
    val basePath = File(base).toPath()

    fun parseFile(path: Path) {
        try {
            var validationResult = ValidationResult.withLocation(path.toUri());
            X509ResourceCertificateParser.parseCertificate(validationResult, Files.readAllBytes(path))

            if (validationResult.hasFailureForCurrentLocation()) {
                logger.info("Parsed {}", basePath.relativize(path))
                logger.info("failures for {}: {}", path, validationResult.failuresForAllLocations)
            }
        } catch (e: IOException) {
        }
    }

    fun run() {
        val paths = basePath.toFile().walkTopDown()
                .asStream()
                .map(File::toPath)
                .filter(Predicate.not(Path::isDirectory))
                .filter { it.extension.equals("cer") }
                .collect(Collectors.toList())

        logger.info("Gathered {} files.", paths.size)

        paths.parallelStream()
                .forEach { parseFile(it) }
    }
}

fun main(args: Array<String>) {
    if (args.size < 1) {
       logger.error("Please pass a path as argument.")
        exitProcess(1)
    }
    logger.info("Scanning {}", args[0])
    App(args[0]).run()
    exitProcess(0)
}
