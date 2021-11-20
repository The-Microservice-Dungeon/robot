package com.msd.robot.application

import com.msd.robot.application.dtos.RobotDto
import com.msd.robot.application.dtos.RobotSpawnDto
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("robots")
class RobotController(
    val robotApplicationService: RobotApplicationService,
    val robotMapper: RobotMapper
) {

    @PostMapping
    fun spawnRobot(@RequestBody spawnDto: RobotSpawnDto): ResponseEntity<RobotDto> {
        val robot = robotApplicationService.spawn(spawnDto.player, spawnDto.planet)
        return ResponseEntity.ok(robotMapper.robotToRobotDto(robot))
    }
}
