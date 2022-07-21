package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.msd.application.dto.GameMapPlanetDto
import com.msd.application.dto.MineRequestDto
import com.msd.application.dto.MineResponseDto
import com.msd.config.properties.MicroserviceMapConfig
import com.msd.config.kafka.core.FailureException
import com.msd.domain.ResourceType
import com.msd.robot.application.exception.TargetPlanetNotReachableException
import com.msd.robot.application.exception.UnknownPlanetException
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class GameMapService(
    @Autowired val mapConfig: MicroserviceMapConfig
) {

    private val gameMapClient: WebClient
    private val logger = KotlinLogging.logger {}

    object GameMapServiceMetaData {
        const val PLANETS_URI = "/planets"
    }

    init {
        val httpClient: HttpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofMillis(5000))
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS))
            }

        gameMapClient = WebClient.builder()
            .baseUrl(mapConfig.address)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .build()
    }

    /**
     * Calls the GameMap MicroService to determine whether two [Planet]s are neighbors and returns a [GameMapPlanetDto]
     * of the target planet, if they do. Otherwise an [TargetPlanetNotReachableException] gets thrown.
     * If the MicroService is not reachable or has an internal error during processing of the request,
     * a [ClientException] gets thrown.
     *
     * @param startPlanetID: The ID of the planet the robot wants to move away from
     * @param targetPlanetID: The ID of the planet the robot wants to move to
     *
     * @return A DTO of the planet the robot moves to
     */
    fun retrieveTargetPlanetIfRobotCanReach(startPlanetID: UUID, targetPlanetID: UUID): GameMapPlanetDto {
        val uriSpec = gameMapClient.get()
        val querySpec = uriSpec.uri {
            it.path("${GameMapServiceMetaData.PLANETS_URI}/$targetPlanetID").build()
        }
        try {
            val response = querySpec.exchangeToMono { response ->
                if (response.statusCode() == HttpStatus.OK)
                    response.bodyToMono<String>()
                else if (response.statusCode() == HttpStatus.BAD_REQUEST)
                    throw FailureException("The requested planet does not exist")
                else
                    throw ClientException("Could not get planet data from MapService")
            }.block()!!
            val planetDto: GameMapPlanetDto = jacksonObjectMapper().readValue(response)
            if (planetDto.neighbours.find { it.planetId == startPlanetID } != null)
                return planetDto
            else {
                throw TargetPlanetNotReachableException("The robot cannot move to the planet with ID $targetPlanetID")
            }
        } catch (wcre: WebClientRequestException) {
            logger.error("Map Service Client failed to connect to the map service with exception: ${wcre.message}")
            throw ClientException("Could not connect to Map Service")
        }
    }

    fun getPlanet(planetId: UUID): GameMapPlanetDto {
        val uriSpec = gameMapClient.get()
        val querySpec = uriSpec.uri {
            it.path("${GameMapServiceMetaData.PLANETS_URI}/$planetId").build()
        }
        try {
            val response = querySpec.exchangeToMono { response ->
                if (response.statusCode() == HttpStatus.OK)
                    response.bodyToMono<String>()
                else if (response.statusCode() == HttpStatus.BAD_REQUEST)
                    throw FailureException("The requested planet does not exist")
                else
                    throw ClientException("Could not get planet data from MapService")
            }.block()!!
            return jacksonObjectMapper().readValue(response)
        } catch (wcre: WebClientRequestException) {
            logger.error("Map Service Client failed to connect to the map service with exception: ${wcre.message}")
            throw ClientException("Could not connect to Map Service")
        }
    }

    /**
     * Retrieves all `Planets` from the Map Service
     *
     * @return a `List` of [GameMapPlanetDtos] [GameMapPlanetDto] containing all `Planets`
     * @throws ClientException  when the GameMap Microservice is down
     */
    fun getAllPlanets(): List<GameMapPlanetDto> {
        val uriSpec = gameMapClient.get()
        val querySpec = uriSpec.uri {
            it.path(GameMapServiceMetaData.PLANETS_URI)
                .build()
        }
        try {
            val response = querySpec.exchangeToMono { response ->
                if (response.statusCode() == HttpStatus.OK)
                    response.bodyToMono<List<GameMapPlanetDto>>()
                else {
                    logger.error("GameMap Client returned internal error when retrieving targetPlanet for movement")
                    throw ClientException(
                        "GameMap Client returned internal error when retrieving all planets"
                    )
                }
            }.block()!!
            return response
        } catch (wcre: WebClientRequestException) {
            logger.error("Map Service Client failed to connect to the map service with exception: ${wcre.message}")
            throw ClientException("Could not connect to Map Service")
        }
    }

    fun getResourceOnPlanet(planetId: UUID): ResourceType {
        val uriSpec = gameMapClient.get()
        val querySpec = uriSpec.uri {
            it.path(GameMapServiceMetaData.PLANETS_URI + "/$planetId").build()
        }
        try {
            val response = querySpec.exchangeToMono { response ->
                if (response.statusCode() == HttpStatus.OK)
                    response.bodyToMono<GameMapPlanetDto>()
                else if (response.statusCode().is4xxClientError)
                    throw UnknownPlanetException(planetId)
                else {
                    logger.error("Received non-200 Return Code: " + response.statusCode())
                    logger.error("GameMap Client returned internal error while retrieving resource from planet")
                    logger.info { response.statusCode() }
                    throw ClientException(
                        "GameMap Client returned internal error when retrieving resource on planet $planetId"
                    )
                }
            }.block()!!
            return response.resource?.resourceType ?: throw NoResourceOnPlanetException(planetId)
        } catch (wcre: WebClientRequestException) {
            logger.error("Map Service Client failed to connect to the map service with exception: ${wcre.message}")
            throw ClientException("Could not connect to Map Service")
        }
    }

    fun mine(planetId: UUID, amount: Int): Int {
        val uriSpec = gameMapClient.post()
        val querySpec = uriSpec.uri {
            it.path(GameMapServiceMetaData.PLANETS_URI + "/$planetId/minings").build()
        }.body(Mono.just(MineRequestDto(amount)), MineRequestDto::class.java)

        try {
            val response = querySpec.exchangeToMono { response ->
                if (response.statusCode() == HttpStatus.CREATED)
                    response.bodyToMono<MineResponseDto>()
                else if (response.statusCode() == HttpStatus.NOT_FOUND)
                    throw UnknownPlanetException(planetId)
                else {
                    logger.error("GameMap Client returned internal error while mining on planet $planetId")
                    throw ClientException("Map Service returned internal error when trying to mine on planet $planetId")
                }
            }.block()!!
            return response.amount_mined
        } catch (wcre: WebClientRequestException) {
            logger.error("Map Service Client failed to connect to the map service with exception: ${wcre.message}")
            throw ClientException("Could not connect to Map Service")
        }
    }
}
