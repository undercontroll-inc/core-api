package com.undercontroll.domain.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Demand {

    private Integer id;

    private Long quantity;

    private ComponentPart component;

    private Order order;

}
