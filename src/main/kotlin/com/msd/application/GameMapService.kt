package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.msd.robot.application.exception.TargetPlanetNotReachableException
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class GameMapService {

    private val gameMapClient: WebClient

    object GameMapServiceMetaData {
        const val GAME_MAP_SERVICE_URL = "http://localhost:8080" // TODO change port in the future
        const val NEIGHBOR_CHECK_URI = "/getNeighbor"
        const val PLANETS_URI = "/planets"
        const val NEIGHBOR_CHECK_START_PLANET_PARAM = "startPlanet"
        const val NEIGHBOR_CHECK_TARGET_PLANET_PARAM = "targetPlanet"
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
            .baseUrl(GameMapServiceMetaData.GAME_MAP_SERVICE_URL)
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
            it.path(GameMapServiceMetaData.NEIGHBOR_CHECK_URI)
                .queryParam(GameMapServiceMetaData.NEIGHBOR_CHECK_START_PLANET_PARAM, startPlanetID.toString())
                .queryParam(GameMapServiceMetaData.NEIGHBOR_CHECK_TARGET_PLANET_PARAM, targetPlanetID.toString())
                .build()
        }
        try {
            val response = querySpec.exchangeToMono { response ->
                if (response.statusCode() == HttpStatus.OK)
                    response.bodyToMono<String>()

                // Right now we assume a 4xx being returned if the two planets are no neighbors
                else if (response.statusCode().is4xxClientError)
                    throw TargetPlanetNotReachableException("The robot cannot move to the planet with ID $targetPlanetID")

                else
                    throw ClientException(
                        "GameMap Client returned internal error when retrieving targetPlanet " +
                            "for movement"
                    )
            }.block()!!
            return jacksonObjectMapper().readValue(response)
        } catch (wcre: WebClientRequestException) {
            throw ClientException("Could not connect to GameMap client")
        }
    }

    /**
     * Retrieves all `Planets` from the GameMap Microservice.
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
                else
                    throw ClientException(
                        "GameMap Client returned internal error when retrieving all planets"
                    )
            }.block()!!
            return response
        } catch (wcre: WebClientRequestException) {
            throw ClientException("Could not connect to GameMap MicroService")
        }
    }
}
