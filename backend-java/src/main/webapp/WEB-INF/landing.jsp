<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!--
################################################################################################# 
대기전용 landing page
################################################################################################# 
-->

<!DOCTYPE html>
<html>
<!--
* ==============================================================================================
* 메가펜스 유량제어서비스 Landing 방식 샘플 V.24.1
* ----------------------------------------------------------------------------------------------
* 이 샘플을 메가펜스 서비스 계약 및 테스트(POC) 고객에게 제공됩니다.
* 허가된 대상 및 목적 이외의 무단 복사, 배포, 수정, 동작 등 일체의 이용을 금합니다.
* ----------------------------------------------------------------------------------------------
* 작성자 : ysd@devy.co.kr
* ----------------------------------------------------------------------------------------------

※ 하단부에 유량제어 서비스 호출 예시가 설명되어 있습니다
-->
<head>

    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta http-equiv="Cache-Control" content="no-cache, no-store, must-revalidate">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, minimum-scale=1.0, user-scalable=no">

</head>
<body>
    <!--#########################################################################################
        begin of megafence
        #########################################################################################-->
    <!--begin of init & import -->
    <script src="https://cdn2.devy.kr/9000/js/webgate.js?v=1"></script>
    <!--end of init & import-->
    <!--begin of calling code-->
    <script>
        // 페이지 load 시 유량제어 호출합니다
        window.addEventListener("load", function () {
            var config = {
                gateId: 1,                          // GATE-ID
                uiMode: "BACKEND",                  // FRONTEND : 대기취소버튼 표시, BACKEND : 대기취소버튼 미표시
                topTitle: "서비스 접속 대기 중",    // 대기UI 제목
                uiShowDelay: 0 * 1000,              // 대기UI 표시 지연시간(ms)
                uiHideDelay: 10 * 1000,             // 대기 종료 후 대기UI 감추기 지연시간(ms)
                resetForce: false,                  // 대기표 강제초기화. true:실행시마다 대기표 강제발급, false:유효한 내 대기표가 있으면 사용
                customType: "",                     // UI 분기 변경 필요 시
                onSuccess: function (data) {
                    console.log("onSuccess", data);
                    console.log("대기완료. 여기에서 페이지 이동처리를 구현해 주세요...");
					window.location.href = "/index";  /* Backend에서 Redirect했던 페이지로 복귀 */
                },
                onAlert: function (data) {
                    console.log("onAlert", data);
                    alert(res.GateOperationMessageDetail);
                },
                onWaiting: function (data) {
                    console.log("onWaiting", data);
                },
                onFail: function (data) { // 유량제어 서비스 장애 시 처리
                    console.log("onFail", data);
                    alert("죄송합니다. 잠시 후 다시 시도해 주세요");
                },
                onFinally: function () {
                    console.log("onFinally : 호출코드 종료");
                }
            };
            // call WG_StartWebGate
            WG_StartWebGate(config);
        });
    </script>
    <!--end of calling code-->
    <!--end of megafence -------------------------------------------------------------------->


</body>
</html>
