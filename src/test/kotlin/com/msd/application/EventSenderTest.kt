package com.msd.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.msd.command.application.command.*
import com.msd.domain.DomainEvent
import com.msd.domain.ResourceType
import com.msd.event.application.EventSender
import com.msd.event.application.EventType
import com.msd.event.application.ProducerTopicConfiguration
import com.msd.event.application.dto.*
import com.msd.item.domain.AttackItemType
import com.msd.item.domain.MovementItemType
import com.msd.item.domain.RepairItemType
import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetRepository
import com.msd.robot.application.exception.TargetPlanetNotReachableException
import com.msd.robot.application.exception.UnknownPlanetException
import com.msd.robot.domain.LevelTooLowException
import com.msd.robot.domain.Robot
import com.msd.robot.domain.RobotRepository
import com.msd.robot.domain.exception.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = ["listeners=PLAINTEXT://\${spring.kafka.bootstrap-servers}", "port=9092"]
)
@Transactional
internal class EventSenderTest(
    @Autowired private val eventSender: EventSender,
    @Autowired private val embeddedKafka: EmbeddedKafkaBroker,
    @Autowired private val robotRepository: RobotRepository,
    @Autowired private val planetRepository: PlanetRepository,
    @Autowired private val topicConfig: ProducerTopicConfiguration
) : AbstractKafkaProducerTest(embeddedKafka, topicConfig) {

    private lateinit var robotId: UUID

    @BeforeEach
    override fun setup() {
        super.setup()
        val robot = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotId = robot.id
        robotRepository.save(robot)
    }

    @Test
    fun `when a PlanetBlockedException is handled when moving an Event is thrown in the 'movement' topic`() {
        // given
        startMovementContainer()

        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val planetBlockedException = PlanetBlockedException("Planet is blocked")
        // when
        eventSender.handleException(planetBlockedException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )
        eventTestUtils.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventTestUtils.checkMovementPayload(false, "Planet is blocked", 20, null, listOf(), domainEvent.payload)
    }

    @Test
    fun `When a NotEnoughEnergyException is handled when moving an event is sent to the 'movement' topic`() {
        // given
        startMovementContainer()

        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Not enough Energy")
        // when
        eventSender.handleException(notEnoughEnergyException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventTestUtils.checkMovementPayload(false, "Not enough Energy", 20, null, listOf(), domainEvent.payload)
    }

    @Test
    fun `when a TargetPlanetNotReachableException is thrown while moving an event is send to 'movement' topic`() {
        // given
        startMovementContainer()

        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val targetPlanetNotReachableException = TargetPlanetNotReachableException("Planet not reachable")
        // when
        eventSender.handleException(targetPlanetNotReachableException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventTestUtils.checkMovementPayload(false, "Planet not reachable", 20, null, listOf(), domainEvent.payload)
    }

    @Test
    fun `when a UnknownPlanetException is thrown while moving an event is send to 'movement' topic`() {
        // given
        startMovementContainer()

        val planetId = UUID.randomUUID()
        val movementCommand = MovementCommand(robotId, UUID.randomUUID(), planetId)
        val unknownPlanetException = UnknownPlanetException(planetId)
        // when
        eventSender.handleException(unknownPlanetException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventTestUtils.checkMovementPayload(
            false,
            "Map Service doesn't have a planet with the id $planetId",
            20,
            null,
            listOf(),
            domainEvent.payload
        )
    }

    @Test
    fun `when a RobotNotFoundException is handled due to Movement an event is send to the 'movement' topic`() {
        // given
        startMovementContainer()

        val movementCommand = MovementCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot Not Found")
        // when
        eventSender.handleException(robotNotFoundException, movementCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(movementCommand.transactionUUID, EventType.MOVEMENT, domainEvent)
        eventTestUtils.checkMovementPayload(false, "Robot Not Found", null, null, listOf(), domainEvent.payload)
    }

    @Test
    fun `when NotEnoughEnergyException is thrown while blocking an event is send to 'planet-blocked' topic`() {
        // given
        startPlanetBlockedContainer()

        val blockCommand = BlockCommand(robotId, UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Robot has not enough Energy")
        // when
        eventSender.handleException(notEnoughEnergyException, blockCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_BLOCKED, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), BlockEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(blockCommand.transactionUUID, EventType.PLANET_BLOCKED, domainEvent)
        eventTestUtils.checkBlockPayload(false, "Robot has not enough Energy", null, 20, domainEvent.payload)
    }

    @Test
    fun `when RobotNotFoundException is thrown while blocking an event is send to 'planet-blocked' topic`() {
        // given
        startPlanetBlockedContainer()

        val blockCommand = BlockCommand(UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot not Found")
        // when
        eventSender.handleException(robotNotFoundException, blockCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_BLOCKED, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), BlockEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(blockCommand.transactionUUID, EventType.PLANET_BLOCKED, domainEvent)
        eventTestUtils.checkBlockPayload(false, "Robot not Found", null, null, domainEvent.payload)
    }

    @Test
    fun `when RobotNotFoundException is thrown when regenerating an event is send to 'regeneration' topic`() {
        // given
        startRegenerationContainer()

        val regenCommand = EnergyRegenCommand(UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot not Found")
        // when
        eventSender.handleException(robotNotFoundException, regenCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_REGENERATION, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), RegenerationEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(regenCommand.transactionUUID, EventType.REGENERATION, domainEvent)
        eventTestUtils.checkRegenerationPayload(false, "Robot not Found", null, domainEvent.payload)
    }

    @Test
    fun `when RobotNotFoundException is thrown while mining an event is send to 'mining' topic`() {
        // given
        startMiningContainer()
        val planet = Planet(UUID.randomUUID(), ResourceType.COAL)
        planetRepository.save(planet)

        val miningCommand = MiningCommand(UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot not found")
        // when
        eventSender.handleException(robotNotFoundException, miningCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MINING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MiningEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(miningCommand.transactionUUID, EventType.MINING, domainEvent)
        eventTestUtils.checkMiningPayload(false, "Robot not found", null, 0, "NONE", domainEvent.payload)
    }

    @Test
    fun `when NotEnoughEnergyException is thrown while mining an event is send to 'mining' topic`() {
        // given
        startMiningContainer()
        val planet = Planet(UUID.randomUUID(), ResourceType.COAL)
        planetRepository.save(planet)
        val robot = robotRepository.findByIdOrNull(robotId)
        robot!!.move(planet, 0)
        robotRepository.save(robot)

        val miningCommand = MiningCommand(robotId, UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Not enough energy")
        // when
        eventSender.handleException(notEnoughEnergyException, miningCommand)

        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MINING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MiningEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(miningCommand.transactionUUID, EventType.MINING, domainEvent)
        eventTestUtils.checkMiningPayload(false, "Not enough energy", 20, 0, "COAL", domainEvent.payload)
    }

    @Test
    fun `when NoResourceOnPlanetException is thrown while mining an event is send to 'mining' topic`() {
        // given
        startMiningContainer()
        val planet = Planet(UUID.randomUUID(), null)
        planetRepository.save(planet)
        val robot = robotRepository.findByIdOrNull(robotId)
        robot!!.move(planet, 0)
        robotRepository.save(robot)

        val miningCommand = MiningCommand(robotId, UUID.randomUUID())
        val noResourceOnPlanetException = NoResourceOnPlanetException(planet.planetId)
        // when
        eventSender.handleException(noResourceOnPlanetException, miningCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MINING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MiningEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(miningCommand.transactionUUID, EventType.MINING, domainEvent)
        eventTestUtils.checkMiningPayload(
            false,
            "Map Service did not return any resource on the planet ${planet.planetId}",
            20,
            0,
            "NONE",
            domainEvent.payload
        )
    }

    @Test
    fun `when LevelTooLowException is thrown while mining an event is send to 'mining' topic`() {
        // given
        startMiningContainer()
        val planet = Planet(UUID.randomUUID(), ResourceType.PLATIN)
        planetRepository.save(planet)
        val robot = robotRepository.findByIdOrNull(robotId)
        robot!!.move(planet, 0)
        robotRepository.save(robot)

        val miningCommand = MiningCommand(robotId, UUID.randomUUID())
        val levelTooLowException = LevelTooLowException("Level too low")
        // when
        eventSender.handleException(levelTooLowException, miningCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_MINING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), MiningEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(miningCommand.transactionUUID, EventType.MINING, domainEvent)
        eventTestUtils.checkMiningPayload(false, "Level too low", 20, 0, "PLATIN", domainEvent.payload)
    }

    @Test
    fun `when RobotNotFoundException is thrown while fighting, due to the attacker not being found, an event is send to the 'fighting' topic with the attacker being null`() {
        // given
        startFightingContainer()

        val fightingCommand = FightingCommand(UUID.randomUUID(), robotId, UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Attacker not found")
        // when
        eventSender.handleException(robotNotFoundException, fightingCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), FightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingCommand.transactionUUID, EventType.FIGHTING, domainEvent)
        eventTestUtils.checkFightingPayload(
            false,
            "Attacker not found",
            null,
            robotId,
            10,
            null,
            domainEvent.payload
        )
    }

    @Test
    fun `when RobotNotFoundException is thrown while fighting, due to the defender not being found, an event is send to the 'fighting' topic with the defender being null`() {
        // given
        startFightingContainer()

        val fightingCommand = FightingCommand(robotId, UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Defender not found")
        // when
        eventSender.handleException(robotNotFoundException, fightingCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), FightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingCommand.transactionUUID, EventType.FIGHTING, domainEvent)
        eventTestUtils.checkFightingPayload(
            false,
            "Defender not found",
            robotId,
            null,
            null,
            20,
            domainEvent.payload
        )
    }

    @Test
    fun `when NotEnoughEnergyException is thrown while fighting an event is send to 'fighting' topic`() {
        // given
        startFightingContainer()
        val defender = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotRepository.save(defender)

        val fightingCommand = FightingCommand(robotId, defender.id, UUID.randomUUID())
        val notEnoughEnergyException = NotEnoughEnergyException("Not enough energy")
        // when
        eventSender.handleException(notEnoughEnergyException, fightingCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), FightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingCommand.transactionUUID, EventType.FIGHTING, domainEvent)
        eventTestUtils.checkFightingPayload(
            false,
            "Not enough energy",
            robotId,
            defender.id,
            10,
            20,
            domainEvent.payload
        )
    }

    @Test
    fun `when TargetRobotOutOfReachException is thrown while fighting an event is send to 'fighting' topic`() {
        // given
        startFightingContainer()
        val defender = Robot(UUID.randomUUID(), Planet(UUID.randomUUID()))
        robotRepository.save(defender)

        val fightingCommand = FightingCommand(robotId, defender.id, UUID.randomUUID())
        val targetRobotOutOfReachException = TargetRobotOutOfReachException("Target out of reach")
        // when
        eventSender.handleException(targetRobotOutOfReachException, fightingCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), FightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingCommand.transactionUUID, EventType.FIGHTING, domainEvent)
        eventTestUtils.checkFightingPayload(
            false,
            "Target out of reach",
            robotId,
            defender.id,
            10,
            20,
            domainEvent.payload
        )
    }

    @Test
    fun `when RobotNotFoundException is thrown while using fighting item, due to target being not found, an event is send to 'item-fighting' topic`() {
        // given
        startItemFightingContainer()
        val robot = robotRepository.findByIdOrNull(robotId)
        robot!!.inventory.addItem(AttackItemType.ROCKET)
        robot.inventory.addItem(AttackItemType.ROCKET)
        robotRepository.save(robot)

        val fightingItemUsageCommand = FightingItemUsageCommand(robotId, AttackItemType.ROCKET, UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Target Robot not found")
        // when
        eventSender.handleException(robotNotFoundException, fightingItemUsageCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemFightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingItemUsageCommand.transactionUUID, EventType.ITEM_FIGHTING, domainEvent)
        eventTestUtils.checkItemFightingPayload(false, "Target Robot not found", 2, listOf(), domainEvent.payload)
    }

    @Test
    fun `when RobotNotFoundException is thrown while using fighting item, due to attacker being not found, an event is send to 'item-fighting' topic`() {
        // given
        startItemFightingContainer()

        val fightingItemUsageCommand = FightingItemUsageCommand(UUID.randomUUID(), AttackItemType.ROCKET, UUID.randomUUID(), UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Attacker Robot not found")
        // when
        eventSender.handleException(robotNotFoundException, fightingItemUsageCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemFightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingItemUsageCommand.transactionUUID, EventType.ITEM_FIGHTING, domainEvent)
        eventTestUtils.checkItemFightingPayload(false, "Attacker Robot not found", null, listOf(), domainEvent.payload)
    }

    @Test
    fun `when UnknownPlanetException is thrown while using fighting item an event is send to 'item-fighting' topic`() {
        // given
        startItemFightingContainer()
        val planetId = UUID.randomUUID()

        val fightingItemUsageCommand = FightingItemUsageCommand(robotId, AttackItemType.LONG_RANGE_BOMBARDMENT, planetId, UUID.randomUUID())
        val unknownPlanetException = UnknownPlanetException(planetId)
        // when
        eventSender.handleException(unknownPlanetException, fightingItemUsageCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemFightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingItemUsageCommand.transactionUUID, EventType.ITEM_FIGHTING, domainEvent)
        eventTestUtils.checkItemFightingPayload(false, "Map Service doesn't have a planet with the id $planetId", 0, listOf(), domainEvent.payload)
    }

    @Test
    fun `when TargetRobotOutOfReachException is thrown while using fighting item an event is send 'item-fighting' topic`() {
        // given
        startItemFightingContainer()
        val planetId = UUID.randomUUID()

        val fightingItemUsageCommand = FightingItemUsageCommand(robotId, AttackItemType.ROCKET, planetId, UUID.randomUUID())
        val targetRobotOutOfReachException = TargetRobotOutOfReachException("Target out of reach")
        // when
        eventSender.handleException(targetRobotOutOfReachException, fightingItemUsageCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemFightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingItemUsageCommand.transactionUUID, EventType.ITEM_FIGHTING, domainEvent)
        eventTestUtils.checkItemFightingPayload(false, "Target out of reach", 0, listOf(), domainEvent.payload)
    }

    @Test
    fun `when NotEnoughItemsException is thrown while using fighting item an event is send to 'item-fighting' topic`() {
        // given
        startItemFightingContainer()
        val planetId = UUID.randomUUID()

        val fightingItemUsageCommand = FightingItemUsageCommand(robotId, AttackItemType.ROCKET, planetId, UUID.randomUUID())
        val notEnoughItemsException = NotEnoughItemsException("Not enough items", AttackItemType.ROCKET)
        // when
        eventSender.handleException(notEnoughItemsException, fightingItemUsageCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_FIGHTING, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemFightingEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(fightingItemUsageCommand.transactionUUID, EventType.ITEM_FIGHTING, domainEvent)
        eventTestUtils.checkItemFightingPayload(false, "Not enough items\n Missing item: ${fightingItemUsageCommand.itemType}", 0, listOf(), domainEvent.payload)
    }

    @Test
    fun `when RobotNotFoundException is thrown while using repair item an event is send to 'item-repair' topic`() {
        // given
        startItemRepairContainer()

        val regenCommand = RepairItemUsageCommand(UUID.randomUUID(), RepairItemType.REPAIR_SWARM, UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot not Found")
        // when
        eventSender.handleException(robotNotFoundException, regenCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_REPAIR, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemRepairEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(regenCommand.transactionUUID, EventType.ITEM_REPAIR, domainEvent)
        eventTestUtils.checkItemRepairPayload(
            false,
            "Robot not Found",
            listOf(),
            domainEvent.payload
        )
    }

    @Test
    fun `when NotEnoughItemsException is thrown while using repair item an event is send to 'item-repair' topic`() {
        startItemRepairContainer()

        val regenCommand = RepairItemUsageCommand(UUID.randomUUID(), RepairItemType.REPAIR_SWARM, UUID.randomUUID())
        val noteEnoughItemsException = NotEnoughItemsException("Not Enough Items", RepairItemType.REPAIR_SWARM)
        // when
        eventSender.handleException(noteEnoughItemsException, regenCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_REPAIR, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemRepairEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(regenCommand.transactionUUID, EventType.ITEM_REPAIR, domainEvent)
        eventTestUtils.checkItemRepairPayload(
            false,
            "Not Enough Items\n Missing item: ${RepairItemType.REPAIR_SWARM}",
            listOf(),
            domainEvent.payload
        )
    }

    @Test
    fun `when NotEnoughItemsException is thrown while using move item an event is send to 'item-movement' topic`() {
        startItemMovementContainer()

        val regenCommand = MovementItemsUsageCommand(UUID.randomUUID(), MovementItemType.WORMHOLE, UUID.randomUUID())
        val notEnoughItemsException = NotEnoughItemsException("Not Enough Items", MovementItemType.WORMHOLE)
        // when
        eventSender.handleException(notEnoughItemsException, regenCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemMovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(regenCommand.transactionUUID, EventType.ITEM_MOVEMENT, domainEvent)
        eventTestUtils.checkItemMovementPayload(
            false,
            "Not Enough Items\n Missing item: ${MovementItemType.WORMHOLE}",
            null,
            domainEvent.payload
        )
    }

    @Test
    fun `when PlanetBlockedException is thrown while using move item an event is send to 'item-movement' topic`() {
        startItemMovementContainer()

        val regenCommand = MovementItemsUsageCommand(UUID.randomUUID(), MovementItemType.WORMHOLE, UUID.randomUUID())
        val planetBlockedException = PlanetBlockedException("Planet blocked")
        // when
        eventSender.handleException(planetBlockedException, regenCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemMovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(regenCommand.transactionUUID, EventType.ITEM_MOVEMENT, domainEvent)
        eventTestUtils.checkItemMovementPayload(
            false,
            "Planet blocked",
            null,
            domainEvent.payload
        )
    }

    @Test
    fun `When RobotNotFoundException is thrown while using move item an event is send to 'item-movement' topic`() {
        startItemMovementContainer()

        val regenCommand = MovementItemsUsageCommand(UUID.randomUUID(), MovementItemType.WORMHOLE, UUID.randomUUID())
        val robotNotFoundException = RobotNotFoundException("Robot not found")
        // when
        eventSender.handleException(robotNotFoundException, regenCommand)
        // then
        val singleRecord = consumerRecords.poll(100, TimeUnit.MILLISECONDS)
        assertNotNull(singleRecord!!)
        assertEquals(topicConfig.ROBOT_ITEM_MOVEMENT, singleRecord.topic())

        val domainEvent = DomainEvent.build(
            jacksonObjectMapper().readValue(singleRecord.value(), ItemMovementEventDTO::class.java),
            singleRecord.headers()
        )

        eventTestUtils.checkHeaders(regenCommand.transactionUUID, EventType.ITEM_MOVEMENT, domainEvent)
        eventTestUtils.checkItemMovementPayload(
            false,
            "Robot not found",
            null,
            domainEvent.payload
        )
    }
}
