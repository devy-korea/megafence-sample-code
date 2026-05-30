# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

**메가펜스(MegaFence)** 트래픽 유량제어(Virtual Waiting Room) 서비스의 백엔드 라이브러리 샘플 코드 모음입니다.  
고객사가 자신의 서버에 유량제어 코드를 삽입할 수 있도록 언어/플랫폼별 구현체를 제공합니다.

## 빌드 및 실행

### backend-java-intercepter (메인 Java 샘플)
```bash
cd backend-java-intercepter
mvn spring-boot:run          # 개발 실행 (포트 8080)
mvn clean package            # WAR 빌드
```

### backend-java / backend-jsp
```bash
cd backend-java   # 또는 backend-jsp
./mvnw spring-boot:run
```

> 로컬 PC에서는 유량제어 API 호출이 정상 동작하지 않을 수 있음. SSL(https) 적용된 웹서버에서 테스트 권장.

## 아키텍처

### 소스 관계 (중요)

**`backend-java-intercepter/src/main/java/com/devy/megafence/WebGate.java`가 마스터 소스**입니다.  
나머지 폴더의 라이브러리 파일들은 모두 이 파일을 각 개발 환경에 맞게 컨버팅한 결과물입니다.

```
WebGate.java (backend-java-intercepter)  ← 마스터 소스
    ├── webgate-lib.jsp   (backend-jsp)   ← Java → JSP 문법 컨버팅
    ├── webgate-lib.php   (backend-php)   ← Java → PHP 컨버팅
    ├── webgate-lib.asp   (backend-asp)   ← Java → VBScript 컨버팅
    └── WebGateLib.cs     (backend-aspx)  ← Java → C# 컨버팅
```

따라서 **로직 변경은 항상 `WebGate.java`에서 먼저 수행**하고, 이후 해당 변경을 각 컨버팅 파일에 동일하게 반영합니다.

### 핵심 동작 원리

3단계 유량제어 판정 흐름:

```
STEP-1: URL Parameter (WG_TOKEN) 검증  → ACTION=OUT API 호출
STEP-2: Cookie (WG_TOKEN_NO + WG_CLIENT_ID + WG_WAS_IP) 검증 → ACTION=OUT API 호출
STEP-3: 신규 접속자 → ACTION=MATCHING API 호출 (최대 3회 retry)
```

- `WG_IsNeedToWaiting()` 반환값 `true` → 대기 필요 (응답을 대기UI로 교체)
- `WG_IsNeedToWaiting()` 반환값 `false` → 통과 (정상 응답 진행)

Gate 운영모드: `GATE` (정상 대기열) / `ALERT` (서비스 미오픈 등 알림)  
Token 상태: `WAIT` / `CONNECT` / `IN` / `OUT`

### 폴더별 구현 방식

| 폴더 | 방식 | 라이브러리 파일 |
|------|------|-----------|
| `backend-java-intercepter/` ⭐ | Spring Boot + Interceptor | `WebGate.java` **(마스터)** |
| `backend-jsp/` | JSP include 방식 | `webgate-lib.jsp` (컨버팅) |
| `backend-php/` | PHP require 방식 | `webgate-lib.php` (컨버팅) |
| `backend-asp/` | Classic ASP | `webgate-lib.asp` (컨버팅) |
| `backend-aspx/` | ASP.NET C# | `WebGateLib.cs` (컨버팅) |

### backend-java-intercepter 내부 구조

```
WebConfig.java          → Interceptor를 모든 경로("/**")에 등록
WebGateInterceptor.java → preHandle()에서 URI별 gateId 매핑 후 WebGate 호출
WebGate.java            → 유량제어 판정 라이브러리 (마스터 소스)
SampleController.java   → 샘플 페이지 경로 매핑
```

`WebConfig`가 `/**` 전체에 Interceptor를 걸기 때문에, `WebGateInterceptor.preHandle()`에서 **gateId가 null인 경로는 반드시 switch-case default로 null 처리**해야 정적 리소스 등이 차단되지 않음.

## 소스 수정 규칙

### 수정 순서
1. `backend-java-intercepter/WebGate.java` (마스터)를 먼저 수정
2. 변경 내용을 해당 컨버팅 파일들에 동일하게 반영

### 현재 컨버팅 파일 버전 현황
- `WebGate.java`: V26.1.**520** — STEP-1에서 `ACTION=OUT` 직행
- `webgate-lib.jsp`: V26.1.**510** — STEP-1에서 `ACTION=ACK` → `ACTION=OUT` 2단계, STEP-2B(서브도메인 쿠키) 추가 (미반영 상태)

### serviceId / gateId 설정 위치
- **Interceptor 방식**: `WebGateInterceptor.java` 내 switch-case
- **Controller 방식**: 각 Controller 메서드 내
- **JSP 방식**: 각 JSP 파일 상단 include 직후
- **현재 샘플 값**: `serviceId = "9000"`, `gateId = "1"` (실제 고객 값으로 교체 필요)

### 응답 교체 모드
- **Replace MODE**: `WG_GetWaitingUi()` HTML로 응답 직접 교체 (현재 샘플 기본값)
- **Redirect MODE**: CDN 대기 페이지로 리다이렉트 (주석 처리된 코드 참조)

## API 서버 통신

- API 엔드포인트: `https://{serviceId}-{n}.devy.kr/` (n = 0~5, 6개 서버 로드밸런싱)
- SSRF 방어: `WG_IsValidApiUrl()`이 `^\d{4,20}-\w{1,10}\.devy\.kr` 패턴만 허용
- Timeout: MATCHING 3~9초(retry별 증가), OUT 10초, ACK 5초
- API 호출 실패(ResultCode < 0) 시 통과(`false`) 처리 — 서비스 중단 방지

## 쿠키 설계

| 쿠키명 | 용도 | 만료 |
|--------|------|------|
| `WG_CLIENT_ID` | 클라이언트 식별 토큰 키 (12자, 체크코드 포함) | 365일 |
| `WG_TOKEN_NO` | 대기 토큰 번호 | 1일 |
| `WG_WAS_IP` | 담당 게이트 서버 호스트 | 1일 |
| `WG_GATE_ID` | 현재 게이트 ID | 1일 |
| `WG_TRACE_LEVEL` | 디버그 레벨 (0/1/2, 쿠키로 세팅) | — |

`Secure` 플래그는 로컬 테스트 지원을 위해 주석 처리되어 있음. 운영 배포 시 주석 해제 필요.
