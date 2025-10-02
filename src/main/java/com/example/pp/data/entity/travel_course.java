package com.example.pp.data.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name="travel_course")
public class travel_course {

    @Id
    @Column(name = "id",updatable = false)
    private String Id; // 노선 ID (예: GS05_DOWN, 기본 키)

    @Column(name = "name",updatable = false)
    private String Name;

    @Column(name = "category",updatable = false)
    private String Category;

    @Column(name = "postal_code",updatable = false)
    private String postal_code;

    @Column(name = "manager",updatable = false)
    private String manager;

    @Column(name = "phone_number",updatable = false)
    private String phone_number;

    @Column(name = "address",updatable = false)
    private String address;

    @Column(name = "latitude",updatable = false)
    private String latitude;

    @Column(name = "longitude",updatable = false)
    private String longitude;

    @Column(name = "other_info",updatable = false)
    private String other_info;

    @Column(name = "inquiry_and_info",updatable = false)
    private String inquiry_and_info;

    @Column(name = "total_distance",updatable = false)
    private String total_distance;

    @Column(name = "duration",updatable = false)
    private String duration;

    @Column(name = "detailed_info",updatable = false)
    private String detailed_info;
}
