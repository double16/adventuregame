package org.patdouble.adventuregame.storage.jpa

import org.patdouble.adventuregame.model.World
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.repository.query.Param
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource
interface WorldRepository extends PagingAndSortingRepository<World, Long> {
    List<World> findByName(@Param('name') String name)
}
