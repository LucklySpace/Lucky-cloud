package com.xy.lucky.knowledge.repository;

import com.xy.lucky.knowledge.domain.po.GroupPo;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface GroupRepository extends ReactiveCrudRepository<GroupPo, Long> {
    Flux<GroupPo> findByOwner(String owner);

    Mono<GroupPo> findByOwnerAndName(String owner, String name);
}
