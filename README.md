# PP Project

---

## 아키텍처 수정 (2025-10-20)

### 1. 문제 상황

프로젝트 초기 설정 시, 두 가지 다른 웹 기술인 Spring MVC (`spring-boot-starter-web`)와 Spring WebFlux (`spring-boot-starter-webflux`) 의존성이 모두 포함되어 있었습니다. 이로 인해 다음과 같은 심각한 문제들이 발생했습니다.

- **Bean 충돌로 인한 애플리케이션 실행 실패**
  - 두 기술이 동일한 이름의 Bean을 등록하려 시도하여 `BeanDefinitionOverrideException` 에러가 발생하며 애플리케이션이 시작되지 않았습니다.
- **불안정한 런타임 동작**
  - OAuth2 소셜 로그인 성공 후 `status=999` 또는 `status=404` 와 같은 비정상적인 오류 페이지가 표시되었습니다.

이 모든 문제의 근본 원인은 `spring-boot-starter-websocket` 의존성이 내부적으로 MVC 기반인 `spring-boot-starter-web`을 자동으로 포함했기 때문입니다.

### 2. 수정 내용

프로젝트의 아키텍처를 안정적이고 일관된 **순수 리액티브(Pure Reactive) 스택**으로 통일하기 위해 다음과 같은 수정을 진행했습니다.

1.  **`build.gradle` 수정**
    - `spring-boot-starter-websocket` 의존성에서 `spring-boot-starter-web`이 자동으로 포함되지 않도록 `exclude` 구문을 추가했습니다. 이를 통해 프로젝트에서 Spring MVC 관련 의존성을 완전히 제거했습니다.

2.  **리액티브 보안 설정으로 전면 전환**
    - **`SecurityConfig.java`**: 기존의 서블릿(MVC) 기반의 `HttpSecurity` 설정을 리액티브 환경에 맞는 `ServerHttpSecurity` 설정으로 전면 교체했습니다.
    - **`CustomOAuth2UserService.java`**: 서블릿 API(`HttpSession` 등)에 의존하던 코드를 리액티브 인터페이스인 `ReactiveOAuth2UserService`를 구현하도록 수정했습니다.

3.  **불필요한 설정 파일 삭제**
    - **`WebConfig.java`**: MVC 환경에서만 사용되던 CORS 설정 파일로, 리액티브 스택으로 전환하면서 불필요해졌기 때문에 삭제했습니다. 새로운 CORS 설정은 `SecurityConfig` 내에 구현되어 있습니다.

### 3. 결과

- **아키텍처 통일**: 이제 이 프로젝트는 **Spring WebFlux**와 **Netty** 서버를 기반으로 동작하는 완전한 리액티브 애플리케이션이 되었습니다.
- **안정성 확보**: 의존성 충돌의 원인이 사라져 애플리케이션이 안정적으로 실행되며, 소셜 로그인 후 발생하던 모든 오류가 해결되었습니다.

---

## 혼잡도 예측 API 추가 (2025-10-22)

### 1. 기능 개요

외부 Python 서버(`PP_algorithm`)에서 계산된 실시간 장소 혼잡도 예측 결과를 받아오는 API를 추가했습니다.

이 기능은 Spring 서버가 클라이언트로부터 위경도 및 시간 값을 받아 Python 서버에 전달하고, 그 응답을 다시 클라이언트에게 반환하는 중계 역할을 수행합니다. 이를 통해 Java 기반의 메인 애플리케이션과 Python 기반의 AI 예측 모델이 효과적으로 통합됩니다.

### 2. 추가 및 수정된 파일

- **`CongestionController.java`**:
  - 클라이언트 요청을 받는 `/api/congestion` 엔드포인트를 정의한 새로운 컨트롤러입니다.
- **`CongestionService.java`**:
  - `WebClient`를 사용하여 실제로 Python 서버에 API 요청을 보내고 응답을 받아오는 비즈니스 로직을 담당하는 새로운 서비스입니다.
- **`CongestionRequestDto.java` / `CongestionResponseDto.java`**:
  - Python 서버와의 데이터 통신을 위해 특별히 제작된 데이터 전송 객체(DTO)입니다.
- **`WebClientConfig.java`**:
  - Python 서버(`http://127.0.0.1:5001`)와 통신하기 위한 새로운 `congestionWebClient` Bean 설정을 추가했습니다.
- **`application.properties`**:
  - `congestion.api.base-url` 프로퍼티를 추가하여 Python 서버의 주소를 설정했습니다.

### 3. API 사용 방법

- **Endpoint**: `GET /api/congestion`
- **Method**: `GET`
- **설명**: 특정 위치와 시간에 대한 혼잡도 예측 레벨을 조회합니다.
- **쿼리 파라미터**:
  - `lat` (String): 조회할 위치의 위도
  - `lon` (String): 조회할 위치의 경도
  - `datetime` (String): 조회할 날짜 및 시간 (ISO 8601 형식)

- **요청 예시**:
  ```
  GET http://localhost:8082/api/congestion?lat=37.5665&lon=126.9780&datetime=2025-10-22T14:30:00
  ```

- **성공 응답 예시**:
  ```json
  {
      "latitude": 37.5665,
      "longitude": 126.9780,
      "datetime": "2025-10-22T14:30:00",
      "congestion_level": "붐빔"
  }
  ```

---

## 기능 개선 및 리팩토링 (2025-10-23)

### 1. 장소 추천 + 혼잡도 결합 API 개발

- **기능**: 기존의 장소 추천 기능과 혼잡도 조회 기능을 하나로 통합한 새로운 API 엔드포인트인 `/api/recommend/with-congestion`을 개발했습니다.
- **동작 흐름**:
  1. 클라이언트로부터 추천 기준(위치, 시간 등)과 **혼잡도 조회 기준 시간(`congestionDateTime`)**을 함께 받습니다.
  2. 추천 기준을 사용하여 장소 목록을 추천받습니다.
  3. 추천된 장소들의 위치 정보와 `congestionDateTime` 값을 사용하여 Python 서버로부터 각 장소의 혼잡도를 조회합니다.
  4. 최종적으로, 추천된 장소 정보에 `congestionLevel` 필드를 추가하여 클라이언트에게 반환합니다.
- **추가된 파일**:
  - `CombinedRecommendationService.java`: 장소 추천과 혼잡도 조회를 조율하고 결과를 병합하는 핵심 서비스입니다.
- **API 사용법**:
  - **Endpoint**: `GET /api/recommend/with-congestion`
  - **쿼리 파라미터**:
    - `lat`, `lon`, `time`, `types` 등 (기존 추천 API와 동일)
    - `congestionDateTime` (String): 혼잡도를 조회할 기준 시간 (ISO 8601 형식, 예: `2025-10-23T15:00:00`)
  - **요청 예시**:
    ```bash
    curl "http://localhost:8082/api/recommend/with-congestion?lat=37.5665&lon=126.9780&time=14:30:00&congestionDateTime=2025-10-23T15:00:00&types=14"
    ```
  - **응답 예시**:
    ```json
    {
      "name": "명보아트홀",
      "latitude": "37.5647463847",
      "longitude": "126.9932030652",
      "congestionLevel": "보통" 
    }
    ```

### 2. 코드 최적화 및 버그 수정

- **불필요한 코드 제거**: 디버깅 목적으로 사용되었으나 실제 로직에는 영향을 주지 않는 `list1_runtime_buffer` 관련 코드를 모두 삭제하여 코드의 가독성과 효율성을 높였습니다.
- **로직 수정**: 장소 추천 시, 초기 지하철역 탐색 반경이 100m로 너무 좁아 추천이 실패하는 문제를 발견하여, 검색 반경을 1000m(1km)로 확장하여 안정성을 개선했습니다.
- **버그 수정**: 혼잡도 결합 API 개발 과정에서 발생했던 다수의 버그(데이터 타입 불일치, 누락된 필드, 잘못된 로직 등)를 수정하여 기능이 의도대로 정확히 동작하도록 바로잡았습니다.