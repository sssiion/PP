package com.example.pp.rec.repository;


import com.example.pp.rec.entity.BusTimetable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusTimetableRepository extends JpaRepository<BusTimetable, Long> {
}