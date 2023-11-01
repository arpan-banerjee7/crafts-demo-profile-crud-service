package com.crafts.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO implements Serializable {

    @NotNull(message = "Address line cannot be empty")
    private String line1;

    private String line2;

    @NotNull
    private String city;

    @NotNull
    private String state;

    @NotNull
    private String zip;

    @NotNull
    private String country;

}
