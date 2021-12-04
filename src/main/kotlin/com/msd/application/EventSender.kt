package com.msd.application

import com.msd.application.dto.*
import com.msd.command.application.command.BlockCommand
import com.msd.command.application.command.Command
import com.msd.command.application.command.EnergyRegenCommand
import com.msd.command.application.command.MovementCommand
import com.msd.core.FailureException
import com.msd.domain.DomainEvent
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.exception.RobotNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class EventSender(
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

    @Value(value = "\${spring.kafka.topic.producer.robot-resource-distribution}")
    private lateinit var resourceDistributionTopic: String

    private val eventVersion = 1

    /**
     * Convert the Exception into a corresponding Kafka Event
     */
    fun handle(fe: FailureException, command: Command) {
        when (val event = getEventFromCommandAndException(command, fe)) {
            is MovementEventDTO -> {
                kafkaMessageProducer.send(
                    movementTopic,
                    buildDomainEvent(
                        event, EventType.MOVEMENT, command.transactionUUID
                    )
                )
            }
            is BlockEventDTO -> {
                kafkaMessageProducer.send(
                    planetBlockedTopic,
                    buildDomainEvent(
                        event, EventType.PLANET_BLOCKED, command.transactionUUID
                    )
                )
            }
            is EnergyRegenEvent -> {
                kafkaMessageProducer.send(
                    regenerationTopic,
                    buildDomainEvent(
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

    fun sendEvent(event: GenericEventDTO, transactionId: UUID): UUID {
        val domainEvent = buildDomainEvent(event, getEventTypeByEvent(event), transactionId)
        kafkaMessageProducer.send(
            getTopicByEvent(event),
            domainEvent
        )
        return UUID.fromString(domainEvent.id)
    }

    fun sendGenericEvent(event: GenericEventDTO) {
        kafkaMessageProducer.send(
            getTopicByEvent(event),
            buildDomainEvent(event, getEventTypeByEvent(event), UUID.fromString("00000000-0000-0000-0000-000000000000"))
        )
    }

    private fun buildDomainEvent(
        eventDTO: GenericEventDTO,
        eventType: EventType,
        transactionId: UUID
    ): DomainEvent<Any> {
        return DomainEvent(
            eventDTO,
            eventType.eventString,
            transactionId.toString(),
            eventVersion,
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        )
    }

    private fun getTopicByEvent(event: GenericEventDTO): String {
        return when (event) {
            is MiningEventDTO -> miningTopic
            is FightingEventDTO -> fightingTopic
            is ResourceDistributionEventDTO -> resourceDistributionTopic
            is ItemFightingEventDTO -> itemFightingTopic
            else -> TODO()
        }
    }

    private fun getEventTypeByEvent(event: GenericEventDTO): EventType {
        return when (event) {
            is MiningEventDTO -> EventType.MINING
            is FightingEventDTO -> EventType.FIGHTING
            is ResourceDistributionEventDTO -> EventType.RESOURCE_DISTRIBUTION
            is ItemFightingEventDTO -> EventType.ITEM_FIGHTING
            else -> TODO()
        }
    }

    private fun getEventFromCommandAndException(command: Command, e: FailureException): EventDTO {
        val robot = if (e is RobotNotFoundException)
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
}
