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
                <h1 class="title">BACKEND : REPLACE 방식</h1>
                <h2 class="has-text-light">페이지 첫 진입 시 Backend 호출코드가 동작하였습니다. (TEST MODE인 경우 대기화면 표시)</h2>
                <h2 class="has-text-light">FreePass 설정을 초과하여 새로고침을 반복하면 유량제어가 다시 체크됩니다.</h2>
                <h2 class="has-text-light">AdminPage의 FreePass 즉시초기화 버튼을 클릭하면 즉시 intro 페이지를 다녀옵니다. </h2>
                
                <hr/>
                <a href="/index">HOME</a>
            </div>
        </form>
    </div>
    
	<!-- begin of megafence -->
	<!-- 유입차단 모드용 Token validation 코드입니다.
		★TO-DO : 
			1) webgate.js script의 src 속성값에서 ServiceId(숫자4자리)맞는지 확인 
			2) 개발/운영 환경에 맞는 introPage 값 세팅 
	-->
	<!-- begin of megafence -->
    <script defer src="https://cdn2.devy.kr/9000/js/webgate.js?v=1"></script>    
    <script>
    	/*
    	ex) var introPage = "intro.html"; // 개발 시 
    	ex) var introPage = "https://cdn.yourdomain.com/intro.html"; //운영 시   	
    	*/ 
    	var introPage = ""; // 유입차단모드 미사용 시 공백
        function WG_PostInit() {
            WG_SetACK({
                invalidTokenUrl : introPage,
                notOpenUrl      : introPage,
                errorUrl        : introPage
            });
        }
    </script>
    <!-- end of megafence -->
</body>
