package com.example.pp.rec.entity;


import com.example.pp.rec.config.ListToStringConverter;
import com.example.pp.rec.config.MapToStringConverter;

import jakarta.persistence.*; // (JPA 임포트)
import jakarta.persistence.Column; // jakarta.persistence.* 임포트
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant; // updated_at을 위해 추가
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity // <--- (1) 이것이 DB 테이블임을 선언
@Table(name = "chat_context") // <--- (2) DB 테이블 이름 지정
@Getter @Setter
public class ChatContext {

    @Id // <--- (3) 이것이 기본 키(PK)임을 선언
    private String userId;

    @Enumerated(EnumType.STRING) // <--- Enum을 문자열로 저장
    private ChatState currentState;

    @Lob // <--- 대용량 텍스트 (TEXT 또는 CLOB 타입)
    @Column(length = 5000) // (필요시 길이 지정)
    private String summarizedContext;

    @Lob
    @Column(length = 10000)
    @Convert(converter = ListToStringConverter.class)
    private List<String> fullHistory;

    private int turnCount;

    // (Map은 @ElementCollection 등으로 복잡하게 매핑해야 하지만,
    //  간단하게 JSON 문자열로 저장하는 것이 더 쉬울 수 있습니다. 여기서는 일단 Lob)
    @Lob
    @Convert(converter = MapToStringConverter.class)
    private Map<String, String> collectedData;

    private Instant updatedAt; // <--- (4) 데이터 정리를 위한 타임스탬프

    // (JPA는 기본 생성자가 필수입니다!)
    public ChatContext() {
        this.fullHistory = new ArrayList<>();
        this.collectedData = new HashMap<>();
    }

    // (기존 생성자)
    public ChatContext(String userId) {
        this.userId = userId;
        this.currentState = ChatState.INITIAL;
        this.summarizedContext = "대화 시작";
        this.fullHistory = new ArrayList<>();
        this.turnCount = 0;
        this.collectedData = new HashMap<>();
        this.updatedAt = Instant.now();
    }


}