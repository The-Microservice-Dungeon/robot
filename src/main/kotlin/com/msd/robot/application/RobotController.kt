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

    @PostMapping
    fun spawnRobot(@RequestBody spawnDto: RobotSpawnDto): ResponseEntity<RobotDto> {
        val robot = robotApplicationService.spawn(spawnDto.player, spawnDto.planet)
        return ResponseEntity.ok(robotMapper.robotToRobotDto(robot))
    }

    @GetMapping
    fun getRobotOfPlayer(@Param("player-id") playerId: UUID): ResponseEntity<List<RobotDto>> {
        val robots = robotDomainService.getRobotsByPlayer(playerId)
        return ResponseEntity.ok(robotMapper.robotsToRobotDtos(robots))
    }

    @GetMapping("/{id}")
    fun getRobot(@PathVariable("id") robotId: UUID): ResponseEntity<RobotDto> {
        val robot = robotDomainService.getRobot(robotId)
        return ResponseEntity.ok(robotMapper.robotToRobotDto(robot))
    }

    @PostMapping("/{id}/upgrades")
    fun upgradeRobot(@PathVariable("id") robotId: UUID, @RequestBody upgradeDto: UpgradeDto): ResponseEntity<String> {
        robotApplicationService.upgrade(robotId, upgradeDto.`upgrade-type`, upgradeDto.`target-level`)
        return ResponseEntity.ok("") // TODO remove target-level and return new level in DTO
    }

    @PostMapping("/{id}/inventory/items")
    fun addItemToRobot(@PathVariable("id") robotId: UUID, @RequestBody addItemDto: ItemAdditionDto): ResponseEntity<String> {
        robotDomainService.addItem(robotId, addItemDto.`item-type`)
        return ResponseEntity.ok("Item ${addItemDto.`item-type`} added to robot $robotId")
    }

    @PostMapping("/{id}/inventory/clearResources")
    fun clearAllResourcesOfRobot(@PathVariable("id") robotId: UUID): ResponseEntity<Map<ResourceType, Int>> {
        val takenResources = robotDomainService.takeAllResources(robotId)
        return ResponseEntity.ok(takenResources)
    }
}
