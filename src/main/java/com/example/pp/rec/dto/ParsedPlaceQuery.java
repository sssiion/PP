package com.example.pp.rec.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParsedPlaceQuery {

    private String location;
    private double radius;
    private String category;
    private String atmosphere;
}
