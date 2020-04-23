package org.patdouble.adventuregame.storage.jpa

import org.patdouble.adventuregame.model.World
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource

/**
 * Store World using JPA.
 */
@RepositoryRestResource
interface WorldRepository extends JpaRepository<World, UUID> {
    List<World> findByName(@Param('name') String name)
}
