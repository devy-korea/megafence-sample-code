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
    <style>
    	button.button {
    		min-width:10rem;
    	}
    	a.button.is-large {
    		margin-top:0.5rem;
    		min-width:20rem;
    	}
    </style>
</head>
<body>
    <div id="app" class="container">
        <form id="form1">
            <div class="notification is-info">
                <h1 class="title">BACKEND SAMPLE HOME</h1>
                <h2 class="has-text-light">Backend 방식의 유량제어를 적용한 SAMPLE 업무페이지.</h2>
                <h2 class="has-text-light">대문(index), 이벤트 안내, 상품상세, 주문하기 페이지 등 Backend 코드(JAVA) 삽입이 가능한 모든 페이지에 적용할 수 있습니다.</h2>
                <br/>
                
                <p class="title">REPLACE 또는 REDIRECT 방식 권장</p>
                
                <br/>
                
                
                <div class='box'>
                <p> <button type="button" class="button is-dark is-small" onclick="$WG.cancelWebGate(); alert('OK');">대기표 초기화</button> 클릭 후 아래 동작방식 테스트 </p>
                <a class="button is-large is-danger" href="/backend_replace">응답교체(REPLACE) 방식 </a>
                <p class="has-text-info"> Backend API 체크 + PAGE 응답교체
                </div>
                <div class='box'>
                <p> <button type="button" class="button is-dark is-small" onclick="$WG.cancelWebGate(); alert('OK');">대기표 초기화</button> 클릭 후 아래 동작방식 테스트 </p>
                <a class="button is-large is-danger" href="/backend_landing">LANDING (REDIRECT) 방식</a>
                <p class="has-text-info"> Backend API 체크 + 대기용 페이지 Redirect 
                </div>
                <div class='box'>
                <p> <button type="button" class="button is-dark is-small" onclick="$WG.cancelWebGate(); alert('OK');">대기표 초기화</button> 클릭 후 아래 동작방식 테스트 </p>
                <a class="button is-large is-danger" href="/backend_intro">INTRO (REDIRECT) 방식</a>
                <p class="has-text-info"> Backend API 체크 + 대기용 CDN 페이지 Redirect 
                </div>
                <div class='box'>
                <p> <button type="button" class="button is-dark is-small" onclick="$WG.cancelWebGate(); alert('OK');">대기표 초기화</button> 클릭 후 아래 동작방식 테스트 </p>
                <a class="button is-large is-danger" href="/backend_noapi">API 미사용 방식 (특수 환경용) </a>
                <p class="has-text-info"> Backend 쿠키체크 + Frontend API 체크
                </div>	
            </div>
        </form>
    </div>
    
    
	<script src="https://cdn2.devy.kr/9000/js/webgate.js?v=1"></script>    
</body>
