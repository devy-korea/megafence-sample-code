<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!--
#################################################################################################
구매하기 페이지 DEMO PAGE입니다.
frontend 토큰 유효성 체크 및 결제버튼 클릭 시 유량제어 호출 코드가 구현되어 있습니다.
begin of ~ end of megafence 코드블럭 그대로 복사 후 TO-DO 부분 수정해서 사용 바랍니다. 
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
                <h1 class="title">구매페이지(couponSelect.do) SAMPLE</h1>
                <h2 class="has-text-light">페이지 첫 진입 시 Backend 호출코드가 동작하여 대기표발급 페이지(intro.html)를 거쳐 왔습니다. (TEST MODE인 경우 대기화면 표시)</h2>
                <h2 class="has-text-light">FreePass 설정을 초과하여 새로고침을 반복하면 대기표발급 페이지(intro.html)를 다시 다녀옵니다.</h2>
                <h2 class="has-text-light">AdminPage의 FreePass 즉시초기화 버튼을 클릭하면 즉시 intro 페이지를 다녀옵니다. </h2>
                <hr/>
                <a href="/main.do">대문페이지(/main.do)</a>
                <hr/>
                
                <button id="payButton" type="button" class="button is-large is-danger" onclick="startWebGate(26492)">결제하기</button>
            </div>
        </form>
    </div>

	<!-- begin of megafence -->
	<!-- 유입차단 모드용 Token validation 코드입니다.
		★TO-DO #1: 개발/운영 환경에 맞는 stage 값 세팅 
		★TO-DO #2: 결제하기 버튼 클릭 시 startWebGate(26492) 호출 처리 사용
		★TO-DO #3: startWebGate()의 callback 처리부 (onSuccess)에서 업무로직 시작처리 (goCoupon() call..)
	-->
	<!-- begin of megafence -->
    <script defer src="https://cdn2.devy.kr/7070/js/webgate.js?v=1"></script>    
    <script>
    	// 토큰 유효성 검사
    	var stage = "DEV";  // SET "DEV" OR "PRD"
    	var introPage = stage == "DEV" ? "/intro.html" : "https://cdn.pintopia.co.kr/intro.html"; 
        function WG_PostInit() {
            WG_SetACK({
                invalidTokenUrl : introPage,
                notOpenUrl      : introPage,
                errorUrl        : introPage
            });
        }
		
        // 버튼클릭 용 유량제어 
        function startWebGate(gateId) {
	        //console.log("유량제어 호출코드 start");
	
	        var config = {
	            gateId: gateId,                     // GATE-ID
	            uiMode: "BACKEND",                 // FRONTEND : 대기취소버튼 표시, BACKEND : 대기취소버튼 미표시
	            topTitle: "서비스 접속 대기 중",    // 대기UI 제목
	            uiShowDelay: 0 * 1000,              // 대기UI 표시 지연시간(ms)
	            uiHideDelay: 1 * 1000,              // 대기 종료 후 대기UI 감추기 지연시간(ms)
	            resetForce: false,                  // 대기표 강제초기화. true:실행시마다 대기표 강제발급, false:유효한 내 대기표가 있으면 사용
	            customType: "",                     // UI 분기 변경 필요 시
	            onDataReceive : function (data) {
	                console.log("onDataReceive", data);
	            },
	            onSuccess: function (data) {
	                console.log("onSuccess", data);
	                $WG.hideUi();
	                // 버튼 클릭 시 동작하는 업무로직을 여기에서 시작해 주세요
	                alert("여기에서 결제 팝업 띄우기(goCoupon())");
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
	            }
	        };
	        // call WG_StartWebGate
	        WG_StartWebGate(config);
	    }    
    </script>
    <!-- end of megafence -->
    
</body>
</html>