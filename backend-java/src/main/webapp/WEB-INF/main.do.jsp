<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!--
#################################################################################################
backend 호출코드는 JAVA controller에서 동작합니다.
이 페이지는 ACK 동작을 위한 SAMPLE CODE 입니다. 
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
                <h1 class="title">대문페이지(main.do) SAMPLE</h1>
                <h2 class="has-text-light">페이지 첫 진입 시 Backend 호출코드가 동작하여 대기표발급 페이지(intro.html)를 거쳐 왔습니다. (TEST MODE인 경우 대기화면 표시)</h2>
                <h2 class="has-text-light">FreePass 설정을 초과하여 새로고침을 반복하면 대기표발급 페이지(intro.html)를 다시 다녀옵니다.</h2>
                <h2 class="has-text-light">AdminPage의 FreePass 즉시초기화 버튼을 클릭하면 즉시 intro 페이지를 다녀옵니다. </h2>
                <hr/>
                <a href="/purchase/couponSelect.do">구매페이지(/purchase/couponSelect.do)</a>
            </div>
        </form>
    </div>

	<!-- begin of megafence -->
	<!-- 유입차단 모드용 Token validation 코드입니다.
		★TO-DO : 개발/운영 환경에 맞는 stage 값 세팅 
	-->
	<!-- begin of megafence -->
    <script defer src="https://cdn2.devy.kr/7070/js/webgate.js?v=1"></script>    
    <script>
    	var stage = "DEV";  // SET "DEV" OR "PRD"
    	var introPage = stage == "DEV" ? "/intro.html" : "https://cdn.pintopia.co.kr/intro.html"; 
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
</html>