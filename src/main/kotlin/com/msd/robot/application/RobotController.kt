package com.msd.robot.application

import com.msd.domain.ResourceType
import com.msd.robot.application.dtos.*
import com.msd.robot.domain.RobotDomainService
import org.springframework.data.repository.query.Param
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.util.*

@Controller
@RequestMapping("robots")
class RobotController(
    val robotApplicationService: RobotApplicationService,
    val robotDomainService: RobotDomainService,
    val robotMapper: RobotMapper
) {

    /**
     * Spawns a new robot with the attributes given in [RobotSpawnDto] and returns a full [RobotDto] for the
     * newly created robot.
     *
     * @see <a href="https://the-microservice-dungeon.github.io/docs/openapi/robot#tag/robot/paths/~1robots/post"></a>
     */
    @PostMapping
    fun spawnRobot(@RequestBody spawnDto: RobotSpawnDto): ResponseEntity<RobotDto> {
        val robot = robotApplicationService.spawn(spawnDto.player, spawnDto.planet)
        return ResponseEntity.ok(robotMapper.robotToRobotDto(robot))
    }

    /**
     * Get all robots of the specified player.
     *
     * @see <a href="https://the-microservice-dungeon.github.io/docs/openapi/robot#tag/robot/paths/~1robots/get"></a>
     * @return A List of [RobotDto]s
     */
    @GetMapping
    fun getRobotsOfPlayer(@Param("player-id") playerId: UUID): ResponseEntity<List<RobotDto>> {
        val robots = robotDomainService.getRobotsByPlayer(playerId)
        return ResponseEntity.ok(robotMapper.robotsToRobotDtos(robots))
    }

    /**
     * Get a specific robot by its UUID.
     *
     * @see <a href="https://the-microservice-dungeon.github.io/docs/openapi/robot#tag/robot/paths/~1robots~1{robot-uuid}/get"></a>
     * @return a [RobotDto] of the specified robot
     */
    @GetMapping("/{id}")
    fun getRobot(@PathVariable("id") robotId: UUID): ResponseEntity<RobotDto> {
        val robot = robotDomainService.getRobot(robotId)
        return ResponseEntity.ok(robotMapper.robotToRobotDto(robot))
    }

    /**
     * Upgrade a specific skill of the specified robot.
     *
     * @see <a href="https://the-microservice-dungeon.github.io/docs/openapi/robot#tag/trading/paths/~1robots~1{robot-uuid}~1upgrades/post"></a>
     * @return success message
     */
    @PostMapping("/{id}/upgrades")
    fun upgradeRobot(@PathVariable("id") robotId: UUID, @RequestBody upgradeDto: UpgradeDto): ResponseEntity<String> {
        robotApplicationService.upgrade(robotId, upgradeDto.`upgrade-type`, upgradeDto.`target-level`)
        return ResponseEntity.ok("") // TODO remove target-level and return new level in DTO
    }

    /**
     * Give the specified robot an item of type addItemDto.`item-type`.
     *
     * @see <a href="https://the-microservice-dungeon.github.io/docs/openapi/robot#tag/trading/paths/~1robots~1{robot-uuid}~1inventory~1items/post"></a>
     * @return success message
     */
    @PostMapping("/{id}/inventory/items")
    fun addItemToRobot(
        @PathVariable("id") robotId: UUID,
        @RequestBody addItemDto: ItemAdditionDto
    ): ResponseEntity<String> {
        robotDomainService.addItem(robotId, addItemDto.`item-type`)
        return ResponseEntity.ok("Item ${addItemDto.`item-type`} added to robot $robotId")
    }

    /**
     * Empty the inventory of the specified robot of all resources and return the removed amount.
     *
     * @see <a href="https://the-microservice-dungeon.github.io/docs/openapi/robot#tag/trading/paths/~1robots~1{robot-uuid}~1inventory~1clearResources/post"></a>
     * @return a Map pairing the [ResourceType]s to the amount the robot had.
     */
    @PostMapping("/{id}/inventory/clearResources")
    fun clearAllResourcesOfRobot(@PathVariable("id") robotId: UUID): ResponseEntity<Map<ResourceType, Int>> {
        val takenResources = robotDomainService.takeAllResources(robotId)
        return ResponseEntity.ok(takenResources)
    }
}
