package com.msd.planet.application

import com.msd.planet.domain.Planet
import com.msd.planet.domain.PlanetType
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

@Mapper(componentModel = "spring")
abstract class PlanetMapper {

    @Mappings(
        Mapping(target = "planetId", source = "planet.planetId"),
        Mapping(target = "movementDifficulty", source = "movementCost"),
        Mapping(target = "planetType", source = "planetType"),
        Mapping(target = "resourceType", source = "planet.resourceType")
    )
    abstract fun planetToPlanetDTO(planet: Planet, movementCost: Int, planetType: PlanetType): PlanetDTO
}
