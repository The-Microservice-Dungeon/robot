package com.msd.command.application

import com.msd.command.domain.BlockCommand
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class CommandApplicationServiceTest(
    @Autowired val commandApplicationService: CommandApplicationService
) {

    val randomUUID = UUID.randomUUID()

    @Test
    fun `Parses all valid command types`() {
        val commandStrings = arrayListOf(
            "block $randomUUID  $randomUUID  $randomUUID",
            "movE $randomUUID  $randomUUID  $randomUUID $randomUUID",
            "fight $randomUUID  $randomUUID  $randomUUID $randomUUID",
            "MINE $randomUUID  $randomUUID  $randomUUID",
            "regenerate $randomUUID  $randomUUID  $randomUUID",
            "Use-Item-Fighting $randomUUID  $randomUUID Rocket $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID bombardEment $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID self_destruct $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID REPARATION_SWARM $randomUUID  $randomUUID ",
            "Use-Item-Fighting $randomUUID  $randomUUID NuKe $randomUUID  $randomUUID",
            "use-item-movement $randomUUID  $randomUUID wormhole $randomUUID  $randomUUID "
        )

        val commands = commandApplicationService.parseCommandsFromStrings(commandStrings)

        assertAll(
            {
                assert(commands.find { it::class == BlockCommand::class } != null)
            },
        )
    }
}
