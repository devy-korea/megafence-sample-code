# Project Overview

WebGate Backend Library 개발 및 DEMO용 프로젝트

## Tech Stack
N/A

## Coding Rules

1. 모든 주석은 한국어 사용하고 영어는 의미전달에 필요한 경우 함께 사용
2. 새로 작성하는 함수명은 CamelCase 사용하고 기존 소스는 별도의 요청이 없는 한 제외
3. 새로 작성하는 변수명은 Java 버전과 동일하게 유지하고 기존 소스는 별도의 요청이 없는 한 제외
4. null 체크를 반드시 수행
5. try-catch는 필요한 경우에만 사용

## Architecture
N/A

## Logging

1. 로그는 NLog 형식과 유사하게 출력

## Test

1. 신규 기능 추가 시 Unit Test 작성
2. 기존 테스트 실패 금지

## Important
1.  JAVA 소스(WebGate.java)를 기준으로 소스 수정이 완료되면 다른 4개 언어(JSP,  PHP, ASP.NET, ASP) 소스에 동일한 기능을 하도록 로직 동기화 함.
2. 로직동기화 개념 : Backend Library 소스는 언어/개발환경에 따른 구현 내용의 차이가 발생할 수 있으나 동작 로직은 모두 동일해야 함.

---

# Reference

## 소스 관계 (마스터 → 컨버팅)

**`backend-java-intercepter/src/main/java/com/devy/megafence/WebGate.java`가 마스터 소스**이며, 나머지 언어 파일은 이를 각 환경에 맞게 컨버팅한 결과물이다.

```
WebGate.java (backend-java-intercepter)  ← 마스터 소스
    ├── webgate-lib.jsp   (backend-jsp/src/main/webapp/WEB-INF/views/)        ← Java → JSP 문법
    ├── webgate-lib.php   (backend-php/)                                      ← Java → PHP
    ├── webgate-lib.asp   (backend-asp/)                                      ← Java → VBScript
    └── C#  ┬ WebGateLib.cs (backend-aspnet/aspnet-webform/)                  ← Java → C# (.NET Framework)
            │   └ backend-aspnet/aspnet-mvc/ 는 위 파일을 **링크 참조**(단일 소스)
            └ WebGateLib.cs (backend-aspnet/aspnet-core/Lib/)                 ← Java → C# (ASP.NET Core 컨버팅)
```

현재 버전: **모든 구현체 V26.1.605.1 동기화 완료** (java, jsp, php, asp, C#-Framework, C#-Core).

> ⚠️ **C#은 변형이 2개다.** .NET Framework용(aspnet-webform, mvc가 공유)과 ASP.NET Core용(aspnet-core/Lib)을 **둘 다** 반영해야 한다. Core는 `System.Web` 미지원으로 `HttpContext` 주입/`System.Text.Json`/`HttpClient` 기반이라 구현이 다르되 로직은 동일하다.

## 로직 동기화 규칙

"로직 동기화" = **WebGate.java(마스터) 기준으로 나머지 4개 언어(JSP, PHP, ASP.NET, ASP)를 맞추는 작업.**

### 작업 순서
1. 마스터 소스 `WebGate.java`를 먼저 수정한다.
2. 변경 내용을 각 컨버팅 파일(jsp, php, asp, aspx)에 동일하게 반영한다.
3. 반영 후 마스터와 각 파일을 대조하여 누락/불일치가 없는지 확인한다.

### 동기화 원칙
- **동작 로직은 모두 동일해야 한다.** 언어/개발환경에 따른 구현 방식 차이는 허용하되, STEP 구성·분기 조건·반환 로직 등 논리 흐름은 일치시킨다.
- **함수명·변수명은 Java 버전과 동일하게 유지한다.** 언어 관용 표기로 임의 변경하지 않는다. (예: `WG_IsNeedToWaiting`, `WG_RETURN_FLAG`, `WG_GATE_OPERATION_MODE`, `WG_TOKEN_STATE`)
- 버전 번호(`$WG_VER_BACKEND`), API Action 종류/순서(OUT, MATCHING), STEP 구성(STEP-1/2/3), TokenState 분기(WAIT/CONNECT/IN/OUT), GateOperationMode 분기(GATE/ALERT), TRACE 포맷, TRACE_LEVEL별 쿠키 write/delete 처리가 모두 일치해야 한다.

### 언어별 컨버팅 주의사항
- **JSP**: Java 클래스 문법을 `<%! %>` 선언부 방식으로 변환. 버전 변수는 `static final` 클래스 멤버로 선언(메서드 지역변수 선언 불가).
- **PHP**: `WG_BuildApiUrl`(http_build_query), `WG_ParseJson`(json_decode), cURL 기반 `WG_CallApi` 사용.
- **C# (.NET Framework / aspnet-webform·mvc 공유)**: 클래스 프로퍼티가 아닌 메서드 로컬 변수 방식. `HttpContext.Current` 사용, `JavaScriptSerializer`로 JSON 파싱, `HttpWebRequest` 사용.
- **C# (ASP.NET Core / aspnet-core/Lib)**: `System.Web` 미지원 → 생성자로 `HttpContext` 주입, `System.Text.Json`(`JsonDocument`)으로 파싱, `HttpClient.Send`(동기) 사용, 쿠키는 `Request/Response.Cookies` + `CookieOptions`. 로직은 Framework판과 동일.
- **ASP(VBScript)**: JSON 파서가 없어 `WG_JsonGetStr/Int`(RegExp 기반) 헬퍼로 파싱. try/catch가 없어 `On Error Resume Next` + `Err` 체크로 모사. `WG_CallApi`는 실패 시 ErrorJson을 반환해 ResultCode 음수 처리 일관성 유지.

## 빌드 및 실행

```bash
# backend-java-intercepter (메인 Java 샘플)
cd backend-java-intercepter
mvn spring-boot:run          # 개발 실행 (포트 8080)
mvn clean package            # WAR 빌드

# backend-jsp
cd backend-jsp
./mvnw spring-boot:run
```

> 로컬 PC에서는 유량제어 API 호출이 정상 동작하지 않을 수 있음. SSL(https) 적용된 웹서버에서 테스트 권장.

## 핵심 동작 원리 (3-STEP 유량제어 판정)

```
STEP-1: URL Parameter (WG_TOKEN) 검증            → ACTION=OUT API 호출
STEP-2: Cookie (WG_TOKEN_NO + WG_CLIENT_ID + WG_WAS_IP) 검증 → ACTION=OUT API 호출
STEP-3: 신규 접속자                              → ACTION=MATCHING API 호출 (최대 3회 retry)
```

- `WG_IsNeedToWaiting()` 반환 `true` → 대기 필요 (응답을 대기UI로 교체)
- `WG_IsNeedToWaiting()` 반환 `false` → 통과 (정상 응답 진행)
- `WG_RETURN_FLAG`로 STEP 간 흐름 제어, 반환은 `result = WG_IS_NEED_TO_WAIT` 단순 반환
- Gate 운영모드: `GATE` (정상 대기열) / `ALERT` (서비스 미오픈 등 알림)
- Token 상태: `WAIT`(대기) / `CONNECT`·`IN`·`OUT`(통과)
- `WG_GetWaitingUi()`: config 객체 방식 (onSuccess/onAlert/onMdsDetected 등 콜백)

## backend-java-intercepter 내부 구조

```
WebConfig.java          → Interceptor를 모든 경로("/**")에 등록
WebGateInterceptor.java → preHandle()에서 URI별 gateId 매핑 후 WebGate 호출
WebGate.java            → 유량제어 판정 라이브러리 (마스터 소스)
SampleController.java   → 샘플 페이지 경로 매핑
```

`WebConfig`가 `/**` 전체에 Interceptor를 걸기 때문에, `WebGateInterceptor.preHandle()`에서 **gateId가 null인 경로는 반드시 switch-case default로 null 처리**해야 정적 리소스 등이 차단되지 않음.

## serviceId / gateId 설정 위치

- **Interceptor 방식**: `WebGateInterceptor.java` 내 switch-case
- **Controller 방식**: 각 Controller 메서드 내
- **JSP 방식**: 각 JSP 파일 상단 include 직후
- **현재 샘플 값**: `serviceId = "9000"`, `gateId = "1"` (실제 고객 값으로 교체 필요)

## 응답 교체 모드

- **Replace MODE**: `WG_GetWaitingUi()` HTML로 응답 직접 교체 (현재 샘플 기본값)
- **Redirect MODE**: CDN 대기 페이지로 리다이렉트 (주석 처리된 코드 참조)

## API 서버 통신

- API 엔드포인트: `https://{serviceId}-{n}.devy.kr/` (n = 0~5, 6개 서버 로드밸런싱)
- SSRF 방어: `WG_IsValidApiUrl()`이 `^\d{4,20}-\w{1,10}\.devy\.kr` 패턴만 허용
- Timeout: MATCHING 3~9초(retry별 증가), OUT 10초
- API 호출 실패(ResultCode < 0) 시 통과(`false`) 처리 — 서비스 중단 방지
- VBScript(asp)는 JSON 파서가 없어 `WG_JsonGetStr/Int` (RegExp 기반) 헬퍼로 응답 파싱

## 쿠키 설계

| 쿠키명 | 용도 | 만료 |
|--------|------|------|
| `WG_CLIENT_ID` | 클라이언트 식별 토큰 키 (12자, 체크코드 포함) | 365일 |
| `WG_TOKEN_NO` | 대기 토큰 번호 | 1일 |
| `WG_WAS_IP` | 담당 게이트 서버 호스트 | 1일 |
| `WG_GATE_ID` | 현재 게이트 ID | 1일 |
| `WG_TRACE_LEVEL` | 디버그 레벨 (0/1/2, 쿠키로 세팅) | — |

`Secure` 플래그는 로컬 테스트 지원을 위해 주석 처리되어 있음. 운영 배포 시 주석 해제 필요.
