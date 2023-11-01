package com.crafts.profileservice.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionRequestDTO implements Serializable {
    String productId;
}
