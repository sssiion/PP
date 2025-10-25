# PP Project

---

## API Usage Guide (v2025.10.24)

이 문서는 PP 프로젝트 백엔드 API의 최신 사용 방법을 안내합니다.

### 1. 추천 API (`/api/recommend`)

장소 추천과 관련된 모든 기능을 제공합니다.

#### 1.1. 혼잡도를 포함한 장소 추천

- **Endpoint**: `/api/recommend/with-congestion`
- **Methods**: `GET`, `POST`
- **설명**: 특정 위치와 시간을 기준으로 주변 장소를 추천하며, 각 장소의 예상 혼잡도 정보를 포함하여 반환합니다. 추천 결과는 `sessionId`를 통해 세션에 저장하여 채팅 등 다른 기능에서 활용할 수 있습니다.

- **파라미터**:
  - `lat` (Double, 필수): 기준 위치의 위도
  - `lon` (Double, 필수): 기준 위치의 경도
  - `time` (String, 필수): 추천 기준 시간 (형식: `HH:mm:ss`)
  - `congestionDateTime` (String, 필수): 혼잡도를 조회할 기준 날짜와 시간 (ISO 8601 형식: `YYYY-MM-DDTHH:mm:ss`)
  - `types` (List<Integer>, 선택): 조회할 장소 타입 코드 리스트. (타입 코드는 `부록 A` 참고)
  - `radius` (Integer, 선택, 기본값: `5000`): 검색 반경 (미터 단위)
  - `pageSize` (Integer, 선택, 기본값: `2000`): 최대 결과 수
  - `sortBy` (String, 선택, 기본값: `distance`): 결과 정렬 방식.
    - `distance`: 가까운 순으로 정렬
    - `congestion`: 혼잡도가 낮은 순('여유'→'붐빔')으로 정렬. 혼잡도 같을 시 거리순.
  - `sessionId` (String, 선택): 추천 결과를 세션에 저장할 때 사용할 고유 ID.

- **`GET` 요청 예시**:
  ```bash
  curl "http://localhost:8082/api/recommend/with-congestion?lat=37.5665&lon=126.9780&time=14:30:00&congestionDateTime=2025-10-24T18:00:00&types=39&sortBy=congestion&sessionId=user1-query1"
  ```

- **`POST` 요청 예시**:
  ```bash
  curl -X POST "http://localhost:8082/api/recommend/with-congestion?sessionId=user1-query1" \
       -H "Content-Type: application/json" \
       -d 
             {
               "lat": 37.5665,
               "lon": 126.9780,
               "time": "14:30:00",
               "congestionDateTime": "2025-10-24T18:00:00",
               "types": [39],
               "sortBy": "congestion"
             }
  ```

- **성공 응답 예시**:
  ```json
  [
    {
      "name": "명동교자 본점",
      "latitude": "37.5634",
      "longitude": "126.9848",
      "congestionLevel": "여유",
      "distance": 0.88
    }
  ]
  ```

#### 1.2. 세션에 저장된 추천 결과 조회

- **Endpoint**: `/api/recommend/result/{sessionId}`
- **Method**: `GET`
- **설명**: `with-congestion` API 호출 시 `sessionId`로 저장했던 추천 결과를 다시 조회합니다.

- **URL 파라미터**:
  - `sessionId` (String, 필수): 조회할 추천 결과의 고유 ID.

- **요청 예시**:
  ```bash
  # user1-query1 ID로 저장된 추천 결과를 불러옵니다.
  # 세션 유지를 위해 쿠키 사용이 필요할 수 있습니다.
  curl --cookie "cookies.txt" http://localhost:8082/api/recommend/result/user1-query1
  ```
- **응답**: 성공 시 `1.1` API의 응답과 동일한 JSON을 반환하며, ID가 없거나 세션 만료 시 `404 Not Found`를 반환합니다.

#### 1.3. 장소 상세 정보 조회

- **Endpoint**: `/api/recommend/detail/{category}/{id}`
- **Methods**: `GET`, `POST`
- **설명**: 특정 장소 1개의 모든 상세 정보를 조회합니다.

- **URL 파라미터**:
  - `category` (String, 필수): 장소의 카테고리명 (예: `food`, `tourist_attraction`). `부록 A` 참고.
  - `id` (String, 필수): 장소의 고유 ID (`contentid`).

- **요청 예시**:
  ```bash
  curl http://localhost:8082/api/recommend/detail/food/2667283
  ```

#### 1.4. 여러 장소의 특정 정보 조회

- **Endpoint**: `/api/recommend/detail/{category}/column`
- **Methods**: `GET`, `POST`
- **설명**: 여러 장소에 대해 지정된 하나 이상의 정보(컬럼)만 선택적으로 조회합니다.

- **URL 파라미터**:
  - `category` (String, 필수): 장소 카테고리명.

- **요청 파라미터**:
  - `ids` (List<String>, 필수): 조회할 장소들의 ID 목록.
  - `columns` (List<String>, 필수): 조회할 정보의 컬럼명 리스트 (예: `title`, `addr1`, `firstimage`).

- **`GET` 요청 예시**:
  ```bash
  curl "http://localhost:8082/api/recommend/detail/food/column?ids=2667283,2849939&columns=title,firstimage"
  ```

### 2. 채팅 API (`/chat`)

AI 기반의 대화형 인터페이스를 제공합니다.

- **Endpoint**: `/chat`
- **Method**: `POST`
- **설명**: 사용자의 메시지를 받아 대화의 맥락을 유지하며 응답을 생성합니다. `sessionId`를 함께 전달하면, 이전에 추천받은 장소 정보를 AI가 인지하고 대화에 활용할 수 있습니다.

- **요청 본문**:
  - `userId` (String, 필수): 사용자를 식별하는 고유 ID. 채팅 내역(`ChatContext`)을 관리하는 데 사용됩니다.
  - `message` (String, 필수): 사용자가 입력한 메시지.
  - `sessionId` (String, 선택): 참고할 추천 결과가 저장된 세션 ID. (세션 관리 정책은 아래 `사용자 식별 및 세션 관리` 참고)

#### 사용자 식별 및 세션 관리

채팅 API는 로그인 상태와 관계없이 사용할 수 있으며, 사용자를 구분하는 방식에 차이가 있습니다.

**1. 로그인 사용자 (Logged-in User)**

- **개요**: OAuth2를 통해 로그인한 사용자는 서버가 세션 쿠키를 통해 자동으로 식별합니다.
- **`userId` 설정**: 채팅 내역 관리를 위해 `userId`를 보내야 합니다. 이 값은 로그인 시 발급받는 사용자의 고유 ID, 즉 **`providerId`**를 사용하기를 **강력히 권장**합니다.
- **`sessionId` 설정**: 로그인한 사용자의 경우, 서버는 내부적으로 `providerId`를 세션 ID로 사용합니다. 따라서 요청 시 `sessionId` 필드를 보내더라도 **서버는 이 값을 무시**합니다. 추천 결과는 자동으로 해당 유저의 세션에 귀속됩니다.

**2. 게스트 사용자 (Guest User)**

- **개요**: 로그인하지 않은 사용자는 클라이언트(프론트엔드)에서 고유 ID를 생성하고 관리해야 합니다.
- **`userId` 및 `sessionId` 설정**:
  - 클라이언트는 사용자를 식별할 수 있는 고유한 ID(예: UUID)를 생성해야 합니다.
  - `/chat` API 호출 시, 생성한 ID를 **`userId`와 `sessionId` 두 필드에 모두 동일한 값**으로 설정하여 보내야 합니다.
  - **예시**: `"userId": "guest-d5a7-469a-b1c3-1e2f3a4b5c6d"`, `"sessionId": "guest-d5a7-469a-b1c3-1e2f3a4b5c6d"`
  - 이렇게 ID를 통일해야 게스트 사용자의 채팅 내역과 추천 결과가 올바르게 연결됩니다.

- **요청 예시 (게스트)**:
  ```bash
  # 게스트 사용자를 위해 클라이언트에서 생성한 UUID를 userId와 sessionId에 동일하게 사용
  curl -X POST http://localhost:8082/chat \
       -H "Content-Type: application/json" \
       -b "cookies.txt" \
       -d 
             {
               "userId": "guest-uuid-12345",
               "message": "강남역 근처 맛집 추천해줘",
               "sessionId": "guest-uuid-12345"
             }
  ```

- **요청 예시 (로그인 사용자)**:
  ```bash
  # 로그인 사용자는 발급받은 providerId를 userId로 사용
  # sessionId는 보내지 않거나, 보내더라도 서버에서 무시됨
  curl -X POST http://localhost:8082/chat \
       -H "Content-Type: application/json" \
       -b "cookies.txt" \
       -d 
             {
               "userId": "1234567890" # 사용자의 providerId
               "message": "아까 추천해준 곳들 다시 알려줄래?"
             }
  ```
  * `-b "cookies.txt"`: 세션 유지를 위해 쿠키 파일을 사용합니다.

- **응답 예시**:
  ```json
  {
    "replyText": "네, 이전에 추천해 드렸던 장소는 명동교자 본점입니다. 칼국수가 유명한 곳이죠. 더 궁금한 점이 있으신가요?",
    "pros": null,
    "cons": null
  }
  ```

### 3. 혼잡도 일괄 조회 API (`/api/congestion`)

- **Endpoint**: `/api/congestion`
- **Method**: `POST`
- **설명**: 여러 장소와 시간에 대한 혼잡도 예측 결과를 일괄적으로 조회합니다.

- **요청 본문**: `CongestionRequestDto` 객체의 리스트.
  ```json
  [
    {
      "latitude": 37.5665,
      "longitude": 126.9780,
      "datetime": "2025-10-24T18:00:00"
    },
    {
      "latitude": 37.5803,
      "longitude": 126.9762,
      "datetime": "2025-10-24T19:30:00"
    }
  ]
  ```

- **요청 예시**:
  ```bash
  curl -X POST http://localhost:8082/api/congestion \
       -H "Content-Type: application/json" \
       -d 
             [
               {"latitude": 37.5665, "longitude": 126.9780, "datetime": "2025-10-24T18:00:00"}
             ]
  ```

- **응답 예시**:
  ```json
  [
    {
      "latitude": 37.5665,
      "longitude": 126.9780,
      "datetime": "2025-10-24T18:00:00",
      "congestionLevel": "붐빔"
    }
  ]
  ```

---

### 부록 A: 장소 타입 코드

| 타입 코드 (`type`) | 카테고리명 (`category`)         | 설명           | 
| ---------------- | ------------------------------- | -------------- | 
| 12               | `tourist_attraction`            | 관광지         | 
| 14               | `cultural_facilities`           | 문화시설       | 
| 15               | `festivals_performances_events` | 축제/공연/행사 | 
| 25               | `travel_course`                 | 여행코스       | 
| 28               | `leisure_sports`                | 레포츠         | 
| 32               | `accommodation`                 | 숙박           | 
| 38               | `shopping`                      | 쇼핑           | 
| 39               | `food`                          | 음식점         | 

---

### 부록 B: 서버 주요 설정

#### 세션 관리 (Session Management)

- **세션 만료 시간**: 사용자의 세션은 마지막 활동 후 **30분**이 지나면 자동으로 만료되도록 설정되어 있습니다.
- **설정 위치**: `src/main/resources/application.properties`
- **설정 키**: `server.reactive.session.timeout=30m`
- **동작**: 세션이 만료되면 로그인 정보 및 세션에 저장된 모든 임시 데이터(예: 캐시된 추천 결과)가 영구적으로 삭제됩니다.

---

## 변경 이력 (Change History)

*이전 변경 이력은 생략되었습니다.*

---

## 채팅 기능 개선: 추천 결과 연동 (2025-10-24)

### 1. 기능 개요

채팅 기능이 사용자가 이전에 추천받은 장소 목록을 기억하고 대화의 맥락(Context)으로 활용할 수 있도록 개선되었습니다. 이제 사용자가 "아까 추천해준 곳들 중에 첫 번째 장소 정보 알려줘"와 같이 질문하면, AI가 세션에 저장된 추천 결과를 바탕으로 답변을 생성할 수 있습니다.

### 2. 기술적 변경 사항

- **`UserMessageRequest` DTO 수정**: 채팅 메시지 요청 시, 이전에 추천 결과를 저장할 때 사용했던 `sessionId`를 포함할 수 있도록 필드가 추가되었습니다.
- **`ChatOrchestrationService` 로직 변경**: 채팅 서비스는 요청에 `sessionId`가 포함된 경우, `WebSession`에서 해당 ID로 저장된 추천 결과 데이터를 조회합니다. 조회된 데이터가 있으면, 이 정보를 대화 요약본에 추가하여 AI 프롬프트에 주입합니다. 이를 통해 AI는 이전 추천 내용을 인지하게 됩니다.

---
*이하 이전 변경 이력 생략*
