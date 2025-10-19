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
    @Column(name = "id",updatable = false, length = 255)
    private String Id; // 노선 ID (예: GS05_DOWN, 기본 키)

    @Column(name = "name",updatable = false,columnDefinition="TEXT")
    private String Name;

    @Column(name = "category",updatable = false,columnDefinition="TEXT")
    private String Category;

    @Column(name = "postal_code",updatable = false,columnDefinition="TEXT")
    private String postal_code;

    @Column(name = "manager",updatable = false,columnDefinition="TEXT")
    private String manager;

    @Column(name = "phone_number",updatable = false,columnDefinition="TEXT")
    private String phone_number;

    @Column(name = "address",updatable = false,columnDefinition="TEXT")
    private String address;

    @Column(name = "latitude",updatable = false,columnDefinition="TEXT")
    private String latitude;

    @Column(name = "longitude",updatable = false,columnDefinition="TEXT")
    private String longitude;

    @Column(name = "other_info",updatable = false,columnDefinition="TEXT")
    private String other_info;

    @Column(name = "inquiry_and_info",updatable = false,columnDefinition="TEXT")
    private String inquiry_and_info;

    @Column(name = "total_distance",updatable = false,columnDefinition="TEXT")
    private String total_distance;

    @Column(name = "duration",updatable = false,columnDefinition="TEXT")
    private String duration;

    @Column(name = "detailed_info",updatable = false,columnDefinition="TEXT")
    private String detailed_info;
}
