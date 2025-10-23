package com.example.pp.rec.repository;

import com.example.pp.rec.entity.BusTimetable;
import com.example.pp.rec.entity.ChatContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContextRepository extends JpaRepository<ChatContext, String> {
    Optional<ChatContext> findByUserId(String userId);
    ChatContext save(ChatContext context);
}
