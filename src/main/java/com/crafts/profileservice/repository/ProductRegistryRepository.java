package com.crafts.profileservice.repository;

import com.crafts.profileservice.entity.ProductRegistryEO;

import java.util.List;

public interface ProductRegistryRepository {
    List<ProductRegistryEO> findAll();

}

