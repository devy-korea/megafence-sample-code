<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!--
################################################################################################# 
Controller (IndexController.java)에 유량제어 서비스를 적용했으므로, 
이 페이지(JSP)에는 유량제어 관련 어떤 작업도 필요하지 않습니다!!!
원래의 업무페이지를 그대로 사용합니다.
################################################################################################# 
-->

<!DOCTYPE html>
<html>
<head>
    <!-- custom css -->
    <link href="https://dist.devy.kr/bulma-0.7.1/bulma.css" rel="stylesheet" />
</head>
<body>
    <div id="app" class="container">
        <form id="form1">
            <div class="notification is-info">
                <h1 class="title">SAMPLE 업무페이지 (UNUSE API 방식)</h1>
                <h2 class="has-text-light">WAS 방화벽 등의 이슈로 Backend 호출코드의 API 호출이 불가 시 권장</h2>
                <h2 class="has-text-light">Backend : API 호출 없이 COOKIE 유효성 검사만 하고, 필요 시 대기 UI 화면으로 응답을 교체합니다.</h2>
                <h2 class="has-text-light">Frontend : 페이지 load 시 js로 API 호출을 통해 토큰 유효성 체크를 하고 필요 시 대기화면을 표시(팝업)합니다.</h2>
                <h2 class="has-text-light">Freepass 강제초기화 OR 페이지 새고고침을 5~10회 반복하면 대기UI가 다시 표시 됩니다. </h2>
                
                <hr/>
                <a href="/index">HOME</a>
            </div>
        </form>
    </div>
    
    
    
<!--begin of calling code-->
<script src="https://cdn2.devy.kr/9000/js/webgate.js?v=1"></script>
<script>
    // 페이지 로드 시 유량제어 호출합니다
    window.addEventListener("load", function () {
        // 실제 수정할 코드는 여기 한 줄
        startWebGate(1); // GATE ID
    });

    function startWebGate(gateId) {
        var config = {
            gateId: gateId,                     // GATE-ID
            uiMode: "BACKEND",                  // FRONTEND : 대기취소버튼 표시, BACKEND : 대기취소버튼 미표시
            topTitle: "서비스 접속 대기 중",    // 대기UI 제목
            uiShowDelay: 0 * 1000,              // 대기UI 표시 지연시간(ms)
            uiHideDelay: 1 * 1000,             // 대기 종료 후 대기UI 감추기 지연시간(ms)
            resetForce: false,                  // 대기표 강제초기화. true:실행시마다 대기표 강제발급, false:유효한 내 대기표가 있으면 사용
            customType: "",                     // UI 분기 변경 필요 시
            onSuccess: function (data) {
            },
        };
        // call WG_StartWebGate
        WG_StartWebGate(config);
    }
</script>
<!--end of calling code-->
    
</body>
