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
@Table(name="food")
public class food {

    @Id
    @Column(name = "id",updatable = false, length = 255)
    private String Id;

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

    @Column(name = "table_count",updatable = false,columnDefinition="TEXT")
    private String table_count;

    @Column(name = "parking_availability",updatable = false,columnDefinition="TEXT")
    private String parking_availability;


    @Column(name = "business_hours",updatable = false,columnDefinition="TEXT")
    private String business_hours;

    @Column(name = "closing_day",updatable = false,columnDefinition="TEXT")
    private String closing_day;

    @Column(name = "signature_menu",updatable = false,columnDefinition="TEXT")
    private String signature_menu;

    @Column(name = "reservation_info",updatable = false,columnDefinition="TEXT")
    private String reservation_info;

    @Column(name = "smoking_policy",updatable = false,columnDefinition="TEXT")
    private String smoking_policy;

    @Column(name = "credit_card_accepted",updatable = false,columnDefinition="TEXT")
    private String credit_card_accepted;

    @Column(name = "takeout_available",updatable = false,columnDefinition="TEXT")
    private String takeout_available;

    @Column(name = "menu_items",updatable = false,columnDefinition="TEXT")
    private String menu_items;

    @Column(name = "license_number",updatable = false,columnDefinition="TEXT")
    private String license_number;


}
