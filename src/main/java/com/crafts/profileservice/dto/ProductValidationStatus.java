package com.crafts.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductValidationStatus implements Serializable {
    private String status;
    private List<String> errors = new ArrayList<>(); // Initialized to avoid null
}