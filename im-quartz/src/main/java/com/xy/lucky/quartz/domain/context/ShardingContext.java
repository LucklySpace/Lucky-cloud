package com.xy.lucky.quartz.domain.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShardingContext {

    private int shardingItem;

    private int shardingTotalCount;
}
