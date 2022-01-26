package com.msd.application.dto

import com.msd.application.RoundStatus
import java.util.*

class RoundStatusDTO(val roundNumber: Int, val roundStatus: RoundStatus, val roundId: UUID, val gameId: UUID)
