/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package net.ripe.objectscan

import io.github.oshai.kotlinlogging.KotlinLogging
import net.ripe.rpki.commons.crypto.cms.aspa.AspaCmsParser
import net.ripe.rpki.commons.crypto.cms.ghostbuster.GhostbustersCmsParser
import net.ripe.rpki.commons.crypto.cms.manifest.ManifestCmsParser
import net.ripe.rpki.commons.crypto.cms.roa.RoaCmsParser
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificateParser
import net.ripe.rpki.commons.util.RepositoryObjectType
import net.ripe.rpki.commons.validation.ValidationCheck
import net.ripe.rpki.commons.validation.ValidationResult
import org.bouncycastle.jce.provider.X509CRLParser
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.streams.asStream
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}


class App(val base: String){
    val basePath = File(base).toPath()

    fun parseFile(path: Path): Stream<Pair<String, ValidationCheck>> {
        try {
            var validationResult = ValidationResult.withLocation(path.toUri());
            val bytes = Files.readAllBytes(path)

            when (RepositoryObjectType.parse(path.fileName.toString())) {
                RepositoryObjectType.Manifest -> {
                    ManifestCmsParser().parse(validationResult, bytes)
                }
                RepositoryObjectType.Aspa -> {
                    AspaCmsParser().parse(validationResult, bytes)
                }
                RepositoryObjectType.Certificate -> {
                       X509ResourceCertificateParser().parse(validationResult, bytes)
                }
                RepositoryObjectType.Crl -> {
                    // ignored
                }
                RepositoryObjectType.Gbr -> {
                    GhostbustersCmsParser().parse(validationResult, bytes)
                }
                RepositoryObjectType.Roa -> {
                    RoaCmsParser().parse(validationResult, bytes)
                }
                RepositoryObjectType.Unknown -> {
                    logger.info("Skipping {}", path)
                }
            }

            return Stream.concat(
                validationResult.failuresForAllLocations.map { failure ->
                    Pair(basePath.relativize(path).toString(), failure)
                }.stream(),
                validationResult.warnings.map { warning ->
                    Pair(basePath.relativize(path).toString(), warning)
                }.stream()
            )
        } catch (ex: Exception) {
            logger.error("Exception parsing {}: {}", path, ex)
        }

        return Stream.empty()
    }

    fun run() {
        val paths = basePath.toFile().walkTopDown()
                .asStream()
                .map(File::toPath)
                .filter(Predicate.not(Path::isDirectory))
                .collect(Collectors.toList())

        logger.info("Gathered {} files.", paths.size)

        val failures = paths.parallelStream()
                .flatMap(this::parseFile)
                .collect(Collectors.toList())
        logger.info("Finished processing.")

        failures.groupBy { failedObject -> Pair(failedObject.second.status, failedObject.second.key) }.forEach() { (group, failedChecksAndNames) ->
            val (status, key) = group
            logger.info("level: {} check: {} {} failures", status, key, failedChecksAndNames.size)
            failedChecksAndNames.forEach { elem ->
                logger.info("  * {}", elem.first)
            }
        }
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
