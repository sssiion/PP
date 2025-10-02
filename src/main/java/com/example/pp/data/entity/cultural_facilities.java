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
@Table(name="cultural_facilities")
public class cultural_facilities {
    @Id
    @Column(name = "id",updatable = false,columnDefinition="TEXT")
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

    @Column(name = "size",updatable = false,columnDefinition="TEXT")
    private String size;

    @Column(name = "capacity",updatable = false,columnDefinition="TEXT")
    private String capacity;

    @Column(name = "discount_info",updatable = false,columnDefinition="TEXT")
    private String discount_info;

    @Column(name = "stroller_rental",updatable = false,columnDefinition="TEXT")
    private String stroller_rental;

    @Column(name = "parking_availability",updatable = false,columnDefinition="TEXT")
    private String parking_availability;

    @Column(name = "tour_duration",updatable = false,columnDefinition="TEXT")
    private String tour_duration;

    @Column(name = "closing_day",updatable = false,columnDefinition="TEXT")
    private String closing_day;

    @Column(name = "pets_allowed",updatable = false,columnDefinition="TEXT")
    private String pets_allowed;

    @Column(name = "usage_fee",updatable = false,columnDefinition="TEXT")
    private String usage_fee;

    @Column(name = "operating_hours",updatable = false,columnDefinition="TEXT")
    private String operating_hours;

    @Column(name = "credit_card_accepted",updatable = false,columnDefinition="TEXT")
    private String credit_card_accepted;

    @Column(name = "detailed_info",updatable = false,columnDefinition="TEXT")
    private String detailed_info;

    @Column(name = "parking_fee",updatable = false,columnDefinition="TEXT")
    private String parking_fee;


}
