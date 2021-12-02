package com.msd.application

import com.msd.application.dto.BlockEventDTO
import com.msd.application.dto.EnergyRegenEvent
import com.msd.application.dto.EventDTO
import com.msd.application.dto.MovementEventDTO
import com.msd.command.application.command.BlockCommand
import com.msd.command.application.command.Command
import com.msd.command.application.command.EnergyRegenCommand
import com.msd.command.application.command.MovementCommand
import com.msd.core.FailureException
import com.msd.domain.DomainEvent
import com.msd.robot.domain.exception.RobotNotFoundException
import com.msd.robot.domain.RobotRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class ExceptionConverter(
    private val kafkaMessageProducer: KafkaMessageProducer,
    private val robotRepository: RobotRepository
) {
    @Value(value = "\${spring.kafka.topic.producer.robot-movement}")
    private lateinit var movementTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-blocked}")
    private lateinit var planetBlockedTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-mining}")
    private lateinit var miningTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-fighting}")
    private lateinit var fightingTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-regeneration}")
    private lateinit var regenerationTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-item-fighting}")
    private lateinit var itemFightingTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-item-repair}")
    private lateinit var itemRepairTopic: String

    @Value(value = "\${spring.kafka.topic.producer.robot-item-movement}")
    private lateinit var itemMovementTopic: String

    private val eventVersion = 1

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handle(fe: FailureException, command: Command) {
        when (val event = getEventFromCommandAndException(command, fe)) {
            is MovementEventDTO -> {
                kafkaMessageProducer.send(
                    movementTopic,
                    buildFailureDomainEvent(
                        event, EventType.MOVEMENT, command.transactionUUID
                    )
                )
            }
            is BlockEventDTO -> {
                kafkaMessageProducer.send(
                    planetBlockedTopic,
                    buildFailureDomainEvent(
                        event, EventType.PLANET_BLOCKED, command.transactionUUID
                    )
                )
            }
            is EnergyRegenEvent -> {
                kafkaMessageProducer.send(
                    regenerationTopic,
                    buildFailureDomainEvent(
                        event, EventType.REGENERATION, command.transactionUUID
                    )
                )
            }
//            is NotEnoughItemsException -> TODO()
//            is TargetRobotOutOfReachException -> TODO()
//            is UpgradeException -> TODO()
//            is TargetPlanetNotReachableException -> TODO()
        }
    }

    fun handleAll(exception: FailureException, commands: List<Command>) {
        commands.forEach {
            handle(exception, it)
        }
    }

    private fun getEventFromCommandAndException(command: Command, e: FailureException): EventDTO {
        val robot = if(e is RobotNotFoundException)
            null
        else
            robotRepository.findByIdOrNull(command.robotUUID)
        return when (command) {
            is MovementCommand -> MovementEventDTO(
                false,
                e.message!!,
                robot?.energy,
                null,
                listOf()
            )
            is BlockCommand -> BlockEventDTO(
                false,
                e.message!!,
                null,
                robot?.energy
            )
            is EnergyRegenCommand -> EnergyRegenEvent(
                false, e.message!!, robot?.energy
            )
//            is MiningCommand -> EventType.MINING
//            is ReparationItemUsageCommand -> EventType.ITEM_REPAIR
//            is MovementItemsUsageCommand -> EventType.ITEM_MOVEMENT
            else -> throw IllegalArgumentException("This is not a Heterogeneous command")
        }
    }

    private fun buildFailureDomainEvent(
        failureEventDTO: EventDTO,
        eventType: EventType,
        transactionId: UUID
    ): DomainEvent<Any> {
        return DomainEvent(
            failureEventDTO,
            eventType.eventString,
            transactionId.toString(),
            eventVersion,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )
    }
}
