package com.msd.event.application

import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
class ErrorEvent(
    val topic: String,
    @Column(length = 2048)
    val eventString: String,
    val eventType: EventType
) {
    @Id
    @Type(type = "uuid-char")
    val errorId: UUID = UUID.randomUUID()
}
