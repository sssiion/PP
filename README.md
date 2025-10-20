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
