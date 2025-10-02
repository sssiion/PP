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
@Table(name="Festivals_Performances_Events")
public class Festivals_Performances_Events {

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

    @Column(name = "organizer",updatable = false)
    private String organizer;

    @Column(name = "supervisor_info",updatable = false)
    private String supervisor_info;

    @Column(name = "supervisor_contact",updatable = false)
    private String supervisor_contact;

    @Column(name = "event_start_date ",updatable = false)
    private String event_start_date;

    @Column(name = "event_end_date ",updatable = false)
    private String event_end_date;

    @Column(name = "performance_time ",updatable = false)
    private String performance_time;

    @Column(name = "event_venue",updatable = false)
    private String event_venue;

    @Column(name = "event_homepage",updatable = false)
    private String event_homepage;

    @Column(name = "usage_fee",updatable = false)
    private String usage_fee;

    @Column(name = "viewing_age_limit",updatable = false)
    private String viewing_age_limit;

    @Column(name = "tour_duration",updatable = false)
    private String tour_duration;

    @Column(name = "detailed_info",updatable = false)
    private String detailed_info;
}
