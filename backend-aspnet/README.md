# backend-aspnet

메가펜스 유량제어(Backend 방식) **ASP.NET 통합 데모 솔루션**입니다.
하나의 솔루션(`backend-aspnet.sln`)에 3가지 ASP.NET 스택의 데모 프로젝트를 포함합니다.

| 프로젝트 | 스택 | 대상 프레임워크 | 유량제어 적용 위치 | 라이브러리 |
|----------|------|------------------|--------------------|------------|
| `aspnet-webform` | ASP.NET WebForm | .NET Framework 4.8 | `Default.aspx` → `Page_Load` | `WebGateLib.cs` (원본 C#) |
| `aspnet-mvc` | ASP.NET MVC 5 | .NET Framework 4.8 | `HomeController.Index` → `[WebGateFilter]` (OnActionExecuting) | `WebGateLib.cs` (webform 링크) |
| `aspnet-core` | ASP.NET Core MVC | .NET 10 | `HomeController.Index` → `[WebGateFilter]` (OnActionExecuting) | `Lib/WebGateLib.cs` (Core 컨버팅) |

## 공통 동작 (3-STEP 유량제어)

세 프로젝트 모두 **대문 페이지 로드 시** 메가펜스 유량제어 호출코드가 동작하며,
대기가 필요하면 응답을 대기UI로 교체(Replace 방식)합니다. 동작 로직은 마스터 소스
(`WebGate.java` V26.1.530)와 동일합니다.

```
WG_IsNeedToWaiting() == true  → 대기 필요 → 대기UI로 응답 교체
WG_IsNeedToWaiting() == false → 통과 → 정상 업무로직 진행
```

## 라이브러리(WebGateLib.cs) 관리

- **WebForm / MVC** (.NET Framework): 동일한 C# 원본을 사용. `aspnet-webform/WebGateLib.cs`가 물리 파일이고,
  `aspnet-mvc`는 이를 **링크 참조**하여 단일 소스로 동기화한다.
- **Core** (.NET 10): `System.Web` 미지원으로 `HttpContext`/`System.Text.Json`/`HttpClient` 기반으로
  **컨버팅한 별도 파일**(`aspnet-core/Lib/WebGateLib.cs`)을 사용한다. 동작 로직은 동일하게 유지한다.

> ⚠️ 마스터(`WebGate.java`) 로직 변경 시: WebForm/MVC 공용 C#과 Core 컨버팅본 **둘 다** 반영해야 한다.

## serviceId / gateId

현재 샘플 값은 `serviceId = "9000"`, `gateId = "1"` 입니다. 실제 고객 값으로 교체하세요.

## 실행

- **WebForm / MVC**: Visual Studio에서 `backend-aspnet.sln` 열고 NuGet 복원 후 IIS Express 실행.
- **Core**: `cd aspnet-core && dotnet run` (또는 VS에서 실행). 기본 `http://localhost:5080`.

> 로컬 PC에서는 유량제어 API 호출이 정상 동작하지 않을 수 있습니다. SSL(https) 웹서버에서 테스트 권장.
