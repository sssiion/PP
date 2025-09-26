package com.example.pp.repository;


import com.example.pp.entity.BusTimetable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusTimetableRepository extends JpaRepository<BusTimetable, Long> {
}