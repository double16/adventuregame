package org.patdouble.adventuregame.storage.jpa

import org.patdouble.adventuregame.state.Story
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

/**
 * Store World using JPA.
 */
@RepositoryRestResource
interface StoryRepository extends JpaRepository<Story, UUID> { }
