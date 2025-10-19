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
@Table(name="accommodation")
public class accommodation {

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

    @Column(name = "size",updatable = false,columnDefinition="TEXT")
    private String size;

    @Column(name = "capacity",updatable = false,columnDefinition="TEXT")
    private String capacity;

    @Column(name = "room_count",updatable = false,columnDefinition="TEXT")
    private String room_count;

    @Column(name = "room_type",updatable = false,columnDefinition="TEXT")
    private String room_type;

    @Column(name = "parking_availability",updatable = false,columnDefinition="TEXT")
    private String parking_availability;

    @Column(name = "cooking_availability",updatable = false,columnDefinition="TEXT")
    private String cooking_availability;

    @Column(name = "check_in_time",updatable = false,columnDefinition="TEXT")
    private String check_in_time;

    @Column(name = "check_out_time",updatable = false,columnDefinition="TEXT")
    private String check_out_time;

    @Column(name = "reservation_info",updatable = false,columnDefinition="TEXT")
    private String reservation_info;

    @Column(name = "reservation_page",updatable = false,columnDefinition="TEXT")
    private String reservation_page;

    @Column(name = "pickup_service",updatable = false,columnDefinition="TEXT")
    private String pickup_service;

    @Column(name = "fnb_area",updatable = false,columnDefinition="TEXT")
    private String fnb_area;

    @Column(name = "amenities",updatable = false,columnDefinition="TEXT")
    private String amenities;

    @Column(name = "seminar_room",updatable = false,columnDefinition="TEXT")
    private String seminar_room;

    @Column(name = "sports_facility",updatable = false,columnDefinition="TEXT")
    private String sports_facility;

    @Column(name = "sauna_room",updatable = false,columnDefinition="TEXT")
    private String sauna_room;

    @Column(name = "beauty_facility",updatable = false,columnDefinition="TEXT")
    private String beauty_facility;

    @Column(name = "karaoke_room",updatable = false,columnDefinition="TEXT")
    private String karaoke_room;

    @Column(name = "barbecue_area",updatable = false,columnDefinition="TEXT")
    private String barbecue_area;

    @Column(name = "campfire_area",updatable = false,columnDefinition="TEXT")
    private String campfire_area;

    @Column(name = "bicycle_rack",updatable = false,columnDefinition="TEXT")
    private String bicycle_rack;

    @Column(name = "fitness_center",updatable = false,columnDefinition="TEXT")
    private String fitness_center;

    @Column(name = "public_pc_room",updatable = false,columnDefinition="TEXT")
    private String public_pc_room;

    @Column(name = "public_shower_room",updatable = false,columnDefinition="TEXT")
    private String public_shower_room;

    @Column(name = "detailed_info",updatable = false,columnDefinition="TEXT")
    private String detailed_info;

    @Column(name = "refund_policy",updatable = false,columnDefinition="TEXT")
    private String refund_policy;








}
