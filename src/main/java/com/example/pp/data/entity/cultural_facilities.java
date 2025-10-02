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

    @Column(name = "size",updatable = false)
    private String size;

    @Column(name = "capacity",updatable = false)
    private String capacity;

    @Column(name = "discount_info",updatable = false)
    private String discount_info;

    @Column(name = "stroller_rental",updatable = false)
    private String stroller_rental;

    @Column(name = "parking_availability",updatable = false)
    private String parking_availability;

    @Column(name = "tour_duration",updatable = false)
    private String tour_duration;

    @Column(name = "closing_day",updatable = false)
    private String closing_day;

    @Column(name = "pets_allowed",updatable = false)
    private String pets_allowed;

    @Column(name = "usage_fee",updatable = false)
    private String usage_fee;

    @Column(name = "operating_hours",updatable = false)
    private String operating_hours;

    @Column(name = "credit_card_accepted",updatable = false)
    private String credit_card_accepted;

    @Column(name = "detailed_info",updatable = false)
    private String detailed_info;

    @Column(name = "parking_fee",updatable = false)
    private String parking_fee;


}
