package org.patdouble.adventuregame.storage.jpa

import org.patdouble.adventuregame.state.Story
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.data.rest.core.annotation.RepositoryRestResource

@RepositoryRestResource
interface StoryRepository extends PagingAndSortingRepository<Story, Long> { }
